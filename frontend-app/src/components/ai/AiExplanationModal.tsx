import { useState, useEffect } from 'react'
import { useGenerateAiExplanation, useAiFindingExplanation } from '@/hooks/useAiAnalysisSummary'
import { Dialog } from '@/components/common/Dialog'
import { Button } from '@/components/common/Button'
import { LoadingSpinner } from '@/components/common/LoadingSpinner'
import { AlertCircle, RefreshCw, Sparkles, X } from 'lucide-react'

interface AiExplanationModalProps {
  isOpen: boolean
  onClose: () => void
  findingId: string
  findingTitle: string
}

export function AiExplanationModal({
  isOpen,
  onClose,
  findingId,
  findingTitle,
}: AiExplanationModalProps) {
  const [autoLoadAttempted, setAutoLoadAttempted] = useState(false)

  // Try to auto-load existing explanation
  const {
    data: explanation,
    isLoading: isLoadingExplanation,
    error: explanationError,
  } = useAiFindingExplanation(isOpen && findingId ? findingId : null, !autoLoadAttempted)

  // Mutation for generating new explanation
  const generateMutation = useGenerateAiExplanation()

  const handleGenerate = async (force = false) => {
    setAutoLoadAttempted(true)
    await generateMutation.mutateAsync({ findingId, force })
  }

  useEffect(() => {
    if (isOpen) {
      setAutoLoadAttempted(false)
    }
  }, [isOpen])

  const displayedExplanation = generateMutation.data || explanation
  const isGenerating = generateMutation.isPending || isLoadingExplanation
  const hasError = generateMutation.isError || (explanationError && !displayedExplanation)
  const error = generateMutation.error || explanationError

  return (
    <Dialog isOpen={isOpen} onClose={onClose} title="AI Explanation & Remediation" size="lg">
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center space-x-3">
          <Sparkles size={24} className="text-blue-600" />
          <div>
            <h2 className="text-2xl font-bold text-gray-900">AI Explanation & Remediation</h2>
            <p className="text-sm text-gray-600 mt-1">{findingTitle}</p>
          </div>
        </div>
        <button
          onClick={onClose}
          className="text-gray-400 hover:text-gray-600 transition"
        >
          <X size={24} />
        </button>
      </div>

      {isGenerating && (
        <div className="flex flex-col items-center justify-center py-16">
          <LoadingSpinner />
          <p className="mt-4 text-gray-600">Analyzing finding with AI...</p>
        </div>
      )}

      {hasError && !displayedExplanation && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-6">
          <div className="flex items-start space-x-3">
            <AlertCircle size={20} className="text-red-600 flex-shrink-0 mt-0.5" />
            <div className="flex-1">
              <h3 className="font-semibold text-red-900 mb-1">Analysis Error</h3>
              <p className="text-sm text-red-800 mb-4">
                {error instanceof Error ? error.message : 'Failed to generate explanation'}
              </p>
              <Button
                onClick={() => handleGenerate(true)}
                disabled={isGenerating}
                size="sm"
                className="bg-red-600 hover:bg-red-700 text-white"
              >
                Retry
              </Button>
            </div>
          </div>
        </div>
      )}

      {!displayedExplanation && !isGenerating && !hasError && (
        <div className="text-center py-12">
          <p className="text-gray-600 mb-4">
            Generate an AI-powered explanation and remediation for this finding
          </p>
          <Button
            onClick={() => handleGenerate()}
            disabled={isGenerating}
            className="bg-blue-600 hover:bg-blue-700 text-white"
          >
            <Sparkles size={18} className="mr-2" />
            Generate Explanation
          </Button>
        </div>
      )}

      {displayedExplanation && displayedExplanation.generationStatus === 'SUCCESS' && (
        <div className="space-y-6">
          {/* Regenerate Button */}
          <div className="flex justify-end">
            <Button
              onClick={() => handleGenerate(true)}
              disabled={isGenerating}
              variant="secondary"
              size="sm"
              className="flex items-center space-x-1"
            >
              <RefreshCw size={16} />
              <span>Regenerate</span>
            </Button>
          </div>

          {/* What It Means */}
          {displayedExplanation.whatItMeans && (
            <div className="bg-blue-50 border-l-4 border-blue-500 rounded-r-lg p-4">
              <h3 className="font-semibold text-gray-900 mb-2">What This Means</h3>
              <p className="text-sm text-gray-700">{displayedExplanation.whatItMeans}</p>
            </div>
          )}

          {/* Why It's Dangerous */}
          {displayedExplanation.whyDangerous && (
            <div className="bg-orange-50 border-l-4 border-orange-500 rounded-r-lg p-4">
              <h3 className="font-semibold text-gray-900 mb-2">Why It's Dangerous</h3>
              <p className="text-sm text-gray-700">{displayedExplanation.whyDangerous}</p>
            </div>
          )}

          {/* Impact */}
          {displayedExplanation.impactDescription && (
            <div className="bg-red-50 border-l-4 border-red-500 rounded-r-lg p-4">
              <h3 className="font-semibold text-gray-900 mb-2">Potential Impact</h3>
              <p className="text-sm text-gray-700">{displayedExplanation.impactDescription}</p>
            </div>
          )}

          {/* Remediation */}
          {displayedExplanation.remediationSuggestion && (
            <div className="bg-green-50 border-l-4 border-green-500 rounded-r-lg p-4">
              <h3 className="font-semibold text-gray-900 mb-2">Recommended Fix</h3>
              <div className="text-sm text-gray-700 whitespace-pre-wrap max-h-48 overflow-y-auto">
                {displayedExplanation.remediationSuggestion}
              </div>
            </div>
          )}

          {/* Version Suggestion */}
          {displayedExplanation.versionSuggestion && (
            <div className="bg-purple-50 border-l-4 border-purple-500 rounded-r-lg p-4">
              <h3 className="font-semibold text-gray-900 mb-2">Recommended Version</h3>
              <p className="text-sm font-mono bg-white rounded px-2 py-1 inline-block">
                {displayedExplanation.versionSuggestion}
              </p>
            </div>
          )}

          {/* Configuration Suggestion */}
          {displayedExplanation.configurationSuggestion && (
            <div className="bg-indigo-50 border-l-4 border-indigo-500 rounded-r-lg p-4">
              <h3 className="font-semibold text-gray-900 mb-2">Configuration Changes</h3>
              <div className="text-sm text-gray-700 whitespace-pre-wrap bg-white rounded px-2 py-1 max-h-48 overflow-y-auto">
                <code>{displayedExplanation.configurationSuggestion}</code>
              </div>
            </div>
          )}

          {/* Metadata */}
          <div className="flex items-center justify-between text-xs text-gray-500 pt-4 border-t border-gray-200">
            <div>Model: {displayedExplanation.aiModel || 'Gemini'}</div>
            <div>Generated: {new Date(displayedExplanation.createdAt).toLocaleString()}</div>
          </div>
        </div>
      )}

      {displayedExplanation && displayedExplanation.generationStatus === 'FAILED' && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4">
          <div className="flex items-start space-x-3">
            <AlertCircle size={20} className="text-red-600 flex-shrink-0 mt-0.5" />
            <div>
              <h4 className="font-semibold text-red-900 mb-1">Analysis Failed</h4>
              <p className="text-sm text-red-800 mb-3">
                {displayedExplanation.errorMessage || 'An error occurred during AI analysis'}
              </p>
              <Button
                onClick={() => handleGenerate(true)}
                disabled={isGenerating}
                size="sm"
                className="bg-red-600 hover:bg-red-700 text-white"
              >
                Retry
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* Close Button */}
      <div className="flex justify-end gap-2 mt-6 pt-6 border-t border-gray-200">
        <Button onClick={onClose} variant="secondary">
          Close
        </Button>
      </div>
    </Dialog>
  )
}
