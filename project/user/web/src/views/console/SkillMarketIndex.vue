<template>
  <div class="market-page">
    <!-- ① 平台 Banner 区 -->
    <section class="market-banner">
      <div class="market-banner-text">
        <h1 class="market-banner-title">爱创作 · 提示词市场</h1>
        <p class="market-banner-sub">
          官方运营 · 精选创作者提示词 · 使用即获收益分成
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
        <div class="market-upload-title">上传你的提示词，开始赚创作币</div>
        <div class="market-upload-sub">每被他人使用 1 次即得 2 币；周里程碑最高额外 +600</div>
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
      <div v-if="featuredSkills.length === 0" class="market-featured-empty">
        官方精选即将上线
      </div>
      <div v-else class="market-featured-rail">
        <div
          v-for="s in featuredSkills"
          :key="s.id"
          class="market-featured-card"
          @click="openStyleDetail(s)"
        >
          <span class="market-featured-badge">官方精选</span>
          <div class="market-featured-head">
            <div class="market-featured-name">{{ s.name }}</div>
            <div class="market-featured-uses">{{ s.weeklyUses }}</div>
          </div>
          <div class="market-featured-meta">
            <span v-if="s.scope"># {{ firstScope(s.scope) }} · </span>by {{ s.creatorName || '匿名用户' }}
          </div>
        </div>
      </div>
    </section>

    <!-- ④ 收益潜力榜 -->
    <section class="market-creators">
      <div class="market-section-head">
        <div class="market-section-title-wrap">
          <h2 class="market-section-title">收益潜力榜</h2>
          <span class="market-section-sub">看看谁在用提示词赚到币</span>
        </div>
        <button
          v-if="hasMoreCreators"
          class="market-section-link"
          @click="creatorsExpanded = !creatorsExpanded"
        >
          {{ creatorsExpanded ? '收起 ↑' : '查看完整榜单 ↓' }}
        </button>
      </div>
      <div v-if="topCreators.length === 0" class="market-creators-empty">
        暂无上榜创作者
      </div>
      <div v-else class="market-creators-list">
        <div
          v-for="(c, idx) in visibleCreators"
          :key="c.creatorId"
          class="market-creator-row"
          @click="openCreator(c)"
        >
          <div :class="['market-creator-rank', { top3: idx < 3 }]">
            {{ String(idx + 1).padStart(2, '0') }}
          </div>
          <div class="market-creator-avatar">
            {{ (c.creatorName || '匿').charAt(0) }}
          </div>
          <div class="market-creator-info">
            <div class="market-creator-name">{{ c.creatorName || '匿名用户' }}</div>
            <div v-if="c.bestSkill" class="market-creator-best">
              代表提示词 · {{ c.bestSkill.name }}
            </div>
          </div>
          <div class="market-creator-earning">
            <div class="market-creator-amount">+{{ formatCoins(c.weeklyEarnings) }}</div>
            <div class="market-creator-amount-label">本周币</div>
          </div>
        </div>
      </div>
    </section>

    <!-- ⑤ 全部提示词区 -->
    <section class="market-grid-section" ref="gridSection">
      <div class="market-section-head">
        <div class="market-section-title-wrap">
          <h2 class="market-section-title">全部提示词</h2>
          <span class="market-section-sub">共 {{ total }} 款</span>
        </div>
        <div class="market-search">
          <input
            v-model="searchQuery"
            type="text"
            class="market-search-input"
            placeholder="搜索提示词名或适用范围"
            @keyup.enter="onSearch"
          />
          <button class="market-search-btn" @click="onSearch">搜索</button>
        </div>
      </div>

      <div class="market-tabs">
        <button
          v-for="tab in tabOptions"
          :key="tab.key"
          :class="['market-tab', { active: activeTab === tab.key }]"
          @click="onTabChange(tab.key)"
        >
          {{ tab.label }}
        </button>
      </div>

      <div v-if="loading" class="market-empty">加载中...</div>
      <div v-else-if="pagedStyles.length === 0" class="market-empty">
        暂无已上架提示词
      </div>
      <div v-else class="market-grid">
        <SkillCard
          v-for="s in pagedStyles"
          :key="s.id"
          :name="s.name"
          :desc="s.description || s.promptSummary || s.desc || ''"
          :prompt="promptSummary(s.prompt)"
          :show-avatar="false"
          clickable
          @click="openStyleDetail(s)"
        >
          <template #meta>
            <div class="market-card-meta-row">
              <span class="market-card-creator">
                <span class="market-card-creator-avatar">
                  {{ (s.creatorName || '匿').charAt(0) }}
                </span>
                <span class="market-card-creator-name">by {{ s.creatorName || '匿名用户' }}</span>
              </span>
              <span
                v-if="parseScopeTags(s.scope).length || s.creatorId === currentUserId"
                class="market-card-scope-inline"
              >
                <span
                  v-for="t in parseScopeTags(s.scope).slice(0, 2)"
                  :key="t"
                  class="market-card-tag-compact"
                >
                  # {{ t }}
                </span>
                <span
                  v-if="parseScopeTags(s.scope).length > 2"
                  class="market-card-tag-more"
                >
                  +{{ parseScopeTags(s.scope).length - 2 }}
                </span>
                <span v-if="s.creatorId === currentUserId" class="market-card-mine-compact">我的</span>
              </span>
            </div>
          </template>
          <template #extra>
            <div class="market-card-extra-row">
              <span v-if="s.createdAt" class="market-card-published">
                发布于 {{ formatTimeAgo(s.createdAt) }}
              </span>
              <span class="market-card-extra-dot" v-if="s.createdAt">·</span>
              <span>本周 {{ s.weeklyUses }} 次</span>
              <span class="market-card-extra-dot">·</span>
              <span>累计 {{ s.totalUses }} 次</span>
            </div>
          </template>
          <template #footer>
            <div class="market-card-actions">
              <button class="market-card-use" @click.stop="handleUse(s)">使用</button>
              <button
                :class="['market-card-fav', { active: isFavorite(s.id) }]"
                @click.stop="handleToggleFavorite(s.id)"
              >
                {{ isFavorite(s.id) ? '♥' : '♡' }}
              </button>
              <button class="market-card-view" @click.stop="openStyleDetail(s)">查看</button>
              <button
                v-if="s.creatorId === currentUserId"
                class="market-card-sim"
                @click.stop="handleSimulate(s)"
              >
                模拟
              </button>
            </div>
          </template>
        </SkillCard>
      </div>

      <div v-if="!loading && total > 0" class="market-pagination">
        <a-pagination
          v-model:current="page"
          :page-size="pageSize"
          :total="total"
          :show-size-changer="false"
          show-less-items
          @change="onPageChange"
        />
      </div>
    </section>
  </div>

  <!-- 收益规则弹框 — 保留 v1 写法 -->
  <a-modal
    v-model:open="rulesVisible"
    class="rules-modal"
    title="提示词市场收益规则"
    :footer="null"
    :width="560"
    centered
  >
    <ol class="style-market-rules-list">
      <li>他人每使用一次你分享的提示词，你将获得 <span class="style-market-rule-highlight">2 创作币</span> 奖励。</li>
      <li>每周根据提示词被使用次数发放里程碑奖励：<span class="style-market-rule-highlight">50 次 50 币</span>、<span class="style-market-rule-highlight">200 次 150 币</span>、<span class="style-market-rule-highlight">500 次 300 币</span>、<span class="style-market-rule-highlight">1000 次 600 币</span>。</li>
      <li>里程碑奖励 <span class="style-market-rule-highlight">每周结算一次</span>，结算后当周使用次数清零并重新累计。</li>
      <li>使用他人分享的提示词 <span class="style-market-rule-highlight">无需支付创作币</span>，创作者仍可正常获得收益。</li>
      <li>如发现违规刷量行为，平台有权 <span class="style-market-rule-highlight">取消相关收益并下架提示词</span>。</li>
      <li>使用提示词市场上的提示词生成文章并成功后，该提示词的累计使用次数与本月使用次数会增加，创作者可获得对应创作币收益。</li>
      <li>使用自己创建或学习获得的提示词生成文章并成功后，仅累计该提示词的使用次数，不产生收益。</li>
      <li>生成失败或手动停止的任务不计入使用次数与收益。</li>
    </ol>
    <div class="style-market-rules-footer">* 活动最终解释权归平台所有。</div>
  </a-modal>

  <!-- 创作者详情 modal — 显示 ta 对外公布的提示词 -->
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
          <div class="creator-modal-stat-value">{{ formatUses(creatorTotalEarnings) }}</div>
          <div class="creator-modal-stat-label">累计币 (按使用×2)</div>
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

  <!-- 提示词详情 modal -->
  <SkillDetailModal
    v-if="selectedStyle"
    :skill="selectedStyle"
    :visible="styleDetailVisible"
    :current-user-id="currentUserId"
    @update:visible="onStyleDetailVisibleChange"
  />
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  marketSkills,
  marketStats,
  topCreators,
  featuredSkills,
  useMarketSkill,
  simulateExternalUse,
  toggleFavorite,
  isFavorite,
  loadMarketSkills,
  loadMarketSkillOverview,
  loadMarketSkillPage,
  loadFavoriteIds
} from '@/composables/useSkillMarket.js'
import SkillCard from '@/components/SkillCard.vue'
import SkillDetailModal from '@/components/SkillDetailModal.vue'

