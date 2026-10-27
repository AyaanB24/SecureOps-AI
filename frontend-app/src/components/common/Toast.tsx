import { useState, useEffect } from 'react'
import { AlertCircle, CheckCircle, AlertTriangle, Info, X } from 'lucide-react'

export type ToastType = 'success' | 'error' | 'warning' | 'info'

export interface Toast {
  id: string
  type: ToastType
  message: string
  duration?: number
}

interface ToastProps extends Toast {
  onClose: (id: string) => void
}

const typeConfig = {
  success: {
    bg: 'bg-green-50',
    border: 'border-green-200',
    text: 'text-green-800',
    icon: CheckCircle,
  },
  error: {
    bg: 'bg-red-50',
    border: 'border-red-200',
    text: 'text-red-800',
    icon: AlertCircle,
  },
  warning: {
    bg: 'bg-yellow-50',
    border: 'border-yellow-200',
    text: 'text-yellow-800',
    icon: AlertTriangle,
  },
  info: {
    bg: 'bg-blue-50',
    border: 'border-blue-200',
    text: 'text-blue-800',
    icon: Info,
  },
}

export function ToastItem({ id, type, message, duration = 5000, onClose }: ToastProps) {
  useEffect(() => {
    if (duration) {
      const timeout = setTimeout(() => onClose(id), duration)
      return () => clearTimeout(timeout)
    }
  }, [id, duration, onClose])

  const config = typeConfig[type]
  const Icon = config.icon

  return (
    <div
      className={`${config.bg} border ${config.border} ${config.text} px-4 py-3 rounded-lg flex items-center justify-between gap-3 animate-slideIn`}
      role="alert"
    >
      <div className="flex items-center gap-3">
        <Icon size={20} className="flex-shrink-0" />
        <p className="text-sm font-medium">{message}</p>
      </div>
      <button
        onClick={() => onClose(id)}
        className="flex-shrink-0 p-1 hover:opacity-70 transition"
        aria-label="Close notification"
      >
        <X size={18} />
      </button>
    </div>
  )
}

/**
 * Toast Container Component
 * Use in root layout to display all toasts
 */
interface ToastContainerProps {
  toasts: Toast[]
  onClose: (id: string) => void
}

export function ToastContainer({ toasts, onClose }: ToastContainerProps) {
  return (
    <div className="fixed bottom-4 right-4 space-y-2 z-50 max-w-md">
      {toasts.map(toast => (
        <ToastItem key={toast.id} {...toast} onClose={onClose} />
      ))}
    </div>
  )
}

/**
 * Hook to manage toast notifications
 */
export function useToast() {
  const [toasts, setToasts] = useState<Toast[]>([])

  const addToast = (type: ToastType, message: string, duration?: number) => {
    const id = Date.now().toString()
    setToasts(prev => [...prev, { id, type, message, duration }])
  }

  const removeToast = (id: string) => {
    setToasts(prev => prev.filter(t => t.id !== id))
  }

  const success = (message: string) => addToast('success', message)
  const error = (message: string) => addToast('error', message, 7000)
  const warning = (message: string) => addToast('warning', message)
  const info = (message: string) => addToast('info', message)

  return {
    toasts,
    addToast,
    removeToast,
    success,
    error,
    warning,
    info,
  }
}
