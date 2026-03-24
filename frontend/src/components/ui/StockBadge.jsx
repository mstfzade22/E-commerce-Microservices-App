import { STATUS_COLORS } from '../../utils/constants'
import { formatStatus } from '../../utils/formatters'

export function StockBadge({ status }) {
  if (!status) return null

  const colorClass = STATUS_COLORS[status] || 'bg-gray-100 text-gray-800'

  return (
    <span className={`inline-block rounded-full px-2.5 py-0.5 text-xs font-medium ${colorClass}`}>
      {formatStatus(status)}
    </span>
  )
}
