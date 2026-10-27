import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import React from 'react'
import { ErrorBoundary } from './ErrorBoundary'

// Mock window.location
Object.defineProperty(window, 'location', {
  value: { href: '' },
  writable: true,
})

// Component that throws an error
const ThrowError: React.FC = () => {
  throw new Error('Test error message')
}

// Component that renders normally
const NormalComponent: React.FC = () => {
  return <div>Normal component</div>
}

describe('ErrorBoundary', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('should render children when there is no error', () => {
    render(
      <ErrorBoundary>
        <NormalComponent />
      </ErrorBoundary>
    )

    expect(screen.getByText('Normal component')).toBeInTheDocument()
  })

  it('should display error UI when an error is caught', () => {
    // Suppress console.error for this test
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {})

    render(
      <ErrorBoundary>
        <ThrowError />
      </ErrorBoundary>
    )

    expect(screen.getByText('Oops!')).toBeInTheDocument()
    expect(screen.getByText(/Something went wrong/)).toBeInTheDocument()

    consoleErrorSpy.mockRestore()
  })

  it('should display error message in development mode', () => {
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {})

    render(
      <ErrorBoundary>
        <ThrowError />
      </ErrorBoundary>
    )

    // Error message should be visible in development
    if (process.env.NODE_ENV === 'development') {
      expect(screen.getByText(/Test error message/)).toBeInTheDocument()
    }

    consoleErrorSpy.mockRestore()
  })

  it('should have a home button', () => {
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {})

    render(
      <ErrorBoundary>
        <ThrowError />
      </ErrorBoundary>
    )

    expect(screen.getByText('Go to Home')).toBeInTheDocument()

    consoleErrorSpy.mockRestore()
  })

  it('should navigate to home when button is clicked', async () => {
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {})

    render(
      <ErrorBoundary>
        <ThrowError />
      </ErrorBoundary>
    )

    const button = screen.getByText('Go to Home')
    await userEvent.click(button)

    expect(window.location.href).toBe('/')

    consoleErrorSpy.mockRestore()
  })

  it('should log error to console', () => {
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {})

    render(
      <ErrorBoundary>
        <ThrowError />
      </ErrorBoundary>
    )

    expect(consoleErrorSpy).toHaveBeenCalled()

    consoleErrorSpy.mockRestore()
  })

  it('should have gradient background for error state', () => {
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {})

    const { container } = render(
      <ErrorBoundary>
        <ThrowError />
      </ErrorBoundary>
    )

    const errorContainer = container.querySelector('.min-h-screen')
    expect(errorContainer).toHaveClass('bg-gradient-to-br', 'from-red-50', 'to-orange-50')

    consoleErrorSpy.mockRestore()
  })
})
