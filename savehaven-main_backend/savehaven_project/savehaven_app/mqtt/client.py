import logging
import threading
from typing import Optional

import paho.mqtt.client as mqtt
from django.conf import settings

from .handlers import route_message

logger = logging.getLogger(__name__)

_client: Optional[mqtt.Client] = None
_started = False
_start_lock = threading.Lock()


def _build_client() -> mqtt.Client:
    client_id = settings.MQTT_CLIENT_ID
    if mqtt.CallbackAPIVersion is not None:
        client = mqtt.Client(
            callback_api_version=mqtt.CallbackAPIVersion.VERSION2,
            client_id=client_id,
        )
    else:
        client = mqtt.Client(client_id=client_id)

    if settings.MQTT_USERNAME:
        client.username_pw_set(settings.MQTT_USERNAME, settings.MQTT_PASSWORD or None)

    client.on_connect = _on_connect
    client.on_message = _on_message
    client.on_disconnect = _on_disconnect
    return client


def _on_connect(client, userdata, flags, reason_code, properties=None):
    if reason_code != 0 and str(reason_code) != "Success":
        logger.error("MQTT connect failed: %s", reason_code)
        return

    topic = f"{settings.MQTT_TOPIC_PREFIX.rstrip('/')}/#"
    client.subscribe(topic, qos=1)
    logger.info("MQTT connected; subscribed to %s", topic)


def _on_message(client, userdata, message):
    try:
        route_message(message.topic, message.payload)
    except Exception:
        logger.exception("Failed to process MQTT message on %s", message.topic)


def _on_disconnect(client, userdata, flags, reason_code, properties=None):
    logger.warning("MQTT disconnected: %s", reason_code)


def get_mqtt_client() -> Optional[mqtt.Client]:
    return _client


def start_mqtt_client() -> None:
    global _client, _started

    with _start_lock:
        if _started:
            return
        _started = True

    try:
        _client = _build_client()
        _client.connect(
            settings.MQTT_BROKER_HOST,
            settings.MQTT_BROKER_PORT,
            keepalive=settings.MQTT_KEEPALIVE,
        )
        _client.loop_start()
        logger.info(
            "MQTT client started (%s:%s)",
            settings.MQTT_BROKER_HOST,
            settings.MQTT_BROKER_PORT,
        )
    except Exception:
        logger.exception("Unable to start MQTT client")
        _client = None


def stop_mqtt_client() -> None:
    global _client, _started

    with _start_lock:
        if _client is not None:
            _client.loop_stop()
            _client.disconnect()
            _client = None
        _started = False
