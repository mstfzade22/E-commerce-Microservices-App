import { Link } from 'react-router'
import { PackageOpen } from 'lucide-react'

export function EmptyState({ title = 'Nothing found', message, linkTo, linkLabel }) {
  return (
    <div className="flex flex-col items-center justify-center py-16 text-center">
      <PackageOpen className="h-12 w-12 text-gray-300" />
      <h3 className="mt-4 text-lg font-medium text-gray-700">{title}</h3>
      {message && <p className="mt-1 text-sm text-gray-500">{message}</p>}
      {linkTo && linkLabel && (
        <Link
          to={linkTo}
          className="mt-4 rounded-md bg-blue-600 px-4 py-2 text-sm text-white hover:bg-blue-700"
        >
          {linkLabel}
        </Link>
      )}
    </div>
  )
}
