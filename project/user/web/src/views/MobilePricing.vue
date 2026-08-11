<template>
  <div class="mobile-pricing">
    <!-- 顶部导航 -->
    <header class="mp-header">
      <router-link to="/" class="mp-header__brand">
        <img
          src="https://foruda.gitee.com/images/1782986808430461164/e0ab39dc_8060302.png"
          alt="爱创作"
          class="mp-header__logo"
        />
        <span class="mp-header__name">爱创作</span>
      </router-link>
      <div class="mp-header__actions">
        <button class="mp-header__menu" aria-label="菜单" @click="menuOpen = true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="3" y1="6" x2="21" y2="6" />
            <line x1="3" y1="12" x2="21" y2="12" />
            <line x1="3" y1="18" x2="21" y2="18" />
          </svg>
        </button>
        <router-link to="/login" class="mp-header__cta">开始创作</router-link>
      </div>
    </header>

    <!-- 菜单抽屉 -->
    <div v-if="menuOpen" class="mp-menu-backdrop" @click="menuOpen = false"></div>
    <div :class="['mp-menu', { open: menuOpen }]">
      <div class="mp-menu__header">
        <span class="mp-menu__title">菜单</span>
        <button class="mp-menu__close" aria-label="关闭" @click="menuOpen = false">×</button>
      </div>
      <nav class="mp-menu__nav">
        <router-link
          v-for="link in navLinks"
          :key="link.to"
          :to="link.to"
          class="mp-menu__link"
          :class="{ active: route.path === link.to }"
          @click="menuOpen = false"
        >{{ link.label }}</router-link>
      </nav>
    </div>

    <!-- 主体 -->
    <main class="mp-body">
      <div class="mp-hero">
        <h1 class="mp-hero__title">每天 3 分钟，AI 帮你写完一篇文章</h1>
        <p class="mp-hero__desc">告别熬夜憋稿，轻松开启内容变现之旅</p>
      </div>

      <!-- 周期切换 -->
      <div class="mp-cycle" :class="{ locked: cycleLocked() }">
        <button
          v-for="cycle in cycles"
          :key="cycle.key"
          :class="['mp-cycle__btn', { active: activeCycle === cycle.key, disabled: isCycleDisabled(cycle.key) }]"
          :disabled="isCycleDisabled(cycle.key)"
          @click="setCycle(cycle.key)"
        >
          {{ cycle.label }}
          <span v-if="cycle.key === 'year'" class="mp-cycle__badge">最高省 ¥359</span>
        </button>
      </div>

      <div class="mp-compare-link" @click="scrollToCompare">
        查看完整权益对比 <span>↓</span>
      </div>

      <!-- 新人首冲 -->
      <div v-if="newcomerOffer" class="mp-newcomer">
        <div class="mp-newcomer__badge">新人首冲</div>
        <div class="mp-newcomer__title">旗舰版年包再享 8 折</div>
        <div class="mp-newcomer__desc">限时一次，非邀请用户专享</div>
        <div class="mp-newcomer__price">
          <span class="mp-newcomer__final">¥{{ newcomerOffer.finalPrice }}</span>
          <span class="mp-newcomer__regular">¥{{ newcomerOffer.regularPrice }}</span>
          <span class="mp-newcomer__original">¥{{ newcomerOffer.originalPrice }}</span>
          <span class="mp-newcomer__period">/年</span>
        </div>
        <div class="mp-newcomer__savings">共省 ¥{{ newcomerOffer.savings }}</div>
        <button class="mp-newcomer__btn" @click="handleNewcomerSubscribe">立即开通</button>
      </div>

      <!-- 套餐卡片 -->
      <div v-if="catalogLoading" class="mp-loading">套餐加载中…</div>
      <div v-else class="mp-cards">
        <div
          v-for="plan in plans"
          :key="plan.key"
          :class="['mp-card', { recommended: plan.recommended }]"
        >
          <div v-if="plan.recommended" class="mp-card__badge">推荐</div>
          <div class="mp-card__name">{{ plan.name }}</div>
          <div v-if="getPrice(plan).original" class="mp-card__original">¥{{ getPrice(plan).original }}</div>
          <div class="mp-card__price">
            ¥{{ getPrice(plan).current }}
            <span>/{{ getPeriodLabel() }}</span>
          </div>
          <div class="mp-card__meta">
            <span>{{ getArticles(plan) }}</span>
            <span v-if="getSavings(plan)">· 省¥{{ getSavings(plan) }}</span>
          </div>
          <button
            :class="['mp-card__btn', { primary: getPlanButton(plan).primary || plan.recommended, disabled: getPlanButton(plan).disabled }]"
            :disabled="getPlanButton(plan).disabled"
            @click="handleSubscribe(plan)"
          >{{ getPlanButton(plan).text }}</button>
        </div>
      </div>

      <!-- 权益对比 -->
      <div id="pricing-compare" class="mp-compare">
        <div class="mp-compare__header">
          <h2>功能权益对比</h2>
          <span>✓ 包含 · ✗ 不包含</span>
        </div>

        <div class="mp-compare__plans">
          <button
            v-for="key in ['basic', 'pro', 'flagship']"
            :key="key"
            :class="['mp-compare__tab', { active: activeComparePlan === key }]"
            @click="activeComparePlan = key"
          >
            {{ planNames[key] }}
            <span v-if="key === 'pro'">最受欢迎</span>
          </button>
        </div>

        <div class="mp-compare__list">
          <div
            v-for="row in compareRows"
            :key="row.code"
            class="mp-compare__row"
          >
            <div class="mp-compare__label">{{ row.label }}</div>
            <div class="mp-compare__value">
              <span v-if="cellValue(row[activeComparePlan]) === true" class="yes">✓</span>
              <span v-else-if="cellValue(row[activeComparePlan]) === false" class="yes">✗</span>
              <span v-else class="text">{{ cellValue(row[activeComparePlan]) }}</span>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- 底部 -->
    <footer class="mp-footer">
      <div>© 2026 爱创作 · 杭州爱启云网络科技有限公司</div>
      <div>浙ICP备XXXXXXXX号-1</div>
    </footer>

    <!-- 升级确认弹框 -->
    <a-modal
      v-model:open="upgradeModalVisible"
      :title="`确认升级 ${selectedPlan ? selectedPlan.name : ''}`"
      :width="320"
      centered
      class="mp-upgrade-modal"
      @ok="confirmUpgrade"
      :confirm-loading="upgradeLoading"
    >
      <div v-if="upgradePreview" class="mp-upgrade-panel">
        <div class="mp-upgrade-row">
          <span class="mp-upgrade-label">当前套餐</span>
          <span class="mp-upgrade-value">{{ upgradePreview.currentPlanName }}</span>
        </div>
        <div class="mp-upgrade-row">
          <span class="mp-upgrade-label">剩余天数</span>
          <span class="mp-upgrade-value">{{ upgradePreview.remainingDays }} 天</span>
        </div>
        <div class="mp-upgrade-row">
          <span class="mp-upgrade-label">抵扣金额</span>
          <span class="mp-upgrade-value credit">-¥{{ upgradePreview.creditAmount }}</span>
        </div>
        <div v-if="selectedCoinAmount > 0" class="mp-upgrade-row">
          <span class="mp-upgrade-label">创作币抵扣</span>
          <span class="mp-upgrade-value credit">-{{ selectedCoinAmount }} 创作币（-¥{{ (selectedCoinAmount / COIN_TO_YUAN_RATIO).toFixed(2) }}）</span>
        </div>
        <div class="mp-upgrade-row">
          <span class="mp-upgrade-label">新套餐价格</span>
          <span class="mp-upgrade-value">¥{{ upgradePreview.originalPrice }}</span>
        </div>
        <div class="mp-upgrade-row total">
          <span class="mp-upgrade-label">实付金额</span>
          <span class="mp-upgrade-value final">¥{{ getFinalCash() }}</span>
        </div>
        <p class="mp-upgrade-tip">升级后立即生效，有效期 {{ upgradePreview.targetDays }} 天至 {{ upgradePreview.newExpiresAt }}。</p>
      </div>
    </a-modal>

    <!-- 支付弹框 -->
    <a-modal
      v-model:open="modalVisible"
      :title="upgradePreview ? '确认支付升级' : `确认订阅 ${selectedPlan ? selectedPlan.name : ''}`"
      :width="320"
      centered
      class="mp-subscribe-modal"
      @ok="handlePay"
      :confirm-loading="subscribeLoading"
    >
      <div class="mp-pay-panel">
        <CoinDiscountPanel
          v-if="coinBalance > 0 && getMaxCoinAmount() > 0"
          v-model:selectedCoinAmount="selectedCoinAmount"
          :coinBalance="coinBalance"
          :maxCoinAmount="getMaxCoinAmount()"
          :coinToYuanRatio="COIN_TO_YUAN_RATIO"
          :finalCash="getFinalCash()"
        />
        <p class="mp-pay-tip">
          测试阶段，请输入支付码 <strong>123456</strong> 完成{{ upgradePreview ? '升级' : '订阅' }}。
        </p>
        <a-input
          v-model:value="payCode"
          placeholder="请输入 6 位支付码"
          maxlength="6"
          size="large"
          @pressEnter="handlePay"
        />
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import CoinDiscountPanel from '@/components/pricing/CoinDiscountPanel.vue'
import { usePricing } from '@/composables/usePricing.js'

