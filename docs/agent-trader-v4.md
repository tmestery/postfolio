# Agent Trader v4 — Paper Portfolio P&L + Alpaca Marks (Basic plan)

**Status:** In progress (portfolio book + Alpaca REST marks).  
**Audience:** Coding agents + humans adding lasting gain/loss tracking for the simulated house trader, marked via **Alpaca Market Data** on the **Basic (free) plan**.  
**Depends on:** [agent-trader-v2.md](./agent-trader-v2.md) (fills + allowance) · [agent-trader-v3.md](./agent-trader-v3.md) (research crew; Yahoo quotes at execute time).  
**Related:** [system-architecture.md](./system-architecture.md) · [rest-api-contract.md](./rest-api-contract.md) · [frontend-ui-guide.md](./frontend-ui-guide.md) · [social-network-design.md](./social-network-design.md) · [product-decisions.md](./product-decisions.md) Q5/Q6

> **No real money.** Postfolio keeps a **simulated** house book in Postgres. Alpaca supplies **price marks only** — we never route orders through Alpaca (or any broker).

---

## 1. Goal

Give the agent desk a **living paper portfolio**: after execute runs, Postfolio remembers what the house trader bought, marks it to market with **Alpaca IEX snapshots** (Basic plan), and shows **unrealized / total P&L** on a **Refresh** cadence — plus an **equity chart** on the agent desk (user/friend charts in v4.1).

Refresh model for v4.0 on **Alpaca Basic**:

| Layer | Mechanism |
|-------|-----------|
| **Marks** | Batched Alpaca REST `GET /v2/stocks/snapshots?feed=iex` — one call per refresh for all open tickers |
| **UI poll** | **Refresh P&L** button + optional **30s poll** while `/agent` is visible |
| **Rate limit** | **200 REST calls/min** on Basic — batch snapshots + 30s poll stays well under cap |
| **Coverage** | **IEX only** (~2–3% of US volume) — not full SIP / not “every exchange” real time |
| **History chart** | `agent_portfolio_mark` table + `GET /trade/portfolio/history/` |

**Not on Basic:** full-market SIP WebSocket, 10k calls/min, or historical SIP within the last 15 minutes. Upgrade path documented in §13 if product needs SIP later.

**v4 pitch:** “Same multi-agent desk — plus a house book that compounds across demos, with P&L and a chart, powered by Alpaca IEX marks. Still 100% paper.”

---

## 2. Why v4

| Today (v2/v3) | Gap |
|---------------|-----|
| Each execute run is a self-contained `$1000` story | No cumulative track record |
| `executedTrades` live only inside that run’s `result_json` | Hard to show “up $42 since Tuesday” |
| Yahoo chart quotes are pull-only at run time | No live tick feel on the desk |
| Desk focuses on one-run narrative | Missing the scoreboard + chart investors expect |
| User posts show static invested amounts | Friends can’t see “how is this portfolio doing *now*?” |

---

## 3. Locked decisions (proposed)

| ID | Decision |
|----|----------|
| **C1** | **Shared house portfolio** for the demo (aligns with Q5 working assumption: shared agent). Not per-user books in v4.0. Optional `username` attribution on runs stays for history, but P&L is house-level. |
| **C2** | **Simulated only** — never real brokerage order routing (Q6). Alpaca is **market data only**, not Alpaca paper-trading API for fills. |
| **C3** | **Continuing book** — execute fills append to open lots; cash is the running cash balance, **not** a fresh `$1000` every click. |
| **C4** | **Initial cash** = `$1000` once (seed). Configurable via env `AGENT_STARTING_CASH` (default `1000`). |
| **C5** | **Long-only in v4.0** — buys from Executor only. Sells / closes are v4.1 (needed for realized P&L beyond “still holding”). |
| **C6** | **Mark-to-market via Alpaca REST** — batched IEX snapshots on refresh/poll (Basic plan). Manual refresh always available. |
| **C7** | **Alpaca Basic plan** — `feed=iex`, **200 calls/min**, IEX real-time snapshots (not full SIP). Execute-time gate still uses v3 **Price Scout (Yahoo)**. Optional **WebSocket IEX** (30 symbols) is a v4.1 upgrade if poll feels too slow. |
| **C8** | **Cost basis = fill price × shares** (average cost per ticker for v4.0 simplicity). |
| **C9** | **Research runs do not change the book** — paper book from `/trade/stock/test/` stays ephemeral. Only `/trade/stock/execute/` mutates portfolio. |
| **C10** | **Reset is explicit** — “Reset house book” (confirm) restores cash to starting seed and clears lots. |
| **C11** | **Secrets server-side only** — `ALPACA_API_KEY_ID` + `ALPACA_API_SECRET_KEY` in backend env; frontend uses Postfolio SSE/REST only. |
| **C12** | **Mark price rule** — prefer **quote mid** `(bid + ask) / 2`; fallback to **last trade** from trades stream; never invent prices. |
| **C13** | **Charts use real data** — equity history from `agent_portfolio_mark`; no decorative fake sparklines ([frontend-ui-guide.md](./frontend-ui-guide.md) §7). |
| **C14** | **User / friend charts (v4.1)** — aggregate a **user’s public investment posts** + Alpaca marks on profile / friend views; house agent chart ships first in v4.0. |

