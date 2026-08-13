<template>
  <div class="model-config">
    <h3 class="page-title">模型配置</h3>
    <p class="page-desc">按厂商池化管理多个模型 key，数字越小优先级越高；多厂商可同时开启。</p>

    <a-spin :spinning="loading">
      <a-empty v-if="!groupedProviders.length" description="暂无配置" />

      <a-row :gutter="[16, 16]">
        <a-col
          v-for="group in groupedProviders"
          :key="group.providerType"
          :xs="24"
          :lg="12"
        >
          <a-card :title="group.providerName" class="provider-card">
            <template #extra>
              <a-button type="primary" size="small" @click="openModal(group.providerType)">
                新增配置
              </a-button>
            </template>

            <a-table
              :columns="columns"
              :data-source="group.configs"
              :pagination="false"
              size="small"
              row-key="id"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'status'">
                  <a-tag v-if="record.isActive" color="green">已启用</a-tag>
                  <a-tag v-else color="default">已停用</a-tag>
                </template>
                <template v-else-if="column.key === 'apiKey'">
                  <span class="masked-key">{{ maskKey(record.apiKey) }}</span>
                </template>
                <template v-else-if="column.key === 'action'">
                  <a-space>
                    <a-button type="link" size="small" @click="openModal(record.providerType, record)">
                      编辑
                    </a-button>
                    <a-button type="link" size="small" @click="handleToggle(record)">
                      {{ record.isActive ? '停用' : '启用' }}
                    </a-button>
                    <a-dropdown>
                      <a-button type="link" size="small">更多</a-button>
                      <template #overlay>
                        <a-menu>
                          <a-menu-item @click="handleRowTestConnection(record)">测试连接</a-menu-item>
                          <a-menu-item @click="openChatModal(record)">问答测试</a-menu-item>
                        </a-menu>
                      </template>
                    </a-dropdown>
                    <a-popconfirm
                      title="确定删除该配置？"
                      ok-text="确认"
                      cancel-text="取消"
                      @confirm="handleDelete(record.id)"
                    >
                      <a-button type="link" danger size="small">删除</a-button>
                    </a-popconfirm>
                  </a-space>
                </template>
              </template>
            </a-table>
          </a-card>
        </a-col>
      </a-row>
    </a-spin>

    <!-- 新增/编辑弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="editingId ? '编辑配置' : '新增配置'"
      width="600px"
      :confirm-loading="modalLoading"
      @ok="handleSave"
    >
      <a-form
        ref="formRef"
        :model="form"
        :rules="rules"
        layout="vertical"
        style="height: 420px; overflow-y: auto;"
      >
        <a-form-item label="厂商" name="providerType">
          <a-select
            v-model:value="form.providerType"
            :options="providerOptions"
            placeholder="请选择厂商"
            :disabled="!!editingId"
          />
        </a-form-item>

        <a-form-item label="配置名称" name="name">
          <a-input v-model:value="form.name" placeholder="例如：Kimi-生产主-key" />
        </a-form-item>

        <a-form-item label="优先级" name="priority">
          <a-input-number
            v-model:value="form.priority"
            :min="0"
            style="width: 100%"
            placeholder="数字越小越优先"
          />
        </a-form-item>

        <a-form-item label="Base URL" name="baseUrl">
          <a-input v-model:value="form.baseUrl" placeholder="https://api.moonshot.cn" />
        </a-form-item>

        <a-form-item label="API Key" name="apiKey">
          <a-input-password v-model:value="form.apiKey" placeholder="sk-..." />
        </a-form-item>

        <a-form-item label="模型编码" name="modelCode">
          <a-input-group compact style="display: flex;">
            <a-auto-complete
              v-model:value="form.modelCode"
              :options="providerModelOptions"
              placeholder="请选择或输入模型编码"
              allow-clear
              style="flex: 1;"
              :filter-option="filterModelOption"
              @select="handleModelCodeSelect"
            />
            <a-button :loading="fetchingModels" @click="handleFetchModels">
              获取模型
            </a-button>
          </a-input-group>
        </a-form-item>

        <a-form-item label="模型显示名" name="modelName">
          <a-input v-model:value="form.modelName" placeholder="可选" />
        </a-form-item>

        <a-form-item label="状态" name="isActive">
          <a-switch
            v-model:checked="form.isActive"
            checked-children="启用"
            un-checked-children="停用"
          />
        </a-form-item>

        <a-form-item>
          <a-button @click="handleTestConnection">测试连接</a-button>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 问答测试弹窗 -->
    <a-modal
      v-model:open="chatVisible"
      title="问答测试"
      width="700px"
      :footer="null"
    >
      <div style="height: 480px; display: flex; flex-direction: column;">
        <a-form layout="vertical">
          <a-form-item label="问题">
            <a-textarea
              v-model:value="chatForm.prompt"
              :rows="3"
              placeholder="例如：用一句话介绍你自己"
              allow-clear
            />
          </a-form-item>
          <a-form-item>
            <a-space>
              <a-switch
                v-model:checked="chatForm.stream"
                checked-children="流式"
                un-checked-children="非流式"
              />
              <a-button
                type="primary"
                :loading="chatLoading"
                @click="handleChatTest"
              >
                发送测试
              </a-button>
            </a-space>
          </a-form-item>
        </a-form>
        <pre v-if="chatResult" class="chat-result">{{ chatResult }}</pre>
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { useModelConfig } from '@/composables/useModelConfig.js'

