from django.db import models


class Home(models.Model):
    name = models.CharField(max_length=120)
    slug = models.SlugField(max_length=80, unique=True)
    address = models.TextField(blank=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ["name"]

    def __str__(self):
        return self.name


class Device(models.Model):
    class DeviceType(models.TextChoices):
        GATE = "gate", "Gate"
        CAMERA = "camera", "Camera"
        MOTION_SENSOR = "motion_sensor", "Motion Sensor"
        THERMOSTAT = "thermostat", "Thermostat"
        LOCK = "lock", "Smart Lock"
        LIGHT = "light", "Light"
        HUB = "hub", "Hub"

    class Category(models.TextChoices):
        SECURITY = "Security", "Security"
        CLIMATE = "Climate", "Climate"
        LIGHTING = "Lighting", "Lighting"

    home = models.ForeignKey(Home, on_delete=models.CASCADE, related_name="devices")
    name = models.CharField(max_length=120)
    slug = models.SlugField(max_length=80)
    device_type = models.CharField(max_length=32, choices=DeviceType.choices)
    category = models.CharField(max_length=32, choices=Category.choices)
    status_text = models.CharField(max_length=160, blank=True)
    float_value = models.FloatField(default=0.0)
    icon_name = models.CharField(max_length=32)
    is_on = models.BooleanField(default=False)
    is_online = models.BooleanField(default=True)
    mqtt_topic = models.CharField(max_length=255, blank=True)
    last_seen = models.DateTimeField(null=True, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        ordering = ["category", "name"]
        unique_together = [("home", "slug")]

    def __str__(self):
        return f"{self.name} ({self.device_type})"

    @property
    def mqtt_device_path(self):
        return self.mqtt_topic or f"devices/{self.slug}"


class SecurityAlert(models.Model):
    class Level(models.TextChoices):
        CRITICAL = "Critical", "Critical"
        WARNING = "Warning", "Warning"
        INFO = "Info", "Info"

    home = models.ForeignKey(Home, on_delete=models.CASCADE, related_name="alerts")
    device = models.ForeignKey(
        Device,
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name="alerts",
    )
    title = models.CharField(max_length=160)
    message = models.TextField()
    level = models.CharField(max_length=16, choices=Level.choices, default=Level.INFO)
    tag = models.CharField(max_length=80, blank=True)
    is_read = models.BooleanField(default=False)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ["-created_at"]

    def __str__(self):
        return self.title

    @property
    def timestamp(self):
        return self.created_at.strftime("%H:%M")

    @property
    def date_group(self):
        from django.utils import timezone

        today = timezone.localdate()
        alert_date = timezone.localtime(self.created_at).date()
        if alert_date == today:
            return "Today"
        if (today - alert_date).days == 1:
            return "Yesterday"
        return alert_date.strftime("%b %d")


class SystemState(models.Model):
    class ArmedMode(models.TextChoices):
        HOME = "Home", "Home"
        AWAY = "Away", "Away"
        NIGHT = "Night", "Night"
        DISARMED = "Disarmed", "Disarmed"

    home = models.OneToOneField(Home, on_delete=models.CASCADE, related_name="system_state")
    is_armed = models.BooleanField(default=False)
    armed_mode = models.CharField(
        max_length=16,
        choices=ArmedMode.choices,
        default=ArmedMode.DISARMED,
    )
    updated_at = models.DateTimeField(auto_now=True)

    def __str__(self):
        return f"{self.home.slug}: {'Armed' if self.is_armed else 'Disarmed'} ({self.armed_mode})"
