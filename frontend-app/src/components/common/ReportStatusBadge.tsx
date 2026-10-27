import { ReportStatus } from '@/types/project'

interface ReportStatusBadgeProps {
  status: ReportStatus
}

/**
 * Badge component for report processing status
 * Shows RECEIVED, PROCESSING, PROCESSED, or FAILED status
 * with appropriate color coding
 */
export function ReportStatusBadge({ status }: ReportStatusBadgeProps) {
  const statusConfig: Record<ReportStatus, { bg: string; text: string; label: string }> = {
    RECEIVED: {
      bg: 'bg-blue-100',
      text: 'text-blue-800',
      label: 'Received',
    },
    PROCESSING: {
      bg: 'bg-yellow-100',
      text: 'text-yellow-800',
      label: 'Processing',
    },
    PROCESSED: {
      bg: 'bg-green-100',
      text: 'text-green-800',
      label: 'Processed',
    },
    FAILED: {
      bg: 'bg-red-100',
      text: 'text-red-800',
      label: 'Failed',
    },
  }

  const config = statusConfig[status] || statusConfig.RECEIVED

  return (
    <span className={`inline-flex items-center rounded-full px-3 py-1 text-sm font-medium ${config.bg} ${config.text}`}>
      {config.label}
    </span>
  )
}
