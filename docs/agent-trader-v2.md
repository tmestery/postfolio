# Agent Trader v2 — Deep Multi-Agent Design

**Status:** Implemented (v2 pipeline live — Groq multi-agent + capital committee).  
**Audience:** Coding agents + humans rebuilding the LLM agent trader.  
**Related:** [architecture.md](./architecture.md) · [api.md](./api.md) · [plan.md](./plan.md) · [setup.md](./setup.md)

---

## 1. Goal

Rebuild Postfolio’s agent trader as a **deep multi-agent system**: a supervisor coordinating specialized sub-agents, a **bull/bear debate + judging** layer for ticker selection, and a **capital committee** that fights over a `$1000` allowance before simulated execution.

This is a **resume / demo project**. Architectural complexity is intentional and should be visible in:

- Package layout and class names
- Runtime `agentTrace[]` returned to the UI
- Docs / README diagrams

It must still produce a **working demo**: one button → structured result within a bounded timeout, without requiring local Ollama.

---

## 2. Why v2 (problems with v1)

| v1 reality | Pain |
|------------|------|
| Local Ollama (`llama3` + `nomic-embed-text`) | Slow, brittle, blocks demos without local models |
| File-based RAG vector store | Extra failure mode; embeddings tied to Ollama |
| “Multi-agent” is a fixed sequential loop | Hard to explain / show; little debate or judgment |
| Allowance sizing is one weak LLM + regex | Not interesting; easy to get 0 shares / junk |
| Loose `Map` responses | FE can’t reliably render reasoning |
| No persistence | Can’t show history / screenshots of past runs |

---

## 3. Locked design decisions

