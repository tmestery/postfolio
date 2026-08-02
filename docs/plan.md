# Plan — what needs to be done

**Goal:** Ship a working social investment **web** app: auth, post trades, browse a feed, view LLM agent simulated trades.

**Constraints:** Backend APIs mostly exist; frontend is scaffolding with known bugs; agent needs Ollama + Finnhub; posts not reliably scoped to auth users; no weekly scheduler. Some design questions still open in [open-questions.md](./open-questions.md).

**Locked for demo:** localStorage auth · Postgres local · signup field set · ranked order below. See open-questions “Locked decisions.”

**How to work:** Vertical slices. Each slice demoable. Tests: **1 positive + 3 negative** per behavior (tmestz-skills). Read [frontend.md](./frontend.md) before UI PRs. Stop for approval on large phases unless told to proceed.

---

## Ranked order (execute in this sequence)

1. ✅ Postgres local + Spring datasource  
2. ✅ Signup/login FE fixes + API client + `VITE_*` + 404  
3. ✅ AuthContext + `postfolio.session` localStorage (+ auto-login after signup)  
4. ✅ Demo post attribution (API resolves user by `username` when no principal)  
5. ✅ Feed UI  
6. ✅ Create post UI  
7. ✅ Agent UI  
8. ✅ Search / delete / account privacy  
9. ✅ CI (JDK 21 + frontend lint/test/build job)  

**Remaining:** Phase 3.1 agent hardening (structured errors when Ollama/Finnhub down) and Phase 4 polish as needed.

---

## Phase 0 — Stabilize foundations

### Task 0.0: Postgres from the start (Rank 1)
- **Goal:** App boots against **local PostgreSQL** (no H2).
- **Likely files:** `application.properties` (or `application-local.properties`), `.env.example` / setup docs, `.gitignore`
- **Done when:**
  - Datasource URL/user/password via env or documented local defaults
  - `ddl-auto` sensible for demo (`update` OK for local)
  - Entities create/read against Postgres
  - [setup.md](./setup.md) has `createdb` / Docker one-liner
- **Tests:** App context loads with Postgres; signup row visible in DB; boot fails clearly if Postgres down
- **Risks:** CI needs Postgres service or testcontainers later

### Task 0.1: Fix known auth / wiring bugs (Rank 2)
- **Goal:** Login and signup work end-to-end against the API with correct contracts.
- **Likely files:** `Frontend/src/pages/signup/`, `login/`, `api/constants.js`, `routes.jsx`; account controller package if broken.
- **Done when:**
  - Signup → `POST ${SERVER_URL}/credentials/signup/` with **camelCase** locked fields (no DOB)
  - Login → `POST .../credentials/login/`; handles **202 plain-text** username
  - Login heading/copy correct; inputs work
  - Catch-all 404 route registered
  - `VITE_SERVER_URL` via `import.meta.env`
- **Tests:** 1 signup success; negatives: bad password login, missing fields, malformed email
- **FE checklist:** [frontend.md](./frontend.md) §§4–5, §11

### Task 0.2: Config and secrets hygiene
- **Goal:** Env examples for Postgres, Finnhub, Ollama; no secrets in git.
- **Likely files:** Spring config, `Frontend/.env.example`, `.gitignore`, [setup.md](./setup.md)
- **Done when:** Examples exist; app starts without Finnhub (agent routes fail clearly); `.env` ignored
- **Tests:** Boot without Finnhub key; agent endpoint actionable error; Vite env fallback

### Task 0.3: Align CI with Java 21 (Rank 9 — defer until demo path works)
- **Goal:** Maven workflow JDK matches `pom.xml`; Postgres available in CI when tests need DB.
- **Files:** `.github/workflows/maven.yml`
- **Done when:** CI green on JDK 21
- **Risks:** Low if deferred

---

## Phase 1 — Auth + localStorage session (Ranks 2–3)

### Task 1.1: Shared API client
- **Goal:** One fetch helper: base URL, JSON/text parsing, error object.
- **Files:** `Frontend/src/api/`
- **Done when:** Login/signup use it; forms show inline errors (no `alert`)
- **Tests:** success parse; network fail; 4xx body; non-JSON 202 text login
- **See:** [api.md](./api.md), [frontend.md](./frontend.md) §4

