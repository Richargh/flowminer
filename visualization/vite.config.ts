import {defineConfig} from 'vite'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
    base: './',
    plugins: [
        tailwindcss(),
    ],
    build: {
        target: 'esnext',
        minify: 'oxc',
        rolldownOptions: {
            external: [/\.test\.ts$/, /__fixtures__/],
            output: {
                manualChunks: (id) => {
                    if (id.includes('/node_modules/echarts')) return 'echarts'
                    if (id.includes('/node_modules/lit')) return 'lit'
                },
            },
        },
    },
})
