import { afterEach, describe, expect, it, vi } from 'vitest'
import { formatAbsoluteDate, formatRelativeTime } from './time'

describe('formatRelativeTime', () => {
  afterEach(() => {
    vi.useRealTimers()
  })

  // Positive: recent posts collapse to a short relative label.
  it('formats a post from a few hours ago', () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-08-02T12:00:00Z'))
    expect(formatRelativeTime('2026-08-02T09:00:00Z')).toBe('3h')
  })

  // Negative: null / invalid values return null instead of crashing.
  it('returns null for missing values', () => {
    expect(formatRelativeTime(null)).toBeNull()
    expect(formatRelativeTime('')).toBeNull()
  })

  it('returns null for invalid dates', () => {
    expect(formatRelativeTime('not-a-date')).toBeNull()
  })

  // Negative: older timestamps fall back to a short calendar date.
  it('falls back to month/day for older posts', () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-08-02T12:00:00Z'))
    expect(formatRelativeTime('2026-03-12T12:00:00Z')).toBe('Mar 12')
  })
})

describe('formatAbsoluteDate', () => {
  it('returns null for invalid input', () => {
    expect(formatAbsoluteDate(undefined)).toBeNull()
    expect(formatAbsoluteDate('bogus')).toBeNull()
  })
})
