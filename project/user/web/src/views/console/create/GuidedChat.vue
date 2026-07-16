<template>
  <div class="guided-chat">
    <div class="guided-topbar">
      <h2 class="create-title">开始创作</h2>
      <button class="topbar-btn" @click="setCreateMode('minimal')">熟手模式 →</button>
    </div>

    <div ref="msgListEl" class="chat-list">
      <ChatMessage v-for="m in messages" :key="m.id" :role="m.role">
        <!-- 纯文本 -->
        <template v-if="m.kind === 'text'">{{ m.text }}</template>

        <!-- 步骤 1：主题输入 + 流式灵感气泡 -->
        <template v-else-if="m.kind === 'topic'">
          <div class="chat-question">想写一篇什么主题的文章？</div>
          <div class="topic-input-row">
            <input
              v-model="topicInput"
              type="text"
              class="topic-input"
              placeholder="输入主题，回车发送…"
              @keyup.enter="submitTopic(topicInput)"
            />
            <button class="topic-send" @click="submitTopic(topicInput)">发送</button>
          </div>
          <TopicSuggestionBubble @select="onTopicCapsule" />
        </template>

        <!-- 平台/风格快捷回复（两段式：点选 → 效果卡 → 确认；确认后收起） -->
        <template v-else-if="m.kind === 'quick'">
          <div class="chat-question">{{ m.text }}</div>
          <QuickReplies v-if="!m.done" :options="m.options" @confirm="(opt) => onQuickConfirm(m, opt)">
            <template #preview="{ option }">
              <!-- 平台效果卡 -->
              <div v-if="m.optionsType === 'platform'" class="effect-card">
                <div class="effect-title">{{ option.label }}</div>
                <div class="effect-line">· 推荐 {{ option.raw.recommendWords }} 字，{{ platformTraitWordLabel(option.raw) }}</div>
                <div class="effect-line">· 平台特性：{{ option.raw.trait }}</div>
              </div>
              <!-- 风格效果卡 -->
              <div v-else-if="m.optionsType === 'style'" class="effect-card">
                <div class="effect-title">{{ option.label }}</div>
                <div class="effect-line">· {{ option.raw.desc }}</div>
                <div class="effect-line effect-prompt">· {{ option.raw.promptSummary }}</div>
              </div>
              <!-- 模板效果卡 -->
              <div v-else-if="m.optionsType === 'template'" class="effect-card">
                <div class="effect-title">{{ option.label }}</div>
                <div class="effect-line">· 平台：{{ platformLabel(option.raw.platform) }}</div>
                <div class="effect-line">· 适用：{{ option.raw.desc || '通用场景' }}</div>
                <div class="effect-line">· 主色：
                  <span class="color-swatch" :style="{ background: option.raw.bgColor || '#fff' }"></span>
                  <span class="color-swatch" :style="{ background: option.raw.textColor || '#1a1a1a' }"></span>
                </div>
                <button class="template-preview-btn" @click="openFullPreview(option.raw)">查看完整预览 →</button>
              </div>
            </template>
          </QuickReplies>
        </template>

        <!-- 步骤 4：确认卡片 -->
        <template v-else-if="m.kind === 'confirm'">
          <div class="confirm-card">
            <div class="confirm-title">📄 {{ customTitle }}</div>
            <div class="confirm-meta">
              {{ currentPlatform?.name || '未选' }} · {{ currentStyle?.name || '默认风格' }} · {{ currentTemplate?.name || '默认模板' }}
            </div>
            <div class="confirm-meta">字数：{{ currentWordCount?.count || 800 }} 字</div>
            <div class="confirm-quota">本次消耗 1 次 · 剩余 {{ quotaRemaining }} 次</div>
            <div class="confirm-actions">
              <button class="confirm-generate" @click="handleConfirmGenerate(m)">⚡ 开始生成</button>
              <button class="confirm-edit" @click="editTopic">改主题</button>
              <button class="confirm-edit" @click="editPlatform">改平台</button>
              <button class="confirm-edit" @click="editStyle">改风格</button>
              <button class="confirm-edit" @click="editTemplate">改模板</button>
            </div>
          </div>
        </template>

        <!-- 额度拦截 -->
        <template v-else-if="m.kind === 'quota'">
          <div class="chat-question">{{ m.text }}</div>
          <button class="confirm-generate" @click="m.action">{{ m.actionText }}</button>
        </template>

        <!-- 进度卡片 -->
        <template v-else-if="m.kind === 'progress'">
          <div class="confirm-card">
            <div class="confirm-title">📄 {{ customTitle }}</div>
            <template v-if="m.status === 'generating'">
              <div class="chat-progress">
                <div class="chat-progress-fill" :style="{ width: Math.min(100, Math.round(m.progress)) + '%' }"></div>
              </div>
              <div class="progress-stage">{{ stageText(m.progress) }} {{ Math.min(100, Math.round(m.progress)) }}%</div>
            </template>
            <template v-else>
              <div class="failed-text">❌ {{ m.failedReason }}</div>
              <div class="confirm-actions">
                <button class="confirm-generate" @click="retryTask(m)">重试</button>
              </div>
            </template>
          </div>
        </template>

        <!-- 结果卡片 -->
        <template v-else-if="m.kind === 'result'">
          <div class="confirm-card">
            <div class="confirm-title">✅ {{ customTitle }}</div>
            <div class="confirm-meta">已生成完成</div>
            <div class="confirm-actions">
              <button class="confirm-generate" @click="router.push('/console/works')">查看文章</button>
              <button class="confirm-edit" @click="restart">再写一篇</button>
            </div>
          </div>
        </template>
      </ChatMessage>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import ChatMessage from './ChatMessage.vue'
