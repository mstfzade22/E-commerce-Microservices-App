import { AlertCircle, CheckCircle, Info, XCircle } from 'lucide-react'

const VARIANTS = {
  error: { icon: XCircle, bg: 'bg-red-50', text: 'text-red-800', iconColor: 'text-red-400' },
  success: { icon: CheckCircle, bg: 'bg-green-50', text: 'text-green-800', iconColor: 'text-green-400' },
  warning: { icon: AlertCircle, bg: 'bg-yellow-50', text: 'text-yellow-800', iconColor: 'text-yellow-400' },
  info: { icon: Info, bg: 'bg-blue-50', text: 'text-blue-800', iconColor: 'text-blue-400' },
}

export function Alert({ variant = 'error', message }) {
  if (!message) return null

  const { icon: IconComponent, bg, text, iconColor } = VARIANTS[variant]

  return (
    <div className={`flex items-start gap-3 rounded-md p-3 ${bg}`}>
      <IconComponent className={`h-5 w-5 shrink-0 ${iconColor}`} />
      <p className={`text-sm ${text}`}>{message}</p>
    </div>
  )
}
