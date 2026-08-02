---
name: pull-requests
description: Creates high-quality pull requests with clear summaries, test plans, and review-friendly diffs. Use when opening a PR, updating a PR description, responding to review, or splitting oversized changes.
---

# Pull requests

## Before opening

- [ ] Branch updated from default branch
- [ ] CI-relevant tests pass locally
- [ ] New behavior has **1 positive + 3 negative** tests
- [ ] No secrets; no unrelated churn
- [ ] Diff is reviewable (split if huge)

## PR body template

```markdown
## Summary
- <1–3 bullets: why + what>

## Test plan
- [ ] <command or manual check>
- [ ] Positive path covered
- [ ] Three edge/negative cases covered

## Notes
- Risk / rollout / follow-ups (if any)
```

## Good practices

- Title: imperative, specific (`Add rate limit to login API`)
- Link issues (`Closes #n`)
- Call out breaking changes and migrations
- Keep discussion on the PR; resolve threads with code or clear decisions
- Prefer stacked PRs over megadiffs

## Responding to review

1. Group feedback: must-fix / nice-to-have / question
2. Fix must-fix in small commits
3. Reply when you disagree — with rationale, not silence
4. Re-request review when ready

## Anti-patterns

- Empty "fix stuff" title
- No test plan
- Mixing refactor + feature + dependency bump
- Force-pushing after reviewers started (unless agreed)
