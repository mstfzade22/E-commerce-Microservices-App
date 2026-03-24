import { useEffect, useRef } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import { useAuth } from './useAuth'
import { QUERY_KEYS } from '../utils/constants'

const SSE_URL = import.meta.env.VITE_SSE_URL || '/api/v1/notifications/stream'

const EVENT_HANDLERS = {
  ORDER_CONFIRMED: { message: 'Your order has been confirmed', keys: [QUERY_KEYS.ORDERS] },
  ORDER_SHIPPED: { message: 'Your order has been shipped', keys: [QUERY_KEYS.ORDERS] },
  ORDER_DELIVERED: { message: 'Your order has been delivered', keys: [QUERY_KEYS.ORDERS] },
  PAYMENT_SUCCESS: { message: 'Payment successful', keys: [QUERY_KEYS.ORDERS, QUERY_KEYS.PAYMENTS] },
  PAYMENT_FAILED: { message: 'Payment failed', keys: [QUERY_KEYS.PAYMENTS], isError: true },
  PAYMENT_REFUNDED: { message: 'Payment refunded', keys: [QUERY_KEYS.PAYMENTS] },
  LOW_STOCK_ALERT: { message: 'Low stock alert', keys: [QUERY_KEYS.INVENTORY_LOW_STOCK] },
}

export function useSSE() {
  const { isAuthenticated } = useAuth()
  const qc = useQueryClient()
  const retryDelay = useRef(1000)
  const esRef = useRef(null)

  useEffect(() => {
    if (!isAuthenticated) {
      cleanup()
      return
    }

    connect()

    return cleanup

    function connect() {
      cleanup()

      const es = new EventSource(SSE_URL, { withCredentials: true })
      esRef.current = es

      es.addEventListener('connected', () => {
        retryDelay.current = 1000
      })

      Object.entries(EVENT_HANDLERS).forEach(([eventType, config]) => {
        es.addEventListener(eventType, (event) => {
          let text = config.message
          try {
            const data = JSON.parse(event.data)
            if (data.referenceId) text = `${config.message}: ${data.referenceId}`
          } catch { /* ignore parse errors */ }

          if (config.isError) {
            toast.error(text)
          } else {
            toast.success(text)
          }

          config.keys.forEach((key) => {
            qc.invalidateQueries({ queryKey: Array.isArray(key) ? key : [key] })
          })
          qc.invalidateQueries({ queryKey: [QUERY_KEYS.NOTIFICATIONS] })
        })
      })

      es.onerror = () => {
        cleanup()
        const delay = retryDelay.current
        retryDelay.current = Math.min(delay * 2, 30000)
        setTimeout(connect, delay)
      }
    }

    function cleanup() {
      if (esRef.current) {
        esRef.current.close()
        esRef.current = null
      }
    }
  }, [isAuthenticated, qc])
}
