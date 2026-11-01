import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { aiService } from '@/services/aiService'

/**
 * Hook to fetch AI analysis summary for a scan
 */
export function useAiAnalysisSummary(scanId: string | null, enabled = true) {
  return useQuery({
    queryKey: ['aiSummary', scanId],
    queryFn: () => (scanId ? aiService.generateScanSummary(scanId) : Promise.reject('No scanId')),
    enabled: !!scanId && enabled,
    staleTime: 5 * 60 * 1000, // 5 minutes
  })
}

/**
 * Hook to generate/regenerate AI analysis summary
 */
export function useGenerateAiSummary() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ scanId, force }: { scanId: string; force?: boolean }) =>
      aiService.generateScanSummary(scanId, force),
    onSuccess: (data) => {
      queryClient.setQueryData(['aiSummary', data.scanId], data)
    },
  })
}

/**
 * Hook to fetch AI finding explanation/remediation
 */
export function useAiFindingExplanation(findingId: string | null, enabled = true) {
  return useQuery({
    queryKey: ['aiExplanation', findingId],
    queryFn: () => (findingId ? aiService.generateFindingRemediation(findingId) : Promise.reject('No findingId')),
    enabled: !!findingId && enabled,
    staleTime: 5 * 60 * 1000, // 5 minutes
  })
}

/**
 * Hook to generate/regenerate AI finding explanation
 */
export function useGenerateAiExplanation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ findingId, force }: { findingId: string; force?: boolean }) =>
      aiService.generateFindingRemediation(findingId, force),
    onSuccess: (data) => {
      queryClient.setQueryData(['aiExplanation', data.findingId], data)
    },
  })
}
