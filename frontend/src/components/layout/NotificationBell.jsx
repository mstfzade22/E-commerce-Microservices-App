import { useState } from 'react'
import { Link } from 'react-router'
import { Bell } from 'lucide-react'
import { useQuery } from '@tanstack/react-query'
import { getNotifications } from '../../api/notificationApi'
import { useAuth } from '../../hooks/useAuth'
import { QUERY_KEYS, STALE_TIMES, ADMIN_ROLES } from '../../utils/constants'
import { formatRelativeTime, formatStatus } from '../../utils/formatters'

export function NotificationBell() {
  const { user } = useAuth()
  const [open, setOpen] = useState(false)
  const isAdmin = user && ADMIN_ROLES.includes(user.role)

  const { data } = useQuery({
    queryKey: [QUERY_KEYS.NOTIFICATIONS, 'bell'],
    queryFn: () => getNotifications({ size: 5 }),
    staleTime: STALE_TIMES.SHORT,
  })

  const notifications = data?.content || []
  const total = data?.totalElements || 0

  return (
    <div className="relative">
      <button
        onClick={() => setOpen(!open)}
        className="relative p-2 text-gray-600 hover:text-gray-900"
      >
        <Bell className="h-5 w-5" />
        {total > 0 && (
          <span className="absolute -right-0.5 -top-0.5 flex h-4 w-4 items-center justify-center rounded-full bg-red-500 text-[10px] font-bold text-white">
            {total > 99 ? '99+' : total}
          </span>
        )}
      </button>

      {open && (
        <div className="absolute right-0 top-full mt-1 w-72 rounded-md border border-gray-200 bg-white shadow-lg">
          <div className="border-b border-gray-100 px-4 py-2">
            <p className="text-sm font-medium text-gray-900">Notifications</p>
          </div>
          {notifications.length === 0 ? (
            <p className="px-4 py-6 text-center text-sm text-gray-500">No notifications</p>
          ) : (
            <div className="max-h-64 overflow-y-auto">
              {notifications.map((n) => (
                <NotificationItem key={n.id} notification={n} onClose={() => setOpen(false)} />
              ))}
            </div>
          )}
          <div className="border-t border-gray-100 px-4 py-2">
            <Link
              to={isAdmin ? '/admin/notifications' : '/orders'}
              onClick={() => setOpen(false)}
              className="text-sm text-blue-600 hover:text-blue-700"
            >
              View all
            </Link>
          </div>
        </div>
      )}
    </div>
  )
}

function NotificationItem({ notification, onClose }) {
  const linkTo = notification.referenceId
    ? `/orders/${notification.referenceId}`
    : '/orders'

  return (
    <Link
      to={linkTo}
      onClick={onClose}
      className="block border-b border-gray-50 px-4 py-2.5 hover:bg-gray-50"
    >
      <p className="text-sm text-gray-900">{formatStatus(notification.type)}</p>
      <p className="mt-0.5 text-xs text-gray-500 line-clamp-1">{notification.subject}</p>
      <p className="mt-0.5 text-xs text-gray-400">{formatRelativeTime(notification.createdAt)}</p>
    </Link>
  )
}
