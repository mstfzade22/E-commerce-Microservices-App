import { useState } from 'react'
import { Link, useNavigate, useLocation } from 'react-router'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useAuth } from '../../hooks/useAuth'
import { loginSchema } from '../../utils/validators'
import { normalizeError } from '../../api/errorNormalizer'
import { FormField } from '../../components/ui/FormField'
import { Alert } from '../../components/ui/Alert'

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [serverError, setServerError] = useState(null)

  const from = location.state?.from?.pathname || '/'

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm({ resolver: zodResolver(loginSchema) })

  async function onSubmit(data) {
    setServerError(null)
    try {
      await login(data)
      navigate(from, { replace: true })
    } catch (err) {
      const normalized = normalizeError(err)
      setServerError(normalized.message)
    }
  }

  return (
    <div>
      <h1 className="text-center text-2xl font-bold text-gray-900">Sign in to your account</h1>

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
          label="Password"
          name="password"
          type="password"
          register={register}
          error={errors.password}
          autoComplete="current-password"
        />

        <button
          type="submit"
          disabled={isSubmitting}
          className="w-full rounded-md bg-blue-600 py-2.5 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
        >
          {isSubmitting ? 'Signing in...' : 'Sign in'}
        </button>
      </form>

      <p className="mt-6 text-center text-sm text-gray-600">
        Don&apos;t have an account?{' '}
        <Link to="/register" className="font-medium text-blue-600 hover:text-blue-500">
          Register
        </Link>
      </p>
    </div>
  )
}