const router = useRouter()
const currentUserId = ref(localStorage.getItem('aichuangzuo_user_id') || '')
const rulesVisible = ref(false)

const formatCoins = (n) => Number(n || 0).toFixed(2)
const formatUses = (n) => Number(n || 0).toLocaleString()

const goUpload = () => {
  router.push('/console/skills')
}

// ④ 排行榜点击 → 打开创作者详情 modal（只查看 ta 对外公布的提示词，不直接使用）
const creatorModalVisible = ref(false)
const selectedCreator = ref(null)
const openCreator = (c) => {
  selectedCreator.value = c
  creatorModalVisible.value = true
}
const closeCreatorModal = () => {
  creatorModalVisible.value = false
  selectedCreator.value = null
}
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
  return creatorStyles.value.reduce((sum, s) => sum + (s.totalUses || 0), 0) * 2
})
const creatorFeaturedCount = computed(() =>
  creatorStyles.value.filter((s) => s.featured === true).length
)

const firstScope = (scope) => (scope || '').split(/[,，]/)[0]?.trim() || ''

const handleUse = (s) => {
  try {
    useMarketSkill(s.id)
    router.push(`/console/create?marketSkillId=${s.id}`)
  } catch (err) {
    alert(err.message)
  }
}

// 提示词详情 modal：点击卡片/查看按钮展示提示词内容（prompt、统计等），不再直接跳转创作页
const styleDetailVisible = ref(false)
const selectedStyle = ref(null)
const openStyleDetail = (s) => {
  selectedStyle.value = s
  styleDetailVisible.value = true
}
const onStyleDetailVisibleChange = (val) => {
  styleDetailVisible.value = val
  if (!val) selectedStyle.value = null
}

