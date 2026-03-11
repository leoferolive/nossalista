/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      fontFamily: {
        sans: ['"Plus Jakarta Sans"', 'Segoe UI', 'sans-serif'],
        display: ['"Fraunces"', 'Georgia', 'serif'],
      },
      colors: {
        nl: {
          bg: 'var(--nl-bg)',
          'bg-soft': 'var(--nl-bg-soft)',
          surface: 'var(--nl-surface)',
          'surface-strong': 'var(--nl-surface-strong)',
          border: 'var(--nl-border)',
          'border-strong': 'var(--nl-border-strong)',
          text: 'var(--nl-text)',
          muted: 'var(--nl-text-muted)',
          primary: 'var(--nl-primary)',
          'primary-strong': 'var(--nl-primary-strong)',
          accent: 'var(--nl-accent)',
          'accent-strong': 'var(--nl-accent-strong)',
          danger: 'var(--nl-danger)',
          success: 'var(--nl-success)',
        },
      },
      boxShadow: {
        earthen: 'var(--nl-card-shadow)',
        'earthen-strong': 'var(--nl-card-shadow-strong)',
      },
    },
  },
  plugins: [],
}
