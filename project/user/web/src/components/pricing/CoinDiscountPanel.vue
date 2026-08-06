<template>
  <div class="coin-discount-panel">
    <div class="coin-discount-header">
      <span class="coin-discount-title">创作币抵扣</span>
      <span class="coin-discount-balance">可用 {{ coinBalance.toFixed(0) }} 创作币</span>
    </div>
    <div class="coin-discount-row">
      <span class="coin-discount-label">最多可抵 {{ maxCoinAmount }} 创作币</span>
      <span class="coin-discount-value">-¥{{ coinDiscountYuan.toFixed(2) }}</span>
    </div>
    <div class="coin-discount-slider-wrap">
      <a-slider
        :min="0"
        :max="maxCoinAmount"
        :step="1"
        :value="selectedCoinAmount"
        @change="onSliderChange"
      />
      <a-input-number
        :min="0"
        :max="maxCoinAmount"
        :precision="0"
        :value="selectedCoinAmount"
        @change="onInputChange"
        class="coin-discount-input"
      />
    </div>
    <div class="coin-discount-final">
      <span>实付现金</span>
      <span class="coin-discount-final-value">¥{{ finalCash.toFixed(2) }}</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  coinBalance: { type: Number, default: 0 },
  maxCoinAmount: { type: Number, default: 0 },
  selectedCoinAmount: { type: Number, default: 0 },
  coinToYuanRatio: { type: Number, default: 10 },
  finalCash: { type: Number, default: 0 }
})

const emit = defineEmits(['update:selectedCoinAmount'])

const coinDiscountYuan = computed(() => props.selectedCoinAmount / props.coinToYuanRatio)

function onSliderChange(value) {
  emit('update:selectedCoinAmount', Math.max(0, Math.min(value, props.maxCoinAmount)))
}

function onInputChange(value) {
  const n = Number.isFinite(value) ? value : 0
  emit('update:selectedCoinAmount', Math.max(0, Math.min(Math.floor(n), props.maxCoinAmount)))
}
</script>

<style scoped>
.coin-discount-panel {
  background: #fff5f7;
  border-radius: 10px;
  padding: 14px 16px;
  margin-bottom: 16px;
}

.coin-discount-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.coin-discount-title {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
}

.coin-discount-balance {
  font-size: 13px;
  color: #8c8c8c;
}

.coin-discount-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  font-size: 13px;
}

.coin-discount-label {
  color: #595959;
}

.coin-discount-value {
  color: #ff2442;
  font-weight: 600;
}

.coin-discount-slider-wrap {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.coin-discount-slider-wrap :deep(.ant-slider) {
  flex: 1;
}

.coin-discount-input {
  width: 80px;
}

.coin-discount-final {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 10px;
  border-top: 1px dashed #ffd6dd;
  font-size: 14px;
  color: #595959;
}

.coin-discount-final-value {
  color: #ff2442;
  font-size: 18px;
  font-weight: 700;
}

body[data-theme="dark"] .coin-discount-panel {
  background: rgba(255, 36, 66, 0.12);
}

body[data-theme="dark"] .coin-discount-title,
body[data-theme="dark"] .coin-discount-label,
body[data-theme="dark"] .coin-discount-final {
  color: #a6a6a6;
}

body[data-theme="dark"] .coin-discount-balance {
  color: #8c8c8c;
}

body[data-theme="dark"] .coin-discount-final-value,
body[data-theme="dark"] .coin-discount-value {
  color: #ff4d6f;
}

body[data-theme="dark"] .coin-discount-final {
  border-top-color: rgba(255, 77, 111, 0.25);
}
</style>
