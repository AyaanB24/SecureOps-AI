import axios from '@/lib/axios-config'
import { Policy, CreatePolicyRequest, PolicyEnvironment } from '@/types/project'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081/api'

/**
 * Policy Service
 * Handles all policy-related API calls
 */

/**
 * Create a new security policy for a project
 * POST /api/projects/{projectId}/policies
 * 
 * @param projectId - UUID of the project
 * @param request - Policy details (environment, max thresholds)
 * @returns Created policy
 */
export async function createPolicy(
  projectId: string,
  request: CreatePolicyRequest
): Promise<Policy> {
  const response = await axios.post<Policy>(
    `${API_BASE_URL}/projects/${projectId}/policies`,
    request
  )
  return response.data
}

/**
 * Get all policies for a project
 * GET /api/projects/{projectId}/policies
 * 
 * @param projectId - UUID of the project
 * @returns List of policies for the project
 */
export async function getPoliciesByProject(projectId: string): Promise<Policy[]> {
  const response = await axios.get<Policy[]>(
    `${API_BASE_URL}/projects/${projectId}/policies`
  )
  return response.data
}

/**
 * Get a specific policy for a project and environment
 * GET /api/projects/{projectId}/policies/{environment}
 * 
 * @param projectId - UUID of the project
 * @param environment - Policy environment (DEVELOPMENT, STAGING, PRODUCTION)
 * @returns Policy for the specific environment
 */
export async function getPolicyByEnvironment(
  projectId: string,
  environment: PolicyEnvironment
): Promise<Policy> {
  const response = await axios.get<Policy>(
    `${API_BASE_URL}/projects/${projectId}/policies/${environment}`
  )
  return response.data
}

/**
 * Update a security policy
 * PUT /api/projects/{projectId}/policies/{environment}
 * 
 * @param projectId - UUID of the project
 * @param environment - Policy environment to update
 * @param request - Updated policy details
 * @returns Updated policy
 */
export async function updatePolicy(
  projectId: string,
  environment: PolicyEnvironment,
  request: CreatePolicyRequest
): Promise<Policy> {
  const response = await axios.put<Policy>(
    `${API_BASE_URL}/projects/${projectId}/policies/${environment}`,
    request
  )
  return response.data
}
