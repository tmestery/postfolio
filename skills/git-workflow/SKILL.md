---
name: git-workflow
description: Guides branching, atomic commits, and change sizing for safe collaboration. Use when creating branches, committing, rebasing onto main, or deciding how to split work across commits and PRs.
---

# Git workflow

## Defaults

- Branch from the default branch (`main`/`master`)
- Name: `type/short-description` (e.g. `feat/unit-test-skill`)
- Atomic commits: one logical change each
- Prefer rebase/update onto latest default branch before opening a PR
- Never rewrite shared history unless explicitly requested

## Change sizing

| Size | Guidance |
|------|----------|
| Small | Single concern; preferred |
| Medium | Split commits; one PR OK |
| Large | Split into stacked PRs |

## Safety

- Do not commit secrets (`.env`, keys, tokens)
- Do not use `--force` on shared default branches
- Do not amend commits you did not create / already pushed unless asked
- Run `git status` / `git diff` before committing

## Related

- `commit-messages` for message format
- `pull-requests` for PR creation
