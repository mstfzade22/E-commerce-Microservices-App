import { Link, useSearchParams } from 'react-router'
import { AlertTriangle } from 'lucide-react'

export function PaymentDeclinePage() {
  const [searchParams] = useSearchParams()
  const orderNumber = searchParams.get('order')

  return (
    <div className="mx-auto max-w-md py-12 text-center">
      <AlertTriangle className="mx-auto h-16 w-16 text-red-500" />
      <h1 className="mt-4 text-2xl font-bold text-gray-900">Payment Declined</h1>
      <p className="mt-2 text-gray-600">
        Your payment was declined. Please try again or use a different payment method.
      </p>

      <div className="mt-6 flex gap-3">
        {orderNumber && (
          <Link
            to={`/orders/${orderNumber}`}
            className="flex-1 rounded-md bg-blue-600 py-2.5 text-sm font-medium text-white hover:bg-blue-700"
          >
            Try Again
          </Link>
        )}
        <Link
          to="/products"
          className="flex-1 rounded-md border border-gray-300 py-2.5 text-sm font-medium text-gray-700 hover:bg-gray-50"
        >
          Continue Shopping
        </Link>
      </div>

      <p className="mt-6 text-xs text-gray-500">
        If the problem persists, please contact support.
      </p>
    </div>
  )
}
