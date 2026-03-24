import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import toast from 'react-hot-toast'
import { useAuth } from '../hooks/useAuth'
import { updateProfile, changePassword } from '../api/userApi'
import { profileSchema, changePasswordSchema } from '../utils/validators'
import { normalizeError, hasFieldErrors } from '../api/errorNormalizer'
import { FormField } from '../components/ui/FormField'
import { Alert } from '../components/ui/Alert'

export function ProfilePage() {
  return (
    <div className="mx-auto max-w-xl space-y-8">
      <h1 className="text-2xl font-bold text-gray-900">Profile</h1>
      <ProfileForm />
      <hr className="border-gray-200" />
      <PasswordForm />
    </div>
  )
}

function ProfileForm() {
  const { user } = useAuth()
  const [serverError, setServerError] = useState(null)

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm({
    resolver: zodResolver(profileSchema),
    defaultValues: {
      firstName: user?.firstName || '',
      lastName: user?.lastName || '',
      email: user?.email || '',
    },
  })

  async function onSubmit(data) {
    setServerError(null)
    try {
      await updateProfile(data)
      toast.success('Profile updated')
    } catch (err) {
      setServerError(normalizeError(err).message)
    }
  }

  return (
    <div>
      <h2 className="mb-4 text-lg font-semibold text-gray-900">Personal Information</h2>
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <Alert message={serverError} />
        <div>
          <label className="block text-sm font-medium text-gray-700">Username</label>
          <input
            value={user?.username || ''}
            disabled
            className="mt-1 block w-full rounded-md border border-gray-200 bg-gray-50 px-3 py-2 text-sm text-gray-500"
          />
        </div>
        <div className="grid grid-cols-2 gap-4">
          <FormField label="First name" name="firstName" register={register} error={errors.firstName} />
          <FormField label="Last name" name="lastName" register={register} error={errors.lastName} />
        </div>
        <FormField label="Email" name="email" type="email" register={register} error={errors.email} />
        <button
          type="submit"
          disabled={isSubmitting}
          className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
        >
          {isSubmitting ? 'Saving...' : 'Save Changes'}
        </button>
      </form>
    </div>
  )
}

function PasswordForm() {
  const [serverError, setServerError] = useState(null)

  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors, isSubmitting },
  } = useForm({ resolver: zodResolver(changePasswordSchema) })

  async function onSubmit(data) {
    setServerError(null)
    try {
      await changePassword(data)
      toast.success('Password changed')
      reset()
    } catch (err) {
      const normalized = normalizeError(err)
      if (hasFieldErrors(normalized)) {
        Object.entries(normalized.validationErrors).forEach(([field, message]) => {
          setError(field, { message })
        })
      } else {
        setServerError(normalized.message)
      }
    }
  }

  return (
    <div>
      <h2 className="mb-4 text-lg font-semibold text-gray-900">Change Password</h2>
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <Alert message={serverError} />
        <FormField label="Current password" name="currentPassword" type="password" register={register} error={errors.currentPassword} />
        <FormField label="New password" name="newPassword" type="password" register={register} error={errors.newPassword} />
        <FormField label="Confirm new password" name="confirmPassword" type="password" register={register} error={errors.confirmPassword} />
        <button
          type="submit"
          disabled={isSubmitting}
          className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
        >
          {isSubmitting ? 'Changing...' : 'Change Password'}
        </button>
      </form>
    </div>
  )
}
