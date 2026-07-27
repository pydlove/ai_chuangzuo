<template>
  <div class="market-page">
    <!-- ① 平台 Banner 区 -->
    <section class="market-banner">
      <div class="market-banner-text">
        <h1 class="market-banner-title">爱创作 · 风格市场</h1>
        <p class="market-banner-sub">
          官方运营 · 精选创作者风格 · 使用即获收益分成
          <span class="market-banner-rules-link" @click="rulesVisible = true">收益规则</span>
        </p>
      </div>
      <div class="market-banner-stats">
        <div class="market-banner-stat">
          <div class="market-banner-stat-num">{{ marketStats.approvedCount }}</div>
          <div class="market-banner-stat-label">已上架款</div>
        </div>
        <div class="market-banner-stat">
          <div class="market-banner-stat-num">{{ formatUses(marketStats.totalUses) }}</div>
          <div class="market-banner-stat-label">累计使用次</div>
        </div>
        <div class="market-banner-stat">
          <div class="market-banner-stat-num">{{ formatCoins(marketStats.totalEarnings) }}</div>
          <div class="market-banner-stat-label">累计发放币</div>
        </div>
      </div>
    </section>

    <!-- ② 上传激励卡 -->
    <section class="market-upload-card" @click="goUpload">
      <div class="market-upload-icon">＋</div>
      <div class="market-upload-body">
        <div class="market-upload-title">上传你的风格，开始赚创作币</div>
        <div class="market-upload-sub">每被他人使用 1 次即得 0.2 币；周里程碑最高额外 +60</div>
      </div>
      <button class="market-upload-cta" @click.stop="goUpload">立即上架</button>
    </section>

    <!-- ③ 官方精选大卡 -->
    <section class="market-featured">
      <div class="market-section-head">
        <div class="market-section-title-wrap">
          <h2 class="market-section-title">官方精选</h2>
          <span class="market-official-badge">官方</span>
        </div>
        <button class="market-section-link" @click="scrollToGrid">查看全部 →</button>
      </div>
      <div v-if="featuredStyles.length === 0" class="market-featured-empty">
        官方精选即将上线
      </div>
      <div v-else class="market-featured-rail">
        <div
          v-for="s in featuredStyles"
          :key="s.id"
          class="market-featured-card"
          :style="{ background: featuredBackground(s) }"
          @click="handleUse(s)"
        >
          <div class="market-featured-name">{{ s.name }}</div>
          <div v-if="s.scope" class="market-featured-tag"># {{ firstScope(s.scope) }}</div>
          <div class="market-featured-uses">🔥 本周使用 {{ s.weeklyUses }} 次</div>
          <div class="market-featured-creator">
            by {{ s.creatorName || '匿名用户' }} · 累计赚 {{ formatCoins(getMarketStyleEarnings(s.id)) }} 币
          </div>
        </div>
      </div>
    </section>

    <!-- ④ 收益潜力榜 -->
    <section class="market-creators">
      <div class="market-section-head">
        <div class="market-section-title-wrap">
          <h2 class="market-section-title">收益潜力榜</h2>
          <span class="market-section-sub">看看谁在用风格赚到币</span>
        </div>
      </div>
      <div v-if="topCreators.length === 0" class="market-creators-empty">
        暂无上榜创作者
      </div>
      <div v-else class="market-creators-rail">
        <div
          v-for="c in topCreators"
          :key="c.creatorId"
          class="market-creator-card"
        >
          <div class="market-creator-avatar">{{ (c.creatorName || '匿').charAt(0) }}</div>
          <div class="market-creator-name">{{ c.creatorName || '匿名用户' }}</div>
          <div class="market-creator-weekly">本周 +{{ formatCoins(c.weeklyEarnings) }} 币</div>
          <div v-if="c.bestStyle" class="market-creator-best">
            代表风格 · {{ c.bestStyle.name }}
          </div>
          <button
            v-if="c.bestStyle"
            class="market-creator-use"
            @click="handleUse(c.bestStyle)"
          >
            使用
          </button>
        </div>
      </div>
    </section>

    <!-- ⑤ 全部风格区 -->
    <section class="market-grid-section" ref="gridSection">
      <div class="market-section-head">
        <div class="market-section-title-wrap">
          <h2 class="market-section-title">全部风格</h2>
          <span class="market-section-sub">共 {{ approvedStyles.length }} 款</span>
        </div>
        <div class="market-search">
          <input
            v-model="searchQuery"
            type="text"
            class="market-search-input"
            placeholder="搜索风格名或适用范围"
          />
        </div>
      </div>

      <div class="market-tabs">
        <button
          v-for="tab in tabOptions"
          :key="tab.key"
          :class="['market-tab', { active: activeTab === tab.key }]"
          @click="activeTab = tab.key"
        >
          {{ tab.label }}
        </button>
      </div>

      <div v-if="filteredStyles.length === 0" class="market-empty">
        暂无已上架风格
      </div>
      <div v-else class="market-grid">
        <div
          v-for="s in filteredStyles"
          :key="s.id"
          class="market-card"
        >
          <div class="market-card-cover" :style="{ background: featuredBackground(s) }">
            <div class="market-card-cover-tags">
              <span v-for="t in parseScopeTags(s.scope)" :key="t" class="market-card-tag">
                # {{ t }}
              </span>
            </div>
            <span v-if="s.creatorId === currentUserId" class="market-card-mine">我的</span>
          </div>
          <div class="market-card-body">
            <div class="market-card-title">{{ s.name }}</div>
            <div class="market-card-creator">
              <span class="market-card-creator-avatar">
                {{ (s.creatorName || '匿').charAt(0) }}
              </span>
              <span>by {{ s.creatorName || '匿名用户' }}</span>
            </div>
            <div class="market-card-prompt">{{ promptSummary(s.prompt) }}</div>
            <div class="market-card-stats">
              <span>🔥 本周 {{ s.weeklyUses }} 次</span>
              <span>累计 {{ s.totalUses }} 次</span>
            </div>
            <div class="market-card-actions">
              <button class="market-card-use" @click="handleUse(s)">使用</button>
              <button
                :class="['market-card-fav', { active: isFavorite(s.id) }]"
                @click="handleToggleFavorite(s.id)"
              >
                {{ isFavorite(s.id) ? '♥' : '♡' }}
              </button>
              <button class="market-card-view" @click="handleUse(s)">查看</button>
              <button
                v-if="s.creatorId === currentUserId"
                class="market-card-sim"
                @click="handleSimulate(s)"
              >
                模拟
              </button>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>

  <!-- 收益规则弹框 — 保留 v1 写法 -->
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
  marketStats,
  topCreators,
  featuredStyles,
  useMarketStyle,
  simulateExternalUse,
  toggleFavorite,
  isFavorite,
  getMarketStyleEarnings,
  loadMarketStyles
} from '@/composables/useStyleMarket.js'

