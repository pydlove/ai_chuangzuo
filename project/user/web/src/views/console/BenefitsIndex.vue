<template>
  <!-- 桌面端 -->
  <div v-if="!isMobile" class="benefits-index">
    <!-- 页面头部 -->
    <div class="benefits-header">
      <div class="benefits-header-left">
        <h2 class="benefits-title">我的权益</h2>
        <p class="benefits-subtitle">查看当前套餐权益</p>
      </div>
      <button class="benefits-upgrade" @click="router.push('/pricing')">
        {{ planKey === 'free' ? '开通会员' : '升级套餐' }}
      </button>
    </div>

    <!-- 当前套餐卡片 -->
    <div class="benefits-plan-card">
      <div class="benefits-plan-main">
        <div class="benefits-plan-label">当前套餐</div>
        <div class="benefits-plan-name" :class="planColorClass">{{ planName }}</div>
        <div v-if="expiresAt" class="benefits-plan-expiry">有效期至 {{ expiresAt }}</div>
        <div v-else class="benefits-plan-expiry">未开通会员</div>
      </div>
      <div class="benefits-plan-badge" :class="planColorClass">
        <CrownOutlined />
      </div>
    </div>

    <!-- 权益列表 -->
    <div class="benefits-section">
      <SectionTitle title="权益明细" size="sm" />
      <ul v-if="!catalogLoading" class="benefits-list">
        <li
          v-for="item in displayBenefits"
          :key="item.code"
          class="benefits-item"
          :class="{ disabled: !item.included }"
        >
          <div class="benefits-item-icon">
            <component :is="iconFor(item.code)" />
          </div>
          <div class="benefits-item-body">
            <div class="benefits-item-header">
              <span class="benefits-item-name">{{ item.name }}</span>
              <span v-if="!item.included && item.requiredPlanName" :class="['benefits-item-tag', 'required', item.requiredPlan]">
                需{{ item.requiredPlanName }}
              </span>
              <span v-else-if="item.displayType === 'quota'" class="benefits-item-tag quota">
                {{ item.type === 'lifetime' ? '永久额度' : '月度额度' }}
              </span>
              <span v-else-if="item.displayType === 'limit'" class="benefits-item-tag tier">
                {{ item.limitLabel }}
              </span>
              <span v-else class="benefits-item-tag enabled">{{ item.inclusionLabel || '已包含' }}</span>
            </div>

            <!-- 额度类展示 -->
            <div v-if="item.displayType === 'quota' && item.included" class="benefits-quota">
              <div class="benefits-quota-meta">
                <span class="benefits-quota-remaining">剩余 {{ item.remaining }}</span>
                <span class="benefits-quota-used">已用 {{ item.used }} / 总额 {{ item.value }}{{ item.unit }}</span>
              </div>
              <div class="benefits-progress">
                <div class="benefits-progress-bar" :style="{ width: usagePercent(item) + '%' }" />
              </div>
            </div>
          </div>
        </li>

        <li v-if="displayBenefits.length === 0" class="benefits-empty">
          暂无权益信息
        </li>
      </ul>

      <div v-else class="benefits-loading">
        权益加载中…
      </div>
    </div>
  </div>

  <!-- 移动端：参考 VIP 会员权益页风格 -->
  <div v-else class="mobile-benefits">
    <!-- 自定义顶部栏 -->
    <header class="mb-header">
      <div class="mb-header-back" @click="router.back()">
        <LeftOutlined />
        <span>返回</span>
      </div>
      <h1 class="mb-header-title">我的权益</h1>
    </header>

    <!-- 深色沉浸式头部 -->
    <section class="mb-hero">
      <!-- 会员等级切换 -->
      <div class="mb-tier-tabs">
        <button
          v-for="plan in pricingPlans"
          :key="plan.key"
          :class="[
            'mb-tier-tab',
            { active: mobileSelectedPlan === plan.key, current: plan.key === planKey }
          ]"
          @click="mobileSelectedPlan = plan.key"
        >
          {{ plan.name }}
          <span v-if="plan.key === planKey" class="mb-tier-current">当前</span>
        </button>
      </div>

      <!-- VIP 大卡片 -->
      <div class="mb-vip-card">
        <div class="mb-vip-card-main">
          <div class="mb-vip-label">
            <img :src="vipBadgeFor(mobileSelectedPlan)" :alt="selectedPlanDisplay.name" class="mb-vip-badge" />
            <span class="mb-vip-name">{{ selectedPlanDisplay.name }}</span>
          </div>
          <div class="mb-vip-count">{{ selectedPlanDisplay.benefitCount }}大尊享特权</div>
          <div class="mb-vip-desc">{{ selectedPlanDisplay.desc }}</div>
          <div v-if="mobileSelectedPlan === planKey" class="mb-vip-expiry">
            {{ expiresAt ? `有效期至 ${expiresAt}` : '未开通会员' }}
          </div>
        </div>
        <button class="mb-vip-btn" @click="handleMobileSubscribe">
          {{ mobileButton.text }}
        </button>
      </div>

      <!-- 快捷权益图标 -->
      <div class="mb-quick-benefits">
        <div
          v-for="item in mobileTopBenefits"
          :key="item.code"
          class="mb-quick-item"
        >
          <div class="mb-quick-icon">
            <component :is="iconFor(item.code)" />
          </div>
          <span class="mb-quick-label">{{ item.name }}</span>
          <span class="mb-quick-value">{{ item.value }}</span>
        </div>
      </div>

      <div class="mb-rules-link" @click="router.push('/pricing')">查看完整权益规则</div>
    </section>

    <!-- 权益对比 -->
    <section class="mb-compare">
      <div class="mb-compare-header">
        <h2 class="mb-compare-title">{{ PLAN_SHORT_NAMES[mobileSelectedPlan] }}尊享</h2>
        <span class="mb-compare-sub">{{ PLAN_SHORT_NAMES[mobileSelectedPlan] }} vs 普通用户</span>
      </div>

      <div class="mb-compare-tabs">
        <button
          v-for="tab in compareTabs"
          :key="tab.key"
          :class="['mb-compare-tab', { active: activeCompareTab === tab.key }]"
          @click="activeCompareTab = tab.key"
        >
          {{ tab.label }}
        </button>
      </div>

      <div class="mb-compare-list">
        <div class="mb-compare-row header">
          <div class="mb-compare-feature">
            <span class="mb-compare-feature-name">权益</span>
          </div>
          <div class="mb-compare-values">
            <div class="mb-compare-plan current">
              <img :src="vipBadgeFor(mobileSelectedPlan)" :alt="selectedPlanDisplay.name" class="mb-compare-plan-badge" />
            </div>
            <div class="mb-compare-plan free">
              <span class="mb-compare-plan-name">普通用户</span>
            </div>
          </div>
        </div>
        <div
          v-for="row in filteredCompareRows"
          :key="row.code"
          class="mb-compare-row"
        >
          <div class="mb-compare-feature">
            <span class="mb-compare-feature-name">{{ row.label }}</span>
            <span v-if="isPlanExclusive(row)" class="mb-compare-feature-tag">专属</span>
          </div>
          <div class="mb-compare-values">
            <div class="mb-compare-plan current">
              <span class="mb-compare-plan-value yes">{{ formatCompareValue(row[mobileSelectedPlan]) }}</span>
            </div>
            <div class="mb-compare-plan free">
              <span class="mb-compare-plan-value no">{{ formatCompareValue(row.free) }}</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 底部固定操作栏 -->
    <div class="mb-footer-space" />
    <div class="mb-footer-action">
      <div class="mb-footer-info">
        <div class="mb-footer-price">
          <span class="mb-footer-current">¥{{ selectedPlanPrice.current }}</span>
          <span v-if="selectedPlanPrice.original" class="mb-footer-original">¥{{ selectedPlanPrice.original }}</span>
          <span class="mb-footer-period">/{{ periodLabel }}</span>
        </div>
        <div class="mb-footer-cycle">
          <button
            v-for="cycle in cycles"
            :key="cycle.key"
            :class="['mb-footer-cycle-btn', { active: activeCycle === cycle.key, disabled: isCycleDisabled(cycle.key) }]"
            :disabled="isCycleDisabled(cycle.key)"
            @click="setCycle(cycle.key)"
          >
            {{ cycle.label }}
          </button>
        </div>
      </div>
      <button
        :class="['mb-footer-btn', { disabled: mobileButton.disabled }]"
        :disabled="mobileButton.disabled"
        @click="handleMobileSubscribe"
      >
        {{ mobileButton.text }}
      </button>
    </div>

    <!-- 支付相关弹框（复用 usePricing） -->
    <a-modal
      v-model:open="upgradeModalVisible"
      :title="`确认升级 ${selectedPlan ? selectedPlan.name : ''}`"
      :width="320"
      centered
      class="mb-upgrade-modal"
      @ok="confirmUpgrade"
      :confirm-loading="upgradeLoading"
    >
      <div v-if="upgradePreview" class="mb-upgrade-panel">
        <div class="mb-upgrade-row">
          <span class="mb-upgrade-label">当前套餐</span>
          <span class="mb-upgrade-value">{{ upgradePreview.currentPlanName }}</span>
        </div>
        <div class="mb-upgrade-row">
          <span class="mb-upgrade-label">剩余天数</span>
          <span class="mb-upgrade-value">{{ upgradePreview.remainingDays }} 天</span>
        </div>
        <div class="mb-upgrade-row">
          <span class="mb-upgrade-label">抵扣金额</span>
          <span class="mb-upgrade-value credit">-¥{{ upgradePreview.creditAmount }}</span>
        </div>
        <div v-if="selectedCoinAmount > 0" class="mb-upgrade-row">
          <span class="mb-upgrade-label">创作币抵扣</span>
          <span class="mb-upgrade-value credit">-{{ selectedCoinAmount }} 创作币（-¥{{ (selectedCoinAmount / COIN_TO_YUAN_RATIO).toFixed(2) }}）</span>
        </div>
        <div class="mb-upgrade-row">
          <span class="mb-upgrade-label">新套餐价格</span>
          <span class="mb-upgrade-value">¥{{ upgradePreview.originalPrice }}</span>
        </div>
        <div class="mb-upgrade-row total">
          <span class="mb-upgrade-label">实付金额</span>
          <span class="mb-upgrade-value final">¥{{ getFinalCash() }}</span>
        </div>
        <p class="mb-upgrade-tip">升级后立即生效，有效期 {{ upgradePreview.targetDays }} 天至 {{ upgradePreview.newExpiresAt }}。</p>
      </div>
    </a-modal>

    <a-modal
      v-model:open="modalVisible"
      :title="modalTitle"
      :width="320"
      centered
      class="mb-subscribe-modal membership-confirm-modal"
      :closable="!payQrUrl"
      :mask-closable="!payQrUrl"
      :keyboard="!payQrUrl"
      :footer="null"
      @cancel="handleModalCancel"
    >
      <div class="mb-pay-panel">
        <template v-if="isTestMode()">
          <CoinDiscountPanel
            v-if="coinBalance > 0 && getMaxCoinAmount() > 0"
            v-model:selectedCoinAmount="selectedCoinAmount"
            :coinBalance="coinBalance"
            :maxCoinAmount="getMaxCoinAmount()"
            :coinToYuanRatio="COIN_TO_YUAN_RATIO"
            :finalCash="getFinalCash()"
          />
          <p class="mb-pay-tip">
            测试阶段，请输入支付码 <strong>123456</strong> 完成{{ upgradePreview ? '升级' : '订阅' }}。
          </p>
          <a-input
            v-model:value="payCode"
            placeholder="请输入 6 位支付码"
            maxlength="6"
            size="large"
            @pressEnter="handlePay"
          />
          <div class="mb-pay-actions">
            <a-button type="primary" :loading="subscribeLoading" size="large" block @click="handlePay">
              确认{{ upgradePreview ? '升级' : '订阅' }}
            </a-button>
          </div>
        </template>
        <template v-else-if="payQrUrl">
          <div class="mb-qr-pay">
            <div class="mb-qr-pay-amount">
              <span class="mb-qr-pay-amount-label">微信支付</span>
              <span class="mb-qr-pay-amount-value">¥{{ getFinalCash() }}</span>
            </div>
            <div class="mb-qr-code-wrap" :class="{ expired: qrExpired }">
              <img :src="payQrUrl" alt="微信支付二维码" class="mb-qr-code-img" />
              <img src="/assets/images/微信.png" alt="微信" class="mb-qr-code-logo" />
              <div v-if="qrExpired" class="mb-qr-code-mask" @click="handlePay">
                <div class="mb-qr-code-refresh">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M21.5 2v6h-6M2.5 22v-6h6M2 11.5a10 10 0 0 1 18.8-4.3M22 12.5a10 10 0 0 1-18.8 4.2"/>
                  </svg>
                  <span>点击刷新</span>
                </div>
              </div>
            </div>
            <p v-if="!qrExpired" class="mb-qr-code-tip">
              请使用微信扫一扫完成支付
              <span class="mb-qr-code-countdown">（{{ qrExpireSeconds }} 秒后失效）</span>
            </p>
            <p v-else class="mb-qr-code-tip mb-qr-code-tip--refresh" @click="handlePay">
              点击刷新二维码
            </p>
            <ul class="mb-qr-pay-terms">
              <li>开通{{ selectedPlan?.name }}{{ cycleLabel[activeCycle] }}套餐</li>
              <li>会员服务属于虚拟商品，一经支付无法退款，请你谅解</li>
              <li>会员到期日前 7 天，系统将通过消息中心给您发送提醒消息</li>
              <li>未成年用户请在监护人陪同下理性充值，避免过度消费</li>
              <li>支付完成后会员将自动开通</li>
            </ul>
            <div class="mb-qr-pay-agreement">
              <PaidServiceAgreement />
            </div>
          </div>
          <div class="mb-pay-actions">
            <a-button size="large" block @click="handleModalCancel">关闭</a-button>
          </div>
        </template>
        <template v-else>
          <CoinDiscountPanel
            v-if="coinBalance > 0 && getMaxCoinAmount() > 0"
            v-model:selectedCoinAmount="selectedCoinAmount"
            :coinBalance="coinBalance"
            :maxCoinAmount="getMaxCoinAmount()"
            :coinToYuanRatio="COIN_TO_YUAN_RATIO"
            :finalCash="getFinalCash()"
          />
          <div class="mb-pay-agreement-confirm">
            <div class="mb-pay-agreement-confirm-body">
              <PaidServiceAgreement />
            </div>
          </div>
          <div class="mb-pay-actions dual">
            <a-button size="large" block @click="handleModalCancel">取消</a-button>
            <a-button type="primary" :loading="subscribeLoading" size="large" block @click="handlePay">
              同意并继续
            </a-button>
          </div>
        </template>
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useDevice } from '@/composables/useDevice.js'
import { useBenefits } from '@/composables/useBenefits.js'
import { usePricing } from '@/composables/usePricing.js'
import CoinDiscountPanel from '@/components/pricing/CoinDiscountPanel.vue'
import PaidServiceAgreement from '@/components/PaidServiceAgreement.vue'
import SectionTitle from '@/components/common/SectionTitle.vue'
import {
  CrownOutlined,
  LeftOutlined,
  FileTextOutlined,
  FileWordOutlined,
  CopyOutlined,
  BulbOutlined,
  EditOutlined,
  DesktopOutlined,
  SmileOutlined,
  TagsOutlined,
  LayoutOutlined,
  PictureOutlined,
  DatabaseOutlined,
  HistoryOutlined,
  RocketOutlined,
  UnorderedListOutlined,
  ShopOutlined,
  BookOutlined,
  FontSizeOutlined,
  ThunderboltOutlined,
  AppstoreOutlined,
  ShareAltOutlined
} from '@ant-design/icons-vue'