const scrollToGrid = () => {
  document.querySelector('.market-grid-section')?.scrollIntoView({ behavior: 'smooth' })
}

const searchQuery = ref('')
const activeTab = ref('all')
const gridSection = ref(null)

// ④ 收益榜：默认前 5 行，「查看完整榜单」可展开到前 20
const creatorsExpanded = ref(false)
const visibleCreators = computed(() =>
  creatorsExpanded.value ? topCreators.value : topCreators.value.slice(0, 5)
)
const hasMoreCreators = computed(() => topCreators.value.length > 5)

const tabOptions = [
  { key: 'all', label: '全部' },
  { key: 'week-hot', label: '本周最热' },
  { key: 'all-hot', label: '历史最热' },
  { key: 'new', label: '最新' },
  { key: 'featured', label: '官方精选' }
]

// ⑤ 全部提示词：后端分页
const PAGE_SIZE = 15
const page = ref(1)
const pageSize = ref(PAGE_SIZE)
const total = ref(0)
const pagedStyles = ref([])
const loading = ref(false)

const loadPage = async (targetPage = 1) => {
  loading.value = true
  page.value = targetPage
  const result = await loadMarketSkillPage({
    page: targetPage,
    pageSize: pageSize.value,
    keyword: searchQuery.value.trim(),
    sortType: activeTab.value
  })
  pagedStyles.value = result.list
  total.value = result.total
  loading.value = false
}

const onSearch = () => loadPage(1)
const onTabChange = (key) => {
  activeTab.value = key
  loadPage(1)
}
const onPageChange = (p) => loadPage(p)

const parseScopeTags = (scopeStr) =>
  !scopeStr ? [] : scopeStr.split(/[,，]/).map((t) => t.trim()).filter(Boolean)

const promptSummary = (prompt) => {
  if (!prompt) return ''
  return prompt.length > 60 ? prompt.slice(0, 60) + '...' : prompt
}

