<template>
  <Teleport to="body">
    <Transition name="title-opt-fade">
      <div v-if="visible" class="cards-modal-overlay" @click.self="close">
        <div class="cards-modal-box">
          <button class="cards-modal-close" @click="close">&times;</button>
          <div class="cards-modal-header">
            <h2 class="cards-modal-title">生成贴图</h2>
            <button class="cards-modal-download-all" @click="downloadAllCards">全部下载</button>
          </div>

          <div class="cards-modal-tabs">
            <button
              v-for="key in cardStyleKeys"
              :key="key"
              class="cards-modal-tab"
              :class="{ active: cardsStyle === key }"
              :style="cardsStyle === key ? { background: cardStyles[key].accent, borderColor: cardStyles[key].accent, color: '#fff' } : {}"
              @click="cardsStyle = key"
            >
              {{ cardStyles[key].label }}
            </button>
          </div>

          <div class="cards-modal-sub">
            共 {{ cardsData.length }} 张 · {{ cardStyles[cardsStyle].label }}风格 · 点击单张可下载
          </div>

          <div class="cards-modal-grid">
            <div
              v-for="(card, index) in cardsData"
              :key="index"
              class="cards-modal-item"
              @click="downloadCard(index)"
            >
              <canvas
                :ref="el => { if (el) cardCanvasRefs[index] = el }"
                class="cards-modal-canvas"
                width="750"
                height="1000"
              ></canvas>
              <div class="cards-modal-item-label">
                图 {{ index + 1 }} / {{ cardsData.length }}{{ card.type === 'cover' ? ' · 封面' : ' · ' + card.title }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, computed, watch, nextTick } from 'vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  article: { type: Object, default: null }
})

const emit = defineEmits(['update:visible'])

const cardsStyle = ref('xiaohongshu')
const cardsData = ref([])
const cardCanvasRefs = ref([])

const cardStyles = {
  xiaohongshu: {
    label: '小红书',
    accent: '#ff2442',
    coverGrad: ['#ff2442', '#ff7a8a', '#ffd1d9'],
    coverCircle: 'rgba(255,255,255,0.18)',
    coverCircle2: 'rgba(255,255,255,0.12)',
    tagBg: 'rgba(255,255,255,0.95)',
    tagText: '干货分享',
    brandText: '— 作者昵称 —',
    contentBg: '#fff',
    headingColor: '#1a1a1a',
    bodyColor: '#595959',
    numColor: '#fff',
    footerBg: '#fff0f3',
    font: '"PingFang SC", sans-serif'
  },
  wechat: {
    label: '公众号',
    accent: '#07c160',
    coverGrad: ['#07c160', '#95de64', '#d9f7be'],
    coverCircle: 'rgba(255,255,255,0.2)',
    coverCircle2: 'rgba(255,255,255,0.14)',
    tagBg: 'rgba(255,255,255,0.95)',
    tagText: '深度好文',
    brandText: '— 作者昵称 —',
    contentBg: '#fff',
    headingColor: '#1a1a1a',
    bodyColor: '#595959',
    numColor: '#fff',
    footerBg: '#f6ffed',
    font: '"PingFang SC", sans-serif'
  },
  douyin: {
    label: '抖音',
    accent: '#25f4ee',
    coverGrad: ['#0a0a0a', '#1a1a1a', '#fe2c55'],
    coverCircle: 'rgba(37,244,238,0.25)',
    coverCircle2: 'rgba(254,44,85,0.25)',
    tagBg: '#25f4ee',
    tagText: '上热门',
    brandText: '— 作者昵称 —',
    contentBg: '#1a1a1a',
    headingColor: '#fff',
    bodyColor: '#d9d9d9',
    numColor: '#0a0a0a',
    footerBg: '#000',
    font: '"PingFang SC", sans-serif'
  },
  literary: {
    label: '文艺',
    accent: '#8b5e34',
    coverGrad: ['#8b5e34', '#d4a373', '#f0e6d8'],
    coverCircle: 'rgba(255,255,255,0.18)',
    coverCircle2: 'rgba(255,255,255,0.12)',
    tagBg: 'rgba(255,255,255,0.92)',
    tagText: '慢读时光',
    brandText: '— 作者昵称 —',
    contentBg: '#faf5ef',
    headingColor: '#5a3e2b',
    bodyColor: '#8b5e34',
    numColor: '#fff',
    footerBg: '#f0e6d8',
    font: 'Georgia, "Songti SC", serif'
  },
  minimal: {
    label: '极简',
    accent: '#1a1a1a',
    coverGrad: ['#1a1a1a', '#262626', '#404040'],
    coverCircle: 'rgba(255,255,255,0.06)',
    coverCircle2: 'rgba(255,255,255,0.04)',
    tagBg: '#fff',
    tagText: 'NOTE',
    brandText: '— YOUR NAME —',
    contentBg: '#fff',
    headingColor: '#000',
    bodyColor: '#262626',
    numColor: '#fff',
    footerBg: '#fafafa',
    font: '"Helvetica Neue", "PingFang SC", sans-serif'
  },
  business: {
    label: '商务',
    accent: '#1677ff',
    coverGrad: ['#003a8c', '#1677ff', '#bae0ff'],
    coverCircle: 'rgba(255,255,255,0.15)',
    coverCircle2: 'rgba(255,255,255,0.1)',
    tagBg: 'rgba(255,255,255,0.95)',
    tagText: 'INSIGHT',
    brandText: '— YOUR NAME —',
    contentBg: '#fff',
    headingColor: '#003a8c',
    bodyColor: '#595959',
    numColor: '#fff',
    footerBg: '#f0f5ff',
    font: '"PingFang SC", sans-serif'
  }
}

