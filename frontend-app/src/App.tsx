import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom'
import { QueryClientProvider, QueryClient, } from '@tanstack/react-query'
import { HelmetProvider } from 'react-helmet-async'
import { ErrorBoundary } from '@/pages/ErrorBoundary'
import { ToastProvider } from '@/contexts/ToastContext'
import { AuthProvider } from '@/contexts/AuthContext'
import { ProtectedRoute } from '@/components/ProtectedRoute'
import MainLayout from '@/layouts/MainLayout'
import Dashboard from '@/pages/Dashboard'
import Projects from '@/pages/Projects'
import ProjectDetails from '@/pages/ProjectDetails'
import Pipelines from '@/pages/Pipelines'
import Scans from '@/pages/Scans'
import Findings from '@/pages/Findings'
import Decisions from '@/pages/Decisions'
import Policies from '@/pages/Policies'
import AIAnalysis from '@/pages/AIAnalysis'
import Settings from '@/pages/Settings'
import Credentials from '@/pages/Credentials'
import NotFound from '@/pages/NotFound'
import { Login } from '@/pages/Login'
import { Register } from '@/pages/Register'
import { AuthGitHubCallback } from '@/pages/AuthGitHubCallback'
import { AuthGoogleCallback } from '@/pages/AuthGoogleCallback'

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
          <Router>
            <AuthProvider>
              <ErrorBoundary>
                <Routes>
                  {/* Public Routes */}
                  <Route path="/login" element={<Login />} />
                  <Route path="/register" element={<Register />} />
                  
                  {/* OAuth Callback Routes */}
                  <Route path="/auth/github/callback" element={<AuthGitHubCallback />} />
                  <Route path="/auth/google/callback" element={<AuthGoogleCallback />} />

                  {/* Protected Routes */}
                  <Route
                    path="/"
                    element={
                      <ProtectedRoute>
                        <MainLayout>
                          <Dashboard />
                        </MainLayout>
                      </ProtectedRoute>
                    }
                  />
                  <Route
                    path="/dashboard"
                    element={
                      <ProtectedRoute>
                        <MainLayout>
                          <Dashboard />
                        </MainLayout>
                      </ProtectedRoute>
                    }
                  />
                  <Route
                    path="/projects"
                    element={
                      <ProtectedRoute>
                        <MainLayout>
                          <Projects />
                        </MainLayout>
                      </ProtectedRoute>
                    }
                  />
                  <Route
                    path="/projects/:projectId"
                    element={
                      <ProtectedRoute>
                        <MainLayout>
                          <ProjectDetails />
                        </MainLayout>
                      </ProtectedRoute>
                    }
                  />
                  <Route
                    path="/pipelines"
                    element={
                      <ProtectedRoute>
                        <MainLayout>
                          <Pipelines />
                        </MainLayout>
                      </ProtectedRoute>
                    }
                  />
                  <Route
                    path="/scans"
                    element={
                      <ProtectedRoute>
                        <MainLayout>
                          <Scans />
                        </MainLayout>
                      </ProtectedRoute>
                    }
                  />
                  <Route
                    path="/findings"
                    element={
                      <ProtectedRoute>
                        <MainLayout>
                          <Findings />
                        </MainLayout>
                      </ProtectedRoute>
                    }
                  />
                  <Route
                    path="/decisions"
                    element={
                      <ProtectedRoute>
                        <MainLayout>
                          <Decisions />
                        </MainLayout>
                      </ProtectedRoute>
                    }
                  />
                  <Route
                    path="/policies"
                    element={
                      <ProtectedRoute>
                        <MainLayout>
                          <Policies />
                        </MainLayout>
                      </ProtectedRoute>
                    }
                  />
                  <Route
                    path="/ai-analysis"
                    element={
                      <ProtectedRoute>
                        <MainLayout>
                          <AIAnalysis />
                        </MainLayout>
                      </ProtectedRoute>
                    }
                  />
                  <Route
                    path="/settings"
                    element={
                      <ProtectedRoute>
                        <MainLayout>
                          <Settings />
                        </MainLayout>
                      </ProtectedRoute>
                    }
                  />
                  <Route
                    path="/credentials"
                    element={
                      <ProtectedRoute>
                        <MainLayout>
                          <Credentials />
                        </MainLayout>
                      </ProtectedRoute>
                    }
                  />
                  
                  {/* 404 Route */}
                  <Route path="/not-found" element={<NotFound />} />
                  <Route path="*" element={<Navigate to="/not-found" replace />} />
                </Routes>
              </ErrorBoundary>
            </AuthProvider>
          </Router>
        </ToastProvider>
      </QueryClientProvider>
    </HelmetProvider>
  )
}

export default App
