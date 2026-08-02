---
name: debugging
description: Systematically finds root causes with evidence before patching. Use when tests fail, production bugs appear, CI is red, or the user asks to debug unexplained behavior.
---

# Debugging

## Loop

1. **Reproduce** — smallest reliable repro
2. **Locate** — read the failing test/log/stack; form one hypothesis
3. **Prove** — add a failing test or assert that captures the bug
4. **Fix** — minimal change
5. **Verify** — test passes; nearby regressions checked
6. **Prevent** — keep the new tests (1 positive if missing + negatives for the bug class)

## Rules

- Do not shotgun-edit without a repro
- Prefer evidence (logs, bisect, bisecting commits) over guessing
- Fix root cause, not only symptoms
- If flaky: isolate timing/order/shared state

## Output

```markdown
### Repro
### Root cause
### Fix
### Tests added
```
