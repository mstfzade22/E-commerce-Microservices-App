import { API_ROUTES } from '../utils/constants'
import { normalizeError } from './errorNormalizer'

let isRefreshing = false
let failedQueue = []

function processQueue(error) {
  failedQueue.forEach(({ resolve, reject }) => {
    if (error) {
      reject(error)
    } else {
      resolve()
    }
  })
  failedQueue = []
}

export function setupAuthInterceptor(apiClient) {
  apiClient.interceptors.response.use(
    (response) => response,
    async (error) => {
      const originalRequest = error.config

      if (!error.response || error.response.status !== 401) {
        return Promise.reject(normalizeError(error))
      }

      const requestUrl = originalRequest.url || ''
      if (
        requestUrl.includes(API_ROUTES.AUTH_REFRESH) ||
        requestUrl.includes(API_ROUTES.AUTH_LOGIN) ||
        originalRequest._retry
      ) {
        return Promise.reject(normalizeError(error))
      }

      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject })
        }).then(() => apiClient(originalRequest))
      }

      isRefreshing = true
      originalRequest._retry = true

      try {
        await apiClient.post(API_ROUTES.AUTH_REFRESH)
        processQueue(null)
        return apiClient(originalRequest)
      } catch (refreshError) {
        processQueue(refreshError)
        window.dispatchEvent(new CustomEvent('auth:session-expired'))
        return Promise.reject(normalizeError(refreshError))
      } finally {
        isRefreshing = false
      }
    },
  )
}
