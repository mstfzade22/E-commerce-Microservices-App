import { Link, useSearchParams } from 'react-router'
import { CheckCircle } from 'lucide-react'
import { useOrderByNumber } from '../../hooks/useOrders'
import { PageSpinner } from '../../components/ui/LoadingSpinner'
import { formatCurrency } from '../../utils/formatters'

export function PaymentSuccessPage() {
  const [searchParams] = useSearchParams()
  const orderNumber = searchParams.get('order')
  const { data: order, isLoading } = useOrderByNumber(orderNumber)

  if (isLoading) return <PageSpinner />

  return (
    <div className="mx-auto max-w-md py-12 text-center">
      <CheckCircle className="mx-auto h-16 w-16 text-green-500" />
      <h1 className="mt-4 text-2xl font-bold text-gray-900">Payment Successful!</h1>
      <p className="mt-2 text-gray-600">Your payment has been processed successfully.</p>

      {order && (
        <div className="mt-6 rounded-md border border-gray-200 p-4 text-left text-sm">
          <div className="flex justify-between">
            <span className="text-gray-500">Order</span>
            <span className="font-medium">{order.orderNumber}</span>
          </div>
          <div className="mt-1 flex justify-between">
            <span className="text-gray-500">Amount</span>
            <span className="font-medium">{formatCurrency(order.finalAmount, order.currency)}</span>
          </div>
          <div className="mt-1 flex justify-between">
            <span className="text-gray-500">Status</span>
            <span className="font-medium">{order.status}</span>
          </div>
        </div>
      )}

      <div className="mt-6 flex gap-3">
        {orderNumber && (
          <Link
            to={`/orders/${orderNumber}`}
            className="flex-1 rounded-md bg-blue-600 py-2.5 text-sm font-medium text-white hover:bg-blue-700"
          >
            View Order
          </Link>
        )}
        <Link
          to="/products"
          className="flex-1 rounded-md border border-gray-300 py-2.5 text-sm font-medium text-gray-700 hover:bg-gray-50"
        >
          Continue Shopping
        </Link>
      </div>
    </div>
  )
}
