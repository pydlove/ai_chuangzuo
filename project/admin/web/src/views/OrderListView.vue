<template>
  <a-card :bordered="false" class="order-admin">
    <div class="page-header">
      <h3 class="page-title">订单列表</h3>
      <p class="page-desc">查看和管理用户订阅订单</p>
    </div>

    <!-- 汇总提示 -->
    <div class="summary-bar">
      <span>订单数：<strong>{{ total }}</strong></span>
      <span>订单总额：<strong>¥{{ totalAmount }}</strong></span>
      <span class="summary-tip">（注意：仅保留最近1年的订单）</span>
      <span class="summary-default">默认仅展示当月订单</span>
    </div>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <a-input
        v-model:value="keyword"
        placeholder="搜索昵称或邮箱"
        style="width: 200px"
        allow-clear
        @press-enter="handleSearch"
      />
      <a-select v-model:value="planKey" placeholder="套餐" style="width: 120px" allow-clear>
        <a-select-option value="basic">基础版</a-select-option>
        <a-select-option value="pro">专业版</a-select-option>
        <a-select-option value="flagship">旗舰版</a-select-option>
      </a-select>
      <a-select v-model:value="statusFilter" placeholder="状态" style="width: 120px" allow-clear>
        <a-select-option :value="0">待支付</a-select-option>
        <a-select-option :value="1">已支付</a-select-option>
        <a-select-option :value="2">已退款</a-select-option>
        <a-select-option :value="3">已取消</a-select-option>
      </a-select>
      <a-range-picker v-model:value="dateRange" style="width: 240px" />
      <a-button type="primary" @click="handleSearch">搜索</a-button>
      <a-button @click="handleReset">重置</a-button>
      <div style="flex: 1" />
      <a-button
        type="primary"
        danger
        ghost
        :disabled="selectedRowKeys.length === 0 || !selectedRows.every((r) => r.status === 0)"
        @click="handleBatchCancel"
      >
        批量取消
      </a-button>
      <a-button
        danger
        ghost
        :disabled="selectedRowKeys.length === 0 || !selectedRows.every((r) => r.status === 3)"
        @click="handleBatchDelete"
      >
        批量删除
      </a-button>
      <a-divider type="vertical" />
      <a-button type="primary" ghost @click="openGrantModal">手动发放会员</a-button>
      <a-button ghost @click="openAdjustModal">手动调整会员</a-button>
    </div>

    <!-- 订单表格 -->
    <a-table
      :columns="columns"
      :data-source="list"
      :loading="loading"
      :pagination="pagination"
      row-key="id"
      size="middle"
      :row-selection="{ selectedRowKeys, onChange: onSelectChange }"
      @change="onTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'id'">
          {{ record.id }}
        </template>
        <template v-else-if="column.key === 'orderNo'">
          {{ record.orderNo }}
        </template>
        <template v-else-if="column.key === 'app'">
          <a class="app-link" @click.prevent>爱创作工坊</a>
        </template>
        <template v-else-if="column.key === 'title'">
          {{ orderTitle(record) }}
        </template>
        <template v-else-if="column.key === 'amount'">
          ¥{{ record.amount }}
        </template>
        <template v-else-if="column.key === 'payment'">
          <div class="cell-payment">
            <a-tag color="blue">{{ paymentMethodLabel(record.paymentMethod) }}</a-tag>
            <div class="cell-payment-line">交易时间：{{ record.paidAt ? formatTime(record.paidAt) : '-' }}</div>
            <div class="cell-payment-line">交易号：{{ record.thirdPartyTradeId || '-' }}</div>
          </div>
        </template>
        <template v-else-if="column.key === 'createdAt'">
          {{ formatTime(record.createdAt) }}
        </template>
        <template v-else-if="column.key === 'status'">
          <a-tag :color="statusColor(record.status)">{{ record.statusName }}</a-tag>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button v-if="record.status === 0" type="link" size="small" @click="handleMarkPaid(record)">标记已支付</a-button>
            <a-button v-if="record.status === 0" type="link" size="small" danger @click="handleCancel(record)">取消</a-button>
            <a-button v-if="record.status === 1" type="link" size="small" danger @click="openRefundModal(record)">退款</a-button>
            <a-button type="link" size="small" @click="openDetailDrawer(record)">详情</a-button>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 退款弹框 -->
    <a-modal v-model:open="refundModalOpen" title="退款" :confirm-loading="refunding" @ok="submitRefund">
      <div style="height: 120px; overflow-y: auto;">
        <p>订单号：{{ refundTarget?.orderNo }}</p>
        <p>金额：¥{{ refundTarget?.amount }}</p>
        <a-textarea v-model:value="refundReason" placeholder="请输入退款原因" :rows="3" style="margin-top: 8px" />
      </div>
    </a-modal>

    <!-- 手动发放会员弹框 -->
    <a-modal v-model:open="grantModalOpen" title="手动发放会员" :confirm-loading="granting" @ok="submitGrant">
      <div style="height: 320px; overflow-y: auto;">
        <a-form layout="vertical">
          <a-form-item label="用户" required>
            <a-select
              v-model:value="grantForm.userId"
              placeholder="搜索昵称或邮箱选择用户"
              style="width: 100%"
              show-search
              :filter-option="false"
              :loading="userSelectLoading"
              @search="onUserSearch"
            >
              <a-select-option v-for="u in userOptions" :key="u.id" :value="u.id">
                {{ u.nickname }}（{{ u.email }}）
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="套餐" required>
            <a-select v-model:value="grantForm.planKey" placeholder="选择套餐" style="width: 100%">
              <a-select-option v-for="p in planOptions" :key="p.planKey" :value="p.planKey">
                {{ p.displayName }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="时间范围" required>
            <a-range-picker v-model:value="grantForm.dateRange" style="width: 100%" />
          </a-form-item>
          <a-form-item label="备注">
            <a-input v-model:value="grantForm.remark" placeholder="选填" />
          </a-form-item>
        </a-form>
      </div>
    </a-modal>

    <!-- 手动调整会员弹框 -->
    <a-modal v-model:open="adjustModalOpen" title="手动调整会员" :confirm-loading="adjusting" @ok="submitAdjust">
      <div style="height: 260px; overflow-y: auto;">
        <a-form layout="vertical">
          <a-form-item label="用户ID" required>
            <a-input-number v-model:value="adjustForm.userId" placeholder="输入用户ID" style="width: 100%" :min="1" />
          </a-form-item>
          <a-form-item label="会员等级" required>
            <a-select v-model:value="adjustForm.level" placeholder="选择等级" style="width: 100%">
              <a-select-option value="basic">基础版</a-select-option>
              <a-select-option value="pro">专业版</a-select-option>
              <a-select-option value="flagship">旗舰版</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="到期日期" required>
            <a-date-picker v-model:value="adjustForm.expiresAt" style="width: 100%" />
          </a-form-item>
          <a-form-item label="备注">
            <a-input v-model:value="adjustForm.remark" placeholder="选填" />
          </a-form-item>
        </a-form>
      </div>
    </a-modal>

    <!-- 订单详情抽屉 -->
    <a-drawer v-model:open="detailDrawerOpen" title="订单详情" :width="480">
      <template v-if="detailData">
        <div class="detail-row"><span class="detail-label">订单号</span><span>{{ detailData.orderNo }}</span></div>
        <div class="detail-row"><span class="detail-label">APP</span><span>爱创作工坊</span></div>
        <div class="detail-row"><span class="detail-label">订单标题</span><span>{{ orderTitle(detailData) }}</span></div>
        <div class="detail-row"><span class="detail-label">用户</span><span>{{ detailData.nickname || '-' }} ({{ detailData.email || '-' }})</span></div>
        <div class="detail-row"><span class="detail-label">套餐</span><span>{{ detailData.planName }}</span></div>
        <div class="detail-row"><span class="detail-label">周期</span><span>{{ detailData.cycleName }}</span></div>
        <div class="detail-row"><span class="detail-label">金额</span><span>¥{{ detailData.amount }}</span></div>
        <div class="detail-row"><span class="detail-label">支付方式</span><span>{{ paymentMethodLabel(detailData.paymentMethod) }}</span></div>
        <div class="detail-row"><span class="detail-label">交易号</span><span>{{ detailData.thirdPartyTradeId || '-' }}</span></div>
        <div class="detail-row"><span class="detail-label">状态</span>
          <a-tag :color="statusColor(detailData.status)">{{ detailData.statusName }}</a-tag>
        </div>
        <div class="detail-row"><span class="detail-label">支付时间</span><span>{{ detailData.paidAt ? formatTime(detailData.paidAt) : '-' }}</span></div>
        <div class="detail-row"><span class="detail-label">退款时间</span><span>{{ detailData.refundedAt ? formatTime(detailData.refundedAt) : '-' }}</span></div>
        <div class="detail-row"><span class="detail-label">退款原因</span><span>{{ detailData.refundReason || '-' }}</span></div>
        <div class="detail-row"><span class="detail-label">备注</span><span>{{ detailData.adminRemark || '-' }}</span></div>
        <div class="detail-row"><span class="detail-label">创建时间</span><span>{{ formatTime(detailData.createdAt) }}</span></div>
      </template>
    </a-drawer>
  </a-card>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import dayjs from 'dayjs'
import { getOrderList, getOrderDetail, markOrderPaid, refundOrder, cancelOrder, batchCancelOrder, batchDeleteOrder, adjustMembership, grantMembership } from '@/api/order.js'
import { listUserOptions } from '@/api/userOptions.js'
import { fetchPlans } from '@/api/plan.js'

// ── 搜索 & 列表 ──
const keyword = ref('')
const planKey = ref(undefined)
const statusFilter = ref(undefined)
const dateRange = ref([dayjs().startOf('month'), dayjs().endOf('month')])
const list = ref([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const totalAmountFromBackend = ref(null)
const selectedRowKeys = ref([])
const selectedRows = ref([])

const totalAmount = computed(() => {
  if (totalAmountFromBackend.value != null) {
    return Number(totalAmountFromBackend.value).toFixed(2)
  }
  const sum = list.value.reduce((acc, item) => acc + Number(item.amount || 0), 0)
  return sum.toFixed(2)
})

const columns = [
  { title: '订单ID', dataIndex: 'id', key: 'id', width: 120 },
  { title: '商户订单ID', dataIndex: 'orderNo', key: 'orderNo', width: 160 },
  { title: 'APP', key: 'app', width: 100 },
  { title: '订单标题', key: 'title', width: 140 },
  { title: '支付金额', key: 'amount', width: 100 },
  { title: '支付方式', key: 'payment', width: 220 },
  { title: '下单时间', key: 'createdAt', width: 160 },
  { title: '状态', key: 'status', width: 90 },
  { title: '操作', key: 'action', width: 160, fixed: 'right' }
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

function statusColor(s) {
  return { 0: 'orange', 1: 'green', 2: 'red', 3: 'default' }[s] || 'default'
}

function paymentMethodLabel(method) {
  return { xunhupay: '微信支付' }[method] || method || '-'
}

function orderTitle(record) {
  const cycleText = { month: '月度', quarter: '季度', year: '年度' }[record.cycle] || record.cycleName
  return `${record.planName}${cycleText}会员`
}

async function reload() {
  loading.value = true
  selectedRowKeys.value = []
  selectedRows.value = []
  try {
    const params = { page: page.value, pageSize: pageSize.value }
    if (keyword.value) params.keyword = keyword.value
    if (planKey.value) params.planKey = planKey.value
    if (statusFilter.value !== undefined) params.status = statusFilter.value
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0].format('YYYY-MM-DD')
      params.endDate = dateRange.value[1].format('YYYY-MM-DD')
    }
    const data = await getOrderList(params)
    list.value = data.list || []
    total.value = data.total || 0
    totalAmountFromBackend.value = data.totalAmount != null ? data.totalAmount : null
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
  keyword.value = ''
  planKey.value = undefined
  statusFilter.value = undefined
  dateRange.value = [dayjs().startOf('month'), dayjs().endOf('month')]
  page.value = 1
  reload()
}

function onTableChange(p) {
  page.value = p.current
  pageSize.value = p.pageSize
  reload()
}

function onSelectChange(keys, rows) {
  selectedRowKeys.value = keys
  selectedRows.value = rows
}

// ── 标记已支付 ──
function handleMarkPaid(record) {
  Modal.confirm({
    title: '确认标记已支付？',
    content: `订单号：${record.orderNo}，金额：¥${record.amount}。标记后将激活/延长用户会员。`,
    async onOk() {
      await markOrderPaid(record.id)
      message.success('已标记为已支付')
      reload()
    }
  })
}

// ── 取消订单 ──
function handleCancel(record) {
  Modal.confirm({
    title: '确认取消订单？',
    content: `订单号：${record.orderNo}`,
    async onOk() {
      await cancelOrder(record.id)
      message.success('已取消')
      reload()
    }
  })
}

// ── 批量取消 ──
function handleBatchCancel() {
  if (selectedRowKeys.value.length === 0) return
  const invalid = selectedRows.value.filter((r) => r.status !== 0)
  if (invalid.length > 0) {
    message.warning('仅允许批量取消待支付订单')
    return
  }
  Modal.confirm({
    title: '确认批量取消订单？',
    content: `已选中 ${selectedRowKeys.value.length} 个订单，取消后不可恢复。`,
    async onOk() {
      await batchCancelOrder(selectedRowKeys.value)
      message.success('批量取消成功')
      selectedRowKeys.value = []
      selectedRows.value = []
      reload()
    }
  })
}

// ── 批量删除 ──
function handleBatchDelete() {
  if (selectedRowKeys.value.length === 0) return
  const invalid = selectedRows.value.filter((r) => r.status !== 3)
  if (invalid.length > 0) {
    message.warning('仅允许批量删除已取消订单')
    return
  }
  Modal.confirm({
    title: '确认批量删除订单？',
    content: `已选中 ${selectedRowKeys.value.length} 个订单，删除后不可恢复。`,
    async onOk() {
      await batchDeleteOrder(selectedRowKeys.value)
      message.success('批量删除成功')
      selectedRowKeys.value = []
      selectedRows.value = []
      reload()
    }
  })
}

// ── 退款 ──
const refundModalOpen = ref(false)
const refundTarget = ref(null)
const refundReason = ref('')
const refunding = ref(false)

function openRefundModal(record) {
  refundTarget.value = record
  refundReason.value = ''
  refundModalOpen.value = true
}

async function submitRefund() {
  if (!refundReason.value.trim()) {
    message.warning('请输入退款原因')
    return
  }
  refunding.value = true
  try {
    await refundOrder(refundTarget.value.id, { reason: refundReason.value })
    message.success('退款成功')
    refundModalOpen.value = false
    reload()
  } catch (e) {
    // handled
  } finally {
    refunding.value = false
  }
}

// ── 手动发放会员 ──
const grantModalOpen = ref(false)
const granting = ref(false)
const grantForm = reactive({ userId: null, planKey: undefined, dateRange: null, remark: '' })
const userOptions = ref([])
const userSelectLoading = ref(false)
const planOptions = ref([])

let userSearchTimer = null
function onUserSearch(keyword) {
  if (userSearchTimer) clearTimeout(userSearchTimer)
  userSearchTimer = setTimeout(async () => {
    userSelectLoading.value = true
    try {
      userOptions.value = await listUserOptions(keyword, 20)
    } catch (e) {
      userOptions.value = []
    } finally {
      userSelectLoading.value = false
    }
  }, 300)
}

async function loadPlanOptions() {
  try {
    planOptions.value = await fetchPlans()
  } catch (e) {
    planOptions.value = []
  }
}

function openGrantModal() {
  grantForm.userId = null
  grantForm.planKey = undefined
  grantForm.dateRange = null
  grantForm.remark = ''
  userOptions.value = []
  grantModalOpen.value = true
}

async function submitGrant() {
  if (!grantForm.userId || !grantForm.planKey || !grantForm.dateRange || grantForm.dateRange.length !== 2) {
    message.warning('请填写完整信息')
    return
  }
  granting.value = true
  try {
    await grantMembership({
      userId: grantForm.userId,
      planKey: grantForm.planKey,
      startDate: grantForm.dateRange[0].format('YYYY-MM-DD'),
      endDate: grantForm.dateRange[1].format('YYYY-MM-DD'),
      remark: grantForm.remark
    })
    message.success('发放成功')
    grantModalOpen.value = false
    reload()
  } catch (e) {
    // handled
  } finally {
    granting.value = false
  }
}

// ── 手动调整会员 ──
const adjustModalOpen = ref(false)
const adjusting = ref(false)
const adjustForm = reactive({ userId: null, level: undefined, expiresAt: null, remark: '' })

function openAdjustModal() {
  adjustForm.userId = null
  adjustForm.level = undefined
  adjustForm.expiresAt = null
  adjustForm.remark = ''
  adjustModalOpen.value = true
}

async function submitAdjust() {
  if (!adjustForm.userId || !adjustForm.level || !adjustForm.expiresAt) {
    message.warning('请填写完整信息')
    return
  }
  adjusting.value = true
  try {
    const payload = {
      userId: adjustForm.userId,
      level: adjustForm.level,
      expiresAt: adjustForm.expiresAt.format('YYYY-MM-DD'),
      remark: adjustForm.remark
    }
    await adjustMembership(payload)
    message.success('调整成功')
    adjustModalOpen.value = false
    reload()
  } catch (e) {
    // handled
  } finally {
    adjusting.value = false
  }
}

// ── 详情抽屉 ──
const detailDrawerOpen = ref(false)
const detailData = ref(null)

async function openDetailDrawer(record) {
  try {
    detailData.value = await getOrderDetail(record.id)
    detailDrawerOpen.value = true
  } catch (e) {
    // handled
  }
}

onMounted(() => {
  reload()
  loadPlanOptions()
})
</script>

<style scoped>
.order-admin {
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

.search-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
  align-items: center;
}

.summary-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
  font-size: 13px;
}

.summary-bar strong {
  font-weight: 600;
}

.summary-tip {
  color: #ff4d4f;
}

.summary-default {
  color: #ff4d4f;
}

.app-link {
  color: #1890ff;
  cursor: pointer;
}

.cell-payment {
  line-height: 1.6;
}

.cell-payment-line {
  font-size: 12px;
  color: #8c8c8c;
}

.detail-row {
  display: flex;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.detail-label {
  min-width: 80px;
  color: #8c8c8c;
  flex-shrink: 0;
}
</style>
