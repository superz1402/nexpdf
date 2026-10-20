# worklog — NexPDF

## 2026-10-20 SESSION 1 — Task #9 (UX polish wave) CLOSED
Task: issue #9 · status CLOSED · branch main · head 9e43519

### Objective
Close the deferred nits from #3/#5/#8: page-slider sheet, skeleton placeholders, pen widths + eraser, signature reuse.

### Investigation
- Verified against material-icons-extended 1.7.8 classes.jar (AAR downloaded from maven.google.com) that no dedicated eraser glyph exists (only PhonelinkErase variants) → used CleaningServices for the eraser tool rather than guessing an icon into a red CI run.
- Signature persistence: chose org.json over adding kotlinx.serialization — zero new deps, app-private files dir.

### Implementation
- ui/common/Skeleton.kt: shared pulsing skeleton (alpha 0.5→1, 700ms reverse repeat), used by viewer pages (A4-fallback aspect) and annotate loading.
- ViewerScreen: clickable page indicator (enabled when pageCount > 1) opens ModalBottomSheet with Slider; onValueChangeFinished animates `listState.animateScrollToItem`.
- AnnotateScreen: PEN_WIDTHS (0.003/0.005/0.009 width-norm, dot previews) shown only on ink tool; InkStroke carries widthNorm; saveAll groups ops by (color,width); eraser drag removes strokes via hit-test (4% of shorter overlay dimension, min 16px); note dot and draft use per-width stroke rendering.
- data/SignatureStore.kt: list/save/delete signatures as JSON in filesDir/signatures.json (all IO-dispatched). Signature dialog: saved previews row (tap to reuse, X to delete, 110×56dp chips), draw-new pad unchanged, buttons Clear / Use signature / Save & use (last also persists).
- Compact 20dp delete affordance instead of IconButton inside chips (48dp min touch target would swallow preview taps).

### Verification
- CI run 39378096248 → success @ 9e43519; artifact `nexpdf-debug-apk` 27,361,923 bytes verified via API.
- Runtime verification (slider feel, eraser precision, signature reuse) requires on-device testing by owner.

### Remaining work
- text-selection highlight (needs PDFTextStripper coordinate mapping) — optional queue item.
- Pan clamping at zoom>1 still nit-open; signature dedupe on save possible later.

## 2026-10-19 SESSION 0 — Task #8 (Annotate & Fill & Sign) CLOSED
Task: issue #8 · status CLOSED · branch main · head 877deab

### Objective
Last major Adobe-parity block: annotate + fill & sign on a saved-copy workflow.

### Investigation
- Chose flatten-into-content over native annotation objects because Android PdfRenderer skips annotations without appearance streams; engine builds one AP modifying content stream per op (AppendMode.APPEND, keep-content) with rotation-aware coordinate mapping.
- Verified PDPageContentStream.AppendMode + PDColor support from classes.jar.

### Implementation
- data/PdfAnnotate.kt: Op sealed set (Ink/Note/Signature), FormValue; applyAll() chains ops + AcroForm set + optional flatten; listFields(); single stamp-named output via shared PdfTools.saveTo (made internal).
- ui/annotate/AnnotateScreen.kt: per-page render 720px, ink draft/commit into per-page stroke map, note dialog, signature dialog (own canvas strokes reused for placement+PDF), form dialog (LazyColumn fields, editable text), tool rail with colors + form + save, snackbar results, busy gating.
- Viewer gains pencil action → annotate route; AppRoot routing extended (annotate route returns to viewer).

### Verification
- 1 red run (FormValue reference slip + pfd stream load regression copy of PdfTools mistake) → fixed; success head 877deab.
- Content flattening visible-in-any-viewer claim verified by code reasoning; visual on-device verification pending user.

### Remaining work
Deferred listed in issue comment (highlight w/ text-selection, pen widths, eraser, signature reuse). Session backlog nits unchanged.

## 2026-10-19 SESSION 0 — Task #6 (M3 Expressive bump) CLOSED
Task: issue #6 · status CLOSED · branch main · head e440b46

### Objective
Move the stack to the Expressive-era Compose line without gambling the build.

### Investigation
- Pinned from maven.google.com evidence: BOM 2025.12.00 → compose ui 1.10.0 + material3 1.4.0 (verified via POM), AGP 8.10.1 (compileSdk 36 support; Gradle 8.11.1 compatible), kept Kotlin 2.1.20.
- classes.jar inspection: MotionScheme + FlexibleBottomSheet are @Internal in material3 1.4.0 (MaterialThemeKt has motionScheme param but interface internal). 1.5.0-beta01 exists but unchecked publicness → chose stable.

