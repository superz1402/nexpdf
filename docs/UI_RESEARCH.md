# UI RESEARCH — NexPDF (source: superz1402 / bilal140202 / ansaribilal14)

Method: discovery via GitHub API 2026-10-19; deep-dive (Phase 2, issue #4 same day) read actual source via raw.githubusercontent.com at pinned SHAs. Facts are labeled; judgments are in the final section. No repos were locally cloned; key findings below cite file paths.

## Phase 2 deep-dive results (verified facts, per repo)

### Your-Files-Redesign — Bilal140202 (main @ c54b2305, 2026-09) ★ closest analog
- Compose BOM **2025.12.00**, compileSdk 36, Nav 2.9.6, Paging, Coil3. Full M3.
- Has formal design audit artifacts: `.audit/DESIGN_SYSTEM.md` (theme "Calm Files", warm-paper #FAF9F7/#141317, category hues at 40% chroma, Plus Jakarta Sans, motion 150/220/320ms EmphasizedCubic) and `.audit/FINDINGS.md`.
- Reusable: `MotionTokens` object + NavHost fade+⅓-slide (`app/App.kt`); `LocalNavController` CompositionLocal; shared `ScreenScaffold(title, subtitle, …, FAB)` (`presentation/ui/components/common/ScreenScaffold.kt`); selection-session model with `semantics{stateDescription}` + 48dp checkbox overlay (`SelectableFileItem.kt`); **existing `PdfViewerScreen.kt`: HorizontalPager + PdfRenderer + ±1 page cache + bitmap eviction**.
- Anti-patterns: 63KB monolith `FileBrowserScreen.kt`; `fallbackToDestructiveMigration`; mixed LiveData/Compose.

### AuraGallery — Bilal140202 (main @ 0722ea8)
- BOM 2024.09.03, Kotlin 2.0.20, Hilt, telephoto zoomable images.
- Reusable: `LargeTopAppBar` + `exitUntilCollapsedScrollBehavior` (`GalleryScreen.kt`); bottom NavigationBar filtered by top-level routes (tonalElevation 0); **`IndexingProgressStrip` — inline secondaryContainer progress strip instead of dialogs** (good for PDF job progress); date-grouped grid with keyed items; 180ms debounced search.
- Anti-patterns: manual `chunked(3)` grid rows instead of `LazyVerticalGrid`; hardcoded 4dp radii outside theme.

### tabit-android — Bilal140202 (main @ d84dafd; ansaribilal14 fork older)
- BOM 2024.09.00. Flat token objects `Spacing/Sizing/Motion/Alpha` (motion tiers 120/250/350/500ms; MUTED_TEXT=0.60, DISABLED=0.38 alphas) in `core/theme/Dimens.kt`; hairline components (`EmptyState`, `SectionHeader`, tabular numerics); `animateColorAsState`+`animateContentSize` state cards.
- Anti-pattern: hardcoded `TabitStatusColors` hexes contradicting "computed from theme".

### comet-x-browser — Bilal140202 (HEAD 26b4cc2)
- Main browser is **View-system**; only Cloud AI center is Compose M3 Expressive (`ui/cloud/CloudTheme.kt`).
- Key UX facts: fake bottom sheet (28dp rounded Row + drag handle) chosen deliberately because real sheets fight ListView+IME insets (documented in `docs/ui/COMET_UI_CURRENT_STATE.md` §5–6); "one-moment rule": a single signature animated effect app-wide.
- Anti-patterns: Unicode-glyph buttons w/o contentDescription; panel toggles via visibility.

### MomoCut — superz1402 (main @ d055a6d4)
- BOM 2024.10.01, **dark-only** Ink0-3 + Coral/Teal/Amber tokens, edge-to-edge, Lucide icons.
- Reusable: BackHandler cascade (selection→menu→screen) `EditorScreen.kt:204-214`; derived `toolbarMode` state machine = contextual toolbar without navigation; `ModalBottomSheet(skipPartiallyExpanded)` + `rememberSaveable` + `nameTouched` validation gating; commit-on-`onValueChangeFinished` slider + nudge buttons (one undo per gesture); manual `WindowInsets.statusBars` padding on custom top bar; media resources released on dispose/ON_PAUSE.
- Anti-patterns: 86KB `EditorPanels.kt`; token objects imported straight into screens bypassing theme.

### Pdfeditor / Pdfcanvas — Bilal140202 (web, feature-split skim only)
- Pdfeditor @7940a8d: tool-module sidebar taxonomy (viewer/edit/text/shape/draw/signature/forms/scanner/security/converter). Pdfcanvas @ee52d9a: DrawingEngine/canvasStore split; marketing claims unverified.
- Judgment only: their tool-hub taxonomy (viewer / edit / sign / secure / convert / organize) is a clean nav map for a PDF app.

### Not accessible / irrelevant
- bilal140202/anchor-terminal → 404 (requested in plan, absent).
- All other repos in the three accounts are non-Android (Python/TS/HTML pipelines); excluded by relevance filter.

## Original discovery (Phase 1, methodology note)

| Repo | Owner | Relevance | Verified via |
|---|---|---|---|
| Your-Files-Redesign | bilal140202 | document library + built-in PDF viewer | source-read |
| AuraGallery | bilal140202 | media gallery UI | source-read |
| tabit-android | bilal140202 / ansaribilal14 | Compose tokens/components | source-read |
| comet-x-browser | bilal140202 | browser chrome patterns | source-read |
| MomoCut | superz1402 | editor architecture | source-read |
| VoxFlow, jarvis-agent, MobileAgent, EchoID-Android, babymomo, zerium-*, open-video-engine-android, linuxify, HeyYou, Your-Files-Android, YourFiles, Pdfeditor, Pdfcanvas, tabit fork | mixed | discovered; partially sidelined by relevance (Pdfeditor/Pdfcanvas skimmed for feature taxonomy) | metadata + partial reads |

## Decisions for NexPDF (judgments — applying the above)

1. Adopt YFR's scaffold architecture: shared `PdfScaffold(title, subtitle, actions)` + `LocalNavController` later; MotionTokens with EmphasizedCubic 320ms screen transitions. Target BOM ≥2025.12.00 line in the Expressive bump (#6).
2. Viewer: keep PdfRenderer-but-upgrade to HorizontalPager with ±1 page cache+eviction (YFR pattern) replacing current per-item render; add dismissible page-slider sheet and skeleton (not spinner) placeholders; chrome auto-hide only if it doesn't fight zoom gestures.
3. Multi-select sessions (merge/delete/share) use YFR selection model + a11y semantics.
4. Token discipline: flat `Spacing/Sizing/Motion/Alpha` token objects; PDF-category hues (Docs/Scans/Forms/Signed) at fixed 40% chroma; 48dp targets via `Sizing` floor.
5. Tools = contextual `toolbarMode` swap (MomoCut pattern) + Expressive FlexibleBottomSheets (skipPartiallyExpanded); job progress via IndexingProgressStrip (AuraGallery) with "N of M" snackbars.
6. One signature moment policy (Comet/YFR): choose one hero motion (document shelf reveal on library); everything else standard speeds; respect reduced-motion.
7. Web Pdfeditor/Pdfcanvas are taxonomy references only — no code reuse (different platform).

