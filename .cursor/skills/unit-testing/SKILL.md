---
name: unit-testing
description: Requires 1 positive and 3 negative/edge-case unit tests for every feature or behavior change. Use when adding features, fixing bugs, writing tests, or reviewing test coverage.
---

# Unit testing (1 positive + 3 negative)

## Mandate

For **every feature, bugfix, or behavior change**, add at least:

| # | Kind | Purpose |
|---|------|---------|
| 1 | **Positive** | Happy path — correct input → expected output/effect |
| 2 | **Negative** | Invalid / rejected input or unauthorized path |
| 3 | **Negative / edge** | Boundary (empty, zero, max, off-by-one, null/absent) |
| 4 | **Negative / edge** | Failure mode (timeout, conflict, not-found, partial failure) |

Do not merge behavior changes that only have happy-path coverage.

## Workflow

1. Name the behavior under test in one sentence
2. List the 1+3 cases **before** or while implementing
3. Use the project's test runner (Jest, Vitest, pytest, go test, etc.)
4. Assert behavior, not implementation details
5. Run the new tests; fix failures before moving on

## Case design prompts

Ask for each change:

- What does success look like? → **positive**
- What input must be rejected? → **negative**
- What happens at empty / zero / max / missing? → **edge**
- What happens when a dependency fails? → **failure**

## Example (pseudocode)

```text
Feature: create_user(email, name)

+ positive: valid email+name → user persisted, id returned
- negative: malformed email → validation error, nothing persisted
- edge: empty name → validation error
- failure: duplicate email → conflict error, original row unchanged
```

## Anti-patterns

- Only snapshot/UI tests with no unit coverage for logic
- Tests that assert mocks were called but not outcomes
- Skipping negatives "because validation is obvious"
- One mega-test that hides which case failed

## Checklist

- [ ] 1 positive named and implemented
- [ ] 3 negatives/edges named and implemented
- [ ] Tests fail if the bug/feature is reverted
- [ ] No flaky time/network assumptions without control
