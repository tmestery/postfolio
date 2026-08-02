# System architecture

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
   PostgreSQL         Public web (RSS/HTML) + Yahoo chart JSON
   (local, required)  research crew + Price Scout
                              │
                              ▼
                        Groq (LLM debate + capital)
```

**Auth (demo):** Browser `localStorage` key `postfolio.session` — not JWT. See [frontend-ui-guide.md](./frontend-ui-guide.md) §5.

For API shapes see [rest-api-contract.md](./rest-api-contract.md). For UI rules see [frontend-ui-guide.md](./frontend-ui-guide.md).

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

README mentions “connections” — follow graph is **not built**. v1 assumption (until [product-decisions.md](./product-decisions.md) Q4): global feed of public posts.

### 2. LLM agent trader

A multi-agent pipeline (package `stockInvestmentAgents`) — see [agent-trader-v3.md](./agent-trader-v3.md):

1. **Research crew** — Source Planner → Web Scout → Evidence Packer (allowlisted public RSS/HTML)
2. **Ticker debate** — Bull → Bear → Stock Judge (Groq)
3. **Price Scout** — Yahoo chart JSON quotes for advanced tickers
4. **Capital committee** — allocators → Cash Guard → Capital Judge → Risk Gate
5. **Execute** — simulated fills within ~$1000 allowance

Weekly scheduling is described in marketing copy but **not implemented** (manual `GET` only).

---

## Backend map (`Backend/postfolio/src/main/java/.../`)

| Package / area | Role | Maturity |
|----------------|------|----------|
| `controllers/loginSignup` | Signup + login | Works; login returns plain string; FE stores localStorage session |
| `controllers/account` | Public/private toggle | Fragile (package / wiring issues historically) |
| `controllers/post` | CRUD-ish posts + feed + search | Needs **username demo bridge** for create without principal |
| `controllers/longTermStockController` | Agent HTTP entry | Works locally with `GROQ_API_KEY` |
| `models/user`, `models/post` | JPA entities | Core fields exist |
| `security/SecurityConfig` | BCrypt, CORS, authorizeHttpRequests | Over-permissive; fine for localStorage demo |
| `stockInvestmentAgents/**` | Groq debate + web research + capital | Functional locally; see agent-trader-v3 |
| `services/`, `utils/` | Empty placeholders | — |

### Config

`src/main/resources/application.properties` is currently minimal. **Locked:** configure **PostgreSQL** for local (not H2). Datasource URL/user/password via env — see [local-development.md](./local-development.md).

### CI

`.github/workflows/maven.yml` builds the backend. Align JDK with `pom.xml` (Java 21) — called out in [implementation-roadmap.md](./implementation-roadmap.md) Phase 0.3.

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

Detailed FE rules: [frontend-ui-guide.md](./frontend-ui-guide.md).

---

## Trust boundaries (today vs target)

| Concern | Today | Target |
|---------|-------|--------|
| Who can POST a trade | Principal often null; use username bridge | Username from localStorage session (demo) |
| Who can delete | Unclear binding; no owner check | Owner check via username for demo |
| Agent cost | Groq + scrape budget / wall-clock timeout | Rate limit later; manual trigger for demo |
| Secrets | Env for Groq/Postgres; never in FE | Same; document in `.env.example` |
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
| [frontend-ui-guide.md](./frontend-ui-guide.md) | Any UI change |
| [rest-api-contract.md](./rest-api-contract.md) | Wiring fetch / contracts |
| [implementation-roadmap.md](./implementation-roadmap.md) | What to build next |
| [local-development.md](./local-development.md) | Running locally |
| [product-decisions.md](./product-decisions.md) | Unresolved product/design choices |
