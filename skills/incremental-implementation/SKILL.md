---
name: incremental-implementation
description: Implements changes as thin vertical slices — code, test, verify, then commit. Use when building features, refactors spanning multiple files, or when changes risk becoming large unreviewable diffs.
---

# Incremental implementation

## Loop

For each slice:

1. Write or update tests first when behavior is clear (see `unit-testing`)
2. Implement the minimum code to pass
3. Run the relevant test/lint commands
4. Commit the slice (see `commit-messages`)
5. Only then start the next slice

## Rules

- Prefer ~100–300 line diffs per commit when practical
- Do not leave the tree broken between slices
- Feature flags / safe defaults for risky behavior
- No drive-by refactors outside the slice

## Exit criteria

- [ ] Slice meets its done criteria
- [ ] 1 positive + 3 negative tests for new behavior
- [ ] Lint clean without unjustified suppressions