const router = useRouter()
const { isMobile } = useDevice()
const { benefits, planKey, planName, expiresAt, loadBenefits } = useBenefits()

const pricing = usePricing()
const {
  plans: pricingPlans,
  compareRows,
  catalogLoading,
  activeCycle,
  cycles,
  cycleLocked,
  setCycle,
  isCycleDisabled,
  cycleLabel,
  getPeriodLabel,
  getPrice,
  getPlanButton,
  handleSubscribe,
  modalVisible,
  selectedPlan,
  payCode,
  subscribeLoading,
  selectedCoinAmount,
  payQrUrl,
  currentOrderNo,
  qrExpireSeconds,
  qrExpired,
  resetQrExpire,
  upgradeModalVisible,
  upgradePreview,
  upgradeLoading,
  confirmUpgrade,
  handlePay,
  stopPolling,
  isTestMode,
  coinBalance,
  COIN_TO_YUAN_RATIO,
  getMaxCoinAmount,
  getFinalCash
} = pricing

const modalTitle = computed(() => {
  if (payQrUrl.value) {
    return '微信扫码支付'
  }
  if (upgradePreview.value) {
    return '确认支付升级'
  }
  return '同意 爱创作工坊 的协议'
})

const handleModalCancel = () => {
  modalVisible.value = false
  payQrUrl.value = ''
  currentOrderNo.value = ''
  resetQrExpire()
  stopPolling()
}

