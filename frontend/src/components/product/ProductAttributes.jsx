export function ProductAttributes({ attributes }) {
  if (!attributes || Object.keys(attributes).length === 0) return null

  return (
    <table className="w-full text-sm">
      <tbody>
        {Object.entries(attributes).map(([key, value]) => (
          <tr key={key} className="border-b border-gray-100">
            <td className="py-2 pr-4 font-medium capitalize text-gray-600">{key}</td>
            <td className="py-2 text-gray-900">{value}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}
