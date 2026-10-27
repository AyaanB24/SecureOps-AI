/**
 * Finding Service
 * Handles all finding-related API calls
 */

import axios from '@/lib/axios-config'
import { Finding, Severity, FindingStatus } from '@/types/project'

/**
 * Filter criteria for findings
 */
export interface FindingFilterCriteria {
  severity?: Severity[]
  tool?: string[]
  status?: FindingStatus[]
  packageName?: string
  filePath?: string
  searchText?: string
}

class FindingService {
  /**
   * Get all findings for a specific scan
   */
  async getFindingsByScan(scanId: string): Promise<Finding[]> {
    const { data } = await axios.get<Finding[]>(
      `/scans/${scanId}/findings`
    )
    return data
  }

  /**
   * Get a specific finding by ID
   */
  async getFindingById(findingId: string): Promise<Finding> {
    const { data } = await axios.get<Finding>(`/findings/${findingId}`)
    return data
  }

  /**
   * Filter findings based on criteria
   * Filtering happens client-side since backend doesn't expose filtering endpoint
   */
  filterFindings(
    findings: Finding[],
    criteria: FindingFilterCriteria
  ): Finding[] {
    return findings.filter((finding) => {
      // Filter by severity
      if (criteria.severity && criteria.severity.length > 0) {
        if (!criteria.severity.includes(finding.severity)) {
          return false
        }
      }

      // Filter by tool
      if (criteria.tool && criteria.tool.length > 0) {
        if (!criteria.tool.includes(finding.tool)) {
          return false
        }
      }

      // Filter by status
      if (criteria.status && criteria.status.length > 0) {
        if (!criteria.status.includes(finding.status)) {
          return false
        }
      }

      // Filter by package name
      if (criteria.packageName) {
        if (!finding.packageName?.includes(criteria.packageName)) {
          return false
        }
      }

      // Filter by file path
      if (criteria.filePath) {
        if (!finding.filePath?.includes(criteria.filePath)) {
          return false
        }
      }

      // Filter by search text (searches title and description)
      if (criteria.searchText) {
        const searchLower = criteria.searchText.toLowerCase()
        const matchesTitle = finding.title.toLowerCase().includes(searchLower)
        const matchesDescription = finding.description
          .toLowerCase()
          .includes(searchLower)
        const matchesRuleId = finding.ruleId.toLowerCase().includes(searchLower)

        if (!matchesTitle && !matchesDescription && !matchesRuleId) {
          return false
        }
      }

      return true
    })
  }

  /**
   * Sort findings by various criteria
   */
  sortFindings(
    findings: Finding[],
    sortBy: 'severity' | 'createdAt' | 'title' = 'severity'
  ): Finding[] {
    const severityOrder: Record<Severity, number> = {
      CRITICAL: 0,
      HIGH: 1,
      MEDIUM: 2,
      LOW: 3,
    }

    const sorted = [...findings]
    switch (sortBy) {
      case 'severity':
        sorted.sort(
          (a, b) => severityOrder[a.severity] - severityOrder[b.severity]
        )
        break
      case 'createdAt':
        sorted.sort(
          (a, b) =>
            new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
        )
        break
      case 'title':
        sorted.sort((a, b) => a.title.localeCompare(b.title))
        break
    }

    return sorted
  }

  /**
   * Get unique tools from findings
   */
  getUniqueScannerTools(findings: Finding[]): string[] {
    const tools = new Set(findings.map((f) => f.tool))
    return Array.from(tools).sort()
  }

  /**
   * Get count of findings by severity
   */
  countBySeverity(findings: Finding[]): Record<Severity, number> {
    return {
      CRITICAL: findings.filter((f) => f.severity === 'CRITICAL').length,
      HIGH: findings.filter((f) => f.severity === 'HIGH').length,
      MEDIUM: findings.filter((f) => f.severity === 'MEDIUM').length,
      LOW: findings.filter((f) => f.severity === 'LOW').length,
    }
  }
}

export const findingService = new FindingService()
