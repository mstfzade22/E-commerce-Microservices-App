import { Link, useSearchParams } from 'react-router'
import { useQuery } from '@tanstack/react-query'
import { useOrders } from '../../hooks/useOrders'
import { getOrdersByStatus } from '../../api/adminOrderApi'
import { Pagination } from '../../components/ui/Pagination'
import { PageSpinner } from '../../components/ui/LoadingSpinner'
import { QUERY_KEYS, ORDER_STATUSES, STATUS_COLORS, STALE_TIMES } from '../../utils/constants'
import { formatCurrency, formatDate, formatStatus } from '../../utils/formatters'

const TABS = ['ALL', ...ORDER_STATUSES]

export function OrdersManagePage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const activeTab = searchParams.get('status') || 'ALL'
  const page = Number(searchParams.get('page')) || 0

  const allQuery = useOrders(activeTab === 'ALL' ? { page, size: 20 } : undefined)
  const statusQuery = useQuery({
    queryKey: [QUERY_KEYS.ORDERS, 'admin', activeTab, page],
    queryFn: () => getOrdersByStatus(activeTab, { page, size: 20 }),
    enabled: activeTab !== 'ALL',
    staleTime: STALE_TIMES.SHORT,
  })

  const query = activeTab === 'ALL' ? allQuery : statusQuery
  const orders = query.data?.content || []
  const totalPages = query.data?.totalPages || 0

  function setTab(status) {
    const params = { status }
    if (status === 'ALL') setSearchParams({})
    else setSearchParams(params)
  }

  return (
    <div>
      <h1 className="mb-4 text-2xl font-bold text-gray-900">Orders</h1>

      <div className="mb-4 flex flex-wrap gap-1">
        {TABS.map((tab) => (
          <button
            key={tab}
            onClick={() => setTab(tab)}
            className={`rounded-md px-3 py-1.5 text-sm ${
              activeTab === tab ? 'bg-blue-600 font-medium text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
            }`}
          >
            {formatStatus(tab)}
          </button>
        ))}
      </div>

      {query.isLoading ? (
        <PageSpinner />
      ) : orders.length === 0 ? (
        <p className="py-8 text-center text-sm text-gray-500">No orders found</p>
      ) : (
        <>
          <div className="overflow-x-auto rounded-lg border border-gray-200">
            <table className="w-full text-sm">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-4 py-3 text-left font-medium text-gray-600">Order Number</th>
                  <th className="px-4 py-3 text-left font-medium text-gray-600">Status</th>
                  <th className="px-4 py-3 text-left font-medium text-gray-600">Amount</th>
                  <th className="px-4 py-3 text-left font-medium text-gray-600">Date</th>
                </tr>
              </thead>
              <tbody>
                {orders.map((order) => {
                  const colorClass = STATUS_COLORS[order.status] || 'bg-gray-100 text-gray-800'
                  return (
                    <tr key={order.orderNumber} className="border-t border-gray-100 hover:bg-gray-50">
                      <td className="px-4 py-3">
                        <Link to={`/admin/orders/${order.orderNumber}`} className="font-medium text-blue-600 hover:text-blue-700">
                          {order.orderNumber}
                        </Link>
                      </td>
                      <td className="px-4 py-3">
                        <span className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${colorClass}`}>
                          {formatStatus(order.status)}
                        </span>
                      </td>
                      <td className="px-4 py-3">{formatCurrency(order.finalAmount)}</td>
                      <td className="px-4 py-3 text-gray-500">{formatDate(order.createdAt)}</td>
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
