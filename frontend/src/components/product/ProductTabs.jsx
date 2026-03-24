import { useState } from 'react'
import { ProductAttributes } from './ProductAttributes'

export function ProductTabs({ product }) {
  const [activeTab, setActiveTab] = useState('description')

  const tabs = [
    { key: 'description', label: 'Description' },
    { key: 'specifications', label: 'Specifications' },
  ]

  return (
    <div>
      <div className="flex border-b border-gray-200">
        {tabs.map((tab) => (
          <button
            key={tab.key}
            onClick={() => setActiveTab(tab.key)}
            className={`px-4 py-2.5 text-sm font-medium ${
              activeTab === tab.key
                ? 'border-b-2 border-blue-600 text-blue-600'
                : 'text-gray-500 hover:text-gray-700'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>
      <div className="py-4">
        {activeTab === 'description' && <DescriptionTab description={product.description} />}
        {activeTab === 'specifications' && <SpecificationsTab product={product} />}
      </div>
    </div>
  )
}

function DescriptionTab({ description }) {
  if (!description) return <p className="text-sm text-gray-500">No description available.</p>

  return <div className="prose prose-sm max-w-none text-gray-700" dangerouslySetInnerHTML={{ __html: description }} />
}

function SpecificationsTab({ product }) {
  const specs = {}
  if (product.weightKg != null) specs['Weight'] = `${product.weightKg} kg`
  if (product.lengthCm != null) specs['Length'] = `${product.lengthCm} cm`
  if (product.widthCm != null) specs['Width'] = `${product.widthCm} cm`
  if (product.heightCm != null) specs['Height'] = `${product.heightCm} cm`

  if (Object.keys(specs).length === 0 && (!product.attributes || Object.keys(product.attributes).length === 0)) {
    return <p className="text-sm text-gray-500">No specifications available.</p>
  }

  return (
    <div className="space-y-4">
      {Object.keys(specs).length > 0 && (
        <table className="w-full text-sm">
          <tbody>
            {Object.entries(specs).map(([key, value]) => (
              <tr key={key} className="border-b border-gray-100">
                <td className="py-2 pr-4 font-medium text-gray-600">{key}</td>
                <td className="py-2 text-gray-900">{value}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      <ProductAttributes attributes={product.attributes} />
    </div>
  )
}
