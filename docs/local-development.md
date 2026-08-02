# Local development

## Fastest path (Docker Compose)

```bash
cp .env.example .env
# put GROQ_API_KEY and FINNHUB_API_KEY in .env
make up          # or: docker compose up --build
```

- Frontend: http://localhost:5173  
- Backend:  http://localhost:8080  
- Postgres: localhost:5432 (`postfolio` / `postfolio` / `postfolio`)

Postgres only (run Spring + Vite on the host):

```bash
make env && make postgres
make backend    # separate terminal
make frontend   # separate terminal
```

See root `Makefile` and `.env.example`.

---

## Prerequisites

| Tool | Version / notes |
|------|-----------------|
| Node.js | 20+ recommended |
| npm | Comes with Node |
| Java | **21** (matches `pom.xml`) |
| Maven | Use `./mvnw` wrapper in `Backend/postfolio` |
| **PostgreSQL** | **Required** (local or Docker). Do **not** use H2 at runtime. |
| Docker | Optional; easiest way to run Postgres / full stack |
| Groq account | API key for agent LLM calls (`GROQ_API_KEY`) |
| Finnhub account | API key for news/quotes (`FINNHUB_API_KEY`) |

Ollama is **not** required for the v2 agent path (replaced by Groq).

---

## Environment file (API keys)

Put secrets in the **repo root** `.env` (gitignored):

```bash
cp .env.example .env
# edit GROQ_API_KEY=... and FINNHUB_API_KEY=...
make env   # also writes Frontend/.env with VITE_* vars
```

The backend loads root `.env` automatically on startup (`DotenvBootstrap`). Docker Compose reads the same file via `env_file`. Never commit `.env`.

---

## PostgreSQL (local)

Create a database and user (example):

```bash
# Option A: local Homebrew Postgres
createdb postfolio
# or
psql -c "CREATE DATABASE postfolio;"
psql -c "CREATE USER postfolio WITH PASSWORD 'postfolio';"
psql -c "GRANT ALL PRIVILEGES ON DATABASE postfolio TO postfolio;"

# Option B: Docker
docker run --name postfolio-pg -e POSTGRES_USER=postfolio -e POSTGRES_PASSWORD=postfolio -e POSTGRES_DB=postfolio -p 5432:5432 -d postgres:16
```

Spring should use env (exact property names to be wired in Phase 0.0):

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/postfolio
export SPRING_DATASOURCE_USERNAME=postfolio
export SPRING_DATASOURCE_PASSWORD=postfolio
```

Or document equivalent keys in `application.properties` / `application-local.properties` once implemented. Prefer `spring.jpa.hibernate.ddl-auto=update` for local demo.

**Verify:**

```bash
psql -d postfolio -c '\dt'
```

After first signup you should see user/post tables.

---

## Backend

```bash
cd Backend/postfolio
export FINNHUB_API_KEY=your_key_here
export GROQ_API_KEY=your_groq_key_here
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/postfolio
export SPRING_DATASOURCE_USERNAME=postfolio
export SPRING_DATASOURCE_PASSWORD=postfolio
# optional overrides: GROQ_MODEL_FAST, GROQ_MODEL_JUDGE, AGENT_ALLOWANCE, AGENT_CASH_RESERVE_PCT
./mvnw spring-boot:run
```

- Default URL: `http://localhost:8080`
- CORS allows `http://localhost:5173` only (see `SecurityConfig`).
- App **must** reach Postgres on boot — if connection refused, fix DB before debugging FE.

### Smoke checks

```bash
# signup (camelCase; no DOB)
curl -s -X POST http://localhost:8080/credentials/signup/ \
  -H 'Content-Type: application/json' \
  -d '{"username":"demo","email":"demo@example.com","password":"password123","firstName":"Demo","lastName":"User","accountPublicStatus":true}'

# login (body is plain username string on 202)
curl -s -i -X POST http://localhost:8080/credentials/login/ \
  -H 'Content-Type: application/json' \
  -d '{"username":"demo","password":"password123"}'

# feed
curl -s http://localhost:8080/post/feed/

# agent (needs Groq + Finnhub)
curl -s http://localhost:8080/trade/stock/test/
```

More contract detail: [rest-api-contract.md](./rest-api-contract.md).

---

## Frontend

```bash
cd Frontend
cp .env.example .env   # create when file exists; see below
npm install
npm run dev
```

- App: `http://localhost:5173`
- Scripts: `dev`, `build`, `lint`, `preview`

### Env (Vite)

```
VITE_SERVER_URL=http://localhost:8080
VITE_CLIENT_URL=http://localhost:5173
```

Code must read `import.meta.env.VITE_*` — not `process.env` (see [frontend-ui-guide.md](./frontend-ui-guide.md) §4).

### Demo auth

After login/signup, FE stores `localStorage['postfolio.session']` = `{"username":"...","id":...}`. See [frontend-ui-guide.md](./frontend-ui-guide.md) §5. This is intentional for demos — not production security.

### Lint & tests

```bash
cd Frontend
npm run lint
npm test        # Vitest (jsdom)
```

Follow [lint-anti-patterns.md](./lint-anti-patterns.md).

---

## Agent skills (coding agents)

| Path | Purpose |
|------|---------|
| `AGENTS.md` | Always-on project instructions |
| `skills/` | Portable Agent Skills |
| `.cursor/skills/` | Cursor copy of the same |

Before UI work → **`docs/frontend-ui-guide.md`**. Locked vs open decisions → **`docs/product-decisions.md`**. Build order → **`docs/implementation-roadmap.md`**.

---

## Common failures

| Symptom | Likely cause |
|---------|----------------|
| Backend won’t start / datasource error | Postgres not running or wrong URL/user/password |
| Browser CORS error | FE not on `http://localhost:5173` |
| Signup “works” but user incomplete | Snake_case JSON fields (use camelCase) |
| Login FE can’t parse JSON | Response is **plain text** username |
| Create post 500 / null user | Need username demo bridge; principal is null |
| Agent 503 | `GROQ_API_KEY` / `FINNHUB_API_KEY` unset, or provider down |
| Tailwind classes do nothing | Invalid util (`w-200`, `text-large`) |

---

## Verify checklist

1. Postgres accepts connections; DB `postfolio` exists.  
2. Backend starts against Postgres with no stacktrace loop.  
3. Frontend loads `/`, `/login`, `/signup`.  
4. Signup + login succeed via curl; row in Postgres.  
5. (After FE) Signup auto-logs in; `postfolio.session` set.  
6. (Optional) Agent test returns a JSON map.

---

## Production note

No deploy target is documented yet. Local Postgres is the development standard. When adding deploy, update this file, CORS origins, and do **not** assume H2.
