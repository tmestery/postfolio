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
![Ollama](https://img.shields.io/badge/Ollama-llama3-black)

*Web app (desktop + mobile browsers) — not a native mobile app. Simulated trades only; not financial advice.*

</div>

---

## What it does

| Pillar | Description |
|--------|-------------|
| **Social investment feed** | Sign up, post trades (ticker, shares, amount, date), and browse everyone's public positions newest-first. Search by ticker, delete your own posts, and flip your account private to drop out of the feed. |
| **AI agent trader** | A multi-agent pipeline pulls ~75 market headlines from Finnhub, embeds them into a local RAG vector store (`nomic-embed-text`), has `llama3` pick the strongest ticker, sizes the position within a $1,000 allowance, and prices the simulated fills with live quotes. |
| **Demo-grade auth** | Deliberately simple `localStorage` session (`postfolio.session`) with auto-login after signup and protected routes — no JWT/cookie machinery, by design (see [locked decisions](docs/open-questions.md)). |

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
  PostgreSQL           Finnhub API           Ollama (local)
  users + posts        news + quotes         llama3 + embeddings
                                                  │
                                                  ▼
                                          RAG vector store
```

### The agent pipeline, step by step

1. **Collect** — `dataCollection` fetches ~75 market headlines from Finnhub
2. **Embed** — each headline is embedded via Ollama `nomic-embed-text` into a vector store
3. **Retrieve** — a growth/sentiment query pulls the 25 most relevant headlines (RAG)
4. **Analyze** — `dataAnalyzerAgent` (`llama3`) names the single strongest ticker
5. **Size** — `costAnalysisAgent` converts an LLM dollar recommendation into whole shares
6. **Loop** — repeat until the $1,000 allowance is spent
7. **Execute** — `executeAgent` prices the basket with live Finnhub quotes and reports cost, total invested, and remaining allowance

Every dependency failure (missing Finnhub key, Finnhub down, Ollama offline) returns a
structured `503 + {"error": "..."}` that the UI surfaces verbatim.

## Feature checklist

- [x] Signup with validation — `201`, duplicate username/email → `409`, passwords BCrypt-hashed and never serialized
- [x] Login (`202` + username), auto-login after signup, session survives refresh
- [x] Protected routes: `/post/new`, `/agent`, `/account` redirect guests to login
- [x] Feed with loading / error / empty states, public-accounts-only filtering
- [x] Ticker search (uppercase-normalized) and two-step inline owner delete — no `alert()`
- [x] Create post with field validation; server computes price/share
- [x] Agent page with elapsed-seconds long-run UX for research + execution
- [x] Account privacy toggle that immediately hides/shows your posts in the feed
- [x] 404 route, custom design tokens (Fraunces + Instrument Sans, warm paper + market green)
- [x] 46 automated tests (35 backend MockMvc/unit · 11 frontend Vitest) and a two-job CI

## Quick start

**Prerequisites:** Java 21 · Node 20+ · local PostgreSQL · *(optional, for the agent)* [Ollama](https://ollama.com) + a [Finnhub](https://finnhub.io) API key.

```bash
# 1. Database (once)
createdb postfolio

# 2. Backend  →  http://localhost:8080
cd Backend/postfolio
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/postfolio
export SPRING_DATASOURCE_USERNAME=$USER
export SPRING_DATASOURCE_PASSWORD=
export FINNHUB_API_KEY=your_key        # optional — agent endpoints 503 without it
./mvnw spring-boot:run

# 3. Frontend  →  http://localhost:5173   (separate terminal)
cd Frontend
npm install
npm run dev
```

For the agent, also pull the models once:

```bash
ollama pull llama3
ollama pull nomic-embed-text
```

Full walkthrough, Docker Postgres option, and smoke-test curls: [docs/setup.md](docs/setup.md).

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
| `/trade/stock/test/` | GET | `200` + ticker → shares | `503` + `{error}` |
| `/trade/stock/execute/` | GET | `200` + fills + allowance | `503` + `{error}` |

Full contracts with request/response bodies: [docs/api.md](docs/api.md).

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
| [docs/architecture.md](docs/architecture.md) | Understanding how the pieces fit |
| [docs/api.md](docs/api.md) | Wiring any HTTP call |
| [docs/frontend.md](docs/frontend.md) | **Any UI work** — design tokens, rules, anti-patterns |
| [docs/setup.md](docs/setup.md) | Running locally / troubleshooting |
| [docs/plan.md](docs/plan.md) | Roadmap and what shipped |
| [docs/open-questions.md](docs/open-questions.md) | Locked decisions + open product questions |

Coding agents start at [`AGENTS.md`](AGENTS.md).

## Design notes

The UI follows a deliberate direction documented in [docs/frontend.md](docs/frontend.md):
warm paper background, ink text, a single deep market-green accent, **Fraunces** display
serif with **Instrument Sans** body type, and monospaced tickers. No purple gradient
SaaS clones, no `alert()` dialogs, no component-library dumps.

## Contributors

- **Mason Hart**
- **Tyler Mestery** — [@tmestery](https://github.com/tmestery)
