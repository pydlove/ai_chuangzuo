<template>
  <div class="feedback-admin">
    <a-card :bordered="false">
      <div class="page-header">
        <h3 class="page-title">用户反馈</h3>
        <p class="page-desc">查看用户提交的问题与建议，回复后用户会在消息中心收到通知</p>
      </div>

      <a-tabs v-model:active-key="activeTab" @change="reload">
        <a-tab-pane key="0" tab="待回复" />
        <a-tab-pane key="1" tab="已回复" />
      </a-tabs>

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
          <template v-if="column.key === 'userEmail'">
            <div class="cell-user">
              <div>{{ record.userEmail || '—' }}</div>
              <div class="cell-biz-no">{{ record.userBizNo }}</div>
            </div>
          </template>
          <template v-else-if="column.key === 'type'">
            <a-tag color="blue">{{ record.type }}</a-tag>
          </template>
          <template v-else-if="column.key === 'content'">
            <span class="cell-ellipsis" :title="record.content">{{ record.content }}</span>
          </template>
          <template v-else-if="column.key === 'createdAt'">
            {{ formatTime(record.createdAt) }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a @click="openReply(record)">
              {{ record.status === 0 ? '回复' : '查看' }}
            </a>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-drawer
      v-model:open="drawerOpen"
      :width="600"
      :title="current ? `反馈 #${current.id}` : ''"
    >
      <div v-if="current" class="reply-panel">
        <div class="reply-row">
          <span class="reply-label">用户</span>
          <span>
            {{ current.userEmail || '—' }}
            <span class="reply-biz-no">{{ current.userBizNo }}</span>
          </span>
        </div>
        <div class="reply-row">
          <span class="reply-label">类型</span>
          <a-tag color="blue">{{ current.type }}</a-tag>
        </div>
        <div class="reply-row reply-row-stack">
          <span class="reply-label">反馈内容</span>
          <pre class="reply-content">{{ current.content }}</pre>
        </div>
        <div class="reply-row">
          <span class="reply-label">联系方式</span>
          <span>{{ current.contact || '—' }}</span>
        </div>
        <div class="reply-row">
          <span class="reply-label">提交时间</span>
          <span>{{ formatTime(current.createdAt) }}</span>
        </div>
        <a-divider />
        <template v-if="current.status === 1">
          <div class="reply-row reply-row-stack">
            <span class="reply-label">管理员回复</span>
            <pre class="reply-content reply-content-admin">{{ current.replyContent }}</pre>
          </div>
          <div class="reply-row">
            <span class="reply-label">回复时间</span>
            <span>{{ formatTime(current.repliedAt) }}</span>
          </div>
        </template>
        <template v-else>
          <div class="reply-row reply-row-stack">
            <span class="reply-label">回复内容</span>
            <a-textarea
              v-model:value="replyContent"
              :maxlength="2000"
              :rows="6"
              placeholder="请输入回复内容"
              show-count
            />
          </div>
          <a-button
            type="primary"
            :loading="replying"
            class="reply-btn"
            @click="submitReply"
          >
            发送回复
          </a-button>
        </template>
      </div>
    </a-drawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { pageFeedbacks, replyFeedback, getFeedback } from '@/api/feedback.js'

const activeTab = ref('0')
const list = ref([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const size = ref(20)

const drawerOpen = ref(false)
const current = ref(null)
const replyContent = ref('')
const replying = ref(false)

const columns = [
  { title: 'ID', dataIndex: 'id', width: 80 },
  { title: '用户', key: 'userEmail', width: 220 },
  { title: '类型', key: 'type', width: 100 },
  { title: '反馈内容', key: 'content', ellipsis: true },
  { title: '联系方式', dataIndex: 'contact', width: 160 },
  { title: '提交时间', key: 'createdAt', width: 180 },
  { title: '操作', key: 'action', width: 80, fixed: 'right' }
]

const pagination = computed(() => ({
  current: page.value,
  pageSize: size.value,
  total: total.value,
  showTotal: (t) => `共 ${t} 条`,
  showSizeChanger: true
}))

const formatTime = (t) => (t ? new Date(t).toLocaleString('zh-CN') : '-')

const reload = async () => {
  loading.value = true
  try {
    const res = await pageFeedbacks({ status: Number(activeTab.value), page: page.value, size: size.value })
    list.value = res.list
    total.value = res.total
    page.value = res.page
    size.value = res.size
  } catch (e) {
    message.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const onTableChange = (p) => {
  page.value = p.current
  size.value = p.pageSize
  reload()
}

const openReply = async (rec) => {
  try {
    const res = await getFeedback(rec.id)
    current.value = res
    replyContent.value = ''
    drawerOpen.value = true
  } catch (e) {
    message.error(e?.message || '加载详情失败')
  }
}

const submitReply = async () => {
  if (!replyContent.value.trim()) {
    message.warning('请输入回复内容')
    return
  }
  replying.value = true
  try {
    await replyFeedback(current.value.id, { content: replyContent.value })
    message.success('回复已发送，用户会收到通知')
    drawerOpen.value = false
    await reload()
  } catch (e) {
    message.error(e?.message || '回复失败')
  } finally {
    replying.value = false
  }
}

onMounted(reload)
</script>

<style scoped>
.feedback-admin { padding: 16px; }
.page-header { margin-bottom: 16px; }
.page-title { margin: 0 0 4px 0; font-size: 18px; font-weight: 600; }
.page-desc { margin: 0; color: #8c8c8c; font-size: 13px; }
.cell-user { display: flex; flex-direction: column; gap: 2px; }
.cell-biz-no { font-size: 12px; color: #8c8c8c; }
.cell-ellipsis {
  display: inline-block;
  max-width: 360px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
}

.reply-panel { padding: 0 8px; }
.reply-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}
.reply-row-stack { align-items: flex-start; flex-direction: column; gap: 6px; }
.reply-label {
  display: inline-block;
  min-width: 80px;
  color: #8c8c8c;
  font-size: 13px;
}
.reply-content {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  background: #f7f7f7;
  padding: 12px;
  border-radius: 6px;
  font-family: inherit;
  font-size: 13px;
  line-height: 1.7;
  width: 100%;
  box-sizing: border-box;
}
.reply-content-admin { background: #e6f7ff; }
.reply-biz-no { color: #8c8c8c; font-size: 12px; margin-left: 4px; }
.reply-btn { margin-top: 8px; }
</style>
