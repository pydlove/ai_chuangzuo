<template>
  <div class="preview-index">
    <div class="preview-header">
      <button class="back-btn" @click="$router.push('/console/works')">
        ← 返回
      </button>
      <h2 class="preview-title-text">预览/导出</h2>
      <div class="preview-header-actions">
        <button class="action-btn" @click="goToEditPage">
          编辑正文
        </button>
        <button class="action-btn" @click="copyText">
          <CopyOutlined /> 复制正文
        </button>
        <button class="action-btn primary" @click="exportWord">
          导出 Word
        </button>
      </div>
    </div>

    <div v-if="!article" class="preview-empty">
      <div class="empty-icon">📄</div>
      <div class="empty-text">暂无文章内容</div>
      <button class="empty-btn" @click="$router.push('/console/create')">去创作</button>
    </div>

    <div v-else class="preview-content">
      <div class="preview-article">
        <h1 class="article-title">{{ article.title }}</h1>
        <div class="article-meta">
          <span>{{ article.completedAt || '' }}</span>
          <span>·</span>
          <span>约 {{ article.wordCount }} 字</span>
          <span class="article-style-badge">
            风格:{{ article.style || '专业严谨' }}
          </span>
        </div>
        <div v-if="!isEditing" class="article-body" v-html="formattedBody" @click="onBodyClick"></div>
        <div v-else class="article-body editing-body" @click="onBodyClick">
          <div
            v-for="(block, idx) in blocks"
            :key="idx"
            :class="['edit-block', block.type, { modified: modifiedIndices.has(idx) }]"
            :data-type="block.type"
            contenteditable="true"
            @paste="onPaste"
            @input="onBlockInput(idx, $event)"
            v-html="renderBlockHtml(block)"
          />
        </div>
      </div>

      <!-- 发布描述 -->
      <div class="publish-meta-card">
        <div class="meta-section-title">发布描述</div>
        <textarea
          v-model="publishDesc"
          class="publish-desc-input"
          placeholder="输入发布描述..."
        ></textarea>
        <div class="meta-actions">
          <button class="meta-btn" @click="regenerateDesc">换一版</button>
          <button class="meta-btn primary" @click="copyDesc">复制描述</button>
        </div>

        <div class="meta-section-title" style="margin-top: 24px;">推荐标签</div>
        <div class="publish-tags">
          <span
            v-for="tag in publishTags"
            :key="tag"
            class="publish-tag"
          >
            {{ tag }}
          </span>
        </div>
        <div class="meta-actions">
          <button class="meta-btn" @click="regenerateTags">换一批</button>
          <button class="meta-btn primary" @click="copyTags">复制全部标签</button>
        </div>
      </div>
    </div>

    <!-- 浮动操作栏 -->
    <div v-if="article && !isEditing" class="floating-action-bar">
      <button class="float-btn" @click="optimizeTitle">
        ✧ AI 优化标题
      </button>
      <button class="float-btn primary" @click="exportWord">
        导出 Word
      </button>
      <button class="float-btn outline" @click="copyText">
        复制正文
      </button>
      <button class="float-btn danger" @click="generateCards">
        生成贴图
      </button>
    </div>

    <!-- 编辑态保存条 -->
    <div v-if="article && isEditing" class="edit-floating-bar">
      <span class="edit-hint">{{ modifiedIndices.size > 0 ? `已修改 ${modifiedIndices.size} 处` : '正在编辑' }}</span>
      <button class="float-btn" @click="cancelEdit">取消</button>
      <button class="float-btn primary" @click="saveEdit">保存修改</button>
    </div>

    <!-- AI 标题优化弹窗 -->
    <Teleport to="body">
      <Transition name="title-opt-fade">
        <div v-if="titleOptVisible" class="title-opt-overlay" @click.self="closeTitleOpt">
          <div class="title-opt-box">
            <button class="title-opt-close" @click="closeTitleOpt">&times;</button>
            <div class="title-opt-header">AI 标题优化</div>

            <div class="title-opt-section">
              <div class="title-opt-original">
                <span>原标题</span>{{ article.title }}
              </div>
              <div class="title-opt-section-label">
                <span>AI 推荐标题</span>
                <button class="title-opt-refresh" :disabled="refreshing" @click="refreshTitles">
                  {{ refreshing ? '加载中...' : '换一批' }}
                </button>
              </div>
              <div class="title-opt-list">
                <div
                  v-for="(title, index) in currentTitleOptSet"
                  :key="`ai-${index}`"
                  class="title-opt-item"
                  :class="{ selected: selectedTitle === title }"
                  :style="getItemDelayStyle(index)"
                  @click="selectTitle(title)"
                >
                  <div class="title-opt-radio"></div>
                  <div class="title-opt-item-text">{{ title }}</div>
                </div>
              </div>
            </div>

            <div class="title-opt-section">
              <div class="title-opt-section-label">
                <span>平台适配标题</span>
              </div>
              <div class="title-opt-platform-tabs">
                <button
                  v-for="tab in platformTabMeta"
                  :key="tab.key"
                  class="title-opt-platform-tab"
                  :class="{ active: currentPlatform === tab.key }"
                  @click="currentPlatform = tab.key"
                >
                  {{ tab.label }}
                </button>
              </div>
              <div class="title-opt-list">
                <div
                  v-for="(title, index) in currentPlatformTitles"
                  :key="`platform-${index}`"
                  class="title-opt-item"
                  :class="{ selected: selectedTitle === title }"
                  @click="selectTitle(title)"
                >
                  <div class="title-opt-radio"></div>
                  <div class="title-opt-item-text">{{ title }}</div>
                </div>
              </div>
            </div>

            <div class="title-opt-footer">
              <button class="title-opt-btn-cancel" @click="closeTitleOpt">取消</button>
              <button class="title-opt-btn-confirm" :disabled="!selectedTitle" @click="confirmTitle">确认替换</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <CardsModal v-model:visible="cardsModalVisible" :article="article" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'

