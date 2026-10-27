/**
 * Pipeline Service
 * Handles all pipeline-related API calls
 */

import axios from '@/lib/axios-config'
import { Pipeline, CreatePipelineRequest } from '@/types/project'

class PipelineService {
  /**
   * Create a new pipeline for a project
   */
  async createPipeline(projectId: string, request: CreatePipelineRequest): Promise<Pipeline> {
    const { data } = await axios.post<Pipeline>(
      `/projects/${projectId}/pipelines`,
      request
    )
    return data
  }

  /**
   * Get all pipelines for a specific project
   */
  async getPipelinesByProject(projectId: string): Promise<Pipeline[]> {
    const { data } = await axios.get<Pipeline[]>(
      `/projects/${projectId}/pipelines`
    )
    return data
  }

  /**
   * Get a specific pipeline by ID
   */
  async getPipelineById(pipelineId: string): Promise<Pipeline> {
    const { data } = await axios.get<Pipeline>(`/pipelines/${pipelineId}`)
    return data
  }
}

export const pipelineService = new PipelineService()
