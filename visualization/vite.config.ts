import {defineConfig} from 'vite'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
    base: './',
    plugins: [
        tailwindcss(),
    ],
    build: {
        target: 'esnext',
        minify: 'esbuild',
        rollupOptions: {
            external: [/\.test\.ts$/, /__fixtures__/],
            output: {
                manualChunks: {
                    'echarts': ['echarts/core', 'echarts/charts', 'echarts/components', 'echarts/renderers'],
                    'lit': ['lit'],
                },
            },
        },
    },
})
