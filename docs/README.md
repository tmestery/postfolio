# Postfolio docs

Start here depending on the job:

| Doc | Purpose |
|-----|---------|
| **[frontend.md](./frontend.md)** | **Required before any UI work** — structure, design rules, auth UX, anti-patterns |
| **[open-questions.md](./open-questions.md)** | **Locked decisions** + remaining open questions |
| **[agent-trader-v2.md](./agent-trader-v2.md)** | **Deep multi-agent trader redesign** (Groq, debate, capital committee) |
| [plan.md](./plan.md) | Ranked roadmap — vertical slices with done criteria |
| [architecture.md](./architecture.md) | System overview (web app, API, LLM agent) |
| [api.md](./api.md) | HTTP contracts, status codes, FE parsing traps |
| [setup.md](./setup.md) | Local development setup + smoke curls |
| [anti-linter-patterns.md](./anti-linter-patterns.md) | Lint hygiene (from tmestz-skills) |
| [ATTRIBUTION-tmestz-skills.md](./ATTRIBUTION-tmestz-skills.md) | Attribution for vendored agent skills |

Repo root: [`AGENTS.md`](../AGENTS.md) · skills in [`skills/`](../skills/) and [`.cursor/skills/`](../.cursor/skills/).

## Reading order for a new agent session

1. `AGENTS.md`
2. `docs/open-questions.md` — note unanswered P0/P1
3. `docs/plan.md` — current phase
4. If touching the LLM agent → **`docs/agent-trader-v2.md`**
5. If touching UI → **`docs/frontend.md` end-to-end**
6. If wiring HTTP → `docs/api.md`
