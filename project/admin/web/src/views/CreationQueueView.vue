<template>
  <div class="creation-queue">
    <a-card :bordered="false">
      <div class="page-header">
        <h3 class="page-title">创作队列</h3>
        <p class="page-desc">
          展示用户提交的创作任务，4 个 tab 分别对应：执行中（processing）/ 排队中（queued）/ 已完成（completed）/ 未执行（failed）。
          每 5 秒自动刷新；点表格里的操作可手动重试 / 释放 lease / 标记失败。
        </p>
      </div>

      <a-tabs :active-key="activeTabKey" @change="onTabChange">
        <a-tab-pane key="processing" tab="执行中" />
        <a-tab-pane key="queued" tab="排队中" />
        <a-tab-pane key="completed" tab="已完成" />
        <a-tab-pane key="failed" tab="未执行" />
      </a-tabs>

      <div class="toolbar">
        <a-input
          v-model:value="keyword"
          placeholder="按业务号 / 用户昵称搜索"
          allow-clear
          style="width: 280px"
          @press-enter="handleSearch"
        />
        <a-button type="primary" @click="handleSearch">查询</a-button>
        <a-button @click="handleReset">重置</a-button>
        <a-button @click="refresh">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
        <span class="auto-refresh-tip">5s 自动刷新中</span>
      </div>

      <a-table
        :columns="columns"
        :data-source="list"
        :loading="loading"
        :pagination="false"
        :scroll="{ x: 1440 }"
        row-key="id"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'bizNo'">
            <span class="biz-no">{{ record.bizNo }}</span>
          </template>
          <template v-else-if="column.key === 'user'">
            <span>{{ record.userNickname || '-' }}</span>
            <span class="user-id">ID: {{ record.userId }}</span>
          </template>
          <template v-else-if="column.key === 'waiting'">
            <span>{{ formatSeconds(record.waitingSeconds) }}</span>
          </template>
          <template v-else-if="column.key === 'retry'">
            <a-tag :color="record.retryCount >= (record.maxRetry || 3) ? 'red' : 'blue'">
              {{ record.retryCount || 0 }} / {{ record.maxRetry || 3 }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'failedReason'">
            <a-tooltip v-if="record.failedReason" :title="record.failedReason">
              <span class="reason-cell">{{ truncate(record.failedReason, 60) }}</span>
            </a-tooltip>
            <span v-else>-</span>
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-space v-if="activeTabKey !== 'completed'">
              <a-button
                type="link"
                size="small"
                @click="openCallLogs(record)"
              >执行过程</a-button>
              <a-button
                v-if="record.status !== 2"
                type="link"
                size="small"
                @click="handleRetry(record.id)"
              >重试</a-button>
              <a-button
                v-if="record.status === 1"
                type="link"
                size="small"
                @click="handleRelease(record.id)"
              >释放 lease</a-button>
              <a-button
                v-if="record.status !== 2"
                type="link"
                size="small"
                danger
                @click="handleMarkFailed(record.id)"
              >标记失败</a-button>
            </a-space>
            <span v-else>-</span>
          </template>
        </template>
      </a-table>

      <!-- 执行过程抽屉：12 阶段时间线（仅 AI 阶段有调用日志） -->
      <a-drawer
        v-model:open="callLogDrawer.open"
        title="执行过程"
        width="600"
        :body-style="{ paddingTop: '8px' }"
        @close="onDrawerClose"
      >
        <template #extra>
          <span v-if="callLogDrawer.task" class="drawer-biz">
            {{ callLogDrawer.task.bizNo }}
          </span>
        </template>

        <a-spin :spinning="callLogDrawer.loading">
          <a-alert
            v-if="callLogDrawer.task"
            type="info"
            show-icon
            class="drawer-tip"
            :message="`规则/直通阶段（意图锚定、韵律检测、字数统计、导出渲染）不产生 AI 调用，故无单独日志；其余 AI 阶段全部可见。`"
          />

          <a-empty
            v-if="!callLogDrawer.loading && !hasAnyCallLog"
            description="该任务暂无调用日志（可能仍在执行中，或尚未开始）"
          />

          <a-timeline v-else class="stage-timeline">
            <a-timeline-item
              v-for="s in stageView"
              :key="s.meta.index"
              :color="s.color"
            >
              <div class="stage-head">
                <span class="stage-name">{{ s.meta.index }}. {{ s.meta.name }}</span>
                <a-tag :color="s.tagColor" class="stage-status">{{ s.status }}</a-tag>
                <span v-if="s.meta.ai && s.attempts" class="stage-dur">
                  {{ formatMs(s.totalDuration) }}
                </span>
                <span v-if="s.attempts > 1" class="stage-attempts">
                  尝试 {{ s.attempts }} 次
                </span>
              </div>

              <div v-if="!s.meta.ai" class="stage-note">规则/直通处理，无 AI 调用</div>

              <div v-if="s.meta.ai && s.attempts === 0" class="stage-note">未执行</div>

              <div v-if="s.errorText" class="stage-error">{{ s.errorText }}</div>

              <a-collapse
                v-if="s.meta.ai && s.attempts"
                ghost
                size="small"
                class="stage-collapse"
              >
                <a-collapse-panel key="detail" header="查看 prompt / AI 返回">
                  <div
                    v-for="log in s.logs"
                    :key="log.id"
                    class="attempt-block"
                  >
                    <div class="attempt-title">
                      第 {{ log.attempt }} 次 ·
                      <span :class="log.success ? 'ok' : 'ng'">
                        {{ log.success ? '成功' : '失败' }}
                      </span>
                      · {{ formatMs(log.durationMs) }} · {{ formatTime(log.calledAt) }}
                    </div>
                    <div v-if="log.error" class="attempt-error">{{ log.error }}</div>
                    <div class="attempt-label">发送 prompt</div>
                    <pre class="attempt-preview">{{ log.userMsg || '-' }}</pre>
                    <div class="attempt-label">AI 返回</div>
                    <pre class="attempt-preview">{{ log.responseContent || '-' }}</pre>
                  </div>
                </a-collapse-panel>
              </a-collapse>
            </a-timeline-item>
          </a-timeline>
        </a-spin>
      </a-drawer>

      <div class="pagination">
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
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import { useCreationQueue } from '@/composables/useCreationQueue.js'
import { getGenerationCallLogsGrouped } from '@/api/creationQueue.js'

const {
  list,
  total,
  loading,
  page,
  pageSize,
  keyword,
  activeStatus,
  fetch,
  switchTab,
  handleSearch,
  handleReset,
  handlePageChange,
  refresh,
  handleRetry,
  handleRelease,
  handleMarkFailed,
  startAutoRefresh
} = useCreationQueue()

// ===== 执行过程抽屉 =====
// 12 阶段元信息（与后端 PipelineStage 一一对应）。ai=true 的阶段才产生调用日志。
const STAGE_META = [
  { index: 1, key: 'intent_anchor', name: '意图锚定', ai: false },
  { index: 2, key: 'outline', name: '结构骨架', ai: true },
  { index: 3, key: 'material_list', name: '素材清单', ai: true },
  { index: 4, key: 'draft', name: '分块初稿', ai: true },
  { index: 5, key: 'rhythm_detect', name: '韵律检测', ai: false },
  { index: 6, key: 'rhythm_rewrite', name: '韵律改写', ai: true },
  { index: 7, key: 'external_review', name: '外部审视', ai: true },
  { index: 8, key: 'targeted_rewrite', name: '定向改写', ai: true },
  { index: 9, key: 'rhythm_polish', name: '节奏打磨', ai: true },
  { index: 10, key: 'word_count', name: '字数统计', ai: false },
  { index: 11, key: 'word_adjust', name: '字数调整', ai: true },
  { index: 12, key: 'export_render', name: '导出模板渲染', ai: false }
]

const POLL_INTERVAL_MS = 5000

const callLogDrawer = reactive({
  open: false,
  loading: false,
  task: null,
  grouped: {}, // { "2": [log,...], ... }
  taskStatus: null,
  pollTimer: null
})

const fetchCallLogs = async () => {
  if (!callLogDrawer.task) return
  try {
    const data = await getGenerationCallLogsGrouped(callLogDrawer.task.id)
    callLogDrawer.grouped = data?.grouped || {}
    callLogDrawer.taskStatus = data?.taskStatus ?? null
    if (callLogDrawer.taskStatus === 2) {
      stopPoll()
    }
  } catch (e) {
    message.error(e.message || '加载调用日志失败')
  }
}

const startPoll = () => {
  stopPoll()
  callLogDrawer.pollTimer = setInterval(() => {
    if (document.visibilityState !== 'visible') return
    if (!callLogDrawer.open || callLogDrawer.taskStatus === 2) {
      stopPoll()
      return
    }
    fetchCallLogs()
  }, POLL_INTERVAL_MS)
}

const stopPoll = () => {
  if (callLogDrawer.pollTimer) {
    clearInterval(callLogDrawer.pollTimer)
    callLogDrawer.pollTimer = null
  }
}

const onDrawerClose = () => {
  stopPoll()
}

const openCallLogs = async (record) => {
  callLogDrawer.task = record
  callLogDrawer.grouped = {}
  callLogDrawer.taskStatus = record.status ?? null
  callLogDrawer.open = true
  callLogDrawer.loading = true
  try {
    await fetchCallLogs()
    if (callLogDrawer.taskStatus !== 2) {
      startPoll()
    }
  } finally {
    callLogDrawer.loading = false
  }
}

// 把分组日志 + stage 元信息合并成时间线视图模型
const stageView = computed(() => {
  const grouped = callLogDrawer.grouped || {}
  return STAGE_META.map((meta) => {
    const logs = grouped[meta.index] || grouped[String(meta.index)] || []
    const attempts = logs.length
    const anySuccess = logs.some((l) => l.success)
    const totalDuration = logs.reduce((sum, l) => sum + (l.durationMs || 0), 0)
    const lastFailed = [...logs].reverse().find((l) => !l.success)

    let status
    let color
    let tagColor
    if (!meta.ai) {
      status = '规则/直通'
      color = 'gray'
      tagColor = 'default'
    } else if (attempts === 0) {
      status = '未执行'
      color = 'gray'
      tagColor = 'default'
    } else if (anySuccess) {
      status = '成功'
      color = 'green'
      tagColor = 'success'
    } else {
      status = '失败'
      color = 'red'
      tagColor = 'error'
    }

    return {
      meta,
      logs,
      attempts,
      totalDuration,
      status,
      color,
      tagColor,
      errorText: lastFailed ? lastFailed.error : ''
    }
  })
})

const hasAnyCallLog = computed(() =>
  Object.values(callLogDrawer.grouped || {}).some((arr) => Array.isArray(arr) && arr.length > 0)
)

const formatMs = (ms) => {
  if (ms == null) return '-'
  if (ms < 1000) return `${ms}ms`
  const s = ms / 1000
  if (s < 60) return `${s.toFixed(1)}s`
  const m = Math.floor(s / 60)
  const rs = Math.round(s % 60)
  return `${m}m${rs}s`
}

const formatTime = (t) => {
  if (!t) return '-'
  // calledAt 形如 2026-07-14T14:18:29，取时分秒即可
  const str = String(t)
  const m = str.match(/T(\d{2}:\d{2}:\d{2})/)
  return m ? m[1] : str
}

// 当前 active tab key
const activeTabKey = computed(() => {
  if (activeStatus.value === 0) return 'queued'
  if (activeStatus.value === 1) return 'processing'
  if (activeStatus.value === 3) return 'failed'
  return 'processing'
})

const onTabChange = (key) => switchTab(key)

const columns = computed(() => {
  const base = [
    { title: '业务号', dataIndex: 'bizNo', key: 'bizNo', width: 180 },
    { title: '用户', key: 'user', width: 180 },
    { title: '状态', key: 'status', width: 100,
      customRender: ({ record }) => statusTag(record.status) },
    { title: '目标字数', dataIndex: 'wordLimitTarget', key: 'wordLimitTarget', width: 90 },
    { title: '已等待 / 已耗时', key: 'waiting', width: 130 },
    { title: '重试', key: 'retry', width: 90 },
    { title: '失败原因', key: 'failedReason', width: 200 },
    { title: '提交时间', dataIndex: 'createdAt', key: 'createdAt', width: 170 },
    { title: '操作', key: 'actions', fixed: 'right', width: 300 }
  ]
  return base
})

const statusTag = (s) => {
  if (s === 0) return '排对中'
  if (s === 1) return '执行中'
  if (s === 2) return '已完成'
  if (s === 3) return '未执行'
  return '-'
}

const formatSeconds = (sec) => {
  if (sec == null) return '-'
  if (sec < 60) return `${sec}s`
  if (sec < 3600) return `${Math.floor(sec / 60)}m${sec % 60}s`
  const h = Math.floor(sec / 3600)
  const m = Math.floor((sec % 3600) / 60)
  return `${h}h${m}m`
}

const truncate = (s, n) => {
  if (!s) return ''
  return s.length > n ? s.slice(0, n) + '…' : s
}

onMounted(() => {
  fetch()
  startAutoRefresh()
})

onBeforeUnmount(() => {
  stopPoll()
})
</script>

<style scoped>
.creation-queue :deep(.ant-table-row) {
  background: #fff;
}
.page-header {
  margin-bottom: 16px;
}
.page-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 4px;
}
.page-desc {
  color: #8c8c8c;
  font-size: 13px;
  margin: 0 0 16px;
}
.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  align-items: center;
}
.auto-refresh-tip {
  margin-left: auto;
  color: #52c41a;
  font-size: 12px;
}
.biz-no {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 12px;
  color: #262626;
}
.user-id {
  margin-left: 6px;
  color: #8c8c8c;
  font-size: 12px;
}
.reason-cell {
  color: #cf1322;
  font-size: 12px;
}
.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

