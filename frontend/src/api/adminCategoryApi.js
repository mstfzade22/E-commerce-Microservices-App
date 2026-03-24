import apiClient from './axios'
import { API_ROUTES } from '../utils/constants'

export function createCategory(data) {
  return apiClient.post(API_ROUTES.CATEGORIES, data).then((r) => r.data)
}

export function updateCategory(id, data) {
  return apiClient.put(API_ROUTES.CATEGORY_BY_ID(id), data).then((r) => r.data)
}

export function deleteCategory(id) {
  return apiClient.delete(API_ROUTES.CATEGORY_BY_ID(id))
}
