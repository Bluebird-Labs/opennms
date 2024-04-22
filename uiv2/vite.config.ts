import {defineConfig} from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'

// https://vitejs.dev/config/
export default defineConfig({
    resolve: {
        alias: {
            '@/': new URL('./src/', import.meta.url).pathname,
        },
        dedupe: ['vue']
    },
    plugins: [vue(),
        // https://github.com/antfu/unplugin-auto-import
        AutoImport({
            imports: ['vue', 'vue-router', '@vueuse/core'],
            eslintrc: {
                enabled: true,
                filepath: './.eslintrc-auto-import.json'
            }
        })],
})
