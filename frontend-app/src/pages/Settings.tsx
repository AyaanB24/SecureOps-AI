/**
 * Settings Page
 * Application settings and connection status
 */

import { useState, useEffect } from 'react'
import { Helmet } from 'react-helmet-async'
import axios from '@/lib/axios-config'
import { PageHeading } from '@/components/common/PageHeading'
import { Button } from '@/components/common/Button'
import { Card } from '@/components/common/Card'
import { AlertCircle, CheckCircle, AlertTriangle, RefreshCw, Copy } from 'lucide-react'

interface ConnectionStatus {
  status: 'connecting' | 'connected' | 'disconnected'
  latency?: number
  error?: string
}

export default function Settings() {
  const [connectionStatus, setConnectionStatus] = useState<ConnectionStatus>({
    status: 'connecting',
  })
  const [copied, setCopied] = useState(false)

  const apiBaseUrl =
    import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081/api'

  // Check connection on mount and periodically
  useEffect(() => {
    checkConnection()
    const interval = setInterval(checkConnection, 30000) // Check every 30 seconds
    return () => clearInterval(interval)
  }, [])

  const checkConnection = async () => {
    setConnectionStatus({ status: 'connecting' })
    const startTime = Date.now()

    try {
      // Use a simple GET request to a known endpoint
      const response = await axios.get('/projects', {
        timeout: 5000,
      })
      const latency = Date.now() - startTime

      if (response.status === 200) {
        setConnectionStatus({
          status: 'connected',
          latency,
        })
      } else {
        setConnectionStatus({
          status: 'disconnected',
          error: `HTTP ${response.status}`,
        })
      }
    } catch (error: any) {
      setConnectionStatus({
        status: 'disconnected',
        error: error.message || 'Connection failed',
      })
    }
  }

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  return (
    <>
      <Helmet>
        <title>Settings - SecureOps AI</title>
      </Helmet>

      <div className="space-y-6 max-w-4xl">
        <PageHeading title="Settings" />

        {/* API Connection Status */}
        <Card>
          <div className="flex items-start justify-between">
            <div>
              <h3 className="text-lg font-semibold text-gray-900 mb-2">
                API Connection
              </h3>
              <p className="text-sm text-gray-600 mb-4">
                Backend service connection status and configuration
              </p>
            </div>
            <Button
              size="sm"
              variant="secondary"
              onClick={checkConnection}
              disabled={connectionStatus.status === 'connecting'}
            >
              <RefreshCw
                size={16}
                className={connectionStatus.status === 'connecting' ? 'animate-spin' : ''}
              />
              Check
            </Button>
          </div>

          <div className="space-y-4">
            {/* Connection Status */}
            <div className="bg-gray-50 p-4 rounded border border-gray-200">
              <div className="flex items-center space-x-3 mb-3">
                {connectionStatus.status === 'connected' ? (
                  <CheckCircle size={20} className="text-green-600" />
                ) : connectionStatus.status === 'disconnected' ? (
                  <AlertCircle size={20} className="text-red-600" />
                ) : (
                  <RefreshCw size={20} className="text-yellow-600 animate-spin" />
                )}
                <span className="font-medium text-gray-900">
                  {connectionStatus.status === 'connected'
                    ? 'Connected'
                    : connectionStatus.status === 'disconnected'
                      ? 'Disconnected'
                      : 'Checking...'}
                </span>
              </div>

              {connectionStatus.latency && (
                <p className="text-sm text-gray-600">
                  Response time: <span className="font-mono">{connectionStatus.latency}ms</span>
                </p>
              )}

              {connectionStatus.error && (
                <p className="text-sm text-red-600">Error: {connectionStatus.error}</p>
              )}
            </div>

            {/* API URL */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                API Base URL
              </label>
              <div className="flex items-center space-x-2">
                <input
                  type="text"
                  value={apiBaseUrl}
                  readOnly
                  className="flex-1 px-3 py-2 bg-gray-100 border border-gray-300 rounded-lg text-sm font-mono text-gray-600"
                />
                <Button
                  size="sm"
                  variant="secondary"
                  onClick={() => copyToClipboard(apiBaseUrl)}
                >
                  {copied ? 'Copied!' : <Copy size={16} />}
                </Button>
              </div>
              <p className="text-xs text-gray-500 mt-1">
                Read-only. Configured via VITE_API_BASE_URL environment variable.
              </p>
            </div>

            {/* Troubleshooting Info */}
            {connectionStatus.status === 'disconnected' && (
              <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
                <h4 className="font-medium text-yellow-900 mb-2 flex items-center space-x-2">
                  <AlertTriangle size={16} />
                  <span>Troubleshooting</span>
                </h4>
                <ul className="text-sm text-yellow-800 space-y-1 list-disc list-inside">
                  <li>Ensure backend is running: <code className="bg-yellow-100 px-1 rounded">./mvnw spring-boot:run</code></li>
                  <li>Check backend is on port 8081</li>
                  <li>Verify database connection is established</li>
                  <li>Check browser console for CORS errors</li>
                </ul>
              </div>
            )}
          </div>
        </Card>

        {/* Application Information */}
        <Card>
          <h3 className="text-lg font-semibold text-gray-900 mb-4">
            Application Information
          </h3>

          <div className="space-y-3">
            <div className="flex justify-between py-3 border-b border-gray-200">
              <span className="text-sm font-medium text-gray-700">Application</span>
              <span className="text-sm text-gray-900">SecureOps AI</span>
            </div>
            <div className="flex justify-between py-3 border-b border-gray-200">
              <span className="text-sm font-medium text-gray-700">Frontend Version</span>
              <span className="text-sm text-gray-900 font-mono">0.1.0</span>
            </div>
            <div className="flex justify-between py-3 border-b border-gray-200">
              <span className="text-sm font-medium text-gray-700">Build Type</span>
              <span className="text-sm text-gray-900">React 18 + TypeScript + Vite</span>
            </div>
            <div className="flex justify-between py-3">
              <span className="text-sm font-medium text-gray-700">Backend Port</span>
              <span className="text-sm text-gray-900 font-mono">8081</span>
            </div>
          </div>
        </Card>

        {/* Data & Privacy */}
        <Card>
          <h3 className="text-lg font-semibold text-gray-900 mb-4">
            Data & Privacy
          </h3>

          <div className="space-y-3 text-sm text-gray-700">
            <p>
              <strong>Data Storage:</strong> All application data is stored in the backend
              PostgreSQL database. No personal data is stored in local browser storage except
              session tokens.
            </p>
            <p>
              <strong>API Communication:</strong> All communication with the backend uses HTTP
              on port 8081. Set up HTTPS in production via reverse proxy.
            </p>
            <p>
              <strong>Session Management:</strong> Session data is managed server-side. Browser
              local storage is cleared on logout.
            </p>
          </div>
        </Card>

        {/* Keyboard Shortcuts */}
        <Card>
          <h3 className="text-lg font-semibold text-gray-900 mb-4">
            Keyboard Shortcuts
          </h3>

          <div className="space-y-2 text-sm">
            <div className="flex justify-between py-2 border-b border-gray-200">
              <span className="text-gray-700">
                <kbd className="px-2 py-1 bg-gray-100 border border-gray-300 rounded text-xs">
                  /
                </kbd>
              </span>
              <span className="text-gray-600">Focus search (if implemented)</span>
            </div>
            <div className="flex justify-between py-2 border-b border-gray-200">
              <span className="text-gray-700">
                <kbd className="px-2 py-1 bg-gray-100 border border-gray-300 rounded text-xs">
                  Esc
                </kbd>
              </span>
              <span className="text-gray-600">Close dialogs</span>
            </div>
            <div className="flex justify-between py-2">
              <span className="text-gray-700">
                <kbd className="px-2 py-1 bg-gray-100 border border-gray-300 rounded text-xs">
                  Tab
                </kbd>
              </span>
              <span className="text-gray-600">Navigate form fields</span>
            </div>
          </div>
        </Card>
      </div>
    </>
  )
}
