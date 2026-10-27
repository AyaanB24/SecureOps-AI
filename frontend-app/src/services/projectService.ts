import { apiClient, handleApiError } from '@/lib/axios-config'
import { Project, CreateProjectRequest } from '@/types/project'

export const projectService = {
  getAll: async (): Promise<Project[]> => {
    try {
      const response = await apiClient.get<Project[]>('/projects')
      return response.data
    } catch (error) {
      throw handleApiError(error)
    }
  },

  create: async (data: CreateProjectRequest): Promise<Project> => {
    try {
      const response = await apiClient.post<Project>('/projects', data)
      return response.data
    } catch (error) {
      throw handleApiError(error)
    }
  },

  getById: async (id: string): Promise<Project> => {
    try {
      const response = await apiClient.get<Project>(`/projects/${id}`)
      return response.data
    } catch (error) {
      throw handleApiError(error)
    }
  },

  findByName: async (name: string): Promise<Project> => {
    try {
      const response = await apiClient.get<Project>('/projects', {
        params: { name },
      })
      return response.data
    } catch (error) {
      throw handleApiError(error)
    }
  },

  delete: async (id: string): Promise<void> => {
    try {
      await apiClient.delete(`/projects/${id}`)
    } catch (error) {
      throw handleApiError(error)
    }
  },
}
