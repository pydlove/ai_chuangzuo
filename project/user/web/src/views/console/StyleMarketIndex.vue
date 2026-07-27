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

    <!-- ③④⑤ placeholder — 后续 Task 填充 -->
    <section class="market-featured" data-tbd="task-4"></section>
    <section class="market-creators" data-tbd="task-5"></section>
    <section class="market-grid-section" data-tbd="task-6"></section>
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

/* ②③④⑤ 占位防 build break（Task 4-6 填充实际样式） */
.market-featured,
.market-creators,
.market-grid-section { min-height: 1px; }

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
