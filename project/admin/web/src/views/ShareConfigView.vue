<template>
  <div class="share-config-page">
    <a-card :bordered="false" class="list-card">
      <div class="list-header">
        <div>
          <h3 class="list-title">分享管理</h3>
          <p class="list-desc">配置用户端各分享场景的复制文案，支持 {title}、{url}、{code} 等占位符。</p>
        </div>
        <a-button type="primary" @click="handleCreate">+ 新增配置</a-button>
      </div>

      <a-table
        :columns="columns"
        :data-source="items"
        :loading="loading"
        :pagination="{ current: page, pageSize: pageSize, total: total }"
        row-key="id"
        size="middle"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'enabled'">
            <a-tag :color="record.enabled === 1 ? 'green' : 'default'">{{ record.enabled === 1 ? '启用' : '禁用' }}</a-tag>
          </template>
          <template v-else-if="column.key === 'content'">
            <div class="content-preview">{{ record.content }}</div>
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-button type="link" size="small" @click="handleEdit(record)">编辑</a-button>
            <a-popconfirm
              title="确定删除该配置？"
              ok-text="删除"
              cancel-text="取消"
              @confirm="handleDelete(record.id)"
            >
              <a-button type="link" size="small" danger>删除</a-button>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 新增/编辑弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="isEdit ? '编辑分享配置' : '新增分享配置'"
      :confirm-loading="saving"
      @ok="handleSave"
      @cancel="closeModal"
      width="640px"
    >
      <a-form
        v-if="form"
        :model="form"
        :rules="rules"
        ref="formRef"
        layout="vertical"
      >
        <a-form-item label="场景标识" name="sceneKey">
          <a-select
            v-model:value="form.sceneKey"
            placeholder="请选择分享场景"
            :disabled="isEdit"
            :options="sceneOptions"
          />
        </a-form-item>
        <a-form-item label="配置标题" name="title">
          <a-input v-model:value="form.title" placeholder="管理端展示用，如：抽奖活动分享文案" />
        </a-form-item>
        <a-form-item label="分享文案" name="content">
          <a-textarea
            v-model:value="form.content"
            :rows="6"
            placeholder="请输入分享文案，支持占位符"
          />
          <p class="form-tip">可用占位符：{title} 活动标题、{url} 当前页面链接、{code} 邀请码/兑换码。</p>
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="启用状态" name="enabled">
              <a-radio-group v-model:value="form.enabled">
                <a-radio :value="1">启用</a-radio>
                <a-radio :value="0">禁用</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="排序" name="sortOrder">
              <a-input-number v-model:value="form.sortOrder" :min="0" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  listShareConfigs,
  createShareConfig,
  updateShareConfig,
  deleteShareConfig
} from '@/api/shareConfig.js'

const items = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const loading = ref(false)
const modalVisible = ref(false)
const saving = ref(false)
const formRef = ref()
const isEdit = ref(false)

const form = reactive({
  id: null,
  sceneKey: undefined,
  title: '',
  content: '',
  enabled: 1,
  sortOrder: 0
})

const sceneOptions = [
  { value: 'lottery', label: '抽奖活动 (lottery)' },
  { value: 'invite', label: '邀请有礼 (invite)' }
]

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 70 },
  { title: '场景', dataIndex: 'sceneKey', key: 'sceneKey', width: 140 },
  { title: '标题', dataIndex: 'title', key: 'title', width: 180 },
  { title: '文案', dataIndex: 'content', key: 'content', ellipsis: true },
  { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder', width: 80 },
  { title: '状态', dataIndex: 'enabled', key: 'enabled', width: 90 },
  { title: '更新时间', dataIndex: 'updatedAt', key: 'updatedAt', width: 170 },
  { title: '操作', key: 'actions', width: 140, fixed: 'right' }
]

const rules = {
  sceneKey: [{ required: true, message: '请选择场景标识' }],
  title: [{ required: true, message: '请输入配置标题' }],
  content: [{ required: true, message: '请输入分享文案' }],
  enabled: [{ required: true, message: '请选择启用状态' }]
}

const fetchList = async () => {
  loading.value = true
  try {
    const res = await listShareConfigs({ page: page.value, pageSize: pageSize.value })
    items.value = res.items || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

const handleTableChange = (pagination) => {
  page.value = pagination.current
  pageSize.value = pagination.pageSize
  fetchList()
}

const resetForm = () => {
  form.id = null
  form.sceneKey = undefined
  form.title = ''
  form.content = ''
  form.enabled = 1
  form.sortOrder = 0
}

const handleCreate = () => {
  isEdit.value = false
  resetForm()
  modalVisible.value = true
}

const handleEdit = (record) => {
  isEdit.value = true
  Object.assign(form, record)
  modalVisible.value = true
}

const closeModal = () => {
  modalVisible.value = false
  resetForm()
  formRef.value?.resetFields()
}

const handleSave = async () => {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = {
      sceneKey: form.sceneKey,
      title: form.title,
      content: form.content,
      enabled: form.enabled,
      sortOrder: form.sortOrder
    }
    if (isEdit.value) {
      await updateShareConfig(form.id, payload)
    } else {
      await createShareConfig(payload)
    }
    message.success('保存成功')
    closeModal()
    fetchList()
  } finally {
    saving.value = false
  }
}

const handleDelete = async (id) => {
  await deleteShareConfig(id)
  message.success('删除成功')
  fetchList()
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.share-config-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.list-card {
  border-radius: 8px;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.list-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 4px 0;
}

.list-desc {
  font-size: 13px;
  color: #8c8c8c;
  margin: 0;
}

.content-preview {
  max-width: 320px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.form-tip {
  font-size: 12px;
  color: #8c8c8c;
  margin: 4px 0 0;
}
</style>
