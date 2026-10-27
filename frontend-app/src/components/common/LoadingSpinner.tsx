import React from 'react'
import clsx from 'clsx'

interface LoadingSpinnerProps {
  size?: 'sm' | 'md' | 'lg'
  text?: string
}

export const LoadingSpinner: React.FC<LoadingSpinnerProps> = ({ size = 'md', text }) => {
  const sizeClasses = {
    sm: 'w-4 h-4',
    md: 'w-8 h-8',
    lg: 'w-12 h-12',
  }

  return (
    <div className="flex flex-col items-center justify-center gap-3 py-12">
      <div
        className={clsx(sizeClasses[size], 'border-2 border-gray-200 border-t-gray-900 rounded-full animate-spin')}
      />
      {text && <p className="text-gray-600 text-sm">{text}</p>}
    </div>
  )
}

interface LoadingPageProps {
  fullHeight?: boolean
}

export const LoadingPage: React.FC<LoadingPageProps> = ({ fullHeight = true }) => {
  return (
    <div className={clsx(fullHeight && 'min-h-[calc(100vh-256px)]', 'flex items-center justify-center')}>
      <LoadingSpinner size="lg" text="Loading..." />
    </div>
  )
}
