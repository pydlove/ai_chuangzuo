<template>
  <a-card :bordered="false" class="order-admin">
    <div class="page-header">
      <h3 class="page-title">订单列表</h3>
      <p class="page-desc">查看和管理用户订阅订单</p>
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
      @change="onTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'user'">
          <div class="cell-user">
            <div>{{ record.nickname || '-' }}</div>
            <div class="cell-user-sub">{{ record.email || '-' }}</div>
          </div>
        </template>
        <template v-else-if="column.key === 'planKey'">
          <a-tag :color="planColor(record.planKey)">{{ record.planName }}</a-tag>
        </template>
        <template v-else-if="column.key === 'amount'">
          ¥{{ record.amount }}
        </template>
        <template v-else-if="column.key === 'status'">
          <a-tag :color="statusColor(record.status)">{{ record.statusName }}</a-tag>
        </template>
        <template v-else-if="column.key === 'paidAt'">
          {{ record.paidAt ? formatTime(record.paidAt) : '-' }}
        </template>
        <template v-else-if="column.key === 'createdAt'">
          {{ formatTime(record.createdAt) }}
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
      <div style="height: 260px; overflow-y: auto;">
        <a-form layout="vertical">
          <a-form-item label="用户ID" required>
            <a-input-number v-model:value="grantForm.userId" placeholder="输入用户ID" style="width: 100%" :min="1" />
          </a-form-item>
          <a-form-item label="套餐" required>
            <a-select v-model:value="grantForm.planKey" placeholder="选择套餐" style="width: 100%">
              <a-select-option value="basic">基础版</a-select-option>
              <a-select-option value="pro">专业版</a-select-option>
              <a-select-option value="flagship">旗舰版</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="周期" required>
            <a-select v-model:value="grantForm.cycle" placeholder="选择周期" style="width: 100%">
              <a-select-option value="month">月付（30天）</a-select-option>
              <a-select-option value="quarter">季付（90天）</a-select-option>
              <a-select-option value="year">年付（365天）</a-select-option>
            </a-select>
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
        <div class="detail-row"><span class="detail-label">用户</span><span>{{ detailData.nickname || '-' }} ({{ detailData.email || '-' }})</span></div>
        <div class="detail-row"><span class="detail-label">套餐</span><span>{{ detailData.planName }}</span></div>
        <div class="detail-row"><span class="detail-label">周期</span><span>{{ detailData.cycleName }}</span></div>
        <div class="detail-row"><span class="detail-label">金额</span><span>¥{{ detailData.amount }}</span></div>
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
import { getOrderList, getOrderDetail, markOrderPaid, refundOrder, cancelOrder, adjustMembership, grantMembership } from '@/api/order.js'

// ── 搜索 & 列表 ──
const keyword = ref('')
const planKey = ref(undefined)
const statusFilter = ref(undefined)
const dateRange = ref(null)
const list = ref([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const columns = [
  { title: '订单号', dataIndex: 'orderNo', key: 'orderNo', width: 180 },
  { title: '用户', key: 'user', width: 160 },
  { title: '套餐', key: 'planKey', width: 100 },
  { title: '周期', dataIndex: 'cycleName', key: 'cycleName', width: 80 },
  { title: '金额', key: 'amount', width: 90 },
  { title: '状态', key: 'status', width: 90 },
  { title: '支付时间', key: 'paidAt', width: 160 },
  { title: '创建时间', key: 'createdAt', width: 160 },
  { title: '操作', key: 'action', width: 200, fixed: 'right' }
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

function planColor(key) {
  return { basic: 'blue', pro: 'green', flagship: 'gold' }[key] || 'default'
}

function statusColor(s) {
  return { 0: 'orange', 1: 'green', 2: 'red', 3: 'default' }[s] || 'default'
}

async function reload() {
  loading.value = true
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
  dateRange.value = null
  page.value = 1
  reload()
}

function onTableChange(p) {
  page.value = p.current
  pageSize.value = p.pageSize
  reload()
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
const grantForm = reactive({ userId: null, planKey: undefined, cycle: undefined, remark: '' })

function openGrantModal() {
  grantForm.userId = null
  grantForm.planKey = undefined
  grantForm.cycle = undefined
  grantForm.remark = ''
  grantModalOpen.value = true
}

async function submitGrant() {
  if (!grantForm.userId || !grantForm.planKey || !grantForm.cycle) {
    message.warning('请填写完整信息')
    return
  }
  granting.value = true
  try {
    await grantMembership({ ...grantForm })
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

onMounted(reload)
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

.cell-user {
  line-height: 1.4;
}

.cell-user-sub {
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
