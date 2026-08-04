<template>
  <div class="creator-rank-page">
    <header class="creator-rank-header">
      <h1 class="creator-rank-header__title">收益潜力榜</h1>
      <p class="creator-rank-header__subtitle">
        按创作者近一个月提示词被使用次数 × 单次收益计算排名，Top5 可获额外月度奖励
      </p>
    </header>

    <section v-if="loading" class="creator-rank-empty">加载中...</section>
    <section v-else-if="topCreators.length === 0" class="creator-rank-empty">暂无上榜创作者</section>
    <section v-else class="creator-rank-list">
      <div
        v-for="(c, idx) in topCreators"
        :key="c.creatorId"
        class="creator-rank-card"
        @click="openCreator(c)"
      >
        <div :class="['creator-rank-card__rank', { top3: idx < 3 }]">{{ String(idx + 1).padStart(2, '0') }}</div>
        <div class="creator-rank-card__avatar">{{ (c.creatorName || '匿').charAt(0) }}</div>
        <div class="creator-rank-card__body">
          <div class="creator-rank-card__name">{{ c.creatorName || '匿名用户' }}</div>
          <div v-if="c.bestSkill" class="creator-rank-card__best">代表提示词 · {{ c.bestSkill.name }}</div>
          <div class="creator-rank-card__stats">
            <span>本周 {{ formatCoins(c.weeklyEarnings) }} 币</span>
            <span v-if="c.monthlyEarnings">本月 {{ formatCoins(c.monthlyEarnings) }} 币</span>
          </div>
        </div>
        <div class="creator-rank-card__earning">
          <div class="creator-rank-card__amount">+{{ formatCoins(c.weeklyEarnings) }}</div>
          <div class="creator-rank-card__amount-label">本周币</div>
        </div>
      </div>
    </section>

    <!-- 创作者详情弹框 -->
    <a-modal
      v-if="selectedCreator"
      class="creator-modal"
      :open="creatorModalVisible"
      :footer="null"
      :width="640"
      centered
      :destroy-on-close="true"
      @cancel="closeCreatorModal"
    >
      <template #title>
        <div class="creator-modal-title">
          <div class="creator-modal-avatar">{{ (selectedCreator.creatorName || '匿').charAt(0) }}</div>
          <div class="creator-modal-title-text">
            <div class="creator-modal-name">{{ selectedCreator.creatorName || '匿名用户' }}</div>
            <div class="creator-modal-sub">TA 的提示词市场主页</div>
          </div>
        </div>
      </template>

      <div class="creator-modal-body">
        <div class="creator-modal-stats">
          <div class="creator-modal-stat">
            <div class="creator-modal-stat-value">{{ creatorStyles.length }}</div>
            <div class="creator-modal-stat-label">对外公布提示词</div>
          </div>
          <div class="creator-modal-stat">
            <div class="creator-modal-stat-value">+{{ formatCoins(selectedCreator.weeklyEarnings) }}</div>
            <div class="creator-modal-stat-label">本周币</div>
          </div>
          <div class="creator-modal-stat">
            <div class="creator-modal-stat-value">{{ formatCoins(creatorTotalEarnings) }}</div>
            <div class="creator-modal-stat-label">累计币</div>
          </div>
          <div v-if="creatorFeaturedCount > 0" class="creator-modal-stat highlight">
            <div class="creator-modal-stat-value">{{ creatorFeaturedCount }}</div>
            <div class="creator-modal-stat-label">官方精选</div>
          </div>
        </div>

        <div v-if="creatorStyles.length === 0" class="creator-modal-empty">
          该创作者暂未对外公布提示词。
        </div>
        <div v-else class="creator-modal-list">
          <div
            v-for="s in creatorStyles"
            :key="s.id"
            class="creator-modal-style-row"
          >
            <div class="creator-modal-style-main">
              <div class="creator-modal-style-name">
                {{ s.name }}
                <span v-if="s.featured" class="creator-modal-style-featured">官方精选</span>
              </div>
              <div v-if="s.scope" class="creator-modal-style-scope"># {{ firstScope(s.scope) }}</div>
            </div>
            <div class="creator-modal-style-meta">
              <span class="creator-modal-style-uses">{{ formatUses(s.totalUses) }} 次使用</span>
              <span class="creator-modal-style-earning">+{{ formatCoins(s.weeklyEarnings) }} 币/周</span>
            </div>
          </div>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import {
  marketSkills,
  marketOverview,
  topCreators,
  loadMarketSkills,
  loadMarketSkillOverview
} from '@/composables/useSkillMarket.js'

