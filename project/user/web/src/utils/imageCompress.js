/**
 * 使用浏览器原生 Canvas 将图片压缩到目标大小以内。
 * @param {File} file 原始图片文件
 * @param {number} maxSizeKB 目标最大大小，默认 200 KB
 * @param {number} maxWidth 最大边长，默认 512 px
 * @returns {Promise<File>} 压缩后的 jpeg File
 */
export function compressAvatar(file, maxSizeKB = 200, maxWidth = 512) {
  return new Promise((resolve, reject) => {
    if (!file || !file.type.startsWith('image/')) {
      reject(new Error('请选择图片文件'))
      return
    }

    const reader = new FileReader()
    reader.onerror = () => reject(new Error('文件读取失败'))
    reader.onload = (e) => {
      const img = new Image()
      img.onerror = () => reject(new Error('图片加载失败'))
      img.onload = () => {
        try {
          const canvas = document.createElement('canvas')
          const ctx = canvas.getContext('2d')
          let { width, height } = img

          if (width > maxWidth || height > maxWidth) {
            const scale = maxWidth / Math.max(width, height)
            width = Math.floor(width * scale)
            height = Math.floor(height * scale)
          }

          canvas.width = width
          canvas.height = height
          ctx.drawImage(img, 0, 0, width, height)

          const maxBytes = maxSizeKB * 1024
          let quality = 0.92
          const minQuality = 0.30
          const step = 0.06

          const tryCompress = () => {
            canvas.toBlob(
              (blob) => {
                if (!blob) {
                  reject(new Error('图片压缩失败'))
                  return
                }
                if (blob.size <= maxBytes || quality <= minQuality) {
                  if (blob.size > maxBytes) {
                    reject(new Error(`图片无法压缩到 ${maxSizeKB}KB 以内，请选择更小的图片`))
                    return
                  }
                  const compressed = new File([blob], 'avatar.jpg', { type: 'image/jpeg' })
                  resolve(compressed)
                  return
                }
                quality = Math.max(minQuality, quality - step)
                tryCompress()
              },
              'image/jpeg',
              quality
            )
          }

          tryCompress()
        } catch (err) {
          reject(err)
        }
      }
      img.src = e.target.result
    }
    reader.readAsDataURL(file)
  })
}
