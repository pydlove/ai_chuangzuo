<template>
  <div class="style-market-index">
    <div class="style-market-header">
      <h2 class="style-market-title">风格市场</h2>
      <p class="style-market-subtitle">
        发现优质写作风格，支持原创创作者
        <span class="style-market-rules-link" @click="rulesVisible = true">收益规则</span>
      </p>
    </div>

    <div class="style-market-search-bar">
      <input
        v-model="searchQuery"
        type="text"
        class="style-market-search-input"
        placeholder="搜索风格名或适用范围"
      />
    </div>

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
        <div v-if="s.scope" class="style-market-card-scope-list">
          <span v-for="tag in parseScopeTags(s.scope)" :key="tag" class="style-market-card-scope">{{ tag }}</span>
        </div>
        <div class="style-market-card-prompt">{{ promptSummary(s.prompt) }}</div>
        <div v-show="expandedIds.has(s.id)" class="style-market-prompt-full">{{ s.prompt }}</div>
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
            :class="['style-market-favorite-btn', { active: isFavorite(s.id) }]"
            :title="isFavorite(s.id) ? '已收藏' : '收藏'"
            @click="toggleFavorite(s.id)"
          >
            {{ isFavorite(s.id) ? '♥' : '♡' }}
          </button>
          <button
            class="style-market-simulate-btn"
            @click="togglePrompt(s.id)"
          >
            {{ expandedIds.has(s.id) ? '收起' : '查看' }}
          </button>
          <button
            v-if="s.creatorId === currentUserId"
            class="style-market-simulate-btn"
            @click="handleSimulate(s)"
          >
            模拟
          </button>
        </div>
      </div>
    </div>
  </div>

  <a-modal
    class="rules-modal"
    :open="rulesVisible"
    title="风格市场收益规则"
    :footer="null"
    :width="560"
    centered
    @cancel="rulesVisible = false"
  >
    <ol class="style-market-rules-list">
      <li>他人每使用一次你分享的风格，你将获得 <span class="style-market-rule-highlight">0.2 创作币</span> 奖励。</li>
      <li>每周根据风格被使用次数发放里程碑奖励：<span class="style-market-rule-highlight">50 次 5 币</span>、<span class="style-market-rule-highlight">200 次 15 币</span>、<span class="style-market-rule-highlight">500 次 30 币</span>、<span class="style-market-rule-highlight">1000 次 60 币</span>。</li>
      <li>里程碑奖励 <span class="style-market-rule-highlight">每周结算一次</span>，结算后当周使用次数清零并重新累计。</li>
      <li>使用他人分享的风格 <span class="style-market-rule-highlight">无需支付创作币</span>，创作者仍可正常获得收益。</li>
      <li>如发现违规刷量行为，平台有权 <span class="style-market-rule-highlight">取消相关收益并下架风格</span>。</li>
    </ol>
    <div class="style-market-rules-footer">* 活动最终解释权归平台所有。</div>
  </a-modal>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  marketStyles,
  useMarketStyle,
  simulateExternalUse,
  toggleFavorite,
  isFavorite,
  loadMarketStyles
} from '@/composables/useStyleMarket.js'

const router = useRouter()
const searchQuery = ref('')
const activeTab = ref('all')
const currentUserId = ref(localStorage.getItem('aichuangzuo_user_id') || '')
const expandedIds = ref(new Set())
const rulesVisible = ref(false)

const parseScopeTags = (scopeStr) => {
  if (!scopeStr) return []
  return scopeStr.split(/[,，]/).map(t => t.trim()).filter(Boolean)
}

const tabOptions = [
  { key: 'all', label: '全部' },
  { key: 'week-hot', label: '本周最热' },
  { key: 'all-hot', label: '历史最热' },
  { key: 'new', label: '最新' },
  { key: 'featured', label: '精选' }
]

const FEATURED_USES_THRESHOLD = 5

const approvedStyles = computed(() =>
  marketStyles.value.filter(s => s.status === 'approved')
)

