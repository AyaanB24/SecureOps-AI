import React from 'react'
import { AlertCircle, RotateCcw } from 'lucide-react'
import { Button } from './Button'

interface ErrorStateProps {
  title?: string
  message: string
  onRetry?: () => void
  fullHeight?: boolean
}

export const ErrorState: React.FC<ErrorStateProps> = ({
  title = 'Error',
  message,
  onRetry,
  fullHeight = true,
}) => {
  return (
    <div className={fullHeight ? 'min-h-[calc(100vh-256px)]' : ''}>
      <div className="flex items-center justify-center py-12">
        <div className="text-center max-w-md">
          <AlertCircle className="w-12 h-12 text-status-block mx-auto mb-4" />
          <h3 className="text-lg font-semibold text-gray-900 mb-2">{title}</h3>
          <p className="text-gray-600 mb-6">{message}</p>
          {onRetry && (
            <Button variant="secondary" onClick={onRetry} className="inline-flex items-center gap-2">
              <RotateCcw className="w-4 h-4" />
              Try again
            </Button>
          )}
        </div>
      </div>
    </div>
  )
}

interface EmptyStateProps {
  title: string
  description?: string
  action?: React.ReactNode
  fullHeight?: boolean
}

export const EmptyState: React.FC<EmptyStateProps> = ({ title, description, action, fullHeight = true }) => {
  return (
    <div className={fullHeight ? 'min-h-[calc(100vh-256px)]' : ''}>
      <div className="flex items-center justify-center py-12">
        <div className="text-center max-w-md">
          <h3 className="text-lg font-semibold text-gray-900 mb-2">{title}</h3>
          {description && <p className="text-gray-600 mb-6">{description}</p>}
          {action}
        </div>
      </div>
    </div>
  )
}
