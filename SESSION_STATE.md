# SESSION_STATE — NexPDF

Updated: 2026-10-19 (Session 0)

## Repository
- Repo: `superz1402/nexpdf` (private) · branch `main`
- App: NexPDF, package `com.nexpdf.app` (name/package change request pending with owner)
- Last commit: Increment 0 (scaffold, CI, home library, viewer) — SHA recorded in worklog after push

## Build pipeline
- CI-only. GitHub Actions `.github/workflows/ci.yml` on push.
- last_ci_run: PENDING (poll via docs/RECOVERY.md after each push; update this line with run id + conclusion)

## App state
- Working: Home library (MediaStore + SAF picker + search), Viewer (PdfRenderer pages)
- Not yet: tools engine, annotations, signatures, zoom, page tracking, M3 Expressive bump

- Last commit: CI: 1-day artifact retention — SHA b290df9..(latest, see git log) · remote verified
- CI: run 39233978878 → **success**; artifact `nexpdf-debug-apk` (16.4 MB, 1-day retention)

## App state
- Working: library, viewer (tracking/zoom/progress), tools hub (merge/images→PDF/split/organize), organizer, annotate mode (ink/notes/signature/form-flatten). Stack: BOM 2025.12.00 (m3 1.4.0), compileSdk 36, AGP 8.10.1, Kotlin 2.1.20, pdfbox-android 2.0.27.0.
- Not yet: text-selection highlight, eraser/pen widths, page-slider sheet, skeleton placeholders, signature reuse, pan clamping

## Task queue (authoritative: docs/TASK_REGISTER.md + GitHub Issues)
1. (next) UX polish wave: page-slider sheet, skeleton placeholders, pen width/eraser, signature library
2. On material3 1.5.0 stable: MotionScheme.expressive + FlexibleBottomSheet (issue #7, blocked on stable)
3. Optional: text-selection highlight (needs PDFTextStripper word-coordinate mapping), OCR/convert (later)

Closed this session (evidence in issues): #1 bootstrap, #2 CI+APK, #3 viewer polish, #4 UI research, #5 tools engine, #6 Expressive bump, #8 annotate+fill&sign. Stack now: BOM 2025.12.00 (ui 1.10.0, m3 1.4.0), compileSdk 36, AGP 8.10.1, Kotlin 2.1.20.

## Blockers
- No device/emulator: runtime verification impossible here — CI build verification only; user does on-device testing.
- Manifest querying MediaStore.Files may return empty on some OEMs (runCatching + SAF picker is the safe fallback; accepted for v0.1).
