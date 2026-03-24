import { useParams, useSearchParams } from 'react-router'
import { useCategoryBySlug } from '../hooks/useCategories'
import { useProducts } from '../hooks/useProducts'
import { ProductCard } from '../components/product/ProductCard'
import { ProductGridSkeleton } from '../components/product/ProductCardSkeleton'
import { SortDropdown } from '../components/product/SortDropdown'
import { Pagination } from '../components/ui/Pagination'
import { Breadcrumbs } from '../components/ui/Breadcrumbs'
import { EmptyState } from '../components/ui/EmptyState'
import { PageSpinner } from '../components/ui/LoadingSpinner'
import { PAGINATION_DEFAULTS } from '../utils/constants'

export function CategoryPage() {
  const { slug } = useParams()
  const [searchParams, setSearchParams] = useSearchParams()
  const { data: category, isLoading: catLoading } = useCategoryBySlug(slug)

  const page = Number(searchParams.get('page')) || PAGINATION_DEFAULTS.PAGE
  const size = Number(searchParams.get('size')) || PAGINATION_DEFAULTS.SIZE
  const sortBy = searchParams.get('sortBy') || ''
  const sortDir = searchParams.get('sortDir') || ''

  const apiParams = { page, size, categoryId: category?.id }
  if (sortBy) apiParams.sortBy = sortBy
  if (sortDir) apiParams.sortDir = sortDir

  const { data, isLoading } = useProducts(apiParams)
  const products = data?.content || []
  const totalPages = data?.totalPages || 0
  const totalElements = data?.totalElements || 0

  if (catLoading) return <PageSpinner />

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

  const breadcrumbs = [
    { label: 'Products', to: '/products' },
    { label: category?.name || slug },
  ]

  return (
    <div className="space-y-4">
      <Breadcrumbs items={breadcrumbs} />
      <h1 className="text-2xl font-bold text-gray-900">{category?.name || slug}</h1>

      <div className="flex items-center justify-between">
        <p className="text-sm text-gray-600">
          {isLoading ? 'Loading...' : `${totalElements} product${totalElements !== 1 ? 's' : ''}`}
        </p>
        <SortDropdown sortBy={sortBy} sortDir={sortDir} onChange={updateParams} />
      </div>

      {isLoading ? (
        <ProductGridSkeleton />
      ) : products.length === 0 ? (
        <EmptyState title="No products in this category" linkTo="/products" linkLabel="Browse all" />
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
