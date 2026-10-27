/**
 * Dashboard-specific types for statistics and visualizations
 */

import { Scan, SecurityDecision, Severity } from './project'

export interface DashboardStats {
  totalProjects: number
  totalPipelines: number
  totalScans: number
  totalFindings: number
  findingsBySeverity: {
    critical: number
    high: number
    medium: number
    low: number
  }
  decisionStats: {
    passed: number
    blocked: number
  }
  recentScans: RecentScan[]
  recentDecisions: RecentDecision[]
  severityDistribution: SeverityDistribution[]
}

export interface RecentScan extends Scan {
  projectName?: string
}

export interface RecentDecision extends SecurityDecision {
  projectName?: string
  pipelineInfo?: {
    provider: string
    jobName: string
  }
}

export interface SeverityDistribution {
  severity: Severity
  count: number
  color: string
}

export interface DashboardError {
  message: string
  code?: string
  timestamp: string
}
