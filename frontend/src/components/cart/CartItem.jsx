import { Link } from 'react-router'
import { Trash2, AlertTriangle } from 'lucide-react'
import { QuantitySelector } from '../product/QuantitySelector'
import { formatCurrency } from '../../utils/formatters'
import { useUpdateCartItem, useRemoveCartItem } from '../../hooks/useCart'

export function CartItem({ item }) {
  const updateItem = useUpdateCartItem()
  const removeItem = useRemoveCartItem()

  return (
    <div className="flex gap-4 border-b border-gray-100 py-4">
      <div className="h-20 w-20 shrink-0 overflow-hidden rounded-md bg-gray-100">
        {item.productImageUrl ? (
          <img src={item.productImageUrl} alt={item.productName} className="h-full w-full object-cover" />
        ) : (
          <div className="flex h-full items-center justify-center text-gray-300 text-xs">No img</div>
        )}
      </div>

      <div className="flex flex-1 flex-col gap-1">
        <Link to={`/products/${item.productId}`} className="text-sm font-medium text-gray-900 hover:text-blue-600">
          {item.productName}
        </Link>
        <p className="text-sm text-gray-500">{formatCurrency(item.unitPrice)}</p>
        {!item.inStock && (
          <p className="flex items-center gap-1 text-xs text-red-600">
            <AlertTriangle className="h-3 w-3" /> Out of stock
          </p>
        )}
      </div>

      <div className="flex flex-col items-end gap-2">
        <QuantitySelector
          value={item.quantity}
          onChange={(q) => updateItem.mutate({ productId: item.productId, quantity: q })}
        />
        <p className="text-sm font-medium text-gray-900">{formatCurrency(item.subtotal)}</p>
        <button
          onClick={() => removeItem.mutate(item.productId)}
          disabled={removeItem.isPending}
          className="text-gray-400 hover:text-red-500"
        >
          <Trash2 className="h-4 w-4" />
        </button>
      </div>
    </div>
  )
}
