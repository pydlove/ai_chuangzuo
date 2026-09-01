<template>
  <a-card :bordered="false" class="upgrade-management">
    <div class="page-header">
      <h3 class="page-title">升级管理</h3>
      <p class="page-desc">配置脚本根目录与服务器连接信息，可视化执行升级/重启脚本并查看日志。</p>
    </div>

    <!-- 配置卡片 -->
    <a-card title="执行配置" class="config-card" size="small">
      <a-form :model="configForm" :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
        <a-row :gutter="24">
          <a-col :span="12">
            <a-form-item label="脚本根目录">
              <a-input v-model:value="configForm.scriptRootDir" placeholder="/Users/panyong/aio_project/ai_chuangzuo/scripts" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="超时秒数">
              <a-input-number v-model:value="configForm.commandTimeoutSeconds" :min="10" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="24">
          <a-col :span="12">
            <a-form-item label="服务器 IP">
              <a-input v-model:value="configForm.serverIp" placeholder="101.126.15.58" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="SSH 用户名">
              <a-input v-model:value="configForm.serverUser" placeholder="root" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="24">
          <a-col :span="12">
            <a-form-item label="SSH 密码">
              <a-input-password v-model:value="configForm.serverPassword" placeholder="留空表示不修改" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="SSH 密钥路径">
              <a-input v-model:value="configForm.sshKeyPath" placeholder="~/.ssh/id_rsa" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row>
          <a-col :span="24" style="text-align: right">
            <a-button type="primary" :loading="saving" @click="handleSaveConfig">保存配置</a-button>
          </a-col>
        </a-row>
      </a-form>
    </a-card>

    <!-- 脚本列表 / 执行记录 -->
    <a-tabs v-model:activeKey="activeTab" class="upgrade-tabs">
      <template #tabBarExtraContent>
        <a-button type="link" size="small" :loading="scriptLoading || jobLoading" @click="refreshAll">
          <ReloadOutlined />
          刷新
        </a-button>
      </template>
      <a-tab-pane key="scripts" tab="脚本列表">
        <a-table
          :columns="scriptColumns"
          :data-source="scriptList"
          :loading="scriptLoading"
          :pagination="false"
          row-key="relativePath"
          size="middle"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'args'">
              <a-input v-model:value="scriptArgs[record.relativePath]" size="small" placeholder="可选，如 admin-api user-web" />
            </template>
            <template v-else-if="column.key === 'action'">
              <a-button type="primary" size="small" :loading="executing[record.relativePath]" @click="handleExecute(record)">
                执行
              </a-button>
            </template>
          </template>
        </a-table>
      </a-tab-pane>

      <a-tab-pane key="jobs" tab="执行记录">
        <a-table
          :columns="jobColumns"
          :data-source="jobList"
          :loading="jobLoading"
          :pagination="jobPagination"
          row-key="id"
          size="middle"
          @change="onJobTableChange"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'runStatus'">
              <a-tag :color="statusColor(record.runStatus)">{{ statusText(record.runStatus) }}</a-tag>
            </template>
            <template v-else-if="column.key === 'time'">
              <div>{{ formatTime(record.startedAt) }}</div>
              <div v-if="record.finishedAt" class="time-finished">结束 {{ formatTime(record.finishedAt) }}</div>
            </template>
            <template v-else-if="column.key === 'exitCode'">
              <span v-if="record.exitCode !== null && record.exitCode !== undefined">{{ record.exitCode }}</span>
              <span v-else class="text-muted">-</span>
            </template>
            <template v-else-if="column.key === 'action'">
              <a-button type="link" size="small" @click="handleViewLog(record)">查看日志</a-button>
            </template>
          </template>
        </a-table>
      </a-tab-pane>
    </a-tabs>

    <!-- 日志详情抽屉 -->
    <a-drawer
      v-model:open="logDrawerOpen"
      :title="`执行日志 - ${currentJob?.scriptName || ''}`"
      width="720px"
      :footer="null"
      @close="stopPolling"
    >
      <a-alert v-if="currentJob?.outputTruncated" type="warning" show-icon class="log-truncated-alert">
        <template #message>输出过长，已截断显示。完整输出请查看服务器日志文件。</template>
      </a-alert>
      <a-tabs v-model:activeKey="logActiveTab">
        <a-tab-pane key="stdout" tab="标准输出">
          <pre class="log-pre">{{ currentJob?.stdout || '无输出' }}</pre>
        </a-tab-pane>
        <a-tab-pane key="stderr" tab="标准错误">
          <pre class="log-pre error">{{ currentJob?.stderr || '无输出' }}</pre>
        </a-tab-pane>
      </a-tabs>
    </a-drawer>
  </a-card>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import {
  getUpgradeConfig,
  updateUpgradeConfig,
  listUpgradeScripts,
  executeUpgradeScript,
  listUpgradeJobs,
  getUpgradeJob
} from '@/api/upgradeManagement.js'

const activeTab = ref('scripts')
const configForm = ref({
  scriptRootDir: '/Users/panyong/aio_project/ai_chuangzuo/scripts',
  serverIp: '',
  serverUser: '',
  serverPassword: '',
  sshKeyPath: '',
  commandTimeoutSeconds: 600
})
const saving = ref(false)

const scriptList = ref([])
const scriptLoading = ref(false)
const executing = ref({})
const scriptArgs = ref({})

const jobList = ref([])
const jobLoading = ref(false)
const jobPage = ref(1)
const jobPageSize = ref(10)
const jobTotal = ref(0)

const logDrawerOpen = ref(false)
const logActiveTab = ref('stdout')
const currentJob = ref(null)
let pollingTimer = null