const cardStyleKeys = computed(() => Object.keys(cardStyles))

const close = () => {
  emit('update:visible', false)
}

const generateCardsData = (article) => {
  if (!article) return []
  const title = article.title || '未命名文章'
  const body = article.body || ''
  const paragraphs = body.split(/\n+/).filter(line => line.trim())
  const cards = []

  const firstP = paragraphs.find(p => !p.startsWith('【'))
  cards.push({
    type: 'cover',
    title,
    desc: firstP ? firstP.trim().slice(0, 80) : ''
  })

  let currentHeading = null
  let currentContent = []
  let headingIndex = 0

  paragraphs.forEach(line => {
    const headingMatch = line.match(/^【([^】]+)】$/)
    if (headingMatch) {
      if (currentHeading) {
        cards.push({
          type: 'content',
          num: headingIndex,
          title: currentHeading,
          content: currentContent.slice(0, 5).join('\n')
        })
      }
      headingIndex++
      currentHeading = headingMatch[1].trim()
      currentContent = []
    } else if (currentHeading) {
      currentContent.push(line.trim().slice(0, 120))
    }
  })

  if (currentHeading) {
    cards.push({
      type: 'content',
      num: headingIndex,
      title: currentHeading,
      content: currentContent.slice(0, 5).join('\n')
    })
  }

  return cards
}

const renderAll = () => {
  if (!props.visible) return
  nextTick(() => {
    cardCanvasRefs.value.forEach((canvas, index) => {
      if (canvas) drawCard(canvas, cardsData.value[index], cardsStyle.value)
    })
  })
}

watch(() => props.visible, (val) => {
  if (val) {
    cardsData.value = generateCardsData(props.article)
    cardsStyle.value = 'xiaohongshu'
    renderAll()
  }
})

watch(cardsStyle, () => {
  renderAll()
})

const wrapCardText = (ctx, text, x, y, maxWidth, lineHeight, maxLines) => {
  if (!text) return y
  const chars = text.split('')
  let line = ''
  let yy = y
  let drawnLines = 0

  for (let i = 0; i < chars.length; i++) {
    line += chars[i]
    if (ctx.measureText(line).width > maxWidth) {
      line = line.slice(0, -1)
      if (drawnLines >= maxLines - 1 && i < chars.length - 1) {
        while (ctx.measureText(line + '...').width > maxWidth) line = line.slice(0, -1)
        ctx.fillText(line + '...', x, yy)
        return yy + lineHeight
      }
      ctx.fillText(line, x, yy)
      drawnLines++
      yy += lineHeight
      line = chars[i]
    }
  }

  if (line) {
    if (drawnLines >= maxLines - 1) {
      while (ctx.measureText(line + '...').width > maxWidth) line = line.slice(0, -1)
      ctx.fillText(line + '...', x, yy)
    } else {
      ctx.fillText(line, x, yy)
    }
  }
  return yy
}

