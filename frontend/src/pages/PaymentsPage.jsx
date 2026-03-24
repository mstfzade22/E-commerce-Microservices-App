import { useState } from 'react'
import { useSearchParams } from 'react-router'
import { ChevronDown, ChevronRight } from 'lucide-react'
import { useMyPayments, usePaymentHistory } from '../hooks/usePayments'
import { Pagination } from '../components/ui/Pagination'
import { EmptyState } from '../components/ui/EmptyState'
import { PageSpinner } from '../components/ui/LoadingSpinner'
import { STATUS_COLORS } from '../utils/constants'
import { formatCurrency, formatDateTime, formatStatus } from '../utils/formatters'

export function PaymentsPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const page = Number(searchParams.get('page')) || 0
  const size = 10

  const { data, isLoading } = useMyPayments({ page, size })
  const payments = data?.content || []
  const totalPages = data?.totalPages || 0

  if (isLoading) return <PageSpinner />

  if (payments.length === 0) {
    return <EmptyState title="No payments yet" linkTo="/orders" linkLabel="View Orders" />
  }

  return (
    <div>
      <h1 className="mb-6 text-2xl font-bold text-gray-900">My Payments</h1>
      <div className="overflow-hidden rounded-lg border border-gray-200">
        <table className="w-full text-sm">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-4 py-3 text-left font-medium text-gray-600"></th>
              <th className="px-4 py-3 text-left font-medium text-gray-600">Order</th>
              <th className="px-4 py-3 text-left font-medium text-gray-600">Amount</th>
              <th className="px-4 py-3 text-left font-medium text-gray-600">Status</th>
              <th className="px-4 py-3 text-left font-medium text-gray-600">Date</th>
            </tr>
          </thead>
          <tbody>
            {payments.map((payment) => (
              <PaymentRow key={payment.id} payment={payment} />
            ))}
          </tbody>
        </table>
      </div>
      <div className="mt-6">
        <Pagination page={page} totalPages={totalPages} onPageChange={(p) => setSearchParams({ page: String(p) })} />
      </div>
    </div>
  )
}

function PaymentRow({ payment }) {
  const [expanded, setExpanded] = useState(false)
  const { data: history } = usePaymentHistory(expanded ? payment.id : null)
  const colorClass = STATUS_COLORS[payment.status] || 'bg-gray-100 text-gray-800'

  return (
    <>
      <tr
        onClick={() => setExpanded(!expanded)}
        className="cursor-pointer border-t border-gray-100 hover:bg-gray-50"
      >
        <td className="px-4 py-3 text-gray-400">
          {expanded ? <ChevronDown className="h-4 w-4" /> : <ChevronRight className="h-4 w-4" />}
        </td>
        <td className="px-4 py-3 font-medium text-gray-900">{payment.orderNumber}</td>
        <td className="px-4 py-3">{formatCurrency(payment.amount)}</td>
        <td className="px-4 py-3">
          <span className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${colorClass}`}>
            {formatStatus(payment.status)}
          </span>
        </td>
        <td className="px-4 py-3 text-gray-500">{formatDateTime(payment.createdAt)}</td>
      </tr>
      {expanded && history && (
        <tr>
          <td colSpan={5} className="bg-gray-50 px-8 py-3">
            <p className="mb-2 text-xs font-medium text-gray-600">Payment History</p>
            {history.map((entry, i) => (
              <div key={i} className="flex gap-4 py-1 text-xs">
                <span className="text-gray-500">{formatDateTime(entry.changedAt || entry.createdAt)}</span>
                <span className="font-medium">{formatStatus(entry.status)}</span>
                {entry.reason && <span className="text-gray-500">{entry.reason}</span>}
              </div>
            ))}
          </td>
        </tr>
      )}
    </>
  )
}
