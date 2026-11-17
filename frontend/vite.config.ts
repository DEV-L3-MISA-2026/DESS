import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// The backend WAR is served under /dess (see backend/platform/pom.xml, finalName).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '/dess/api'),
      },
    },
  },
})
