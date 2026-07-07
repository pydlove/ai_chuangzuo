<template>
  <div class="leaderboard-review">
    <a-card title="收入审核">
      <a-form layout="inline" :model="query" class="filter-bar">
        <a-form-item label="状态">
          <a-select v-model:value="query.status" allow-clear style="width: 120px" placeholder="全部">
            <a-select-option :value="0">待审核</a-select-option>
            <a-select-option :value="1">已通过</a-select-option>
            <a-select-option :value="2">已拒绝</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="reload">查询</a-button>
        </a-form-item>
      </a-form>

      <a-table
        :data-source="state.submissions.items"
        :columns="columns"
        :loading="state.loading"
        row-key="id"
        :pagination="pagination"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="statusColor(record.auditStatus)">{{ statusText(record.auditStatus) }}</a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space v-if="record.auditStatus === 0">
              <a-button size="small" type="primary" @click="handleApprove(record.id)">通过</a-button>
              <a-button size="small" danger @click="openReject(record.id)">拒绝</a-button>
            </a-space>
            <span v-else>-</span>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="rejectVisible" title="拒绝原因" @ok="confirmReject">
      <a-textarea v-model:value="rejectReason" placeholder="请输入拒绝原因" :rows="4" />
    </a-modal>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, computed } from 'vue'
import { useLeaderboardReview } from '@/composables/useLeaderboardReview.js'

const { state, fetchSubmissions, approve, reject } = useLeaderboardReview()

const columns = [
  { title: '申报月份', dataIndex: 'periodMonth', key: 'periodMonth', width: 120 },
  { title: '用户ID', dataIndex: 'userId', key: 'userId', width: 100 },
  { title: '平台', dataIndex: 'platform', key: 'platform', width: 120 },
  { title: '金额（元）', dataIndex: 'amount', key: 'amount', width: 120 },
  { title: '状态', key: 'status', width: 100 },
  { title: '提交时间', dataIndex: 'createdAt', key: 'createdAt', width: 180 },
  { title: '操作', key: 'action', width: 180 }
]

const query = reactive({ status: null, page: 1, size: 20 })
const pagination = computed(() => ({
  current: Number(state.submissions.page) || 1,
  pageSize: Number(state.submissions.size) || 20,
  total: Number(state.submissions.total) || 0,
  showSizeChanger: true
}))

const reload = () => fetchSubmissions({ ...query })
const handleTableChange = (p) => {
  query.page = p.current
  query.size = p.pageSize
  fetchSubmissions({ ...query })
}

const statusText = (status) => {
  const map = { 0: '待审核', 1: '已通过', 2: '已拒绝' }
  return map[status] || status
}
const statusColor = (status) => {
  const map = { 0: 'warning', 1: 'success', 2: 'error' }
  return map[status] || 'default'
}

const rejectVisible = ref(false)
const rejectReason = ref('')
const rejectId = ref(null)

const openReject = (id) => {
  rejectId.value = id
  rejectReason.value = ''
  rejectVisible.value = true
}

const confirmReject = async () => {
  if (!rejectReason.value.trim()) return
  await reject(rejectId.value, rejectReason.value.trim())
  rejectVisible.value = false
}

const handleApprove = async (id) => {
  await approve(id)
}

onMounted(() => {
  reload()
})
</script>

<style scoped>
.leaderboard-review .filter-bar {
  margin-bottom: 16px;
}
</style>
