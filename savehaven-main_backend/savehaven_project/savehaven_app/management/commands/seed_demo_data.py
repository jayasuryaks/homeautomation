from django.core.management.base import BaseCommand

from savehaven_app.models import Device, Home, SecurityAlert, SystemState


class Command(BaseCommand):
    help = "Seed SafeHaven demo home, devices, and alerts."

    def handle(self, *args, **options):
        home, created = Home.objects.update_or_create(
            slug="default",
            defaults={
                "name": "SafeHaven Residence",
                "address": "1248 Sentinel Heights, Austin, TX 78701",
            },
        )

        SystemState.objects.update_or_create(
            home=home,
            defaults={"is_armed": True, "armed_mode": SystemState.ArmedMode.AWAY},
        )

        devices = [
            {
                "slug": "main-gate",
                "name": "Main Gate",
                "device_type": Device.DeviceType.GATE,
                "category": Device.Category.SECURITY,
                "status_text": "Closed",
                "icon_name": "router",
                "is_on": True,
            },
            {
                "slug": "driveway-camera",
                "name": "Driveway Camera",
                "device_type": Device.DeviceType.CAMERA,
                "category": Device.Category.SECURITY,
                "status_text": "Live • HD",
                "icon_name": "videocam",
                "is_on": True,
            },
            {
                "slug": "front-door-camera",
                "name": "Front Door Camera",
                "device_type": Device.DeviceType.CAMERA,
                "category": Device.Category.SECURITY,
                "status_text": "Live • HD",
                "icon_name": "videocam",
                "is_on": True,
            },
            {
                "slug": "garage-motion-sensor",
                "name": "Garage Motion Sensor",
                "device_type": Device.DeviceType.MOTION_SENSOR,
                "category": Device.Category.SECURITY,
                "status_text": "Monitoring",
                "icon_name": "sensors",
                "is_on": True,
            },
            {
                "slug": "main-thermostat",
                "name": "Main Thermostat",
                "device_type": Device.DeviceType.THERMOSTAT,
                "category": Device.Category.CLIMATE,
                "status_text": "Cooling to 72°F",
                "float_value": 74.0,
                "icon_name": "thermostat",
                "is_on": True,
            },
            {
                "slug": "front-door-lock",
                "name": "Front Door Lock",
                "device_type": Device.DeviceType.LOCK,
                "category": Device.Category.SECURITY,
                "status_text": "Locked",
                "icon_name": "lock",
                "is_on": True,
            },
            {
                "slug": "living-room-lights",
                "name": "Living Room Lights",
                "device_type": Device.DeviceType.LIGHT,
                "category": Device.Category.LIGHTING,
                "status_text": "On • 80% Brightness",
                "float_value": 80.0,
                "icon_name": "lightbulb",
                "is_on": True,
            },
        ]

        for device_data in devices:
            slug = device_data.pop("slug")
            Device.objects.update_or_create(home=home, slug=slug, defaults=device_data)

        SecurityAlert.objects.filter(home=home).delete()
        SecurityAlert.objects.bulk_create(
            [
                SecurityAlert(
                    home=home,
                    title="System Armed",
                    message="Perimeter secured in Away mode. All sensors active.",
                    level="Info",
                    tag="System",
                ),
                SecurityAlert(
                    home=home,
                    title="Gate Closed",
                    message="Main gate reported closed and latched.",
                    level="Info",
                    tag="Main Gate",
                ),
            ]
        )

        action = "Created" if created else "Updated"
        self.stdout.write(self.style.SUCCESS(f"{action} demo data for home '{home.slug}'."))
