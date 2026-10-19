# RECOVERY — recovering a hosted session (superz1402/nexpdf)

## If the session/server dies

1. Provision a fresh environment (any Linux box with git + python3 + curl).
2. Re-clone: `git clone https://github.com/superz1402/nexpdf && cd nexpdf` (auth: fresh PAT, only if private).
3. Read, in order: `SESSION_STATE.md` → latest entries in `worklog.md` → `docs/TASK_REGISTER.md` → `docs/BUILD_ENVIRONMENT.md`.
4. Check GitHub Issues for status (labels: `in-progress`, `blocked`).
5. Read `github.com/superz1402/nexpdf/actions` — the latest CI run is the authoritative build result for the last commit.
6. Resume the first incomplete task per `docs/TASK_REGISTER.md` priority. Do not redo completed tasks.

## Build verification without a local SDK

- Builds are CI-only. After every push, poll:
  `curl -H "Authorization: token $GH_TOKEN" https://api.github.com/repos/superz1402/nexpdf/actions/runs?per_page=1`
  - `conclusion: success` → build green; artifact `nexpdf-debug-apk` attached.
  - `conclusion: failure` → fetch log: `curl -H auth .../actions/runs/<id>/logs -L -o /tmp/ci.log` and triage.
- Iterate on compile errors by editing locally → commit → push → re-poll. Typical red→green loop is ~4–8 min/run.

## Committing and pushing (no git credentials stored)

```
export GH_TOKEN=$(cat /tmp/opencode/gh-token)   # ephemeral, never committed
git add <explicit files>
git commit -m "message"
git push https://x-access-token:$GH_TOKEN@github.com/superz1402/nexpdf.git main
```
Never persist the token into `.git/config`.

## Known: suspension / 403 from GitHub

Accounts in this org have had suspensions before (see `ansaribilal14` mirror repos). If API calls return 403, stop writing, verify latest commit was pushed (`git log -1`, remote ref check), and record blocker in `SESSION_STATE.md`.
