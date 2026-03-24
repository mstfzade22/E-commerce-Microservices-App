export const API_ROUTES = {
  AUTH_LOGIN: '/api/v1/auth/login',
  AUTH_REGISTER: '/api/v1/auth/register',
  AUTH_REFRESH: '/api/v1/auth/refresh',
  AUTH_LOGOUT: '/api/v1/auth/logout',
  AUTH_VALIDATE: '/api/v1/auth/validate',

  USERS_ME: '/api/v1/users/me',
  USERS_PASSWORD: '/api/v1/users/password',
  USERS_BY_ID: (userId) => `/api/v1/users/${userId}`,
  USERS_LOCK: (userId) => `/api/v1/users/${userId}/lock`,
  USERS_UNLOCK: (userId) => `/api/v1/users/${userId}/unlock`,
  USERS_DISABLE: (userId) => `/api/v1/users/${userId}/disable`,
  USERS_ENABLE: (userId) => `/api/v1/users/${userId}/enable`,

  PRODUCTS: '/api/v1/products',
  PRODUCT_BY_ID: (id) => `/api/v1/products/${id}`,
  PRODUCT_BY_SLUG: (slug) => `/api/v1/products/slug/${slug}`,
  PRODUCT_IMAGES: (productId) => `/api/v1/products/${productId}/images`,
  PRODUCT_IMAGE: (productId, imageId) => `/api/v1/products/${productId}/images/${imageId}`,
  PRODUCT_IMAGE_PRIMARY: (productId, imageId) => `/api/v1/products/${productId}/images/${imageId}/primary`,
  PRODUCT_IMAGES_REORDER: (productId) => `/api/v1/products/${productId}/images/reorder`,

  CATEGORIES: '/api/v1/categories',
  CATEGORIES_TREE: '/api/v1/categories/tree',
  CATEGORY_BY_ID: (id) => `/api/v1/categories/${id}`,
  CATEGORY_BY_SLUG: (slug) => `/api/v1/categories/slug/${slug}`,
  CATEGORY_CHILDREN: (parentId) => `/api/v1/categories/${parentId}/children`,

  CART: '/api/v1/cart',
  CART_ITEMS: '/api/v1/cart/items',
  CART_ITEM: (productId) => `/api/v1/cart/items/${productId}`,
  CART_SUMMARY: '/api/v1/cart/summary',
  CART_VALIDATE: '/api/v1/cart/validate',

  ORDERS: '/api/v1/orders',
  ORDER_BY_NUMBER: (orderNumber) => `/api/v1/orders/${orderNumber}`,
  ORDER_CONFIRM: (orderNumber) => `/api/v1/orders/${orderNumber}/confirm`,
  ORDER_CANCEL: (orderNumber) => `/api/v1/orders/${orderNumber}/cancel`,
  ORDER_HISTORY: (orderNumber) => `/api/v1/orders/${orderNumber}/history`,
  ORDER_PROCESS: (orderNumber) => `/api/v1/orders/${orderNumber}/process`,
  ORDER_SHIP: (orderNumber) => `/api/v1/orders/${orderNumber}/ship`,
  ORDER_DELIVER: (orderNumber) => `/api/v1/orders/${orderNumber}/deliver`,
  ORDERS_BY_STATUS: (status) => `/api/v1/orders/status/${status}`,

  PAYMENTS_INITIATE: '/api/v1/payments/initiate',
  PAYMENT_BY_ID: (paymentId) => `/api/v1/payments/${paymentId}`,
  PAYMENT_BY_ORDER: (orderNumber) => `/api/v1/payments/order/${orderNumber}`,
  PAYMENTS_MY: '/api/v1/payments/my',
  PAYMENT_HISTORY: (paymentId) => `/api/v1/payments/${paymentId}/history`,
  PAYMENT_REFUND: (paymentId) => `/api/v1/payments/${paymentId}/refund`,

  INVENTORY_BY_PRODUCT: (productId) => `/api/v1/inventory/${productId}`,
  INVENTORY_STATUS: (productId) => `/api/v1/inventory/${productId}/status`,
  INVENTORY_STOCK: (productId) => `/api/v1/inventory/${productId}/stock`,
  INVENTORY_LOW_STOCK: '/api/v1/inventory/low-stock',

  NOTIFICATIONS: '/api/v1/notifications',
  NOTIFICATIONS_BY_USER: (userId) => `/api/v1/notifications/user/${userId}`,
  NOTIFICATIONS_FAILED: '/api/v1/notifications/failed',
  NOTIFICATION_RETRY: (id) => `/api/v1/notifications/${id}/retry`,
  NOTIFICATIONS_STREAM: '/api/v1/notifications/stream',
}

export const QUERY_KEYS = {
  AUTH_USER: ['auth', 'user'],
  PRODUCTS: 'products',
  PRODUCT: 'product',
  CATEGORIES: 'categories',
  CATEGORIES_TREE: ['categories', 'tree'],
  CART: ['cart'],
  CART_SUMMARY: ['cart', 'summary'],
  CART_VALIDATION: ['cart', 'validation'],
  ORDERS: 'orders',
  ORDER: 'order',
  ORDER_HISTORY: 'order-history',
  PAYMENTS: 'payments',
  PAYMENT: 'payment',
  PAYMENT_HISTORY: 'payment-history',
  INVENTORY: 'inventory',
  INVENTORY_LOW_STOCK: ['inventory', 'low-stock'],
  NOTIFICATIONS: 'notifications',
}

export const ROLES = {
  CUSTOMER: 'CUSTOMER',
  STORE: 'STORE',
  ADMIN: 'ADMIN',
}

export const ADMIN_ROLES = [ROLES.ADMIN, ROLES.STORE]

export const ORDER_STATUSES = ['PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED', 'REFUNDED']

export const PAYMENT_STATUSES = ['INITIATED', 'PROCESSING', 'APPROVED', 'DECLINED', 'CANCELLED', 'REFUNDED', 'ERROR']

export const STOCK_STATUSES = ['AVAILABLE', 'LOW_STOCK', 'OUT_OF_STOCK']

export const STATUS_COLORS = {
  PENDING: 'bg-yellow-100 text-yellow-800',
  CONFIRMED: 'bg-blue-100 text-blue-800',
  PROCESSING: 'bg-purple-100 text-purple-800',
  SHIPPED: 'bg-indigo-100 text-indigo-800',
  DELIVERED: 'bg-green-100 text-green-800',
  CANCELLED: 'bg-red-100 text-red-800',
  REFUNDED: 'bg-gray-100 text-gray-800',

  INITIATED: 'bg-yellow-100 text-yellow-800',
  APPROVED: 'bg-green-100 text-green-800',
  DECLINED: 'bg-red-100 text-red-800',
  ERROR: 'bg-red-100 text-red-800',

  AVAILABLE: 'bg-green-100 text-green-800',
  LOW_STOCK: 'bg-yellow-100 text-yellow-800',
  OUT_OF_STOCK: 'bg-red-100 text-red-800',

  SENT: 'bg-green-100 text-green-800',
  FAILED: 'bg-red-100 text-red-800',
}

export const PAGINATION_DEFAULTS = {
  PAGE: 0,
  SIZE: 20,
}

export const STALE_TIMES = {
  SHORT: 1000 * 60,
  MEDIUM: 1000 * 60 * 5,
  LONG: 1000 * 60 * 15,
}
