import { Navigate, Outlet } from 'react-router'
import { useAuth } from '../../hooks/useAuth'
import { PageSpinner } from '../../components/ui/LoadingSpinner'

export function RoleGuard({ allowedRoles }) {
  const { user, isAuthenticated, isLoading } = useAuth()

  if (isLoading) return <PageSpinner />
  if (!isAuthenticated) return <Navigate to="/login" replace />
  if (!allowedRoles.includes(user.role)) return <Navigate to="/" replace />

  return <Outlet />
}
