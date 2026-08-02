import { useEffect, useRef, useState } from 'react'
import { executeAgentTrades, listAgentRuns, runAgentResearch } from '../../api/trades'
import { useAuth } from '../../auth/useAuth'

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

function TraceTimeline({ steps }) {
  if (!Array.isArray(steps) || steps.length === 0) {
    return <p className="text-sm text-muted">No agent steps recorded.</p>
  }
  return (
    <ol className="space-y-3">
      {steps.map((step) => (
        <li key={`${step.step}-${step.agent}`} className="border-l-2 border-line pl-3">
          <div className="flex flex-wrap items-baseline gap-x-2">
            <span className="font-mono text-xs text-muted">#{step.step}</span>
            <span className="text-sm font-semibold text-ink">{step.agent}</span>
            <span className="text-xs uppercase tracking-wide text-muted">{step.status}</span>
          </div>
          <p className="mt-0.5 text-sm text-muted">{step.summary}</p>
        </li>
      ))}
    </ol>
  )
}

function CapitalPanel({ result }) {
  const proposals = result.allocatorProposals ?? {}
  const styles = Object.keys(proposals)
  const decision = result.capitalJudgeDecision ?? {}
  const approved = decision.approved ?? {}

  return (
    <div className="space-y-4">
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
              <ul className="mt-2 space-y-1 text-sm text-muted">
                {Object.entries(proposals[style] ?? {}).map(([ticker, amount]) => (
                  <li key={ticker} className="flex justify-between gap-2">
                    <span className="font-mono text-ink">{ticker}</span>
                    <span>{currency.format(amount)}</span>
                  </li>
                ))}
              </ul>
            </div>
          ))}
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
              <span className="text-muted">{currency.format(amount)}</span>
            </li>
          ))}
        </ul>
      </div>
    </div>
  )
}

function CandidatesList({ candidates }) {
  if (!Array.isArray(candidates) || candidates.length === 0) {
    return <p className="text-sm text-muted">No candidates advanced.</p>
  }
  return (
    <ul className="divide-y divide-line">
      {candidates.map((c) => (
        <li key={c.ticker} className="py-3">
          <div className="flex items-baseline justify-between gap-2">
            <span className="font-mono text-lg font-bold text-ink">{c.ticker}</span>
            <span className="text-sm text-muted">score {c.score ?? '—'}</span>
          </div>
          {c.thesis && <p className="mt-1 text-sm text-muted">{c.thesis}</p>}
        </li>
      ))}
    </ul>
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
  return (
    <div className="space-y-8">
      {(result.status === 'partial' || result.error) && (
        <p className="rounded-md bg-danger-soft px-4 py-3 text-sm text-danger">
          {result.status === 'partial' ? 'Partial run: ' : ''}
          {result.error || 'Run finished with warnings.'}
        </p>
      )}

      <section>
        <h2 className="font-display text-xl font-semibold text-ink">Trace</h2>
        <div className="mt-3">
          <TraceTimeline steps={result.agentTrace} />
        </div>
      </section>

      <section>
        <h2 className="font-display text-xl font-semibold text-ink">Candidates</h2>
        <div className="mt-3">
          <CandidatesList candidates={result.candidates} />
        </div>
      </section>

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
  const activeRun = useRef(0)

  useEffect(() => {
    listAgentRuns()
      .then(setHistory)
      .catch(() => setHistory([]))
  }, [status])

  async function run(nextMode) {
    const runId = ++activeRun.current
    setMode(nextMode)
    setStatus('running')
    setResult(null)
    setError('')
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

  const running = status === 'running'

  return (
    <section className="overflow-hidden rounded-2xl border border-line bg-surface px-5 py-6 shadow-[0_1px_0_rgba(20,24,22,0.04)] sm:px-6">
      <h1 className="font-display text-2xl font-semibold text-ink">Agent desk</h1>
      <p className="mt-2 max-w-lg text-sm text-muted">
        News scout, bull/bear debate, stock judge, then a capital committee that sizes a $1,000
        simulated book — with a full decision trace.
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
          <p className="text-sm text-muted">
            Requires <code className="font-mono">GROQ_API_KEY</code> and{' '}
            <code className="font-mono">FINNHUB_API_KEY</code> on the backend.
          </p>
        )}

        {running && (
          <div aria-live="polite" className="rounded-md bg-accent-soft px-4 py-3 text-sm">
            <p className="font-medium text-accent-deep">
              The agent is {mode === 'research' ? 'researching' : 'executing trades'}… (
              <ElapsedTimer startedAt={startedAt} />)
            </p>
            <p className="mt-1 text-muted">
              Groq is usually faster than a local model, but debate + capital still take a bit. Leaving
              this page does not cancel the server run.
            </p>
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
          <ul className="mt-3 divide-y divide-line text-sm">
            {history.map((run) => (
              <li key={run.runId} className="flex flex-wrap items-baseline justify-between gap-2 py-2">
                <span className="font-mono text-xs text-muted">{run.runId?.slice(0, 8)}</span>
                <span className="text-ink">{run.status}</span>
                <span className="text-muted">
                  {typeof run.totalInvested === 'number'
                    ? currency.format(run.totalInvested)
                    : '—'}{' '}
                  invested
                </span>
              </li>
            ))}
          </ul>
        </section>
      )}
    </section>
  )
}
