import apiClient from './axios'
import { API_ROUTES } from '../utils/constants'

export function createProduct(data) {
  return apiClient.post(API_ROUTES.PRODUCTS, data).then((r) => r.data)
}

export function updateProduct(id, data) {
  return apiClient.put(API_ROUTES.PRODUCT_BY_ID(id), data).then((r) => r.data)
}

export function deleteProduct(id) {
  return apiClient.delete(API_ROUTES.PRODUCT_BY_ID(id))
}

export function uploadProductImage(productId, formData) {
  return apiClient
    .post(API_ROUTES.PRODUCT_IMAGES(productId), formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    .then((r) => r.data)
}

export function deleteProductImage(productId, imageId) {
  return apiClient.delete(API_ROUTES.PRODUCT_IMAGE(productId, imageId))
}

export function setImagePrimary(productId, imageId) {
  return apiClient.put(API_ROUTES.PRODUCT_IMAGE_PRIMARY(productId, imageId)).then((r) => r.data)
}

export function reorderImages(productId, imageIds) {
  return apiClient.put(API_ROUTES.PRODUCT_IMAGES_REORDER(productId), imageIds).then((r) => r.data)
}
