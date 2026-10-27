export interface Project {
  id: string
  name: string
  repositoryUrl?: string
  createdAt: string
}

export interface CreateProjectRequest {
  name: string
  repositoryUrl?: string
}

export type ProjectResponse = Project

// Scan types
export type ScanEnvironment = 'DEV' | 'STAGING' | 'PRODUCTION'
export type ScanStatus = 'CREATED' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED'

export interface Scan {
  id: string
  projectId: string
  pipelineId: string
  environment: ScanEnvironment
  status: ScanStatus
  startedAt: string
  completedAt: string | null
}

export interface CreateScanRequest {
  pipelineId: string
  environment: ScanEnvironment
}

// Pipeline types
export type PipelineProvider =
  | 'JENKINS'
  | 'GITHUB_ACTIONS'
  | 'GITLAB_CI'
  | 'AZURE_PIPELINES'
  | 'CIRCLECI'
  | 'TRAVIS_CI'

export interface Pipeline {
  id: string
  projectId: string
  provider: PipelineProvider
  jobName: string
  buildNumber: number
  branch: string
  commitSha?: string
  createdAt: string
}

export interface CreatePipelineRequest {
  provider: PipelineProvider
  jobName: string
  buildNumber: number
  branch: string
  commitSha?: string
}

// Finding types
export type Severity = 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW'
export type FindingStatus = 'OPEN' | 'RESOLVED'

export interface Finding {
  id: string
  scanId: string
  reportId: string
  tool: string
  ruleId: string
  title: string
  description: string
  severity: Severity
  filePath: string
  lineNumber: number | null
  packageName: string | null
  packageVersion: string | null
  fingerprint: string
  status: FindingStatus
  createdAt: string
}

// Policy types
export enum PolicyEnvironment {
  DEVELOPMENT = 'DEVELOPMENT',
  STAGING = 'STAGING',
  PRODUCTION = 'PRODUCTION',
}

export interface Policy {
  id: string
  projectId: string
  environment: PolicyEnvironment
  maxCritical: number
  maxHigh: number
  maxRiskScore: number
  createdAt: string
  updatedAt: string
}

export interface CreatePolicyRequest {
  environment: PolicyEnvironment
  maxCritical: number
  maxHigh: number
  maxRiskScore: number
}

// Security Decision types
export enum Decision {
  PASS = 'PASS',
  BLOCK = 'BLOCK',
}

export interface SecurityDecision {
  scanId: string
  decision: Decision
  riskScore: number
  criticalCount: number
  highCount: number
  mediumCount: number
  lowCount: number
  reason: string
  createdAt: string
}

// Report types
export type ReportTool = 'TRIVY' | 'SEMGREP' | 'OWASP_DEPENDENCY_CHECK'
export type ReportStatus = 'RECEIVED' | 'PROCESSING' | 'PROCESSED' | 'FAILED'

export interface Report {
  id: string
  scanId: string
  tool: ReportTool
  fileName: string
  status: ReportStatus
  receivedAt: string
}

export interface ReportProcessingResult {
  reportId: string
  tool: ReportTool
  findingsCreated: number
  status: ReportStatus
}
