# Agent Trader v4 — Paper Portfolio P&L Tracking

**Status:** Design / plan (not implemented).  
**Audience:** Coding agents + humans adding lasting gain/loss tracking for the simulated house trader.  
**Depends on:** [agent-trader-v2.md](./agent-trader-v2.md) (fills + allowance) · [agent-trader-v3.md](./agent-trader-v3.md) (quote/price sources without Finnhub).  
**Related:** [system-architecture.md](./system-architecture.md) · [rest-api-contract.md](./rest-api-contract.md) · [frontend-ui-guide.md](./frontend-ui-guide.md) · [product-decisions.md](./product-decisions.md) Q5/Q6

---

## 1. Goal

Give the agent desk a **living paper portfolio**: after execute runs, Postfolio remembers what the house trader bought, marks it to market, and shows **unrealized / realized / total P&L** so demos answer “how is the agent doing?” — not only “what did the last run fill?”

Refresh model for v4.0: **user clicks Refresh** (simple, reliable). Optional **soft poll** (every N seconds while the desk is open) if it stays cheap. Full push/SSE mark-to-market is a stretch (v4.1).

---

## 2. Why v4

| Today (v2) | Gap |
|------------|-----|
| Each execute run is a self-contained `$1000` story | No cumulative track record |
| `executedTrades` live only inside that run’s `result_json` | Hard to show “up $42 since Tuesday” |
| Research runs never create positions | Fine — but execute history is underused |
| Desk focuses on one-run narrative | Missing the scoreboard investors expect |

**v4 pitch:** “Same multi-agent desk — plus a house book that compounds across demos, with one-click mark-to-market.”

---

## 3. Locked decisions (proposed)

| ID | Decision |
|----|----------|
| **C1** | **Shared house portfolio** for the demo (aligns with Q5 working assumption: shared agent). Not per-user books in v4.0. Optional `username` attribution on runs stays for history, but P&L is house-level. |
| **C2** | **Simulated only** — never real brokerage (Q6). |
| **C3** | **Continuing book** — execute fills append to open lots; cash is the running cash balance, **not** a fresh `$1000` every click. |
| **C4** | **Initial cash** = `$1000` once (seed). Configurable via env `AGENT_STARTING_CASH` (default `1000`). |
| **C5** | **Long-only in v4.0** — buys from Executor only. Sells / closes are v4.1 (needed for realized P&L beyond “still holding”). |
| **C6** | **Mark-to-market on demand** — primary UX is a **Refresh P&L** button. Optional interval poll while `/agent` is focused. |
| **C7** | **Quotes from v3 Price Scout** (or interim quote service). No Finnhub. Missing quote → hold last mark / show stale flag; never invent prices. |
| **C8** | **Cost basis = fill price × shares** (average cost per ticker for v4.0 simplicity). |
| **C9** | **Research runs do not change the book** — paper book from `/trade/stock/test/` stays ephemeral. Only `/trade/stock/execute/` mutates portfolio. |
| **C10** | **Reset is explicit** — “Reset house book” (confirm) restores cash to starting seed and clears lots. Prevents demo doom loops without hiding the feature. |

### Working knobs

| Knob | Default |
|------|---------|
| Starting cash seed | `$1000` |
| P&L currency | USD |
| Soft poll interval (optional) | `30s` while desk mounted + tab visible |
| Quote timeout for refresh | `8–10s` |
| Max tickers marked per refresh | all open positions (expect ≤ 15) |
| Stale quote threshold | show “stale” if mark older than `15m` |

---

## 4. Concepts & formulas

### Positions

For each open ticker:

| Field | Meaning |
|-------|---------|
| `shares` | Current quantity (sum of buy lots) |
| `avgCost` | Average cost per share |
| `costBasis` | `shares × avgCost` |
| `markPrice` | Last successful mark-to-market quote |
| `marketValue` | `shares × markPrice` |
| `unrealizedPnl` | `marketValue − costBasis` |
| `unrealizedPnlPct` | `unrealizedPnl / costBasis` (null if basis 0) |

### Cash & totals

| Field | Meaning |
|-------|---------|
| `cash` | Uninvested cash after fills |
| `investedCost` | Sum of open `costBasis` |
| `holdingsValue` | Sum of open `marketValue` |
| `equity` | `cash + holdingsValue` |
| `totalPnl` | `equity − startingCashSeed` |
| `totalPnlPct` | `totalPnl / startingCashSeed` |
| `realizedPnl` | `0` in v4.0 (no sells); populated in v4.1 |

