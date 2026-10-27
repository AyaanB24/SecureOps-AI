import { useQuery, UseQueryResult } from '@tanstack/react-query'
import axios from '@/lib/axios-config'
import { Report } from '@/types/project'

/**
 * Hook to fetch reports for a specific scan
 * 
 * @param scanId - UUID of the scan
 * @returns Query result with reports data
 */
export function useReports(scanId: string): UseQueryResult<Report[]> {
  return useQuery({
    queryKey: ['reports', scanId],
    queryFn: async () => {
      const { data } = await axios.get<Report[]>(
        `/scans/${scanId}/reports`
      )
      return data
    },
    enabled: !!scanId,
    staleTime: 30000, // 30 seconds
    gcTime: 5 * 60 * 1000, // 5 minutes (formerly cacheTime)
  })
}

/**
 * Hook to fetch a specific report by ID
 * 
 * @param reportId - UUID of the report
 * @returns Query result with report data
 */
export function useReportById(reportId: string): UseQueryResult<Report> {
  return useQuery({
    queryKey: ['reports', reportId],
    queryFn: async () => {
      const { data } = await axios.get<Report>(
        `/reports/${reportId}`
      )
      return data
    },
    enabled: !!reportId,
    staleTime: 30000,
    gcTime: 5 * 60 * 1000,
  })
}