const router = useRouter()
const currentUserId = ref(localStorage.getItem('aichuangzuo_user_id') || '')
const rulesVisible = ref(false)

const formatCoins = (n) => Number(n || 0).toFixed(2)
const formatUses = (n) => Number(n || 0).toLocaleString()

const goUpload = () => {
  router.push('/console/styles')
}

const firstScope = (scope) => (scope || '').split(/[,，]/)[0]?.trim() || ''

const featuredBackground = (s) => {
  const palette = [
    'linear-gradient(135deg, #1a1a1a 0%, #2a1015 100%)',
    'linear-gradient(135deg, #1f1f1f 0%, #2c1f0a 100%)',
    'linear-gradient(135deg, #14142b 0%, #2a0a1f 100%)',
    'linear-gradient(135deg, #0d1f1f 0%, #1f3a2a 100%)',
    'linear-gradient(135deg, #2a1f1f 0%, #1a1010 100%)'
  ]
  const idx = (s.id || '').split('').reduce((sum, c) => sum + c.charCodeAt(0), 0) % palette.length
  return palette[idx]
}

const handleUse = (s) => {
  try {
    useMarketStyle(s.id)
    router.push(`/console/create?marketStyleId=${s.id}`)
  } catch (err) {
    alert(err.message)
  }
}

const scrollToGrid = () => {
  document.querySelector('.market-grid-section')?.scrollIntoView({ behavior: 'smooth' })
}

const searchQuery = ref('')
const activeTab = ref('all')
const gridSection = ref(null)

