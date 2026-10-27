import { useState } from 'react'
import { Header } from '@/components/common/Header'
import { Sidebar } from '@/components/common/Sidebar'
import { ToastContainer } from '@/components/common/Toast'
import { useToast } from '@/contexts/ToastContext'

interface MainLayoutProps {
  children: React.ReactNode
}

export default function MainLayout({ children }: MainLayoutProps) {
  const [isSidebarOpen, setIsSidebarOpen] = useState(false)
  const { toasts, removeToast } = useToast()

  const handleMenuToggle = () => {
    setIsSidebarOpen(!isSidebarOpen)
  }

  return (
    <div className="flex h-screen bg-gray-50">
      {/* Sidebar */}
      <Sidebar isOpen={isSidebarOpen} />

      {/* Main content */}
      <div className="flex-1 flex flex-col overflow-hidden">
        <Header onMenuToggle={handleMenuToggle} isSidebarOpen={isSidebarOpen} />

        {/* Backdrop for mobile */}
        {isSidebarOpen && (
          <div
            className="fixed inset-0 bg-black bg-opacity-50 md:hidden z-30"
            onClick={() => setIsSidebarOpen(false)}
          />
        )}

        {/* Page content */}
        <main className="flex-1 overflow-y-auto">
          <div className="max-w-7xl mx-auto px-4 md:px-6 lg:px-8 py-8">{children}</div>
        </main>
      </div>

      {/* Toast container */}
      <ToastContainer toasts={toasts} onClose={removeToast} />
    </div>
  )
}