const PLAN_ORDER = ['basic', 'pro', 'flagship']
const PLAN_NAMES = {
  basic: '基础版',
  pro: '专业版',
  flagship: '旗舰版'
}

const PLAN_BADGES = {
  basic: '/assets/images/vip/VIP.svg',
  pro: '/assets/images/vip/SVIP.svg',
  flagship: '/assets/images/vip/SSVIP.svg'
}

const PLAN_SHORT_NAMES = {
  basic: 'VIP',
  pro: 'SVIP',
  flagship: 'SSVIP'
}

const PLAN_DESC = {
  basic: '入门创作者首选，轻松开启 AI 写作',
  pro: '爆款内容加速器，效率翻倍更省心',
  flagship: '全功能 unlimited，团队级创作赋能'
}

function vipBadgeFor(planKey) {
  return PLAN_BADGES[planKey] || PLAN_BADGES.basic
}

// 按「额度」展示进度条的权益（有剩余/已用/总额概念）
const QUOTA_CODES = [
  'ai_article_quota',
  'plan_adjust_quota',
  'skill_custom',
  'skill_learn_analyze',
  'skill_market_publish',
  'sticker_quota'
]

// 这些权益显示为限制值/文案，不展示额度进度
const LIMIT_CODES = ['generation_rate_limit', 'generation_word_limit', 'history_days', 'queue_max_tasks']

const QUOTA_UNITS = {
  plan_adjust_quota: '次',
  skill_custom: '个',
  skill_learn_analyze: '次',
  ai_article_quota: '篇',
  sticker_quota: '张',
  skill_market_publish: '个'
}

const iconMap = {
  ai_article_quota: FileTextOutlined,
  plan_adjust_quota: EditOutlined,
  repost_plan: ShareAltOutlined,
  export_word: FileWordOutlined,
  copy_text: CopyOutlined,
  ai_topic: BulbOutlined,
  ai_title_optimize: EditOutlined,
  online_edit: DesktopOutlined,
  skill_custom: SmileOutlined,
  seo_keywords: TagsOutlined,
  template_access: LayoutOutlined,
  sticker_quota: PictureOutlined,
  history_days: HistoryOutlined,
  queue_priority: RocketOutlined,
  queue_max_tasks: UnorderedListOutlined,
  skill_market_publish: ShopOutlined,
  skill_learn_analyze: BookOutlined,
  generation_word_limit: FontSizeOutlined,
  generation_rate_limit: ThunderboltOutlined
}

function iconFor(code) {
  return iconMap[code] || AppstoreOutlined
}

function usagePercent(item) {
  const limit = parseInt(item.value, 10) || 0
  const used = item.used || 0
  if (limit <= 0) return 0
  return Math.min((used / limit) * 100, 100)
}

function isIncluded(cellValue) {
  if (cellValue === true) return true
  if (cellValue === false) return false
  if (cellValue === null || cellValue === undefined) return false
  const str = String(cellValue).trim()
  if (str === '' || str === 'false' || str === 'none' || str === '0') return false
  return true
}

function formatInclusionLabel(cellValue) {
  if (cellValue === true) return '已包含'
  if (cellValue === false) return ''
  const str = String(cellValue).trim()
  if (str === '' || str === 'false' || str === 'none') return ''
  return str
}

