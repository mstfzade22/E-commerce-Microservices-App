import { useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { Pencil, Trash2, Plus, Search } from 'lucide-react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import { useProducts } from '../../hooks/useProducts'
import { deleteProduct } from '../../api/adminProductApi'
import { normalizeError } from '../../api/errorNormalizer'
import { Pagination } from '../../components/ui/Pagination'
import { PageSpinner } from '../../components/ui/LoadingSpinner'
import { StockBadge } from '../../components/ui/StockBadge'
import { ConfirmModal } from '../../components/ui/ConfirmModal'
import { QUERY_KEYS, PAGINATION_DEFAULTS } from '../../utils/constants'
import { formatCurrency } from '../../utils/formatters'

export function ProductsManagePage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [keyword, setKeyword] = useState(searchParams.get('keyword') || '')
  const [deleteTarget, setDeleteTarget] = useState(null)

  const page = Number(searchParams.get('page')) || PAGINATION_DEFAULTS.PAGE
  const apiParams = { page, size: 20 }
  if (searchParams.get('keyword')) apiParams.keyword = searchParams.get('keyword')

  const { data, isLoading } = useProducts(apiParams)
  const products = data?.content || []
  const totalPages = data?.totalPages || 0

  const qc = useQueryClient()
  const deleteMutation = useMutation({
    mutationFn: (id) => deleteProduct(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: [QUERY_KEYS.PRODUCTS] })
      toast.success('Product deleted')
      setDeleteTarget(null)
    },
    onError: (err) => toast.error(normalizeError(err).message),
  })

  function handleSearch(e) {
    e.preventDefault()
    setSearchParams(keyword ? { keyword, page: '0' } : {})
  }

  if (isLoading) return <PageSpinner />

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">Products</h1>
        <Link
          to="/admin/products/new"
          className="flex items-center gap-1 rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
        >
          <Plus className="h-4 w-4" /> Add Product
        </Link>
      </div>

      <form onSubmit={handleSearch} className="mb-4 flex gap-2">
        <div className="relative flex-1">
          <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
          <input
            type="text"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            placeholder="Search products..."
            className="w-full rounded-md border border-gray-300 py-2 pl-9 pr-3 text-sm"
          />
        </div>
        <button type="submit" className="rounded-md bg-gray-100 px-4 py-2 text-sm hover:bg-gray-200">Search</button>
      </form>

      <div className="overflow-x-auto rounded-lg border border-gray-200">
        <table className="w-full text-sm">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-3 py-3 text-left font-medium text-gray-600">ID</th>
              <th className="px-3 py-3 text-left font-medium text-gray-600">Image</th>
              <th className="px-3 py-3 text-left font-medium text-gray-600">Name</th>
              <th className="px-3 py-3 text-left font-medium text-gray-600">SKU</th>
              <th className="px-3 py-3 text-left font-medium text-gray-600">Price</th>
              <th className="px-3 py-3 text-left font-medium text-gray-600">Stock</th>
              <th className="px-3 py-3 text-center font-medium text-gray-600">Active</th>
              <th className="px-3 py-3 text-center font-medium text-gray-600">Featured</th>
              <th className="px-3 py-3 text-right font-medium text-gray-600">Actions</th>
            </tr>
          </thead>
          <tbody>
            {products.map((p) => (
              <tr key={p.id} className="border-t border-gray-100 hover:bg-gray-50">
                <td className="px-3 py-2 text-gray-500">{p.id}</td>
                <td className="px-3 py-2">
                  <div className="h-10 w-10 overflow-hidden rounded bg-gray-100">
                    {p.primaryImageUrl && <img src={p.primaryImageUrl} alt="" className="h-full w-full object-cover" />}
                  </div>
                </td>
                <td className="px-3 py-2 font-medium text-gray-900">{p.name}</td>
                <td className="px-3 py-2 text-gray-500">{p.sku || '-'}</td>
                <td className="px-3 py-2">{formatCurrency(p.price)}</td>
                <td className="px-3 py-2"><StockBadge status={p.stockStatus} /></td>
                <td className="px-3 py-2 text-center">{p.isActive ? '✓' : '—'}</td>
                <td className="px-3 py-2 text-center">{p.isFeatured ? '✓' : '—'}</td>
                <td className="px-3 py-2">
                  <div className="flex justify-end gap-1">
                    <Link to={`/admin/products/${p.id}/edit`} className="rounded p-1.5 text-gray-500 hover:bg-gray-100 hover:text-blue-600">
                      <Pencil className="h-4 w-4" />
                    </Link>
                    <button onClick={() => setDeleteTarget(p)} className="rounded p-1.5 text-gray-500 hover:bg-gray-100 hover:text-red-600">
                      <Trash2 className="h-4 w-4" />
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="mt-4">
        <Pagination page={page} totalPages={totalPages} onPageChange={(p) => setSearchParams((prev) => { const n = new URLSearchParams(prev); n.set('page', String(p)); return n })} />
      </div>

      {deleteTarget && (
        <ConfirmModal
          title="Delete Product"
          message={`Are you sure you want to delete "${deleteTarget.name}"?`}
          confirmLabel="Delete"
          onConfirm={() => deleteMutation.mutate(deleteTarget.id)}
          onCancel={() => setDeleteTarget(null)}
          isPending={deleteMutation.isPending}
        />
      )}
    </div>
  )
}