const router = useRouter()
import { CopyOutlined } from '@ant-design/icons-vue'
import { loadCurrentArticle, saveCurrentArticle, syncArticleToQueue } from '@/utils/articleStorage.js'
import { parseBodyToBlocks, serializeBlocksToArticle, BLOCK_TYPES } from '@/utils/articleBlocks.js'
import CardsModal from '@/components/CardsModal.vue'

const article = ref(null)
const publishDesc = ref('')
const publishTags = ref([])

// AI 标题优化
const titleOptVisible = ref(false)
const titleOptSetIndex = ref(0)
const currentPlatform = ref('wechat')
const selectedTitle = ref('')
const refreshing = ref(false)
const animateItems = ref(false)

// 贴图生成弹窗（复用 CardsModal 组件）
const cardsModalVisible = ref(false)

// 编辑态
const isEditing = ref(false)
const blocks = ref([])
const modifiedIndices = ref(new Set())
const articleSnapshot = ref(null)


const titleOptSets = [
  [
    '如何高效管理时间：从混乱到掌控的 5 个方法',
    '时间管理终极指南：5 个技巧让你每天多出 2 小时',
    '告别瞎忙！这 5 个时间管理方法让你效率翻倍',
    '为什么你总感觉时间不够？5 个方法帮你找回掌控感'
  ],
  [
    '每天多出 2 小时的秘密：5 个时间管理技巧',
    '从拖延到高效：5 个步骤重塑你的时间观',
    '时间管理的真相：不是管理时间，而是管理自己',
    '每天瞎忙却一事无成？这 5 个方法让你效率翻 3 倍',
    '时间管理大师不会告诉你的 5 个秘诀，学会你就赢了'
  ]
]

