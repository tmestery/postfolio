import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../../auth/useAuth'
import { getFeed, searchPosts, deletePost } from '../../api/posts'
import PostCard from '../../components/PostCard'
import Avatar from '../../components/Avatar'

function GuestHero() {
  return (
    <section className="flex flex-col items-start gap-6 py-14 sm:py-20">
      <p className="animate-rise font-display text-4xl font-semibold tracking-tight text-ink sm:text-5xl">
        Postfolio
      </p>
      <h1 className="animate-rise-delay max-w-lg font-display text-3xl font-semibold leading-tight tracking-tight text-ink sm:text-4xl">
        Share your trades.
        <br />
        <span className="text-accent">Watch the market together.</span>
      </h1>
      <p className="animate-rise-delay max-w-md text-base text-muted sm:text-lg">
        Follow investors you care about, get notified when they post, and peek at what the agent desk
        is paper-trading.
      </p>
      <div className="animate-rise-delay flex gap-3">
        <Link
          to="/signup"
          className="rounded-md bg-accent px-5 py-2.5 font-medium text-white transition-colors hover:bg-accent-deep"
        >
          Join the feed
        </Link>
        <Link
          to="/login"
          className="rounded-md border border-line bg-surface px-5 py-2.5 font-medium text-ink transition-colors hover:border-accent hover:text-accent-deep"
        >
          Log in
        </Link>
      </div>
    </section>
  )
}

function ComposePrompt({ username }) {
  return (
    <Link
      to="/post/new"
      className="flex items-center gap-3 border-b border-line px-4 py-3.5 transition-colors hover:bg-accent-soft/40 sm:px-5"
    >
      <Avatar name={username} />
      <span className="flex-1 rounded-full border border-line bg-paper px-4 py-2.5 text-sm text-muted">
        What did you buy?
      </span>
      <span className="rounded-md bg-accent px-3 py-2 text-sm font-medium text-white">Post</span>
    </Link>
  )
}

function Feed({ username }) {
  const [tab, setTab] = useState('following')
  const [posts, setPosts] = useState([])
  const [status, setStatus] = useState('loading')
  const [errorMessage, setErrorMessage] = useState('')
  const [query, setQuery] = useState('')
  const [searching, setSearching] = useState(false)

  const loadFeed = useCallback(async () => {
    setStatus('loading')
    setErrorMessage('')
    try {
      setPosts(await getFeed({ username, mode: tab }))
      setStatus('ready')
    } catch (err) {
      setErrorMessage(err.message)
      setStatus('error')
    }
  }, [username, tab])

  useEffect(() => {
    loadFeed()
  }, [loadFeed])

  async function handleSearch(event) {
    event.preventDefault()
    const ticker = query.trim()
    if (!ticker) {
      loadFeed()
      return
    }
    setSearching(true)
    setErrorMessage('')
    setTab('discover')
    try {
      setPosts(await searchPosts(ticker))
      setStatus('ready')
    } catch (err) {
      setErrorMessage(err.message)
      setStatus('error')
    } finally {
      setSearching(false)
    }
  }

  async function handleDelete(postId) {
    await deletePost({ postId, username })
    setPosts((current) => current.filter((post) => post.id !== postId))
  }

  return (
    <section className="overflow-hidden rounded-2xl border border-line bg-surface shadow-[0_1px_0_rgba(20,24,22,0.04)]">
      <div className="flex border-b border-line">
        {['following', 'discover'].map((name) => (
          <button
            key={name}
            type="button"
            onClick={() => setTab(name)}
            className={`flex-1 px-3 py-3 text-sm font-semibold capitalize transition-colors ${
              tab === name
                ? 'border-b-2 border-accent text-ink'
                : 'text-muted hover:bg-accent-soft/30 hover:text-ink'
            }`}
          >
            {name}
          </button>
        ))}
      </div>

      <div className="flex items-center justify-between gap-3 border-b border-line px-4 py-3 sm:px-5">
        <Link to="/people" className="text-sm font-medium text-accent-deep hover:underline">
          Find people
        </Link>
        <form onSubmit={handleSearch} className="flex min-w-0 flex-1 justify-end gap-2 sm:max-w-xs">
          <label htmlFor="ticker-search" className="sr-only">
            Search by ticker
          </label>
          <input
            id="ticker-search"
            type="search"
            placeholder="Search ticker"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            className="w-full min-w-0 rounded-full border border-line bg-paper px-3.5 py-1.5 text-sm outline-none transition-colors placeholder:text-muted/70 focus:border-accent"
          />
          <button
            type="submit"
            disabled={searching}
            className="shrink-0 rounded-full border border-line px-3 py-1.5 text-sm font-medium text-ink transition-colors hover:border-accent disabled:opacity-50"
          >
            {searching ? '…' : 'Go'}
          </button>
        </form>
      </div>

      <ComposePrompt username={username} />

      {status === 'loading' && <p className="px-4 py-14 text-center text-sm text-muted">Loading the feed…</p>}

      {status === 'error' && (
        <div className="px-4 py-14 text-center">
          <p aria-live="polite" className="text-sm text-danger">
            {errorMessage}
          </p>
          <button
            type="button"
            onClick={loadFeed}
            className="mt-3 rounded-md border border-line px-4 py-1.5 text-sm font-medium hover:border-accent"
          >
            Try again
          </button>
        </div>
      )}

      {status === 'ready' && posts.length === 0 && (
        <div className="px-4 py-14 text-center">
          <p className="text-sm text-muted">
            {tab === 'following'
              ? 'Your following feed is quiet — find people to connect with.'
              : 'No public posts yet.'}
          </p>
          <div className="mt-4 flex justify-center gap-3">
            {tab === 'following' && (
              <Link
                to="/people"
                className="rounded-md border border-line px-4 py-2 text-sm font-medium hover:border-accent"
              >
                Find people
              </Link>
            )}
            <Link
              to="/post/new"
              className="rounded-md bg-accent px-4 py-2 text-sm font-medium text-white hover:bg-accent-deep"
            >
              Share a trade
            </Link>
          </div>
        </div>
      )}

      {status === 'ready' &&
        posts.map((post) => (
          <PostCard
            key={post.id}
            post={post}
            currentUsername={username}
            onDelete={handleDelete}
          />
        ))}
    </section>
  )
}

export default function HomePage() {
  const { user, isAuthenticated } = useAuth()
  return isAuthenticated ? <Feed username={user.username} /> : <GuestHero />
}
