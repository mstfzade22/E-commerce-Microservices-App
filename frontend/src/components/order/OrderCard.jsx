import { Link } from 'react-router'
import { STATUS_COLORS } from '../../utils/constants'
import { formatCurrency, formatDate, formatStatus } from '../../utils/formatters'

export function OrderCard({ order }) {
  const colorClass = STATUS_COLORS[order.status] || 'bg-gray-100 text-gray-800'

  return (
    <div className="rounded-lg border border-gray-200 bg-white p-4">
      <div className="flex items-start justify-between">
        <div>
          <p className="font-medium text-gray-900">{order.orderNumber}</p>
          <p className="mt-0.5 text-sm text-gray-500">{formatDate(order.createdAt)}</p>
        </div>
        <span className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${colorClass}`}>
          {formatStatus(order.status)}
        </span>
      </div>
      <div className="mt-3 flex items-center justify-between">
        <div className="text-sm text-gray-600">
          <span>{order.itemCount || order.items?.length || 0} item(s)</span>
          <span className="mx-2">·</span>
          <span className="font-medium text-gray-900">{formatCurrency(order.finalAmount)}</span>
        </div>
        <Link
          to={`/orders/${order.orderNumber}`}
          className="text-sm font-medium text-blue-600 hover:text-blue-700"
        >
          View Details
        </Link>
      </div>
    </div>
  )
}
