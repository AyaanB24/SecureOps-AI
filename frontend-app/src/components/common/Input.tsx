import React from 'react'
import clsx from 'clsx'

interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string
  error?: string
  helperText?: string
}

export const Input: React.FC<InputProps> = ({ label, error, helperText, className, ...props }) => {
  return (
    <div className="w-full">
      {label && <label className="block text-sm font-medium text-gray-900 mb-2">{label}</label>}
      <input
        {...props}
        className={clsx(
          'w-full px-4 py-2 border border-gray-200 rounded-lg',
          'bg-white text-gray-900 placeholder-gray-500',
          'focus:outline-none focus:ring-2 focus:ring-gray-900 focus:border-transparent',
          'transition-colors disabled:bg-gray-50 disabled:cursor-not-allowed',
          error && 'border-status-block focus:ring-status-block',
          className
        )}
      />
      {error && <p className="text-sm text-status-block mt-1">{error}</p>}
      {helperText && !error && <p className="text-sm text-gray-500 mt-1">{helperText}</p>}
    </div>
  )
}

interface TextareaProps extends React.TextareaHTMLAttributes<HTMLTextAreaElement> {
  label?: string
  error?: string
}

export const Textarea: React.FC<TextareaProps> = ({ label, error, className, ...props }) => {
  return (
    <div className="w-full">
      {label && <label className="block text-sm font-medium text-gray-900 mb-2">{label}</label>}
      <textarea
        {...props}
        className={clsx(
          'w-full px-4 py-2 border border-gray-200 rounded-lg',
          'bg-white text-gray-900 placeholder-gray-500',
          'focus:outline-none focus:ring-2 focus:ring-gray-900 focus:border-transparent',
          'transition-colors disabled:bg-gray-50 disabled:cursor-not-allowed resize-none',
          error && 'border-status-block focus:ring-status-block',
          className
        )}
      />
      {error && <p className="text-sm text-status-block mt-1">{error}</p>}
    </div>
  )
}
