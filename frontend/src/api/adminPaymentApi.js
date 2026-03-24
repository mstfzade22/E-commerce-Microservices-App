import apiClient from './axios'
import { API_ROUTES } from '../utils/constants'

export function refundPayment(paymentId) {
  return apiClient.post(API_ROUTES.PAYMENT_REFUND(paymentId)).then((r) => r.data)
}
