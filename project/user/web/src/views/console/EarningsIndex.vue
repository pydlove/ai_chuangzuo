<template>
  <div class="earnings-index">
    <div class="earnings-header">
      <h2 class="earnings-title">收益明细</h2>
      <p class="earnings-subtitle">查看风格市场带来的创作币收益</p>
    </div>

    <div class="earnings-stats">
      <div class="earnings-stat">
        <div class="earnings-stat-value">{{ totalEarnings.toFixed(2) }}</div>
        <div class="earnings-stat-label">累计收益</div>
      </div>
      <div class="earnings-stat">
        <div class="earnings-stat-value">{{ weeklyEarnings.toFixed(2) }}</div>
        <div class="earnings-stat-label">本周收益</div>
      </div>
      <div class="earnings-stat">
        <div class="earnings-stat-value">{{ recordCount }}</div>
        <div class="earnings-stat-label">收益笔数</div>
      </div>
    </div>

    <div v-if="earningsRecords.length === 0" class="earnings-empty">
      暂无收益记录
    </div>
    <div v-else class="earnings-list">
      <div
        v-for="r in earningsRecords"
        :key="r.id"
        class="earnings-item"
      >
        <div class="earnings-item-left">
          <div class="earnings-item-title">{{ r.description }}</div>
          <div class="earnings-item-meta">
            {{ r.styleName }} · {{ formatType(r.type) }} · {{ r.createdAt.slice(0, 16).replace('T', ' ') }}
          </div>
        </div>
        <div class="earnings-item-amount" :class="{ negative: r.amount < 0 }">
          {{ r.amount > 0 ? '+' : '' }}{{ r.amount.toFixed(2) }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import {
  earningsRecords,
  getTotalEarnings,
  getWeeklyEarnings
} from '@/composables/useStyleMarket.js'

const totalEarnings = computed(() => getTotalEarnings())
const weeklyEarnings = computed(() => getWeeklyEarnings())
const recordCount = computed(() => earningsRecords.value.length)

const formatType = (type) => {
  const map = { usage: '使用收益', milestone: '里程碑奖励' }
  return map[type] || type
}
</script>

<style scoped>
.earnings-index {
  height: 100%;
  padding: 24px;
  overflow-y: auto;
}

.earnings-header {
  margin-bottom: 20px;
}

.earnings-title {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 4px;
}

.earnings-subtitle {
  font-size: 13px;
  color: #8c8c8c;
  margin: 0;
}

.earnings-stats {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
}

.earnings-stat {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 16px 24px;
  min-width: 120px;
}

.earnings-stat-value {
  font-size: 22px;
  font-weight: 700;
  color: #ff2442;
}

.earnings-stat-label {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 4px;
}

.earnings-empty {
  padding: 60px 0;
  text-align: center;
  color: #8c8c8c;
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

.earnings-item-amount {
  font-size: 15px;
  font-weight: 600;
  color: #ff2442;
}

.earnings-item-amount.negative {
  color: #ff4d4f;
}
</style>
