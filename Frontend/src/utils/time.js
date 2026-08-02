/** Short relative time for a social feed (e.g. 3h, Yesterday, Mar 12). */
export function formatRelativeTime(value) {
  if (!value) return null
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return null

  const now = Date.now()
  const diffSec = Math.round((now - date.getTime()) / 1000)
  if (diffSec < 60) return 'just now'
  if (diffSec < 3600) return `${Math.floor(diffSec / 60)}m`
  if (diffSec < 86400) return `${Math.floor(diffSec / 3600)}h`
  if (diffSec < 172800) return 'Yesterday'
  if (diffSec < 604800) return `${Math.floor(diffSec / 86400)}d`

  return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' })
}

export function formatAbsoluteDate(value) {
  if (!value) return null
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return null
  return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
}
