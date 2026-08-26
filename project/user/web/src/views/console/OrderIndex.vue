<template>
  <div class="order-page">
    <!-- 页面标题：PC 端显示 -->
    <div class="order-page-header">
      <h1 class="order-page-title">我的订单</h1>
      <p class="order-page-subtitle">查看会员套餐订单与支付记录</p>
    </div>

    <!-- 状态筛选 -->
    <div class="order-tabs">
      <button
        v-for="tab in tabs"
        :key="tab.value"
        class="order-tab"
        :class="{ active: activeTab === tab.value }"
        @click="activeTab = tab.value"
      >
        {{ tab.label }}
      </button>
    </div>

    <!-- 空状态 -->
    <div v-if="orders.length === 0 && !loading" class="order-empty">
      <div class="order-empty__icon"><FileTextOutlined /></div>
      <div class="order-empty__title">暂无{{ currentTabLabel }}订单</div>
      <p class="order-empty__desc">您可以在会员页面选择套餐下单</p>
      <button class="order-empty__btn" @click="router.push('/pricing')">去开通会员</button>
    </div>

    <!-- 桌面端：表格 -->
    <div v-show="orders.length > 0 || loading" class="order-desktop-table">
      <a-table
        :columns="columns"
        :data-source="orders"
        :pagination="false"
        :custom-row="customRow"
        :loading="loading"
        class="order-table"
      />
    </div>

    <!-- 移动端：卡片列表 -->
    <div v-show="orders.length > 0 || loading" class="order-mobile-list">
      <div v-if="loading" class="order-skeleton">
        <a-skeleton v-for="i in 3" :key="i" active :paragraph="{ rows: 3 }" />
      </div>
      <div v-else class="order-list">
        <div
          v-for="order in orders"
          :key="order.id"
          class="order-card"
          @click="openDetail(order)"
        >
          <div class="order-card__header">
            <span class="order-card__no">{{ order.orderNo }}</span>
            <span class="order-card__status" :class="statusClass(order.status)">
              {{ order.statusName }}
            </span>
          </div>
          <div class="order-card__body">
            <div class="order-card__plan">
              <span class="order-card__name">{{ order.planName || order.planKey }}</span>
              <span class="order-card__cycle">{{ order.cycleName || order.cycle }}</span>
            </div>
            <div class="order-card__amount">
              <span class="order-card__amount-label">实付</span>
              <span class="order-card__amount-value">¥{{ formatAmount(order.amount) }}</span>
            </div>
          </div>
          <div class="order-card__footer">
            <span class="order-card__time">{{ formatTime(order.createdAt) }}</span>
            <button v-if="order.status === 0" class="order-card__pay-btn" @click.stop="goPay(order)">
              去支付
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 订单详情弹框 -->
    <a-modal
      v-model:open="detailVisible"
      title="订单详情"
      :footer="null"
      :width="modalWidth"
      centered
      class="order-detail-modal"
      @cancel="closeDetail"
    >
      <div v-if="currentOrder" class="order-detail">
        <div class="order-detail__section">
          <div class="order-detail__row">
            <span class="order-detail__label">支付名称</span>
            <span class="order-detail__value">{{ currentOrder.planName || currentOrder.planKey }}</span>
          </div>
          <div class="order-detail__row">
            <span class="order-detail__label">订单号</span>
            <span class="order-detail__value order-detail__value--mono">{{ currentOrder.orderNo }}</span>
          </div>
          <div class="order-detail__row">
            <span class="order-detail__label">订单状态</span>
            <span class="order-detail__value">
              <span class="order-detail__status" :class="statusClass(currentOrder.status)">
                {{ currentOrder.statusName }}
              </span>
            </span>
          </div>
        </div>
        <div class="order-detail__section">
          <div class="order-detail__row">
            <span class="order-detail__label">支付方式</span>
            <span class="order-detail__value">{{ payMethodText(currentOrder) }}</span>
          </div>
          <div v-if="currentOrder.coinAmount > 0" class="order-detail__row">
            <span class="order-detail__label">创作币抵扣</span>
            <span class="order-detail__value">{{ currentOrder.coinAmount }} 币</span>
          </div>
          <div v-if="currentOrder.couponDiscount > 0" class="order-detail__row">
            <span class="order-detail__label">优惠券抵扣</span>
            <span class="order-detail__value">-¥{{ formatAmount(currentOrder.couponDiscount) }}</span>
          </div>
          <div class="order-detail__row order-detail__row--total">
            <span class="order-detail__label">实付金额</span>
            <span class="order-detail__value order-detail__value--amount">¥{{ formatAmount(currentOrder.amount) }}</span>
          </div>
        </div>
        <div class="order-detail__section">
          <div class="order-detail__row">
            <span class="order-detail__label">创建时间</span>
            <span class="order-detail__value">{{ formatTime(currentOrder.createdAt) }}</span>
          </div>
          <div v-if="currentOrder.paidAt" class="order-detail__row">
            <span class="order-detail__label">支付时间</span>
            <span class="order-detail__value">{{ formatTime(currentOrder.paidAt) }}</span>
          </div>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount, h } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { FileTextOutlined } from '@ant-design/icons-vue'
