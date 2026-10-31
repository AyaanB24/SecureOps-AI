import React, { useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '@/contexts/AuthContext'
import { Button } from '@/components/common/Button'
import { Input } from '@/components/common/Input'
import { useToast } from '@/contexts/ToastContext'
import { Github, Chrome } from 'lucide-react'

export function Login() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const { login } = useAuth()
  const { success, error } = useToast()

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setIsLoading(true)

    try {
      await login(email, password)
      success('Logged in successfully')
    } catch (err) {
      console.error('Login failed:', err)
      error('Login failed. Please check your credentials.')
    } finally {
      setIsLoading(false)
    }
  }

  const handleGitHubLogin = () => {
    const clientId = import.meta.env.VITE_GITHUB_CLIENT_ID
    const redirectUri = `${window.location.origin}/auth/github/callback`
    const scope = 'user:email'
    const state = Math.random().toString(36).substring(7)
    
    localStorage.setItem('oauth_state', state)
    
    const githubAuthUrl = `https://github.com/login/oauth/authorize?` +
      `client_id=${clientId}&` +
      `redirect_uri=${encodeURIComponent(redirectUri)}&` +
      `scope=${scope}&` +
      `state=${state}`
    
    window.location.href = githubAuthUrl
  }

  const handleGoogleLogin = () => {
    const clientId = import.meta.env.VITE_GOOGLE_CLIENT_ID
    const redirectUri = `${window.location.origin}/auth/google/callback`
    const scope = 'openid profile email'
    const state = Math.random().toString(36).substring(7)
    
    localStorage.setItem('oauth_state', state)
    
    const googleAuthUrl = `https://accounts.google.com/o/oauth2/v2/auth?` +
      `client_id=${clientId}&` +
      `redirect_uri=${encodeURIComponent(redirectUri)}&` +
      `response_type=code&` +
      `scope=${encodeURIComponent(scope)}&` +
      `state=${state}`
    
    window.location.href = googleAuthUrl
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 to-slate-800 flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        {/* Header */}
        <div className="text-center mb-8">
          <h1 className="text-4xl font-bold text-white mb-2">SecureOps AI</h1>
          <p className="text-slate-400">Security Orchestration Platform</p>
        </div>

        {/* Login Card */}
        <div className="bg-slate-800 rounded-lg shadow-xl p-8 border border-slate-700">
          <h2 className="text-2xl font-bold text-white mb-6">Login</h2>

          <form onSubmit={handleSubmit} className="space-y-4">
            {/* Email Input */}
            <div>
              <label htmlFor="email" className="block text-sm font-medium text-slate-300 mb-2">
                Email
              </label>
              <Input
                id="email"
                type="email"
                placeholder="your@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                disabled={isLoading}
                className="w-full"
              />
            </div>

            {/* Password Input */}
            <div>
              <label htmlFor="password" className="block text-sm font-medium text-slate-300 mb-2">
                Password
              </label>
              <Input
                id="password"
                type="password"
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                disabled={isLoading}
                className="w-full"
              />
            </div>

            {/* Login Button */}
            <Button
              type="submit"
              disabled={isLoading || !email || !password}
              className="w-full"
            >
              {isLoading ? 'Logging in...' : 'Login'}
            </Button>
          </form>

          {/* OAuth Section */}
          <div className="mt-6">
            <div className="relative">
              <div className="absolute inset-0 flex items-center">
                <div className="w-full border-t border-slate-600"></div>
              </div>
              <div className="relative flex justify-center text-sm">
                <span className="px-2 bg-slate-800 text-slate-400">Or login with</span>
              </div>
            </div>

            <div className="mt-4 space-y-3">
              {/* GitHub Login Button */}
              <Button
                type="button"
                onClick={handleGitHubLogin}
                className="w-full bg-gray-700 hover:bg-gray-600 text-white flex items-center justify-center gap-2"
              >
                <Github size={20} />
                Sign in with GitHub
              </Button>

              {/* Google Login Button */}
              <Button
                type="button"
                onClick={handleGoogleLogin}
                className="w-full bg-blue-600 hover:bg-blue-700 text-white flex items-center justify-center gap-2"
              >
                <Chrome size={20} />
                Sign in with Google
              </Button>
            </div>
          </div>

          {/* Register Link */}
          <div className="mt-6 text-center">
            <p className="text-slate-400">
              Don't have an account?{' '}
              <Link to="/register" className="text-blue-500 hover:text-blue-400 font-medium">
                Register here
              </Link>
            </p>
          </div>
        </div>

        {/* Footer */}
        <div className="text-center mt-8 text-slate-500 text-sm">
          <p>Secure Container & Application Security Platform</p>
        </div>
      </div>
    </div>
  )
}