const platformTitleData = {
  wechat: [
    '如何高效管理时间：从混乱到掌控的 5 个方法',
    '告别忙乱！这 5 个时间管理术，让你的每一天都井井有条'
  ],
  toutiao: [
    '每天多出 2 小时！5 个时间管理技巧，告别低效人生',
    '时间不够用？试试这 5 个方法，效率提升 300%'
  ],
  xiaohongshu: [
    '⏰ 拯救拖延症｜5 个超实用时间管理法，亲测有效！',
    '打工人必看✨ 5 个时间管理技巧，让你工作生活两不误'
  ],
  baijiahao: [
    '高效时间管理的 5 个核心方法，助你成为时间的主人',
    '深度解析：为什么时间管理是当代人最稀缺的能力'
  ],
  zhihu: [
    '如何高效地管理时间？5 个经过验证的系统方法',
    '时间管理到底管什么？从认知到实践的 5 个步骤'
  ]
}

const platformTabMeta = [
  { key: 'wechat', label: '公众号' },
  { key: 'toutiao', label: '头条' },
  { key: 'xiaohongshu', label: '小红书' },
  { key: 'baijiahao', label: '百家号' },
  { key: 'zhihu', label: '知乎' }
]

const currentTitleOptSet = computed(() => titleOptSets[titleOptSetIndex.value] || titleOptSets[0])
const currentPlatformTitles = computed(() => platformTitleData[currentPlatform.value] || platformTitleData.wechat)

const getItemDelayStyle = (index) => {
  if (!animateItems.value) return {}
  return {
    animationDelay: `${index * 0.08}s`
  }
}

// 加载文章
const loadArticle = () => {
  const saved = loadCurrentArticle()
  if (saved) {
    article.value = saved
    generateMeta()
  }
}

// 编辑态
const goToEditPage = () => {
  if (!article.value) return
  localStorage.setItem('aichuangzuo_current_article', JSON.stringify(article.value))
  router.push('/console/edit')
}

const enterEditMode = () => {
  if (!article.value) return
  articleSnapshot.value = JSON.parse(JSON.stringify(article.value))
  blocks.value = parseBodyToBlocks(article.value.title, article.value.body)
  modifiedIndices.value = new Set()
  isEditing.value = true
}

const onBodyClick = (e) => {
  if (isEditing.value) return
  const target = e.target
  const editableTags = ['H1', 'H2', 'H3', 'P', 'LI', 'DIV']
  if (editableTags.includes(target.tagName) && target.closest('.article-body')) {
    enterEditMode()
    nextTick(() => {
      target.focus()
    })
  }
}

const onPaste = (e) => {
  e.preventDefault()
  const text = (e.clipboardData || window.clipboardData).getData('text/plain')
  document.execCommand('insertText', false, text)
}

const onBlockInput = (idx, e) => {
  blocks.value[idx].html = e.target.innerHTML
  modifiedIndices.value.add(idx)
}

const renderBlockHtml = (block) => {
  if (block.type === BLOCK_TYPES.TITLE) {
    return `<h1 style="font-size:24px;font-weight:700;margin-bottom:16px;line-height:1.4;color:#1a1a1a;">${block.html}</h1>`
  }
  if (block.type === BLOCK_TYPES.HEADING) {
    return `<h2 style="font-size:18px;font-weight:600;color:#1a1a1a;margin:24px 0 12px;">${block.html}</h2>`
  }
  if (block.type === BLOCK_TYPES.LIST_ITEM) {
    return `<li style="margin-bottom:8px;">${block.html}</li>`
  }
  if (block.type === BLOCK_TYPES.HIGHLIGHT) {
    return `<div style="background:#f6ffed;border-left:4px solid #07c160;padding:16px;margin:20px 0;border-radius:0 8px 8px 0;">${block.html}</div>`
  }
  return `<p style="margin-bottom:16px;">${block.html}</p>`
}

const saveEdit = () => {
  const titleBlock = blocks.value.find(b => b.type === BLOCK_TYPES.TITLE)
  if (!titleBlock || !stripHtml(titleBlock.html).trim()) {
    message.error('标题不能为空')
    return
  }

  const { title, body } = serializeBlocksToArticle(blocks.value)
  const updated = { ...article.value, title, body }

  if (!saveCurrentArticle(updated)) {
    message.error('保存失败，请检查浏览器存储权限')
    return
  }

  syncArticleToQueue(updated)
  article.value = updated
  isEditing.value = false
  modifiedIndices.value = new Set()
  message.success('内容已保存')
}

