# Agent Trader v3 — Multi-Agent Web Research (No Finnhub)

**Status:** Design / plan (not implemented).  
**Audience:** Coding agents + humans replacing Finnhub-backed market data with a scrapable, multi-agent research desk.  
**Supersedes for data ingress:** Finnhub usage in [agent-trader-v2.md](./agent-trader-v2.md) (**A3**).  
**Keeps from v2:** Groq LLMs, bull/bear/judge, capital committee, Risk Gate, simulated fills, Postgres run history.  
**Related:** [system-architecture.md](./system-architecture.md) · [rest-api-contract.md](./rest-api-contract.md) · [frontend-ui-guide.md](./frontend-ui-guide.md) · [implementation-roadmap.md](./implementation-roadmap.md)

---

## 1. Goal

Remove **Finnhub** entirely. Replace news + quote ingress with a **crew of research agents** that plan sources, fetch public web pages, extract structured evidence, and hand a clean pack into the existing debate → capital → execute pipeline.

This stays a **demo / resume project**: complexity should be visible (named agents, live desk events), but one button still finishes inside a wall-clock budget.

---

## 2. Why v3

| v2 reality | Pain |
|------------|------|
| Finnhub news + quotes | Extra API key, rate limits, “finance API wrapper” vibe |
| News Scout is a thin HTTP client | Not interesting as an agent story |
| Quote snapshot tied to one vendor | Single point of failure; outdated feel |
| Desk UI dumps long call traces | Scroll fatigue; demo energy leaks |

**v3 pitch:** “A supervisor spawns researchers that scrape the open web, pack evidence, then the trading committee fights over a $1,000 book.”

---

## 3. Locked decisions (proposed)

| ID | Decision |
|----|----------|
| **B1** | **Delete Finnhub** from the happy path. No `FINNHUB_API_KEY` required. Remove NewsScout/QuoteService Finnhub URLs. |
| **B2** | **Keep Groq** for all LLM roles (`GROQ_API_KEY` only required cloud key). |
| **B3** | **Multi-agent research layer** before debate — not one mega-prompt that “pretends” to browse. |
| **B4** | **Public web only** — fetch HTML/RSS/JSON that is publicly reachable; cite URL + title on every evidence item. |
| **B5** | **No paid scrapers / browser farms** for v3.0 — Java `HttpClient` + HTML→text + Groq extraction. Optional Playwright stretch later. |
| **B6** | **Bounded fetch budget** — max sources, max bytes, max parallel fetches, hard timeout. Prefer partial evidence over hanging. |
| **B7** | **Capital + debate from v2 stay** — research layer is a drop-in replacement for Finnhub news/quotes. |
| **B8** | **Desk is a single live window** — events append as agents spawn; no expandable call-trace dump. `agentTrace` may still exist in the API for persistence, but the FE does not render a verbose payload inspector by default. |
| **B9** | **Simulated trades only** — unchanged. |

### Working knobs

| Knob | Default |
|------|---------|
| Max source URLs planned | `12` |
| Max successful fetches | `8` |
| Max parallel fetches | `3` |
| Max body bytes / page | `250 KB` |
| Research wall-clock slice | `20–25s` of the overall run budget |
| Overall run timeout | `60–75s` (research is slower than Finnhub) |
| Price sources tried per ticker | `2` (fallback chain) |
| Evidence items into debate | top `40` snippets / headlines |

---

## 4. Architecture

```
                         ┌─────────────────────────┐
                         │     Run Supervisor      │
                         │  budget · timeout · I/O │
                         └────────────┬────────────┘
                                      │
                    ┌─────────────────┴─────────────────┐
                    ▼                                   ▼
         ┌────────────────────┐              ┌────────────────────┐
         │  Research Crew     │              │  (later) Memory    │
         │  see §5            │              │  optional cache    │
         └─────────┬──────────┘              └────────────────────┘
                   │ EvidencePack + QuoteMap
                   ▼
         ┌────────────────────┐
         │ Ticker Debate      │  Bull → Bear → Judge  (v2)
         └─────────┬──────────┘
                   ▼
         ┌────────────────────┐
         │ Capital Committee  │  (v2)
         └─────────┬──────────┘
                   ▼
         ┌────────────────────┐
         │ Sizer → Risk Gate  │
         │ → Executor         │
         └────────────────────┘
```

### Research crew (the Finnhub replacement)

