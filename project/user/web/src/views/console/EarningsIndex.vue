<template>
  <div class="account-index">
    <div class="account-header">
      <h2 class="account-title">我的账户</h2>
      <p class="account-subtitle">查看账户余额、收益明细</p>
    </div>

    <Tabs
      v-model="activeTab"
      :tabs="[
        { label: '账户明细', value: 'overview' },
        { label: '收益明细', value: 'earnings' }
      ]"
      variant="segment"
      active-type="surface"
      :scrollable="false"
    />

    <!-- 账户明细 -->
    <div v-show="activeTab === 'overview'" class="account-content">
      <StatCardGroup :columns="2">
        <StatCard variant="primary" value-first :value="summary.coinBalance.toFixed(2)" unit="创作币">
          <template #label>
            <div class="account-stat-label-row">
              <CoinInfoTooltip>
                <div class="account-stat-label account-stat-label-tooltip">
                  <span>账户余额</span>
                  <Icon name="info" class="account-info-icon" :size="14" />
                </div>
              </CoinInfoTooltip>
              <button class="account-stat-withdraw" @click="goToWithdraw">去提现</button>
            </div>
          </template>
        </StatCard>
        <StatCard variant="flat" value-first :value="summary.totalEarnings.toFixed(2)" label="累计收益" />
      </StatCardGroup>

      <div class="account-section">
        <div class="account-section-header">
          <span class="account-section-title">排行榜月度奖励</span>
        </div>
        <EmptyState v-if="monthlyList.length === 0" title="还没有收益" description="看看怎么赚创作币" action-text="查看指南" :action-handler="openGuide" size="md" />
        <div v-else class="monthly-list">
          <ListCard
            v-for="item in monthlyList"
            :key="item.month"
            :hover="false"
            custom-class="monthly-item"
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
            </div>
          </ListCard>
        </div>
      </div>
    </div>

    <!-- 收益详情 -->
    <a-modal
      v-model:open="detailVisible"
      title="收益详情"
      :footer="null"
      :width="440"
      centered
      class="earnings-detail-modal"
      @ok="detailVisible = false"
    >
      <div v-if="detailRecord" class="earnings-detail-body">
        <div class="earnings-detail-amount">
          <span class="earnings-detail-amount-value">{{ detailRecord.amount > 0 ? '+' : '' }}{{ detailRecord.amount.toFixed(2) }}</span>
          <span class="earnings-detail-amount-unit">创作币</span>
        </div>
        <div class="earnings-detail-status">
          <span class="earnings-status">实时到账</span>
        </div>

        <div class="earnings-detail-section">
          <div class="earnings-detail-row">
            <span class="earnings-detail-label">标题</span>
            <span class="earnings-detail-value">{{ detailRecord.title || '—' }}</span>
          </div>
          <div class="earnings-detail-row">
            <span class="earnings-detail-label">收益类型</span>
            <span class="earnings-detail-value">{{ detailRecord.typeLabel }}</span>
          </div>
          <div v-if="detailRecord.fromSkillMarket" class="earnings-detail-row">
            <span class="earnings-detail-label">来源</span>
            <span class="earnings-detail-value earnings-detail-source">提示词收益</span>
          </div>
          <div v-else-if="detailRecord.sourceLabel" class="earnings-detail-row">
            <span class="earnings-detail-label">来源</span>
            <span class="earnings-detail-value">{{ detailRecord.sourceLabel }}</span>
          </div>
          <div class="earnings-detail-row">
            <span class="earnings-detail-label">流水号</span>
            <span class="earnings-detail-value">
              <span class="biz-no">{{ detailRecord.bizNo || '—' }}</span>
              <button v-if="detailRecord.bizNo" class="copy-btn" @click="copyBizNo">复制</button>
            </span>
          </div>
        </div>

        <div v-if="isInviteReward(detailRecord)" class="earnings-detail-section">
          <div class="earnings-detail-row">
            <span class="earnings-detail-label">被邀请人</span>
            <span class="earnings-detail-value">{{ detailRecord.sourceLabel || '—' }}</span>
          </div>
          <div class="earnings-detail-row">
            <span class="earnings-detail-label">订阅套餐</span>
            <span class="earnings-detail-value">{{ detailPlanText }}</span>
          </div>
          <div class="earnings-detail-row">
            <span class="earnings-detail-label">订单金额</span>
            <span class="earnings-detail-value">¥{{ detailRecord.orderAmount.toFixed(2) }}</span>
          </div>
          <div class="earnings-detail-row">
            <span class="earnings-detail-label">返佣类型</span>
            <span class="earnings-detail-value">{{ detailRecord.isFirstPurchase === 1 ? '首购返佣' : '续费返佣' }}</span>
          </div>
          <div class="earnings-detail-row">
            <span class="earnings-detail-label">返佣比例</span>
            <span class="earnings-detail-value">{{ (detailRecord.commissionRate * 100).toFixed(0) }}%</span>
          </div>
        </div>

        <div v-if="isInviteReward(detailRecord)" class="earnings-detail-formula">
          <div class="earnings-detail-formula-label">计算明细</div>
          <div class="earnings-detail-formula-value">
            {{ detailPlanText }} ¥{{ detailRecord.orderAmount.toFixed(2) }} × {{ (detailRecord.commissionRate * 100).toFixed(0) }}% = {{ detailRecord.amount.toFixed(2) }} 创作币
          </div>
        </div>

        <div class="earnings-detail-section">
          <div class="earnings-detail-row">
            <span class="earnings-detail-label">到账时间</span>
            <span class="earnings-detail-value">{{ formatTime(detailRecord.createdAt) }}</span>
          </div>
        </div>
      </div>
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
        <EmptyState v-if="monthlyList.length === 0" title="暂无排行榜月度奖励" compact size="sm" />
        <div v-else class="monthly-list">
          <ListCard
            v-for="item in monthlyList"
            :key="item.month"
            :hover="false"
            custom-class="monthly-item"
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
            </div>
          </ListCard>
        </div>
      </div>

      <div v-else class="earnings-list">
        <EmptyState v-if="filteredRecords.length === 0" title="暂无收益记录" compact size="sm" />
        <div
          v-for="r in filteredRecords"
          :key="r.id"
          class="earnings-item"
          @click="openDetail(r)"
        >
          <div class="earnings-item-left">
            <div class="earnings-item-title">{{ r.title }}</div>
            <div class="earnings-item-meta">
              <span v-if="r.fromSkillMarket" class="earnings-source-tag">提示词收益</span>
              <span>{{ r.typeLabel }} · {{ formatTime(r.createdAt) }}</span>
              <span v-if="r.sourceLabel" class="earnings-item-source"> · {{ r.sourceLabel }}</span>
            </div>
            <div v-if="isInviteReward(r)" class="earnings-item-commission">
              {{ formatCommissionDetail(r) }}
            </div>
          </div>
          <div class="earnings-item-right">
            <span class="earnings-item-amount" :class="{ negative: r.amount < 0 }">
              {{ r.amount > 0 ? '+' : '' }}{{ r.amount.toFixed(2) }}
            </span>
            <button
              class="earnings-detail-btn"
              @click.stop="openDetail(r)"
            >
              详情
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import CoinInfoTooltip from '@/components/CoinInfoTooltip.vue'
import { useEarnings } from '@/composables/useEarnings.js'
import { useCopy } from '@/composables/useCopy.js'
import EmptyState from '@/components/common/EmptyState.vue'
import Tabs from '@/components/common/Tabs.vue'
import Icon from '@/components/common/Icon.vue'
import StatCard from '@/components/common/StatCard.vue'
import StatCardGroup from '@/components/common/StatCardGroup.vue'
import ListCard from '@/components/common/ListCard.vue'

