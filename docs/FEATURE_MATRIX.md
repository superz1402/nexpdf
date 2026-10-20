# FEATURE_MATRIX — NexPDF

Status: **Working** (runtime-verified by owner or code-verified w/o device) · **Partial** · **Absent** · **NOT RUN** where a claim lacks verification.
Owner-side runtime test list: docs/TEST_PLAN.md · Current-state rationale: docs/AUDIT.md.

## A. Reading
| Capability | Status | Note |
|---|---|---|
| Open via picker (SAF) / library / VIEW intent | Working | persistable grants |
| Continuous vertical scroll | Working | LazyColumn + PdfRenderer |
| Pinch zoom 1–4x, double-tap, clamped pan | Working | issue #11 clamp |
| Fit-width / fit-page / actual-size | Absent | planned UI issue |
| Page counter + progress | Working | a11y description present |
| Jump-to-page slider sheet | Working | issue #9 |
| Thumbnails panel (reader) | Absent | organizer thumbnails exist |
| TOC / outline panel | Absent | pdfbox outlines extractable |
| Bookmarks | Absent | |
| **Persistent reading position** | **Absent** | top gap |
| Restore state on resume | Partial | process death loses everything |
| Night mode | Absent | trivial on rendered bitmaps |
| Encrypted PDF open | Absent | renderer exception → error page |
| Malformed/unsupported handling | Working | runCatching + error page |
| Single-page / dual-page modes | Absent | |

## B. Search
| Capability | Status | Note |
|---|---|---|
| Text search over document | **Absent UI** | engine exists (PdfText per-page word rects, rot-aware) |
| Highlight matches + navigate | Absent | |
| No-results state | n/a | |
| OCR-derived search | Absent | engine decision pending |

## C. Annotate / markup
| Capability | Status |
|---|---|
| Freehand ink, 3 colors × 3 widths | Working |
| Highlighter (word-snapping) | Working (alpha 0.35 ExtGState) |
| Eraser | Working (ink + highlights only) |
| Sticky/text notes at anchor | Working (no font size) |
| Underline / strikeout | Absent |
| Shapes (arrow, rectangle) | Absent |
| Signature placement + reuse library | Working |
| Select / move / resize / delete objects | Absent |
| **Undo / redo** | Absent |
| Annotations survive exit (no save) | Absent — in-memory only |
| Saved output reopens with edits | Working (content-stream embedding; rotation-aware) |

## D. Existing-content editing
| Capability | Status |
|---|---|
| Add text over page | Working (notes) |
| White-out / cover-and-retype | Absent |
| Redact (content removed) | Absent — planned layered (visual+flatten) rollout |
| Edit existing text objects | Not supported by engine — documented; cover-and-retype is the honest path |
| Replace embedded images | Absent |

## E. Page management
| Capability | Status |
|---|---|
| Rotate / delete / reorder / duplicate-free organize | Working (rotate, delete, move-up) |
| Merge documents | Working |
| Split (per page) | Working; split-by-range Absent |
| Insert pages (blank/import position) | Absent |
| Duplicate / extract range / crop | Absent |
| Drag-drop reorder | Absent (move-up semantic) |
| Preview before applying | Absent |

## F. Creation & conversion
| Capability | Status |
|---|---|
| Images → PDF | Working (JPEG92, Letter fit) |
| Blank PDF, scan-to-PDF | Absent |
| PDF → images, text extraction UI | Absent ((PdfText) engine exists) |
| Compress | Absent |
| Metadata view/edit | Absent |
| OCR / searchable-PDF | Absent (engine decision pending) |

## G. Forms / signatures / security
| Capability | Status |
|---|---|
| AcroForm text fields fill + flatten | Working |
| Checkbox / radio / choice widgets | Absent |
| Password-protect export / remove | Absent |
| Permission handling | Absent |
| Digital (cryptographic) signature | Not planned short-term; visual signature clearly labeled ink-on-paper |
| Secure temp-file handling | n/a today; contract needed with decrypt-then-render |

## H. Save / export / recovery
| Capability | Status |
|---|---|
| Save-as-copy only (original untouched) | Working — deliberate |
| Save / Save As distinction | Partial (every save is Save As; surfaced honestly) |
| Dirty state + unsaved warning | Absent |
| Draft/journal recovery | Absent |
| Atomic replacement + validation | n/a (never replaces) |
| Output validation + MediaStore lifecycle | Working (IS_PENDING) |

## I. Cross-cutting
| Capability | Status |
|---|---|
| Automated tests | **Not present** — seed planned (engine ops, pdfPoint algebra) |
| Performance: bounded caches / zoom-tier render / cancellation | Absent (no LruCache; fixed 1080/720px renders) |
| Phone / tablet / foldable / two-pane | Partial (phone-first layouts) |
| Dark/light theme | Working (dynamic color + ember fallback) |
| a11y interactions | Partial (content descriptions on key controls) |
| Privacy posture | Working: no INTERNET, no telemetry, local-only, optional-output consent |