import { getMyOrders } from '@/api/order'
import dayjs from 'dayjs'

const router = useRouter()
const route = useRoute()

const tabs = [
  { value: 'all', label: '全部' },
  { value: 'pending', label: '待支付' },
  { value: 'paid', label: '已支付' }
]

const activeTab = ref('all')
const orders = ref([])
const loading = ref(false)
const detailVisible = ref(false)
const currentOrder = ref(null)

const modalWidth = ref(360)

function updateModalWidth() {
  modalWidth.value = window.innerWidth >= 769 ? 520 : 360
}

const columns = [
  { title: '订单号', dataIndex: 'orderNo', key: 'orderNo', width: 170 },
  { title: '支付名称', key: 'planName', customRender: ({ record }) => record.planName || record.planKey },
  { title: '周期', key: 'cycle', width: 90, customRender: ({ record }) => record.cycleName || record.cycle },
  { title: '实付金额', dataIndex: 'amount', key: 'amount', align: 'right', width: 110, customRender: ({ text }) => `¥${formatAmount(text)}` },
  { title: '状态', dataIndex: 'statusName', key: 'statusName', width: 90, customRender: ({ text, record }) => h('span', { class: ['order-table__status', statusClass(record.status)] }, text) },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 160, customRender: ({ text }) => formatTime(text) },
  {
    title: '操作',
    key: 'action',
    width: 100,
    customRender: ({ record }) => {
      if (record.status === 0) {
        return h('button', { class: 'order-table__pay-btn', onClick: (e) => { e.stopPropagation(); goPay(record); } }, '去支付')
      }
      return h('span', { class: 'order-table__view' }, '查看')
    }
  }
]

function customRow(record) {
  return {
    onClick: () => openDetail(record)
  }
}

const currentTabLabel = computed(() => {
  const tab = tabs.find((t) => t.value === activeTab.value)
  return tab ? tab.label : ''
})

const statusMap = {
  all: undefined,
  pending: 0,
  paid: 1
}

function statusClass(status) {
  if (status === 0) return 'pending'
  if (status === 1) return 'paid'
  return ''
}

function formatAmount(amount) {
  const num = Number(amount)
  return Number.isFinite(num) ? num.toFixed(2) : '0.00'
}

function formatTime(t) {
  return t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '-'
}

function goPay(order) {
  // 当前支付流程直接完成，未支付订单引导到会员购买页
  router.push({ path: '/pricing', query: { plan: order.planKey, cycle: order.cycle } })
}

function openDetail(order) {
  currentOrder.value = order
  detailVisible.value = true
}

function closeDetail() {
  detailVisible.value = false
  currentOrder.value = null
}

function payMethodText(order) {
  if (!order) return '-'
  if (order.status === 0) return '待支付'
  const methods = []
  if (order.coinAmount > 0) methods.push('创作币抵扣')
  if (order.amount > 0) methods.push('在线支付')
  if (order.couponDiscount > 0) methods.push('优惠券')
  return methods.length > 0 ? methods.join(' + ') : '在线支付'
}

async function loadOrders() {
  loading.value = true
  try {
    const res = await getMyOrders({
      status: statusMap[activeTab.value],
      page: 1,
      pageSize: 100
    })
    orders.value = res.list || []
  } catch (e) {
    message.error('加载订单失败')
  } finally {
    loading.value = false
  }
}

watch(activeTab, () => {
  router.replace({ query: { status: activeTab.value === 'all' ? undefined : activeTab.value } })
  loadOrders()
})

onMounted(() => {
  const queryStatus = route.query.status
  if (queryStatus && tabs.some((t) => t.value === queryStatus)) {
    activeTab.value = queryStatus
  }
  loadOrders()
  updateModalWidth()
  window.addEventListener('resize', updateModalWidth)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateModalWidth)
})
</script>

<style scoped>
.order-page {
  min-height: 100%;
  background: #f5f6fa;
  padding: 12px 12px calc(24px + env(safe-area-inset-bottom));
  max-width: 720px;
  margin: 0 auto;
  box-sizing: border-box;
}

.order-page-header {
  display: none;
}

.order-desktop-table {
  display: none;
}

.order-mobile-list {
  display: block;
}

.order-tabs {
  display: flex;
  gap: 0;
  margin-bottom: 12px;
  background: #fff;
  border-radius: 12px;
  padding: 4px;
}

