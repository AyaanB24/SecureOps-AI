/**
 * Findings Page
 * Displays all findings with advanced filtering and search
 */

import { useState } from 'react'
import { Helmet } from 'react-helmet-async'
import { useProjects } from '@/hooks/useProjects'
import { useScans } from '@/hooks/useScans'
import { useFindings } from '@/hooks/useFindings'
import { findingService } from '@/services/findingService'
import { PageHeading } from '@/components/common/PageHeading'
import { LoadingSpinner } from '@/components/common/LoadingSpinner'
import { Button } from '@/components/common/Button'
import { Dialog } from '@/components/common/Dialog'
import { StatusBadge } from '@/components/common/StatusBadge'
import { AlertCircle, Search, X, Filter } from 'lucide-react'
import { Finding, Severity, FindingStatus } from '@/types/project'

const SEVERITY_OPTIONS: Severity[] = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW']
const STATUS_OPTIONS: FindingStatus[] = ['OPEN', 'RESOLVED']

interface FilterState {
  severities: Severity[]
  statuses: FindingStatus[]
  searchText: string
  packageName: string
}

export default function Findings() {
  const { data: projects = [] } = useProjects()

  const [selectedProjectId, setSelectedProjectId] = useState<string | null>(
    projects.length > 0 ? projects[0].id : null
  )

  const { data: scans = [], isLoading: scansLoading } = useScans(selectedProjectId || '')
  const [selectedScanId, setSelectedScanId] = useState<string | null>(null)

  const { data: allFindings = [], isLoading: findingsLoading } = useFindings(
    selectedScanId || ''
  )

  // Filtering state
  const [filters, setFilters] = useState<FilterState>({
    severities: [],
    statuses: [],
    searchText: '',
    packageName: '',
  })

  const [showFindingDetail, setShowFindingDetail] = useState(false)
  const [selectedFinding, setSelectedFinding] = useState<Finding | null>(null)

  // Apply filters
  const filteredFindings = findingService.filterFindings(allFindings, {
    severity: filters.severities.length > 0 ? filters.severities : undefined,
    status: filters.statuses.length > 0 ? filters.statuses : undefined,
    searchText: filters.searchText || undefined,
    packageName: filters.packageName || undefined,
  })

  // Sort by severity
  const sortedFindings = findingService.sortFindings(filteredFindings, 'severity')

  // Get severity counts
  const severityCounts = findingService.countBySeverity(sortedFindings)

  const toggleSeverityFilter = (severity: Severity) => {
    setFilters(prev => ({
      ...prev,
      severities: prev.severities.includes(severity)
        ? prev.severities.filter(s => s !== severity)
        : [...prev.severities, severity],
    }))
  }

  const toggleStatusFilter = (status: FindingStatus) => {
    setFilters(prev => ({
      ...prev,
      statuses: prev.statuses.includes(status)
        ? prev.statuses.filter(s => s !== status)
        : [...prev.statuses, status],
    }))
  }

  const resetFilters = () => {
    setFilters({
      severities: [],
      statuses: [],
      searchText: '',
      packageName: '',
    })
  }

  const hasActiveFilters =
    filters.severities.length > 0 ||
    filters.statuses.length > 0 ||
    filters.searchText !== '' ||
    filters.packageName !== ''

  const handleViewFinding = (finding: Finding) => {
    setSelectedFinding(finding)
    setShowFindingDetail(true)
  }

  return (
    <>
      <Helmet>
        <title>Findings - SecureOps AI</title>
      </Helmet>

      <div className="space-y-6">
        <PageHeading title="Findings" />

        {/* Project Selector */}
        {projects.length > 0 && (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="bg-white p-4 rounded-lg border border-gray-200">
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Select Project
              </label>
              <select
                value={selectedProjectId || ''}
                onChange={e => {
                  setSelectedProjectId(e.target.value)
                  setSelectedScanId(null)
                }}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                {projects.map(project => (
                  <option key={project.id} value={project.id}>
                    {project.name}
                  </option>
                ))}
              </select>
            </div>

            <div className="bg-white p-4 rounded-lg border border-gray-200">
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Select Scan
              </label>
              {scansLoading ? (
                <div className="text-sm text-gray-600">Loading scans...</div>
              ) : (
                <select
                  value={selectedScanId || ''}
                  onChange={e => setSelectedScanId(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option value="">Select a scan...</option>
                  {scans.map(scan => (
                    <option key={scan.id} value={scan.id}>
                      {scan.environment} - {new Date(scan.startedAt).toLocaleDateString()}
                    </option>
                  ))}
                </select>
              )}
            </div>
          </div>
        )}

        {/* Filter Section */}
        {selectedScanId && (
          <div className="bg-white rounded-lg border border-gray-200 p-6">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold text-gray-900 flex items-center space-x-2">
                <Filter size={20} />
                <span>Filters</span>
              </h3>
              {hasActiveFilters && (
                <Button size="sm" variant="secondary" onClick={resetFilters}>
                  <X size={16} className="inline mr-1" />
                  Reset Filters
                </Button>
              )}
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {/* Search */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  <Search size={16} className="inline mr-1" />
                  Search Title/Description
                </label>
                <input
                  type="text"
                  placeholder="Search findings..."
                  value={filters.searchText}
                  onChange={e =>
                    setFilters(prev => ({ ...prev, searchText: e.target.value }))
                  }
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>

              {/* Package Filter */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Package Name
                </label>
                <input
                  type="text"
                  placeholder="Filter by package..."
                  value={filters.packageName}
                  onChange={e =>
                    setFilters(prev => ({ ...prev, packageName: e.target.value }))
                  }
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>

              {/* Severity Filter */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Severity
                </label>
                <div className="flex flex-wrap gap-2">
                  {SEVERITY_OPTIONS.map(severity => (
                    <button
                      key={severity}
                      onClick={() => toggleSeverityFilter(severity)}
                      className={`px-3 py-1 rounded text-sm font-medium transition ${
                        filters.severities.includes(severity)
                          ? severity === 'CRITICAL'
                            ? 'bg-red-100 text-red-800'
                            : severity === 'HIGH'
                              ? 'bg-orange-100 text-orange-800'
                              : severity === 'MEDIUM'
                                ? 'bg-yellow-100 text-yellow-800'
                                : 'bg-green-100 text-green-800'
                          : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                      }`}
                    >
                      {severity}
                      {sortedFindings.length > 0 && (
                        <span className="ml-1">({severityCounts[severity]})</span>
                      )}
                    </button>
                  ))}
                </div>
              </div>

              {/* Status Filter */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Status
                </label>
                <div className="flex flex-wrap gap-2">
                  {STATUS_OPTIONS.map(status => (
                    <button
                      key={status}
                      onClick={() => toggleStatusFilter(status)}
                      className={`px-3 py-1 rounded text-sm font-medium transition ${
                        filters.statuses.includes(status)
                          ? 'bg-blue-100 text-blue-800'
                          : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                      }`}
                    >
                      {status}
                    </button>
                  ))}
                </div>
              </div>
            </div>

            {/* Results Summary */}
            <div className="mt-4 pt-4 border-t border-gray-200">
              <p className="text-sm text-gray-600">
                Showing <span className="font-semibold">{sortedFindings.length}</span> of{' '}
                <span className="font-semibold">{allFindings.length}</span> findings
              </p>
            </div>
          </div>
        )}

        {/* Findings List */}
        <div className="bg-white rounded-lg border border-gray-200 p-6">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Findings</h3>

          {!selectedScanId ? (
            <div className="text-center py-12">
              <AlertCircle size={48} className="mx-auto text-gray-400 mb-4" />
              <p className="text-gray-600">Select a scan to view findings</p>
            </div>
          ) : findingsLoading ? (
            <LoadingSpinner />
          ) : sortedFindings.length === 0 ? (
            <div className="text-center py-12">
              <AlertCircle size={48} className="mx-auto text-green-400 mb-4" />
              <p className="text-gray-600">
                {allFindings.length === 0
                  ? 'No findings for this scan'
                  : 'No findings match the selected filters'}
              </p>
            </div>
          ) : (
            <div className="space-y-3">
              {sortedFindings.map(finding => (
                <div
                  key={finding.id}
                  className="border border-gray-200 rounded-lg p-4 hover:bg-gray-50 cursor-pointer transition"
                  onClick={() => handleViewFinding(finding)}
                  style={{
                    borderLeftWidth: 4,
                    borderLeftColor:
                      finding.severity === 'CRITICAL'
                        ? '#ef4444'
                        : finding.severity === 'HIGH'
                          ? '#f97316'
                          : finding.severity === 'MEDIUM'
                            ? '#eab308'
                            : '#22c55e',
                  }}
                >
                  <div className="flex items-start justify-between">
                    <div className="flex-1">
                      <h4 className="font-semibold text-gray-900">
                        {finding.title}
                      </h4>
                      <p className="text-sm text-gray-600 mt-1">
                        {finding.description}
                      </p>

                      <div className="flex flex-wrap items-center gap-2 mt-3">
                        <span
                          className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                            finding.severity === 'CRITICAL'
                              ? 'bg-red-100 text-red-800'
                              : finding.severity === 'HIGH'
                                ? 'bg-orange-100 text-orange-800'
                                : finding.severity === 'MEDIUM'
                                  ? 'bg-yellow-100 text-yellow-800'
                                  : 'bg-green-100 text-green-800'
                          }`}
                        >
                          {finding.severity}
                        </span>

                        <StatusBadge status={finding.status} />

                        <span className="text-xs bg-blue-100 text-blue-800 px-2.5 py-0.5 rounded-full font-medium">
                          {finding.tool}
                        </span>

                        {finding.packageName && (
                          <span className="text-xs bg-purple-100 text-purple-800 px-2.5 py-0.5 rounded-full font-medium">
                            {finding.packageName}
                            {finding.packageVersion ? `@${finding.packageVersion}` : ''}
                          </span>
                        )}
                      </div>

                      <div className="text-xs text-gray-600 mt-3">
                        {finding.filePath} {finding.lineNumber ? `:${finding.lineNumber}` : ''}
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Finding Detail Modal */}
      <Dialog
        isOpen={showFindingDetail}
        onClose={() => setShowFindingDetail(false)}
        title="Finding Details"
      >
        {selectedFinding && (
          <div className="space-y-4">
            <div>
              <h3 className="font-semibold text-lg text-gray-900 mb-2">
                {selectedFinding.title}
              </h3>
              <p className="text-gray-600">{selectedFinding.description}</p>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <p className="text-sm font-medium text-gray-700">Severity</p>
                <span
                  className={`inline-flex items-center mt-1 px-3 py-1 rounded-full text-sm font-medium ${
                    selectedFinding.severity === 'CRITICAL'
                      ? 'bg-red-100 text-red-800'
                      : selectedFinding.severity === 'HIGH'
                        ? 'bg-orange-100 text-orange-800'
                        : selectedFinding.severity === 'MEDIUM'
                          ? 'bg-yellow-100 text-yellow-800'
                          : 'bg-green-100 text-green-800'
                  }`}
                >
                  {selectedFinding.severity}
                </span>
              </div>

              <div>
                <p className="text-sm font-medium text-gray-700">Status</p>
                <div className="mt-1">
                  <StatusBadge status={selectedFinding.status} />
                </div>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <p className="text-sm font-medium text-gray-700">Tool</p>
                <p className="text-sm text-gray-600 mt-1">{selectedFinding.tool}</p>
              </div>

              <div>
                <p className="text-sm font-medium text-gray-700">Rule ID</p>
                <p className="text-sm font-mono text-gray-600 mt-1">
                  {selectedFinding.ruleId}
                </p>
              </div>
            </div>

            <div>
              <p className="text-sm font-medium text-gray-700">File Path</p>
              <p className="text-sm font-mono text-gray-600 mt-1">
                {selectedFinding.filePath}
                {selectedFinding.lineNumber ? `:${selectedFinding.lineNumber}` : ''}
              </p>
            </div>

            {selectedFinding.packageName && (
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <p className="text-sm font-medium text-gray-700">Package</p>
                  <p className="text-sm text-gray-600 mt-1">
                    {selectedFinding.packageName}
                  </p>
                </div>

                {selectedFinding.packageVersion && (
                  <div>
                    <p className="text-sm font-medium text-gray-700">Version</p>
                    <p className="text-sm text-gray-600 mt-1">
                      {selectedFinding.packageVersion}
                    </p>
                  </div>
                )}
              </div>
            )}

            <div>
              <p className="text-sm font-medium text-gray-700">Fingerprint</p>
              <p className="text-xs font-mono text-gray-600 mt-1 break-all">
                {selectedFinding.fingerprint}
              </p>
            </div>

            <div>
              <p className="text-sm font-medium text-gray-700">Created</p>
              <p className="text-sm text-gray-600 mt-1">
                {new Date(selectedFinding.createdAt).toLocaleString()}
              </p>
            </div>

            <div className="flex justify-end pt-4 border-t border-gray-200">
              <Button
                variant="secondary"
                onClick={() => setShowFindingDetail(false)}
              >
                Close
              </Button>
            </div>
          </div>
        )}
      </Dialog>
    </>
  )
}
