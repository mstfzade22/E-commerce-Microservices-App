import apiClient from './axios'
import { API_ROUTES } from '../utils/constants'

export function getUserById(userId) {
  return apiClient.get(API_ROUTES.USERS_BY_ID(userId)).then((r) => r.data)
}

export function lockUser(userId) {
  return apiClient.post(API_ROUTES.USERS_LOCK(userId)).then((r) => r.data)
}

export function unlockUser(userId) {
  return apiClient.post(API_ROUTES.USERS_UNLOCK(userId)).then((r) => r.data)
}

export function disableUser(userId) {
  return apiClient.post(API_ROUTES.USERS_DISABLE(userId)).then((r) => r.data)
}

export function enableUser(userId) {
  return apiClient.post(API_ROUTES.USERS_ENABLE(userId)).then((r) => r.data)
}
