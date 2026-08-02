import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../../auth/useAuth'
import {
  listNotifications,
  markNotificationsRead,
} from '../../api/notifications'
import { acceptFollowRequest, declineFollowRequest } from '../../api/social'
import Avatar from '../../components/Avatar'

export default function NotificationsPage() {
  const { user } = useAuth()
  const [items, setItems] = useState([])
  const [status, setStatus] = useState('loading')
  const [error, setError] = useState('')
  const [busyId, setBusyId] = useState(null)

  const load = useCallback(async () => {
    setStatus('loading')
    setError('')
    try {
      setItems(await listNotifications(user.username))
      setStatus('ready')
    } catch (err) {
      setError(err.message)
      setStatus('error')
    }
  }, [user.username])

  useEffect(() => {
    load()
  }, [load])

  async function markAll() {
    await markNotificationsRead({ username: user.username, all: true })
    await load()
  }

  async function openItem(item) {
    if (!item.read) {
      await markNotificationsRead({ username: user.username, ids: [item.id] })
    }
  }

  async function accept(item) {
    setBusyId(item.id)
    try {
      await acceptFollowRequest({
        username: user.username,
        requesterUsername: item.actorUsername,
      })
      await markNotificationsRead({ username: user.username, ids: [item.id] })
      await load()
    } catch (err) {
      setError(err.message)
    } finally {
      setBusyId(null)
    }
  }

  async function decline(item) {
    setBusyId(item.id)
    try {
      await declineFollowRequest({
        username: user.username,
        requesterUsername: item.actorUsername,
      })
      await load()
    } catch (err) {
      setError(err.message)
    } finally {
      setBusyId(null)
    }
  }

  return (
    <section className="overflow-hidden rounded-2xl border border-line bg-surface shadow-[0_1px_0_rgba(20,24,22,0.04)]">
      <div className="flex items-center justify-between gap-3 border-b border-line px-5 py-4">
        <h1 className="font-display text-xl font-semibold text-ink">Notifications</h1>
        <button
          type="button"
          onClick={markAll}
          className="text-sm font-medium text-accent-deep hover:underline"
        >
          Mark all read
        </button>
      </div>

      {error && (
        <p aria-live="polite" className="px-5 py-3 text-sm text-danger">
          {error}
        </p>
      )}
      {status === 'loading' && <p className="px-5 py-10 text-center text-sm text-muted">Loading…</p>}
      {status === 'ready' && items.length === 0 && (
        <p className="px-5 py-10 text-center text-sm text-muted">You are all caught up.</p>
      )}
      {status === 'ready' &&
        items.map((item) => (
          <article
            key={item.id}
            className={`flex gap-3 border-b border-line px-5 py-3 last:border-b-0 ${
              item.read ? '' : 'bg-accent-soft/40'
            }`}
          >
            <Avatar name={item.actorUsername || '?'} />
            <div className="min-w-0 flex-1">
              <p className="text-sm text-ink">{item.message}</p>
              <p className="mt-0.5 text-xs text-muted">{item.createdAt?.replace('T', ' ').slice(0, 16)}</p>
              {item.type === 'follow_request' ? (
                <div className="mt-2 flex gap-2">
                  <button
                    type="button"
                    disabled={busyId === item.id}
                    onClick={() => accept(item)}
                    className="rounded-md bg-accent px-3 py-1 text-sm font-medium text-white hover:bg-accent-deep disabled:opacity-50"
                  >
                    Accept
                  </button>
                  <button
                    type="button"
                    disabled={busyId === item.id}
                    onClick={() => decline(item)}
                    className="rounded-md border border-line px-3 py-1 text-sm font-medium hover:border-danger hover:text-danger disabled:opacity-50"
                  >
                    Decline
                  </button>
                </div>
              ) : (
                <Link
                  to={item.actorUsername ? `/u/${item.actorUsername}` : '/'}
                  onClick={() => openItem(item)}
                  className="mt-1 inline-block text-sm font-medium text-accent-deep hover:underline"
                >
                  View
                </Link>
              )}
            </div>
          </article>
        ))}
    </section>
  )
}
