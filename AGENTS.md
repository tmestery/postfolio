# AGENTS.md

Instructions for any coding agent working in **Postfolio**.

Postfolio is a full-stack **web** app: React (Vite) frontend + Spring Boot REST API. Users track and share stock investments; an LLM agent trader researches news (RAG) and simulates trades. **Not** a native mobile app.

## Before you start

1. Read this file.
2. Skim `docs/open-questions.md` — honor **locked** decisions; do not invent answers for remaining open items (use working assumptions only).
3. Read `docs/plan.md` for the **ranked** slice you are implementing.
4. **Any LLM agent trader work:** read `docs/agent-trader-v2.md` (Groq deep multi-agent redesign).
5. **Any follows / feed graph / notifications work:** read `docs/social-network.md` first; do not implement until that design is approved.
6. **Any frontend / UI / CSS / page work:** read `docs/frontend.md` fully and follow it literally (it overrides generic UI habits).
7. **Any API wiring:** read `docs/api.md`.
7. Load matching skills from `skills/` or `.cursor/skills/`.

## Locked demo decisions (summary)

- **Auth:** `localStorage` key `postfolio.session` — not JWT/cookies.
- **DB:** Local **PostgreSQL** only — not H2.
- **Signup:** username, email, password required; names optional; no DOB; `accountPublicStatus: true`.
- **Post-signup:** auto-login → `/`.
- **Create post:** send `username` from session (demo bridge) when server has no principal.
- **Build order:** Postgres → auth FE → localStorage → username bridge → feed → create post → agent → extras.

## Non-negotiables (tmestz-skills)

- **Tests**: For every feature or behavior change, ship **1 positive** test and **3 negative / edge-case** tests. See `skills/unit-testing/SKILL.md`.
- **PRs & issues**: Clear summaries, repro steps, and test plans.
- **Lint**: No blanket linter disables; see `docs/anti-linter-patterns.md`.
- **Security**: No secrets in git; validate untrusted input. API keys via env (`FINNHUB_API_KEY`, etc.) — never in the frontend bundle.
- **Scope**: Small, reviewable diffs. Prefer vertical slices (`docs/plan.md`).

## Project map

| Path | Role |
|------|------|
| `Frontend/` | React 19 + Vite + Tailwind SPA |
| `Backend/postfolio/` | Spring Boot 4 / Java 21 API |
| `docs/` | Architecture, API, frontend bible, setup, roadmap, open questions |
| `skills/` | Portable agent skills (LLM-agnostic) |

## Skill routing

| Task | Skill |
|------|-------|
| Plan / break down work | `planning` |
| Implement a feature | `incremental-implementation` + `unit-testing` |
| Commit | `commit-messages` + `git-workflow` |
| Open or update a PR | `pull-requests` |
| Create / triage issues | `issue-triage` |
| Review code | `code-review` |
| Fix a bug | `debugging` + `unit-testing` |
| Lint / CI noise | `lint-hygiene` |
| Security-sensitive change | `security-basics` |
| Short / cheap replies | `caveman` |

When user says `caveman` / `talk like caveman` / `/caveman`, load `caveman`. Off: `normal mode`.

## Stack notes

- Frontend API base: Vite `import.meta.env.VITE_SERVER_URL` (see `docs/frontend.md` — current `process.env` usage is wrong).
- Backend CORS expects Vite at `http://localhost:5173`.
- Login success is **HTTP 202 + plain text username**, not JSON — see `docs/api.md`.
- Demo session: `localStorage['postfolio.session']`.
- Database: **PostgreSQL** local — see `docs/setup.md`.
- Agent pipeline needs local **Ollama** (`llama3`, `nomic-embed-text`) + Finnhub key.
- Prefer project docs over improvising design systems, auth schemes, or mobile frameworks.
