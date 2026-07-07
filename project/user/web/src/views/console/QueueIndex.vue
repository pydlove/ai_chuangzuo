<template>
  <div class="queue-index">
    <div class="queue-header">
      <h2 class="queue-title">我的作品</h2>
      <button class="queue-create-btn" @click="$router.push('/console/create')">+ 去创作</button>
    </div>

    <div class="queue-list">
      <div v-if="worksList.length === 0" class="queue-empty">
        <div class="empty-icon">📝</div>
        <div class="empty-text">还没有生成的文章</div>
        <button class="empty-btn" @click="$router.push('/console/create')">去创作</button>
      </div>

      <div v-else class="works-cards">
        <div
          v-for="work in worksList"
          :key="work.id"
          class="work-card"
        >
          <div class="work-info">
            <div class="work-title">{{ work.title }}</div>
            <div class="work-meta">
              {{ work.completedAt }} · 约 {{ work.wordCount }} 字 · {{ work.template }}
            </div>
          </div>
          <div class="work-actions">
            <button class="work-action-btn" @click="previewArticle(work)">预览</button>
            <button class="work-action-btn" @click="exportArticle(work)">导出</button>
            <button class="work-action-btn danger" @click="deleteWork(work.id)">删除</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 预览导出弹框 -->
    <a-modal
      v-model:open="previewVisible"
      :footer="null"
      :width="800"
      centered
      class="preview-modal"
    >
      <template #title>
        <span class="modal-title">预览/导出</span>
      </template>

      <div v-if="currentArticle" class="preview-modal-content">
        <div class="preview-article">
          <h1 class="article-title">{{ currentArticle.title }}</h1>
          <div class="article-meta">
            <span>{{ currentArticle.completedAt || '' }}</span>
            <span>·</span>
            <span>约 {{ currentArticle.wordCount }} 字</span>
            <span class="article-style-badge">
              风格:{{ currentArticle.style || '专业严谨' }}
            </span>
          </div>
          <div class="article-body" v-html="formattedBody"></div>
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

        <!-- 浮动操作栏 -->
        <div class="floating-action-bar">
          <button class="float-btn" @click="optimizeTitle">
            ✧ AI 优化标题
          </button>
          <button class="float-btn primary" @click="handleExportWord">
            导出 Word
          </button>
          <button class="float-btn outline" @click="copyText">
            复制正文
          </button>
          <button class="float-btn danger" @click="generateCards">
            生成贴图
          </button>
        </div>
      </div>
    </a-modal>

    <!-- AI 标题优化弹窗 -->
    <Teleport to="body">
      <Transition name="title-opt-fade">
        <div v-if="titleOptVisible" class="title-opt-overlay" @click.self="closeTitleOpt">
          <div class="title-opt-box">
            <button class="title-opt-close" @click="closeTitleOpt">&times;</button>
            <div class="title-opt-header">AI 标题优化</div>

            <div class="title-opt-section">
              <div class="title-opt-original">
                <span>原标题</span>{{ currentArticle.title }}
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

    <CardsModal v-model:visible="cardsModalVisible" :article="currentArticle" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import CardsModal from '@/components/CardsModal.vue'

const router = useRouter()

const WORKS_KEY = 'aichuangzuo_generation_queue'

// 作品列表（只显示已完成的）
const worksList = ref([])

// 预览弹框
const previewVisible = ref(false)
const currentArticle = ref(null)
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

// 加载作品列表
const loadWorks = () => {
  const saved = localStorage.getItem(WORKS_KEY)
  if (saved) {
    try {
      const queue = JSON.parse(saved)
      // 只显示已完成的，按完成时间倒序
      worksList.value = queue
        .filter(item => item.status === 'completed')
        .map(item => ({
          id: item.id,
          title: item.title,
          platform: item.platform,
          wordCount: item.wordCount,
          template: item.template || '未选择',
          completedAt: formatDate(item.completedAt),
          content: item.content
        }))
        .sort((a, b) => new Date(b.id) - new Date(a.id))
    } catch (e) {
      worksList.value = []
    }
  }
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const year = d.getFullYear()
  const month = (d.getMonth() + 1).toString().padStart(2, '0')
  const day = d.getDate().toString().padStart(2, '0')
  const hour = d.getHours().toString().padStart(2, '0')
  const min = d.getMinutes().toString().padStart(2, '0')
  return `${year}-${month}-${day} ${hour}:${min}`
}

// 预览文章（跳转页面）
const previewArticle = (work) => {
  localStorage.setItem('aichuangzuo_current_article', JSON.stringify(work.content))
  router.push('/console/preview')
}

// 导出文章（弹框）
const exportArticle = (work) => {
  currentArticle.value = {
    ...work.content,
    completedAt: work.completedAt,
    wordCount: work.wordCount,
    style: work.content?.style || '专业严谨'
  }
  generateMeta()
  previewVisible.value = true
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
  if (!currentArticle.value?.body) return ''
  return currentArticle.value.body
    .replace(/\n\n/g, '</p><p style="margin-bottom: 16px;">')
    .replace(/\n/g, '<br>')
    .replace(/^/, '<p style="margin-bottom: 16px;">')
    .replace(/$/, '</p>')
    .replace(/【([^】]+)】/g, '<h2 style="font-size: 18px; font-weight: 600; color: #1a1a1a; margin: 24px 0 12px;">$1</h2>')
})

