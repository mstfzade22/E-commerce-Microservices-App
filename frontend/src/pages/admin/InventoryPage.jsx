import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import toast from 'react-hot-toast'
import { getLowStock, updateStock } from '../../api/inventoryApi'
import { normalizeError } from '../../api/errorNormalizer'
import { stockUpdateSchema } from '../../utils/validators'
import { PageSpinner } from '../../components/ui/LoadingSpinner'
import { StockBadge } from '../../components/ui/StockBadge'
import { QUERY_KEYS, STALE_TIMES } from '../../utils/constants'

export function InventoryPage() {
  const { data: items, isLoading } = useQuery({
    queryKey: QUERY_KEYS.INVENTORY_LOW_STOCK,
    queryFn: getLowStock,
    staleTime: STALE_TIMES.SHORT,
  })
  const [editTarget, setEditTarget] = useState(null)

  if (isLoading) return <PageSpinner />

  const lowStockItems = items || []

  return (
    <div>
      <h1 className="mb-4 text-2xl font-bold text-gray-900">Inventory</h1>

      <h2 className="mb-3 text-lg font-semibold text-gray-900">Low Stock Items</h2>

      {lowStockItems.length === 0 ? (
        <p className="py-8 text-center text-sm text-gray-500">No low stock items</p>
      ) : (
        <div className="overflow-x-auto rounded-lg border border-gray-200">
          <table className="w-full text-sm">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-4 py-3 text-left font-medium text-gray-600">Product ID</th>
                <th className="px-4 py-3 text-left font-medium text-gray-600">Quantity</th>
                <th className="px-4 py-3 text-left font-medium text-gray-600">Reserved</th>
                <th className="px-4 py-3 text-left font-medium text-gray-600">Available</th>
                <th className="px-4 py-3 text-left font-medium text-gray-600">Threshold</th>
                <th className="px-4 py-3 text-left font-medium text-gray-600">Status</th>
                <th className="px-4 py-3 text-right font-medium text-gray-600">Actions</th>
              </tr>
            </thead>
            <tbody>
              {lowStockItems.map((item) => (
                <tr key={item.productId} className="border-t border-gray-100 hover:bg-gray-50">
                  <td className="px-4 py-3 font-medium text-gray-900">#{item.productId}</td>
                  <td className="px-4 py-3">{item.quantity}</td>
                  <td className="px-4 py-3">{item.reservedQuantity}</td>
                  <td className="px-4 py-3">{item.quantity - item.reservedQuantity}</td>
                  <td className="px-4 py-3">{item.lowStockThreshold}</td>
                  <td className="px-4 py-3"><StockBadge status={item.stockStatus} /></td>
                  <td className="px-4 py-3 text-right">
                    <button
                      onClick={() => setEditTarget(item)}
                      className="rounded-md bg-gray-100 px-3 py-1.5 text-sm text-gray-700 hover:bg-gray-200"
                    >
                      Update Stock
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {editTarget && (
        <StockUpdateModal item={editTarget} onClose={() => setEditTarget(null)} />
      )}
    </div>
  )
}

function StockUpdateModal({ item, onClose }) {
  const qc = useQueryClient()

  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm({
    resolver: zodResolver(stockUpdateSchema),
    defaultValues: {
      quantity: item.quantity,
      lowStockThreshold: item.lowStockThreshold,
    },
  })

  const mutation = useMutation({
    mutationFn: (data) => updateStock(item.productId, {
      quantity: Number(data.quantity),
      lowStockThreshold: data.lowStockThreshold != null ? Number(data.lowStockThreshold) : undefined,
    }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: QUERY_KEYS.INVENTORY_LOW_STOCK })
      toast.success('Stock updated')
      onClose()
    },
    onError: (err) => toast.error(normalizeError(err).message),
  })

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
      <div className="w-full max-w-sm rounded-lg bg-white p-6 shadow-xl">
        <h3 className="text-lg font-semibold text-gray-900">Update Stock — Product #{item.productId}</h3>
        <form onSubmit={handleSubmit((data) => mutation.mutate(data))} className="mt-4 space-y-3">
          <div>
            <label className="block text-sm font-medium text-gray-700">Quantity</label>
            <input type="number" {...register('quantity', { valueAsNumber: true })} className="mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-sm" />
            {errors.quantity && <p className="mt-1 text-sm text-red-600">{errors.quantity.message}</p>}
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700">Low Stock Threshold</label>
            <input type="number" {...register('lowStockThreshold', { valueAsNumber: true })} className="mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-sm" />
            {errors.lowStockThreshold && <p className="mt-1 text-sm text-red-600">{errors.lowStockThreshold.message}</p>}
          </div>
          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={onClose} className="rounded-md border border-gray-300 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50">Cancel</button>
            <button type="submit" disabled={isSubmitting || mutation.isPending} className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50">
              {mutation.isPending ? 'Saving...' : 'Save'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
