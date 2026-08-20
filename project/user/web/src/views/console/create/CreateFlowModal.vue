<template>
  <a-modal
    :open='visible'
    :title='modalTitle'
    width='900px'
    :footer='null'
    @cancel='close'
  >
    <div class='create-flow'>
      <div class='flow-header'>
        <div class='flow-loading' v-if='loading'>
          <a-spin />
          <span>正在为你制定今日创作任务...</span>
        </div>
      </div>

      <div class='flow-steps'>
        <div class='flow-line-bg'></div>
        <div
          class='flow-line-progress'
          :style='{ width: ((flowData.step - 1) / (steps.length - 1) * 100) + "%" }'
        ></div>
        <div
          v-for='(s, idx) in steps'
          :key='idx'
          :class='["flow-step", { active: flowData.step >= idx + 1, current: flowData.step === idx + 1 }]'
        >
          <div class='flow-step-num'>{{ idx + 1 }}</div>
          <div class='flow-step-title'>{{ s.title }}</div>
        </div>
      </div>

      <div v-if='flowData.step === 1' class='flow-panel'>
        <div class='panel-title'>第一步：选择今日创作方向</div>
        <div class='panel-desc'>基于你的「{{ plan?.niche || '运营方案' }}」方案，从低粉高赞案例中挑了 {{ topicOptions.length }} 个选题</div>
        <div class='topic-options'>
          <div
            v-for='topic in topicOptions'
            :key='topic.id'
            :class='["topic-option", { selected: flowData.selectedTopic?.id === topic.id }]'
            @click='selectTopic(topic)'
          >
            <div class='topic-option-title'>{{ topic.title }}</div>
            <div class='topic-option-meta'>
              <a-tag :color='riskColor(topic.risk)'>{{ topic.riskLabel }}</a-tag>
              <span><FireOutlined /> {{ topic.caseCount }} 篇案例</span>
            </div>
            <div class='topic-option-angle'>
              <BulbOutlined /> 推荐角度：{{ topic.recommendedAngle }}
            </div>
          </div>
        </div>
      </div>

      <div v-if='flowData.step === 2' class='flow-panel'>
        <div class='panel-title'>第二步：确定文章观点</div>
        <div class='panel-desc'>围绕标题写作，为你生成了 {{ generatedAngleList.length }} 个观点角度，建议最多选择 3 个组合使用；选中后可点击「编辑」修改成你的表达</div>
        <div class='angle-options'>
          <div
            v-for='angle in generatedAngleList'
            :key='angle.id'
            :class='["angle-option", { selected: isAngleSelected(angle.id), editing: editingAngleId === angle.id }]'
            @click='toggleAngle(angle)'
          >
            <a-checkbox :checked='isAngleSelected(angle.id)' class='angle-check' @click.stop />
            <div v-if='editingAngleId !== angle.id' class='angle-text'>{{ angle.text }}</div>
            <a-input
              v-else
              v-model:value='angle.text'
              size='small'
              class='angle-edit-input'
              @click.stop
              @pressEnter='editingAngleId = null'
            />
            <a-button
              v-if='isAngleSelected(angle.id)'
              type='link'
              size='small'
              class='angle-edit-btn'
              @click.stop='toggleEdit(angle)'
            >
              {{ editingAngleId === angle.id ? '完成' : '编辑' }}
            </a-button>
          </div>
        </div>
        <div class='angle-summary' v-if='flowData.selectedAngleIds.length'>
          已选 {{ flowData.selectedAngleIds.length }}/3 个观点：{{ selectedAngleTexts.join('；') }}
        </div>
      </div>

      <div v-if='flowData.step === 3' class='flow-panel'>
        <div class='panel-title'>第三步：选择文章字数</div>
        <div class='panel-desc'>平台推荐 vs 自定义，会员可设置更高上限</div>
        <div class='word-presets'>
          <a-button
            v-for='p in wordPresets'
            :key='p.value'
            :type='flowData.wordCount === p.value ? "primary" : "default"'
            @click='flowData.wordCount = p.value'
          >
            {{ p.label }}
          </a-button>
        </div>
        <div class='word-slider'>
          <span>自定义：</span>
          <a-slider v-model:value='flowData.wordCount' :min='500' :max='memberMaxWords' :step='100' style='flex: 1' />
          <span class='word-count'>{{ flowData.wordCount }} 字</span>
        </div>
        <div class='word-limit-tip'>当前会员等级字数上限：{{ memberMaxWords }} 字</div>
      </div>

      <div v-if='flowData.step === 4' class='flow-panel'>
        <div class='panel-title'>第四步：选择创作提示词</div>
        <a-alert
          class='prompt-market-tip'
          type='info'
          show-icon
          :message='"提示词不够顺手？去提示词市场逛逛，收藏你常用的风格，下次一键调用。"'
        />
        <a-tabs v-model:activeKey='flowData.promptTab'>
          <a-tab-pane key='mine' tab='我的'>
            <div class='prompt-options'>
              <div
                v-for='(prompt, idx) in minePrompts'
                :key='idx'
                :class='["prompt-option", { selected: flowData.selectedPrompt === prompt }]'
                @click='selectPrompt(prompt)'
              >
                {{ prompt }}
              </div>
              <div v-if='!minePrompts.length' class='prompt-empty'>你还没有保存自己的提示词，可在「学习」或「系统」里先选一个</div>
            </div>
          </a-tab-pane>
          <a-tab-pane key='learn' tab='学习'>
            <div class='prompt-options'>
              <div
                v-for='(prompt, idx) in learnPrompts'
                :key='idx'
                :class='["prompt-option", { selected: flowData.selectedPrompt === prompt }]'
                @click='selectPrompt(prompt)'
              >
                {{ prompt }}
              </div>
            </div>
          </a-tab-pane>
          <a-tab-pane key='favorite' tab='收藏'>
            <div class='prompt-options'>
              <div
                v-for='(prompt, idx) in favoritePrompts'
                :key='idx'
                :class='["prompt-option", { selected: flowData.selectedPrompt === prompt }]'
                @click='selectPrompt(prompt)'
              >
                {{ prompt }}
              </div>
              <div v-if='!favoritePrompts.length' class='prompt-empty'>还没有收藏提示词，去提示词市场发现更多好风格</div>
            </div>
          </a-tab-pane>
          <a-tab-pane key='system' tab='系统'>
            <div class='prompt-options'>
              <div
                v-for='(prompt, idx) in systemPrompts'
                :key='idx'
                :class='["prompt-option", { selected: flowData.selectedPrompt === prompt }]'
                @click='selectPrompt(prompt)'
              >
                {{ prompt }}
              </div>
            </div>
          </a-tab-pane>
        </a-tabs>
      </div>

      <div v-if='flowData.step === 5' class='flow-panel'>
        <div class='panel-title'>第五步：选择导出模板</div>
        <div class='template-platform-tabs'>
          <button
            v-for='tab in templatePlatformTabs'
            :key='tab.key'
            :class='["template-platform-tab", { active: templatePlatformTab === tab.key }]'
            @click='templatePlatformTab = tab.key'
          >
            {{ tab.label }}
          </button>
        </div>
        <div class='template-grid'>
          <div
            v-for='t in filteredTemplates'
            :key='t.key'
            :class='["template-card", { selected: flowData.selectedTemplate === t.key, locked: !t.accessible }]'
            @click='selectTemplate(t)'
          >
            <div v-if='!t.accessible' class='template-card-badge'>升级可用</div>
            <div class='template-card-name'>{{ t.name }}</div>
            <div class='template-card-desc'>{{ t.desc }}</div>
          </div>
        </div>
      </div>

      <div class='flow-footer'>
        <a-button v-if='flowData.step > 1' size='large' :disabled='loading || submitting' @click='prevStep'>上一步</a-button>
        <a-button
          v-if='flowData.step < 5'
          type='primary'
          size='large'
          :disabled='!canNext || loading || submitting'
          :loading='loading'
          @click='nextStep'
        >
          下一步
        </a-button>
        <a-button
          v-if='flowData.step === 5'
          type='primary'
          size='large'
          :disabled='!canNext || loading || submitting'
          :loading='submitting'
          @click='finish'
        >
          生成文章
        </a-button>
      </div>
    </div>
  </a-modal>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { FireOutlined, BulbOutlined } from '@ant-design/icons-vue'
