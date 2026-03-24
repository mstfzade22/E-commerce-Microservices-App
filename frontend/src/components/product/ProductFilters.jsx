import { useState } from 'react'
import { ChevronDown, ChevronRight } from 'lucide-react'
import { useCategoryTree } from '../../hooks/useCategories'

export function ProductFilters({ filters, onFilterChange }) {
  return (
    <aside className="w-full space-y-6 lg:w-56">
      <CategoryFilter
        selectedId={filters.categoryId}
        onChange={(categoryId) => onFilterChange({ categoryId })}
      />
      <PriceFilter
        minPrice={filters.minPrice}
        maxPrice={filters.maxPrice}
        onChange={onFilterChange}
      />
      <StockFilter
        stockStatus={filters.stockStatus}
        onChange={(stockStatus) => onFilterChange({ stockStatus })}
      />
    </aside>
  )
}

function CategoryFilter({ selectedId, onChange }) {
  const { data: tree } = useCategoryTree()

  if (!tree || tree.length === 0) return null

  return (
    <FilterSection title="Category">
      <button
        onClick={() => onChange('')}
        className={`block w-full text-left text-sm ${!selectedId ? 'font-medium text-blue-600' : 'text-gray-600 hover:text-gray-900'}`}
      >
        All Categories
      </button>
      {tree.map((node) => (
        <CategoryNode key={node.id} node={node} selectedId={selectedId} onChange={onChange} depth={0} />
      ))}
    </FilterSection>
  )
}

function CategoryNode({ node, selectedId, onChange, depth }) {
  const [expanded, setExpanded] = useState(false)
  const isSelected = String(node.id) === String(selectedId)
  const hasChildren = node.children && node.children.length > 0

  return (
    <div style={{ paddingLeft: depth * 12 }}>
      <div className="flex items-center gap-1">
        {hasChildren && (
          <button onClick={() => setExpanded(!expanded)} className="p-0.5 text-gray-400">
            {expanded ? <ChevronDown className="h-3.5 w-3.5" /> : <ChevronRight className="h-3.5 w-3.5" />}
          </button>
        )}
        <button
          onClick={() => onChange(String(node.id))}
          className={`text-sm ${isSelected ? 'font-medium text-blue-600' : 'text-gray-600 hover:text-gray-900'}`}
        >
          {node.name}
          {node.productCount != null && (
            <span className="ml-1 text-xs text-gray-400">({node.productCount})</span>
          )}
        </button>
      </div>
      {expanded && hasChildren && node.children.map((child) => (
        <CategoryNode key={child.id} node={child} selectedId={selectedId} onChange={onChange} depth={depth + 1} />
      ))}
    </div>
  )
}

function PriceFilter({ minPrice, maxPrice, onChange }) {
  const [min, setMin] = useState(minPrice)
  const [max, setMax] = useState(maxPrice)

  function handleApply() {
    onChange({ minPrice: min, maxPrice: max })
  }

  return (
    <FilterSection title="Price Range">
      <div className="flex items-center gap-2">
        <input
          type="number"
          placeholder="Min"
          value={min}
          onChange={(e) => setMin(e.target.value)}
          className="w-full rounded border border-gray-300 px-2 py-1.5 text-sm"
        />
        <span className="text-gray-400">-</span>
        <input
          type="number"
          placeholder="Max"
          value={max}
          onChange={(e) => setMax(e.target.value)}
          className="w-full rounded border border-gray-300 px-2 py-1.5 text-sm"
        />
      </div>
      <button
        onClick={handleApply}
        className="mt-2 w-full rounded bg-gray-100 py-1.5 text-sm text-gray-700 hover:bg-gray-200"
      >
        Apply
      </button>
    </FilterSection>
  )
}

function StockFilter({ stockStatus, onChange }) {
  const options = [
    { value: '', label: 'All' },
    { value: 'AVAILABLE', label: 'Available' },
    { value: 'LOW_STOCK', label: 'Low Stock' },
  ]

  return (
    <FilterSection title="Availability">
      <div className="space-y-1">
        {options.map((opt) => (
          <label key={opt.value} className="flex items-center gap-2 text-sm text-gray-600">
            <input
              type="radio"
              name="stockStatus"
              checked={stockStatus === opt.value}
              onChange={() => onChange(opt.value)}
              className="accent-blue-600"
            />
            {opt.label}
          </label>
        ))}
      </div>
    </FilterSection>
  )
}

function FilterSection({ title, children }) {
  return (
    <div>
      <h3 className="mb-2 text-sm font-semibold text-gray-900">{title}</h3>
      {children}
    </div>
  )
}
