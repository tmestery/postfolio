import { useCallback, useState } from 'react'
import { AuthContext } from './context'
import { readSession, writeSession, clearSession } from './session'

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => readSession())

  const login = useCallback((session) => {
    setUser(writeSession(session))
  }, [])

  const logout = useCallback(() => {
    clearSession()
    setUser(null)
  }, [])

  const value = { user, login, logout, isAuthenticated: Boolean(user?.username) }
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
