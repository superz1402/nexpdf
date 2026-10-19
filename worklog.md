# worklog — NexPDF

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
