import { useQuery } from '@tanstack/react-query'
import { getProducts, getProductBySlug, getProductById } from '../api/productApi'
import { QUERY_KEYS, STALE_TIMES } from '../utils/constants'

export function useProducts(params = {}) {
  return useQuery({
    queryKey: [QUERY_KEYS.PRODUCTS, params],
    queryFn: () => getProducts(params),
    staleTime: STALE_TIMES.SHORT,
  })
}

export function useProductBySlug(slug) {
  return useQuery({
    queryKey: [QUERY_KEYS.PRODUCT, 'slug', slug],
    queryFn: () => getProductBySlug(slug),
    enabled: !!slug,
    staleTime: STALE_TIMES.MEDIUM,
  })
}

export function useProductById(id) {
  return useQuery({
    queryKey: [QUERY_KEYS.PRODUCT, id],
    queryFn: () => getProductById(id),
    enabled: !!id,
    staleTime: STALE_TIMES.MEDIUM,
  })
}
