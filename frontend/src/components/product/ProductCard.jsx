import { Link } from 'react-router'
import { ShoppingCart } from 'lucide-react'
import { StockBadge } from '../ui/StockBadge'
import { formatCurrency } from '../../utils/formatters'
import { useAddToCart } from '../../hooks/useCart'
import { useAuth } from '../../hooks/useAuth'

export function ProductCard({ product }) {
  const hasDiscount = product.discountPrice != null && product.discountPrice < product.price
  const outOfStock = product.stockStatus === 'OUT_OF_STOCK'
  const { isAuthenticated } = useAuth()
  const addToCart = useAddToCart()

  function handleAddToCart(e) {
    e.preventDefault()
    if (!isAuthenticated) return
    addToCart.mutate({ productId: product.id, quantity: 1 })
  }

  return (
    <div className="group overflow-hidden rounded-lg border border-gray-200 bg-white transition-shadow hover:shadow-md">
      <Link to={`/products/${product.slug}`} className="block">
        <div className="aspect-square overflow-hidden bg-gray-100">
          {product.primaryImageUrl ? (
            <img
              src={product.primaryImageUrl}
              alt={product.name}
              className="h-full w-full object-cover transition-transform group-hover:scale-105"
            />
          ) : (
            <div className="flex h-full items-center justify-center text-gray-300">
              <ShoppingCart className="h-12 w-12" />
            </div>
          )}
        </div>
      </Link>

      <div className="p-3">
        <Link to={`/products/${product.slug}`} className="block">
          <p className="text-xs text-gray-500">{product.categoryName}</p>
          <h3 className="mt-0.5 line-clamp-2 text-sm font-medium text-gray-900 group-hover:text-blue-600">
            {product.name}
          </h3>
        </Link>

        <div className="mt-2 flex items-center gap-2">
          {hasDiscount ? (
            <>
              <span className="text-sm font-bold text-red-600">{formatCurrency(product.discountPrice)}</span>
              <span className="text-xs text-gray-400 line-through">{formatCurrency(product.price)}</span>
            </>
          ) : (
            <span className="text-sm font-bold text-gray-900">{formatCurrency(product.price)}</span>
          )}
        </div>

        <div className="mt-2 flex items-center justify-between">
          <StockBadge status={product.stockStatus} />
          <button
            onClick={handleAddToCart}
            disabled={outOfStock || addToCart.isPending}
            className="rounded-md bg-blue-600 px-3 py-1.5 text-xs text-white hover:bg-blue-700 disabled:bg-gray-300"
          >
            Add to Cart
          </button>
        </div>
      </div>
    </div>
  )
}
