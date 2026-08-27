<template>
  <div class="coupon-page">
    <!-- PC 端页面标题与操作 -->
    <header class="coupon-page-header">
      <div class="coupon-page-title-wrap">
        <h1 class="coupon-page-title">我的优惠券</h1>
        <p class="coupon-page-subtitle">查看活动获得的优惠券与抵扣记录</p>
      </div>
      <button class="coupon-page__action" @click="router.push('/lottery')">去参与活动</button>
    </header>

    <!-- 状态筛选 -->
    <div class="coupon-tabs">
      <button
        v-for="tab in tabs"
        :key="tab.value"
        class="coupon-tab"
        :class="{ active: activeTab === tab.value }"
        @click="activeTab = tab.value"
      >
        {{ tab.label }}
        <span class="coupon-tab__count">{{ couponCounts[tab.value] }}</span>
      </button>
    </div>

    <div class="coupon-page-body">
      <!-- 桌面端：表格 -->
      <div class="coupon-desktop-table">
        <a-table
          :columns="columns"
          :data-source="coupons"
          :row-key="record => record.id"
          :pagination="false"
          :loading="loading"
          :scroll="{ x: 760 }"
          :locale="tableLocale"
          class="coupon-table"
        />
      </div>

      <!-- 移动端：卡片列表 -->
      <div v-show="coupons.length > 0 || loading" class="coupon-mobile-list">
        <div v-if="loading" class="coupon-skeleton">
          <a-skeleton v-for="i in 3" :key="i" active :paragraph="{ rows: 3 }" />
        </div>
        <div v-else class="coupon-list">
          <div
            v-for="coupon in coupons"
            :key="coupon.id"
            class="coupon-card"
          >
            <div class="coupon-card__header">
              <span class="coupon-card__code">{{ coupon.couponCode }}</span>
              <span class="coupon-card__status" :class="coupon.status">
                {{ statusLabel(coupon.status) }}
              </span>
            </div>
            <div class="coupon-card__body">
              <div class="coupon-card__value-wrap">
                <span class="coupon-card__value">{{ couponValueText(coupon) }}</span>
                <span class="coupon-card__type">{{ couponTypeText(coupon) }}</span>
              </div>
              <div class="coupon-card__scope">{{ scopeText(coupon) }}</div>
            </div>
            <div class="coupon-card__footer">
              <span class="coupon-card__time">有效期至 {{ formatTime(coupon.validEnd) }}</span>
              <button v-if="coupon.status === 'unused'" class="coupon-card__btn" @click="handleUse(coupon)">
                去使用
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 移动端空状态 -->
      <EmptyState
        v-if="coupons.length === 0 && !loading"
        :icon="TagsOutlined"
        :title="`暂无${currentTabLabel}优惠券`"
        description="参与抽奖或活动，有机会获得会员、创作币等优惠券"
        action-text="去参与活动"
        action-to="/lottery"
        size="lg"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, h } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { TagsOutlined } from '@ant-design/icons-vue'
import { getMyCoupons } from '@/api/lottery'
import EmptyState from '@/components/common/EmptyState.vue'
import dayjs from 'dayjs'

const router = useRouter()

const tabs = [
  { value: 'all', label: '全部' },
  { value: 'unused', label: '未使用' },
  { value: 'used', label: '已使用' },
  { value: 'expired', label: '已失效' }
]

const activeTab = ref('all')
const allCoupons = ref([])
const loading = ref(false)
let expiryTimer = null

const coupons = computed(() => {
  if (activeTab.value === 'all') return allCoupons.value
  return allCoupons.value.filter(c => c.status === activeTab.value)
})

const couponCounts = computed(() => {
  const counts = { all: allCoupons.value.length, unused: 0, used: 0, expired: 0 }
  allCoupons.value.forEach((c) => {
    if (counts[c.status] !== undefined) counts[c.status]++
  })
  return counts
})

