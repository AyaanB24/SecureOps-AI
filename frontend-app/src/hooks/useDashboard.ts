/**
 * Dashboard Hook
 * Aggregates statistics from multiple API endpoints
 * Uses TanStack Query for efficient caching and refetching
 */

import { useQuery } from '@tanstack/react-query'
import { projectService } from '@/services/projectService'
import { scanService } from '@/services/scanService'
import { findingService } from '@/services/findingService'
import * as decisionServiceModule from '@/services/decisionService'
import { pipelineService } from '@/services/pipelineService'
import { DashboardStats, RecentScan, RecentDecision, SeverityDistribution } from '@/types/dashboard'
import { Severity } from '@/types/project'

const SEVERITY_COLORS: Record<Severity, string> = {
  CRITICAL: '#dc2626', // red-600
  HIGH: '#f97316', // orange-500
  MEDIUM: '#eab308', // yellow-400
  LOW: '#22c55e', // green-500
}

async function fetchDashboardStats(): Promise<DashboardStats> {
  try {
    // Fetch all projects
    const projects = await projectService.getAll()
    const totalProjects = projects.length

    let totalPipelines = 0
    let totalScans = 0
    let totalFindings = 0
    const findingsBySeverity: Record<Lowercase<Severity>, number> = {
      critical: 0,
      high: 0,
      medium: 0,
      low: 0,
    }
    let passedCount = 0
    let blockedCount = 0
    const recentScans: RecentScan[] = []
    const recentDecisions: RecentDecision[] = []

    // Aggregate data from all projects
    for (const project of projects) {
      try {
        // Fetch pipelines
        const pipelines = await pipelineService.getPipelinesByProject(project.id)
        totalPipelines += pipelines.length

        // Fetch scans
        const scans = await scanService.getScansByProject(project.id)
        totalScans += scans.length

        // Add to recent scans (keep last 5 across all projects)
        recentScans.push(
          ...scans.map(scan => ({
            ...scan,
            projectName: project.name,
          }))
        )

        // For each scan, fetch findings and decision
        for (const scan of scans) {
          try {
            // Fetch findings
            const findings = await findingService.getFindingsByScan(scan.id)
            totalFindings += findings.length

            // Count by severity
            findings.forEach(finding => {
              const key = finding.severity.toLowerCase() as Lowercase<Severity>
              findingsBySeverity[key]++
            })

            // Fetch decision
            try {
              const decision = await decisionServiceModule.getDecisionByScan(scan.id)
              if (decision.decision === 'PASS') {
                passedCount++
              } else {
                blockedCount++
              }

              // Add to recent decisions
              const pipeline = pipelines.find(p => p.id === scan.pipelineId)
              recentDecisions.push({
                ...decision,
                projectName: project.name,
                pipelineInfo: pipeline
                  ? {
                      provider: pipeline.provider,
                      jobName: pipeline.jobName,
                    }
                  : undefined,
              })
            } catch (error) {
              // Decision might not exist for a scan yet, skip
              console.debug(`No decision for scan ${scan.id}`)
            }
          } catch (error) {
            // Log but continue - findings or decisions might fail for a scan
            console.debug(`Error fetching data for scan ${scan.id}:`, error)
          }
        }
      } catch (error) {
        // Log but continue - one project shouldn't break the whole dashboard
        console.debug(`Error fetching data for project ${project.id}:`, error)
      }
    }

    // Sort and limit recent items to 5 each
    const sortedRecentScans = recentScans
      .sort((a, b) => new Date(b.startedAt).getTime() - new Date(a.startedAt).getTime())
      .slice(0, 5)

    const sortedRecentDecisions = recentDecisions
      .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
      .slice(0, 5)

    // Build severity distribution chart data
    const severityDistribution: SeverityDistribution[] = [
      { severity: 'CRITICAL', count: findingsBySeverity.critical, color: SEVERITY_COLORS.CRITICAL },
      { severity: 'HIGH', count: findingsBySeverity.high, color: SEVERITY_COLORS.HIGH },
      { severity: 'MEDIUM', count: findingsBySeverity.medium, color: SEVERITY_COLORS.MEDIUM },
      { severity: 'LOW', count: findingsBySeverity.low, color: SEVERITY_COLORS.LOW },
    ]

    return {
      totalProjects,
      totalPipelines,
      totalScans,
      totalFindings,
      findingsBySeverity,
      decisionStats: {
        passed: passedCount,
        blocked: blockedCount,
      },
      recentScans: sortedRecentScans,
      recentDecisions: sortedRecentDecisions,
      severityDistribution,
    }
  } catch (error) {
    console.error('Failed to fetch dashboard stats:', error)
    throw error
  }
}

export function useDashboard() {
  return useQuery({
    queryKey: ['dashboard', 'stats'],
    queryFn: fetchDashboardStats,
    staleTime: 30000, // 30 seconds
    refetchInterval: 60000, // Refetch every minute
    retry: 3,
  })
}
