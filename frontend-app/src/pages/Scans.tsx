/**
 * Scans Page
 * Displays scans for a selected project and allows viewing reports and findings
 */

import { useState } from 'react'
import { Helmet } from 'react-helmet-async'
import { useProjects } from '@/hooks/useProjects'
import { useScans } from '@/hooks/useScans'
import { useReports } from '@/hooks/useReports'
import { useFindings } from '@/hooks/useFindings'
import * as reportService from '@/services/reportService'
import { PageHeading } from '@/components/common/PageHeading'
import { Button } from '@/components/common/Button'
import { LoadingSpinner } from '@/components/common/LoadingSpinner'
import { Dialog } from '@/components/common/Dialog'
import { ReportStatusBadge } from '@/components/common/ReportStatusBadge'
import { StatusBadge } from '@/components/common/StatusBadge'
import { AlertCircle, Upload, RefreshCw, ChevronDown, ChevronUp } from 'lucide-react'
import { useQueryClient } from '@tanstack/react-query'
import { Scan, Report, Finding } from '@/types/project'

interface ExpandedScan {
  scanId: string
  showReports: boolean
  selectedReportId: string | null
}

export default function Scans() {
  const queryClient = useQueryClient()
  const { data: projects = [] } = useProjects()

  const [selectedProjectId, setSelectedProjectId] = useState<string | null>(
    projects.length > 0 ? projects[0].id : null
  )

  const { data: scans = [], isLoading: scansLoading } = useScans(selectedProjectId || '')
  
  // Track which scans have reports/findings expanded
  const [expandedScans, setExpandedScans] = useState<ExpandedScan[]>([])
  const [showUploadDialog, setShowUploadDialog] = useState(false)
  const [uploadScanId, setUploadScanId] = useState<string | null>(null)
  const [uploadFile, setUploadFile] = useState<File | null>(null)
  const [uploadTool, setUploadTool] = useState('TRIVY')
  const [uploadError, setUploadError] = useState<string | null>(null)
  const [isUploading, setIsUploading] = useState(false)
  const [uploadSuccess, setUploadSuccess] = useState<string | null>(null)

  // Get expanded scan state
  const toggleScanExpanded = (scanId: string) => {
    setExpandedScans(prev => {
      const existing = prev.find(s => s.scanId === scanId)
      if (existing) {
        return prev.filter(s => s.scanId !== scanId)
      } else {
        return [...prev, { scanId, showReports: true, selectedReportId: null }]
      }
    })
  }

  // Handle upload
  const handleUpload = async () => {
    setUploadError(null)

    if (!uploadScanId || !uploadFile) {
      setUploadError('Please select a file')
      return
    }

    setIsUploading(true)
    try {
      await reportService.uploadReport(uploadScanId, uploadTool, uploadFile)
      
      // Invalidate reports query to refresh
      queryClient.invalidateQueries({ queryKey: ['reports', uploadScanId] })
      
      setUploadSuccess('Report uploaded successfully')
      setTimeout(() => setUploadSuccess(null), 3000)
      
      setShowUploadDialog(false)
      setUploadFile(null)
      setUploadTool('TRIVY')
    } catch (err: any) {
      setUploadError(err.response?.data?.message || err.message || 'Failed to upload report')
    } finally {
      setIsUploading(false)
    }
  }

  // Handle process report
  const handleProcessReport = async (scanId: string, reportId: string) => {
    try {
      await reportService.processReport(scanId, reportId)
      
      // Invalidate findings and reports queries
      queryClient.invalidateQueries({ queryKey: ['findings', scanId] })
      queryClient.invalidateQueries({ queryKey: ['reports', scanId] })
      
      setUploadSuccess('Report processing started')
      setTimeout(() => setUploadSuccess(null), 3000)
    } catch (err: any) {
      setUploadError(err.response?.data?.message || err.message || 'Failed to process report')
      setTimeout(() => setUploadError(null), 5000)
    }
  }

  const handleOpenUploadDialog = (scanId: string) => {
    setUploadScanId(scanId)
    setShowUploadDialog(true)
    setUploadError(null)
  }

  return (
    <>
      <Helmet>
        <title>Scans - SecureOps AI</title>
      </Helmet>

      <div className="space-y-6">
        <PageHeading title="Scans" />

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

        {uploadSuccess && (
          <div className="bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded-lg flex items-start space-x-3">
            <AlertCircle size={20} className="flex-shrink-0 mt-0.5" />
            <p>{uploadSuccess}</p>
          </div>
        )}

        {uploadError && (
          <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg flex items-start space-x-3">
            <AlertCircle size={20} className="flex-shrink-0 mt-0.5" />
            <p>{uploadError}</p>
          </div>
        )}

        {/* Scans List */}
        <div className="bg-white rounded-lg border border-gray-200 p-6">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Scans</h3>

          {scansLoading ? (
            <LoadingSpinner />
          ) : scans.length === 0 ? (
            <div className="text-center py-8">
              <p className="text-gray-600">No scans found for this project</p>
            </div>
          ) : (
            <div className="space-y-4">
              {scans.map((scan: Scan) => {
                const isExpanded = expandedScans.some(s => s.scanId === scan.id)

                return (
                  <div key={scan.id} className="border border-gray-200 rounded-lg overflow-hidden">
                    {/* Scan Header */}
                    <div className="bg-gray-50 p-4 flex items-center justify-between hover:bg-gray-100 cursor-pointer"
                      onClick={() => toggleScanExpanded(scan.id)}>
                      <div className="flex items-center flex-1 space-x-4">
                        {isExpanded ? (
                          <ChevronUp size={20} className="text-gray-500" />
                        ) : (
                          <ChevronDown size={20} className="text-gray-500" />
                        )}
                        <div className="flex-1">
                          <div className="font-medium text-gray-900">
                            Scan {scan.id.substring(0, 8)}
                          </div>
                          <div className="flex items-center space-x-4 mt-1 text-sm text-gray-600">
                            <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
                              {scan.environment}
                            </span>
                            <StatusBadge status={scan.status} />
                            <span>Started: {new Date(scan.startedAt).toLocaleString()}</span>
                          </div>
                        </div>
                      </div>
                      <Button
                        size="sm"
                        onClick={(e) => {
                          e.stopPropagation()
                          handleOpenUploadDialog(scan.id)
                        }}
                      >
                        <Upload size={16} className="inline mr-2" />
                        Upload Report
                      </Button>
                    </div>

                    {/* Scan Expanded Content */}
                    {isExpanded && (
                      <div className="border-t border-gray-200 p-4 space-y-4">
                        <ScanReportsSection
                          scanId={scan.id}
                          onProcessReport={handleProcessReport}
                        />
                      </div>
                    )}
                  </div>
                )
              })}
            </div>
          )}
        </div>
      </div>

      {/* Upload Report Dialog */}
      <Dialog
        isOpen={showUploadDialog}
        onClose={() => {
          setShowUploadDialog(false)
          setUploadError(null)
          setUploadFile(null)
        }}
        title="Upload Security Report"
      >
        <div className="space-y-4">
          {uploadError && (
            <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm">
              {uploadError}
            </div>
          )}

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Tool *</label>
            <select
              value={uploadTool}
              onChange={e => setUploadTool(e.target.value)}
              disabled={isUploading}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="TRIVY">Trivy</option>
              <option value="SEMGREP">Semgrep</option>
              <option value="OWASP_DEPENDENCY_CHECK">OWASP Dependency Check</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Report File *</label>
            <input
              type="file"
              onChange={e => setUploadFile(e.target.files?.[0] || null)}
              disabled={isUploading}
              accept=".json"
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            <p className="text-xs text-gray-600 mt-1">JSON format required</p>
          </div>

          <div className="flex justify-end space-x-3 pt-4">
            <Button
              variant="secondary"
              onClick={() => setShowUploadDialog(false)}
              disabled={isUploading}
            >
              Cancel
            </Button>
            <Button
              onClick={handleUpload}
              disabled={isUploading || !uploadFile}
            >
              {isUploading ? 'Uploading...' : 'Upload'}
            </Button>
          </div>
        </div>
      </Dialog>
    </>
  )
}

