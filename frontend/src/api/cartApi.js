import apiClient from './axios'
import { API_ROUTES } from '../utils/constants'

export function getCart() {
  return apiClient.get(API_ROUTES.CART).then((r) => r.data)
}

export function addCartItem({ productId, quantity }) {
  return apiClient.post(API_ROUTES.CART_ITEMS, { productId, quantity }).then((r) => r.data)
}

export function updateCartItem(productId, quantity) {
  return apiClient.put(API_ROUTES.CART_ITEM(productId), { quantity }).then((r) => r.data)
}

export function removeCartItem(productId) {
  return apiClient.delete(API_ROUTES.CART_ITEM(productId)).then((r) => r.data)
}

export function clearCart() {
  return apiClient.delete(API_ROUTES.CART).then((r) => r.data)
}

export function getCartSummary() {
  return apiClient.get(API_ROUTES.CART_SUMMARY).then((r) => r.data)
}

export function validateCart() {
  return apiClient.post(API_ROUTES.CART_VALIDATE).then((r) => r.data)
}
