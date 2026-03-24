const dateFormatter = new Intl.DateTimeFormat('en-US', {
  year: 'numeric',
  month: 'short',
  day: 'numeric',
})

const dateTimeFormatter = new Intl.DateTimeFormat('en-US', {
  year: 'numeric',
  month: 'short',
  day: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
})

export function formatCurrency(amount, currency = 'USD') {
  if (amount == null) return '-'
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency,
    minimumFractionDigits: 2,
  }).format(amount)
}

export function formatDate(isoString) {
  if (!isoString) return '-'
  return dateFormatter.format(new Date(isoString))
}

export function formatDateTime(isoString) {
  if (!isoString) return '-'
  return dateTimeFormatter.format(new Date(isoString))
}

const SECONDS = 1
const MINUTES = 60 * SECONDS
const HOURS = 60 * MINUTES
const DAYS = 24 * HOURS

export function formatRelativeTime(isoString) {
  if (!isoString) return '-'

  const now = Date.now()
  const then = new Date(isoString).getTime()
  const diffSeconds = Math.floor((now - then) / 1000)

  if (diffSeconds < 0) return 'just now'
  if (diffSeconds < MINUTES) return 'just now'
  if (diffSeconds < HOURS) {
    const mins = Math.floor(diffSeconds / MINUTES)
    return `${mins}m ago`
  }
  if (diffSeconds < DAYS) {
    const hrs = Math.floor(diffSeconds / HOURS)
    return `${hrs}h ago`
  }
  if (diffSeconds < 30 * DAYS) {
    const days = Math.floor(diffSeconds / DAYS)
    return `${days}d ago`
  }

  return formatDate(isoString)
}

export function truncateText(text, maxLength = 100) {
  if (!text) return ''
  if (text.length <= maxLength) return text
  return text.slice(0, maxLength).trimEnd() + '...'
}

export function formatStatus(status) {
  if (!status) return '-'
  return status.replace(/_/g, ' ')
}

export function slugify(text) {
  return text
    .toLowerCase()
    .trim()
    .replace(/[^\w\s-]/g, '')
    .replace(/[\s_]+/g, '-')
    .replace(/-+/g, '-')
}
