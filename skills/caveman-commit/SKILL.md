---
name: caveman-commit
description: >
  Ultra-compressed Conventional Commit messages. Subject ≤50 chars when possible, body only
  for non-obvious why. Use with caveman mode or when user wants short commits, /caveman-commit,
  or terse commit messages.
---

# Caveman commit

Write commit messages terse and exact. Conventional Commits. Why over what.

Companion to `caveman`. Inspired by [JuliusBrussee/caveman](https://github.com/JuliusBrussee/caveman) (MIT).

## Subject

```
type(scope): imperative summary
```

- Types: `feat`, `fix`, `refactor`, `perf`, `docs`, `test`, `chore`, `build`, `ci`, `style`, `revert`
- Imperative: "add"/"fix" — not "added"/"adds"
- ≤50 chars when possible, hard cap 72
- No trailing period

## Body

Skip if subject enough. Add only for: non-obvious why, breaking changes, migrations, linked issues.

Wrap 72. Bullets `-`. End with `Closes #n` / `Refs #n` when relevant.

## Never

- "This commit does X", "I", "we", "now"
- Restating filenames already in scope
- Emoji unless project requires
- AI attribution unless user/project requires trailer

## Examples

```
feat(api): add GET /users/:id/profile

Mobile needs slim profile payload on cold launch.

Closes #128
```

```
feat(api)!: rename /v1/orders to /v1/checkout

BREAKING CHANGE: /v1/orders → 410 after 2026-06-01.
```

## Boundaries

Message only unless user asks to commit. Prefer this over verbose `commit-messages` when caveman active.
