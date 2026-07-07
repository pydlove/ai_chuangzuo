import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'
import fs from 'fs'
import path from 'path'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    {
      name: 'static-uploads',
      configureServer(server) {
        server.middlewares.use('/uploads/leaderboard', (req, res, next) => {
          const uploadRoot = resolve(__dirname, '../../../data/uploads/leaderboard')
          const filePath = path.join(uploadRoot, req.url.replace(/^\/uploads\/leaderboard/, ''))
          if (!filePath.startsWith(uploadRoot)) {
            res.statusCode = 403
            res.end('Forbidden')
            return
          }
          fs.readFile(filePath, (err, data) => {
            if (err) {
              res.statusCode = 404
              res.end('Not found')
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
      '/api': {
        target: 'http://localhost:25050',
        changeOrigin: true
      }
    },
    fs: {
      allow: ['..', '../../data/uploads']
    }
  }
})
