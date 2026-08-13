<template>
  <div class="global-style">
    <a-card :bordered="false" class="global-style-card">
      <div class="global-style-header">
        <h3 class="global-style-title">预设提示词</h3>
        <p class="global-style-desc">管理用户端可见的系统预设提示词</p>
      </div>

      <!-- 工具栏 -->
      <div class="global-style-toolbar">
        <a-select
          v-model:value="status"
          style="width: 140px"
          :options="statusOptions"
          @change="handleSearch"
        />
        <a-input
          v-model:value="keyword"
          placeholder="按提示词名搜索"
          allow-clear
          style="width: 240px"
          @press-enter="handleSearch"
        />
        <a-button type="primary" @click="handleSearch">查询</a-button>
        <a-button @click="handleReset">重置</a-button>
        <a-button type="primary" @click="openCreateModal">
          <template #icon><PlusOutlined /></template>
          新建预设提示词
        </a-button>
      </div>

      <!-- 批量操作 -->
      <div v-if="selectedRowKeys.length > 0" class="global-style-batch-bar">
        <span>已选 {{ selectedRowKeys.length }} 项</span>
        <a-popconfirm
          title="确定批量删除选中的预设提示词？"
          ok-text="删除"
          cancel-text="取消"
          @confirm="confirmBatchDelete"
        >
          <a-button type="primary" danger size="small">批量删除</a-button>
        </a-popconfirm>
      </div>

      <!-- 表格 -->
      <a-table
        :columns="columns"
        :data-source="list"
        :loading="loading"
        :pagination="false"
        :scroll="{ x: 1200 }"
        :row-selection="rowSelection"
        row-key="id"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 'enabled' ? 'green' : 'default'">
              {{ record.status === 'enabled' ? '已上架' : '已下架' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'description'">
            <span class="cell-ellipsis">{{ record.description || '—' }}</span>
          </template>
          <template v-else-if="column.key === 'prompt'">
            <a-tooltip :title="record.prompt">
              <span class="cell-ellipsis">{{ record.prompt || '—' }}</span>
            </a-tooltip>
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-button type="link" size="small" @click="openEditModal(record)">编辑</a-button>
            <a-popconfirm
              :title="record.status === 'enabled' ? '确定下架此预设提示词？' : '确定上架此预设提示词？'"
              ok-text="确定"
              cancel-text="取消"
              @confirm="toggleStatus(record)"
            >
              <a-button type="link" size="small" :danger="record.status === 'enabled'">{{ record.status === 'enabled' ? '下架' : '上架' }}</a-button>
            </a-popconfirm>
            <a-popconfirm
              title="确定删除此预设提示词？"
              ok-text="删除"
              cancel-text="取消"
              @confirm="confirmDelete(record)"
            >
              <a-button type="link" size="small" danger>删除</a-button>
            </a-popconfirm>
          </template>
        </template>
      </a-table>

      <!-- 分页 -->
      <div class="global-style-pagination">
        <a-pagination
          :current="page"
          :page-size="pageSize"
          :total="total"
          :page-size-options="['10', '20', '50']"
          show-size-changer
          show-total
          @change="handlePageChange"
          @show-size-change="handlePageChange"
        />
      </div>
    </a-card>

    <!-- 新建 / 编辑 Modal -->
    <a-modal
      v-model:open="editorVisible"
      :title="editingBizNo ? '编辑预设提示词' : '新建预设提示词'"
      ok-text="保存"
      cancel-text="取消"
      :confirm-loading="submitting"
      :width="720"
      @ok="confirmSubmit"
    >
      <a-form :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
        <a-form-item label="提示词名称" required>
          <a-input
            v-model:value="form.skillName"
            placeholder="例如：年度总结"
            :maxlength="64"
            show-count
          />
        </a-form-item>
        <a-form-item label="简短描述">
          <a-input
            v-model:value="form.description"
            placeholder="一句话描述，方便用户浏览"
            :maxlength="256"
          />
        </a-form-item>

        <a-form-item label="基于模版创建">
          <a-switch
            v-model:checked="form.templateBased"
            checked-children="是"
            un-checked-children="否"
          />
          <span class="form-hint">开启后按「角色 / 受众 / 写作要求 / 语气 / 禁区」五部分填写，保存时自动拼接为完整提示词</span>
        </a-form-item>

        <!-- 自由编辑模式 -->
        <template v-if="!form.templateBased">
          <a-form-item label="提示词" required>
            <a-textarea
              v-model:value="form.prompt"
              placeholder="喂给 AI 的完整提示词"
              :rows="8"
            />
          </a-form-item>
        </template>

        <!-- 模版编辑模式 -->
        <template v-else>
          <a-form-item label="角色" required>
            <a-textarea
              v-model:value="form.promptExtra.role"
              placeholder="例如：你是一位擅长把专业知识翻译成大白话的科普作者"
              :rows="2"
            />
          </a-form-item>
          <a-form-item label="受众">
            <a-textarea
              v-model:value="form.promptExtra.audience"
              placeholder="例如：对行业术语不熟悉但想快速理解的普通读者"
              :rows="2"
            />
          </a-form-item>
          <a-form-item label="写作要求" required>
            <a-textarea
              v-model:value="form.promptExtra.requirements"
              placeholder="例如：1. 开篇从生活场景入手\n2. 必须使用至少一个比喻或类比\n3. 按「是什么→为什么→会怎样」推进"
              :rows="8"
            />
          </a-form-item>
          <a-form-item label="语气">
            <a-textarea
              v-model:value="form.promptExtra.tone"
              placeholder="例如：耐心、亲切，像在给朋友讲一件有趣的事情"
              :rows="2"
            />
          </a-form-item>
          <a-form-item label="禁区">
            <a-textarea
              v-model:value="form.promptExtra.restrictions"
              placeholder="例如：不要堆砌专业术语；不要给出无法验证的数据"
              :rows="3"
            />
          </a-form-item>
          <a-form-item label="拼接预览">
            <a-textarea
              :value="displayPrompt"
              :rows="8"
              readonly
              class="prompt-preview"
            />
          </a-form-item>
        </template>

        <a-form-item label="适用范围">
          <div class="global-style-scope-tags">
            <a-tag
              v-for="tag in scopeTags"
              :key="tag"
              closable
              :disable="!editingBizNo && scopeTags.length >= MAX_SCOPE_TAGS"
              @close.prevent="removeTag(tag)"
            >
              {{ tag }}
            </a-tag>
            <input
              v-if="scopeTags.length < MAX_SCOPE_TAGS"
              v-model="scopeInput"
              type="text"
              class="global-style-scope-input"
              placeholder="输入标签后按 Tab 或回车"
              :maxlength="MAX_SCOPE_TAG_LENGTH"
              @keydown.enter.prevent="addTag"
              @keydown.tab="handleScopeTab"
            />
          </div>
          <div class="global-style-scope-hint">
            最多 {{ MAX_SCOPE_TAGS }} 个标签，每个不超过 {{ MAX_SCOPE_TAG_LENGTH }} 个字（可选）
          </div>
        </a-form-item>
        <a-form-item label="上架状态">
          <a-switch
            v-model:checked="form.enableStatus"
            checked-children="上架"
            un-checked-children="下架"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { useGlobalStyleManagement } from '@/composables/useGlobalStyleManagement.js'
import { useScopeTags } from '@/composables/useScopeTags.js'

const {
  list,
  total,
  loading,
  submitting,
  page,
  pageSize,
  keyword,
  status,
  fetch,
  handleSearch,
  handleReset,
  handlePageChange,
  handleCreate,
  handleUpdate,
  handleDelete,
  handleBatchDelete
} = useGlobalStyleManagement()

const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '已上架', value: 1 },
  { label: '已下架', value: 0 }
]

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 140 },
  { title: '提示词名称', dataIndex: 'name', key: 'name', width: 140 },
  { title: '描述', dataIndex: 'description', key: 'description', width: 180 },
  { title: '提示词内容', dataIndex: 'prompt', key: 'prompt', width: 320, ellipsis: true },
  { title: '创作者', dataIndex: 'creatorName', key: 'creatorName', width: 80 },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 170 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
  { title: '操作', key: 'actions', width: 180, fixed: 'right' }
]

