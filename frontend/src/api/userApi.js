import apiClient from './axios'
import { API_ROUTES } from '../utils/constants'

export function updateProfile(data) {
  return apiClient.put(API_ROUTES.USERS_ME, data).then((r) => r.data)
}

export function changePassword(data) {
  return apiClient.post(API_ROUTES.USERS_PASSWORD, data).then((r) => r.data)
}
