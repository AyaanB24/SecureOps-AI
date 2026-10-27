/**
 * Security Policies Page
 * Display and manage security policies for project environments
 */

import { useState } from 'react'
import { Helmet } from 'react-helmet-async'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useProjects } from '@/hooks/useProjects'
import { usePolicies } from '@/hooks/usePolicies'
import * as policyService from '@/services/policyService'
import { PageHeading } from '@/components/common/PageHeading'
import { Button } from '@/components/common/Button'
import { LoadingSpinner } from '@/components/common/LoadingSpinner'
import { Dialog } from '@/components/common/Dialog'
import { ConfirmDialog } from '@/components/common/ConfirmDialog'
import { useToast } from '@/contexts/ToastContext'
import { AlertCircle, Edit2, Plus } from 'lucide-react'
import { Policy, PolicyEnvironment, CreatePolicyRequest } from '@/types/project'

interface EditingPolicy {
  projectId: string
  policy: Policy
}

const POLICY_ENVIRONMENTS: PolicyEnvironment[] = [
  PolicyEnvironment.DEVELOPMENT,
  PolicyEnvironment.STAGING,
  PolicyEnvironment.PRODUCTION,
]

export default function Policies() {
  const queryClient = useQueryClient()
  const { success, error } = useToast()
  const { data: projects = [] } = useProjects()

  const [selectedProjectId, setSelectedProjectId] = useState<string | null>(
    projects.length > 0 ? projects[0].id : null
  )

  const { data: policies = [], isLoading: policiesLoading } = usePolicies(selectedProjectId || '')

  // Edit state
  const [editingPolicy, setEditingPolicy] = useState<EditingPolicy | null>(null)
  const [showEditDialog, setShowEditDialog] = useState(false)
  const [formData, setFormData] = useState<CreatePolicyRequest | null>(null)
  const [formError, setFormError] = useState<string | null>(null)

  // Create state
  const [showCreateDialog, setShowCreateDialog] = useState(false)
  const [createFormData, setCreateFormData] = useState<CreatePolicyRequest>({
    environment: PolicyEnvironment.DEVELOPMENT,
    maxCritical: 5,
    maxHigh: 10,
    maxRiskScore: 100,
  })
  const [createFormError, setCreateFormError] = useState<string | null>(null)

  // Confirm state
  const [showConfirmSave, setShowConfirmSave] = useState(false)
  const [showConfirmCreate, setShowConfirmCreate] = useState(false)

  // Success/Error messages
  const [successMessage, setSuccessMessage] = useState<string | null>(null)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  // Mutations
  const updateMutation = useMutation({
    mutationFn: async ({
      projectId,
      environment,
      data,
    }: {
      projectId: string
      environment: PolicyEnvironment
      data: CreatePolicyRequest
    }) => {
      return policyService.updatePolicy(projectId, environment, data)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['policies', selectedProjectId] })
      setSuccessMessage('Policy updated successfully')
      setTimeout(() => setSuccessMessage(null), 3000)
      setShowEditDialog(false)
      setEditingPolicy(null)
      setFormData(null)
    },
    onError: (error: any) => {
      setErrorMessage(
        error.response?.data?.message || error.message || 'Failed to update policy'
      )
      setTimeout(() => setErrorMessage(null), 5000)
    },
  })

  const createMutation = useMutation({
    mutationFn: async ({ projectId, data }: { projectId: string; data: CreatePolicyRequest }) => {
      return policyService.createPolicy(projectId, data)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['policies', selectedProjectId] })
      success('Policy created successfully')
      setShowCreateDialog(false)
      setCreateFormData({
        environment: PolicyEnvironment.DEVELOPMENT,
        maxCritical: 5,
        maxHigh: 10,
        maxRiskScore: 100,
      })
      setCreateFormError(null)
    },
    onError: (err: any) => {
      const errorMsg = err.response?.data?.message || err.message || 'Failed to create policy'
      setCreateFormError(errorMsg)
      error(errorMsg)
    },
  })

  const handleEditClick = (policy: Policy) => {
    setEditingPolicy({ projectId: selectedProjectId!, policy })
    setFormData({
      environment: policy.environment,
      maxCritical: policy.maxCritical,
      maxHigh: policy.maxHigh,
      maxRiskScore: policy.maxRiskScore,
    })
    setFormError(null)
    setShowEditDialog(true)
  }

  const validateForm = (): boolean => {
    if (!formData) {
      setFormError('Form data is missing')
      return false
    }

    if (formData.maxCritical < 0) {
      setFormError('Max Critical must be non-negative')
      return false
    }

    if (formData.maxHigh < 0) {
      setFormError('Max High must be non-negative')
      return false
    }

    if (formData.maxRiskScore < 0) {
      setFormError('Max Risk Score must be non-negative')
      return false
    }

    return true
  }

  const handleSaveClick = () => {
    setFormError(null)
    if (!validateForm()) return
    setShowConfirmSave(true)
  }

  const handleConfirmSave = async () => {
    setShowConfirmSave(false)
    if (!editingPolicy || !formData) return

    updateMutation.mutate({
      projectId: editingPolicy.projectId,
      environment: editingPolicy.policy.environment,
      data: formData,
    })
  }

  const validateCreateForm = (): boolean => {
    if (createFormData.maxCritical < 0) {
      setCreateFormError('Max Critical must be non-negative')
      return false
    }

    if (createFormData.maxHigh < 0) {
      setCreateFormError('Max High must be non-negative')
      return false
    }

    if (createFormData.maxRiskScore < 0) {
      setCreateFormError('Max Risk Score must be non-negative')
      return false
    }

    // Check if policy already exists for this environment
    if (policies.some(p => p.environment === createFormData.environment)) {
      setCreateFormError(`Policy for ${createFormData.environment} already exists`)
      return false
    }

    return true
  }

  const handleCreateClick = () => {
    setCreateFormError(null)
    if (!validateCreateForm()) return
    setShowConfirmCreate(true)
  }

  const handleConfirmCreate = async () => {
    setShowConfirmCreate(false)
    if (!selectedProjectId) return

    createMutation.mutate({
      projectId: selectedProjectId,
      data: createFormData,
    })
  }

  return (
    <>
      <Helmet>
        <title>Security Policies - SecureOps AI</title>
      </Helmet>

      <div className="space-y-6">
        <PageHeading title="Security Policies" />

        {/* Info Section */}
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 flex items-start space-x-3">
          <AlertCircle size={20} className="text-blue-600 flex-shrink-0 mt-0.5" />
          <div className="text-sm text-blue-900">
            <p className="font-medium">Policy Thresholds</p>
            <p className="mt-1">
              Set maximum limits for each environment. When a scan exceeds these thresholds, the
              decision will be BLOCKED. Different projects can have different policies for the same
              environment.
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

        {successMessage && (
          <div className="bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded-lg flex items-start space-x-3">
            <AlertCircle size={20} className="flex-shrink-0 mt-0.5" />
            <p>{successMessage}</p>
          </div>
        )}

        {errorMessage && (
          <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg flex items-start space-x-3">
            <AlertCircle size={20} className="flex-shrink-0 mt-0.5" />
            <p>{errorMessage}</p>
          </div>
        )}

        {/* Policies List */}
        <div className="bg-white rounded-lg border border-gray-200 p-6">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-lg font-semibold text-gray-900">Project Policies</h3>
            {selectedProjectId && policies.length < 3 && (
              <Button
                onClick={() => {
                  setCreateFormError(null)
                  setShowCreateDialog(true)
                }}
              >
                <Plus size={20} className="inline mr-2" />
                Create Policy
              </Button>
            )}
          </div>

          {policiesLoading ? (
            <LoadingSpinner />
          ) : policies.length === 0 ? (
            <div className="text-center py-12">
              <AlertCircle size={48} className="mx-auto text-gray-400 mb-4" />
              <p className="text-gray-600">No policies configured for this project</p>
              <p className="text-sm text-gray-500 mt-2">
                Create a policy to define security thresholds for each environment
              </p>
            </div>
          ) : (
            <div className="space-y-4">
              {policies.map((policy: Policy) => (
                <PolicyCard
                  key={policy.id}
                  policy={policy}
                  onEdit={handleEditClick}
                />
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Edit Policy Dialog */}
      <Dialog
        isOpen={showEditDialog}
        onClose={() => {
          setShowEditDialog(false)
          setEditingPolicy(null)
          setFormData(null)
          setFormError(null)
        }}
        title={`Edit Policy - ${editingPolicy?.policy.environment}`}
      >
        {formData && (
          <div className="space-y-4">
            {formError && (
              <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm">
                {formError}
              </div>
            )}

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Max Critical Findings
              </label>
              <p className="text-xs text-gray-600 mb-2">
                Maximum allowed CRITICAL severity findings (0 = zero tolerance)
              </p>
              <input
                type="number"
                min="0"
                value={formData.maxCritical}
                onChange={e =>
                  setFormData({ ...formData, maxCritical: parseInt(e.target.value) || 0 })
                }
                disabled={updateMutation.isPending}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Max High Findings
              </label>
              <p className="text-xs text-gray-600 mb-2">
                Maximum allowed HIGH severity findings
              </p>
              <input
                type="number"
                min="0"
                value={formData.maxHigh}
                onChange={e =>
                  setFormData({ ...formData, maxHigh: parseInt(e.target.value) || 0 })
                }
                disabled={updateMutation.isPending}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Max Risk Score
              </label>
              <p className="text-xs text-gray-600 mb-2">
                Maximum allowed cumulative risk score
              </p>
              <input
                type="number"
                min="0"
                value={formData.maxRiskScore}
                onChange={e =>
                  setFormData({ ...formData, maxRiskScore: parseInt(e.target.value) || 0 })
                }
                disabled={updateMutation.isPending}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <div className="pt-4 border-t border-gray-200 flex justify-end space-x-3">
              <Button
                variant="secondary"
                onClick={() => setShowEditDialog(false)}
                disabled={updateMutation.isPending}
              >
                Cancel
              </Button>
              <Button onClick={handleSaveClick} disabled={updateMutation.isPending}>
                {updateMutation.isPending ? 'Saving...' : 'Save Changes'}
              </Button>
            </div>
          </div>
        )}
      </Dialog>

      {/* Confirm Save Dialog */}
      <ConfirmDialog
        isOpen={showConfirmSave}
        onClose={() => setShowConfirmSave(false)}
        onConfirm={handleConfirmSave}
        title="Confirm Policy Change"
        message="Are you sure you want to update this security policy? This will affect how future scans for this environment are evaluated."
        isDestructive={false}
      />

      {/* Create Policy Dialog */}
      <Dialog
        isOpen={showCreateDialog}
        onClose={() => {
          setShowCreateDialog(false)
          setCreateFormData({
            environment: PolicyEnvironment.DEVELOPMENT,
            maxCritical: 5,
            maxHigh: 10,
            maxRiskScore: 100,
          })
          setCreateFormError(null)
        }}
        title="Create New Policy"
      >
        <div className="space-y-4">
          {createFormError && (
            <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm">
              {createFormError}
            </div>
          )}

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Environment *
            </label>
            <select
              value={createFormData.environment}
              onChange={e =>
                setCreateFormData({
                  ...createFormData,
                  environment: e.target.value as PolicyEnvironment,
                })
              }
              disabled={createMutation.isPending}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              {POLICY_ENVIRONMENTS.filter(env => !policies.some(p => p.environment === env)).map(
                env => (
                  <option key={env} value={env}>
                    {env}
                  </option>
                )
              )}
            </select>
            <p className="text-xs text-gray-600 mt-1">
              Choose an environment that doesn't already have a policy
            </p>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Max Critical Findings
            </label>
            <p className="text-xs text-gray-600 mb-2">
              Maximum allowed CRITICAL severity findings (0 = zero tolerance)
            </p>
            <input
              type="number"
              min="0"
              value={createFormData.maxCritical}
              onChange={e =>
                setCreateFormData({
                  ...createFormData,
                  maxCritical: parseInt(e.target.value) || 0,
                })
              }
              disabled={createMutation.isPending}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Max High Findings
            </label>
            <p className="text-xs text-gray-600 mb-2">
              Maximum allowed HIGH severity findings
            </p>
            <input
              type="number"
              min="0"
              value={createFormData.maxHigh}
              onChange={e =>
                setCreateFormData({
                  ...createFormData,
                  maxHigh: parseInt(e.target.value) || 0,
                })
              }
              disabled={createMutation.isPending}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Max Risk Score
            </label>
            <p className="text-xs text-gray-600 mb-2">
              Maximum allowed cumulative risk score
            </p>
            <input
              type="number"
              min="0"
              value={createFormData.maxRiskScore}
              onChange={e =>
                setCreateFormData({
                  ...createFormData,
                  maxRiskScore: parseInt(e.target.value) || 0,
                })
              }
              disabled={createMutation.isPending}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <div className="pt-4 border-t border-gray-200 flex justify-end space-x-3">
            <Button
              variant="secondary"
              onClick={() => setShowCreateDialog(false)}
              disabled={createMutation.isPending}
            >
              Cancel
            </Button>
            <Button onClick={handleCreateClick} disabled={createMutation.isPending}>
              {createMutation.isPending ? 'Creating...' : 'Create Policy'}
            </Button>
          </div>
        </div>
      </Dialog>

      {/* Confirm Create Dialog */}
      <ConfirmDialog
        isOpen={showConfirmCreate}
        onClose={() => setShowConfirmCreate(false)}
        onConfirm={handleConfirmCreate}
        title="Confirm New Policy"
        message={`Are you sure you want to create a new policy for ${createFormData.environment}? This will define the security thresholds for this environment.`}
        isDestructive={false}
      />
    </>
  )
}

/**
 * Component to display a single policy
 */
function PolicyCard({
  policy,
  onEdit,
}: {
  policy: Policy
  onEdit: (policy: Policy) => void
}) {
  return (
    <div className="border border-gray-200 rounded-lg p-4 hover:border-gray-300 transition">
      <div className="flex items-start justify-between mb-4">
        <div>
          <h4 className="font-semibold text-gray-900">
            {policy.environment} Environment
          </h4>
          <p className="text-xs text-gray-600 mt-1">
            Last updated: {new Date(policy.updatedAt).toLocaleDateString()}
          </p>
        </div>
        <Button size="sm" variant="secondary" onClick={() => onEdit(policy)}>
          <Edit2 size={16} className="inline mr-1" />
          Edit
        </Button>
      </div>

      <div className="grid grid-cols-3 gap-4">
        <div className="bg-red-50 p-3 rounded border border-red-100">
          <p className="text-xs font-medium text-red-700 mb-1">Max Critical</p>
          <p className="text-2xl font-bold text-red-900">{policy.maxCritical}</p>
          <p className="text-xs text-red-600 mt-1">Zero tolerance if 0</p>
        </div>

        <div className="bg-orange-50 p-3 rounded border border-orange-100">
          <p className="text-xs font-medium text-orange-700 mb-1">Max High</p>
          <p className="text-2xl font-bold text-orange-900">{policy.maxHigh}</p>
          <p className="text-xs text-orange-600 mt-1">Findings allowed</p>
        </div>

        <div className="bg-blue-50 p-3 rounded border border-blue-100">
          <p className="text-xs font-medium text-blue-700 mb-1">Max Risk Score</p>
          <p className="text-2xl font-bold text-blue-900">{policy.maxRiskScore}</p>
          <p className="text-xs text-blue-600 mt-1">Score threshold</p>
        </div>
      </div>

      <div className="mt-4 p-3 bg-gray-50 rounded text-xs text-gray-700">
        <p>
          Scans in <strong>{policy.environment}</strong> will be <strong>BLOCKED</strong> if:
        </p>
        <ul className="list-disc list-inside mt-2 space-y-1">
          <li>CRITICAL findings exceed {policy.maxCritical}</li>
          <li>HIGH findings exceed {policy.maxHigh}</li>
          <li>Risk score exceeds {policy.maxRiskScore}</li>
        </ul>
      </div>
    </div>
  )
}
