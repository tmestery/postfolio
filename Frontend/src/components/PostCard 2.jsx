import { useState } from 'react'

const currency = new Intl.NumberFormat('en-US', {
  style: 'currency',
  currency: 'USD',
})

function formatDate(value) {
  if (!value) return null
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return null
  return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
}

/** One investment post in the feed. Owner sees a two-step inline delete. */
export default function PostCard({ post, currentUsername, onDelete }) {
  const [confirming, setConfirming] = useState(false)
  const [deleting, setDeleting] = useState(false)
  const [error, setError] = useState('')

  const username = post.user?.username ?? 'unknown'
  const isOwner = currentUsername && currentUsername === username
  const posted = formatDate(post.datePosted)
  const invested = formatDate(post.dateInvested)

  async function handleDelete() {
    setDeleting(true)
    setError('')
    try {
      await onDelete(post.id)
    } catch (err) {
      setError(err.message)
      setDeleting(false)
      setConfirming(false)
    }
  }

  return (
    <article className="border-b border-line py-5 first:pt-0">
      <div className="flex items-center justify-between gap-3">
        <div className="flex items-center gap-2.5">
          <span
            aria-hidden
            className="flex h-8 w-8 items-center justify-center rounded-full bg-accent-soft font-display text-sm font-semibold text-accent-deep"
          >
            {username.charAt(0).toUpperCase()}
          </span>
          <div>
            <p className="text-sm font-semibold text-ink">{username}</p>
            {posted && <p className="text-xs text-muted">{posted}</p>}
          </div>
        </div>

        {isOwner &&
          (confirming ? (
            <span className="flex items-center gap-2 text-sm">
              <button
                type="button"
                onClick={handleDelete}
                disabled={deleting}
                className="font-medium text-danger hover:underline disabled:opacity-50"
              >
                {deleting ? 'Deleting…' : 'Confirm delete'}
              </button>
              <button
                type="button"
                onClick={() => setConfirming(false)}
                disabled={deleting}
                className="text-muted hover:text-ink"
              >
                Cancel
              </button>
            </span>
          ) : (
            <button
              type="button"
              onClick={() => setConfirming(true)}
              className="text-sm text-muted transition-colors hover:text-danger"
            >
              Delete
            </button>
          ))}
      </div>

      <div className="mt-3 flex flex-wrap items-baseline gap-x-4 gap-y-1">
        <span className="font-mono text-lg font-bold tracking-wide text-ink">{post.stock}</span>
        <span className="text-sm text-muted">
          {post.shares} {post.shares === 1 ? 'share' : 'shares'} ·{' '}
          {currency.format(post.pricePerShare ?? 0)}/share
        </span>
        <span className="ml-auto text-sm font-semibold text-accent-deep">
          {currency.format(post.investedAmount ?? 0)}
        </span>
      </div>

      {invested && <p className="mt-1 text-xs text-muted">Invested {invested}</p>}
      {error && (
        <p aria-live="polite" className="mt-2 text-sm text-danger">
          {error}
        </p>
      )}
    </article>
  )
}