const tabOptions = [
  { key: 'all', label: '全部' },
  { key: 'week-hot', label: '本周最热' },
  { key: 'all-hot', label: '历史最热' },
  { key: 'new', label: '最新' },
  { key: 'featured', label: '官方精选' }
]

const approvedStyles = computed(() =>
  marketStyles.value.filter((s) => s.status === 'approved')
)

const filteredStyles = computed(() => {
  let list = approvedStyles.value
  if (activeTab.value === 'week-hot') {
    list = [...list].sort((a, b) => (b.weeklyUses || 0) - (a.weeklyUses || 0))
  } else if (activeTab.value === 'all-hot') {
    list = [...list].sort((a, b) => (b.totalUses || 0) - (a.totalUses || 0))
  } else if (activeTab.value === 'new') {
    list = [...list].sort((a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0))
  } else if (activeTab.value === 'featured') {
    list = list
      .filter((s) => (s.totalUses || 0) >= 5)
      .sort((a, b) => (b.totalUses || 0) - (a.totalUses || 0))
  }
  const q = searchQuery.value.trim().toLowerCase()
  if (!q) return list
  return list.filter(
    (s) =>
      s.name.toLowerCase().includes(q) ||
      (s.scope && s.scope.toLowerCase().includes(q))
  )
})

const parseScopeTags = (scopeStr) =>
  !scopeStr ? [] : scopeStr.split(/[,，]/).map((t) => t.trim()).filter(Boolean)

const promptSummary = (prompt) => {
  if (!prompt) return ''
  return prompt.length > 60 ? prompt.slice(0, 60) + '...' : prompt
}

const handleToggleFavorite = (id) => toggleFavorite(id)
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
.market-page {
  padding: var(--space-lg) var(--space-xl);
  display: flex;
  flex-direction: column;
  gap: var(--space-xl);
  max-width: 1280px;
  margin: 0 auto;
}

.market-banner {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: var(--space-xl);
  align-items: center;
  background: var(--color-bg-card);
  border-radius: var(--radius-xl);
  padding: var(--space-xl);
  box-shadow: var(--shadow-sm2);
}

.market-banner-title {
  font-size: var(--font-h1);
  font-weight: 700;
  color: var(--color-primary);
  margin: 0 0 var(--space-sm) 0;
  letter-spacing: -0.5px;
}

.market-banner-sub {
  font-size: var(--font-body);
  color: var(--color-text-secondary);
  margin: 0;
  display: flex;
  align-items: center;
  gap: var(--space-md);
  flex-wrap: wrap;
}

.market-banner-rules-link {
  color: var(--color-primary);
  cursor: pointer;
  text-decoration: underline;
  text-underline-offset: 4px;
  font-weight: 500;
}
.market-banner-rules-link:hover { color: var(--color-primary-hover); }

.market-banner-stats {
  display: flex;
  gap: var(--space-lg);
}

.market-banner-stat {
  background: var(--color-bg-page);
  border-radius: var(--radius-xl);
  padding: var(--space-md) var(--space-lg);
  min-width: 120px;
}
.market-banner-stat-num {
  font-size: var(--font-h2);
  font-weight: 700;
  color: var(--color-text-primary);
  line-height: 1.2;
}
.market-banner-stat-label {
  font-size: var(--font-small);
  color: var(--color-text-secondary);
  margin-top: var(--space-xs);
}

/* ⑤ 占位防 build break（实样式在本区块内） */
.market-grid-section { scroll-margin-top: var(--space-xl); }

/* === ⑤ 全部风格 === */
.market-search { display: flex; align-items: center; }
.market-search-input {
  width: 100%;
  min-width: 240px;
  max-width: 480px;
  height: 40px;
  padding: 0 var(--space-md);
  border: 1px solid var(--color-border-default);
  border-radius: var(--radius-full);
  font-size: var(--font-body);
  background: var(--color-bg-page);
  outline: none;
}
.market-search-input:focus {
  background: var(--color-bg-card);
  border-color: var(--color-primary);
}

.market-tabs {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: var(--color-bg-page);
  padding: 4px;
  border-radius: var(--radius-lg);
  margin-bottom: var(--space-lg);
}
.market-tab {
  padding: var(--space-sm) var(--space-md);
  border: 0;
  background: transparent;
  border-radius: var(--radius-md);
  font-size: var(--font-body);
  font-weight: 500;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all 0.2s;
}
.market-tab.active {
  background: var(--color-bg-card);
  color: var(--color-text-primary);
  box-shadow: var(--shadow-sm);
}
.market-tab:hover { color: var(--color-text-primary); }

