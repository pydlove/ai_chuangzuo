<template>
  <a-card :bordered="false" class="scheduled-task-admin">
    <div class="page-header">
      <h3 class="page-title">定时任务</h3>
      <p class="page-desc">管理用户端和管理端所有定时任务的执行频率、业务说明与上次执行情况，支持手动触发。</p>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <a-select v-model:value="moduleFilter" style="width: 140px" placeholder="所属端" allow-clear @change="handleSearch">
        <a-select-option value="">全部</a-select-option>
        <a-select-option value="admin">管理端</a-select-option>
        <a-select-option value="user">用户端</a-select-option>
      </a-select>
      <a-button type="primary" :loading="loading" @click="handleSearch">刷新</a-button>
    </div>

    <!-- 任务列表 -->
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
        <template v-if="column.key === 'module'">
          <a-tag :color="record.module === 'admin' ? 'blue' : 'green'">
            {{ record.module === 'admin' ? '管理端' : '用户端' }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'trigger'">
          <div class="trigger-cell">
            <a-tag>{{ record.triggerType === 'cron' ? 'Cron' : '固定间隔' }}</a-tag>
            <span class="trigger-expr">{{ record.expression }}</span>
          </div>
        </template>
        <template v-else-if="column.key === 'lastRun'">
          <div v-if="record.lastRunAt" class="last-run">
            <a-tag :color="statusColor(record.lastRunStatus)">{{ statusText(record.lastRunStatus) }}</a-tag>
            <div class="last-run-time">{{ formatTime(record.lastRunAt) }}</div>
            <div v-if="record.lastRunMessage" class="last-run-msg" :title="record.lastRunMessage">{{ record.lastRunMessage }}</div>
          </div>
          <span v-else class="no-run">未执行</span>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-button type="link" size="small" :loading="triggering[record.id]" @click="handleTrigger(record)">
            手动触发
          </a-button>
          <a-button type="link" size="small" @click="handleViewLogs(record)">执行记录</a-button>
        </template>
      </template>
    </a-table>

    <!-- 执行记录弹窗 -->
    <a-modal
      v-model:open="logModalOpen"
      title="执行记录"
      width="720px"
      :footer="null"
      @cancel="logModalOpen = false"
    >
      <a-table
        :columns="logColumns"
        :data-source="logList"
        :loading="logLoading"
        :pagination="false"
        row-key="id"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'triggerType'">
            <a-tag>{{ record.triggerType === 'manual' ? '手动' : '自动' }}</a-tag>
          </template>
          <template v-else-if="column.key === 'runStatus'">
            <a-tag :color="statusColor(record.runStatus)">{{ statusText(record.runStatus) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'time'">
            <div>{{ formatTime(record.startedAt) }}</div>
            <div v-if="record.finishedAt" class="time-finished">结束 {{ formatTime(record.finishedAt) }}</div>
          </template>
        </template>
        <template #emptyText>
          <div class="empty-logs">暂无执行记录</div>
        </template>
      </a-table>
    </a-modal>
  </a-card>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { getScheduledTasks, triggerScheduledTask, getScheduledTaskLogs } from '@/api/scheduledTask.js'

const moduleFilter = ref('')
const list = ref([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const triggering = ref({})

const columns = [
  { title: '任务名称', dataIndex: 'taskName', key: 'taskName', width: 180 },
  { title: '所属端', key: 'module', width: 90 },
  { title: '触发频率', key: 'trigger', width: 220 },
  { title: '业务说明', dataIndex: 'description', key: 'description', ellipsis: true, width: 300 },
  { title: '上次执行', key: 'lastRun', width: 220 },
  { title: '操作', key: 'action', width: 150, fixed: 'right' }
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

function statusColor(status) {
  switch (status) {
    case 'success': return 'green'
    case 'running': return 'blue'
    case 'failed': return 'red'
    case 'PARTIAL': return 'orange'
    default: return 'default'
  }
}

function statusText(status) {
  switch (status) {
    case 'success': return '成功'
    case 'running': return '执行中'
    case 'failed': return '失败'
    case 'PARTIAL': return '部分成功'
    default: return status || '未知'
  }
}

async function reload() {
  loading.value = true
  try {
    const params = { page: page.value, pageSize: pageSize.value }
    if (moduleFilter.value) params.module = moduleFilter.value
    const data = await getScheduledTasks(params)
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

function onTableChange(p) {
  page.value = p.current
  pageSize.value = p.pageSize
  reload()
}

async function handleTrigger(record) {
  triggering.value[record.id] = true
  try {
    await triggerScheduledTask(record.id)
    message.success('任务已触发，稍后刷新查看结果')
    setTimeout(() => reload(), 1500)
  } catch (e) {
    // handled by interceptor
  } finally {
    triggering.value[record.id] = false
  }
}

const logModalOpen = ref(false)
const logLoading = ref(false)
const logList = ref([])
const currentTask = ref(null)

const logColumns = [
  { title: '触发方式', key: 'triggerType', width: 90 },
  { title: '状态', key: 'runStatus', width: 90 },
  { title: '执行时间', key: 'time', width: 170 },
  { title: '结果', dataIndex: 'message', key: 'message', ellipsis: true }
]

async function handleViewLogs(record) {
  currentTask.value = record
  logModalOpen.value = true
  logLoading.value = true
  try {
    // 用户端任务在 admin 侧不保留日志，直接返回空列表
    if (record.module === 'user') {
      logList.value = []
      return
    }
    logList.value = await getScheduledTaskLogs(record.id)
  } catch (e) {
    logList.value = []
  } finally {
    logLoading.value = false
  }
}

onMounted(() => {
  reload()
})
</script>

<style scoped>
.scheduled-task-admin {
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

.filter-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  align-items: center;
}

.trigger-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.trigger-expr {
  font-family: monospace;
  color: #595959;
}

.last-run {
  line-height: 1.5;
}

.last-run-time {
  font-size: 12px;
  color: #8c8c8c;
}

.last-run-msg {
  font-size: 12px;
  color: #595959;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 180px;
}

.no-run {
  color: #bfbfbf;
}

.time-finished {
  font-size: 12px;
  color: #8c8c8c;
}

.empty-logs {
  text-align: center;
  color: #8c8c8c;
  padding: 24px 0;
}
</style>
