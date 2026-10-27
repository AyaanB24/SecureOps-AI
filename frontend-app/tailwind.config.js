/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        'status-pass': '#16a34a',
        'status-block': '#dc2626',
        'status-warn': '#f59e0b',
      },
      spacing: {
        'sidebar-width': '256px',
        'header-height': '64px',
      },
      animation: {
        slideIn: 'slideIn 0.3s ease-out',
      },
      keyframes: {
        slideIn: {
          'from': { opacity: '0', transform: 'translateY(1rem)' },
          'to': { opacity: '1', transform: 'translateY(0)' },
        },
      },
    },
  },
  plugins: [],
}
