<template>
  <div class="market-page">
    <!-- ① 平台 Banner 区 -->
    <section class="market-banner">
      <div class="market-banner-text">
        <div class="market-banner-title-wrap">
          <h1 class="market-banner-title">爱创作 · 提示词市场</h1>
          <a-tooltip
            placement="top"
            :mouse-enter-delay="0.1"
            :trigger="['hover', 'click']"
            overlay-class-name="market-prompt-intro-tooltip"
          >
            <template #title>
              <div class="market-prompt-intro-tooltip-content">
                <div class="market-prompt-intro-tooltip-title">什么是提示词？</div>
                <ul class="market-prompt-intro-tooltip-list">
                  <li><b>提示词</b>是你写给 AI 的指令，用来描述你想要的选题、风格、结构、语气等。</li>
                  <li>使用优质提示词可以让 AI 一次性生成更贴合你需求的文章，减少反复修改。</li>
                  <li>你也可以把自己的提示词上架到市场，他人每次使用你都能获得创作币收益。</li>
                </ul>
              </div>
            </template>
            <QuestionCircleOutlined class="market-banner-help-icon" />
          </a-tooltip>
        </div>
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
    <div class="market-content-wrapper">
      <div class="market-main">

        <!-- ② 上传激励卡 -->
        <section class="market-upload-card" @click="goUpload">
          <div class="market-upload-icon">＋</div>
          <div class="market-upload-body">
            <div class="market-upload-title">上传你的提示词，开始赚创作币</div>
            <div class="market-upload-sub">每被他人使用一次，获得{{ formatCoinInt(pricePerUse) }}创作币的收益</div>
          </div>
          <button class="market-upload-cta" @click.stop="goUpload">立即上架</button>
        </section>

        <!-- ⑤ 全部提示词区 -->
        <section class="market-grid-section">
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
              :featured="s.featured"
              :show-avatar="false"
              clickable
              :actions="[
                { label: '使用', type: 'primary', handler: () => handleUse(s) },
                { label: isFavorite(s.id) ? '♥' : '♡', active: isFavorite(s.id), handler: () => handleToggleFavorite(s.id) },
                { label: '查看', handler: () => openStyleDetail(s) },
                { label: '模拟', visible: s.creatorId === currentUserId, handler: () => handleSimulate(s) },
                { label: '下架', visible: s.creatorId === currentUserId, handler: () => handleDelete(s) }
              ]"
              @click="openStyleDetail(s)"
            >
              <template #meta>
                <div class="skill-card__meta-row">
                  <span class="skill-card__creator">
                    <span class="skill-card__creator-avatar">
                      {{ (s.creatorName || '匿').charAt(0) }}
                    </span>
                    <span class="skill-card__creator-name">by {{ s.creatorName || '匿名用户' }}</span>
                  </span>
                  <span
                    v-if="parseScopeTags(s.scope).length || s.creatorId === currentUserId"
                    class="skill-card__scope-inline"
                  >
                    <span
                      v-for="t in parseScopeTags(s.scope).slice(0, 2)"
                      :key="t"
                      class="skill-card__tag-compact"
                    >
                      # {{ t }}
                    </span>
                    <span
                      v-if="parseScopeTags(s.scope).length > 2"
                      class="skill-card__tag-more"
                    >
                      +{{ parseScopeTags(s.scope).length - 2 }}
                    </span>
                    <span v-if="s.creatorId === currentUserId" class="skill-card__mine-compact">我的</span>
                  </span>
                </div>
              </template>
              <template #extra>
                <div class="skill-card__extra-row">
                  <span v-if="s.createdAt" class="skill-card__published">
                    发布于 {{ formatTimeAgo(s.createdAt) }}
                  </span>
                  <span class="skill-card__extra-dot" v-if="s.createdAt">·</span>
                  <span>本周 {{ s.weeklyUses }} 次</span>
                  <span class="skill-card__extra-dot">·</span>
                  <span>累计 {{ s.totalUses }} 次</span>
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
      <div class="market-sidebar">

        <!-- ③ 收益潜力榜 -->
        <section class="market-creators">
          <div class="market-section-head">
            <div class="market-section-title-wrap">
              <h2 class="market-section-title">收益潜力榜</h2>
              <a-tooltip
                placement="top"
                :mouse-enter-delay="0.1"
                :trigger="['hover', 'click']"
                overlay-class-name="market-earnings-rank-tooltip"
              >
                <template #title>
                  <div class="market-earnings-rank-tooltip-content">
                    <div class="market-earnings-rank-tooltip-title">收益潜力榜计算规则</div>
                   <ul class="market-earnings-rank-tooltip-list">
                      <li>榜单按创作者近一个月提示词被使用次数 × 单次使用收益单价计算每月收益，并按收益从高到低排序。</li>
                     <li>他人每使用一次你的提示词，你都会获得相应创作币收益。</li>
                      <li>每月结算时，榜单 Top5 会获得额外月度奖励，具体金额以平台公布为准。</li>
                    </ul>
                  </div>
                </template>
                <InfoCircleOutlined class="market-earnings-rank-icon" />
              </a-tooltip>
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
      </div>
    </div>
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
      <li>他人每使用一次你分享的提示词，你将获得 <span class="style-market-rule-highlight">{{ formatCoinInt(pricePerUse) }} 创作币</span> 奖励。</li>
      <li v-if="rewardAmounts.length">
        每月根据提示词市场 <span class="style-market-rule-highlight">收益潜力榜 Top5</span> 发放月度奖励：
        <span
          v-for="(item, idx) in rewardAmounts"
          :key="item.rank"
        >
          <span class="style-market-rule-highlight">Top{{ item.rank }} {{ formatCoinInt(item.amount) }} 币</span>
          <span v-if="idx < rewardAmounts.length - 1">、</span>
        </span>。
      </li>
      <li v-else>
        每月根据提示词市场 <span class="style-market-rule-highlight">收益潜力榜 Top5</span> 发放月度奖励，具体金额以平台公布为准。
      </li>
      <li>月度奖励 <span class="style-market-rule-highlight">每月结算一次</span>，结算后当月使用次数与月度收益清零并重新累计。</li>
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
          <div class="creator-modal-stat-value">{{ formatCoins(creatorTotalEarnings) }}</div>
          <div class="creator-modal-stat-label">累计币 (按使用×单价)</div>
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
    :is-favorite="isFavorite(selectedStyle.id)"
    @update:visible="onStyleDetailVisibleChange"
    @use="handleUse(selectedStyle)"
    @toggle-favorite="handleToggleFavorite(selectedStyle.id)"
  >
    <template #footer-actions>
      <button
        class="skill-detail-btn-fav"
        :class="{ active: isFavorite(selectedStyle.id) }"
        @click="handleToggleFavorite(selectedStyle.id)"
      >
        {{ isFavorite(selectedStyle.id) ? '♥ 已收藏' : '♡ 收藏' }}
      </button>
      <button class="skill-detail-btn-use" @click="handleUse(selectedStyle)">使用</button>
      <button
        v-if="String(selectedStyle?.creatorId) === String(currentUserId)"
        class="skill-detail-btn-fav"
        @click="handleDelete(selectedStyle); onStyleDetailVisibleChange(false)"
      >下架</button>
    </template>
  </SkillDetailModal>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { InfoCircleOutlined, QuestionCircleOutlined } from '@ant-design/icons-vue'
