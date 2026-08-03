<template>
  <div class="hot-search-config">
    <a-row :gutter="[16, 16]">
      <a-col :xs="24" :lg="14">
        <a-card title="抓取配置">
          <a-form layout="vertical" :model="form" :rules="rules" ref="formRef">
            <a-form-item label="cron 表达式" name="cron" :extra="cronDescription">
              <a-input v-model:value="form.cron" placeholder="0 0 2 * * ?" />
              <div style="margin-top: 8px">
                <a-space wrap>
                  <a-button size="small" @click="applyPreset('0 0 2 * * ?')">每天 02:00</a-button>
                  <a-button size="small" @click="applyPreset('0 0 8 * * ?')">每天 08:00</a-button>
                  <a-button size="small" @click="applyPreset('0 0 15 * * ?')">每天 15:00</a-button>
                  <a-button size="small" @click="applyPreset('0 0 0 * * ?')">每天 00:00</a-button>
                </a-space>
              </div>
            </a-form-item>
            <a-form-item label="启用定时抓取" name="enabled">
              <a-switch v-model:checked="enabledBool" />
            </a-form-item>
            <a-form-item label="每个平台前 N 条" name="topN">
              <a-input-number v-model:value="form.topN" :min="1" :max="200" />
            </a-form-item>
            <a-form-item label="连接超时 (ms)" name="connectTimeoutMillis">
              <a-input-number v-model:value="form.connectTimeoutMillis" :min="100" />
            </a-form-item>
            <a-form-item label="读取超时 (ms)" name="readTimeoutMillis">
              <a-input-number v-model:value="form.readTimeoutMillis" :min="100" />
            </a-form-item>
            <a-form-item>
              <a-space>
                <a-button type="primary" @click="handleSave">保存配置</a-button>
                <a-button @click="handleCrawlNow" :loading="crawling">立即抓取一次</a-button>
              </a-space>
            </a-form-item>
          </a-form>
        </a-card>
      </a-col>
      <a-col :xs="24" :lg="10">
        <a-card title="上次抓取摘要">
          <template v-if="state.lastRun.lastRunAt">
            <p>抓取时间：{{ formatTime(state.lastRun.lastRunAt) }}</p>
            <p>总条数：{{ state.lastRun.totalFetched }}</p>
            <p>成功：<a-tag color="green">{{ state.lastRun.successCount }}</a-tag> 失败：<a-tag color="red">{{ state.lastRun.failCount }}</a-tag></p>
            <a-divider />
            <a-list
              :data-source="state.lastRun.results"
              size="small"
              :pagination="{ pageSize: 5 }"
            >
              <template #renderItem="{ item }">
                <a-list-item>
                  <a-space>
                    <span>{{ item.platformName || item.platformCode }}</span>
                    <a-tag :color="item.success ? 'green' : 'red'">
                      {{ item.success ? '成功' : '失败' }}
                    </a-tag>
                    <span v-if="item.success">{{ item.fetched }} 条</span>
                    <span v-else style="color:#cf1322">{{ item.error }}</span>
                  </a-space>
                </a-list-item>
              </template>
            </a-list>
          </template>
          <a-empty v-else description="暂无抓取记录" />
        </a-card>
      </a-col>
    </a-row>

    <!-- 执行记录 -->
    <a-row :gutter="[16, 16]" style="margin-top: 16px;">
      <a-col :span="24">
        <a-card title="执行记录" :loading="logsLoading">
          <a-table
            :columns="logColumns"
            :data-source="crawlLogs.items"
            :pagination="{
              current: crawlLogs.page,
              pageSize: crawlLogs.size,
              total: crawlLogs.total,
              showSizeChanger: true,
              pageSizeOptions: ['10', '20', '50']
            }"
            @change="handleLogTableChange"
            row-key="id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'triggerType'">
                <a-tag :color="record.triggerType === 'AUTO' ? 'blue' : 'orange'">
                  {{ record.triggerType === 'AUTO' ? '定时' : '手动' }}
                </a-tag>
              </template>
              <template v-if="column.key === 'status'">
                <a-tag :color="statusColor(record.status)">
                  {{ statusText(record.status) }}
                </a-tag>
              </template>
              <template v-if="column.key === 'time'">
                <div>开始：{{ formatTime(record.startedAt) }}</div>
                <div v-if="record.finishedAt">结束：{{ formatTime(record.finishedAt) }}</div>
              </template>
              <template v-if="column.key === 'count'">
                <span>成功 {{ record.successCount }} / 失败 {{ record.failCount }} / 共 {{ record.totalFetched }} 条</span>
              </template>
              <template v-if="column.key === 'action'">
                <a-button type="link" size="small" @click="showLogDetail(record)">详情</a-button>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>
    </a-row>

    <!-- 执行记录详情 -->
    <a-modal
      v-model:open="logDetailVisible"
      title="执行详情"
      :width="680"
      :footer="null"
    >
      <template v-if="selectedLog">
        <p>
          触发方式：
          <a-tag :color="selectedLog.triggerType === 'AUTO' ? 'blue' : 'orange'">
            {{ selectedLog.triggerType === 'AUTO' ? '定时' : '手动' }}
          </a-tag>
        </p>
        <p>开始时间：{{ formatTime(selectedLog.startedAt) }}</p>
        <p>结束时间：{{ selectedLog.finishedAt ? formatTime(selectedLog.finishedAt) : '-' }}</p>
        <p>状态：<a-tag :color="statusColor(selectedLog.status)">{{ statusText(selectedLog.status) }}</a-tag></p>
        <p>成功 {{ selectedLog.successCount }} / 失败 {{ selectedLog.failCount }} / 总条数 {{ selectedLog.totalFetched }}</p>
        <p v-if="selectedLog.errorMsg" style="color:#cf1322">异常：{{ selectedLog.errorMsg }}</p>
        <a-divider />
        <a-list
          :data-source="selectedLogResults"
          size="small"
        >
          <template #renderItem="{ item }">
            <a-list-item>
              <a-space>
                <span>{{ item.platformName || item.platformCode }}</span>
                <a-tag :color="item.success ? 'green' : 'red'">
                  {{ item.success ? '成功' : '失败' }}
                </a-tag>
                <span v-if="item.success">{{ item.fetched }} 条</span>
                <span v-else style="color:#cf1322">{{ item.error }}</span>
              </a-space>
            </a-list-item>
          </template>
        </a-list>
      </template>
    </a-modal>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, computed } from 'vue'
