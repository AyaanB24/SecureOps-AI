import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter } from 'react-router-dom'
import { HelmetProvider } from 'react-helmet-async'
import Policies from './Policies'
import { Policy, PolicyEnvironment } from '@/types/project'

const mockPolicies: Policy[] = [
  {
    id: 'policy-1',
    projectId: 'project-1',
    environment: PolicyEnvironment.PRODUCTION,
    maxCritical: 0,
    maxHigh: 3,
    maxRiskScore: 70,
    createdAt: '2024-10-26T10:00:00Z',
    updatedAt: '2024-10-26T10:00:00Z',
  },
  {
    id: 'policy-2',
    projectId: 'project-1',
    environment: PolicyEnvironment.STAGING,
    maxCritical: 5,
    maxHigh: 10,
    maxRiskScore: 150,
    createdAt: '2024-10-26T10:05:00Z',
    updatedAt: '2024-10-26T10:05:00Z',
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

vi.mock('@/hooks/usePolicies', () => ({
  usePolicies: (projectId: string) => ({
    data: projectId ? mockPolicies : [],
    isLoading: false,
    error: null,
  }),
}))

vi.mock('@/services/policyService', () => ({
  updatePolicy: vi.fn(async () => mockPolicies[0]),
}))

const createQueryClient = () =>
  new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
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

describe('Policies Page', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders page with title', async () => {
    renderWithProviders(<Policies />)
    await waitFor(() => {
      expect(screen.getByText('Security Policies')).toBeInTheDocument()
    })
  })

  it('displays all policies', async () => {
    renderWithProviders(<Policies />)
    await waitFor(() => {
      expect(screen.getByText('PRODUCTION Environment')).toBeInTheDocument()
      expect(screen.getByText('STAGING Environment')).toBeInTheDocument()
    })
  })

  it('shows correct policy thresholds', async () => {
    renderWithProviders(<Policies />)
    await waitFor(() => {
      // PRODUCTION policy: maxCritical=0, maxHigh=3, maxRiskScore=70
      const zeroValues = screen.getAllByText('0')
      expect(zeroValues.length).toBeGreaterThan(0)
      
      const threeValues = screen.getAllByText('3')
      expect(threeValues.length).toBeGreaterThan(0)

      const seventyValues = screen.getAllByText('70')
      expect(seventyValues.length).toBeGreaterThan(0)
    })
  })

  it('displays policy explanation', async () => {
    renderWithProviders(<Policies />)
    await waitFor(() => {
      const explanations = screen.getAllByText(/Zero tolerance if 0/)
      expect(explanations.length).toBeGreaterThan(0)
    })
  })

  it('opens edit dialog when Edit is clicked', async () => {
    renderWithProviders(<Policies />)

    await waitFor(() => {
      const editButtons = screen.getAllByText('Edit')
      expect(editButtons.length).toBeGreaterThan(0)
      fireEvent.click(editButtons[0])
    })

    await waitFor(() => {
      expect(screen.getByText(/Edit Policy/i)).toBeInTheDocument()
    })
  })

  it('validates non-negative threshold values', async () => {
    renderWithProviders(<Policies />)

    await waitFor(() => {
      const editButtons = screen.getAllByText('Edit')
      fireEvent.click(editButtons[0])
    })

    await waitFor(() => {
      expect(screen.getByText(/Edit Policy/i)).toBeInTheDocument()
    })

    // Find input field for max critical and try to enter negative value
    const inputs = screen.getAllByDisplayValue('0')
    const maxCriticalInput = inputs[0] as HTMLInputElement

    fireEvent.change(maxCriticalInput, { target: { value: '-1' } })
    fireEvent.click(screen.getByText(/Save Changes/i))

    await waitFor(() => {
      expect(screen.getByText(/non-negative/i)).toBeInTheDocument()
    })
  })

  it('cancels edit without saving', async () => {
    renderWithProviders(<Policies />)

    await waitFor(() => {
      const editButtons = screen.getAllByText('Edit')
      fireEvent.click(editButtons[0])
    })

    await waitFor(() => {
      expect(screen.getByText(/Edit Policy/i)).toBeInTheDocument()
    })

    const cancelButton = screen.getByText('Cancel')
    fireEvent.click(cancelButton)

    await waitFor(() => {
      expect(screen.queryByText(/Edit Policy/i)).not.toBeInTheDocument()
    })
  })
})
