# Anti-linter patterns

Patterns that create false-green CI, hide bugs, or fight the toolchain. Agents and humans should avoid these.

## 1. Blanket file-level disables

```js
/* eslint-disable */
```

**Why bad:** Turns off the safety net for the whole file.  
**Do instead:** Fix issues or disable one rule on one line with a reason.

## 2. Ignoring types to ship faster

```ts
// @ts-ignore
// @ts-expect-error without explanation
const x: any = getData()
```

**Why bad:** Deletes the compiler's ability to catch real bugs.  
**Do instead:** Type the value, narrow with guards, or `@ts-expect-error` with a ticket/why on the next line only.

## 3. Disabling security-relevant rules

Examples: `no-eval`, `no-implied-eval`, React `dangerouslySetInnerHTML` without review, Bandit/`# nosec` without justification.

**Do instead:** Redesign; if unavoidable, document threat model and scope the exception.

## 4. Global rule downgrades in CI

Changing `"error"` → `"warn"` / removing `--max-warnings 0` to pass a PR.

**Do instead:** Fix or quarantine with a tracked debt issue and time-boxed allowlist.

## 5. Generated formatting thrash

Mixed Prettier/ESLint versions or editor settings reformatting thousands of unrelated lines.

**Do instead:** One formatter version in CI; format only touched files unless doing a dedicated format PR.

## 6. Inline suppressions without rationale

```python
# noqa
```

**Do instead:**

```python
# noqa: F401  # re-export for public API
```

## 7. Catching-all to silence linters

```ts
try {
  doWork()
} catch (e) {
  // empty — "so lint is happy about unused"
}
```

**Why bad:** Swallows failures.  
**Do instead:** Handle or rethrow; assert in tests.

## 8. Copy-paste `any` / `object` / untyped configs

Especially in API clients and form handlers.

**Do instead:** Shared schemas (Zod/JSON Schema/OpenAPI types).

## 9. Excluding large paths from lint forever

`ignorePatterns: ["src/**"]` style escapes.

**Do instead:** Exclude only `dist/`, generated code, or vendored third parties.

## 10. Fixing lint by deleting the test

Never remove coverage to quiet a rule about unused vars or floating promises.

---

## Agent checklist

Before committing:

- [ ] No new file-level disables
- [ ] Every suppression has rule id + reason
- [ ] CI lint config not weakened
- [ ] Diff doesn't include unrelated format churn

Related skill: `skills/lint-hygiene/SKILL.md`.
