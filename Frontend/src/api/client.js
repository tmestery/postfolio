import { SERVER_URL } from './constants'

export class ApiError extends Error {
  constructor(message, status) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

/**
 * Thin fetch wrapper for the Postfolio API.
 *
 * - Prefixes SERVER_URL and appends query params.
 * - Parses JSON when possible, otherwise returns raw text
 *   (login success is `202` + plain-text username).
 * - Throws ApiError with the server's `{error}` message on non-2xx.
 *
 * @returns {Promise<{status: number, data: any}>}
 */
export async function apiFetch(path, { method = 'GET', body, params } = {}) {
  let url = `${SERVER_URL}${path}`
  if (params) {
    const search = new URLSearchParams(params).toString()
    if (search) url += `?${search}`
  }

  let response
  try {
    response = await fetch(url, {
      method,
      headers: body !== undefined ? { 'Content-Type': 'application/json' } : undefined,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    })
  } catch {
    throw new ApiError('Cannot reach the server. Is the backend running?', 0)
  }

  const text = await response.text()
  let data = null
  if (text) {
    try {
      data = JSON.parse(text)
    } catch {
      data = text
    }
  }

  if (!response.ok) {
    const message =
      data && typeof data === 'object' && data.error
        ? data.error
        : typeof data === 'string' && data
          ? data
          : `Request failed (${response.status})`
    throw new ApiError(message, response.status)
  }

  return { status: response.status, data }
}
