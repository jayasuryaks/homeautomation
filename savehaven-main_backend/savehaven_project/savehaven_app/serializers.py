from rest_framework import serializers

from .models import Device, Home, SecurityAlert, SystemState


class HomeSerializer(serializers.ModelSerializer):
    class Meta:
        model = Home
        fields = ["id", "name", "slug", "address"]


class DeviceSerializer(serializers.ModelSerializer):
    statusText = serializers.CharField(source="status_text")
    floatValue = serializers.FloatField(source="float_value")
    iconName = serializers.CharField(source="icon_name")
    isOn = serializers.BooleanField(source="is_on")
    isOnline = serializers.BooleanField(source="is_online")
    deviceType = serializers.CharField(source="device_type", read_only=True)
    mqttTopic = serializers.CharField(source="mqtt_topic", read_only=True)

    class Meta:
        model = Device
        fields = [
            "id",
            "name",
            "category",
            "statusText",
            "floatValue",
            "iconName",
            "isOn",
            "isOnline",
            "deviceType",
            "mqttTopic",
        ]

    def to_internal_value(self, data):
        mapped = {}
        field_map = {
            "statusText": "status_text",
            "floatValue": "float_value",
            "iconName": "icon_name",
            "isOn": "is_on",
            "isOnline": "is_online",
        }
        for key, value in data.items():
            mapped[field_map.get(key, key)] = value
        return super().to_internal_value(mapped)


class DeviceWriteSerializer(serializers.ModelSerializer):
    class Meta:
        model = Device
        fields = [
            "name",
            "slug",
            "device_type",
            "category",
            "status_text",
            "float_value",
            "icon_name",
            "is_on",
            "is_online",
            "mqtt_topic",
        ]


class SecurityAlertSerializer(serializers.ModelSerializer):
    timestamp = serializers.CharField(read_only=True)
    dateGroup = serializers.CharField(source="date_group", read_only=True)
    isRead = serializers.BooleanField(source="is_read")

    class Meta:
        model = SecurityAlert
        fields = [
            "id",
            "title",
            "message",
            "timestamp",
            "dateGroup",
            "level",
            "tag",
            "isRead",
        ]


class SystemStateSerializer(serializers.ModelSerializer):
    isArmed = serializers.BooleanField(source="is_armed")
    armedMode = serializers.CharField(source="armed_mode")

    class Meta:
        model = SystemState
        fields = ["isArmed", "armedMode", "updated_at"]

    def to_internal_value(self, data):
        mapped = {}
        field_map = {
            "isArmed": "is_armed",
            "armedMode": "armed_mode",
        }
        for key, value in data.items():
            mapped[field_map.get(key, key)] = value
        return super().to_internal_value(mapped)


class DeviceCommandSerializer(serializers.Serializer):
    action = serializers.ChoiceField(
        choices=[
            "toggle",
            "lock",
            "unlock",
            "open_gate",
            "close_gate",
            "set_temperature",
            "arm",
            "disarm",
        ]
    )
    value = serializers.FloatField(required=False)
