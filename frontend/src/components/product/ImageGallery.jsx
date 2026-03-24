import { useState } from 'react'
import { ShoppingCart } from 'lucide-react'

export function ImageGallery({ images }) {
  const sorted = images ? [...images].sort((a, b) => a.displayOrder - b.displayOrder) : []
  const primary = sorted.find((img) => img.isPrimary) || sorted[0]
  const [selected, setSelected] = useState(primary || null)

  if (sorted.length === 0) {
    return (
      <div className="flex aspect-square items-center justify-center rounded-lg bg-gray-100">
        <ShoppingCart className="h-16 w-16 text-gray-300" />
      </div>
    )
  }

  return (
    <div>
      <div className="overflow-hidden rounded-lg bg-gray-100">
        <img
          src={selected?.imageUrl || primary?.imageUrl}
          alt={selected?.altText || 'Product image'}
          className="aspect-square w-full object-cover"
        />
      </div>
      {sorted.length > 1 && (
        <div className="mt-3 flex gap-2 overflow-x-auto">
          {sorted.map((img) => (
            <button
              key={img.id}
              onClick={() => setSelected(img)}
              className={`shrink-0 overflow-hidden rounded-md border-2 ${
                selected?.id === img.id ? 'border-blue-600' : 'border-transparent'
              }`}
            >
              <img
                src={img.imageUrl}
                alt={img.altText || 'Thumbnail'}
                className="h-16 w-16 object-cover"
              />
            </button>
          ))}
        </div>
      )}
    </div>
  )
}
