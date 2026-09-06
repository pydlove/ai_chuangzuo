import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [
    vue(),
    (() => {
      // 一次构建共用一个版本号，保证 index.html 注入值与 version.json 一致
      let buildVersion = null
      return {
        name: 'build-version',
        buildStart() {
          buildVersion = `build-${Date.now()}`
        },
        transformIndexHtml(html, ctx) {
          const version = ctx.bundle
            ? buildVersion
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
        },
        generateBundle() {
          // 心跳检测用：随 dist 一起发布
          this.emitFile({
            type: 'asset',
            fileName: 'version.json',
            source: JSON.stringify({
              version: buildVersion,
              buildTime: new Date().toISOString()
            })
          })
        }
      }
    })()
  ],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 22347,
    proxy: {
      // 用户端资源（如用户上传头像）：开发环境转发到本地 user-api（见 src/utils/userAsset.js）
      '/api/v1/user': {
        target: 'http://localhost:25050',
        changeOrigin: true
      },
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