import {
  getRecommendedCreationSession,
  generateRecommendedTopics,
  generateRecommendedAngles,
  updateRecommendedSession,
  submitRecommendedGeneration
} from '@/api/recommendedCreation.js'

const props = defineProps({
  visible: Boolean,
  plan: Object
})

const emit = defineEmits(['update:visible', 'success'])

const loading = ref(false)
const submitting = ref(false)
const steps = [
  { title: '选题', desc: '选择创作方向' },
  { title: '观点', desc: '确定文章观点' },
  { title: '字数', desc: '设置文章长度' },
  { title: '提示词', desc: '选择创作提示词' },
  { title: '模板', desc: '选择导出模板' }
]

const flowData = reactive({
  step: 1,
  selectedTopic: null,
  selectedAngleIds: [],
  wordCount: 1500,
  promptTab: 'system',
  selectedPrompt: '',
  customPrompt: '',
  selectedTemplate: ''
})

const editingAngleId = ref(null)
const memberMaxWords = 3000

const topicOptions = ref([])
const generatedAngleList = ref([])
const lastGeneratedTopicId = ref('')

const wordPresets = [
  { label: '小红书笔记（800字）', value: 800 },
  { label: '公众号文章（1500字）', value: 1500 },
  { label: '头条号（1200字）', value: 1200 },
  { label: '深度长文（2500字）', value: 2500 }
]

