import { useState } from 'react'
import { useGenerateAiSummary, useAiAnalysisSummary } from '@/hooks/useAiAnalysisSummary'
import { Card } from '@/components/common/Card'
import { Button } from '@/components/common/Button'
import { LoadingSpinner } from '@/components/common/LoadingSpinner'
import { AlertCircle, Sparkles, RefreshCw } from 'lucide-react'

interface AiSummaryCardProps {
  scanId: string
}

export function AiSummaryCard({ scanId }: AiSummaryCardProps) {
  const [autoLoadAttempted, setAutoLoadAttempted] = useState(false)
  
  // Try to auto-load existing summary (skip if user explicitly generates new one)
  const {
    data: summary,
    isLoading: isLoadingSummary,
    error: summaryError,
  } = useAiAnalysisSummary(scanId, !autoLoadAttempted)

  // Mutation for generating new summary
  const generateMutation = useGenerateAiSummary()

  const handleGenerateOrRegenerate = async (force = false) => {
    setAutoLoadAttempted(true)
    await generateMutation.mutateAsync({ scanId, force })
  }

  // Determine if we have summary data and what status to show
  const displayedSummary = generateMutation.data || summary
  const isGenerating = generateMutation.isPending || isLoadingSummary
  const hasError = generateMutation.isError || (summaryError && !displayedSummary)
  const error = generateMutation.error || summaryError

  return (
    <Card className="border-2 border-blue-100 bg-gradient-to-br from-blue-50 to-indigo-50">
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center space-x-2">
          <Sparkles size={24} className="text-blue-600" />
          <h3 className="text-lg font-semibold text-gray-900">AI Security Analysis</h3>
        </div>
        {displayedSummary && displayedSummary.generationStatus === 'SUCCESS' && (
          <Button
            onClick={() => handleGenerateOrRegenerate(true)}
            disabled={isGenerating}
            variant="secondary"
            size="sm"
            className="flex items-center space-x-1"
          >
            <RefreshCw size={16} />
            <span>Regenerate</span>
          </Button>
        )}
      </div>

      {isGenerating && (
        <div className="flex items-center justify-center py-8">
          <LoadingSpinner />
          <p className="ml-3 text-gray-600">Analyzing security findings with AI...</p>
        </div>
      )}

      {hasError && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-6">
          <div className="flex items-start space-x-3">
            <AlertCircle size={20} className="text-red-600 flex-shrink-0 mt-0.5" />
            <div className="flex-1">
              <h4 className="font-semibold text-red-900 mb-1">AI Analysis Error</h4>
              <p className="text-sm text-red-800 mb-3">
                {error instanceof Error ? error.message : 'Failed to generate AI analysis. Please try again.'}
              </p>
              <Button
                onClick={() => handleGenerateOrRegenerate(true)}
                disabled={isGenerating}
                variant="secondary"
                size="sm"
              >
                Retry
              </Button>
            </div>
          </div>
        </div>
      )}

      {!displayedSummary && !isGenerating && !hasError && (
        <div className="py-8 text-center">
          <p className="text-gray-600 mb-4">
            Generate an AI-powered security analysis for this scan
          </p>
          <Button
            onClick={() => handleGenerateOrRegenerate()}
            disabled={isGenerating}
            className="bg-blue-600 hover:bg-blue-700 text-white"
          >
            <Sparkles size={18} className="mr-2" />
            Generate AI Analysis
          </Button>
        </div>
      )}

      {displayedSummary && displayedSummary.generationStatus === 'SUCCESS' && (
        <div className="space-y-4">
          {/* Overall Assessment */}
          <div className="bg-white rounded-lg p-4 border border-blue-200">
            <h4 className="font-semibold text-gray-900 mb-2">Overall Assessment</h4>
            <p className="text-gray-700">{displayedSummary.overallAssessment}</p>
          </div>

          {/* Severity Breakdown */}
          <div className="grid grid-cols-4 gap-2">
            <div className="bg-white rounded-lg p-3 border border-red-200 text-center">
              <div className="text-2xl font-bold text-red-600">{displayedSummary.criticalCount}</div>
              <div className="text-xs text-gray-600 font-medium">CRITICAL</div>
            </div>
            <div className="bg-white rounded-lg p-3 border border-orange-200 text-center">
              <div className="text-2xl font-bold text-orange-600">{displayedSummary.highCount}</div>
              <div className="text-xs text-gray-600 font-medium">HIGH</div>
            </div>
            <div className="bg-white rounded-lg p-3 border border-yellow-200 text-center">
              <div className="text-2xl font-bold text-yellow-600">{displayedSummary.mediumCount}</div>
              <div className="text-xs text-gray-600 font-medium">MEDIUM</div>
            </div>
            <div className="bg-white rounded-lg p-3 border border-green-200 text-center">
              <div className="text-2xl font-bold text-green-600">{displayedSummary.lowCount}</div>
              <div className="text-xs text-gray-600 font-medium">LOW</div>
            </div>
          </div>

          {/* Top Three Risks */}
          {displayedSummary.topThreeRisks && (
            <div className="bg-white rounded-lg p-4 border border-blue-200">
              <h4 className="font-semibold text-gray-900 mb-2">Top Security Risks</h4>
              <div className="text-sm text-gray-700 whitespace-pre-wrap">
                {displayedSummary.topThreeRisks}
              </div>
            </div>
          )}

          {/* Key Concerns */}
          {displayedSummary.keyConcerns && (
            <div className="bg-white rounded-lg p-4 border border-blue-200">
              <h4 className="font-semibold text-gray-900 mb-2">Key Concerns</h4>
              <p className="text-sm text-gray-700">{displayedSummary.keyConcerns}</p>
            </div>
          )}

          {/* Recommended Actions */}
          {displayedSummary.recommendedActions && (
            <div className="bg-white rounded-lg p-4 border border-green-200 bg-green-50">
              <h4 className="font-semibold text-gray-900 mb-2">Recommended Actions</h4>
              <div className="text-sm text-gray-700 whitespace-pre-wrap">
                {displayedSummary.recommendedActions}
              </div>
            </div>
          )}

          {/* Significant Concerns Badge */}
          {displayedSummary.hasSignificantConcerns && (
            <div className="flex items-center space-x-2 bg-red-50 border border-red-200 rounded-lg p-3">
              <AlertCircle size={20} className="text-red-600 flex-shrink-0" />
              <div>
                <p className="text-sm font-medium text-red-900">
                  This build has significant security concerns that require attention
                </p>
              </div>
            </div>
          )}

          {/* Metadata */}
          <div className="flex items-center justify-between text-xs text-gray-500 pt-2 border-t border-gray-200">
            <div>Model: {displayedSummary.aiModel || 'Gemini'}</div>
            <div>Generated: {new Date(displayedSummary.createdAt).toLocaleString()}</div>
          </div>
        </div>
      )}

      {displayedSummary && displayedSummary.generationStatus === 'FAILED' && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4">
          <div className="flex items-start space-x-3">
            <AlertCircle size={20} className="text-red-600 flex-shrink-0 mt-0.5" />
            <div>
              <h4 className="font-semibold text-red-900 mb-1">Analysis Failed</h4>
              <p className="text-sm text-red-800 mb-3">
                {displayedSummary.errorMessage || 'An error occurred during AI analysis'}
              </p>
              <Button
                onClick={() => handleGenerateOrRegenerate(true)}
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
    </Card>
  )
}