### Working knobs

| Knob | Default |
|------|---------|
| Starting cash seed | `$1000` |
| P&L currency | USD |
| Alpaca data feed | `iex` (Basic plan) |
| REST refresh poll (desk open) | `30s` |
| Alpaca REST rate budget | stay ≤ **200/min** (batch one snapshot call per refresh) |
| Quote staleness flag | `marksStale` when any open ticker missing from snapshot |
| Equity history retention | last `500` marks |

### Environment (backend only)

```bash
# Required for live P&L (v4)
ALPACA_API_KEY_ID=
ALPACA_API_SECRET_KEY=
ALPACA_DATA_FEED=iex                    # Basic plan: IEX snapshots
ALPACA_DATA_BASE_URL=https://data.alpaca.markets

# Existing
GROQ_API_KEY=                           # agent pipeline (v2/v3)
AGENT_STARTING_CASH=1000
```

Add to `.env.example` when implementing — never commit real keys. Frontend must **not** receive Alpaca credentials.

---

## 4. Concepts & formulas

### Positions

For each open ticker:

| Field | Meaning |
|-------|---------|
| `shares` | Current quantity (sum of buy lots) |
| `avgCost` | Average cost per share |
| `costBasis` | `shares × avgCost` |
| `markPrice` | Last Alpaca quote mid or trade |
| `marketValue` | `shares × markPrice` |
| `unrealizedPnl` | `marketValue − costBasis` |
| `unrealizedPnlPct` | `unrealizedPnl / costBasis` (null if basis 0) |
| `markedAt` | Timestamp of last Alpaca message used |
| `live` | `true` if mark received within staleness window |

### Cash & totals

| Field | Meaning |
|-------|---------|
| `cash` | Uninvested cash after fills |
| `investedCost` | Sum of open `costBasis` |
| `holdingsValue` | Sum of open `marketValue` |
| `equity` | `cash + holdingsValue` |
| `totalPnl` | `equity − startingCashSeed` |
| `totalPnlPct` | `totalPnl / startingCashSeed` |
| `dayPnl` | Optional: vs previous close per ticker (v4.1; needs Alpaca daily bar or prior mark) |
| `realizedPnl` | `0` in v4.0 (no sells); populated in v4.1 |

**Identity check:** after any execute or live mark,

```text
equity ≈ cash + Σ(shares × markPrice)
totalPnl = equity − startingCashSeed
```

### Average-cost buy update

On a new fill of `q` shares at price `p` for ticker `T`:

```text
newShares = oldShares + q
newBasis  = oldBasis + q * p
avgCost   = newBasis / newShares
cash     -= q * p
```

Reject / skip fill if `q * p > cash` (should already be impossible if Executor respects remaining cash — see §6).

---

## 5. Architecture

```
  Execute run (v2/v3 pipeline)
           │
           │ fills { ticker → shares, price, cost }
           ▼
  ┌─────────────────────┐
  │  Portfolio Service  │  apply fills → lots + cash
  └──────────┬──────────┘
             │
             │  subscribe open tickers
             ▼
  ┌─────────────────────┐       wss://stream.data.alpaca.markets/v2/iex
  │  AlpacaMarketStream │ ◄──────────────────────────────────────────────
  │  (single backend    │       auth + subscribe quotes/trades
  │   connection)       │
  └──────────┬──────────┘
             │ quote/trade → recalc marks + equity
             ▼
  ┌─────────────────────┐
  │  PortfolioSnapshot  │──► SSE /trade/portfolio/stream/ ──► Agent desk UI
  └──────────┬──────────┘         (scoreboard + chart + positions)
             │
             ▼
  agent_portfolio_mark (equity history for chart)
```

