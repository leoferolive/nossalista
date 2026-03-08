/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  darkMode: ['selector', '[data-theme="dark"]'],
  theme: {
    extend: {
      fontFamily: {
        sans: ['"DM Sans"', 'Segoe UI', 'sans-serif'],
        display: ['"Playfair Display"', 'Georgia', 'serif'],
      },
      colors: {
        nl: {
          bg: 'rgb(var(--nl-bg) / <alpha-value>)',
          'bg-soft': 'rgb(var(--nl-bg-soft) / <alpha-value>)',
          surface: 'rgb(var(--nl-surface) / <alpha-value>)',
          'surface-strong': 'rgb(var(--nl-surface-strong) / <alpha-value>)',
          border: 'rgb(var(--nl-border) / <alpha-value>)',
          'border-strong': 'rgb(var(--nl-border-strong) / <alpha-value>)',
          text: 'rgb(var(--nl-text) / <alpha-value>)',
          muted: 'rgb(var(--nl-text-muted) / <alpha-value>)',
          primary: 'rgb(var(--nl-primary) / <alpha-value>)',
          'primary-strong': 'rgb(var(--nl-primary-strong) / <alpha-value>)',
          accent: 'rgb(var(--nl-accent) / <alpha-value>)',
          'accent-strong': 'rgb(var(--nl-accent-strong) / <alpha-value>)',
          danger: 'rgb(var(--nl-danger) / <alpha-value>)',
          success: 'rgb(var(--nl-success) / <alpha-value>)',
        },
      },
      boxShadow: {
        earthen: '0 18px 40px rgb(var(--nl-accent-strong) / 0.12), 0 2px 8px rgb(0 0 0 / 0.08)',
        'earthen-strong':
          '0 24px 60px rgb(var(--nl-accent-strong) / 0.18), 0 4px 12px rgb(0 0 0 / 0.12)',
      },
    },
  },
  plugins: [],
}
