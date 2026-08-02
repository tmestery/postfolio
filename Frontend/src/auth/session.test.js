import { beforeEach, describe, expect, it } from 'vitest'
import { SESSION_KEY, readSession, writeSession, clearSession } from './session'

describe('demo session storage', () => {
  beforeEach(() => {
    window.localStorage.clear()
  })

  it('round-trips a session through localStorage', () => {
    writeSession({ username: 'tyler', id: 7 })
    expect(readSession()).toEqual({ username: 'tyler', id: 7 })
  })

  it('returns null when no session is stored', () => {
    expect(readSession()).toBeNull()
  })

  it('returns null for corrupt JSON', () => {
    window.localStorage.setItem(SESSION_KEY, '{not json')
    expect(readSession()).toBeNull()
  })

  it('returns null when username is missing or empty', () => {
    window.localStorage.setItem(SESSION_KEY, JSON.stringify({ id: 1 }))
    expect(readSession()).toBeNull()
    window.localStorage.setItem(SESSION_KEY, JSON.stringify({ username: '', id: 1 }))
    expect(readSession()).toBeNull()
  })

  it('clearSession removes the stored session', () => {
    writeSession({ username: 'tyler' })
    clearSession()
    expect(readSession()).toBeNull()
  })
})
