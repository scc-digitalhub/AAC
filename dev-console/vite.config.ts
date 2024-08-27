import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { splitVendorChunkPlugin } from 'vite';

// https://vitejs.dev/config/
export default defineConfig({
    plugins: [react(), splitVendorChunkPlugin()],
    define: {
        'process.env': process.env,
    },
    server: {
        host: true,
    },
    base: '/console/dev/',
    build: {
        manifest: true,
        commonjsOptions: { transformMixedEsModules: true },
    },
    optimizeDeps: {
        include: ['@mui/material/Tooltip'],
        exclude: ['js-big-decimal'],
    },
});
