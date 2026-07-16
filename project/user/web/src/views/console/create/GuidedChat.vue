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

        <!-- 步骤 1：主题输入 + 灵感胶囊 -->
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
          <TopicCapsules @apply="onTopicCapsule" />
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
                <div class="effect-line">· 默认模板：{{ defaultTemplateName(option.raw) }}</div>
                <div class="effect-line">· {{ option.raw.trait }}</div>
              </div>
              <!-- 风格效果卡 -->
              <div v-else class="effect-card">
                <div class="effect-title">{{ option.label }}</div>
                <div class="effect-line">· {{ option.raw.desc }}</div>
                <div class="effect-line effect-prompt">· {{ option.raw.promptSummary }}</div>
              </div>
            </template>
          </QuickReplies>
        </template>

        <!-- 步骤 4：确认卡片 -->
        <template v-else-if="m.kind === 'confirm'">
          <div class="confirm-card">
            <div class="confirm-title">📄 {{ customTitle }}</div>
            <div class="confirm-meta">
              {{ currentPlatform.name }} · {{ currentWordCount.count }} 字 · {{ currentStyle?.name || '默认风格' }}
            </div>
            <div class="confirm-meta">模板：{{ currentTemplate?.name }}</div>
            <div class="confirm-quota">本次消耗 1 次 · 剩余 {{ quotaRemaining }} 次</div>
            <div class="confirm-actions">
              <button class="confirm-generate" @click="handleConfirmGenerate(m)">⚡ 开始生成</button>
              <button class="confirm-edit" @click="editTopic">改主题</button>
              <button class="confirm-edit" @click="editConfig">改配置</button>
            </div>
          </div>
        </template>
      </ChatMessage>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, nextTick } from 'vue'
import ChatMessage from './ChatMessage.vue'
import QuickReplies from './QuickReplies.vue'
import TopicCapsules from './TopicCapsules.vue'
import { platforms, wordCountPresets, useCreateForm } from './useCreateForm.js'
import { systemStyles, currentStyle, applyStyle } from '@/composables/useStyles.js'
import { useExportTemplates } from '@/composables/useExportTemplates.js'
import { useBenefits } from '@/composables/useBenefits.js'

const {
  setCreateMode, customTitle, customRequirement,
  currentPlatform, currentWordCount, selectedTemplateKey,
  platformVisible
} = useCreateForm()
const { templates: apiTemplates } = useExportTemplates()
const { benefits } = useBenefits()

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

// 初始化：第一条 AI 消息（额度拦截在 Task 7 加）
push({ role: 'ai', kind: 'topic' })

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

// 平台确认后自动带默认配置：推荐字数 + 平台默认模板
const applyPlatformDefault = (p) => {
  const presets = wordCountPresets.platform[p.key] || wordCountPresets.platform.general
  const wc = presets.find(x => x.count === p.recommendWords) || presets[0]
  currentWordCount.value = { count: wc.count, label: wc.label, desc: wc.desc || '' }
  const t = apiTemplates.value.find(x => x.key.startsWith(p.key)) || apiTemplates.value[0]
  if (t) selectedTemplateKey.value = t.key
}

const onQuickConfirm = (m, opt) => {
  m.done = true
  if (m.optionsType === 'platform') {
    currentPlatform.value = opt.raw
    applyPlatformDefault(opt.raw)
    push({ role: 'user', kind: 'text', text: opt.label })
    askStyle()
  } else if (m.optionsType === 'style') {
    applyStyle(opt.raw)
    push({ role: 'user', kind: 'text', text: opt.label })
    push({ role: 'ai', kind: 'confirm' })
  }
}

const platformTraitWordLabel = (p) => {
  const presets = wordCountPresets.platform[p.key] || []
  return presets.find(x => x.count === p.recommendWords)?.label || '标准'
}

const defaultTemplateName = (p) => {
  const t = apiTemplates.value.find(x => x.key.startsWith(p.key)) || apiTemplates.value[0]
  return t?.name || '默认'
}

const editTopic = () => {
  editingTopic.value = true
  push({ role: 'ai', kind: 'topic' })
}

const editConfig = () => {
  platformVisible.value = true
}

// Task 7：生成 + 进度卡片
const handleConfirmGenerate = () => {}
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

@media (max-width: 768px) {
  .topic-input-row {
    position: sticky;
    bottom: 0;
    background: var(--color-bg-page);
    padding: 8px 0;
  }
}
</style>
