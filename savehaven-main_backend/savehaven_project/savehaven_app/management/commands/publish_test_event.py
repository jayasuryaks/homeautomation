import json

from django.conf import settings
from django.core.management.base import BaseCommand

from savehaven_app.models import Home


class Command(BaseCommand):
    help = "Publish a test MQTT event to the Mosquitto broker."

    def add_arguments(self, parser):
        parser.add_argument("--home", default="default")
        parser.add_argument("--device", default="garage-motion-sensor")
        parser.add_argument(
            "--event",
            default="motion",
            choices=["motion", "person_detected", "gate_opened", "temperature_alert"],
        )

    def handle(self, *args, **options):
        import paho.mqtt.client as mqtt

        home_slug = options["home"]
        device_slug = options["device"]
        event = options["event"]

        if not Home.objects.filter(slug=home_slug).exists():
            self.stderr.write(self.style.ERROR(f"Home '{home_slug}' not found. Run seed_demo_data first."))
            return

        payloads = {
            "motion": {
                "event": "motion",
                "title": "Motion Detected",
                "message": "Garage motion sensor triggered while system is armed.",
                "level": "Warning",
                "tag": "Garage",
            },
            "person_detected": {
                "event": "person_detected",
                "title": "Person Spotted",
                "message": "Person detected at the front door camera.",
                "level": "Info",
                "tag": "Front Door",
            },
            "gate_opened": {
                "event": "gate_opened",
                "title": "Gate Opened",
                "message": "Main gate was opened remotely.",
                "level": "Warning",
                "tag": "Main Gate",
                "is_on": False,
                "status_text": "Open",
            },
            "temperature_alert": {
                "temperature": 82.0,
                "target": 72,
                "mode": "cooling",
            },
        }

        topic_prefix = settings.MQTT_TOPIC_PREFIX.rstrip("/")
        if event == "temperature_alert":
            topic = f"{topic_prefix}/{home_slug}/devices/{device_slug}/telemetry"
        else:
            topic = f"{topic_prefix}/{home_slug}/devices/{device_slug}/event"

        payload = payloads[event]
        client = mqtt.Client(client_id="safehaven-test-publisher")
        if settings.MQTT_USERNAME:
            client.username_pw_set(settings.MQTT_USERNAME, settings.MQTT_PASSWORD or None)

        client.connect(settings.MQTT_BROKER_HOST, settings.MQTT_BROKER_PORT)
        client.publish(topic, json.dumps(payload), qos=1)
        client.disconnect()

        self.stdout.write(self.style.SUCCESS(f"Published to {topic}: {payload}"))