.market-empty {
  padding: var(--space-xl) 0;
  text-align: center;
  color: var(--color-text-placeholder);
  font-size: var(--font-body);
}

.market-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: var(--space-lg);
}

.market-card {
  background: var(--color-bg-card);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-sm2);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  transition: transform 0.2s, box-shadow 0.2s;
}
.market-card:hover {
  transform: translateY(-6px);
  box-shadow: var(--shadow-md);
}

.market-card-cover {
  position: relative;
  height: 80px;
  padding: var(--space-sm) var(--space-md);
}
.market-card-cover-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
.market-card-tag {
  background: rgba(255, 255, 255, 0.85);
  color: var(--color-primary);
  font-size: var(--font-caption);
  border-radius: var(--radius-md);
  padding: 2px 8px;
  font-weight: 500;
}
.market-card-mine {
  position: absolute;
  top: var(--space-sm);
  right: var(--space-sm);
  background: var(--color-info);
  color: #fff;
  font-size: var(--font-caption);
  border-radius: var(--radius-md);
  padding: 2px 8px;
  font-weight: 600;
}

.market-card-body {
  padding: var(--space-md);
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
  flex: 1;
}
.market-card-title {
  font-size: var(--font-h3);
  font-weight: 700;
  color: var(--color-text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.market-card-creator {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
  font-size: var(--font-body);
  color: var(--color-text-regular);
}
.market-card-creator-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--color-primary-light);
  color: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-small);
  font-weight: 700;
}
.market-card-prompt {
  font-size: var(--font-body);
  color: var(--color-text-regular);
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.market-card-stats {
  display: flex;
  gap: var(--space-md);
  font-size: var(--font-small);
  color: var(--color-text-placeholder);
}
.market-card-actions {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  margin-top: auto;
  padding-top: var(--space-sm);
}
.market-card-use {
  background: var(--color-primary);
  color: #fff;
  border: 0;
  border-radius: var(--radius-lg);
  height: 40px;
  padding: 0 var(--space-md);
  font-size: var(--font-body);
  font-weight: 600;
  cursor: pointer;
}
.market-card-use:hover { background: var(--color-primary-hover); }
.market-card-fav {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border-default);
  border-radius: var(--radius-lg);
  font-size: var(--font-body);
  color: var(--color-text-placeholder);
  cursor: pointer;
}
.market-card-fav.active {
  background: var(--color-primary-light);
  color: var(--color-primary);
  border-color: var(--color-primary);
}
.market-card-view, .market-card-sim {
  background: transparent;
  border: 0;
  font-size: var(--font-body);
  color: var(--color-text-secondary);
  cursor: pointer;
  padding: var(--space-sm);
}
.market-card-view:hover, .market-card-sim:hover {
  color: var(--color-primary);
}

