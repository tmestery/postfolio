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
| Dates | `LocalDate` → `yyyy-MM-DD`; `LocalDateTime` → ISO-8601 |
| Password | Never send hash back to UI for display; signup response may include encoded password — strip in UI |
| Errors | Inconsistent today (string bodies, empty 204, exceptions). Normalize later; FE must handle status + text |

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

- Password is BCrypt-hashed server-side before save.
- No uniqueness check documented — duplicate username/email may 500 or violate DB constraints. Frontend should show a generic failure until backend returns clean 409s.
- Field names are **camelCase**. Snake_case will silently leave Java fields null.

**Success:** `200` (default) + saved `WebUser` JSON.

**Frontend:** On success, navigate to login or auto-login (product decision).

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

### `POST /account/status/` (intended)

Toggle public/private. Controller file lives under `controllers/account/` but has historically had **package declaration bugs** and missing wiring — verify before depending on it.

Expected concept:

- Input: user identity + boolean `accountPublicStatus`
- Effect: private accounts’ posts hidden from public feed (feed filtering **not fully implemented** yet)

Treat as **unstable** until Phase 2.5.

---

## Posts — `/post`

### `GET /post/feed/`

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

`username` is required for the **demo** while there is no server session. Backend should: if `@AuthenticationPrincipal` is null, resolve `WebUser` by `username` and attach to the post. Reject with `400` if username missing/unknown.

**Response:** `201` + `Post`.

**Frontend:** Send `username` from `postfolio.session`. Do not ship the hardcoded `POST /post/stock/test/` (user id `1`) as the product path.

### `POST /post/stock/test/`

Same body as create without needing username; forces user id `1`. **Dev only** — prefer username bridge for demos with real signups.

### `POST /post/stock/search/?stockName=AAPL`

**Note:** Stock name is a **query param**, not JSON body (`@RequestParam`).

**Success:** `200` + `Post[]`  
**No matches:** `204 No Content` (frontend must not call `.json()` on empty 204).

### `POST /post/delete/`

Intended to delete by `postId`. Current signature is ambiguous (`deletePost(Long postId)` without `@RequestParam` / `@RequestBody`) — **verify/fix before building UI**. Owner checks not implemented.

---

## Agent trades — `/trade`

Long-running. Expect timeouts; set a long client timeout or show “still working” after ~5s.

### `GET /trade/stock/test/`

Runs LLM agent pipeline (`manager.deployAgents()`).

**Response:** `200` + map:

```json
{
  "AAPL": 2.0,
  "MSFT": 1.5
}
```

Values = share counts (doubles). Requires Finnhub + Ollama.

### `GET /trade/stock/execute/`

Re-runs decisions then prices them (`executeAgent`) with allowance `1000`.

**Response:** `200` + `Map<String, Object>` including trade details and allowance fields (shape defined in `executeAgent` — treat as loosely typed; render keys defensively).

**Frontend:** Always defensive render (`Object.entries`, check typeof). Never assume nested schema until we freeze a DTO.

---

## Status code cheat sheet

| Endpoint | Success | Failure (today) |
|----------|---------|-----------------|
| signup | 200 + user | often 500 on constraint |
| login | 202 + username string | 400 + `"Failed!"` |
| feed | 200 + array | 500 |
| create post | 201 + post | 500 / null user |
| search | 200 or **204** | 500 |
| trade test/execute | 200 + map | 500 if Ollama/Finnhub down |

---

## Required backend follow-ups (demo path)

1. **Postgres** datasource configured for local (locked — not H2).
2. **Username demo bridge** on create-post when principal is null.
3. Signup: prefer `409` on duplicate username/email (nice-to-have).
4. Search: prefer `200 + []` over `204` for easier SPA handling (or keep 204 and handle in FE).
5. Delete: explicit `@RequestParam Long postId` + optional username owner check for demo.
6. Account status: fix package + return clear JSON.
7. Agent: stable response DTO + clear error when Ollama/Finnhub unavailable.

**Out of scope for this demo:** JWT, Spring session cookies, real security hardening — unless requested later.
