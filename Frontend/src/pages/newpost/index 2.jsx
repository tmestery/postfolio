import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { createPost } from '../../api/posts'
import { useAuth } from '../../auth/AuthContext'
import Field from '../../components/Field'

function todayISO() {
  return new Date().toISOString().slice(0, 10)
}

function validate(form) {
  const errors = {}
  if (!form.stock.trim()) errors.stock = 'Ticker is required.'
  const shares = Number(form.shares)
  if (!form.shares || Number.isNaN(shares) || shares <= 0) {
    errors.shares = 'Shares must be a number greater than 0.'
  }
  const amount = Number(form.investedAmount)
  if (!form.investedAmount || Number.isNaN(amount) || amount <= 0) {
    errors.investedAmount = 'Amount must be a number greater than 0.'
  }
  if (!form.dateInvested) {
    errors.dateInvested = 'Date is required.'
  } else if (form.dateInvested > todayISO()) {
    errors.dateInvested = 'Date cannot be in the future.'
  }
  return errors
}

export default function NewPostPage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({
    stock: '',
    shares: '',
    investedAmount: '',
    dateInvested: todayISO(),
  })
  const [fieldErrors, setFieldErrors] = useState({})
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

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
      await createPost({
        username: user.username,
        stock: form.stock.trim().toUpperCase(),
        shares: Number(form.shares),
        investedAmount: Number(form.investedAmount),
        dateInvested: form.dateInvested,
      })
      navigate('/')
    } catch (err) {
      setError(err.message)
      setSubmitting(false)
    }
  }

  return (
    <section className="mx-auto max-w-sm py-10">
      <h1 className="font-display text-3xl font-semibold text-ink">Share a trade</h1>
      <p className="mt-1 text-sm text-muted">Post an investment to your feed.</p>

      <form onSubmit={handleSubmit} noValidate className="mt-8 flex flex-col gap-4">
        <Field
          label="Ticker"
          id="stock"
          type="text"
          placeholder="AAPL"
          value={form.stock}
          onChange={(event) =>
            setForm({ ...form, stock: event.target.value.toUpperCase() })
          }
          error={fieldErrors.stock}
          required
        />
        <div className="grid grid-cols-2 gap-3">
          <Field
            label="Shares"
            id="shares"
            type="number"
            min="0"
            step="any"
            placeholder="10"
            value={form.shares}
            onChange={update('shares')}
            error={fieldErrors.shares}
            required
          />
          <Field
            label="Amount invested ($)"
            id="investedAmount"
            type="number"
            min="0"
            step="any"
            placeholder="1800"
            value={form.investedAmount}
            onChange={update('investedAmount')}
            error={fieldErrors.investedAmount}
            required
          />
        </div>
        <Field
          label="Date invested"
          id="dateInvested"
          type="date"
          max={todayISO()}
          value={form.dateInvested}
          onChange={update('dateInvested')}
          error={fieldErrors.dateInvested}
          required
        />

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
          {submitting ? 'Posting…' : 'Post trade'}
        </button>
      </form>
    </section>
  )
}
