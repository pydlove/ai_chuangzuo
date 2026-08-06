<template>
  <div class="activities-page">
    <!-- Hero -->
    <section class="activities-hero">
      <div class="activities-hero__copy">
        <span class="activities-hero__eyebrow">ACTIVITY CENTER</span>
        <h1 class="activities-hero__title">活动中心</h1>
        <p class="activities-hero__subtitle">边赚边学：投稿赚币、上架赚币、学院提技能</p>
      </div>
      <div class="activities-hero__orbit" aria-hidden="true"><span></span><span></span></div>
    </section>

    <!-- 收益区：约稿 + 市场 -->
    <section class="activities-section">
      <div class="activities-section__header">
        <span class="activities-section__badge">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 1v22"/>
            <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/>
          </svg>
          可获收益
        </span>
      </div>

      <div class="activities-earnings">
        <router-link to="/console/commission" class="activity-card activity-card--commission">
          <div class="activity-card__bg" aria-hidden="true"></div>
          <div class="activity-card__icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
              <polyline points="14 2 14 8 20 8"/>
              <line x1="16" y1="13" x2="8" y2="13"/>
              <line x1="16" y1="17" x2="8" y2="17"/>
              <polyline points="10 9 9 9 8 9"/>
            </svg>
          </div>
          <div class="activity-card__body">
            <div class="activity-card__title">约稿中心</div>
            <p class="activity-card__desc">挑选任务投稿，稿件采纳后获得创作币奖励</p>
            <div class="activity-card__stats">
              <span class="activity-card__stats-num">{{ stats.activeTaskCount }}</span>
              <span class="activity-card__stats-label">个进行中的任务</span>
            </div>
          </div>
          <div class="activity-card__arrow">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="9 18 15 12 9 6"/>
            </svg>
          </div>
        </router-link>

        <router-link to="/console/skill-market" class="activity-card activity-card--market">
          <div class="activity-card__bg" aria-hidden="true"></div>
          <div class="activity-card__icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.3 2.3c-.4.5-.1 1.2.5 1.2H19"/>
              <circle cx="9" cy="21" r="1"/>
              <circle cx="20" cy="21" r="1"/>
            </svg>
          </div>
          <div class="activity-card__body">
            <div class="activity-card__title">提示词市场</div>
            <p class="activity-card__desc">发现优质提示词，上架自己的提示词赚收益</p>
            <div class="activity-card__stats">
              <span class="activity-card__stats-num">{{ stats.approvedCount }}</span>
              <span class="activity-card__stats-label">款已上架</span>
            </div>
          </div>
          <div class="activity-card__arrow">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="9 18 15 12 9 6"/>
            </svg>
          </div>
        </router-link>

        <router-link to="/console/leaderboard" class="activity-card activity-card--leaderboard">
          <div class="activity-card__bg" aria-hidden="true"></div>
          <div class="activity-card__icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M6 9H4.5a2.5 2.5 0 0 1 0-5H6"/>
              <path d="M18 9h1.5a2.5 2.5 0 0 0 0-5H18"/>
              <path d="M4 22h16"/>
              <path d="M10 14.66V17c0 .55-.47.98-.97 1.21C7.85 18.75 7 20.24 7 22"/>
              <path d="M14 14.66V17c0 .55.47.98.97 1.21C16.15 18.75 17 20.24 17 22"/>
              <path d="M18 2H6v7a6 6 0 0 0 12 0V2Z"/>
            </svg>
          </div>
          <div class="activity-card__body">
            <div class="activity-card__title">收益排行榜</div>
            <p class="activity-card__desc">查看平台创作者收益排名，了解头部变现趋势</p>
            <div class="activity-card__stats">
              <span class="activity-card__stats-label">周榜 · 月榜 · 总榜</span>
            </div>
          </div>
          <div class="activity-card__arrow">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="9 18 15 12 9 6"/>
            </svg>
          </div>
        </router-link>

        <router-link to="/console/invite" class="activity-card activity-card--invite">
          <div class="activity-card__bg" aria-hidden="true"></div>
          <div class="activity-card__icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="11" width="18" height="10" rx="1"/>
              <line x1="12" y1="11" x2="12" y2="21"/>
              <path d="M6 11a3 3 0 0 1 3-3 3 3 0 0 1 3 3"/>
              <path d="M18 11a3 3 0 0 0-3-3 3 3 0 0 0-3 3"/>
              <line x1="3" y1="15" x2="21" y2="15"/>
            </svg>
          </div>
          <div class="activity-card__body">
            <div class="activity-card__title">邀请有礼</div>
            <p class="activity-card__desc">邀请好友加入，双方均可获得会员天数和创作币返利</p>
            <div class="activity-card__stats">
              <span class="activity-card__stats-label">邀请 3 人得 3 天会员</span>
            </div>
          </div>
          <div class="activity-card__arrow">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="9 18 15 12 9 6"/>
            </svg>
          </div>
        </router-link>
      </div>
    </section>

    <!-- 学习区：创作学院 -->
    <section class="activities-section">
      <div class="activities-section__header">
        <span class="activities-section__badge activities-section__badge--learn">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"/>
            <path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"/>
          </svg>
          提升技能
        </span>
      </div>

      <router-link to="/console/learn" class="activity-card activity-card--academy">
        <div class="activity-card__bg" aria-hidden="true"></div>
        <div class="activity-card__icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M22 10v6M2 10l10-5 10 5-10 5z"/>
            <path d="M6 12v5c3 3 9 3 12 0v-5"/>
          </svg>
        </div>
        <div class="activity-card__body">
          <div class="activity-card__title">创作学院</div>
          <p class="activity-card__desc">从 0 到 1 的自媒体实战指南，系统提升内容创作能力</p>
          <div class="activity-card__stats">
            <span class="activity-card__stats-label">内容定位 · 爆款方法 · 平台运营</span>
          </div>
        </div>
        <div class="activity-card__arrow">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="9 18 15 12 9 6"/>
          </svg>
        </div>
      </router-link>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { listCommissionTasks } from '@/api/commission'
