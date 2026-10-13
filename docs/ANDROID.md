# Android development guide

[Back to the Trash Patrol project overview](../README.md)

The Android component uses Kotlin and Jetpack Compose. It captures report and confirmation photos, attaches optional GPS coordinates, and polls the backend for mission progress.

## Install the hackathon snapshot

A [debug APK](../.build-outputs/app-debug.apk) is checked into this repository. It is the hackathon development snapshot.

After installing it, open Settings and replace the historic coordinator URL with your running backend's address. Use **Test Connection** to check `GET /health`. A physical phone needs a reachable LAN address or an active tunnel, rather than the laptop's `localhost`.

## Build from source

1. Clone this repository and open its root in Android Studio.
2. Install the Android SDK required by [app/build.gradle.kts](../app/build.gradle.kts), then sync Gradle using versions compatible with the project's plugins.
3. Restore the local debug keystore referenced by the build:
   ```bash
   python3 -c "import base64, pathlib; pathlib.Path('debug.keystore').write_bytes(base64.b64decode(pathlib.Path('debug.keystore.base64').read_text()))"
   ```
4. Build the debug variant and run it on an emulator or an Android device.

This repository snapshot does not include a Gradle wrapper. If Android Studio requires one, generate a wrapper compatible with the configured Android Gradle plugin before building. Build configuration lives in [build.gradle.kts](../build.gradle.kts), [app/build.gradle.kts](../app/build.gradle.kts), and [the version catalog](../gradle/libs.versions.toml).

The Secrets Gradle plugin reads `.env` with `.env.example` as defaults. Backend GMI and RocketRide credentials belong in the backend environment, not the Android app.

## Screens

| Screen | Behavior |
| --- | --- |
| Report | Capture and preview a photo, submit it for classification, show a rejection reason if the image is rejected |
| Mission | Show backend state, classification evidence, and an animated soccer pitch; request a confirmation photo after robot completion |
| Settings | Set the coordinator URL and check backend health |

The soccer pitch illustrates mission progress. It is not a map of measured robot position.

## Visual design

The World Cup theme uses a dark field background, green success states, gold robot indicators, and referee-card colors for rejection or caution. The pitch is drawn with a Compose canvas; robot and ball animations represent progress through mission stages.

## API integration

The backend contract is documented in the [backend API reference](https://github.com/Muifrend/rocketride/blob/main/docs/DEVELOPMENT.md#api-reference).

| Endpoint | App behavior |
| --- | --- |
| `GET /health` | Test the configured coordinator URL |
| `POST /android/report` | Upload a JPEG report photo, a generated mission ID, and optional location metadata |
| `GET /missions/{mission_id}` | Poll the mission every two seconds |
| `POST /android/confirmation` | Upload a confirmation photo for the active mission |

Photos are compressed to 85% JPEG quality before upload. Location is optional and sent as a JSON string containing latitude and longitude.

## Polling and recovery

[MainViewModel.kt](../app/src/main/java/com/example/MainViewModel.kt) owns the polling loop. It tracks changes to the backend's mission state and shows a local `timeout` state after 30 seconds without a change.

A timeout does not prove the robot failed. A robot command may take longer than the app's wait. **Retry Sync** refetches the mission; if the backend advanced to `awaiting_confirmation_photo`, the app can resume the confirmation flow. The backend guide recommends a longer dispatch wait as a future improvement; that recommendation is not yet implemented in this app.

For network errors, check the coordinator address, backend health, and the tunnel or LAN connection.

## Main source files

- [MainViewModel.kt](../app/src/main/java/com/example/MainViewModel.kt): report submission, confirmation submission, mission polling.
- [NetworkClient.kt](../app/src/main/java/com/example/NetworkClient.kt): HTTP client and service interface.
- [NetworkModels.kt](../app/src/main/java/com/example/NetworkModels.kt): API payload models.
- [Screens.kt](../app/src/main/java/com/example/ui/Screens.kt): Compose screens and soccer-themed visualization.
- [PreferencesManager.kt](../app/src/main/java/com/example/PreferencesManager.kt): coordinator URL storage.

## Dependencies

Jetpack Compose / Material 3, Kotlin coroutines / StateFlow, Retrofit / OkHttp, Moshi, Coil, Google Play Services Location, and Accompanist Permissions.
