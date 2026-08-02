---
name: commit-messages
description: Writes clear Conventional Commit messages focused on why. Use when the user asks to commit, draft a commit message, or summarize staged changes.
---

# Commit messages

## Format

```
type(scope): short summary in imperative mood

Optional body: why this change, not a file list.
```

Common types: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `ci`, `perf`

## Rules

- Subject ≤ ~72 chars; no trailing period
- Explain **why**; diff already shows what
- One logical change per commit
- Reference issues when relevant (`Fixes #123`)

## Examples

```
feat(auth): reject expired refresh tokens

Expired tokens previously returned 500. Map to 401 with a stable error code.
```

```
test(billing): cover zero-quantity and negative amount edges
```

## Checklist

- [ ] Type accurate
- [ ] Summary matches the diff
- [ ] No secrets in message or staged files
