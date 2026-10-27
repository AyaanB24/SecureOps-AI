import React from 'react'
import { Link, useLocation } from 'react-router-dom'
import { LayoutDashboard, Package, GitBranch, Shield, Search, CheckCircle, Lock, Sparkles, Settings } from 'lucide-react'
import clsx from 'clsx'

interface SidebarProps {
  isOpen: boolean
}

interface NavItem {
  label: string
  path: string
  icon: React.ReactNode
}

const navItems: NavItem[] = [
  { label: 'Dashboard', path: '/', icon: <LayoutDashboard className="w-5 h-5" /> },
  { label: 'Projects', path: '/projects', icon: <Package className="w-5 h-5" /> },
  { label: 'Pipelines', path: '/pipelines', icon: <GitBranch className="w-5 h-5" /> },
  { label: 'Scans', path: '/scans', icon: <Shield className="w-5 h-5" /> },
  { label: 'Findings', path: '/findings', icon: <Search className="w-5 h-5" /> },
  { label: 'Decisions', path: '/decisions', icon: <CheckCircle className="w-5 h-5" /> },
  { label: 'Policies', path: '/policies', icon: <Lock className="w-5 h-5" /> },
  { label: 'AI Analysis', path: '/ai-analysis', icon: <Sparkles className="w-5 h-5" /> },
  { label: 'Settings', path: '/settings', icon: <Settings className="w-5 h-5" /> },
]

export const Sidebar: React.FC<SidebarProps> = ({ isOpen }) => {
  const location = useLocation()

  return (
    <aside
      className={clsx(
        'fixed md:sticky top-header-height left-0 w-sidebar-width h-[calc(100vh-var(--header-height))]',
        'bg-white border-r border-gray-200 transition-transform duration-200 ease-in-out',
        'md:translate-x-0 md:block overflow-y-auto',
        !isOpen && '-translate-x-full md:translate-x-0'
      )}
    >
      <nav className="p-4 space-y-2">
        {navItems.map((item) => {
          const isActive = location.pathname === item.path
          return (
            <Link
              key={item.path}
              to={item.path}
              className={clsx(
                'flex items-center gap-3 px-4 py-3 rounded-lg font-medium transition-colors',
                isActive
                  ? 'bg-gray-100 text-gray-900 border-l-4 border-gray-900'
                  : 'text-gray-700 hover:bg-gray-50'
              )}
            >
              {item.icon}
              <span>{item.label}</span>
            </Link>
          )
        })}
      </nav>
    </aside>
  )
}

