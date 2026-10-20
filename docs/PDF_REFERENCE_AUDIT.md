# PDF_REFERENCE_AUDIT

Inspected 2026-10-20 (metadata + README + source-tree verification via GitHub API; raw source reads where noted).
Classification: **verified** = source inspected and implementation seen · **claimed** = documented by project, not verified here · **not supported** = explicitly absent.

## 1. FanFeast/anonpdf — Android, Kotlin, MIT, Android12+
Offline reader/editor; **no INTERNET permission** enforced by tests (PrivacyContractTest asserts sockets/DNS fail); optional MANAGE_EXTERNAL_STORAGE.
- **Verified (source tree + README claims consistent):** single-engine = `PdfRenderer` for raster + pdfbox-android for structure; op-list editor → normalized `EditPlan` consumed by BOTH preview and export (pixel-equality test); page-edits-in-place via page-tree re-attachment (keeps annotations/resources); merge-insert-at-position; white-out = page flattened under box so hidden text is truly gone (text selection lost on that page, documented); AES-256 protect/remove (JCE, BouncyCastle excluded, −4 MB); compress two modes (image re-encode keeps text / flatten raster); page numbers, watermark, crop-with-handles, refit to paper sizes; recents (20) + preferences in DataStore; preview = extract one page → apply plan → render (400-page scales like 4-page).
- **claimed, not verified here:** "opens password-protected PDFs" (decryption path), 55 JVM + device test suite pass.
- **Explicitly not supported:** OCR, office conversion, crypto signatures, cloud sync.
- **Reusable for NexPDF:** EditPlan op-list pattern (we already keep an ops list in the annotate engine — extend to page ops); preview-one-page trick; white-out via flatten; page-tree in-place edits (vs. our rebuild-by-Splitter which drops page-tree-inherited resources — known fidelity gap to fix); BouncyCastle exclusion; permission-contract tests.

## 2. Karna14314/Pdf_Tools — Android, Kotlin (Compose, MVVM+Clean), Apache-2.0, minSdk 26
Privacy-first utility, F-Droid + Play flavors (Tesseract vs ML Kit OCR).
- **Verified:** `DocumentSearchEngine.kt`, `PdfCompressor.kt`, `TextExtractor.kt`, `PdfRedactor.kt` exist with unit tests (`PdfCompressorTest`). Read `PdfRedactor.kt` directly: **visual redaction only** — overlays opaque boxes; true removal only via optional post-flatten-to-image; the file's own comment says so. Good honest taxonomy.
- **claimed:** drag-drop reorder, html→pdf, scan-to-pdf with edge detect, form fill incl. checkbox/radio, lock/unlock passwords, watermarks, EXIF-strip.
- **Reusable:** search-engine pattern (pdfbox text → rects → UI highlights; matches our `PdfText.wordRects` foundation); flavor-split OCR approach (proprietary ML Kit only in store flavor); redactor's flatten-after-redact option.

## 3. Stirling-Tools/Stirling-PDF — server/desktop platform (Java core), open-core license (NOASSERTION SPDX)
50+ tools, REST API, pipelines, SSO/enterprise. **Not an Android implementation** — nothing copyable wholesale; license is not cleanly FOSS (do not vendor code). Use purely as a **feature-gap checklist**: redact (true content-region removal using pdfbox text/image coords), OCR pipelines, form-field editing, stamping, metadata scrub, compare/split-by-size, PDF-to-office. License review result: reference only, no code borrowing.

## 4. mokshablr/gander — Android (Kotlin), MIT, minSdk 26, ≈5 MB APK
Zero-permission multi-format **viewer** (no editing). PDF via pdf.js in sandboxed WebView — different engine choice from ours (raster vs vector); benefits: text select/copy, screen-reader support, night mode (hue-preserving invert), match search. Costs: WebView dependency, no annotations.
- **verified:** architecture paragraph matches tree (ViewerActivity/FileKind/ArchiveProvider/Thumbs); "picks up where you left off" = persisted reading position per document (DataStore) — reusable behavior spec for us.
- **claimed:** night-mode hue preservation, range-serving for >16 MB docs.
- **Reusable:** per-document reading-position persistence contract; hue-preserving night mode on rendered bitmaps (ColorMatrix invert preserving hues — doable on PdfRenderer output); recents-with-thumbnail home pattern; no-INTERNET self-audit idea ("About screen asks Android what permissions the app requests").

## 5. codegeasse1/docreader — Android, Kotlin (Compose), MIT, minSdk 34-ish
WPS-style reader; PdfRenderer + pdfbox-android split like ours; addresses CI-only build (same constraint as ours).
- **Verified:** file list matches README (PdfDocumentHandle.kt has password decryption through PDFBox feeding PdfRenderer; AnnotationStore persisted per document in display space; PdfOps merge/split/sign/compress; PdfPrint print adapter).
- **claimed-by-changelog, partially verified by file presence:** zoom-bucket re-rendering (re-render only when zoom crosses a bucket threshold — keep old bitmap until sharp one lands, pan with raw deltas); auto-hiding reader bars (tap → show, fade after 3 s, scrolling doesn't count as tap); move-tool hit-testing; boxless text notes; CMS/PKCS#7 digital signature w/ self-signed cert (BouncyCastle); compress = rasterize-JPEG (text not selectable — documented trade-off); preview-before-save for all ops.
- **Reusable for NexPDF:** password-decrypt-then-render flow (encryption detected via pdfbox → decrypt to private temp → render, matching our no-network rules); persisted annotation store (display-space journal = our op list + per-doc JSON on disk); zoom-bucket rendering; auto-hiding reader chrome spec; recents+starred store.

## Cross-cutting decisions for NexPDF
| Topic | Decision | Basis |
|---|---|---|
| Engines | Keep PdfRenderer (render) + pdfbox-android (structure) — single pipeline, no duplicates | all 3 top Android refs use same split |
| Annotation model | Keep op-list/dirty-journal (documented embedding = content-stream ops + AcroForm), add persisted journal for recovery | anonpdf EditPlan, docreader AnnotationStore |
| Search | Build on our existing PDFTextStripper word-rect foundation (same as Pdf_Tools DocumentSearchEngine) | verified in our app in #10 |
| Redaction | Visual + flatten fallback; never claim security beyond documented. True content-stream removal = later, scoped | Pdf_Tools verified impl, prompt rule G |
| Position/bookmarks | Per-document persisted restore (gander contract) | user-visible regression we lack |
| OCR | Deferred; requires engine decision (Tesseract4Android vs ML Kit) — flag as blocked on size/consent decision | all refs: costly; anonpdf refuses, Pdf_Tools splits flavors |
| License hygiene | Stirling-PDF = checklist only (open-core, not borrowable); Apache-2.0/MIT refs OK for patterns | AGENTS.md license care |
