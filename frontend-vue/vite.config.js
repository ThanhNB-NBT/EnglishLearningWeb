// vite.config.js

import { fileURLToPath, URL } from 'node:url'
import process from 'node:process'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },

  server: {
    host: '0.0.0.0',
    port: 5173,
    strictPort: false,

    // ✅ Cho phép tất cả hosts
    allowedHosts: [
      '.trycloudflare.com',
      'issued-looking-postcards-tonight.trycloudflare.com',
      'localhost',
    ],

    watch: {
      usePolling: true,  // ✅ IMPORTANT for Docker
      interval: 100,     // ✅ Poll every 100ms
    },

    // ✅ CONDITIONAL HMR: Bật khi dev local, tắt khi dùng tunnel
    hmr: process.env.DISABLE_HMR === 'true' ? false : {
      host: 'localhost',
      protocol: 'ws',
      port: 5173,
      // clientPort: 5173  // Uncomment nếu cần
    },

    // Proxy API requests
    proxy: {
      '/api': {
        target: 'http://app:8980',
        changeOrigin: true,
      }
    }
  }
})
