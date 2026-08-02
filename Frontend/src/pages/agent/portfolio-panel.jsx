const currency = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' })
const pct = new Intl.NumberFormat('en-US', { style: 'percent', minimumFractionDigits: 2, maximumFractionDigits: 2 })

function pnlTone(value) {
  if (typeof value !== 'number' || value === 0) return 'text-muted'
  return value > 0 ? 'text-accent-deep' : 'text-danger'
}

export function EquityChart({ points }) {
  const rows = Array.isArray(points) ? points.filter((p) => typeof p.equity === 'number') : []
  if (rows.length < 2) {
    return (
      <p className="flex h-36 items-center justify-center text-xs text-muted">
        Chart fills in after a few marks.
      </p>
    )
  }
  const width = 320
  const height = 120
  const pad = 8
  const xs = rows.map((_, i) => i)
  const ys = rows.map((p) => p.equity)
  const minY = Math.min(...ys)
  const maxY = Math.max(...ys)
  const spanY = maxY - minY || 1
  const coords = xs.map((x, i) => {
    const px = pad + (x / (rows.length - 1)) * (width - pad * 2)
    const py = height - pad - ((ys[i] - minY) / spanY) * (height - pad * 2)
    return `${px},${py}`
  })
  return (
    <svg viewBox={`0 0 ${width} ${height}`} className="h-36 w-full text-accent" aria-hidden>
      <polyline
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinejoin="round"
        points={coords.join(' ')}
      />
    </svg>
  )
}

export function PortfolioPanel({ portfolio, history, refreshing, onRefresh, onReset }) {
  if (!portfolio || typeof portfolio.equity !== 'number') {
    return null
  }
  const up = portfolio.totalPnl
  return (
    <section className="mb-5 overflow-hidden rounded-xl border border-line bg-paper/60">
      <div className="flex flex-wrap items-start justify-between gap-3 border-b border-line px-4 py-3">
        <div>
          <p className="text-xs font-semibold uppercase tracking-wide text-muted">House book</p>
          <p className="mt-1 font-display text-xl font-semibold text-ink">
            {currency.format(portfolio.equity)}
          </p>
          <p className={`text-sm font-medium ${pnlTone(up)}`}>
            {typeof up === 'number' ? currency.format(up) : '—'}
            {typeof portfolio.totalPnlPct === 'number' ? ` (${pct.format(portfolio.totalPnlPct)})` : ''}
          </p>
        </div>
        <dl className="grid grid-cols-2 gap-x-4 gap-y-1 text-xs sm:grid-cols-3">
          <div>
            <dt className="text-muted">Cash</dt>
            <dd className="font-semibold text-ink">{currency.format(portfolio.cash ?? 0)}</dd>
          </div>
          <div>
            <dt className="text-muted">Holdings</dt>
            <dd className="font-semibold text-ink">{currency.format(portfolio.holdingsValue ?? 0)}</dd>
          </div>
          <div>
            <dt className="text-muted">Feed</dt>
            <dd className="font-mono text-[11px] uppercase text-ink">{portfolio.dataFeed ?? 'iex'}</dd>
          </div>
        </dl>
        <div className="flex gap-2">
          <button
            type="button"
            onClick={onRefresh}
            disabled={refreshing}
            className="rounded-md border border-line px-3 py-1.5 text-xs font-medium text-ink hover:border-accent disabled:opacity-50"
          >
            {refreshing ? 'Refreshing…' : 'Refresh P&L'}
          </button>
          <button
            type="button"
            onClick={onReset}
            className="rounded-md border border-line px-3 py-1.5 text-xs font-medium text-muted hover:border-danger hover:text-danger"
          >
            Reset book
          </button>
        </div>
      </div>

      <div className="border-b border-line px-4 py-2">
        <EquityChart points={history} />
      </div>

      {Array.isArray(portfolio.positions) && portfolio.positions.length > 0 && (
        <div className="overflow-x-auto px-4 py-3">
          <table className="w-full min-w-[20rem] text-left text-xs">
            <thead className="text-muted">
              <tr>
                <th className="pb-2 font-medium">Ticker</th>
                <th className="pb-2 font-medium">Shares</th>
                <th className="pb-2 font-medium">Mark</th>
                <th className="pb-2 font-medium">U/P&L</th>
              </tr>
            </thead>
            <tbody>
              {portfolio.positions.map((row) => (
                <tr key={row.ticker} className="border-t border-line/80">
                  <td className="py-1.5 font-mono font-semibold text-ink">{row.ticker}</td>
                  <td className="py-1.5 text-muted">{row.shares}</td>
                  <td className="py-1.5 text-ink">
                    {typeof row.markPrice === 'number' ? currency.format(row.markPrice) : '—'}
                  </td>
                  <td className={`py-1.5 font-medium ${pnlTone(row.unrealizedPnl)}`}>
                    {typeof row.unrealizedPnl === 'number' ? currency.format(row.unrealizedPnl) : '—'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {portfolio.marksStale && (
        <p className="border-t border-line px-4 py-2 text-xs text-muted">
          Some marks are stale — Basic plan uses IEX snapshots (not full-market real time).
        </p>
      )}
    </section>
  )
}
