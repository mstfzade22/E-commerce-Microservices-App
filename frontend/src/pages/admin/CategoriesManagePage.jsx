import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Pencil, Trash2, Plus, ChevronDown, ChevronRight } from 'lucide-react'
import toast from 'react-hot-toast'
import { useCategoryTree } from '../../hooks/useCategories'
import { createCategory, updateCategory, deleteCategory } from '../../api/adminCategoryApi'
import { normalizeError } from '../../api/errorNormalizer'
import { PageSpinner } from '../../components/ui/LoadingSpinner'
import { ConfirmModal } from '../../components/ui/ConfirmModal'
import { QUERY_KEYS } from '../../utils/constants'

export function CategoriesManagePage() {
  const { data: tree, isLoading } = useCategoryTree()
  const [modal, setModal] = useState(null)
  const [deleteTarget, setDeleteTarget] = useState(null)

  if (isLoading) return <PageSpinner />

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">Categories</h1>
        <button
          onClick={() => setModal({ mode: 'create', parentId: null })}
          className="flex items-center gap-1 rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
        >
          <Plus className="h-4 w-4" /> Add Category
        </button>
      </div>

      <div className="rounded-lg border border-gray-200 bg-white p-4">
        {!tree || tree.length === 0 ? (
          <p className="py-4 text-center text-sm text-gray-500">No categories yet</p>
        ) : (
          tree.map((node) => (
            <CategoryNode
              key={node.id}
              node={node}
              depth={0}
              onEdit={(cat) => setModal({ mode: 'edit', category: cat })}
              onAddChild={(parentId) => setModal({ mode: 'create', parentId })}
              onDelete={setDeleteTarget}
            />
          ))
        )}
      </div>

      {modal && (
        <CategoryModal
          mode={modal.mode}
          category={modal.category}
          parentId={modal.parentId}
          onClose={() => setModal(null)}
        />
      )}

      {deleteTarget && (
        <DeleteCategoryModal target={deleteTarget} onClose={() => setDeleteTarget(null)} />
      )}
    </div>
  )
}

function CategoryNode({ node, depth, onEdit, onAddChild, onDelete }) {
  const [expanded, setExpanded] = useState(true)
  const hasChildren = node.children && node.children.length > 0

  return (
    <div style={{ paddingLeft: depth * 20 }}>
      <div className="flex items-center gap-2 rounded-md px-2 py-1.5 hover:bg-gray-50">
        {hasChildren ? (
          <button onClick={() => setExpanded(!expanded)} className="text-gray-400">
            {expanded ? <ChevronDown className="h-4 w-4" /> : <ChevronRight className="h-4 w-4" />}
          </button>
        ) : (
          <span className="w-4" />
        )}
        <span className="flex-1 text-sm font-medium text-gray-900">{node.name}</span>
        {node.productCount != null && (
          <span className="text-xs text-gray-400">{node.productCount} products</span>
        )}
        <button onClick={() => onAddChild(node.id)} className="rounded p-1 text-gray-400 hover:text-blue-600" title="Add child">
          <Plus className="h-3.5 w-3.5" />
        </button>
        <button onClick={() => onEdit(node)} className="rounded p-1 text-gray-400 hover:text-blue-600">
          <Pencil className="h-3.5 w-3.5" />
        </button>
        <button onClick={() => onDelete(node)} className="rounded p-1 text-gray-400 hover:text-red-600">
          <Trash2 className="h-3.5 w-3.5" />
        </button>
      </div>
      {expanded && hasChildren && node.children.map((child) => (
        <CategoryNode key={child.id} node={child} depth={depth + 1} onEdit={onEdit} onAddChild={onAddChild} onDelete={onDelete} />
      ))}
    </div>
  )
}

function CategoryModal({ mode, category, parentId, onClose }) {
  const [name, setName] = useState(category?.name || '')
  const [slug, setSlug] = useState(category?.slug || '')
  const [description, setDescription] = useState(category?.description || '')
  const [imageUrl, setImageUrl] = useState(category?.imageUrl || '')
  const [error, setError] = useState(null)
  const qc = useQueryClient()

  const mutation = useMutation({
    mutationFn: () => {
      const data = { name, slug: slug || name.toLowerCase().replace(/\s+/g, '-'), description: description || undefined, imageUrl: imageUrl || undefined, parentId: parentId || undefined }
      return mode === 'edit' ? updateCategory(category.id, data) : createCategory(data)
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: QUERY_KEYS.CATEGORIES_TREE })
      qc.invalidateQueries({ queryKey: [QUERY_KEYS.CATEGORIES] })
      toast.success(mode === 'edit' ? 'Category updated' : 'Category created')
      onClose()
    },
    onError: (err) => setError(normalizeError(err).message),
  })

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
      <div className="w-full max-w-md rounded-lg bg-white p-6 shadow-xl">
        <h3 className="text-lg font-semibold text-gray-900">{mode === 'edit' ? 'Edit Category' : 'New Category'}</h3>
        {error && <p className="mt-2 text-sm text-red-600">{error}</p>}
        <div className="mt-4 space-y-3">
          <input value={name} onChange={(e) => setName(e.target.value)} placeholder="Name" className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm" />
          <input value={slug} onChange={(e) => setSlug(e.target.value)} placeholder="Slug (auto-generated)" className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm" />
          <input value={description} onChange={(e) => setDescription(e.target.value)} placeholder="Description" className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm" />
          <input value={imageUrl} onChange={(e) => setImageUrl(e.target.value)} placeholder="Image URL" className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm" />
        </div>
        <div className="mt-5 flex justify-end gap-3">
          <button onClick={onClose} className="rounded-md border border-gray-300 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50">Cancel</button>
          <button
            onClick={() => mutation.mutate()}
            disabled={mutation.isPending || !name.trim()}
            className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
          >
            {mutation.isPending ? 'Saving...' : 'Save'}
          </button>
        </div>
      </div>
    </div>
  )
}

function DeleteCategoryModal({ target, onClose }) {
  const qc = useQueryClient()
  const mutation = useMutation({
    mutationFn: () => deleteCategory(target.id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: QUERY_KEYS.CATEGORIES_TREE })
      qc.invalidateQueries({ queryKey: [QUERY_KEYS.CATEGORIES] })
      toast.success('Category deleted')
      onClose()
    },
    onError: (err) => toast.error(normalizeError(err).message),
  })

  return (
    <ConfirmModal
      title="Delete Category"
      message={`Are you sure you want to delete "${target.name}"?`}
      confirmLabel="Delete"
      onConfirm={() => mutation.mutate()}
      onCancel={onClose}
      isPending={mutation.isPending}
    />
  )
}