| ID | Decision |
|----|----------|
| **A1** | **LLM provider = Groq** ([groq.com](https://groq.com)) via OpenAI-compatible API. Env: `GROQ_API_KEY`. Never expose to the frontend. |
| **A2** | **Drop Ollama as a required dependency** for the happy path. Remove localhost:11434 generate/embed calls from the demo pipeline. |
| **A3** | **Keep Finnhub** for market news + live quotes (`FINNHUB_API_KEY`). |
| **A4** | **Deep agents structure** — supervisor + specialized sub-agents + judging, not a single prompt. |
| **A5** | **Capital subsystem is multi-agent** — competing allocators + Cash Guard + Capital Judge + Risk Gate (see §6). |
| **A6** | **Bounded execution** — caps on candidates, positions, debate rounds, wall-clock timeout. Prefer partial structured results over hanging. |
| **A7** | **Simulated trades only** — no real brokerage. |
| **A8** | **Trace-first API** — every run returns an `agentTrace` suitable for the FE agent page. |
| **A9** | **Persist runs in Postgres** (recommended for demo screenshots / history). |

### Working defaults (tunable via config)

| Knob | Default |
|------|---------|
| Starting allowance | `$1000` |
| Min cash reserve (Cash Guard) | `15%` ($150) |
| Max stock candidates into debate | `8` |
| Max positions after capital judge | `5` |
| Max single-name weight (of starting allowance) | `35%` |
| Min position notional | `$50` or 1 share (whichever is feasible) |
| Stock debate rounds | `1` (Bull → Bear → Judge) |
| Stock reflection rounds | `0` or `1` (optional) |
| Capital allocation debate rounds | `1` |
| Risk Gate rebalance micro-rounds | max `1` |
| Wall-clock timeout | `45–60s` → graceful stop + partial result |
| Groq model (fast roles) | e.g. `llama-3.1-8b-instant` (env override) |
| Groq model (judge roles) | e.g. `llama-3.3-70b-versatile` (env override) |

---

## 4. High-level architecture

```
                         ┌─────────────────────────┐
                         │     Run Supervisor      │
                         │  budget · timeout · I/O │
                         └────────────┬────────────┘
                                      │
              ┌───────────────────────┼───────────────────────┐
              ▼                       ▼                       ▼
     ┌────────────────┐      ┌────────────────┐      ┌────────────────┐
     │  News Scout    │      │  (optional)    │      │  Quote Tool    │
     │  Finnhub news  │      │  Headline pack │      │  Finnhub       │
     └────────┬───────┘      │  / light RAG   │      └────────┬───────┘
              │              └────────┬───────┘               │
              └───────────────────────┼───────────────────────┘
                                      ▼
                         ┌─────────────────────────┐
                         │   Ticker Debate Layer   │
                         │  Bull → Bear → Judge    │
                         │  (+ optional reflect)   │
                         └────────────┬────────────┘
                                      │ candidates + scores
                                      ▼
                         ┌─────────────────────────┐
                         │   Capital Committee     │
                         │  see §6 (allowance)     │
                         └────────────┬────────────┘
                                      │ approved book ($ / ticker)
                                      ▼
                         ┌─────────────────────────┐
                         │  Position Sizer         │
                         │  $ → shares via quotes  │
                         └────────────┬────────────┘
                                      ▼
                         ┌─────────────────────────┐
                         │  Risk / Book Gate       │
                         │  hard rules + 1 retry   │
                         └────────────┬────────────┘
                                      ▼
                         ┌─────────────────────────┐
                         │  Executor               │
                         │  simulated fills        │
                         └─────────────────────────┘
```

**Infra boundary**

| Concern | Owner |
|---------|--------|
| Chat completions | Groq (`GROQ_API_KEY`) |
| News + quotes | Finnhub (`FINNHUB_API_KEY`) |
| Run + step history | PostgreSQL |
| Session / who clicked Run | FE `postfolio.session` (demo); optional `username` on request |

---

## 5. Ticker selection — sub-agents

### 5.1 Run Supervisor

**Role:** Orchestrates one agent run. Owns wall-clock timeout, round counters, and assembly of the final DTO + trace.

**Does:**
- Create `runId`, start timer
- Call News Scout → pack headlines
- Invoke Bull, Bear, Judge (in order)
- Optionally one reflection pass
- Hand winners to Capital Supervisor
- Call Position Sizer → Risk Gate → Executor
- Persist run (when enabled)
- On timeout / dependency failure: return partial DTO + clear `error` / `status`

**Does not:** Invent tickers itself or spend money directly.

### 5.2 News Scout

**Role:** Fetch and normalize market news.

**Inputs:** Finnhub general market news (cap ~50–75 headlines).  
**Outputs:** `HeadlinePack` — list of `{ headline, source, url?, datetime? }`.

**Rules:**
- No Groq required (code path). Optional light summarize-via-Groq later.
- Fail with `AgentUnavailableException`-style 503 if Finnhub key missing / empty / down.
- Prefer **Option A light retrieval:** filter/rank headlines by keyword density or mentioned tickers before debate. Full vector RAG is an optional stretch (see §10) — not required for v2 demo.

### 5.3 Bull / Thesis Agent (Groq — fast model)

**Role:** Argue *for* investments given the headline pack.

**Output (structured JSON):**
```json
{
  "candidates": [
    {
      "ticker": "NVDA",
      "thesis": "Strong AI demand narrative in recent headlines…",
      "confidence": 0.82,
      "supportingHeadlineIndexes": [0, 3, 12]
    }
  ]
}
```

**Constraints:** Max N candidates; tickers must look like symbols (`^[A-Z]{1,5}$`); no prose outside JSON.

### 5.4 Bear / Skeptic Agent (Groq — fast model)

**Role:** Attack or qualify each bull candidate (and optionally propose exclusions).

**Output:**
```json
{
  "critiques": [
    {
      "ticker": "NVDA",
      "risks": ["Valuation risk not addressed", "News may be hype-driven"],
      "severityDown": 0.15,
      "reject": false
    }
  ]
}
```

### 5.5 Stock Judge (Groq — stronger model)

**Role:** Score candidates using a fixed rubric; produce the shortlist for capital.

**Rubric dimensions (0–10 each, documented in prompt):**
1. News support strength  
2. Thesis clarity  
3. Risk acknowledgment (did bull/bear surface real risks?)  
4. Symbol validity / investability for a demo book  

**Output:**
```json
{
  "ranked": [
    {
      "ticker": "NVDA",
      "score": 8.1,
      "decision": "advance",
      "rationale": "…"
    }
  ],
  "rejected": [
    { "ticker": "XYZ", "reason": "Invalid / low support" }
  ]
}
```

Only `decision: "advance"` tickers enter the capital committee (cap 5–8).

### 5.5b Quote snapshot + investability gate (code, no LLM)

Immediately after the Stock Judge, the Supervisor fetches **one Finnhub quote snapshot** for all advanced tickers:

- Ticker has no quote / price ≤ 0 → **rejected here** (trace `rejected: "no_valid_quote"`), *before* any capital is debated over it.
- The snapshot `{ ticker → price }` is passed to the allocators, Position Sizer, Risk Gate, **and Executor** — quotes are fetched **once per run** so prices cannot drift between sizing and execution (drift could otherwise breach the cash floor after Risk Gate approval).

### 5.6 Optional reflection (max 1 round)

Supervisor may ask Bull to revise top pick(s) given Bear + Judge notes. Default **off** or **max 1** so demos stay snappy.

---

## 6. Allowance / capital committee (deep complexity)

This is the intentional complexity centerpiece. The `$1000` book is **not** a single sizing prompt. Competing capital agents propose different books; a Capital Judge merges them; Cash Guard and Risk Gate constrain spend.

### 6.1 Flow

```
Stock Judge winners (ticker + score + thesis)
              │
              ▼
     ┌────────────────────┐
     │ Capital Supervisor │  owns $1000, cash floor, stop rules
     └─────────┬──────────┘
       ┌───────┼───────────┬──────────────┐
       ▼       ▼           ▼              ▼
 ┌──────────┐ ┌──────────┐ ┌───────────┐ ┌──────────────┐
 │Aggressive│ │ Balanced │ │ Defensive │ │ Cash Guard   │
 │Allocator │ │Allocator │ │Allocator  │ │              │
 └─────┬────┘ └────┬─────┘ └─────┬─────┘ └──────┬───────┘
       │           │             │              │
       └───────────┼─────────────┘              │
                   ▼                            │
        ┌─────────────────────┐                 │
        │ Allocation Debate   │◄────────────────┘
        │ Capital Judge merge │
        └──────────┬──────────┘
                   ▼
        ┌─────────────────────┐
        │ Position Sizer      │  dollars → shares (Finnhub)
        └──────────┬──────────┘
                   ▼
        ┌─────────────────────┐
        │ Risk / Book Gate    │  veto / 1 rebalance round
        └──────────┬──────────┘
                   ▼
             Executor (fills)
```

### 6.2 Capital Supervisor

**Owns:**
- `startingAllowance` (default 1000)
- `remainingCash`
- `cashReserveTarget` (default 15%)
- Round / stop rules (“stop if remaining ≤ reserve” or “no affordable positions”)

**Emits to trace:** starting book state, which allocators ran, final approved notional map.

### 6.3 Aggressive Allocator (Groq — fast)

**Bias:** Concentrate into highest-confidence winners.  
**Typical behavior:** 40–60% of *deployable* capital (allowance − reserve) into top score; remainder into #2/#3 or cash.

**Output:**
```json
{
  "style": "aggressive",
  "proposal": { "NVDA": 400, "AAPL": 200, "MSFT": 150 },
  "argument": "Concentrate in clearest winners while leaving room for reserve."
}
```

Dollar amounts are **notionals**, not shares. Sum of proposal ≤ deployable capital.

### 6.4 Balanced Allocator (Groq — fast)

**Bias:** Diversify across 3–5 names proportional to judge scores.  
**Caps:** Respect max single-name weight (e.g. 35%) in its own proposal.

### 6.5 Defensive Allocator (Groq — fast)

**Bias:** Smaller positions, prefer higher-consensus / lower-drama names, leave more cash than the minimum reserve if uncertain.

### 6.6 Cash Guard (Groq — fast **or** pure rules + short LLM rationale)

**Role:** Oppose over-deployment.

**Hard rule (code):** `remainingCash` after proposed spend must be ≥ `cashReserveTarget`.

**Soft role (LLM):** Critique proposals that skim the reserve or crowd into one name; may attach `vetoSpend: true` on a specific proposal line.

**Always visible in the FE trace** — this is resume candy.

### 6.7 Capital Judge (Groq — stronger model)

**Role:** Score the three allocation proposals and emit **one approved book**.

**Score dimensions (example):**
1. Alignment with stock-judge rankings  
2. Diversification vs concentration fitness (given market tone in headlines)  
3. Cash discipline (Cash Guard compliance)  
4. Feasibility (amounts ≥ min notional; ≤ deployable)

**Output:**
```json
{
  "approved": { "NVDA": 350, "AAPL": 250, "MSFT": 200 },
  "cashHeld": 200,
  "winnerStyle": "blend",
  "scores": {
    "aggressive": 6.2,
    "balanced": 8.4,
    "defensive": 7.1
  },
  "rationale": "Blended balanced core with a mild aggressive overweight on NVDA."
}
```

`cashHeld` should be ≥ reserve target unless Supervisor documents an override (default: **no override**).

### 6.8 Position Sizer (code-heavy; optional tiny LLM)

**Role:** Convert approved dollar map → share counts using the **run's quote snapshot** (§5.5b) — no re-fetch.

**Rules:**
- Whole shares only for demo clarity (`floor(dollars / price)`).
- If shares would be 0 → skip, return dollars to cash, trace `skipped: "cannot_afford_one_share"`.
- (No-quote tickers were already rejected at §5.5b, so the sizer never sees them.)
- Recompute `remainingAllowance` after all conversions.

### 6.9 Risk / Book Gate (code rules + optional LLM check)

**Hard rules (must enforce in code):**
1. No invalid tickers  
2. ≤ max positions  
3. Single-name weight ≤ 35% of the **starting allowance** (i.e. ≤ $350 on a $1000 book). Basis is starting allowance, not deployed capital — a deployed-capital basis is unsatisfiable when only 1–2 names advance (one name = 100% of deployed).  
4. Total cost ≤ starting allowance − reserve (i.e. never breach Cash Guard floor)  
5. No duplicate tickers  

**On failure:** Produce `violations[]`. Allow **one** micro-round: Capital Supervisor asks Capital Judge (or Balanced Allocator) to fix the book. If still invalid → drop offending lines and keep cash.

### 6.10 Allowance accounting (always returned)

| Field | Meaning |
|-------|---------|
| `startingAllowance` | e.g. 1000 |
| `cashReserveTarget` | e.g. 150 |
| `totalInvested` | Sum of fill costs |
| `remainingAllowance` | Cash left after fills |
| `rejectedAllocations[]` | Dollars/tickers cut by gates or unaffordable shares |
| `allocatorProposals` | Aggressive / Balanced / Defensive raw proposals |
| `capitalJudgeDecision` | Approved book + scores + rationale |

---

## 7. Executor

Same spirit as v1 `executeAgent`, cleaned up:

- Input: `{ ticker → shares }` from Position Sizer / Risk Gate  
- Fill at the **run's quote snapshot price** (§5.5b) — no re-fetch, so cost math exactly matches what Risk Gate approved  
- Build `executedTrades`: `{ shares, price, cost }`  
- Update totals  
- **No one-shot lockout** — runs are repeatable  
- Append an `executor` step to `agentTrace`

---

## 8. API contracts (target)

Preserve paths for FE compatibility; **upgrade bodies**.

### `GET /trade/stock/test/` — research / debate only (optional lighter path)

May stop after Stock Judge (no capital spend) **or** run capital proposals without executing fills. Prefer documenting one behavior and sticking to it.

**Recommended v2 behavior:** Full pipeline through Capital Judge + Position Sizer **without** calling Executor (paper book only).  
**Alternative:** Stop after Stock Judge. Choose during implementation and update [api.md](./api.md).

### `GET /trade/stock/execute/` — full run including fills

### Success shape (sketch)

```json
{
  "runId": "uuid",
  "status": "completed",
  "startingAllowance": 1000,
  "cashReserveTarget": 150,
  "totalInvested": 780.5,
  "remainingAllowance": 219.5,
  "candidates": [ /* stock judge ranked */ ],
  "rejectedTickers": [ /* … */ ],
  "allocatorProposals": {
    "aggressive": { "NVDA": 400, "AAPL": 200 },
    "balanced": { "NVDA": 250, "AAPL": 250, "MSFT": 200 },
    "defensive": { "AAPL": 200, "MSFT": 200, "JNJ": 150 }
  },
  "capitalJudgeDecision": {
    "approved": { "NVDA": 350, "AAPL": 250, "MSFT": 200 },
    "winnerStyle": "blend",
    "rationale": "…"
  },
  "executedTrades": {
    "NVDA": { "shares": 2, "price": 120.0, "cost": 240.0 }
  },
  "agentTrace": [
    {
      "step": 1,
      "agent": "news_scout",
      "status": "ok",
      "summary": "Fetched 75 headlines",
      "detail": {}
    },
    {
      "step": 2,
      "agent": "bull",
      "status": "ok",
      "summary": "Proposed 5 candidates",
      "detail": {}
    }
  ]
}
```

### Failure

| Condition | HTTP | Body |
|-----------|------|------|
| Missing `GROQ_API_KEY` | 503 | `{ "error": "GROQ_API_KEY is not configured…" }` |
| Groq unreachable / rate limit | 503 | `{ "error": "…" }` |
| Missing Finnhub / empty news | 503 | existing style messages |
| Timeout → partial | 200 or 504 (pick one; prefer **200 + `status: "partial"`** with trace) | include `error` or `warning` |

Update [api.md](./api.md) when implementing.

---

## 9. Frontend expectations

Agent page (`/agent`) should sell the architecture:

1. **Run research** / **Execute trades** (existing)  
2. Long-run UX still required (but Groq should be much faster than Ollama)  
3. **Trace timeline** — render `agentTrace[]` as ordered steps (agent name, summary, expand detail)  
4. **Capital panel** — show three allocator proposals side-by-side + Cash Guard + Capital Judge winner  
5. **Fills table** — existing execution results  
6. History list when persistence lands (`GET /trade/runs/` or similar — add when built)

No secrets in the browser. No Groq key in Vite env.

---

## 10. Persistence (recommended)

### Tables (sketch)

**`agent_run`**
- `id` (UUID), `created_at`, `username` (nullable demo), `status`, `starting_allowance`, `total_invested`, `remaining_allowance`, `result_json` (full DTO), `error`

**`agent_step`** (optional normalization)
- `id`, `run_id`, `step_index`, `agent_name`, `status`, `summary`, `detail_json`

**Done when:** Execute endpoint writes a run; FE can list recent runs for the demo user.

---

## 11. Package / code layout (target)

Suggest evolving `stockInvestmentAgents/` toward clear roles (names illustrative):

```
stockInvestmentAgents/
  AgentUnavailableException.java          # keep / extend for Groq
  groq/
    GroqClient.java                       # OpenAI-compatible chat
    GroqConfig.java                       # models, timeouts, keys
  supervisor/
    RunSupervisor.java
  news/
    NewsScout.java                        # was dataCollection
  debate/
    BullAgent.java
    BearAgent.java
    StockJudgeAgent.java
  capital/
    CapitalSupervisor.java
    AggressiveAllocator.java
    BalancedAllocator.java
    DefensiveAllocator.java
    CashGuard.java
    CapitalJudgeAgent.java
    PositionSizer.java
    RiskBookGate.java
  execute/
    TradeExecutor.java                    # was executeAgent
  model/                                  # DTOs: HeadlinePack, TraceStep, RunResult…
  rag/                                    # optional stretch; not required for demo path
```

v1 classes may be replaced in place or deleted once v2 path is green — prefer **one pipeline**, not dual Ollama/Groq forever.

---

## 12. Config / secrets

| Env var | Required for | Notes |
|---------|--------------|-------|
| `GROQ_API_KEY` | All LLM agents | From GroqCloud; never commit |
| `FINNHUB_API_KEY` | News + quotes | Existing |
| `GROQ_MODEL_FAST` | Optional | Default fast model id |
| `GROQ_MODEL_JUDGE` | Optional | Default stronger model id |
| `AGENT_ALLOWANCE` | Optional | Default `1000` |
| `AGENT_CASH_RESERVE_PCT` | Optional | Default `0.15` |
| `AGENT_TIMEOUT_MS` | Optional | Default `60000` |

Document in [setup.md](./setup.md) and `.env.example` (backend). Remove Ollama as a **required** setup step; may mention as legacy removed.

---

## 13. Complexity knobs vs resume narrative

| Wanted on resume | How we show it |
|------------------|----------------|
| Multi-agent orchestration | Run Supervisor + named sub-agents |
| Adversarial debate | Bull vs Bear → Stock Judge |
| Capital markets flavor | Aggressive / Balanced / Defensive + Cash Guard |
| Governance / risk | Capital Judge + Risk Gate + reserve floor |
| Observability | `agentTrace[]` + persisted runs |
| Modern inference | Groq (fast, cloud) instead of local Ollama |

| Must not sacrifice | Mechanism |
|--------------------|-----------|
| Demo finishes | Timeouts, max rounds, max positions |
| Testability | Mock `GroqClient`; pure functions for Risk Gate / Position Sizer |
| Clear failures | 503 + `{error}` for missing keys / provider down |

---

## 14. Out of scope (v2)

- Real order routing / brokerage  
- Unbounded autonomous tool-browsing loops  
- Requiring local Ollama for the happy path  
- Weekly cron scheduler (still optional later)  
- Deep agents without a wall-clock timeout  
- JWT / production auth for agent routes (demo localStorage remains)

---

## 15. Implementation slices

Work in vertical slices. Each slice: code + **1 positive + 3 negative** tests + demoable checkpoint. Stop for approval between large phases if needed.

### Slice 0 — Design doc (this file)
- **Done when:** Doc merged; open questions answered or defaults locked above.

### Slice 1 — Groq client + config
- **Goal:** Shared `GroqClient` (chat completions), env wiring, healthful errors.  
- **Done when:** Unit test can mock client; missing key → clear exception.  
- **Tests:** success parse; missing key; HTTP 429/5xx mapped; invalid JSON response handling.

### Slice 2 — DTOs + Trace + Supervisor skeleton
- **Goal:** `RunResult` / `TraceStep` types; Supervisor runs stub agents returning canned traces.  
- **Done when:** `/trade/stock/execute/` returns stable JSON with `agentTrace` (stub data).  
- **Tests:** shape present; empty stubs; timeout flag; serialization.

### Slice 3 — News Scout (Finnhub)
- **Goal:** Replace brittle news fetch; feed Supervisor.  
- **Tests:** success; missing key; empty list; malformed payload.

### Slice 4 — Bull + Bear + Stock Judge
- **Goal:** Real Groq debate; structured JSON; ticker validation.  
- **Done when:** Research path returns ranked candidates + critiques in trace.  
- **Tests:** happy path (mocked Groq); bad JSON from model; all rejected; invalid tickers stripped.

### Slice 5 — Capital committee
- **Goal:** Three allocators + Cash Guard + Capital Judge producing approved notionals.  
- **Done when:** Trace shows proposals + judge decision; reserve floor respected.  
- **Tests:** Cash Guard blocks full spend; concentration proposal loses to balanced; judge blend; sum ≤ deployable.

### Slice 6 — Position Sizer + Risk Gate + Executor
- **Goal:** Dollars → shares → fills; one rebalance micro-round max.  
- **Done when:** Full execute path works end-to-end with mocks.  
- **Tests:** can’t afford 1 share; no quote; weight violation triggers rebalance; successful fills.

### Slice 7 — Persist runs + list API
- **Goal:** Postgres `agent_run` (+ optional steps); FE history.  
- **Tests:** save success; list empty; list filters; failed run still stored with error.

### Slice 8 — FE agent UX
- **Goal:** Trace timeline + capital panel + fills; update copy (Groq, not Ollama).  
- **FE checklist:** [frontend.md](./frontend.md).  
- **Tests:** render mocked result; empty trace; error 503 message; partial status.

### Slice 9 — Docs cleanup
- Update [api.md](./api.md), [setup.md](./setup.md), [architecture.md](./architecture.md), [plan.md](./plan.md), README agent section. Remove Ollama as required.

---

## 16. Test strategy (cross-cutting)

| Layer | Approach |
|-------|----------|
| `GroqClient` | Mock HTTP; never call real Groq in CI |
| Risk Gate / Position Sizer / Cash Guard rules | Pure unit tests (no Spring) |
| Supervisor | Mock sub-agents; assert order + stop rules |
| Controllers | MockMvc; 200 DTO + 503 paths |
| FE | Mock `/trade/*` payloads including rich `agentTrace` |

CI must stay green **without** Groq/Finnhub/Ollama credentials.

---

## 17. Migration notes from v1

| v1 | v2 |
|----|----|
| `dataCollection` | `NewsScout` |
| `dataAnalyzerAgent` + Ollama | `BullAgent` / `BearAgent` / `StockJudgeAgent` via Groq |
| `costAnalysisAgent` + Ollama | Capital committee (§6) |
| `EmbeddingService` + `VectorStore` | Optional stretch; not on happy path |
| `manager.deployAgents()` | `RunSupervisor.run(...)` |
| `executeAgent` | `TradeExecutor` (repeatable) |
| Map-only responses | Versioned `RunResult` DTO + trace |

Delete or quarantine dead Ollama code once Slice 6 is green to avoid two sources of truth.

---

## 18. Open items (non-blocking defaults chosen)

| Item | Default in this doc | Change if you want |
|------|---------------------|--------------------|
| Persist runs | **Yes** (Slice 7) | Defer to later |
| Auto-post fills to social feed as house user | **No** for v2 | Add as Slice 10 later |
| Stock reflection round | **Optional, max 1** | Force off |
| `/trade/stock/test/` depth | Paper book through sizer, no executor | Stop after stock judge |
| Light headline filtering vs full RAG | **Light filter** | Add embeddings later |
| Single-name weight basis | **% of starting allowance** (deployed-capital basis is degenerate with 1–2 names) | — |
| Quote fetching | **One snapshot per run** (§5.5b) | — |

---

## 19. Definition of done (v2 demoable)

1. Backend boots **without** Ollama.  
2. With `GROQ_API_KEY` + `FINNHUB_API_KEY`, `/trade/stock/execute/` returns fills **and** a multi-step `agentTrace` including bull/bear/judge **and** capital proposals + Cash Guard + Capital Judge.  
3. Missing Groq or Finnhub key → clear **503** `{error}`.  
4. Cash reserve floor is visible and enforced.  
5. FE shows trace + capital debate, not just a bare map.  
6. CI green with mocked LLM.  
7. Docs updated; this file remains the source of truth for agent design.

---

## 20. Reading order for implementers

1. This document (§3–§6 especially)  
2. [api.md](./api.md) — update as slices land  
3. [setup.md](./setup.md) — Groq env  
4. [frontend.md](./frontend.md) — before agent UI changes  
5. Existing `stockInvestmentAgents/**` — replace deliberately, don’t layer forever  
