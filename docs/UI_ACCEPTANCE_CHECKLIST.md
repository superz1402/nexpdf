# UI ACCEPTANCE CHECKLIST (per screen)

Apply every push that touches UI:

- [ ] Builds green in CI (`assembleDebug`).
- [ ] Edge-to-edge insets: no content under system bars on either screen state.
- [ ] Single primary action; no generic hero-card patterns; no fake data.
- [ ] Empty/loading/error states all designed (not blank screens).
- [ ] Touch targets ≥48dp; content descriptions on all icons.
- [ ] Contrast satisfies Material role pairings (scheme tokens, not raw colors).
- [ ] Motion: spring/fade ≤450ms, no input blocking.
- [ ] Typography: only MaterialTheme roles; titles bold/semibold.
- [ ] Real runtime data rendering verified by code review of data path (MediaStore / renderer); no device here, so device runtime verification recorded as limitation.
- [ ] Before/after screenshots where tooling allows (CI workflow for screenshots tracked as issue).

Known limitation: no emulator/device in this environment ("runtime-first checkpoint" adapted: build verification only; manual on-device testing is on the user).
