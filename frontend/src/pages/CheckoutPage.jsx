import { useState } from 'react'
import { useNavigate } from 'react-router'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import toast from 'react-hot-toast'
import { useCart } from '../hooks/useCart'
import { useCreateOrder, useConfirmOrder } from '../hooks/useOrders'
import { useInitiatePayment } from '../hooks/usePayments'
import { shippingAddressSchema } from '../utils/validators'
import { normalizeError } from '../api/errorNormalizer'
import { FormField } from '../components/ui/FormField'
import { Alert } from '../components/ui/Alert'
import { PageSpinner } from '../components/ui/LoadingSpinner'
import { EmptyState } from '../components/ui/EmptyState'
import { formatCurrency } from '../utils/formatters'

export function CheckoutPage() {
  const { data: cart, isLoading } = useCart()
  const navigate = useNavigate()
  const createOrder = useCreateOrder()
  const confirmOrder = useConfirmOrder()
  const initiatePayment = useInitiatePayment()

  const [step, setStep] = useState('shipping')
  const [orderNumber, setOrderNumber] = useState(null)
  const [serverError, setServerError] = useState(null)

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm({ resolver: zodResolver(shippingAddressSchema) })

  if (isLoading) return <PageSpinner />
  if (!cart || cart.items.length === 0) {
    return <EmptyState title="Your cart is empty" linkTo="/products" linkLabel="Browse Products" />
  }

  async function onShippingSubmit(data) {
    setServerError(null)
    try {
      const order = await createOrder.mutateAsync(data)
      setOrderNumber(order.orderNumber)
      setStep('confirm')
      toast.success('Order created')
    } catch (err) {
      setServerError(normalizeError(err).message)
    }
  }

  async function handleConfirm() {
    setServerError(null)
    try {
      await confirmOrder.mutateAsync(orderNumber)
      setStep('pay')
    } catch (err) {
      setServerError(normalizeError(err).message)
    }
  }

  async function handlePay() {
    setServerError(null)
    try {
      const result = await initiatePayment.mutateAsync(orderNumber)
      window.location.href = result.paymentUrl
    } catch (err) {
      setServerError(normalizeError(err).message)
    }
  }

  return (
    <div className="mx-auto max-w-2xl">
      <h1 className="mb-6 text-2xl font-bold text-gray-900">Checkout</h1>
      <StepIndicator current={step} />
      <Alert message={serverError} />

      {step === 'shipping' && (
        <ShippingForm register={register} errors={errors} isSubmitting={isSubmitting} onSubmit={handleSubmit(onShippingSubmit)} />
      )}
      {step === 'confirm' && (
        <ConfirmStep cart={cart} orderNumber={orderNumber} onConfirm={handleConfirm} isPending={confirmOrder.isPending} navigate={navigate} />
      )}
      {step === 'pay' && (
        <PayStep orderNumber={orderNumber} onPay={handlePay} isPending={initiatePayment.isPending} />
      )}
    </div>
  )
}

function StepIndicator({ current }) {
  const steps = ['shipping', 'confirm', 'pay']
  return (
    <div className="mb-8 flex items-center gap-2">
      {steps.map((s, i) => (
        <div key={s} className="flex items-center gap-2">
          <span className={`flex h-7 w-7 items-center justify-center rounded-full text-xs font-medium ${
            s === current ? 'bg-blue-600 text-white' : 'bg-gray-200 text-gray-500'
          }`}>
            {i + 1}
          </span>
          <span className={`text-sm capitalize ${s === current ? 'font-medium text-gray-900' : 'text-gray-400'}`}>{s}</span>
          {i < steps.length - 1 && <div className="mx-1 h-px w-8 bg-gray-300" />}
        </div>
      ))}
    </div>
  )
}

function ShippingForm({ register, errors, isSubmitting, onSubmit }) {
  return (
    <form onSubmit={onSubmit} className="space-y-4">
      <FormField label="Address Line 1" name="shippingAddressLine1" register={register} error={errors.shippingAddressLine1} />
      <FormField label="Address Line 2" name="shippingAddressLine2" register={register} error={errors.shippingAddressLine2} />
      <div className="grid grid-cols-2 gap-4">
        <FormField label="City" name="shippingCity" register={register} error={errors.shippingCity} />
        <FormField label="State" name="shippingState" register={register} error={errors.shippingState} />
      </div>
      <div className="grid grid-cols-2 gap-4">
        <FormField label="Postal Code" name="shippingPostalCode" register={register} error={errors.shippingPostalCode} />
        <FormField label="Country" name="shippingCountry" register={register} error={errors.shippingCountry} />
      </div>
      <FormField label="Notes" name="notes" register={register} error={errors.notes} />
      <button
        type="submit"
        disabled={isSubmitting}
        className="w-full rounded-md bg-blue-600 py-2.5 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
      >
        {isSubmitting ? 'Placing order...' : 'Place Order'}
      </button>
    </form>
  )
}

function ConfirmStep({ cart, orderNumber, onConfirm, isPending }) {
  return (
    <div className="space-y-4">
      <p className="text-sm text-gray-600">Order <span className="font-medium">{orderNumber}</span> created. Review and confirm:</p>
      <div className="rounded-md border border-gray-200 p-4">
        {cart.items.map((item) => (
          <div key={item.productId} className="flex justify-between py-1 text-sm">
            <span className="text-gray-700">{item.productName} x{item.quantity}</span>
            <span className="text-gray-900">{formatCurrency(item.subtotal)}</span>
          </div>
        ))}
        <div className="mt-2 flex justify-between border-t border-gray-200 pt-2 text-sm font-semibold">
          <span>Total</span>
          <span>{formatCurrency(cart.totalPrice)}</span>
        </div>
      </div>
      <button
        onClick={onConfirm}
        disabled={isPending}
        className="w-full rounded-md bg-blue-600 py-2.5 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
      >
        {isPending ? 'Confirming...' : 'Confirm Order'}
      </button>
    </div>
  )
}

function PayStep({ orderNumber, onPay, isPending }) {
  return (
    <div className="space-y-4 text-center">
      <p className="text-sm text-gray-600">Order <span className="font-medium">{orderNumber}</span> confirmed! Proceed to payment.</p>
      <button
        onClick={onPay}
        disabled={isPending}
        className="w-full rounded-md bg-green-600 py-2.5 text-sm font-medium text-white hover:bg-green-700 disabled:opacity-50"
      >
        {isPending ? 'Redirecting...' : 'Pay Now'}
      </button>
    </div>
  )
}
