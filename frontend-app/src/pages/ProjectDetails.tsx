/**
 * Project Details Page
 * Shows project information including Project ID for Jenkins integration
 */

import { useParams, useNavigate } from 'react-router-dom'
import { Helmet } from 'react-helmet-async'
import { useState, useEffect } from 'react'
import { PageHeading } from '@/components/common/PageHeading'
import { Button } from '@/components/common/Button'
import { LoadingSpinner } from '@/components/common/LoadingSpinner'
import { ErrorState } from '@/components/common/ErrorState'
import { Copy, CheckCircle, ExternalLink, ArrowLeft } from 'lucide-react'
import { projectService } from '@/services/projectService'
import { Project } from '@/types/project'

export default function ProjectDetails() {
  const { projectId } = useParams<{ projectId: string }>()
  const navigate = useNavigate()
  const [project, setProject] = useState<Project | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [copiedId, setCopiedId] = useState(false)

  useEffect(() => {
    loadProject()
  }, [projectId])

  const loadProject = async () => {
    if (!projectId) return

    setIsLoading(true)
    setError(null)

    try {
      console.log('Fetching project:', projectId)
      const data = await projectService.getById(projectId)
      console.log('Project data received:', data)
      setProject(data)
    } catch (err: any) {
      console.error('Error fetching project:', err)
      if (err.status === 404) {
        setError('Project not found')
      } else if (err.status === 403) {
        setError('You do not have access to this project')
      } else {
        setError(err.message || 'Failed to load project')
      }
    } finally {
      setIsLoading(false)
    }
  }

  const copyProjectId = async () => {
    if (project?.id) {
      await navigator.clipboard.writeText(project.id)
      setCopiedId(true)
      setTimeout(() => setCopiedId(false), 2000)
    }
  }

  if (isLoading) {
    return (
      <>
        <Helmet>
          <title>Project Details - SecureOps AI</title>
        </Helmet>
        <div className="space-y-6">
          <PageHeading title="Project Details" />
          <LoadingSpinner />
        </div>
      </>
    )
  }

  if (error || !project) {
    return (
      <>
        <Helmet>
          <title>Project Details - SecureOps AI</title>
        </Helmet>
        <div className="space-y-6">
          <div className="flex items-center gap-2">
            <Button variant="secondary" size="sm" onClick={() => navigate('/projects')}>
              <ArrowLeft size={16} />
            </Button>
            <PageHeading title="Project Details" />
          </div>
          <ErrorState
            title="Failed to load project"
            message={error || 'Project not found'}
            onRetry={() => loadProject()}
          />
        </div>
      </>
    )
  }

  return (
    <>
      <Helmet>
        <title>{project?.name || 'Project'} - SecureOps AI</title>
      </Helmet>

      <div className="space-y-6">
        <div className="flex items-center gap-2">
          <Button variant="secondary" size="sm" onClick={() => navigate('/projects')}>
            <ArrowLeft size={16} />
          </Button>
          <PageHeading title={project?.name || 'Project Details'} />
        </div>

        {error && (
          <div className="bg-red-50 border border-red-200 rounded-lg p-4 text-sm text-red-800">
            <p><strong>Error:</strong> {error}</p>
            <p className="text-xs mt-2">Project ID: {projectId}</p>
          </div>
        )}

        {/* Project Information */}
        <div className="bg-white rounded-lg border border-gray-200 p-6">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">Project Information</h2>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {/* Project Name */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Project Name</label>
              <p className="text-gray-900 font-medium">{project?.name || 'N/A'}</p>
            </div>

            {/* Created Date */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Created</label>
              <p className="text-gray-900 font-medium">
                {project?.createdAt ? new Date(project.createdAt).toLocaleDateString() : 'N/A'}
              </p>
            </div>

            {/* Repository URL */}
            {project?.repositoryUrl && (
              <div className="md:col-span-2">
                <label className="block text-sm font-medium text-gray-700 mb-2">Repository URL</label>
                <a
                  href={project.repositoryUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-blue-600 hover:text-blue-700 flex items-center gap-2 font-medium"
                >
                  {project.repositoryUrl}
                  <ExternalLink size={16} />
                </a>
              </div>
            )}
          </div>
        </div>

        {/* Project ID for Jenkins */}
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-6">
          <h2 className="text-lg font-semibold text-blue-900 mb-4">🔑 Jenkins Integration</h2>

          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-blue-900 mb-2">Your Project ID</label>
              <p className="text-sm text-blue-800 mb-3">
                Use this ID in your Jenkinsfile to authenticate with SecureOps
              </p>

              <div className="flex gap-2">
                <div className="flex-1">
                  <input
                    type="text"
                    value={project?.id || ''}
                    readOnly
                    className="w-full px-4 py-3 border border-blue-300 rounded-lg bg-white font-mono text-sm text-gray-900"
                  />
                </div>
                <Button onClick={copyProjectId} variant="primary" className="px-4">
                  {copiedId ? <CheckCircle size={20} /> : <Copy size={20} />}
                </Button>
              </div>
              <p className="text-xs text-blue-700 mt-2">
                {copiedId ? '✅ Copied to clipboard!' : 'Click to copy'}
              </p>
            </div>

            <div className="bg-white rounded-lg p-4 space-y-3">
              <h3 className="font-semibold text-blue-900">Example Jenkinsfile:</h3>
              <pre className="bg-gray-50 p-3 rounded text-xs overflow-x-auto text-gray-800">
{`environment {
  PROJECT_ID = "${project?.id || 'YOUR_PROJECT_ID'}"
  SECUREOPS_API_KEY = credentials('secureops-api-key')
}

stage('SecureOps Scan') {
  steps {
    sh '''
      curl -X POST \\
        "http://host.docker.internal:8081/api/projects/\${PROJECT_ID}/scans" \\
        -H "X-API-Key: \${SECUREOPS_API_KEY}" \\
        -H "Content-Type: application/json" \\
        -d '{"environment":"PRODUCTION"}'
    '''
  }
}`}
              </pre>
            </div>

            <div className="bg-white border border-blue-200 rounded-lg p-4">
              <h3 className="font-semibold text-blue-900 mb-2">Setup Steps:</h3>
              <ol className="text-sm text-blue-800 space-y-2 list-decimal list-inside">
                <li>Copy your Project ID above</li>
                <li>
                  Go to <strong>Credentials</strong> page → Generate API Key
                </li>
                <li>
                  Add to Jenkins: <code className="bg-gray-100 px-2 py-1 rounded">secureops-api-key</code>
                </li>
                <li>Use Project ID and API key in your Jenkinsfile</li>
                <li>Push code → Jenkins builds → Results appear in SecureOps</li>
              </ol>
            </div>
          </div>
        </div>

        {/* Quick Links */}
        <div className="flex gap-3">
          <Button variant="secondary" onClick={() => navigate('/credentials')}>
            🔐 Go to Credentials
          </Button>
          <Button variant="secondary" onClick={() => navigate('/projects')}>
            ← Back to Projects
          </Button>
        </div>
      </div>
    </>
  )
}
