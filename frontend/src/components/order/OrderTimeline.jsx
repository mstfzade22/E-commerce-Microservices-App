import { formatDateTime, formatStatus } from '../../utils/formatters'

export function OrderTimeline({ history }) {
  if (!history || history.length === 0) return null

  return (
    <div className="space-y-0">
      {history.map((entry, i) => (
        <div key={i} className="flex gap-3">
          <div className="flex flex-col items-center">
            <div className={`h-3 w-3 rounded-full ${i === 0 ? 'bg-blue-600' : 'bg-gray-300'}`} />
            {i < history.length - 1 && <div className="h-full w-px bg-gray-200" />}
          </div>
          <div className="pb-4">
            <p className="text-sm font-medium text-gray-900">{formatStatus(entry.status)}</p>
            <p className="text-xs text-gray-500">{formatDateTime(entry.changedAt || entry.createdAt)}</p>
            {entry.reason && <p className="mt-0.5 text-xs text-gray-500">{entry.reason}</p>}
          </div>
        </div>
      ))}
    </div>
  )
}
