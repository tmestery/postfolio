---
name: planning
description: Breaks work into small, ordered, verifiable tasks with clear done criteria. Use when planning features, refactors, migrations, or when the user asks for a plan, task breakdown, or roadmap before coding.
---

# Planning

## Workflow

1. Restate the goal in one sentence.
2. List constraints (time, APIs, compatibility, risk).
3. Split into **vertical slices** (each slice deliverable + testable).
4. Order by dependency risk (foundations first).
5. Attach **done criteria** and test expectations (1 positive + 3 negative) per slice.
6. Stop and get approval before large implementation unless told to proceed.

## Task template

```markdown
### Task N: <title>
- Goal:
- Files likely touched:
- Done when:
- Tests: 1 positive, 3 negative (list them)
- Risks:
```

## Anti-patterns

- One giant "implement everything" task
- Tasks without verification
- Mixing research and implementation without a checkpoint
