import json
import logging
import threading
from typing import Any

from django.conf import settings
from django.utils import timezone

from savehaven_app.models import Device, Home, SecurityAlert, SystemState

logger = logging.getLogger(__name__)

_client = None
_lock = threading.Lock()


def _topic_prefix(home_slug: str) -> str:
    base = settings.MQTT_TOPIC_PREFIX.rstrip("/")
    return f"{base}/{home_slug}"


def publish_command(home_slug: str, device_slug: str, payload: dict[str, Any]) -> None:
    from .client import get_mqtt_client

    topic = f"{_topic_prefix(home_slug)}/devices/{device_slug}/command"
    client = get_mqtt_client()
    if client is None:
        logger.warning("MQTT client unavailable; command not published to %s", topic)
        return
    client.publish(topic, json.dumps(payload), qos=1)
    logger.info("Published MQTT command to %s: %s", topic, payload)


def publish_system_command(home_slug: str, payload: dict[str, Any]) -> None:
    from .client import get_mqtt_client

    topic = f"{_topic_prefix(home_slug)}/system/command"
    client = get_mqtt_client()
    if client is None:
        logger.warning("MQTT client unavailable; system command not published")
        return
    client.publish(topic, json.dumps(payload), qos=1)


def _resolve_home(home_slug: str) -> Home | None:
    try:
        return Home.objects.get(slug=home_slug)
    except Home.DoesNotExist:
        logger.warning("Unknown home slug in MQTT message: %s", home_slug)
        return None


def _resolve_device(home: Home, device_slug: str) -> Device | None:
    try:
        return Device.objects.get(home=home, slug=device_slug)
    except Device.DoesNotExist:
        logger.warning("Unknown device slug %s for home %s", device_slug, home.slug)
        return None


def _create_alert(
    home: Home,
    device: Device | None,
    title: str,
    message: str,
    level: str = "Info",
    tag: str = "",
) -> SecurityAlert:
    return SecurityAlert.objects.create(
        home=home,
        device=device,
        title=title,
        message=message,
        level=level,
        tag=tag,
    )


def handle_status(home_slug: str, device_slug: str, payload: dict[str, Any]) -> None:
    home = _resolve_home(home_slug)
    if home is None:
        return
    device = _resolve_device(home, device_slug)
    if device is None:
        return

    if "online" in payload:
        device.is_online = bool(payload["online"])
    if "is_on" in payload:
        device.is_on = bool(payload["is_on"])
    if "status_text" in payload:
        device.status_text = str(payload["status_text"])
    if "float_value" in payload:
        device.float_value = float(payload["float_value"])

    device.last_seen = timezone.now()
    device.save()


def handle_telemetry(home_slug: str, device_slug: str, payload: dict[str, Any]) -> None:
    home = _resolve_home(home_slug)
    if home is None:
        return
    device = _resolve_device(home, device_slug)
    if device is None:
        return

    temperature = payload.get("temperature")
    target = payload.get("target")
    mode = payload.get("mode", "cooling")

    if temperature is not None:
        device.float_value = float(temperature)
    if target is not None:
        device.status_text = f"{str(mode).title()} to {int(float(target))}°F"
    elif "status_text" in payload:
        device.status_text = str(payload["status_text"])

    if "is_on" in payload:
        device.is_on = bool(payload["is_on"])

    device.is_online = True
    device.last_seen = timezone.now()
    device.save()


def handle_event(home_slug: str, device_slug: str, payload: dict[str, Any]) -> None:
    home = _resolve_home(home_slug)
    if home is None:
        return
    device = _resolve_device(home, device_slug)

    event = payload.get("event", "event")
    title = payload.get("title") or _default_event_title(device, event)
    message = payload.get("message") or f"{device.name if device else device_slug} reported {event}."
    level = payload.get("level", _default_event_level(event))
    tag = payload.get("tag") or (device.name if device else device_slug)

    _create_alert(home, device, title, message, level=level, tag=tag)

    if device is not None:
        if event in {"motion", "person_detected"}:
            device.status_text = "Motion detected"
            device.is_on = True
        elif event == "gate_opened":
            device.status_text = "Open"
            device.is_on = False
        elif event == "gate_closed":
            device.status_text = "Closed"
            device.is_on = True
        elif event == "unlocked":
            device.status_text = "Unlocked"
            device.is_on = False
        elif event == "locked":
            device.status_text = "Locked"
            device.is_on = True
        elif "status_text" in payload:
            device.status_text = str(payload["status_text"])
        if "is_on" in payload:
            device.is_on = bool(payload["is_on"])
        device.is_online = True
        device.last_seen = timezone.now()
        device.save()


def handle_system_state(home_slug: str, payload: dict[str, Any]) -> None:
    home = _resolve_home(home_slug)
    if home is None:
        return

    state, _ = SystemState.objects.get_or_create(home=home)
    if "is_armed" in payload:
        state.is_armed = bool(payload["is_armed"])
    if "armed_mode" in payload:
        state.armed_mode = str(payload["armed_mode"])
        if state.armed_mode == SystemState.ArmedMode.DISARMED:
            state.is_armed = False
        else:
            state.is_armed = True
    state.save()


def _default_event_title(device: Device | None, event: str) -> str:
    mapping = {
        "motion": "Motion Detected",
        "person_detected": "Person Spotted",
        "gate_opened": "Gate Opened",
        "gate_closed": "Gate Closed",
        "temperature_alert": "Temperature Alert",
        "offline": "Device Offline",
        "online": "Device Online",
        "intrusion": "Intrusion Detected",
    }
    return mapping.get(event, f"{device.name if device else 'Device'} Event")


def _default_event_level(event: str) -> str:
    critical = {"intrusion", "panic"}
    warning = {"motion", "gate_opened", "unlocked", "temperature_alert", "offline"}
    if event in critical:
        return "Critical"
    if event in warning:
        return "Warning"
    return "Info"


def route_message(topic: str, payload_bytes: bytes) -> None:
    try:
        payload = json.loads(payload_bytes.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError):
        logger.exception("Invalid MQTT payload on topic %s", topic)
        return

    if not isinstance(payload, dict):
        logger.warning("MQTT payload must be a JSON object on topic %s", topic)
        return

    parts = topic.split("/")
    prefix = settings.MQTT_TOPIC_PREFIX.strip("/")
    prefix_parts = prefix.split("/")

    if len(parts) < len(prefix_parts) + 2:
        logger.warning("Unexpected MQTT topic format: %s", topic)
        return

    if parts[: len(prefix_parts)] != prefix_parts:
        return

    home_slug = parts[len(prefix_parts)]
    remainder = parts[len(prefix_parts) + 1 :]

    if not remainder:
        return

    if remainder[0] == "devices" and len(remainder) >= 3:
        device_slug = remainder[1]
        message_type = remainder[2]
        if message_type == "status":
            handle_status(home_slug, device_slug, payload)
        elif message_type == "telemetry":
            handle_telemetry(home_slug, device_slug, payload)
        elif message_type == "event":
            handle_event(home_slug, device_slug, payload)
    elif remainder[0] == "system" and len(remainder) >= 2:
        if remainder[1] in {"state", "armed"}:
            handle_system_state(home_slug, payload)
    elif remainder[0] == "alerts":
        home = _resolve_home(home_slug)
        if home is None:
            return
        _create_alert(
            home=home,
            device=None,
            title=payload.get("title", "System Alert"),
            message=payload.get("message", ""),
            level=payload.get("level", "Info"),
            tag=payload.get("tag", "System"),
        )