const drawCard = (canvas, data, styleName) => {
  const style = cardStyles[styleName] || cardStyles.xiaohongshu
  const ctx = canvas.getContext('2d')
  const w = canvas.width
  const h = canvas.height
  ctx.clearRect(0, 0, w, h)

  if (data.type === 'cover') {
    const grad = ctx.createLinearGradient(0, 0, w, h)
    grad.addColorStop(0, style.coverGrad[0])
    grad.addColorStop(0.5, style.coverGrad[1])
    grad.addColorStop(1, style.coverGrad[2])
    ctx.fillStyle = grad
    ctx.fillRect(0, 0, w, h)

    ctx.fillStyle = style.coverCircle || 'rgba(255,255,255,0.18)'
    ctx.beginPath(); ctx.arc(w - 80, 120, 160, 0, Math.PI * 2); ctx.fill()
    ctx.beginPath(); ctx.arc(60, h - 220, 120, 0, Math.PI * 2); ctx.fill()
    ctx.fillStyle = style.coverCircle2 || 'rgba(255,255,255,0.12)'
    ctx.beginPath(); ctx.arc(w - 200, h - 120, 90, 0, Math.PI * 2); ctx.fill()

    ctx.fillStyle = style.tagBg
    ctx.beginPath()
    ctx.moveTo(60 + 80, 80)
    ctx.arcTo(60 + 160, 80, 60 + 160, 80 + 44, 22)
    ctx.arcTo(60 + 160, 80 + 44, 60, 80 + 44, 22)
    ctx.arcTo(60, 80 + 44, 60, 80, 22)
    ctx.arcTo(60, 80, 60 + 160, 80, 22)
    ctx.closePath()
    ctx.fill()
    ctx.fillStyle = style.accent
    ctx.font = `bold 22px ${style.font}`
    ctx.textAlign = 'center'
    ctx.fillText(style.tagText, 140, 111)

    ctx.fillStyle = '#fff'
    ctx.font = `bold 60px ${style.font}`
    ctx.textAlign = 'left'
    ctx.shadowColor = 'rgba(0,0,0,0.1)'
    ctx.shadowBlur = 8
    ctx.shadowOffsetY = 4
    wrapCardText(ctx, data.title, 60, 320, w - 120, 78, 4)
    ctx.shadowColor = 'transparent'
    ctx.shadowBlur = 0

    ctx.fillStyle = 'rgba(255,255,255,0.92)'
    ctx.font = `26px ${style.font}`
    wrapCardText(ctx, data.desc, 60, h - 220, w - 120, 40, 2)

    ctx.fillStyle = '#fff'
    ctx.font = `bold 26px ${style.font}`
    ctx.fillText(style.brandText, 60, h - 80)
  } else {
    const bg = style.contentBg || '#fff'
    ctx.fillStyle = bg
    ctx.fillRect(0, 0, w, h)

    ctx.fillStyle = style.accent
    ctx.fillRect(0, 0, w, 14)

    ctx.fillStyle = style.accent
    ctx.beginPath()
    ctx.arc(110, 140, 56, 0, Math.PI * 2)
    ctx.fill()
    ctx.fillStyle = style.numColor || '#fff'
    ctx.font = `bold 48px ${style.font}`
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    const numStr = String(data.num).padStart(2, '0')
    ctx.fillText(numStr, 110, 148)
    ctx.textBaseline = 'alphabetic'

    ctx.fillStyle = style.headingColor
    ctx.font = `bold 46px ${style.font}`
    ctx.textAlign = 'left'
    const titleEndY = wrapCardText(ctx, data.title, 60, 260, w - 120, 60, 2)

    ctx.fillStyle = style.accent
    ctx.fillRect(60, titleEndY + 8, 80, 5)

    ctx.fillStyle = style.bodyColor
    ctx.font = `28px ${style.font}`
    let contentY = titleEndY + 60
    const lines = (data.content || '').split('\n')
    lines.forEach(line => {
      if (!line.trim()) return
      wrapCardText(ctx, line.trim(), 60, contentY, w - 120, 44, 1)
      contentY += 50
    })

    ctx.fillStyle = style.footerBg
    ctx.fillRect(0, h - 110, w, 110)
    ctx.fillStyle = style.accent
    ctx.font = `bold 22px ${style.font}`
    ctx.fillText(style.brandText, 60, h - 55)
  }
}

const renderCardCanvas = (index) => {
  const canvas = cardCanvasRefs.value[index]
  if (!canvas) return null
  const card = cardsData.value[index]
  drawCard(canvas, card, cardsStyle.value)
  return canvas
}

const downloadCanvas = (canvas, filename) => {
  canvas.toBlob((blob) => {
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = filename
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  }, 'image/png')
}

