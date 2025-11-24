import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import path from "path";
import { fileURLToPath } from 'url'

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },

  // ✅ Optimize dependencies (cho react-pdf nếu cần)
  optimizeDeps: {
    include: ["pdfjs-dist"],
    esbuildOptions: {
      // Node.js global to browser globalThis
      define: {
        global: "globalThis",
      },
    },
  },

  // ✅ Worker support
  worker: {
    format: "es",
  },

  // ✅ Build options
  build: {
    commonjsOptions: {
      include: [/node_modules/],
      transformMixedEsModules: true,
    },
    rollupOptions: {
      output: {
        manualChunks: {
          // Split vendor chunks
          "react-vendor": ["react", "react-dom", "react-router-dom"],
        },
      },
    },
  },
  css: {
    postcss: "./postcss.config.js",
  },
  server: {
    port: 3000,
    host: '0.0.0.0',
    proxy: {
      "/api": {
        target: "http://localhost:8980",
        changeOrigin: true,
        secure: false,
      },
    },
  },
});
