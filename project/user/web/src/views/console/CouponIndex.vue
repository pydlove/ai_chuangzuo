<template>
  <div class="coupon-page">
   <!-- 顶部栏 -->
   <header class="coupon-header">
      <div class="coupon-header__left">
        <h1 class="coupon-header__title">我的优惠券</h1>
        <span class="coupon-header__count">共 <em>{{ filteredCoupons.length }}</em> 张</span>
      </div>
      <button class="coupon-header__action" @click="router.push('/lottery')">去参与活动</button>
   </header>

    <!-- 状态筛选标签 -->
    <div class="coupon-desktop-tabs">
      <button
        v-for="s in statusStats"
        :key="s.value"
        class="coupon-desktop-tab"
        :class="{ active: currentStatus === s.value }"
        @click="currentStatus = s.value"
      >
        {{ s.label }}
        <span class="coupon-desktop-tab__count">{{ s.count }}</span>
      </button>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="coupon-empty">
      <a-skeleton active :paragraph="{ rows: 4 }" />
    </div>

    <!-- 空状态 -->
    <div v-else-if="filteredCoupons.length === 0" class="coupon-empty">
      <div class="coupon-empty__icon"><TagsOutlined /></div>
      <div class="coupon-empty__title">您暂时没有优惠券</div>
      <p class="coupon-empty__desc">参与抽奖或活动，有机会获得会员、创作币等优惠券</p>
      <button class="coupon-empty__btn" @click="router.push('/lottery')">去参与活动</button>
    </div>

    <!-- 优惠券表格 -->
    <div v-else class="coupon-table-wrapper">
      <table class="coupon-table">
        <thead>
          <tr>
            <th>优惠内容</th>
            <th>券码</th>
            <th>适用范围</th>
            <th>状态</th>
            <th>有效期至</th>
            <th class="coupon-table__action-col">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="coupon in filteredCoupons"
            :key="coupon.id"
            :class="coupon.status"
          >
            <td>
              <div class="coupon-table__value">{{ couponValueText(coupon) }}</div>
              <div class="coupon-table__type">{{ couponTypeText(coupon) }}</div>
            </td>
            <td class="coupon-table__code">{{ coupon.couponCode }}</td>
            <td class="coupon-table__scope">{{ scopeText(coupon) }}</td>
            <td>
              <span class="coupon-table__status" :class="coupon.status">{{ statusLabel(coupon.status) }}</span>
            </td>
            <td class="coupon-table__time">{{ formatTime(coupon.validEnd) }}</td>
            <td class="coupon-table__action-col">
              <button v-if="coupon.status === 'unused'" class="coupon-table__btn" @click="handleUse(coupon)">去使用</button>
              <span v-else class="coupon-table__noop">—</span>
            </td>
          </tr>
        </tbody>
      </table>
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

const filteredCoupons = computed(() => {
  if (currentStatus.value === 'all') return coupons.value
  return coupons.value.filter(c => c.status === currentStatus.value)
})

const statusStats = computed(() => {
  const all = coupons.value.length
  const unused = coupons.value.filter(c => c.status === 'unused').length
  const used = coupons.value.filter(c => c.status === 'used').length
  const expired = coupons.value.filter(c => c.status === 'expired').length
  return [
    { value: 'all', label: '全部', count: all },
    { value: 'unused', label: '未使用', count: unused },
    { value: 'used', label: '已使用', count: used },
    { value: 'expired', label: '已失效', count: expired }
  ]
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

function scopeText(coupon) {
  const plan = coupon.applicablePlan && coupon.applicablePlan !== 'all' ? coupon.applicablePlan : '全部套餐'
  const cycle = coupon.applicableCycle && coupon.applicableCycle !== 'all' ? coupon.applicableCycle : '全部周期'
  return `${plan} · ${cycle}`
}

function formatTime(t) {
  return t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '-'
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
  min-height: 100%;
  background: #fafafa;
  padding: 16px 12px calc(80px + env(safe-area-inset-bottom));
  max-width: 1200px;
  margin: 0 auto;
  box-sizing: border-box;
}

/* 顶部栏 */
.coupon-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 14px;
  gap: 12px;
}

.coupon-header__left {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.coupon-header__title {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0;
}

.coupon-header__count {
  font-size: 14px;
  color: #8c8c8c;
}

.coupon-header__count em {
  font-style: normal;
  font-weight: 700;
  color: var(--color-primary);
}

.coupon-header__action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 6px 14px;
  background: var(--color-primary);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}

.coupon-header__action:active {
  background: var(--color-primary-active);
}

/* 状态筛选标签 */
.coupon-desktop-tabs {
  display: flex;
  gap: 0;
  margin-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
  overflow-x: auto;
  scrollbar-width: none;
}

.coupon-desktop-tabs::-webkit-scrollbar {
  display: none;
}

.coupon-desktop-tab {
  position: relative;
  padding: 10px 12px;
  background: transparent;
  border: none;
  border-bottom: 2px solid transparent;
  color: #595959;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-bottom: -1px;
  white-space: nowrap;
}

.coupon-desktop-tab.active {
  color: var(--color-primary);
  border-bottom-color: var(--color-primary);
}

.coupon-desktop-tab__count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  background: #f5f5f5;
  color: #8c8c8c;
  border-radius: 8px;
  font-size: 10px;
  font-weight: 700;
}

