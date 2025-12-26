import {defineConfig} from 'vitest/config';
import tailwindcss from '@tailwindcss/vite';
import {playwright} from '@vitest/browser-playwright';

export default defineConfig({
    plugins: [tailwindcss()],
    test: {
        browser: {
            enabled: true,
            provider: playwright(),
            instances: [
                {browser: 'chromium'}
            ],
            headless: true,
        },
        include: ['src/**/*.test.ts'],
    },
});
