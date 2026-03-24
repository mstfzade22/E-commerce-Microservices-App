import { useState } from 'react'
import { useParams } from 'react-router'
import { ShoppingCart } from 'lucide-react'
import { useProductBySlug } from '../hooks/useProducts'
import { Breadcrumbs } from '../components/ui/Breadcrumbs'
import { StockBadge } from '../components/ui/StockBadge'
import { PageSpinner } from '../components/ui/LoadingSpinner'
import { ImageGallery } from '../components/product/ImageGallery'
import { QuantitySelector } from '../components/product/QuantitySelector'
import { ProductTabs } from '../components/product/ProductTabs'
import { useAddToCart } from '../hooks/useCart'
import { useAuth } from '../hooks/useAuth'
import { formatCurrency } from '../utils/formatters'

export function ProductDetailPage() {
  const { slug } = useParams()
  const { data: product, isLoading, isError } = useProductBySlug(slug)

  if (isLoading) return <PageSpinner />
  if (isError || !product) {
    return <p className="py-12 text-center text-gray-500">Product not found.</p>
  }

  const breadcrumbs = [
    { label: 'Products', to: '/products' },
  ]
  if (product.category) {
    breadcrumbs.push({ label: product.category.name, to: `/category/${product.category.slug}` })
  }
  breadcrumbs.push({ label: product.name })

  return (
    <div className="space-y-8">
      <Breadcrumbs items={breadcrumbs} />
      <div className="grid gap-8 md:grid-cols-2">
        <ImageGallery images={product.images} />
        <ProductInfo product={product} />
      </div>
      <ProductTabs product={product} />
    </div>
  )
}

function ProductInfo({ product }) {
  const [quantity, setQuantity] = useState(1)
  const { isAuthenticated } = useAuth()
  const addToCart = useAddToCart()
  const hasDiscount = product.discountPrice != null && product.discountPrice < product.price
  const outOfStock = product.stockStatus === 'OUT_OF_STOCK'

  function handleAddToCart() {
    if (!isAuthenticated) return
    addToCart.mutate({ productId: product.id, quantity })
  }

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold text-gray-900">{product.name}</h1>

      <div className="flex items-center gap-3">
        {hasDiscount ? (
          <>
            <span className="text-2xl font-bold text-red-600">{formatCurrency(product.discountPrice)}</span>
            <span className="text-lg text-gray-400 line-through">{formatCurrency(product.price)}</span>
          </>
        ) : (
          <span className="text-2xl font-bold text-gray-900">{formatCurrency(product.price)}</span>
        )}
      </div>

      <div className="flex items-center gap-3">
        <StockBadge status={product.stockStatus} />
        {product.sku && <span className="text-sm text-gray-500">SKU: {product.sku}</span>}
      </div>

      {product.shortDescription && (
        <p className="text-sm text-gray-600">{product.shortDescription}</p>
      )}

      <div className="flex items-center gap-4 pt-2">
        <QuantitySelector value={quantity} onChange={setQuantity} />
        <button
          onClick={handleAddToCart}
          disabled={outOfStock || addToCart.isPending}
          className="flex flex-1 items-center justify-center gap-2 rounded-md bg-blue-600 py-2.5 text-sm font-medium text-white hover:bg-blue-700 disabled:bg-gray-300"
        >
          <ShoppingCart className="h-4 w-4" />
          {outOfStock ? 'Out of Stock' : 'Add to Cart'}
        </button>
      </div>
    </div>
  )
}
