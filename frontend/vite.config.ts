import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// In dev, /api goes to the gateway so the browser sees a single origin (no CORS setup needed locally).
export default defineConfig({
  plugins: [react()],
  server: { port: 5173, proxy: { '/api': 'http://localhost:8080' } },
});
