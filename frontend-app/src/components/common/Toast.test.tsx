import { describe, it, expect, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ToastItem, ToastContainer } from './Toast'
import type { Toast } from './Toast'

describe('Toast Component', () => {
  describe('ToastItem', () => {
    it('should render a success toast', () => {
      const mockOnClose = vi.fn()
      const toast: Toast = {
        id: '1',
        type: 'success',
        message: 'Operation successful',
      }

      render(<ToastItem {...toast} onClose={mockOnClose} />)

      expect(screen.getByText('Operation successful')).toBeInTheDocument()
    })

    it('should render an error toast', () => {
      const mockOnClose = vi.fn()
      const toast: Toast = {
        id: '1',
        type: 'error',
        message: 'An error occurred',
      }

      render(<ToastItem {...toast} onClose={mockOnClose} />)

      expect(screen.getByText('An error occurred')).toBeInTheDocument()
    })

    it('should render a warning toast', () => {
      const mockOnClose = vi.fn()
      const toast: Toast = {
        id: '1',
        type: 'warning',
        message: 'Warning message',
      }

      render(<ToastItem {...toast} onClose={mockOnClose} />)

      expect(screen.getByText('Warning message')).toBeInTheDocument()
    })

    it('should render an info toast', () => {
      const mockOnClose = vi.fn()
      const toast: Toast = {
        id: '1',
        type: 'info',
        message: 'Information',
      }

      render(<ToastItem {...toast} onClose={mockOnClose} />)

      expect(screen.getByText('Information')).toBeInTheDocument()
    })

    it('should call onClose when close button is clicked', async () => {
      const mockOnClose = vi.fn()
      const toast: Toast = {
        id: '1',
        type: 'success',
        message: 'Test message',
      }

      render(<ToastItem {...toast} onClose={mockOnClose} />)

      const closeButton = screen.getByLabelText('Close notification')
      await userEvent.click(closeButton)

      expect(mockOnClose).toHaveBeenCalledWith('1')
    })

    it('should auto-dismiss after duration', async () => {
      const mockOnClose = vi.fn()
      const toast: Toast = {
        id: '1',
        type: 'success',
        message: 'Test message',
        duration: 100, // 100ms for testing
      }

      render(<ToastItem {...toast} onClose={mockOnClose} />)

      await waitFor(() => {
        expect(mockOnClose).toHaveBeenCalledWith('1')
      })
    })

    it('should not dismiss if duration is 0', async () => {
      const mockOnClose = vi.fn()
      const toast: Toast = {
        id: '1',
        type: 'success',
        message: 'Test message',
        duration: 0,
      }

      render(<ToastItem {...toast} onClose={mockOnClose} />)

      // Wait a bit to ensure it doesn't dismiss
      await waitFor(() => {
        expect(mockOnClose).not.toHaveBeenCalled()
      }, { timeout: 500 })
    })

    it('should have slideIn animation class', () => {
      const mockOnClose = vi.fn()
      const toast: Toast = {
        id: '1',
        type: 'success',
        message: 'Test message',
      }

      const { container } = render(<ToastItem {...toast} onClose={mockOnClose} />)
      const toastElement = container.querySelector('.animate-slideIn')

      expect(toastElement).toBeInTheDocument()
    })
  })

  describe('ToastContainer', () => {
    it('should render all toasts', () => {
      const toasts: Toast[] = [
        { id: '1', type: 'success', message: 'Success' },
        { id: '2', type: 'error', message: 'Error' },
        { id: '3', type: 'warning', message: 'Warning' },
      ]
      const mockOnClose = vi.fn()

      render(<ToastContainer toasts={toasts} onClose={mockOnClose} />)

      expect(screen.getByText('Success')).toBeInTheDocument()
      expect(screen.getByText('Error')).toBeInTheDocument()
      expect(screen.getByText('Warning')).toBeInTheDocument()
    })

    it('should render empty container when no toasts', () => {
      const mockOnClose = vi.fn()
      const { container } = render(<ToastContainer toasts={[]} onClose={mockOnClose} />)

      expect(container.querySelector('.space-y-2')).toBeInTheDocument()
    })

    it('should call onClose for each toast', async () => {
      const mockOnClose = vi.fn()
      const toast: Toast = {
        id: '1',
        type: 'success',
        message: 'Test',
      }

      render(<ToastContainer toasts={[toast]} onClose={mockOnClose} />)

      const closeButton = screen.getByLabelText('Close notification')
      await userEvent.click(closeButton)

      expect(mockOnClose).toHaveBeenCalledWith('1')
    })

    it('should be positioned fixed at bottom right', () => {
      const { container } = render(
        <ToastContainer toasts={[]} onClose={vi.fn()} />
      )

      const containerDiv = container.querySelector('.fixed')
      expect(containerDiv).toHaveClass('bottom-4', 'right-4', 'z-50')
    })
  })
})
