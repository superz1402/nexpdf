# UI_COMPONENTS — NexPDF

Companion to docs/SCREEN_INVENTORY.md (screens) and docs/UI_SPEC.md / DESIGN_TOKENS.md (spec). This file tracks the reusable component layer that the upgrade program builds on screen-on-screen.

## Shared components today
| Component | Location | Notes |
|---|---|---|
| Skeleton | ui/common/Skeleton.kt | pulse placeholder, loading-only (no fake data) |
| NexMotion tokens | ui/theme/Motion.kt | emphasized decel/accel + expressiveSpring — apply to new transitions |
| Output progress card | ui/tools/ToolsScreen.kt | label + fraction pattern; reuse for batch jobs |
| Tool selection circle | ui/annotate AnnotateScreen tool rail | secondaryContainer halo state |
| DocumentHolder | ui/viewer/ViewerScreen.kt | AutoCloseable renderer lifecycle; pattern for new panels |

## Program component additions (planned, built once, shared)
1. **PageThumbnail** — 480px renderer-backed thumbnail w/ skeleton; used by reader panel + organizer.
2. **ReaderPanelHost** — bottom-sheet host for bookmarks/search/thumbnails in reader (single shared bottom sheet, not per-option duplicates).
3. **Segmented tool drawer (annotate)** — current rail grows: group tools (Draw / Text / Objects / Page) so no duplicated option rows.
4. **UndoRedoController** — composable-friendly stack API used by annotate; later page ops.
5. **SaveBar** — dirty-state indicator + unsaved-changes dialog (Back/Cancel/Save/Discard cascade).

## Layout rules (from the program §8)
- One consistent toolbar geometry across screens: top app bar + bottom action bar; mode changes swap tool rows, never reposition the anchors.
- Panels open *under* the preview (annotate width-picker pattern) instead of over it wherever options need live page feedback.
- Bottom action bars must remain anchored when IME opens (`imePadding` on the bar when dialogs need it).
- Touch targets ≥ 48dp (compact 20–28dp affordances only *inside* larger hit boxes).
- Dialogs: Back/Cancel/outside-tap all dismiss consistently; destructive dialogs declare the destructive verb explicitly.
