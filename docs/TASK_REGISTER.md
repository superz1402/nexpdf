# TASK REGISTER — NexPDF

Authoritative status live in GitHub Issues; this is the index map.

| # | Issue | Priority | Status | Deps |
|---|---|---|---|---|
| 1 | Bootstrap: scaffold + CI + home library + viewer (increment 0) | P0 | CLOSED (CI 39233978878 success) | — |
| 2 | CI green verification + APK artifact | P0 | CLOSED (artifact 16.4 MB verified) | 1 |
| 3 | Viewer polish: page tracking, zoom-pan, progress | P1 | CLOSED (run 39235990433) | 2 |
| 4 | UI research deep-dive over user's Android repos | P1 | CLOSED | 2 |
| 5 | Tools engine (pdfbox): merge/split/rotate/delete | P1 | CLOSED (run 39240720254) | 2 |
| 6 | M3 Expressive bump (BOM 2025.12.00, compileSdk 36, AGP 8.10.1) | P2 | CLOSED (run e440b46) | 2 |
| 7 | Adopt material3 1.5.0 stable MotionScheme + FlexibleBottomSheet when stable | P2 | open (blocked on 1.5.0 stable) | 6 |
| 8 | Annotate + Fill & Sign | P2 | CLOSED (run 39248398340) | 5 |
| 9 | UX polish wave: page-slider sheet, skeleton placeholders, pen width/eraser, signature library | P1 | CLOSED (run 39378096248) | 5, 8 |

Issues #1–#9 created 2026-10-19/20; #8 and #9 closed with CI evidence. #7 stays open while material3 1.5.0 remains beta.
