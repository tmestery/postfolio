import { Link, NavLink, useNavigate } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { useAuth } from '../auth/useAuth'
import { unreadNotificationCount } from '../api/notifications'
import Avatar from './Avatar'

function navLinkClass({ isActive }) {
  return [
    'rounded-md px-3 py-2 text-sm font-medium transition-colors',
    isActive ? 'bg-accent-soft text-accent-deep' : 'text-muted hover:bg-surface hover:text-ink',
  ].join(' ')
}

export default function Layout({ children }) {
  const { user, isAuthenticated, logout } = useAuth()
  const navigate = useNavigate()
  const [unread, setUnread] = useState(0)

  useEffect(() => {
    if (!isAuthenticated || !user?.username) return undefined
    let ignore = false
    async function poll() {
      try {
        const count = await unreadNotificationCount(user.username)
        if (!ignore) setUnread(count)
      } catch {
        if (!ignore) setUnread(0)
      }
    }
    poll()
    const id = setInterval(poll, 30000)
    return () => {
      ignore = true
      clearInterval(id)
    }
  }, [isAuthenticated, user?.username])

  const badgeCount = isAuthenticated ? unread : 0

  function handleLogout() {
    logout()
    navigate('/')
  }

  return (
    <div className="flex min-h-screen flex-col">
      <header className="sticky top-0 z-20 border-b border-line/80 bg-paper/85 backdrop-blur-md">
        <div className="mx-auto flex h-14 max-w-xl items-center justify-between gap-3 px-4 sm:max-w-2xl">
          <Link to="/" className="flex items-center gap-2.5">
            <span
              aria-hidden
              className="flex h-8 w-8 items-center justify-center rounded-lg bg-accent font-display text-sm font-bold text-white"
            >
              P
            </span>
            <span className="font-display text-xl font-semibold tracking-tight text-ink">
              Postfolio
            </span>
          </Link>

          <nav className="flex items-center gap-0.5">
            {isAuthenticated ? (
              <>
                <NavLink to="/" end className={navLinkClass}>
                  Home
                </NavLink>
                <NavLink to="/people" className={navLinkClass}>
                  People
                </NavLink>
                <NavLink to="/notifications" className={navLinkClass}>
                  <span className="inline-flex items-center gap-1">
                    Alerts
                    {badgeCount > 0 && (
                      <span className="rounded-md bg-accent px-1.5 text-[11px] font-semibold text-white">
                        {badgeCount > 99 ? '99+' : badgeCount}
                      </span>
                    )}
                  </span>
                </NavLink>
                <NavLink to="/agent" className={navLinkClass}>
                  Agent
                </NavLink>
                <Link
                  to="/post/new"
                  className="ml-1 hidden rounded-md bg-accent px-3 py-2 text-sm font-medium text-white transition-colors hover:bg-accent-deep sm:inline-flex"
                >
                  Post
                </Link>
                <NavLink
                  to="/account"
                  className={({ isActive }) =>
                    `ml-1 rounded-full p-0.5 transition ring-offset-2 ring-offset-paper ${
                      isActive ? 'ring-2 ring-accent' : 'hover:ring-2 hover:ring-line'
                    }`
                  }
                  aria-label={`Account for ${user.username}`}
                >
                  <Avatar name={user.username} size="sm" />
                </NavLink>
                <button
                  type="button"
                  onClick={handleLogout}
                  className="ml-1 rounded-md px-2 py-2 text-sm text-muted transition-colors hover:text-danger"
                >
                  Log out
                </button>
              </>
            ) : (
              <>
                <NavLink to="/login" className={navLinkClass}>
                  Log in
                </NavLink>
                <Link
                  to="/signup"
                  className="ml-1 rounded-md bg-accent px-3 py-2 text-sm font-medium text-white transition-colors hover:bg-accent-deep"
                >
                  Sign up
                </Link>
              </>
            )}
          </nav>
        </div>
      </header>

      <main className="mx-auto w-full max-w-xl flex-1 px-3 py-4 sm:max-w-2xl sm:px-4 sm:py-6">
        {children}
      </main>

      {isAuthenticated && (
        <Link
          to="/post/new"
          className="fixed bottom-5 right-5 z-20 flex h-12 w-12 items-center justify-center rounded-full bg-accent text-xl font-semibold text-white shadow-md transition hover:bg-accent-deep sm:hidden"
          aria-label="Create a new post"
        >
          +
        </Link>
      )}

      <footer className="border-t border-line/70 py-5">
        <p className="text-center text-xs text-muted">
          Postfolio — simulated trades only. Not financial advice.
        </p>
      </footer>
    </div>
  )
}
