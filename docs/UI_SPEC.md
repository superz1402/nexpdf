# UI SPEC — NexPDF (v0.1)

## Screens & flows

### 1. Home / Library
Purpose: surface every PDF on the device; one tap to read; obvious entry to tools.
- Top bar: "NexPDF" large title (M3 topapp bar, monochrome on surface).
- Search field: rounded, icon-prefixed, live filtering with AnimatedContent fade.
- Document rows: tonal cards (surfaceContainer, 16dp radius), PDF glyph in primary, title (1 line) + metadata line (KB · date). Clickable whole row.
- FAB: folder icon, opens SAF picker (OpenDocument, application/pdf).
- Empty state: centered glyph + "No documents yet" + explainer + primary button.

### 2. Viewer
Purpose: distraction-free reading.
- Top bar on surfaceContainerHigh: filename (1 line) + page-count caption.
- Vertical LazyColumn pages, 16dp insets/12dp gutters; pages are 1080px-wide bitmaps (PdfRenderer), white canvas, crossfade on ready.
- Loading placeholder per page; global failure page with message.
- Back gesture handled by BackHandler → closes viewer (SAF URIs persistable).

### Tool surfaces (planned, v0.2+)
Bottom-sheet tool rail (FlexibleBottomSheet when M3 1.4 available), organize grid with drag handles, annotation canvas overlay + toolbar.

## Interaction rules
- Source lists refresh on each return to home (LaunchedEffect key).
- Viewer opens from home tap and SAF picks and external VIEW intents; closing always returns to previous surface.
- Long-press actions (rename/delete/share) deferred to v0.2 — entry point must exist visually before wiring.
