import apiClient from './axios'
import { API_ROUTES } from '../utils/constants'

export function getProducts(params = {}) {
  return apiClient.get(API_ROUTES.PRODUCTS, { params }).then((r) => r.data)
}

export function getProductById(id) {
  return apiClient.get(API_ROUTES.PRODUCT_BY_ID(id)).then((r) => r.data)
}

export function getProductBySlug(slug) {
  return apiClient.get(API_ROUTES.PRODUCT_BY_SLUG(slug)).then((r) => r.data)
}
