import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter } from 'react-router-dom'
import { HelmetProvider } from 'react-helmet-async'
import Decisions from './Decisions'
import { Decision, SecurityDecision } from '@/types/project'

const mockDecision: SecurityDecision = {
  scanId: 'scan-1',
  decision: 'PASS' as Decision,
  riskScore: 38,
  criticalCount: 0,
  highCount: 1,
  mediumCount: 2,
  lowCount: 0,
  reason: 'Security policy satisfied',
  createdAt: '2024-10-26T10:30:00Z',
}

const mockBlockDecision: SecurityDecision = {
  scanId: 'scan-2',
  decision: 'BLOCK' as Decision,
  riskScore: 85,
  criticalCount: 1,
  highCount: 2,
  mediumCount: 1,
  lowCount: 0,
  reason: 'Critical findings (1) exceed the configured policy limit (0)',
  createdAt: '2024-10-26T10:35:00Z',
}

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
            environment: 'PRODUCTION',
            status: 'COMPLETED',
            startedAt: '2024-10-26T10:00:00Z',
            completedAt: '2024-10-26T10:05:00Z',
          },
          {
            id: 'scan-2',
            projectId: 'project-1',
            pipelineId: 'pipeline-1',
            environment: 'STAGING',
            status: 'COMPLETED',
            startedAt: '2024-10-26T10:10:00Z',
            completedAt: '2024-10-26T10:15:00Z',
          },
        ]
      : [],
    isLoading: false,
    error: null,
  }),
}))

vi.mock('@/hooks/useDecision', () => ({
  useDecision: (scanId: string) => {
    if (scanId === 'scan-1') {
      return {
        data: mockDecision,
        isLoading: false,
        error: null,
      }
    } else if (scanId === 'scan-2') {
      return {
        data: mockBlockDecision,
        isLoading: false,
        error: null,
      }
    }
    return { data: null, isLoading: false, error: null }
  },
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

describe('Decisions Page', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders page with title', async () => {
    renderWithProviders(<Decisions />)
    await waitFor(() => {
      expect(screen.getByText('Security Decisions')).toBeInTheDocument()
    })
  })

  it('displays PASS decision badge', async () => {
    renderWithProviders(<Decisions />)
    await waitFor(() => {
      expect(screen.getByText(/✓ PASS/i)).toBeInTheDocument()
    })
  })

  it('displays BLOCK decision badge', async () => {
    renderWithProviders(<Decisions />)
    await waitFor(() => {
      const blockBadges = screen.getAllByText(/✗ BLOCK/i)
      expect(blockBadges.length).toBeGreaterThan(0)
    })
  })

  it('displays risk score for each decision', async () => {
    renderWithProviders(<Decisions />)
    await waitFor(() => {
      expect(screen.getByText('38')).toBeInTheDocument() // PASS decision risk score
      expect(screen.getByText('85')).toBeInTheDocument() // BLOCK decision risk score
    })
  })

  it('displays finding counts', async () => {
    renderWithProviders(<Decisions />)
    await waitFor(() => {
      // PASS decision counts
      expect(screen.getAllByText('0').length).toBeGreaterThan(0) // criticalCount
      expect(screen.getAllByText('1').length).toBeGreaterThan(0) // highCount
    })
  })

  it('shows decision reason on expansion', async () => {
    renderWithProviders(<Decisions />)

    await waitFor(() => {
      expect(screen.getByText('Security policy satisfied')).toBeInTheDocument()
    })
  })

  it('allows viewing decision details', async () => {
    renderWithProviders(<Decisions />)

    await waitFor(() => {
      const viewButtons = screen.getAllByText('View Details')
      expect(viewButtons.length).toBeGreaterThan(0)
    })

    const firstViewButton = screen.getAllByText('View Details')[0]
    fireEvent.click(firstViewButton)

    await waitFor(() => {
      expect(screen.getByText('Security Decision Details')).toBeInTheDocument()
    })
  })

  it('displays all severity counts in detail modal', async () => {
    renderWithProviders(<Decisions />)

    await waitFor(() => {
      const viewButtons = screen.getAllByText('View Details')
      fireEvent.click(viewButtons[0])
    })

    await waitFor(() => {
      expect(screen.getByText('Critical')).toBeInTheDocument()
      expect(screen.getByText('High')).toBeInTheDocument()
      expect(screen.getByText('Medium')).toBeInTheDocument()
      expect(screen.getByText('Low')).toBeInTheDocument()
    })
  })
})
