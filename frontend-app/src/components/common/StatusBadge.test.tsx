/**
 * StatusBadge Component Tests
 */

import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { StatusBadge } from './StatusBadge'

describe('StatusBadge', () => {
  it('should render PASS status with correct styling', () => {
    render(<StatusBadge status="PASS" />)
    const badge = screen.getByText('Pass')
    expect(badge).toHaveClass('bg-green-100', 'text-green-800')
  })

  it('should render BLOCK status with correct styling', () => {
    render(<StatusBadge status="BLOCK" />)
    const badge = screen.getByText('Block')
    expect(badge).toHaveClass('bg-red-100', 'text-red-800')
  })

  it('should render CRITICAL severity with correct styling', () => {
    render(<StatusBadge status="CRITICAL" />)
    const badge = screen.getByText('Critical')
    expect(badge).toHaveClass('bg-red-100', 'text-red-800')
  })

  it('should render HIGH severity with correct styling', () => {
    render(<StatusBadge status="HIGH" />)
    const badge = screen.getByText('High')
    expect(badge).toHaveClass('bg-orange-100', 'text-orange-800')
  })

  it('should render MEDIUM severity with correct styling', () => {
    render(<StatusBadge status="MEDIUM" />)
    const badge = screen.getByText('Medium')
    expect(badge).toHaveClass('bg-amber-100', 'text-amber-800')
  })

  it('should render LOW severity with correct styling', () => {
    render(<StatusBadge status="LOW" />)
    const badge = screen.getByText('Low')
    expect(badge).toHaveClass('bg-green-100', 'text-green-800')
  })

  it('should render PROCESSING status with correct styling', () => {
    render(<StatusBadge status="PROCESSING" />)
    const badge = screen.getByText('Processing')
    expect(badge).toHaveClass('bg-blue-100', 'text-blue-800')
  })

  it('should render custom label when provided', () => {
    render(<StatusBadge status="PASS" label="Custom Label" />)
    expect(screen.getByText('Custom Label')).toBeInTheDocument()
  })
})
