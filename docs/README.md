# Postfolio docs

Start here depending on the job:

| Doc | Purpose |
|-----|---------|
| **[frontend-ui-guide.md](./frontend-ui-guide.md)** | **Required before any UI work** — structure, design rules, auth UX, anti-patterns |
| **[product-decisions.md](./product-decisions.md)** | **Locked decisions** + remaining open questions |
| **[agent-trader-v2.md](./agent-trader-v2.md)** | Multi-agent trader (current Groq pipeline) |
| **[agent-trader-v3.md](./agent-trader-v3.md)** | **Implemented:** web research crew (no Finnhub) |
| **[agent-trader-v4.md](./agent-trader-v4.md)** | Plan: paper portfolio P&L tracking |
| **[social-network-design.md](./social-network-design.md)** | Follows, networked feed, profiles, notifications (design) |
| [implementation-roadmap.md](./implementation-roadmap.md) | Ranked roadmap — vertical slices with done criteria |
| [system-architecture.md](./system-architecture.md) | System overview (web app, API, LLM agent) |
| [rest-api-contract.md](./rest-api-contract.md) | HTTP contracts, status codes, FE parsing traps |
| [local-development.md](./local-development.md) | Local development setup + smoke curls |
| [lint-anti-patterns.md](./lint-anti-patterns.md) | Lint hygiene (from tmestz-skills) |
| [attribution-tmestz-skills.md](./attribution-tmestz-skills.md) | Attribution for vendored agent skills |

Repo root: [`AGENTS.md`](../AGENTS.md) · skills in [`skills/`](../skills/) and [`.cursor/skills/`](../.cursor/skills/).

## Reading order for a new agent session

1. `AGENTS.md`
2. `docs/product-decisions.md` — note unanswered P0/P1
3. `docs/implementation-roadmap.md` — current phase
4. If touching the LLM agent → **`docs/agent-trader-v2.md`**
5. If touching UI → **`docs/frontend-ui-guide.md` end-to-end**
6. If wiring HTTP → `docs/rest-api-contract.md`
