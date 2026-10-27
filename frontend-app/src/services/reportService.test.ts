import { describe, it, expect, vi, beforeEach } from 'vitest'
import * as reportService from './reportService'
import axios from 'axios'

vi.mock('axios')

describe('Report Service', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('uploadReport', () => {
    it('uploads a report file with tool parameter', async () => {
      const mockResponse = {
        data: {
          id: 'report-1',
          scanId: 'scan-1',
          tool: 'TRIVY',
          fileName: 'report.json',
          status: 'RECEIVED',
          receivedAt: '2024-10-26T10:00:00Z',
        },
      }

      vi.mocked(axios.post).mockResolvedValueOnce(mockResponse)

      const file = new File(['content'], 'report.json', { type: 'application/json' })
      const result = await reportService.uploadReport('scan-1', 'TRIVY', file)

      expect(result).toEqual(mockResponse.data)
      expect(axios.post).toHaveBeenCalled()
    })

    it('throws error on upload failure', async () => {
      const error = new Error('Upload failed')
      vi.mocked(axios.post).mockRejectedValueOnce(error)

      const file = new File(['content'], 'report.json', { type: 'application/json' })
      await expect(reportService.uploadReport('scan-1', 'TRIVY', file)).rejects.toThrow(
        'Upload failed'
      )
    })
  })

  describe('getReportsByScan', () => {
    it('fetches reports for a scan', async () => {
      const mockResponse = {
        data: [
          {
            id: 'report-1',
            scanId: 'scan-1',
            tool: 'TRIVY',
            fileName: 'report.json',
            status: 'RECEIVED',
            receivedAt: '2024-10-26T10:00:00Z',
          },
        ],
      }

      vi.mocked(axios.get).mockResolvedValueOnce(mockResponse)

      const result = await reportService.getReportsByScan('scan-1')

      expect(result).toEqual(mockResponse.data)
      expect(axios.get).toHaveBeenCalledWith(expect.stringContaining('scan-1/reports'))
    })

    it('returns empty array when no reports found', async () => {
      const mockResponse = { data: [] }
      vi.mocked(axios.get).mockResolvedValueOnce(mockResponse)

      const result = await reportService.getReportsByScan('scan-1')

      expect(result).toEqual([])
    })
  })

  describe('getReportById', () => {
    it('fetches a specific report', async () => {
      const mockResponse = {
        data: {
          id: 'report-1',
          scanId: 'scan-1',
          tool: 'TRIVY',
          fileName: 'report.json',
          status: 'PROCESSED',
          receivedAt: '2024-10-26T10:00:00Z',
        },
      }

      vi.mocked(axios.get).mockResolvedValueOnce(mockResponse)

      const result = await reportService.getReportById('report-1')

      expect(result).toEqual(mockResponse.data)
      expect(axios.get).toHaveBeenCalledWith(expect.stringContaining('report-1'))
    })

    it('throws error when report not found', async () => {
      const error = new Error('Not found')
      vi.mocked(axios.get).mockRejectedValueOnce(error)

      await expect(reportService.getReportById('nonexistent')).rejects.toThrow(
        'Not found'
      )
    })
  })

  describe('processReport', () => {
    it('processes a report and extracts findings', async () => {
      const mockResponse = {
        data: {
          reportId: 'report-1',
          tool: 'TRIVY',
          findingsCreated: 5,
          status: 'PROCESSED',
        },
      }

      vi.mocked(axios.post).mockResolvedValueOnce(mockResponse)

      const result = await reportService.processReport('scan-1', 'report-1')

      expect(result).toEqual(mockResponse.data)
      expect(axios.post).toHaveBeenCalledWith(
        expect.stringContaining('scan-1/reports/report-1/process')
      )
    })

    it('throws error when processing fails', async () => {
      const error = new Error('Processing failed')
      vi.mocked(axios.post).mockRejectedValueOnce(error)

      await expect(reportService.processReport('scan-1', 'report-1')).rejects.toThrow(
        'Processing failed'
      )
    })

    it('returns finding count in processing result', async () => {
      const mockResponse = {
        data: {
          reportId: 'report-1',
          tool: 'TRIVY',
          findingsCreated: 12,
          status: 'PROCESSED',
        },
      }

      vi.mocked(axios.post).mockResolvedValueOnce(mockResponse)

      const result = await reportService.processReport('scan-1', 'report-1')

      expect(result.findingsCreated).toBe(12)
    })
  })
})