.coupon-desktop-tab.active .coupon-desktop-tab__count {
  background: var(--color-primary);
  color: #fff;
}

/* 优惠券表格 */
.coupon-table-wrapper {
  display: block;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  overflow-x: auto;
}

.coupon-table {
  width: 100%;
  min-width: 560px;
  border-collapse: collapse;
  font-size: 13px;
}

.coupon-table th {
  padding: 12px;
  text-align: left;
  font-weight: 600;
  color: #8c8c8c;
  background: #fafafa;
  border-bottom: 1px solid #f0f0f0;
  font-size: 12px;
  white-space: nowrap;
}

.coupon-table td {
  padding: 12px;
  border-bottom: 1px solid #f5f5f5;
  vertical-align: middle;
}

.coupon-table tbody tr:last-child td {
  border-bottom: none;
}

.coupon-table tbody tr.used,
.coupon-table tbody tr.expired {
  opacity: 0.75;
}

.coupon-table__value {
  font-size: 14px;
  font-weight: 700;
  color: #1a1a1a;
}

.coupon-table__type {
  font-size: 11px;
  color: #8c8c8c;
  margin-top: 2px;
}

.coupon-table__code {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  color: #595959;
  word-break: break-all;
}

.coupon-table__scope {
  font-size: 12px;
  color: #595959;
}

.coupon-table__status {
  display: inline-flex;
  align-items: center;
  padding: 3px 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
  white-space: nowrap;
}

.coupon-table__status.unused {
  color: var(--color-primary);
  background: var(--color-primary-light);
}

.coupon-table__status.used {
  color: #8c8c8c;
  background: #f5f5f5;
}

.coupon-table__status.expired {
  color: #ff4d4f;
  background: #fff1f0;
}

.coupon-table__time {
  font-size: 12px;
  color: #595959;
  white-space: nowrap;
}

.coupon-table__action-col {
  width: 80px;
  text-align: right;
}

.coupon-table__btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 5px 12px;
  background: var(--color-primary);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s;
}

.coupon-table__noop {
  color: #bfbfbf;
}

/* 空状态 */
.coupon-empty {
  text-align: center;
  padding: 56px 16px;
}

