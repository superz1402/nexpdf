# SESSION_STATE — NexPDF

Updated: 2026-10-20 (Session 2 — master upgrade program running)

## Repository
- Repo: `superz1402/nexpdf`
- `main`: production, last pushed @ 9467837 (v0.1.1 icon release; https://github.com/superz1402/nexpdf/releases/download/v0.1.1/NexPDF-v0.1.1-debug.apk)
- `feat/pdf-upgrade`: ALL program work happens here (PR #29 → main; merge needs owner approval). HEAD bb896f9→e09c0cd.
- Program parent: issue #13; children #14–#28 (docs/TASK_REGISTER.md + docs/FEATURE_MATRIX.md track status).

## Build pipeline
- CI-only. `.github/workflows/ci.yml`: push to [main, feat/pdf-upgrade] + PRs; runs assembleDebug + testDebugUnitTest. 1-day artifact retention.
- latest green: run 39402694314 @ e09c0cd (26 JVM tests, 0 failures).
- Note: transient silent Kotlin-daemon compile failures observed twice; re-running the workflow dispatched run passes — always re-run before diagnosing deeper.

## Program progress
- Phase 0/1 complete: docs/AUDIT.md (baseline), docs/PDF_REFERENCE_AUDIT.md (5 refs), FEATURE_MATRIX, ARCHITECTURE, UI_COMPONENTS, TESTING, PRODUCTION_CHECKLIST.
- CLOSED: #28 (JVM test seed; caught real rot-algebra bug), #14 (reading position persistence), #15 (text search UI + engine).
- CLOSED earlier (session 1): #1–#8, #9, #10, #11, #12.
- Next unblocked: #16 (reader thumbnails/outline panel) or #18 (annotation journal → unblocks #19 undo/redo).

## Blockers
- No local Android SDK: runtime verification = owner device only; label NOT RUN in issues until the owner tests a build (batch into next release, v0.2.0).
- material3 1.5.0 stable MotionScheme still blocked upstream (old issue #7).

## Blockers
- No device/emulator: runtime verification impossible here — CI build verification only; user does on-device testing (docs/TEST_PLAN.md).
- Manifest querying MediaStore.Files may return empty on some OEMs (runCatching + SAF picker is the safe fallback; accepted for v0.1).

