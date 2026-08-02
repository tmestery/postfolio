import { describe, expect, it, vi, beforeEach } from 'vitest'
import { getFeed } from './posts'

vi.mock('./client', () => ({
  apiFetch: vi.fn(),
}))

import { apiFetch } from './client'

describe('getFeed', () => {
  beforeEach(() => {
    apiFetch.mockReset()
  })

  // Positive: following mode sends username + mode.
  it('requests following feed with username', async () => {
    apiFetch.mockResolvedValue({ status: 200, data: [{ stock: 'NVDA' }] })
    const posts = await getFeed({ username: 'alice', mode: 'following' })
    expect(posts).toHaveLength(1)
    expect(apiFetch).toHaveBeenCalledWith('/post/feed/', {
      params: { mode: 'following', username: 'alice' },
    })
  })

  // Negative: discover is the default mode.
  it('defaults to discover without username', async () => {
    apiFetch.mockResolvedValue({ status: 200, data: [] })
    await getFeed()
    expect(apiFetch).toHaveBeenCalledWith('/post/feed/', { params: { mode: 'discover' } })
  })

  // Negative: non-array payloads become [].
  it('returns empty array for non-array payloads', async () => {
    apiFetch.mockResolvedValue({ status: 200, data: { error: 'nope' } })
    expect(await getFeed({ mode: 'discover' })).toEqual([])
  })

  // Negative: API errors propagate.
  it('propagates api errors', async () => {
    apiFetch.mockRejectedValue(new Error('down'))
    await expect(getFeed({ username: 'a', mode: 'following' })).rejects.toThrow('down')
  })
})
