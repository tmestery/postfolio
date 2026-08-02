---
name: using-tmestz-skills
description: Routes work to the correct tmestz skill and enforces shared operating rules. Use at session start, when unsure which skill applies, or when the user mentions tmestz-skills, AGENTS.md, or project bootstrap.
---

# Using tmestz-skills

## Operating rules

1. Load only the skills needed for the current task.
2. Follow `AGENTS.md` non-negotiables (tests 1+3, PR/issue hygiene, lint, security).
3. Prefer project conventions when they conflict with a skill.
4. Skills are LLM-agnostic — do not require a specific IDE or cloud vendor.

## Router

| Signal | Load |
|--------|------|
| "plan", "break down", "tasks" | `planning` |
| Implement feature / slice | `incremental-implementation`, `unit-testing` |
| Commit / staging | `commit-messages`, `git-workflow` |
| PR / pull request | `pull-requests` |
| Issue / ticket / triage | `issue-triage` |
| Review / merge gate | `code-review` |
| Bug / failing test | `debugging`, `unit-testing` |
| Lint / eslint-disable / CI noise | `lint-hygiene` |
| Auth, secrets, XSS, injection | `security-basics` |
| "caveman", "less tokens", "be brief" | `caveman` (+ `caveman-commit` / `caveman-review` if committing/reviewing) |

## Done check

- [ ] Correct skill(s) loaded
- [ ] Tests satisfy 1 positive + 3 negative for new behavior
- [ ] No secrets committed
- [ ] Diff is reviewably sized
