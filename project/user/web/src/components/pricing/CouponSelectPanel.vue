<template>
  <div v-if="coupons.length" class="coupon-select-panel">
    <div class="coupon-select-header">
      <span class="coupon-select-title">优惠券</span>
      <span class="coupon-select-count">可用 {{ applicableCount }} 张</span>
    </div>
    <a-select
      class="coupon-select"
      allow-clear
      popup-class-name="coupon-select-popup"
      :value="selectedCode || undefined"
      placeholder="选择优惠券"
      @change="onChange"
    >
      <a-select-option v-for="c in coupons" :key="c.couponCode" :value="c.couponCode" :disabled="!isApplicable(c)">
        <div class="coupon-option">
          <span class="coupon-option__value">{{ couponValueText(c) }}</span>
          <span class="coupon-option__scope">{{ scopeText(c) }}</span>
        </div>
      </a-select-option>
    </a-select>
    <div v-if="discountYuan > 0" class="coupon-select-discount">
      <span>优惠金额</span>
      <span class="coupon-select-discount-value">-¥{{ discountYuan.toFixed(2) }}</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  coupons: { type: Array, default: () => [] },
  selectedCode: { type: String, default: '' },
  planKey: { type: String, default: '' },
  cycle: { type: String, default: '' },
  discountYuan: { type: Number, default: 0 }
})

const emit = defineEmits(['update:selectedCode', 'change'])

const PLAN_NAMES = { basic: '基础版', pro: '专业版', flagship: '旗舰版' }
const CYCLE_NAMES = { month: '月度', quarter: '季度', year: '年度' }

const isApplicable = (coupon) => {
  const planOk = !coupon.applicablePlan || coupon.applicablePlan === 'all' || coupon.applicablePlan === props.planKey
  const cycleOk = !coupon.applicableCycle || coupon.applicableCycle === 'all' || coupon.applicableCycle === props.cycle
  return planOk && cycleOk
}

const applicableCount = computed(() => props.coupons.filter(isApplicable).length)

const couponValueText = (coupon) => {
  if (coupon.couponType === 'percent') {
    const discount = parseFloat((Number(coupon.discountValue) * 10).toFixed(2))
    return `${discount}折`
  }
  return `减¥${parseFloat(Number(coupon.discountValue).toFixed(2))}`
}

const scopeText = (coupon) => {
  const planText = coupon.applicablePlan && coupon.applicablePlan !== 'all' ? (PLAN_NAMES[coupon.applicablePlan] || coupon.applicablePlan) : ''
  const cycleText = coupon.applicableCycle && coupon.applicableCycle !== 'all' ? (CYCLE_NAMES[coupon.applicableCycle] || coupon.applicableCycle) : ''
  if (planText && cycleText) return `仅限${planText}${cycleText}`
  if (planText) return `仅限${planText}`
  if (cycleText) return `仅限${cycleText}`
  return '全场通用'
}

function onChange(value) {
  const code = value || ''
  emit('update:selectedCode', code)
  emit('change', code)
}
</script>

<style scoped>
.coupon-select-panel {
  background: #fff5f7;
  border-radius: 10px;
  padding: 14px 16px;
  margin-bottom: 16px;
}

.coupon-select-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.coupon-select-title {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
}

.coupon-select-count {
  font-size: 13px;
  color: #8c8c8c;
}

.coupon-select {
  width: 100%;
}

.coupon-select:deep(.ant-select:hover .ant-select-selector),
.coupon-select:deep(.ant-select-focused .ant-select-selector) {
  border-color: var(--color-primary, #ff2442) !important;
}

.coupon-select:deep(.ant-select-focused .ant-select-selector) {
  box-shadow: 0 0 0 2px rgba(255, 36, 66, 0.15) !important;
}

/* 下拉弹层挂载在 body 下，用全局样式覆盖选中项主题色 */
:global(.coupon-select-popup .ant-select-item-option-selected:not(.ant-select-item-option-disabled)) {
  background-color: rgba(255, 36, 66, 0.1);
  color: var(--color-primary, #ff2442);
  font-weight: 600;
}

:global(.coupon-select-popup .ant-select-item-option-active:not(.ant-select-item-option-disabled)) {
  background-color: rgba(255, 36, 66, 0.06);
}

.coupon-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.coupon-option__value {
  font-weight: 600;
  color: #ff2442;
}

.coupon-option__scope {
  font-size: 12px;
  color: #8c8c8c;
}

body[data-theme="dark"] .coupon-select-panel {
  background: rgba(255, 36, 66, 0.12);
}

body[data-theme="dark"] .coupon-select-title {
  color: #f5f5f5;
}

body[data-theme="dark"] .coupon-select-count,
body[data-theme="dark"] .coupon-option__scope {
  color: #8c8c8c;
}

body[data-theme="dark"] .coupon-option__value {
  color: #ff4d6f;
}

.coupon-select-discount {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
  font-size: 13px;
  color: #595959;
}

.coupon-select-discount-value {
  color: var(--color-primary, #ff2442);
  font-weight: 600;
}

body[data-theme="dark"] .coupon-select-discount {
  color: #a6a6a6;
}

body[data-theme="dark"] .coupon-select-discount-value {
  color: #ff4d6f;
}
</style>
