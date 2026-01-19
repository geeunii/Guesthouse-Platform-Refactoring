import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/ws-chat': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        ws: true,
      },
      // OAuth2 소셜 로그인 경로 프록시 (/oauth2/authorization만, /oauth2/redirect는 프론트엔드)
      '/oauth2/authorization': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/login/oauth2': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      }
    }
  },
  define: {
    global: 'window',
  },
  build: {
    emptyOutDir: true,
  }
})

