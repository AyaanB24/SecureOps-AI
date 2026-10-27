/**
 * Scan Service
 * Handles all scan-related API calls
 */

import axios from '@/lib/axios-config'
import { Scan, CreateScanRequest } from '@/types/project'

class ScanService {
  /**
   * Create a new scan for a project
   */
  async createScan(projectId: string, request: CreateScanRequest): Promise<Scan> {
    const { data } = await axios.post<Scan>(
      `/projects/${projectId}/scans`,
      request
    )
    return data
  }

  /**
   * Get all scans for a specific project
   */
  async getScansByProject(projectId: string): Promise<Scan[]> {
    const { data } = await axios.get<Scan[]>(
      `/projects/${projectId}/scans`
    )
    return data
  }

  /**
   * Get a specific scan by ID
   */
  async getScanById(scanId: string): Promise<Scan> {
    const { data } = await axios.get<Scan>(`/scans/${scanId}`)
    return data
  }
}

export const scanService = new ScanService()
