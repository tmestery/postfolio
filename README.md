<div align="center">

# Postfolio

**Share your trades. Watch the market together.**

A full-stack social investing web app — post your stock trades, browse a shared feed,
and follow an LLM agent trader that reads market news and simulates its own picks.

[![CI](https://github.com/tmestery/postfolio/actions/workflows/maven.yml/badge.svg)](https://github.com/tmestery/postfolio/actions/workflows/maven.yml)
![Java](https://img.shields.io/badge/Java-21-b07219?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-6DB33F?logo=springboot&logoColor=white)
![React](https://img.shields.io/badge/React-19-61DAFB?logo=react&logoColor=black)
![Vite](https://img.shields.io/badge/Vite-7-646CFF?logo=vite&logoColor=white)
![Tailwind CSS](https://img.shields.io/badge/Tailwind-4-38BDF8?logo=tailwindcss&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-local-4169E1?logo=postgresql&logoColor=white)
![Groq](https://img.shields.io/badge/Groq-LLM-orange)

*Web app (desktop + mobile browsers) — not a native mobile app. Simulated trades only; not financial advice.*

</div>

---

## What it does

| Pillar | Description |
|--------|-------------|
| **Social investment feed** | Sign up, post trades (ticker, shares, amount, date), and browse everyone's public positions newest-first. Search by ticker, delete your own posts, and flip your account private to drop out of the feed. |
| **AI agent trader** | A deep multi-agent desk on Groq: Finnhub news → bull/bear debate → stock judge → capital committee (aggressive / balanced / defensive + cash guard + capital judge) → risk gate → simulated fills within a $1,000 allowance. Full `agentTrace` for the UI. |
| **Demo-grade auth** | Deliberately simple `localStorage` session (`postfolio.session`) with auto-login after signup and protected routes — no JWT/cookie machinery, by design (see [locked decisions](docs/product-decisions.md)). |

## Architecture

```
┌──────────────────────────────────────────────────────────────┐
│  Browser — React 19 + Vite 7 + Tailwind 4                    │
│  /  /login  /signup  /post/new  /agent  /account  (+404)     │
│  session: localStorage["postfolio.session"]                  │
└─────────────────────────────┬────────────────────────────────┘
                              │ JSON over HTTP
                              │ CORS: localhost:5173 → :8080
┌─────────────────────────────▼────────────────────────────────┐
│  Spring Boot 4 (Java 21)                                     │
│  controllers → services → Spring Data JPA                    │
│  BCrypt passwords · username demo bridge · owner checks      │
└──────┬────────────────────┬─────────────────────┬────────────┘
       │                    │                     │
       ▼                    ▼                     ▼
  PostgreSQL           Finnhub API           Groq API
  users + posts        news + quotes         chat completions
  + agent_run
```

### The agent pipeline, step by step

1. **News Scout** — Finnhub market headlines
2. **Bull → Bear → Stock Judge** — adversarial ticker debate on Groq
3. **Quote snapshot** — one Finnhub price map; drop unquoted names
4. **Capital committee** — Aggressive / Balanced / Defensive allocators + Cash Guard + Capital Judge
5. **Position Sizer + Risk Gate** — dollars → whole shares; enforce reserve floor & weight caps
6. **Executor** — simulated fills at the same snapshot prices
7. **Persist** — run summary + full `RunResult` JSON for history

Every dependency failure (missing Groq/Finnhub key, provider down) returns a
structured `503 + {"error": "..."}` that the UI surfaces verbatim. Details: [docs/agent-trader-v2.md](docs/agent-trader-v2.md).

## Feature checklist

- [x] Signup with validation — `201`, duplicate username/email → `409`, passwords BCrypt-hashed and never serialized
- [x] Login (`202` + username), auto-login after signup, session survives refresh
- [x] Protected routes: `/post/new`, `/agent`, `/account` redirect guests to login
- [x] Feed with loading / error / empty states, public-accounts-only filtering
- [x] Ticker search (uppercase-normalized) and two-step inline owner delete — no `alert()`
- [x] Create post with field validation; server computes price/share
- [x] Agent page with trace timeline, capital committee panel, fills, and recent-run history
- [x] Account privacy toggle that immediately hides/shows your posts in the feed
- [x] 404 route, custom design tokens (Fraunces + Instrument Sans, warm paper + market green)
- [x] Automated tests (backend MockMvc/unit · frontend Vitest) and CI

## Quick start

**Prerequisites:** Docker **or** (Java 21 · Node 20+ · PostgreSQL) · *(for the agent)* [Groq](https://groq.com) + [Finnhub](https://finnhub.io) API keys.

```bash
# 0. Secrets (repo root — never commit .env)
cp .env.example .env
# edit GROQ_API_KEY=... and FINNHUB_API_KEY=...

# Option A — full stack via Docker
make up
# Frontend http://localhost:5173 · Backend http://localhost:8080

# Option B — Postgres in Docker, apps on the host
make postgres
make backend     # terminal 1
make frontend    # terminal 2
```

<details>
<summary>Manual host setup (without Make)</summary>

```bash
# 1. Database (once) — or: docker compose up -d postgres
createdb postfolio

# 2. Backend  →  http://localhost:8080
cd Backend/postfolio
# root .env is auto-loaded; or export keys yourself
./mvnw spring-boot:run

# 3. Frontend  →  http://localhost:5173   (separate terminal)
cd Frontend
cp ../.env.example .env   # or: make env
npm install
npm run dev
```

</details>

Full walkthrough and smoke-test curls: [docs/local-development.md](docs/local-development.md). Keys live in root [`.env.example`](.env.example).

## API at a glance

| Endpoint | Method | Success | Failure |
|----------|--------|---------|---------|
| `/credentials/signup/` | POST | `201` + user | `400` missing fields · `409` duplicate |
| `/credentials/login/` | POST | `202` + username (plain text) | `400` |
| `/post/feed/` | GET | `200` + posts (public accounts, newest first) | — |
| `/post/stock/` | POST | `201` + post | `400` validation / unknown user |
| `/post/stock/search/?stockName=` | POST | `200` + posts (may be `[]`) | — |
| `/post/delete/?postId=&username=` | POST | `204` | `400` / `403` not owner / `404` |
| `/account/status/` | GET / POST | `200` + visibility | `400` / `404` |
| `/trade/stock/test/` | GET | `200` + `RunResult` (paper book + trace) | `503` + `{error}` |
| `/trade/stock/execute/` | GET | `200` + `RunResult` (fills + trace) | `503` + `{error}` |
| `/trade/runs/` | GET | `200` + recent run summaries | — |

Full contracts with request/response bodies: [docs/rest-api-contract.md](docs/rest-api-contract.md).

## Testing

```bash
# Backend — 35 tests (MockMvc + unit, in-memory H2; runtime stays Postgres-only)
cd Backend/postfolio && ./mvnw test

# Frontend — 11 tests (Vitest + jsdom: API client, session storage, app render)
cd Frontend && npm test && npm run lint
```

CI runs both suites on every push and pull request
([workflow](.github/workflows/maven.yml)): a Maven job on JDK 21 and a Node job
(install → lint → test → build).

## Project structure

```
postfolio/
├── Backend/postfolio/          # Spring Boot 4 · Java 21 REST API
│   └── src/main/java/com/postfolio/postfolio/
│       ├── controllers/        # auth, posts, account, agent trades
│       ├── models/             # WebUser + Post entities, repos, services
│       ├── security/           # BCrypt, CORS, route permits
│       └── stockInvestmentAgents/  # manager, analyzers, RAG, executor
├── Frontend/                   # React 19 + Vite 7 + Tailwind 4 SPA
│   └── src/
│       ├── api/                # fetch client + per-domain helpers
│       ├── auth/               # localStorage session + AuthContext
│       ├── components/         # Layout, PostCard, Field
│       └── pages/              # home, login, signup, newpost, agent, account, 404
├── docs/                       # architecture · api · frontend bible · setup · plan
├── skills/                     # portable coding-agent skills (tmestz-skills)
└── AGENTS.md                   # instructions for coding agents
```

## Documentation

| Doc | Read it when |
|-----|--------------|
| [docs/README.md](docs/README.md) | You want the index |
| [docs/system-architecture.md](docs/system-architecture.md) | Understanding how the pieces fit |
| [docs/rest-api-contract.md](docs/rest-api-contract.md) | Wiring any HTTP call |
| [docs/frontend-ui-guide.md](docs/frontend-ui-guide.md) | **Any UI work** — design tokens, rules, anti-patterns |
| [docs/local-development.md](docs/local-development.md) | Running locally / troubleshooting |
| [docs/implementation-roadmap.md](docs/implementation-roadmap.md) | Roadmap and what shipped |
| [docs/product-decisions.md](docs/product-decisions.md) | Locked decisions + open product questions |
| [docs/social-network-design.md](docs/social-network-design.md) | Follows, networked feed, notifications (design) |
| [docs/agent-trader-v2.md](docs/agent-trader-v2.md) | Multi-agent trader design |
| [docs/agent-trader-v3.md](docs/agent-trader-v3.md) | Plan: web research (no Finnhub) |
| [docs/agent-trader-v4.md](docs/agent-trader-v4.md) | Plan: paper portfolio P&L |

Coding agents start at [`AGENTS.md`](AGENTS.md).

## Design notes

The UI follows a deliberate direction documented in [docs/frontend-ui-guide.md](docs/frontend-ui-guide.md):
warm paper background, ink text, a single deep market-green accent, **Fraunces** display
serif with **Instrument Sans** body type, and monospaced tickers. No purple gradient
SaaS clones, no `alert()` dialogs, no component-library dumps.

## Contributors

- **Mason Hart**
- **Tyler Mestery** — [@tmestery](https://github.com/tmestery)
