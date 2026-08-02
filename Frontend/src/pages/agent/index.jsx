import { useEffect, useRef, useState } from 'react'
import {
  executeAgentTrades,
  getAgentRun,
  listAgentRuns,
  runAgentResearch,
} from '../../api/trades'
import { useAuth } from '../../auth/useAuth'
import { agentMeta, buildPipelineView } from './pipeline'

const currency = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' })

function ElapsedTimer({ startedAt }) {
  const [seconds, setSeconds] = useState(0)
  useEffect(() => {
    const interval = setInterval(() => {
      setSeconds(Math.round((Date.now() - startedAt) / 1000))
    }, 1000)
    return () => clearInterval(interval)
  }, [startedAt])
  return <span>{seconds}s</span>
}

function StatusChip({ status }) {
  const tone =
    status === 'ok' || status === 'done'
      ? 'bg-accent-soft text-accent-deep'
      : status === 'running' || status === 'active'
        ? 'bg-accent-soft text-accent-deep'
        : status === 'error' || status === 'timeout' || status === 'failed'
          ? 'bg-danger-soft text-danger'
          : 'bg-paper text-muted'
  return (
    <span className={`rounded px-1.5 py-0.5 text-[10px] font-semibold uppercase tracking-wide ${tone}`}>
      {status}
    </span>
  )
}

function EventRow({ step }) {
  const meta = agentMeta(step.agent)
  return (
    <li className="animate-rise flex gap-3 border-b border-line/80 py-2.5 last:border-0">
      <span className="mt-0.5 w-6 shrink-0 font-mono text-[11px] text-muted">#{step.step}</span>
      <div className="min-w-0 flex-1">
        <div className="flex flex-wrap items-center gap-2">
          <span className="text-sm font-semibold text-ink">{meta.label}</span>
          <StatusChip status={step.status} />
          <span className="text-[11px] text-muted">{meta.call}</span>
        </div>
        <p className="mt-0.5 text-sm text-muted">{step.summary}</p>
      </div>
    </li>
  )
}

function CompactPipeline({ steps, running, mode }) {
  const view = buildPipelineView(steps, { running, mode: mode ?? 'execute' })
  return (
    <ul className="flex gap-1.5 overflow-x-auto pb-1">
      {view.map((stage) => {
        const tone =
          stage.phase === 'done'
            ? 'border-accent/40 bg-accent-soft text-accent-deep'
            : stage.phase === 'active'
              ? 'border-accent bg-accent-soft text-accent-deep'
              : 'border-line bg-paper text-muted'
        return (
          <li
            key={stage.id}
            className={`shrink-0 rounded-md border px-2 py-1 text-[11px] font-medium ${tone}`}
            title={stage.summary}
          >
            {stage.label}
          </li>
        )
      })}
    </ul>
  )
}

function DebateBoard({ candidates, rejected }) {
  const advanced = Array.isArray(candidates) ? candidates : []
  const rejectedList = Array.isArray(rejected) ? rejected : []
  if (advanced.length === 0 && rejectedList.length === 0) {
    return <p className="text-sm text-muted">No debate output yet.</p>
  }
  return (
    <div className="space-y-3">
      <ul className="space-y-3">
        {advanced.map((c) => (
          <li key={c.ticker} className="border-t border-line pt-3 first:border-0 first:pt-0">
            <div className="flex flex-wrap items-baseline justify-between gap-2">
              <span className="font-mono text-base font-bold text-ink">{c.ticker}</span>
              <span className="text-xs text-muted">score {c.score ?? '—'}</span>
            </div>
            {c.thesis && (
              <p className="mt-1 text-sm text-ink">
                <span className="font-medium text-accent-deep">Bull · </span>
                {c.thesis}
              </p>
            )}
            {Array.isArray(c.risks) && c.risks.length > 0 && (
              <ul className="mt-1 space-y-0.5 text-sm text-muted">
                {c.risks.slice(0, 3).map((risk) => (
                  <li key={risk}>
                    <span className="font-medium text-danger">Bear · </span>
                    {risk}
                  </li>
                ))}
              </ul>
            )}
          </li>
        ))}
      </ul>
      {rejectedList.length > 0 && (
        <p className="text-xs text-muted">
          Rejected:{' '}
          {rejectedList.map((row) => row.ticker).filter(Boolean).join(', ') || '—'}
        </p>
      )}
    </div>
  )
}

