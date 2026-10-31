import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useToast } from '@/contexts/ToastContext'
import { LoadingSpinner } from '@/components/common/LoadingSpinner'
import { apiClient } from '@/lib/axios-config'

export function AuthGoogleCallback() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const { error: errorToast } = useToast()
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const handleCallback = async () => {
      try {
        const code = searchParams.get('code')
        const state = searchParams.get('state')
        const storedState = localStorage.getItem('oauth_state')

        // CSRF protection
        if (state !== storedState) {
          throw new Error('Invalid state parameter - CSRF check failed')
        }

        if (!code) {
          throw new Error('No authorization code received from Google')
        }

        // Send code to backend for token exchange
        const redirectUri = `${window.location.origin}/auth/google/callback`
        const response = await apiClient.post('/auth/google/callback', 
          { code },
          { params: { redirectUri } }
        )

        if (!response.data.token) {
          throw new Error('No token in response')
        }

        const { token, id, email, name, role, enabled } = response.data

        // Store auth data
        localStorage.setItem('auth_token', token)
        localStorage.setItem('auth_user', JSON.stringify({ id, email, name, role, enabled }))

        // Update axios default header
        apiClient.defaults.headers.common['Authorization'] = `Bearer ${token}`

        // Redirect to dashboard
        navigate('/dashboard')
      } catch (err) {
        console.error('Google callback error:', err)
        const errorMsg = err instanceof Error ? err.message : 'Authentication failed'
        setError(errorMsg)
        errorToast(errorMsg)

        // Redirect to login after 3 seconds
        setTimeout(() => navigate('/login'), 3000)
      }
    }

    handleCallback()
  }, [searchParams, navigate, errorToast])

  if (error) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-slate-900 to-slate-800 flex items-center justify-center p-4">
        <div className="bg-slate-800 rounded-lg p-8 border border-slate-700 max-w-md w-full">
          <h1 className="text-2xl font-bold text-white mb-4">Google Authentication Error</h1>
          <p className="text-slate-400 mb-4">{error}</p>
          <p className="text-slate-500">Redirecting to login...</p>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 to-slate-800 flex items-center justify-center p-4">
      <div className="bg-slate-800 rounded-lg p-8 border border-slate-700 text-center">
        <LoadingSpinner />
        <p className="text-white mt-4">Completing Google authentication...</p>
      </div>
    </div>
  )
}
