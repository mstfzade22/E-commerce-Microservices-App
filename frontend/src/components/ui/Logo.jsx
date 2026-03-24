import { Link } from 'react-router'
import { ShoppingBag } from 'lucide-react'

export function Logo() {
  return (
    <Link to="/" className="flex items-center gap-2 text-xl font-bold text-gray-900">
      <ShoppingBag className="h-6 w-6 text-blue-600" />
      <span>E-Shop</span>
    </Link>
  )
}
