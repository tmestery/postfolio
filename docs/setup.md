# Local setup

## Prerequisites

| Tool | Version / notes |
|------|-----------------|
| Node.js | 20+ recommended |
| npm | Comes with Node |
| Java | **21** (matches `pom.xml`) |
| Maven | Use `./mvnw` wrapper in `Backend/postfolio` |
| **PostgreSQL** | **Required** (local). Do **not** use H2. |
| Ollama | Optional; required for agent routes |
| Finnhub account | API key for news/quotes |

Ollama models used by code today:

```bash
ollama pull llama3
ollama pull nomic-embed-text
```

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
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/postfolio
export SPRING_DATASOURCE_USERNAME=postfolio
export SPRING_DATASOURCE_PASSWORD=postfolio
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

# agent (slow; needs Ollama + Finnhub)
curl -s http://localhost:8080/trade/stock/test/
```

More contract detail: [api.md](./api.md).

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

Code must read `import.meta.env.VITE_*` — not `process.env` (see [frontend.md](./frontend.md) §4).

### Demo auth

After login/signup, FE stores `localStorage['postfolio.session']` = `{"username":"...","id":...}`. See [frontend.md](./frontend.md) §5. This is intentional for demos — not production security.

### Lint & tests

```bash
cd Frontend
npm run lint
npm test        # Vitest (jsdom)
```

Follow [anti-linter-patterns.md](./anti-linter-patterns.md).

---

## Agent skills (coding agents)

| Path | Purpose |
|------|---------|
| `AGENTS.md` | Always-on project instructions |
| `skills/` | Portable Agent Skills |
| `.cursor/skills/` | Cursor copy of the same |

Before UI work → **`docs/frontend.md`**. Locked vs open decisions → **`docs/open-questions.md`**. Build order → **`docs/plan.md`**.

---

## Common failures

| Symptom | Likely cause |
|---------|----------------|
| Backend won’t start / datasource error | Postgres not running or wrong URL/user/password |
| Browser CORS error | FE not on `http://localhost:5173` |
| Signup “works” but user incomplete | Snake_case JSON fields (use camelCase) |
| Login FE can’t parse JSON | Response is **plain text** username |
| Create post 500 / null user | Need username demo bridge; principal is null |
| Agent hangs / 500 | Ollama not running, models missing, or Finnhub key unset |
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
