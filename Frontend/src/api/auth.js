import { apiFetch } from './client'

/**
 * @returns {Promise<{id: number|null, username: string}>} created user
 */
export async function signup({ username, email, password, firstName, lastName }) {
  const body = { username, email, password, accountPublicStatus: true }
  if (firstName) body.firstName = firstName
  if (lastName) body.lastName = lastName
  const { data } = await apiFetch('/credentials/signup/', { method: 'POST', body })
  return { id: data?.id ?? null, username: data?.username ?? username }
}

/**
 * Login success is HTTP 202 with the username as plain text.
 * @returns {Promise<{id: null, username: string}>}
 */
export async function login({ username, password }) {
  const { data } = await apiFetch('/credentials/login/', {
    method: 'POST',
    body: { username, password },
  })
  return { id: null, username: typeof data === 'string' ? data : username }
}