/* ⑤ 暗色 */
body[data-theme="dark"] .market-card { background: #1f1f1f; }
body[data-theme="dark"] .market-search-input {
  background: #141414;
  border-color: #303030;
  color: var(--color-text-primary);
}
body[data-theme="dark"] .market-search-input:focus {
  background: #1f1f1f;
  border-color: var(--color-primary);
}
body[data-theme="dark"] .market-tabs { background: #141414; }
body[data-theme="dark"] .market-tab { color: var(--color-text-secondary); }
body[data-theme="dark"] .market-tab.active {
  background: #2a2a2a;
  color: var(--color-text-primary);
  box-shadow: none;
}
body[data-theme="dark"] .market-card-title { color: var(--color-text-primary); }
body[data-theme="dark"] .market-card-creator { color: var(--color-text-regular); }
body[data-theme="dark"] .market-card-creator-avatar {
  background: rgba(255, 36, 66, 0.12);
  color: #ff6b81;
}
body[data-theme="dark"] .market-card-prompt { color: #d9d9d9; }
body[data-theme="dark"] .market-card-stats { color: #a6a6a6; }
body[data-theme="dark"] .market-card-tag {
  background: rgba(255, 255, 255, 0.12);
  color: #ff6b81;
}
body[data-theme="dark"] .market-card-fav {
  background: transparent;
  border-color: #303030;
}
body[data-theme="dark"] .market-card-fav.active {
  background: rgba(255, 36, 66, 0.15);
  color: #ff6b81;
  border-color: var(--color-primary);
}

/* === ④ 收益潜力榜 === */
.market-section-sub {
  font-size: var(--font-body);
  color: var(--color-text-secondary);
  font-weight: 400;
}

.market-creators-empty {
  padding: var(--space-xl);
  text-align: center;
  color: var(--color-text-placeholder);
  background: var(--color-bg-card);
  border-radius: var(--radius-xl);
  font-size: var(--font-body);
}

.market-creators-rail {
  display: flex;
  gap: var(--space-md);
  overflow-x: auto;
  scroll-snap-type: x mandatory;
  padding-bottom: var(--space-sm);
  scrollbar-width: thin;
}
.market-creators-rail::-webkit-scrollbar { height: 6px; }
.market-creators-rail::-webkit-scrollbar-thumb {
  background: var(--color-bg-hover);
  border-radius: 3px;
}

.market-creator-card {
  flex: 0 0 200px;
  background: var(--color-bg-card);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-sm2);
  padding: var(--space-md);
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  scroll-snap-align: start;
  transition: transform 0.2s, box-shadow 0.2s;
}
.market-creator-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}
.market-creator-avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: var(--color-primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-h3);
  font-weight: 700;
  margin-bottom: var(--space-sm);
}
.market-creator-name {
  font-size: var(--font-body);
  font-weight: 700;
  color: var(--color-text-primary);
  margin-bottom: var(--space-xs);
}
.market-creator-weekly {
  font-size: var(--font-h2);
  font-weight: 700;
  color: var(--color-primary);
  line-height: 1.2;
  margin-bottom: var(--space-sm);
}
.market-creator-best {
  font-size: var(--font-small);
  color: var(--color-text-secondary);
  margin-bottom: var(--space-md);
  white-space: nowrap;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
}
.market-creator-use {
  background: var(--color-primary);
  color: #fff;
  border: 0;
  border-radius: var(--radius-lg);
  height: 36px;
  width: 100%;
  font-size: var(--font-body);
  font-weight: 600;
  cursor: pointer;
}
.market-creator-use:hover { background: var(--color-primary-hover); }

/* ④ 暗色 */
body[data-theme="dark"] .market-creator-card { background: #1f1f1f; }
body[data-theme="dark"] .market-creator-name { color: var(--color-text-primary); }
body[data-theme="dark"] .market-section-sub { color: var(--color-text-secondary); }

/* === ③ 官方精选 === */
.market-section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-md);
}
.market-section-title-wrap {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}
.market-section-title {
  font-size: var(--font-h2);
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0;
}
.market-official-badge {
  background: var(--color-primary);
  color: #fff;
  font-size: var(--font-caption);
  border-radius: var(--radius-md);
  padding: 2px 8px;
  font-weight: 600;
}
.market-section-link {
  background: transparent;
  border: 0;
  color: var(--color-primary);
  cursor: pointer;
  font-size: var(--font-body);
  font-weight: 500;
}
.market-section-link:hover { color: var(--color-primary-hover); }