const { configs, loading, fetchConfigs, saveConfig, removeConfig, fetchModelOptions, loadProviderModels, testConfigConnection, toggleConfigActive, chatTestConfig } = useModelConfig()

const providerOptions = [
  { label: 'Kimi', value: 'kimi' },
  { label: 'MiniMax', value: 'minimax' }
]

const columns = [
  { title: '名称', dataIndex: 'name', key: 'name', ellipsis: true },
  { title: '优先级', dataIndex: 'priority', key: 'priority', width: 70 },
  { title: 'Base URL', dataIndex: 'baseUrl', key: 'baseUrl', ellipsis: true },
  { title: '模型', dataIndex: 'modelCode', key: 'modelCode', ellipsis: true },
  { title: 'API Key', key: 'apiKey', ellipsis: true },
  { title: '状态', key: 'status', width: 90 },
  { title: '操作', key: 'action', width: 180, fixed: 'right' }
]

const groupedProviders = computed(() => {
  const map = new Map()
  providerOptions.forEach((p) => map.set(p.value, { providerType: p.value, providerName: p.label, configs: [] }))
  configs.value.forEach((cfg) => {
    const group = map.get(cfg.providerType)
    if (group) {
      group.configs.push(cfg)
    } else {
      map.set(cfg.providerType, { providerType: cfg.providerType, providerName: cfg.providerName || cfg.providerType, configs: [cfg] })
    }
  })
  return Array.from(map.values())
})

const maskKey = (key) => {
  if (!key) return ''
  if (key.length <= 8) return '****'
  return key.slice(0, 4) + '****' + key.slice(-4)
}

// 新增/编辑弹窗
const modalVisible = ref(false)
const modalLoading = ref(false)
const editingId = ref(null)
const formRef = ref()
const form = reactive({
  providerType: 'kimi',
  name: '',
  baseUrl: '',
  apiKey: '',
  modelCode: '',
  modelName: '',
  priority: 0,
  isActive: true
})

const rules = {
  providerType: [{ required: true, message: '请选择厂商' }],
  name: [{ required: true, message: '请输入配置名称' }],
  priority: [{ required: true, message: '请输入优先级' }],
  baseUrl: [{ required: true, message: '请输入 Base URL' }],
  apiKey: [{ required: true, message: '请输入 API Key' }],
  modelCode: [{ required: true, message: '请输入模型编码' }]
}

const providerModelOptions = ref([])
const fetchingModels = ref(false)

const filterModelOption = (input, option) => {
  const label = option.label || ''
  const value = option.value || ''
  return label.toLowerCase().includes(input.toLowerCase()) || value.toLowerCase().includes(input.toLowerCase())
}

const loadPersistedModels = async (providerType) => {
  try {
    const models = await loadProviderModels(providerType)
    providerModelOptions.value = models.map((m) => ({ label: m.modelName || m.modelCode, value: m.modelCode }))
  } catch (error) {
    providerModelOptions.value = []
  }
}

const resetForm = (providerType = 'kimi') => {
  form.providerType = providerType
  form.name = ''
  form.baseUrl = ''
  form.apiKey = ''
  form.modelCode = ''
  form.modelName = ''
  form.priority = 0
  form.isActive = true
  providerModelOptions.value = []
}

const openModal = (providerType, record) => {
  editingId.value = record ? record.id : null
  if (record) {
    form.providerType = record.providerType
    form.name = record.name
    form.baseUrl = record.baseUrl
    form.apiKey = record.apiKey
    form.modelCode = record.modelCode
    form.modelName = record.modelName || ''
    form.priority = record.priority
    form.isActive = record.isActive === 1
    loadPersistedModels(record.providerType)
  } else {
    resetForm(providerType)
    loadPersistedModels(providerType)
  }
  modalLoading.value = false
  modalVisible.value = true
}

watch(modalVisible, (visible) => {
  if (!visible) {
    modalLoading.value = false
  }
})

watch(() => form.providerType, (providerType) => {
  if (modalVisible.value && providerType) {
    form.modelCode = ''
    form.modelName = ''
    loadPersistedModels(providerType)
  }
})

