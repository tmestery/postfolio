---
name: lint-hygiene
description: Prevents anti-linter patterns such as blanket eslint-disable, ignored type errors, and formatting fights. Use when fixing lint CI, adding suppressions, or cleaning lint debt.
---

# Lint hygiene

## Rules

1. Fix the root cause before suppressing
2. Suppressions must be **narrow** (single line/rule) and **justified**
3. Never commit `eslint-disable` / `@ts-ignore` / `nolint` without a why comment
4. Do not weaken CI to greenwash (turning errors to warnings globally)
5. Match project formatter; don't reformat unrelated files

## Allowed suppression shape

```js
// eslint-disable-next-line no-await-in-loop -- sequential API rate limit
await send(batch[i])
```

## Forbidden

- File-level disable of many rules
- Disabling `no-explicit-any` across a module to avoid typing
- Committing generated `prettier` thrash from mixed versions
- Leaving failing lint "for later" on shared branches

## Full reference

See [docs/lint-anti-patterns.md](../../docs/lint-anti-patterns.md).
