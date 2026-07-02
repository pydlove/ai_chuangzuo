<template>
  <div class="style-market-index">
    <div class="style-market-header">
      <h2 class="style-market-title">风格市场</h2>
      <p class="style-market-subtitle">发现优质写作风格，支持原创创作者</p>
    </div>

    <div class="style-market-filter-bar">
      <div class="style-market-tabs">
        <button
          v-for="tab in tabOptions"
          :key="tab.key"
          :class="['style-market-tab', { active: activeTab === tab.key }]"
          @click="activeTab = tab.key"
        >
          {{ tab.label }}
        </button>
      </div>

      <div class="style-market-search">
        <input
          v-model="searchQuery"
          type="text"
          class="style-market-search-input"
          placeholder="搜索风格名或适用范围"
        />
      </div>
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
        <div class="style-market-card-head">
          <div class="style-market-card-avatar">{{ s.name.charAt(0) }}</div>
          <div class="style-market-card-title-wrap">
            <div class="style-market-card-title">{{ s.name }}</div>
            <div class="style-market-card-creator">by {{ s.creatorName }}</div>
          </div>
        </div>
        <div v-if="s.scope" class="style-market-card-scope">{{ s.scope }}</div>
        <div class="style-market-card-prompt">{{ promptSummary(s.prompt) }}</div>
        <div class="style-market-card-stats">
          <span>🔥 本周 {{ s.weeklyUses }} 次</span>
          <span>累计 {{ s.totalUses }} 次</span>
        </div>
        <div class="style-market-card-actions">
          <button
            class="style-market-use-btn"
            @click="handleUse(s)"
          >
            使用
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
  simulateExternalUse
} from '@/composables/useStyleMarket.js'

const router = useRouter()
const searchQuery = ref('')
const activeTab = ref('all')
const currentUserId = ref(localStorage.getItem('aichuangzuo_user_id') || '')

const tabOptions = [
  { key: 'all', label: '全部' },
  { key: 'hot', label: '最热' },
  { key: 'new', label: '最新' },
  { key: 'featured', label: '精选' }
]

const FEATURED_USES_THRESHOLD = 5

const approvedStyles = computed(() =>
  marketStyles.value.filter(s => s.status === 'approved')
)

const filteredStyles = computed(() => {
  let list = approvedStyles.value
  if (activeTab.value === 'hot') {
    list = [...list].sort((a, b) => b.totalUses - a.totalUses)
  } else if (activeTab.value === 'new') {
    list = [...list].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
  } else if (activeTab.value === 'featured') {
    list = list.filter(s => s.totalUses >= FEATURED_USES_THRESHOLD)
      .sort((a, b) => b.totalUses - a.totalUses)
  }

  const q = searchQuery.value.trim().toLowerCase()
  if (!q) return list
  return list.filter(
    s =>
      s.name.toLowerCase().includes(q) ||
      (s.scope && s.scope.toLowerCase().includes(q))
  )
})

const promptSummary = (prompt) => {
  if (!prompt) return ''
  return prompt.length > 60 ? prompt.slice(0, 60) + '...' : prompt
}

const handleUse = (s) => {
  try {
    useMarketStyle(s.id)
    router.push(`/console/create?marketStyleId=${s.id}`)
  } catch (err) {
    alert(err.message)
  }
}

const handleSimulate = (s) => {
  try {
    simulateExternalUse(s.id)
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

.style-market-filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.style-market-tabs {
  display: flex;
  gap: 4px;
  background: #f5f5f5;
  padding: 4px;
  border-radius: 8px;
  width: fit-content;
}

.style-market-tab {
  padding: 8px 16px;
  border: none;
  background: transparent;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  color: #595959;
  cursor: pointer;
  transition: all 0.2s;
}

.style-market-tab.active {
  background: #fff;
  color: #1a1a1a;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
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
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 24px;
}

.style-market-card {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.05);
  transition: transform 0.25s ease, box-shadow 0.25s ease;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.style-market-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 16px 36px rgba(255, 36, 66, 0.13);
}

.style-market-card-head {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 16px;
}

.style-market-card-avatar {
  flex-shrink: 0;
  width: 48px;
  height: 48px;
  border-radius: 14px;
  background: linear-gradient(135deg, #ff2442, #ff8a9b);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 600;
}

.style-market-card-title-wrap {
  flex: 1;
  min-width: 0;
}

.style-market-card-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 4px;
  line-height: 1.35;
  word-break: break-all;
}

.style-market-card-creator {
  font-size: 12px;
  color: #8c8c8c;
}

.style-market-card-scope {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  width: fit-content;
  font-size: 13px;
  color: #ff2442;
  background: #fff0f2;
  border: 1px solid #ffd1d9;
  padding: 4px 12px;
  border-radius: 20px;
  margin-bottom: 14px;
}

.style-market-card-scope::before {
  content: '#';
  opacity: 0.7;
}

.style-market-card-prompt {
  font-size: 14px;
  color: #595959;
  line-height: 1.7;
  margin-bottom: 16px;
  flex: 1;
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.style-market-card-stats {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #8c8c8c;
  margin-bottom: 16px;
}

.style-market-card-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.style-market-use-btn {
  flex: 1;
  padding: 10px 18px;
  background: #ff2442;
  color: #fff;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;
}

.style-market-use-btn:hover {
  background: #e61e3a;
}

.style-market-use-btn:disabled {
  background: #d9d9d9;
  cursor: not-allowed;
}

.style-market-simulate-btn {
  padding: 10px 16px;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 10px;
  font-size: 14px;
  color: #595959;
  cursor: pointer;
  transition: all 0.2s;
}

.style-market-simulate-btn:hover {
  border-color: #ff2442;
  color: #ff2442;
  background: #fff0f2;
}
</style>
