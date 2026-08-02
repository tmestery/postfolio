import { useEffect, useRef, useState } from 'react'
import {
  executeAgentTrades,
  getAgentRun,
  listAgentRuns,
  runAgentResearch,
} from '../../api/trades'
import { useAuth } from '../../auth/useAuth'
import {
  agentMeta,
  buildPipelineView,
  detailEntries,
  isEmptyDetail,
} from './pipeline'

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
          : status === 'adjusted' || status === 'stopped'
            ? 'bg-paper text-muted'
            : 'bg-paper text-muted'
  return (
    <span className={`rounded px-1.5 py-0.5 text-[10px] font-semibold uppercase tracking-wide ${tone}`}>
      {status}
    </span>
  )
}

function DetailValue({ value }) {
  if (value == null) return <span className="text-muted">—</span>
  if (typeof value === 'number') {
    return <span className="font-mono text-ink">{Number.isInteger(value) ? value : value.toFixed(2)}</span>
  }
  if (typeof value === 'boolean') return <span className="text-ink">{value ? 'true' : 'false'}</span>
  if (typeof value === 'string') return <span className="text-ink">{value}</span>
  if (Array.isArray(value)) {
    if (value.length === 0) return <span className="text-muted">[]</span>
    if (value.every((item) => typeof item === 'string' || typeof item === 'number')) {
      return (
        <span className="flex flex-wrap gap-1">
          {value.map((item) => (
            <span key={String(item)} className="rounded bg-paper px-1.5 py-0.5 font-mono text-xs text-ink">
              {item}
            </span>
          ))}
        </span>
      )
    }
  }
  return (
    <pre className="max-h-48 overflow-auto whitespace-pre-wrap break-words rounded bg-paper px-2 py-1.5 font-mono text-[11px] leading-relaxed text-ink">
      {JSON.stringify(value, null, 2)}
    </pre>
  )
}

function TraceStepRow({ step, defaultOpen = false }) {
  const [open, setOpen] = useState(defaultOpen)
  const meta = agentMeta(step.agent)
  const hasDetail = !isEmptyDetail(step.detail)

  return (
    <li className="animate-rise rounded-lg border border-line bg-surface/80">
      <button
        type="button"
        onClick={() => hasDetail && setOpen((v) => !v)}
        className={`flex w-full items-start gap-3 px-3 py-3 text-left ${hasDetail ? 'cursor-pointer hover:bg-paper/60' : 'cursor-default'}`}
        aria-expanded={hasDetail ? open : undefined}
      >
        <span className="mt-0.5 w-7 shrink-0 font-mono text-xs text-muted">#{step.step}</span>
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <span className="text-sm font-semibold text-ink">{meta.label}</span>
            <StatusChip status={step.status} />
            <span className="text-[11px] text-muted">{meta.call}</span>
          </div>
          <p className="mt-0.5 text-sm text-muted">{step.summary}</p>
        </div>
        {hasDetail && (
          <span className="mt-1 text-xs text-muted">{open ? 'Hide' : 'Detail'}</span>
        )}
      </button>
      {open && hasDetail && (
        <dl className="space-y-2 border-t border-line px-3 py-3">
          {detailEntries(step.detail).map(({ key, value }) => (
            <div key={key} className="grid gap-1 sm:grid-cols-[8rem_1fr] sm:gap-3">
              <dt className="font-mono text-[11px] uppercase tracking-wide text-muted">{key}</dt>
              <dd className="text-sm">
                <DetailValue value={value} />
              </dd>
            </div>
          ))}
        </dl>
      )}
    </li>
  )
}

function PipelineRail({ steps, running, mode }) {
  const view = buildPipelineView(steps, { running, mode: mode ?? 'execute' })
  return (
    <ol className="grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
      {view.map((stage, index) => {
        const tone =
          stage.phase === 'done'
            ? 'border-accent/40 bg-accent-soft/50'
            : stage.phase === 'active'
              ? 'border-accent bg-accent-soft shadow-[0_0_0_1px_rgba(15,107,76,0.12)]'
              : 'border-line bg-paper/40'
        return (
          <li
            key={stage.id}
            className={`rounded-lg border px-3 py-2.5 transition-colors ${tone} ${
              stage.phase === 'active' ? 'animate-rise' : ''
            }`}
            style={stage.phase === 'active' ? { animationDelay: `${index * 40}ms` } : undefined}
          >
            <div className="flex items-center justify-between gap-2">
              <span className="text-sm font-semibold text-ink">{stage.label}</span>
              <StatusChip status={stage.status} />
            </div>
            <p className="mt-1 text-[11px] text-muted">
              {stage.kind === 'llm' ? 'LLM' : stage.kind === 'api' ? 'API' : stage.kind.toUpperCase()} ·{' '}
              {stage.call}
            </p>
            <p className="mt-1 line-clamp-2 text-xs text-muted">{stage.summary}</p>
          </li>
        )
      })}
    </ol>
  )
}