const route = useRoute()
const menuOpen = ref(false)
const activeComparePlan = ref('pro')

const navLinks = [
  { to: '/', label: '首页' },
  { to: '/pricing', label: '会员' },
  { to: '/lottery', label: '活动' },
  { to: '/guide', label: '玩法指南' },
  { to: '/learn', label: '创作学院' }
]

const planNames = {
  basic: '基础版',
  pro: '专业版',
  flagship: '旗舰版'
}

const {
  modalVisible,
  selectedPlan,
  payCode,
  subscribeLoading,
  selectedCoinAmount,
  plans,
  compareRows,
  catalogLoading,
  newcomerOffer,
  activeCycle,
  cycles,
  cycleLocked,
  setCycle,
  isCycleDisabled,
  upgradeModalVisible,
  upgradePreview,
  upgradeLoading,
  getPeriodLabel,
  getPrice,
  getArticles,
  getSavings,
  cellValue,
  getPlanButton,
  handleSubscribe,
  handleNewcomerSubscribe,
  confirmUpgrade,
  handlePay,
  scrollToCompare,
  coinBalance,
  COIN_TO_YUAN_RATIO,
  getMaxCoinAmount,
  getFinalCash
} = usePricing()
</script>

<style scoped>
.mobile-pricing {
  min-height: 100vh;
  background: #f8f9fa;
  color: #1a1a1a;
  -webkit-font-smoothing: antialiased;
}

