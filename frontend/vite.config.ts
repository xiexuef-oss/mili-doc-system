import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  },
  server: {
    port: 5173,
    allowedHosts: ['.ngrok-free.dev', 'localhost'],
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        timeout: 600000,  // 10min for SSE generation
        proxyTimeout: 600000,
        configure: (proxy: any) => {
          proxy.on('proxyRes', (_proxyRes: any, req: any) => {
            // Prevent buffering for SSE endpoints
            if (req.url?.includes('/chat/message') || req.url?.includes('/draft/stream') || req.url?.includes('/ai-documents/')) {
              _proxyRes.headers['cache-control'] = 'no-cache'
              _proxyRes.headers['x-accel-buffering'] = 'no'
            }
          })
        }
      }
    }
  }
})
