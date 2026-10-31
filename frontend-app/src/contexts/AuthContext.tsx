import { createContext, useContext, useState, useEffect, ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'
import { apiClient } from '@/lib/axios-config'

interface User {
  id: string
  email: string
  name: string
  role: string
  enabled: boolean
}

interface AuthContextType {
  user: User | null
  token: string | null
  isAuthenticated: boolean
  isLoading: boolean
  login: (email: string, password: string) => Promise<void>
  register: (email: string, name: string, password: string) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [token, setToken] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const navigate = useNavigate()

  // Initialize from localStorage on mount
  useEffect(() => {
    const savedToken = localStorage.getItem('auth_token')
    const savedUser = localStorage.getItem('auth_user')

    if (savedToken && savedUser) {
      try {
        setToken(savedToken)
        setUser(JSON.parse(savedUser))
        // Update axios default header
        apiClient.defaults.headers.common['Authorization'] = `Bearer ${savedToken}`
      } catch (error) {
        console.error('Failed to restore auth state:', error)
        localStorage.removeItem('auth_token')
        localStorage.removeItem('auth_user')
      }
    }
    setIsLoading(false)
  }, [])

  const login = async (email: string, password: string) => {
    try {
      setIsLoading(true)
      const response = await apiClient.post('/auth/login', { email, password })
      const { token: newToken, id, name, role, enabled } = response.data

      const userData: User = { id, email, name, role, enabled }
      setUser(userData)
      setToken(newToken)

      // Store in localStorage
      localStorage.setItem('auth_token', newToken)
      localStorage.setItem('auth_user', JSON.stringify(userData))

      // Update axios header
      apiClient.defaults.headers.common['Authorization'] = `Bearer ${newToken}`

      navigate('/dashboard')
    } catch (error) {
      console.error('Login failed:', error)
      throw error
    } finally {
      setIsLoading(false)
    }
  }

  const register = async (email: string, name: string, password: string) => {
    try {
      setIsLoading(true)
      const response = await apiClient.post('/auth/register', { email, name, password })
      const { token: newToken, id, role, enabled } = response.data

      const userData: User = { id, email, name, role, enabled }
      setUser(userData)
      setToken(newToken)

      // Store in localStorage
      localStorage.setItem('auth_token', newToken)
      localStorage.setItem('auth_user', JSON.stringify(userData))

      // Update axios header
      apiClient.defaults.headers.common['Authorization'] = `Bearer ${newToken}`

      navigate('/dashboard')
    } catch (error) {
      console.error('Registration failed:', error)
      throw error
    } finally {
      setIsLoading(false)
    }
  }

  const logout = () => {
    setUser(null)
    setToken(null)
    localStorage.removeItem('auth_token')
    localStorage.removeItem('auth_user')
    delete apiClient.defaults.headers.common['Authorization']
    navigate('/login')
  }

  return (
    <AuthContext.Provider value={{ user, token, isAuthenticated: !!token, isLoading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
