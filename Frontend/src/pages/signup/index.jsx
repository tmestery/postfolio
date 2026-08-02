import { useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { signup } from '../../api/auth'
import { useAuth } from '../../auth/useAuth'
import Field from '../../components/Field'

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

function validate(form) {
  const errors = {}
  if (!form.username.trim()) errors.username = 'Username is required.'
  if (!form.email.trim()) {
    errors.email = 'Email is required.'
  } else if (!EMAIL_PATTERN.test(form.email.trim())) {
    errors.email = 'Enter a valid email address.'
  }
  if (!form.password) {
    errors.password = 'Password is required.'
  } else if (form.password.length < 8) {
    errors.password = 'Password must be at least 8 characters.'
  }
  return errors
}

export default function SignupPage() {
  const { isAuthenticated, login } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({
    username: '',
    email: '',
    password: '',
    firstName: '',
    lastName: '',
  })
  const [fieldErrors, setFieldErrors] = useState({})
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  if (isAuthenticated) return <Navigate to="/" replace />

  function update(key) {
    return (event) => setForm({ ...form, [key]: event.target.value })
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    const errors = validate(form)
    setFieldErrors(errors)
    if (Object.keys(errors).length > 0) return

    setSubmitting(true)
    try {
      const session = await signup({
        username: form.username.trim(),
        email: form.email.trim(),
        password: form.password,
        firstName: form.firstName.trim(),
        lastName: form.lastName.trim(),
      })
      // Locked decision: auto-login after signup, straight to the feed.
      login(session)
      navigate('/')
    } catch (err) {
      setError(err.message)
      setSubmitting(false)
    }
  }

  return (
    <section className="mx-auto max-w-sm overflow-hidden rounded-2xl border border-line bg-surface px-5 py-7 shadow-[0_1px_0_rgba(20,24,22,0.04)] sm:px-6">
      <p className="font-display text-2xl font-semibold text-ink">Postfolio</p>
      <h1 className="mt-3 font-display text-xl font-semibold text-ink">Create your account</h1>
      <p className="mt-1 text-sm text-muted">Start sharing your trades in under a minute.</p>

      <form onSubmit={handleSubmit} noValidate className="mt-6 flex flex-col gap-4">
        <Field
          label="Username"
          id="username"
          type="text"
          autoComplete="username"
          value={form.username}
          onChange={update('username')}
          error={fieldErrors.username}
          required
        />
        <Field
          label="Email"
          id="email"
          type="email"
          autoComplete="email"
          value={form.email}
          onChange={update('email')}
          error={fieldErrors.email}
          required
        />
        <Field
          label="Password"
          id="password"
          type="password"
          autoComplete="new-password"
          hint="at least 8 characters"
          value={form.password}
          onChange={update('password')}
          error={fieldErrors.password}
          required
        />
        <div className="grid grid-cols-2 gap-3">
          <Field
            label="First name"
            id="firstName"
            type="text"
            autoComplete="given-name"
            hint="optional"
            value={form.firstName}
            onChange={update('firstName')}
          />
          <Field
            label="Last name"
            id="lastName"
            type="text"
            autoComplete="family-name"
            hint="optional"
            value={form.lastName}
            onChange={update('lastName')}
          />
        </div>

        {error && (
          <p aria-live="polite" className="rounded-md bg-danger-soft px-3 py-2 text-sm text-danger">
            {error}
          </p>
        )}

        <button
          type="submit"
          disabled={submitting}
          className="rounded-md bg-accent px-4 py-2.5 font-medium text-white transition-colors hover:bg-accent-deep disabled:opacity-50"
        >
          {submitting ? 'Creating account…' : 'Sign up'}
        </button>
      </form>

      <p className="mt-6 text-sm text-muted">
        Already have an account?{' '}
        <Link to="/login" className="font-medium text-accent-deep hover:underline">
          Log in
        </Link>
      </p>
    </section>
  )
}