.order-tab {
  flex: 1;
  padding: 10px 8px;
  border: none;
  border-radius: 10px;
  background: transparent;
  color: #595959;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  -webkit-tap-highlight-color: transparent;
}

.order-tab.active {
  background: var(--color-primary);
  color: #fff;
}

.order-skeleton {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.order-skeleton :deep(.ant-skeleton) {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.order-card {
  background: #fff;
  border-radius: 14px;
  padding: 14px 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.15s;
  -webkit-tap-highlight-color: transparent;
}

.order-card:active {
  transform: translateY(1px);
}

.order-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.order-card__no {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  color: #8c8c8c;
}

.order-card__status {
  display: inline-flex;
  align-items: center;
  padding: 3px 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
}

.order-card__status.pending {
  color: #fa8c16;
  background: #fff7e6;
}

.order-card__status.paid {
  color: var(--color-primary);
  background: var(--color-primary-light);
}

.order-card__body {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.order-card__plan {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.order-card__name {
  font-size: 15px;
  font-weight: 700;
  color: #1a1a1a;
}

.order-card__cycle {
  font-size: 12px;
  color: #8c8c8c;
}

.order-card__amount {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
}

.order-card__amount-label {
  font-size: 11px;
  color: #8c8c8c;
}

.order-card__amount-value {
  font-size: 17px;
  font-weight: 700;
  color: #1a1a1a;
}

.order-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 10px;
  border-top: 1px solid #f5f5f5;
}

.order-card__time {
  font-size: 12px;
  color: #bfbfbf;
}

.order-card__pay-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 5px 12px;
  background: var(--color-primary);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
  -webkit-tap-highlight-color: transparent;
}

.order-card__pay-btn:active {
  background: var(--color-primary-active);
}

.order-empty {
  text-align: center;
  padding: 56px 16px;
}

.order-empty__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  margin: 0 auto 16px;
  border-radius: 50%;
  background: var(--color-primary-bg);
  color: var(--color-primary);
}

.order-empty__icon :deep(svg) {
  width: 32px;
  height: 32px;
}

.order-empty__title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 8px;
}

.order-empty__desc {
  font-size: 13px;
  color: #8c8c8c;
  margin: 0 0 20px;
}

.order-empty__btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 10px 24px;
  background: var(--color-primary);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  border: none;
  border-radius: 999px;
  cursor: pointer;
  transition: background 0.2s;
}

.order-empty__btn:active {
  background: var(--color-primary-active);
}

@media (min-width: 769px) {
  .order-page {
    padding: 24px;
    max-width: none;
  }

  .order-page-header {
    display: block;
    margin-bottom: 20px;
  }

  .order-page-title {
    font-size: 22px;
    font-weight: 700;
    color: #1a1a1a;
    margin: 0 0 6px;
  }

  .order-page-subtitle {
    font-size: 14px;
    color: #8c8c8c;
    margin: 0;
  }

  .order-tabs {
    margin-bottom: 16px;
  }

  .order-tab {
    padding: 12px 16px;
    font-size: 15px;
  }

  .order-desktop-table {
    display: block;
  }

  .order-mobile-list {
    display: none;
  }

  .order-tab:hover:not(.active) {
    color: #1a1a1a;
  }

  .order-card {
    padding: 16px 20px;
  }

  .order-card:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
  }

  .order-card__name {
    font-size: 16px;
  }

  .order-card__amount-value {
    font-size: 18px;
  }

  .order-empty {
    padding: 80px 16px;
  }

  .order-empty__icon {
    width: 88px;
    height: 88px;
  }

  .order-empty__icon :deep(svg) {
    width: 40px;
    height: 40px;
  }

  .order-empty__title {
    font-size: 20px;
  }

  .order-empty__desc {
    font-size: 14px;
  }

  .order-empty__btn:hover {
    background: var(--color-primary-hover);
  }

  .order-card__pay-btn:hover {
    background: var(--color-primary-hover);
  }
}

/* 桌面端表格 */
.order-desktop-table .order-table {
  background: #fff;
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.order-table :deep(.ant-table-thead > tr > th) {
  background: #fafafa;
  font-weight: 600;
  color: #595959;
  border-bottom: 1px solid #f0f0f0;
  white-space: nowrap;
}

.order-table :deep(.ant-table-tbody > tr > td) {
  border-bottom: 1px solid #f5f5f5;
}

.order-table :deep(.ant-table-tbody > tr:hover > td) {
  background: #f7f7f7;
  cursor: pointer;
}

.order-table :deep(.ant-table-tbody > tr:last-child > td) {
  border-bottom: none;
}

.order-table__status {
  display: inline-flex;
  align-items: center;
  padding: 3px 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.order-table__status.pending {
  color: #fa8c16;
  background: #fff7e6;
}

.order-table__status.paid {
  color: var(--color-primary);
  background: var(--color-primary-light);
}

.order-table__pay-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 4px 10px;
  background: var(--color-primary);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s;
}

.order-table__pay-btn:hover {
  background: var(--color-primary-hover);
}

.order-table__view {
  color: #8c8c8c;
  font-size: 13px;
}

/* 订单详情弹框 */
.order-detail {
  padding: 4px 0 8px;
}

.order-detail__section {
  padding: 14px 0;
  border-bottom: 1px solid #f0f0f0;
}

.order-detail__section:last-child {
  border-bottom: none;
}

.order-detail__row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 6px 0;
  font-size: 14px;
  line-height: 1.5;
}

