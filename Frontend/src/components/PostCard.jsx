import { useState } from 'react'
import { Link } from 'react-router-dom'
import Avatar from './Avatar'
import { formatAbsoluteDate, formatRelativeTime } from '../utils/time'

const currency = new Intl.NumberFormat('en-US', {
  style: 'currency',
  currency: 'USD',
})

/** One investment post in the feed. Owner sees a two-step inline delete. */
export default function PostCard({ post, currentUsername, onDelete }) {
  const [confirming, setConfirming] = useState(false)
  const [deleting, setDeleting] = useState(false)
  const [error, setError] = useState('')

  const username = post.user?.username ?? 'unknown'
  const isOwner = Boolean(onDelete) && currentUsername && currentUsername === username
  const relative = formatRelativeTime(post.datePosted)
  const invested = formatAbsoluteDate(post.dateInvested)

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
    <article className="border-b border-line px-4 py-4 transition-colors hover:bg-accent-soft/30 sm:px-5">
      <div className="flex gap-3">
        <Link to={`/u/${username}`} aria-label={`Profile of ${username}`}>
          <Avatar name={username} />
        </Link>

        <div className="min-w-0 flex-1">
          <header className="flex flex-wrap items-baseline gap-x-2 gap-y-0.5">
            <Link to={`/u/${username}`} className="text-sm font-semibold text-ink hover:underline">
              {username}
            </Link>
            <span className="text-muted">·</span>
            {relative && (
              <time className="text-xs text-muted" dateTime={post.datePosted}>
                {relative}
              </time>
            )}
            {isOwner &&
              (confirming ? (
                <span className="ml-auto flex items-center gap-2 text-sm">
                  <button
                    type="button"
                    onClick={handleDelete}
                    disabled={deleting}
                    className="font-medium text-danger hover:underline disabled:opacity-50"
                  >
                    {deleting ? 'Deleting…' : 'Confirm'}
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
                  className="ml-auto text-sm text-muted transition-colors hover:text-danger"
                >
                  Delete
                </button>
              ))}
          </header>

          <p className="mt-1 text-sm leading-relaxed text-ink">
            Bought <span className="font-semibold">{post.stock}</span>
            {post.shares != null && (
              <>
                {' '}
                · {post.shares} {post.shares === 1 ? 'share' : 'shares'}
              </>
            )}
          </p>

          <div className="mt-3 overflow-hidden rounded-xl border border-line bg-paper/80">
            <div className="flex items-end justify-between gap-3 px-3.5 py-3">
              <div>
                <p className="font-mono text-xl font-bold tracking-wide text-ink">{post.stock}</p>
                <p className="mt-0.5 text-xs text-muted">
                  {currency.format(post.pricePerShare ?? 0)} / share
                  {invested ? ` · invested ${invested}` : ''}
                </p>
              </div>
              <p className="text-base font-semibold text-accent-deep">
                {currency.format(post.investedAmount ?? 0)}
              </p>
            </div>
          </div>

          {error && (
            <p aria-live="polite" className="mt-2 text-sm text-danger">
              {error}
            </p>
          )}
        </div>
      </div>
    </article>
  )
}