function findMinRequiredPlan(row) {
  if (!row) return null
  for (const key of PLAN_ORDER) {
    const cell = row[key]
    if (cell != null && isIncluded(cell.value)) {
      return key
    }
  }
  return null
}

function getCellForPlan(row, planKey) {
  if (!row) return null
  return row[planKey] || null
}

function inferType(code) {
  const item = benefits.value?.[code]
  if (item?.type) return item.type
  if (LIMIT_CODES.includes(code)) return 'quota'
  return 'boolean'
}

const displayBenefits = computed(() => {
  const rows = compareRows.value || []
  const userBenefits = benefits.value || {}
  const currentPlan = planKey.value || 'free'

  // catalog 未加载完成时，先按用户已有权益兜底展示
  if (rows.length === 0) {
    return Object.values(userBenefits).map((item) => ({
      code: item.code,
      name: item.name,
      type: item.type,
      displayType: QUOTA_CODES.includes(item.code) ? 'quota' : 'limit',
      value: item.value,
      used: item.used ?? 0,
      remaining: item.remaining ?? 0,
      included: true,
      requiredPlan: null,
      requiredPlanName: '',
      limitLabel: '',
      unit: QUOTA_UNITS[item.code] || '',
      inclusionLabel: ''
    }))
  }

  const items = rows.map((row) => {
    const code = row.code
    const userItem = userBenefits[code]
    const rowType = inferType(code)
    const displayType = QUOTA_CODES.includes(code)
      ? 'quota'
      : LIMIT_CODES.includes(code)
        ? 'limit'
        : 'inclusion'

    const currentCell = getCellForPlan(row, currentPlan)
    const currentIncluded = currentPlan !== 'free' && currentCell != null && isIncluded(currentCell.value)
    const minRequiredPlan = findMinRequiredPlan(row)

    let limitLabel = ''
    if (displayType === 'limit') {
      const labelCell = currentCell || (minRequiredPlan ? getCellForPlan(row, minRequiredPlan) : null)
      limitLabel = labelCell && labelCell.value != null ? String(labelCell.value) : ''
    }

    return {
      code,
      name: row.label || userItem?.name || code,
      type: rowType,
      displayType,
      value: userItem?.value ?? null,
      used: userItem?.used ?? 0,
      remaining: userItem?.remaining ?? 0,
      included: currentIncluded,
      requiredPlan: currentIncluded ? null : minRequiredPlan,
      requiredPlanName: currentIncluded ? '' : (PLAN_NAMES[minRequiredPlan] || ''),
      limitLabel,
      unit: QUOTA_UNITS[code] || '',
      inclusionLabel: currentIncluded && displayType === 'inclusion'
        ? formatInclusionLabel(currentCell.value)
        : ''
    }
  })

  // 已包含在前；同状态下按 额度 > 限制 > 包含 排序
  const typeOrder = { quota: 1, limit: 2, inclusion: 3 }
  return items.sort((a, b) => {
    if (a.included !== b.included) return a.included ? -1 : 1
    return (typeOrder[a.displayType] || 4) - (typeOrder[b.displayType] || 4)
  })
})

const planColorClass = computed(() => {
  const key = planKey.value || 'free'
  return {
    'plan-free': key === 'free',
    'plan-basic': key === 'basic',
    'plan-pro': key === 'pro',
    'plan-flagship': key === 'flagship'
  }
})

// ================== 移动端 ==================
const mobileSelectedPlan = ref(planKey.value && planKey.value !== 'free' ? planKey.value : 'pro')
const activeCompareTab = ref('all')

watch(planKey, (key) => {
  if (key && key !== 'free') {
    mobileSelectedPlan.value = key
  }
})

const compareTabs = [
  { key: 'all', label: '全部' },
  { key: 'exclusive', label: '专属特权' },
  { key: 'quota', label: '额度权益' },
  { key: 'limit', label: '功能权益' }
]

const selectedPlanDisplay = computed(() => {
  const key = mobileSelectedPlan.value
  const rows = compareRows.value || []
  const includedRows = rows.filter((row) => isIncluded(row[key]?.value))
  return {
    key,
    name: PLAN_NAMES[key] || '专业版',
    benefitCount: includedRows.length || 10,
    desc: PLAN_DESC[key] || '解锁更多 AI 创作特权'
  }
})

const mobileTopBenefits = computed(() => {
  const rows = compareRows.value || []
  const key = mobileSelectedPlan.value
  const picked = []
  for (const row of rows) {
    if (isIncluded(row[key]?.value)) {
      const val = row[key]?.value
      picked.push({
        code: row.code,
        name: row.label,
        value: val === true || val === 'true' ? '已包含' : String(val)
      })
      if (picked.length >= 4) break
    }
  }
  return picked.length ? picked : [
    { code: 'ai_article_quota', name: 'AI 文章', value: '不限量' },
    { code: 'export_word', name: '导出 Word', value: '已包含' },
    { code: 'ai_title_optimize', name: '标题优化', value: '已包含' },
    { code: 'online_edit', name: '在线编辑', value: '已包含' }
  ]
})

const filteredCompareRows = computed(() => {
  const rows = compareRows.value || []
  const key = mobileSelectedPlan.value
  if (activeCompareTab.value === 'all') {
    return rows.filter((row) => row[key] != null || row.free != null)
  }
  if (activeCompareTab.value === 'exclusive') {
    return rows.filter((row) => isIncluded(row[key]?.value) && !isIncluded(row.free?.value))
  }
  if (activeCompareTab.value === 'quota') {
    return rows.filter((row) => QUOTA_CODES.includes(row.code))
  }
  if (activeCompareTab.value === 'limit') {
    return rows.filter((row) => LIMIT_CODES.includes(row.code))
  }
  return rows
})

function formatCompareValue(cell) {
  if (cell == null) return '—'
  const val = cell.value
  if (val === true || val === 'true') return '✓'
  if (val === false || val === 'false') return '✗'
  if (val === '' || val == null) return '—'
  return String(val)
}

function isPlanExclusive(row) {
  return isIncluded(row[mobileSelectedPlan.value]?.value) && !isIncluded(row.free?.value)
}

const selectedPlanObj = computed(() => pricingPlans.value.find((p) => p.key === mobileSelectedPlan.value))
const selectedPlanPrice = computed(() => {
  const plan = selectedPlanObj.value
  if (!plan) return { current: 0, original: 0 }
  return getPrice(plan)
})
const periodLabel = computed(() => getPeriodLabel())

const mobileButton = computed(() => {
  const plan = selectedPlanObj.value
  if (!plan) return { text: '立即开通', disabled: true }
  const btn = getPlanButton(plan)
  return { text: btn.text, disabled: btn.disabled }
})

function handleMobileSubscribe() {
  const plan = selectedPlanObj.value
  if (!plan) return
  handleSubscribe(plan)
}