const loading = ref(false)
const creatorModalVisible = ref(false)
const selectedCreator = ref(null)

const formatCoins = (n) => Number(n || 0).toFixed(2)
const formatUses = (n) => Number(n || 0).toLocaleString()
const firstScope = (scope) => (scope || '').split(/[,，]/)[0]?.trim() || ''

const creatorStyles = computed(() => {
  const c = selectedCreator.value
  if (!c) return []
  return marketSkills.value
    .filter((s) => s.creatorId === c.creatorId && s.status === 'approved')
    .sort((a, b) => (b.totalUses || 0) - (a.totalUses || 0))
})

const creatorTotalEarnings = computed(() => {
  const c = selectedCreator.value
  if (!c) return 0
  const price = 2
  return creatorStyles.value.reduce((sum, s) => {
    return sum + (s.totalUses || 0) * price
  }, 0)
})

const creatorFeaturedCount = computed(() =>
  creatorStyles.value.filter((s) => s.featured === true).length
)

function openCreator(c) {
  selectedCreator.value = c
  creatorModalVisible.value = true
}

function closeCreatorModal() {
  creatorModalVisible.value = false
  selectedCreator.value = null
}

onMounted(async () => {
  loading.value = true
  try {
    await Promise.all([
      loadMarketSkills(),
      loadMarketSkillOverview()
    ])
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.creator-rank-page {
  width: 100%;
  max-width: 1280px;
  margin: 0 auto;
  padding: 16px;
  box-sizing: border-box;
  background: #fafafa;
  min-height: 100%;
}

.creator-rank-header {
  margin-bottom: 16px;
}
.creator-rank-header__title {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0 0 8px;
}
.creator-rank-header__subtitle {
  font-size: 14px;
  color: #595959;
  line-height: 1.6;
  margin: 0;
}

.creator-rank-empty {
  padding: 56px 20px;
  text-align: center;
  color: #8c8c8c;
  background: #fff;
  border-radius: 18px;
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.04);
  font-size: 14px;
}

.creator-rank-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.creator-rank-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: #fff;
  border-radius: 18px;
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}
.creator-rank-card:active {
  transform: scale(0.99);
}

.creator-rank-card__rank {
  font-size: 18px;
  font-weight: 700;
  color: #8c8c8c;
  min-width: 32px;
  font-variant-numeric: tabular-nums;
}
.creator-rank-card__rank.top3 {
  color: var(--color-primary, #ff2442);
}

.creator-rank-card__avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: rgba(255, 36, 66, 0.08);
  color: var(--color-primary, #ff2442);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 700;
  flex-shrink: 0;
}

.creator-rank-card__body {
  flex: 1;
  min-width: 0;
}

.creator-rank-card__name {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.creator-rank-card__best {
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.creator-rank-card__stats {
  display: flex;
  gap: 10px;
  font-size: 11px;
  color: #8c8c8c;
}

.creator-rank-card__earning {
  text-align: right;
  flex-shrink: 0;
}

.creator-rank-card__amount {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-primary, #ff2442);
  line-height: 1.2;
}

.creator-rank-card__amount-label {
  font-size: 11px;
  color: #8c8c8c;
}

/* PC 端适配 */
@media (min-width: 769px) {
  .creator-rank-page {
    padding: 24px 32px;
  }
  .creator-rank-header__title {
    font-size: 28px;
  }
  .creator-rank-card {
    padding: 20px 22px;
  }
  .creator-rank-card__name {
    font-size: 16px;
  }
  .creator-rank-card__stats {
    font-size: 12px;
  }
  .creator-rank-card__amount {
    font-size: 20px;
  }
}

/* 暗色主题 */
body[data-theme="dark"] .creator-rank-page { background: #141414; }
body[data-theme="dark"] .creator-rank-header__title { color: #f5f5f5; }
body[data-theme="dark"] .creator-rank-header__subtitle { color: #a6a6a6; }
body[data-theme="dark"] .creator-rank-empty,
body[data-theme="dark"] .creator-rank-card { background: #1f1f1f; }
body[data-theme="dark"] .creator-rank-card__name { color: #f5f5f5; }
body[data-theme="dark"] .creator-rank-card__best,
body[data-theme="dark"] .creator-rank-card__stats,
body[data-theme="dark"] .creator-rank-card__amount-label { color: #a6a6a6; }
body[data-theme="dark"] .creator-rank-card__avatar {
  background: rgba(255, 36, 66, 0.12);
  color: #ff6b81;
}
body[data-theme="dark"] .creator-rank-card__rank { color: #8c8c8c; }
body[data-theme="dark"] .creator-rank-card__rank.top3 { color: #ff6b81; }

/* 创作者详情弹框 */
.creator-modal-body {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.creator-modal-title {
  display: flex;
  align-items: center;
  gap: 12px;
}
.creator-modal-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-primary) 0%, #ff5577 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 600;
}
.creator-modal-title-text { display: flex; flex-direction: column; gap: 2px; }
.creator-modal-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
}
.creator-modal-sub {
  font-size: 12px;
  color: var(--color-text-placeholder);
}

.creator-modal-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  padding: 14px;
  background: var(--color-bg-page);
  border-radius: var(--radius-md);
  margin-bottom: 16px;
}
.creator-modal-stat { text-align: center; }
.creator-modal-stat.highlight .creator-modal-stat-value { color: var(--color-primary); }
.creator-modal-stat-value {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-primary);
  font-variant-numeric: tabular-nums;
}
.creator-modal-stat-label {
  font-size: 12px;
  color: var(--color-text-placeholder);
  margin-top: 2px;
}

.creator-modal-empty {
  padding: 32px 0;
  text-align: center;
  color: var(--color-text-placeholder);
  font-size: 13px;
}

.creator-modal-list {
  flex: 1;
  min-height: 0;
  overflow: auto;
  border-top: 1px solid var(--color-border-light);
}
.creator-modal-style-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 4px;
  border-bottom: 1px solid var(--color-border-light);
}
.creator-modal-style-main { min-width: 0; flex: 1; }
.creator-modal-style-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-primary);
  display: flex;
  align-items: center;
  gap: 8px;
}
.creator-modal-style-featured {
  font-size: 11px;
  color: var(--color-primary);
  background: rgba(255, 36, 66, 0.08);
  padding: 1px 6px;
  border-radius: 4px;
  font-weight: 500;
}
.creator-modal-style-scope {
  font-size: 12px;
  color: var(--color-text-placeholder);
  margin-top: 2px;
}
.creator-modal-style-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
  font-size: 12px;
  color: var(--color-text-secondary);
  white-space: nowrap;
}
.creator-modal-style-earning {
  color: var(--color-primary);
  font-weight: 500;
  font-variant-numeric: tabular-nums;
}

@media (max-width: 640px) {
  .creator-modal-stats { grid-template-columns: repeat(2, 1fr); }
}
body[data-theme="dark"] .creator-modal-style-featured {
  background: rgba(255, 36, 66, 0.12);
  color: #ff6b81;
}

.creator-modal .ant-modal-body {
  height: 520px;
  padding: 20px 24px 24px;
}
</style>
