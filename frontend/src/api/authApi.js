import apiClient from './axios'
import { API_ROUTES } from '../utils/constants'

export function login({ username, password }) {
  return apiClient.post(API_ROUTES.AUTH_LOGIN, { username, password }).then((r) => r.data)
}

export function register({ username, email, password, firstName, lastName, role }) {
  return apiClient
    .post(API_ROUTES.AUTH_REGISTER, { username, email, password, firstName, lastName, role })
    .then((r) => r.data)
}

export function logout() {
  return apiClient.post(API_ROUTES.AUTH_LOGOUT).then((r) => r.data)
}

export function refreshToken() {
  return apiClient.post(API_ROUTES.AUTH_REFRESH).then((r) => r.data)
}

export function validateSession() {
  return apiClient.get(API_ROUTES.AUTH_VALIDATE).then((r) => r.data)
}

export function fetchCurrentUser() {
  return apiClient.get(API_ROUTES.USERS_ME).then((r) => r.data)
}
