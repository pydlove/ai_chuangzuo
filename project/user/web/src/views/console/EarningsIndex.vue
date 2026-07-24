<template>
  <div class="account-index">
    <div class="account-header">
      <h2 class="account-title">我的账户</h2>
      <p class="account-subtitle">
        查看账户余额、收益明细与结算状态
        <span class="account-rules-link" @click="rulesVisible = true">结算规则</span>
      </p>
    </div>

    <div class="account-tabs">
      <button
        :class="['account-tab', { active: activeTab === 'overview' }]"
        @click="activeTab = 'overview'"
      >
        账户明细
      </button>
      <button
        :class="['account-tab', { active: activeTab === 'earnings' }]"
        @click="activeTab = 'earnings'"
      >
        收益明细
      </button>
    </div>

    <!-- 账户明细 -->
    <div v-show="activeTab === 'overview'" class="account-content">
      <div class="account-stats">
        <div class="account-stat-card primary">
          <div class="account-stat-value">{{ summary.coinBalance.toFixed(2) }} <span class="account-stat-unit">创作币</span></div>
          <div class="account-stat-label-row">
            <CoinInfoTooltip>
              <div class="account-stat-label account-stat-label-tooltip">
                <span>账户余额</span>
                <svg class="account-info-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="12" cy="12" r="10"/>
                  <line x1="12" y1="16" x2="12" y2="12"/>
                  <line x1="12" y1="8" x2="12.01" y2="8"/>
                </svg>
              </div>
            </CoinInfoTooltip>
            <button class="account-stat-withdraw" @click="goToWithdraw">去提现</button>
          </div>
        </div>
        <div class="account-stat-card">
          <div class="account-stat-value">{{ summary.totalEarnings.toFixed(2) }}</div>
          <div class="account-stat-label">累计收益</div>
        </div>
        <div class="account-stat-card">
          <div class="account-stat-value">{{ summary.settledEarnings.toFixed(2) }}</div>
          <div class="account-stat-label">已结算</div>
        </div>
        <div class="account-stat-card">
          <div class="account-stat-value">{{ summary.unsettledEarnings.toFixed(2) }}</div>
          <div class="account-stat-label">未结算</div>
        </div>
      </div>

      <div class="account-section">
        <div class="account-section-header">
          <span class="account-section-title">按月结算</span>
          <button
            v-if="canSettleLastMonth"
            class="account-settle-btn"
            @click="handleMonthlySettle"
          >
            结算上月
          </button>
        </div>
        <div v-if="monthlyList.length === 0" class="account-empty">
          <div>还没有收益</div>
          <router-link to="/guide" class="guide-link">看看怎么赚创作币 →</router-link>
        </div>
        <div v-else class="monthly-list">
          <div
            v-for="item in monthlyList"
            :key="item.month"
            class="monthly-item"
          >
            <div class="monthly-info">
              <div class="monthly-title">{{ item.month }}</div>
              <div class="monthly-count">{{ item.count }} 笔收益</div>
            </div>
            <div class="monthly-amounts">
              <div class="monthly-amount">
                <span class="monthly-amount-label">总额</span>
                <span class="monthly-amount-value">{{ item.total.toFixed(2) }}</span>
              </div>
              <div class="monthly-amount">
                <span class="monthly-amount-label">已结算</span>
                <span class="monthly-amount-value settled">{{ item.settled.toFixed(2) }}</span>
              </div>
              <div class="monthly-amount">
                <span class="monthly-amount-label">未结算</span>
                <span class="monthly-amount-value unsettled">{{ item.unsettled.toFixed(2) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <a-modal
      v-model:open="rulesVisible"
      title="结算规则"
      :footer="null"
      :width="560"
      centered
      class="account-rules-modal"
    >
      <ol class="account-rules-list">
        <li>收益按<span class="account-rules-highlight">自然月</span>统计，每月<span class="account-rules-highlight">1 日</span>起可手动结算上月收益。</li>
        <li>点击「<span class="account-rules-highlight">结算上月</span>」后，上月未结算收益将<span class="account-rules-highlight">立即</span>转入账户余额。</li>
        <li>账户余额满 <span class="account-rules-highlight">100 创作币</span>可申请提现到支付宝，<span class="account-rules-highlight">1 创作币 = 1 元</span>人民币。</li>
        <li><span class="account-rules-highlight">未结算收益不可提现</span>，结算前请确认收益明细无误。</li>
      </ol>
      <div class="account-rules-footer">* 活动最终解释权归平台所有。</div>
    </a-modal>

    <!-- 收益明细 -->
    <div v-show="activeTab === 'earnings'" class="account-content">
      <div class="earnings-filters">
        <button
          v-for="filter in filters"
          :key="filter.key"
          :class="['earnings-filter', { active: activeFilter === filter.key }]"
          @click="activeFilter = filter.key"
        >
          {{ filter.label }}
        </button>
      </div>

      <div v-if="activeFilter === 'monthly'" class="account-section">
        <div v-if="monthlyList.length === 0" class="account-empty">
          暂无按月结算数据
        </div>
        <div v-else class="monthly-list">
          <div
            v-for="item in monthlyList"
            :key="item.month"
            class="monthly-item"
          >
            <div class="monthly-info">
              <div class="monthly-title">{{ item.month }}</div>
              <div class="monthly-count">{{ item.count }} 笔收益</div>
            </div>
            <div class="monthly-amounts">
              <div class="monthly-amount">
                <span class="monthly-amount-label">总额</span>
                <span class="monthly-amount-value">{{ item.total.toFixed(2) }}</span>
              </div>
              <div class="monthly-amount">
                <span class="monthly-amount-label">已结算</span>
                <span class="monthly-amount-value settled">{{ item.settled.toFixed(2) }}</span>
              </div>
              <div class="monthly-amount">
                <span class="monthly-amount-label">未结算</span>
                <span class="monthly-amount-value unsettled">{{ item.unsettled.toFixed(2) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-else class="earnings-list">
        <div v-if="filteredRecords.length === 0" class="account-empty">
          暂无收益记录
        </div>
        <div
          v-for="r in filteredRecords"
          :key="r.id"
          class="earnings-item"
        >
          <div class="earnings-item-left">
            <div class="earnings-item-title">{{ r.title }}</div>
            <div class="earnings-item-meta">
              {{ r.typeLabel }} · {{ r.statusLabel }} · {{ formatTime(r.createdAt) }}
              <span v-if="r.sourceLabel" class="earnings-item-source"> · {{ r.sourceLabel }}</span>
            </div>
            <div v-if="isInviteReward(r)" class="earnings-item-commission">
              {{ formatCommissionDetail(r) }}
            </div>
          </div>
          <div class="earnings-item-right">
            <span :class="['earnings-status', r.status]">{{ r.statusLabel }}</span>
            <span class="earnings-item-amount" :class="{ negative: r.amount < 0 }">
              {{ r.amount > 0 ? '+' : '' }}{{ r.amount.toFixed(2) }}
            </span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import CoinInfoTooltip from '@/components/CoinInfoTooltip.vue'
import { useEarnings } from '@/composables/useEarnings.js'

const router = useRouter()
const {
  summary,
  monthlyList,
  records,
  previousMonth,
  refreshAll,
  settle
} = useEarnings()

const activeTab = ref('overview')
const activeFilter = ref('all')
const rulesVisible = ref(false)

const filters = [
  { key: 'all', label: '全部' },
  { key: 'settled', label: '已结算' },
  { key: 'unsettled', label: '未结算' },
  { key: 'monthly', label: '按月结算' }
]

const canSettleLastMonth = computed(() => {
  return monthlyList.value.some(
    (item) => item.month === previousMonth.value && item.unsettled > 0
  )
})

const handleMonthlySettle = async () => {
  try {
    const result = await settle()
    if (result.settledCount > 0) {
      message.success(`上月收益已结算：${Number(result.settledAmount).toFixed(2)} 创作币`)
    } else {
      message.info('上月没有可结算的收益')
    }
  } catch (e) {
    message.error(e?.message || '结算失败')
  }
}

const goToWithdraw = () => {
  router.push('/console/coin?from=account')
}

const filteredRecords = computed(() => {
  if (activeFilter.value === 'all') return records.value
  return records.value.filter((r) => r.status === activeFilter.value)
})

const formatTime = (iso) => {
  if (!iso) return ''
  const d = new Date(iso)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

const isInviteReward = (record) => record.type === 'INVITE_REWARD' && record.orderAmount > 0

const formatCommissionDetail = (record) => {
  if (!record.orderAmount || record.orderAmount <= 0 || !record.commissionRate) {
    return record.description || ''
  }
  const purchaseLabel = record.isFirstPurchase === 1 ? '首购返佣' : '续费返佣'
  const ratePercent = (record.commissionRate * 100).toFixed(0)
  const orderAmount = record.orderAmount.toFixed(2)
  const amount = record.amount.toFixed(2)
  const cycleLabel = {
    month: '月卡',
    quarter: '季卡',
    year: '年卡'
  }[record.cycle] || record.cycle
  const planName = record.planName || record.planKey
  return `${purchaseLabel}：${planName}${cycleLabel} ¥${orderAmount} × ${ratePercent}% = ${amount} 创作币`
}

onMounted(() => {
  refreshAll()
})
</script>

<style scoped>
.account-index {
  height: 100%;
  padding: 24px;
  overflow-y: auto;
}

.account-header {
  margin-bottom: 20px;
}

.account-title {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 4px;
}

.account-subtitle {
  font-size: 13px;
  color: #8c8c8c;
  margin: 0;
}

.account-rules-link {
  color: #ff2442;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  text-decoration: underline;
  text-underline-offset: 3px;
  margin-left: 8px;
}

.account-rules-link:hover {
  color: #e61e3a;
}

.account-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
  background: #f5f5f5;
  padding: 4px;
  border-radius: 8px;
  width: fit-content;
}

.account-tab {
  padding: 8px 20px;
  border: none;
  background: transparent;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  color: #595959;
  cursor: pointer;
  transition: all 0.2s;
}

.account-tab.active {
  background: #fff;
  color: #1a1a1a;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
}

.account-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.account-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.account-stat-card {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 18px 20px;
}

.account-stat-card.primary {
  background: linear-gradient(135deg, #fff0f2 0%, #fff 100%);
  border-color: #ffd1d9;
}

.account-stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #ff2442;
  margin-bottom: 6px;
}

.account-stat-label {
  font-size: 13px;
  color: #8c8c8c;
}

.account-stat-label-tooltip {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  cursor: help;
  transition: color 0.2s;
}

.account-stat-label-tooltip:hover {
  color: #ff2442;
}

.account-info-icon {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
  color: #bfbfbf;
  transition: color 0.2s;
}

.account-stat-label-tooltip:hover .account-info-icon {
  color: #ff2442;
}

.account-stat-unit {
  font-size: 13px;
  font-weight: 500;
  color: #595959;
  margin-left: 4px;
}

.account-stat-label-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.account-stat-withdraw {
  font-size: 11px;
  padding: 2px 10px;
  border-radius: 10px;
  background: #ff2442;
  color: #fff;
  border: none;
  cursor: pointer;
  transition: background 0.2s, transform 0.15s;
  flex-shrink: 0;
}

.account-stat-withdraw:hover {
  background: #e0203b;
  transform: translateY(-1px);
}

.account-stat-withdraw:active {
  transform: translateY(0);
}

.account-section {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 20px;
}

.account-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.account-section-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.account-settle-btn {
  padding: 6px 14px;
  background: #ff2442;
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.2s;
}

.account-settle-btn:hover {
  background: #e61e3a;
}

.account-empty {
  padding: 48px 0;
  text-align: center;
  color: #8c8c8c;
  font-size: 14px;
}

.guide-link {
  display: inline-block;
  margin-top: 8px;
  color: #ff2442;
  font-size: 14px;
  text-decoration: underline;
  text-underline-offset: 3px;
}
.guide-link:hover {
  color: #e61e3a;
}

.account-rules-link {
  color: #ff2442;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  text-decoration: underline;
  text-underline-offset: 3px;
  margin-left: 8px;
}

.account-rules-link:hover {
  color: #e61e3a;
}

.account-rules-list {
  margin: 0;
  padding-left: 20px;
  font-size: 14px;
  color: #595959;
  line-height: 1.8;
}

.account-rules-list li {
  margin-bottom: 10px;
}

.account-rules-footer {
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid #f0f0f0;
  font-size: 13px;
  color: #8c8c8c;
}

.account-rules-highlight {
  color: #ff2442;
  font-weight: 500;
  text-decoration: underline;
  text-underline-offset: 3px;
}

.monthly-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.monthly-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  background: #fafafa;
  border-radius: 10px;
}

.monthly-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 4px;
}

.monthly-count {
  font-size: 12px;
  color: #8c8c8c;
}

.monthly-amounts {
  display: flex;
  gap: 24px;
}

.monthly-amount {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
}

.monthly-amount-label {
  font-size: 12px;
  color: #8c8c8c;
}

.monthly-amount-value {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
}

.monthly-amount-value.settled {
  color: #52c41a;
}

.monthly-amount-value.unsettled {
  color: #fa8c16;
}

.earnings-filters {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.earnings-filter {
  padding: 6px 14px;
  background: #f5f5f5;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  color: #595959;
  cursor: pointer;
  transition: all 0.2s;
}

.earnings-filter.active,
.earnings-filter:hover {
  background: #fff0f2;
  color: #ff2442;
}

.earnings-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.earnings-item {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  padding: 14px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.earnings-item-title {
  font-size: 14px;
  color: #1a1a1a;
  margin-bottom: 4px;
}

.earnings-item-meta {
  font-size: 12px;
  color: #8c8c8c;
}

.earnings-item-source {
  color: #ff2442;
}

.earnings-item-commission {
  margin-top: 6px;
  font-size: 12px;
  color: #595959;
  line-height: 1.5;
}

.earnings-item-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.earnings-status {
  font-size: 12px;
  padding: 3px 8px;
  border-radius: 4px;
  font-weight: 500;
}

.earnings-status.settled {
  background: #f6ffed;
  color: #389e0d;
}

.earnings-status.unsettled {
  background: #fff7e6;
  color: #d48806;
}

.earnings-item-amount {
  font-size: 15px;
  font-weight: 600;
  color: #ff2442;
  min-width: 70px;
  text-align: right;
}

.earnings-item-amount.negative {
  color: #ff4d4f;
}

@media (max-width: 900px) {
  .account-stats {
    grid-template-columns: repeat(2, 1fr);
  }

  .monthly-item {
    flex-direction: column;
    align-items: flex-start;
  }

  .monthly-amounts {
    width: 100%;
    justify-content: space-between;
  }
}

@media (max-width: 600px) {
  .account-stats {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .account-index {
    padding: 16px 12px;
  }
}

/* 深色模式 */
body[data-theme="dark"] .account-title,
body[data-theme="dark"] .account-section-title,
body[data-theme="dark"] .monthly-title,
body[data-theme="dark"] .earnings-item-title,
body[data-theme="dark"] .monthly-amount-value {
  color: #f0f0f0;
}

body[data-theme="dark"] .account-subtitle,
body[data-theme="dark"] .account-stat-label,
body[data-theme="dark"] .account-stat-unit,
body[data-theme="dark"] .account-empty,
body[data-theme="dark"] .account-rules-list,
body[data-theme="dark"] .account-rules-footer,
body[data-theme="dark"] .monthly-count,
body[data-theme="dark"] .monthly-amount-label,
body[data-theme="dark"] .earnings-item-meta {
  color: #a6a6a6;
}

body[data-theme="dark"] .earnings-item-source {
  color: #ff4d6f;
}

body[data-theme="dark"] .earnings-item-commission {
  color: #a6a6a6;
}

body[data-theme="dark"] .account-tabs {
  background: #141414;
}

body[data-theme="dark"] .account-tab {
  background-color: transparent !important;
  color: #a6a6a6;
}

body[data-theme="dark"] .account-tab:hover {
  color: #f0f0f0;
}

body[data-theme="dark"] .account-tab.active {
  background-color: #2a2a2a !important;
  color: #f0f0f0;
  box-shadow: none;
}

body[data-theme="dark"] .account-stat-card,
body[data-theme="dark"] .account-section,
body[data-theme="dark"] .monthly-item,
body[data-theme="dark"] .earnings-item,
body[data-theme="dark"] .earnings-filter {
  background: #1f1f1f;
  border-color: #303030;
}

body[data-theme="dark"] .account-stat-card.primary {
  background: linear-gradient(135deg, #331018 0%, #1f1f1f 100%);
  border-color: #52222b;
}

body[data-theme="dark"] .earnings-filter.active,
body[data-theme="dark"] .earnings-filter:hover {
  background: #331018;
}

body[data-theme="dark"] .account-rules-footer {
  border-color: #303030;
}

body[data-theme="dark"] .account-info-icon {
  color: #737373;
}

body[data-theme="dark"] .account-stat-label-tooltip:hover .account-info-icon {
  color: #ff4d6f;
}

body[data-theme="dark"] .account-stat-withdraw {
  background: var(--color-primary, #ff2442);
}

body[data-theme="dark"] .account-stat-withdraw:hover {
  background: var(--color-primary-hover, #e61e3a);
}

body[data-theme="dark"] .account-settle-btn {
  background: var(--color-primary, #ff2442);
}

body[data-theme="dark"] .account-settle-btn:hover {
  background: var(--color-primary-hover, #e61e3a);
}

body[data-theme="dark"] .earnings-status.settled {
  background: rgba(82, 196, 26, 0.15);
  color: #4ade80;
}

body[data-theme="dark"] .earnings-status.unsettled {
  background: rgba(250, 140, 22, 0.15);
  color: #ffa940;
}

body[data-theme="dark"] .guide-link {
  color: #ff4d6f;
}
</style>

<style>
/* 结算规则弹框：暗色全局覆盖（弹框 teleport 到 body，需非 scoped） */
body[data-theme="dark"] .account-rules-modal .ant-modal-content,
body[data-theme="dark"] .account-rules-modal .ant-modal-header {
  background: #1f1f1f !important;
  border-color: #303030 !important;
}

body[data-theme="dark"] .account-rules-modal .ant-modal-title {
  color: #f0f0f0 !important;
}

body[data-theme="dark"] .account-rules-modal .ant-modal-close-x {
  color: #a6a6a6 !important;
}

body[data-theme="dark"] .account-rules-modal .ant-modal-close:hover {
  background: #2a2a2a !important;
  color: #f0f0f0 !important;
}
</style>
