import { useState, useEffect, useCallback, useMemo } from 'react'
import * as authApi from '../api/authApi'
import { ROLES } from '../utils/constants'
import { AuthContext } from './authContextDef'

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [isLoading, setIsLoading] = useState(true)

  const isAuthenticated = user !== null

  useEffect(() => {
    let cancelled = false

    async function hydrate() {
      try {
        const session = await authApi.validateSession()
        if (!session.valid) {
          if (!cancelled) setIsLoading(false)
          return
        }
        const profile = await authApi.fetchCurrentUser()
        if (!cancelled) {
          setUser({
            userId: session.userId,
            role: session.role,
            username: profile.username,
            email: profile.email,
            firstName: profile.firstName,
            lastName: profile.lastName,
          })
        }
      } catch {
        if (!cancelled) setUser(null)
      } finally {
        if (!cancelled) setIsLoading(false)
      }
    }

    hydrate()
    return () => { cancelled = true }
  }, [])

  useEffect(() => {
    function onSessionExpired() {
      setUser(null)
    }
    window.addEventListener('auth:session-expired', onSessionExpired)
    return () => window.removeEventListener('auth:session-expired', onSessionExpired)
  }, [])

  const login = useCallback(async (credentials) => {
    const loginResponse = await authApi.login(credentials)
    const profile = await authApi.fetchCurrentUser()
    const userData = {
      userId: loginResponse.userId,
      role: loginResponse.role,
      username: profile.username,
      email: profile.email,
      firstName: profile.firstName,
      lastName: profile.lastName,
    }
    setUser(userData)
    return userData
  }, [])

  const register = useCallback(async (data) => {
    return authApi.register({ ...data, role: data.role || ROLES.CUSTOMER })
  }, [])

  const logoutFn = useCallback(async () => {
    try {
      await authApi.logout()
    } finally {
      setUser(null)
    }
  }, [])

  const value = useMemo(
    () => ({ user, isAuthenticated, isLoading, login, register, logout: logoutFn }),
    [user, isAuthenticated, isLoading, login, register, logoutFn],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
