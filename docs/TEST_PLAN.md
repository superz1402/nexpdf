# TEST_PLAN — v0.1.0 (APK `NexPDF-v0.1.0-debug.apk`, built from 9e43519)

Owner-side on-device pass/fail list. CI verifies compile only; runtime is data-driven, so every item below needs real files.

## 0. Install
1. Download the APK from the release page (`v0.1.0`).
2. Open it; if blocked, allow "Install unknown apps" for the source app, then install.
3. Launch NexPDF. Expect: dark/light follows system, dynamic color on Pixel-style launchers, edge-to-edge status bar.

## 1. Home library
- [ ] Empty state shows an intentional empty message + picker entry (no fake cards, no dummy metrics).
- [ ] With PDFs in Downloads/Recents, the grid/card list populates with `PdfRenderer`-derived thumbnails (real data).
- [ ] Search filters by filename.
- [ ] SAF picker: "Add" flow → pick a PDF from a folder → appears; unlinked/missing files don't crash the library scan.
- [ ] Card tap opens the viewer; annotate/open actions reach the right document (title header = filename).

## 2. Viewer
- [ ] Header "Page X of N" updates while scrolling (no jump-derived pause at >0.6 fraction only).
- [ ] Progress bar animates smoothly (~240ms) per page change.
- [ ] Pinch zoom 1x–4x on a page; pager scrolling is locked while zoomed; double-tap toggles zoom.
- [ ] Tap the page indicator (top) → ModalBottomSheet with slider → release jumps to the page (animate scroll).
- [ ] Skeleton (pulsing) shows while page bitmaps render — especially on a 20+ page doc; no flat dead grey box.
- [ ] System back closes the viewer to Home.

## 3. Tools
- [ ] Merge: pick 2+ PDFs → progress → success snackbar; `NexPDF_merged_*.pdf` in Downloads; open it and confirm page order/rotation kept.
- [ ] Images→PDF: pick 1+ images (JPEG) → `NexPDF_images_*.pdf` letter-paginated in Downloads.
- [ ] Split: pick a multi-page PDF → part files `NexPDF_split_*_pageN.pdf`; each opens with its own page count.
- [ ] Organizer: thumbnails render (480px, cached); select → rotate/delete/move-up; Save copy applies delete→rotate→reorder correctly against ORIGINAL page numbers.

## 4. Annotate
- [ ] Ink: 3 colors × 3 widths — draw and confirm thickness changes on screen; strokes render after page navigation back and forth.
- [ ] Eraser: drag over an ink stroke → removes it (ink only; notes/signatures stay).
- [ ] Note: tap with note tool → dialog → save; yellow dot visible; note text written near the point in the PDF.
- [ ] Signature (first use): draw → "Save & use" → placed by tap on page; dialog reopens and shows the saved preview.
- [ ] Signature (reuse): open SIGN tool → saved library row → tap preview → place on another page/document.
- [ ] Signature library delete: X on a preview chip removes it; list updates.
- [ ] Form fields (needs a fillable PDF, e.g. IRS W-9): dialog lists text fields; "Apply & flatten" saves a flattened copy — fields no longer editable in any viewer.
- [ ] Save: "Saved copy in Downloads" snackbar; overlays reset; re-open the saved copy to verify ink width/color, highlight, notes, signatures visible.

## 5. Intents
- [ ] From Files app "Open with" → NexPDF on a PDF → opens directly in viewer.

## 6. A11y
- [ ] TalkBack (optional): progress bar announces "Reading progress: page X of N"; icon buttons have descriptions.

## 7. Regression watch-list
- Rotation change retains viewer/annotate page state.
- 2 PDFs opened back-to-back — no leaked PdfRenderer crashes (Android "cannot re-use stream").
- Empty/OEM-restricted MediaStore returns: library stays usable through SAF picker.
