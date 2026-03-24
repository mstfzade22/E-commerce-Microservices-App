import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import * as cartApi from '../api/cartApi'
import { normalizeError } from '../api/errorNormalizer'
import { QUERY_KEYS, STALE_TIMES } from '../utils/constants'
import { useAuth } from './useAuth'

export function useCart() {
  const { isAuthenticated } = useAuth()

  return useQuery({
    queryKey: QUERY_KEYS.CART,
    queryFn: cartApi.getCart,
    enabled: isAuthenticated,
    staleTime: STALE_TIMES.SHORT,
  })
}

export function useCartSummary() {
  const { isAuthenticated } = useAuth()

  return useQuery({
    queryKey: QUERY_KEYS.CART_SUMMARY,
    queryFn: cartApi.getCartSummary,
    enabled: isAuthenticated,
    staleTime: STALE_TIMES.SHORT,
  })
}

export function useAddToCart() {
  const qc = useQueryClient()

  return useMutation({
    mutationFn: cartApi.addCartItem,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: QUERY_KEYS.CART })
      qc.invalidateQueries({ queryKey: QUERY_KEYS.CART_SUMMARY })
      toast.success('Added to cart')
    },
    onError: (err) => {
      toast.error(normalizeError(err).message)
    },
  })
}

export function useUpdateCartItem() {
  const qc = useQueryClient()

  return useMutation({
    mutationFn: ({ productId, quantity }) => cartApi.updateCartItem(productId, quantity),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: QUERY_KEYS.CART })
      qc.invalidateQueries({ queryKey: QUERY_KEYS.CART_SUMMARY })
    },
    onError: (err) => {
      toast.error(normalizeError(err).message)
    },
  })
}

export function useRemoveCartItem() {
  const qc = useQueryClient()

  return useMutation({
    mutationFn: cartApi.removeCartItem,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: QUERY_KEYS.CART })
      qc.invalidateQueries({ queryKey: QUERY_KEYS.CART_SUMMARY })
      toast.success('Item removed')
    },
    onError: (err) => {
      toast.error(normalizeError(err).message)
    },
  })
}

export function useClearCart() {
  const qc = useQueryClient()

  return useMutation({
    mutationFn: cartApi.clearCart,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: QUERY_KEYS.CART })
      qc.invalidateQueries({ queryKey: QUERY_KEYS.CART_SUMMARY })
      toast.success('Cart cleared')
    },
    onError: (err) => {
      toast.error(normalizeError(err).message)
    },
  })
}

export function useValidateCart() {
  return useQuery({
    queryKey: QUERY_KEYS.CART_VALIDATION,
    queryFn: cartApi.validateCart,
    enabled: false,
  })
}