const minePrompts = [
  '我的默认风格：用亲身经历+反同质化角度写干货',
  '我的小红书风格：开头抓人、中段有料、结尾引导互动'
]

const learnPrompts = [
  '职场转型：突出真实复盘和数据',
  '副业变现：强调收入拆解和路径',
  '个人成长：用反鸡汤视角写干货'
]

const favoritePrompts = [
  '爆款标题拆解：先写 5 个标题，再选最抓人的一个展开'
]

const systemPrompts = [
  '用亲身经历+反同质化角度，写一篇有信息增量的文章',
  '用清单体结构，给出可执行的步骤和避坑建议',
  '用故事化叙述，增强可读性和转发欲',
  '用对比论证，突出观点差异和认知冲击'
]

const templatePlatformTabs = [
  { key: 'all', label: '全部' },
  { key: 'wechat', label: '公众号' },
  { key: 'xiaohongshu', label: '小红书' },
  { key: 'toutiao', label: '头条' },
  { key: 'baijiahao', label: '百家号' },
  { key: 'zhihu', label: '知乎' },
  { key: 'douyin', label: '抖音' },
  { key: 'general', label: '通用' }
]
const templatePlatformTab = ref('all')

const templates = ref([
  { key: 'wechat-default', name: '公众号默认', desc: '适合深度长文，标题醒目，段落清晰', platform: 'wechat', accessible: true },
  { key: 'wechat-story', name: '公众号故事体', desc: '故事化叙述，增强可读性', platform: 'wechat', accessible: true },
  { key: 'xiaohongshu-default', name: '小红书默认', desc: 'emoji 分段，适合快速阅读', platform: 'xiaohongshu', accessible: true },
  { key: 'xiaohongshu-list', name: '小红书清单体', desc: '清单+痛点，互动率高', platform: 'xiaohongshu', accessible: false },
  { key: 'toutiao-default', name: '今日头条默认', desc: '资讯体结构，开头抓人', platform: 'toutiao', accessible: true },
  { key: 'baijiahao-default', name: '百家号默认', desc: '搜索友好，关键词布局', platform: 'baijiahao', accessible: true },
  { key: 'zhihu-default', name: '知乎回答', desc: '问答体，强调专业背书', platform: 'zhihu', accessible: true },
  { key: 'douyin-default', name: '抖音文案', desc: '短视频脚本结构，节奏快', platform: 'douyin', accessible: true },
  { key: 'general-default', name: '通用文章', desc: '通用排版，适配多平台', platform: 'general', accessible: true }
])