import {
  marketSkills,
  marketStats,
  topCreators,
  pricePerUse,
  useMarketSkill,
  simulateExternalUse,
  toggleFavorite,
  isFavorite,
  loadMarketSkills,
  loadMarketSkillOverview,
  loadMarketSkillPage,
  loadFavoriteIds,
  loadPricePerUse,
  unpublishSkill
} from '@/composables/useSkillMarket.js'
import { getMarketSkillMonthlyRewardConfig } from '@/api/marketSkill.js'
import SkillCard from '@/components/SkillCard.vue'
import SkillDetailModal from '@/components/SkillDetailModal.vue'

const router = useRouter()
const currentUserId = ref(localStorage.getItem('aichuangzuo_user_id') || '')
const rulesVisible = ref(false)

const formatCoins = (n) => Number(n || 0).toFixed(2)
const formatCoinInt = (n) => String(Math.round(Number(n || 0)))
const formatUses = (n) => Number(n || 0).toLocaleString()

const rewardConfig = ref(null)
const rewardAmounts = computed(() => {
  const cfg = rewardConfig.value
  if (!cfg || !cfg.enabled) return []
  return [
    { rank: 1, amount: cfg.firstAmount },
    { rank: 2, amount: cfg.secondAmount },
    { rank: 3, amount: cfg.thirdAmount },
    { rank: 4, amount: cfg.fourthAmount },
    { rank: 5, amount: cfg.fifthAmount }
  ]
})

