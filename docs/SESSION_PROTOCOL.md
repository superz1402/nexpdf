# SESSION PROTOCOL — NexPDF

## Start of session
1. `git log -1` + `git status` + fetch remote; confirm source revision is on GitHub.
2. Read `SESSION_STATE.md`, latest `worklog.md` entries, this file, `docs/RECOVERY.md`, `docs/TASK_REGISTER.md`.
3. Check last CI run conclusion (REST poll per docs/RECOVERY.md). CI failure on latest commit ⇒ triage log before new work.
4. Resume first incomplete task by priority/dependencies. Never redo verified-complete work.

## During task
- Issue created/labeled before implementation; set `in-progress`.
- Checkpoint after each subsystem: record task, files, state, commit SHA, push status, next action in worklog.
- Before risky ops (dependency bumps, migrations): commit+push partial work first; bound every long command with timeouts; one build at a time.
- Never leave critical output only in scrollback: CI logs live in Actions; local logs → /tmp/opencode.

## End of session
1. Stop new work; inspect `git status`; run checklist (docs/UI_ACCEPTANCE_CHECKLIST.md for UI changes).
2. Commit everything legitimate (explicit `git add`; never secrets); push; verify remote (`git log origin/main -1` via API or refs).
3. Update `worklog.md` entry + `SESSION_STATE.md` (last commit, CI state, next exact action, blockers).

## Recovery exercise (repeat after major workflow changes)
Make small change → CI green → record SHA → fresh-clone in /tmp/opencode → follow docs/RECOVERY.md → resume without conversation context. Result documented in worklog. Do not delete the only copy of anything during the test.