### Implementation
- build.gradle.kts: AGP 8.10.1; app: compileSdk 36, BOM 2025.12.00.
- Theme.kt reverted from MotionScheme (compile error proved internal-ness).
- ui/theme/Motion.kt: NexMotion tokens (EmphasizedDecelerate/Accelerate/StandardEase + QUICK/STANDARD/EMPHASIZED durations + expressiveSpring) wired into Home library AnimatedContent transitions.
- Follow-up issue created: adopt real MotionScheme on material3 1.5.0 stable.

### Verification
- CI success head e440b46. Runtime verification remains user-side (no device).

### Remaining work
All 6 original issues closed. Open: follow-up (1.5.0 stable adoption), annotate/fill&sign tracked under earlier scope note (new issue when started). Deferred nits in SESSION_STATE.

## 2026-10-19 SESSION 0 — Task #5 (tools engine) CLOSED
Task: issue #5 · status CLOSED · branch main · head 68a1816

### Objective
Adobe-level organize/merge/split/convert capabilities with production output plumbing (Downloads on API29+).

### Investigation
- Verified real library API by inspecting classes.jar from pdfbox-android 2.0.27.0 AAR locally (multipdf.Splitter with .split(pdf), pdfbox.android.PDFBoxResourceLoader, PDDocument.load(InputStream), getPage/getNumberOfPages) — avoided guess-fix loops.
- Output contract decision: write to MediaStore.Downloads (IS_PENDING 0/1 flow) on API29+; app-external Documents/NexPDF dir below API29. No storage permission needed (MediaStore contributes).

### Implementation
- data/PdfTools.kt (merge/extract/delete/reorder via Splitter page-wise assembly; rotate; split per-part; imagesToPdf JPEG92 LETTER fit) + Output class (pending→finished MediaStore lifecycle).
- NexPdfApp + manifest application name: PDFBoxResourceLoader.init.
- ui/tools/ToolsScreen.kt: 4 tool cards, IO-scope job with label()+fraction into animated progress card, OpenMultipleDocuments/OpenDocument pickers.
- ui/organize/OrganizerScreen.kt: thumbnail grid (480px PdfRenderer renders, cached map), selection semantics, rotate/delete/move-up, save-copy chaining delete→rotate→reorder with position mapping (original→working index).
- AppRoot: internal string routing home/viewer/tools/organizer; Home gains tune icon for tools.

### Verification
- 4 red runs fixed via root-cause from logs (each committed separately):
  1. 39239233559 — package paths + pfd stream + string template + List<Int?>
  2. 39239739472 — Splitter().split + organizer mapping nullability
  3. 39240014433 — update() arity + pages range check
  4. Final: run 39240720254 → SUCCESS (assembleDebug, head 68a1816).
- Runtime (on-device) verification of generated PDFs not possible here — user-side to verify from Downloads; structural correctness relies on pdfbox save success + CI compile.

