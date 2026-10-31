import React, { useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '@/contexts/AuthContext'
import { Button } from '@/components/common/Button'
import { Input } from '@/components/common/Input'
import { useToast } from '@/contexts/ToastContext'
import { Github, Chrome } from 'lucide-react'

export function Register() {
  const [email, setEmail] = useState('')
  const [name, setName] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const { register } = useAuth()
  const { error, success } = useToast()

  const validateForm = (): boolean => {
    if (!email || !name || !password || !confirmPassword) {
      error('Please fill in all fields')
      return false
    }

    if (password.length < 8) {
      error('Password must be at least 8 characters long')
      return false
    }

    if (password !== confirmPassword) {
      error('Passwords do not match')
      return false
    }

    if (!email.includes('@')) {
      error('Please enter a valid email address')
      return false
    }

    return true
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()

    if (!validateForm()) {
      return
    }

    setIsLoading(true)

    try {
      await register(email, name, password)
      success('Account created successfully')
    } catch (err) {
      console.error('Registration failed:', err)
      error('Registration failed. Please try again.')
    } finally {
      setIsLoading(false)
    }
  }

  const handleGitHubSignup = () => {
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

  const handleGoogleSignup = () => {
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

        {/* Register Card */}
        <div className="bg-slate-800 rounded-lg shadow-xl p-8 border border-slate-700">
          <h2 className="text-2xl font-bold text-white mb-6">Create Account</h2>

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

            {/* Name Input */}
            <div>
              <label htmlFor="name" className="block text-sm font-medium text-slate-300 mb-2">
                Full Name
              </label>
              <Input
                id="name"
                type="text"
                placeholder="John Doe"
                value={name}
                onChange={(e) => setName(e.target.value)}
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
              <p className="text-xs text-slate-500 mt-1">Minimum 8 characters</p>
            </div>

            {/* Confirm Password Input */}
            <div>
              <label htmlFor="confirmPassword" className="block text-sm font-medium text-slate-300 mb-2">
                Confirm Password
              </label>
              <Input
                id="confirmPassword"
                type="password"
                placeholder="••••••••"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                required
                disabled={isLoading}
                className="w-full"
              />
            </div>

            {/* Register Button */}
            <Button
              type="submit"
              disabled={isLoading || !email || !name || !password || !confirmPassword}
              className="w-full"
            >
              {isLoading ? 'Creating Account...' : 'Create Account'}
            </Button>
          </form>

          {/* OAuth Section */}
          <div className="mt-6">
            <div className="relative">
              <div className="absolute inset-0 flex items-center">
                <div className="w-full border-t border-slate-600"></div>
              </div>
              <div className="relative flex justify-center text-sm">
                <span className="px-2 bg-slate-800 text-slate-400">Or sign up with</span>
              </div>
            </div>

            <div className="mt-4 space-y-3">
              {/* GitHub Signup Button */}
              <Button
                type="button"
                onClick={handleGitHubSignup}
                className="w-full bg-gray-700 hover:bg-gray-600 text-white flex items-center justify-center gap-2"
              >
                <Github size={20} />
                Sign up with GitHub
              </Button>

              {/* Google Signup Button */}
              <Button
                type="button"
                onClick={handleGoogleSignup}
                className="w-full bg-blue-600 hover:bg-blue-700 text-white flex items-center justify-center gap-2"
              >
                <Chrome size={20} />
                Sign up with Google
              </Button>
            </div>
          </div>

          {/* Login Link */}
          <div className="mt-6 text-center">
            <p className="text-slate-400">
              Already have an account?{' '}
              <Link to="/login" className="text-blue-500 hover:text-blue-400 font-medium">
                Login here
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
