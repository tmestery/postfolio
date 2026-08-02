/** Simple letter avatar used in the nav and feed. */
export default function Avatar({ name = '?', size = 'md' }) {
  const initial = (name?.trim()?.charAt(0) || '?').toUpperCase()
  const sizeClass = size === 'lg' ? 'h-11 w-11 text-base' : size === 'sm' ? 'h-7 w-7 text-xs' : 'h-9 w-9 text-sm'

  return (
    <span
      aria-hidden
      className={`inline-flex shrink-0 items-center justify-center rounded-full bg-accent-soft font-display font-semibold text-accent-deep ${sizeClass}`}
    >
      {initial}
    </span>
  )
}
