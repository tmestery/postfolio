export const SESSION_KEY = 'postfolio.session'

/**
 * Demo session (locked decision): `{ username, id }` in localStorage.
 * Not secure — acceptable for the demo only.
 */
export function readSession() {
  let raw
  try {
    raw = localStorage.getItem(SESSION_KEY)
  } catch {
    return null
  }
  if (!raw) return null
  try {
    const session = JSON.parse(raw)
    if (!session || typeof session.username !== 'string' || !session.username) {
      return null
    }
    return { username: session.username, id: session.id ?? null }
  } catch {
    return null
  }
}

export function writeSession({ username, id = null }) {
  const session = { username, id }
  localStorage.setItem(SESSION_KEY, JSON.stringify(session))
  return session
}

export function clearSession() {
  localStorage.removeItem(SESSION_KEY)
}