const scriptColumns = [
  { title: '分类', dataIndex: 'category', key: 'category', width: 180 },
  { title: '脚本路径', dataIndex: 'relativePath', key: 'relativePath', ellipsis: true },
  { title: '说明', dataIndex: 'description', key: 'description', ellipsis: true, width: 260 },
  { title: '执行参数', key: 'args', width: 180 },
  { title: '操作', key: 'action', width: 100, fixed: 'right' }
]

const jobColumns = [
  { title: '脚本', dataIndex: 'scriptRelativePath', key: 'scriptRelativePath', ellipsis: true },
  { title: '状态', key: 'runStatus', width: 100 },
  { title: '时间', key: 'time', width: 180 },
  { title: '退出码', key: 'exitCode', width: 90 },
  { title: '操作', key: 'action', width: 110, fixed: 'right' }
]

const jobPagination = computed(() => ({
  current: jobPage.value,
  pageSize: jobPageSize.value,
  total: jobTotal.value,
  showTotal: (t) => `共 ${t} 条`,
  showSizeChanger: true
}))

onMounted(() => {
  loadConfig()
  loadScripts()
  loadJobs()
})

onUnmounted(() => {
  stopPolling()
})

async function loadConfig() {
  try {
    const data = await getUpgradeConfig()
    configForm.value = {
      ...data,
      serverPassword: data.serverPassword ? '********' : ''
    }
  } catch (e) {
    message.error(e.message || '加载配置失败')
  }
}

async function handleSaveConfig() {
  saving.value = true
  try {
    await updateUpgradeConfig(configForm.value)
    message.success('配置已保存')
    await loadConfig()
  } catch (e) {
    message.error(e.message || '保存配置失败')
  } finally {
    saving.value = false
  }
}

async function loadScripts() {
  scriptLoading.value = true
  try {
    scriptList.value = await listUpgradeScripts()
  } catch (e) {
    message.error(e.message || '加载脚本列表失败')
  } finally {
    scriptLoading.value = false
  }
}

function refreshAll() {
  loadScripts()
  loadJobs()
}

function handleExecute(record) {
  const argsStr = scriptArgs.value[record.relativePath] || ''
  const args = argsStr.trim().split(/\s+/).filter(Boolean)
  const argsDisplay = args.length > 0 ? `参数：${args.join(' ')}` : '无参数'
  Modal.confirm({
    title: '确认执行脚本？',
    content: `即将执行：${record.relativePath}\n${argsDisplay}，请确认当前不是业务高峰期。`,
    okText: '确认执行',
    cancelText: '取消',
    onOk: () => {
      executing.value[record.relativePath] = true
      executeUpgradeScript(record.relativePath, args)
        .then(({ jobId }) => {
          message.success('脚本已开始执行')
          activeTab.value = 'jobs'
          loadJobs()
          openLogDrawer(jobId)
        })
        .catch((e) => {
          message.error(e.message || '执行失败')
        })
        .finally(() => {
          executing.value[record.relativePath] = false
        })
    }
  })
}

async function loadJobs() {
  jobLoading.value = true
  try {
    const res = await listUpgradeJobs({ pageNum: jobPage.value, pageSize: jobPageSize.value })
    jobList.value = res.records || []
    jobTotal.value = res.total || 0
  } catch (e) {
    message.error(e.message || '加载执行记录失败')
  } finally {
    jobLoading.value = false
  }
}

function onJobTableChange(pagination) {
  jobPage.value = pagination.current
  jobPageSize.value = pagination.pageSize
  loadJobs()
}

async function openLogDrawer(jobId) {
  logDrawerOpen.value = true
  logActiveTab.value = 'stdout'
  await fetchJob(jobId)
  startPolling(jobId)
}

function handleViewLog(record) {
  openLogDrawer(record.id)
}

async function fetchJob(jobId) {
  try {
    currentJob.value = await getUpgradeJob(jobId)
  } catch (e) {
    message.error(e.message || '加载日志失败')
  }
}

function startPolling(jobId) {
  stopPolling()
  pollingTimer = setInterval(async () => {
    await fetchJob(jobId)
    if (currentJob.value?.runStatus && currentJob.value.runStatus !== 'running') {
      stopPolling()
    }
  }, 2000)
}

function stopPolling() {
  if (pollingTimer) {
    clearInterval(pollingTimer)
    pollingTimer = null
  }
}

function statusColor(status) {
  switch (status) {
    case 'success': return 'success'
    case 'failed': return 'error'
    case 'timeout': return 'warning'
    case 'running': return 'processing'
    default: return 'default'
  }
}

function statusText(status) {
  switch (status) {
    case 'success': return '成功'
    case 'failed': return '失败'
    case 'timeout': return '超时'
    case 'running': return '执行中'
    default: return status
  }
}

function formatTime(time) {
  return time ? dayjs(time).format('YYYY-MM-DD HH:mm:ss') : '-'
}
</script>

<style scoped>
.upgrade-management {
  min-height: 100%;
}
.page-header {
  margin-bottom: 20px;
}
.page-title {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 4px;
}
.page-desc {
  color: #8c8c8c;
  margin: 0;
}
.config-card {
  margin-bottom: 24px;
}
.upgrade-tabs {
  margin-top: 8px;
}
.time-finished {
  color: #8c8c8c;
  font-size: 12px;
}
.text-muted {
  color: #bfbfbf;
}
.log-truncated-alert {
  margin-bottom: 12px;
}
.log-pre {
  background: #f6f6f6;
  border-radius: 6px;
  padding: 12px;
  max-height: 60vh;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-all;
  font-family: 'Menlo', 'Monaco', 'Consolas', monospace;
  font-size: 13px;
}
.log-pre.error {
  color: #cf1322;
}
</style>