const cancelEdit = () => {
  if (articleSnapshot.value) {
    article.value = articleSnapshot.value
  }
  isEditing.value = false
  modifiedIndices.value = new Set()
  blocks.value = []
}

function stripHtml(html) {
  if (!html) return ''
  const tmp = document.createElement('div')
  tmp.innerHTML = html
  return tmp.textContent || tmp.innerText || ''
}

// 生成模拟meta
const generateMeta = () => {
  const descOptions = [
    '深度解析时间管理的核心技巧，帮助你从忙碌中解脱出来',
    '学会这5个时间管理方法，让你的效率提升300%',
    '自律从管理时间开始，这篇文章告诉你如何做到'
  ]
  const tagOptions = ['时间管理', '效率提升', '自我管理', '职场成长', '习惯养成', '目标规划']
  publishDesc.value = descOptions[Math.floor(Math.random() * descOptions.length)]
  publishTags.value = tagOptions.sort(() => Math.random() - 0.5).slice(0, 4)
}

// 格式化正文
const formattedBody = computed(() => {
  if (!article.value?.body) return ''
  return article.value.body
    .replace(/\n\n/g, '</p><p style="margin-bottom: 16px;">')
    .replace(/\n/g, '<br>')
    .replace(/^/, '<p style="margin-bottom: 16px;">')
    .replace(/$/, '</p>')
    .replace(/【([^】]+)】/g, '<h2 style="font-size: 18px; font-weight: 600; color: #1a1a1a; margin: 24px 0 12px;">$1</h2>')
})

// 复制正文
const copyText = () => {
  if (!article.value) return
  const text = `${article.value.title}\n\n${article.value.body}`
  navigator.clipboard.writeText(text).then(() => {
    message.success('已复制到剪贴板')
  }).catch(() => {
    message.error('复制失败')
  })
}

