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

        <!-- 平台/风格快捷回复（Task 6 填充 preview 内容） -->
        <template v-else-if="m.kind === 'quick'">
          <div class="chat-question">{{ m.text }}</div>
          <QuickReplies :options="m.options" @confirm="(opt) => onQuickConfirm(m, opt)" />
        </template>
      </ChatMessage>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import ChatMessage from './ChatMessage.vue'
import QuickReplies from './QuickReplies.vue'
import TopicCapsules from './TopicCapsules.vue'
import { platforms, useCreateForm } from './useCreateForm.js'

const { setCreateMode, customTitle, currentPlatform } = useCreateForm()

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

const submitTopic = (text) => {
  const title = (text || '').trim()
  if (!title) return
  customTitle.value = title
  push({ role: 'user', kind: 'text', text: title })
  topicInput.value = ''
  askPlatform()
}

const onTopicCapsule = (topic) => {
  // TopicCapsules 已把标题/概要写入 customTitle/customRequirement
  push({ role: 'user', kind: 'text', text: topic.title })
  askPlatform()
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

const onQuickConfirm = (m, opt) => {
  if (m.optionsType === 'platform') {
    currentPlatform.value = opt.raw
    push({ role: 'user', kind: 'text', text: opt.label })
    // Task 6：askStyle()
  }
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

@media (max-width: 768px) {
  .topic-input-row {
    position: sticky;
    bottom: 0;
    background: var(--color-bg-page);
    padding: 8px 0;
  }
}
</style>
