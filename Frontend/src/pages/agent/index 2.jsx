import { useEffect, useRef, useState } from 'react'
import { runAgentResearch, executeAgentTrades } from '../../api/trades'

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

function ResearchResults({ picks }) {
  const entries = Object.entries(picks)
  if (entries.length === 0) {
    return <p className="text-muted">The agent returned no picks this run.</p>
  }
  return (
    <ul className="divide-y divide-line">
      {entries.map(([ticker, shares]) => (
        <li key={ticker} className="flex items-baseline justify-between py-3">
          <span className="font-mono text-lg font-bold text-ink">{ticker}</span>
          <span className="text-sm text-muted">
            {typeof shares === 'number' ? shares : String(shares)} shares
          </span>
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

export default function AgentPage() {
  const [mode, setMode] = useState(null) // 'research' | 'execute'
  const [status, setStatus] = useState('idle') // idle | running | done | error
  const [result, setResult] = useState(null)
  const [error, setError] = useState('')
  const [startedAt, setStartedAt] = useState(null)
  const activeRun = useRef(0)

  async function run(nextMode) {
    const runId = ++activeRun.current
    setMode(nextMode)
    setStatus('running')
    setResult(null)
    setError('')
    setStartedAt(Date.now())
    try {
      const data = nextMode === 'research' ? await runAgentResearch() : await executeAgentTrades()
      if (activeRun.current !== runId) return
      if (data?.error) {
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
    <section className="py-4">
      <h1 className="font-display text-3xl font-semibold text-ink">Agent trader</h1>
      <p className="mt-2 max-w-lg text-sm text-muted">
        A local LLM reads the latest market headlines, picks stocks, and simulates trades within a
        $1,000 allowance. Runs take a while — the model works through 75 headlines each time.
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
            Requires the backend to have Ollama running and a Finnhub API key configured.
          </p>
        )}

        {running && (
          <div aria-live="polite" className="rounded-md bg-accent-soft px-4 py-3 text-sm">
            <p className="font-medium text-accent-deep">
              The agent is {mode === 'research' ? 'researching' : 'executing trades'}… (
              <ElapsedTimer startedAt={startedAt} />)
            </p>
            <p className="mt-1 text-muted">
              This can take a minute or more with a local model. Leaving this page cancels nothing —
              the run finishes on the server.
            </p>
          </div>
        )}

        {status === 'error' && (
          <p aria-live="polite" className="rounded-md bg-danger-soft px-4 py-3 text-sm text-danger">
            {error}
          </p>
        )}

        {status === 'done' && mode === 'research' && <ResearchResults picks={result ?? {}} />}
        {status === 'done' && mode === 'execute' && <ExecutionResults result={result ?? {}} />}
      </div>
    </section>
  )
}
