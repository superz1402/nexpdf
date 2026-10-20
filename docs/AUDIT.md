# AUDIT — NexPDF baseline (Phase 0 of the PDF upgrade program)

Recorded 2026-10-20 @ 9467837 (v0.1.1). Source of truth: `main`; upgrade work happens on `feat/pdf-upgrade`.

## Actual stack (discovered, not assumed)
- Kotlin 2.1.20, Jetpack Compose (BOM 2025.12.00: ui 1.10.0, material3 1.4.0), minSdk 26 / target 35 / compileSdk 36, AGP 8.10.1, Gradle 8.11.1, JDK 17.
- Application ID `com.nexpdf.app`, name NexPDF, versionCode 2 / versionName 0.1.1.
- PDF engines (single pipeline, both already present): `android.graphics.pdf.PdfRenderer` (raster/render — cannot open encrypted PDFs) + pdfbox-android 2.0.27.0 `com.tom_roush.pdfbox` (structure/ops; text via PDFTextStripper).
- Architecture: no DI, no ViewModels; screens in `ui/**` hold state with Compose `remember`; engine/repo layer in `data/**` (PdfLibrary, PdfTools, PdfAnnotate, PdfText, SignatureStore, Output/MediaStore lifecycle). Shape is adequate; intro of ViewModels/blob state would be churn without demonstrated need — kept.
- CI: GitHub Actions `assembleDebug` on push to `main` (+ PRs). No room for local Android SDK (server disk constraint).

## Working today (preserve)
1. Home library: MediaStore query + SAF picker (persistable grant) + search + AnimatedContent + thumbnails.
2. Viewer: continuous vertical pager, page tracking, LinearProgress, pinch 1–4x + double-tap, pan clamp, jump-sheet slider, skeleton placeholders, VIEW-intent entry, error/loading states, BackHandler.
3. Tools hub: merge, images→PDF, split-per-page, progress + MediaStore output lifecycle (`Output`, IS_PENDING flow, Downloads on API 29+).
4. Organizer: thumbnails (480px, cached per session), selection, rotate/delete/move-up, save-copy chain with index remap.
5. Annotate: per-page rendering, ink (3 colors × S/M/L widths), eraser (ink + highlights), notes (typed text at anchor), highlight (drag-select snaps to real words via PDFTextStripper), signatures (draw, persist to `files/signatures.json`, reuse/delete), AcroForm text fields fill + optional flatten; saves always produce a NEW copy (originals never modified).
6. Viewer/annotate display→user-space mapping shared with pdfbox ops (`pdfPoint`), rotation-aware.

## Missing / incomplete vs the reference set (priority order)
1. **No reading-position persistence** — reopening any document starts at page 1 (every reader-grade reference app restores position). Also no bookmarks.
2. **No in-document text search UI** — `data/PdfText.kt` (per-page word rects) exists from the highlight work; a search pass + match navigation is a small delta on a verified foundation.
3. **No thumbnails/TOC panel in the reader** — organizer already renders page thumbnails; not surfaced for navigation.
4. **Encrypted PDFs fail** — PdfRenderer throws on password-protected files; reader shows a static error page only. PDFBox can decrypt to a private temp copy for rendering (pattern verified in docreader).
5. **No undo/redo** anywhere; annotation journal is in-memory only — process death loses unsaved edits entirely.
6. **No annotation object management** — cannot move/delete/resize individual annotations (eraser is the only removal); no selection.
7. **No underline/strikethrough/shape annotations** (arrow/rect); notes have no font sizing.
8. **No fit-width/fit-page/actual-size modes, no reader chrome auto-hide, no single/dual-page modes, no tablet/two-pane layout.**
9. **No night mode** (hue-preserving invert is trivially achievable on the rendered bitmap).
10. **Page ops gaps**: no insert/duplicate/extract-range/split-by-range/crop; organizer covers rotate/delete/reorder/whole-file split only.
11. **No export paths**: no PDF→image, no text extraction UI, no compress, no metadata view/edit, no redaction, no password-protect on export.
12. **Zero automated tests** — no unit/source-test source sets at all; CI compiles only.
13. **Perf**: no LruCache bound, no zoom-tier re-render, no job cancellation (obsolete renders can complete late — images can fight each other), bitmap width fixed (1080/720) regardless of zoom.

## Known defects / risks
- Splitter-based page assembly (Splitter page-wise rebuild) drops page-tree-inherited attributes (docreader note) — acceptable for rotate/delete/reorder chained ops today, revisit when organizer grows.
- Annotate render loop re-opens the whole PdfRenderer per page switch (expensive on huge docs; acceptable at 720px but wasteful).
- Ink commit groups strokes by (color,width) into ops — eraser handles ink + highlights only; notes/signatures are NOT erasable (must be object-manageable later).
- `pageCount` inside `renderPage` hard-assigns from renderer each time — benign today.
- MediaStore.Files query can return empty on some OEMs (SAF fallback accepted, documented).
- No tests of `pdfPoint` rotation algebra — it is the linchpin of user-space correctness; first tests should pin it.

## Build/test status
- CI green throughout main history (latest run 39392165914 success @ 9467837). No local SDK → runtime behavior validated only on user device; everything else CI-verified. Runtime claims in the app are limited to code-verified reasoning where noted.

## Security posture
- Permission: none beyond defaults (SAF + MediaStore). No INTERNET. Documents never leave device. Signature library + prefs in app-private storage. Temp files: none produced today (decrypt-then-render will introduce private-file temps — must add cleanup contract per prompt §7.H). No telemetry. `PDFBoxResourceLoader.init` on app start. Untrusted-input handling: every open path is `runCatching`-wrapped; error pages shown.

## Recommended sequence (children of the parent issue)
1. P1: persist+restore reading position; text search UI; reader thumbnails panel; night mode (small, user-visible, low-risk, all on verified foundations).
2. P1: annotation journal persistence (draft recovery) + undo/redo stack (engine change, testable in JVM).
3. P1: encrypted-PDF open flow.
4. P2: annotation objects (move/delete/select), shapes (underline/strike/arrow/rect), text-box sizing.
5. P2: page ops expansion (insert/duplicate/extract/split-range/crop) + preview-before-save pattern.
6. P2: export paths (PDF→image, text extraction, compress, metadata editor) with honest trade-off copy.
7. P2: redaction (visual + flatten), password-protect export; temp-file hygiene.
8. P3: performance (LruCache bounds, zoom-tier renders, cancellation, memory-pressure).
9. P3: reader UX (chrome auto-hide, fit modes, two-pane tablet, dual-page).
10. Ongoing: JVM test seed (engine ops, pdfPoint algebra) + growth.

Stack constraint: CI-only builds; runtime verification is the owner's device — every issue must state which acceptance items are RUN (CI/JVM) vs NOT RUN (device).
