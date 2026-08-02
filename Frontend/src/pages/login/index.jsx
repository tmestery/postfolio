import { useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { login as loginRequest } from '../../api/auth'
import { useAuth } from '../../auth/useAuth'
import Field from '../../components/Field'

export default function LoginPage() {
  const { isAuthenticated, login } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ username: '', password: '' })
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  if (isAuthenticated) return <Navigate to="/" replace />

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      const session = await loginRequest(form)
      login(session)
      navigate('/')
    } catch (err) {
      setError(err.status === 400 ? 'Invalid username or password.' : err.message)
      setSubmitting(false)
    }
  }

  return (
    <section className="mx-auto max-w-sm py-10">
      <h1 className="font-display text-3xl font-semibold text-ink">Welcome back</h1>
      <p className="mt-1 text-sm text-muted">Log in to see your feed.</p>

      <form onSubmit={handleSubmit} noValidate className="mt-8 flex flex-col gap-4">
        <Field
          label="Username"
          id="username"
          type="text"
          autoComplete="username"
          value={form.username}
          onChange={(event) => setForm({ ...form, username: event.target.value })}
          required
        />
        <Field
          label="Password"
          id="password"
          type="password"
          autoComplete="current-password"
          value={form.password}
          onChange={(event) => setForm({ ...form, password: event.target.value })}
          required
        />

        {error && (
          <p aria-live="polite" className="rounded-md bg-danger-soft px-3 py-2 text-sm text-danger">
            {error}
          </p>
        )}

        <button
          type="submit"
          disabled={submitting || !form.username || !form.password}
          className="rounded-md bg-accent px-4 py-2.5 font-medium text-white transition-colors hover:bg-accent-deep disabled:opacity-50"
        >
          {submitting ? 'Logging in…' : 'Log in'}
        </button>
      </form>

      <p className="mt-6 text-sm text-muted">
        New here?{' '}
        <Link to="/signup" className="font-medium text-accent-deep hover:underline">
          Create an account
        </Link>
      </p>
    </section>
  )
}
