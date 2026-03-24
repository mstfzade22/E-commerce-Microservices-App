import { useQuery } from '@tanstack/react-query'
import { getCategories, getCategoryTree, getCategoryBySlug } from '../api/categoryApi'
import { QUERY_KEYS, STALE_TIMES } from '../utils/constants'

export function useCategories() {
  return useQuery({
    queryKey: [QUERY_KEYS.CATEGORIES],
    queryFn: getCategories,
    staleTime: STALE_TIMES.LONG,
  })
}

export function useCategoryTree() {
  return useQuery({
    queryKey: QUERY_KEYS.CATEGORIES_TREE,
    queryFn: getCategoryTree,
    staleTime: STALE_TIMES.LONG,
  })
}

export function useCategoryBySlug(slug) {
  return useQuery({
    queryKey: [QUERY_KEYS.CATEGORIES, 'slug', slug],
    queryFn: () => getCategoryBySlug(slug),
    enabled: !!slug,
    staleTime: STALE_TIMES.LONG,
  })
}