.coupon-empty__icon {
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

.coupon-empty__btn {
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

.coupon-empty__btn:active {
  background: var(--color-primary-active);
}

@media (min-width: 769px) {
  .coupon-page {
    padding: 32px 24px calc(80px + env(safe-area-inset-bottom));
  }

  .coupon-header {
    align-items: center;
    margin-bottom: 20px;
  }

  .coupon-header__action {
    padding: 8px 18px;
    font-size: 13px;
  }

  .coupon-header__action:hover {
    background: var(--color-primary-hover);
  }

  .coupon-desktop-tabs {
    margin-bottom: 20px;
  }

  .coupon-desktop-tab {
    padding: 12px 16px;
    font-size: 14px;
    gap: 6px;
  }

  .coupon-desktop-tab:hover {
    color: #1a1a1a;
  }

  .coupon-desktop-tab__count {
    min-width: 18px;
    height: 18px;
    padding: 0 5px;
    border-radius: 9px;
    font-size: 11px;
  }

  .coupon-table-wrapper {
    overflow: hidden;
  }

  .coupon-table {
    font-size: 14px;
  }

  .coupon-table th {
    padding: 14px 16px;
    font-size: 13px;
  }

  .coupon-table td {
    padding: 16px;
  }

  .coupon-table tbody tr:hover {
    background: #fafafa;
  }

  .coupon-table__value {
    font-size: 16px;
  }

  .coupon-table__type {
    font-size: 12px;
  }

  .coupon-table__code {
    font-size: 13px;
  }

  .coupon-table__scope {
    font-size: 13px;
  }

  .coupon-table__status {
    padding: 4px 10px;
    font-size: 12px;
  }

  .coupon-table__time {
    font-size: 13px;
  }

  .coupon-table__action-col {
    width: 100px;
  }

  .coupon-table__btn {
    padding: 6px 16px;
    font-size: 13px;
  }

  .coupon-table__btn:hover {
    background: var(--color-primary-hover);
  }

  .coupon-empty {
    padding: 80px 16px;
  }

  .coupon-empty__icon {
    width: 88px;
    height: 88px;
  }

  .coupon-empty__icon :deep(svg) {
    width: 40px;
    height: 40px;
  }

  .coupon-empty__title {
    font-size: 20px;
  }

  .coupon-empty__desc {
    font-size: 14px;
  }

  .coupon-empty__btn {
    padding: 12px 32px;
    font-size: 15px;
  }

  .coupon-empty__btn:hover {
    background: var(--color-primary-hover);
  }
}

/* 暗色主题 */
body[data-theme="dark"] .coupon-page {
  background: #141414;
}

body[data-theme="dark"] .coupon-header__title,
body[data-theme="dark"] .coupon-empty__title {
  color: #f5f5f5;
}

body[data-theme="dark"] .coupon-header__count,
body[data-theme="dark"] .coupon-empty__desc {
  color: #a6a6a6;
}

body[data-theme="dark"] .coupon-header__action {
  background: linear-gradient(135deg, #FF6B8A 0%, #FF2442 100%);
}

body[data-theme="dark"] .coupon-header__action:hover {
  background: linear-gradient(135deg, #FF4D6F 0%, #E61E3A 100%);
}

body[data-theme="dark"] .coupon-desktop-tabs {
  border-bottom-color: #303030;
}

body[data-theme="dark"] .coupon-desktop-tab {
  color: #a6a6a6;
}

body[data-theme="dark"] .coupon-desktop-tab:hover {
  color: #f0f0f0;
}

body[data-theme="dark"] .coupon-desktop-tab.active {
  color: #ff6b81;
  border-bottom-color: #ff4d6f;
}

body[data-theme="dark"] .coupon-desktop-tab__count {
  background: #2a2a2a;
  color: #a6a6a6;
}

body[data-theme="dark"] .coupon-desktop-tab.active .coupon-desktop-tab__count {
  background: #ff4d6f;
  color: #fff;
}

body[data-theme="dark"] .coupon-table-wrapper {
  background: #1f1f1f;
  box-shadow: none;
}

body[data-theme="dark"] .coupon-table th {
  background: #141414;
  color: #a6a6a6;
  border-bottom-color: #303030;
}

body[data-theme="dark"] .coupon-table td {
  border-bottom-color: #2a2a2a;
}

body[data-theme="dark"] .coupon-table tbody tr:hover {
  background: #262626;
}

body[data-theme="dark"] .coupon-table__value {
  color: #f0f0f0;
}

body[data-theme="dark"] .coupon-table__type,
body[data-theme="dark"] .coupon-table__scope,
body[data-theme="dark"] .coupon-table__time,
body[data-theme="dark"] .coupon-table__code {
  color: #a6a6a6;
}

body[data-theme="dark"] .coupon-table__status.unused {
  color: #ff6b81;
  background: rgba(255, 36, 66, 0.12);
}

body[data-theme="dark"] .coupon-table__status.used {
  color: #a6a6a6;
  background: #2a2a2a;
}

body[data-theme="dark"] .coupon-table__status.expired {
  color: #ff7875;
  background: rgba(255, 77, 79, 0.12);
}

body[data-theme="dark"] .coupon-table__noop {
  color: #666;
}

body[data-theme="dark"] .coupon-table__btn {
  background: linear-gradient(135deg, #FF6B8A 0%, #FF2442 100%);
}

body[data-theme="dark"] .coupon-table__btn:hover {
  background: linear-gradient(135deg, #FF4D6F 0%, #E61E3A 100%);
}

body[data-theme="dark"] .coupon-empty__icon {
  background: rgba(255, 36, 66, 0.12);
  color: #ff6b81;
}

body[data-theme="dark"] .coupon-empty__btn:hover {
  background: linear-gradient(135deg, #FF4D6F 0%, #E61E3A 100%);
}
</style>
