import apiClient from './axios'
import { API_ROUTES } from '../utils/constants'

export function initiatePayment(orderNumber) {
  return apiClient.post(API_ROUTES.PAYMENTS_INITIATE, { orderNumber }).then((r) => r.data)
}

export function getPaymentByOrder(orderNumber) {
  return apiClient.get(API_ROUTES.PAYMENT_BY_ORDER(orderNumber)).then((r) => r.data)
}

export function getMyPayments(params = {}) {
  return apiClient.get(API_ROUTES.PAYMENTS_MY, { params }).then((r) => r.data)
}

export function getPaymentHistory(paymentId) {
  return apiClient.get(API_ROUTES.PAYMENT_HISTORY(paymentId)).then((r) => r.data)
}
