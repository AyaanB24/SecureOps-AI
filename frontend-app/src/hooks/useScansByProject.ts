/**
 * Hook for fetching scans for a specific project
 */

import { useQuery } from '@tanstack/react-query'
import { scanService } from '@/services/scanService'
import { Scan } from '@/types/project'

export function useScansByProject(projectId: string | null) {
  return useQuery({
    queryKey: ['scans', projectId],
    queryFn: () => (projectId ? scanService.getScansByProject(projectId) : Promise.resolve([] as Scan[])),
    enabled: !!projectId,
    staleTime: 15000,
  })
}