/* ===== 执行过程抽屉 ===== */
.drawer-biz {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 12px;
  color: #8c8c8c;
}
.drawer-tip {
  margin-bottom: 12px;
}
.stage-timeline {
  margin-top: 8px;
  padding-left: 4px;
}
.stage-head {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.stage-name {
  font-weight: 600;
  color: #262626;
}
.stage-status {
  margin: 0;
}
.stage-dur {
  font-size: 12px;
  color: #8c8c8c;
}
.stage-attempts {
  font-size: 12px;
  color: #fa8c16;
}
.stage-note {
  font-size: 12px;
  color: #bfbfbf;
  margin-top: 2px;
}
.stage-error {
  font-size: 12px;
  color: #cf1322;
  margin-top: 4px;
  white-space: pre-wrap;
  word-break: break-all;
}
.stage-collapse {
  margin-top: 4px;
}
.stage-collapse :deep(.ant-collapse-header) {
  padding: 4px 0;
  font-size: 12px;
  color: #1677ff;
}
.attempt-block {
  border-left: 2px solid #f0f0f0;
  padding: 4px 0 8px 12px;
  margin-bottom: 8px;
}
.attempt-title {
  font-size: 12px;
  color: #595959;
  margin-bottom: 4px;
}
.attempt-title .ok {
  color: #52c41a;
}
.attempt-title .ng {
  color: #cf1322;
}
.attempt-error {
  font-size: 12px;
  color: #cf1322;
  margin-bottom: 6px;
  white-space: pre-wrap;
  word-break: break-all;
}
.attempt-label {
  font-size: 11px;
  color: #8c8c8c;
  margin: 6px 0 2px;
}
.attempt-preview {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 11px;
  color: #434343;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  padding: 6px 8px;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 600px;
  overflow-y: auto;
}
</style>