**Identity check:** after any execute or refresh,

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
             │  Refresh P&L (button / optional poll)
             ▼
  ┌─────────────────────┐
  │  Mark-to-Market     │  Price Scout for open tickers
  └──────────┬──────────┘
             │
             ▼
  ┌─────────────────────┐
  │  PortfolioSnapshot  │  → FE desk scoreboard
  └─────────────────────┘
```

Persistence:

```
agent_portfolio          (singleton house row: cash, seed, updated_at)
agent_position           (ticker PK, shares, avg_cost, …)
agent_portfolio_mark     (optional history of equity snapshots for a tiny sparkline)
agent_run                (existing — link fill application via run_id)
```

---

## 6. Interaction with the $1000 execute allowance

v2 sized each run as if allowance were always `$1000`. With a continuing book that is wrong.

**v4 rule:** Capital committee + Risk Gate + Executor use **`availableCash = portfolio.cash`** (and reserve % of *seed* or of *equity* — pick one; recommendation below).

| Topic | Recommendation |
|-------|----------------|
| Deployable cash | `portfolio.cash` at run start |
| Cash reserve floor | `15%` of **starting seed** (stable; `$150`), not of current equity |
| Single-name cap | `35%` of **starting seed** (same as v2 spirit) |
| If cash &lt; min position | Execute returns completed/partial with **no new fills** + clear message |

Research (`/test/`) still uses the configured seed allowance for “what would we do” fantasy sizing — or optionally also reads live cash for realism. **Working assumption:** research continues to use seed `$1000` so it stays a clean scenario; execute uses live cash.

---

## 7. API (proposed)

### `GET /trade/portfolio/`

Current house snapshot.

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
      "markedAt": "2026-08-02T18:00:00Z"
    }
  ]
}
```

### `POST /trade/portfolio/refresh/`

Re-quote all open positions via Price Scout; update marks; return same snapshot shape.

| Status | Meaning |
|--------|---------|
| `200` | Snapshot (may set `marksStale: true` if some quotes failed) |
| `503` | Quote layer unavailable / Groq-only outage if scout needs LLM |

### `POST /trade/portfolio/reset/`

Clears positions, sets `cash = startingCashSeed`. Demo-only; require confirm on FE.

### Execute path change

`GET /trade/stock/execute/` after successful fills:

1. Apply fills transactionally to portfolio  
2. Optionally auto-mark just-filled tickers at fill price (mark = fill → unrealized `0` until refresh)  
3. Include `portfolio` summary on `RunResult` (new optional field) so the desk updates without a second round-trip  

```json
{
  "runId": "…",
  "executedTrades": { "…": { } },
  "portfolio": { "equity": 1032.4, "totalPnl": 32.4, "cash": 520.0 }
}
```

---

## 8. Frontend (agent desk)

Fit the **single desk window** from v3 §6 — scoreboard is a sticky header strip *inside* or just above the window, not a second dashboard page.

| Element | Behavior |
|---------|----------|
| **Scoreboard** | Equity · total P&L ($ and %) · cash · holdings value; green/muted for up/down (use existing accent/danger tokens — no neon trading terminal) |
| **Refresh P&L** | Button; loading state; updates scoreboard + per-ticker marks |
| **Positions table** | Compact rows in the desk scroll area (ticker, shares, avg, mark, u/pnl) |
| **After execute** | Spawns feed + fills, then scoreboard updates from `RunResult.portfolio` |
| **Reset** | Secondary control with confirm dialog |
| **Optional poll** | If `document.visibilityState === 'visible'`, refresh every 30s; pause when tab hidden |

**Real-time note:** True tick streaming is out of scope without a market data socket. “Feels live enough” = button + optional 30s poll using the same refresh endpoint.

---

## 9. Data model (sketch)

```sql
CREATE TABLE agent_portfolio (
  id            SMALLINT PRIMARY KEY DEFAULT 1 CHECK (id = 1),  -- singleton
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
```

Optional: `agent_fill_lot` if we later need tax-lot / FIFO sells; v4.0 average-cost needs only `agent_position`.

---

## 10. Package layout (target)

```
stockInvestmentAgents/
  portfolio/
    Portfolio.java              # aggregate root / snapshot DTO
    Position.java
    PortfolioService.java       # applyFills, reset, snapshot
    MarkToMarketService.java    # calls Price Scout
    PortfolioRepository.java
    PositionRepository.java
  execute/TradeExecutor.java    # after fills → PortfolioService.applyFills
  research/PriceScout.java      # v3 — reused for marks
```

