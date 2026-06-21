import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

// Dev server on 5173 (matches the backend CORS allow-list).
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
  },
});
