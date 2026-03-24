import { Link } from 'react-router'
import { formatCurrency } from '../../utils/formatters'

export function CartSummary({ cart, onClearCart, validationWarning }) {
  return (
    <div className="rounded-lg border border-gray-200 bg-gray-50 p-5">
      <h2 className="text-lg font-semibold text-gray-900">Order Summary</h2>

      <dl className="mt-4 space-y-2 text-sm">
        <div className="flex justify-between">
          <dt className="text-gray-600">Items</dt>
          <dd className="font-medium text-gray-900">{cart.totalItems}</dd>
        </div>
        <div className="flex justify-between border-t border-gray-200 pt-2">
          <dt className="text-base font-semibold text-gray-900">Subtotal</dt>
          <dd className="text-base font-semibold text-gray-900">{formatCurrency(cart.totalPrice)}</dd>
        </div>
      </dl>

      {validationWarning && (
        <p className="mt-3 rounded bg-yellow-50 p-2 text-xs text-yellow-800">{validationWarning}</p>
      )}

      <Link
        to="/checkout"
        className="mt-4 block w-full rounded-md bg-blue-600 py-2.5 text-center text-sm font-medium text-white hover:bg-blue-700"
      >
        Proceed to Checkout
      </Link>

      <button
        onClick={onClearCart}
        className="mt-2 w-full text-center text-sm text-gray-500 hover:text-red-600"
      >
        Clear Cart
      </button>
    </div>
  )
}