const downloadCard = (index) => {
  const canvas = renderCardCanvas(index)
  if (!canvas) return
  const style = cardStyles[cardsStyle.value]
  downloadCanvas(canvas, `${style.label}_贴图_${index + 1}_${cardsData.value.length}.png`)
}

const downloadAllCards = () => {
  cardsData.value.forEach((_, index) => {
    setTimeout(() => {
      downloadCard(index)
    }, index * 400)
  })
}
</script>

<style scoped>
.cards-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  z-index: 1000;
}

.cards-modal-box {
  background: #fff;
  border-radius: 16px;
  width: 100%;
  max-width: 1200px;
  max-height: 92vh;
  overflow-y: auto;
  padding: 32px;
  position: relative;
  box-sizing: border-box;
}

.cards-modal-close {
  position: absolute;
  top: 12px;
  right: 16px;
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  font-size: 28px;
  color: #595959;
  cursor: pointer;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
  z-index: 2;
  transition: all 0.2s;
}

.cards-modal-close:hover {
  color: #262626;
  background: #f5f5f5;
}

.cards-modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  padding-right: 40px;
  flex-wrap: wrap;
  gap: 12px;
}

.cards-modal-title {
  margin: 0;
  font-size: 22px;
  color: #1a1a1a;
}

.cards-modal-download-all {
  padding: 10px 20px;
  background: #07c160;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.cards-modal-download-all:hover {
  background: #06ad56;
}

.cards-modal-tabs {
  display: flex;
  gap: 10px;
  margin: 12px 0;
  flex-wrap: wrap;
}

.cards-modal-tab {
  padding: 8px 18px;
  border: 1px solid #d9d9d9;
  background: #fff;
  border-radius: 20px;
  font-size: 13px;
  color: #595959;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.15s;
}

.cards-modal-tab:hover {
  border-color: #07c160;
  color: #07c160;
}

.cards-modal-sub {
  color: #8c8c8c;
  font-size: 13px;
  margin-bottom: 16px;
}

.cards-modal-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 20px;
}

.cards-modal-item {
  text-align: center;
  cursor: pointer;
}

.cards-modal-canvas {
  width: 100%;
  height: auto;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  transition: transform 0.2s;
}

.cards-modal-item:hover .cards-modal-canvas {
  transform: translateY(-4px);
}

.cards-modal-item-label {
  margin-top: 10px;
  font-size: 13px;
  color: #595959;
  line-height: 1.4;
}

.title-opt-fade-enter-active,
.title-opt-fade-leave-active {
  transition: opacity 0.2s ease;
}

.title-opt-fade-enter-from,
.title-opt-fade-leave-to {
  opacity: 0;
}

@media (max-width: 768px) {
  .cards-modal-overlay {
    padding: 12px;
  }

  .cards-modal-box {
    padding: 16px;
    border-radius: 12px;
  }

  .cards-modal-title {
    font-size: 17px;
  }

  .cards-modal-download-all {
    padding: 7px 14px;
    font-size: 13px;
  }

  .cards-modal-tabs {
    gap: 6px;
  }

  .cards-modal-tab {
    padding: 6px 12px;
    font-size: 12px;
  }

  .cards-modal-grid {
    grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
    gap: 12px;
  }

  .cards-modal-item-label {
    font-size: 11px;
  }
}

/* 深色模式 */
body[data-theme="dark"] .cards-modal-overlay {
  background: rgba(0, 0, 0, 0.7);
}

body[data-theme="dark"] .cards-modal-box {
  background: #1f1f1f;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5);
}

body[data-theme="dark"] .cards-modal-close {
  color: #a6a6a6;
}

body[data-theme="dark"] .cards-modal-close:hover {
  background: #2a2a2a;
  color: #f0f0f0;
}

body[data-theme="dark"] .cards-modal-title {
  color: #f0f0f0;
}

body[data-theme="dark"] .cards-modal-download-all {
  background: transparent;
  border-color: #07c160;
  color: #07c160;
}

body[data-theme="dark"] .cards-modal-download-all:hover {
  background: rgba(7, 193, 96, 0.15);
}

body[data-theme="dark"] .cards-modal-tab {
  background: #2a2a2a;
  border-color: #434343;
  color: #a6a6a6;
}

body[data-theme="dark"] .cards-modal-tab:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

body[data-theme="dark"] .cards-modal-sub {
  color: #a6a6a6;
}

body[data-theme="dark"] .cards-modal-item-label {
  color: #a6a6a6;
}
</style>