import QuickReplies from './QuickReplies.vue'
import TopicSuggestionBubble from './TopicSuggestionBubble.vue'
import { platforms, wordCountPresets, useCreateForm } from './useCreateForm.js'
import { useGenerationQueue } from './useGenerationQueue.js'
import { systemStyles, currentStyle, applyStyle } from '@/composables/useStyles.js'
import { useExportTemplates } from '@/composables/useExportTemplates.js'
import { useBenefits } from '@/composables/useBenefits.js'
import { submitGeneration, getGenerationTask, retryGenerationTask } from '@/api/generation.js'

const router = useRouter()
const {
  setCreateMode, customTitle, customRequirement,
  currentPlatform, currentWordCount, selectedTemplateKey,
  templateVisible
} = useCreateForm()
const { templates: apiTemplates } = useExportTemplates()
const { benefits, loadBenefits } = useBenefits()
const { loadQueue } = useGenerationQueue()

const quotaTotal = computed(() => Number(benefits.value['ai_article_quota']?.value) || 0)
const quotaRemaining = computed(() => benefits.value['ai_article_quota']?.remaining ?? 0)
const currentTemplate = computed(() => apiTemplates.value.find(t => t.key === selectedTemplateKey.value) || apiTemplates.value[0])

let seq = 0
const messages = ref([])
const topicInput = ref('')
const msgListEl = ref(null)

const scrollToBottom = async () => {
  await nextTick()
  const el = msgListEl.value
  if (el) el.scrollTop = el.scrollHeight
}

const push = (msg) => {
  messages.value.push({ id: ++seq, ...msg })
  scrollToBottom()
}

// 初始化：先确保权益已加载，再决定第一条消息（额度拦截）
onMounted(async () => {
  await loadBenefits()
  if (quotaTotal.value <= 0) {
    push({ role: 'ai', kind: 'quota', text: '开通会员后才能使用 AI 生成文章', actionText: '去开通会员', action: () => router.push('/pricing') })
  } else if (quotaRemaining.value <= 0) {
    push({ role: 'ai', kind: 'quota', text: '本月额度已用完，升级会员可获得更多额度', actionText: '去升级', action: () => router.push('/pricing') })
  } else {
    push({ role: 'ai', kind: 'topic' })
  }
})

// 改主题模式：答完直接回确认卡（平台/风格已答保留）
const editingTopic = ref(false)

const submitTopic = (text) => {
  const title = (text || '').trim()
  if (!title) return
  customTitle.value = title
  push({ role: 'user', kind: 'text', text: title })
  topicInput.value = ''
  afterTopic()
}

const onTopicCapsule = (topic) => {
  // TopicCapsules 已把标题/概要写入 customTitle/customRequirement
  push({ role: 'user', kind: 'text', text: topic.title })
  afterTopic()
}

const afterTopic = () => {
  if (editingTopic.value) {
    editingTopic.value = false
    push({ role: 'ai', kind: 'confirm' })
  } else {
    askPlatform()
  }
}

const askPlatform = () => {
  push({
    role: 'ai',
    kind: 'quick',
    text: '准备发哪个平台？',
    optionsType: 'platform',
    options: platforms.map(p => ({ key: p.key, label: p.name, raw: p }))
  })
}

