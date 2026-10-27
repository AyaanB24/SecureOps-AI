/**
 * MetricCard Component
 * Displays a single metric with optional status icon and loading/error states
 */

import React from 'react'
import { LoadingSpinner } from './LoadingSpinner'

export interface MetricCardProps {
  label: string
  value: string | number
  isLoading?: boolean
  error?: Error | null
  icon?: React.ReactNode
  variant?: 'default' | 'pass' | 'block' | 'warning'
  onClick?: () => void
}

const variantStyles = {
  default: 'text-gray-900',
  pass: 'text-green-600',
  block: 'text-red-600',
  warning: 'text-amber-600',
}

export const MetricCard: React.FC<MetricCardProps> = ({
  label,
  value,
  isLoading = false,
  error = null,
  icon,
  variant = 'default',
  onClick,
}) => {
  const handleClick = onClick ? { onClick, role: 'button', tabIndex: 0 } : {}

  return (
    <div
      className={`bg-white border border-gray-200 rounded-lg p-6 ${
        onClick ? 'cursor-pointer hover:shadow-md transition-shadow' : ''
      }`}
      {...handleClick}
    >
      <div className="flex items-start justify-between">
        <div className="flex-1">
          <p className="text-sm font-medium text-gray-600 mb-2">{label}</p>
          {isLoading ? (
            <div className="flex items-center">
              <LoadingSpinner size="sm" />
            </div>
          ) : error ? (
            <p className="text-xs text-red-600">Error loading metric</p>
          ) : (
            <p className={`text-3xl font-bold ${variantStyles[variant]}`}>
              {value}
            </p>
          )}
        </div>
        {icon && <div className="text-gray-400">{icon}</div>}
      </div>
    </div>
  )
}
