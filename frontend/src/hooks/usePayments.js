import { useQuery, useMutation } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import * as paymentApi from '../api/paymentApi'
import { normalizeError } from '../api/errorNormalizer'
import { QUERY_KEYS, STALE_TIMES } from '../utils/constants'

export function useMyPayments(params = {}) {
  return useQuery({
    queryKey: [QUERY_KEYS.PAYMENTS, params],
    queryFn: () => paymentApi.getMyPayments(params),
    staleTime: STALE_TIMES.SHORT,
  })
}

export function usePaymentByOrder(orderNumber) {
  return useQuery({
    queryKey: [QUERY_KEYS.PAYMENT, 'order', orderNumber],
    queryFn: () => paymentApi.getPaymentByOrder(orderNumber),
    enabled: !!orderNumber,
    staleTime: STALE_TIMES.SHORT,
    retry: false,
  })
}

export function usePaymentHistory(paymentId) {
  return useQuery({
    queryKey: [QUERY_KEYS.PAYMENT_HISTORY, paymentId],
    queryFn: () => paymentApi.getPaymentHistory(paymentId),
    enabled: !!paymentId,
    staleTime: STALE_TIMES.SHORT,
  })
}

export function useInitiatePayment() {
  return useMutation({
    mutationFn: paymentApi.initiatePayment,
    onError: (err) => {
      toast.error(normalizeError(err).message)
    },
  })
}
