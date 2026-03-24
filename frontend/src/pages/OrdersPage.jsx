import { useSearchParams } from 'react-router'
import { useOrders } from '../hooks/useOrders'
import { OrderCard } from '../components/order/OrderCard'
import { Pagination } from '../components/ui/Pagination'
import { EmptyState } from '../components/ui/EmptyState'
import { PageSpinner } from '../components/ui/LoadingSpinner'

export function OrdersPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const page = Number(searchParams.get('page')) || 0
  const size = 10

  const { data, isLoading } = useOrders({ page, size })
  const orders = data?.content || []
  const totalPages = data?.totalPages || 0

  if (isLoading) return <PageSpinner />

  if (orders.length === 0) {
    return <EmptyState title="No orders yet" message="Start shopping to see your orders here" linkTo="/products" linkLabel="Browse Products" />
  }

  return (
    <div>
      <h1 className="mb-6 text-2xl font-bold text-gray-900">My Orders</h1>
      <div className="space-y-3">
        {orders.map((order) => (
          <OrderCard key={order.orderNumber} order={order} />
        ))}
      </div>
      <div className="mt-6">
        <Pagination
          page={page}
          totalPages={totalPages}
          onPageChange={(p) => setSearchParams({ page: String(p) })}
        />
      </div>
    </div>
  )
}