// 导出 Word
const exportWord = () => {
  if (!article.value) return

  const title = article.value.title || '未命名文章'
  const bodyHtml = formattedBody.value

  const html = `
    <html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:w="urn:schemas-microsoft-com:office:word" xmlns="http://www.w3.org/1999/xhtml">
      <head>
        <meta charset="UTF-8">
        <title>${title}</title>
      </head>
      <body style="font-family: -apple-system, BlinkMacSystemFont, sans-serif; padding: 40px; color: #262626;">
        <h1 style="font-size: 24px; margin-bottom: 16px; line-height: 1.4; color: #1a1a1a;">${title}</h1>
        <div style="font-size: 16px; line-height: 1.8;">${bodyHtml}</div>
      </body>
    </html>
  `

  const blob = new Blob(['\ufeff', html], { type: 'application/msword' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = title.replace(/[\\/:*?"<>|]/g, '_') + '.doc'
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)

  message.success('Word 导出成功')
}

// 复制描述
const copyDesc = () => {
  navigator.clipboard.writeText(publishDesc.value).then(() => {
    message.success('描述已复制')
  }).catch(() => {
    message.error('复制失败')
  })
}

// 换一版描述
const regenerateDesc = () => {
  generateMeta()
  message.success('已生成新描述')
}

// 复制全部标签
const copyTags = () => {
  navigator.clipboard.writeText(publishTags.value.join(' ')).then(() => {
    message.success('标签已复制')
  }).catch(() => {
    message.error('复制失败')
  })
}

// 换一批标签
const regenerateTags = () => {
  const allTags = ['时间管理', '效率提升', '自我管理', '职场成长', '习惯养成', '目标规划', '专注力', '计划执行']
  publishTags.value = allTags.sort(() => Math.random() - 0.5).slice(0, 4)
  message.success('已生成新标签')
}

// AI优化标题
const optimizeTitle = () => {
  if (!article.value) return
  titleOptVisible.value = true
  selectedTitle.value = ''
  titleOptSetIndex.value = 0
  currentPlatform.value = 'wechat'
  animateItems.value = false
}

const closeTitleOpt = () => {
  titleOptVisible.value = false
  selectedTitle.value = ''
}

const refreshTitles = () => {
  refreshing.value = true
  animateItems.value = true
  setTimeout(() => {
    titleOptSetIndex.value = (titleOptSetIndex.value + 1) % titleOptSets.length
    refreshing.value = false
  }, 600)
}

const selectTitle = (title) => {
  selectedTitle.value = title
}

const confirmTitle = () => {
  if (!selectedTitle.value || !article.value) return
  article.value.title = selectedTitle.value
  // 同步更新本地存储，方便返回后标题一致
  localStorage.setItem('aichuangzuo_current_article', JSON.stringify(article.value))
  message.success('标题已替换')
  closeTitleOpt()
}

// 生成贴图（打开复用弹框）
const generateCards = () => {
  cardsModalVisible.value = true
}


onMounted(() => {
  loadArticle()
})
</script>

<style scoped>
.preview-index {
  height: 100%;
  padding: 24px;
  padding-bottom: 80px;
  overflow-y: auto;
}

.preview-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.back-btn {
  padding: 6px 12px;
  border: 1px solid #d9d9d9;
  background: #fff;
  border-radius: 6px;
  font-size: 13px;
  color: #595959;
  cursor: pointer;
}

.back-btn:hover {
  border-color: #ff2442;
  color: #ff2442;
}

.preview-title-text {
  flex: 1;
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0;
  line-height: 1.2;
}

.preview-header-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: 1px solid #d9d9d9;
  background: #fff;
  border-radius: 6px;
  font-size: 13px;
  color: #595959;
  cursor: pointer;
  transition: all 0.2s;
}

.action-btn:hover {
  border-color: #ff2442;
  color: #ff2442;
}

.action-btn.primary {
  background: #ff2442;
  border-color: #ff2442;
  color: #fff;
}

.action-btn.primary:hover {
  background: #e61e3a;
  border-color: #e61e3a;
}

.preview-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.empty-text {
  font-size: 14px;
  color: #8c8c8c;
  margin-bottom: 16px;
}

.empty-btn {
  padding: 8px 20px;
  background: #ff2442;
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
}

.empty-btn:hover {
  background: #e61e3a;
}

.preview-content {
  max-width: 680px;
  margin: 0 auto;
}

.preview-article {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 32px;
}

.article-title {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 16px;
  line-height: 1.4;
}

.article-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #8c8c8c;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #eee;
  flex-wrap: wrap;
}

.article-style-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  background: #fff0f2;
  color: #ff2442;
  border: 1px solid #ffccc7;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 500;
}

.article-body {
  font-size: 16px;
  line-height: 1.8;
  color: #262626;
}

.article-body h2 {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 24px 0 12px;
}

.publish-meta-card {
  max-width: 680px;
  margin: 32px auto 0;
  padding-top: 28px;
  border-top: 1px solid #f0f0f0;
}

.meta-section-title {
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 12px;
  font-size: 15px;
}

.publish-desc-input {
  width: 100%;
  min-height: 90px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  padding: 12px;
  font-size: 14px;
  line-height: 1.6;
  color: #262626;
  resize: vertical;
  box-sizing: border-box;
  font-family: inherit;
}

.publish-desc-input:focus {
  outline: none;
  border-color: #ff2442;
}

.meta-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 10px;
}

.meta-btn {
  padding: 6px 14px;
  background: #fff;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  font-size: 13px;
  color: #595959;
  cursor: pointer;
  transition: all 0.2s;
}

.meta-btn:hover {
  border-color: #ff2442;
  color: #ff2442;
}

.meta-btn.primary {
  background: #fff;
  border-color: #ff2442;
  color: #ff2442;
}

.meta-btn.primary:hover {
  background: #fff0f2;
}

.publish-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.publish-tag {
  padding: 4px 12px;
  background: #f5f5f5;
  border-radius: 14px;
  font-size: 13px;
  color: #595959;
}

