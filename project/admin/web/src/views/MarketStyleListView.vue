<template>
  <div class="market-style">
    <a-card :bordered="false" class="market-style-card">
      <div class="market-style-header">
        <h3 class="market-style-title">风格市场</h3>
        <p class="market-style-desc">管理用户端风格市场中展示的风格条目</p>
      </div>

      <!-- 工具栏 -->
      <div class="market-style-toolbar">
        <a-select
          v-model:value="status"
          style="width: 140px"
          :options="statusOptions"
          @change="handleSearch"
        />
        <a-input
          v-model:value="keyword"
          placeholder="按风格名或发布者搜索"
          allow-clear
          style="width: 240px"
          @press-enter="handleSearch"
        />
        <a-button type="primary" @click="handleSearch">查询</a-button>
        <a-button @click="handleReset">重置</a-button>
        <a-button type="primary" @click="openCreateModal">
          <template #icon><PlusOutlined /></template>
          新建风格
        </a-button>
      </div>

      <!-- 表格 -->
      <a-table
        :columns="columns"
        :data-source="list"
        :loading="loading"
        :pagination="false"
        row-key="id"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 'enabled' ? 'green' : 'default'">
              {{ record.status === 'enabled' ? '已启用' : '已禁用' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'description'">
            <span class="cell-ellipsis">{{ record.description || '—' }}</span>
          </template>
          <template v-else-if="column.key === 'promptSummary'">
            <span class="cell-ellipsis">{{ record.promptSummary || '—' }}</span>
          </template>
          <template v-else-if="column.key === 'publisherName'">
            <span>{{ record.publisherName || record.publisherUserId }}</span>
          </template>
          <template v-else-if="column.key === 'price'">
            <span>{{ record.price }}</span>
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-button type="link" size="small" @click="openEditModal(record)">编辑</a-button>
            <a-popconfirm
              title="确定删除此风格市场条目？"
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
      <div class="market-style-pagination">
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
      :title="editingBizNo ? '编辑风格市场条目' : '新建风格市场条目'"
      ok-text="保存"
      cancel-text="取消"
      :confirm-loading="submitting"
      :width="720"
      @ok="confirmSubmit"
    >
      <a-form :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
        <a-form-item label="风格名称" required>
          <a-input
            v-model:value="form.styleName"
            placeholder="例如：爆款情感文"
            :maxlength="64"
            show-count
          />
        </a-form-item>
        <a-form-item label="发布者" required>
          <a-select
            v-model:value="form.publisherUserId"
            placeholder="搜索并选择发布者"
            show-search
            :filter-option="false"
            :options="publisherOptions"
            :loading="publisherLoading"
            @search="searchPublisher"
            @dropdown-visible-change="onPublisherDropdownOpen"
          />
        </a-form-item>
        <a-form-item label="简短描述">
          <a-input
            v-model:value="form.description"
            placeholder="一句话描述，方便用户浏览"
            :maxlength="256"
          />
        </a-form-item>
        <a-form-item label="提示词摘要">
          <a-textarea
            v-model:value="form.promptSummary"
            placeholder="UI 卡片展示用，可换行"
            :maxlength="512"
            :rows="3"
          />
        </a-form-item>
        <a-form-item label="提示词" required>
          <a-textarea
            v-model:value="form.prompt"
            placeholder="喂给 AI 的完整风格提示词"
            :rows="6"
          />
        </a-form-item>
        <a-form-item label="适用范围">
          <div class="market-style-scope-tags">
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
              class="market-style-scope-input"
              placeholder="输入标签后回车"
              :maxlength="MAX_SCOPE_TAG_LENGTH"
              @keydown.enter.prevent="addTag"
            />
          </div>
          <div class="market-style-scope-hint">
            最多 {{ MAX_SCOPE_TAGS }} 个标签，每个不超过 {{ MAX_SCOPE_TAG_LENGTH }} 个字（可选）
          </div>
        </a-form-item>
        <a-form-item label="累计使用">
          <a-input-number
            v-model:value="form.totalUses"
            :min="0"
            style="width: 160px"
          />
        </a-form-item>
        <a-form-item label="单次价格">
          <a-input :value="form.price + ' 创作币'" disabled style="width: 160px" />
        </a-form-item>
        <a-form-item label="启用状态">
          <a-switch
            v-model:checked="form.enableStatus"
            checked-children="启用"
            un-checked-children="禁用"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { useMarketStyleManagement } from '@/composables/useMarketStyleManagement.js'
import { useScopeTags } from '@/composables/useScopeTags.js'
import { listUserOptions } from '@/api/userOptions.js'

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
  handleDelete
} = useMarketStyleManagement()

const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '已启用', value: 1 },
  { label: '已禁用', value: 0 }
]

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 160 },
  { title: '风格名称', dataIndex: 'name', key: 'name', width: 140 },
  { title: '描述', dataIndex: 'description', key: 'description', width: 160 },
  { title: '提示词摘要', dataIndex: 'promptSummary', key: 'promptSummary', width: 220 },
  { title: '发布者', dataIndex: 'publisherName', key: 'publisherName', width: 110 },
  { title: '累计使用', dataIndex: 'totalUses', key: 'totalUses', width: 90 },
  { title: '价格', dataIndex: 'price', key: 'price', width: 90 },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 170 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
  { title: '操作', key: 'actions', width: 130, fixed: 'right' }
]

