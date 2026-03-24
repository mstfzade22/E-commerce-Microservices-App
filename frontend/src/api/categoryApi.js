import apiClient from './axios'
import { API_ROUTES } from '../utils/constants'

export function getCategories() {
  return apiClient.get(API_ROUTES.CATEGORIES).then((r) => r.data)
}

export function getCategoryTree() {
  return apiClient.get(API_ROUTES.CATEGORIES_TREE).then((r) => r.data)
}

export function getCategoryById(id) {
  return apiClient.get(API_ROUTES.CATEGORY_BY_ID(id)).then((r) => r.data)
}

export function getCategoryBySlug(slug) {
  return apiClient.get(API_ROUTES.CATEGORY_BY_SLUG(slug)).then((r) => r.data)
}