const editorVisible = ref(false)
const editingBizNo = ref(null)
const selectedRowKeys = ref([])
const selectedRows = ref([])

const rowSelection = {
  selectedRowKeys,
  onChange: (keys, rows) => {
    selectedRowKeys.value = keys
    selectedRows.value = rows
  }
}

const scopeRef = ref('')
const {
  scopeInput,
  scopeTags,
  scopeError,
  addTag,
  removeTag,
  MAX_SCOPE_TAGS,
  MAX_SCOPE_TAG_LENGTH
} = useScopeTags(scopeRef)

const DEFAULT_PROMPT_EXTRA = {
  role: '',
  audience: '',
  requirements: '',
  tone: '',
  restrictions: ''
}

const form = reactive({
  skillName: '',
  description: '',
  prompt: '',
  templateBased: false,
  promptExtra: { ...DEFAULT_PROMPT_EXTRA },
  enableStatus: true
})

const displayPrompt = computed(() => {
  if (!form.templateBased) return form.prompt
  return buildPromptFromExtra(form.promptExtra)
})

function buildPromptFromExtra(extra) {
  const parts = []
  if (extra.role?.trim()) parts.push(`- 角色：${extra.role.trim()}`)
  if (extra.audience?.trim()) parts.push(`- 受众：${extra.audience.trim()}`)
  if (extra.requirements?.trim()) parts.push(`- 写作要求：\n${extra.requirements.trim()}`)
  if (extra.tone?.trim()) parts.push(`- 语气：${extra.tone.trim()}`)
  if (extra.restrictions?.trim()) parts.push(`- 禁区：${extra.restrictions.trim()}`)
  return parts.join('\n\n')
}

