/**
 * Dashboard Page
 * Displays overall platform metrics, recent activity, and security insights
 * All data is fetched from the backend API in real-time
 */

import { Helmet } from 'react-helmet-async'
import { useDashboard } from '@/hooks/useDashboard'
import { LoadingSpinner } from '@/components/common/LoadingSpinner'
import { ErrorState } from '@/components/common/ErrorState'
import Stat from '@/components/common/Stat'
import { PageHeading } from '@/components/common/PageHeading'
import { AlertCircle, CheckCircle, TrendingUp, Package } from 'lucide-react'

export default function Dashboard() {
  const { data, isLoading, error, refetch } = useDashboard()

  if (isLoading) {
    return (
      <>
        <Helmet>
          <title>Dashboard - SecureOps AI</title>
        </Helmet>
        <div className="space-y-6">
          <PageHeading title="Dashboard" />
          <LoadingSpinner />
        </div>
      </>
    )
  }

  if (error) {
    return (
      <>
        <Helmet>
          <title>Dashboard - SecureOps AI</title>
        </Helmet>
        <div className="space-y-6">
          <PageHeading title="Dashboard" />
          <ErrorState
            title="Failed to load dashboard"
            message={error instanceof Error ? error.message : 'Unable to fetch dashboard data'}
            onRetry={() => refetch()}
          />
        </div>
      </>
    )
  }

  if (!data) {
    return (
      <>
        <Helmet>
          <title>Dashboard - SecureOps AI</title>
        </Helmet>
        <div className="space-y-6">
          <PageHeading title="Dashboard" />
          <div className="bg-white p-8 rounded-lg border border-gray-200 text-center">
            <p className="text-gray-600">No data available. Register a project to get started.</p>
          </div>
        </div>
      </>
    )
  }

  return (
    <>
      <Helmet>
        <title>Dashboard - SecureOps AI</title>
      </Helmet>

      <div className="space-y-8">
        <PageHeading title="Dashboard" />

        {/* Key Metrics */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          <Stat label="Total Projects" value={data.totalProjects} icon={<TrendingUp size={32} />} />
          <Stat label="Total Pipelines" value={data.totalPipelines} icon={<Package size={32} />} />
          <Stat label="Total Scans" value={data.totalScans} icon={<TrendingUp size={32} />} />
          <Stat label="Total Findings" value={data.totalFindings} icon={<AlertCircle size={32} />} />
        </div>

        {/* Finding Distribution and Decision Stats */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Severity Distribution */}
          <div className="bg-white p-6 rounded-lg border border-gray-200">
            <h3 className="text-lg font-semibold text-gray-900 mb-4">Findings by Severity</h3>
            <div className="space-y-3">
              {data.severityDistribution.map(dist => (
                <div key={dist.severity}>
                  <div className="flex justify-between mb-1">
                    <span className="text-sm font-medium text-gray-700">{dist.severity}</span>
                    <span className="text-sm font-semibold text-gray-900">{dist.count}</span>
                  </div>
                  <div className="w-full bg-gray-200 rounded-full h-2">
                    <div
                      className="h-2 rounded-full"
                      style={{
                        width: `${data.totalFindings > 0 ? (dist.count / data.totalFindings) * 100 : 0}%`,
                        backgroundColor: dist.color,
                      }}
                    />
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Decision Stats */}
          <div className="bg-white p-6 rounded-lg border border-gray-200">
            <h3 className="text-lg font-semibold text-gray-900 mb-4">Security Decisions</h3>
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-3">
                  <CheckCircle size={24} className="text-green-600" />
                  <span className="text-gray-700">PASS</span>
                </div>
                <span className="text-2xl font-bold text-green-600">{data.decisionStats.passed}</span>
              </div>
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-3">
                  <AlertCircle size={24} className="text-red-600" />
                  <span className="text-gray-700">BLOCK</span>
                </div>
                <span className="text-2xl font-bold text-red-600">{data.decisionStats.blocked}</span>
              </div>
            </div>
          </div>
        </div>

        {/* Recent Scans */}
        {data.recentScans.length > 0 && (
          <div className="bg-white p-6 rounded-lg border border-gray-200">
            <h3 className="text-lg font-semibold text-gray-900 mb-4">Recent Scans</h3>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-gray-200">
                    <th className="text-left py-3 px-4 font-semibold text-gray-700">Project</th>
                    <th className="text-left py-3 px-4 font-semibold text-gray-700">Environment</th>
                    <th className="text-left py-3 px-4 font-semibold text-gray-700">Status</th>
                    <th className="text-left py-3 px-4 font-semibold text-gray-700">Started</th>
                  </tr>
                </thead>
                <tbody>
                  {data.recentScans.map(scan => (
                    <tr key={scan.id} className="border-b border-gray-100 hover:bg-gray-50">
                      <td className="py-3 px-4 text-gray-900">{scan.projectName}</td>
                      <td className="py-3 px-4">
                        <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
                          {scan.environment}
                        </span>
                      </td>
                      <td className="py-3 px-4">
                        <span
                          className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                            scan.status === 'COMPLETED'
                              ? 'bg-green-100 text-green-800'
                              : scan.status === 'FAILED'
                                ? 'bg-red-100 text-red-800'
                                : 'bg-yellow-100 text-yellow-800'
                          }`}
                        >
                          {scan.status}
                        </span>
                      </td>
                      <td className="py-3 px-4 text-gray-600">
                        {new Date(scan.startedAt).toLocaleDateString()} {new Date(scan.startedAt).toLocaleTimeString()}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* Recent Decisions */}
        {data.recentDecisions.length > 0 && (
          <div className="bg-white p-6 rounded-lg border border-gray-200">
            <h3 className="text-lg font-semibold text-gray-900 mb-4">Recent Security Decisions</h3>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-gray-200">
                    <th className="text-left py-3 px-4 font-semibold text-gray-700">Project</th>
                    <th className="text-left py-3 px-4 font-semibold text-gray-700">Decision</th>
                    <th className="text-left py-3 px-4 font-semibold text-gray-700">Risk Score</th>
                    <th className="text-left py-3 px-4 font-semibold text-gray-700">Findings</th>
                    <th className="text-left py-3 px-4 font-semibold text-gray-700">Reason</th>
                  </tr>
                </thead>
                <tbody>
                  {data.recentDecisions.map(decision => (
                    <tr key={decision.scanId} className="border-b border-gray-100 hover:bg-gray-50">
                      <td className="py-3 px-4 text-gray-900">{decision.projectName}</td>
                      <td className="py-3 px-4">
                        <span
                          className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                            decision.decision === 'PASS'
                              ? 'bg-green-100 text-green-800'
                              : 'bg-red-100 text-red-800'
                          }`}
                        >
                          {decision.decision}
                        </span>
                      </td>
                      <td className="py-3 px-4 font-semibold text-gray-900">{decision.riskScore}</td>
                      <td className="py-3 px-4">
                        <div className="flex space-x-2 text-xs">
                          {decision.criticalCount > 0 && (
                            <span className="bg-red-100 text-red-800 px-2 py-1 rounded">C:{decision.criticalCount}</span>
                          )}
                          {decision.highCount > 0 && (
                            <span className="bg-orange-100 text-orange-800 px-2 py-1 rounded">H:{decision.highCount}</span>
                          )}
                          {decision.mediumCount > 0 && (
                            <span className="bg-yellow-100 text-yellow-800 px-2 py-1 rounded">M:{decision.mediumCount}</span>
                          )}
                          {decision.lowCount > 0 && (
                            <span className="bg-green-100 text-green-800 px-2 py-1 rounded">L:{decision.lowCount}</span>
                          )}
                        </div>
                      </td>
                      <td className="py-3 px-4 text-gray-600 max-w-xs truncate">{decision.reason}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </div>
    </>
  )
}
