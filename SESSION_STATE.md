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

## Task queue (authoritative: docs/TASK_REGISTER.md + GitHub Issues)
1. (next) Viewer polish: current-page tracking w/ scroll, zoom-pan, wavy progress — after CI green
2. Tools engine via pdfbox-android (merge, split, rotate, delete pages)
3. M3 1.4 Expressive bump (compileSdk 36+, AGP bump) — riskiest, do as dedicated issue
4. Annotate & Fill&Sign
5. UI research deep-dive (per-file Compose inspection of superz1402/bilal140202/ansaribilal14 Android repos)

## Blockers
- No device/emulator: runtime verification impossible here — CI build verification only; user does on-device testing.
- Manifest querying MediaStore.Files may return empty on some OEMs (runCatching + SAF picker is the safe fallback; accepted for v0.1).
