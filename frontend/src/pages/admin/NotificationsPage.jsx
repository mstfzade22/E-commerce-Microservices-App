import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useSearchParams } from 'react-router'
import toast from 'react-hot-toast'
import { getNotifications, getFailedNotifications, retryNotification } from '../../api/notificationApi'
import { normalizeError } from '../../api/errorNormalizer'
import { Pagination } from '../../components/ui/Pagination'
import { PageSpinner } from '../../components/ui/LoadingSpinner'
import { QUERY_KEYS, STATUS_COLORS, STALE_TIMES } from '../../utils/constants'
import { formatDateTime, formatStatus } from '../../utils/formatters'

export function NotificationsPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const tab = searchParams.get('tab') || 'all'
  const page = Number(searchParams.get('page')) || 0

  const allQuery = useQuery({
    queryKey: [QUERY_KEYS.NOTIFICATIONS, 'all', page],
    queryFn: () => getNotifications({ page, size: 20 }),
    enabled: tab === 'all',
    staleTime: STALE_TIMES.SHORT,
  })

  const failedQuery = useQuery({
    queryKey: [QUERY_KEYS.NOTIFICATIONS, 'failed', page],
    queryFn: () => getFailedNotifications({ page, size: 20 }),
    enabled: tab === 'failed',
    staleTime: STALE_TIMES.SHORT,
  })

  const query = tab === 'all' ? allQuery : failedQuery
  const notifications = query.data?.content || []
  const totalPages = query.data?.totalPages || 0

  const qc = useQueryClient()
  const retryMut = useMutation({
    mutationFn: retryNotification,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: [QUERY_KEYS.NOTIFICATIONS] })
      toast.success('Notification retried')
    },
    onError: (err) => toast.error(normalizeError(err).message),
  })

  function setTab(t) {
    setSearchParams(t === 'all' ? {} : { tab: t })
  }

  return (
    <div>
      <h1 className="mb-4 text-2xl font-bold text-gray-900">Notifications</h1>

      <div className="mb-4 flex gap-1">
        {['all', 'failed'].map((t) => (
          <button
            key={t}
            onClick={() => setTab(t)}
            className={`rounded-md px-3 py-1.5 text-sm capitalize ${
              tab === t ? 'bg-blue-600 font-medium text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
            }`}
          >
            {t}
          </button>
        ))}
      </div>

      {query.isLoading ? (
        <PageSpinner />
      ) : notifications.length === 0 ? (
        <p className="py-8 text-center text-sm text-gray-500">No notifications found</p>
      ) : (
        <>
          <div className="overflow-x-auto rounded-lg border border-gray-200">
            <table className="w-full text-sm">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-4 py-3 text-left font-medium text-gray-600">ID</th>
                  <th className="px-4 py-3 text-left font-medium text-gray-600">Type</th>
                  <th className="px-4 py-3 text-left font-medium text-gray-600">Subject</th>
                  <th className="px-4 py-3 text-left font-medium text-gray-600">Status</th>
                  <th className="px-4 py-3 text-left font-medium text-gray-600">Date</th>
                  <th className="px-4 py-3 text-right font-medium text-gray-600">Actions</th>
                </tr>
              </thead>
              <tbody>
                {notifications.map((n) => {
                  const colorClass = STATUS_COLORS[n.status] || 'bg-gray-100 text-gray-800'
                  return (
                    <tr key={n.id} className="border-t border-gray-100 hover:bg-gray-50">
                      <td className="px-4 py-3 text-gray-500">{n.id}</td>
                      <td className="px-4 py-3">{formatStatus(n.type)}</td>
                      <td className="px-4 py-3 text-gray-900">{n.subject}</td>
                      <td className="px-4 py-3">
                        <span className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${colorClass}`}>{formatStatus(n.status)}</span>
                      </td>
                      <td className="px-4 py-3 text-gray-500">{formatDateTime(n.createdAt)}</td>
                      <td className="px-4 py-3 text-right">
                        {n.status === 'FAILED' && (
                          <button
                            onClick={() => retryMut.mutate(n.id)}
                            disabled={retryMut.isPending}
                            className="rounded-md bg-blue-600 px-3 py-1 text-xs font-medium text-white hover:bg-blue-700 disabled:opacity-50"
                          >
                            Retry
                          </button>
                        )}
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
          <div className="mt-4">
            <Pagination page={page} totalPages={totalPages} onPageChange={(p) => setSearchParams((prev) => { const n = new URLSearchParams(prev); n.set('page', String(p)); return n })} />
          </div>
        </>
      )}
    </div>
  )
}