const loadRewardConfig = async () => {
  try {
    rewardConfig.value = await getMarketSkillMonthlyRewardConfig()
  } catch (e) {
    console.warn('[loadRewardConfig]', e?.message || '加载失败')
    rewardConfig.value = null
  }
}

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
  const price = Number(pricePerUse.value || 2)
  return creatorStyles.value.reduce((sum, s) => {
    return sum + (s.totalUses || 0) * price
  }, 0)
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

const handleDelete = (s) => {
  Modal.confirm({
    title: '下架提示词',
    content: `确定要下架已发布的提示词「${s.name}」吗？下架后其他人将无法在市场中看到该提示词。`,
    okText: '下架',
    cancelText: '取消',
    okButtonProps: { danger: true },
    centered: true,
    onOk: async () => {
      try {
        await unpublishSkill(s.id)
        message.success('提示词已下架')
        loadPage(page.value)
      } catch {
        // composable 内已输出错误
      }
    }
  })
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

const searchQuery = ref('')
const activeTab = ref('all')

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
  loadRewardConfig()
  loadPricePerUse()
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

.market-banner-title-wrap {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  margin-bottom: var(--space-sm);
}
.market-banner-title {
  font-size: var(--font-h1);
  font-weight: 700;
  color: var(--color-primary);
  margin: 0;
  letter-spacing: -0.5px;
}
.market-banner-help-icon {
  color: var(--color-text-secondary);
  font-size: 18px;
  cursor: pointer;
  transition: color 0.2s;
}
.market-banner-help-icon:hover { color: var(--color-primary); }

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

.market-pagination :deep(.ant-pagination) {
  color: var(--color-text-secondary);
}
.market-pagination :deep(.ant-pagination-item) {
  background: var(--color-bg-card);
  border-color: var(--color-border-default);
  border-radius: var(--radius-md);
  transition: all 0.2s;
}
.market-pagination :deep(.ant-pagination-item a) {
  color: var(--color-text-secondary);
}
.market-pagination :deep(.ant-pagination-item:hover) {
  border-color: var(--color-primary);
}
.market-pagination :deep(.ant-pagination-item:hover a) {
  color: var(--color-primary);
}
.market-pagination :deep(.ant-pagination-item-active) {
  background: var(--color-primary);
  border-color: var(--color-primary);
}
.market-pagination :deep(.ant-pagination-item-active a) {
  color: #fff;
}
.market-pagination :deep(.ant-pagination-prev .ant-pagination-item-link,
                         .ant-pagination-next .ant-pagination-item-link) {
  background: var(--color-bg-card);
  border-color: var(--color-border-default);
  color: var(--color-text-secondary);
  border-radius: var(--radius-md);
  transition: all 0.2s;
}
.market-pagination :deep(.ant-pagination-prev:hover .ant-pagination-item-link,
                         .ant-pagination-next:hover .ant-pagination-item-link) {
  border-color: var(--color-primary);
  color: var(--color-primary);
}
.market-pagination :deep(.ant-pagination-disabled .ant-pagination-item-link,
                         .ant-pagination-disabled:hover .ant-pagination-item-link) {
  color: var(--color-text-placeholder);
  border-color: var(--color-border-default);
  cursor: not-allowed;
}
.market-pagination :deep(.ant-pagination-jump-prev .ant-pagination-item-container .ant-pagination-item-link-icon,
                         .ant-pagination-jump-next .ant-pagination-item-container .ant-pagination-item-link-icon) {
  color: var(--color-primary);
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

/* ⑤ 暗色 */
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

body[data-theme="dark"] .market-pagination :deep(.ant-pagination-item) {
  background: #1f1f1f;
  border-color: #303030;
}
body[data-theme="dark"] .market-pagination :deep(.ant-pagination-item a) {
  color: #a6a6a6;
}
body[data-theme="dark"] .market-pagination :deep(.ant-pagination-item:hover) {
  border-color: var(--color-primary);
}
body[data-theme="dark"] .market-pagination :deep(.ant-pagination-item:hover a) {
  color: var(--color-primary);
}
body[data-theme="dark"] .market-pagination :deep(.ant-pagination-item-active) {
  background: var(--color-primary);
  border-color: var(--color-primary);
}
body[data-theme="dark"] .market-pagination :deep(.ant-pagination-item-active a) {
  color: #fff;
}
body[data-theme="dark"] .market-pagination :deep(.ant-pagination-prev .ant-pagination-item-link) {
  background: #1f1f1f;
  border-color: #303030;
  color: #a6a6a6;
}
body[data-theme="dark"] .market-pagination :deep(.ant-pagination-next .ant-pagination-item-link) {
  background: #1f1f1f;
  border-color: #303030;
  color: #a6a6a6;
}
body[data-theme="dark"] .market-pagination :deep(.ant-pagination-prev:hover .ant-pagination-item-link) {
  border-color: var(--color-primary);
  color: var(--color-primary);
}
body[data-theme="dark"] .market-pagination :deep(.ant-pagination-next:hover .ant-pagination-item-link) {
  border-color: var(--color-primary);
  color: var(--color-primary);
}
body[data-theme="dark"] .market-pagination :deep(.ant-pagination-disabled .ant-pagination-item-link) {
  color: #595959;
  border-color: #303030;
}
body[data-theme="dark"] .market-pagination :deep(.ant-pagination-disabled:hover .ant-pagination-item-link) {
  color: #595959;
  border-color: #303030;
}

/* === 左右布局 === */
.market-content-wrapper {
  display: grid;
  grid-template-columns: 1fr 360px;
  gap: var(--space-xl);
  align-items: start;
}

.market-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-xl);
}

.market-sidebar {
  display: flex;
  flex-direction: column;
  gap: var(--space-xl);
  position: sticky;
  top: var(--space-lg);
  max-height: calc(100vh - 32px);
  overflow-y: auto;
  padding-right: var(--space-sm);
}

.market-sidebar::-webkit-scrollbar {
  width: 4px;
}

.market-sidebar::-webkit-scrollbar-thumb {
  background: var(--color-border-default);
  border-radius: 2px;
}

@media (max-width: 1024px) {
  .market-content-wrapper {
    grid-template-columns: 1fr;
  }
  .market-sidebar {
    position: static;
    max-height: none;
    overflow-y: visible;
    padding-right: 0;
  }
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

/* === ③ 收益潜力榜 === */
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
.market-earnings-rank-icon {
  color: var(--color-text-secondary);
  font-size: 16px;
  cursor: pointer;
  transition: color 0.2s;
}
.market-earnings-rank-icon:hover { color: var(--color-primary); }
.market-section-link {
  background: transparent;
  border: 0;
  color: var(--color-primary);
  cursor: pointer;
  font-size: var(--font-body);
  font-weight: 500;
}
.market-section-link:hover { color: var(--color-primary-hover); }

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
