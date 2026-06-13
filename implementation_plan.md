# SafeHaven Frontend-Backend Sync Plan

Provide a unified integration between the Kotlin Android frontend and the Django REST API backend. This enables the frontend to display correct device states and alerts that synchronize in real time via the Mosquitto MQTT broker.

## User Review Required

> [!IMPORTANT]
> The backend Django server is currently running at `http://localhost:8000` (PID 36800) and is successfully connected to the Mosquitto MQTT broker (`localhost:1883`).
> The frontend application currently displays static dummy data pre-populated in Room DB. We will update the frontend's database synchronization layer to connect directly with the Django backend.
>
> We will configure the Android client to call the backend APIs at `http://10.0.2.2:8000/` (which redirects to localhost in the Android emulator environment).

## Open Questions

There are no major open questions, as the backend's API schemas and fields (camelCase mapping in Django serializers) already perfectly match the fields in the Room entities (`Device`, `SecurityAlert`).

## Proposed Changes

We will introduce a Retrofit REST API interface and configure the `SafeHavenRepository` and `SafeHavenViewModel` to fetch/mutate state on the Django backend, which in turn publishes updates to the Mosquitto broker.

---

### Room Database

#### [MODIFY] [SafeHavenDatabase.kt](file:///c:/Users/Jayas/Desktop/cctv/savehaven-main/savehaven-main/app/src/main/java/com/example/data/SafeHavenDatabase.kt)
- Add a `@Query("DELETE FROM devices") suspend fun deleteAll()` method to `DeviceDao` to clear cached devices when synchronizing from the backend.

---

### Network Client

#### [NEW] [SafeHavenApiService.kt](file:///c:/Users/Jayas/Desktop/cctv/savehaven-main/savehaven-main/app/src/main/java/com/example/data/SafeHavenApiService.kt)
- Create the Retrofit interface `SafeHavenApiService` detailing endpoints for:
  - `POST api/auth/login/` and `api/auth/register/`
  - `GET api/auth/profile/`
  - `GET / POST / PATCH / DELETE api/homes/{home_slug}/devices/`
  - `POST api/homes/{home_slug}/devices/{device_id}/command/`
  - `GET api/homes/{home_slug}/alerts/`
  - `PATCH api/homes/{home_slug}/alerts/{alert_id}/read/`
  - `GET / PATCH api/homes/{home_slug}/system/`

#### [NEW] [RetrofitClient.kt](file:///c:/Users/Jayas/Desktop/cctv/savehaven-main/savehaven-main/app/src/main/java/com/example/data/RetrofitClient.kt)
- Create a singleton object `RetrofitClient` with base URL `http://10.0.2.2:8000/` and configure Moshi converter and OkHttp client with a logging interceptor.

---

### Data Synchronization Layer

#### [MODIFY] [SafeHavenRepository.kt](file:///c:/Users/Jayas/Desktop/cctv/savehaven-main/savehaven-main/app/src/main/java/com/example/data/SafeHavenRepository.kt)
- Add database-clearing and synchronization methods:
  - `suspend fun syncDevices(homeSlug: String)`: Fetches devices from backend and populates Room DB.
  - `suspend fun syncAlerts(homeSlug: String)`: Fetches alerts from backend and populates Room DB.
  - Add API command dispatch methods:
    - `suspend fun sendDeviceCommand(homeSlug: String, deviceId: Int, action: String, value: Float? = null)`
    - `suspend fun createDeviceOnBackend(...)`
    - `suspend fun updateDeviceOnBackend(...)`
    - `suspend fun deleteDeviceOnBackend(...)`

#### [MODIFY] [SafeHavenViewModel.kt](file:///c:/Users/Jayas/Desktop/cctv/savehaven-main/savehaven-main/app/src/main/java/com/example/viewmodel/SafeHavenViewModel.kt)
- Refactor authentication methods (`login`, `register`) to call backend endpoints and fetch user profile.
- Store default home slug and authentication token.
- Call the synchronization methods upon successful login and setup a recurring coroutine job to poll the backend state (every 3 seconds) while the app is active.
- Refactor command methods (`toggleDevicePower`, `toggleLock`, `updateThermostatTemp`, `saveDevice`, `deleteDevice`, `renameDevice`, `markAlertRead`, `triggerPanicAlert`, `addSecurityAlert`, `setSystemArmed`, `setArmedMode`) to perform network requests.

---

### Test Configuration

#### [MODIFY] [ExampleRobolectricTest.kt](file:///c:/Users/Jayas/Desktop/cctv/savehaven-main/savehaven-main/app/src/test/java/com/example/ExampleRobolectricTest.kt)
- Change `@Config(sdk = [36])` to `@Config(sdk = [34])` to fix compatibility with Java 17.

#### [MODIFY] [GreetingScreenshotTest.kt](file:///c:/Users/Jayas/Desktop/cctv/savehaven-main/savehaven-main/app/src/test/java/com/example/GreetingScreenshotTest.kt)
- Change `@Config(..., sdk = [36])` to `@Config(..., sdk = [34])` to fix compatibility with Java 17.

---

## Verification Plan

### Automated Tests
- Run `.\gradlew.bat testDebugUnitTest` to ensure compilation is successful and unit tests pass.

### Manual Verification
- Verify that Django backend and Mosquitto broker are running.
- Run a test MQTT publish to verify the Django backend database updates correctly.
- Verify that calling the synchronized frontend functions correctly propagates state changes to the Django SQLite database.