const router = useRouter()
const {
  summary,
  monthlyList,
  records,
  refreshAll
} = useEarnings()

const activeTab = ref('overview')
const activeFilter = ref('all')
const detailVisible = ref(false)
const detailRecord = ref(null)

const filters = [
  { key: 'all', label: '全部' },
  { key: 'monthly', label: '排行榜奖励' }
]

const goToWithdraw = () => {
  router.push('/console/coin?from=account')
}

const openGuide = () => {
  window.open('/guide', '_blank')
}

const filteredRecords = computed(() => records.value)

const formatTime = (iso) => {
  if (!iso) return ''
  const d = new Date(iso)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

const isInviteReward = (record) => record.type === 'INVITE_REWARD' && record.orderAmount > 0

const openDetail = (record) => {
  detailRecord.value = record
  detailVisible.value = true
}

const { copy } = useCopy({
  successText: '流水号已复制',
  errorText: '复制失败'
})
const copyBizNo = () => {
  if (!detailRecord.value?.bizNo) return
  copy(detailRecord.value.bizNo)
}

const detailPlanText = computed(() => {
  if (!detailRecord.value) return ''
  const cycleLabel = {
    month: '月卡',
    quarter: '季卡',
    year: '年卡'
  }[detailRecord.value.cycle] || detailRecord.value.cycle
  const planName = detailRecord.value.planName || detailRecord.value.planKey
  return `${planName}${cycleLabel}`
})

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
  width: 100%;
  height: 100%;
  max-width: 1280px;
  margin: 0 auto;
  padding: 24px 32px;
  overflow-y: auto;
  box-sizing: border-box;
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

.account-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
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
  box-shadow: none;
}

.monthly-item:hover {
  transform: none;
  border-color: #f0f0f0;
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
  cursor: pointer;
  transition: background 0.2s, border-color 0.2s;
}

