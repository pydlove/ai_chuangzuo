<template>
  <a-card :bordered="false" class="audit-log-admin">
    <div class="page-header">
      <h3 class="page-title">操作审计</h3>
      <p class="page-desc">查看用户端操作行为审计日志</p>
    </div>

    <!-- 配置栏 -->
    <a-card title="日志保留配置" size="small" class="config-card">
      <div class="config-row">
        <a-form layout="inline">
          <a-form-item label="保留天数">
            <a-input-number v-model:value="configForm.retentionDays" :min="1" :max="365" />
          </a-form-item>
          <a-form-item label="清理定时">
            <a-input v-model:value="configForm.cleanupCron" style="width: 180px" placeholder="cron 表达式" />
          </a-form-item>
          <a-form-item>
            <a-button type="primary" :loading="savingConfig" @click="saveConfig">保存配置</a-button>
          </a-form-item>
        </a-form>
      </div>
    </a-card>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <a-input-number v-model:value="searchUserId" placeholder="用户ID" style="width: 120px" :min="1" />
      <a-input v-model:value="keyword" placeholder="搜索昵称或邮箱" style="width: 200px" allow-clear @press-enter="handleSearch" />
      <a-range-picker v-model:value="dateRange" style="width: 240px" />
      <a-button type="primary" @click="handleSearch">搜索</a-button>
      <a-button @click="handleReset">重置</a-button>
    </div>

    <!-- 审计日志表格 -->
    <a-table
      :columns="columns"
      :data-source="list"
      :loading="loading"
      :pagination="pagination"
      row-key="id"
      size="middle"
      @change="onTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'user'">
          <div class="cell-user">
            <div>{{ record.nickname || '-' }}</div>
            <div class="cell-user-sub">{{ record.email || '-' }}</div>
          </div>
        </template>
        <template v-else-if="column.key === 'actionType'">
          <a-tag>{{ record.actionType }}</a-tag>
        </template>
        <template v-else-if="column.key === 'statusCode'">
          <a-tag :color="statusColor(record.statusCode)">{{ record.statusCode }}</a-tag>
        </template>
        <template v-else-if="column.key === 'createdAt'">
          {{ formatTime(record.createdAt) }}
        </template>
      </template>
    </a-table>
  </a-card>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { getAuditLogs, getAuditLogConfig, updateAuditLogConfig } from '@/api/auditLog.js'

// ── 搜索 & 列表 ──
const searchUserId = ref(null)
const keyword = ref('')
const dateRange = ref(null)
const list = ref([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)

const columns = [
  { title: '用户', key: 'user', width: 160 },
  { title: '模块', dataIndex: 'module', key: 'module', width: 120 },
  { title: '操作', key: 'actionType', width: 100 },
  { title: '方法', dataIndex: 'requestMethod', key: 'requestMethod', width: 80 },
  { title: 'URI', dataIndex: 'requestUri', key: 'requestUri', ellipsis: true, width: 260 },
  { title: 'IP', dataIndex: 'clientIp', key: 'clientIp', width: 120 },
  { title: '状态码', key: 'statusCode', width: 90 },
  { title: '耗时(ms)', dataIndex: 'durationMs', key: 'durationMs', width: 100 },
  { title: '操作时间', key: 'createdAt', width: 170 }
]

const pagination = computed(() => ({
  current: page.value,
  pageSize: pageSize.value,
  total: total.value,
  showTotal: (t) => `共 ${t} 条`,
  showSizeChanger: true
}))

function formatTime(t) {
  if (!t) return '-'
  return new Date(t).toLocaleString('zh-CN')
}

function statusColor(code) {
  if (code == null) return 'default'
  if (code >= 200 && code < 300) return 'green'
  if (code >= 400) return 'red'
  return 'default'
}

async function reload() {
  loading.value = true
  try {
    const params = { page: page.value, pageSize: pageSize.value }
    if (searchUserId.value) params.userId = searchUserId.value
    if (keyword.value) params.keyword = keyword.value
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0].format('YYYY-MM-DD')
      params.endDate = dateRange.value[1].format('YYYY-MM-DD')
    }
    const data = await getAuditLogs(params)
    list.value = data.list || []
    total.value = data.total || 0
  } catch (e) {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  reload()
}

function handleReset() {
  searchUserId.value = null
  keyword.value = ''
  dateRange.value = null
  page.value = 1
  reload()
}

function onTableChange(p) {
  page.value = p.current
  pageSize.value = p.pageSize
  reload()
}

// ── 配置 ──
const configForm = reactive({ retentionDays: 30, cleanupCron: '0 0 3 * * ?' })
const savingConfig = ref(false)

async function loadConfig() {
  try {
    const cfg = await getAuditLogConfig()
    configForm.retentionDays = cfg.retentionDays
    configForm.cleanupCron = cfg.cleanupCron
  } catch (e) {
    // handled
  }
}

async function saveConfig() {
  if (!configForm.retentionDays || configForm.retentionDays < 1 || configForm.retentionDays > 365) {
    message.warning('保留天数必须在 1-365 之间')
    return
  }
  if (!configForm.cleanupCron || configForm.cleanupCron.trim() === '') {
    message.warning('请填写清理定时表达式')
    return
  }
  savingConfig.value = true
  try {
    await updateAuditLogConfig({
      retentionDays: configForm.retentionDays,
      cleanupCron: configForm.cleanupCron.trim()
    })
    message.success('配置已保存')
    loadConfig()
  } catch (e) {
    // handled
  } finally {
    savingConfig.value = false
  }
}

onMounted(() => {
  loadConfig()
  reload()
})
</script>

<style scoped>
.audit-log-admin {
  padding: 0;
}

.page-header {
  margin-bottom: 16px;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
}

.page-desc {
  color: #8c8c8c;
  font-size: 13px;
  margin: 4px 0 0;
}

.config-card {
  margin-bottom: 16px;
}

.config-row {
  display: flex;
  align-items: center;
}

.search-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
  align-items: center;
}

.cell-user {
  line-height: 1.4;
}

.cell-user-sub {
  font-size: 12px;
  color: #8c8c8c;
}
</style>