const filteredStyles = computed(() => {
  let list = approvedStyles.value
  if (activeTab.value === 'week-hot') {
    list = [...list].sort((a, b) => b.weeklyUses - a.weeklyUses)
  } else if (activeTab.value === 'all-hot') {
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

const togglePrompt = (id) => {
  const set = new Set(expandedIds.value)
  if (set.has(id)) {
    set.delete(id)
  } else {
    set.add(id)
  }
  expandedIds.value = set
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

onMounted(() => {
  loadMarketStyles()
})
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
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.style-market-rules-link {
  color: #ff2442;
  cursor: pointer;
  font-weight: 500;
  text-decoration: underline;
  text-underline-offset: 3px;
}

.style-market-rules-link:hover {
  color: #e61e3a;
}

.style-market-rules-list {
  margin: 0;
  padding-left: 20px;
  font-size: 14px;
  color: #595959;
  line-height: 1.8;
}

.style-market-rules-list li {
  margin-bottom: 10px;
}

.style-market-rule-highlight {
  color: #ff2442;
  font-weight: 500;
  text-decoration: underline;
  text-underline-offset: 3px;
}

.style-market-rules-footer {
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid #f0f0f0;
  font-size: 13px;
  color: #8c8c8c;
}

body[data-theme="dark"] .style-market-title {
  color: #f0f0f0;
}

body[data-theme="dark"] .style-market-subtitle {
  color: #a6a6a6;
}

body[data-theme="dark"] .style-market-rules-list {
  color: #a6a6a6;
}

body[data-theme="dark"] .style-market-rules-footer {
  border-top-color: #303030;
  color: #8c8c8c;
}

.style-market-search-bar {
  margin-bottom: 12px;
}

.style-market-tabs {
  display: flex;
  align-items: center;
  gap: 4px;
  background: #f5f5f5;
  padding: 4px;
  border-radius: 8px;
  height: 44px;
  width: fit-content;
  margin-bottom: 20px;
}

.style-market-tab {
  padding: 8px 16px;
  border: none;
  background: transparent;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  line-height: 1;
  color: #595959;
  cursor: pointer;
  transition: all 0.2s;
}

.style-market-tab.active {
  background: #fff;
  color: #1a1a1a;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
}

body[data-theme="dark"] .style-market-tabs {
  background: #141414;
}

body[data-theme="dark"] .style-market-tab {
  background-color: transparent !important;
  color: #a6a6a6;
}

body[data-theme="dark"] .style-market-tab:hover {
  color: #f0f0f0;
}

body[data-theme="dark"] .style-market-tab.active {
  background-color: #2a2a2a !important;
  color: #f0f0f0;
  box-shadow: none;
}

.style-market-search {
  display: flex;
  align-items: center;
}

.style-market-search-input {
  width: 100%;
  min-width: 320px;
  max-width: 520px;
  height: 44px;
  padding: 0 12px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 14px;
  box-sizing: border-box;
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

@media (max-width: 768px) {
  .style-market-index {
    padding: 16px 12px;
  }

  /* Tabs：横滑 + 隐藏滚动条 */
  .style-market-tabs {
    flex-wrap: nowrap;
    overflow-x: auto;
    scrollbar-width: none;
    max-width: 100%;
    -webkit-mask-image: linear-gradient(to right, #000 0, #000 calc(100% - 16px), transparent 100%);
    mask-image: linear-gradient(to right, #000 0, #000 calc(100% - 16px), transparent 100%);
  }
  .style-market-tabs::-webkit-scrollbar { display: none; }
  .style-market-tab { flex-shrink: 0; padding: 8px 14px; font-size: 13px; }

  /* 搜索框：单独一行 */
  .style-market-search {
    flex: 1;
    min-width: 0;
    width: 100%;
  }
  .style-market-search-input {
    min-width: 0;
    max-width: 100%;
  }

  /* 卡片网格：单列 */
  .style-market-grid {
    grid-template-columns: minmax(0, 1fr);
    gap: 16px;
  }
  .style-market-card {
    padding: 18px 16px;
    border-radius: 14px;
  }

  /* 卡片底部按钮：移动到自动换行 + 适应宽度 */
  .style-market-card-actions {
    flex-wrap: wrap;
  }
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
}

.style-market-card-scope-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
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

.style-market-prompt-full {
  font-size: 14px;
  color: #595959;
  line-height: 1.7;
  background: #fafafa;
  border-radius: 12px;
  padding: 14px 16px;
  margin-bottom: 16px;
  white-space: pre-line;
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

.style-market-favorite-btn {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 10px;
  font-size: 20px;
  color: #8c8c8c;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
}

.style-market-favorite-btn:hover {
  border-color: #ff2442;
  color: #ff2442;
  background: #fff0f2;
}

.style-market-favorite-btn.active {
  background: #fff0f2;
  color: #ff2442;
  border-color: #ff2442;
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

/* 暗色主题 */
body[data-theme="dark"] .style-market-search-input {
  background: #1f1f1f;
  border-color: #303030;
  color: #f0f0f0;
}

body[data-theme="dark"] .style-market-search-input::placeholder {
  color: #737373;
}

body[data-theme="dark"] .style-market-search-input:focus {
  border-color: var(--color-primary);
}

body[data-theme="dark"] .style-market-empty {
  background-color: transparent !important;
  color: #a6a6a6;
}

body[data-theme="dark"] .style-market-card {
  box-shadow: none;
}

body[data-theme="dark"] .style-market-card-title {
  color: #f0f0f0;
}

body[data-theme="dark"] .style-market-card-creator,
body[data-theme="dark"] .style-market-card-prompt,
body[data-theme="dark"] .style-market-card-stats {
  color: #a6a6a6;
}

body[data-theme="dark"] .style-market-prompt-full {
  background: #141414;
  color: #a6a6a6;
}

body[data-theme="dark"] .style-market-card-scope {
  background: rgba(255, 36, 66, 0.12);
  border-color: rgba(255, 36, 66, 0.35);
  color: #ff6b81;
}

body[data-theme="dark"] .style-market-favorite-btn,
body[data-theme="dark"] .style-market-simulate-btn {
  background: #2a2a2a;
  border-color: #434343;
  color: #a6a6a6;
}

body[data-theme="dark"] .style-market-favorite-btn:hover,
body[data-theme="dark"] .style-market-simulate-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: rgba(255, 36, 66, 0.12);
}

body[data-theme="dark"] .style-market-favorite-btn.active {
  background: rgba(255, 36, 66, 0.15);
  border-color: var(--color-primary);
  color: #ff6b81;
}
</style>

<style>
/* 暗色主题 - 收益规则弹层外壳适配（全局，非 scoped） */
body[data-theme="dark"] .rules-modal .ant-modal-content {
  background: #141414;
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.6);
}

body[data-theme="dark"] .rules-modal .ant-modal-header {
  background: #141414;
  border-bottom-color: #303030;
}

body[data-theme="dark"] .rules-modal .ant-modal-title {
  color: #e0e0e0;
}

body[data-theme="dark"] .rules-modal .ant-modal-close {
  color: #a6a6a6;
}

body[data-theme="dark"] .rules-modal .ant-modal-close:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.08);
}
</style>
