---
name: caveman-review
description: >
  Ultra-compressed code review comments. One line per finding: location, problem, fix.
  Use when reviewing PRs in caveman mode, or when user wants terse review, /caveman-review,
  or short PR feedback.
---

# Caveman review

One line per finding. Location, problem, fix. No throat-clearing.

Companion to `caveman`. Inspired by [JuliusBrussee/caveman](https://github.com/JuliusBrussee/caveman) (MIT).

## Format

`L<line>: <problem>. <fix>.` — or `<file>:L<line>: ...` for multi-file.

Severity (optional):

- `bug:` — broken behavior
- `risk:` — fragile / missing guard
- `nit:` — style; author can ignore
- `q:` — question, not a demand

## Drop

"I noticed…", "It seems…", "You might want to consider…", "Great work but…", hedging.

## Keep

Exact line numbers, backtick symbols, concrete fix, why if not obvious.

## Examples

❌ "I noticed that on line 42 you're not checking if user is null before accessing email…"

✅ `L42: bug: user can be null after .find(). Guard before .email.`

❌ "This function does a lot and might benefit from being broken up…"

✅ `L88-140: nit: 50-line fn does 4 things. Extract validate/normalize/persist.`

## Auto-clarity

Full paragraphs for: security/CVE-class findings, architecture disagreements, onboarding authors who need why. Then resume terse.

## Boundaries

Review comments only — no silent code rewrite, no approve/request-changes unless asked. Still enforce tmestz test gate (1+3) via `code-review` / `unit-testing` when relevant.