```
Source Planner (Groq)
   │  ranked query + URL targets
   ▼
┌──────────────┐   ┌──────────────┐   ┌──────────────┐
│ Web Scout A  │   │ Web Scout B  │   │ Web Scout C  │  parallel fetch+extract
└──────┬───────┘   └──────┬───────┘   └──────┬───────┘
       └──────────────────┼──────────────────┘
                          ▼
                 Evidence Packer (code + light Groq)
                          │
          ┌───────────────┴───────────────┐
          ▼                               ▼
   Headline / snippet pack         Price Scout crew
   → Bull/Bear/Judge               (per advanced ticker)
```

---

## 5. Research sub-agents

### 5.1 Source Planner (Groq — fast)

**Role:** Decide *what to fetch*, not invent prices.

**Input:** run mode, wall-clock remaining, optional theme (“US megacap tech”, etc.).  
**Output (JSON):**
```json
{
  "queries": ["NVDA AI demand", "S&P 500 market news today"],
  "targets": [
    { "url": "https://…", "kind": "news|markets|company", "why": "…" }
  ]
}
```

**Rules:**
- Prefer known-safe public domains (allowlist starter set — see §7).
- Cap targets; no login walls / obvious PDF dumps.
- May emit search-page URLs **or** direct article URLs. Prefer direct.

### 5.2 Web Scout (code fetch + Groq extract) — N workers

**Role:** Fetch one URL, strip boilerplate, extract evidence rows.

**Pipeline per URL:**
1. HTTP GET (timeout, size cap, User-Agent identifying Postfolio demo).
2. Charset-safe HTML → visible text (Jsoup or equivalent).
3. Groq extract → `{ title, publishedAt?, bullets[], tickersMentioned[], sentimentHint }`.

**Output:** `EvidenceItem[]` with required `sourceUrl`.

**Failures:** soft — log skipped URL in trace event; do not fail the whole run unless **zero** items survive.

### 5.3 Evidence Packer (mostly code)

**Role:** Dedupe, rank, and pack for debate.

- Dedupe by normalized title/url  
- Prefer recent + ticker-rich items  
- Produce `HeadlinePack` compatible with today’s Bull/Bear prompts (string headlines OK; keep URLs in detail for the desk)

### 5.4 Price Scout (parallel, code-first)

**Role:** Build `{ ticker → price }` without Finnhub.

**Strategy (v3.0):** ordered fallbacks per ticker, stop on first sane price:
1. Public quote page / endpoint on allowlist (e.g. Yahoo-style quote pages — parse carefully).  
2. Secondary public page if #1 fails.  
3. If still missing → reject ticker with `no_valid_quote` (same as v2 investability gate).

**Rules:**
- One snapshot per run (no drift between sizer and executor) — keep v2 §5.5b semantics.  
- Reject non-positive / absurd prices.  
- Never call Finnhub.

### 5.5 Research Supervisor (optional thin wrapper)

Owns parallel fan-out, fetch semaphore, and research-slice timeout. Can live inside `RunSupervisor` for v3.0 to avoid over-abstraction.

---

## 6. Desk UX (ship with / before backend scrape)

The agent page should feel like a **single ops window**, not a long report:

| Element | Behavior |
|---------|----------|
| Header | Title + Run research / Execute |
| **Desk window** | Fixed height (~min 22rem / max ~70vh); internal scroll only |
| Live feed | Rows append as agents spawn / finish (estimated live progress today; real SSE later) |
| Result blocks | Debate, quotes, capital, fills **append inside the same window** — no page-length stack |
| Call trace | **Removed from UI** (payload dumps are noise) |
| History | Compact list; click reloads result into the same window |

**Later (v3.1):** true SSE / WebSocket event stream from supervisor so the feed is real, not estimated.

---

## 7. Source allowlist & safety

Starter allowlist categories (exact domains TBD in implementation):

- Major public market news / wire sites  
- Company IR press-release pages when Planner emits them  
- Public quote pages used only by Price Scout  

**Hard rules:**
- No secrets scraping, no authenticated sessions, no CAPTCHA farms.  
- Honor timeouts; limit concurrency.  
- Strip scripts; never `eval` page JS.  
- Store only text evidence + URLs in Postgres `result_json`.  
- Rate-limit per host.  
- If robots / block detected → skip URL (soft fail).

**Legal note for docs/README:** this is a **demo research desk** over public pages; not a production scraping product; operators must comply with site terms.

---

## 8. API / DTO changes

Keep `RunResult` shape mostly stable so the FE does not break.

