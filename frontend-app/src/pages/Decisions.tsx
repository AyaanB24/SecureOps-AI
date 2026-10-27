/**
 * Security Decisions Page
 * Displays PASS/BLOCK decisions for scans with reasoning and metrics
 */

import { useState } from 'react'
import { Helmet } from 'react-helmet-async'
import { useProjects } from '@/hooks/useProjects'
import { useScans } from '@/hooks/useScans'
import { useDecision } from '@/hooks/useDecision'
import { PageHeading } from '@/components/common/PageHeading'
import { Button } from '@/components/common/Button'
import { LoadingSpinner } from '@/components/common/LoadingSpinner'
import { DecisionBadge } from '@/components/common/DecisionBadge'
import { Dialog } from '@/components/common/Dialog'
import { AlertCircle, ChevronDown, ChevronUp, Info } from 'lucide-react'
import { Scan } from '@/types/project'

interface ExpandedScan {
  scanId: string
  showDecision: boolean
}

export default function Decisions() {
  const { data: projects = [] } = useProjects()

  const [selectedProjectId, setSelectedProjectId] = useState<string | null>(
    projects.length > 0 ? projects[0].id : null
  )

  const { data: scans = [], isLoading: scansLoading } = useScans(selectedProjectId || '')

  // Track which scans have decisions expanded
  const [expandedScans, setExpandedScans] = useState<ExpandedScan[]>([])

  // Detail modal state
  const [showDetailModal, setShowDetailModal] = useState(false)
  const [selectedScanId, setSelectedScanId] = useState<string | null>(null)

  const isExpanded = (scanId: string) =>
    expandedScans.some(s => s.scanId === scanId)

  const toggleExpanded = (scanId: string) => {
    setExpandedScans(prev => {
      const existing = prev.find(s => s.scanId === scanId)
      if (existing) {
        return prev.filter(s => s.scanId !== scanId)
      } else {
        return [...prev, { scanId, showDecision: true }]
      }
    })
  }

  const handleViewDetails = (scanId: string) => {
    setSelectedScanId(scanId)
    setShowDetailModal(true)
  }

  return (
    <>
      <Helmet>
        <title>Decisions - SecureOps AI</title>
      </Helmet>

      <div className="space-y-6">
        <PageHeading title="Security Decisions" />

        {/* Info Section */}
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 flex items-start space-x-3">
          <Info size={20} className="text-blue-600 flex-shrink-0 mt-0.5" />
          <div className="text-sm text-blue-900">
            <p className="font-medium">Understanding Decisions</p>
            <p className="mt-1">
              A <strong>PASS</strong> means the scan meets your project's security policy thresholds.
              A <strong>BLOCK</strong> means one or more thresholds were exceeded. PASS does NOT mean deployment
              happened—that's a separate Jenkins operation.
            </p>
          </div>
        </div>

        {/* Project Selector */}
        {projects.length > 0 && (
          <div className="bg-white p-4 rounded-lg border border-gray-200">
            <label className="block text-sm font-medium text-gray-700 mb-2">Select Project</label>
            <select
              value={selectedProjectId || ''}
              onChange={e => setSelectedProjectId(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              {projects.map(project => (
                <option key={project.id} value={project.id}>
                  {project.name}
                </option>
              ))}
            </select>
          </div>
        )}

        {/* Decisions List */}
        <div className="bg-white rounded-lg border border-gray-200 p-6">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Scan Decisions</h3>

          {scansLoading ? (
            <LoadingSpinner />
          ) : scans.length === 0 ? (
            <div className="text-center py-12">
              <AlertCircle size={48} className="mx-auto text-gray-400 mb-4" />
              <p className="text-gray-600">No scans found for this project</p>
            </div>
          ) : (
            <div className="space-y-3">
              {scans.map((scan: Scan) => (
                <ScanDecisionRow
                  key={scan.id}
                  scan={scan}
                  isExpanded={isExpanded(scan.id)}
                  onToggle={() => toggleExpanded(scan.id)}
                  onViewDetails={() => handleViewDetails(scan.id)}
                />
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Decision Detail Modal */}
      {selectedScanId && (
        <DecisionDetailModal
          isOpen={showDetailModal}
          scanId={selectedScanId}
          onClose={() => setShowDetailModal(false)}
        />
      )}
    </>
  )
}

/**
 * Component to display a single scan's decision
 */
function ScanDecisionRow({
  scan,
  isExpanded,
  onToggle,
  onViewDetails,
}: {
  scan: Scan
  isExpanded: boolean
  onToggle: () => void
  onViewDetails: () => void
}) {
  const { data: decision, isLoading, error } = useDecision(scan.id)

  return (
    <div className="border border-gray-200 rounded-lg overflow-hidden">
      {/* Header */}
      <div
        className="bg-gray-50 p-4 flex items-center justify-between hover:bg-gray-100 cursor-pointer"
        onClick={onToggle}
      >
        <div className="flex items-center flex-1 space-x-4">
          {isExpanded ? (
            <ChevronUp size={20} className="text-gray-500" />
          ) : (
            <ChevronDown size={20} className="text-gray-500" />
          )}

          <div className="flex-1">
            <div className="font-medium text-gray-900">
              Scan {scan.id.substring(0, 8)} - {scan.environment}
            </div>
            <div className="text-sm text-gray-600 mt-1">
              {new Date(scan.startedAt).toLocaleString()}
            </div>
          </div>

          {isLoading ? (
            <LoadingSpinner />
          ) : error ? (
            <span className="text-xs bg-red-100 text-red-800 px-2 py-1 rounded">
              Error loading decision
            </span>
          ) : decision ? (
            <DecisionBadge decision={decision.decision} size="lg" />
          ) : (
            <span className="text-xs bg-gray-100 text-gray-700 px-2 py-1 rounded">
              No decision
            </span>
          )}
        </div>

        {decision && (
          <Button
            size="sm"
            variant="secondary"
            onClick={(e) => {
              e.stopPropagation()
              onViewDetails()
            }}
          >
            View Details
          </Button>
        )}
      </div>

      {/* Expanded Content */}
      {isExpanded && decision && (
        <div className="border-t border-gray-200 p-4 bg-gray-50 space-y-4">
          {/* Decision Summary */}
          <div className="bg-white p-4 rounded border border-gray-200">
            <div className="flex items-center justify-between mb-4">
              <h4 className="font-semibold text-gray-900">Decision</h4>
              <DecisionBadge decision={decision.decision} />
            </div>

            <div className="space-y-2">
              <div>
                <p className="text-sm text-gray-700">
                  <span className="font-medium">Reason:</span> {decision.reason}
                </p>
              </div>

              <div>
                <p className="text-sm text-gray-700">
                  <span className="font-medium">Risk Score:</span> {decision.riskScore}
                </p>
              </div>

              <div>
                <p className="text-sm text-gray-700">
                  <span className="font-medium">Decision Generated:</span>{' '}
                  {new Date(decision.createdAt).toLocaleString()}
                </p>
              </div>
            </div>
          </div>

          {/* Finding Counts */}
          <div className="bg-white p-4 rounded border border-gray-200">
            <h4 className="font-semibold text-gray-900 mb-3">Finding Counts</h4>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
              <div className="text-center">
                <div className="text-2xl font-bold text-red-600">
                  {decision.criticalCount}
                </div>
                <p className="text-xs text-gray-600 mt-1">Critical</p>
              </div>
              <div className="text-center">
                <div className="text-2xl font-bold text-orange-600">
                  {decision.highCount}
                </div>
                <p className="text-xs text-gray-600 mt-1">High</p>
              </div>
              <div className="text-center">
                <div className="text-2xl font-bold text-yellow-600">
                  {decision.mediumCount}
                </div>
                <p className="text-xs text-gray-600 mt-1">Medium</p>
              </div>
              <div className="text-center">
                <div className="text-2xl font-bold text-green-600">
                  {decision.lowCount}
                </div>
                <p className="text-xs text-gray-600 mt-1">Low</p>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

/**
 * Modal to display full decision details
 */
function DecisionDetailModal({
  isOpen,
  scanId,
  onClose,
}: {
  isOpen: boolean
  scanId: string
  onClose: () => void
}) {
  const { data: decision, isLoading, error, refetch } = useDecision(scanId)

  if (!isOpen) return null

  return (
    <Dialog isOpen={isOpen} onClose={onClose} title="Security Decision Details">
      {isLoading ? (
        <LoadingSpinner />
      ) : error ? (
        <div className="text-red-600 text-center py-8">
          <p>Failed to load decision</p>
          <Button size="sm" onClick={() => refetch()} className="mt-4">
            Retry
          </Button>
        </div>
      ) : decision ? (
        <div className="space-y-6">
          {/* Decision Status */}
          <div>
            <h3 className="font-semibold text-lg text-gray-900 mb-3 flex items-center space-x-3">
              <span>Decision Status</span>
              <DecisionBadge decision={decision.decision} size="lg" />
            </h3>
          </div>

          {/* Risk Score */}
          <div>
            <p className="text-sm font-medium text-gray-700">Risk Score</p>
            <p className="text-3xl font-bold text-gray-900 mt-2">{decision.riskScore}</p>
          </div>

          {/* Reasoning */}
          <div className="bg-gray-50 p-4 rounded border border-gray-200">
            <p className="text-sm font-medium text-gray-700 mb-2">Reason</p>
            <p className="text-gray-900">{decision.reason}</p>
          </div>

          {/* Finding Summary */}
          <div>
            <p className="text-sm font-medium text-gray-700 mb-3">Findings by Severity</p>
            <div className="space-y-2">
              <div className="flex justify-between">
                <span className="text-gray-700">Critical</span>
                <span className="font-bold text-red-600">{decision.criticalCount}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-700">High</span>
                <span className="font-bold text-orange-600">{decision.highCount}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-700">Medium</span>
                <span className="font-bold text-yellow-600">{decision.mediumCount}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-700">Low</span>
                <span className="font-bold text-green-600">{decision.lowCount}</span>
              </div>
            </div>
          </div>

          {/* Timestamp */}
          <div>
            <p className="text-sm font-medium text-gray-700">Decision Generated</p>
            <p className="text-gray-600 mt-1">
              {new Date(decision.createdAt).toLocaleString()}
            </p>
          </div>

          {/* Close Button */}
          <div className="flex justify-end pt-4 border-t border-gray-200">
            <Button variant="secondary" onClick={onClose}>
              Close
            </Button>
          </div>
        </div>
      ) : null}
    </Dialog>
  )
}