const editorVisible = ref(false)
const editingBizNo = ref(null)
const publisherOptions = ref([])
const publisherLoading = ref(false)
const publisherKeyword = ref('')

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

const form = reactive({
  styleName: '',
  publisherUserId: null,
  description: '',
  promptSummary: '',
  prompt: '',
  totalUses: 0,
  price: '0.20',
  enableStatus: true
})

function resetForm() {
  form.styleName = ''
  form.publisherUserId = null
  form.description = ''
  form.promptSummary = ''
  form.prompt = ''
  scopeRef.value = ''
  scopeInput.value = ''
  form.totalUses = 0
  form.price = '0.20'
  form.enableStatus = true
  publisherOptions.value = []
}

const searchPublisher = async (kw) => {
  publisherKeyword.value = kw
  await loadPublisherOptions(kw)
}

const onPublisherDropdownOpen = async (open) => {
  if (open && publisherOptions.value.length === 0) {
    await loadPublisherOptions(publisherKeyword.value)
  }
}

const loadPublisherOptions = async (kw = '') => {
  publisherLoading.value = true
  try {
    const users = await listUserOptions(kw, 20)
    publisherOptions.value = users.map((u) => ({
      label: u.nickname ? `${u.nickname}（${u.email}）` : u.email,
      value: u.id
    }))
  } catch (error) {
    message.error(error.message || '加载发布者失败')
  } finally {
    publisherLoading.value = false
  }
}

const openCreateModal = () => {
  editingBizNo.value = null
  resetForm()
  editorVisible.value = true
}

const openEditModal = (record) => {
  editingBizNo.value = record.id
  form.styleName = record.name || ''
  form.publisherUserId = record.publisherUserId
  form.description = record.description || ''
  form.promptSummary = record.promptSummary || ''
  form.prompt = record.prompt || ''
  scopeRef.value = record.scope || ''
  scopeInput.value = ''
  form.totalUses = record.totalUses || 0
  form.price = String(record.price || '0.20')
  form.enableStatus = record.status === 'enabled'
  publisherOptions.value = [{
    label: record.publisherName ? `${record.publisherName}（${record.publisherUserId}）` : String(record.publisherUserId),
    value: record.publisherUserId
  }]
  editorVisible.value = true
}

const confirmSubmit = async () => {
  if (!form.styleName.trim() || !form.prompt.trim()) {
    message.error('请填写风格名称和提示词')
    return
  }
  if (form.publisherUserId == null) {
    message.error('请选择发布者')
    return
  }
  if (scopeError.value) {
    message.error(scopeError.value)
    return
  }
  const payload = {
    styleName: form.styleName.trim(),
    publisherUserId: form.publisherUserId,
    description: form.description || '',
    promptSummary: form.promptSummary || '',
    prompt: form.prompt.trim(),
    scope: scopeRef.value || '',
    totalUses: form.totalUses || 0,
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

onMounted(() => {
  fetch()
})
</script>

<style scoped>
.market-style-card {
  border-radius: 8px;
}

.market-style-header {
  margin-bottom: 16px;
}

.market-style-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 4px 0;
}

.market-style-desc {
  font-size: 13px;
  color: #8c8c8c;
  margin: 0;
}

.market-style-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  align-items: center;
}

.market-style-pagination {
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

.market-style-scope-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.market-style-scope-input {
  min-width: 160px;
  flex: 1;
  padding: 4px 8px;
  border: 1px dashed #d9d9d9;
  border-radius: 4px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
}

.market-style-scope-input:focus {
  border-color: #ff2442;
}

.market-style-scope-input::placeholder {
  color: #bfbfbf;
}

.market-style-scope-hint {
  margin-top: 4px;
  font-size: 12px;
  color: #8c8c8c;
}
</style>
