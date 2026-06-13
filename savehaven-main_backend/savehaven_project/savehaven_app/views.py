from django.contrib.auth.models import User
from django.shortcuts import get_object_or_404
from rest_framework import status
from rest_framework.authtoken.models import Token
from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import AllowAny, IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from .models import Device, Home, SecurityAlert, SystemState
from .mqtt import publish_command, publish_system_command
from .serializers import (
    DeviceCommandSerializer,
    DeviceSerializer,
    DeviceWriteSerializer,
    HomeSerializer,
    SecurityAlertSerializer,
    SystemStateSerializer,
)


def _default_home() -> Home:
    home, _ = Home.objects.get_or_create(
        slug="default",
        defaults={
            "name": "SafeHaven Residence",
            "address": "1248 Sentinel Heights, Austin, TX 78701",
        },
    )
    return home


class HealthView(APIView):
    permission_classes = [AllowAny]

    def get(self, request):
        from .mqtt import get_mqtt_client

        client = get_mqtt_client()
        return Response(
            {
                "status": "ok",
                "mqtt_connected": client is not None and client.is_connected(),
            }
        )


class HomeListView(APIView):
    permission_classes = [AllowAny]

    def get(self, request):
        homes = Home.objects.all()
        return Response(HomeSerializer(homes, many=True).data)


class DeviceListCreateView(APIView):
    permission_classes = [AllowAny]

    def get(self, request, home_slug):
        home = get_object_or_404(Home, slug=home_slug)
        devices = home.devices.all()
        return Response(DeviceSerializer(devices, many=True).data)

    def post(self, request, home_slug):
        home = get_object_or_404(Home, slug=home_slug)
        serializer = DeviceWriteSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        device = serializer.save(home=home)
        return Response(DeviceSerializer(device).data, status=status.HTTP_201_CREATED)


class DeviceDetailView(APIView):
    permission_classes = [AllowAny]

    def patch(self, request, home_slug, device_id):
        home = get_object_or_404(Home, slug=home_slug)
        device = get_object_or_404(Device, home=home, id=device_id)
        serializer = DeviceWriteSerializer(device, data=request.data, partial=True)
        serializer.is_valid(raise_exception=True)
        device = serializer.save()
        return Response(DeviceSerializer(device).data)

    def delete(self, request, home_slug, device_id):
        home = get_object_or_404(Home, slug=home_slug)
        device = get_object_or_404(Device, home=home, id=device_id)
        device.delete()
        return Response(status=status.HTTP_204_NO_CONTENT)


class DeviceCommandView(APIView):
    permission_classes = [AllowAny]

    def post(self, request, home_slug, device_id):
        home = get_object_or_404(Home, slug=home_slug)
        device = get_object_or_404(Device, home=home, id=device_id)
        serializer = DeviceCommandSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        action = serializer.validated_data["action"]
        value = serializer.validated_data.get("value")
        payload = {"action": action}
        if value is not None:
            payload["value"] = value

        publish_command(home.slug, device.slug, payload)
        _apply_local_command(device, action, value)

        return Response({"status": "command_sent", "action": action})


def _apply_local_command(device: Device, action: str, value: float | None) -> None:
    if action == "toggle":
        device.is_on = not device.is_on
        if device.device_type == Device.DeviceType.LIGHT:
            device.status_text = (
                f"On • {int(device.float_value or 80)}% Brightness"
                if device.is_on
                else "Off"
            )
    elif action == "lock":
        device.is_on = True
        device.status_text = "Locked"
    elif action == "unlock":
        device.is_on = False
        device.status_text = "Unlocked"
    elif action == "open_gate":
        device.is_on = False
        device.status_text = "Open"
    elif action == "close_gate":
        device.is_on = True
        device.status_text = "Closed"
    elif action == "set_temperature" and value is not None:
        device.float_value = value
        device.status_text = f"Cooling to {int(value)}°F"
        device.is_on = True

    device.save()


