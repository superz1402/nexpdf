# SESSION_STATE — NexPDF

Updated: 2026-10-20 (Session 1)

## Repository
- Repo: `superz1402/nexpdf` · branch `main`
- Release v0.1.0: `NexPDF-v0.1.0-debug.apk` (tag at 9e43519) — https://github.com/superz1402/nexpdf/releases/tag/v0.1.0
- On-device test checklist: docs/TEST_PLAN.md

## Build pipeline
- CI-only. GitHub Actions `.github/workflows/ci.yml` on push. 1-day artifact retention.
- last_ci_run: run 39386034930 → success @ 368e3ed (highlight + pan clamp); release build verified at 39378096248 @ 9e43519.

## App state
- Working: library, viewer (tracking/zoom/pan-clamped progress, jump sheet, skeletons), tools hub (merge/images→PDF/split/organize), organizer, annotate mode (ink w/ widths + eraser, notes, highlight via text selection, signature with persistent library, form-fill+flatten). Stack: BOM 2025.12.00 (m3 1.4.0), compileSdk 36, AGP 8.10.1, Kotlin 2.1.20, pdfbox-android 2.0.27.0.
- Not yet: signature dedupe on save; OCR/convert.

## Task queue (authoritative: docs/TASK_REGISTER.md + GitHub Issues)
1. (next) Optional: OCR/convert (later, needs runtime engine decision)
2. Blocked: material3 1.5.0 stable → MotionScheme.expressive + FlexibleBottomSheet (issue #7)

Closed this session: #9 UX polish wave (CI 39378096248), #10 text-selection highlight + #11 pan clamp (CI 39386034930). Previously closed: #1–#8.

## Blockers
- No device/emulator: runtime verification impossible here — CI build verification only; user does on-device testing (docs/TEST_PLAN.md).
- Manifest querying MediaStore.Files may return empty on some OEMs (runCatching + SAF picker is the safe fallback; accepted for v0.1).

