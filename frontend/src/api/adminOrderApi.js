import apiClient from './axios'
import { API_ROUTES } from '../utils/constants'

export function getOrdersByStatus(status, params = {}) {
  return apiClient.get(API_ROUTES.ORDERS_BY_STATUS(status), { params }).then((r) => r.data)
}

export function processOrder(orderNumber) {
  return apiClient.post(API_ROUTES.ORDER_PROCESS(orderNumber)).then((r) => r.data)
}

export function shipOrder(orderNumber) {
  return apiClient.post(API_ROUTES.ORDER_SHIP(orderNumber)).then((r) => r.data)
}

export function deliverOrder(orderNumber) {
  return apiClient.post(API_ROUTES.ORDER_DELIVER(orderNumber)).then((r) => r.data)
}
