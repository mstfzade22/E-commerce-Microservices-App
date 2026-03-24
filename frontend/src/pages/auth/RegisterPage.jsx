import { useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import toast from 'react-hot-toast'
import { useAuth } from '../../hooks/useAuth'
import { registerSchema } from '../../utils/validators'
import { normalizeError, hasFieldErrors } from '../../api/errorNormalizer'
import { FormField } from '../../components/ui/FormField'
import { Alert } from '../../components/ui/Alert'

export function RegisterPage() {
  const { register: registerUser } = useAuth()
  const navigate = useNavigate()
  const [serverError, setServerError] = useState(null)

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm({ resolver: zodResolver(registerSchema) })

  async function onSubmit(data) {
    setServerError(null)
    try {
      const { confirmPassword: _, ...payload } = data
      await registerUser(payload)
      toast.success('Registration successful! Please sign in.')
      navigate('/login')
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
      <h1 className="text-center text-2xl font-bold text-gray-900">Create your account</h1>

      <form onSubmit={handleSubmit(onSubmit)} className="mt-8 space-y-5">
        <Alert message={serverError} />

        <FormField
          label="Username"
          name="username"
          register={register}
          error={errors.username}
          autoComplete="username"
          autoFocus
        />

        <FormField
          label="Email"
          name="email"
          type="email"
          register={register}
          error={errors.email}
          autoComplete="email"
        />

        <div className="grid grid-cols-2 gap-4">
          <FormField
            label="First name"
            name="firstName"
            register={register}
            error={errors.firstName}
            autoComplete="given-name"
          />
          <FormField
            label="Last name"
            name="lastName"
            register={register}
            error={errors.lastName}
            autoComplete="family-name"
          />
        </div>

        <div>
          <label htmlFor="role" className="block text-sm font-medium text-gray-700">
            Account type
          </label>
          <select
            id="role"
            {...register('role')}
            className="mt-1 block w-full rounded-md border border-gray-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-blue-500 focus:ring-1 focus:ring-blue-500 focus:outline-none"
          >
            <option value="">Select account type</option>
            <option value="CUSTOMER">Customer</option>
            <option value="STORE">Store Owner</option>
          </select>
          {errors.role && (
            <p className="mt-1 text-sm text-red-600">{errors.role.message}</p>
          )}
        </div>

        <FormField
          label="Password"
          name="password"
          type="password"
          register={register}
          error={errors.password}
          autoComplete="new-password"
        />

        <FormField
          label="Confirm password"
          name="confirmPassword"
          type="password"
          register={register}
          error={errors.confirmPassword}
          autoComplete="new-password"
        />

        <button
          type="submit"
          disabled={isSubmitting}
          className="w-full rounded-md bg-blue-600 py-2.5 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
        >
          {isSubmitting ? 'Creating account...' : 'Create account'}
        </button>
      </form>

      <p className="mt-6 text-center text-sm text-gray-600">
        Already have an account?{' '}
        <Link to="/login" className="font-medium text-blue-600 hover:text-blue-500">
          Sign in
        </Link>
      </p>
    </div>
  )
}
