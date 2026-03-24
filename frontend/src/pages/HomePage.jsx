import { Link } from 'react-router'
import { ShoppingBag, ArrowRight } from 'lucide-react'
import { useProducts } from '../hooks/useProducts'
import { useCategories } from '../hooks/useCategories'
import { ProductCard } from '../components/product/ProductCard'
import { ProductGridSkeleton } from '../components/product/ProductCardSkeleton'
import { CategoryGrid } from '../components/category/CategoryGrid'

export function HomePage() {
  return (
    <div className="space-y-12">
      <HeroBanner />
      <CategorySection />
      <ProductSection
        title="Featured Products"
        params={{ featured: true, size: 8 }}
      />
      <ProductSection
        title="New Arrivals"
        params={{ sortBy: 'createdAt', sortDir: 'desc', size: 8 }}
      />
    </div>
  )
}

function HeroBanner() {
  return (
    <section className="rounded-lg bg-blue-600 px-8 py-16 text-center text-white">
      <ShoppingBag className="mx-auto mb-4 h-12 w-12" />
      <h1 className="text-4xl font-bold">Welcome to E-Shop</h1>
      <p className="mt-3 text-lg text-blue-100">
        Discover amazing products at great prices
      </p>
      <Link
        to="/products"
        className="mt-6 inline-flex items-center gap-2 rounded-md bg-white px-6 py-3 font-medium text-blue-600 hover:bg-blue-50"
      >
        Browse Products
        <ArrowRight className="h-4 w-4" />
      </Link>
    </section>
  )
}

function CategorySection() {
  const { data: categories, isLoading } = useCategories()

  if (isLoading) {
    return (
      <section>
        <h2 className="mb-4 text-xl font-bold text-gray-900">Shop by Category</h2>
        <div className="grid grid-cols-2 gap-4 md:grid-cols-3 lg:grid-cols-6">
          {Array.from({ length: 6 }, (_, i) => (
            <div key={i} className="h-28 animate-pulse rounded-lg bg-gray-200" />
          ))}
        </div>
      </section>
    )
  }

  if (!categories || categories.length === 0) return null

  return (
    <section>
      <h2 className="mb-4 text-xl font-bold text-gray-900">Shop by Category</h2>
      <CategoryGrid categories={categories} />
    </section>
  )
}

function ProductSection({ title, params }) {
  const { data, isLoading } = useProducts(params)

  const products = data?.content || []

  return (
    <section>
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-xl font-bold text-gray-900">{title}</h2>
        <Link to="/products" className="text-sm text-blue-600 hover:text-blue-700">
          View all <ArrowRight className="inline h-3.5 w-3.5" />
        </Link>
      </div>
      {isLoading ? (
        <ProductGridSkeleton count={4} />
      ) : products.length === 0 ? (
        <p className="py-8 text-center text-sm text-gray-500">No products yet</p>
      ) : (
        <div className="grid grid-cols-2 gap-4 md:grid-cols-3 lg:grid-cols-4">
          {products.map((product) => (
            <ProductCard key={product.id} product={product} />
          ))}
        </div>
      )}
    </section>
  )
}