const filteredTemplates = computed(() => {
  if (templatePlatformTab.value === 'all') return templates.value
  return templates.value.filter(t => t.platform === templatePlatformTab.value)
})

const selectedAngleTexts = computed(() => {
  return flowData.selectedAngleIds
    .map(id => generatedAngleList.value.find(a => a.id === id))
    .filter(Boolean)
    .map(a => a.text)
})

const modalTitle = computed(() => `今日创作 · ${steps[flowData.step - 1].title}`)

function riskColor(risk) {
  return risk === 'low' ? 'success' : risk === 'medium' ? 'warning' : 'error'
}

function selectTopic(topic) {
  flowData.selectedTopic = topic
}

function isAngleSelected(id) {
  return flowData.selectedAngleIds.includes(id)
}

function toggleAngle(angle) {
  if (editingAngleId.value === angle.id) return
  const idx = flowData.selectedAngleIds.indexOf(angle.id)
  if (idx > -1) {
    flowData.selectedAngleIds.splice(idx, 1)
    if (editingAngleId.value === angle.id) editingAngleId.value = null
  } else {
    if (flowData.selectedAngleIds.length >= 3) {
      message.warning('最多选择 3 个观点进行组合')
      return
    }
    flowData.selectedAngleIds.push(angle.id)
  }
}

function toggleEdit(angle) {
  if (editingAngleId.value === angle.id) {
    editingAngleId.value = null
  } else {
    if (!isAngleSelected(angle.id)) return
    editingAngleId.value = angle.id
  }
}

function selectPrompt(prompt) {
  flowData.selectedPrompt = prompt
}

function selectTemplate(t) {
  if (!t.accessible) {
    message.info('该模板当前套餐不可用，请升级套餐后使用')
    return
  }
  flowData.selectedTemplate = t.key
}

const canNext = computed(() => {
  if (flowData.step === 1) return !!flowData.selectedTopic
  if (flowData.step === 2) return flowData.selectedAngleIds.length > 0 && flowData.selectedAngleIds.length <= 3
  if (flowData.step === 3) return flowData.wordCount >= 500
  if (flowData.step === 4) return !!flowData.selectedPrompt
  if (flowData.step === 5) return !!flowData.selectedTemplate
  return true
})

async function nextStep() {
  if (!canNext.value || loading.value || submitting.value) return

  let ok = true
  if (flowData.step === 1) {
    await ensureAnglesForSelectedTopic()
    if (!generatedAngleList.value.length) return
  } else if (flowData.step === 2) {
    const selectedAngles = flowData.selectedAngleIds
      .map(id => generatedAngleList.value.find(a => a.id === id))
      .filter(Boolean)
    ok = await persistStep(3, { selectedAngles })
  } else if (flowData.step === 3) {
    ok = await persistStep(4, { wordCount: flowData.wordCount })
  } else if (flowData.step === 4) {
    ok = await persistStep(5, { prompt: flowData.selectedPrompt })
  }

  if (ok) {
    flowData.step++
  }
}

function prevStep() {
  if (flowData.step > 1) {
    flowData.step--
  }
}

async function finish() {
  if (!canNext.value || submitting.value) return
  submitting.value = true
  try {
    const task = await submitRecommendedGeneration()
    message.success('已加入生成队列')
    emit('success', task)
    close()
  } catch (err) {
    message.error(err?.message || '提交生成失败，请重试')
  } finally {
    submitting.value = false
  }
}

function close() {
  emit('update:visible', false)
}