.earnings-item:hover {
  background: #fff8f9;
  border-color: #ffd1d9;
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

.earnings-source-tag {
  display: inline-block;
  padding: 1px 6px;
  margin-right: 6px;
  background: #fff0f2;
  color: #ff2442;
  border-radius: 4px;
  font-size: 11px;
  line-height: 1.4;
  vertical-align: middle;
}

.copy-btn {
  margin-left: 8px;
  padding: 2px 8px;
  font-size: 11px;
  line-height: 1.4;
  border-radius: 4px;
  border: none;
  background: #fff0f2;
  color: #ff2442;
  cursor: pointer;
  transition: background 0.2s;
}

.copy-btn:hover {
  background: #ffd1d9;
}

.biz-no {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  word-break: break-all;
}

.earnings-detail-source {
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

.earnings-detail-btn {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
  background: #fff0f2;
  color: #ff2442;
  border: none;
  cursor: pointer;
  transition: background 0.2s;
}

.earnings-detail-btn:hover {
  background: #ffd1d9;
}

.earnings-status {
  font-size: 12px;
  padding: 3px 8px;
  border-radius: 4px;
  font-weight: 500;
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

.earnings-detail-body {
  padding: 8px 4px;
}

.earnings-detail-amount {
  text-align: center;
  margin-bottom: 12px;
}

.earnings-detail-amount-value {
  font-size: 32px;
  font-weight: 700;
  color: #ff2442;
}

.earnings-detail-amount-unit {
  font-size: 14px;
  color: #595959;
  margin-left: 4px;
}

.earnings-detail-status {
  text-align: center;
  margin-bottom: 24px;
}

.earnings-detail-section {
  background: #fafafa;
  border-radius: 10px;
  padding: 14px 16px;
  margin-bottom: 16px;
}

.earnings-detail-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px dashed #f0f0f0;
  font-size: 14px;
}

.earnings-detail-row:last-child {
  border-bottom: none;
}

.earnings-detail-label {
  color: #8c8c8c;
}

.earnings-detail-value {
  color: #1a1a1a;
  font-weight: 500;
}

.earnings-detail-formula {
  background: #fff5f7;
  border: 1px dashed #ffd1d9;
  border-radius: 10px;
  padding: 14px 16px;
  margin-bottom: 16px;
}

.earnings-detail-formula-label {
  font-size: 13px;
  color: #8c8c8c;
  margin-bottom: 8px;
}

.earnings-detail-formula-value {
  font-size: 15px;
  color: #1a1a1a;
  font-weight: 500;
  line-height: 1.6;
  word-break: break-all;
}

@media (max-width: 900px) {
  .monthly-item {
    flex-direction: column;
    align-items: flex-start;
  }

  .monthly-amounts {
    width: 100%;
    justify-content: space-between;
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
body[data-theme="dark"] .account-empty,
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

body[data-theme="dark"] .earnings-item:hover {
  background: #331018;
  border-color: #52222b;
}

body[data-theme="dark"] .earnings-source-tag {
  background: rgba(255, 36, 66, 0.12);
  color: #ff4d6f;
}

body[data-theme="dark"] .copy-btn {
  background: rgba(255, 36, 66, 0.12);
  color: #ff4d6f;
}

body[data-theme="dark"] .copy-btn:hover {
  background: rgba(255, 36, 66, 0.2);
}

body[data-theme="dark"] .earnings-detail-source {
  color: #ff4d6f;
}

body[data-theme="dark"] .earnings-detail-btn {
  background: rgba(255, 36, 66, 0.12);
}

body[data-theme="dark"] .earnings-detail-btn:hover {
  background: rgba(255, 36, 66, 0.2);
}

body[data-theme="dark"] .earnings-detail-section {
  background: #262626;
}

body[data-theme="dark"] .earnings-detail-row {
  border-bottom-color: #303030;
}

body[data-theme="dark"] .earnings-detail-value,
body[data-theme="dark"] .earnings-detail-formula-value {
  color: #f0f0f0;
}

body[data-theme="dark"] .earnings-detail-formula {
  background: rgba(255, 36, 66, 0.08);
  border-color: rgba(255, 36, 66, 0.3);
}

body[data-theme="dark"] .account-section,
body[data-theme="dark"] .monthly-item,
body[data-theme="dark"] .earnings-item,
body[data-theme="dark"] .earnings-filter {
  background: #1f1f1f;
  border-color: #303030;
}

body[data-theme="dark"] .earnings-filter.active,
body[data-theme="dark"] .earnings-filter:hover {
  background: #331018;
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

body[data-theme="dark"] .guide-link {
  color: #ff4d6f;
}
</style>

<style>
/* 邀请奖励结算详情弹框：暗色全局覆盖 */
body[data-theme="dark"] .earnings-detail-modal .ant-modal-content,
body[data-theme="dark"] .earnings-detail-modal .ant-modal-header {
  background: #1f1f1f !important;
  border-color: #303030 !important;
}

body[data-theme="dark"] .earnings-detail-modal .ant-modal-title {
  color: #f0f0f0 !important;
}

body[data-theme="dark"] .earnings-detail-modal .ant-modal-close-x {
  color: #a6a6a6 !important;
}

body[data-theme="dark"] .earnings-detail-modal .ant-modal-close:hover {
  background: #2a2a2a !important;
  color: #f0f0f0 !important;
}
</style>
