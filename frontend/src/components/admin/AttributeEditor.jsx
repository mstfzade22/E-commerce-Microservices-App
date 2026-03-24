import { useState } from 'react'
import { Plus, X } from 'lucide-react'

export function AttributeEditor({ attributes, onChange }) {
  const [newKey, setNewKey] = useState('')
  const [newValue, setNewValue] = useState('')

  function handleAdd() {
    const key = newKey.trim()
    const value = newValue.trim()
    if (!key || !value) return
    onChange({ ...attributes, [key]: value })
    setNewKey('')
    setNewValue('')
  }

  function handleRemove(key) {
    const next = { ...attributes }
    delete next[key]
    onChange(next)
  }

  return (
    <div>
      <label className="block text-sm font-medium text-gray-700">Attributes</label>
      <div className="mt-1 space-y-2">
        {Object.entries(attributes).map(([key, value]) => (
          <div key={key} className="flex items-center gap-2">
            <span className="w-28 text-sm font-medium text-gray-600">{key}</span>
            <span className="flex-1 text-sm text-gray-900">{value}</span>
            <button type="button" onClick={() => handleRemove(key)} className="text-gray-400 hover:text-red-500">
              <X className="h-4 w-4" />
            </button>
          </div>
        ))}
        <div className="flex items-center gap-2">
          <input
            type="text"
            value={newKey}
            onChange={(e) => setNewKey(e.target.value)}
            placeholder="Key"
            className="w-28 rounded border border-gray-300 px-2 py-1.5 text-sm"
          />
          <input
            type="text"
            value={newValue}
            onChange={(e) => setNewValue(e.target.value)}
            placeholder="Value"
            className="flex-1 rounded border border-gray-300 px-2 py-1.5 text-sm"
          />
          <button type="button" onClick={handleAdd} className="rounded bg-gray-100 p-1.5 text-gray-600 hover:bg-gray-200">
            <Plus className="h-4 w-4" />
          </button>
        </div>
      </div>
    </div>
  )
}
