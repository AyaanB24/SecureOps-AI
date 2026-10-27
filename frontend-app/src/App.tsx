import { BrowserRouter as Router, Routes, Route } from 'react-router-dom'
import { QueryClientProvider, QueryClient, } from '@tanstack/react-query'
import { HelmetProvider } from 'react-helmet-async'
import { ErrorBoundary } from '@/pages/ErrorBoundary'
import { ToastProvider } from '@/contexts/ToastContext'
import MainLayout from '@/layouts/MainLayout'
import Dashboard from '@/pages/Dashboard'
import Projects from '@/pages/Projects'
import Pipelines from '@/pages/Pipelines'
import Scans from '@/pages/Scans'
import Findings from '@/pages/Findings'
import Decisions from '@/pages/Decisions'
import Policies from '@/pages/Policies'
import AIAnalysis from '@/pages/AIAnalysis'
import Settings from '@/pages/Settings'
import NotFound from '@/pages/NotFound'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60 * 5, // 5 minutes
      gcTime: 1000 * 60 * 10, // 10 minutes (formerly cacheTime)
      retry: 1,
    },
  },
})

function App() {
  return (
    <HelmetProvider>
      <QueryClientProvider client={queryClient}>
        <ToastProvider>
          <ErrorBoundary>
            <Router>
              <MainLayout>
                <Routes>
                  <Route path="/" element={<Dashboard />} />
                  <Route path="/projects" element={<Projects />} />
                  <Route path="/pipelines" element={<Pipelines />} />
                  <Route path="/scans" element={<Scans />} />
                  <Route path="/findings" element={<Findings />} />
                  <Route path="/decisions" element={<Decisions />} />
                  <Route path="/policies" element={<Policies />} />
                  <Route path="/ai-analysis" element={<AIAnalysis />} />
                  <Route path="/settings" element={<Settings />} />
                  <Route path="*" element={<NotFound />} />
                </Routes>
              </MainLayout>
            </Router>
          </ErrorBoundary>
        </ToastProvider>
      </QueryClientProvider>
    </HelmetProvider>
  )
}

export default App
