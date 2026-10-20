# TESTING — NexPDF

Reconciles the existing on-device plan (docs/TEST_PLAN.md) with the upgrade program's testing rules.

## Where tests run
| Level | What | Runs on |
|---|---|---|
| CI build | `assembleDebug` compile check | GitHub Actions (every push/PR) |
| JVM unit tests | engine logic: `pdfPoint` rotation algebra, op grouping, page-range parsing, word-rect intersection, journal serialization | Actions + locally if ever available |
| Runtime tests | gestures, saving, erasers, on-device rendering fidelity, encrypted opens | **owner device only — label NOT RUN in issues/logs when not done** |

## Rules (from the program)
- Every issue adds tests appropriate to its scope; engine-layer changes get JVM tests first.
- A successful build is NOT a test result. Never substitute compile success for runtime verification.
- Runtime-claimed behaviors must cite the test-plan item they were exercised under.
- Screenshot/visual checks are the owner's; implementation may attach expected-behavior notes.

## Current coverage
- JVM: seeded on `feat/pdf-upgrade` (issue #28): `PdfAnnotateTest` (pdfPoint rot 0/90/180/270 algebra + stroke-width clamp), `PdfTextTest` (word-gap grouping + toDisplay rotation conversions).
- Instrumented: none (no emulator in CI).
- Manual: docs/TEST_PLAN.md (v0.1.0/v0.1.1 baseline; extend per release).

## Test dependencies (license-reviewed per program §7)
- `org.jetbrains.kotlin:kotlin-test-junit:2.1.20` — Apache-2.0; transitively JUnit 4 (EPL-1.0/BSD-3). Purpose: JVM assertions for pure engine functions without Android SDK. Test-only dependency: zero APK size/runtime impact.

## Performance measures (program targets, definition-of-record)
- Definition when measured: reference device = owner's current phone (record model/Android ver at first measurement).
- Tracked: cold open time, time-to-first-page, scroll jank (missed frames), export duration, memory after 100-page scroll, cancellation latency.
- NOT RUN until instrumented (Phase 6); do not report estimates as measurements.
