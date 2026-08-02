# API reference (frontend ↔ backend)

Living contract for the Spring Boot API as it exists today, plus notes on what must change. Frontend agents should treat this as source of truth over guessing from controller comments.

Base URL (local): `http://localhost:8080`  
CORS origin allowed: `http://localhost:5173` (`Allow-Credentials: true`)  
CSRF: disabled  
Authn on `/credentials/**`, `/post/**`, `/trade/**`: **permitAll** (not actually protected yet)

---

## Conventions

| Topic | Behavior |
|-------|----------|
| Content type | `application/json` on JSON bodies |
| Trailing slashes | Controllers use trailing `/` — match them |
| Dates | ISO strings: `LocalDate` → `"2026-07-15"`; `LocalDateTime` → `"2026-08-01T22:20:15"` |
| Password | Write-only on `WebUser` — never serialized in any response |
| Errors | Validation/conflict errors return JSON `{"error": "message"}` with 400/403/404/409 |

---

## Auth — `/credentials`

### `POST /credentials/signup/`

**Request body (`WebUser`):**

```json
{
  "username": "string",
  "email": "string",
  "password": "string",
  "firstName": "string",
  "lastName": "string",
  "dateOfBirth": "2000-01-15",
  "accountPublicStatus": true
}
```

**Notes:**

- Password is BCrypt-hashed server-side and **never returned** (write-only).
- `accountPublicStatus` defaults to `true` when omitted.
- Field names are **camelCase**. Snake_case will silently leave Java fields null.

**Responses:**

| Status | Meaning |
|--------|---------|
| `201` | Created — body is the saved `WebUser` (id, username, email, names; no password) |
| `400` | Missing username/email/password — `{"error": "..."}` |
| `409` | Duplicate username or email — `{"error": "..."}` |

**Frontend:** On success, auto-login (write `postfolio.session` with `id` from response) → `/`.

### `POST /credentials/login/`

**Request:**

```json
{
  "username": "string",
  "password": "string"
}
```

Email in body is ignored by current controller.

**Success:** `202 Accepted` + **raw string** body = username (not JSON object).

**Failure:** `400` + `"Failed!"`.

**Frontend parsing trap:**

```js
const text = await response.text()
// success: text === username
// do not response.json() unless Content-Type is JSON
```

**Gap (demo):** No session cookie, no JWT. SPA uses **localStorage** (`postfolio.session`) — see [open-questions.md](./open-questions.md) / [frontend.md](./frontend.md). Create-post’s `@AuthenticationPrincipal` will not populate; use the **username demo bridge** below.

---

## Account — `/account`

### `GET /account/status/?username=demo`

**Success:** `200` + `{"username": "demo", "accountPublicStatus": true}`  
**Unknown user:** `404` + `{"error": "..."}`

### `POST /account/status/`

**Request:**

```json
{ "username": "demo", "accountPublic": false }
```

**Responses:** `200` + updated status body · `400` missing fields · `404` unknown user.

**Effect:** Private accounts’ posts are excluded from `GET /post/feed/`.

---

## Posts — `/post`

### `GET /post/feed/`

Newest-first posts from **public accounts only**.

**Response:** `200` + `Post[]`

```json
[
  {
    "id": 1,
    "dateInvested": "2025-01-10",
    "datePosted": "2025-01-11T12:00:00",
    "stock": "AAPL",
    "shares": 10.0,
    "pricePerShare": 180.5,
    "investedAmount": 1805.0,
    "user": {
      "id": 1,
      "username": "tyler",
      "accountPublicStatus": true
    }
  }
]
```

`user` omits `password`, `dateOfBirth`, `firstName`, `lastName`, `email` via `@JsonIgnoreProperties`.

**Frontend:** Empty array → empty state. Network error → retry UI.

### `POST /post/stock/`

**Request (`StockPostRequest` + demo bridge):**

```json
{
  "dateInvested": "2025-01-10",
  "stock": "AAPL",
  "shares": 10,
  "investedAmount": 1805,
  "username": "tyler"
}
```

