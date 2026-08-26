<template>
  <div class="order-page">
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

    <!-- 加载中 -->
    <div v-if="loading" class="order-skeleton">
      <a-skeleton v-for="i in 3" :key="i" active :paragraph="{ rows: 3 }" />
    </div>

    <!-- 空状态 -->
    <div v-else-if="orders.length === 0" class="order-empty">
      <div class="order-empty__icon"><FileTextOutlined /></div>
      <div class="order-empty__title">暂无{{ currentTabLabel }}订单</div>
      <p class="order-empty__desc">您可以在会员页面选择套餐下单</p>
      <button class="order-empty__btn" @click="router.push('/pricing')">去开通会员</button>
    </div>

    <!-- 订单列表 -->
    <div v-else class="order-list">
      <div
        v-for="order in orders"
        :key="order.id"
        class="order-card"
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
          <button v-if="order.status === 0" class="order-card__pay-btn" @click="goPay(order)">
            去支付
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
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
    padding: 24px 24px calc(32px + env(safe-area-inset-bottom));
    max-width: 960px;
  }

  .order-tabs {
    margin-bottom: 16px;
  }

  .order-tab {
    padding: 12px 16px;
    font-size: 15px;
  }

  .order-tab:hover:not(.active) {
    color: #1a1a1a;
  }

  .order-list {
    gap: 12px;
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
</style>
