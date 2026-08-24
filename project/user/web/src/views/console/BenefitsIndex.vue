<template>
  <div class="benefits-index">
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
      <h3 class="benefits-section-title">权益明细</h3>
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
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useBenefits } from '@/composables/useBenefits.js'
import { getPlanCatalog } from '@/api/membership.js'
import {
  CrownOutlined,
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
const { benefits, planKey, planName, expiresAt, loadBenefits } = useBenefits()

const catalog = ref(null)
const catalogLoading = ref(false)

const PLAN_ORDER = ['basic', 'pro', 'flagship']
const PLAN_NAMES = {
  basic: '基础版',
  pro: '专业版',
  flagship: '旗舰版'
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
  const rows = catalog.value?.compareRows || []
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

onMounted(() => {
  loadBenefits()
  catalogLoading.value = true
  getPlanCatalog()
    .then((res) => {
      catalog.value = res.data || res
    })
    .catch(() => {
      message.error('权益目录加载失败')
    })
    .finally(() => {
      catalogLoading.value = false
    })
})
</script>

<style scoped>
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

.benefits-section-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0;
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

/* 暗色主题 */
body[data-theme="dark"] .benefits-title,
body[data-theme="dark"] .benefits-section-title,
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
</style>
