import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
// React 版本前端：开发端口 5174（Vue 版占用 5173），避免与旧工程端口冲突
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5174,
  },
})
