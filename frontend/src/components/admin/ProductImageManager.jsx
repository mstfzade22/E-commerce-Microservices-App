import { useState, useRef } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Trash2, Star, Upload } from 'lucide-react'
import toast from 'react-hot-toast'
import { uploadProductImage, deleteProductImage, setImagePrimary } from '../../api/adminProductApi'
import { normalizeError } from '../../api/errorNormalizer'
import { QUERY_KEYS } from '../../utils/constants'

export function ProductImageManager({ productId, images }) {
  const qc = useQueryClient()
  const fileRef = useRef(null)
  const [uploading, setUploading] = useState(false)

  const sorted = [...images].sort((a, b) => a.displayOrder - b.displayOrder)

  const deleteMut = useMutation({
    mutationFn: (imageId) => deleteProductImage(productId, imageId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: [QUERY_KEYS.PRODUCT, String(productId)] })
      toast.success('Image deleted')
    },
    onError: (err) => toast.error(normalizeError(err).message),
  })

  const primaryMut = useMutation({
    mutationFn: (imageId) => setImagePrimary(productId, imageId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: [QUERY_KEYS.PRODUCT, String(productId)] })
      toast.success('Primary image set')
    },
    onError: (err) => toast.error(normalizeError(err).message),
  })

  async function handleUpload(e) {
    const file = e.target.files?.[0]
    if (!file) return
    if (file.size > 5 * 1024 * 1024) {
      toast.error('Max file size is 5MB')
      return
    }
    const formData = new FormData()
    formData.append('file', file)
    setUploading(true)
    try {
      await uploadProductImage(productId, formData)
      qc.invalidateQueries({ queryKey: [QUERY_KEYS.PRODUCT, String(productId)] })
      toast.success('Image uploaded')
    } catch (err) {
      toast.error(normalizeError(err).message)
    } finally {
      setUploading(false)
      if (fileRef.current) fileRef.current.value = ''
    }
  }

  return (
    <div>
      <div className="mb-3 flex items-center justify-between">
        <h2 className="text-lg font-semibold text-gray-900">Images</h2>
        <label className="flex cursor-pointer items-center gap-1 rounded-md bg-gray-100 px-3 py-1.5 text-sm text-gray-700 hover:bg-gray-200">
          <Upload className="h-4 w-4" />
          {uploading ? 'Uploading...' : 'Upload'}
          <input ref={fileRef} type="file" accept="image/*" onChange={handleUpload} className="hidden" disabled={uploading} />
        </label>
      </div>

      {sorted.length === 0 ? (
        <p className="py-4 text-center text-sm text-gray-500">No images yet</p>
      ) : (
        <div className="grid grid-cols-4 gap-3">
          {sorted.map((img) => (
            <div key={img.id} className="group relative overflow-hidden rounded-md border border-gray-200">
              <img src={img.imageUrl} alt={img.altText || ''} className="aspect-square w-full object-cover" />
              {img.isPrimary && (
                <span className="absolute left-1 top-1 rounded bg-blue-600 px-1.5 py-0.5 text-[10px] font-medium text-white">Primary</span>
              )}
              <div className="absolute bottom-0 left-0 right-0 flex justify-center gap-1 bg-black/50 p-1 opacity-0 transition-opacity group-hover:opacity-100">
                {!img.isPrimary && (
                  <button onClick={() => primaryMut.mutate(img.id)} className="rounded bg-white/80 p-1 text-blue-600 hover:bg-white">
                    <Star className="h-3.5 w-3.5" />
                  </button>
                )}
                <button onClick={() => deleteMut.mutate(img.id)} className="rounded bg-white/80 p-1 text-red-600 hover:bg-white">
                  <Trash2 className="h-3.5 w-3.5" />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
