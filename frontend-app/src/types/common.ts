export interface ApiResponse<T> {
  data: T
  status: number
  message?: string
}

export interface ApiError {
  code: string
  message: string
  status?: number
}

export type Environment = 'DEVELOPMENT' | 'STAGING' | 'PRODUCTION'
export type Severity = 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW'
export type ScanStatus = 'CREATED' | 'PROCESSING' | 'COMPLETED' | 'FAILED'
export type ReportStatus = 'RECEIVED' | 'PROCESSING' | 'PROCESSED' | 'FAILED'
export type FindingStatus = 'OPEN' | 'RESOLVED'
export type Decision = 'PASS' | 'BLOCK'
