import { useQuery, UseQueryResult } from '@tanstack/react-query'
import axios from '@/lib/axios-config'
import { Finding } from '@/types/project'

/**
 * Hook to fetch findings for a specific scan
 * 
 * @param scanId - UUID of the scan
 * @returns Query result with findings data
 */
export function useFindings(scanId: string): UseQueryResult<Finding[]> {
  return useQuery({
    queryKey: ['findings', scanId],
    queryFn: async () => {
      const { data } = await axios.get<Finding[]>(
        `/scans/${scanId}/findings`
      )
      return data
    },
    enabled: !!scanId,
    staleTime: 30000, // 30 seconds
    gcTime: 5 * 60 * 1000, // 5 minutes (formerly cacheTime)
  })
}

/**
 * Hook to fetch a specific finding by ID
 * 
 * @param findingId - UUID of the finding
 * @returns Query result with finding data
 */
export function useFindingById(findingId: string): UseQueryResult<Finding> {
  return useQuery({
    queryKey: ['findings', findingId],
    queryFn: async () => {
      const { data } = await axios.get<Finding>(
        `/findings/${findingId}`
      )
      return data
    },
    enabled: !!findingId,
    staleTime: 30000,
    gcTime: 5 * 60 * 1000,
  })
}
