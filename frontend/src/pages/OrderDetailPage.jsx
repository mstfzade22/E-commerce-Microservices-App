import { useParams } from 'react-router'
import { useOrderByNumber, useOrderHistory, useConfirmOrder, useCancelOrder } from '../hooks/useOrders'
import { usePaymentByOrder, useInitiatePayment } from '../hooks/usePayments'
import { OrderTimeline } from '../components/order/OrderTimeline'
import { PageSpinner } from '../components/ui/LoadingSpinner'
import { STATUS_COLORS } from '../utils/constants'
import { formatCurrency, formatDate, formatStatus } from '../utils/formatters'

export function OrderDetailPage() {
  const { orderNumber } = useParams()
  const { data: order, isLoading } = useOrderByNumber(orderNumber)
  const { data: history } = useOrderHistory(orderNumber)
  const { data: payment } = usePaymentByOrder(orderNumber)

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
        <span className={`rounded-full px-3 py-1 text-sm font-medium ${colorClass}`}>
          {formatStatus(order.status)}
        </span>
      </div>

      <OrderActions order={order} />

      <div className="grid gap-6 md:grid-cols-2">
        <ItemsTable items={order.items} />
        <div className="space-y-6">
          <AmountSummary order={order} />
          {order.shippingAddress && <ShippingAddress address={order.shippingAddress} />}
          {payment && <PaymentInfo payment={payment} />}
        </div>
      </div>

      {history && history.length > 0 && (
        <div>
          <h2 className="mb-3 text-lg font-semibold text-gray-900">Status History</h2>
          <OrderTimeline history={history} />
        </div>
      )}
    </div>
  )
}

function OrderActions({ order }) {
  const confirmOrder = useConfirmOrder()
  const cancelOrder = useCancelOrder()
  const initiatePayment = useInitiatePayment()

  async function handlePay() {
    const result = await initiatePayment.mutateAsync(order.orderNumber)
    window.location.href = result.paymentUrl
  }

  if (order.status === 'PENDING') {
    return (
      <div className="flex gap-3">
        <button
          onClick={() => confirmOrder.mutate(order.orderNumber)}
          disabled={confirmOrder.isPending}
          className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
        >
          Confirm Order
        </button>
        <button
          onClick={() => cancelOrder.mutate({ orderNumber: order.orderNumber, reason: 'Cancelled by customer' })}
          disabled={cancelOrder.isPending}
          className="rounded-md border border-red-300 px-4 py-2 text-sm font-medium text-red-600 hover:bg-red-50 disabled:opacity-50"
        >
          Cancel Order
        </button>
      </div>
    )
  }

  if (order.status === 'CONFIRMED') {
    return (
      <div className="flex gap-3">
        <button
          onClick={handlePay}
          disabled={initiatePayment.isPending}
          className="rounded-md bg-green-600 px-4 py-2 text-sm font-medium text-white hover:bg-green-700 disabled:opacity-50"
        >
          Pay Now
        </button>
        <button
          onClick={() => cancelOrder.mutate({ orderNumber: order.orderNumber, reason: 'Cancelled by customer' })}
          disabled={cancelOrder.isPending}
          className="rounded-md border border-red-300 px-4 py-2 text-sm font-medium text-red-600 hover:bg-red-50 disabled:opacity-50"
        >
          Cancel
        </button>
      </div>
    )
  }

  return null
}

function ItemsTable({ items }) {
  return (
    <div>
      <h2 className="mb-3 text-lg font-semibold text-gray-900">Items</h2>
      <div className="rounded-md border border-gray-200">
        {items.map((item) => (
          <div key={item.id} className="flex items-center gap-3 border-b border-gray-100 p-3 last:border-b-0">
            <div className="h-12 w-12 shrink-0 overflow-hidden rounded bg-gray-100">
              {item.productImageUrl && (
                <img src={item.productImageUrl} alt={item.productName} className="h-full w-full object-cover" />
              )}
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
        <div className="flex justify-between">
          <span className="text-gray-600">Subtotal</span>
          <span>{formatCurrency(order.totalAmount)}</span>
        </div>
        {order.discountAmount > 0 && (
          <div className="flex justify-between text-green-600">
            <span>Discount</span>
            <span>-{formatCurrency(order.discountAmount)}</span>
          </div>
        )}
        <div className="flex justify-between border-t border-gray-200 pt-1 font-semibold">
          <span>Total</span>
          <span>{formatCurrency(order.finalAmount, order.currency)}</span>
        </div>
      </div>
    </div>
  )
}

function ShippingAddress({ address }) {
  return (
    <div>
      <h2 className="mb-3 text-lg font-semibold text-gray-900">Shipping Address</h2>
      <div className="text-sm text-gray-600">
        <p>{address.addressLine1}</p>
        {address.addressLine2 && <p>{address.addressLine2}</p>}
        <p>{address.city}{address.state ? `, ${address.state}` : ''} {address.postalCode}</p>
        <p>{address.country}</p>
      </div>
    </div>
  )
}

function PaymentInfo({ payment }) {
  const colorClass = STATUS_COLORS[payment.status] || 'bg-gray-100 text-gray-800'

  return (
    <div>
      <h2 className="mb-3 text-lg font-semibold text-gray-900">Payment</h2>
      <div className="text-sm">
        <span className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${colorClass}`}>
          {formatStatus(payment.status)}
        </span>
      </div>
    </div>
  )
}
