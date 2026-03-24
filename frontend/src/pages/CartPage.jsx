import { useState, useEffect } from 'react'
import { useCart, useClearCart } from '../hooks/useCart'
import { validateCart } from '../api/cartApi'
import { CartItem } from '../components/cart/CartItem'
import { CartSummary } from '../components/cart/CartSummary'
import { EmptyState } from '../components/ui/EmptyState'
import { PageSpinner } from '../components/ui/LoadingSpinner'

export function CartPage() {
  const { data: cart, isLoading } = useCart()
  const clearCartMutation = useClearCart()
  const [validationWarning, setValidationWarning] = useState(null)

  useEffect(() => {
    if (!cart || cart.items.length === 0) return

    validateCart()
      .then((result) => {
        if (result.valid) {
          setValidationWarning(null)
          return
        }
        const priceChanged = result.items.some((i) => i.priceChanged)
        const outOfStock = result.items.some((i) => !i.inStock)
        if (outOfStock) setValidationWarning('Some items are out of stock. Please remove them before checkout.')
        else if (priceChanged) setValidationWarning('Some prices have changed since you added items.')
        else setValidationWarning(null)
      })
      .catch(() => {})
  }, [cart])

  if (isLoading) return <PageSpinner />

  if (!cart || cart.items.length === 0) {
    return (
      <EmptyState
        title="Your cart is empty"
        message="Add some products to get started"
        linkTo="/products"
        linkLabel="Continue Shopping"
      />
    )
  }

  return (
    <div>
      <h1 className="mb-6 text-2xl font-bold text-gray-900">Shopping Cart</h1>
      <div className="flex flex-col gap-8 lg:flex-row">
        <div className="flex-1">
          {cart.items.map((item) => (
            <CartItem key={item.productId} item={item} />
          ))}
        </div>
        <div className="w-full lg:w-80">
          <CartSummary
            cart={cart}
            onClearCart={() => clearCartMutation.mutate()}
            validationWarning={validationWarning}
          />
        </div>
      </div>
    </div>
  )
}