| Field | v3 change |
|-------|-----------|
| `agentTrace[]` | Still produced for persistence / debugging; FE shows **short event lines** only |
| `evidencePack` (new, optional) | `{ items: [{ title, url, bullets[], tickers[] }] }` for desk “Sources” strip |
| `quoteSnapshot` | Filled by Price Scout instead of Finnhub |
| Errors | `503` only for missing Groq / total research failure; Finnhub messages deleted |

Endpoints stay:

- `GET /trade/stock/test/`  
- `GET /trade/stock/execute/`  
- `GET /trade/runs/` · `GET /trade/runs/{id}/`

Optional v3.1: `GET /trade/stock/stream/?mode=research|execute` (SSE).

---

## 9. Package layout (target)

```
stockInvestmentAgents/
  supervisor/RunSupervisor.java
  research/
    ResearchSupervisor.java
    SourcePlanner.java
    WebScout.java
    EvidencePacker.java
    PriceScout.java
    HtmlTextExtractor.java
    SourceAllowlist.java
  debate/…          # keep
  capital/…         # keep
  execute/…         # keep
  groq/…            # keep
  news/             # DELETE Finnhub NewsScout + QuoteService after cutover
```

---

## 10. Implementation slices (ranked)

### Slice 0 — Desk window UX (can ship first)
- **Goal:** Single scrollable desk; remove call-trace UI; live event rows.  
- **Done when:** `/agent` fits the narrative without page scroll; tests updated.  
- **Tests:** happy render; empty result; error; partial warning.

### Slice 1 — Delete Finnhub boundary
- **Goal:** Remove key checks / clients; temporary **stub EvidencePack + quotes** so pipeline still runs in CI.  
- **Done when:** App boots with only `GROQ_API_KEY`; Finnhub strings gone from setup docs.  
- **Tests:** missing Groq → 503; stub research → debate still runs; no Finnhub env required.

### Slice 2 — Source Planner + Web Scout (1 worker)
- **Goal:** Real fetch+extract for a tiny allowlist; pack headlines for Bull.  
- **Done when:** Research run cites real URLs in `evidencePack`.  
- **Tests:** mocked HTTP 200 extract; timeout skip; empty body soft-fail; allowlist reject.

### Slice 3 — Parallel scouts + Evidence Packer
- **Goal:** N workers, dedupe/rank, budget caps.  
- **Tests:** semaphore cap; dedupe; zero-success → 503/partial.

### Slice 4 — Price Scout
- **Goal:** Quote map without Finnhub; investability gate unchanged.  
- **Tests:** parse success; fallback; all miss → stop before capital.

### Slice 5 — Wire + polish
- Docs/README/env examples; desk Sources strip; optional SSE.

---

## 11. Test expectations (per behavior)

For each slice: **1 positive + 3 negative/edge** (project rule).

Example — Web Scout:
1. **+** allowlisted URL returns evidence items with `sourceUrl`  
2. **−** non-allowlisted URL skipped  
3. **edge** oversize body truncated / rejected  
4. **failure** HTTP 403/timeout → soft skip, run continues if others succeed  

---

## 12. Out of scope (explicit)

- Real brokerage / live trading  
- Full Google SERP scraping / CAPTCHA solving  
- Vector RAG revival (optional later)  
- Mobile native app  
- Keeping Finnhub “just for quotes”

---

## 13. Acceptance criteria (v3 demo)

1. No `FINNHUB_API_KEY` in `.env.example` / setup happy path.  
2. With only `GROQ_API_KEY`, execute run returns candidates + quotes + fills (or clear partial).  
3. Evidence items include URLs the scouts fetched.  
4. Agent desk shows a **single live window** of spawns + results; no call-trace inspector.  
5. Capital committee + Risk Gate behavior from v2 still holds.

---

## 14. Open questions (answer before Slice 2 if possible)

| ID | Question | Working assumption |
|----|----------|--------------------|
| **V3-Q1** | Exact allowlist domains for news? | Start with 4–6 major public finance/news sites |
| **V3-Q2** | Price parse target #1? | Public Yahoo quote HTML/JSON if stable; else document fallback |
| **V3-Q3** | SSE in v3.0 or v3.1? | **v3.1** — estimated live feed OK for v3.0 desk |
| **V3-Q4** | Cache pages in Postgres? | No for v3.0; in-memory per run only |

---

## 15. Suggested build order vs UI

1. **Now:** Desk UX (Slice 0) + this doc.  
2. **Next PR:** Slice 1 stub cutover (Finnhub gone).  
3. **Then:** Slices 2–4 until research crew is real.  

Do not layer scrape code on top of Finnhub — **replace**, then delete dead packages.
