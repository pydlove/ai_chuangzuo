<template>
  <div class="market-page">
    <!-- 手机端 header：参考约稿中心 -->
    <div class="market-banner-mobile-header">
      <div class="market-banner-mobile-header__left">
        <div class="market-banner-mobile-header__donuts" aria-hidden="true">
          <span></span>
          <span></span>
        </div>
        <img
          src="/assets/images/提示词市场logo-v1.png"
          alt="提示词市场"
          class="market-banner-mobile-header__logo"
        />
        <p class="market-banner-mobile-header__subtitle">
          官方运营 · 精选创作者提示词 · 使用即获收益分成
        </p>
      </div>
      <img
        src="/assets/images/提示词市场宣传-v1.png"
        alt=""
        class="market-banner-mobile-header__illustration"
      />
    </div>

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

      <div class="market-banner-mobile-stats">
        <div class="market-banner-mobile-stat">
          <div class="market-banner-mobile-stat__text">
            <span class="market-banner-mobile-stat__label">已上架款</span>
            <strong>{{ marketStats.approvedCount }}</strong>
          </div>
          <img
            src="/assets/images/已上架-v1.png"
            alt=""
            class="market-banner-mobile-stat__icon-img"
          />
        </div>
        <div class="market-banner-mobile-stat">
          <div class="market-banner-mobile-stat__text">
            <span class="market-banner-mobile-stat__label">累计使用次</span>
            <strong>{{ formatUses(marketStats.totalUses) }}</strong>
          </div>
          <img
            src="/assets/images/累计使用-v1.png"
            alt=""
            class="market-banner-mobile-stat__icon-img"
          />
        </div>
        <div class="market-banner-mobile-stat">
          <div class="market-banner-mobile-stat__text">
            <span class="market-banner-mobile-stat__label">累计发放币</span>
            <strong>{{ formatCoins(marketStats.totalEarnings) }}</strong>
          </div>
          <img
            src="/assets/images/累计发布-v1.png"
            alt=""
            class="market-banner-mobile-stat__icon-img"
          />
        </div>
      </div>
    </section>
    <div class="market-body">
      <div class="market-content-wrapper">
        <div class="market-main">

          <!-- ② 上传激励卡 -->
        <section class="market-upload-card" @click="goUpload">
          <div class="market-upload-icon-wrap">
            <img
              src="/assets/images/立即上架-v1.png"
              alt=""
              class="market-upload-icon-img"
            />
          </div>
          <div class="market-upload-body">
            <div class="market-upload-title">上传你的提示词，开始赚创作币</div>
            <div class="market-upload-sub">每被他人使用一次，获得{{ formatCoinInt(pricePerUse) }}创作币的收益</div>
          </div>
          <div class="market-upload-arrow">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="9 18 15 12 9 6"/>
            </svg>
          </div>
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
          <EmptyState v-else-if="pagedStyles.length === 0" title="暂无已上架提示词" compact size="sm" />
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
                { label: isFavorite(s.id) ? '取消收藏' : '收藏', type: isFavorite(s.id) ? undefined : 'primary', handler: () => handleFavoriteClick(s) },
                { label: '查看', handler: () => openStyleDetail(s) },
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
                  <span v-if="s.approvedAt || s.createdAt" class="skill-card__published">
                    发布于 {{ formatTimeAgo(s.approvedAt || s.createdAt) }}
                  </span>
                  <span class="skill-card__extra-dot" v-if="s.approvedAt || s.createdAt">·</span>
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
      <li>月度奖励 <span class="style-market-rule-highlight">每月结算一次</span>，结算后当月使用次数与月度收益清零并重新累计。</li>
      <li>使用他人分享的提示词 <span class="style-market-rule-highlight">无需支付创作币</span>，创作者仍可正常获得收益。</li>
      <li>如发现违规刷量行为，平台有权 <span class="style-market-rule-highlight">取消相关收益并下架提示词</span>。</li>
      <li>使用提示词市场上的提示词生成文章并成功后，该提示词的累计使用次数会增加，创作者可获得对应创作币收益。</li>
      <li>使用自己创建或学习获得的提示词生成文章并成功后，仅累计该提示词的使用次数，不产生收益。</li>
      <li>生成失败或手动停止的任务不计入使用次数与收益。</li>
    </ol>
    <div class="style-market-rules-footer">* 活动最终解释权归平台所有。</div>
  </a-modal>

  <!-- 收藏提示弹框 -->
  <a-modal
    v-model:open="favoriteTipVisible"
    class="favorite-tip-modal"
    title="收藏成功"
    :footer="null"
    :width="400"
    centered
    @ok="favoriteTipVisible = false"
  >
    <p class="favorite-tip-content">
      可以在推荐创作和自由创作中-提示词-收藏页，选择收藏的提示词，好的提示词可以让文章生成的质量更高。
    </p>
    <div class="favorite-tip-footer">
      <button class="favorite-tip-btn" @click="favoriteTipVisible = false">我知道了</button>
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
import { QuestionCircleOutlined } from '@ant-design/icons-vue'
import {
  marketSkills,
  marketStats,
  pricePerUse,
  toggleFavorite,
  isFavorite,
  loadMarketSkills,
  loadMarketSkillOverview,
  loadMarketSkillPage,
  loadFavoriteSkills,
  loadPricePerUse,
  unpublishSkill
} from '@/composables/useSkillMarket.js'
import SkillCard from '@/components/SkillCard.vue'
import SkillDetailModal from '@/components/SkillDetailModal.vue'

