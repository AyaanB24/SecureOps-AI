import React from 'react'
import { Menu, X } from 'lucide-react'

interface HeaderProps {
  onMenuToggle: () => void
  isSidebarOpen: boolean
}

export const Header: React.FC<HeaderProps> = ({ onMenuToggle, isSidebarOpen }) => {
  return (
    <header className="h-header-height bg-white border-b border-gray-200 flex items-center px-4 md:px-6 gap-4 sticky top-0 z-40">
      <button
        onClick={onMenuToggle}
        className="p-2 hover:bg-gray-100 rounded-lg transition-colors md:hidden"
        aria-label="Toggle menu"
      >
        {isSidebarOpen ? (
          <X className="w-5 h-5 text-gray-700" />
        ) : (
          <Menu className="w-5 h-5 text-gray-700" />
        )}
      </button>

      <div className="flex-1">
        <h1 className="text-xl font-semibold text-gray-900">SecureOps AI</h1>
      </div>

      <div className="flex items-center gap-4">
        <span className="text-sm text-gray-600">v0.1.0</span>
      </div>
    </header>
  )
}
