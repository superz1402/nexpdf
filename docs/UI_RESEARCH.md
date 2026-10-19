# UI RESEARCH — NexPDF (source: superz1402 / bilal140202 / ansaribilal14)

Method: repositories discovered via GitHub API on 2026-10-19 (token owner `superz1402`). Relevance judged by README + metadata; actual source inspection via raw.githubusercontent.com, no local clones. Facts vs judgments are separated. Access was full for public repos; nothing was found to be private/unavailable.

## Candidate inventory (Android / Kotlin / UI-relevant)

| Repo | Owner | Relevance | Verified via |
|---|---|---|---|
| MomoCut | superz1402 | Video editor, Kotlin — architecture/UX-adjacent | API only |
| linuxify | superz1402 / bilal140202 | Kotlin Android, terminal UI | API only |
| Your-Files-Redesign | bilal140202 | File manager "redesigned UI/UX (Material …)" — closest analog: document library | API only |
| AuraGallery | bilal140202 | Offline AI photo gallery — grid/list UI, search | API only |
| tabit-android | bilal140202 / ansaribilal14 | "100% offline Kotlin/Compose habit table" — Compose patterns | API only |
| VoxFlow | bilal140202 | Voice dictation Android — recording UX | API only |
| jarvis-agent / jarvis | bilal140202 / ansaribilal14 | Android on-device agent — dialogs/sheets patterns | API only |
| comet-x-browser | bilal140202 | Android WebView browser — browser chrome, toolbar UX | API only |
| MobileAgent | bilal140202 | Screen-control agent | API only |
| EchoID-Android | ansaribilal14 | Kotlin + Compose, music identity | API only |
| babymomo | ansaribilal14 | Kotlin AI companion app | API only |
| zerium-browser / zerium-gecko | ansaribilal14 | Java Android browsers — privacy chrome | API only |
| open-video-engine-android | ansaribilal14 | Android client of video engine | API only |
| Pdfeditor / Pdfcanvas | bilal140202 | Client-side PDF editing web apps — direct domain overlap | API only |

## Deeper inspection status

Inspection of actual Compose source files (theme, components, navigation) had NOT yet started at session start — this was blocked by the priority of establishing repo persistence first. Deep per-file analysis (≥5 repos, actual `*.kt` reading, commit SHAs recorded) is tracked as GitHub Issue and will be appended to this document. **Do not treat rows above as pattern evidence yet** — they are only discovery facts.

## Design judgments for NexPDF (initial, will be revised after inspection)

1. Library screen should follow archive/list idioms (files managers, galleries): dense grid or list rows, real metadata (size/date/pages), visual differentiation by content, not icon-only rows.
2. Browser repos hint: powerful tools need collapsible toolbars/bottom sheets — PDF tools (annotate, organize, sign) map to bottom-sheet tool rails, not overflowing menus.
3. Pdfeditor/Pdfcanvas (web) likely encode our exact domain (client-side PDF editing); audit their feature splits for a feature-parity reference.
4. No repo is a Compose Material-3 reference for a *documents* app yet — NexPDF becomes the reference; design tokens must be original (see DESIGN_TOKENS.md), not copied.