const askStyle = () => {
  push({
    role: 'ai',
    kind: 'quick',
    text: '想要什么风格？',
    optionsType: 'style',
    options: systemStyles.value.slice(0, 6).map(s => ({ key: s.name, label: s.name, raw: s }))
  })
}

const askTemplate = () => {
  push({
    role: 'ai',
    kind: 'quick',
    text: '想用哪种模板渲染？',
    optionsType: 'template',
    options: apiTemplates.value.map(t => ({
      key: t.key, label: t.name, raw: t
    }))
  })
}

// 平台确认后只带默认字数（模板由用户独立选择）
const applyPlatformDefault = (p) => {
  const presets = wordCountPresets.platform[p.key] || wordCountPresets.platform.general
  const wc = presets.find(x => x.count === p.recommendWords) || presets[0]
  currentWordCount.value = { count: wc.count, label: wc.label, desc: wc.desc || '' }
}

const onQuickConfirm = (m, opt) => {
  m.done = true
  if (m.optionsType === 'platform') {
    currentPlatform.value = opt.raw
    applyPlatformDefault(opt.raw)
    push({ role: 'user', kind: 'text', text: opt.label })
    if (m.editingMode) { push({ role: 'ai', kind: 'confirm' }); return }
    askStyle()
  } else if (m.optionsType === 'style') {
    applyStyle(opt.raw)
    push({ role: 'user', kind: 'text', text: opt.label })
    if (m.editingMode) { push({ role: 'ai', kind: 'confirm' }); return }
    askTemplate()
  } else if (m.optionsType === 'template') {
    selectedTemplateKey.value = opt.raw.key
    push({ role: 'user', kind: 'text', text: opt.label })
    push({ role: 'ai', kind: 'confirm' })
  }
}

const platformTraitWordLabel = (p) => {
  const presets = wordCountPresets.platform[p.key] || []
  return presets.find(x => x.count === p.recommendWords)?.label || '标准'
}

const platformLabel = (key) => {
  const p = platforms.find(x => x.key === key)
  return p ? p.name : '通用'
}

const openFullPreview = (tplRaw) => {
  selectedTemplateKey.value = tplRaw.key
  templateVisible.value = true
}

const editTopic = () => {
  editingTopic.value = true
  push({ role: 'ai', kind: 'topic' })
}

// 编辑入口：push 的 quick 消息带 editingMode: true，让 onQuickConfirm 答完不再继续下一问，直接 push confirm
const editPlatform = () => push({
  role: 'ai',
  kind: 'quick',
  text: '准备发哪个平台？',
  optionsType: 'platform',
  options: platforms.map(p => ({ key: p.key, label: p.name, raw: p })),
  editingMode: true
})
const editStyle = () => push({
  role: 'ai',
  kind: 'quick',
  text: '想要什么风格？',
  optionsType: 'style',
  options: systemStyles.value.slice(0, 6).map(s => ({ key: s.name, label: s.name, raw: s })),
  editingMode: true
})
const editTemplate = () => push({
  role: 'ai',
  kind: 'quick',
  text: '想用哪种模板渲染？',
  optionsType: 'template',
  options: apiTemplates.value.map(t => ({ key: t.key, label: t.name, raw: t })),
  editingMode: true
})

// ===== 生成链路 =====
const stageText = (pct) =>
  pct < 30 ? '正在生成大纲…' : pct < 70 ? '正在撰写正文…' : pct < 95 ? '正在排版润色…' : '即将完成…'

let pollTimer = null
const stopTaskPoll = () => { clearInterval(pollTimer); pollTimer = null }
onUnmounted(stopTaskPoll)

const handleConfirmGenerate = async (confirmMsg) => {
  if (!customTitle.value.trim()) return
  try {
    const task = await submitGeneration({
      title: customTitle.value,
      description: customRequirement.value,
      platform: currentPlatform.value?.key || '',
      styleRef: currentStyle.value?.id || currentStyle.value?.name || '',
      wordCount: currentWordCount.value?.count || 800,
      template: currentTemplate.value?.key || 'wechat'
    })
    // 确认卡原地替换为进度卡
    Object.assign(confirmMsg, { kind: 'progress', taskId: task.id, progress: 0, status: 'generating' })
    loadBenefits()
    loadQueue()
    pollTask(confirmMsg, task.id)
  } catch (e) {
    message.error(e?.message || '提交失败，请稍后重试')
  }
}

