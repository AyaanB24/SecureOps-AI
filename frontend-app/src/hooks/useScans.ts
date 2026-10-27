import { useQuery, UseQueryResult } from '@tanstack/react-query'
import axios from '@/lib/axios-config'
import { Scan } from '@/types/project'

/**
 * Hook to fetch scans for a specific project
 * 
 * @param projectId - UUID of the project
 * @returns Query result with scans data
 */
export function useScans(projectId: string): UseQueryResult<Scan[]> {
  return useQuery({
    queryKey: ['scans', projectId],
    queryFn: async () => {
      const { data } = await axios.get<Scan[]>(
        `/projects/${projectId}/scans`
      )
      return data
    },
    enabled: !!projectId,
    staleTime: 30000, // 30 seconds
    gcTime: 5 * 60 * 1000, // 5 minutes (formerly cacheTime)
  })
}

/**
 * Hook to fetch a specific scan by ID
 * 
 * @param scanId - UUID of the scan
 * @returns Query result with scan data
 */
export function useScanById(scanId: string): UseQueryResult<Scan> {
  return useQuery({
    queryKey: ['scans', scanId],
    queryFn: async () => {
      const { data } = await axios.get<Scan>(
        `/scans/${scanId}`
      )
      return data
    },
    enabled: !!scanId,
    staleTime: 30000,
    gcTime: 5 * 60 * 1000,
  })
}