/* 浮动操作栏 */
.floating-action-bar {
  position: fixed;
  bottom: 0;
  left: 200px;
  right: 0;
  background: #fff;
  border-top: 1px solid #eee;
  box-shadow: 0 -4px 16px rgba(0, 0, 0, 0.06);
  padding: 10px 24px;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  z-index: 100;
}

.float-btn {
  padding: 8px 16px;
  background: #fff;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 13px;
  color: #595959;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.15s;
}

.float-btn:hover {
  border-color: #ff2442;
  color: #ff2442;
}

.float-btn.primary {
  background: #ff2442;
  border-color: #ff2442;
  color: #fff;
}

.float-btn.primary:hover {
  background: #e61e3a;
}

.float-btn.outline {
  background: #fff;
  border-color: #ff2442;
  color: #ff2442;
}

.float-btn.outline:hover {
  background: #fff0f2;
}

.float-btn.danger {
  background: #fff;
  border-color: #ff4d4f;
  color: #ff4d4f;
}

.float-btn.danger:hover {
  background: #fff2f0;
}

/* AI 标题优化弹窗 */
.title-opt-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  z-index: 1000;
}

.title-opt-box {
  background: #fff;
  border-radius: 16px;
  width: 100%;
  max-width: 560px;
  max-height: 86vh;
  overflow-y: auto;
  padding: 28px;
  position: relative;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
}

