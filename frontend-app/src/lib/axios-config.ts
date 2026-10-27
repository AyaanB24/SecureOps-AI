import axios, { AxiosError, AxiosInstance } from 'axios'
import { ApiError } from '@/types/common'

const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081/api'

export const apiClient: AxiosInstance = axios.create({
  baseURL,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Request interceptor
apiClient.interceptors.request.use(
  (config) => {
    return config
  },
  (error) => Promise.reject(error)
)

// Response interceptor
apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    const apiError: ApiError = {
      code: error.code || 'UNKNOWN_ERROR',
      message: error.message,
      status: error.response?.status,
    }
    return Promise.reject(apiError)
  }
)

export const handleApiError = (error: unknown): ApiError => {
  if (error instanceof AxiosError) {
    return {
      code: error.code || 'NETWORK_ERROR',
      message: error.message,
      status: error.response?.status,
    }
  }
  if (error instanceof Error) {
    return {
      code: 'ERROR',
      message: error.message,
    }
  }
  return {
    code: 'UNKNOWN_ERROR',
    message: 'An unknown error occurred',
  }
}

export default apiClient

