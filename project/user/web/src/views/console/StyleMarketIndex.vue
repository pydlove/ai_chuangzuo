<template>
  <div class="style-market-index">
    <div class="style-market-header">
      <h2 class="style-market-title">风格市场</h2>
      <p class="style-market-subtitle">发现优质写作风格，支持原创创作者</p>
    </div>

    <div class="style-market-stats">
      <div class="style-market-stat">
        <div class="style-market-stat-value">{{ approvedStyles.length }}</div>
        <div class="style-market-stat-label">上架风格</div>
      </div>
      <div class="style-market-stat">
        <div class="style-market-stat-value">{{ totalWeeklyUses }}</div>
        <div class="style-market-stat-label">本周使用</div>
      </div>
      <div class="style-market-stat">
        <div class="style-market-stat-value">{{ coinBalance }}</div>
        <div class="style-market-stat-label">我的余额</div>
      </div>
    </div>

    <div class="style-market-search">
      <input
        v-model="searchQuery"
        type="text"
        class="style-market-search-input"
        placeholder="搜索风格名或适用范围"
      />
    </div>

    <div v-if="filteredStyles.length === 0" class="style-market-empty">
      暂无已上架风格
    </div>
    <div v-else class="style-market-grid">
      <div
        v-for="s in filteredStyles"
        :key="s.id"
        class="style-market-card"
      >
        <div class="style-market-card-title">{{ s.name }}</div>
        <div class="style-market-card-creator">by {{ s.creatorName }}</div>
        <div v-if="s.scope" class="style-market-card-scope">适用：{{ s.scope }}</div>
        <div class="style-market-card-prompt">{{ promptSummary(s.prompt) }}</div>
        <div class="style-market-card-stats">
          <span>本周 {{ s.weeklyUses }} 次</span>
          <span>累计 {{ s.totalUses }} 次</span>
        </div>
        <div class="style-market-card-actions">
          <button
            class="style-market-use-btn"
            :disabled="coinBalance < s.price"
            @click="handleUse(s)"
          >
            使用（{{ s.price }} 币）
          </button>
          <button
            v-if="s.creatorId === currentUserId"
            class="style-market-simulate-btn"
            @click="handleSimulate(s)"
          >
            模拟他人使用
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  marketStyles,
  useMarketStyle,
  simulateExternalUse,
  getCoinBalance
} from '@/composables/useStyleMarket.js'

const router = useRouter()
const searchQuery = ref('')
const currentUserId = ref(localStorage.getItem('aichuangzuo_user_id') || '')
const coinBalance = ref(getCoinBalance())

const approvedStyles = computed(() =>
  marketStyles.value.filter(s => s.status === 'approved')
)

const totalWeeklyUses = computed(() =>
  approvedStyles.value.reduce((sum, s) => sum + s.weeklyUses, 0)
)

const filteredStyles = computed(() => {
  const q = searchQuery.value.trim().toLowerCase()
  if (!q) return approvedStyles.value
  return approvedStyles.value.filter(
    s =>
      s.name.toLowerCase().includes(q) ||
      (s.scope && s.scope.toLowerCase().includes(q))
  )
})

const promptSummary = (prompt) => {
  if (!prompt) return ''
  return prompt.length > 60 ? prompt.slice(0, 60) + '...' : prompt
}

const refreshBalance = () => {
  coinBalance.value = getCoinBalance()
}

const handleUse = (s) => {
  try {
    useMarketStyle(s.id)
    refreshBalance()
    router.push(`/console/create?marketStyleId=${s.id}`)
  } catch (err) {
    alert(err.message)
  }
}

const handleSimulate = (s) => {
  try {
    simulateExternalUse(s.id)
    refreshBalance()
  } catch (err) {
    alert(err.message)
  }
}
</script>

<style scoped>
.style-market-index {
  height: 100%;
  padding: 24px;
  overflow-y: auto;
}

.style-market-header {
  margin-bottom: 20px;
}

.style-market-title {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 4px;
}

.style-market-subtitle {
  font-size: 13px;
  color: #8c8c8c;
  margin: 0;
}

.style-market-stats {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
}

.style-market-stat {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 16px 24px;
  min-width: 120px;
}

.style-market-stat-value {
  font-size: 22px;
  font-weight: 700;
  color: #ff2442;
}

.style-market-stat-label {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 4px;
}

.style-market-search {
  margin-bottom: 20px;
}

.style-market-search-input {
  width: 100%;
  max-width: 400px;
  padding: 10px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 14px;
}

.style-market-search-input:focus {
  outline: none;
  border-color: #ff2442;
}

.style-market-empty {
  padding: 60px 0;
  text-align: center;
  color: #8c8c8c;
}

.style-market-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}

.style-market-card {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 16px;
  display: flex;
  flex-direction: column;
}

.style-market-card-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 4px;
}

.style-market-card-creator {
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 8px;
}

.style-market-card-scope {
  font-size: 12px;
  color: #ff2442;
  background: #fff0f2;
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  margin-bottom: 8px;
}

.style-market-card-prompt {
  font-size: 12px;
  color: #595959;
  line-height: 1.5;
  margin-bottom: 12px;
  flex: 1;
  white-space: pre-line;
}

.style-market-card-stats {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 12px;
}

.style-market-card-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.style-market-use-btn {
  flex: 1;
  padding: 8px 16px;
  background: #ff2442;
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
}

.style-market-use-btn:hover {
  background: #e61e3a;
}

.style-market-use-btn:disabled {
  background: #d9d9d9;
  cursor: not-allowed;
}

.style-market-simulate-btn {
  padding: 8px 12px;
  background: #fff;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  font-size: 13px;
  color: #595959;
  cursor: pointer;
}

.style-market-simulate-btn:hover {
  border-color: #ff2442;
  color: #ff2442;
}
</style>
