import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'
import fs from 'fs'
import os from 'os'
import path from 'path'

function getLocalIp() {
  const nets = os.networkInterfaces()
  for (const name of Object.keys(nets)) {
    for (const net of nets[name]) {
      if (net.family === 'IPv4' && !net.internal) {
        return net.address
      }
    }
  }
  return 'localhost'
}

function qrBaseUrlPlugin() {
  let baseUrl = ''
  return {
    name: 'qr-base-url',
    configureServer(server) {
      server.httpServer?.on('listening', () => {
        const address = server.httpServer.address()
        if (address && typeof address === 'object') {
          baseUrl = `http://${getLocalIp()}:${address.port}`
        }
      })
    },
    transformIndexHtml(html, ctx) {
      if (!ctx.bundle && baseUrl && html.includes('</head>')) {
        const script = `  <script>window.__QR_BASE_URL__ = ${JSON.stringify(baseUrl)}</script>\n  </head>`
        return html.replace('</head>', script)
      }
      return html
    }
  }
}

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    qrBaseUrlPlugin(),
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
    },
    {
      name: 'static-uploads',
      configureServer(server) {
        server.middlewares.use('/uploads', (req, res, next) => {
          const relativePath = req.url.replace(/^\/uploads/, '')
          const roots = [
            resolve(__dirname, '../api/data/uploads'),
            resolve(__dirname, '../../admin/api/data/uploads')
          ]

          function trySend(index) {
            if (index >= roots.length) {
              res.statusCode = 404
              res.end('Not found')
              return
            }
            const uploadRoot = roots[index]
            const filePath = path.join(uploadRoot, relativePath)
            if (!filePath.startsWith(uploadRoot)) {
              res.statusCode = 403
              res.end('Forbidden')
              return
            }
            fs.readFile(filePath, (err, data) => {
              if (err) {
                trySend(index + 1)
                return
              }
              const ext = path.extname(filePath).toLowerCase()
              const mime = ext === '.png' ? 'image/png'
                : ext === '.jpg' || ext === '.jpeg' ? 'image/jpeg'
                : ext === '.gif' ? 'image/gif'
                : 'application/octet-stream'
              res.setHeader('Content-Type', mime)
              res.end(data)
            })
          }

          trySend(0)
        })
      }
    }
  ],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 22345,
    proxy: {
      '/api/v1/admin': {
        target: 'http://localhost:26060',
        changeOrigin: true
      },
      '/api': {
        target: 'http://localhost:25050',
        changeOrigin: true
      }
    },
    fs: {
      allow: ['..', '../api/data/uploads']
    }
  }
})