const formatTimeAgo = (isoStr) => {
  if (!isoStr) return ''
  const diff = Date.now() - new Date(isoStr).getTime()
  if (diff < 0) return '刚刚'
  const minutes = Math.floor(diff / 60000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes} 分钟前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours} 小时前`
  const days = Math.floor(hours / 24)
  if (days < 30) return `${days} 天前`
  const months = Math.floor(days / 30)
  if (months < 12) return `${months} 个月前`
  return `${Math.floor(days / 365)} 年前`
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
  loadMarketSkills()
  loadFavoriteIds()
  loadMarketSkillOverview()
  loadPage(1)
})
</script>

<style scoped>
.market-page {
  width: 100%;
  max-width: 1280px;
  margin: 0 auto;
  padding: var(--space-lg) var(--space-xl);
  display: flex;
  flex-direction: column;
  gap: var(--space-xl);
  box-sizing: border-box;
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

/* === ⑤ 全部提示词 === */
.market-search { display: flex; align-items: center; gap: var(--space-sm); }
.market-search-input {
  width: 100%;
  min-width: 240px;
  max-width: 360px;
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
.market-search-btn {
  flex-shrink: 0;
  white-space: nowrap;
  height: 40px;
  padding: 0 var(--space-md);
  border: 0;
  border-radius: var(--radius-full);
  background: var(--color-primary);
  color: #fff;
  font-size: var(--font-body);
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}
.market-search-btn:hover { background: var(--color-primary-hover); }

.market-pagination {
  display: flex;
  justify-content: flex-end;
  padding-top: var(--space-lg);
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

.market-card-meta-row {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  flex-wrap: wrap;
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
.market-card-creator-name {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 120px;
}
.market-card-scope-inline {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}
.market-card-tag-compact {
  font-size: var(--font-caption);
  color: var(--color-primary);
  background: var(--color-primary-light);
  padding: 1px 6px;
  border-radius: var(--radius-md);
  white-space: nowrap;
}
.market-card-tag-more {
  font-size: var(--font-caption);
  color: var(--color-text-placeholder);
  white-space: nowrap;
}
.market-card-mine-compact {
  font-size: var(--font-caption);
  color: #07c160;
  background: rgba(7, 193, 96, 0.08);
  padding: 1px 6px;
  border-radius: var(--radius-md);
  font-weight: 600;
  white-space: nowrap;
}
.market-card-extra-row {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
  flex-wrap: wrap;
  font-size: var(--font-caption);
  color: var(--color-text-placeholder);
}
.market-card-published {
  font-size: var(--font-caption);
  color: var(--color-text-placeholder);
}
.market-card-extra-dot {
  opacity: 0.6;
}
.market-card-actions {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  margin-top: auto;
  padding-top: var(--space-sm);
}
.market-card-use {
  background: transparent;
  color: var(--color-primary);
  border: 1px solid var(--color-primary);
  border-radius: var(--radius-lg);
  height: 32px;
  padding: 0 12px;
  font-size: var(--font-body);
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
}
.market-card-use:hover {
  background: var(--color-primary);
  color: #fff;
}
.market-card-fav {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 0;
  border-radius: var(--radius-lg);
  font-size: var(--font-body);
  color: var(--color-text-placeholder);
  cursor: pointer;
}
.market-card-fav.active {
  background: transparent;
  color: var(--color-primary);
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
body[data-theme="dark"] .market-card-creator { color: var(--color-text-regular); }
body[data-theme="dark"] .market-card-creator-avatar {
  background: rgba(255, 36, 66, 0.12);
  color: #ff6b81;
}
body[data-theme="dark"] .market-card-published { color: #a6a6a6; }
body[data-theme="dark"] .market-card-tag-compact {
  background: rgba(255, 36, 66, 0.12);
  color: #ff6b81;
}
body[data-theme="dark"] .market-card-mine-compact {
  background: rgba(7, 193, 96, 0.12);
  color: #07c160;
}
body[data-theme="dark"] .market-card-fav {
  background: transparent;
  border-color: transparent;
}
body[data-theme="dark"] .market-card-fav.active {
  background: transparent;
  color: #ff6b81;
}
body[data-theme="dark"] .market-featured-card {
  border-color: #303030;
}
body[data-theme="dark"] .market-featured-badge {
  background: var(--color-primary);
}

/* === 响应式 ≤768px === */
@media (max-width: 768px) {
  .market-page {
    padding: var(--space-md);
    gap: var(--space-lg);
  }
  .market-banner {
    grid-template-columns: 1fr;
    padding: var(--space-md);
  }
  .market-banner-stats {
    display: flex;
    overflow-x: auto;
    gap: var(--space-sm);
    padding-bottom: var(--space-sm);
    scrollbar-width: none;
    -webkit-mask-image: linear-gradient(to right, #000 0, #000 calc(100% - 16px), transparent 100%);
            mask-image: linear-gradient(to right, #000 0, #000 calc(100% - 16px), transparent 100%);
  }
  .market-banner-stats::-webkit-scrollbar { display: none; }
  .market-banner-stat {
    flex: 0 0 120px;
    min-width: 120px;
  }
  .market-upload-card {
    grid-template-columns: auto 1fr;
    padding: var(--space-md);
  }
  .market-upload-cta { grid-column: 1 / -1; width: 100%; margin-top: var(--space-sm); }
  .market-featured-rail { grid-template-columns: 1fr; }
  .market-creator-row {
    grid-template-columns: auto auto 1fr;
    gap: var(--space-sm);
    padding: var(--space-sm) var(--space-md);
  }
  .market-creator-earning {
    grid-column: 1 / -1;
    display: flex;
    align-items: baseline;
    gap: var(--space-xs);
    text-align: left;
  }
  .market-creator-amount-label { margin-top: 0; }
  .market-tabs {
    flex-wrap: nowrap;
    overflow-x: auto;
    max-width: 100%;
    scrollbar-width: none;
  }
  .market-tabs::-webkit-scrollbar { display: none; }
  .market-tab { flex-shrink: 0; }
  .market-grid { grid-template-columns: 1fr; }
  .market-section-head { flex-wrap: wrap; gap: var(--space-sm); }
  .market-search-input { min-width: 200px; }
}

/* === ④ 收益潜力榜（排行榜式列表） === */
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

.market-creators-list {
  display: flex;
  flex-direction: column;
  background: var(--color-bg-card);
  border-radius: var(--radius-xl);
  overflow: hidden;
  box-shadow: var(--shadow-sm2);
}

.market-creator-row {
  display: grid;
  grid-template-columns: auto auto 1fr auto;
  align-items: center;
  gap: var(--space-md);
  padding: var(--space-md) var(--space-lg);
  cursor: pointer;
  transition: background 0.2s;
  border-bottom: 1px solid var(--color-border-light);
}
.market-creator-row:last-child { border-bottom: 0; }
.market-creator-row:hover { background: var(--color-primary-light); }

.market-creator-rank {
  font-size: var(--font-h3);
  font-weight: 700;
  color: var(--color-text-placeholder);
  min-width: 44px;
  font-variant-numeric: tabular-nums;
}
.market-creator-rank.top3 { color: var(--color-primary); }

.market-creator-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--color-primary-light);
  color: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-body);
  font-weight: 700;
}

.market-creator-info { min-width: 0; }
.market-creator-name {
  font-size: var(--font-body);
  font-weight: 700;
  color: var(--color-text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.market-creator-best {
  font-size: var(--font-small);
  color: var(--color-text-secondary);
  margin-top: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.market-creator-earning {
  text-align: right;
  font-variant-numeric: tabular-nums;
}
.market-creator-amount {
  font-size: var(--font-h3);
  font-weight: 700;
  color: var(--color-primary);
  line-height: 1.2;
}
.market-creator-amount-label {
  font-size: var(--font-caption);
  color: var(--color-text-secondary);
  margin-top: 2px;
}

/* ④ 暗色 */
body[data-theme="dark"] .market-creators-list { background: #1f1f1f; }
body[data-theme="dark"] .market-creator-row { border-bottom-color: #303030; }
body[data-theme="dark"] .market-creator-row:hover { background: rgba(255, 36, 66, 0.10); }
body[data-theme="dark"] .market-creator-name { color: var(--color-text-primary); }
body[data-theme="dark"] .market-creator-avatar {
  background: rgba(255, 36, 66, 0.12);
  color: #ff6b81;
}
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
  height: 96px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-placeholder);
  font-size: var(--font-body);
  background: var(--color-bg-card);
  border-radius: var(--radius-xl);
}
.market-featured-rail {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: var(--space-sm);
}
.market-featured-card {
  position: relative;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-lg);
  padding: var(--space-md) var(--space-lg);
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;
}
.market-featured-badge {
  position: absolute;
  top: 0;
  right: 0;
  font-size: 10px;
  font-weight: 600;
  color: #fff;
  background: var(--color-primary);
  padding: 2px 8px;
  border-radius: 0 var(--radius-lg) 0 var(--radius-md);
  line-height: 1.4;
  z-index: 1;
}
.market-featured-card:hover {
  border-color: var(--color-primary);
  background: var(--color-primary-light);
}
.market-featured-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-sm);
}
.market-featured-name {
  font-size: var(--font-body);
  font-weight: 700;
  color: var(--color-text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
}
.market-featured-uses {
  font-size: var(--font-h3);
  font-weight: 700;
  color: var(--color-primary);
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}
.market-featured-meta {
  font-size: var(--font-small);
  color: var(--color-text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
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

</style>

<style>
/* ===== 创作者详情 modal ===== */
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
body[data-theme="dark"] .style-market-rules-footer {
  border-top-color: #303030;
  color: #a6a6a6;
}
body[data-theme="dark"] .style-market-rule-highlight { color: #ff6b81; }

.creator-modal .ant-modal-body {
  height: 520px;
  padding: 20px 24px 24px;
}
</style>
