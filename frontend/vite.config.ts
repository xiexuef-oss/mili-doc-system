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
        timeout: 1800000,       // 30min for SSE
        proxyTimeout: 1800000,  // 30min for SSE response
        configure: (proxy: any) => {
          proxy.on('proxyReq', (_proxyReq: any, req: any) => {
            // Disable request timeout for SSE endpoints
            if (req.url?.includes('/ai-documents/') && (req.url?.includes('/generate') || req.url?.includes('/generate-content'))) {
              req.socket?.setTimeout(0)
              req.socket?.setKeepAlive(true)
            }
          })
          proxy.on('proxyRes', (_proxyRes: any, req: any) => {
            // Prevent buffering for SSE endpoints
            if (req.url?.includes('/chat/message') || req.url?.includes('/draft/stream') || req.url?.includes('/ai-documents/')) {
              _proxyRes.headers['cache-control'] = 'no-cache'
              _proxyRes.headers['x-accel-buffering'] = 'no'
              _proxyRes.headers['connection'] = 'keep-alive'
            }
          })
        }
      }
    }
  }
})
