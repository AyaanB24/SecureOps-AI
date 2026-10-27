import React from 'react'
import clsx from 'clsx'

interface CardProps {
  children: React.ReactNode
  className?: string
  onClick?: () => void
}

export const Card: React.FC<CardProps> = ({ children, className, onClick }) => {
  return (
    <div
      onClick={onClick}
      className={clsx(
        'bg-white rounded-lg border border-gray-200 p-6',
        'transition-shadow hover:shadow-md',
        onClick && 'cursor-pointer',
        className
      )}
    >
      {children}
    </div>
  )
}

interface CardHeaderProps {
  title?: string
  subtitle?: string
  children?: React.ReactNode
}

export const CardHeader: React.FC<CardHeaderProps> = ({
  title,
  subtitle,
  children,
}) => {
  return (
    <div className="mb-4">
      {children ? (
        children
      ) : (
        <>
          <h3 className="text-lg font-semibold text-gray-900">{title}</h3>
          {subtitle && <p className="text-sm text-gray-600 mt-1">{subtitle}</p>}
        </>
      )}
    </div>
  )
}

interface CardBodyProps {
  children: React.ReactNode
}

export const CardBody: React.FC<CardBodyProps> = ({ children }) => {
  return <div className="text-gray-700">{children}</div>
}

interface CardFooterProps {
  children: React.ReactNode
}

export const CardFooter: React.FC<CardFooterProps> = ({ children }) => {
  return <div className="mt-6 pt-6 border-t border-gray-200 flex items-center gap-3">{children}</div>
}