// 复制正文
const copyText = () => {
  if (!currentArticle.value) return
  const text = `${currentArticle.value.title}\n\n${currentArticle.value.body}`
  navigator.clipboard.writeText(text).then(() => {
    message.success('已复制到剪贴板')
  }).catch(() => {
    message.error('复制失败')
  })
}

// 导出 Word
const handleExportWord = () => {
  if (!currentArticle.value) return

  const title = currentArticle.value.title || '未命名文章'
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
  if (!currentArticle.value) return
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
  if (!selectedTitle.value || !currentArticle.value) return
  currentArticle.value.title = selectedTitle.value
  message.success('标题已替换')
  closeTitleOpt()
}

// 生成贴图（打开复用弹框）
const generateCards = () => {
  cardsModalVisible.value = true
}


// 删除作品
const deleteWork = (id) => {
  const queue = JSON.parse(localStorage.getItem(WORKS_KEY) || '[]')
  const filtered = queue.filter(item => item.id !== id)
  localStorage.setItem(WORKS_KEY, JSON.stringify(filtered))
  worksList.value = worksList.value.filter(w => w.id !== id)
  message.success('删除成功')
}

onMounted(() => {
  loadWorks()
})
</script>

<style scoped>
.queue-index {
  display: flex;
  flex-direction: column;
}

.queue-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.queue-title {
  font-size: 22px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0;
}

.queue-create-btn {
  padding: 10px 20px;
  background: #ff2442;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
}

.queue-create-btn:hover {
  background: #e61e3a;
}

.queue-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
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

.works-cards {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.work-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.work-info {
  flex: 1;
}

.work-title {
  font-weight: 600;
  font-size: 16px;
  color: #1a1a1a;
  margin-bottom: 6px;
}

.work-meta {
  color: #8c8c8c;
  font-size: 13px;
}

.work-actions {
  display: flex;
  gap: 8px;
}

.work-action-btn {
  padding: 8px 14px;
  background: #fff;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  font-size: 13px;
  color: #262626;
  cursor: pointer;
  transition: all 0.2s;
}

.work-action-btn:hover {
  border-color: #ff2442;
  color: #ff2442;
}

.work-action-btn.danger {
  color: #ff4d4f;
}

.work-action-btn.danger:hover {
  border-color: #ff4d4f;
  color: #ff4d4f;
}

/* 弹框样式 */
.modal-title {
  font-size: 18px;
  font-weight: 600;
}

.preview-modal-content {
  max-height: 70vh;
  overflow-y: auto;
  padding-bottom: 80px;
}

.preview-article {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 24px;
}

.article-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 12px;
  line-height: 1.4;
}

.article-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #8c8c8c;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #eee;
  flex-wrap: wrap;
}

.article-style-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  background: #f6ffed;
  color: #389e0d;
  border: 1px solid #b7eb8f;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 500;
}

.article-body {
  font-size: 15px;
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
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #f0f0f0;
  position: relative;
}

.meta-section-title {
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 12px;
  font-size: 15px;
}

.publish-desc-input {
  width: 100%;
  min-height: 80px;
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
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
  border-top: 1px solid #eee;
  box-shadow: 0 -4px 16px rgba(0, 0, 0, 0.06);
  padding: 10px 16px;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
  margin-top: 20px;
}

.float-btn {
  padding: 8px 14px;
  background: #fff;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
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
  background: #f6ffed;
  border-radius: 8px;
  padding: 12px 14px;
  margin-bottom: 16px;
  line-height: 1.5;
}

.title-opt-original span {
  color: #07c160;
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
  color: #07c160;
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: all 0.2s;
}

.title-opt-refresh:hover {
  background: #f6ffed;
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
  border-color: #95de64;
  background: #fcfff6;
}

.title-opt-item.selected {
  border-color: #07c160;
  background: #f6ffed;
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
  border-color: #07c160;
  background: #07c160;
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
  border-color: #07c160;
  color: #07c160;
}

.title-opt-platform-tab.active {
  background: #07c160;
  color: #fff;
  border-color: #07c160;
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
  background: #07c160;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  color: #fff;
  cursor: pointer;
  font-weight: 600;
  transition: all 0.2s;
}

.title-opt-btn-confirm:hover {
  background: #06ad56;
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


/* 深色模式 */
body[data-theme="dark"] .work-card {
  background: #1f1f1f;
  border-color: #303030;
  box-shadow: none;
}

body[data-theme="dark"] .work-title {
  color: #f0f0f0;
}

body[data-theme="dark"] .work-meta {
  color: #a6a6a6;
}

body[data-theme="dark"] .work-action-btn {
  background: #2a2a2a;
  border-color: #434343;
  color: #a6a6a6;
}

body[data-theme="dark"] .work-action-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: #333;
}

body[data-theme="dark"] .work-action-btn.danger {
  color: #ff4d4f;
  background: rgba(255, 77, 79, 0.15);
  border-color: rgba(255, 77, 79, 0.3);
}
</style>

<style>
/* 弹框全局样式覆盖 */
.preview-modal .ant-modal-body {
  padding: 20px;
}
</style>