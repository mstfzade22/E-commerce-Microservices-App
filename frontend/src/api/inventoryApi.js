import apiClient from './axios'
import { API_ROUTES } from '../utils/constants'

export function getInventory(productId) {
  return apiClient.get(API_ROUTES.INVENTORY_BY_PRODUCT(productId)).then((r) => r.data)
}

export function getInventoryStatus(productId) {
  return apiClient.get(API_ROUTES.INVENTORY_STATUS(productId)).then((r) => r.data)
}

export function updateStock(productId, data) {
  return apiClient.put(API_ROUTES.INVENTORY_STOCK(productId), data).then((r) => r.data)
}

export function getLowStock() {
  return apiClient.get(API_ROUTES.INVENTORY_LOW_STOCK).then((r) => r.data)
}
