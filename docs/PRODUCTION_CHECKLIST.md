# PRODUCTION_CHECKLIST — NexPDF

Program exit criteria (checked incrementally, never aspirationally).

## Product principles (§2 of the program) — verified continuously
- [x] Free core, no ads, no paywalls, no accounts
- [x] Offline-first; no upload paths; no telemetry; no silent document egress
- [x] Minimum permissions (SAF + MediaStore; no INTERNET) — re-assert on each release
- [x] Original documents never modified by saves
- [x] No fake buttons/placeholder/misleading messages (skeletons only where loading real data)

## Release engineering
- [ ] Minified signed release variant built in CI (owner signs; keystore never lands in repo)
- [ ] Permission-contract check in CI (assert merged manifest declares nothing new without review)
- [ ] versionCode/versionName bump policy documented per release
- [ ] Release notes list honest limitations + NOT RUN items

## Quality gates
- [x] CI green on main (compile) for every release
- [ ] JVM test suite green (once seeded)
- [ ] docs/TEST_PLAN.md executed for the shipped build; results logged in worklog
- [ ] FEATURE_MATRIX statuses updated to match shipped reality

## Performance (§9) — target definitions live in docs/TESTING.md; all NOT RUN until measured on the reference device
- [ ] Open ordinary local PDF < 2 s
- [ ] Scroll jank-free at default zoom
- [ ] Bounded bitmap cache; no all-pages-in-memory regressions
- [ ] Cancellation of stale renders
- [ ] Documented graceful-failure profile for very large docs (>500 MB)

## Security (§7)
- [ ] No logs of document contents; no password echo
- [ ] Temp/decrypt files excluded from backup and cleaned deterministically
- [ ] Dependency/license review per new dependency
- [ ] Optional EXIF/metadata scrub awareness in converters
