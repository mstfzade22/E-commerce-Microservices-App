const SORT_OPTIONS = [
  { label: 'Default', sortBy: '', sortDir: '' },
  { label: 'Price: Low to High', sortBy: 'price', sortDir: 'asc' },
  { label: 'Price: High to Low', sortBy: 'price', sortDir: 'desc' },
  { label: 'Newest', sortBy: 'createdAt', sortDir: 'desc' },
  { label: 'Name: A-Z', sortBy: 'name', sortDir: 'asc' },
]

export function SortDropdown({ sortBy, sortDir, onChange }) {
  const currentKey = `${sortBy}-${sortDir}`

  function handleChange(e) {
    const option = SORT_OPTIONS[e.target.value]
    onChange({ sortBy: option.sortBy, sortDir: option.sortDir })
  }

  return (
    <select
      value={SORT_OPTIONS.findIndex((o) => `${o.sortBy}-${o.sortDir}` === currentKey)}
      onChange={handleChange}
      className="rounded border border-gray-300 px-3 py-1.5 text-sm text-gray-700"
    >
      {SORT_OPTIONS.map((option, i) => (
        <option key={i} value={i}>{option.label}</option>
      ))}
    </select>
  )
}
