import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter } from 'react-router-dom'
import { HelmetProvider } from 'react-helmet-async'
import Findings from './Findings'
import { Finding } from '@/types/project'

// Mock data
const mockFindings: Finding[] = [
  {
    id: 'finding-1',
    scanId: 'scan-1',
    reportId: 'report-1',
    tool: 'TRIVY',
    ruleId: 'CVE-2021-1234',
    title: 'Critical Vulnerability in lodash',
    description: 'A critical security vulnerability found in lodash package',
    severity: 'CRITICAL',
    filePath: 'package.json',
    lineNumber: 10,
    packageName: 'lodash',
    packageVersion: '4.17.0',
    fingerprint: 'fingerprint-1',
    status: 'OPEN',
    createdAt: '2024-10-26T10:02:00Z',
  },
  {
    id: 'finding-2',
    scanId: 'scan-1',
    reportId: 'report-1',
    tool: 'TRIVY',
    ruleId: 'CVE-2021-5678',
    title: 'High Severity Issue',
    description: 'A high severity issue in dependencies',
    severity: 'HIGH',
    filePath: 'src/utils.ts',
    lineNumber: 45,
    packageName: 'express',
    packageVersion: '4.18.0',
    fingerprint: 'fingerprint-2',
    status: 'OPEN',
    createdAt: '2024-10-26T10:03:00Z',
  },
  {
    id: 'finding-3',
    scanId: 'scan-1',
    reportId: 'report-1',
    tool: 'TRIVY',
    ruleId: 'CVE-2021-9999',
    title: 'Medium Severity Issue',
    description: 'A medium severity issue',
    severity: 'MEDIUM',
    filePath: 'config.json',
    lineNumber: null,
    packageName: null,
    packageVersion: null,
    fingerprint: 'fingerprint-3',
    status: 'RESOLVED',
    createdAt: '2024-10-26T10:04:00Z',
  },
]

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

