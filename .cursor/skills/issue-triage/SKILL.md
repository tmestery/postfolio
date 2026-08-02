---
name: issue-triage
description: Creates and triages GitHub issues with clear repro steps, severity, and acceptance criteria. Use when filing bugs, writing feature requests, grooming a backlog, or labeling/prioritizing issues.
---

# Issue triage

## Bug template

```markdown
## Summary
<one sentence>

## Steps to reproduce
1.
2.
3.

## Expected
## Actual
## Environment
- OS / browser / version / commit

## Severity
blocker | high | medium | low

## Acceptance criteria
- [ ]
```

## Feature template

```markdown
## Problem
## Proposal
## Alternatives considered
## Acceptance criteria
- [ ]
## Out of scope
```

## Triage workflow

1. Deduplicate (search existing issues)
2. Confirm repro or mark `needs-info`
3. Label: `bug` / `enhancement` / `docs` / `good-first-issue`
4. Set severity and owner when known
5. Write acceptance criteria before coding starts

## Quality bar

- Repro is specific enough for a stranger
- Acceptance criteria are testable
- No secrets or PII in the issue body