**Why backend proxy, not browser → Alpaca directly**

- Keeps API keys off the client ([security-basics](../skills/security-basics/SKILL.md)).
- One Alpaca connection subscribes to the union of house + (later) actively viewed user tickers.
- Easier to mock in tests (`v2/test` + `FAKEPACA`).

Persistence:

```
agent_portfolio          (singleton house row: cash, seed, updated_at)
agent_position           (ticker PK, shares, avg_cost, mark_price, marked_at, …)
agent_portfolio_mark     (equity snapshots for agent desk chart)
agent_run                (existing — link fill application via run_id)
```

---

## 6. Alpaca WebSocket integration

Reference: [Alpaca streaming market data](https://docs.alpaca.markets/docs/streaming-market-data) · [real-time stock pricing](https://docs.alpaca.markets/docs/real-time-stock-pricing-data).

### Connection lifecycle

1. On Spring startup (or first portfolio subscriber), open WebSocket to `ALPACA_DATA_WS_URL`.
2. Within 10s, send auth:

```json
{"action":"auth","key":"<ALPACA_API_KEY_ID>","secret":"<ALPACA_API_SECRET_KEY>"}
```

3. On success, subscribe to open position tickers:

```json
{"action":"subscribe","quotes":["NVDA","AAPL"],"trades":["NVDA","AAPL"]}
```

4. On quote message (`T: "q"`), compute mid from `bp` / `ap`; update position mark.
5. On trade message (`T: "t"`), use `p` as mark if no fresh quote.
6. When positions change (execute / reset), send incremental subscribe/unsubscribe — do not reconnect unnecessarily.
7. Handle reconnect with exponential backoff; mark all positions `live: false` until fresh data.

### Dev / CI without market hours

Use Alpaca test stream:

```text
wss://stream.data.alpaca.markets/v2/test
{"action":"subscribe","trades":["FAKEPACA"],"quotes":["FAKEPACA"]}
```

Unit tests mock `AlpacaMessageParser`; integration tests optional behind env flag.

### Split: execute quotes vs live marks

| Phase | Source | Why |
|-------|--------|-----|
| **Execute pipeline** (v3) | Yahoo chart via `PriceScout` | Already wired; no Alpaca dependency for agent run CI |
| **Live desk P&L** (v4) | Alpaca WebSocket | Real-time marks; better demo story |
| **Fallback refresh** | Alpaca REST snapshot **or** Yahoo if Alpaca down | `POST /trade/portfolio/refresh/` |

If Alpaca keys missing: desk shows last marks + banner “Live data unavailable”; execute still works on Yahoo.

---

## 7. Interaction with the $1000 execute allowance

v2 sized each run as if allowance were always `$1000`. With a continuing book that is wrong.

**v4 rule:** Capital committee + Risk Gate + Executor use **`availableCash = portfolio.cash`**.

| Topic | Recommendation |
|-------|----------------|
| Deployable cash | `portfolio.cash` at run start |
| Cash reserve floor | `15%` of **starting seed** (stable; `$150`), not of current equity |
| Single-name cap | `35%` of **starting seed** (same as v2 spirit) |
| If cash &lt; min position | Execute returns completed/partial with **no new fills** + clear message |

Research (`/test/`) still uses the configured seed allowance for “what would we do” fantasy sizing. **Working assumption:** research continues to use seed `$1000`; execute uses live cash.

---

## 8. API (proposed)

### `GET /trade/portfolio/`

Current house snapshot (same shape as before, plus live flags).

```json
{
  "startingCashSeed": 1000,
  "cash": 520.0,
  "investedCost": 480.0,
  "holdingsValue": 512.4,
  "equity": 1032.4,
  "totalPnl": 32.4,
  "totalPnlPct": 0.0324,
  "realizedPnl": 0,
  "unrealizedPnl": 32.4,
  "asOf": "2026-08-02T18:00:00Z",
  "streamConnected": true,
  "marksStale": false,
  "positions": [
    {
      "ticker": "NVDA",
      "shares": 2,
      "avgCost": 120.0,
      "costBasis": 240.0,
      "markPrice": 128.5,
      "marketValue": 257.0,
      "unrealizedPnl": 17.0,
      "unrealizedPnlPct": 0.0708,
      "markedAt": "2026-08-02T18:00:00Z",
      "live": true
    }
  ]
}
```

### `GET /trade/portfolio/stream/` (SSE)

Server-sent events for live desk updates. Event types:

| Event | Payload | When |
|-------|---------|------|
| `snapshot` | Full portfolio JSON | On connect + after execute |
| `mark` | `{ ticker, markPrice, unrealizedPnl, … }` | Alpaca quote/trade for one ticker |
| `equity` | `{ equity, totalPnl, totalPnlPct, asOf }` | After any mark recalc (throttle UI to ~4/s) |
| `status` | `{ streamConnected, marksStale }` | Alpaca connect/disconnect |

Frontend: `EventSource` with credentials omitted (demo permitAll). Reconnect with backoff.

### `GET /trade/portfolio/history/`

Equity time series for the agent desk chart.

```json
{
  "points": [
    { "t": "2026-08-02T17:00:00Z", "equity": 1000.0, "totalPnl": 0 },
    { "t": "2026-08-02T18:00:00Z", "equity": 1032.4, "totalPnl": 32.4 }
  ]
}
```

Append a row on execute and at most once per 30s during live streaming (avoid DB spam).

### `POST /trade/portfolio/refresh/`

Force REST re-quote (Alpaca snapshot HTTP or Yahoo fallback); return snapshot. Use when SSE disconnected or user taps **Refresh**.

| Status | Meaning |
|--------|---------|
| `200` | Snapshot (may set `marksStale: true` if some quotes failed) |
| `503` | All quote sources unavailable |

### `POST /trade/portfolio/reset/`

Clears positions, sets `cash = startingCashSeed`, clears chart history (or seeds one point at `$1000`). Demo-only; FE confirm.

### Execute path change

`GET /trade/stock/execute/` after successful fills:

1. Apply fills transactionally to portfolio  
2. Alpaca subscribe new tickers  
3. Include `portfolio` summary on `RunResult`  
4. Push SSE `snapshot` to connected clients  

### User / friend portfolio (v4.1 — design hook)

When [social-network-design.md](./social-network-design.md) profiles ship:

| Endpoint | Purpose |
|----------|---------|
| `GET /users/{username}/portfolio/summary/` | Public aggregate: tickers from user’s posts, marks, unrealized vs invested |
| `GET /users/{username}/portfolio/history/` | Chart points for profile (optional if user has ≥2 post snapshots) |

Only **public** users; respects privacy toggle. Uses same Alpaca stream for marks — no second vendor.

---

## 9. Frontend — agent desk UI

Follow [frontend-ui-guide.md](./frontend-ui-guide.md) §6 `/agent` and §7 visual rules. Fit the **single desk window** from v3 — portfolio UI is **above** the spawn feed, not a separate dashboard page.

### Layout (top → bottom inside `/agent`)

```
┌─────────────────────────────────────────────────────────────┐
│  SCOREBOARD STRIP (sticky)                                  │
│  Equity $1,032.40 · Total P&L +$32.40 (+3.24%) · Cash …    │
│  [● Live] or [Stale — Refresh]                    [Reset]   │
├─────────────────────────────────────────────────────────────┤
│  EQUITY CHART (fixed height ~160px, real data only)         │
│  line: equity vs time from /portfolio/history + live tail   │
├─────────────────────────────────────────────────────────────┤
│  POSITIONS (compact table, live marks via SSE)              │
│  Ticker · Shares · Avg · Mark · U/P&L · live dot            │
├─────────────────────────────────────────────────────────────┤
│  … existing pipeline chips + spawn feed …                   │
└─────────────────────────────────────────────────────────────┘
```

### UI elements

| Element | Behavior |
|---------|----------|
| **Scoreboard** | Equity · total P&L ($ and %) · cash · holdings value; accent/danger tokens for up/down — no neon terminal aesthetic |
| **Live indicator** | Green dot when SSE + Alpaca connected; amber when stale; copy explains IEX feed (not full SIP) on free tier |
| **Equity chart** | Line chart from `GET /trade/portfolio/history/`; extend with SSE `equity` events; empty state: flat line at seed until first execute |
| **Positions table** | Rows update on SSE `mark`; show per-ticker unrealized P&L |
| **Refresh** | Secondary button; calls `POST /trade/portfolio/refresh/` when stream down |
| **After execute** | Spawn feed + fills, then scoreboard/chart update from `RunResult.portfolio` + SSE |
| **Reset** | Confirm dialog; clears chart + positions |
| **Tab hidden** | Keep SSE open but pause chart animations; optional pause history DB writes |

### Chart implementation notes

| Topic | Guidance |
|-------|----------|
| **Library** | Prefer **lightweight-charts** (TradingView) or **Recharts** — pick one; no hand-rolled fake SVG paths |
| **Data** | Only plot points from API; live tail appends from SSE |
| **Style** | Use CSS tokens (`--color-accent`, `--color-danger`, `--color-muted`); thin grid; no gradient fills |
| **Interaction** | Hover tooltip: time, equity, total P&L; no zoom chrome in v4.0 |
| **Accessibility** | Table duplicate of chart summary for screen readers |

### User & friend profile charts (v4.1)

Add to [frontend-ui-guide.md](./frontend-ui-guide.md) when implementing social slice:

| Surface | Chart | Data |
|---------|-------|------|
| **`/account` (self)** | “My holdings” line or bar: invested vs current value per ticker | User’s posts + Alpaca marks |
| **`/users/:username` (friend)** | Same, if profile public | Public posts only |
| **Feed post row (optional)** | Tiny sparkline per ticker — **only** if post has history endpoint data | Defer if cluttered |

Rules: no chart on logged-out views; no chart for private profiles; friend chart links to full profile — not embedded trading terminal in feed hero.

Wire SSE the same way: profile page opens `EventSource` for tickers in that user’s public book (backend subscribes union).

---

## 10. Data model (sketch)

```sql
CREATE TABLE agent_portfolio (
  id            SMALLINT PRIMARY KEY DEFAULT 1 CHECK (id = 1),
  starting_cash DOUBLE PRECISION NOT NULL,
  cash          DOUBLE PRECISION NOT NULL,
  updated_at    TIMESTAMPTZ NOT NULL
);

CREATE TABLE agent_position (
  ticker        VARCHAR(16) PRIMARY KEY,
  shares        DOUBLE PRECISION NOT NULL,
  avg_cost      DOUBLE PRECISION NOT NULL,
  cost_basis    DOUBLE PRECISION NOT NULL,
  mark_price    DOUBLE PRECISION,
  marked_at     TIMESTAMPTZ,
  updated_at    TIMESTAMPTZ NOT NULL
);

CREATE TABLE agent_portfolio_mark (
  id            BIGSERIAL PRIMARY KEY,
  equity        DOUBLE PRECISION NOT NULL,
  total_pnl     DOUBLE PRECISION NOT NULL,
  taken_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX agent_portfolio_mark_taken_at ON agent_portfolio_mark (taken_at);
```

Optional v4.1: `user_portfolio_mark(user_id, …)` mirroring house table for profile charts.

---

## 11. Package layout (target)

```
stockInvestmentAgents/
  portfolio/
    Portfolio.java
    Position.java
    PortfolioService.java
    MarkToMarketService.java      # recalc from marks
    PortfolioRepository.java
    PositionRepository.java
    PortfolioHistoryRepository.java
  marketdata/
    AlpacaWebSocketClient.java    # connect, auth, subscribe
    AlpacaMessageParser.java      # q/t messages → mark
    AlpacaMarketStreamService.java # position-driven subscriptions
    AlpacaRestFallback.java       # optional HTTP snapshot
  portfolio/PortfolioStreamController.java  # SSE endpoint
  execute/TradeExecutor.java      # after fills → PortfolioService.applyFills
  research/PriceScout.java        # v3 — execute-time quotes only
```

Controller additions on `/trade` (same security permitAll demo posture as today).

---

## 12. Implementation slices (ranked)

### Slice 1 — Schema + PortfolioService (no live quotes yet)
- **Goal:** Singleton portfolio; apply fills; snapshot with mark = fill price.  
- **Done when:** Two execute runs accumulate shares/cash; reset works.  
- **Tests:** apply fill; reject over-cash; average cost; reset.

### Slice 2 — Wire Executor + REST API
- **Goal:** Execute mutates book; `GET /trade/portfolio/`; `RunResult.portfolio`.  
- **Tests:** empty fills unchanged; transactional apply.

### Slice 3 — Alpaca WebSocket + live marks
- **Goal:** `AlpacaMarketStreamService` subscribes to open tickers; updates marks on quote/trade.  
- **Done when:** Test stream `FAKEPACA` moves mark in integration test.  
- **Tests:** parse quote mid; parse trade; reconnect resubscribes; missing keys → graceful degrade.

### Slice 4 — SSE + desk scoreboard
- **Goal:** `GET /trade/portfolio/stream/`; scoreboard + positions table + live dot.  
- **Tests:** FE EventSource mock; stale banner; execute updates snapshot.

### Slice 5 — Equity history + agent chart
- **Goal:** `GET /trade/portfolio/history/`; line chart in desk; append on execute + throttled live.  
- **Tests:** history empty → seed line; two points render; SSE extends tail.

### Slice 6 — Refresh fallback
- **Goal:** `POST /trade/portfolio/refresh/` via Alpaca REST or Yahoo.  
- **Tests:** partial failures; 503 when all down.

### Slice 7 (stretch) — User / friend profile charts (v4.1)
- Aggregate public posts per user; same Alpaca marks; profile + friend routes per §9.

### Slice 8 (stretch) — Realized P&L / sells
- Close positions; realized ledger.

---

## 13. Live data vs fallback

| Mode | Effort | Demo value | Recommendation |
|------|--------|------------|----------------|
| **Alpaca WebSocket + SSE** | Medium | **High** — real-time desk | **Ship in v4.0** |
| **Equity history chart** | Medium | High — “track record” story | **Ship in v4.0 (Slice 5)** |
| **Manual Refresh REST** | Low | Medium — recovery | Ship as fallback |
| **30s poll** | Low | Low once SSE exists | Skip if SSE stable |
| **Browser → Alpaca direct** | Medium | Wrong — leaks keys | **Never** |

**Conclusion:** Alpaca WebSocket on the backend + SSE to the browser is the v4 live path. Document that free **IEX** feed is exchange-limited (~2–3% of volume) — acceptable for demo; upgrade to SIP only if product asks.

---

## 14. Edge cases

| Case | Behavior |
|------|----------|
| Execute with `$0` cash | No fills; book unchanged |
| Alpaca auth failure | `streamConnected: false`; banner; Yahoo refresh fallback |
| Market closed | Marks freeze; show “Market closed”; last mark + timestamp |
| Delisted / no quote | Keep last mark; `marksStale: true` |
| >30 tickers (free tier) | Cap subscriptions; refresh REST for overflow |
| Research-only user | Scoreboard visible; unchanged until execute |
| Multiple tabs | SSE per tab OK; DB writes serialized |
| Negative cash from bugs | Reject apply; never persist |

---

## 15. Security / demo notes

- Alpaca keys **backend only** — never `VITE_*`, never commit.  
- Still demo `permitAll` on `/trade/**` unless auth hardens later.  
- Reset is destructive — FE confirm.  
- SSE exposes portfolio JSON — same trust boundary as existing agent endpoints.  
- Do not expose raw Alpaca messages to FE (noise + vendor lock-in).

---

## 16. Acceptance criteria (v4 demo)

1. After several **Execute trades** runs, desk shows cumulative **equity** and **total P&L** vs `$1000` seed.  
2. **Live marks** update via Alpaca WebSocket → SSE without full page refresh (positions + scoreboard).  
3. **Equity chart** on `/agent` plots real history from API (not placeholder data).  
4. Research runs do not mutate the book.  
5. **Reset** returns cash to seed, clears positions, resets chart baseline.  
6. Execute sizing respects **live cash**, not a phantom fresh `$1000`.  
7. Execute pipeline still works with **Groq + v3 research** when Alpaca keys absent (degraded live mode).  
8. (v4.1) Public user profile can show holdings chart for self and friends.

---

## 17. Open questions

| ID | Question | Working assumption |
|----|----------|--------------------|
| **V4-Q1** | House book vs per-user book? | **House** for v4.0; user charts aggregate posts in v4.1 |
| **V4-Q2** | Reserve % of seed or equity? | **Seed** |
| **V4-Q3** | Chart library? | **lightweight-charts** or **Recharts** — decide in Slice 5 PR |
| **V4-Q4** | Alpaca feed tier? | **IEX** free; document SIP upgrade path |
| **V4-Q5** | Sells in v4.0? | **No** — buys only |
| **V4-Q6** | Friend chart in feed vs profile only? | **Profile only** first; no feed sparklines in v4.0 |

---

## 18. Suggested sequencing vs v3

1. ✅ v3 research crew + desk live window (done).  
2. **v4 Slices 1–2** — book + REST (marks at fill price).  
3. **v4 Slices 3–5** — Alpaca stream + SSE + agent chart.  
4. **v4 Slice 6** — refresh fallback.  
5. **v4.1** — user/friend profile charts + sells / realized P&L.

Do not block v3 on Alpaca — Portfolio can mark at fill price until Slice 3 lands.
