import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { projectService } from '@/services/projectService'
import { Project, CreateProjectRequest } from '@/types/project'
import { ApiError } from '@/types/common'

export const useProjects = () => {
  return useQuery<Project[], ApiError>({
    queryKey: ['projects'],
    queryFn: () => projectService.getAll(),
  })
}

export const useProject = (id: string) => {
  return useQuery<Project, ApiError>({
    queryKey: ['project', id],
    queryFn: () => projectService.getById(id),
    enabled: !!id,
  })
}

export const useCreateProject = () => {
  const queryClient = useQueryClient()
  return useMutation<Project, ApiError, CreateProjectRequest>({
    mutationFn: (data) => projectService.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['projects'] })
    },
  })
}

export const useProjectByName = (name: string) => {
  return useQuery<Project, ApiError>({
    queryKey: ['project', 'name', name],
    queryFn: () => projectService.findByName(name),
    enabled: !!name,
  })
}
