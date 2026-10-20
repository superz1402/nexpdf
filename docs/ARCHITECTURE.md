# ARCHITECTURE — NexPDF

Single PDF pipeline; no duplicate engines, no DI ceremony. UI holds Compose state; `data/` holds engines and stores.

```
com.nexpdf.app
├── NexPdfApp.kt                 Application; initializes PDFBoxResourceLoader
├── MainActivity.kt              edge-to-edge + VIEW-intent handoff (uri pass-through)
├── data/
│   ├── PdfLibrary.kt            MediaStore discovery (runCatching + SAF fallback documented)
│   ├── PdfTools.kt              merge / imagesToPdf / split / rotate / extract / delete / reorder
│   │                            (Splitter page-wise rebuild) + Output (MediaStore IS_PENDING→finished)
│   ├── PdfAnnotate.kt           sealed Op set (Ink/Note/Signature/Highlight) + FormValue;
│   │                            applyAll() chains ops into one APPEND content stream per op,
│   │                            rotation-aware pdfPoint display→user mapping; AcroForm set+flatten
│   ├── PdfText.kt               PDFTextStripper word quads → display-normalized WordRect (search/highlight base)
│   └── SignatureStore.kt        drawn signatures → files/signatures.json (JSON, IO-dispatched)
├── ui/
│   ├── AppRoot.kt               string routing home/viewer/tools/organizer/annotate
│   ├── common/Skeleton.kt       pulsing skeleton (loading state only)
│   ├── theme/                   M3 1.4.0; dynamic color + ember fallback; NexMotion tokens
│   ├── home/HomeScreen.kt       library
│   ├── viewer/ViewerScreen.kt   pager + zoom + jump sheet + skeleton pages
│   ├── tools/ToolsScreen.kt     4 tool cards + progress
│   ├── organize/OrganizerScreen.kt
│   └── annotate/AnnotateScreen.kt  tool rail + dialogs + overlay canvas
```

## Core contracts
- **Display space** = normalized 0..1 from top-left of the *rendered* (rotation-applied) page. Every annotation op is stored in display space; `PdfAnnotate.pdfPoint` is the sole display→user mapper. pdfPoint algebra has no tests yet — first test seed targets it.
- **Saves are always new copies** (MediaStore Downloads via `Output`), original URIs never rewritten.
- **Rendering**: PdfRenderer per open document (`DocumentHolder : AutoCloseable`), one renderer per screen, synchronized page open; annotate re-opens per page switch (known inefficiency, audit §Known-defects).
- **No bitmaps cross threads unsafely**: renders on Dispatchers.IO, assignment to Compose state on main via state updates from the IO coroutine.

## Upgrade-program additions on `feat/pdf-upgrade`
- `ui/viewer` gains reader panels (thumbnails, search, night mode) as sibling composables — no new pipeline.
- Persistence additions: per-document reading position + annotation journal (op list) + bookmarks → app-private JSON keyed by document URI + name hash (stable across grant-refresh). Journal replay = applyAll input; recovery on open.
- Encrypt/decrypt path: pdfbox detect → password prompt → decrypt to `cacheDir` (excluded from backup, deleted after save/close) → PdfRenderer on the private file. Temp-file cleanup contract lives in the decrypt helper only.

## Dependency policy
New dependency = documented purpose + license + size/perf review (AGENTS.md). Currently: pdfbox-android (Apache-2.0), CoMpose stack — nothing else.