.order-detail__row--total {
  margin-top: 4px;
  padding-top: 12px;
  border-top: 1px dashed #f0f0f0;
}

.order-detail__label {
  flex-shrink: 0;
  color: #8c8c8c;
}

.order-detail__value {
  flex: 1;
  text-align: right;
  color: #1a1a1a;
  word-break: break-all;
}

.order-detail__value--mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.order-detail__value--amount {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-primary);
}

.order-detail__status {
  display: inline-flex;
  align-items: center;
  padding: 3px 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
}

.order-detail__status.pending {
  color: #fa8c16;
  background: #fff7e6;
}

.order-detail__status.paid {
  color: var(--color-primary);
  background: var(--color-primary-light);
}

/* 暗色主题 */
body[data-theme="dark"] .order-page {
  background: #141414;
}

body[data-theme="dark"] .order-tabs,
body[data-theme="dark"] .order-card,
body[data-theme="dark"] .order-skeleton :deep(.ant-skeleton) {
  background: #1f1f1f;
}

body[data-theme="dark"] .order-tab {
  color: #a6a6a6;
}

body[data-theme="dark"] .order-tab.active {
  background: #ff4d6f;
  color: #fff;
}

body[data-theme="dark"] .order-card__name,
body[data-theme="dark"] .order-empty__title,
body[data-theme="dark"] .order-card__amount-value {
  color: #f0f0f0;
}

body[data-theme="dark"] .order-card__no,
body[data-theme="dark"] .order-card__cycle,
body[data-theme="dark"] .order-card__amount-label,
body[data-theme="dark"] .order-empty__desc {
  color: #a6a6a6;
}

body[data-theme="dark"] .order-card__time {
  color: #737373;
}

body[data-theme="dark"] .order-card__footer {
  border-top-color: #2a2a2a;
}

body[data-theme="dark"] .order-card__status.pending {
  color: #ffc53d;
  background: rgba(250, 140, 22, 0.12);
}

body[data-theme="dark"] .order-card__status.paid {
  color: #ff6b81;
  background: rgba(255, 36, 66, 0.12);
}

body[data-theme="dark"] .order-empty__icon {
  background: rgba(255, 36, 66, 0.12);
  color: #ff6b81;
}

body[data-theme="dark"] .order-detail__section {
  border-bottom-color: #2a2a2a;
}

body[data-theme="dark"] .order-detail__row--total {
  border-top-color: #2a2a2a;
}

body[data-theme="dark"] .order-detail__label {
  color: #a6a6a6;
}

body[data-theme="dark"] .order-detail__value {
  color: #f0f0f0;
}

body[data-theme="dark"] .order-detail__status.pending {
  color: #ffc53d;
  background: rgba(250, 140, 22, 0.12);
}

body[data-theme="dark"] .order-detail__status.paid {
  color: #ff6b81;
  background: rgba(255, 36, 66, 0.12);
}

body[data-theme="dark"] .order-page-title {
  color: #f0f0f0;
}

body[data-theme="dark"] .order-page-subtitle {
  color: #a6a6a6;
}

body[data-theme="dark"] .order-desktop-table .order-table {
  background: #1f1f1f;
}

body[data-theme="dark"] .order-table :deep(.ant-table-thead > tr > th) {
  background: #262626;
  color: #a6a6a6;
  border-bottom-color: #2a2a2a;
}

body[data-theme="dark"] .order-table :deep(.ant-table-tbody > tr > td) {
  border-bottom-color: #2a2a2a;
  color: #f0f0f0;
}

body[data-theme="dark"] .order-table :deep(.ant-table-tbody > tr:hover > td) {
  background: #262626;
}

body[data-theme="dark"] .order-table__status.pending {
  color: #ffc53d;
  background: rgba(250, 140, 22, 0.12);
}

body[data-theme="dark"] .order-table__status.paid {
  color: #ff6b81;
  background: rgba(255, 36, 66, 0.12);
}

body[data-theme="dark"] .order-table__view {
  color: #737373;
}
</style>
