import { Link } from 'react-router'
import { FolderTree } from 'lucide-react'

export function CategoryGrid({ categories }) {
  return (
    <div className="grid grid-cols-2 gap-4 md:grid-cols-3 lg:grid-cols-6">
      {categories.map((category) => (
        <Link
          key={category.id}
          to={`/category/${category.slug}`}
          className="flex flex-col items-center justify-center gap-2 rounded-lg border border-gray-200 bg-white p-4 text-center transition-shadow hover:shadow-md"
        >
          {category.imageUrl ? (
            <img
              src={category.imageUrl}
              alt={category.name}
              className="h-10 w-10 rounded object-cover"
            />
          ) : (
            <FolderTree className="h-8 w-8 text-gray-400" />
          )}
          <span className="text-sm font-medium text-gray-700">{category.name}</span>
          {category.productCount != null && (
            <span className="text-xs text-gray-400">{category.productCount} products</span>
          )}
        </Link>
      ))}
    </div>
  )
}
