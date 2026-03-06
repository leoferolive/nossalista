import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  test: {
    include: ['src/**/*.{test,spec}.{ts,tsx}'],
    globals: true,
    environment: 'jsdom',
    setupFiles: './src/test/setup.ts',
    css: true,
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json-summary', 'lcov'],
      include: ['src/**/*.{ts,tsx}'],
      exclude: [
        'src/**/*.test.{ts,tsx}',
        'src/test/**',
        'src/api/**',
        'src/pages/**',
        'src/main.tsx',
        'src/App.tsx',
        'src/components/EditListNameModal.tsx',
        'src/components/ItemOptionsMenu.tsx',
        'src/components/Toast.tsx',
        'src/components/UserProfile.tsx',
        'src/hooks/useActivities.ts',
        'src/hooks/useLists.ts',
        'src/hooks/useWebSocket.ts',
        'src/types/ActivityLog.ts',
        'src/types/ApiError.ts',
        'src/types/Item.ts',
        'src/types/OnlineMember.ts',
        'src/types/ProblemDetail.ts',
      ],
      thresholds: {
        lines: 80,
        branches: 80,
        functions: 80,
        statements: 80,
      },
    },
  },
})
