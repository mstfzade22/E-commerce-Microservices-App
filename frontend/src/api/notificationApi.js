import apiClient from './axios'
import { API_ROUTES } from '../utils/constants'

export function getNotifications(params = {}) {
  return apiClient.get(API_ROUTES.NOTIFICATIONS, { params }).then((r) => r.data)
}

export function getFailedNotifications(params = {}) {
  return apiClient.get(API_ROUTES.NOTIFICATIONS_FAILED, { params }).then((r) => r.data)
}

export function retryNotification(id) {
  return apiClient.post(API_ROUTES.NOTIFICATION_RETRY(id)).then((r) => r.data)
}
