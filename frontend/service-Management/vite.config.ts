import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

// 说明：Vite 配置，采用同源部署，开发环境可通过 proxy 直连后端
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    open: false,
    proxy: {
      // 开发期将 /api 代理到本地后端，生产通过 Nginx 反代
      '/api': {
        target: 'http://localhost:8888',
        changeOrigin: true
      }
    }
  },
  resolve: {
    alias: {
      '@': '/src'
    }
  }
});

