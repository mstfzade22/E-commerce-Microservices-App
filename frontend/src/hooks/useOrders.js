import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import * as orderApi from '../api/orderApi'
import { normalizeError } from '../api/errorNormalizer'
import { QUERY_KEYS, STALE_TIMES } from '../utils/constants'

export function useOrders(params = {}) {
  return useQuery({
    queryKey: [QUERY_KEYS.ORDERS, params],
    queryFn: () => orderApi.getOrders(params),
    staleTime: STALE_TIMES.SHORT,
  })
}

export function useOrderByNumber(orderNumber) {
  return useQuery({
    queryKey: [QUERY_KEYS.ORDER, orderNumber],
    queryFn: () => orderApi.getOrderByNumber(orderNumber),
    enabled: !!orderNumber,
    staleTime: STALE_TIMES.SHORT,
  })
}

export function useOrderHistory(orderNumber) {
  return useQuery({
    queryKey: [QUERY_KEYS.ORDER_HISTORY, orderNumber],
    queryFn: () => orderApi.getOrderHistory(orderNumber),
    enabled: !!orderNumber,
    staleTime: STALE_TIMES.SHORT,
  })
}

export function useCreateOrder() {
  const qc = useQueryClient()

  return useMutation({
    mutationFn: orderApi.createOrder,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: [QUERY_KEYS.ORDERS] })
    },
    onError: (err) => {
      toast.error(normalizeError(err).message)
    },
  })
}

export function useConfirmOrder() {
  const qc = useQueryClient()

  return useMutation({
    mutationFn: orderApi.confirmOrder,
    onSuccess: (data) => {
      qc.invalidateQueries({ queryKey: [QUERY_KEYS.ORDER, data.orderNumber] })
      qc.invalidateQueries({ queryKey: [QUERY_KEYS.ORDERS] })
      toast.success('Order confirmed')
    },
    onError: (err) => {
      toast.error(normalizeError(err).message)
    },
  })
}

export function useCancelOrder() {
  const qc = useQueryClient()

  return useMutation({
    mutationFn: ({ orderNumber, reason }) => orderApi.cancelOrder(orderNumber, reason),
    onSuccess: (data) => {
      qc.invalidateQueries({ queryKey: [QUERY_KEYS.ORDER, data.orderNumber] })
      qc.invalidateQueries({ queryKey: [QUERY_KEYS.ORDERS] })
      toast.success('Order cancelled')
    },
    onError: (err) => {
      toast.error(normalizeError(err).message)
    },
  })
}
