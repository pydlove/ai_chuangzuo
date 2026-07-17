<template>
  <GuidedHero v-if="isHeroState" @submit="onHeroSubmit" />

  <div v-else class="guided-chat">
    <div class="guided-topbar">
      <h2 class="create-title">开始创作</h2>
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
          <textarea
            v-model="requirementInput"
            class="topic-requirement"
            placeholder="你希望文章突出什么观点？或想从什么角度展开？（可选）"
            rows="2"
            maxlength="500"
          ></textarea>
          <div class="topic-requirement-hint">{{ (requirementInput || '').length }} / 500</div>
          <TopicSuggestionBubble @select="onTopicCapsule" />
        </template>

        <!-- 平台/风格快捷回复（两段式：点选 → 效果卡 → 确认；确认后收起） -->
        <template v-else-if="m.kind === 'quick'">
          <div class="chat-question">{{ m.text }}</div>
          <QuickReplies v-if="!m.done" :options="m.options" :selected-key="chipSelectedKey(m)" @select="(opt) => onChipSelect(m, opt)" @confirm="(opt) => onQuickConfirm(m, opt)">
            <!-- 风格步骤额外提供"新建我的风格"入口 -->
            <template v-if="m.optionsType === 'style'" #footer>
              <button class="quick-create-style" @click="openCreateStyle(m)">＋ 新建我的风格</button>
            </template>
            <template #preview="{ option }">
              <!-- 平台效果卡 -->
              <div v-if="m.optionsType === 'platform'" class="effect-card">
                <div class="effect-title">{{ option.label }}</div>
                <div class="effect-line">· 推荐 {{ option.raw.recommendWords }} 字，{{ platformTraitWordLabel(option.raw) }}</div>
                <div class="effect-line">· 平台特性：{{ option.raw.trait }}</div>
              </div>
              <!-- 风格效果卡：显示来源标签 + 完整 prompt 预览 -->
              <div v-else-if="m.optionsType === 'style'" class="effect-card effect-card-style">
                <div class="effect-title-row">
                  <span class="effect-title">{{ option.label }}</span>
                  <span class="style-tag" :class="`tag-${option.raw.sourceType}`">{{ option.raw.tag }}</span>
                </div>
                <div class="effect-line">· {{ option.raw.desc || '自定义风格' }}</div>
                <div v-if="option.raw.scope" class="effect-line">· 适用：{{ option.raw.scope }}</div>
                <div class="effect-prompt-block">
                  <div class="effect-prompt-label">提示词预览：</div>
                  <pre class="effect-prompt-full">{{ option.raw.prompt || option.raw.promptSummary || '（无）' }}</pre>
                </div>
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
            <div class="confirm-title">{{ customTitle }}</div>
            <div v-if="customRequirement" class="confirm-requirement">{{ customRequirement }}</div>
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
              <button class="confirm-edit" @click="handleSaveDraft">保存草稿</button>
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
            <div class="confirm-title">{{ customTitle }}</div>
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
            <div class="confirm-title">{{ customTitle }}</div>
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
import { ref, computed, nextTick, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import ChatMessage from './ChatMessage.vue'
import QuickReplies from './QuickReplies.vue'
import TopicSuggestionBubble from './TopicSuggestionBubble.vue'
import GuidedHero from './GuidedHero.vue'
import { platforms, wordCountPresets, useCreateForm } from './useCreateForm.js'
import { useGenerationQueue } from './useGenerationQueue.js'
import { systemStyles, myStyles, learnedStyles, currentStyle, applyStyle } from '@/composables/useStyles.js'
import { favoriteStyles } from '@/composables/useStyleMarket.js'
import { useExportTemplates } from '@/composables/useExportTemplates.js'
import { useBenefits } from '@/composables/useBenefits.js'
import { submitGeneration, getGenerationTask, retryGenerationTask } from '@/api/generation.js'
import { saveDraft } from '@/api/draft.js'

const router = useRouter()
const {
  createMode, customTitle, customRequirement,
  currentPlatform, currentWordCount, selectedTemplateKey,
  templateVisible, styleVisible
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
const requirementInput = ref('')  // 主题步骤的可选描述输入（与 customRequirement 双向同步）
const msgListEl = ref(null)

const scrollToBottom = async () => {
  await nextTick()
  // chat-list 不再自己滚动；找到最近的滚动容器（console-content）并滚到底部
  const el = msgListEl.value
  if (!el) return
  let scrollEl = el
  while (scrollEl && scrollEl !== document.body) {
    const style = getComputedStyle(scrollEl)
    if (style.overflowY === 'auto' || style.overflowY === 'scroll') {
      break
    }
    scrollEl = scrollEl.parentElement
  }
  if (scrollEl) scrollEl.scrollTop = scrollEl.scrollHeight
}

const push = (msg) => {
  messages.value.push({ id: ++seq, ...msg })
  scrollToBottom()
}

// 初始化：先确保权益已加载，再决定首屏（额度拦截走对话态，否则走 Kimi 风格 hero）
onMounted(async () => {
  await loadBenefits()
  if (quotaTotal.value <= 0) {
    push({ role: 'ai', kind: 'quota', text: '开通会员后才能使用 AI 生成文章', actionText: '去开通会员', action: () => router.push('/pricing') })
  } else if (quotaRemaining.value <= 0) {
    push({ role: 'ai', kind: 'quota', text: '本月额度已用完，升级会员可获得更多额度', actionText: '去升级', action: () => router.push('/pricing') })
  }
})

const isHeroState = computed(() => messages.value.length === 0)

// hero → 进入聊天态：先回显用户输入，再展示主题步骤（让用户能补描述，再去平台）
// 如果用户在 hero 里点了灵感，customRequirement 已经被 applyTopic 写好，不要清
const onHeroSubmit = (text) => {
  customTitle.value = text
  topicInput.value = text
  requirementInput.value = customRequirement.value || ''  // 同步灵感带过来的观点
  push({ role: 'user', kind: 'text', text })
  push({ role: 'ai', kind: 'topic' })
}

// 改主题模式：答完直接回确认卡（平台/风格已答保留）
const editingTopic = ref(false)

const submitTopic = (text) => {
  const title = (text || '').trim()
  if (!title) return
  customTitle.value = title
  customRequirement.value = (requirementInput.value || '').trim()
  push({ role: 'user', kind: 'text', text: title })
  topicInput.value = ''
  requirementInput.value = ''
  afterTopic()
}

const onTopicCapsule = (topic) => {
  // TopicCapsules 已把标题/概要写入 customTitle/customRequirement
  // 把概要同步到本地输入框，用户点灵感后进入主题步骤看到完整观点，再点发送确认
  requirementInput.value = topic.summary || ''
  topicInput.value = topic.title  // 也回填标题，万一用户想微调
  push({ role: 'user', kind: 'text', text: topic.title })
  push({ role: 'ai', kind: 'topic' })
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

// 合并 4 个来源：我的 > 学习 > 收藏 > 系统（用户自己的优先）
const collectStyleOptions = () => {
  const tagFor = (sourceType) => {
    if (sourceType === 'my') return '我的'
    if (sourceType === 'learned') return '学习'
    if (sourceType === 'favorite') return '收藏'
    return '系统'
  }
  const seen = new Set()
  const options = []
  const push1 = (s, sourceType) => {
    if (!s || !s.name) return
    if (seen.has(s.name)) return
    seen.add(s.name)
    options.push({ key: s.name, label: s.name, raw: { ...s, sourceType, tag: tagFor(sourceType) } })
  }
  myStyles.value.forEach(s => push1(s, 'my'))
  learnedStyles.value.forEach(s => push1(s, 'learned'))
  favoriteStyles.value.forEach(s => push1(s, 'favorite'))
  systemStyles.value.forEach(s => push1(s, 'system'))
  return options
}

const askStyle = () => {
  push({
    role: 'ai',
    kind: 'quick',
    text: '想要什么风格？',
    optionsType: 'style',
    options: collectStyleOptions()
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

// chip 选中态：模板步骤需要和 selectedTemplateKey 双向同步（弹框里改了 → 外面 chip 跟着变）
const chipSelectedKey = (m) => {
  return m.optionsType === 'template' ? selectedTemplateKey.value : null
}
const onChipSelect = (m, opt) => {
  if (m.optionsType === 'template') {
    selectedTemplateKey.value = opt.key  // 用户在 chat 里点了别的 → 弹框下次打开也是新的
  }
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
  // 把当前标题/描述回填到输入框，用户可改
  topicInput.value = customTitle.value
  requirementInput.value = customRequirement.value
  push({ role: 'ai', kind: 'topic' })
}

// 风格步骤：从 chip 列表里的「＋ 新建我的风格」打开弹框；关闭后用最新数据原地刷新当前问题
const openCreateStyle = (m) => {
  m.done = true  // 弹框期间禁用选项，避免用户乱点
  const stop = watch(styleVisible, (open) => {
    if (open) return  // 只关心关闭事件
    stop()
    // 复用当前消息：m.done 复位 + 用最新的 myStyles 重算 options（保留位置，不新增气泡）
    m.options = collectStyleOptions()
    m.done = false
    scrollToBottom()
  })
  styleVisible.value = true
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
  options: collectStyleOptions(),
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

const handleSaveDraft = async () => {
  if (!customTitle.value.trim()) {
    message.warning('请输入文章标题')
    return
  }
  try {
    await saveDraft({
      customTitle: customTitle.value,
      customRequirement: customRequirement.value,
      platform: currentPlatform.value?.name,
      wordCount: currentWordCount.value?.count,
      style: currentStyle.value?.name,
      template: currentTemplate.value?.name,
      createMode: createMode.value
    })
    message.success('草稿已保存')
  } catch (e) {
    message.error(e?.message || '保存草稿失败，请稍后重试')
  }
}

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
  /* 不再自己滚动：让外层 console-content 接管滚动，滚轮因此落到屏幕最右边缘。
     内容保持 720px 居中显示（由 .guided-chat 的 max-width 保证） */
  padding: 4px 0;
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

/* 主题步骤的可选描述输入框 */
.topic-requirement {
  width: 100%;
  border: 1px solid var(--color-border-default);
  border-radius: 12px;
  padding: 8px 14px;
  font-family: inherit;
  font-size: 13px;
  line-height: 1.6;
  color: var(--color-text-regular);
  background: var(--color-bg-card);
  outline: none;
  resize: vertical;
  margin-bottom: 4px;
  transition: border-color 0.2s;
}
.topic-requirement:focus {
  border-color: var(--color-primary);
}
.topic-requirement::placeholder {
  color: var(--color-text-placeholder);
}

.topic-requirement-hint {
  text-align: right;
  font-size: 11px;
  color: var(--color-text-placeholder);
  margin-bottom: 10px;
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

/* 风格效果卡：标题 + 来源标签 */
.effect-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.effect-title-row .effect-title {
  margin-bottom: 0;
}

.style-tag {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 500;
  line-height: 1.4;
}

.style-tag.tag-my { background: #fff0f2; color: #ff2442; }
.style-tag.tag-learned { background: #f0f5ff; color: #2f54eb; }
.style-tag.tag-favorite { background: #fff7e6; color: #fa8c16; }
.style-tag.tag-system { background: #f5f5f5; color: #595959; }

/* 完整提示词预览块 */
.effect-prompt-block {
  margin-top: 8px;
  background: rgba(0, 0, 0, 0.03);
  border-radius: 8px;
  padding: 10px 12px;
  max-height: 160px;
  overflow-y: auto;
}

.effect-prompt-label {
  font-size: 11px;
  color: var(--color-text-placeholder);
  margin-bottom: 4px;
}

.effect-prompt-full {
  margin: 0;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 12px;
  line-height: 1.6;
  color: var(--color-text-regular);
  white-space: pre-wrap;
  word-break: break-word;
}

.color-swatch {
  display: inline-block;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  margin: 0 4px;
  vertical-align: middle;
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

/* 风格步骤底部"＋ 新建我的风格"入口 */
.quick-create-style {
  margin-top: 10px;
  border: 1px dashed var(--color-primary);
  background: transparent;
  color: var(--color-primary);
  font-size: 13px;
  padding: 6px 16px;
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.15s;
}

.quick-create-style:hover {
  background: var(--color-primary-light);
}

/* 确认卡片 */
.confirm-card {
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

.confirm-requirement {
  font-size: 13px;
  color: var(--color-text-regular);
  background: var(--color-primary-light);
  border-left: 3px solid var(--color-primary);
  padding: 8px 12px;
  border-radius: 6px;
  margin-bottom: 8px;
  line-height: 1.6;
  word-break: break-word;
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
