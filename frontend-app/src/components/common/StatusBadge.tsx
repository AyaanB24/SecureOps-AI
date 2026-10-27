/**
 * StatusBadge Component
 * Displays status with appropriate color coding (PASS/BLOCK/CRITICAL/HIGH/etc.)
 */

import React from 'react'

export type StatusType =
  | 'PASS'
  | 'BLOCK'
  | 'CRITICAL'
  | 'HIGH'
  | 'MEDIUM'
  | 'LOW'
  | 'CREATED'
  | 'IN_PROGRESS'
  | 'PROCESSING'
  | 'COMPLETED'
  | 'FAILED'
  | 'OPEN'
  | 'RESOLVED'

interface StatusBadgeProps {
  status: StatusType
  label?: string
}

const statusStyles: Record<StatusType, string> = {
  // Decisions
  PASS: 'bg-green-100 text-green-800 border-green-200',
  BLOCK: 'bg-red-100 text-red-800 border-red-200',

  // Severity
  CRITICAL: 'bg-red-100 text-red-800 border-red-200',
  HIGH: 'bg-orange-100 text-orange-800 border-orange-200',
  MEDIUM: 'bg-amber-100 text-amber-800 border-amber-200',
  LOW: 'bg-green-100 text-green-800 border-green-200',

  // Scan Status
  CREATED: 'bg-gray-100 text-gray-800 border-gray-200',
  IN_PROGRESS: 'bg-yellow-100 text-yellow-800 border-yellow-200',
  PROCESSING: 'bg-blue-100 text-blue-800 border-blue-200',
  COMPLETED: 'bg-green-100 text-green-800 border-green-200',
  FAILED: 'bg-red-100 text-red-800 border-red-200',

  // Finding Status
  OPEN: 'bg-red-100 text-red-800 border-red-200',
  RESOLVED: 'bg-green-100 text-green-800 border-green-200',
}

const statusLabels: Record<StatusType, string> = {
  PASS: 'Pass',
  BLOCK: 'Block',
  CRITICAL: 'Critical',
  HIGH: 'High',
  MEDIUM: 'Medium',
  LOW: 'Low',
  CREATED: 'Created',
  IN_PROGRESS: 'In Progress',
  PROCESSING: 'Processing',
  COMPLETED: 'Completed',
  FAILED: 'Failed',
  OPEN: 'Open',
  RESOLVED: 'Resolved',
}

export const StatusBadge: React.FC<StatusBadgeProps> = ({ status, label }) => {
  const displayLabel = label || statusLabels[status]

  return (
    <span
      className={`inline-flex items-center px-3 py-1 rounded-full text-xs font-medium border ${statusStyles[status]}`}
    >
      {displayLabel}
    </span>
  )
}