/**
 * Component to display reports and findings for a scan
 */
function ScanReportsSection({
  scanId,
  onProcessReport,
}: {
  scanId: string
  onProcessReport: (scanId: string, reportId: string) => Promise<void>
}) {
  const { data: reports = [], isLoading: reportsLoading } = useReports(scanId)
  const [expandedReportId, setExpandedReportId] = useState<string | null>(null)
  const [isProcessing, setIsProcessing] = useState<string | null>(null)

  const handleProcess = async (reportId: string) => {
    setIsProcessing(reportId)
    try {
      await onProcessReport(scanId, reportId)
    } finally {
      setIsProcessing(null)
    }
  }

  if (reportsLoading) {
    return <LoadingSpinner />
  }

  if (reports.length === 0) {
    return <p className="text-gray-600">No reports uploaded for this scan</p>
  }

  return (
    <div className="space-y-3">
      <h4 className="font-medium text-gray-900">Reports</h4>
      {reports.map((report: Report) => (
        <div key={report.id} className="bg-white border border-gray-200 rounded">
          <div
            className="p-3 flex items-center justify-between cursor-pointer hover:bg-gray-50"
            onClick={() =>
              setExpandedReportId(
                expandedReportId === report.id ? null : report.id
              )
            }
          >
            <div className="flex-1">
              <div className="flex items-center space-x-3">
                {expandedReportId === report.id ? (
                  <ChevronUp size={18} className="text-gray-500" />
                ) : (
                  <ChevronDown size={18} className="text-gray-500" />
                )}
                <div>
                  <div className="font-medium text-gray-900">
                    {report.fileName}
                  </div>
                  <div className="flex items-center space-x-2 mt-1">
                    <span className="text-xs bg-purple-100 text-purple-800 px-2 py-0.5 rounded">
                      {report.tool}
                    </span>
                    <ReportStatusBadge status={report.status} />
                  </div>
                </div>
              </div>
            </div>
            {report.status === 'RECEIVED' && (
              <Button
                size="sm"
                onClick={(e) => {
                  e.stopPropagation()
                  handleProcess(report.id)
                }}
                disabled={isProcessing === report.id}
              >
                {isProcessing === report.id ? (
                  <>
                    <RefreshCw size={16} className="inline mr-1 animate-spin" />
                    Processing...
                  </>
                ) : (
                  <>
                    <RefreshCw size={16} className="inline mr-1" />
                    Process
                  </>
                )}
              </Button>
            )}
          </div>

          {expandedReportId === report.id && (
            <ReportFindingsSection scanId={scanId} reportId={report.id} />
          )}
        </div>
      ))}
    </div>
  )
}

