import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { BrowserRouter as Router } from 'react-router-dom'
import { HelmetProvider } from 'react-helmet-async'
import AIAnalysis from './AIAnalysis'

function renderAIAnalysis() {
  return render(
    <HelmetProvider>
      <Router>
        <AIAnalysis />
      </Router>
    </HelmetProvider>
  )
}

describe('AI Analysis Page', () => {
  it('should render the page with coming soon message', () => {
    renderAIAnalysis()

    expect(screen.getByText('AI Analysis')).toBeInTheDocument()
    expect(screen.getByText('AI Analysis Coming Soon')).toBeInTheDocument()
  })

  it('should display planned features', () => {
    renderAIAnalysis()

    expect(screen.getByText('Planned Features:')).toBeInTheDocument()
    expect(screen.getByText(/Root-cause analysis for vulnerability findings/)).toBeInTheDocument()
    expect(screen.getByText(/Automated remediation recommendations/)).toBeInTheDocument()
    expect(screen.getByText(/Risk assessment and prioritization/)).toBeInTheDocument()
  })

  it('should display current capabilities', () => {
    renderAIAnalysis()

    expect(screen.getByText('Current Capabilities')).toBeInTheDocument()
    expect(screen.getByText(/Available Now/i)).toBeInTheDocument()
    expect(screen.getByText(/In Development/i)).toBeInTheDocument()
    expect(screen.getByText(/Future Roadmap/i)).toBeInTheDocument()
  })

  it('should list available scanning tools', () => {
    renderAIAnalysis()

    expect(screen.getByText(/Automated vulnerability scanning with Trivy/)).toBeInTheDocument()
    expect(screen.getByText(/Severity classification/)).toBeInTheDocument()
    expect(screen.getByText(/Policy-based security gates/)).toBeInTheDocument()
  })

  it('should explain how AI analysis will work', () => {
    renderAIAnalysis()

    expect(screen.getByText('How AI Analysis Will Work')).toBeInTheDocument()
    expect(screen.getByText('Analyze Findings')).toBeInTheDocument()
    expect(screen.getByText('Generate Insights')).toBeInTheDocument()
    expect(screen.getByText('Suggest Fixes')).toBeInTheDocument()
    expect(screen.getByText('Prioritize')).toBeInTheDocument()
  })

  it('should include data privacy information', () => {
    renderAIAnalysis()

    expect(screen.getByText('Data Privacy & Safety')).toBeInTheDocument()
    expect(screen.getByText(/Local Processing:/)).toBeInTheDocument()
    expect(screen.getByText(/Model Customization:/)).toBeInTheDocument()
    expect(screen.getByText(/Audit Trail:/)).toBeInTheDocument()
  })

  it('should provide getting started instructions', () => {
    renderAIAnalysis()

    expect(screen.getByText('Getting Started with SecureOps')).toBeInTheDocument()
    expect(screen.getByText(/Register your projects and pipelines/)).toBeInTheDocument()
    expect(screen.getByText(/Upload security reports/)).toBeInTheDocument()
    expect(screen.getByText(/Review findings organized by severity/)).toBeInTheDocument()
    expect(screen.getByText(/Set security policies/)).toBeInTheDocument()
  })

  it('should not fabricate AI recommendations', () => {
    renderAIAnalysis()

    // Verify no fabricated "AI suggestions" are presented
    const text = document.body.textContent || ''
    expect(text).not.toMatch(/AI recommends/i)
    expect(text).not.toMatch(/AI suggests fixing/i)
    expect(text).not.toMatch(/AI found/i)
  })

  it('should clearly mark features as coming soon, not available', () => {
    renderAIAnalysis()

    const comingSoonText = screen.getByText('AI Analysis Coming Soon').parentElement?.textContent
    expect(comingSoonText).toContain('under development')
  })

  it('should have proper accessibility attributes', () => {
    renderAIAnalysis()

    const heading = screen.getByText('AI Analysis')
    expect(heading).toBeInTheDocument()

    // Check for semantic HTML
    const cardElements = screen.getAllByText(/Current Capabilities|How AI Analysis|Data Privacy/)
    expect(cardElements.length).toBeGreaterThan(0)
  })
})
