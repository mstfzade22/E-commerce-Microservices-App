import { useState } from 'react'
import { useNavigate } from 'react-router'
import { Search } from 'lucide-react'

export function HeaderSearch({ onSearch }) {
  const [keyword, setKeyword] = useState('')
  const navigate = useNavigate()

  function handleSubmit(e) {
    e.preventDefault()
    const trimmed = keyword.trim()
    if (!trimmed) return
    navigate(`/search?keyword=${encodeURIComponent(trimmed)}`)
    onSearch?.()
  }

  return (
    <form onSubmit={handleSubmit} className="relative w-full max-w-lg">
      <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
      <input
        type="text"
        value={keyword}
        onChange={(e) => setKeyword(e.target.value)}
        placeholder="Search products..."
        className="w-full rounded-md border border-gray-300 py-2 pl-9 pr-3 text-sm outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500"
      />
    </form>
  )
}
