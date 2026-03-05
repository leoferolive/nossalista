/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      fontFamily: {
        sans: ['Outfit', 'Segoe UI', 'sans-serif'],
        display: ['Sora', 'Outfit', 'sans-serif'],
      },
      colors: {
        nl: {
          bg: '#fff7ed',
          'bg-soft': '#ffedd5',
          surface: '#fffdf8',
          border: '#fed7aa',
          text: '#111827',
          muted: '#475569',
          primary: '#0f766e',
          'primary-strong': '#115e59',
          accent: '#f97316',
          'accent-strong': '#ea580c',
        },
      },
      boxShadow: {
        tropical: '0 18px 40px rgba(234, 88, 12, 0.11), 0 2px 4px rgba(15, 23, 42, 0.05)',
      },
    },
  },
  plugins: [],
}