/* 顶部导航 */
.mp-header {
  position: sticky;
  top: 0;
  z-index: 50;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid #f0f0f0;
}
.mp-header__brand {
  display: flex;
  align-items: center;
  gap: 8px;
}
.mp-header__logo {
  height: 28px;
  width: auto;
  max-width: 40px;
  object-fit: contain;
  border-radius: 6px;
}
.mp-header__name {
  font-size: 16px;
  font-weight: 700;
  color: #1a1a1a;
}
.mp-header__actions {
  display: flex;
  align-items: center;
  gap: 10px;
}
.mp-header__menu {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
  background: #fff;
  color: #595959;
  display: flex;
  align-items: center;
  justify-content: center;
}
.mp-header__menu svg {
  width: 18px;
  height: 18px;
}
.mp-header__cta {
  padding: 8px 18px;
  background: linear-gradient(135deg, #FF4D6F 0%, #FF2442 100%);
  color: #fff;
  border-radius: 18px;
  font-size: 13px;
  font-weight: 600;
  text-decoration: none;
}

/* 菜单抽屉 */
.mp-menu-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.3);
  z-index: 80;
}
.mp-menu {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  width: 240px;
  background: #fff;
  z-index: 90;
  transform: translateX(100%);
  transition: transform 0.25s ease;
  display: flex;
  flex-direction: column;
}
.mp-menu.open {
  transform: translateX(0);
}
.mp-menu__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid #f0f0f0;
}
.mp-menu__title {
  font-size: 16px;
  font-weight: 700;
  color: #1a1a1a;
}
.mp-menu__close {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: none;
  background: #f5f5f5;
  color: #595959;
  font-size: 20px;
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}
.mp-menu__nav {
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.mp-menu__link {
  padding: 12px;
  border-radius: 8px;
  font-size: 15px;
  color: #1a1a1a;
  text-decoration: none;
}
.mp-menu__link.active,
.mp-menu__link:active {
  background: #FFF5F7;
  color: #FF2442;
}

/* Hero */
.mp-body {
  padding: 28px 16px 40px;
}
.mp-hero {
  text-align: center;
  margin-bottom: 24px;
}
.mp-hero__title {
  font-size: 24px;
  font-weight: 800;
  line-height: 1.35;
  margin-bottom: 10px;
  color: #1a1a1a;
}
.mp-hero__desc {
  font-size: 14px;
  color: #8c8c8c;
}

/* 周期切换 */
.mp-cycle {
  display: flex;
  background: #fff;
  border-radius: 12px;
  padding: 4px;
  margin-bottom: 14px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.mp-cycle__btn {
  flex: 1;
  padding: 10px 0;
  background: transparent;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #595959;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}
.mp-cycle__btn.active {
  background: #FF2442;
  color: #fff;
  box-shadow: 0 2px 8px rgba(255, 36, 66, 0.25);
}
.mp-cycle__btn.disabled {
  color: #bfbfbf;
}
.mp-cycle.locked .mp-cycle__btn.active {
  cursor: default;
}
.mp-cycle__badge {
  font-size: 10px;
  font-weight: 500;
  background: rgba(255, 255, 255, 0.25);
  padding: 1px 5px;
  border-radius: 8px;
}

.mp-compare-link {
  text-align: center;
  color: #FF2442;
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 24px;
}
.mp-compare-link span {
  display: inline-block;
  font-size: 12px;
}

/* 新人首冲 */
.mp-newcomer {
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #fff5f7 0%, #ffe8ec 100%);
  border: 2px solid #ff2442;
  border-radius: 16px;
  padding: 24px 20px;
  margin-bottom: 20px;
  text-align: center;
}
.mp-newcomer__badge {
  position: absolute;
  top: 10px;
  right: -30px;
  background: #ff2442;
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  padding: 4px 32px;
  transform: rotate(30deg);
}
.mp-newcomer__title {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 4px;
  color: #1a1a1a;
}
.mp-newcomer__desc {
  font-size: 13px;
  color: #8c8c8c;
  margin-bottom: 12px;
}
.mp-newcomer__price {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 8px;
  margin-bottom: 6px;
}
.mp-newcomer__final {
  font-size: 32px;
  font-weight: 800;
  color: #ff2442;
}
.mp-newcomer__regular {
  font-size: 15px;
  color: #8c8c8c;
  text-decoration: line-through;
}
.mp-newcomer__original {
  font-size: 13px;
  color: #bfbfbf;
  text-decoration: line-through;
}
.mp-newcomer__period {
  font-size: 14px;
  color: #595959;
}
.mp-newcomer__savings {
  font-size: 13px;
  color: #ff2442;
  font-weight: 500;
  margin-bottom: 16px;
}
.mp-newcomer__btn {
  width: 100%;
  padding: 13px 0;
  background: #ff2442;
  color: #fff;
  border: none;
  border-radius: 24px;
  font-size: 16px;
  font-weight: 600;
}

/* 升级弹框 */
.mp-upgrade-panel {
  padding: 8px 0 16px;
}
.mp-upgrade-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px dashed #f0f0f0;
  font-size: 14px;
}
.mp-upgrade-row:last-child {
  border-bottom: none;
}
.mp-upgrade-row.total {
  padding-top: 14px;
  margin-top: 4px;
  border-top: 2px solid #f0f0f0;
  font-size: 15px;
  font-weight: 600;
}
.mp-upgrade-label {
  color: #595959;
}
.mp-upgrade-value {
  color: #1a1a1a;
  font-weight: 500;
}
.mp-upgrade-value.credit {
  color: #ff2442;
}
.mp-upgrade-value.final {
  color: #ff2442;
  font-size: 18px;
  font-weight: 700;
}
.mp-upgrade-tip {
  margin-top: 14px;
  padding: 10px;
  background: #fff5f7;
  border-radius: 8px;
  color: #595959;
  font-size: 12px;
  line-height: 1.6;
}

/* 加载 */
.mp-loading {
  text-align: center;
  padding: 48px 0;
  color: #8c8c8c;
  font-size: 14px;
}

/* 套餐卡片 */
.mp-cards {
  display: flex;
  flex-direction: row;
  gap: 8px;
  margin-bottom: 40px;
}
.mp-card {
  position: relative;
  flex: 1 1 0;
  min-width: 0;
  background: #fff;
  border-radius: 14px;
  padding: 14px 6px 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  border: 2px solid transparent;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.mp-card.recommended {
  border-color: #FF2442;
  box-shadow: 0 4px 16px rgba(255, 36, 66, 0.12);
}

.mp-card__badge {
  position: absolute;
  top: -8px;
  left: 50%;
  transform: translateX(-50%);
  background: #FF2442;
  color: #fff;
  padding: 2px 8px;
  border-radius: 8px;
  font-size: 10px;
  font-weight: 600;
  white-space: nowrap;
}

.mp-card__name {
  font-size: 13px;
  font-weight: 700;
  margin-bottom: 4px;
  color: #1a1a1a;
}
.mp-card__original {
  font-size: 10px;
  color: #8c8c8c;
  text-decoration: line-through;
  margin-bottom: 2px;
}
.mp-card__price {
  font-size: 20px;
  font-weight: 800;
  color: #1a1a1a;
  margin-bottom: 2px;
  line-height: 1.2;
}
.mp-card__price span {
  font-size: 10px;
  color: #8c8c8c;
  font-weight: 400;
}
.mp-card__meta {
  font-size: 10px;
  color: #FF2442;
  margin-bottom: 10px;
  line-height: 1.3;
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.mp-card__btn {
  width: 100%;
  padding: 7px 0;
  border-radius: 16px;
  font-size: 11px;
  font-weight: 600;
  background: #fff;
  color: #FF2442;
  border: 1px solid #FF2442;
  margin-top: auto;
}

.mp-card__btn.primary {
  background: linear-gradient(135deg, #FF4D6F 0%, #FF2442 100%);
  color: #fff;
  border: none;
  box-shadow: 0 4px 12px rgba(255, 36, 66, 0.25);
}
.mp-card__btn.disabled {
  background: #f5f5f5;
  color: #8c8c8c;
  border-color: #d9d9d9;
}

/* 权益对比 */
.mp-compare {
  background: #fff;
  border-radius: 16px;
  padding: 24px 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}
.mp-compare__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
}
.mp-compare__header h2 {
  font-size: 18px;
  margin: 0;
  color: #1a1a1a;
}
.mp-compare__header span {
  font-size: 12px;
  color: #8c8c8c;
}
.mp-compare__plans {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}
.mp-compare__tab {
  flex: 1;
  padding: 10px 0;
  background: #f5f5f5;
  border: none;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 600;
  color: #595959;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}
.mp-compare__tab span {
  font-size: 10px;
  font-weight: 500;
  color: #8c8c8c;
}
.mp-compare__tab.active {
  background: #FF2442;
  color: #fff;
}
.mp-compare__tab.active span {
  color: rgba(255, 255, 255, 0.85);
}
.mp-compare__list {
  display: flex;
  flex-direction: column;
}
.mp-compare__row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 0;
  border-bottom: 1px solid #f5f5f5;
}
.mp-compare__row:last-child {
  border-bottom: none;
}
.mp-compare__label {
  font-size: 14px;
  color: #595959;
  padding-right: 16px;
}
.mp-compare__value {
  flex-shrink: 0;
  font-size: 14px;
}
.mp-compare__value .yes {
  color: #FF2442;
  font-weight: 600;
}
.mp-compare__value .text {
  color: #1a1a1a;
  font-weight: 500;
}

/* Footer */
.mp-footer {
  padding: 24px 20px 32px;
  text-align: center;
  background: #fff;
  border-top: 1px solid #f0f0f0;
  font-size: 12px;
  color: #8c8c8c;
  line-height: 1.8;
}

/* 支付弹框 */
.mp-pay-panel {
  padding: 8px 0 16px;
}
.mp-pay-tip {
  color: #595959;
  font-size: 14px;
  margin-bottom: 16px;
  line-height: 1.6;
}
.mp-pay-tip strong {
  color: #FF2442;
}

/* 暗色主题 */
body[data-theme="dark"] .mobile-pricing {
  background: #141414;
  color: #e0e0e0;
}
body[data-theme="dark"] .mp-header {
  background: rgba(20, 20, 20, 0.96);
  border-bottom-color: #2a2a2a;
}
body[data-theme="dark"] .mp-header__name { color: #e0e0e0; }
body[data-theme="dark"] .mp-header__menu {
  background: #1f1f1f;
  border-color: #2a2a2a;
  color: #a6a6a6;
}
body[data-theme="dark"] .mp-menu {
  background: #1f1f1f;
}
body[data-theme="dark"] .mp-menu__header {
  border-color: #2a2a2a;
}
body[data-theme="dark"] .mp-menu__title { color: #e0e0e0; }
body[data-theme="dark"] .mp-menu__close {
  background: #2a2a2a;
  color: #a6a6a6;
}
body[data-theme="dark"] .mp-menu__link { color: #e0e0e0; }
body[data-theme="dark"] .mp-menu__link.active,
body[data-theme="dark"] .mp-menu__link:active {
  background: rgba(255, 36, 66, 0.15);
  color: #ff4d6f;
}
body[data-theme="dark"] .mp-hero__title,
body[data-theme="dark"] .mp-card__name,
body[data-theme="dark"] .mp-compare__header h2,
body[data-theme="dark"] .mp-newcomer__title {
  color: #e0e0e0;
}
body[data-theme="dark"] .mp-hero__desc,
body[data-theme="dark"] .mp-compare-link,
body[data-theme="dark"] .mp-compare__label,
body[data-theme="dark"] .mp-pay-tip {
  color: #a6a6a6;
}
body[data-theme="dark"] .mp-cycle {
  background: #1f1f1f;
}
body[data-theme="dark"] .mp-cycle__btn { color: #a6a6a6; }
body[data-theme="dark"] .mp-cycle__btn.active { color: #fff; }
body[data-theme="dark"] .mp-cycle__btn.disabled { color: #666; }
body[data-theme="dark"] .mp-card,
body[data-theme="dark"] .mp-compare {
  background: #1f1f1f;
}
body[data-theme="dark"] .mp-card__price { color: #e0e0e0; }
body[data-theme="dark"] .mp-card__btn {
  background: #1f1f1f;
  color: #ff4d6f;
  border-color: #ff4d6f;
}

body[data-theme="dark"] .mp-card__btn.primary {
  background: linear-gradient(135deg, #FF6B8A 0%, #FF2442 100%);
  color: #fff;
  border: none;
}
body[data-theme="dark"] .mp-card__btn.disabled {
  background: #2a2a2a;
  color: #666;
  border-color: #434343;
}
body[data-theme="dark"] .mp-compare__tab {
  background: #2a2a2a;
  color: #a6a6a6;
}
body[data-theme="dark"] .mp-compare__tab.active {
  background: #ff4d6f;
  color: #fff;
}
body[data-theme="dark"] .mp-compare__row {
  border-bottom-color: #2a2a2a;
}
body[data-theme="dark"] .mp-compare__value .text { color: #e0e0e0; }
body[data-theme="dark"] .mp-newcomer {
  background: linear-gradient(135deg, #331018 0%, #2a0d12 100%);
  border-color: #ff4d6f;
}
body[data-theme="dark"] .mp-newcomer__desc,
body[data-theme="dark"] .mp-newcomer__regular,
body[data-theme="dark"] .mp-newcomer__original,
body[data-theme="dark"] .mp-newcomer__period {
  color: #a6a6a6;
}
body[data-theme="dark"] .mp-newcomer__final,
body[data-theme="dark"] .mp-newcomer__savings,
body[data-theme="dark"] .mp-card__meta,
body[data-theme="dark"] .mp-compare__value .yes,
body[data-theme="dark"] .mp-pay-tip strong {
  color: #ff4d6f;
}
body[data-theme="dark"] .mp-newcomer__btn {
  background: linear-gradient(135deg, #FF6B8A 0%, #FF2442 100%);
}
body[data-theme="dark"] .mp-upgrade-row {
  border-bottom-color: #303030;
}
body[data-theme="dark"] .mp-upgrade-row.total {
  border-top-color: #303030;
}
body[data-theme="dark"] .mp-upgrade-label {
  color: #a6a6a6;
}
body[data-theme="dark"] .mp-upgrade-value {
  color: #e0e0e0;
}
body[data-theme="dark"] .mp-upgrade-tip {
  background: rgba(255, 36, 66, 0.12);
  color: #a6a6a6;
}
body[data-theme="dark"] .mp-footer {
  background: #1f1f1f;
  border-top-color: #2a2a2a;
  color: #a6a6a6;
}
</style>