import { loadMarketSkillOverview, marketOverview } from '@/composables/useSkillMarket'

const stats = ref({
  activeTaskCount: 0,
  approvedCount: 0
})

onMounted(async () => {
  try {
    const [taskData] = await Promise.all([
      listCommissionTasks({ status: 'in_progress', page: 1, size: 1 }),
      loadMarketSkillOverview()
    ])
    stats.value.activeTaskCount = taskData.total || 0
    stats.value.approvedCount = Number(marketOverview.value.approvedCount || 0)
  } catch (e) {
    stats.value = { activeTaskCount: 0, approvedCount: 0 }
  }
})
</script>

<style scoped>
.activities-page {
  min-height: 100%;
  padding: 16px;
  background: #fafafa;
  box-sizing: border-box;
}

/* Hero */
.activities-hero {
  position: relative;
  padding: 28px 24px;
  margin-bottom: 22px;
  border-radius: 24px;
  background: linear-gradient(135deg, #fff0f2 0%, #f3e8ff 60%, #e6f0ff 100%);
  overflow: hidden;
  box-shadow: 0 8px 28px rgba(255, 36, 66, 0.08);
}

.activities-hero__copy {
  position: relative;
  z-index: 2;
}

.activities-hero__eyebrow {
  display: inline-block;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 2px;
  color: var(--color-primary, #ff2442);
  background: rgba(255, 255, 255, 0.65);
  padding: 4px 10px;
  border-radius: 999px;
}

.activities-hero__title {
  font-size: 26px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 12px 0 8px;
}

.activities-hero__subtitle {
  font-size: 14px;
  color: #595959;
  margin: 0;
  line-height: 1.5;
}

.activities-hero__orbit {
  position: absolute;
  top: -50px;
  right: -40px;
  width: 200px;
  height: 200px;
  pointer-events: none;
  opacity: 0.5;
}

.activities-hero__orbit span {
  position: absolute;
  border-radius: 50%;
}

.activities-hero__orbit span:nth-child(1) {
  width: 200px;
  height: 200px;
  top: 0;
  left: 0;
  border: 1.5px solid rgba(255, 36, 66, 0.2);
  border-style: dashed;
  animation: orbit-spin 22s linear infinite;
}

.activities-hero__orbit span:nth-child(2) {
  width: 120px;
  height: 120px;
  top: 40px;
  left: 40px;
  background: radial-gradient(circle at 30% 30%, rgba(255, 36, 66, 0.14), transparent 70%);
}

@keyframes orbit-spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* Section */
.activities-section {
  margin-bottom: 22px;
}

.activities-section__header {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.activities-section__badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  font-weight: 600;
  color: #ff2442;
  background: rgba(255, 36, 66, 0.08);
  padding: 5px 10px;
  border-radius: 999px;
}

.activities-section__badge svg {
  width: 14px;
  height: 14px;
}

.activities-section__badge--learn {
  color: #ff2442;
  background: rgba(255, 36, 66, 0.08);
}

/* 收益区 */
.activities-earnings {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

/* 卡片基础 */
.activity-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 22px 18px;
  background: #fff;
  border-radius: 20px;
  text-decoration: none;
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.04);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  overflow: hidden;
}

.activity-card:active {
  transform: scale(0.99);
}

.activity-card__bg {
  position: absolute;
  inset: 0;
  opacity: 0;
  transition: opacity 0.25s ease;
  pointer-events: none;
}

.activity-card--commission .activity-card__bg {
  background: linear-gradient(135deg, #fff5f7 0%, #fff0f2 100%);
}

.activity-card--market .activity-card__bg {
  background: linear-gradient(135deg, #fff5f7 0%, #fff0f2 100%);
}

.activity-card--leaderboard .activity-card__bg {
  background: linear-gradient(135deg, #fff5f7 0%, #fff0f2 100%);
}

.activity-card--invite .activity-card__bg {
  background: linear-gradient(135deg, #fff7f0 0%, #fff0e6 100%);
}

.activity-card--academy .activity-card__bg {
  background: linear-gradient(135deg, #fff5f7 0%, #fff0f2 100%);
}

.activity-card:hover .activity-card__bg {
  opacity: 1;
}

.activity-card__icon {
  position: relative;
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  z-index: 1;
}

.activity-card__icon svg {
  width: 26px;
  height: 26px;
}

.activity-card--commission .activity-card__icon {
  background: linear-gradient(135deg, #fff0f2 0%, #ffe4e8 100%);
  color: #ff2442;
}

.activity-card--market .activity-card__icon {
  background: linear-gradient(135deg, #fff0f2 0%, #ffe4e8 100%);
  color: #ff2442;
}

.activity-card--leaderboard .activity-card__icon {
  background: linear-gradient(135deg, #fff0f2 0%, #ffe4e8 100%);
  color: #ff2442;
}

.activity-card--invite .activity-card__icon {
  background: linear-gradient(135deg, #fff7f0 0%, #ffe8d6 100%);
  color: #fa8c16;
}

.activity-card--academy .activity-card__icon {
  background: linear-gradient(135deg, #fff0f2 0%, #ffe4e8 100%);
  color: #ff2442;
}

.activity-card--academy .activity-card__stats {
  background: rgba(255, 36, 66, 0.08);
  color: #ff2442;
}

.activity-card__body {
  flex: 1;
  min-width: 0;
  position: relative;
  z-index: 1;
}

.activity-card__title {
  font-size: 17px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 4px;
}

.activity-card__desc {
  font-size: 13px;
  color: #8c8c8c;
  margin: 0 0 10px;
  line-height: 1.45;
}

.activity-card__stats {
  display: inline-flex;
  align-items: baseline;
  gap: 4px;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
}

.activity-card--commission .activity-card__stats {
  background: rgba(255, 36, 66, 0.08);
  color: #ff2442;
}

.activity-card--market .activity-card__stats {
  background: rgba(255, 36, 66, 0.08);
  color: #ff2442;
}

.activity-card--leaderboard .activity-card__stats {
  background: rgba(255, 36, 66, 0.08);
  color: #ff2442;
}

.activity-card--invite .activity-card__stats {
  background: rgba(250, 140, 22, 0.08);
  color: #fa8c16;
}

.activity-card__stats-num {
  font-size: 15px;
  font-weight: 700;
}

.activity-card__arrow {
  position: relative;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #f5f5f5;
  color: #8c8c8c;
  flex-shrink: 0;
  z-index: 1;
  transition: all 0.2s ease;
}

.activity-card__arrow svg {
  width: 18px;
  height: 18px;
}

.activity-card:hover .activity-card__arrow {
  background: var(--color-primary, #ff2442);
  color: #fff;
  transform: translateX(2px);
}

/* 暗色主题 */
body[data-theme="dark"] .activities-page {
  background: #141414;
}

body[data-theme="dark"] .activities-hero {
  background: linear-gradient(135deg, #3a1f2a 0%, #2a1f3d 60%, #1f2a3d 100%);
}

body[data-theme="dark"] .activities-hero__title,
body[data-theme="dark"] .activity-card__title {
  color: #f5f5f5;
}

body[data-theme="dark"] .activities-hero__subtitle,
body[data-theme="dark"] .activity-card__desc {
  color: #a6a6a6;
}

body[data-theme="dark"] .activity-card {
  background: #1f1f1f;
  box-shadow: none;
}

body[data-theme="dark"] .activity-card__arrow {
  background: #262626;
  color: #8c8c8c;
}

body[data-theme="dark"] .activity-card:hover .activity-card__arrow {
  background: var(--color-primary, #ff2442);
  color: #fff;
}

body[data-theme="dark"] .activities-section__badge {
  background: rgba(255, 36, 66, 0.12);
}

body[data-theme="dark"] .activities-section__badge--learn {
  background: rgba(255, 36, 66, 0.12);
}

/* PC 端适配 */
@media (min-width: 769px) {
  .activities-page {
    max-width: 800px;
    margin: 0 auto;
    padding: 24px;
  }

  .activities-hero {
    padding: 36px 32px;
  }

  .activities-hero__title {
    font-size: 30px;
  }

  .activities-earnings {
    flex-direction: row;
  }

  .activity-card {
    flex: 1;
    padding: 26px 22px;
  }
}
</style>
