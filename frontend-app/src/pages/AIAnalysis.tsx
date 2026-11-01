/**
 * AI Analysis Page
 * AI-powered analysis, recommendations, and insights
 */

import { Helmet } from 'react-helmet-async'
import { PageHeading } from '@/components/common/PageHeading'
import { Card } from '@/components/common/Card'
import { AlertCircle, Sparkles, Clock } from 'lucide-react'

export default function AIAnalysis() {
  return (
    <>
      <Helmet>
        <title>AI Analysis - SecureOps AI</title>
      </Helmet>

      <div className="space-y-6 max-w-4xl">
        <PageHeading title="AI Analysis" />

        {/* Live Banner */}
        <div className="bg-gradient-to-r from-green-50 to-emerald-50 border border-green-200 rounded-lg p-6 flex items-start space-x-4">
          <Sparkles size={32} className="text-green-600 flex-shrink-0 mt-0.5" />
          <div>
            <h2 className="text-xl font-semibold text-green-900 mb-2">
              ✓ AI Analysis is Live
            </h2>
            <p className="text-green-800 mb-4">
              AI-powered analysis is now available for your security findings. Get instant
              root-cause analysis, remediation recommendations, and automated insights powered
              by Google Gemini AI.
            </p>
            <div className="space-y-2 text-sm text-green-800">
              <div className="flex items-center space-x-2">
                <span className="font-medium">Live Features:</span>
              </div>
              <ul className="list-disc list-inside space-y-1 ml-2">
                <li>AI root-cause analysis for each finding</li>
                <li>Smart remediation recommendations with version suggestions</li>
                <li>Scan-level security assessment and summary</li>
                <li>Security best practices and configuration suggestions</li>
                <li>Intelligent caching for cost optimization</li>
              </ul>
            </div>
          </div>
        </div>

        {/* Current Capabilities */}
        <Card>
          <h3 className="text-lg font-semibold text-gray-900 mb-4">
            Current Capabilities
          </h3>

          <div className="space-y-4">
            <div className="bg-green-50 border-l-4 border-green-500 p-4 rounded">
              <h4 className="font-medium text-green-900 mb-2">✓ Available Now</h4>
              <ul className="text-sm text-green-800 space-y-1 list-disc list-inside">
                <li>Automated vulnerability scanning with Trivy, Semgrep, OWASP Dependency Check</li>
                <li>Severity classification (CRITICAL, HIGH, MEDIUM, LOW)</li>
                <li>Policy-based security gates (PASS/BLOCK decisions)</li>
                <li>Finding aggregation and deduplication</li>
                <li>Risk scoring based on finding characteristics</li>
                <li>Dashboard with security metrics</li>
              </ul>
            </div>

            <div className="bg-yellow-50 border-l-4 border-yellow-500 p-4 rounded">
              <h4 className="font-medium text-yellow-900 mb-2 flex items-center space-x-2">
                <Clock size={16} />
                <span>Setup Required</span>
              </h4>
              <ul className="text-sm text-yellow-800 space-y-1 list-disc list-inside">
                <li>Set <code className="bg-yellow-100 px-2 py-1 rounded">GEMINI_API_KEY</code> environment variable on backend</li>
                <li>Restart backend service after setting API key</li>
                <li>AI features activate automatically once backend is running</li>
                <li>See "Setup Instructions" section below for details</li>
              </ul>
            </div>

            <div className="bg-blue-50 border-l-4 border-blue-500 p-4 rounded">
              <h4 className="font-medium text-blue-900 mb-2 flex items-center space-x-2">
                <Sparkles size={16} />
                <span>Future Roadmap</span>
              </h4>
              <ul className="text-sm text-blue-800 space-y-1 list-disc list-inside">
                <li>ML-powered false positive filtering</li>
                <li>Attack surface analysis</li>
                <li>Compliance mapping (OWASP, CWE, etc.)</li>
                <li>Multi-repository analysis</li>
              </ul>
            </div>
          </div>
        </Card>

        {/* How It Will Work */}
        <Card>
          <h3 className="text-lg font-semibold text-gray-900 mb-4">
            How AI Analysis Will Work
          </h3>

          <div className="space-y-4">
            <div className="flex items-start space-x-4">
              <div className="flex-shrink-0 w-8 h-8 bg-blue-100 rounded-full flex items-center justify-center text-sm font-semibold text-blue-700">
                1
              </div>
              <div>
                <h4 className="font-medium text-gray-900">Analyze Findings</h4>
                <p className="text-sm text-gray-600 mt-1">
                  AI will examine each finding including code context, severity, and attack vectors
                </p>
              </div>
            </div>

            <div className="flex items-start space-x-4">
              <div className="flex-shrink-0 w-8 h-8 bg-blue-100 rounded-full flex items-center justify-center text-sm font-semibold text-blue-700">
                2
              </div>
              <div>
                <h4 className="font-medium text-gray-900">Generate Insights</h4>
                <p className="text-sm text-gray-600 mt-1">
                  AI will provide root-cause analysis explaining why the vulnerability exists
                </p>
              </div>
            </div>

            <div className="flex items-start space-x-4">
              <div className="flex-shrink-0 w-8 h-8 bg-blue-100 rounded-full flex items-center justify-center text-sm font-semibold text-blue-700">
                3
              </div>
              <div>
                <h4 className="font-medium text-gray-900">Suggest Fixes</h4>
                <p className="text-sm text-gray-600 mt-1">
                  AI will recommend specific code changes and security best practices
                </p>
              </div>
            </div>

            <div className="flex items-start space-x-4">
              <div className="flex-shrink-0 w-8 h-8 bg-blue-100 rounded-full flex items-center justify-center text-sm font-semibold text-blue-700">
                4
              </div>
              <div>
                <h4 className="font-medium text-gray-900">Prioritize</h4>
                <p className="text-sm text-gray-600 mt-1">
                  AI will rank recommendations by impact and effort to fix
                </p>
              </div>
            </div>
          </div>
        </Card>

        {/* Setup Instructions */}
        <Card>
          <h3 className="text-lg font-semibold text-gray-900 mb-4">
            Setup Instructions
          </h3>

          <div className="space-y-4 text-sm text-gray-700">
            <div className="bg-blue-50 border border-blue-200 p-4 rounded">
              <h4 className="font-semibold text-blue-900 mb-2">1. Get Your Gemini API Key</h4>
              <p className="mb-2">
                Visit <a href="https://aistudio.google.com/app/apikeys" target="_blank" rel="noopener noreferrer" className="text-blue-600 hover:underline">Google AI Studio</a> to get a free API key.
              </p>
            </div>

            <div className="bg-blue-50 border border-blue-200 p-4 rounded">
              <h4 className="font-semibold text-blue-900 mb-2">2. Set Environment Variable</h4>
              <p className="mb-2">On your backend server, set:</p>
              <code className="block bg-gray-800 text-green-400 p-3 rounded font-mono text-xs mb-2 overflow-x-auto">
                export GEMINI_API_KEY=your-api-key-here
              </code>
              <p className="text-xs text-gray-600">Or on Windows (PowerShell):</p>
              <code className="block bg-gray-800 text-green-400 p-3 rounded font-mono text-xs overflow-x-auto">
                $env:GEMINI_API_KEY='your-api-key-here'
              </code>
            </div>

            <div className="bg-blue-50 border border-blue-200 p-4 rounded">
              <h4 className="font-semibold text-blue-900 mb-2">3. Start Backend</h4>
              <p className="mb-2">Run the backend with:</p>
              <code className="block bg-gray-800 text-green-400 p-3 rounded font-mono text-xs overflow-x-auto">
                java -jar target/secureops-0.0.1-SNAPSHOT.jar
              </code>
              <p className="text-xs text-gray-600 mt-2">Or use Maven:</p>
              <code className="block bg-gray-800 text-green-400 p-3 rounded font-mono text-xs overflow-x-auto">
                mvn spring-boot:run
              </code>
            </div>

            <div className="bg-green-50 border border-green-200 p-4 rounded">
              <h4 className="font-semibold text-green-900 mb-2">✓ Ready to Use</h4>
              <p>
                Once the backend is running, AI features are available:
              </p>
              <ul className="list-disc list-inside mt-2 space-y-1 ml-2">
                <li>Click <strong>"AI Explain & Fix"</strong> button on any finding in Findings page</li>
                <li>View AI analysis in the Scans page expanded view</li>
              </ul>
            </div>
          </div>
        </Card>

        {/* Data Privacy & Safety */}
        <Card>
          <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center space-x-2">
            <AlertCircle size={20} className="text-blue-600" />
            <span>Data Privacy & Safety</span>
          </h3>

          <div className="space-y-3 text-sm text-gray-700">
            <p>
              <strong>AI Service:</strong> Uses Google Gemini API for analysis. Only finding
              metadata and vulnerability data is sent (no source code sent to external services).
            </p>
            <p>
              <strong>Caching:</strong> Analysis results are cached in your database to minimize
              API calls and reduce costs.
            </p>
            <p>
              <strong>Audit Trail:</strong> All AI recommendations are logged with timestamps
              for compliance and audit purposes.
            </p>
            <p>
              <strong>Manual Review:</strong> AI suggestions should always be reviewed by
              security experts before implementation.
            </p>
          </div>
        </Card>

        {/* Getting Started */}
        <Card>
          <h3 className="text-lg font-semibold text-gray-900 mb-4">
            Getting Started with SecureOps
          </h3>

          <p className="text-sm text-gray-700 mb-4">
            While AI analysis is being developed, you can start using SecureOps to:
          </p>

          <ol className="space-y-2 text-sm text-gray-700 list-decimal list-inside">
            <li>Register your projects and pipelines</li>
            <li>Upload security reports from tools like Trivy, Semgrep, and OWASP Dependency Check</li>
            <li>Review findings organized by severity and location</li>
            <li>Set security policies with pass/fail thresholds</li>
            <li>Make automated security decisions for deployments</li>
          </ol>

          <p className="text-xs text-gray-600 mt-4">
            See Dashboard, Findings, and Policies pages to get started.
          </p>
        </Card>
      </div>
    </>
  )
}
