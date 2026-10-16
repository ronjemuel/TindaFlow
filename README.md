# TindaFlow

TindaFlow is a native Android business-management app in development. Its current implementation combines a retail and service-business demo with account flows backed by an optional Supabase configuration.

## Technologies

- Kotlin, Jetpack Compose, and Material 3
- Android Gradle Plugin 8.11.1, Kotlin 2.2.20, and Gradle 8.14.5
- Supabase Kotlin Auth 3.2.3 and Ktor OkHttp 3.2.3
- Android Keystore for encrypted local authentication storage
- JUnit and AndroidX test tooling

The active build configuration is in the root and app `build.gradle.kts` files. The version catalog contains historical versions that are not used by those scripts.

## Implemented features

- Demo dashboard, sales, orders, inventory, and business switching
- Demo checkout with quantity, discount, stock, and payment handling
- Email registration, login, verification, password recovery, and logout integration
- Session-aware navigation and English/Filipino account messages
- Explicit separation between demo content and authenticated account flows

## Development status

This is a milestone build, not a finished business service. Business records use an in-memory demo store; they are not persisted or synchronized. Creating a real business workspace is reserved for a later milestone. Authentication requires the owner's Supabase configuration and provider setup. Live account journeys and device behavior have not been verified as part of this repository preparation.

Existing development checkpoints and their earlier validation results are under `docs/`. They should be read with their stated limitations. No app functionality was changed during repository preparation.

## Build and run

Install JDK 21 and an Android SDK with API 36. The minimum supported Android API is 26. Configure your SDK locally in Android Studio or in an ignored `local.properties` file.

On Linux or Termux with a compatible Android toolchain:

```sh
chmod +x gradlew
./gradlew :app:assembleDebug
```

On Windows:

```powershell
.\gradlew.bat :app:assembleDebug
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`. Install it on a compatible device or emulator. The Gradle wrapper downloads its configured distribution and dependencies when they are not cached. On-phone builds also require compatible SDK build tools.

### Optional authentication configuration

Copy `supabase.properties.example` to `supabase.local.properties` and set only `SUPABASE_URL` and `SUPABASE_PUBLISHABLE_KEY` for your own project. The build accepts an HTTPS project origin and a publishable key beginning with `sb_publishable_`. Keep the local file out of Git. Server keys, service-role credentials, passwords, and signing keys do not belong in the app.

Configure the email provider and the `tindaflow://auth/callback` redirect in your own Supabase project. The existing auth checkpoint documents the remaining setup and acceptance checks. Demo mode can be explored without this configuration.

### Existing checks

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug
./gradlew :app:assembleDebugAndroidTest
```

Assembling the instrumented-test APK does not run tests on a device. With a suitable connected device, use `./gradlew :app:connectedDebugAndroidTest`.

## Repository layout

- `app/src/main/java/com/tindaflow/app/`: UI, navigation, demo data, and authentication
- `app/src/main/res/`: application resources
- `app/src/test/` and `app/src/androidTest/`: existing tests
- `docs/`: development checkpoints
- `supabase.properties.example`: blank configuration template

Generated builds, IDE caches, local configuration, design handoff packages, and signing material are excluded. No screenshot is presented as a verified current app capture.