const currentTabLabel = computed(() => {
  const tab = tabs.find((t) => t.value === activeTab.value)
  return tab ? tab.label : ''
})

const tableLocale = computed(() => ({
  emptyText: h('div', { class: 'coupon-table-empty' }, [
    h('div', { class: 'coupon-table-empty__text' }, `暂无${currentTabLabel.value}优惠券`),
    h('div', { class: 'coupon-table-empty__desc' }, '参与抽奖或活动，有机会获得会员、创作币等优惠券'),
    h('button', { class: 'coupon-empty__btn coupon-table-empty__btn', onClick: () => router.push('/lottery') }, '去参与活动')
  ])
}))

const columns = [
  {
    title: '优惠内容',
    key: 'value',
    width: 140,
    customRender: ({ record }) => h('div', { class: 'coupon-table__value-wrap' }, [
      h('div', { class: 'coupon-table__value' }, couponValueText(record)),
      h('div', { class: 'coupon-table__type' }, couponTypeText(record))
    ])
  },
  { title: '券码', dataIndex: 'couponCode', key: 'couponCode', width: 170 },
  { title: '适用范围', key: 'scope', customRender: ({ record }) => scopeText(record) },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    width: 100,
    customRender: ({ text }) => h('span', { class: ['coupon-table__status', text] }, statusLabel(text))
  },
  { title: '有效期至', dataIndex: 'validEnd', key: 'validEnd', width: 160, customRender: ({ text }) => formatTime(text) },
  {
    title: '操作',
    key: 'action',
    width: 100,
    customRender: ({ record }) => {
      if (record.status === 'unused') {
        return h('button', { class: 'coupon-table__btn', onClick: (e) => { e.stopPropagation(); handleUse(record); } }, '去使用')
      }
      return h('span', { class: 'coupon-table__noop' }, '—')
    }
  }
]

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
  allCoupons.value
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
    allCoupons.value = res.data || []
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
  background: #f5f6fa;
  padding: 12px 12px calc(24px + env(safe-area-inset-bottom));
  width: 100%;
  max-width: 1280px;
  margin: 0 auto;
  box-sizing: border-box;
}

/* 页面标题：PC 端显示 */
.coupon-page-header {
  display: none;
}

.coupon-page-title-wrap {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.coupon-page-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0;
}

.coupon-page-subtitle {
  font-size: 14px;
  color: #8c8c8c;
  margin: 0;
}

.coupon-page__action {
  display: none;
}

/* 状态筛选 */
.coupon-tabs {
  display: flex;
  gap: 0;
  margin-bottom: 12px;
  background: #fff;
  border-radius: 12px;
  padding: 4px;
}