Controller additions on `/trade` (same security permitAll demo posture as today).

---

## 11. Implementation slices (ranked)

### Slice 1 — Schema + PortfolioService (no live quotes yet)
- **Goal:** Singleton portfolio; apply fills from a completed execute; snapshot with mark = last fill price.  
- **Done when:** Two execute runs accumulate shares/cash correctly; reset works.  
- **Tests:**  
  1. **+** apply fill → cash down, position up  
  2. **−** fill larger than cash rejected  
  3. **edge** second buy averages cost  
  4. **failure** reset restores seed and zero positions  

### Slice 2 — Wire Executor + API
- **Goal:** Execute mutates book; `GET /trade/portfolio/`; `RunResult.portfolio` summary.  
- **Tests:** execute empty fills → book unchanged; unknown ticker skipped; concurrent apply serialized (`@Transactional` + singleton row).

### Slice 3 — Refresh mark-to-market
- **Goal:** `POST /trade/portfolio/refresh/` via Price Scout; stale flags.  
- **Tests:** all quotes ok; partial quote failures; no positions → no-op 200; scout down → 503.

### Slice 4 — Desk scoreboard UI
- **Goal:** Scoreboard + Refresh + positions in the desk window; optional 30s poll.  
- **Tests:** FE renders pnl; refresh error; empty book; poll paused when hidden (if implemented).

### Slice 5 (stretch) — Realized P&L / sells
- Close or trim positions; realized ledger; keep average-cost or move to lots.

### Slice 6 (stretch) — Equity history sparkline
- Append `agent_portfolio_mark` on each refresh; tiny SVG/chart in desk (only if it stays calm — no fake chart junk).

---

## 12. Refresh vs “real time”

| Mode | Effort | Demo value | Recommendation |
|------|--------|------------|----------------|
| **Button Refresh** | Low | High | **Ship in v4.0** |
| **Visibility-aware poll (30s)** | Low–medium | Medium | Ship if Slice 4 stays small |
| **SSE price stream** | High | Marginal for delayed public quotes | Defer to v4.1+ |
| **WebSocket broker** | Very high | Wrong for simulated demo | Out of scope |

**Conclusion:** Prefer **button + optional poll**. Document that marks are delayed public quotes, not trading-floor real time.

---

## 13. Edge cases

| Case | Behavior |
|------|----------|
| Execute with `$0` cash | No fills; message on result; book unchanged |
| Split / reverse split | Out of scope — ignore corporate actions in v4.0 |
| Delisted / no quote | Keep last mark; `marksStale: true`; pnl disclaimer |
| Research-only user | Scoreboard still visible; unchanged until someone executes |
| Multiple browser tabs refresh | Last write wins on marks; cash mutations only via execute (transactional) |
| Negative cash from bugs | Invariant assert / reject apply; never persist negative cash |

---

## 14. Security / demo notes

- Still demo `permitAll` on `/trade/**` unless auth hardens later.  
- Reset is destructive — FE confirm; consider gating reset behind logged-in session only (same as desk).  
- No secrets in snapshot JSON.  
- Do not expose raw scrape HTML in portfolio APIs.

---

## 15. Acceptance criteria (v4 demo)

1. After several **Execute trades** runs, desk shows cumulative **equity** and **total P&L** vs `$1000` seed.  
2. **Refresh P&L** updates marks without starting a full agent run.  
3. Research runs do not mutate the book.  
4. **Reset** returns cash to seed and clears positions.  
5. Execute sizing respects **live cash**, not a phantom fresh `$1000`, when cash has been spent.  
6. Works with v3 quote path (no Finnhub).

---

## 16. Open questions

| ID | Question | Working assumption |
|----|----------|--------------------|
| **V4-Q1** | House book vs per-user book? | **House** for v4.0 |
| **V4-Q2** | Reserve % of seed or equity? | **Seed** |
| **V4-Q3** | Auto-poll on desk? | **Yes, 30s**, pause when tab hidden |
| **V4-Q4** | Include tiny equity history chart? | **No** until Slice 6 |
| **V4-Q5** | Sells in v4.0? | **No** — buys only |

---

## 17. Suggested sequencing vs v3

1. Finish v3 desk UX (done or in flight) + Finnhub removal / Price Scout.  
2. Implement **v4 Slices 1–4** (book + refresh button).  
3. Only then consider sells / SSE.

Do not block v3 research crew on P&L — Portfolio can temporarily mark at fill price until Price Scout lands.
