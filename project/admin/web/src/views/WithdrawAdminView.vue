<template>
  <div class="withdraw-admin">
    <a-card :bordered="false">
      <div class="page-header">
        <h3 class="page-title">创作币提现</h3>
        <p class="page-desc">查看并处理用户发起的创作币提现申请，通过或拒绝后用户端会展示处理结果</p>
      </div>

      <div class="toolbar">
        <a-input v-model:value="query.userId" placeholder="用户ID" style="width: 120px" />
        <a-input v-model:value="query.bizNo" placeholder="业务编号" style="width: 220px" />
        <a-select
          v-model:value="query.status"
          placeholder="状态"
          :allow-clear="true"
          style="width: 120px"
        >
          <a-select-option :value="1">审核中</a-select-option>
          <a-select-option :value="2">已通过</a-select-option>
          <a-select-option :value="3">已拒绝</a-select-option>
        </a-select>
        <a-button type="primary" @click="reload">查询</a-button>
        <a-button @click="reset">重置</a-button>
      </div>

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
              <div>{{ record.nickname || '—' }}</div>
              <div class="cell-user-id">ID: {{ record.userId }}</div>
              <div class="cell-user-email">{{ record.email || '—' }}</div>
            </div>
          </template>
          <template v-else-if="column.key === 'amount'">
            <span class="cell-amount">{{ record.amount }}</span>
            <span class="cell-unit"> 创作币</span>
          </template>
          <template v-else-if="column.key === 'account'">
            <div>{{ record.account }}</div>
            <div class="cell-name">{{ record.name }}</div>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="statusColor(record.status)">{{ record.statusText }}</a-tag>
          </template>
          <template v-else-if="column.key === 'createdAt'">
            {{ formatTime(record.createdAt) }}
          </template>
          <template v-else-if="column.key === 'processedAt'">
            {{ formatTime(record.processedAt) || '—' }}
          </template>
          <template v-else-if="column.key === 'action'">
            <template v-if="record.status === 1">
              <a @click="openApprove(record)">通过</a>
              <a-divider type="vertical" />
              <a @click="openReject(record)">拒绝</a>
            </template>
            <span v-else class="cell-done">已处理</span>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="approveVisible"
      title="确认通过提现申请"
      :confirm-loading="processing"
      @ok="submitApprove"
      @cancel="approveVisible = false"
    >
      <p>
        确认通过用户
        <strong>{{ current?.nickname || current?.userId }}</strong>
        的提现申请吗？
      </p>
      <p>提现金额：<strong class="cell-amount">{{ current?.amount }}</strong> 创作币</p>
      <p class="modal-tip">通过后创作币将完成出账，请确保已实际转账到用户支付宝账户。</p>
    </a-modal>

    <a-modal
      v-model:open="rejectVisible"
      title="拒绝提现申请"
      :confirm-loading="processing"
      @ok="submitReject"
      @cancel="rejectVisible = false"
    >
      <p class="modal-row">拒绝用户 <strong>{{ current?.nickname || current?.userId }}</strong> 的提现申请，</p>
      <p class="modal-row">金额 <strong class="cell-amount">{{ current?.amount }}</strong> 创作币将退回用户账户。</p>
      <a-textarea
        v-model:value="rejectRemark"
        :rows="4"
        :maxlength="200"
        placeholder="请输入拒绝原因，将展示给用户"
        show-count
      />
    </a-modal>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { pageWithdrawals, approveWithdraw, rejectWithdraw } from '@/api/withdraw.js'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = ref({
  userId: null,
  bizNo: '',
  status: undefined,
  page: 1,
  size: 20
})

const current = ref(null)
const approveVisible = ref(false)
const rejectVisible = ref(false)
const rejectRemark = ref('')
const processing = ref(false)

const columns = [
  { title: 'ID', dataIndex: 'id', width: 80 },
  { title: '用户', key: 'user', width: 200 },
  { title: '提现金额', key: 'amount', width: 130 },
  { title: '收款账号', key: 'account', width: 200 },
  { title: '状态', key: 'status', width: 100 },
  { title: '申请时间', key: 'createdAt', width: 170 },
  { title: '处理时间', key: 'processedAt', width: 170 },
  { title: '操作', key: 'action', width: 120, fixed: 'right' }
]

const pagination = computed(() => ({
  current: query.value.page,
  pageSize: query.value.size,
  total: total.value,
  showTotal: (t) => `共 ${t} 条`,
  showSizeChanger: true
}))

const formatTime = (t) => (t ? new Date(t).toLocaleString('zh-CN', { hour12: false }) : '')

const statusColor = (status) => {
  switch (status) {
    case 1: return 'warning'
    case 2: return 'success'
    case 3: return 'error'
    default: return 'default'
  }
}

const reload = async () => {
  loading.value = true
  try {
    const params = {
      ...query.value,
      userId: query.value.userId ? Number(query.value.userId) : undefined,
      status: query.value.status === undefined ? undefined : query.value.status
    }
    const res = await pageWithdrawals(params)
    list.value = res.list
    total.value = res.total
    query.value.page = res.page
    query.value.size = res.size
  } catch (e) {
    message.error(e?.message || '加载提现列表失败')
  } finally {
    loading.value = false
  }
}

const reset = () => {
  query.value = { userId: null, bizNo: '', status: undefined, page: 1, size: 20 }
  reload()
}

const onTableChange = (p) => {
  query.value.page = p.current
  query.value.size = p.pageSize
  reload()
}

const openApprove = (record) => {
  current.value = record
  approveVisible.value = true
}

const openReject = (record) => {
  current.value = record
  rejectRemark.value = ''
  rejectVisible.value = true
}

const submitApprove = async () => {
  processing.value = true
  try {
    await approveWithdraw(current.value.bizNo)
    message.success('已通过该提现申请')
    approveVisible.value = false
    await reload()
  } catch (e) {
    message.error(e?.message || '操作失败')
  } finally {
    processing.value = false
  }
}

const submitReject = async () => {
  if (!rejectRemark.value.trim()) {
    message.warning('请输入拒绝原因')
    return
  }
  processing.value = true
  try {
    await rejectWithdraw(current.value.bizNo, rejectRemark.value.trim())
    message.success('已拒绝该提现申请')
    rejectVisible.value = false
    await reload()
  } catch (e) {
    message.error(e?.message || '操作失败')
  } finally {
    processing.value = false
  }
}

onMounted(reload)
</script>

<style scoped>
.withdraw-admin { padding: 16px; }
.page-header { margin-bottom: 16px; }
.page-title { margin: 0 0 4px 0; font-size: 18px; font-weight: 600; }
.page-desc { margin: 0; color: #8c8c8c; font-size: 13px; }
.toolbar { display: flex; gap: 8px; margin-bottom: 16px; }
.cell-user { display: flex; flex-direction: column; gap: 2px; }
.cell-user-id { font-size: 12px; color: #8c8c8c; }
.cell-user-email { font-size: 12px; color: #8c8c8c; }
.cell-amount { color: #ff2442; font-weight: 600; }
.cell-unit { color: #8c8c8c; font-size: 12px; }
.cell-name { font-size: 12px; color: #8c8c8c; }
.cell-done { color: #8c8c8c; }
.modal-tip { color: #8c8c8c; font-size: 13px; margin-top: 12px; }
.modal-row { margin: 0 0 8px; }
</style>
