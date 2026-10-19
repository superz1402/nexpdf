# AGENTS.md — NexPDF engineering rules

- GitHub (`superz1402/nexpdf`, branch `main`) is the durable source of truth. The server is ephemeral.
- Every actionable task gets a GitHub Issue; lifecycle: OPEN → IN PROGRESS → IMPLEMENTED → TESTED → COMMITTED → PUSHED → VERIFIED → CLOSED.
- Builds run in GitHub Actions (`.github/workflows/ci.yml`). There is NO local Android SDK — never install one; disk is limited. Verify builds via `gh`-less REST API checks (see docs/RECOVERY.md).
- Commit each coherent unit; push promptly; update `worklog.md` + `SESSION_STATE.md` after every task.
- Never commit tokens, keystores, `local.properties`, or build outputs.
- UI is UX-first: Material 3, no generic AI dashboard patterns, real runtime data, no fake metrics/placeholder cards.
- Code style: no comments unless required; match existing conventions.
