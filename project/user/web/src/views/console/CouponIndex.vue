<template>
  <div class="coupon-page">
    <div class="coupon-page__header">
      <h2 class="coupon-page__title">我的优惠券</h2>
      <p class="coupon-page__subtitle">共 {{ filteredCoupons.length }} 张</p>
    </div>

    <div class="coupon-status-tabs">
      <button
        v-for="tab in statusTabs"
        :key="tab.value"
        class="coupon-status-tab"
        :class="{ active: currentStatus === tab.value }"
        @click="currentStatus = tab.value"
      >
        {{ tab.label }}
      </button>
    </div>

    <div v-if="loading" class="coupon-empty">
      <a-skeleton active :paragraph="{ rows: 4 }" />
    </div>

    <div v-else-if="filteredCoupons.length === 0" class="coupon-empty">
      <div class="coupon-empty__icon"><TagsOutlined /></div>
      <div class="coupon-empty__title">您暂时没有优惠券</div>
      <p class="coupon-empty__desc">参与抽奖或活动，有机会获得会员、创作币等优惠券～</p>
      <a-button type="primary" shape="round" @click="router.push('/lottery')">去参与活动</a-button>
    </div>

    <div v-else class="coupon-list">
      <div
        v-for="coupon in filteredCoupons"
        :key="coupon.id"
        class="coupon-card"
        :class="coupon.status"
        @click="handleUse(coupon)"
      >
        <div class="coupon-card__left">
          <div class="coupon-card__value">{{ couponValueText(coupon) }}</div>
          <div class="coupon-card__type">{{ couponTypeText(coupon) }}</div>
        </div>
        <div class="coupon-card__right">
          <div class="coupon-card__row">
            <a-tag :color="statusColor(coupon.status)">{{ statusLabel(coupon.status) }}</a-tag>
            <span class="coupon-card__code">{{ coupon.couponCode }}</span>
          </div>
          <div class="coupon-card__scope">适用：{{ scopeText(coupon) }}</div>
          <div class="coupon-card__time">有效期：{{ formatTime(coupon.validStart) }} ~ {{ formatTime(coupon.validEnd) }}</div>
          <div v-if="coupon.status === 'unused'" class="coupon-card__action">
            <a-button type="primary" size="small" shape="round">去使用</a-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { TagsOutlined } from '@ant-design/icons-vue'
import { getMyCoupons } from '@/api/lottery'
import dayjs from 'dayjs'

const router = useRouter()
const coupons = ref([])
const loading = ref(false)
const currentStatus = ref('all')
let expiryTimer = null

const statusTabs = [
  { value: 'all', label: '全部' },
  { value: 'unused', label: '未使用' },
  { value: 'used', label: '已使用' },
  { value: 'expired', label: '已失效' }
]

const filteredCoupons = computed(() => {
  if (currentStatus.value === 'all') return coupons.value
  return coupons.value.filter(c => c.status === currentStatus.value)
})

function couponValueText(coupon) {
  if (coupon.couponType === 'percent') {
    return `${coupon.discountValue * 10}折`
  }
  return `¥${coupon.discountValue}`
}

function couponTypeText(coupon) {
  return coupon.couponType === 'percent' ? '折扣券' : '抵扣券'
}

function statusLabel(status) {
  const map = { unused: '未使用', used: '已使用', expired: '已失效' }
  return map[status] || status
}

function statusColor(status) {
  const map = { unused: 'green', used: 'default', expired: 'red' }
  return map[status] || 'default'
}

function scopeText(coupon) {
  const plan = coupon.applicablePlan && coupon.applicablePlan !== 'all' ? coupon.applicablePlan : '全部套餐'
  const cycle = coupon.applicableCycle && coupon.applicableCycle !== 'all' ? coupon.applicableCycle : '全部周期'
  return `${plan} · ${cycle}`
}

function formatTime(t) {
  return t ? dayjs(t).format('MM-DD HH:mm') : '-'
}

function handleUse(coupon) {
  if (coupon.status !== 'unused') return
  router.push({ path: '/pricing', query: { coupon: coupon.couponCode } })
}

function checkExpiryReminder() {
  const now = dayjs()
  const threshold = 24 * 60 * 60 * 1000
  coupons.value
    .filter(c => c.status === 'unused' && c.validEnd)
    .forEach(c => {
      const end = dayjs(c.validEnd)
      const diff = end.diff(now)
      if (diff > 0 && diff <= threshold) {
        const key = `coupon_warn_${c.id}`
        if (!localStorage.getItem(key)) {
          message.warning(`优惠券 ${c.couponCode} 将在 24 小时内过期，请及时使用`)
          localStorage.setItem(key, Date.now().toString())
        }
      }
    })
}