class AlertListView(APIView):
    permission_classes = [AllowAny]

    def get(self, request, home_slug):
        home = get_object_or_404(Home, slug=home_slug)
        alerts = home.alerts.all()
        return Response(SecurityAlertSerializer(alerts, many=True).data)

    def post(self, request, home_slug):
        import json
        from .mqtt import get_mqtt_client

        home = get_object_or_404(Home, slug=home_slug)
        title = request.data.get("title", "Alert")
        message = request.data.get("message", "")
        level = request.data.get("level", "Info")
        tag = request.data.get("tag", "System")

        client = get_mqtt_client()
        if client is not None and client.is_connected():
            topic = f"safehaven/{home_slug}/alerts"
            payload = {
                "title": title,
                "message": message,
                "level": level,
                "tag": tag
            }
            client.publish(topic, json.dumps(payload), qos=1)
            return Response({"status": "alert_published"})
        else:
            from .models import SecurityAlert
            alert = SecurityAlert.objects.create(
                home=home,
                title=title,
                message=message,
                level=level,
                tag=tag
            )
            return Response(SecurityAlertSerializer(alert).data, status=status.HTTP_201_CREATED)


class AlertReadView(APIView):
    permission_classes = [AllowAny]

    def patch(self, request, home_slug, alert_id):
        home = get_object_or_404(Home, slug=home_slug)
        alert = get_object_or_404(SecurityAlert, home=home, id=alert_id)
        alert.is_read = True
        alert.save(update_fields=["is_read"])
        return Response(SecurityAlertSerializer(alert).data)


class SystemStateView(APIView):
    permission_classes = [AllowAny]

    def get(self, request, home_slug):
        home = get_object_or_404(Home, slug=home_slug)
        state, _ = SystemState.objects.get_or_create(home=home)
        return Response(SystemStateSerializer(state).data)

    def patch(self, request, home_slug):
        home = get_object_or_404(Home, slug=home_slug)
        state, _ = SystemState.objects.get_or_create(home=home)
        serializer = SystemStateSerializer(state, data=request.data, partial=True)
        serializer.is_valid(raise_exception=True)
        state = serializer.save()

        publish_system_command(
            home.slug,
            {
                "action": "set_armed_mode",
                "is_armed": state.is_armed,
                "armed_mode": state.armed_mode,
            },
        )
        return Response(SystemStateSerializer(state).data)


@api_view(["POST"])
@permission_classes([AllowAny])
def register_user(request):
    username = request.data.get("username") or request.data.get("email")
    email = request.data.get("email", "")
    password = request.data.get("password", "")

    if not username or len(password) < 6:
        return Response(
            {"detail": "Username/email and password (min 6 chars) are required."},
            status=status.HTTP_400_BAD_REQUEST,
        )

    if User.objects.filter(username=username).exists():
        return Response({"detail": "User already exists."}, status=status.HTTP_400_BAD_REQUEST)

    user = User.objects.create_user(username=username, email=email, password=password)
    token, _ = Token.objects.get_or_create(user=user)
    return Response({"token": token.key, "username": user.username}, status=status.HTTP_201_CREATED)


@api_view(["POST"])
@permission_classes([AllowAny])
def login_user(request):
    from django.contrib.auth import authenticate

    username = request.data.get("username") or request.data.get("email")
    password = request.data.get("password", "")
    user = authenticate(username=username, password=password)

    if user is None:
        return Response({"detail": "Invalid credentials."}, status=status.HTTP_401_UNAUTHORIZED)

    token, _ = Token.objects.get_or_create(user=user)
    return Response({"token": token.key, "username": user.username})


@api_view(["GET"])
@permission_classes([IsAuthenticated])
def profile(request):
    return Response(
        {
            "username": request.user.username,
            "email": request.user.email,
            "default_home": HomeSerializer(_default_home()).data,
        }
    )


@api_view(["POST"])
@permission_classes([AllowAny])
def forgot_password(request):
    email = request.data.get("email")
    if not email:
        return Response({"detail": "Email is required."}, status=status.HTTP_400_BAD_REQUEST)

    user = User.objects.filter(email=email).first() or User.objects.filter(username=email).first()
    if not user:
        return Response({"detail": "User with this email does not exist."}, status=status.HTTP_400_BAD_REQUEST)

    # Log simulated reset link
    print(f"\n--- SIMULATED PASSWORD RESET EMAIL TO {user.email or user.username} LOGGED ---", flush=True)
    print(f"Password reset link: http://127.0.0.1:8000/api/auth/reset-password/mock-token-for-{user.username}/\n", flush=True)

    return Response({"detail": "Password reset link sent to your email."}, status=status.HTTP_200_OK)

