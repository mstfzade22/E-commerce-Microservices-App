export function normalizeError(axiosError) {
  if (!axiosError.response) {
    return {
      status: 0,
      message: axiosError.message || 'Network error. Please check your connection.',
      validationErrors: null,
      traceId: null,
      raw: axiosError,
    }
  }

  const { status, data } = axiosError.response

  if (!data || typeof data !== 'object') {
    return {
      status,
      message: `Request failed with status ${status}`,
      validationErrors: null,
      traceId: null,
      raw: axiosError,
    }
  }

  const validationErrors = data.validationErrors || data.fieldErrors || null

  const message =
    data.message ||
    data.error ||
    `Request failed with status ${status}`

  return {
    status,
    message,
    validationErrors,
    traceId: data.traceId || null,
    raw: axiosError,
  }
}

export function isNetworkError(error) {
  return error?.status === 0
}

export function isAuthError(error) {
  return error?.status === 401
}

export function isForbiddenError(error) {
  return error?.status === 403
}

export function isValidationError(error) {
  return error?.status === 400 || error?.status === 422
}

export function hasFieldErrors(error) {
  return error?.validationErrors != null && Object.keys(error.validationErrors).length > 0
}
