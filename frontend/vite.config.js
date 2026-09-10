import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// /api 요청을 백엔드(8080)로 넘긴다 — 프록시를 쓰면 CORS를 신경 쓸 일이 없다.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
    },
  },
})
