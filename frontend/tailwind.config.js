/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      fontFamily: {
        sans: ['"DM Sans"', 'Segoe UI', 'sans-serif'],
        display: ['"Playfair Display"', 'Georgia', 'serif'],
      },
      colors: {
        nl: {
          bg: '#1a1410',
          'bg-soft': '#211a14',
          surface: '#2d2218',
          'surface-strong': '#382b1f',
          border: 'rgba(210,165,110,0.2)',
          'border-strong': 'rgba(210,165,110,0.4)',
          text: '#f5ece0',
          muted: '#a89072',
          primary: '#5c8a5a',
          'primary-strong': '#3d6b3b',
          accent: '#d4845a',
          'accent-strong': '#b8613a',
          danger: '#c0503a',
          success: '#5c8a5a',
        },
      },
      boxShadow: {
        earthen: '0 18px 40px rgba(92,138,90,0.12), 0 2px 8px rgba(0,0,0,0.35)',
        'earthen-strong': '0 24px 60px rgba(92,138,90,0.18), 0 4px 12px rgba(0,0,0,0.45)',
      },
    },
  },
  plugins: [],
}
