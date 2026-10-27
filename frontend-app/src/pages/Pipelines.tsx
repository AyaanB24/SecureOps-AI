import { useState } from 'react'
import { Helmet } from 'react-helmet-async'
import { useProjects } from '@/hooks/useProjects'
import { usePipelinesByProject } from '@/hooks/usePipelinesByProject'
import { useScansByProject } from '@/hooks/useScansByProject'
import { scanService } from '@/services/scanService'
import { pipelineService } from '@/services/pipelineService'
import { PageHeading } from '@/components/common/PageHeading'
import { Button } from '@/components/common/Button'
import { LoadingSpinner } from '@/components/common/LoadingSpinner'
import { Dialog } from '@/components/common/Dialog'
import { Plus, AlertCircle } from 'lucide-react'
import { useQueryClient } from '@tanstack/react-query'
import { useToast } from '@/contexts/ToastContext'
import { ScanEnvironment, PipelineProvider } from '@/types/project'

const ENVIRONMENTS: ScanEnvironment[] = ['DEV', 'STAGING', 'PRODUCTION']

const PIPELINE_PROVIDERS: PipelineProvider[] = [
  'JENKINS',
  'GITHUB_ACTIONS',
  'GITLAB_CI',
  'AZURE_PIPELINES',
  'CIRCLECI',
  'TRAVIS_CI',
]

