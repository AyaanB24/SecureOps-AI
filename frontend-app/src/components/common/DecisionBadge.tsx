import { Decision } from '@/types/project'

interface DecisionBadgeProps {
  decision: Decision
  size?: 'sm' | 'md' | 'lg'
}

/**
 * Badge component for security decision
 * Shows PASS (green) or BLOCK (red) with appropriate styling
 */
export function DecisionBadge({ decision, size = 'md' }: DecisionBadgeProps) {
  const isPass = decision === 'PASS'
  
  const baseClasses = 'inline-flex items-center rounded-full font-bold'
  const sizeClasses = {
    sm: 'px-2 py-1 text-xs',
    md: 'px-3 py-1.5 text-sm',
    lg: 'px-4 py-2 text-base',
  }
  const colorClasses = isPass
    ? 'bg-green-100 text-green-800'
    : 'bg-red-100 text-red-800'

  return (
    <span className={`${baseClasses} ${sizeClasses[size]} ${colorClasses}`}>
      {isPass ? '✓ PASS' : '✗ BLOCK'}
    </span>
  )
}
