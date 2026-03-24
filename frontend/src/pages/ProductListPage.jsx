import { useProducts } from '../hooks/useProducts'
import { useProductFilters } from '../hooks/useUrlParams'
import { ProductCard } from '../components/product/ProductCard'
import { ProductGridSkeleton } from '../components/product/ProductCardSkeleton'
import { ProductFilters } from '../components/product/ProductFilters'
import { SortDropdown } from '../components/product/SortDropdown'
import { Pagination } from '../components/ui/Pagination'
import { EmptyState } from '../components/ui/EmptyState'

export function ProductListPage() {
  const { filters, setFilters, apiParams } = useProductFilters()
  const { data, isLoading } = useProducts(apiParams)

  const products = data?.content || []
  const totalPages = data?.totalPages || 0
  const totalElements = data?.totalElements || 0

  return (
    <div className="flex flex-col gap-6 lg:flex-row">
      <ProductFilters filters={filters} onFilterChange={setFilters} />

      <div className="flex-1">
        <TopBar
          total={totalElements}
          filters={filters}
          onSortChange={setFilters}
          isLoading={isLoading}
        />

        {isLoading ? (
          <ProductGridSkeleton />
        ) : products.length === 0 ? (
          <EmptyState
            title="No products found"
            message="Try adjusting your filters or search terms"
            linkTo="/products"
            linkLabel="Clear filters"
          />
        ) : (
          <>
            <div className="grid grid-cols-2 gap-4 md:grid-cols-3 lg:grid-cols-4">
              {products.map((product) => (
                <ProductCard key={product.id} product={product} />
              ))}
            </div>
            <div className="mt-6">
              <Pagination
                page={filters.page}
                totalPages={totalPages}
                onPageChange={(page) => setFilters({ page })}
              />
            </div>
          </>
        )}
      </div>
    </div>
  )
}

function TopBar({ total, filters, onSortChange, isLoading }) {
  return (
    <div className="mb-4 flex items-center justify-between">
      <p className="text-sm text-gray-600">
        {isLoading ? 'Loading...' : `${total} product${total !== 1 ? 's' : ''} found`}
      </p>
      <SortDropdown
        sortBy={filters.sortBy}
        sortDir={filters.sortDir}
        onChange={onSortChange}
      />
    </div>
  )
}