function DebateBoard({ candidates, rejected }) {
  const advanced = Array.isArray(candidates) ? candidates : []
  const rejectedList = Array.isArray(rejected) ? rejected : []

  if (advanced.length === 0 && rejectedList.length === 0) {
    return <p className="text-sm text-muted">No debate output yet.</p>
  }

  return (
    <div className="space-y-4">
      <ul className="space-y-3">
        {advanced.map((c) => (
          <li key={c.ticker} className="animate-rise border-t border-line pt-3 first:border-0 first:pt-0">
            <div className="flex flex-wrap items-baseline justify-between gap-2">
              <span className="font-mono text-lg font-bold text-ink">{c.ticker}</span>
              <span className="text-sm text-muted">
                score {c.score ?? '—'}
                {typeof c.confidence === 'number' ? ` · conf ${c.confidence}` : ''}
              </span>
            </div>
            {c.thesis && (
              <p className="mt-1 text-sm text-ink">
                <span className="font-medium text-accent-deep">Bull · </span>
                {c.thesis}
              </p>
            )}
            {Array.isArray(c.risks) && c.risks.length > 0 && (
              <ul className="mt-2 space-y-1 text-sm text-muted">
                {c.risks.map((risk) => (
                  <li key={risk}>
                    <span className="font-medium text-danger">Bear · </span>
                    {risk}
                  </li>
                ))}
              </ul>
            )}
            {(c.decision || c.rationale) && (
              <p className="mt-2 text-sm text-muted">
                <span className="font-medium text-ink">Judge · </span>
                {c.decision ? `${c.decision}` : ''}
                {c.rationale ? ` — ${c.rationale}` : ''}
              </p>
            )}
          </li>
        ))}
      </ul>
      {rejectedList.length > 0 && (
        <div className="border-t border-line pt-3">
          <h3 className="text-sm font-semibold text-ink">Rejected</h3>
          <ul className="mt-2 space-y-1 text-sm text-muted">
            {rejectedList.map((row, i) => (
              <li key={`${row.ticker ?? 'x'}-${i}`} className="flex flex-wrap gap-x-2">
                <span className="font-mono font-semibold text-ink">{row.ticker ?? '—'}</span>
                <span>{row.reason ?? 'rejected'}</span>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  )
}

function QuoteStrip({ quotes }) {
  const entries = Object.entries(quotes ?? {})
  if (entries.length === 0) return null
  return (
    <section>
      <h2 className="font-display text-xl font-semibold text-ink">Quote snapshot</h2>
      <ul className="mt-3 flex flex-wrap gap-2">
        {entries.map(([ticker, price]) => (
          <li
            key={ticker}
            className="rounded-md border border-line bg-paper px-3 py-1.5 text-sm"
          >
            <span className="font-mono font-bold text-ink">{ticker}</span>
            <span className="ml-2 text-accent-deep">
              {typeof price === 'number' ? currency.format(price) : '—'}
            </span>
          </li>
        ))}
      </ul>
    </section>
  )
}

function CapitalPanel({ result }) {
  const proposals = result.allocatorProposals ?? {}
  const styles = Object.keys(proposals)
  const decision = result.capitalJudgeDecision ?? {}
  const approved = decision.approved ?? {}
  const guard = result.cashGuard ?? {}
  const argumentsByStyle = {}
  for (const step of result.agentTrace ?? []) {
    if (String(step.agent || '').startsWith('allocator_') && step.detail?.argument) {
      argumentsByStyle[step.agent.replace('allocator_', '')] = step.detail.argument
    }
  }

  return (
    <div className="space-y-5">
      <dl className="flex flex-wrap gap-6 text-sm">
        <div>
          <dt className="text-muted">Starting allowance</dt>
          <dd className="font-semibold text-ink">{currency.format(result.startingAllowance ?? 0)}</dd>
        </div>
        <div>
          <dt className="text-muted">Cash reserve</dt>
          <dd className="font-semibold text-ink">{currency.format(result.cashReserveTarget ?? 0)}</dd>
        </div>
        <div>
          <dt className="text-muted">Cash held (judge)</dt>
          <dd className="font-semibold text-ink">
            {typeof decision.cashHeld === 'number' ? currency.format(decision.cashHeld) : '—'}
          </dd>
        </div>
      </dl>

      {styles.length > 0 && (
        <div className="grid gap-4 sm:grid-cols-3">
          {styles.map((style) => (
            <div key={style} className="border-t border-line pt-3">
              <h3 className="text-sm font-semibold capitalize text-ink">{style}</h3>
              {argumentsByStyle[style] && (
                <p className="mt-1 text-xs text-muted">{argumentsByStyle[style]}</p>
              )}
              <ul className="mt-2 space-y-1 text-sm text-muted">
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

      {Object.keys(guard).length > 0 && (
        <div className="border-t border-line pt-3">
          <h3 className="text-sm font-semibold text-ink">Cash Guard</h3>
          <dl className="mt-2 space-y-2 text-sm">
            {detailEntries(guard).map(({ key, value }) => (
              <div key={key} className="grid gap-1 sm:grid-cols-[9rem_1fr]">
                <dt className="font-mono text-[11px] uppercase tracking-wide text-muted">{key}</dt>
                <dd>
                  <DetailValue value={value} />
                </dd>
              </div>
            ))}
          </dl>
        </div>
      )}

      <div className="border-t border-line pt-3">
        <h3 className="text-sm font-semibold text-ink">
          Capital judge{decision.winnerStyle ? ` · ${decision.winnerStyle}` : ''}
        </h3>
        {decision.rationale && <p className="mt-1 text-sm text-muted">{decision.rationale}</p>}
        <ul className="mt-2 space-y-1 text-sm">
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

function PaperBook({ plannedShares }) {
  const entries = Object.entries(plannedShares ?? {})
  if (entries.length === 0) {
    return <p className="text-sm text-muted">No paper positions sized this run.</p>
  }
  return (
    <ul className="divide-y divide-line">
      {entries.map(([ticker, shares]) => (
        <li key={ticker} className="flex items-baseline justify-between py-3">
          <span className="font-mono text-lg font-bold text-ink">{ticker}</span>
          <span className="text-sm text-muted">{shares} shares</span>
        </li>
      ))}
    </ul>
  )
}

function ExecutionResults({ result }) {
  const trades =
    result.executedTrades && typeof result.executedTrades === 'object'
      ? Object.entries(result.executedTrades)
      : []
  return (
    <div>
      {trades.length === 0 ? (
        <p className="text-muted">No trades were executed within the allowance.</p>
      ) : (
        <ul className="divide-y divide-line">
          {trades.map(([ticker, detail]) => (
            <li key={ticker} className="flex flex-wrap items-baseline gap-x-4 py-3">
              <span className="font-mono text-lg font-bold text-ink">{ticker}</span>
              <span className="text-sm text-muted">
                {detail?.shares ?? '—'} shares @{' '}
                {typeof detail?.price === 'number' ? currency.format(detail.price) : '—'}
              </span>
              <span className="ml-auto text-sm font-semibold text-accent-deep">
                {typeof detail?.cost === 'number' ? currency.format(detail.cost) : '—'}
              </span>
            </li>
          ))}
        </ul>
      )}
      <dl className="mt-4 flex gap-8 border-t border-line pt-4 text-sm">
        <div>
          <dt className="text-muted">Total invested</dt>
          <dd className="font-semibold text-ink">
            {typeof result.totalInvested === 'number'
              ? currency.format(result.totalInvested)
              : '—'}
          </dd>
        </div>
        <div>
          <dt className="text-muted">Remaining allowance</dt>
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

function RunResultView({ result, mode }) {
  const steps = Array.isArray(result.agentTrace) ? result.agentTrace : []
  return (
    <div className="space-y-10">
      {(result.status === 'partial' || result.error) && (
        <p className="rounded-md bg-danger-soft px-4 py-3 text-sm text-danger">
          {result.status === 'partial' ? 'Partial run: ' : ''}
          {result.error || 'Run finished with warnings.'}
        </p>
      )}

      <section>
        <div className="flex flex-wrap items-baseline justify-between gap-2">
          <h2 className="font-display text-xl font-semibold text-ink">Spawned agents</h2>
          <p className="font-mono text-xs text-muted">
            run {result.runId ? String(result.runId).slice(0, 8) : '—'} · {result.status}
          </p>
        </div>
        <p className="mt-1 text-sm text-muted">
          Each tile is a supervisor spawn — LLM, market API, or hard rule gate.
        </p>
        <div className="mt-4">
          <PipelineRail steps={steps} running={false} mode={mode} />
        </div>
      </section>

      <section>
        <h2 className="font-display text-xl font-semibold text-ink">Call trace</h2>
        <p className="mt-1 text-sm text-muted">Expand a step to inspect the payload that agent returned.</p>
        <ol className="mt-4 space-y-2">
          {steps.length === 0 ? (
            <li className="text-sm text-muted">No agent steps recorded.</li>
          ) : (
            steps.map((step, index) => (
              <TraceStepRow
                key={`${step.step}-${step.agent}`}
                step={step}
                defaultOpen={index === steps.length - 1 || step.agent === 'capital_judge'}
              />
            ))
          )}
        </ol>
      </section>

      <section>
        <h2 className="font-display text-xl font-semibold text-ink">Bull / bear debate</h2>
        <div className="mt-3">
          <DebateBoard candidates={result.candidates} rejected={result.rejectedTickers} />
        </div>
      </section>

      <QuoteStrip quotes={result.quoteSnapshot} />

      <section>
        <h2 className="font-display text-xl font-semibold text-ink">Capital committee</h2>
        <div className="mt-3">
          <CapitalPanel result={result} />
        </div>
      </section>

      {mode === 'research' ? (
        <section>
          <h2 className="font-display text-xl font-semibold text-ink">Paper book</h2>
          <div className="mt-3">
            <PaperBook plannedShares={result.plannedShares} />
          </div>
        </section>
      ) : (
        <section>
          <h2 className="font-display text-xl font-semibold text-ink">Fills</h2>
          <div className="mt-3">
            <ExecutionResults result={result} />
          </div>
        </section>
      )}
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

  // Soft progress while the server runs: reveal expected stages on a timer so
  // the desk feels alive even without streaming.
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
      <h1 className="font-display text-2xl font-semibold text-ink">Agent desk</h1>
      <p className="mt-2 max-w-2xl text-sm text-muted">
        Watch the supervisor spawn specialists — news, bull/bear debate, stock judge, then a capital
        committee that fights over a $1,000 simulated book.
      </p>

      <div className="mt-6 flex gap-3">
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

      <div className="mt-8">
        {status === 'idle' && (
          <div className="space-y-4">
            <p className="text-sm text-muted">
              Requires <code className="font-mono">GROQ_API_KEY</code> and{' '}
              <code className="font-mono">FINNHUB_API_KEY</code> on the backend.
            </p>
            <PipelineRail steps={[]} running={false} mode="execute" />
          </div>
        )}

        {running && (
          <div className="space-y-5" aria-live="polite">
            <div className="rounded-md bg-accent-soft px-4 py-3 text-sm">
              <p className="font-medium text-accent-deep">
                Supervisor is spawning agents for{' '}
                {mode === 'research' ? 'research' : 'execution'}… (
                <ElapsedTimer startedAt={startedAt} />)
              </p>
              <p className="mt-1 text-muted">
                Live progress is estimated until the server returns the real call trace. Leaving this
                page does not cancel the run.
              </p>
            </div>
            <PipelineRail steps={liveTrace} running mode={mode} />
          </div>
        )}

        {status === 'error' && (
          <p aria-live="polite" className="rounded-md bg-danger-soft px-4 py-3 text-sm text-danger">
            {error}
          </p>
        )}

        {status === 'done' && result && <RunResultView result={result} mode={mode} />}
      </div>

      {history.length > 0 && (
        <section className="mt-12 border-t border-line pt-6">
          <h2 className="font-display text-xl font-semibold text-ink">Recent runs</h2>
          <p className="mt-1 text-sm text-muted">Open a run to inspect its full spawn trace.</p>
          <ul className="mt-3 divide-y divide-line text-sm">
            {history.map((run) => (
              <li key={run.runId}>
                <button
                  type="button"
                  onClick={() => openHistoryRun(run.runId)}
                  disabled={loadingHistoryId === run.runId}
                  className="flex w-full flex-wrap items-baseline justify-between gap-2 py-2.5 text-left transition-colors hover:text-accent-deep disabled:opacity-50"
                >
                  <span className="font-mono text-xs text-muted">{run.runId?.slice(0, 8)}</span>
                  <span className="text-ink">{run.status}</span>
                  <span className="text-muted">
                    {typeof run.totalInvested === 'number'
                      ? currency.format(run.totalInvested)
                      : '—'}{' '}
                    invested
                  </span>
                  <span className="text-xs text-accent">
                    {loadingHistoryId === run.runId ? 'Loading…' : 'View trace'}
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
