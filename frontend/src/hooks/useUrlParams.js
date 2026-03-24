import { useSearchParams } from 'react-router'
import { useCallback } from 'react'
import { PAGINATION_DEFAULTS } from '../utils/constants'

export function useProductFilters() {
  const [searchParams, setSearchParams] = useSearchParams()

  const filters = {
    keyword: searchParams.get('keyword') || '',
    categoryId: searchParams.get('categoryId') || '',
    minPrice: searchParams.get('minPrice') || '',
    maxPrice: searchParams.get('maxPrice') || '',
    stockStatus: searchParams.get('stockStatus') || '',
    page: Number(searchParams.get('page')) || PAGINATION_DEFAULTS.PAGE,
    size: Number(searchParams.get('size')) || PAGINATION_DEFAULTS.SIZE,
    sortBy: searchParams.get('sortBy') || '',
    sortDir: searchParams.get('sortDir') || '',
  }

  const setFilters = useCallback(
    (updates) => {
      setSearchParams((prev) => {
        const next = new URLSearchParams(prev)

        if ('page' in updates === false) {
          next.set('page', '0')
        }

        Object.entries(updates).forEach(([key, value]) => {
          if (value === '' || value === null || value === undefined) {
            next.delete(key)
          } else {
            next.set(key, String(value))
          }
        })

        return next
      })
    },
    [setSearchParams],
  )

  const apiParams = {}
  Object.entries(filters).forEach(([key, value]) => {
    if (value !== '' && value !== 0 && value !== null) {
      apiParams[key] = value
    }
  })
  if (!apiParams.page) apiParams.page = PAGINATION_DEFAULTS.PAGE
  if (!apiParams.size) apiParams.size = PAGINATION_DEFAULTS.SIZE

  return { filters, setFilters, apiParams }
}
