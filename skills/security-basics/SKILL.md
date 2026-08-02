---
name: security-basics
description: Applies a baseline security checklist for auth, secrets, injection, and trust boundaries. Use when touching authentication, user input, file uploads, SQL/HTML rendering, payments, or dependencies.
---

# Security basics

## Checklist (every relevant change)

- [ ] No secrets in code, tests, logs, or git history
- [ ] Untrusted input validated/sanitized at boundaries
- [ ] Authn/authz checked on every sensitive path
- [ ] Queries parameterized (no string-built SQL)
- [ ] Output encoded for HTML/JS contexts (XSS)
- [ ] SSRF/path traversal considered for URLs and file paths
- [ ] Dependencies pinned; no known critical CVEs introduced
- [ ] Error messages don't leak internals to clients

## Secrets

- Use env / secret manager
- Rotate if exposed
- Add patterns to `.gitignore`

## When unsure

Mark as **Blocker** in review and ask for a human security pass before merge.