import { message } from 'ant-design-vue'
import { useHotSearch } from '@/composables/useHotSearch.js'

const { state, crawlLogs, fetchConfig, fetchLastRun, fetchCrawlLogs, saveConfig, crawlNow } = useHotSearch()

const form = reactive({ cron: '', enabled: 1, topN: 50, connectTimeoutMillis: 5000, readTimeoutMillis: 10000 })
const enabledBool = computed({ get: () => form.enabled === 1, set: (v) => (form.enabled = v ? 1 : 0) })
const formRef = ref()
const rules = {
  cron: [
    { required: true, message: '请输入 cron 表达式' },
    { validator: validateCron, trigger: 'blur' }
  ],
  topN: [{ required: true, message: '请输入条数' }]
}

const formatTime = (s) => s ? new Date(s).toLocaleString() : '-'

function validateCron(_, value) {
  if (!value) return Promise.resolve()
  const parts = value.trim().split(/\s+/)
  if (parts.length !== 6 && parts.length !== 7) {
    return Promise.reject(new Error('Spring cron 需要 6 或 7 个字段：秒 分 时 日 月 周 [年]'))
  }
  return Promise.resolve()
}

function applyPreset(cron) {
  form.cron = cron
}

const cronDescription = computed(() => {
  const cron = form.cron?.trim()
  if (!cron) return ''
  const parts = cron.split(/\s+/)
  if (parts.length !== 6 && parts.length !== 7) {
    return '格式错误：需要 6 或 7 个字段（秒 分 时 日 月 周 [年]）'
  }
  const [sec, min, hour, day, month, week] = parts
  try {
    return describeCron(sec, min, hour, day, month, week)
  } catch {
    return ''
  }
})

function describeCron(sec, min, hour, day, month, week) {
  const simpleTime = (h, m, s) => `${pad(h)}:${pad(m)}${s === '0' ? '' : ':' + pad(s)}`
  const pad = (n) => String(n).padStart(2, '0')

  // 每天固定时间
  if (sec === '0' && !min.includes('/') && !hour.includes('/') && day === '*' && month === '*' && week === '?') {
    return `每天 ${simpleTime(hour, min, sec)} 执行`
  }
  // 每小时
  if (sec === '0' && min === '0' && hour === '*' && day === '*' && month === '*' && week === '?') {
    return '每小时执行一次（整点）'
  }
  // 每 N 分钟
  if (sec === '0' && min.startsWith('0/') && hour === '*' && day === '*' && month === '*' && week === '?') {
    const interval = min.split('/')[1]
    return `每 ${interval} 分钟执行一次`
  }
  // 每月固定日
  if (sec === '0' && !min.includes('/') && !hour.includes('/') && day !== '*' && day !== '?' && month === '*' && week === '?') {
    return `每月 ${day} 日 ${simpleTime(hour, min, sec)} 执行`
  }
  // 每周固定星期几（day=? week=MON 等）
  if (sec === '0' && !min.includes('/') && !hour.includes('/') && day === '?' && month === '*' && week !== '?' && week !== '*') {
    return `每周 ${week} ${simpleTime(hour, min, sec)} 执行`
  }
  return '自定义周期'
}

const handleSave = async () => {
  await formRef.value?.validate()
  await saveConfig({ ...form })
  await fetchConfig()
}
const crawling = ref(false)
const handleCrawlNow = async () => {
  crawling.value = true
  try {
    await crawlNow()
    await fetchLastRun()
    await fetchCrawlLogs()
  } finally {
    crawling.value = false
  }
}

const logsLoading = ref(false)
const logColumns = [
  { title: '触发方式', key: 'triggerType' },
  { title: '执行时间', key: 'time' },
  { title: '状态', key: 'status' },
  { title: '结果统计', key: 'count' },
  { title: '操作', key: 'action' }
]

const handleLogTableChange = async (pagination) => {
  crawlLogs.page = pagination.current
  crawlLogs.size = pagination.pageSize
  logsLoading.value = true
  try {
    await fetchCrawlLogs()
  } finally {
    logsLoading.value = false
  }
}

const logDetailVisible = ref(false)
const selectedLog = ref(null)
const selectedLogResults = computed(() => {
  if (!selectedLog.value?.resultsJson) return []
  try {
    return JSON.parse(selectedLog.value.resultsJson)
  } catch {
    return []
  }
})

const showLogDetail = (record) => {
  selectedLog.value = record
  logDetailVisible.value = true
}

const statusColor = (status) => {
  if (status === 'SUCCESS') return 'green'
  if (status === 'PARTIAL') return 'orange'
  return 'red'
}
const statusText = (status) => {
  if (status === 'SUCCESS') return '成功'
  if (status === 'PARTIAL') return '部分成功'
  return '失败'
}

onMounted(async () => {
  await fetchConfig()
  Object.assign(form, state.config)
  await fetchLastRun()
  logsLoading.value = true
  try {
    await fetchCrawlLogs()
  } finally {
    logsLoading.value = false
  }
})
</script>

<style scoped>.hot-search-config { padding: 0; }</style>
