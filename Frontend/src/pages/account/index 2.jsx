import { useEffect, useState } from 'react'
import { getAccountStatus, setAccountStatus } from '../../api/account'
import { useAuth } from '../../auth/AuthContext'

export default function AccountPage() {
  const { user } = useAuth()
  const [isPublic, setIsPublic] = useState(null)
  const [status, setStatus] = useState('loading') // loading | ready | error
  const [saving, setSaving] = useState(false)
  const [message, setMessage] = useState('')

  useEffect(() => {
    let ignore = false
    async function load() {
      try {
        const data = await getAccountStatus(user.username)
        if (!ignore) {
          setIsPublic(Boolean(data?.accountPublicStatus))
          setStatus('ready')
        }
      } catch (err) {
        if (!ignore) {
          setMessage(err.message)
          setStatus('error')
        }
      }
    }
    load()
    return () => {
      ignore = true
    }
  }, [user.username])

  async function handleToggle() {
    setSaving(true)
    setMessage('')
    try {
      const data = await setAccountStatus({
        username: user.username,
        accountPublic: !isPublic,
      })
      setIsPublic(Boolean(data?.accountPublicStatus))
    } catch (err) {
      setMessage(err.message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <section className="mx-auto max-w-sm py-10">
      <h1 className="font-display text-3xl font-semibold text-ink">Account</h1>
      <p className="mt-1 text-sm text-muted">
        Signed in as <span className="font-medium text-ink">{user.username}</span>
      </p>

      <div className="mt-8 border-t border-line pt-6">
        {status === 'loading' && <p className="text-sm text-muted">Loading account settings…</p>}

        {status === 'error' && (
          <p aria-live="polite" className="text-sm text-danger">
            {message}
          </p>
        )}

        {status === 'ready' && (
          <div className="flex items-start justify-between gap-4">
            <div>
              <p className="font-medium text-ink">Public profile</p>
              <p className="mt-1 text-sm text-muted">
                {isPublic
                  ? 'Your trades appear in the shared feed.'
                  : 'Your trades are hidden from the feed.'}
              </p>
            </div>
            <button
              type="button"
              role="switch"
              aria-checked={isPublic}
              onClick={handleToggle}
              disabled={saving}
              className={`relative h-6 w-11 shrink-0 rounded-full transition-colors disabled:opacity-50 ${
                isPublic ? 'bg-accent' : 'bg-line'
              }`}
            >
              <span className="sr-only">Toggle public profile</span>
              <span
                aria-hidden
                className={`absolute top-0.5 h-5 w-5 rounded-full bg-white shadow transition-transform ${
                  isPublic ? 'translate-x-[1.375rem]' : 'translate-x-0.5'
                }`}
              />
            </button>
          </div>
        )}

        {status === 'ready' && message && (
          <p aria-live="polite" className="mt-3 text-sm text-danger">
            {message}
          </p>
        )}
      </div>
    </section>
  )
}
