# Test plan

The Android CI workflow runs these checks on pushes and pull requests:

1. Local JVM unit tests for Gregorian-to-Jalali conversion, including Nowruz boundaries, zero padding, and invalid date inputs.
2. Android Lint for the debug variant.
3. Debug APK compilation.
4. Instrumented Android tests on an API 35 emulator:
   - Main screen navigation controls are present.
   - SQLite plan and exercise create/list/update/delete behavior.
   - Invalid set counts and negative weights are rejected.
   - Workout log values can be read back for CSV export.

Run locally with JDK 17, Android SDK Platform 35, and Gradle 8.9:

```sh
gradle test lintDebug assembleDebug
gradle connectedDebugAndroidTest
```

The instrumented test task requires a running Android emulator or device.
