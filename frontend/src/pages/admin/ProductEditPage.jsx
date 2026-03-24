import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router'
import { useForm } from 'react-hook-form'
import toast from 'react-hot-toast'
import { useProductById } from '../../hooks/useProducts'
import { useCategoryTree } from '../../hooks/useCategories'
import { createProduct, updateProduct } from '../../api/adminProductApi'
import { normalizeError, hasFieldErrors } from '../../api/errorNormalizer'
import { FormField } from '../../components/ui/FormField'
import { Alert } from '../../components/ui/Alert'
import { PageSpinner } from '../../components/ui/LoadingSpinner'
import { slugify } from '../../utils/formatters'
import { ProductImageManager } from '../../components/admin/ProductImageManager'
import { AttributeEditor } from '../../components/admin/AttributeEditor'

export function ProductEditPage() {
  const { id } = useParams()
  const isEditMode = !!id
  const navigate = useNavigate()
  const { data: product, isLoading } = useProductById(id)
  const { data: categoryTree } = useCategoryTree()
  const [serverError, setServerError] = useState(null)
  const [attributes, setAttributes] = useState({})

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    setError,
    formState: { errors, isSubmitting },
  } = useForm()

  const nameValue = watch('name')

  useEffect(() => {
    if (nameValue && !isEditMode) {
      setValue('slug', slugify(nameValue))
    }
  }, [nameValue, isEditMode, setValue])

  useEffect(() => {
    if (product && isEditMode) {
      const fields = ['name', 'slug', 'sku', 'description', 'shortDescription', 'price', 'discountPrice', 'categoryId', 'weightKg', 'lengthCm', 'widthCm', 'heightCm']
      fields.forEach((f) => { if (product[f] != null) setValue(f, product[f]) })
      if (product.category) setValue('categoryId', product.category.id)
      setValue('isActive', product.isActive ?? true)
      setValue('isFeatured', product.isFeatured ?? false)
      setAttributes(product.attributes || {})
    }
  }, [product, isEditMode, setValue])

  if (isEditMode && isLoading) return <PageSpinner />

  async function onSubmit(data) {
    setServerError(null)
    const payload = {
      ...data,
      price: Number(data.price),
      discountPrice: data.discountPrice ? Number(data.discountPrice) : null,
      categoryId: data.categoryId ? Number(data.categoryId) : null,
      initialStock: data.initialStock ? Number(data.initialStock) : null,
      weightKg: data.weightKg ? Number(data.weightKg) : null,
      lengthCm: data.lengthCm ? Number(data.lengthCm) : null,
      widthCm: data.widthCm ? Number(data.widthCm) : null,
      heightCm: data.heightCm ? Number(data.heightCm) : null,
      attributes: Object.keys(attributes).length > 0 ? attributes : null,
    }

    try {
      if (isEditMode) {
        await updateProduct(id, payload)
        toast.success('Product updated')
      } else {
        const created = await createProduct(payload)
        toast.success('Product created')
        navigate(`/admin/products/${created.id}/edit`)
      }
    } catch (err) {
      const normalized = normalizeError(err)
      if (hasFieldErrors(normalized)) {
        Object.entries(normalized.validationErrors).forEach(([field, message]) => setError(field, { message }))
      } else {
        setServerError(normalized.message)
      }
    }
  }

  const categories = flattenTree(categoryTree || [])

  return (
    <div className="mx-auto max-w-2xl">
      <h1 className="mb-6 text-2xl font-bold text-gray-900">{isEditMode ? 'Edit Product' : 'New Product'}</h1>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <Alert message={serverError} />

        <div className="grid grid-cols-2 gap-4">
          <FormField label="Name" name="name" register={register} error={errors.name} />
          <FormField label="Slug" name="slug" register={register} error={errors.slug} />
        </div>
        <FormField label="SKU" name="sku" register={register} error={errors.sku} />

        <div>
          <label className="block text-sm font-medium text-gray-700">Description</label>
          <textarea {...register('description')} rows={4} className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-sm" />
        </div>
        <FormField label="Short Description" name="shortDescription" register={register} error={errors.shortDescription} />

        <div className="grid grid-cols-2 gap-4">
          <FormField label="Price" name="price" type="number" register={register} error={errors.price} />
          <FormField label="Discount Price" name="discountPrice" type="number" register={register} error={errors.discountPrice} />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700">Category</label>
          <select {...register('categoryId')} className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-sm">
            <option value="">None</option>
            {categories.map((c) => (
              <option key={c.id} value={c.id}>{'—'.repeat(c.depth)} {c.name}</option>
            ))}
          </select>
        </div>

        {!isEditMode && (
          <FormField label="Initial Stock" name="initialStock" type="number" register={register} error={errors.initialStock} />
        )}

        <div className="grid grid-cols-4 gap-4">
          <FormField label="Weight (kg)" name="weightKg" type="number" register={register} error={errors.weightKg} />
          <FormField label="Length (cm)" name="lengthCm" type="number" register={register} error={errors.lengthCm} />
          <FormField label="Width (cm)" name="widthCm" type="number" register={register} error={errors.widthCm} />
          <FormField label="Height (cm)" name="heightCm" type="number" register={register} error={errors.heightCm} />
        </div>

        <div className="flex gap-6">
          <label className="flex items-center gap-2 text-sm">
            <input type="checkbox" {...register('isActive')} className="accent-blue-600" /> Active
          </label>
          <label className="flex items-center gap-2 text-sm">
            <input type="checkbox" {...register('isFeatured')} className="accent-blue-600" /> Featured
          </label>
        </div>

        <AttributeEditor attributes={attributes} onChange={setAttributes} />

        <button
          type="submit"
          disabled={isSubmitting}
          className="w-full rounded-md bg-blue-600 py-2.5 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
        >
          {isSubmitting ? 'Saving...' : isEditMode ? 'Update Product' : 'Create Product'}
        </button>
      </form>

      {isEditMode && product && (
        <div className="mt-8">
          <ProductImageManager productId={Number(id)} images={product.images || []} />
        </div>
      )}
    </div>
  )
}

function flattenTree(nodes, depth = 0) {
  const result = []
  for (const node of nodes) {
    result.push({ id: node.id, name: node.name, depth })
    if (node.children) result.push(...flattenTree(node.children, depth + 1))
  }
  return result
}
