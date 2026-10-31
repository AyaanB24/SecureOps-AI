/**
 * Credentials Page
 * Manage SecureOps API credentials for Jenkins integration
 */

import { useState, useEffect } from 'react'
import { Helmet } from 'react-helmet-async'
import { PageHeading } from '@/components/common/PageHeading'
import { Button } from '@/components/common/Button'
import { Dialog } from '@/components/common/Dialog'
import { LoadingSpinner } from '@/components/common/LoadingSpinner'
import { Copy, Key, AlertCircle, CheckCircle, Eye, EyeOff } from 'lucide-react'
import { apiClient } from '@/lib/axios-config'

export default function Credentials() {
  const [credentials, setCredentials] = useState<any[]>([])
  const [isLoading, setIsLoading] = useState(false)
  const [showGenerateDialog, setShowGenerateDialog] = useState(false)
  const [showApiKeyDialog, setShowApiKeyDialog] = useState(false)
  const [generatedApiKey, setGeneratedApiKey] = useState<string | null>(null)
  const [showApiKey, setShowApiKey] = useState(false)
  const [credentialName, setCredentialName] = useState('jenkins')
  const [isGenerating, setIsGenerating] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [copied, setCopied] = useState(false)

  // Load credentials on mount
  useEffect(() => {
    loadCredentials()
  }, [])

  const loadCredentials = async () => {
    setIsLoading(true)
    try {
      const response = await apiClient.get('/admin/service-accounts')
      // Ensure we always have an array
      const data = Array.isArray(response.data) ? response.data : []
      setCredentials(data)
    } catch (err: any) {
      console.error('Failed to load credentials:', err)
      setCredentials([])
    } finally {
      setIsLoading(false)
    }
  }

  const handleGenerateKey = async () => {
    if (!credentialName.trim()) {
      setError('Credential name is required')
      return
    }

    setIsGenerating(true)
    setError(null)

    try {
      const response = await apiClient.post('/admin/service-accounts', {
        name: credentialName.trim(),
        description: `Jenkins API credential for ${credentialName}`,
        scope: 'JENKINS_CI'
      })

      // Show the API key (only shown once!)
      setGeneratedApiKey(response.data.apiKey)
      setShowGenerateDialog(false)
      setShowApiKeyDialog(true)

      // Reload credentials list
      loadCredentials()
    } catch (err: any) {
      if (err.status === 409) {
        setError('A credential with this name already exists')
      } else {
        setError(err.message || 'Failed to generate credential')
      }
    } finally {
      setIsGenerating(false)
    }
  }

  const copyToClipboard = async () => {
    if (generatedApiKey) {
      await navigator.clipboard.writeText(generatedApiKey)
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    }
  }

  const handleDeleteCredential = async (credentialId: string, credentialName: string) => {
    if (!window.confirm(`Are you sure you want to permanently delete the "${credentialName}" credential? This cannot be undone.`)) {
      return
    }

    try {
      await apiClient.delete(`/admin/service-accounts/${credentialId}`)
      // Reload credentials list
      loadCredentials()
    } catch (err: any) {
      console.error('Failed to delete credential:', err)
      alert('Failed to delete credential')
    }
  }

  const handleCloseApiKeyDialog = () => {
    setShowApiKeyDialog(false)
    setGeneratedApiKey(null)
    setShowApiKey(false)
    setCredentialName('jenkins')
  }

  return (
    <>
      <Helmet>
        <title>API Credentials - SecureOps AI</title>
      </Helmet>

      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <PageHeading title="API Credentials" />
          <Button onClick={() => setShowGenerateDialog(true)}>
            <Key size={20} className="inline mr-2" />
            Generate New Credential
          </Button>
        </div>

        <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 text-sm text-blue-800">
          <p className="flex items-start space-x-3">
            <AlertCircle size={20} className="flex-shrink-0 mt-0.5" />
            <span>
              API credentials are used to authenticate Jenkins with SecureOps. Each credential is shown
              <strong> only once</strong> after generation. Save it securely in your Jenkins credentials manager.
            </span>
          </p>
        </div>

        {isLoading ? (
          <LoadingSpinner />
        ) : credentials.length === 0 ? (
          <div className="bg-white p-12 rounded-lg border border-gray-200 text-center">
            <p className="text-gray-600 mb-4">No API credentials generated yet</p>
            <Button onClick={() => setShowGenerateDialog(true)} variant="primary">
              Generate Your First Credential
            </Button>
          </div>
        ) : (
          <div className="bg-white rounded-lg border border-gray-200 overflow-hidden">
            <table className="w-full">
              <thead>
                <tr className="border-b border-gray-200 bg-gray-50">
                  <th className="px-6 py-4 text-left text-sm font-semibold text-gray-700">Name</th>
                  <th className="px-6 py-4 text-left text-sm font-semibold text-gray-700">Scope</th>
                  <th className="px-6 py-4 text-left text-sm font-semibold text-gray-700">Created</th>
                  <th className="px-6 py-4 text-left text-sm font-semibold text-gray-700">Last Used</th>
                  <th className="px-6 py-4 text-left text-sm font-semibold text-gray-700">Status</th>
                  <th className="px-6 py-4 text-left text-sm font-semibold text-gray-700">Actions</th>
                </tr>
              </thead>
              <tbody>
                {credentials.map((cred) => (
                  <tr key={cred.id} className="border-b border-gray-100 hover:bg-gray-50 transition-colors">
                    <td className="px-6 py-4">
                      <p className="font-medium text-gray-900">{cred.name}</p>
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-600">{cred.scope}</td>
                    <td className="px-6 py-4 text-sm text-gray-600">
                      {new Date(cred.createdAt).toLocaleDateString()}
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-600">
                      {cred.lastUsedAt ? new Date(cred.lastUsedAt).toLocaleDateString() : 'Never'}
                    </td>
                    <td className="px-6 py-4">
                      <span
                        className={`inline-block px-3 py-1 rounded-full text-xs font-semibold ${
                          cred.enabled
                            ? 'bg-green-100 text-green-800'
                            : 'bg-red-100 text-red-800'
                        }`}
                      >
                        {cred.enabled ? 'Active' : 'Disabled'}
                      </span>
                    </td>
                    <td className="px-6 py-4">
                      <button
                        onClick={() => handleDeleteCredential(cred.id, cred.name)}
                        className="text-sm text-red-600 hover:text-red-800 hover:underline"
                      >
                        Delete
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Generate Credential Dialog */}
      <Dialog
        isOpen={showGenerateDialog}
        onClose={() => {
          setShowGenerateDialog(false)
          setError(null)
          setCredentialName('jenkins')
        }}
        title="Generate New API Credential"
      >
        <div className="space-y-4">
          <p className="text-sm text-gray-600">
            Create a new API credential for Jenkins integration. You'll see the API key only once after generation.
          </p>

          {error && (
            <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm">
              {error}
            </div>
          )}

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Credential Name
            </label>
            <input
              type="text"
              value={credentialName}
              onChange={(e) => setCredentialName(e.target.value)}
              placeholder="e.g., jenkins, github-actions"
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              disabled={isGenerating}
            />
            <p className="text-xs text-gray-500 mt-2">
              Choose a descriptive name to identify this credential (e.g., the CI/CD tool it's for)
            </p>
          </div>

          <div className="flex justify-end space-x-3 pt-4">
            <Button
              variant="secondary"
              onClick={() => setShowGenerateDialog(false)}
              disabled={isGenerating}
            >
              Cancel
            </Button>
            <Button onClick={handleGenerateKey} disabled={isGenerating}>
              {isGenerating ? 'Generating...' : 'Generate Credential'}
            </Button>
          </div>
        </div>
      </Dialog>

      {/* API Key Display Dialog */}
      <Dialog
        isOpen={showApiKeyDialog}
        onClose={handleCloseApiKeyDialog}
        title="⚠️ Save Your API Key"
      >
        <div className="space-y-4">
          <div className="bg-amber-50 border border-amber-200 rounded-lg p-4">
            <p className="text-sm text-amber-900 flex items-start space-x-3">
              <AlertCircle size={20} className="flex-shrink-0 mt-0.5" />
              <span>
                <strong>This is your only chance to see this key!</strong> Copy and save it securely in your Jenkins
                credentials manager. It will not be shown again.
              </span>
            </p>
          </div>

          {generatedApiKey && (
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Your API Key
              </label>
              <div className="flex space-x-2">
                <div className="flex-1 relative">
                  <input
                    type={showApiKey ? 'text' : 'password'}
                    value={generatedApiKey}
                    readOnly
                    className="w-full px-4 py-3 border border-gray-300 rounded-lg bg-gray-50 font-mono text-sm"
                  />
                  <button
                    onClick={() => setShowApiKey(!showApiKey)}
                    className="absolute right-3 top-3 text-gray-500 hover:text-gray-700"
                  >
                    {showApiKey ? <EyeOff size={18} /> : <Eye size={18} />}
                  </button>
                </div>
                <Button
                  onClick={copyToClipboard}
                  variant="secondary"
                  className="px-4"
                  title="Copy to clipboard"
                >
                  {copied ? <CheckCircle size={20} /> : <Copy size={20} />}
                </Button>
              </div>
              <p className="text-xs text-gray-500 mt-2">
                {copied ? '✅ Copied to clipboard!' : 'Click to copy to clipboard'}
              </p>
            </div>
          )}

          <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
            <p className="text-sm font-medium text-blue-900 mb-2">Next Steps:</p>
            <ol className="text-sm text-blue-800 space-y-2">
              <li>1. Copy the API key above</li>
              <li>2. Go to Jenkins → Manage Credentials → System → Global credentials</li>
              <li>3. Add Credentials → Secret text</li>
              <li>4. Paste the key as "Secret"</li>
              <li>5. Set ID to: <code className="bg-white px-2 py-1 rounded">secureops-api-key</code></li>
              <li>6. Use in Jenkinsfile: <code className="bg-white px-2 py-1 rounded">credentials('secureops-api-key')</code></li>
            </ol>
          </div>

          <div className="flex justify-end pt-4">
            <Button onClick={handleCloseApiKeyDialog} variant="primary">
              I've Saved the Key
            </Button>
          </div>
        </div>
      </Dialog>
    </>
  )
}