function resetLocalState() {
  flowData.step = 1
  flowData.selectedTopic = null
  flowData.selectedAngleIds = []
  flowData.wordCount = 1500
  flowData.promptTab = 'system'
  flowData.selectedPrompt = ''
  flowData.customPrompt = ''
  flowData.selectedTemplate = ''
  templatePlatformTab.value = 'all'
  editingAngleId.value = null
  topicOptions.value = []
  generatedAngleList.value = []
  lastGeneratedTopicId.value = ''
}

function applySession(session) {
  flowData.step = Math.max(1, Math.min(5, session.currentStep || 1))
  topicOptions.value = session.topics || []
  flowData.selectedTopic = session.selectedTopic || null
  generatedAngleList.value = session.angles || []
  lastGeneratedTopicId.value = session.selectedTopic?.id || ''

  if (session.selectedAngles?.length) {
    flowData.selectedAngleIds = session.selectedAngles.map(a => a.id)
    session.selectedAngles.forEach(sel => {
      const existing = generatedAngleList.value.find(a => a.id === sel.id)
      if (existing) {
        existing.text = sel.text
      } else {
        generatedAngleList.value.push(sel)
      }
    })
  } else {
    flowData.selectedAngleIds = []
  }

  flowData.wordCount = session.wordCount || 1500
  flowData.selectedPrompt = session.prompt || ''
  flowData.selectedTemplate = session.template || ''
  editingAngleId.value = null
}

async function ensureAnglesForSelectedTopic() {
  const topic = flowData.selectedTopic
  if (!topic) return

  if (lastGeneratedTopicId.value === topic.id && generatedAngleList.value.length) {
    return
  }

  loading.value = true
  try {
    const angles = await generateRecommendedAngles(topic.id)
    generatedAngleList.value = angles || []
    flowData.selectedAngleIds = []
    editingAngleId.value = null
    lastGeneratedTopicId.value = topic.id
    if (!generatedAngleList.value.length) {
      message.warning('未生成到观点，请重试')
    }
  } catch (err) {
    message.error(err?.message || '生成观点失败，请重试')
    generatedAngleList.value = []
  } finally {
    loading.value = false
  }
}

async function persistStep(step, extra = {}) {
  try {
    await updateRecommendedSession({ currentStep: step, ...extra })
    return true
  } catch (err) {
    message.error(err?.message || '保存创作进度失败，请重试')
    return false
  }
}

async function initSession() {
  loading.value = true
  try {
    const session = await getRecommendedCreationSession()
    if (session) {
      applySession(session)
    } else {
      resetLocalState()
      const topics = await generateRecommendedTopics()
      topicOptions.value = topics || []
      if (!topicOptions.value.length) {
        message.warning('未生成到选题，请重试')
        close()
      }
    }
  } catch (err) {
    message.error(err?.message || '加载创作任务失败，请重试')
    close()
  } finally {
    loading.value = false
  }
}

watch(() => props.visible, (val) => {
  if (val) {
    initSession()
  }
})
</script>

