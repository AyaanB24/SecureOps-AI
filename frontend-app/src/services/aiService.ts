import { apiClient, handleApiError } from '@/lib/axios-config'

export interface AiAnalysisSummary {
  id: string
  scanId: string
  overallAssessment: string
  criticalCount: number
  highCount: number
  mediumCount: number
  lowCount: number
  totalFindings: number
  topThreeRisks: string
  keyConcerns: string
  recommendedActions: string
  hasSignificantConcerns: boolean
  generationStatus: string
  errorMessage?: string
  aiModel: string
  createdAt: string
  regeneratedAt?: string
}

export interface FindingExplanation {
  id: string
  findingId: string
  whatItMeans: string
  whyDangerous: string
  impactDescription: string
  remediationSuggestion: string
  versionSuggestion?: string
  configurationSuggestion?: string
  generationStatus: string
  errorMessage?: string
  aiModel: string
  createdAt: string
  regeneratedAt?: string
}

export const aiService = {
  /**
   * Generate or retrieve AI summary for a scan
   */
  generateScanSummary: async (scanId: string, force = false): Promise<AiAnalysisSummary> => {
    try {
      const response = await apiClient.post<AiAnalysisSummary>(
        `/ai/scans/${scanId}/summary`,
        {},
        { params: { force } }
      )
      return response.data
    } catch (error) {
      throw handleApiError(error)
    }
  },

  /**
   * Generate or retrieve AI remediation for a finding
   */
  generateFindingRemediation: async (findingId: string, force = false): Promise<FindingExplanation> => {
    try {
      const response = await apiClient.post<FindingExplanation>(
        `/ai/findings/${findingId}/remediation`,
        {},
        { params: { force } }
      )
      return response.data
    } catch (error) {
      throw handleApiError(error)
    }
  },

  /**
   * Get AI configuration status
   */
  getStatus: async (): Promise<{ gemini: string }> => {
    try {
      const response = await apiClient.get('/ai/status')
      return response.data
    } catch (error) {
      throw handleApiError(error)
    }
  },
}