.title-opt-close {
  position: absolute;
  top: 12px;
  right: 16px;
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  font-size: 24px;
  color: #8c8c8c;
  cursor: pointer;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.title-opt-close:hover {
  color: #262626;
  background: #f5f5f5;
}

.title-opt-header {
  font-size: 18px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 20px;
}

.title-opt-section {
  margin-bottom: 24px;
}

.title-opt-original {
  font-size: 13px;
  color: #8c8c8c;
  background: #fff0f2;
  border-radius: 8px;
  padding: 12px 14px;
  margin-bottom: 16px;
  line-height: 1.5;
}

.title-opt-original span {
  color: var(--color-primary);
  font-weight: 600;
  margin-right: 8px;
}

.title-opt-section-label {
  font-size: 13px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.title-opt-refresh {
  font-size: 12px;
  color: var(--color-primary);
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: all 0.2s;
}

.title-opt-refresh:hover {
  background: #fff0f2;
}

.title-opt-refresh:disabled {
  color: #bfbfbf;
  cursor: not-allowed;
}

.title-opt-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.title-opt-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s;
}

.title-opt-item:hover {
  border-color: var(--color-primary);
  background: #fff0f2;
}

.title-opt-item.selected {
  border-color: var(--color-primary);
  background: #fff0f2;
}

.title-opt-radio {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: 2px solid #d9d9d9;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}

.title-opt-item.selected .title-opt-radio {
  border-color: var(--color-primary);
  background: var(--color-primary);
}

.title-opt-item.selected .title-opt-radio::after {
  content: '';
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #fff;
}

.title-opt-item-text {
  flex: 1;
  font-size: 14px;
  color: #262626;
  line-height: 1.5;
}

.title-opt-platform-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.title-opt-platform-tab {
  padding: 6px 14px;
  border: 1px solid #d9d9d9;
  background: #fff;
  border-radius: 16px;
  font-size: 13px;
  color: #595959;
  cursor: pointer;
  transition: all 0.15s;
  font-weight: 500;
}

.title-opt-platform-tab:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.title-opt-platform-tab.active {
  background: var(--color-primary);
  color: #fff;
  border-color: var(--color-primary);
}

.title-opt-footer {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.title-opt-btn-cancel {
  padding: 8px 20px;
  background: #fff;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  font-size: 13px;
  color: #595959;
  cursor: pointer;
  transition: all 0.2s;
}

.title-opt-btn-cancel:hover {
  border-color: #ff2442;
  color: #ff2442;
}

.title-opt-btn-confirm {
  padding: 8px 20px;
  background: var(--color-primary);
  border: none;
  border-radius: 6px;
  font-size: 13px;
  color: #fff;
  cursor: pointer;
  font-weight: 600;
  transition: all 0.2s;
}

.title-opt-btn-confirm:hover {
  background: var(--color-primary-hover);
}

.title-opt-btn-confirm:disabled {
  background: #d9d9d9;
  cursor: not-allowed;
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
  .title-opt-box {
    padding: 20px 16px 16px;
    border-radius: 12px;
    max-height: 80vh;
  }

  .title-opt-header {
    font-size: 16px;
  }
}

.title-opt-list .title-opt-item {
  animation: title-opt-item-in 0.3s ease both;
}

@keyframes title-opt-item-in {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}


.editing-body .edit-block {
  outline: none;
  border: 1px solid transparent;
  border-radius: 6px;
  padding: 4px;
  transition: border-color 0.2s;
}

.editing-body .edit-block:focus,
.editing-body .edit-block:focus-within {
  border-color: #07c160;
  box-shadow: 0 0 0 2px rgba(7, 193, 96, 0.15);
}

.editing-body .edit-block.modified {
  background: #f6ffed;
}

.edit-floating-bar {
  position: fixed;
  bottom: 0;
  left: 200px;
  right: 0;
  background: #fff;
  border-top: 1px solid #eee;
  box-shadow: 0 -4px 16px rgba(0, 0, 0, 0.06);
  padding: 10px 24px;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  z-index: 100;
}

.edit-hint {
  font-size: 13px;
  color: #595959;
  margin-right: 8px;
}

@media (max-width: 768px) {
  .edit-floating-bar {
    left: 64px;
  }
}

/* 深色模式 */
body[data-theme="dark"] .back-btn {
  background: #2a2a2a;
  border-color: #434343;
  color: #a6a6a6;
}

body[data-theme="dark"] .back-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

body[data-theme="dark"] .preview-title-text {
  color: #f0f0f0;
}

body[data-theme="dark"] .action-btn {
  background: #2a2a2a;
  border-color: #434343;
  color: #a6a6a6;
}

body[data-theme="dark"] .action-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

body[data-theme="dark"] .action-btn.primary {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: #fff;
}

body[data-theme="dark"] .action-btn.primary:hover {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
}

body[data-theme="dark"] .empty-text {
  color: #a6a6a6;
}

body[data-theme="dark"] .preview-article {
  background: #1f1f1f;
  border-color: #303030;
}

body[data-theme="dark"] .article-title {
  color: #f0f0f0;
}

body[data-theme="dark"] .article-meta {
  color: #a6a6a6;
  border-bottom-color: #303030;
}

body[data-theme="dark"] .article-style-badge {
  background: rgba(7, 193, 96, 0.15);
  border-color: rgba(7, 193, 96, 0.35);
  color: #4ade80;
}

body[data-theme="dark"] .article-body {
  color: #d9d9d9;
}

body[data-theme="dark"] .article-body h2 {
  color: #f0f0f0;
}

body[data-theme="dark"] .publish-meta-card {
  border-top-color: #303030;
}

body[data-theme="dark"] .meta-section-title {
  color: #f0f0f0;
}

body[data-theme="dark"] .publish-desc-input {
  background: #2a2a2a;
  border-color: #434343;
  color: #f0f0f0;
}

body[data-theme="dark"] .publish-desc-input:focus {
  border-color: var(--color-primary);
}

body[data-theme="dark"] .meta-btn {
  background: #2a2a2a;
  border-color: #434343;
  color: #a6a6a6;
}

body[data-theme="dark"] .meta-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

body[data-theme="dark"] .meta-btn.primary {
  background: transparent;
  border-color: var(--color-primary);
  color: var(--color-primary);
}

body[data-theme="dark"] .meta-btn.primary:hover {
  background: rgba(255, 36, 66, 0.15);
}

body[data-theme="dark"] .publish-tag {
  background: #2a2a2a;
  color: #a6a6a6;
}

body[data-theme="dark"] .floating-action-bar {
  background: #1f1f1f;
  border-top-color: #303030;
  box-shadow: 0 -4px 16px rgba(0, 0, 0, 0.4);
}

body[data-theme="dark"] .float-btn {
  background: #2a2a2a;
  border-color: #434343;
  color: #a6a6a6;
}

body[data-theme="dark"] .float-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

body[data-theme="dark"] .float-btn.primary {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: #fff;
}

body[data-theme="dark"] .float-btn.primary:hover {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
}

body[data-theme="dark"] .float-btn.outline {
  background: transparent;
  border-color: var(--color-primary);
  color: var(--color-primary);
}

body[data-theme="dark"] .float-btn.outline:hover {
  background: rgba(255, 36, 66, 0.15);
}

body[data-theme="dark"] .float-btn.danger {
  background: transparent;
  border-color: #ff4d4f;
  color: #ff4d4f;
}

body[data-theme="dark"] .float-btn.danger:hover {
  background: rgba(255, 77, 79, 0.15);
}

body[data-theme="dark"] .edit-floating-bar {
  background: #1f1f1f;
  border-color: #303030;
  box-shadow: 0 -4px 16px rgba(0, 0, 0, 0.4);
}

body[data-theme="dark"] .edit-hint {
  color: #a6a6a6;
}

/* AI 标题优化弹框（自定义，被 teleport 到 body） */
body[data-theme="dark"] .title-opt-overlay {
  background: rgba(0, 0, 0, 0.6);
}

body[data-theme="dark"] .title-opt-box {
  background: #1f1f1f;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5);
}

body[data-theme="dark"] .title-opt-close {
  color: #a6a6a6;
}

body[data-theme="dark"] .title-opt-close:hover {
  background: #2a2a2a;
  color: #f0f0f0;
}

body[data-theme="dark"] .title-opt-header {
  color: #f0f0f0;
}

body[data-theme="dark"] .title-opt-original {
  background: #2a2a2a;
  color: #f0f0f0;
}

body[data-theme="dark"] .title-opt-original span {
  color: #a6a6a6;
}

body[data-theme="dark"] .title-opt-section-label {
  color: #f0f0f0;
}

body[data-theme="dark"] .title-opt-refresh {
  color: var(--color-primary);
}

body[data-theme="dark"] .title-opt-refresh:hover {
  background: rgba(255, 36, 66, 0.15);
}

body[data-theme="dark"] .title-opt-refresh:disabled {
  color: #737373;
}

body[data-theme="dark"] .title-opt-item {
  background: #1f1f1f;
  border-color: #303030;
}

body[data-theme="dark"] .title-opt-item:hover {
  background: #2a2a2a;
  border-color: var(--color-primary);
}

body[data-theme="dark"] .title-opt-item.selected {
  background: rgba(255, 36, 66, 0.15);
  border-color: var(--color-primary);
}

body[data-theme="dark"] .title-opt-radio {
  border-color: #434343;
}

body[data-theme="dark"] .title-opt-item.selected .title-opt-radio {
  border-color: var(--color-primary);
  background: var(--color-primary);
  box-shadow: inset 0 0 0 3px #1f1f1f;
}

body[data-theme="dark"] .title-opt-item-text {
  color: #f0f0f0;
}

body[data-theme="dark"] .title-opt-platform-tab {
  background: #2a2a2a;
  border-color: #434343;
  color: #a6a6a6;
}

body[data-theme="dark"] .title-opt-platform-tab:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

body[data-theme="dark"] .title-opt-platform-tab.active {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: #fff;
}

body[data-theme="dark"] .title-opt-footer {
  border-top-color: #303030;
}

body[data-theme="dark"] .title-opt-btn-cancel {
  background: #2a2a2a;
  border-color: #434343;
  color: #a6a6a6;
}

body[data-theme="dark"] .title-opt-btn-cancel:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

body[data-theme="dark"] .title-opt-btn-confirm {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: #fff;
}

body[data-theme="dark"] .title-opt-btn-confirm:hover {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
}

body[data-theme="dark"] .title-opt-btn-confirm:disabled {
  background: #434343;
  border-color: #434343;
  color: #737373;
}

body[data-theme="dark"] .editing-body .edit-block {
  border-color: #434343;
}
</style>