.market-featured-empty {
  height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-placeholder);
  font-size: var(--font-body);
  background: var(--color-bg-card);
  border-radius: var(--radius-xl);
}
.market-featured-rail {
  display: flex;
  gap: var(--space-md);
  overflow-x: auto;
  scroll-snap-type: x mandatory;
  padding-bottom: var(--space-sm);
  scrollbar-width: thin;
}
.market-featured-rail::-webkit-scrollbar { height: 6px; }
.market-featured-rail::-webkit-scrollbar-thumb {
  background: var(--color-bg-hover);
  border-radius: 3px;
}
.market-featured-card {
  scroll-snap-align: start;
  flex: 0 0 320px;
  height: 200px;
  border-radius: var(--radius-xl);
  padding: var(--space-lg);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  color: #fff;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}
.market-featured-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-md);
}
.market-featured-name {
  font-size: var(--font-h3);
  font-weight: 700;
  line-height: 1.3;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.market-featured-tag {
  display: inline-flex;
  width: fit-content;
  background: rgba(255, 36, 66, 0.85);
  color: #fff;
  font-size: var(--font-caption);
  border-radius: var(--radius-md);
  padding: 2px 8px;
  margin-top: var(--space-xs);
}
.market-featured-uses {
  font-size: var(--font-h2);
  font-weight: 700;
  line-height: 1.2;
}
.market-featured-creator {
  font-size: var(--font-caption);
  color: rgba(255, 255, 255, 0.7);
}

/* === ② 上传激励卡 === */
.market-upload-card {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: var(--space-lg);
  background: var(--color-bg-card);
  border-left: 4px solid var(--color-primary);
  border-radius: var(--radius-xl);
  padding: var(--space-lg) var(--space-xl);
  cursor: pointer;
  transition: box-shadow 0.2s, transform 0.2s;
  box-shadow: var(--shadow-sm2);
}
.market-upload-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}
.market-upload-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-xl);
  background: var(--color-primary-light);
  color: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-h2);
  font-weight: 600;
}
.market-upload-body { min-width: 0; }
.market-upload-title {
  font-size: var(--font-h3);
  font-weight: 700;
  color: var(--color-text-primary);
  margin-bottom: var(--space-xs);
}
.market-upload-sub {
  font-size: var(--font-small);
  color: var(--color-text-secondary);
}
.market-upload-cta {
  background: var(--color-primary);
  color: #fff;
  border: 0;
  border-radius: var(--radius-lg);
  height: 40px;
  padding: 0 var(--space-lg);
  font-size: var(--font-body);
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}
.market-upload-cta:hover { background: var(--color-primary-hover); }

/* ② 暗色 */
body[data-theme="dark"] .market-upload-card { background: #1f1f1f; }
body[data-theme="dark"] .market-upload-icon {
  background: rgba(255, 36, 66, 0.12);
  color: #ff6b81;
}
body[data-theme="dark"] .market-upload-title { color: var(--color-text-primary); }
body[data-theme="dark"] .market-upload-sub { color: var(--color-text-secondary); }

/* ① 暗色 */
body[data-theme="dark"] .market-banner { background: #1f1f1f; }
body[data-theme="dark"] .market-banner-title { color: #ff6b81; }
body[data-theme="dark"] .market-banner-sub { color: var(--color-text-secondary); }
body[data-theme="dark"] .market-banner-stat { background: #141414; }
body[data-theme="dark"] .market-banner-stat-num { color: var(--color-text-primary); }
body[data-theme="dark"] .market-banner-stat-label { color: var(--color-text-secondary); }
</style>

<style>
/* ① 收益规则弹层外壳（全局）—— 沿用 v1 弹框壳，v2 内层由 scoped 提供类 */
body[data-theme="dark"] .rules-modal .ant-modal-content {
  background: #141414;
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.6);
}
body[data-theme="dark"] .rules-modal .ant-modal-header {
  background: #141414;
  border-bottom-color: #303030;
}
body[data-theme="dark"] .rules-modal .ant-modal-title { color: #e0e0e0; }
body[data-theme="dark"] .rules-modal .ant-modal-close { color: #a6a6a6; }
body[data-theme="dark"] .rules-modal .ant-modal-close:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.08);
}

/* ① 收益规则内层（scoped 也能写，但段落含 nth-of-type 选择器，全局更稳） */
.style-market-rules-list {
  margin: 0;
  padding-left: 20px;
  font-size: var(--font-body);
  color: var(--color-text-regular);
  line-height: 1.8;
}
.style-market-rules-list li { margin-bottom: var(--space-sm); }
.style-market-rule-highlight {
  color: var(--color-primary);
  font-weight: 500;
}
.style-market-rules-footer {
  margin-top: var(--space-md);
  padding-top: var(--space-md);
  border-top: 1px solid var(--color-border-light);
  font-size: var(--font-small);
  color: var(--color-text-placeholder);
}

body[data-theme="dark"] .style-market-rules-list { color: #a6a6a6; }
body[data-theme="dark"] .style-market-rules-footer {
  border-top-color: #303030;
  color: #a6a6a6;
}
body[data-theme="dark"] .style-market-rule-highlight { color: #ff6b81; }
</style>
