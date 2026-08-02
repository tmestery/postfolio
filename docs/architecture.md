# Architecture

Postfolio is a **browser web app** (not a native mobile app): a React SPA talks to a Spring Boot REST API over JSON.

```
┌─────────────────────────────────────────────────────────────┐
│  Browser                                                    │
│  React 19 + Vite + Tailwind                                 │
│  Routes: /, /login, /signup, /post/new, /agent, /account… │
└────────────────────────────┬────────────────────────────────┘
                             │ HTTP JSON
                             │ CORS: localhost:5173 → :8080
┌────────────────────────────▼────────────────────────────────┐
│  Spring Boot 4 (Java 21)                                    │
│  Controllers → Services → JPA Repositories                  │
│  Spring Security (BCrypt; route permits currently loose)    │
└───────┬───────────────────┬───────────────────┬─────────────┘
        │                   │                   │
        ▼                   ▼                   ▼
   PostgreSQL         Finnhub API         Ollama (local)
   (local, required)  news + quotes       llama3 + embeddings
                                              │
                                              ▼
                                        RAG vector store
                                        (JSON file today)
```

**Auth (demo):** Browser `localStorage` key `postfolio.session` — not JWT. See [frontend.md](./frontend.md) §5.

For API shapes see [api.md](./api.md). For UI rules see [frontend.md](./frontend.md).

---

## Product pillars

### 1. Social investment tracking

Users create accounts, optionally mark profiles public/private, and publish **investment posts**:

| Field | Meaning |
|-------|---------|
| `stock` | Ticker symbol |
| `shares` | Quantity |
| `investedAmount` | Dollars put in |
| `pricePerShare` | Derived/stored price |
| `dateInvested` | When they bought |
| `datePosted` | When they shared |

Others browse a **feed**, search by ticker, and (eventually) only owners delete their posts.

README mentions “connections” — follow graph is **not built**. v1 assumption (until [open-questions.md](./open-questions.md) Q4): global feed of public posts.

### 2. LLM agent trader

A multi-agent pipeline (package `stockInvestmentAgents`):

1. **Data collection** — Finnhub market headlines (~75)
2. **RAG** — embed with Ollama `nomic-embed-text`, store/retrieve via `VectorStore`
3. **Analysis** — `dataAnalyzerAgent` (`llama3`) picks a ticker from retrieved context
4. **Sizing** — `costAnalysisAgent` chooses share count within ~$1000 allowance
5. **Loop** — until allowance exhausted
6. **Execute** — `executeAgent` attaches live quotes / simulated fill details

Decision agent stubs (`safeDecisionAgent`, `riskyDecisionAgent`) and Reuters `DataScraper` are incomplete / unused on the happy path. Weekly scheduling is described in marketing copy but **not implemented** (manual `GET` only).

---

## Backend map (`Backend/postfolio/src/main/java/.../`)

| Package / area | Role | Maturity |
|----------------|------|----------|
| `controllers/loginSignup` | Signup + login | Works; login returns plain string; FE stores localStorage session |
| `controllers/account` | Public/private toggle | Fragile (package / wiring issues historically) |
| `controllers/post` | CRUD-ish posts + feed + search | Needs **username demo bridge** for create without principal |
| `controllers/longTermStockController` | Agent HTTP entry | Works locally with keys + Ollama |
| `models/user`, `models/post` | JPA entities | Core fields exist |
| `security/SecurityConfig` | BCrypt, CORS, authorizeHttpRequests | Over-permissive; fine for localStorage demo |
| `stockInvestmentAgents/**` | LLM + RAG pipeline | Functional locally; brittle error handling |
| `services/`, `utils/` | Empty placeholders | — |

### Config

`src/main/resources/application.properties` is currently minimal. **Locked:** configure **PostgreSQL** for local (not H2). Datasource URL/user/password via env — see [setup.md](./setup.md).

### CI

`.github/workflows/maven.yml` builds the backend. Align JDK with `pom.xml` (Java 21) — called out in [plan.md](./plan.md) Phase 0.3.

---

## Frontend map (`Frontend/src/`)

| Path | Role | Maturity |
|------|------|----------|
| `pages/home` | Feed / landing | Placeholder |
| `pages/login` | Auth | Stub / wrong copy |
| `pages/signup` | Registration | Wrong URL + wrong endpoint + snake_case body |
| `pages/notfound` | 404 | Exists; not routed |
| `api/constants.js` | Base URLs | Uses `process.env` (broken under Vite) |
| `routes.jsx` | Router | Partial |
| `index.css` | Tailwind entry | No design tokens yet |

Detailed FE rules: [frontend.md](./frontend.md).

---

## Trust boundaries (today vs target)

| Concern | Today | Target |
|---------|-------|--------|
| Who can POST a trade | Principal often null; use username bridge | Username from localStorage session (demo) |
| Who can delete | Unclear binding; no owner check | Owner check via username for demo |
| Agent cost | Unbounded local Ollama + Finnhub quota | Rate limit later; manual trigger for demo |
| Secrets | Env for Finnhub/Postgres; never in FE | Same; document in `.env.example` |
| Private profiles | Field exists | Feed + profile honor it |
| Session | None on server | `postfolio.session` in localStorage |

---

## Explicit non-goals (current horizon)

- Native iOS / Android apps  
- Real brokerage order routing  
- GraphQL (dependency present, unused)  
- Full social graph (likes, comments, DMs) before feed + auth work  

---

## Doc index

| Doc | Use when |
|-----|----------|
| [frontend.md](./frontend.md) | Any UI change |
| [api.md](./api.md) | Wiring fetch / contracts |
| [plan.md](./plan.md) | What to build next |
| [setup.md](./setup.md) | Running locally |
| [open-questions.md](./open-questions.md) | Unresolved product/design choices |