const handleModelCodeSelect = (value) => {
  const option = providerModelOptions.value.find((o) => o.value === value)
  if (option) {
    form.modelName = option.label
  }
}

const handleSave = () => {
  formRef.value.validate().then(() => {
    modalLoading.value = true
    const payload = {
      providerType: form.providerType,
      name: form.name,
      baseUrl: form.baseUrl,
      apiKey: form.apiKey,
      modelCode: form.modelCode,
      modelName: form.modelName,
      priority: form.priority,
      isActive: form.isActive ? 1 : 0
    }
    return saveConfig(editingId.value, payload)
      .then(() => {
        modalVisible.value = false
      })
      .catch((error) => {
        message.error(error.message || '保存失败')
      })
      .finally(() => {
        modalLoading.value = false
      })
  }).catch(() => {
    // 表单校验失败，错误信息已在字段下方展示
  })
}

const handleFetchModels = async () => {
  if (!form.baseUrl || !form.apiKey) {
    message.warning('请先填写 Base URL 和 API Key')
    return
  }
  fetchingModels.value = true
  try {
    const options = await fetchModelOptions({
      providerType: form.providerType,
      baseUrl: form.baseUrl,
      apiKey: form.apiKey
    })
    providerModelOptions.value = options.map((m) => ({ label: m.modelName || m.modelCode, value: m.modelCode }))
    if (options && options.length) {
      message.success(`获取到 ${options.length} 个模型`)
      if (!form.modelCode) {
        form.modelCode = options[0].modelCode
        form.modelName = options[0].modelName || options[0].modelCode
      }
    } else {
      message.warning('未获取到模型列表')
    }
  } catch (error) {
    message.error(error.message || '获取模型失败')
  } finally {
    fetchingModels.value = false
  }
}

const handleTestConnection = async () => {
  await doTestConnection({
    providerType: form.providerType,
    baseUrl: form.baseUrl,
    apiKey: form.apiKey
  })
}

const handleRowTestConnection = async (record) => {
  await doTestConnection({
    providerType: record.providerType,
    baseUrl: record.baseUrl,
    apiKey: record.apiKey
  })
}

const doTestConnection = async (payload) => {
  if (!payload.baseUrl || !payload.apiKey) {
    message.warning('请先填写 Base URL 和 API Key')
    return
  }
  try {
    await testConfigConnection(payload)
  } catch (error) {
    message.error(error.message || '测试连接失败')
  }
}

const handleToggle = async (record) => {
  await toggleConfigActive(record.id, record.isActive ? 0 : 1)
}

const handleDelete = async (id) => {
  await removeConfig(id)
}

// 问答测试弹窗
const chatVisible = ref(false)
const chatLoading = ref(false)
const chatTarget = ref(null)
const chatForm = reactive({ prompt: '用一句话介绍你自己', stream: false })
const chatResult = ref('')

const openChatModal = (record) => {
  chatTarget.value = record
  chatForm.prompt = '用一句话介绍你自己'
  chatForm.stream = false
  chatResult.value = ''
  chatVisible.value = true
}

const formatChatResult = (vo) => {
  if (!vo) return ''
  const lines = []
  if (vo.statusCode !== undefined && vo.statusCode !== null) {
    lines.push(`HTTP ${vo.statusCode}`)
  }
  if (vo.requestHeaders) {
    lines.push('--- 请求头 ---')
    lines.push(vo.requestHeaders)
  }
  if (vo.requestBody) {
    lines.push('--- 请求体 ---')
    lines.push(vo.requestBody)
  }
  lines.push('--- 响应体 ---')
  lines.push(vo.responseBody || '(空)')
  return lines.join('\n')
}

const handleChatTest = async () => {
  if (!chatTarget.value) return
  if (!chatForm.prompt || !chatForm.prompt.trim()) {
    message.warning('请输入问题')
    return
  }
  chatLoading.value = true
  try {
    const result = await chatTestConfig({
      providerType: chatTarget.value.providerType,
      baseUrl: chatTarget.value.baseUrl,
      apiKey: chatTarget.value.apiKey,
      modelCode: chatTarget.value.modelCode,
      prompt: chatForm.prompt,
      stream: chatForm.stream
    })
    chatResult.value = formatChatResult(result)
  } catch (error) {
    chatResult.value = `请求失败：${error.message || '未知错误'}`
  } finally {
    chatLoading.value = false
  }
}

fetchConfigs()
</script>

<style scoped>
.page-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 4px;
}

.page-desc {
  color: #8c8c8c;
  margin: 0 0 16px;
}

.provider-card {
  border-radius: 8px;
}

.masked-key {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  color: #595959;
}

.chat-result {
  flex: 1;
  background: #fafafa;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  padding: 8px 12px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 12px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-all;
  overflow: auto;
  margin: 0;
}
</style>
