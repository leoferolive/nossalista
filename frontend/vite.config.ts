import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import { createMockApiMiddleware } from './mock/mockServer'

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const useMockServer = env.VITE_USE_MOCK_SERVER === 'true'

  return {
    define: {
      global: 'globalThis',
    },
    plugins: [
      react(),
      {
        name: 'nossalista-mock-api',
        configureServer(server) {
          if (useMockServer) {
            server.middlewares.use(createMockApiMiddleware())
          }
        },
        configurePreviewServer(server) {
          if (useMockServer) {
            server.middlewares.use(createMockApiMiddleware())
          }
        },
      },
    ],
    server: {
      host: '0.0.0.0',
      port: 5173,
      proxy: useMockServer
        ? undefined
        : {
            '/api': {
              target: 'http://localhost:8080',
              changeOrigin: true,
            },
          },
    },
  }
})
