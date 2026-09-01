<template>
  <div class="experience-token-page">
    <a-card :bordered="false" class="list-card">
      <div class="list-header">
        <div>
          <h3 class="list-title">体验会员管理</h3>
          <p class="list-desc">批量生成一次性体验链接，用户注册后自动获得会员。分享文案请在「分享管理」中配置 experience 场景。</p>
        </div>
        <a-button type="primary" @click="openGenerateModal">+ 批量生成</a-button>
      </div>

      <a-form layout="inline" class="filter-form" @finish="handleSearch">
        <a-form-item label="批次号">
          <a-input v-model:value="query.batchId" placeholder="请输入批次号" allow-clear />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="query.status" placeholder="请选择状态" allow-clear style="width: 120px">
            <a-select-option :value="0">未使用</a-select-option>
            <a-select-option :value="1">已使用</a-select-option>
            <a-select-option :value="2">已过期</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" html-type="submit">查询</a-button>
            <a-button @click="resetQuery">重置</a-button>
          </a-space>
        </a-form-item>
      </a-form>

      <a-table
        :columns="columns"
        :data-source="items"
        :loading="loading"
        :pagination="{ current: query.page, pageSize: query.size, total }"
        row-key="id"
        size="middle"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="statusColor(record.status)">{{ statusText(record.status) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'token'">
            <span class="token-text">{{ record.token }}</span>
          </template>
          <template v-else-if="column.key === 'membershipDays'">
            {{ record.membershipDays }} 天
          </template>
          <template v-else-if="column.key === 'expiresAt'">
            {{ record.expiresAt || '-' }}
          </template>
          <template v-else-if="column.key === 'usedAt'">
            {{ record.usedAt || '-' }}
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-button type="link" size="small" :disabled="record.status !== 0" @click="copyShareText(record.token)">
              复制文案
            </a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 批量生成弹窗 -->
    <a-modal
      v-model:open="generateModalVisible"
      title="批量生成体验会员链接"
      :confirm-loading="generating"
      @ok="handleGenerate"
      @cancel="closeGenerateModal"
      width="520px"
    >
      <a-form
        v-if="generateForm"
        :model="generateForm"
        :rules="generateRules"
        ref="generateFormRef"
        layout="vertical"
      >
        <a-form-item label="生成数量" name="count">
          <a-input-number v-model:value="generateForm.count" :min="1" :max="1000" style="width: 100%" placeholder="单次最多 1000 个" />
        </a-form-item>
        <a-form-item label="套餐类型" name="planKey">
          <a-select v-model:value="generateForm.planKey" placeholder="请选择套餐">
            <a-select-option value="basic">基础版</a-select-option>
            <a-select-option value="pro">专业版</a-select-option>
            <a-select-option value="flagship">旗舰版</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="会员天数" name="membershipDays">
          <a-input-number v-model:value="generateForm.membershipDays" :min="1" style="width: 100%" placeholder="例如：30" />
        </a-form-item>
        <a-form-item label="有效期至" name="expiresAt">
          <a-date-picker v-model:value="generateForm.expiresAt" show-time format="YYYY-MM-DD HH:mm:ss" style="width: 100%" placeholder="留空表示永久有效" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { batchGenerateExperienceTokens, listExperienceTokens, listShareConfigs } from '@/api/experienceToken.js'
import dayjs from 'dayjs'

const items = ref([])
const total = ref(0)
const loading = ref(false)
const generateModalVisible = ref(false)
const generating = ref(false)
const generateFormRef = ref()

const query = reactive({
  batchId: '',
  status: undefined,
  page: 1,
  size: 20
})

const generateForm = reactive({
  count: 10,
  planKey: 'pro',
  membershipDays: 30,
  expiresAt: null
})

const generateRules = {
  count: [{ required: true, message: '请输入生成数量', type: 'number' }],
  planKey: [{ required: true, message: '请选择套餐类型' }],
  membershipDays: [{ required: true, message: '请输入会员天数', type: 'number' }]
}

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 70 },
  { title: '批次号', dataIndex: 'batchId', key: 'batchId', width: 160 },
  { title: 'Token', dataIndex: 'token', key: 'token', width: 180 },
  { title: '套餐', dataIndex: 'planKey', key: 'planKey', width: 100 },
  { title: '会员天数', dataIndex: 'membershipDays', key: 'membershipDays', width: 100 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
  { title: '使用人ID', dataIndex: 'usedByUserId', key: 'usedByUserId', width: 100 },
  { title: '使用时间', dataIndex: 'usedAt', key: 'usedAt', width: 170 },
  { title: '有效期', dataIndex: 'expiresAt', key: 'expiresAt', width: 170 },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 170 },
  { title: '操作', key: 'actions', width: 100, fixed: 'right' }
]

const statusText = (status) => {
  const map = { 0: '未使用', 1: '已使用', 2: '已过期' }
  return map[status] || status
}

const statusColor = (status) => {
  const map = { 0: 'green', 1: 'default', 2: 'red' }
  return map[status] || 'default'
}

const fetchList = async () => {
  loading.value = true
  try {
    const params = {
      batchId: query.batchId || undefined,
      status: query.status,
      page: query.page,
      size: query.size
    }
    const res = await listExperienceTokens(params)
    items.value = res.items || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

const handleTableChange = (pagination) => {
  query.page = pagination.current
  query.size = pagination.pageSize
  fetchList()
}

const handleSearch = () => {
  query.page = 1
  fetchList()
}

const resetQuery = () => {
  query.batchId = ''
  query.status = undefined
  query.page = 1
  fetchList()
}

const openGenerateModal = () => {
  generateForm.count = 10
  generateForm.planKey = 'pro'
  generateForm.membershipDays = 30
  generateForm.expiresAt = null
  generateModalVisible.value = true
}

const closeGenerateModal = () => {
  generateModalVisible.value = false
  generateFormRef.value?.resetFields()
}

const handleGenerate = async () => {
  await generateFormRef.value.validate()
  generating.value = true
  try {
    const payload = {
      count: generateForm.count,
      planKey: generateForm.planKey,
      membershipDays: generateForm.membershipDays,
      expiresAt: generateForm.expiresAt ? generateForm.expiresAt.format('YYYY-MM-DD HH:mm:ss') : null
    }
    const tokens = await batchGenerateExperienceTokens(payload)
    message.success(`成功生成 ${tokens.length} 个体验令牌`)
    closeGenerateModal()
    fetchList()
  } finally {
    generating.value = false
  }
}

const copyShareText = async (token) => {
  try {
    const res = await listShareConfigs({ sceneKey: 'experience', enabled: 1, page: 1, size: 1 })
    const config = (res.items || [])[0]
    if (!config) {
      message.warning('未找到 experience 场景的分享文案，请先在「分享管理」中配置')
      return
    }
    const url = `${window.location.origin}/register?experience=${token}`
    const text = config.content.replace(/\{url\}/g, url)
    await navigator.clipboard.writeText(text)
    message.success('分享文案已复制')
  } catch (err) {
    message.error('复制失败：' + (err.message || '未知错误'))
  }
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.experience-token-page {
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

.filter-form {
  margin-bottom: 16px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
}

.token-text {
  font-family: monospace;
  font-weight: 500;
}
</style>
