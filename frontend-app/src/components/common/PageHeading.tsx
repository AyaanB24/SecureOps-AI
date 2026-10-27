import React from 'react'

interface PageHeadingProps {
  title: string
  description?: string
  action?: React.ReactNode
}

export const PageHeading: React.FC<PageHeadingProps> = ({ title, description, action }) => {
  return (
    <div className="flex items-center justify-between mb-8">
      <div className="flex-1">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">{title}</h1>
        {description && <p className="text-gray-600">{description}</p>}
      </div>
      {action && <div className="ml-6">{action}</div>}
    </div>
  )
}