export default function Pipelines() {
  const queryClient = useQueryClient()
  const { success, error } = useToast()
  const { data: projects = [] } = useProjects()
  
  const [selectedProjectId, setSelectedProjectId] = useState<string | null>(
    projects.length > 0 ? projects[0].id : null
  )
  
  const { data: pipelines = [], isLoading: pipelinesLoading } = usePipelinesByProject(selectedProjectId)
  const { data: scans = [], isLoading: scansLoading } = useScansByProject(selectedProjectId)
  
  // Pipeline creation state
  const [showCreatePipelineDialog, setShowCreatePipelineDialog] = useState(false)
  const [pipelineFormData, setPipelineFormData] = useState({
    provider: 'JENKINS' as PipelineProvider,
    jobName: '',
    buildNumber: 1,
    branch: 'main',
    commitSha: '',
  })
  const [pipelineFormError, setPipelineFormError] = useState<string | null>(null)
  const [pipelinApiError, setPipelineApiError] = useState<string | null>(null)
  const [pipelineSubmitting, setPipelineSubmitting] = useState(false)

  // Scan creation state
  const [showCreateScanDialog, setShowCreateScanDialog] = useState(false)
  const [scanFormData, setScanFormData] = useState({ pipelineId: '', environment: 'DEV' as ScanEnvironment })
  const [scanFormError, setScanFormError] = useState<string | null>(null)
  const [scanApiError, setScanApiError] = useState<string | null>(null)
  const [scanSubmitting, setScanSubmitting] = useState(false)

  const handleCreatePipeline = async () => {
    setPipelineFormError(null)
    setPipelineApiError(null)

    // Validation
    if (!pipelineFormData.jobName.trim()) {
      setPipelineFormError('Job name is required')
      return
    }

    if (pipelineFormData.buildNumber < 1) {
      setPipelineFormError('Build number must be at least 1')
      return
    }

    if (!pipelineFormData.branch.trim()) {
      setPipelineFormError('Branch is required')
      return
    }

    if (!selectedProjectId) {
      setPipelineFormError('Please select a project')
      return
    }

    setPipelineSubmitting(true)
    try {
      await pipelineService.createPipeline(selectedProjectId, {
        provider: pipelineFormData.provider,
        jobName: pipelineFormData.jobName,
        buildNumber: pipelineFormData.buildNumber,
        branch: pipelineFormData.branch,
        commitSha: pipelineFormData.commitSha || undefined,
      })

      queryClient.invalidateQueries({ queryKey: ['pipelines', selectedProjectId] })
      success('Pipeline created successfully')

      // Reset form
      setPipelineFormData({
        provider: 'JENKINS' as PipelineProvider,
        jobName: '',
        buildNumber: 1,
        branch: 'main',
        commitSha: '',
      })
      setShowCreatePipelineDialog(false)
    } catch (err: any) {
      const errorMsg = err.response?.data?.message || err.message || 'Failed to create pipeline'
      setPipelineApiError(errorMsg)
      error(errorMsg)
    } finally {
      setPipelineSubmitting(false)
    }
  }

  const handleCreateScan = async () => {
    setScanFormError(null)
    setScanApiError(null)

    if (!scanFormData.pipelineId) {
      setScanFormError('Please select a pipeline')
      return
    }

    if (!selectedProjectId) {
      setScanFormError('Please select a project')
      return
    }

    setScanSubmitting(true)
    try {
      await scanService.createScan(selectedProjectId, {
        pipelineId: scanFormData.pipelineId,
        environment: scanFormData.environment,
      })

      queryClient.invalidateQueries({ queryKey: ['scans', selectedProjectId] })
      success('Scan created successfully')

      setScanFormData({ pipelineId: '', environment: 'DEV' })
      setShowCreateScanDialog(false)
    } catch (err: any) {
      const errorMsg = err.response?.data?.message || err.message || 'Failed to create scan'
      setScanApiError(errorMsg)
      error(errorMsg)
    } finally {
      setScanSubmitting(false)
    }
  }

  return (
    <>
      <Helmet>
        <title>Pipelines - SecureOps AI</title>
      </Helmet>

      <div className="space-y-6">
        <PageHeading title="Pipelines & Scans" />

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

        {/* Pipelines Section */}
        <div className="bg-white rounded-lg border border-gray-200 p-6">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-lg font-semibold text-gray-900">Pipelines</h3>
            <Button
              onClick={() => setShowCreatePipelineDialog(true)}
            >
              <Plus size={20} className="inline mr-2" />
              Create Pipeline
            </Button>
          </div>

          {pipelinesLoading ? (
            <LoadingSpinner />
          ) : pipelines.length === 0 ? (
            <div className="text-center py-8">
              <p className="text-gray-600">No pipelines registered for this project yet</p>
              <p className="text-sm text-gray-500 mt-2">Create a pipeline to register your CI/CD job</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-gray-200">
                    <th className="text-left py-3 px-4 font-semibold text-gray-700">Provider</th>
                    <th className="text-left py-3 px-4 font-semibold text-gray-700">Job Name</th>
                    <th className="text-left py-3 px-4 font-semibold text-gray-700">Build #</th>
                    <th className="text-left py-3 px-4 font-semibold text-gray-700">Branch</th>
                    <th className="text-left py-3 px-4 font-semibold text-gray-700">Commit</th>
                    <th className="text-left py-3 px-4 font-semibold text-gray-700">Created</th>
                  </tr>
                </thead>
                <tbody>
                  {pipelines.map(pipeline => (
                    <tr key={pipeline.id} className="border-b border-gray-100 hover:bg-gray-50">
                      <td className="py-3 px-4">
                        <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-purple-100 text-purple-800">
                          {pipeline.provider}
                        </span>
                      </td>
                      <td className="py-3 px-4 text-gray-900">{pipeline.jobName}</td>
                      <td className="py-3 px-4 font-mono text-gray-600">#{pipeline.buildNumber}</td>
                      <td className="py-3 px-4 text-gray-600">{pipeline.branch}</td>
                      <td className="py-3 px-4 font-mono text-xs text-gray-600">
                        {pipeline.commitSha ? pipeline.commitSha.substring(0, 8) : '—'}
                      </td>
                      <td className="py-3 px-4 text-sm text-gray-600">
                        {new Date(pipeline.createdAt).toLocaleDateString()}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {/* Scans Section */}
        <div className="bg-white rounded-lg border border-gray-200 p-6">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-lg font-semibold text-gray-900">Scans</h3>
            {pipelines.length === 0 ? (
              <div className="text-sm text-gray-600">
                Create a pipeline first
              </div>
            ) : (
              <Button
                onClick={() => setShowCreateScanDialog(true)}
              >
                <Plus size={20} className="inline mr-2" />
                Create Scan
              </Button>
            )}
          </div>

          {pipelines.length === 0 ? (
            <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
              <div className="flex items-start gap-3">
                <AlertCircle size={20} className="text-blue-600 flex-shrink-0 mt-0.5" />
                <div>
                  <p className="font-medium text-blue-900">No pipelines available</p>
                  <p className="text-sm text-blue-800 mt-1">
                    Create a pipeline first to start creating scans for security analysis.
                  </p>
                  <button 
                    onClick={() => setShowCreatePipelineDialog(true)}
                    className="text-blue-600 hover:text-blue-800 text-sm font-medium mt-2 inline-block"
                  >
                    Create Pipeline →
                  </button>
                </div>
              </div>
            </div>
          ) : scansLoading ? (
            <LoadingSpinner />
          ) : scans.length === 0 ? (
            <div className="text-center py-8">
              <p className="text-gray-600">No scans created for this project yet</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-gray-200">
                    <th className="text-left py-3 px-4 font-semibold text-gray-700">Environment</th>
                    <th className="text-left py-3 px-4 font-semibold text-gray-700">Status</th>
                    <th className="text-left py-3 px-4 font-semibold text-gray-700">Started</th>
                    <th className="text-left py-3 px-4 font-semibold text-gray-700">Completed</th>
                  </tr>
                </thead>
                <tbody>
                  {scans.map(scan => (
                    <tr key={scan.id} className="border-b border-gray-100 hover:bg-gray-50">
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
                      <td className="py-3 px-4 text-sm text-gray-600">
                        {new Date(scan.startedAt).toLocaleDateString()} {new Date(scan.startedAt).toLocaleTimeString()}
                      </td>
                      <td className="py-3 px-4 text-sm text-gray-600">
                        {scan.completedAt
                          ? `${new Date(scan.completedAt).toLocaleDateString()} ${new Date(scan.completedAt).toLocaleTimeString()}`
                          : '—'}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      {/* Create Pipeline Dialog */}
      <Dialog
        isOpen={showCreatePipelineDialog}
        onClose={() => {
          setShowCreatePipelineDialog(false)
          setPipelineFormError(null)
          setPipelineApiError(null)
          setPipelineFormData({
            provider: 'JENKINS' as PipelineProvider,
            jobName: '',
            buildNumber: 1,
            branch: 'main',
            commitSha: '',
          })
        }}
        title="Create New Pipeline"
      >
        <div className="space-y-4">
          {pipelinApiError && (
            <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm">
              {pipelinApiError}
            </div>
          )}
          {pipelineFormError && (
            <div className="bg-yellow-50 border border-yellow-200 text-yellow-700 px-4 py-3 rounded-lg text-sm">
              {pipelineFormError}
            </div>
          )}

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Provider *</label>
            <select
              value={pipelineFormData.provider}
              onChange={e => setPipelineFormData({ ...pipelineFormData, provider: e.target.value as PipelineProvider })}
              disabled={pipelineSubmitting}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              {PIPELINE_PROVIDERS.map(provider => (
                <option key={provider} value={provider}>
                  {provider}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Job Name *</label>
            <input
              type="text"
              value={pipelineFormData.jobName}
              onChange={e => setPipelineFormData({ ...pipelineFormData, jobName: e.target.value })}
              placeholder="e.g., my-build-job"
              disabled={pipelineSubmitting}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Build Number *</label>
              <input
                type="number"
                value={pipelineFormData.buildNumber}
                onChange={e => setPipelineFormData({ ...pipelineFormData, buildNumber: parseInt(e.target.value) || 1 })}
                min="1"
                disabled={pipelineSubmitting}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Branch *</label>
              <input
                type="text"
                value={pipelineFormData.branch}
                onChange={e => setPipelineFormData({ ...pipelineFormData, branch: e.target.value })}
                placeholder="e.g., main"
                disabled={pipelineSubmitting}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Commit SHA (Optional)</label>
            <input
              type="text"
              value={pipelineFormData.commitSha}
              onChange={e => setPipelineFormData({ ...pipelineFormData, commitSha: e.target.value })}
              placeholder="e.g., abc123def456..."
              disabled={pipelineSubmitting}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <div className="flex justify-end space-x-3 pt-4">
            <Button
              variant="secondary"
              onClick={() => setShowCreatePipelineDialog(false)}
              disabled={pipelineSubmitting}
            >
              Cancel
            </Button>
            <Button
              onClick={handleCreatePipeline}
              disabled={pipelineSubmitting}
            >
              {pipelineSubmitting ? 'Creating...' : 'Create Pipeline'}
            </Button>
          </div>
        </div>
      </Dialog>

      {/* Create Scan Dialog */}
      <Dialog
        isOpen={showCreateScanDialog}
        onClose={() => {
          setShowCreateScanDialog(false)
          setScanFormError(null)
          setScanApiError(null)
          setScanFormData({ pipelineId: '', environment: 'DEV' })
        }}
        title="Create New Scan"
      >
        <div className="space-y-4">
          {scanApiError && (
            <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm">
              {scanApiError}
            </div>
          )}
          {scanFormError && (
            <div className="bg-yellow-50 border border-yellow-200 text-yellow-700 px-4 py-3 rounded-lg text-sm">
              {scanFormError}
            </div>
          )}

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Pipeline *</label>
            <select
              value={scanFormData.pipelineId}
              onChange={e => setScanFormData({ ...scanFormData, pipelineId: e.target.value })}
              disabled={scanSubmitting}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="">Select a pipeline...</option>
              {pipelines.map(pipeline => (
                <option key={pipeline.id} value={pipeline.id}>
                  {pipeline.jobName} (Build #{pipeline.buildNumber})
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Environment *</label>
            <select
              value={scanFormData.environment}
              onChange={e => setScanFormData({ ...scanFormData, environment: e.target.value as ScanEnvironment })}
              disabled={scanSubmitting}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              {ENVIRONMENTS.map(env => (
                <option key={env} value={env}>
                  {env}
                </option>
              ))}
            </select>
          </div>

          <div className="flex justify-end space-x-3 pt-4">
            <Button
              variant="secondary"
              onClick={() => setShowCreateScanDialog(false)}
              disabled={scanSubmitting}
            >
              Cancel
            </Button>
            <Button
              onClick={handleCreateScan}
              disabled={scanSubmitting}
            >
              {scanSubmitting ? 'Creating...' : 'Create Scan'}
            </Button>
          </div>
        </div>
      </Dialog>
    </>
  )
}
