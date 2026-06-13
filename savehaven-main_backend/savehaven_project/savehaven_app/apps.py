import os
import sys

from django.apps import AppConfig


class SavehavenAppConfig(AppConfig):
    default_auto_field = "django.db.models.BigAutoField"
    name = "savehaven_app"

    def ready(self):
        if self._should_start_mqtt():
            from savehaven_app.mqtt.client import start_mqtt_client

            start_mqtt_client()

    def _should_start_mqtt(self) -> bool:
        if os.environ.get("MQTT_AUTO_START", "1") != "1":
            return False
        skip_commands = (
            "migrate",
            "makemigrations",
            "test",
            "seed_demo_data",
            "publish_test_event",
            "shell",
            "createsuperuser",
        )
        if any(command in sys.argv for command in skip_commands):
            return False
        if "runserver" in sys.argv:
            return os.environ.get("RUN_MAIN") == "true"
        return False
