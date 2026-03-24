import { useSearchParams } from 'react-router'
import { useProducts } from '../hooks/useProducts'
import { ProductCard } from '../components/product/ProductCard'
import { ProductGridSkeleton } from '../components/product/ProductCardSkeleton'
import { SortDropdown } from '../components/product/SortDropdown'
import { Pagination } from '../components/ui/Pagination'
import { EmptyState } from '../components/ui/EmptyState'
import { PAGINATION_DEFAULTS } from '../utils/constants'

export function SearchPage() {
  const [searchParams, setSearchParams] = useSearchParams()

  const keyword = searchParams.get('keyword') || ''
  const page = Number(searchParams.get('page')) || PAGINATION_DEFAULTS.PAGE
  const size = Number(searchParams.get('size')) || PAGINATION_DEFAULTS.SIZE
  const sortBy = searchParams.get('sortBy') || ''
  const sortDir = searchParams.get('sortDir') || ''

  const apiParams = { keyword, page, size }
  if (sortBy) apiParams.sortBy = sortBy
  if (sortDir) apiParams.sortDir = sortDir

  const { data, isLoading } = useProducts(apiParams)
  const products = data?.content || []
  const totalPages = data?.totalPages || 0
  const totalElements = data?.totalElements || 0

  function updateParams(updates) {
    setSearchParams((prev) => {
      const next = new URLSearchParams(prev)
      if (!('page' in updates)) next.set('page', '0')
      Object.entries(updates).forEach(([k, v]) => {
        if (v === '' || v == null) next.delete(k)
        else next.set(k, String(v))
      })
      return next
    })
  }

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold text-gray-900">
        {keyword ? `Search results for "${keyword}"` : 'Search'}
      </h1>

      <div className="flex items-center justify-between">
        <p className="text-sm text-gray-600">
          {isLoading ? 'Loading...' : `${totalElements} result${totalElements !== 1 ? 's' : ''}`}
        </p>
        <SortDropdown sortBy={sortBy} sortDir={sortDir} onChange={updateParams} />
      </div>

      {isLoading ? (
        <ProductGridSkeleton />
      ) : products.length === 0 ? (
        <EmptyState
          title="No results found"
          message={keyword ? `No products match "${keyword}"` : 'Enter a search term to find products'}
          linkTo="/products"
          linkLabel="Browse all products"
        />
      ) : (
        <>
          <div className="grid grid-cols-2 gap-4 md:grid-cols-3 lg:grid-cols-4">
            {products.map((p) => <ProductCard key={p.id} product={p} />)}
          </div>
          <div className="mt-6">
            <Pagination page={page} totalPages={totalPages} onPageChange={(p) => updateParams({ page: p })} />
          </div>
        </>
      )}
    </div>
  )
}