function CapitalPanel({ result }) {
  const proposals = result.allocatorProposals ?? {}
  const styles = Object.keys(proposals)
  const decision = result.capitalJudgeDecision ?? {}
  const approved = decision.approved ?? {}

  return (
    <div className="space-y-3">
      <dl className="flex flex-wrap gap-4 text-xs">
        <div>
          <dt className="text-muted">Allowance</dt>
          <dd className="font-semibold text-ink">{currency.format(result.startingAllowance ?? 0)}</dd>
        </div>
        <div>
          <dt className="text-muted">Reserve</dt>
          <dd className="font-semibold text-ink">{currency.format(result.cashReserveTarget ?? 0)}</dd>
        </div>
        <div>
          <dt className="text-muted">Cash held</dt>
          <dd className="font-semibold text-ink">
            {typeof decision.cashHeld === 'number' ? currency.format(decision.cashHeld) : '—'}
          </dd>
        </div>
      </dl>

      {styles.length > 0 && (
        <div className="grid gap-3 sm:grid-cols-3">
          {styles.map((style) => (
            <div key={style}>
              <h4 className="text-xs font-semibold capitalize text-ink">{style}</h4>
              <ul className="mt-1 space-y-0.5 text-xs text-muted">
                {Object.entries(proposals[style] ?? {}).map(([ticker, amount]) => (
                  <li key={ticker} className="flex justify-between gap-2">
                    <span className="font-mono text-ink">{ticker}</span>
                    <span className="text-accent-deep">{currency.format(amount)}</span>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>
      )}

      <div className="border-t border-line pt-2">
        <h4 className="text-xs font-semibold text-ink">
          Capital judge{decision.winnerStyle ? ` · ${decision.winnerStyle}` : ''}
        </h4>
        {decision.rationale && <p className="mt-1 text-sm text-muted">{decision.rationale}</p>}
        <ul className="mt-1 space-y-0.5 text-sm">
          {Object.entries(approved).map(([ticker, amount]) => (
            <li key={ticker} className="flex justify-between gap-2">
              <span className="font-mono font-semibold text-ink">{ticker}</span>
              <span className="text-accent-deep">{currency.format(amount)}</span>
            </li>
          ))}
        </ul>
      </div>
    </div>
  )
}

function BookOrFills({ result, mode }) {
  if (mode === 'research') {
    const entries = Object.entries(result.plannedShares ?? {})
    if (entries.length === 0) return <p className="text-sm text-muted">No paper positions sized.</p>
    return (
      <ul className="space-y-1 text-sm">
        {entries.map(([ticker, shares]) => (
          <li key={ticker} className="flex justify-between">
            <span className="font-mono font-bold text-ink">{ticker}</span>
            <span className="text-muted">{shares} shares</span>
          </li>
        ))}
      </ul>
    )
  }

  const trades =
    result.executedTrades && typeof result.executedTrades === 'object'
      ? Object.entries(result.executedTrades)
      : []
  return (
    <div>
      {trades.length === 0 ? (
        <p className="text-sm text-muted">No fills this run.</p>
      ) : (
        <ul className="space-y-1 text-sm">
          {trades.map(([ticker, detail]) => (
            <li key={ticker} className="flex flex-wrap items-baseline justify-between gap-2">
              <span className="font-mono font-bold text-ink">{ticker}</span>
              <span className="text-muted">
                {detail?.shares ?? '—'} @{' '}
                {typeof detail?.price === 'number' ? currency.format(detail.price) : '—'}
              </span>
              <span className="font-semibold text-accent-deep">
                {typeof detail?.cost === 'number' ? currency.format(detail.cost) : '—'}
              </span>
            </li>
          ))}
        </ul>
      )}
      <dl className="mt-2 flex gap-6 border-t border-line pt-2 text-xs">
        <div>
          <dt className="text-muted">Invested</dt>
          <dd className="font-semibold text-ink">
            {typeof result.totalInvested === 'number' ? currency.format(result.totalInvested) : '—'}
          </dd>
        </div>
        <div>
          <dt className="text-muted">Remaining</dt>
          <dd className="font-semibold text-ink">
            {typeof result.remainingAllowance === 'number'
              ? currency.format(result.remainingAllowance)
              : '—'}
          </dd>
        </div>
      </dl>
    </div>
  )
}

function ResultBlocks({ result, mode }) {
  const quotes = Object.entries(result.quoteSnapshot ?? {})
  return (
    <div className="space-y-5">
      {(result.status === 'partial' || result.error) && (
        <p className="rounded-md bg-danger-soft px-3 py-2 text-sm text-danger">
          {result.status === 'partial' ? 'Partial run: ' : ''}
          {result.error || 'Run finished with warnings.'}
        </p>
      )}

      <section>
        <h3 className="text-xs font-semibold uppercase tracking-wide text-muted">Debate</h3>
        <div className="mt-2">
          <DebateBoard candidates={result.candidates} rejected={result.rejectedTickers} />
        </div>
      </section>

      {quotes.length > 0 && (
        <section>
          <h3 className="text-xs font-semibold uppercase tracking-wide text-muted">Quotes</h3>
          <ul className="mt-2 flex flex-wrap gap-1.5">
            {quotes.map(([ticker, price]) => (
              <li key={ticker} className="rounded border border-line bg-paper px-2 py-1 text-xs">
                <span className="font-mono font-bold text-ink">{ticker}</span>
                <span className="ml-1.5 text-accent-deep">
                  {typeof price === 'number' ? currency.format(price) : '—'}
                </span>
              </li>
            ))}
          </ul>
        </section>
      )}

      <section>
        <h3 className="text-xs font-semibold uppercase tracking-wide text-muted">Capital</h3>
        <div className="mt-2">
          <CapitalPanel result={result} />
        </div>
      </section>

      <section>
        <h3 className="text-xs font-semibold uppercase tracking-wide text-muted">
          {mode === 'research' ? 'Paper book' : 'Fills'}
        </h3>
        <div className="mt-2">
          <BookOrFills result={result} mode={mode} />
        </div>
      </section>
    </div>
  )
}

function DeskWindow({
  status,
  mode,
  result,
  liveTrace,
  startedAt,
  error,
}) {
  const scrollerRef = useRef(null)
  const steps =
    status === 'running'
      ? liveTrace
      : Array.isArray(result?.agentTrace)
        ? result.agentTrace
        : []

  useEffect(() => {
    const el = scrollerRef.current
    if (!el) return
    el.scrollTop = el.scrollHeight
  }, [steps.length, status, result])

  return (
    <div className="flex h-[min(70vh,36rem)] min-h-[22rem] flex-col overflow-hidden rounded-xl border border-line bg-paper/50">
      <div className="shrink-0 border-b border-line px-3 py-2.5">
        <div className="mb-2 flex flex-wrap items-center justify-between gap-2">
          <p className="text-xs font-semibold uppercase tracking-wide text-muted">Desk</p>
          <p className="font-mono text-[11px] text-muted">
            {status === 'running' && startedAt ? (
              <>
                live · <ElapsedTimer startedAt={startedAt} />
              </>
            ) : result?.runId ? (
              <>
                {String(result.runId).slice(0, 8)} · {result.status}
              </>
            ) : (
              'idle'
            )}
          </p>
        </div>
        <CompactPipeline
          steps={steps}
          running={status === 'running'}
          mode={mode ?? 'execute'}
        />
      </div>

      <div ref={scrollerRef} className="min-h-0 flex-1 overflow-y-auto px-3 py-3" aria-live="polite">
        {status === 'idle' && (
          <p className="text-sm text-muted">
            Hit run to watch the supervisor spawn agents. Results stay in this window — scroll here if
            the book gets long.
          </p>
        )}

        {status === 'error' && (
          <p className="rounded-md bg-danger-soft px-3 py-2 text-sm text-danger">{error}</p>
        )}

        {(status === 'running' || (status === 'done' && steps.length > 0)) && (
          <section className="mb-4">
            <h3 className="mb-1 text-xs font-semibold uppercase tracking-wide text-muted">
              Spawns
            </h3>
            <ol>
              {steps.map((step) => (
                <EventRow key={`${step.step}-${step.agent}`} step={step} />
              ))}
            </ol>
            {status === 'running' && (
              <p className="mt-2 text-xs text-muted">
                Progress is estimated until the server returns. Leaving the page does not cancel the
                run.
              </p>
            )}
          </section>
        )}

        {status === 'done' && result && (
          <>
            {steps.length === 0 && (
              <p className="mb-4 text-sm text-muted">No agent steps recorded for this run.</p>
            )}
            <ResultBlocks result={result} mode={mode} />
          </>
        )}
      </div>
    </div>
  )
}

export default function AgentPage() {
  const { user } = useAuth()
  const [mode, setMode] = useState(null)
  const [status, setStatus] = useState('idle')
  const [result, setResult] = useState(null)
  const [error, setError] = useState('')
  const [startedAt, setStartedAt] = useState(null)
  const [history, setHistory] = useState([])
  const [loadingHistoryId, setLoadingHistoryId] = useState(null)
  const [liveTrace, setLiveTrace] = useState([])
  const activeRun = useRef(0)

  useEffect(() => {
    listAgentRuns()
      .then(setHistory)
      .catch(() => setHistory([]))
  }, [status])

  useEffect(() => {
    if (status !== 'running') return undefined
    const expected = buildPipelineView([], { running: true, mode: mode ?? 'execute' })
    let i = 0
    const tick = () => {
      i += 1
      setLiveTrace(
        expected.slice(0, Math.min(i, expected.length)).map((stage, idx) => ({
          step: idx + 1,
          agent: stage.id,
          status: idx + 1 < i ? 'ok' : 'running',
          summary: idx + 1 < i ? `Spawned ${stage.label}` : `Calling ${stage.call}…`,
          detail: {},
        })),
      )
    }
    const immediate = setTimeout(tick, 0)
    const interval = setInterval(tick, 2200)
    return () => {
      clearTimeout(immediate)
      clearInterval(interval)
    }
  }, [status, mode])

  async function run(nextMode) {
    const runId = ++activeRun.current
    setMode(nextMode)
    setStatus('running')
    setResult(null)
    setError('')
    setLiveTrace([])
    setStartedAt(Date.now())
    const username = user?.username
    try {
      const data =
        nextMode === 'research'
          ? await runAgentResearch(username)
          : await executeAgentTrades(username)
      if (activeRun.current !== runId) return
      if (data?.error && !data?.agentTrace) {
        setError(String(data.error))
        setStatus('error')
        return
      }
      setResult(data)
      setStatus('done')
    } catch (err) {
      if (activeRun.current !== runId) return
      setError(err.message)
      setStatus('error')
    }
  }

  async function openHistoryRun(runId) {
    setLoadingHistoryId(runId)
    setError('')
    try {
      const data = await getAgentRun(runId)
      setResult(data)
      setMode(data?.executedTrades && Object.keys(data.executedTrades).length > 0 ? 'execute' : 'research')
      setStatus('done')
    } catch (err) {
      setError(err.message)
      setStatus('error')
    } finally {
      setLoadingHistoryId(null)
    }
  }

  const running = status === 'running'

  return (
    <section className="overflow-hidden rounded-2xl border border-line bg-surface px-5 py-6 shadow-[0_1px_0_rgba(20,24,22,0.04)] sm:px-6">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="font-display text-2xl font-semibold text-ink">Agent desk</h1>
          <p className="mt-1 max-w-xl text-sm text-muted">
            One window: agents spawn, then debate and capital land in place. No call-trace dump.
          </p>
        </div>
        <div className="flex gap-2">
          <button
            type="button"
            onClick={() => run('research')}
            disabled={running}
            className="rounded-md bg-accent px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-accent-deep disabled:opacity-50"
          >
            Run research
          </button>
          <button
            type="button"
            onClick={() => run('execute')}
            disabled={running}
            className="rounded-md border border-line px-4 py-2 text-sm font-medium text-ink transition-colors hover:border-accent disabled:opacity-50"
          >
            Execute trades
          </button>
        </div>
      </div>

      <div className="mt-5">
        <DeskWindow
          status={status}
          mode={mode}
          result={result}
          liveTrace={liveTrace}
          startedAt={startedAt}
          error={error}
        />
      </div>

      {status === 'idle' && (
        <p className="mt-3 text-xs text-muted">
          Needs <code className="font-mono">GROQ_API_KEY</code>
          {'. '}
          Finnhub is being replaced — see <code className="font-mono">docs/agent-trader-v3.md</code>.
        </p>
      )}

      {history.length > 0 && (
        <section className="mt-6 border-t border-line pt-4">
          <h2 className="text-sm font-semibold text-ink">Recent runs</h2>
          <ul className="mt-2 divide-y divide-line text-sm">
            {history.map((run) => (
              <li key={run.runId}>
                <button
                  type="button"
                  onClick={() => openHistoryRun(run.runId)}
                  disabled={loadingHistoryId === run.runId}
                  className="flex w-full flex-wrap items-baseline justify-between gap-2 py-2 text-left transition-colors hover:text-accent-deep disabled:opacity-50"
                >
                  <span className="font-mono text-xs text-muted">{run.runId?.slice(0, 8)}</span>
                  <span className="text-ink">{run.status}</span>
                  <span className="text-muted">
                    {typeof run.totalInvested === 'number'
                      ? currency.format(run.totalInvested)
                      : '—'}
                  </span>
                  <span className="text-xs text-accent">
                    {loadingHistoryId === run.runId ? 'Loading…' : 'Open'}
                  </span>
                </button>
              </li>
            ))}
          </ul>
        </section>
      )}
    </section>
  )
}
