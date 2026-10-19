# BUILD ENVIRONMENT — NexPDF

## Philosophy

No local Android SDK. Source is edited locally; Gradle builds execute in GitHub Actions on `ubuntu-latest` (`.github/workflows/ci.yml`).

## Toolchain (pinned)

| Component | Version | Where |
|---|---|---|
| JDK | 17 (temurin) | CI `actions/setup-java@v4` |
| Android SDK | latest via `android-actions/setup-android@v3`; compileSdk 35 | CI |
| Gradle | 8.11.1 (invoked via `gradle/actions/setup-gradle@v4`, `gradle-version` input) | CI |
| AGP | 8.9.1 | `build.gradle.kts` |
| Kotlin | 2.1.20 + `org.jetbrains.kotlin.plugin.compose` | `build.gradle.kts` |
| Compose BOM | 2025.05.01 (material3 1.3.2) | `app/build.gradle.kts` |
| compileSdk / targetSdk / minSdk | 35 / 35 / 26 | `app/build.gradle.kts` |

## Build command (CI)

`gradle :app:assembleDebug --stacktrace --no-daemon`

## Planned local-build replica (only if ever needed, ≥10 GB free)

sudo apt install openjdk-17-jdk-headless → SDK cmdline-tools → sdkmanager "platforms;android-35" "build-tools;35.0.0" → gradle wrapper. **Do not do this while disk < 15 GB free.**

## Future bumps (tracked issues)

- material3 1.4.x (Expressive: MotionScheme, FlexibleBottomSheet) — needs compileSdk 36+ / newer AGP.
- pdfbox-android 2.0.27.0 for tools engine (merge/split/rotate/text/sign).
- Gradle wrapper in repo (removes CI gradle-version coupling).
