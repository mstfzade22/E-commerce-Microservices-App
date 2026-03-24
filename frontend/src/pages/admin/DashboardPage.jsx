import { Link } from 'react-router'
import { useQuery } from '@tanstack/react-query'
import { Package, AlertTriangle } from 'lucide-react'
import { getOrdersByStatus } from '../../api/adminOrderApi'
import { getLowStock } from '../../api/inventoryApi'
import { QUERY_KEYS, STATUS_COLORS, STALE_TIMES } from '../../utils/constants'
import { formatCurrency, formatDate, formatStatus } from '../../utils/formatters'
import { PageSpinner } from '../../components/ui/LoadingSpinner'

export function DashboardPage() {
  const { data: pendingData, isLoading: pendingLoading } = useQuery({
    queryKey: [QUERY_KEYS.ORDERS, 'admin', 'PENDING'],
    queryFn: () => getOrdersByStatus('PENDING', { size: 5 }),
    staleTime: STALE_TIMES.SHORT,
  })

  const { data: processingData } = useQuery({
    queryKey: [QUERY_KEYS.ORDERS, 'admin', 'PROCESSING'],
    queryFn: () => getOrdersByStatus('PROCESSING', { size: 1 }),
    staleTime: STALE_TIMES.SHORT,
  })

  const { data: lowStock, isLoading: stockLoading } = useQuery({
    queryKey: QUERY_KEYS.INVENTORY_LOW_STOCK,
    queryFn: getLowStock,
    staleTime: STALE_TIMES.SHORT,
  })

  if (pendingLoading && stockLoading) return <PageSpinner />

  const pendingCount = pendingData?.totalElements || 0
  const processingCount = processingData?.totalElements || 0
  const pendingOrders = pendingData?.content || []
  const lowStockItems = lowStock || []

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>

      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        <StatCard label="Pending Orders" value={pendingCount} color="text-yellow-600" />
        <StatCard label="Processing Orders" value={processingCount} color="text-purple-600" />
        <StatCard label="Low Stock Items" value={lowStockItems.length} color="text-red-600" />
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <RecentOrders orders={pendingOrders} />
        <LowStockAlerts items={lowStockItems} />
      </div>
    </div>
  )
}

function StatCard({ label, value, color }) {
  return (
    <div className="rounded-lg border border-gray-200 bg-white p-4">
      <p className="text-sm text-gray-600">{label}</p>
      <p className={`mt-1 text-2xl font-bold ${color}`}>{value}</p>
    </div>
  )
}

function RecentOrders({ orders }) {
  return (
    <div className="rounded-lg border border-gray-200 bg-white p-4">
      <div className="mb-3 flex items-center justify-between">
        <h2 className="font-semibold text-gray-900">Recent Pending Orders</h2>
        <Link to="/admin/orders" className="text-sm text-blue-600 hover:text-blue-700">View all</Link>
      </div>
      {orders.length === 0 ? (
        <p className="py-4 text-center text-sm text-gray-500">No pending orders</p>
      ) : (
        <div className="space-y-2">
          {orders.map((order) => (
            <Link
              key={order.orderNumber}
              to={`/admin/orders/${order.orderNumber}`}
              className="flex items-center justify-between rounded-md p-2 text-sm hover:bg-gray-50"
            >
              <div>
                <p className="font-medium text-gray-900">{order.orderNumber}</p>
                <p className="text-xs text-gray-500">{formatDate(order.createdAt)}</p>
              </div>
              <span className="font-medium">{formatCurrency(order.finalAmount)}</span>
            </Link>
          ))}
        </div>
      )}
    </div>
  )
}

function LowStockAlerts({ items }) {
  return (
    <div className="rounded-lg border border-gray-200 bg-white p-4">
      <div className="mb-3 flex items-center justify-between">
        <h2 className="font-semibold text-gray-900">Low Stock Alerts</h2>
        <Link to="/admin/inventory" className="text-sm text-blue-600 hover:text-blue-700">View all</Link>
      </div>
      {items.length === 0 ? (
        <p className="py-4 text-center text-sm text-gray-500">No low stock items</p>
      ) : (
        <div className="space-y-2">
          {items.slice(0, 5).map((item) => {
            const colorClass = STATUS_COLORS[item.stockStatus] || 'bg-gray-100 text-gray-800'
            return (
              <div key={item.productId} className="flex items-center justify-between rounded-md p-2 text-sm">
                <div className="flex items-center gap-2">
                  <AlertTriangle className="h-4 w-4 text-yellow-500" />
                  <span className="text-gray-900">Product #{item.productId}</span>
                </div>
                <div className="flex items-center gap-2">
                  <span className="text-gray-600">Qty: {item.quantity}</span>
                  <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${colorClass}`}>
                    {formatStatus(item.stockStatus)}
                  </span>
                </div>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
