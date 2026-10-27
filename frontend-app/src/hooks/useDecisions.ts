import { useQuery, UseQueryResult } from '@tanstack/react-query'
import axios from '@/lib/axios-config'
import { SecurityDecision } from '@/types/project'

/**
 * Hook to fetch security decision for a scan
 * 
 * @param scanId - UUID of the scan
 * @returns Query result with decision data
 */
export function useDecision(scanId: string): UseQueryResult<SecurityDecision> {
  return useQuery({
    queryKey: ['decisions', scanId],
    queryFn: async () => {
      const { data } = await axios.get<SecurityDecision>(
        `/scans/${scanId}/decision`
      )
      return data
    },
    enabled: !!scanId,
    staleTime: 60000, // 1 minute
    gcTime: 10 * 60 * 1000, // 10 minutes
  })
}
