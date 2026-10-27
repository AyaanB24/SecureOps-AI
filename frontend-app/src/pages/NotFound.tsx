import { Link } from 'react-router-dom'
import { Helmet } from 'react-helmet-async'
import { Button } from '@/components/common/Button'
import { AlertCircle, Home, ArrowLeft } from 'lucide-react'

export default function NotFound() {
  return (
    <>
      <Helmet>
        <title>404 - Page Not Found - SecureOps AI</title>
      </Helmet>

      <div className="min-h-screen bg-gradient-to-br from-slate-50 to-slate-100 flex items-center justify-center p-4">
        <div className="max-w-md text-center">
          <AlertCircle size={64} className="text-slate-400 mx-auto mb-6" />
          
          <h1 className="text-5xl font-bold text-gray-900 mb-2">404</h1>
          <h2 className="text-2xl font-semibold text-gray-800 mb-4">
            Page Not Found
          </h2>
          
          <p className="text-gray-600 mb-8">
            The page you're looking for doesn't exist or may have been moved. Let's get you back
            to where you need to be.
          </p>

          <div className="space-y-3">
            <Link to="/" className="block">
              <Button className="w-full">
                <Home size={18} className="inline mr-2" />
                Go to Dashboard
              </Button>
            </Link>
            
            <button
              onClick={() => window.history.back()}
              className="w-full px-4 py-2 text-gray-700 hover:text-gray-900 font-medium flex items-center justify-center space-x-2 hover:bg-gray-200 rounded-lg transition"
            >
              <ArrowLeft size={18} />
              <span>Go Back</span>
            </button>
          </div>

          <div className="mt-8 pt-8 border-t border-gray-200">
            <p className="text-xs text-gray-600">
              Need help? Check out the <Link to="/" className="text-blue-600 hover:underline">main navigation</Link>
            </p>
          </div>
        </div>
      </div>
    </>
  )
}

