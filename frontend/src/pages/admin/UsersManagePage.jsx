import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { Search } from 'lucide-react'
import toast from 'react-hot-toast'
import { getUserById, lockUser, unlockUser, disableUser, enableUser } from '../../api/adminUserApi'
import { normalizeError } from '../../api/errorNormalizer'
import { Alert } from '../../components/ui/Alert'

export function UsersManagePage() {
  const [userId, setUserId] = useState('')
  const [user, setUser] = useState(null)
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(false)

  async function handleSearch(e) {
    e.preventDefault()
    if (!userId.trim()) return
    setError(null)
    setUser(null)
    setLoading(true)
    try {
      const data = await getUserById(userId.trim())
      setUser(data)
    } catch (err) {
      setError(normalizeError(err).message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div>
      <h1 className="mb-4 text-2xl font-bold text-gray-900">Users</h1>
      <p className="mb-4 text-sm text-gray-500">Look up a user by their ID to manage their account.</p>

      <form onSubmit={handleSearch} className="mb-6 flex gap-2">
        <div className="relative flex-1 max-w-md">
          <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
          <input
            type="text"
            value={userId}
            onChange={(e) => setUserId(e.target.value)}
            placeholder="Enter user ID..."
            className="w-full rounded-md border border-gray-300 py-2 pl-9 pr-3 text-sm"
          />
        </div>
        <button type="submit" disabled={loading} className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50">
          {loading ? 'Searching...' : 'Search'}
        </button>
      </form>

      <Alert message={error} />

      {user && <UserDetail user={user} onRefresh={() => handleSearch({ preventDefault: () => {} })} />}
    </div>
  )
}

function UserDetail({ user, onRefresh }) {
  return (
    <div className="rounded-lg border border-gray-200 bg-white p-5">
      <div className="grid grid-cols-2 gap-4 text-sm">
        <Field label="User ID" value={user.userId} />
        <Field label="Username" value={user.username} />
        <Field label="Email" value={user.email} />
        <Field label="Role" value={user.userType || user.role} />
        <Field label="First Name" value={user.firstName || '-'} />
        <Field label="Last Name" value={user.lastName || '-'} />
      </div>

      <div className="mt-4 flex flex-wrap gap-2">
        <UserAction label="Lock" fn={() => lockUser(user.userId)} onSuccess={onRefresh} color="bg-yellow-600 hover:bg-yellow-700" />
        <UserAction label="Unlock" fn={() => unlockUser(user.userId)} onSuccess={onRefresh} color="bg-green-600 hover:bg-green-700" />
        <UserAction label="Disable" fn={() => disableUser(user.userId)} onSuccess={onRefresh} color="bg-red-600 hover:bg-red-700" />
        <UserAction label="Enable" fn={() => enableUser(user.userId)} onSuccess={onRefresh} color="bg-blue-600 hover:bg-blue-700" />
      </div>
    </div>
  )
}

function Field({ label, value }) {
  return (
    <div>
      <dt className="text-gray-500">{label}</dt>
      <dd className="font-medium text-gray-900">{value}</dd>
    </div>
  )
}

function UserAction({ label, fn, onSuccess, color }) {
  const mutation = useMutation({
    mutationFn: fn,
    onSuccess: () => {
      toast.success(`User ${label.toLowerCase()}ed`)
      onSuccess()
    },
    onError: (err) => toast.error(normalizeError(err).message),
  })

  return (
    <button
      onClick={() => mutation.mutate()}
      disabled={mutation.isPending}
      className={`rounded-md px-3 py-1.5 text-sm font-medium text-white disabled:opacity-50 ${color}`}
    >
      {mutation.isPending ? `${label}ing...` : label}
    </button>
  )
}