async function loadCoupons() {
  loading.value = true
  try {
    const res = await getMyCoupons()
    coupons.value = res.data || []
    checkExpiryReminder()
  } catch (e) {
    message.error('加载优惠券失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadCoupons()
  expiryTimer = setInterval(checkExpiryReminder, 60000)
})

onUnmounted(() => {
  if (expiryTimer) clearInterval(expiryTimer)
})
</script>

<style scoped>
.coupon-page {
  padding: 16px 12px calc(80px + env(safe-area-inset-bottom));
  max-width: 720px;
  margin: 0 auto;
  box-sizing: border-box;
}

.coupon-page__header {
  margin-bottom: 16px;
}

.coupon-page__title {
  font-size: 20px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0 0 4px;
}

.coupon-page__subtitle {
  font-size: 13px;
  color: #8c8c8c;
  margin: 0;
}

.coupon-status-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  overflow-x: auto;
}

.coupon-status-tab {
  flex-shrink: 0;
  padding: 6px 14px;
  border: 1px solid #f0f0f0;
  border-radius: 999px;
  background: #fff;
  color: #595959;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.coupon-status-tab.active {
  background: var(--color-primary);
  color: #fff;
  border-color: var(--color-primary);
}

.coupon-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.coupon-card {
  display: flex;
  align-items: stretch;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.04);
  overflow: hidden;
  cursor: pointer;
  transition: opacity 0.2s;
}

.coupon-card.used,
.coupon-card.expired {
  opacity: 0.7;
  cursor: default;
}

.coupon-card__left {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100px;
  padding: 16px 8px;
  background: linear-gradient(135deg, var(--color-primary) 0%, #FF4D6F 100%);
  color: #fff;
  flex-shrink: 0;
}

.coupon-card.used .coupon-card__left,
.coupon-card.expired .coupon-card__left {
  background: #bfbfbf;
}

.coupon-card__value {
  font-size: 22px;
  font-weight: 800;
  line-height: 1.2;
}

.coupon-card__type {
  font-size: 12px;
  margin-top: 4px;
  opacity: 0.9;
}

.coupon-card__right {
  flex: 1;
  padding: 14px 16px;
  min-width: 0;
}

.coupon-card__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 6px;
}

.coupon-card__code {
  font-family: monospace;
  font-size: 12px;
  color: #8c8c8c;
}

.coupon-card__scope,
.coupon-card__time {
  font-size: 12px;
  color: #595959;
  margin-bottom: 4px;
}

.coupon-card__action {
  margin-top: 8px;
  text-align: right;
}

.coupon-empty {
  text-align: center;
  padding: 48px 16px;
}

.coupon-empty__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  margin: 0 auto 16px;
  border-radius: 50%;
  background: rgba(255, 36, 66, 0.08);
  color: var(--color-primary);
}

.coupon-empty__icon :deep(svg) {
  width: 32px;
  height: 32px;
}

.coupon-empty__title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 8px;
}

.coupon-empty__desc {
  font-size: 13px;
  color: #8c8c8c;
  margin: 0 0 20px;
}

@media (min-width: 769px) {
  .coupon-page {
    padding: 24px 16px calc(80px + env(safe-area-inset-bottom));
  }

  .coupon-page__title {
    font-size: 24px;
  }

  .coupon-card__left {
    width: 120px;
  }

  .coupon-card__value {
    font-size: 26px;
  }
}

body[data-theme="dark"] .coupon-page__title,
body[data-theme="dark"] .coupon-empty__title {
  color: #f5f5f5;
}

body[data-theme="dark"] .coupon-page__subtitle,
body[data-theme="dark"] .coupon-empty__desc,
body[data-theme="dark"] .coupon-card__time,
body[data-theme="dark"] .coupon-card__scope {
  color: #a6a6a6;
}

body[data-theme="dark"] .coupon-status-tab {
  background: #1f1f1f;
  border-color: #303030;
  color: #a6a6a6;
}

body[data-theme="dark"] .coupon-card,
body[data-theme="dark"] .coupon-empty__icon {
  background: #1f1f1f;
  box-shadow: none;
}

body[data-theme="dark"] .coupon-card__code {
  color: #8c8c8c;
}
</style>
