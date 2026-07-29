import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import path from 'path'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  // HDRI dosyalarini (.exr/.hdr) asset olarak tani (URL olarak import edilsinler)
  assetsInclude: ['**/*.exr', '**/*.hdr'],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
})
