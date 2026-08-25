/**
 * 检测浏览器是否支持指定的 Canvas 输出格式。
 * @param {string} mimeType 例如 'image/webp'
 * @returns {boolean}
 */
function isCanvasFormatSupported(mimeType) {
  const canvas = document.createElement('canvas')
  canvas.width = 1
  canvas.height = 1
  const dataUrl = canvas.toDataURL(mimeType)
  return dataUrl.startsWith(`data:${mimeType}`)
}

/**
 * 压缩图片并保留透明背景。
 * @param {File} file 原始图片文件
 * @param {number} scale 缩放比例 0-1
 * @param {string} outputFormat 输出格式，默认 'image/webp'（保留透明且更小）；不支持时回退 PNG
 * @returns {Promise<{ blob: Blob, url: string, width: number, height: number, originalSize: number, compressedSize: number, format: string }>}
 */
export function compressImage(file, scale = 0.8, outputFormat = 'image/webp') {
  return new Promise((resolve, reject) => {
    if (!file || !file.type.startsWith('image/')) {
      reject(new Error('请选择图片文件'))
      return
    }

    const clampedScale = Math.max(0.01, Math.min(1, scale))
    const effectiveFormat = isCanvasFormatSupported(outputFormat) ? outputFormat : 'image/png'
    const quality = effectiveFormat === 'image/png' ? undefined : 0.92
    const extension = effectiveFormat === 'image/webp' ? 'webp' : 'png'

    const reader = new FileReader()
    reader.onerror = () => reject(new Error('文件读取失败'))
    reader.onload = (e) => {
      const img = new Image()
      img.onerror = () => reject(new Error('图片加载失败'))
      img.onload = () => {
        try {
          const canvas = document.createElement('canvas')
          const ctx = canvas.getContext('2d')
          const width = Math.max(1, Math.floor(img.width * clampedScale))
          const height = Math.max(1, Math.floor(img.height * clampedScale))

          canvas.width = width
          canvas.height = height
          ctx.drawImage(img, 0, 0, width, height)

          canvas.toBlob((blob) => {
            if (!blob) {
              reject(new Error('图片压缩失败'))
              return
            }
            const url = URL.createObjectURL(blob)
            resolve({
              blob,
              url,
              width,
              height,
              originalSize: file.size,
              compressedSize: blob.size,
              format: effectiveFormat,
              extension
            })
          }, effectiveFormat, quality)
        } catch (err) {
          reject(err)
        }
      }
      img.src = e.target.result
    }
    reader.readAsDataURL(file)
  })
}

export function formatSize(bytes) {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(2)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`
}
