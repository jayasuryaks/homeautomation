from django.contrib import admin

from .models import Device, Home, SecurityAlert, SystemState


@admin.register(Home)
class HomeAdmin(admin.ModelAdmin):
    list_display = ("name", "slug", "address", "created_at")
    prepopulated_fields = {"slug": ("name",)}


@admin.register(Device)
class DeviceAdmin(admin.ModelAdmin):
    list_display = (
        "name",
        "home",
        "device_type",
        "category",
        "status_text",
        "is_on",
        "is_online",
        "last_seen",
    )
    list_filter = ("home", "device_type", "category", "is_online")
    search_fields = ("name", "slug")


@admin.register(SecurityAlert)
class SecurityAlertAdmin(admin.ModelAdmin):
    list_display = ("title", "home", "level", "tag", "is_read", "created_at")
    list_filter = ("home", "level", "is_read")
    search_fields = ("title", "message", "tag")


@admin.register(SystemState)
class SystemStateAdmin(admin.ModelAdmin):
    list_display = ("home", "is_armed", "armed_mode", "updated_at")
