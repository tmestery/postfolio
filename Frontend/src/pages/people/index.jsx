import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../../auth/useAuth'
import { searchUsers } from '../../api/users'
import { followUser, unfollowUser } from '../../api/social'
import Avatar from '../../components/Avatar'

export default function PeoplePage() {
  const { user } = useAuth()
  const [query, setQuery] = useState('')
  const [results, setResults] = useState([])
  const [status, setStatus] = useState('idle')
  const [error, setError] = useState('')
  const [busyUser, setBusyUser] = useState('')

  async function handleSearch(event) {
    event.preventDefault()
    const q = query.trim()
    if (!q) {
      setResults([])
      setStatus('idle')
      return
    }
    setStatus('loading')
    setError('')
    try {
      setResults(await searchUsers(q, user.username))
      setStatus('ready')
    } catch (err) {
      setError(err.message)
      setStatus('error')
    }
  }

  async function toggleFollow(target) {
    setBusyUser(target.username)
    try {
      if (target.viewerRelationship === 'accepted' || target.viewerRelationship === 'pending') {
        await unfollowUser({ username: user.username, targetUsername: target.username })
      } else {
        await followUser({ username: user.username, targetUsername: target.username })
      }
      setResults(await searchUsers(query.trim(), user.username))
    } catch (err) {
      setError(err.message)
    } finally {
      setBusyUser('')
    }
  }

  return (
    <section className="overflow-hidden rounded-2xl border border-line bg-surface shadow-[0_1px_0_rgba(20,24,22,0.04)]">
      <div className="border-b border-line px-5 py-4">
        <h1 className="font-display text-xl font-semibold text-ink">Find people</h1>
        <form onSubmit={handleSearch} className="mt-3 flex gap-2">
          <label htmlFor="people-search" className="sr-only">
            Search usernames
          </label>
          <input
            id="people-search"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search username"
            className="min-w-0 flex-1 rounded-full border border-line bg-paper px-3.5 py-2 text-sm outline-none focus:border-accent"
          />
          <button
            type="submit"
            className="rounded-full bg-accent px-4 py-2 text-sm font-medium text-white hover:bg-accent-deep"
          >
            Search
          </button>
        </form>
      </div>

      {error && (
        <p aria-live="polite" className="px-5 py-3 text-sm text-danger">
          {error}
        </p>
      )}
      {status === 'loading' && <p className="px-5 py-10 text-center text-sm text-muted">Searching…</p>}
      {status === 'idle' && (
        <p className="px-5 py-10 text-center text-sm text-muted">Search for a username to connect.</p>
      )}
      {status === 'ready' && results.length === 0 && (
        <p className="px-5 py-10 text-center text-sm text-muted">No users matched.</p>
      )}
      {status === 'ready' &&
        results.map((person) => {
          const isSelf = person.username === user.username
          let label = person.accountPublicStatus ? 'Follow' : 'Request'
          if (person.viewerRelationship === 'pending') label = 'Requested'
          if (person.viewerRelationship === 'accepted') label = 'Following'
          return (
            <div
              key={person.username}
              className="flex items-center gap-3 border-b border-line px-5 py-3 last:border-b-0"
            >
              <Avatar name={person.username} />
              <div className="min-w-0 flex-1">
                <Link to={`/u/${person.username}`} className="font-semibold text-ink hover:underline">
                  @{person.username}
                </Link>
                {!person.accountPublicStatus && (
                  <p className="text-xs text-muted">Private account</p>
                )}
              </div>
              {!isSelf && (
                <button
                  type="button"
                  disabled={busyUser === person.username}
                  onClick={() => toggleFollow(person)}
                  className="rounded-md border border-line px-3 py-1.5 text-sm font-medium hover:border-accent disabled:opacity-50"
                >
                  {label}
                </button>
              )}
            </div>
          )
        })}
    </section>
  )
}
