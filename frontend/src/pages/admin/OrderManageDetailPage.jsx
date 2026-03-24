import { useState } from 'react'
import { useParams } from 'react-router'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import { useOrderByNumber, useOrderHistory, useCancelOrder } from '../../hooks/useOrders'
import { usePaymentByOrder } from '../../hooks/usePayments'
import { processOrder, shipOrder, deliverOrder } from '../../api/adminOrderApi'
import { normalizeError } from '../../api/errorNormalizer'
import { OrderTimeline } from '../../components/order/OrderTimeline'
import { PageSpinner } from '../../components/ui/LoadingSpinner'
import { ConfirmModal } from '../../components/ui/ConfirmModal'
import { QUERY_KEYS, STATUS_COLORS } from '../../utils/constants'
import { formatCurrency, formatDate, formatStatus } from '../../utils/formatters'

export function OrderManageDetailPage() {
  const { orderNumber } = useParams()
  const { data: order, isLoading } = useOrderByNumber(orderNumber)
  const { data: history } = useOrderHistory(orderNumber)
  const { data: payment } = usePaymentByOrder(orderNumber)
  const [cancelModal, setCancelModal] = useState(false)
  const [cancelReason, setCancelReason] = useState('')

  if (isLoading) return <PageSpinner />
  if (!order) return <p className="py-12 text-center text-gray-500">Order not found.</p>

  const colorClass = STATUS_COLORS[order.status] || 'bg-gray-100 text-gray-800'

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">{order.orderNumber}</h1>
          <p className="text-sm text-gray-500">{formatDate(order.createdAt)}</p>
        </div>
        <span className={`rounded-full px-3 py-1 text-sm font-medium ${colorClass}`}>{formatStatus(order.status)}</span>
      </div>

      <AdminActions order={order} onCancelClick={() => setCancelModal(true)} />

      <div className="grid gap-6 md:grid-cols-2">
        <ItemsTable items={order.items} />
        <div className="space-y-6">
          <AmountSummary order={order} />
          {order.shippingAddress && <ShippingAddress address={order.shippingAddress} />}
          {payment && (
            <div>
              <h2 className="mb-2 text-lg font-semibold text-gray-900">Payment</h2>
              <span className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${STATUS_COLORS[payment.status] || 'bg-gray-100 text-gray-800'}`}>
                {formatStatus(payment.status)}
              </span>
            </div>
          )}
        </div>
      </div>

      {history && history.length > 0 && (
        <div>
          <h2 className="mb-3 text-lg font-semibold text-gray-900">Status History</h2>
          <OrderTimeline history={history} />
        </div>
      )}

      {cancelModal && (
        <CancelModal
          orderNumber={order.orderNumber}
          reason={cancelReason}
          setReason={setCancelReason}
          onClose={() => { setCancelModal(false); setCancelReason('') }}
        />
      )}
    </div>
  )
}

function AdminActions({ order, onCancelClick }) {
  const qc = useQueryClient()

  const mutationOptions = {
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: [QUERY_KEYS.ORDER, order.orderNumber] })
      qc.invalidateQueries({ queryKey: [QUERY_KEYS.ORDERS] })
      toast.success('Order updated')
    },
    onError: (err) => toast.error(normalizeError(err).message),
  }

  const processMut = useMutation({ mutationFn: () => processOrder(order.orderNumber), ...mutationOptions })
  const shipMut = useMutation({ mutationFn: () => shipOrder(order.orderNumber), ...mutationOptions })
  const deliverMut = useMutation({ mutationFn: () => deliverOrder(order.orderNumber), ...mutationOptions })

  const actions = {
    CONFIRMED: { label: 'Process Order', mut: processMut, color: 'bg-purple-600 hover:bg-purple-700' },
    PROCESSING: { label: 'Ship Order', mut: shipMut, color: 'bg-indigo-600 hover:bg-indigo-700' },
    SHIPPED: { label: 'Mark Delivered', mut: deliverMut, color: 'bg-green-600 hover:bg-green-700' },
  }

  const action = actions[order.status]
  const canCancel = ['PENDING', 'CONFIRMED', 'PROCESSING'].includes(order.status)

  if (!action && !canCancel) return null

  return (
    <div className="flex gap-3">
      {action && (
        <button
          onClick={() => action.mut.mutate()}
          disabled={action.mut.isPending}
          className={`rounded-md px-4 py-2 text-sm font-medium text-white disabled:opacity-50 ${action.color}`}
        >
          {action.mut.isPending ? 'Processing...' : action.label}
        </button>
      )}
      {canCancel && (
        <button
          onClick={onCancelClick}
          className="rounded-md border border-red-300 px-4 py-2 text-sm font-medium text-red-600 hover:bg-red-50"
        >
          Cancel Order
        </button>
      )}
    </div>
  )
}

function CancelModal({ orderNumber, reason, setReason, onClose }) {
  const cancelOrder = useCancelOrder()

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
      <div className="w-full max-w-sm rounded-lg bg-white p-6 shadow-xl">
        <h3 className="text-lg font-semibold text-gray-900">Cancel Order</h3>
        <textarea
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          placeholder="Reason for cancellation..."
          rows={3}
          className="mt-3 w-full rounded-md border border-gray-300 px-3 py-2 text-sm"
        />
        <div className="mt-4 flex justify-end gap-3">
          <button onClick={onClose} className="rounded-md border border-gray-300 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50">Cancel</button>
          <button
            onClick={() => cancelOrder.mutate({ orderNumber, reason }, { onSuccess: onClose })}
            disabled={cancelOrder.isPending}
            className="rounded-md bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-700 disabled:opacity-50"
          >
            {cancelOrder.isPending ? 'Cancelling...' : 'Confirm Cancel'}
          </button>
        </div>
      </div>
    </div>
  )
}

function ItemsTable({ items }) {
  return (
    <div>
      <h2 className="mb-3 text-lg font-semibold text-gray-900">Items</h2>
      <div className="rounded-md border border-gray-200">
        {items.map((item) => (
          <div key={item.id} className="flex items-center gap-3 border-b border-gray-100 p-3 last:border-b-0">
            <div className="h-12 w-12 shrink-0 overflow-hidden rounded bg-gray-100">
              {item.productImageUrl && <img src={item.productImageUrl} alt={item.productName} className="h-full w-full object-cover" />}
            </div>
            <div className="flex-1 text-sm">
              <p className="font-medium text-gray-900">{item.productName}</p>
              {item.sku && <p className="text-xs text-gray-500">SKU: {item.sku}</p>}
            </div>
            <div className="text-right text-sm">
              <p className="text-gray-600">{formatCurrency(item.unitPrice)} x {item.quantity}</p>
              <p className="font-medium text-gray-900">{formatCurrency(item.subtotal)}</p>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}

function AmountSummary({ order }) {
  return (
    <div>
      <h2 className="mb-3 text-lg font-semibold text-gray-900">Summary</h2>
      <div className="space-y-1 text-sm">
        <div className="flex justify-between"><span className="text-gray-600">Subtotal</span><span>{formatCurrency(order.totalAmount)}</span></div>
        {order.discountAmount > 0 && <div className="flex justify-between text-green-600"><span>Discount</span><span>-{formatCurrency(order.discountAmount)}</span></div>}
        <div className="flex justify-between border-t border-gray-200 pt-1 font-semibold"><span>Total</span><span>{formatCurrency(order.finalAmount, order.currency)}</span></div>
      </div>
    </div>
  )
}

function ShippingAddress({ address }) {
  return (
    <div>
      <h2 className="mb-3 text-lg font-semibold text-gray-900">Shipping</h2>
      <div className="text-sm text-gray-600">
        <p>{address.addressLine1}</p>
        {address.addressLine2 && <p>{address.addressLine2}</p>}
        <p>{address.city}{address.state ? `, ${address.state}` : ''} {address.postalCode}</p>
        <p>{address.country}</p>
      </div>
    </div>
  )
}