const pollTask = (msg, taskId) => {
  stopTaskPoll()
  pollTimer = setInterval(async () => {
    try {
      const t = await getGenerationTask(taskId)
      msg.progress = t.progressPct || 0
      if (t.status === 2) {
        stopTaskPoll()
        Object.assign(msg, { kind: 'result', status: 'completed' })
        loadQueue()
      } else if (t.status === 3) {
        stopTaskPoll()
        Object.assign(msg, { status: 'failed', failedReason: t.failedReason || '生成失败' })
        loadQueue()
      }
    } catch { /* 单次轮询失败忽略，下轮继续 */ }
  }, 3000)
}

const retryTask = async (msg) => {
  try {
    await retryGenerationTask(msg.taskId)
    Object.assign(msg, { status: 'generating', progress: 0 })
    pollTask(msg, msg.taskId)
  } catch (e) {
    message.error(e?.message || '重试失败，请稍后再试')
  }
}

const restart = () => {
  stopTaskPoll()
  messages.value = []
  push({ role: 'ai', kind: 'topic' })
}
</script>

<style scoped>
.guided-chat {
  max-width: 720px;
  margin: 0 auto;
  width: 100%;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.guided-topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-shrink: 0;
}

.create-title {
  font-size: 22px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.chat-list {
  flex: 1;
  overflow-y: auto;
  padding: 4px 2px;
}

.chat-question {
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 10px;
}

.topic-input-row {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.topic-input {
  flex: 1;
  border: 1px solid var(--color-border-default);
  border-radius: 18px;
  padding: 8px 16px;
  font-size: 14px;
  outline: none;
  background: var(--color-bg-card);
  color: var(--color-text-primary);
  transition: border-color 0.2s;
}

.topic-input:focus {
  border-color: var(--color-primary);
}

.topic-send {
  border: none;
  background: var(--color-primary);
  color: #fff;
  font-size: 14px;
  padding: 8px 20px;
  border-radius: 18px;
  cursor: pointer;
  transition: background 0.2s;
}

.topic-send:hover {
  background: var(--color-primary-hover);
}

/* 效果预览卡 */
.effect-title {
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 4px;
}

.effect-line {
  color: var(--color-text-secondary);
}

.effect-prompt {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.color-swatch {
  display: inline-block;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  margin: 0 4px;
  vertical-align: middle;
  border: 1px solid var(--color-border-light);
}

.template-preview-btn {
  margin-top: 10px;
  padding: 6px 14px;
  background: var(--color-primary-light);
  border: 1px solid var(--color-primary);
  border-radius: 14px;
  color: var(--color-primary);
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
}

.template-preview-btn:hover {
  background: var(--color-primary);
  color: #fff;
}

/* 确认卡片 */
.confirm-card {
  border: 1px solid var(--color-border-light);
  border-radius: 12px;
  padding: 16px;
  background: var(--color-bg-card);
}

.confirm-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 8px;
  word-break: break-all;
}

.confirm-meta {
  font-size: 13px;
  color: var(--color-text-secondary);
  line-height: 1.8;
}

.confirm-quota {
  font-size: 12px;
  color: var(--color-text-placeholder);
  margin: 8px 0 12px;
}

.confirm-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.confirm-generate {
  border: none;
  background: var(--color-primary);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  padding: 9px 22px;
  border-radius: 18px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition: background 0.2s;
}

.confirm-generate:hover {
  background: var(--color-primary-hover);
}

.confirm-edit {
  border: none;
  background: transparent;
  color: var(--color-text-secondary);
  font-size: 13px;
  cursor: pointer;
  padding: 6px 8px;
}

.confirm-edit:hover {
  color: var(--color-primary);
}

/* 进度卡片 */
.chat-progress {
  height: 6px;
  background: rgba(255, 36, 66, 0.15);
  border-radius: 3px;
  overflow: hidden;
  margin: 12px 0 8px;
}

.chat-progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #ff2442, #ff6b81);
  border-radius: 3px;
  transition: width 0.3s;
}

.progress-stage {
  font-size: 12px;
  color: var(--color-text-secondary);
}

.failed-text {
  color: var(--color-error);
  font-size: 13px;
  margin: 8px 0 12px;
}

@media (max-width: 768px) {
  .topic-input-row {
    position: sticky;
    bottom: 0;
    background: var(--color-bg-page);
    padding: 8px 0;
  }
}
</style>
