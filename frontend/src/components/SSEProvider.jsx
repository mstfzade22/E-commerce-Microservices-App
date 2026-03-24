import { useSSE } from '../hooks/useSSE'

export function SSEProvider({ children }) {
  useSSE()
  return children
}
