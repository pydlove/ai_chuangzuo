<template>
  <a-modal
    :open="open"
    :title="`测试提示词：${prompt?.promptName || ''}`"
    width="820px"
    :footer="null"
    @cancel="handleClose"
  >
    <div class="prompt-test-modal">
      <div class="prompt-test-main">
        <!-- 变量表单 -->
        <a-form layout="vertical" class="prompt-test-form">
          <template v-if="variableList.length > 0">
            <a-form-item
              v-for="v in variableList"
              :key="v.name"
              :label="v.description || v.name"
              :required="v.required"
            >
              <a-textarea
                v-model:value="variables[v.name]"
                :rows="2"
                :placeholder="v.example || `请输入 ${v.name}`"
                allow-clear
              />
            </a-form-item>
          </template>
          <a-empty v-else description="该提示词没有定义变量" />

          <a-form-item>
            <a-space>
              <a-button type="primary" :loading="testing" @click="handleTest">
                测试
              </a-button>
              <a-button @click="handleClose">关闭</a-button>
            </a-space>
          </a-form-item>
        </a-form>

        <!-- 渲染后提示词 -->
        <a-collapse v-model:active-key="activeKeys" class="prompt-test-collapse">
          <a-collapse-panel key="system" header="渲染后的 system role">
            <pre class="prompt-test-code">{{ result?.renderedSystemRole || '暂无' }}</pre>
          </a-collapse-panel>
          <a-collapse-panel key="user" header="渲染后的 user prompt">
            <pre class="prompt-test-code">{{ result?.renderedUserPrompt || '暂无' }}</pre>
          </a-collapse-panel>
        </a-collapse>

        <!-- 结果 -->
        <div v-if="result?.content" class="prompt-test-result">
          <div class="prompt-test-result-header">
            <span class="prompt-test-result-title">AI 输出</span>
            <a-space v-if="tokenText" size="small">
              <a-tag>{{ tokenText }}</a-tag>
            </a-space>
          </div>
          <pre class="prompt-test-output">{{ formattedContent }}</pre>
        </div>
      </div>

      <!-- 历史记录 -->
      <div class="prompt-test-history">
        <div class="prompt-test-history-title">历史记录</div>
        <a-empty v-if="history.length === 0" description="暂无记录" />
        <a-list v-else size="small" :data-source="history">
          <template #renderItem="{ item }">
            <a-list-item class="prompt-test-history-item" @click="restoreHistory(item)"
              >
              <div class="prompt-test-history-row">
                <span class="prompt-test-history-time">{{ formatTime(item.createdAt) }}</span>
                <a-button type="link" danger size="small" @click.stop="removeHistory(item)"
                  >删除</a-button
                >
              </div>
              <div class="prompt-test-history-preview">{{ previewContent(item) }}</div>
            </a-list-item>
          </template>
        </a-list>
      </div>
    </div>
  </a-modal>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { useAiPrompt } from '@/composables/useAiPrompt.js'
import storage from '@/utils/storage.js'

const props = defineProps({
  open: { type: Boolean, default: false },
  prompt: { type: Object, default: () => ({}) }
})

const emit = defineEmits(['update:open', 'close', 'success'])

const { testPrompt } = useAiPrompt()

const testing = ref(false)
const result = ref(null)
const activeKeys = ref([])
const variables = reactive({})

const variableList = computed(() => props.prompt?.variableSchema || [])

const historyKey = computed(() => `ai_prompt_test_history:${props.prompt?.id || 0}`)
const history = ref([])

const tokenText = computed(() => {
  if (!result.value) return ''
  const { promptTokens, completionTokens, totalTokens } = result.value
  if (totalTokens != null) return `token: ${totalTokens}`
  if (promptTokens != null && completionTokens != null) {
    return `prompt ${promptTokens} / completion ${completionTokens}`
  }
  return ''
})

const formattedContent = computed(() => {
  const raw = result.value?.content
  if (!raw) return ''
  try {
    const parsed = JSON.parse(raw)
    return JSON.stringify(parsed, null, 2)
  } catch {
    return raw
  }
})

const loadHistory = () => {
  history.value = storage.get(historyKey.value) || []
}

const saveHistory = () => {
  storage.set(historyKey.value, history.value.slice(0, 20))
}

const resetForm = () => {
  Object.keys(variables).forEach((key) => delete variables[key])
  variableList.value.forEach((v) => {
    variables[v.name] = v.example || ''
  })
}