/**
 * Component to display findings for a report
 */
function ReportFindingsSection({
  scanId,
  reportId,
}: {
  scanId: string
  reportId: string
}) {
  const { data: findings = [], isLoading: findingsLoading } = useFindings(scanId)
  const reportFindings = findings.filter(f => f.reportId === reportId)

  if (findingsLoading) {
    return <LoadingSpinner />
  }

  if (reportFindings.length === 0) {
    return (
      <div className="border-t border-gray-200 p-3 text-center text-sm text-gray-600">
        No findings extracted from this report
      </div>
    )
  }

  return (
    <div className="border-t border-gray-200 p-3">
      <h5 className="font-medium text-gray-900 mb-3">
        Findings ({reportFindings.length})
      </h5>
      <div className="space-y-2 max-h-60 overflow-y-auto">
        {reportFindings.map((finding: Finding) => (
          <div
            key={finding.id}
            className="p-2 bg-gray-50 rounded text-sm border-l-4"
            style={{
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
                <div className="font-medium text-gray-900">{finding.title}</div>
                <div className="text-xs text-gray-600 mt-1">
                  {finding.filePath} {finding.lineNumber ? `:${finding.lineNumber}` : ''}
                </div>
                {finding.packageName && (
                  <div className="text-xs text-gray-600">
                    Package: {finding.packageName}{finding.packageVersion ? `@${finding.packageVersion}` : ''}
                  </div>
                )}
              </div>
              <span
                className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ml-2 flex-shrink-0 ${
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
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
