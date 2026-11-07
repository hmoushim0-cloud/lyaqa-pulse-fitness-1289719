# تطبيق لياقة بالس — منصة التدريب الرياضي الذكية (`lyaqa-pulse-fitness-1289719`)
> منظومة fitness المتكاملة مع ربط حزام مراقبة نبضات القلب الذكي عبر البلوتوث منخفض الطاقة

- **Project ID**: `#1289719`
- **Commercial Archetype**: `fitness`
- **Bound Hardware Peripheral**: `Bluetooth BLE Heart Rate Strap (GATT 0x180D)`
- **Package Name**: `sa.factory.lyaqapulsefitness1`
- **Zero-Emoji Invariant**: Strictly Enforced (100% Vector Icons)

---

## [Sidekick Autonomous Update]
- **Summary of Completed Fixes and Features**:
  - **BleHeartRateGattEngine:** Implemented standard Bluetooth SIG Heart Rate Service (GATT Service UUID `0x180D`, Characteristic UUID `0x2A37`). Supports 8-bit and 16-bit BPM decoding, sensor contact status, energy expended parsing (kcal), RR-interval extraction, and autonomic nervous system HRV metrics (rMSSD in ms, pNN50, and readiness recovery score).
  - **AerobicZoneAndVo2MaxEngine:** Implemented comprehensive cardiovascular conditioning engine utilizing the Tanaka maximum heart rate formula (`HRmax = 208 - 0.7 * age`) and Karvonen heart rate reserve (HRR) to calculate 5 athletic training zones (Recovery, Aerobic Fat Burn, Tempo, Anaerobic Threshold, VO2 Max). Includes Uth-Sørensen VO2 Max estimation and Keytel metabolic caloric expenditure burn rates.
  - **Jetpack Compose UI Hardening:** Fixed Kotlin single-quote string interpolation syntax hazards in `HomeScreen.kt` and `TelemetryScreen.kt`, resolved missing `FontWeight` and `sp` imports in `MainActivity.kt`, and replaced deprecated divider components.
  - **Toolchain & Distribution:** Bundled complete Gradle wrapper scripts (`gradle/`, `gradlew`, `gradlew.bat`) and `.gitignore` for zero-friction cloud builds.
  - **Comprehensive Unit Regression Tests:** Established `LyaqaPulseRegressionTest.kt` with 9 unit tests verifying 8-bit/16-bit BPM parsing, sensor contact flags, HRV rMSSD calculation, Tanaka HRmax, Karvonen zone classification, VO2 Max estimation, caloric burn calculations, and driver telemetry streaming.

- **System Architecture**:
  - Clean Architecture & MVVM with unidirectional data flow (`StateFlow` / `SharedFlow`).
  - Modular engine abstractions under `sa.factory.lyaqapulsefitness1.engine` wired directly into `HardwareSensorDriver`.
  - WCAG 2.1 AAA contrast compliance and strict zero-emoji invariant throughout all user-facing strings and logs.

- **Build and Test Verification**:
  - Run unit tests: `./gradlew testDebugUnitTest`
  - Cloud Emulator Test: Executed on `hmoushim0-cloud/Android-test-harness` running Google Pixel 6 (API 34).
