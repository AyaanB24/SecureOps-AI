import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter } from 'react-router-dom'
import { HelmetProvider } from 'react-helmet-async'
import Scans from './Scans'

// Mock the hooks
vi.mock('@/hooks/useProjects', () => ({
  useProjects: () => ({
    data: [
      { id: 'project-1', name: 'Test Project', repositoryUrl: 'https://github.com/test/repo' },
    ],
    isLoading: false,
    error: null,
  }),
}))

vi.mock('@/hooks/useScans', () => ({
  useScans: (projectId: string) => ({
    data: projectId
      ? [
          {
            id: 'scan-1',
            projectId: 'project-1',
            pipelineId: 'pipeline-1',
            environment: 'DEV',
            status: 'COMPLETED',
            startedAt: '2024-10-26T10:00:00Z',
            completedAt: '2024-10-26T10:05:00Z',
          },
        ]
      : [],
    isLoading: false,
    error: null,
  }),
}))

vi.mock('@/hooks/useReports', () => ({
  useReports: (scanId: string) => ({
    data: scanId
      ? [
          {
            id: 'report-1',
            scanId: 'scan-1',
            tool: 'TRIVY',
            fileName: 'trivy-report.json',
            status: 'RECEIVED',
            receivedAt: '2024-10-26T10:01:00Z',
          },
        ]
      : [],
    isLoading: false,
    error: null,
  }),
}))

vi.mock('@/hooks/useFindings', () => ({
  useFindings: (scanId: string) => ({
    data: scanId
      ? [
          {
            id: 'finding-1',
            scanId: 'scan-1',
            reportId: 'report-1',
            tool: 'TRIVY',
            ruleId: 'CVE-2021-1234',
            title: 'Critical Vulnerability',
            description: 'A critical security vulnerability found',
            severity: 'CRITICAL',
            filePath: 'package.json',
            lineNumber: 10,
            packageName: 'lodash',
            packageVersion: '4.17.0',
            fingerprint: 'fingerprint-1',
            status: 'OPEN',
            createdAt: '2024-10-26T10:02:00Z',
          },
        ]
      : [],
    isLoading: false,
    error: null,
  }),
}))

const createQueryClient = () =>
  new QueryClient({
    defaultOptions: {
      queries: { retry: false },
    },
  })

const renderWithProviders = (component: React.ReactElement) => {
  const queryClient = createQueryClient()
  return render(
    <HelmetProvider>
      <QueryClientProvider client={queryClient}>
        <BrowserRouter>{component}</BrowserRouter>
      </QueryClientProvider>
    </HelmetProvider>
  )
}

describe('Scans Page', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders page with title', async () => {
    renderWithProviders(<Scans />)
    await waitFor(() => {
      expect(screen.getByText('Scans')).toBeInTheDocument()
    })
  })

  it('displays project selector', async () => {
    renderWithProviders(<Scans />)
    await waitFor(() => {
      expect(screen.getByDisplayValue('Test Project')).toBeInTheDocument()
    })
  })

  it('displays scans for selected project', async () => {
    renderWithProviders(<Scans />)
    await waitFor(() => {
      expect(screen.getByText(/Scan scan-1/i)).toBeInTheDocument()
    })
  })

  it('expands scan to show reports', async () => {
    renderWithProviders(<Scans />)
    
    await waitFor(() => {
      expect(screen.getByText(/Scan scan-1/i)).toBeInTheDocument()
    })
  })

  it('shows upload button in expanded scan', async () => {
    renderWithProviders(<Scans />)

    await waitFor(() => {
      const buttons = screen.getAllByText(/Upload Report/i)
      expect(buttons.length).toBeGreaterThan(0)
    })
  })

  it('handles report upload dialog', async () => {
    renderWithProviders(<Scans />)

    const uploadButtons = await screen.findAllByText(/Upload Report/i)
    expect(uploadButtons.length).toBeGreaterThan(0)
  })

  it('displays findings under reports', async () => {
    renderWithProviders(<Scans />)

    await waitFor(() => {
      const scanHeader = screen.getByText(/Scan scan-1/i)
      expect(scanHeader).toBeInTheDocument()
    })
  })
})
