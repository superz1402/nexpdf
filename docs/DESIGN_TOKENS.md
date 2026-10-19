# DESIGN TOKENS — NexPDF

Status: v0.1 baseline. Will be extended after per-repo Compose inspection (docs/UI_RESEARCH.md).

## Principles

1. Content first — the document is the hero; chrome recedes.
2. One primary action per screen; tools live in predictable surfaces (bottom sheets, tool page).
3. Motion: spring-based, 200–450ms; nothing decorative that blocks input; Respect reduced-motion later.
4. Accessibility: ≥48dp targets, AA contrast (auto-verified via Material color roles), content descriptions everywhere.

## Color

- Dynamic color (Material You) enabled on Android 12+; fallback brand scheme = warm ember orange (#FF8956 family, Light/Dark schemes in `ui/theme/Theme.kt`).
- Semantic roles only (primary/secondary/surface/error) — never hard-coded hex in composables.

## Typography

- Default (Roboto/system) via MaterialTheme.typography; bold titles, 0–0.15sp letter spacing. No custom font license burden in v0.1.

## Shape

- Cards/lists 16dp, sheets 28dp, buttons stadium. Tool chips 24dp.

## Elevation

- Material default tiers: 0, 1, 3, 6dp. Content surfaces at tonal elevation from scheme roles (surfaceContainer/Highest). No raw shadows.

## Motion tokens (pre-1.4 material3 Expressive)

- Screen enter: fade 220ms + subtle scale (0.96→1). List item appear: 300ms spring. Page render: crossfade 250ms. Tool sheet: slide-spring from bottom.

## Layout

- Edge-to-edge everywhere; `Scaffold` innerPadding always respected; phone portrait first, landscape viewer supported; large-screen 2-pane is a post-1.0 goal (tracked issue later).
