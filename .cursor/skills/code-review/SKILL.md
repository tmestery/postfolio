---
name: code-review
description: Reviews diffs across correctness, readability, architecture, security, and performance. Use before merge, when reviewing PRs, or when evaluating agent- or human-written code.
---

# Code review

## Standard

Approve when the change **improves overall code health**, even if imperfect. Block on correctness bugs, security issues, missing critical tests, or unreadable design.

## Five axes

1. **Correctness** — matches intent; edge cases; error paths; tests assert the right thing
2. **Readability** — clear names; simple control flow; no cleverness debt
3. **Architecture** — fits existing patterns; no leaky abstractions; deps flow correctly
4. **Security** — input trust boundaries; authz; no secrets; injection/XSS
5. **Performance** — no accidental O(n²), N+1, or unbounded work on hot paths

## Severity labels

| Label | Meaning |
|-------|---------|
| Blocker | Must fix before merge |
| Suggestion | Should improve |
| Nit | Optional style |
| FYI | Context only |

## Test gate

For new behavior, require evidence of:

- 1 positive test
- 3 negative / edge tests

If missing, mark **Blocker** and point to `unit-testing`.

## Output format

```markdown
### Summary
### Blockers
### Suggestions
### Nits / FYI
### Test coverage check
```