const router = useRouter()
const currentUserId = ref(localStorage.getItem('aichuangzuo_user_id') || '')
const rulesVisible = ref(false)
const favoriteTipVisible = ref(false)

const handleFavoriteClick = async (s) => {
  const wasFavorite = isFavorite(s.id)
  await toggleFavorite(s.id)
  if (!wasFavorite) {
    favoriteTipVisible.value = true
  }
}

const formatCoins = (n) => Number(n || 0).toFixed(2)
const formatCoinInt = (n) => String(Math.round(Number(n || 0)))
const formatUses = (n) => Number(n || 0).toLocaleString()

const goUpload = () => {
  router.push('/console/skills')
}

const handleUse = (s) => {
  router.push(`/console/create?marketSkillId=${s.id}`)
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
      } catch (err) {
        message.error(err?.message || '下架失败，请重试')
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

onMounted(() => {
  loadMarketSkills()
  loadFavoriteSkills()
  loadMarketSkillOverview()
  loadPricePerUse()
  loadPage(1)
})
</script>

<style scoped>
.market-page {
  width: 100%;
  max-width: 1280px;
  margin: 0 auto;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-xl);
  box-sizing: border-box;
  background: #fafafa;
  min-height: 100%;
}

.market-body {
  padding: var(--space-lg) var(--space-xl);
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

/* 手机端 header：参考约稿中心 */
.market-banner-mobile-header,
.market-banner-mobile-stats {
  display: none;
}

.market-banner-mobile-header {
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  background: linear-gradient(180deg, #FFF0F3 0%, #fff 100%);
  border-radius: 0;
  padding: 20px 16px;
  border-bottom: 1px solid #f0f0f0;
}
.market-banner-mobile-header__left {
  display: flex;
  flex-direction: column;
  gap: 10px;
  flex: 1;
  min-width: 0;
  position: relative;
  z-index: 1;
}
.market-banner-mobile-header__logo {
  height: 48px;
  width: auto;
  max-width: 150px;
  object-fit: contain;
  border-radius: 10px;
  position: relative;
  z-index: 1;
}
.market-banner-mobile-header__subtitle {
  font-size: 13px;
  color: #8c8c8c;
  line-height: 1.5;
  margin: 0;
  padding-left: 2px;
  position: relative;
  z-index: 1;
}
.market-banner-mobile-header__donuts {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 0;
}
.market-banner-mobile-header__donuts span {
  position: absolute;
  border-radius: 50%;
  background: radial-gradient(circle, transparent 42%, rgba(255, 36, 66, 0.12) 43%, rgba(255, 36, 66, 0.12) 66%, transparent 67%);
}
.market-banner-mobile-header__donuts span:nth-child(1) {
  width: 56px;
  height: 56px;
  top: -10px;
  left: 95px;
}
.market-banner-mobile-header__donuts span:nth-child(2) {
  width: 92px;
  height: 92px;
  bottom: -30px;
  left: -22px;
}
.market-banner-mobile-header__illustration {
  height: 110px;
  width: auto;
  object-fit: contain;
  flex-shrink: 0;
}

.market-banner-mobile-stats {
  display: none;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  padding-top: 16px;
  width: 100%;
}
.market-banner-mobile-stat {
  background: linear-gradient(180deg, #FFF8FA 0%, #fff 100%);
  border-radius: 16px;
  padding: 10px;
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.04);
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: center;
  gap: 8px;
  overflow: hidden;
}
.market-banner-mobile-stat__text {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}
.market-banner-mobile-stat__label {
  font-size: 11px;
  color: #8c8c8c;
  white-space: nowrap;
}
.market-banner-mobile-stat strong {
  font-size: 16px;
  font-weight: 700;
  color: #1a1a1a;
  line-height: 1.1;
  white-space: nowrap;
}
.market-banner-mobile-stat__icon-img {
  width: 32px;
  height: 32px;
  object-fit: contain;
  border-radius: 8px;
  justify-self: end;
  align-self: center;
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
  gap: var(--space-xl);
}

/* PC 端提示词市场卡片样式增强 */
.market-grid :deep(.skill-card) {
  padding: 24px;
  border-radius: 20px;
  border-color: var(--color-border-light);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
}

.market-grid :deep(.skill-card:hover) {
  transform: translateY(-4px);
  box-shadow: 0 14px 32px rgba(0, 0, 0, 0.08);
  border-color: rgba(255, 36, 66, 0.25);
}

.market-grid :deep(.skill-card__head) {
  margin-bottom: 18px;
}

.market-grid :deep(.skill-card__title) {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: -0.2px;
}

.market-grid :deep(.skill-card__prompt) {
  font-size: 14px;
  line-height: 1.75;
  color: #595959;
  margin-bottom: 18px;
  -webkit-line-clamp: 3;
}

.market-grid :deep(.skill-card__extra) {
  margin-bottom: 14px;
}

.market-grid :deep(.skill-card__extra-row) {
  font-size: 12px;
  color: #8c8c8c;
  gap: 8px;
}

.market-grid :deep(.skill-card__actions) {
  width: 100%;
  justify-content: flex-end;
  gap: 6px;
  margin-top: 0;
}

.market-grid :deep(.skill-card__action-btn) {
  padding: 7px 14px;
  border-radius: 10px;
  font-size: 13px;
  color: #595959;
  background: #f5f5f5;
  transition: all 0.2s;
}

.market-grid :deep(.skill-card__action-btn:hover) {
  background: var(--color-primary-bg);
  color: var(--color-primary);
}

.market-grid :deep(.skill-card__action-btn--primary) {
  background: var(--color-primary);
  color: #fff;
  border: none;
  font-weight: 600;
}

.market-grid :deep(.skill-card__action-btn--primary:hover) {
  background: var(--color-primary-hover);
  color: #fff;
}

.market-grid :deep(.skill-card__action-btn--active) {
  color: var(--color-primary);
  background: var(--color-primary-bg);
}

.market-grid :deep(.skill-card__featured-badge) {
  border-radius: 0 20px 0 12px;
  padding: 3px 10px;
  font-size: 11px;
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
body[data-theme="dark"] .market-tabs { background: #262626; }
body[data-theme="dark"] .market-tab { color: #a6a6a6; }
body[data-theme="dark"] .market-tab.active {
  background: #1f1f1f;
  color: var(--color-primary);
  box-shadow: none;
}
body[data-theme="dark"] .market-empty { background: #1f1f1f; color: #8c8c8c; }

body[data-theme="dark"] .market-banner-mobile-header {
  background: linear-gradient(180deg, #2a1f22 0%, #141414 100%);
  border-bottom-color: #2a2a2a;
}
body[data-theme="dark"] .market-banner-mobile-header__subtitle { color: #a6a6a6; }
body[data-theme="dark"] .market-banner-mobile-header__donuts span { background: radial-gradient(circle, transparent 42%, rgba(255, 36, 66, 0.22) 43%, rgba(255, 36, 66, 0.22) 66%, transparent 67%); }
body[data-theme="dark"] .market-banner-mobile-stats .market-banner-mobile-stat { background: linear-gradient(180deg, #2a1f22 0%, #1f1f1f 100%); }
body[data-theme="dark"] .market-banner-mobile-stat__label { color: #a6a6a6; }
body[data-theme="dark"] .market-banner-mobile-stat strong { color: #f5f5f5; }

body[data-theme="dark"] .market-grid :deep(.skill-card) {
  background: #1f1f1f;
  border-color: #303030;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
}

body[data-theme="dark"] .market-grid :deep(.skill-card:hover) {
  border-color: rgba(255, 36, 66, 0.4);
  box-shadow: 0 14px 32px rgba(0, 0, 0, 0.35);
}

body[data-theme="dark"] .market-grid :deep(.skill-card__title) { color: #f0f0f0; }
body[data-theme="dark"] .market-grid :deep(.skill-card__prompt) { color: #a6a6a6; }
body[data-theme="dark"] .market-grid :deep(.skill-card__extra-row) { color: #8c8c8c; }

body[data-theme="dark"] .market-grid :deep(.skill-card__action-btn) {
  background: #2a2a2a;
  color: #a6a6a6;
}

body[data-theme="dark"] .market-grid :deep(.skill-card__action-btn:hover) {
  background: rgba(255, 36, 66, 0.12);
  color: #ff6b81;
}

body[data-theme="dark"] .market-grid :deep(.skill-card__action-btn--primary) {
  background: var(--color-primary);
  color: #fff;
}

body[data-theme="dark"] .market-grid :deep(.skill-card__action-btn--primary:hover) {
  background: #ff4d6a;
  color: #fff;
}

body[data-theme="dark"] .market-grid :deep(.skill-card__action-btn--active) {
  background: rgba(255, 36, 66, 0.12);
  color: #ff6b81;
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
  grid-template-columns: 1fr;
  gap: var(--space-xl);
  align-items: start;
}

.market-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-xl);
}


/* === ③ 全部提示词区标题 === */
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
.market-section-sub {
  font-size: var(--font-body);
  color: var(--color-text-secondary);
  font-weight: 400;
}

/* === ② 上传激励卡 === */
.market-upload-card {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: var(--space-lg);
  background: linear-gradient(135deg, #FFF8FA 0%, #fff 100%);
  border-radius: var(--radius-xl);
  padding: var(--space-lg) var(--space-xl);
  cursor: pointer;
  transition: box-shadow 0.2s, transform 0.2s;
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.04);
}
.market-upload-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}
.market-upload-icon-wrap {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: #FFF0F3;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
}
.market-upload-icon-img {
  width: 36px;
  height: 36px;
  object-fit: contain;
  border-radius: 8px;
  flex-shrink: 0;
}
.market-upload-body { min-width: 0; }
.market-upload-title {
  font-size: 17px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin-bottom: var(--space-xs);
}
.market-upload-sub {
  font-size: 13px;
  color: var(--color-text-secondary);
}
.market-upload-arrow {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #fff;
  color: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(255, 36, 66, 0.12);
  flex-shrink: 0;
}
.market-upload-arrow svg {
  width: 18px;
  height: 18px;
}

/* ② 暗色 */
body[data-theme="dark"] .market-upload-card { background: linear-gradient(135deg, #2a1f22 0%, #1f1f1f 100%); }
body[data-theme="dark"] .market-upload-icon-wrap { background: #2a1f22; }
body[data-theme="dark"] .market-upload-title { color: var(--color-text-primary); }
body[data-theme="dark"] .market-upload-sub { color: var(--color-text-secondary); }
body[data-theme="dark"] .market-upload-arrow {
  background: #1f1f1f;
  color: #ff6b81;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

/* ① 暗色 */
body[data-theme="dark"] .market-page { background: #141414; }
body[data-theme="dark"] .market-banner { background: #1f1f1f; }
body[data-theme="dark"] .market-banner-title { color: #ff6b81; }
body[data-theme="dark"] .market-banner-sub { color: var(--color-text-secondary); }
body[data-theme="dark"] .market-banner-stat { background: #141414; }
body[data-theme="dark"] .market-banner-stat-num { color: var(--color-text-primary); }
body[data-theme="dark"] .market-banner-stat-label { color: var(--color-text-secondary); }

/* === 响应式 ≤768px === */
@media (max-width: 768px) {
  .market-page {
    padding: 0;
    gap: 0;
  }
  .market-body {
    padding: 16px;
  }
  .market-banner {
    display: flex;
    flex-direction: column;
    padding: 0 16px;
    background: transparent;
    border-radius: 0;
    box-shadow: none;
    gap: 0;
  }
  .market-banner-text,
  .market-banner-stats {
    display: none;
  }
  .market-banner-mobile-header,
  .market-banner-mobile-stats {
    display: flex;
  }
  .market-banner-mobile-stats {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 8px;
    padding-top: 16px;
    width: 100%;
  }
  .market-banner-mobile-stat {
    padding: 16px 12px;
    min-height: 88px;
    align-items: center;
  }
  .market-banner-mobile-stat__icon-img {
    display: none;
  }
  .market-banner-mobile-stat strong {
    font-size: 20px;
  }
  .market-banner-mobile-stat__label {
    font-size: 12px;
  }

  .market-content-wrapper {
    gap: 16px;
  }

  .market-upload-card {
    grid-template-columns: auto minmax(0, 1fr) auto;
    padding: 10px 4px;
    border-radius: 18px;
    gap: 8px;
    min-height: 72px;
  }
  .market-upload-body {
    min-width: 0;
    overflow: hidden;
  }
  .market-upload-icon-wrap {
    width: 48px;
    height: 48px;
    border-radius: 10px;
  }
  .market-upload-icon-img {
    width: 38px;
    height: 38px;
  }
  .market-upload-title {
    font-size: 15px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  .market-upload-sub {
    font-size: 12px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  .market-upload-arrow {
    width: 24px;
    height: 24px;
  }
  .market-upload-arrow svg {
    width: 14px;
    height: 14px;
  }

  .market-section-head {
    flex-wrap: wrap;
    gap: 10px;
    margin-bottom: 12px;
  }
  .market-section-title {
    font-size: 17px;
  }
  .market-section-sub {
    font-size: 13px;
  }

  .market-search {
    width: 100%;
  }
  .market-search-input {
    flex: 1;
    min-width: 0;
    max-width: none;
    height: 40px;
    border: 1px solid var(--color-primary);
    background: var(--color-bg-card);
    box-shadow: 0 2px 8px rgba(255, 36, 66, 0.08);
  }
  .market-search-input:focus {
    border-color: var(--color-primary);
    box-shadow: 0 2px 12px rgba(255, 36, 66, 0.14);
  }
  .market-search-btn {
    height: 40px;
  }

  .market-tabs {
    position: sticky;
    top: 0;
    z-index: 10;
    flex-wrap: nowrap;
    overflow-x: auto;
    max-width: 100%;
    scrollbar-width: none;
    background: #f5f5f5;
    border-radius: 999px;
    padding: 4px;
    margin-bottom: 12px;
  }
  .market-tabs::-webkit-scrollbar { display: none; }
  .market-tab {
    flex-shrink: 0;
    padding: 7px 14px;
    border-radius: 999px;
    font-size: 13px;
  }
  .market-tab.active {
    background: #fff;
    color: var(--color-primary, #ff2442);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  }

  .market-grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .market-pagination {
    justify-content: center;
    padding-top: 12px;
  }

  .market-empty {
    padding: 48px 16px;
    background: #fff;
    border-radius: 18px;
  }
}
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

/* 收藏提示弹框 */
.favorite-tip-content {
  margin: 0;
  font-size: var(--font-body);
  color: var(--color-text-regular);
  line-height: 1.7;
}
.favorite-tip-footer {
  margin-top: var(--space-lg);
  display: flex;
  justify-content: flex-end;
}
.favorite-tip-btn {
  padding: 8px 20px;
  border: none;
  border-radius: var(--radius-md);
  background: var(--color-primary);
  color: #fff;
  font-size: var(--font-body);
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}
.favorite-tip-btn:hover { background: var(--color-primary-hover); }

body[data-theme="dark"] .favorite-tip-content { color: #a6a6a6; }
body[data-theme="dark"] .favorite-tip-btn { background: var(--color-primary); color: #fff; }
body[data-theme="dark"] .favorite-tip-btn:hover { background: var(--color-primary-hover); }

</style>
