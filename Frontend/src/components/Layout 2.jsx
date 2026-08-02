import { Link, NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

function navLinkClass({ isActive }) {
  return `px-3 py-1.5 rounded-md text-sm font-medium transition-colors ${
    isActive ? 'bg-accent-soft text-accent-deep' : 'text-muted hover:text-ink'
  }`
}

export default function Layout({ children }) {
  const { user, isAuthenticated, logout } = useAuth()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate('/')
  }

  return (
    <div className="min-h-screen flex flex-col">
      <header className="sticky top-0 z-10 border-b border-line bg-paper/95 backdrop-blur">
        <div className="mx-auto flex h-14 max-w-3xl items-center justify-between px-4">
          <Link to="/" className="font-display text-xl font-semibold tracking-tight text-ink">
            Postfolio
          </Link>

          <nav className="flex items-center gap-1">
            {isAuthenticated ? (
              <>
                <NavLink to="/" end className={navLinkClass}>
                  Feed
                </NavLink>
                <NavLink to="/agent" className={navLinkClass}>
                  Agent
                </NavLink>
                <NavLink to="/account" className={navLinkClass}>
                  {user.username}
                </NavLink>
                <Link
                  to="/post/new"
                  className="ml-2 rounded-md bg-accent px-3 py-1.5 text-sm font-medium text-white transition-colors hover:bg-accent-deep"
                >
                  New post
                </Link>
                <button
                  type="button"
                  onClick={handleLogout}
                  className="ml-1 px-2 py-1.5 text-sm text-muted transition-colors hover:text-danger"
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
                  className="ml-2 rounded-md bg-accent px-3 py-1.5 text-sm font-medium text-white transition-colors hover:bg-accent-deep"
                >
                  Sign up
                </Link>
              </>
            )}
          </nav>
        </div>
      </header>

      <main className="mx-auto w-full max-w-3xl flex-1 px-4 py-8">{children}</main>

      <footer className="border-t border-line py-6">
        <p className="text-center text-xs text-muted">
          Postfolio — simulated trades only. Not financial advice.
        </p>
      </footer>
    </div>
  )
}
