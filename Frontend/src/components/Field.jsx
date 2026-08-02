/** Labeled input with inline error, shared by the auth and post forms. */
export default function Field({ label, id, error, hint, ...inputProps }) {
  return (
    <div className="flex flex-col gap-1">
      <label htmlFor={id} className="text-sm font-medium text-ink">
        {label}
        {hint && <span className="ml-2 font-normal text-muted">{hint}</span>}
      </label>
      <input
        id={id}
        className={`rounded-md border bg-surface px-3 py-2 text-sm text-ink outline-none transition-colors placeholder:text-muted/60 focus:border-accent ${
          error ? 'border-danger' : 'border-line'
        }`}
        aria-invalid={Boolean(error)}
        aria-describedby={error ? `${id}-error` : undefined}
        {...inputProps}
      />
      {error && (
        <p id={`${id}-error`} aria-live="polite" className="text-sm text-danger">
          {error}
        </p>
      )}
    </div>
  )
}
