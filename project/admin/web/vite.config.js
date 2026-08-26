import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [
    vue(),
    {
      name: 'build-version',
      transformIndexHtml(html, ctx) {
        const version = ctx.bundle
          ? `build-${Date.now()}`
          : 'dev'
        let result = html.replace(
          '</head>',
          `  <script>window.__APP_VERSION__ = '${version}'</script>\n  </head>`
        )
        if (ctx.bundle) {
          // 给入口 JS/CSS 加构建版本戳，绕过微信浏览器缓存
          result = result.replace(
            /(<script[^>]*type="module"[^>]*src=")([^"]+)(")/g,
            `$1$2?v=${version}$3`
          )
          result = result.replace(
            /(<link[^>]*rel="stylesheet"[^>]*href=")([^"]+)(")/g,
            `$1$2?v=${version}$3`
          )
        }
        return result
      }
    }
  ],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 22347,
    proxy: {
      '/api': {
        target: 'http://localhost:26060',
        changeOrigin: true
      },
      '/uploads': {
        target: 'http://localhost:26060',
        changeOrigin: true
      }
    }
  }
})