.coupon-tab {
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

.coupon-tab.active {
  background: var(--color-primary);
  color: #fff;
}

.coupon-tab__count {
  display: none;
}

.coupon-skeleton {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.coupon-skeleton :deep(.ant-skeleton) {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
}

.coupon-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.coupon-card {
  background: #fff;
  border-radius: 14px;
  padding: 14px 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  cursor: default;
  transition: transform 0.15s, box-shadow 0.15s;
  -webkit-tap-highlight-color: transparent;
}

.coupon-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.coupon-card__code {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  color: #8c8c8c;
}

.coupon-card__status {
  display: inline-flex;
  align-items: center;
  padding: 3px 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
}

.coupon-card__status.unused {
  color: var(--color-primary);
  background: var(--color-primary-light);
}

.coupon-card__status.used {
  color: #8c8c8c;
  background: #f5f5f5;
}

.coupon-card__status.expired {
  color: #ff4d4f;
  background: #fff1f0;
}

.coupon-card__body {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.coupon-card__value-wrap {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.coupon-card__value {
  font-size: 17px;
  font-weight: 700;
  color: #1a1a1a;
}

.coupon-card__type {
  font-size: 12px;
  color: #8c8c8c;
}

.coupon-card__scope {
  font-size: 12px;
  color: #595959;
  text-align: right;
}

.coupon-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 10px;
  border-top: 1px solid #f5f5f5;
}

.coupon-card__time {
  font-size: 12px;
  color: #bfbfbf;
}

.coupon-card__btn {
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

.coupon-card__btn:active {
  background: var(--color-primary-active);
}

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

.coupon-desktop-table {
  display: none;
}

.coupon-mobile-list {
  display: block;
}

@media (min-width: 769px) {
  .coupon-page {
    background: transparent;
    padding: 24px 32px calc(32px + env(safe-area-inset-bottom));
    max-width: 1280px;
  }

  .coupon-page-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 20px;
  }

  .coupon-page__action {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    padding: 8px 18px;
    background: var(--color-primary);
    color: #fff;
    font-size: 13px;
    font-weight: 600;
    border: none;
    border-radius: 8px;
    cursor: pointer;
    transition: background 0.2s;
  }

  .coupon-page__action:hover {
    background: var(--color-primary-hover);
  }

  .coupon-tabs {
    display: flex;
    gap: 0;
    margin-bottom: 20px;
    background: transparent;
    border-radius: 0;
    padding: 0;
    border-bottom: 1px solid #f0f0f0;
    overflow-x: auto;
    scrollbar-width: none;
  }

  .coupon-tabs::-webkit-scrollbar {
    display: none;
  }

  .coupon-tab {
    flex: 0 0 auto;
    position: relative;
    padding: 12px 16px;
    border: none;
    border-radius: 0;
    border-bottom: 2px solid transparent;
    background: transparent;
    color: #595959;
    font-size: 14px;
    font-weight: 600;
    margin-bottom: -1px;
    white-space: nowrap;
  }

  .coupon-tab.active {
    background: transparent;
    color: var(--color-primary);
    border-bottom-color: var(--color-primary);
  }

  .coupon-tab:hover:not(.active) {
    color: #1a1a1a;
  }

  .coupon-tab__count {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-width: 18px;
    height: 18px;
    padding: 0 5px;
    margin-left: 6px;
    background: #f5f5f5;
    color: #8c8c8c;
    border-radius: 9px;
    font-size: 11px;
    font-weight: 700;
  }

  .coupon-tab.active .coupon-tab__count {
    background: var(--color-primary);
    color: #fff;
  }

  .coupon-page-body {
    background: #fff;
    border-radius: 12px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
    overflow: hidden;
  }

  .coupon-desktop-table {
    display: block;
    overflow-x: auto;
  }

  .coupon-mobile-list {
    display: none;
  }

  .coupon-table {
    min-width: 760px;
  }

  .coupon-empty--mobile {
    display: none;
  }

  .coupon-table-empty {
    padding: 48px 24px;
    text-align: center;
  }

  .coupon-table-empty__text {
    font-size: 14px;
    font-weight: 600;
    color: #1a1a1a;
    margin-bottom: 6px;
  }

  .coupon-table-empty__desc {
    font-size: 13px;
    color: #8c8c8c;
    margin-bottom: 16px;
  }

  .coupon-table-empty__btn {
    font-size: 13px;
    padding: 7px 18px;
  }
}

/* 桌面端表格 */
.coupon-desktop-table .coupon-table {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
}

.coupon-table :deep(.ant-table-thead > tr > th) {
  background: #fafafa;
  font-weight: 600;
  color: #595959;
  border-bottom: 1px solid #f0f0f0;
  white-space: nowrap;
}

.coupon-table :deep(.ant-table-tbody > tr > td) {
  border-bottom: 1px solid #f5f5f5;
}

.coupon-table :deep(.ant-table-tbody > tr:hover > td) {
  background: #f7f7f7;
}

.coupon-table :deep(.ant-table-tbody > tr:last-child > td) {
  border-bottom: none;
}

.coupon-table__value-wrap {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.coupon-table__value {
  font-size: 15px;
  font-weight: 700;
  color: #1a1a1a;
}

.coupon-table__type {
  font-size: 12px;
  color: #8c8c8c;
}

.coupon-table__status {
  display: inline-flex;
  align-items: center;
  padding: 3px 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
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

.coupon-table__btn {
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

.coupon-table__btn:hover {
  background: var(--color-primary-hover);
}

.coupon-table__noop {
  color: #bfbfbf;
  font-size: 13px;
}

/* 暗色主题 */
body[data-theme="dark"] .coupon-page {
  background: #141414;
}

body[data-theme="dark"] .coupon-tabs,
body[data-theme="dark"] .coupon-card,
body[data-theme="dark"] .coupon-skeleton :deep(.ant-skeleton) {
  background: #1f1f1f;
}

body[data-theme="dark"] .coupon-tab {
  color: #a6a6a6;
}

body[data-theme="dark"] .coupon-tab.active {
  background: var(--color-primary);
  color: #fff;
}

body[data-theme="dark"] .coupon-card__value,
body[data-theme="dark"] .coupon-empty__title,
body[data-theme="dark"] .coupon-page-title {
  color: #f0f0f0;
}

body[data-theme="dark"] .coupon-card__code,
body[data-theme="dark"] .coupon-card__type,
body[data-theme="dark"] .coupon-card__scope,
body[data-theme="dark"] .coupon-empty__desc,
body[data-theme="dark"] .coupon-page-subtitle {
  color: #a6a6a6;
}

body[data-theme="dark"] .coupon-card__time {
  color: #737373;
}

body[data-theme="dark"] .coupon-card__footer {
  border-top-color: #2a2a2a;
}

body[data-theme="dark"] .coupon-card__status.unused {
  color: var(--color-primary);
  background: var(--color-primary-light);
}

body[data-theme="dark"] .coupon-card__status.used {
  color: #a6a6a6;
  background: #2a2a2a;
}

body[data-theme="dark"] .coupon-card__status.expired {
  color: #ff7875;
  background: rgba(255, 77, 79, 0.12);
}

body[data-theme="dark"] .coupon-empty__icon {
  background: var(--color-primary-bg);
  color: var(--color-primary);
}

body[data-theme="dark"] .coupon-desktop-table .coupon-table {
  background: #1f1f1f;
}

body[data-theme="dark"] .coupon-table :deep(.ant-table-thead > tr > th) {
  background: #262626;
  color: #a6a6a6;
  border-bottom-color: #2a2a2a;
}

body[data-theme="dark"] .coupon-table :deep(.ant-table-tbody > tr > td) {
  border-bottom-color: #2a2a2a;
  color: #f0f0f0;
}

body[data-theme="dark"] .coupon-table :deep(.ant-table-tbody > tr:hover > td) {
  background: #262626;
}

body[data-theme="dark"] .coupon-table__status.unused {
  color: var(--color-primary);
  background: var(--color-primary-light);
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

body[data-theme="dark"] .coupon-table-empty__text {
  color: #f0f0f0;
}

body[data-theme="dark"] .coupon-table-empty__desc {
  color: #a6a6a6;
}

@media (min-width: 769px) {
  body[data-theme="dark"] .coupon-page {
    background: transparent;
  }

  body[data-theme="dark"] .coupon-tabs {
    background: transparent;
    border-bottom-color: #303030;
  }

  body[data-theme="dark"] .coupon-tab {
    color: #a6a6a6;
  }

  body[data-theme="dark"] .coupon-tab.active {
    background: transparent;
    color: var(--color-primary);
    border-bottom-color: var(--color-primary);
  }

  body[data-theme="dark"] .coupon-tab__count {
    background: #303030;
    color: #a6a6a6;
  }

  body[data-theme="dark"] .coupon-tab.active .coupon-tab__count {
    background: var(--color-primary);
    color: #fff;
  }

  body[data-theme="dark"] .coupon-page-body {
    background: #1f1f1f;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.3);
  }
}
</style>