`username` is required while there is no server session (demo bridge): when `@AuthenticationPrincipal` is null, the backend resolves `WebUser` by `username`.

Ticker is normalized to uppercase and **must exist** in the `stock_symbol` table (seeded on empty DB; reload via `scripts/seed-stock-symbols.sql`). `pricePerShare` is computed as `investedAmount / shares`.

**Responses:**

| Status | Meaning |
|--------|---------|
| `201` | Created — body is the `Post` |
| `400` | Missing/unknown username, missing/unknown ticker, `shares <= 0`, `investedAmount <= 0` — `{"error": "..."}` |

**Frontend:** Send `username` from `postfolio.session`. Use `GET /post/symbols/` for typeahead.

### `GET /post/symbols/?q=AA&limit=20`

Prefix search over allowed tickers. Blank `q` → `[]`.

**Response:** `200` + `[{ "symbol": "AAPL", "name": "Apple Inc." }, ...]`

### `POST /post/stock/search/?stockName=AAPL`

**Note:** Stock name is a **query param**, not JSON body (`@RequestParam`). Matching is uppercase-normalized.

**Response:** always `200` + `Post[]` (empty list when no matches — no more 204).

### `POST /post/delete/?postId=1&username=demo`

Both query params required. Only the owner can delete.

| Status | Meaning |
|--------|---------|
| `204` | Deleted |
| `400` | Missing params |
| `403` | Requester is not the post owner |
| `404` | Post not found |

---

## Agent trades — `/trade`

Long-running. Expect timeouts; set a long client timeout or show “still working” after ~5s.

### `GET /trade/stock/test/`

Deep multi-agent research run (paper book through Risk Gate, **no fills**). Optional `?username=` for demo attribution.

**Response:** `200` + `RunResult` DTO — see [agent-trader-v2.md](./agent-trader-v2.md) §8. Key fields: `runId`, `status`, `candidates`, `allocatorProposals`, `capitalJudgeDecision`, `plannedShares`, `agentTrace`.

Requires `GROQ_API_KEY` + `FINNHUB_API_KEY`.

**Failure:** `503` + `{"error": "..."}` when Groq/Finnhub key missing or provider down.

### `GET /trade/stock/execute/`

Same pipeline **including** simulated fills. Repeatable — no one-shot lockout.

**Response:** `200` + full `RunResult` with `executedTrades` (ticker → `{shares, price, cost}`), `totalInvested`, `remainingAllowance`, and `agentTrace`.

**Failure:** `503` + `{"error": "..."}` (same dependency checks as research).

### `GET /trade/runs/`

Recent run summaries (no full `result_json` payload).

### `GET /trade/runs/{runId}/`

Full persisted `RunResult` for the agent desk history drill-in.

| Status | Meaning |
|--------|---------|
| `200` | Parsed `RunResult` from `result_json` |
| `400` | Invalid UUID |
| `404` | Unknown run or empty `result_json` |

**Frontend:** Always defensive render (`Object.entries`, check typeof). Treat `status: "partial"` as a successful response with a warning. Expand `agentTrace[].detail` on the desk.

---

## Status code cheat sheet

| Endpoint | Success | Failure |
|----------|---------|---------|
| signup | 201 + user | 400 missing fields · 409 duplicate |
| login | 202 + username string | 400 + `"Failed!"` |
| feed | 200 + array (public only) | 500 |
| create post | 201 + post | 400 validation/unknown user |
| search | 200 + array (may be empty) | 500 |
| delete | 204 | 400 / 403 / 404 |
| account status | 200 + status JSON | 400 / 404 |
| trade test/execute | 200 + `RunResult` | 503 + `{error}` if Groq/Finnhub down or key missing |
| trade runs | 200 + summary array | 500 |

---

## Remaining backend follow-ups

None on the demo path.

**Done:** Postgres datasource · username demo bridge · 409 duplicates · search `200 + []` · delete params + owner check · account status GET/POST · password write-only · ISO dates · agent 503s with clear dependency errors.

**Out of scope for this demo:** JWT, Spring session cookies, real security hardening — unless requested later.