function parsePromptExtra(raw) {
  if (!raw) return null
  try {
    const obj = typeof raw === 'string' ? JSON.parse(raw) : raw
    return obj && obj.templateBased === true ? obj : null
  } catch (e) {
    return null
  }
}

function resetForm() {
  form.skillName = ''
  form.description = ''
  form.prompt = ''
  form.templateBased = false
  form.promptExtra = { ...DEFAULT_PROMPT_EXTRA }
  scopeRef.value = ''
  scopeInput.value = ''
  form.enableStatus = true
}

function handleScopeTab(e) {
  const raw = scopeInput.value?.trim()
  if (raw) {
    e.preventDefault()
    addTag()
  }
}

const openCreateModal = () => {
  editingBizNo.value = null
  resetForm()
  editorVisible.value = true
}

const openEditModal = (record) => {
  editingBizNo.value = record.id
  form.skillName = record.name || ''
  form.description = record.description || ''
  form.prompt = record.prompt || ''
  const extra = parsePromptExtra(record.promptExtra)
  if (extra) {
    form.templateBased = true
    form.promptExtra = {
      role: extra.role || '',
      audience: extra.audience || '',
      requirements: extra.requirements || '',
      tone: extra.tone || '',
      restrictions: extra.restrictions || ''
    }
  } else {
    form.templateBased = false
    form.promptExtra = { ...DEFAULT_PROMPT_EXTRA }
  }
  scopeRef.value = record.scope || ''
  scopeInput.value = ''
  form.enableStatus = record.status === 'enabled'
  editorVisible.value = true
}

const confirmSubmit = async () => {
  if (!form.skillName.trim()) {
    message.error('请输入提示词名称')
    return
  }
  if (form.templateBased) {
    if (!form.promptExtra.role.trim()) {
      message.error('请填写角色')
      return
    }
    if (!form.promptExtra.requirements.trim()) {
      message.error('请填写写作要求')
      return
    }
  } else {
    if (!form.prompt.trim()) {
      message.error('请填写提示词')
      return
    }
  }
  if (scopeError.value) {
    message.error(scopeError.value)
    return
  }
  const payload = {
    skillName: form.skillName.trim(),
    description: form.description || '',
    prompt: form.templateBased ? displayPrompt.value : form.prompt.trim(),
    promptExtra: form.templateBased ? JSON.stringify({ templateBased: true, ...form.promptExtra }) : null,
    scope: scopeRef.value || '',
    enableStatus: form.enableStatus ? 1 : 0
  }
  const ok = editingBizNo.value
    ? await handleUpdate(editingBizNo.value, payload)
    : await handleCreate(payload)
  if (ok) {
    editorVisible.value = false
  }
}

const confirmDelete = async (record) => {
  await handleDelete(record.id)
}

const confirmBatchDelete = async () => {
  const ok = await handleBatchDelete(selectedRowKeys.value)
  if (ok) {
    selectedRowKeys.value = []
    selectedRows.value = []
  }
}

const toggleStatus = async (record) => {
  const nextStatus = record.status === 'enabled' ? 0 : 1
  const payload = {
    skillName: record.name,
    description: record.description || '',
    prompt: record.prompt || '',
    promptExtra: record.promptExtra || null,
    promptSummary: record.promptSummary || '',
    scope: record.scope || '',
    enableStatus: nextStatus
  }
  const ok = await handleUpdate(record.id, payload)
  if (ok) {
    message.success(nextStatus === 1 ? '预设提示词已上架' : '预设提示词已下架')
  }
}

onMounted(() => {
  fetch()
})
</script>

<style scoped>
.global-style-card {
  border-radius: 8px;
}

.global-style-header {
  margin-bottom: 16px;
}

.global-style-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 4px 0;
}

.global-style-desc {
  font-size: 13px;
  color: #8c8c8c;
  margin: 0;
}

.global-style-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  align-items: center;
}

.global-style-batch-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  padding: 8px 12px;
  background: #fff2f0;
  border: 1px solid #ffccc7;
  border-radius: 6px;
  font-size: 13px;
  color: #434343;
}

.global-style-pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.cell-ellipsis {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
}

.global-style-scope-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.global-style-scope-input {
  min-width: 160px;
  flex: 1;
  padding: 4px 8px;
  border: 1px dashed #d9d9d9;
  border-radius: 4px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
}

.global-style-scope-input:focus {
  border-color: #ff2442;
}

.global-style-scope-input::placeholder {
  color: #bfbfbf;
}

.global-style-scope-hint {
  margin-top: 4px;
  font-size: 12px;
  color: #8c8c8c;
}

.form-hint {
  margin-left: 8px;
  font-size: 12px;
  color: #8c8c8c;
}

.prompt-preview {
  background: #f6ffed;
}
</style>
