<template>
  <div class="global-style">
    <a-card :bordered="false" class="global-style-card">
      <div class="global-style-header">
        <h3 class="global-style-title">预设风格</h3>
        <p class="global-style-desc">管理用户端可见的系统预设写作风格</p>
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
          placeholder="按风格名搜索"
          allow-clear
          style="width: 240px"
          @press-enter="handleSearch"
        />
        <a-button type="primary" @click="handleSearch">查询</a-button>
        <a-button @click="handleReset">重置</a-button>
        <a-button type="primary" @click="openCreateModal">
          <template #icon><PlusOutlined /></template>
          新建预设风格
        </a-button>
      </div>

      <!-- 表格 -->
      <a-table
        :columns="columns"
        :data-source="list"
        :loading="loading"
        :pagination="false"
        :scroll="{ x: 1050 }"
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
            <a-tooltip :title="record.promptSummary || '—'">
              <span class="cell-ellipsis">{{ record.promptSummary || '—' }}</span>
            </a-tooltip>
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-button type="link" size="small" @click="openEditModal(record)">编辑</a-button>
            <a-popconfirm
              title="确定删除此预设风格？"
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
      :title="editingBizNo ? '编辑预设风格' : '新建预设风格'"
      ok-text="保存"
      cancel-text="取消"
      :confirm-loading="submitting"
      :width="640"
      @ok="confirmSubmit"
    >
      <a-form :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
        <a-form-item label="风格名称" required>
          <a-input
            v-model:value="form.styleName"
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
        <a-form-item label="提示词摘要">
          <a-textarea
            v-model:value="form.promptSummary"
            placeholder="UI 卡片展示用，可换行；为空时使用提示词自动摘要"
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
          <a-input
            v-model:value="form.scope"
            placeholder="例如：公众号情感文（可选）"
            :maxlength="256"
          />
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
import { useGlobalStyleManagement } from '@/composables/useGlobalStyleManagement.js'

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
} = useGlobalStyleManagement()

const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '已启用', value: 1 },
  { label: '已禁用', value: 0 }
]

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 140 },
  { title: '风格名称', dataIndex: 'name', key: 'name', width: 140 },
  { title: '描述', dataIndex: 'description', key: 'description', width: 180 },
  { title: '提示词摘要', dataIndex: 'promptSummary', key: 'promptSummary', width: 120 },
  { title: '创作者', dataIndex: 'creatorName', key: 'creatorName', width: 80 },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 170 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
  { title: '操作', key: 'actions', width: 130, fixed: 'right' }
]

const editorVisible = ref(false)
const editingBizNo = ref(null)
const form = reactive({
  styleName: '',
  description: '',
  promptSummary: '',
  prompt: '',
  scope: '',
  enableStatus: true
})

function resetForm() {
  form.styleName = ''
  form.description = ''
  form.promptSummary = ''
  form.prompt = ''
  form.scope = ''
  form.enableStatus = true
}

const openCreateModal = () => {
  editingBizNo.value = null
  resetForm()
  editorVisible.value = true
}

const openEditModal = (record) => {
  editingBizNo.value = record.id
  form.styleName = record.name || ''
  form.description = record.description || ''
  form.promptSummary = record.promptSummary || ''
  form.prompt = record.prompt || ''
  form.scope = record.scope || ''
  form.enableStatus = record.status === 'enabled'
  editorVisible.value = true
}

const confirmSubmit = async () => {
  if (!form.styleName.trim() || !form.prompt.trim()) {
    message.error('请填写风格名称和提示词')
    return
  }
  const payload = {
    styleName: form.styleName.trim(),
    description: form.description || '',
    promptSummary: form.promptSummary || '',
    prompt: form.prompt.trim(),
    scope: form.scope || '',
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
</style>