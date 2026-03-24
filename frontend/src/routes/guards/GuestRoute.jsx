import { Navigate, Outlet, useLocation } from 'react-router'
import { useAuth } from '../../hooks/useAuth'
import { PageSpinner } from '../../components/ui/LoadingSpinner'

export function GuestRoute() {
  const { isAuthenticated, isLoading } = useAuth()
  const location = useLocation()

  if (isLoading) return <PageSpinner />

  const from = location.state?.from?.pathname || '/'
  if (isAuthenticated) return <Navigate to={from} replace />

  return <Outlet />
}
