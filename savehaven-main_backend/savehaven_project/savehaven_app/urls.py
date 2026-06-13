from django.urls import path

from . import views

urlpatterns = [
    path("health/", views.HealthView.as_view(), name="health"),
    path("homes/", views.HomeListView.as_view(), name="home-list"),
    path("homes/<slug:home_slug>/devices/", views.DeviceListCreateView.as_view(), name="device-list"),
    path(
        "homes/<slug:home_slug>/devices/<int:device_id>/",
        views.DeviceDetailView.as_view(),
        name="device-detail",
    ),
    path(
        "homes/<slug:home_slug>/devices/<int:device_id>/command/",
        views.DeviceCommandView.as_view(),
        name="device-command",
    ),
    path("homes/<slug:home_slug>/alerts/", views.AlertListView.as_view(), name="alert-list"),
    path(
        "homes/<slug:home_slug>/alerts/<int:alert_id>/read/",
        views.AlertReadView.as_view(),
        name="alert-read",
    ),
    path("homes/<slug:home_slug>/system/", views.SystemStateView.as_view(), name="system-state"),
    path("auth/register/", views.register_user, name="register"),
    path("auth/login/", views.login_user, name="login"),
    path("auth/forgot-password/", views.forgot_password, name="forgot-password"),
    path("auth/profile/", views.profile, name="profile"),
]
