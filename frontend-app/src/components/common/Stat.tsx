/**
 * Stat Component
 * Displays a single statistic (number) with label
 */

interface StatProps {
  label: string
  value: number | string
  icon?: React.ReactNode
  trend?: 'up' | 'down'
}

export default function Stat({ label, value, icon, trend }: StatProps) {
  return (
    <div className="bg-white p-6 rounded-lg border border-gray-200">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm text-gray-600 mb-2">{label}</p>
          <p className="text-3xl font-bold text-gray-900">{value}</p>
        </div>
        {icon && <div className="text-gray-400">{icon}</div>}
      </div>
      {trend && (
        <p
          className={`mt-2 text-sm ${
            trend === 'up' ? 'text-green-600' : 'text-red-600'
          }`}
        >
          {trend === 'up' ? '↑' : '↓'} Trend
        </p>
      )}
    </div>
  )
}
