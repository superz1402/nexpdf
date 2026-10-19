# worklog — NexPDF

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
