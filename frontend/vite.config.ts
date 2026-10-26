import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

const gatewayOrBackendTarget = process.env.VITE_BACKEND_TARGET ?? 'http://localhost:8085';
const authGatewayTarget = process.env.VITE_GATEWAY_TARGET ?? 'http://localhost:8085';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api/v1/auth': {
        target: authGatewayTarget,
        changeOrigin: true,
      },
      '/api/v1/expenses': {
        target: gatewayOrBackendTarget,
        changeOrigin: true,
      },
      '/api/v1/ocr': {
        target: authGatewayTarget,
        changeOrigin: true,
      },
      '/api/v1/agent': {
        target: authGatewayTarget,
        changeOrigin: true,
      },
    },
  },
});
