/**
 * Projects Page
 * Displays project list with search/pagination
 * Allows registering new projects
 */

import { useState } from 'react'
import { Helmet } from 'react-helmet-async'
import { useProjects } from '@/hooks/useProjects'
import { projectService } from '@/services/projectService'
import { PageHeading } from '@/components/common/PageHeading'
import { Button } from '@/components/common/Button'
import { Input } from '@/components/common/Input'
import { LoadingSpinner } from '@/components/common/LoadingSpinner'
import { ErrorState } from '@/components/common/ErrorState'
import { Dialog } from '@/components/common/Dialog'
import { ConfirmDialog } from '@/components/common/ConfirmDialog'
import { ExternalLink, Plus, AlertCircle } from 'lucide-react'
import { useQueryClient } from '@tanstack/react-query'

export default function Projects() {
  const queryClient = useQueryClient()
  const { data: projects = [], isLoading, error, refetch } = useProjects()
  
  const [showCreateDialog, setShowCreateDialog] = useState(false)
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false)
  const [formData, setFormData] = useState({ name: '', repositoryUrl: '' })
  const [formError, setFormError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [apiError, setApiError] = useState<string | null>(null)
  const [successMessage, setSuccessMessage] = useState<string | null>(null)
  const [selectedProjectId, setSelectedProjectId] = useState<string | null>(null)
  const [isDeleting, setIsDeleting] = useState(false)

  const validateForm = () => {
    if (!formData.name.trim()) {
      setFormError('Project name is required')
      return false
    }
    if (formData.name.length > 255) {
      setFormError('Project name must be less than 255 characters')
      return false
    }
    if (formData.repositoryUrl && formData.repositoryUrl.length > 512) {
      setFormError('Repository URL must be less than 512 characters')
      return false
    }
    if (formData.repositoryUrl && !isValidUrl(formData.repositoryUrl)) {
      setFormError('Repository URL must be a valid URL')
      return false
    }
    return true
  }

  const isValidUrl = (urlString: string) => {
    try {
      new URL(urlString)
      return true
    } catch {
      return false
    }
  }

  const handleCreateProject = async () => {
    setFormError(null)
    setApiError(null)

    if (!validateForm()) {
      return
    }

    setIsSubmitting(true)
    try {
      const newProject = await projectService.create({
        name: formData.name.trim(),
        repositoryUrl: formData.repositoryUrl.trim() || undefined,
      })

      // Invalidate and refetch projects
      queryClient.invalidateQueries({ queryKey: ['projects'] })

      // Show success message
      setSuccessMessage(`Project "${newProject.name}" created successfully`)
      setTimeout(() => setSuccessMessage(null), 3000)

      // Reset form and close dialog
      setFormData({ name: '', repositoryUrl: '' })
      setShowCreateDialog(false)
    } catch (err: any) {
      if (err.response?.status === 409) {
        setApiError('A project with this name already exists')
      } else {
        setApiError(err.response?.data?.message || err.message || 'Failed to create project')
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleDeleteProject = async () => {
    if (!selectedProjectId) return

    setIsDeleting(true)
    try {
      await projectService.delete(selectedProjectId)

      // Invalidate and refetch projects
      queryClient.invalidateQueries({ queryKey: ['projects'] })

      // Show success message
      setSuccessMessage('Project deleted successfully')
      setTimeout(() => setSuccessMessage(null), 3000)

      // Close dialog and reset
      setShowDeleteConfirm(false)
      setSelectedProjectId(null)
    } catch (err: any) {
      setApiError(err.response?.data?.message || err.message || 'Failed to delete project')
      setShowDeleteConfirm(false)
      setSelectedProjectId(null)
    } finally {
      setIsDeleting(false)
    }
  }

  if (isLoading) {
    return (
      <>
        <Helmet>
          <title>Projects - SecureOps AI</title>
        </Helmet>
        <div className="space-y-6">
          <PageHeading title="Projects" />
          <LoadingSpinner />
        </div>
      </>
    )
  }

  if (error) {
    return (
      <>
        <Helmet>
          <title>Projects - SecureOps AI</title>
        </Helmet>
        <div className="space-y-6">
          <PageHeading title="Projects" />
          <ErrorState
            title="Failed to load projects"
            message={error instanceof Error ? error.message : 'Unable to fetch projects'}
            onRetry={() => refetch()}
          />
        </div>
      </>
    )
  }

  return (
    <>
      <Helmet>
        <title>Projects - SecureOps AI</title>
      </Helmet>

      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <PageHeading title="Projects" />
          <Button
            onClick={() => setShowCreateDialog(true)}
          >
            <Plus size={20} className="inline mr-2" />
            New Project
          </Button>
        </div>

        {successMessage && (
          <div className="bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded-lg flex items-start space-x-3">
            <AlertCircle size={20} className="flex-shrink-0 mt-0.5" />
            <p>{successMessage}</p>
          </div>
        )}

        {/* Projects List */}
        {projects.length === 0 ? (
          <div className="bg-white p-12 rounded-lg border border-gray-200 text-center">
            <p className="text-gray-600 mb-4">No projects registered yet</p>
            <Button onClick={() => setShowCreateDialog(true)} variant="primary">
              Create your first project
            </Button>
          </div>
        ) : (
          <div className="bg-white rounded-lg border border-gray-200 overflow-hidden">
            <table className="w-full">
              <thead>
                <tr className="border-b border-gray-200 bg-gray-50">
                  <th className="px-6 py-4 text-left text-sm font-semibold text-gray-700">Project Name</th>
                  <th className="px-6 py-4 text-left text-sm font-semibold text-gray-700">Repository</th>
                  <th className="px-6 py-4 text-left text-sm font-semibold text-gray-700">Created</th>
                  <th className="px-6 py-4 text-right text-sm font-semibold text-gray-700">Actions</th>
                </tr>
              </thead>
              <tbody>
                {projects.map(project => (
                  <tr key={project.id} className="border-b border-gray-100 hover:bg-gray-50 transition-colors">
                    <td className="px-6 py-4">
                      <p className="font-medium text-gray-900">{project.name}</p>
                      <p className="text-xs text-gray-500 mt-1">{project.id}</p>
                    </td>
                    <td className="px-6 py-4">
                      {project.repositoryUrl ? (
                        <a
                          href={project.repositoryUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="text-blue-600 hover:text-blue-700 flex items-center space-x-1"
                        >
                          <span className="truncate max-w-xs">{project.repositoryUrl}</span>
                          <ExternalLink size={16} className="flex-shrink-0" />
                        </a>
                      ) : (
                        <span className="text-gray-500 text-sm">Not specified</span>
                      )}
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-600">
                      {new Date(project.createdAt).toLocaleDateString()}
                    </td>
                    <td className="px-6 py-4 text-right">
                      <Button
                        variant="secondary"
                        size="sm"
                        onClick={() => {
                          setSelectedProjectId(project.id)
                          setShowDeleteConfirm(true)
                        }}
                      >
                        Delete
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Create Project Dialog */}
      <Dialog
        isOpen={showCreateDialog}
        onClose={() => {
          setShowCreateDialog(false)
          setFormError(null)
          setApiError(null)
          setFormData({ name: '', repositoryUrl: '' })
        }}
        title="Register New Project"
      >
        <div className="space-y-4">
          {apiError && (
            <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm">
              {apiError}
            </div>
          )}
          {formError && (
            <div className="bg-yellow-50 border border-yellow-200 text-yellow-700 px-4 py-3 rounded-lg text-sm">
              {formError}
            </div>
          )}
          <Input
            label="Project Name *"
            placeholder="My Awesome Project"
            value={formData.name}
            onChange={e => setFormData({ ...formData, name: e.target.value })}
            disabled={isSubmitting}
          />
          <Input
            label="Repository URL (Optional)"
            placeholder="https://github.com/user/repo"
            value={formData.repositoryUrl}
            onChange={e => setFormData({ ...formData, repositoryUrl: e.target.value })}
            disabled={isSubmitting}
          />
          <div className="flex justify-end space-x-3 pt-4">
            <Button
              variant="secondary"
              onClick={() => setShowCreateDialog(false)}
              disabled={isSubmitting}
            >
              Cancel
            </Button>
            <Button
              onClick={handleCreateProject}
              disabled={isSubmitting}
            >
              {isSubmitting ? 'Creating...' : 'Create Project'}
            </Button>
          </div>
        </div>
      </Dialog>

      {/* Delete Confirmation Dialog */}
      <ConfirmDialog
        isOpen={showDeleteConfirm}
        title="Delete Project"
        message="Are you sure you want to delete this project? This action cannot be undone."
        confirmText="Delete"
        cancelText="Cancel"
        onConfirm={handleDeleteProject}
        onClose={() => {
          setShowDeleteConfirm(false)
          setSelectedProjectId(null)
        }}
        isDestructive
        isLoading={isDeleting}
      />
    </>
  )
}