### Task 1.2: localStorage session (LOCKED — not JWT)
- **Goal:** `postfolio.session`; AuthContext; auto-login after signup; protect `/post/new`, `/agent`, `/account`.
- **Files:** `Frontend/src/context/` (or `auth/`), login/signup pages, routes
- **Done when:** Refresh keeps user; logout clears key; guests redirected from protected routes
- **Tests:** login sets storage; signup auto-login; logout clears; missing/corrupt JSON → logged out
- **Explicit non-goal:** JWT, cookies, Spring Security principal population

---

## Phase 2 — Core social product

### Task 2.1: Create investment post UI
- **Goal:** Logged-in user (localStorage) submits ticker, shares, amount, date invested + `username` bridge.
- **Route:** `/post/new`
- **API:** `POST /post/stock/` with username from session
- **Done when:** Validation + success → feed refetch
- **Tests:** create success; missing ticker; non-numeric shares; missing username rejected
- **Blocked by:** Task 1.2 + Task 2.4 demo bridge

### Task 2.2: Feed page
- **Goal:** Home shows posts from `GET /post/feed/` with empty/error states.
- **Design:** Follow [frontend.md](./frontend.md) §6–7 — not a widget dashboard
- **Working assumptions:** global public feed (Q4); soft-product visuals until Q7 answered
- **Tests:** feed with data; empty; API error; malformed item safe

### Task 2.3: Search by ticker + delete
- **Goal:** Search (`POST /post/stock/search/?stockName=`) handles **204**; delete with username/owner check for demo
- **Tests:** hit; no matches (204); invalid input; delete unauthorized
- **Risks:** Delete endpoint parameter binding must be fixed first ([api.md](./api.md))

### Task 2.4: Demo post attribution (backend) — Rank 4
- **Goal:** Create post works **without** JWT: if principal null, resolve user by request `username`.
- **Done when:** Post stored with correct `WebUser`; unknown/missing username → 400
- **Tests:** create with valid username; unknown username; missing username
- **Non-goal:** Real Spring Security authentication

### Task 2.5: Account privacy toggle UI
- **Goal:** `/account` toggles `accountPublicStatus`
- **Done when:** Persists and affects feed visibility
- **Tests:** toggle ok; unauthenticated; invalid value; private hidden

---

## Phase 3 — LLM agent trader (productize)

### Task 3.1: Harden agent pipeline
- **Goal:** Structured errors if Ollama/Finnhub down; stubs don’t crash happy path
- **Done when:** `/trade/stock/test/` and `/execute/` return JSON or clear 503-style errors
- **Tests:** mock success; missing key; Ollama down; empty news

### Task 3.2: Agent trades UI
- **Route:** `/agent` (authed via localStorage)
- **Goal:** Trigger + display results; **mandatory long-loading UX** (tens of seconds)
- **Working assumption:** shared house agent (Q5)
- **Tests:** mock render; empty; error; timeout messaging

### Task 3.3: Weekly schedule
- **Goal:** Optional; **defer for demo** — manual trigger is enough (Q12 assumption)
- **Risks:** Finnhub rate limits; never enable unbounded in CI

---

## Phase 4 — Hardening (after demo)

### Task 4.1: Tighten Spring Security
- **Defer** unless requested. Demo uses localStorage, not JWT/cookies.

### Task 4.2: Frontend polish
- Responsive web at 375 / 768 / 1280
- Tokens from Q7 applied in `index.css` `@theme` when answered
- Motion per Q8 assumption

### Task 4.3: Meaningful test suite
- Backend service tests; FE util/API client tests; CI with Postgres
- FE test runner choice TBD (Vitest recommended when we add it)

---

## Suggested order

```
0.0 Postgres
  ↓
0.1 + 1.1 auth FE + API client
  ↓
1.2 localStorage AuthContext
  ↓
2.4 username bridge
  ↓
2.2 feed → 2.1 create post
  ↓
3.2 agent UI (3.1 as needed)
  ↓
2.3 / 2.5 extras → 0.3 CI → 4.x polish
```

## Out of scope (for now)

- Native mobile apps  
- GraphQL usage  
- Follows/comments/likes until feed + auth solid  
- Real brokerage execution  
- JWT / Spring session (demo uses localStorage)  

## Definition of “v1 demoable”

1. Postgres running; signup persists.  
2. User can sign up / log in; session survives refresh via localStorage.  
3. User can create a post (username bridge) and see it on the feed.  
4. Feed empty/error states don’t look broken.  
5. Agent page can show a mocked or live result with loading state.  
6. UI follows frontend.md (no purple SaaS clone, no alert hell).
