/**
 * Hook for fetching pipelines for a specific project
 */

import { useQuery } from '@tanstack/react-query'
import { pipelineService } from '@/services/pipelineService'
import { Pipeline } from '@/types/project'

export function usePipelinesByProject(projectId: string | null) {
  return useQuery({
    queryKey: ['pipelines', projectId],
    queryFn: () => 
      (projectId ? pipelineService.getPipelinesByProject(projectId) : Promise.resolve([] as Pipeline[])),
    enabled: !!projectId,
    staleTime: 15000,
  })
}
