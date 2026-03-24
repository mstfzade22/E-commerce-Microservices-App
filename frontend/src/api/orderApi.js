import apiClient from './axios'
import { API_ROUTES } from '../utils/constants'

export function createOrder(shippingData) {
  return apiClient.post(API_ROUTES.ORDERS, shippingData).then((r) => r.data)
}

export function getOrders(params = {}) {
  return apiClient.get(API_ROUTES.ORDERS, { params }).then((r) => r.data)
}

export function getOrderByNumber(orderNumber) {
  return apiClient.get(API_ROUTES.ORDER_BY_NUMBER(orderNumber)).then((r) => r.data)
}

export function confirmOrder(orderNumber) {
  return apiClient.post(API_ROUTES.ORDER_CONFIRM(orderNumber)).then((r) => r.data)
}

export function cancelOrder(orderNumber, reason) {
  return apiClient.post(API_ROUTES.ORDER_CANCEL(orderNumber), { reason }).then((r) => r.data)
}

export function getOrderHistory(orderNumber) {
  return apiClient.get(API_ROUTES.ORDER_HISTORY(orderNumber)).then((r) => r.data)
}
