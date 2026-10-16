# Building TindaFlow

Use JDK 21, as requested by `gradle/gradle-daemon-jvm.properties`.
Set Android Studio's Gradle JDK to a local JDK 21 installation. For PowerShell
in this workspace, the JDK installed during build verification can be used with:

```powershell
$env:JAVA_HOME = 'C:\Users\Administrator\.jdks\jdk-21.0.12.1+1'
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:assembleDebugAndroidTest --console=plain
```

On other machines, replace the JDK path with that machine's installation.
`local.properties` must point to the local Android SDK. The project compiles
against API 36; Gradle can install missing SDK components when the corresponding
SDK licenses have already been accepted.

The command builds the debug app, runs local JVM tests, and builds the existing
instrumented test APK. Building the test APK does not execute tests on a device.
The debug app APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

The active plugin versions are declared in the root `build.gradle.kts`, and
application dependencies are declared in `app/build.gradle.kts`. The existing
version catalog currently has different versions but is not used by those
build scripts.

## Verification on 2026-10-15

Executed with Gradle 8.14.5 and Java 21:

```text
:app:assembleDebug :app:testDebugUnitTest :app:assembleDebugAndroidTest :app:assembleRelease :app:lintDebug
BUILD SUCCESSFUL in 1m 40s
122 actionable tasks: 102 executed, 20 up-to-date
```

- Debug APK and instrumented test APK assembled successfully.
- Release APK assembled as `app/build/outputs/apk/release/app-release-unsigned.apk`;
  release signing is not configured.
- All 8 local JVM tests passed (7 DemoStore tests and 1 starter test).
- Debug lint completed with 0 errors and 40 warnings. See
  `app/build/reports/lint-results-debug.html` for details.
- No device or emulator testing was performed.

Build fixes: replaced the unresolved `PathJoin.Round` with `StrokeJoin.Round`
in the sparkline, and added the AndroidX JUnit extension and test runner required
by the existing instrumented test. Installed a local JDK 21; Gradle installed
Android SDK Platform 36 and Build-Tools 35.0.0. No screen layouts, colors,
typography, or image assets were changed.

Remaining non-blocking findings include deprecated icon APIs and Kotlin
`kotlinOptions.jvmTarget`, Gradle deprecations, the unused version catalog's
version differences, unused resources, bitmap density placement, launcher
monochrome configuration, and manifest/backup lint warnings. The SDK parser
also reported a tooling XML version mismatch during initial configuration.
The native graphics path library was packaged without symbol stripping.
Existing demo data remains in memory and is not persisted or synced.
