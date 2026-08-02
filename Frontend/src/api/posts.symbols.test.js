import { beforeEach, describe, expect, it, vi } from 'vitest'
import { searchSymbols } from './posts'

vi.mock('./client', () => ({
  apiFetch: vi.fn(),
}))

import { apiFetch } from './client'

describe('searchSymbols', () => {
  beforeEach(() => {
    apiFetch.mockReset()
  })

  it('returns matching symbols for a prefix query', async () => {
    apiFetch.mockResolvedValue({
      status: 200,
      data: [{ symbol: 'AAPL', name: 'Apple Inc.' }],
    })
    await expect(searchSymbols('aa')).resolves.toEqual([{ symbol: 'AAPL', name: 'Apple Inc.' }])
    expect(apiFetch).toHaveBeenCalledWith('/post/symbols/', { params: { q: 'aa', limit: 20 } })
  })

  it('skips the network for a blank query', async () => {
    await expect(searchSymbols('   ')).resolves.toEqual([])
    expect(apiFetch).not.toHaveBeenCalled()
  })

  it('returns empty array when the API body is not a list', async () => {
    apiFetch.mockResolvedValue({ status: 200, data: { symbol: 'AAPL' } })
    await expect(searchSymbols('AAPL')).resolves.toEqual([])
  })

  it('propagates api errors', async () => {
    apiFetch.mockRejectedValue(new Error('symbols down'))
    await expect(searchSymbols('AAPL')).rejects.toThrow('symbols down')
  })
})