<style scoped>
.create-flow {
  padding: 8px;
}
.flow-header {
  min-height: 40px;
  margin-bottom: 8px;
}
.flow-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #FF2442;
  font-size: 14px;
}
.flow-steps {
  position: relative;
  display: flex;
  justify-content: space-between;
  margin-bottom: 24px;
  padding-bottom: 16px;
}
.flow-line-bg,
.flow-line-progress {
  position: absolute;
  top: 14px;
  left: 28px;
  right: 28px;
  height: 2px;
}
.flow-line-bg {
  background: #f0f0f0;
}
.flow-line-progress {
  background: #FF2442;
  transition: width 0.3s;
}
.flow-step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  flex: 1;
  position: relative;
  z-index: 1;
}
.flow-step-num {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  color: #8c8c8c;
  transition: all 0.3s;
}
.flow-step.active .flow-step-num {
  background: #FF2442;
  color: #fff;
}
.flow-step-title {
  font-size: 13px;
  color: #8c8c8c;
}
.flow-step.active .flow-step-title {
  color: #FF2442;
  font-weight: 500;
}
.flow-panel {
  min-height: 320px;
}
.panel-title {
  font-size: 18px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 8px;
}
.panel-desc {
  font-size: 14px;
  color: #8c8c8c;
  margin-bottom: 20px;
}
.topic-options {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.topic-option {
  border: 1px solid #e0e0e0;
  border-radius: 10px;
  padding: 14px;
  cursor: pointer;
  transition: all 0.2s;
}
.topic-option:hover {
  border-color: #FF2442;
}
.topic-option.selected {
  border-color: #FF2442;
  background: rgba(255, 36, 66, 0.04);
}
.topic-option-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 8px;
}
.topic-option-meta {
  display: flex;
  gap: 12px;
  font-size: 13px;
  color: #8c8c8c;
  margin-bottom: 6px;
}
.topic-option-angle {
  font-size: 13px;
  color: #595959;
}
.angle-options {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 16px;
}
.angle-option {
  display: flex;
  align-items: center;
  gap: 10px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 10px 12px;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 14px;
  color: #595959;
}
.angle-option:hover {
  border-color: #FF2442;
}
.angle-option.selected {
  border-color: #FF2442;
  background: rgba(255, 36, 66, 0.04);
  color: #1a1a1a;
}
.angle-option.editing {
  background: #fffbe6;
  border-color: #faad14;
}
.angle-check {
  flex-shrink: 0;
}
.angle-text {
  flex: 1;
  line-height: 1.5;
}
.angle-edit-input {
  flex: 1;
}
.angle-edit-btn {
  flex-shrink: 0;
  padding: 0 4px;
}
.angle-summary {
  padding: 10px 12px;
  background: #f6ffed;
  border: 1px solid #b7eb8f;
  border-radius: 8px;
  font-size: 13px;
  color: #237804;
  line-height: 1.6;
}
.word-presets {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}
.word-slider {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}
.word-count {
  min-width: 60px;
  text-align: right;
  font-weight: 600;
  color: #FF2442;
}
.word-limit-tip {
  font-size: 13px;
  color: #8c8c8c;
}
.prompt-market-tip {
  margin-bottom: 16px;
}
.prompt-options {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 12px;
}
.prompt-option {
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 12px;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 14px;
  color: #595959;
}
.prompt-option:hover {
  border-color: #FF2442;
}
.prompt-option.selected {
  border-color: #FF2442;
  background: rgba(255, 36, 66, 0.04);
}
.prompt-empty {
  padding: 24px;
  text-align: center;
  color: #8c8c8c;
  font-size: 14px;
}
.template-platform-tabs {
  display: flex;
  gap: 8px;
  padding: 0 0 14px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 16px;
  overflow-x: auto;
}
.template-platform-tab {
  padding: 6px 14px;
  border-radius: 16px;
  border: 1px solid #d9d9d9;
  background: #fff;
  color: #595959;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;
}
.template-platform-tab.active {
  border-color: #ff2442;
  background: #fff0f2;
  color: #ff2442;
  font-weight: 600;
}
.template-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}
.template-card {
  position: relative;
  border: 1px solid #e0e0e0;
  border-radius: 10px;
  padding: 14px;
  cursor: pointer;
  transition: all 0.2s;
}
.template-card:hover {
  border-color: #FF2442;
}
.template-card.selected {
  border-color: #FF2442;
  background: rgba(255, 36, 66, 0.04);
}
.template-card.locked {
  cursor: not-allowed;
  background: #f5f5f5;
}
.template-card.locked:hover {
  border-color: #e0e0e0;
}
.template-card.locked .template-card-name,
.template-card.locked .template-card-desc {
  opacity: 0.55;
}
.template-card-badge {
  position: absolute;
  top: 6px;
  right: 6px;
  padding: 1px 6px;
  border-radius: 10px;
  font-size: 10px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, #ff9a4d, #ff2442);
}
.template-card-name {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 6px;
}
.template-card-desc {
  font-size: 12px;
  color: #8c8c8c;
  line-height: 1.5;
}
.flow-footer {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}
@media (max-width: 768px) {
  .template-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .word-presets {
    flex-direction: column;
  }
}
</style>
