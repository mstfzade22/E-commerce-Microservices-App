import { useState } from 'react'
import { useSearchParams } from 'react-router'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { ChevronDown, ChevronRight } from 'lucide-react'
import toast from 'react-hot-toast'
import { useMyPayments, usePaymentHistory } from '../../hooks/usePayments'
import { refundPayment } from '../../api/adminPaymentApi'
import { normalizeError } from '../../api/errorNormalizer'
import { Pagination } from '../../components/ui/Pagination'
import { PageSpinner } from '../../components/ui/LoadingSpinner'
import { ConfirmModal } from '../../components/ui/ConfirmModal'
import { QUERY_KEYS, STATUS_COLORS } from '../../utils/constants'
import { formatCurrency, formatDateTime, formatStatus } from '../../utils/formatters'

export function PaymentsManagePage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const page = Number(searchParams.get('page')) || 0
  const { data, isLoading } = useMyPayments({ page, size: 20 })
  const payments = data?.content || []
  const totalPages = data?.totalPages || 0
  const [refundTarget, setRefundTarget] = useState(null)

  const qc = useQueryClient()
  const refundMut = useMutation({
    mutationFn: (id) => refundPayment(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: [QUERY_KEYS.PAYMENTS] })
      toast.success('Payment refunded')
      setRefundTarget(null)
    },
    onError: (err) => toast.error(normalizeError(err).message),
  })

  if (isLoading) return <PageSpinner />

  return (
    <div>
      <h1 className="mb-4 text-2xl font-bold text-gray-900">Payments</h1>

      {payments.length === 0 ? (
        <p className="py-8 text-center text-sm text-gray-500">No payments found</p>
      ) : (
        <>
          <div className="overflow-x-auto rounded-lg border border-gray-200">
            <table className="w-full text-sm">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-3 py-3 text-left font-medium text-gray-600"></th>
                  <th className="px-3 py-3 text-left font-medium text-gray-600">ID</th>
                  <th className="px-3 py-3 text-left font-medium text-gray-600">Order</th>
                  <th className="px-3 py-3 text-left font-medium text-gray-600">Amount</th>
                  <th className="px-3 py-3 text-left font-medium text-gray-600">Status</th>
                  <th className="px-3 py-3 text-left font-medium text-gray-600">Date</th>
                  <th className="px-3 py-3 text-right font-medium text-gray-600">Actions</th>
                </tr>
              </thead>
              <tbody>
                {payments.map((p) => (
                  <PaymentRow key={p.id} payment={p} onRefund={setRefundTarget} />
                ))}
              </tbody>
            </table>
          </div>
          <div className="mt-4">
            <Pagination page={page} totalPages={totalPages} onPageChange={(p) => setSearchParams({ page: String(p) })} />
          </div>
        </>
      )}

      {refundTarget && (
        <ConfirmModal
          title="Refund Payment"
          message={`Refund payment #${refundTarget.id} for order ${refundTarget.orderNumber}?`}
          confirmLabel="Refund"
          onConfirm={() => refundMut.mutate(refundTarget.id)}
          onCancel={() => setRefundTarget(null)}
          isPending={refundMut.isPending}
        />
      )}
    </div>
  )
}

function PaymentRow({ payment, onRefund }) {
  const [expanded, setExpanded] = useState(false)
  const { data: history } = usePaymentHistory(expanded ? payment.id : null)
  const colorClass = STATUS_COLORS[payment.status] || 'bg-gray-100 text-gray-800'

  return (
    <>
      <tr className="border-t border-gray-100 hover:bg-gray-50">
        <td className="px-3 py-3">
          <button onClick={() => setExpanded(!expanded)} className="text-gray-400">
            {expanded ? <ChevronDown className="h-4 w-4" /> : <ChevronRight className="h-4 w-4" />}
          </button>
        </td>
        <td className="px-3 py-3 text-gray-500">{payment.id}</td>
        <td className="px-3 py-3 font-medium text-gray-900">{payment.orderNumber}</td>
        <td className="px-3 py-3">{formatCurrency(payment.amount)}</td>
        <td className="px-3 py-3">
          <span className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${colorClass}`}>{formatStatus(payment.status)}</span>
        </td>
        <td className="px-3 py-3 text-gray-500">{formatDateTime(payment.createdAt)}</td>
        <td className="px-3 py-3 text-right">
          {payment.status === 'APPROVED' && (
            <button
              onClick={() => onRefund(payment)}
              className="rounded-md border border-red-300 px-3 py-1 text-xs font-medium text-red-600 hover:bg-red-50"
            >
              Refund
            </button>
          )}
        </td>
      </tr>
      {expanded && history && (
        <tr>
          <td colSpan={7} className="bg-gray-50 px-8 py-3">
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
