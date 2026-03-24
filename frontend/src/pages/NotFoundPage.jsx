import { Link } from 'react-router'

export function NotFoundPage() {
  return (
    <div className="flex min-h-[60vh] flex-col items-center justify-center text-center">
      <h1 className="text-6xl font-bold text-gray-300">404</h1>
      <p className="mt-4 text-lg text-gray-600">Page not found</p>
      <Link
        to="/"
        className="mt-6 rounded-md bg-blue-600 px-6 py-2 text-sm text-white hover:bg-blue-700"
      >
        Back to Home
      </Link>
    </div>
  )
}
