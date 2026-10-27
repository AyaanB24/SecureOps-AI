import axios from '@/lib/axios-config'
import { SecurityDecision } from '@/types/project'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081/api'

/**
 * Decision Service
 * Handles all security decision-related API calls
 */

/**
 * Generate/Get security decision for a scan
 * GET /api/scans/{scanId}/decision
 * 
 * This endpoint:
 * 1. Loads the scan with its project and environment
 * 2. Loads the project policy for that environment
 * 3. Counts findings by severity
 * 4. Evaluates against policy thresholds
 * 5. Returns deterministic PASS/BLOCK verdict with reasoning
 * 
 * @param scanId - UUID of the scan
 * @returns Security decision with verdict, reasoning, and metrics
 */
export async function getDecisionByScan(scanId: string): Promise<SecurityDecision> {
  const response = await axios.get<SecurityDecision>(
    `${API_BASE_URL}/scans/${scanId}/decision`
  )
  return response.data
}