onMounted(() => {
  loadBenefits()
  // usePricing 已在 mount 时自动加载套餐目录，无需重复请求
})
</script>

<style scoped>
/* ================= 桌面端 ================= */
.benefits-index {
  width: 100%;
  max-width: 1280px;
  margin: 0 auto;
  padding: 24px 32px;
  box-sizing: border-box;
}

.benefits-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.benefits-header-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.benefits-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
}

.benefits-subtitle {
  margin: 0;
  font-size: 13px;
  color: #8c8c8c;
}

.benefits-upgrade {
  padding: 8px 18px;
  border: none;
  border-radius: 8px;
  background: #ff2442;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.benefits-upgrade:hover {
  background: #e61e3a;
}

.benefits-plan-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 24px;
  border-radius: 16px;
  background: #fff;
  border: 1px solid #f0f0f0;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  margin-bottom: 24px;
}

.benefits-plan-main {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.benefits-plan-label {
  width: fit-content;
  padding: 2px 10px;
  border-radius: 999px;
  background: #f5f5f5;
  color: #595959;
  font-size: 12px;
  font-weight: 600;
}

.benefits-plan-name {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a1a;
}

.benefits-plan-name.plan-basic {
  color: #8c6b00;
}

.benefits-plan-name.plan-pro {
  background: linear-gradient(30deg, #dfb738 0%, #fb8301 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  color: #fb8301;
}

.benefits-plan-name.plan-flagship {
  background: linear-gradient(135deg, #a05013 0%, #db3708 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  color: #db3708;
}

.benefits-plan-expiry {
  font-size: 13px;
  color: #8c8c8c;
}

.benefits-plan-badge {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: #f5f5f5;
  color: #8c8c8c;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
  flex-shrink: 0;
}

.benefits-plan-badge.plan-basic {
  background: linear-gradient(135deg, #fffbe6 0%, #fff3a3 100%);
  color: #8c6b00;
  box-shadow: 0 4px 12px rgba(140, 107, 0, 0.12);
}

.benefits-plan-badge.plan-pro {
  background: linear-gradient(30deg, #dfb738 0%, #fb8301 100%);
  color: #fff;
  box-shadow: 0 4px 12px rgba(251, 131, 1, 0.22);
}

.benefits-plan-badge.plan-flagship {
  background: linear-gradient(135deg, #a05013 0%, #db3708 100%);
  color: #fff;
  border: none;
  box-shadow: 0 4px 12px rgba(219, 55, 8, 0.22);
}

.benefits-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.benefits-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

.benefits-item {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 18px;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.benefits-item:hover {
  border-color: #ffd1d9;
  box-shadow: 0 2px 12px rgba(255, 36, 66, 0.08);
}

.benefits-item.disabled {
  background: #fafafa;
}

.benefits-item.disabled:hover {
  border-color: #f0f0f0;
  box-shadow: none;
}

.benefits-item.disabled .benefits-item-icon {
  background: #f0f0f0;
  color: #8c8c8c;
}

.benefits-item.disabled .benefits-item-name {
  color: #8c8c8c;
}

.benefits-item-icon {
  flex-shrink: 0;
  width: 44px;
  height: 44px;
  border-radius: 10px;
  background: #fff5f7;
  color: #ff2442;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.benefits-item-body {
  flex: 1;
  min-width: 0;
}

.benefits-item-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 10px;
}

.benefits-item-name {
  font-size: 15px;
  font-weight: 500;
  color: #1a1a1a;
}

.benefits-item-tag {
  position: relative;
  z-index: 1;
  flex-shrink: 0;
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.benefits-item-tag.enabled {
  background: #52c41a;
  color: #fff;
}

.benefits-item-tag.required {
  background: #fff1f0;
  color: #ff4d4f;
}

.benefits-item-tag.required.basic {
  background: linear-gradient(135deg, #fffbe6 0%, #fff3a3 100%);
  color: #8c6b00;
}

.benefits-item-tag.required.pro {
  background: linear-gradient(30deg, #dfb738 0%, #fb8301 100%);
  color: #fff;
}

.benefits-item-tag.required.flagship {
  background: linear-gradient(135deg, #a05013 0%, #db3708 100%);
  color: #fff;
}

.benefits-item-tag.tier {
  background: #52c41a;
  color: #fff;
}

.benefits-item-tag.quota {
  background: #52c41a;
  color: #fff;
}

.benefits-quota {
  margin-top: 4px;
}

.benefits-quota-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 13px;
}

.benefits-quota-remaining {
  color: #ff2442;
  font-weight: 600;
}

.benefits-quota-used {
  color: #8c8c8c;
}

.benefits-progress {
  height: 6px;
  border-radius: 3px;
  background: #f0f0f0;
  overflow: hidden;
}

.benefits-progress-bar {
  height: 100%;
  border-radius: 3px;
  background: linear-gradient(90deg, #ff2442 0%, #ff6b8a 100%);
  transition: width 0.3s ease;
}

.benefits-empty {
  grid-column: 1 / -1;
  padding: 48px 16px;
  text-align: center;
  color: #8c8c8c;
  font-size: 14px;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
}

.benefits-loading {
  grid-column: 1 / -1;
  padding: 48px 16px;
  text-align: center;
  color: #8c8c8c;
  font-size: 14px;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
}

/* ================= 移动端 VIP 权益页 ================= */
.mobile-benefits {
  min-height: 100vh;
  background: #f8f8f8;
  color: #1a1a1a;
  padding-bottom: calc(80px + env(safe-area-inset-bottom));
  -webkit-font-smoothing: antialiased;
}

/* 顶部栏 */
.mb-header {
  position: sticky;
  top: 0;
  z-index: 40;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 48px;
  padding: 0 12px;
  background: #fff;
  color: #1a1a1a;
  border-bottom: 1px solid #f0f0f0;
}

.mb-header-back {
  position: absolute;
  left: 12px;
  display: flex;
  align-items: center;
  gap: 2px;
  color: #595959;
  font-size: 14px;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
}

.mb-header-back :deep(.anticon) {
  font-size: 14px;
}

.mb-header-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

/* 头部主题区 */
.mb-hero {
  position: relative;
  padding: 16px 16px 24px;
  background: linear-gradient(180deg, #fff5f7 0%, #ffffff 100%);
  color: #1a1a1a;
  overflow: hidden;
}

.mb-hero::before {
  content: '';
  position: absolute;
  top: -60px;
  right: -60px;
  width: 220px;
  height: 220px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 36, 66, 0.12) 0%, transparent 70%);
  pointer-events: none;
}

.mb-hero::after {
  content: '';
  position: absolute;
  bottom: -40px;
  left: -40px;
  width: 160px;
  height: 160px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 36, 66, 0.08) 0%, transparent 70%);
  pointer-events: none;
}

/* 等级标签 */
.mb-tier-tabs {
  position: relative;
  z-index: 1;
  display: flex;
  gap: 10px;
  margin-bottom: 18px;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: none;
}

.mb-tier-tabs::-webkit-scrollbar {
  display: none;
}

.mb-tier-tab {
  position: relative;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 42px;
  padding: 0 22px;
  border: none;
  border-radius: 999px;
  background: #fff;
  color: #595959;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
  -webkit-tap-highlight-color: transparent;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.mb-tier-tab.active {
  background: linear-gradient(135deg, #FF4D6F 0%, #FF2442 100%);
  color: #fff;
  box-shadow: 0 4px 14px rgba(255, 36, 66, 0.25);
}

.mb-tier-tab.current {
  padding-right: 50px;
}

.mb-tier-current {
  position: absolute;
  right: 6px;
  top: 50%;
  transform: translateY(-50%);
  padding: 2px 7px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.25);
  color: #fff;
  font-size: 10px;
  font-weight: 600;
}

/* VIP 卡片 */
.mb-vip-card {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 22px 20px;
  border-radius: 18px;
  background: #fff;
  color: #1a1a1a;
  box-shadow: 0 4px 16px rgba(255, 36, 66, 0.1), 0 2px 8px rgba(0, 0, 0, 0.04);
  overflow: hidden;
}

.mb-vip-card::before {
  content: '';
  position: absolute;
  top: -30px;
  right: -30px;
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 36, 66, 0.1) 0%, transparent 70%);
  pointer-events: none;
}

.mb-vip-card-main {
  position: relative;
  z-index: 1;
}

.mb-vip-label {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
}

.mb-vip-badge {
  height: 38px;
  width: auto;
  display: block;
}

.mb-vip-name {
  font-size: 14px;
  font-weight: 700;
  color: #595959;
}

.mb-vip-count {
  font-size: 24px;
  font-weight: 800;
  margin-bottom: 6px;
  line-height: 1.2;
  color: #1a1a1a;
}

.mb-vip-desc {
  font-size: 13px;
  color: #8c8c8c;
  line-height: 1.4;
}

.mb-vip-expiry {
  margin-top: 8px;
  font-size: 12px;
  color: #8c8c8c;
  line-height: 1.4;
}

.mb-vip-btn {
  position: relative;
  z-index: 1;
  flex-shrink: 0;
  padding: 10px 20px;
  border: none;
  border-radius: 999px;
  background: linear-gradient(135deg, #FF4D6F 0%, #FF2442 100%);
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(255, 36, 66, 0.3);
  -webkit-tap-highlight-color: transparent;
}

.mb-vip-btn:active {
  transform: scale(0.98);
}

/* 快捷权益 */
.mb-quick-benefits {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  margin-top: 20px;
}

.mb-quick-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  text-align: center;
}

.mb-quick-icon {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: #fff5f7;
  color: #FF2442;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  border: 1px solid #ffe5eb;
}

.mb-quick-label {
  font-size: 12px;
  color: #1a1a1a;
  line-height: 1.2;
}

.mb-quick-value {
  font-size: 11px;
  color: #8c8c8c;
}

.mb-rules-link {
  position: relative;
  z-index: 1;
  text-align: center;
  margin-top: 16px;
  font-size: 13px;
  color: #FF2442;
  text-decoration: underline;
  text-underline-offset: 3px;
  cursor: pointer;
}

/* 权益对比 */
.mb-compare {
  margin: 12px;
  padding: 18px 14px 20px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.mb-compare-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 14px;
}

.mb-compare-title {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: #1a1a1a;
}

.mb-compare-sub {
  font-size: 12px;
  color: #8c8c8c;
}

.mb-compare-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 14px;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: none;
}

.mb-compare-tabs::-webkit-scrollbar {
  display: none;
}

.mb-compare-tab {
  flex-shrink: 0;
  padding: 7px 14px;
  border: none;
  border-radius: 999px;
  background: #f5f5f5;
  color: #595959;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  -webkit-tap-highlight-color: transparent;
}

.mb-compare-tab.active {
  background: #FF2442;
  color: #fff;
}

.mb-compare-list {
  display: flex;
  flex-direction: column;
}

.mb-compare-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 0;
  border-bottom: 1px solid #f5f5f5;
}

.mb-compare-row.header {
  padding: 8px 0 10px;
  border-bottom: 1px solid #ececec;
}

.mb-compare-row.header .mb-compare-feature-name {
  color: #8c8c8c;
  font-size: 13px;
}

.mb-compare-row:last-child {
  border-bottom: none;
}

.mb-compare-feature {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.mb-compare-feature-name {
  font-size: 14px;
  color: #1a1a1a;
  font-weight: 500;
}

.mb-compare-feature-tag {
  width: fit-content;
  padding: 2px 6px;
  border-radius: 4px;
  background: #fff5f7;
  color: #ff2442;
  font-size: 10px;
  font-weight: 600;
}

.mb-compare-values {
  display: flex;
  gap: 16px;
  text-align: right;
}

.mb-compare-plan {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 52px;
  line-height: 40px;
}

.mb-compare-plan-name {
  font-size: 11px;
  color: #8c8c8c;
}

.mb-compare-plan-badge {
  height: 38px;
  width: auto;
  display: block;
}

.mb-compare-plan-value {
  font-size: 14px;
  font-weight: 600;
}

.mb-compare-plan-value.yes {
  color: #FF2442;
}

body[data-theme="dark"] .mb-compare-plan-value.yes {
  color: #ff4d6f;
}

.mb-compare-plan-value.no {
  color: #bfbfbf;
}

/* 底部操作栏 */
.mb-footer-space {
  height: 12px;
}

.mb-footer-action {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 50;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 10px 16px calc(10px + env(safe-area-inset-bottom));
  background: #fff;
  border-top: 1px solid #f0f0f0;
  box-shadow: 0 -2px 12px rgba(0, 0, 0, 0.06);
}

.mb-footer-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.mb-footer-price {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.mb-footer-current {
  font-size: 24px;
  font-weight: 800;
  color: #ff2442;
  line-height: 1;
}

.mb-footer-original {
  font-size: 13px;
  color: #bfbfbf;
  text-decoration: line-through;
}

.mb-footer-period {
  font-size: 13px;
  color: #595959;
}

.mb-footer-cycle {
  display: flex;
  gap: 6px;
}

.mb-footer-cycle-btn {
  padding: 3px 8px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  background: #fff;
  color: #595959;
  font-size: 11px;
  cursor: pointer;
  transition: all 0.2s;
  -webkit-tap-highlight-color: transparent;
}

.mb-footer-cycle-btn.active {
  border-color: #ff2442;
  background: #fff5f7;
  color: #ff2442;
  font-weight: 600;
}

.mb-footer-cycle-btn.disabled {
  color: #bfbfbf;
  border-color: #f0f0f0;
  cursor: not-allowed;
}

.mb-footer-btn {
  flex-shrink: 0;
  padding: 12px 26px;
  border: none;
  border-radius: 999px;
  background: linear-gradient(135deg, #ff4d6f 0%, #ff2442 100%);
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 4px 14px rgba(255, 36, 66, 0.3);
  -webkit-tap-highlight-color: transparent;
}

.mb-footer-btn:active {
  transform: scale(0.98);
}

.mb-footer-btn.disabled {
  background: #f5f5f5;
  color: #8c8c8c;
  box-shadow: none;
  cursor: not-allowed;
}

/* 移动端弹框 */
.mb-upgrade-panel,
.mb-pay-panel {
  padding: 8px 0 16px;
}

.mb-upgrade-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px dashed #f0f0f0;
  font-size: 14px;
}

.mb-upgrade-row:last-child {
  border-bottom: none;
}

.mb-upgrade-row.total {
  padding-top: 14px;
  margin-top: 4px;
  border-top: 2px solid #f0f0f0;
  font-size: 15px;
  font-weight: 600;
}

.mb-upgrade-label {
  color: #595959;
}

.mb-upgrade-value {
  color: #1a1a1a;
  font-weight: 500;
}

.mb-upgrade-value.credit {
  color: #ff2442;
}

.mb-upgrade-value.final {
  color: #ff2442;
  font-size: 18px;
  font-weight: 700;
}

.mb-upgrade-tip {
  margin-top: 14px;
  padding: 10px;
  background: #fff5f7;
  border-radius: 8px;
  color: #595959;
  font-size: 12px;
  line-height: 1.6;
}

.mb-pay-tip {
  color: #595959;
  font-size: 14px;
  margin-bottom: 16px;
  line-height: 1.6;
}

.mb-pay-tip strong {
  color: #ff2442;
}

.mb-pay-actions {
  margin-top: 16px;
}

.mb-pay-actions.dual {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.mb-pay-actions.dual .ant-btn {
  width: auto;
  min-width: 110px;
  flex: 0 0 auto;
}

.mb-pay-agreement-confirm {
  text-align: center;
  padding: 8px 0 4px;
}

.mb-pay-agreement-confirm-body {
  display: inline-block;
  text-align: center;
}

.mb-pay-agreement-confirm-body .paid-agreement-text {
  font-size: 16px;
  color: #595959;
  line-height: 1.6;
}

.mb-pay-agreement-confirm-body .paid-agreement-link {
  white-space: nowrap;
}

.mb-qr-pay {
  text-align: center;
}

.mb-qr-pay-amount {
  margin-bottom: 12px;
}

.mb-qr-pay-amount-label {
  display: block;
  font-size: 13px;
  color: #595959;
  margin-bottom: 2px;
}

.mb-qr-pay-amount-value {
  display: block;
  font-size: 22px;
  font-weight: 700;
  color: #1a1a1a;
}

.mb-qr-code-wrap {
  position: relative;
  width: 150px;
  height: 150px;
  margin: 0 auto 10px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 6px;
  background: #fff;
}

.mb-qr-code-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.mb-qr-code-logo {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  width: 34px;
  height: 34px;
  object-fit: contain;
  border-radius: 6px;
  background: #fff;
  padding: 2px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.mb-qr-code-wrap.expired .mb-qr-code-img {
  filter: blur(4px);
  opacity: 0.4;
}

.mb-qr-code-mask {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.72);
  border-radius: 8px;
  cursor: pointer;
}

.mb-qr-code-refresh {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: #FF2442;
  font-size: 13px;
  font-weight: 600;
}

.mb-qr-code-refresh svg {
  width: 32px;
  height: 32px;
}

.mb-qr-code-tip {
  color: #8c8c8c;
  font-size: 12px;
  margin: 0 0 12px;
}

.mb-qr-code-countdown {
  display: block;
  color: #FF2442;
}

.mb-qr-code-tip--refresh {
  color: #FF2442;
  cursor: pointer;
  text-decoration: underline;
}

.mb-qr-code-tip--refresh:hover {
  color: #E61E3A;
}

.mb-qr-pay-terms {
  margin: 0 0 10px;
  padding-left: 16px;
  color: #595959;
  font-size: 11px;
  line-height: 1.55;
  text-align: left;
}

.mb-qr-pay-terms li {
  margin-bottom: 4px;
}

.mb-qr-pay-agreement {
  text-align: left;
  padding-top: 8px;
  border-top: 1px solid #f0f0f0;
}

@media (max-width: 768px) {
  .benefits-index {
    padding: 16px 12px;
  }

  .benefits-header {
    flex-wrap: wrap;
    gap: 12px;
  }

  .benefits-list {
    grid-template-columns: 1fr;
  }
}

/* ================= 暗色主题 ================= */
body[data-theme="dark"] .benefits-title,
body[data-theme="dark"] .benefits-item-name,
body[data-theme="dark"] .benefits-plan-name {
  color: #f0f0f0;
}

body[data-theme="dark"] .benefits-subtitle,
body[data-theme="dark"] .benefits-plan-expiry {
  color: #a6a6a6;
}

body[data-theme="dark"] .benefits-plan-card,
body[data-theme="dark"] .benefits-item,
body[data-theme="dark"] .benefits-empty,
body[data-theme="dark"] .benefits-loading {
  background: #1f1f1f;
  border-color: #303030;
}

body[data-theme="dark"] .benefits-plan-label {
  background: #2a2a2a;
  color: #a6a6a6;
}

body[data-theme="dark"] .benefits-plan-name.plan-basic {
  color: #ffd666;
}

body[data-theme="dark"] .benefits-plan-name.plan-pro {
  background: linear-gradient(30deg, #dfb738 0%, #fb8301 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  color: #fb8301;
}

body[data-theme="dark"] .benefits-plan-name.plan-flagship {
  background: linear-gradient(135deg, #a05013 0%, #db3708 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  color: #db3708;
}

body[data-theme="dark"] .benefits-plan-badge {
  background: #2a2a2a;
  color: #888;
  border-color: transparent;
}

body[data-theme="dark"] .benefits-plan-badge.plan-basic {
  background: linear-gradient(135deg, #5c4a10 0%, #8c6b00 100%);
  color: #fffbe6;
}

body[data-theme="dark"] .benefits-plan-badge.plan-pro {
  background: linear-gradient(30deg, #dfb738 0%, #fb8301 100%);
  color: #fff;
}

body[data-theme="dark"] .benefits-plan-badge.plan-flagship {
  background: linear-gradient(135deg, #a05013 0%, #db3708 100%);
  color: #fff;
  border: none;
}

body[data-theme="dark"] .benefits-item:hover {
  border-color: #52222b;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.3);
}

body[data-theme="dark"] .benefits-item.disabled {
  background: #1a1a1a;
}

body[data-theme="dark"] .benefits-item.disabled:hover {
  border-color: #303030;
  box-shadow: none;
}

body[data-theme="dark"] .benefits-item.disabled .benefits-item-icon {
  background: #2a2a2a;
  color: #666;
}

body[data-theme="dark"] .benefits-item.disabled .benefits-item-name {
  color: #666;
}

body[data-theme="dark"] .benefits-item-icon {
  background: rgba(255, 77, 111, 0.12);
  color: #ff4d6f;
}

body[data-theme="dark"] .benefits-item-tag.required {
  background: #2a1a1a;
  color: #ff7875;
}

body[data-theme="dark"] .benefits-item-tag.required.basic {
  background: linear-gradient(135deg, #5c4a10 0%, #8c6b00 100%);
  color: #fffbe6;
}

body[data-theme="dark"] .benefits-item-tag.required.pro {
  background: linear-gradient(30deg, #dfb738 0%, #fb8301 100%);
  color: #fff;
}

body[data-theme="dark"] .benefits-item-tag.required.flagship {
  background: linear-gradient(135deg, #a05013 0%, #db3708 100%);
  color: #fff;
  border: none;
}

body[data-theme="dark"] .benefits-item-tag.enabled,
body[data-theme="dark"] .benefits-item-tag.tier,
body[data-theme="dark"] .benefits-item-tag.quota {
  background: #52c41a;
  color: #fff;
}

body[data-theme="dark"] .benefits-quota-used,
body[data-theme="dark"] .benefits-empty,
body[data-theme="dark"] .benefits-loading {
  color: #a6a6a6;
}

body[data-theme="dark"] .benefits-progress {
  background: #303030;
}

/* 移动端暗色 */
body[data-theme="dark"] .mobile-benefits {
  background: #141414;
}

body[data-theme="dark"] .mb-header {
  background: #1f1f1f;
  border-bottom-color: #2a2a2a;
}

body[data-theme="dark"] .mb-header-title,
body[data-theme="dark"] .mb-header-back {
  color: #e0e0e0;
}

body[data-theme="dark"] .mb-hero {
  background: linear-gradient(180deg, #2a0d12 0%, #141414 100%);
}

body[data-theme="dark"] .mb-compare {
  background: #1f1f1f;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.2);
}

body[data-theme="dark"] .mb-tier-tab {
  background: #2a2a2a;
  color: #a6a6a6;
  box-shadow: none;
}

body[data-theme="dark"] .mb-tier-tab.active {
  background: linear-gradient(135deg, #FF6B8A 0%, #FF2442 100%);
  color: #fff;
}

body[data-theme="dark"] .mb-vip-card {
  background: #1f1f1f;
  box-shadow: 0 4px 16px rgba(255, 36, 66, 0.15), 0 2px 8px rgba(0, 0, 0, 0.2);
}

body[data-theme="dark"] .mb-vip-card::before {
  background: radial-gradient(circle, rgba(255, 36, 66, 0.12) 0%, transparent 70%);
}

body[data-theme="dark"] .mb-vip-count {
  color: #f0f0f0;
}

body[data-theme="dark"] .mb-vip-name,
body[data-theme="dark"] .mb-vip-desc,
body[data-theme="dark"] .mb-vip-expiry {
  color: #a6a6a6;
}

body[data-theme="dark"] .mb-quick-icon {
  background: rgba(255, 36, 66, 0.12);
  color: #ff4d6f;
  border-color: rgba(255, 36, 66, 0.2);
}

body[data-theme="dark"] .mb-quick-label {
  color: #e0e0e0;
}

body[data-theme="dark"] .mb-quick-value {
  color: #8c8c8c;
}

body[data-theme="dark"] .mb-rules-link {
  color: #ff4d6f;
}

body[data-theme="dark"] .mb-compare-title,
body[data-theme="dark"] .mb-compare-feature-name {
  color: #f0f0f0;
}

body[data-theme="dark"] .mb-compare-sub,
body[data-theme="dark"] .mb-compare-plan-name {
  color: #8c8c8c;
}

body[data-theme="dark"] .mb-compare-tab {
  background: #2a2a2a;
  color: #a6a6a6;
}

body[data-theme="dark"] .mb-compare-tab.active {
  background: #ff4d6f;
  color: #fff;
}

body[data-theme="dark"] .mb-compare-row {
  border-bottom-color: #2a2a2a;
}

body[data-theme="dark"] .mb-compare-feature-tag {
  background: rgba(255, 36, 66, 0.12);
  color: #ff4d6f;
}

body[data-theme="dark"] .mb-footer-action {
  background: #1f1f1f;
  border-top-color: #2a2a2a;
}

body[data-theme="dark"] .mb-footer-current {
  color: #ff4d6f;
}

body[data-theme="dark"] .mb-footer-cycle-btn {
  background: #1f1f1f;
  border-color: #434343;
  color: #a6a6a6;
}

body[data-theme="dark"] .mb-footer-cycle-btn.active {
  border-color: #ff4d6f;
  background: rgba(255, 36, 66, 0.12);
  color: #ff4d6f;
}

body[data-theme="dark"] .mb-footer-cycle-btn.disabled {
  border-color: #2a2a2a;
  color: #666;
}

body[data-theme="dark"] .mb-footer-btn:not(.disabled) {
  background: linear-gradient(135deg, #FF6B8A 0%, #FF2442 100%);
}

body[data-theme="dark"] .mb-footer-btn.disabled {
  background: #2a2a2a;
  color: #666;
}

body[data-theme="dark"] .mb-upgrade-row {
  border-bottom-color: #303030;
}

body[data-theme="dark"] .mb-upgrade-row.total {
  border-top-color: #303030;
}

body[data-theme="dark"] .mb-upgrade-label,
body[data-theme="dark"] .mb-pay-tip {
  color: #a6a6a6;
}

body[data-theme="dark"] .mb-upgrade-value {
  color: #e0e0e0;
}

body[data-theme="dark"] .mb-upgrade-tip {
  background: rgba(255, 36, 66, 0.12);
  color: #a6a6a6;
}

body[data-theme="dark"] .mb-pay-agreement-confirm-body .paid-agreement-text {
  color: #a6a6a6;
}

body[data-theme="dark"] .mb-qr-pay-amount-label {
  color: #a6a6a6;
}

body[data-theme="dark"] .mb-qr-pay-amount-value {
  color: #e0e0e0;
}

body[data-theme="dark"] .mb-qr-code-wrap {
  background: #fff;
  border-color: #2a2a2a;
}

body[data-theme="dark"] .mb-qr-code-mask {
  background: rgba(20, 20, 20, 0.72);
}

body[data-theme="dark"] .mb-qr-code-tip {
  color: #a6a6a6;
}

body[data-theme="dark"] .mb-qr-pay-terms {
  color: #a6a6a6;
}

body[data-theme="dark"] .mb-qr-pay-agreement {
  border-top-color: #2a2a2a;
}
</style>