### Remaining work
Deferred nits tracked in SESSION_STATE; annotate/sign (#6 line) next along with expressive bump #6.

## 2026-10-19 SESSION 0 — Tasks #3 (viewer polish) + #4 (UI research) CLOSED
Task: issues #3, #4 · status CLOSED · branch main

### Objective
#3: raise viewer to pro-grade (page tracking, zoom, progress). #4: extract reusable UI patterns from the owner's own Android repos into docs/UI_RESEARCH.md.

### Investigation
Subagent read actual Compose source at pinned SHAs: YFR@c54b2305 (BOM 2025.12.00; owns PdfViewerScreen: HorizontalPager+PdfRenderer+±1 page cache), AuraGallery@0722ea8 (IndexingProgressStrip), tabit@d84dafd (flat token objects), comet@26b4cc2 (one-moment rule), MomoCut@d055a6d4 (toolbarMode state machine, BackHandler cascade), Pdfeditor@7940a8d/Pdfcanvas@ee52d9a (tool taxonomy). Full facts/judgments in docs/UI_RESEARCH.md.

### Implementation
- app/src/main/java/com/nexpdf/app/ui/viewer/ViewerScreen.kt rewritten: derivedStateOf current-page tracking; animateFloatAsState progress (240ms) + a11y description; per-page pinch zoom 1x–4x + double-tap toggle; pager scroll locked while zoomed; DocumentHolder cleanup unchanged.
- docs/UI_RESEARCH.md Phase 2 rewrite: per-repo facts, SHAs, file-path citations, anti-patterns, anchor-terminal 404 recorded, 7 decisions for NexPDF.

### Verification
- CI run 39235990433 success (assembleDebug). Gesture/rotation runtime verification impossible here (no device) — user-side manual test required; noted as limitation, not silently claimed done.

### Git persistence
- Commits pushed to main; remote verified via API.

### Remaining work / next
Issues #5 (pdfbox tools engine), #6 (M3 expressive bump). Deferred nits: pan-offset clamping at zoom>1, page-slider sheet, skeleton placeholders.

## 2026-10-19 SESSION 0 — CI GREEN (Increment 0 verified)
Task: issues #1, #2 · status CLOSED · branch main · commit chain b290df9 → CI-fix commits

### Objective
Make the CI-only build pipeline green and deliver the first debug APK.

### Investigation / failures fixed (failure-driven issues)
1. Run 39232525582: android-actions/setup-android@v3 sdkmanager exit 1 (runner SDK conflict/licenses) → replaced with runner-preinstalled SDK + license acceptance step. Root cause, not symptom.
2. Run 39232750739: Kotlin compile errors — AppRoot Uri→String mismatch; Folder/PictureAsPdf icons unresolved (icons-core only) → AppRoot `.toString()`, added material-icons-extended dep.
3. Run 39233404604: BUILD **SUCCESSFUL** but "Artifact storage quota has been hit" on artifact upload (account-wide) → repo switched public (free artifact storage; user's accounts build in public by default) + 1-day retention; flagged to user.

### Verification
- Run 39233978878: conclusion **success**; artifacts `nexpdf-debug-apk` 16,400,651 bytes verified via API; APK listing on runner: 16,882,426 bytes app-debug.apk.
- Limitation: no device/emulator here — runtime verification (launch, MediaStore on-device, renderer) pending user side-install.

### Git persistence
- main pushed after each fix; remote verified via commits API.

### Remaining work
Issues #3 (viewer polish), #4 (UI research deep-dive), #5 (pdfbox tools), #6 (M3 expressive bump) — next: #3.

---

## 2026-10-19 SESSION 0 — Bootstrap
Task: Session 0 bootstrap · status DONE · branch main

### Objective
Establish GitHub-first persistence, CI-only build pipeline, and first working increment (Home + Viewer) so an ephemeral session can never lose project state.

### Investigation
- Environment: no Java/Android SDK, 24 GB free disk, Node 18, JDK 17 installed via apt (abandoned SDK path to honor user's disk constraint → CI-only builds).
- Toolchain research (2026-10): Compose BOM 2025.05.01 stable-safe combo chosen over bleeding-edge M3 1.4 Expressive to guarantee first-run CI green; Expressive bump tracked as follow-up issue.
- Library research summary (for v0.2+): rendering = androidx.pdf (beta, 12L+) or Infomaniak pdfview 3.2.18 fork; manipulation = tom-roush pdfbox-android 2.0.27.0 (Apache-2.0); MuPDF excluded (AGPL).
- User accounts surveyed for UI-research candidates (see docs/UI_RESEARCH.md); deep per-file inspection deferred (explicit gap).

### Implementation (files)
- Root: settings.gradle.kts, build.gradle.kts, gradle.properties, .gitignore, AGENTS.md, README.md, worklog.md, SESSION_STATE.md
- CI: .github/workflows/ci.yml (JDK17 + setup-android + gradle 8.11.1 + assembleDebug + artifact upload)
- App: AndroidManifest.xml (VIEW pdf intent filter), MainActivity (edge-to-edge + VIEW intent handling), ui/theme (Theme.kt dynamic-color + ember fallback, Type.kt), ui/AppRoot.kt (two-surface routing), ui/home/HomeScreen.kt (MediaStore query, search + AnimatedContent, SAF picker + persistable permission, empty state, tonal cards), data/PdfLibrary.kt, ui/viewer/ViewerScreen.kt (PdfRenderer + DocumentHolder AutoCloseable + lazy per-page bitmaps + BackHandler + error/loading states)
- Docs: docs/RECOVERY.md, docs/BUILD_ENVIRONMENT.md, docs/UI_RESEARCH.md, docs/DESIGN_TOKENS.md, docs/UI_SPEC.md, docs/SCREEN_INVENTORY.md, docs/UI_ACCEPTANCE_CHECKLIST.md, docs/TASK_REGISTER.md
- Deliberately ugly-first-throw viewer code was rewritten in-place before commit (no poor code shipped into main).

### Verification
- CI build: see SESSION_STATE.md `last_ci_run` — updated each push; first run pending at time of write.

### Git persistence
- Commit: "Increment 0: project scaffold, CI, home library, viewer" — push pending (recorded before verification; final status in SESSION_STATE.md).

### Remaining work
- Issues #1–#6 creation after push; CI green verification; then Task 1 (viewer page tracking + zoom), then tools engine.