vi.mock('@/hooks/useFindings', () => ({
  useFindings: (scanId: string) => ({
    data: scanId ? mockFindings : [],
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

describe('Findings Page', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders page with title', async () => {
    renderWithProviders(<Findings />)
    await waitFor(() => {
      expect(screen.getByText('Findings')).toBeInTheDocument()
    })
  })

  it('displays project selector', async () => {
    renderWithProviders(<Findings />)
    await waitFor(() => {
      expect(screen.getByDisplayValue('Test Project')).toBeInTheDocument()
    })
  })

  it('displays scan selector', async () => {
    renderWithProviders(<Findings />)
    await waitFor(() => {
      expect(screen.getByText('Select Scan')).toBeInTheDocument()
    })
  })

  it('prompts to select scan when none selected', async () => {
    renderWithProviders(<Findings />)
    await waitFor(() => {
      expect(screen.getByText('Select a scan to view findings')).toBeInTheDocument()
    })
  })

  it('displays findings after scan selection', async () => {
    renderWithProviders(<Findings />)

    await waitFor(() => {
      const scanSelect = screen.getByDisplayValue('')
      expect(scanSelect).toBeInTheDocument()
    })

    const scanSelect = screen.getByDisplayValue('') as HTMLSelectElement
    fireEvent.change(scanSelect, { target: { value: 'scan-1' } })

    await waitFor(() => {
      expect(screen.getByText('Critical Vulnerability in lodash')).toBeInTheDocument()
    })
  })

  it('filters findings by severity', async () => {
    renderWithProviders(<Findings />)

    // Select scan
    const scanSelect = screen.getByDisplayValue('') as HTMLSelectElement
    fireEvent.change(scanSelect, { target: { value: 'scan-1' } })

    await waitFor(() => {
      expect(screen.getByText('Critical Vulnerability in lodash')).toBeInTheDocument()
    })

    // Click CRITICAL severity filter
    const criticalButton = screen.getByText('CRITICAL')
    fireEvent.click(criticalButton)

    await waitFor(() => {
      expect(screen.getByText('Critical Vulnerability in lodash')).toBeInTheDocument()
      expect(screen.queryByText('High Severity Issue')).not.toBeInTheDocument()
    })
  })

  it('filters findings by status', async () => {
    renderWithProviders(<Findings />)

    const scanSelect = screen.getByDisplayValue('') as HTMLSelectElement
    fireEvent.change(scanSelect, { target: { value: 'scan-1' } })

    await waitFor(() => {
      expect(screen.getByText('Critical Vulnerability in lodash')).toBeInTheDocument()
    })

    // Click RESOLVED status filter
    const resolvedButtons = screen.getAllByText('RESOLVED')
    fireEvent.click(resolvedButtons[0])

    await waitFor(() => {
      expect(screen.getByText('Medium Severity Issue')).toBeInTheDocument()
      expect(screen.queryByText('Critical Vulnerability in lodash')).not.toBeInTheDocument()
    })
  })

  it('filters findings by search text', async () => {
    renderWithProviders(<Findings />)

    const scanSelect = screen.getByDisplayValue('') as HTMLSelectElement
    fireEvent.change(scanSelect, { target: { value: 'scan-1' } })

    await waitFor(() => {
      expect(screen.getByText('Critical Vulnerability in lodash')).toBeInTheDocument()
    })

    const searchInput = screen.getByPlaceholderText('Search findings...')
    fireEvent.change(searchInput, { target: { value: 'High Severity' } })

    await waitFor(() => {
      expect(screen.getByText('High Severity Issue')).toBeInTheDocument()
      expect(screen.queryByText('Critical Vulnerability in lodash')).not.toBeInTheDocument()
    })
  })

  it('displays finding details in modal', async () => {
    renderWithProviders(<Findings />)

    const scanSelect = screen.getByDisplayValue('') as HTMLSelectElement
    fireEvent.change(scanSelect, { target: { value: 'scan-1' } })

    await waitFor(() => {
      expect(screen.getByText('Critical Vulnerability in lodash')).toBeInTheDocument()
    })

    const findingCard = screen.getByText('Critical Vulnerability in lodash')
    fireEvent.click(findingCard)

    await waitFor(() => {
      expect(screen.getByText('Finding Details')).toBeInTheDocument()
      expect(screen.getByText('Severity')).toBeInTheDocument()
      expect(screen.getByText('CRITICAL')).toBeInTheDocument()
    })
  })

  it('shows severity distribution counts', async () => {
    renderWithProviders(<Findings />)

    const scanSelect = screen.getByDisplayValue('') as HTMLSelectElement
    fireEvent.change(scanSelect, { target: { value: 'scan-1' } })

    await waitFor(() => {
      expect(screen.getByText('Critical Vulnerability in lodash')).toBeInTheDocument()
    })

    // Check severity counts are displayed
    const criticalButtons = screen.getAllByText(/CRITICAL/)
    expect(criticalButtons.length).toBeGreaterThan(0)
  })

  it('resets filters', async () => {
    renderWithProviders(<Findings />)

    const scanSelect = screen.getByDisplayValue('') as HTMLSelectElement
    fireEvent.change(scanSelect, { target: { value: 'scan-1' } })

    await waitFor(() => {
      expect(screen.getByText('Critical Vulnerability in lodash')).toBeInTheDocument()
    })

    // Apply a filter
    const criticalButton = screen.getByText('CRITICAL')
    fireEvent.click(criticalButton)

    await waitFor(() => {
      expect(screen.getByText('Reset Filters')).toBeInTheDocument()
    })

    // Reset filters
    const resetButton = screen.getByText('Reset Filters')
    fireEvent.click(resetButton)

    await waitFor(() => {
      expect(screen.getByText('Critical Vulnerability in lodash')).toBeInTheDocument()
      expect(screen.getByText('High Severity Issue')).toBeInTheDocument()
    })
  })

  it('handles package name filter', async () => {
    renderWithProviders(<Findings />)

    const scanSelect = screen.getByDisplayValue('') as HTMLSelectElement
    fireEvent.change(scanSelect, { target: { value: 'scan-1' } })

    await waitFor(() => {
      expect(screen.getByText('Critical Vulnerability in lodash')).toBeInTheDocument()
    })

    const packageInput = screen.getByPlaceholderText('Filter by package...')
    fireEvent.change(packageInput, { target: { value: 'express' } })

    await waitFor(() => {
      expect(screen.getByText('High Severity Issue')).toBeInTheDocument()
      expect(screen.queryByText('Critical Vulnerability in lodash')).not.toBeInTheDocument()
    })
  })
})
