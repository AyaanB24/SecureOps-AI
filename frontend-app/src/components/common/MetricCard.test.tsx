/**
 * MetricCard Component Tests
 */

import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MetricCard } from './MetricCard'

describe('MetricCard', () => {
  it('should render label and value', () => {
    render(
      <MetricCard label="Total Projects" value="42" />
    )
    expect(screen.getByText('Total Projects')).toBeInTheDocument()
    expect(screen.getByText('42')).toBeInTheDocument()
  })

  it('should display loading spinner when isLoading is true', () => {
    const { container } = render(
      <MetricCard label="Total Projects" value="42" isLoading={true} />
    )
    // Spinner should be in the DOM - check for LoadingSpinner component
    const spinner = container.querySelector('[class*="animate-spin"]') || container.querySelector('svg')
    expect(spinner).toBeTruthy()
  })

  it('should display error message when error is provided', () => {
    render(
      <MetricCard
        label="Total Projects"
        value="42"
        error={new Error('Test error')}
      />
    )
    expect(screen.getByText(/Error loading metric/i)).toBeInTheDocument()
  })

  it('should apply pass variant styling', () => {
    render(
      <MetricCard label="Passed" value="10" variant="pass" />
    )
    const valueElement = screen.getByText('10')
    expect(valueElement).toHaveClass('text-green-600')
  })

  it('should apply block variant styling', () => {
    render(
      <MetricCard label="Blocked" value="5" variant="block" />
    )
    const valueElement = screen.getByText('5')
    expect(valueElement).toHaveClass('text-red-600')
  })

  it('should apply default variant styling when not specified', () => {
    render(<MetricCard label="Metric" value="100" />)
    const valueElement = screen.getByText('100')
    expect(valueElement).toHaveClass('text-gray-900')
  })

  it('should handle numeric and string values', () => {
    const { rerender } = render(
      <MetricCard label="Count" value={0} />
    )
    expect(screen.getByText('0')).toBeInTheDocument()

    rerender(
      <MetricCard label="Count" value="500" />
    )
    expect(screen.getByText('500')).toBeInTheDocument()
  })
})
