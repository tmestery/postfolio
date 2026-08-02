import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../../auth/useAuth'
import { getFeed, searchPosts, deletePost } from '../../api/posts'
import PostCard from '../../components/PostCard'

function GuestHero() {
  return (
    <section className="flex flex-col items-start gap-6 py-16 sm:py-24">
      <h1 className="font-display text-5xl font-semibold leading-tight tracking-tight text-ink sm:text-6xl">
        Share your trades.
        <br />
        <span className="text-accent">Watch the market together.</span>
      </h1>
      <p className="max-w-md text-lg text-muted">
        Post your stock investments, follow what your connections are buying, and see what an AI
        agent trader picks each week.
      </p>
      <div className="flex gap-3">
        <Link
          to="/signup"
          className="rounded-md bg-accent px-5 py-2.5 font-medium text-white transition-colors hover:bg-accent-deep"
        >
          Get started
        </Link>
        <Link
          to="/login"
          className="rounded-md border border-line px-5 py-2.5 font-medium text-ink transition-colors hover:border-accent hover:text-accent-deep"
        >
          Log in
        </Link>
      </div>
    </section>
  )
}

function Feed({ username }) {
  const [posts, setPosts] = useState([])
  const [status, setStatus] = useState('loading') // loading | ready | error
  const [errorMessage, setErrorMessage] = useState('')
  const [query, setQuery] = useState('')
  const [searching, setSearching] = useState(false)

  const loadFeed = useCallback(async () => {
    setStatus('loading')
    setErrorMessage('')
    try {
      setPosts(await getFeed())
      setStatus('ready')
    } catch (err) {
      setErrorMessage(err.message)
      setStatus('error')
    }
  }, [])

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
    <section>
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <h1 className="font-display text-3xl font-semibold text-ink">Feed</h1>
        <form onSubmit={handleSearch} className="flex gap-2">
          <label htmlFor="ticker-search" className="sr-only">
            Search by ticker
          </label>
          <input
            id="ticker-search"
            type="search"
            placeholder="Search ticker (e.g. AAPL)"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            className="w-48 rounded-md border border-line bg-surface px-3 py-1.5 text-sm outline-none transition-colors placeholder:text-muted/60 focus:border-accent"
          />
          <button
            type="submit"
            disabled={searching}
            className="rounded-md border border-line px-3 py-1.5 text-sm font-medium text-ink transition-colors hover:border-accent disabled:opacity-50"
          >
            {searching ? 'Searching…' : 'Search'}
          </button>
        </form>
      </div>

      {status === 'loading' && <p className="py-12 text-center text-muted">Loading the feed…</p>}

      {status === 'error' && (
        <div className="py-12 text-center">
          <p aria-live="polite" className="text-danger">
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
        <div className="py-12 text-center">
          <p className="text-muted">No posts yet.</p>
          <Link
            to="/post/new"
            className="mt-3 inline-block rounded-md bg-accent px-4 py-2 text-sm font-medium text-white hover:bg-accent-deep"
          >
            Share your first trade
          </Link>
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
