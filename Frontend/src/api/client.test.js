import { afterEach, describe, expect, it, vi } from 'vitest'
import { apiFetch, ApiError } from './client'

function mockFetchOnce(response) {
  const fetchMock = vi.fn().mockResolvedValue(response)
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

function textResponse({ ok = true, status = 200, body = '' }) {
  return { ok, status, text: () => Promise.resolve(body) }
}

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('apiFetch', () => {
  it('parses JSON bodies and returns status + data', async () => {
    const fetchMock = mockFetchOnce(
      textResponse({ status: 201, body: JSON.stringify({ id: 1, username: 'tyler' }) }),
    )

    const result = await apiFetch('/credentials/signup/', {
      method: 'POST',
      body: { username: 'tyler' },
    })

    expect(result).toEqual({ status: 201, data: { id: 1, username: 'tyler' } })
    const [url, options] = fetchMock.mock.calls[0]
    expect(url).toContain('/credentials/signup/')
    expect(options.headers['Content-Type']).toBe('application/json')
  })

  it('returns plain text data when the body is not JSON (202 login)', async () => {
    mockFetchOnce(textResponse({ status: 202, body: 'tyler' }))
    const result = await apiFetch('/credentials/login/', { method: 'POST', body: {} })
    expect(result).toEqual({ status: 202, data: 'tyler' })
  })

  it('throws ApiError with the server error message on non-2xx', async () => {
    mockFetchOnce(
      textResponse({ ok: false, status: 409, body: JSON.stringify({ error: 'username is already taken' }) }),
    )

    await expect(apiFetch('/credentials/signup/', { method: 'POST', body: {} })).rejects.toThrow(
      'username is already taken',
    )
  })

  it('throws a friendly ApiError when the network is unreachable', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')))

    const failure = apiFetch('/post/feed/')
    await expect(failure).rejects.toBeInstanceOf(ApiError)
    await expect(apiFetch('/post/feed/')).rejects.toThrow('Cannot reach the server')
  })

  it('appends query params to the request URL', async () => {
    const fetchMock = mockFetchOnce(textResponse({ status: 200, body: '[]' }))
    await apiFetch('/post/stock/search/', { method: 'POST', params: { stockName: 'AAPL' } })
    expect(fetchMock.mock.calls[0][0]).toContain('/post/stock/search/?stockName=AAPL')
  })
})
