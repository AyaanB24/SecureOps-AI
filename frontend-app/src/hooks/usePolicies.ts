import { useQuery, UseQueryResult } from '@tanstack/react-query'
import axios from '@/lib/axios-config'
import { Policy, PolicyEnvironment } from '@/types/project'

/**
 * Hook to fetch all policies for a project
 * 
 * @param projectId - UUID of the project
 * @returns Query result with policies
 */
export function usePolicies(projectId: string): UseQueryResult<Policy[]> {
  return useQuery({
    queryKey: ['policies', projectId],
    queryFn: async () => {
      const { data } = await axios.get<Policy[]>(
        `/projects/${projectId}/policies`
      )
      return data
    },
    enabled: !!projectId,
    staleTime: 30000,
    gcTime: 5 * 60 * 1000,
  })
}

/**
 * Hook to fetch a specific policy by environment
 * 
 * @param projectId - UUID of the project
 * @param environment - Policy environment
 * @returns Query result with policy
 */
export function usePolicyByEnvironment(
  projectId: string,
  environment: PolicyEnvironment
): UseQueryResult<Policy> {
  return useQuery({
    queryKey: ['policies', projectId, environment],
    queryFn: async () => {
      const { data } = await axios.get<Policy>(
        `/projects/${projectId}/policies/${environment}`
      )
      return data
    },
    enabled: !!projectId && !!environment,
    staleTime: 30000,
    gcTime: 5 * 60 * 1000,
  })
}
