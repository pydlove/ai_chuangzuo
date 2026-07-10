<template>
  <div class="creation-queue">
    <a-card :bordered="false">
      <div class="page-header">
        <h3 class="page-title">创作队列</h3>
        <p class="page-desc">
          展示用户提交的创作任务，3 个 tab 分别对应：执行中（processing）/ 排对中（queued）/ 未执行（failed）。
          每 5 秒自动刷新；点表格里的操作可手动重试 / 释放 lease / 标记失败。
        </p>
      </div>

      <a-tabs :active-key="activeTabKey" @change="onTabChange">
        <a-tab-pane key="processing" tab="执行中" />
        <a-tab-pane key="queued" tab="排对中" />
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
        :scroll="{ x: 1380 }"
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
            <a-space>
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
          </template>
        </template>
      </a-table>

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
import { computed, onMounted } from 'vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import { useCreationQueue } from '@/composables/useCreationQueue.js'

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
    { title: '操作', key: 'actions', fixed: 'right', width: 240 }
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
</style>
