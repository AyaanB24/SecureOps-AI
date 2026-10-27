import axios from 'axios'
import { Report, ReportProcessingResult } from '../types/project'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081/api'

/**
 * Report Service
 * Handles all report-related API calls
 */

/**
 * Upload a security report for a scan
 * POST /api/scans/{scanId}/reports
 * 
 * @param scanId - UUID of the scan
 * @param tool - Report tool (TRIVY, SEMGREP, OWASP_DEPENDENCY_CHECK)
 * @param file - Report file to upload
 * @returns Report response from backend
 */
export async function uploadReport(
  scanId: string,
  tool: string,
  file: File
): Promise<Report> {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('tool', tool)

  const response = await axios.post<Report>(
    `${API_BASE_URL}/scans/${scanId}/reports`,
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    }
  )

  return response.data
}

/**
 * Get all reports for a specific scan
 * GET /api/scans/{scanId}/reports
 * 
 * @param scanId - UUID of the scan
 * @returns List of reports
 */
export async function getReportsByScan(scanId: string): Promise<Report[]> {
  const response = await axios.get<Report[]>(
    `${API_BASE_URL}/scans/${scanId}/reports`
  )
  return response.data
}

/**
 * Get a specific report by ID
 * GET /api/reports/{reportId}
 * 
 * @param reportId - UUID of the report
 * @returns Report details
 */
export async function getReportById(reportId: string): Promise<Report> {
  const response = await axios.get<Report>(
    `${API_BASE_URL}/reports/${reportId}`
  )
  return response.data
}

/**
 * Process a report and extract findings
 * POST /api/scans/{scanId}/reports/{reportId}/process
 * 
 * This triggers parsing of the report file and creates Finding records.
 * 
 * @param scanId - UUID of the scan
 * @param reportId - UUID of the report to process
 * @returns Processing result with findings count
 */
export async function processReport(
  scanId: string,
  reportId: string
): Promise<ReportProcessingResult> {
  const response = await axios.post<ReportProcessingResult>(
    `${API_BASE_URL}/scans/${scanId}/reports/${reportId}/process`
  )
  return response.data
}