watch(
  () => props.open,
  (val) => {
    if (val) {
      result.value = null
      activeKeys.value = []
      resetForm()
      loadHistory()
    }
  },
  { immediate: true }
)

watch(
  () => props.prompt?.variableSchema,
  () => resetForm(),
  { deep: true }
)

const validateVariables = () => {
  for (const v of variableList.value) {
    if (v.required && !variables[v.name]?.toString().trim()) {
      message.error(`变量 "${v.description || v.name}" 不能为空`)
      return false
    }
  }
  return true
}

const handleTest = async () => {
  if (!validateVariables()) return
  testing.value = true
  try {
    const data = await testPrompt(props.prompt?.id, { ...variables })
    result.value = data
    activeKeys.value = []
    addHistory(data)
    emit('success', data)
  } catch (e) {
    message.error(e.message || '测试失败')
  } finally {
    testing.value = false
  }
}

const addHistory = (data) => {
  const record = {
    id: Date.now(),
    promptId: props.prompt?.id,
    promptCode: props.prompt?.promptCode,
    promptName: props.prompt?.promptName,
    variables: { ...variables },
    renderedSystemRole: data.renderedSystemRole,
    renderedUserPrompt: data.renderedUserPrompt,
    content: data.content,
    promptTokens: data.promptTokens,
    completionTokens: data.completionTokens,
    totalTokens: data.totalTokens,
    createdAt: new Date().toISOString()
  }
  history.value.unshift(record)
  history.value = history.value.slice(0, 20)
  saveHistory()
}

const restoreHistory = (item) => {
  Object.keys(variables).forEach((key) => delete variables[key])
  Object.entries(item.variables || {}).forEach(([key, value]) => {
    variables[key] = value
  })
  result.value = {
    content: item.content,
    renderedSystemRole: item.renderedSystemRole,
    renderedUserPrompt: item.renderedUserPrompt,
    promptTokens: item.promptTokens,
    completionTokens: item.completionTokens,
    totalTokens: item.totalTokens
  }
}

const removeHistory = (item) => {
  history.value = history.value.filter((h) => h.id !== item.id)
  saveHistory()
}

const formatTime = (iso) => {
  if (!iso) return ''
  const d = new Date(iso)
  return `${d.getMonth() + 1}/${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

const previewContent = (item) => {
  const text = item.content || ''
  return text.length > 40 ? text.slice(0, 40) + '...' : text
}

const handleClose = () => {
  emit('update:open', false)
  emit('close')
}
</script>

<style scoped>
.prompt-test-modal {
  display: flex;
  gap: 16px;
  min-height: 480px;
}

.prompt-test-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.prompt-test-form {
  margin-bottom: 8px;
}

.prompt-test-collapse {
  background: #fafafa;
}

.prompt-test-code {
  margin: 0;
  padding: 12px;
  background: #f5f5f5;
  border-radius: 4px;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 200px;
  overflow: auto;
}

.prompt-test-result {
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  overflow: hidden;
}

.prompt-test-result-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  background: #fafafa;
  border-bottom: 1px solid #f0f0f0;
}

.prompt-test-result-title {
  font-weight: 600;
  font-size: 14px;
}

.prompt-test-output {
  margin: 0;
  padding: 12px;
  background: #fff;
  font-size: 13px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 320px;
  overflow: auto;
}

.prompt-test-history {
  width: 220px;
  flex-shrink: 0;
  border-left: 1px solid #f0f0f0;
  padding-left: 16px;
  display: flex;
  flex-direction: column;
}

.prompt-test-history-title {
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 12px;
}

.prompt-test-history-item {
  cursor: pointer;
  padding: 8px;
  border-radius: 4px;
  transition: background 0.2s;
}

.prompt-test-history-item:hover {
  background: #f5f5f5;
}

.prompt-test-history-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.prompt-test-history-time {
  font-size: 12px;
  color: #8c8c8c;
}

.prompt-test-history-preview {
  width: 100%;
  margin-top: 4px;
  font-size: 12px;
  color: #595959;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

@media (max-width: 768px) {
  .prompt-test-modal {
    flex-direction: column;
  }

  .prompt-test-history {
    width: 100%;
    border-left: none;
    border-top: 1px solid #f0f0f0;
    padding-left: 0;
    padding-top: 16px;
  }
}
</style>
