import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import { VitePWA } from 'vite-plugin-pwa'
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
      VitePWA({
        registerType: 'autoUpdate',
        strategies: 'injectManifest',
        srcDir: 'src',
        filename: 'sw.ts',
        manifest: {
          name: 'NossaLista',
          short_name: 'NossaLista',
          start_url: '/',
          display: 'standalone',
          theme_color: '#1a1a2e',
          background_color: '#1a1a2e',
          icons: [{ src: '/favicon.ico', sizes: '64x64', type: 'image/x-icon' }],
        },
        injectManifest: {
          globPatterns: ['**/*.{js,css,html,ico,png,svg}'],
        },
        devOptions: {
          enabled: true,
        },
      }),
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
