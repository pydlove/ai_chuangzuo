<template>
  <MobilePricing v-if="isMobile" />
  <div v-else class="pricing-page">
    <NavBar :links="landingNavLinks" :cta-to="landingTopCta.to" :cta-label="landingTopCta.label" />

    <!-- 主内容 -->
    <div class="pricing-body">
      <div class="pricing-content">
        <h1 class="pricing-title">每天 3 分钟，AI 帮你写完一篇文章</h1>
        <p class="pricing-subtitle">告别熬夜憋稿，轻松开启内容变现之旅</p>

        <!-- 周期切换 -->
        <div class="billing-toggle" :class="{ locked: cycleLocked() }">
          <button
            v-for="cycle in cycles"
            :key="cycle.key"
            :class="['toggle-btn', { active: activeCycle === cycle.key, disabled: isCycleDisabled(cycle.key) }]"
            :disabled="isCycleDisabled(cycle.key)"
            @click="setCycle(cycle.key)"
          >
            {{ cycle.label }}
            <span v-if="cycle.key === 'year' && maxYearSavings" class="toggle-badge">最高立省 ¥{{ maxYearSavings }}</span>
          </button>
        </div>

        <!-- 查看对比 -->
        <div class="compare-link">
          <span @click="scrollToCompare">
            查看完整权益对比 <span class="arrow">↓</span>
          </span>
        </div>

        <!-- 新人首冲优惠卡片 -->
        <div v-if="newcomerOffer" class="newcomer-offer-card">
          <div class="newcomer-offer-badge">新人首冲</div>
          <div class="newcomer-offer-main">
            <div class="newcomer-offer-title">旗舰版年包再享 8 折</div>
            <div class="newcomer-offer-desc">限时一次，非邀请用户专享</div>
            <div class="newcomer-offer-price-row">
              <span class="newcomer-offer-final">¥{{ newcomerOffer.finalPrice }}</span>
              <span class="newcomer-offer-regular">¥{{ newcomerOffer.regularPrice }}</span>
              <span class="newcomer-offer-original">¥{{ newcomerOffer.originalPrice }}</span>
              <span class="newcomer-offer-period">/年</span>
            </div>
            <div class="newcomer-offer-monthly">相当于 ¥{{ (Number(newcomerOffer.finalPrice) / 12).toFixed(2) }}/月</div>
            <div class="newcomer-offer-savings">共省 ¥{{ newcomerOffer.savings }}</div>
          </div>
          <button class="newcomer-offer-btn" @click="handleNewcomerSubscribe">立即开通</button>
        </div>

        <!-- 定价卡片 -->
        <div class="pricing-cards">
          <div v-if="catalogLoading" class="pricing-loading">套餐加载中…</div>
          <div
            v-for="plan in plans"
            v-else
            :key="plan.key"
            :class="['pricing-card', { recommended: plan.recommended }]"
          >
            <div v-if="plan.recommended" class="recommended-badge">最受欢迎</div>
            <div class="plan-name">{{ plan.name }}</div>
            <div v-if="getPrice(plan).original" class="plan-original">
              ¥{{ getPrice(plan).original }}
            </div>
            <div class="plan-price">
              ¥{{ getPrice(plan).current }}<span class="plan-period">/{{ getPeriodLabel() }}</span>
            </div>
            <div v-if="getMonthlyEquivalent(plan)" class="plan-monthly">
              相当于 ¥{{ getMonthlyEquivalent(plan) }}/月
            </div>
            <div class="plan-articles">{{ getArticles(plan) }}</div>
            <div v-if="getSavings(plan)" class="plan-savings">年付立省 ¥{{ getSavings(plan) }}</div>
            <button
              class="plan-btn"
              :class="{ primary: getPlanButton(plan).primary || plan.recommended, disabled: getPlanButton(plan).disabled }"
              :disabled="getPlanButton(plan).disabled"
              @click="handleSubscribe(plan)"
            >
              {{ getPlanButton(plan).text }}
            </button>
          </div>
        </div>
      </div>

      <!-- 权益对比表 -->
      <div class="compare-table-wrap">
        <div id="pricing-compare" class="compare-section">
          <div class="compare-header">
            <h2>功能权益对比</h2>
            <span class="compare-hint">✓ 包含 · ✗ 不包含</span>
          </div>
          <table class="compare-table">
            <thead>
              <tr>
                <th style="width: 32%;">权益</th>
                <th>基础版</th>
                <th class="recommended-col">专业版<span>最受欢迎</span></th>
                <th>旗舰版</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in compareRows" :key="row.code">
                <td>{{ row.label }}</td>
                <td v-html="getCell(row, 'basic')"></td>
                <td class="recommended-col" v-html="getCell(row, 'pro')"></td>
                <td v-html="getCell(row, 'flagship')"></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- 底部 -->
    <AppFooter />
    <!-- 升级确认弹框 -->
    <a-modal
      v-model:open="upgradeModalVisible"
      :title="`确认升级 ${selectedPlan ? selectedPlan.name : ''}`"
      :width="480"
      centered
      class="upgrade-modal membership-confirm-modal"
      @ok="confirmUpgrade"
      :confirm-loading="upgradeLoading"
    >
      <div v-if="upgradePreview" class="upgrade-panel">
        <div class="upgrade-row">
          <span class="upgrade-label">当前套餐</span>
          <span class="upgrade-value">{{ upgradePreview.currentPlanName }}（剩余 {{ upgradePreview.remainingDays }} 天，到期 {{ upgradePreview.currentExpiresAt }}）</span>
        </div>
        <div class="upgrade-row">
          <span class="upgrade-label">抵扣金额</span>
          <span class="upgrade-value credit">-¥{{ upgradePreview.creditAmount }}</span>
        </div>
        <div v-if="selectedCoinAmount > 0" class="upgrade-row">
          <span class="upgrade-label">创作币抵扣</span>
          <span class="upgrade-value credit">-{{ selectedCoinAmount }} 创作币（-¥{{ (selectedCoinAmount / COIN_TO_YUAN_RATIO).toFixed(2) }}）</span>
        </div>
        <div class="upgrade-row">
          <span class="upgrade-label">{{ upgradePreview.targetPlanName }} {{ cycleLabel[upgradePreview.targetCycle] }}价</span>
          <span class="upgrade-value">¥{{ upgradePreview.originalPrice }}</span>
        </div>
        <div class="upgrade-row total">
          <span class="upgrade-label">实付金额</span>
          <span class="upgrade-value final">¥{{ getFinalCash() }}</span>
        </div>
        <p class="upgrade-tip">
          升级后新套餐立即生效，有效期 {{ upgradePreview.targetDays }} 天至 {{ upgradePreview.newExpiresAt }}；当前订阅剩余价值已折算为抵扣金额。
        </p>
      </div>
    </a-modal>

    <!-- 订阅支付弹框 -->
    <a-modal
      v-model:open="modalVisible"
      :title="modalTitle"
      :width="520"
      centered
      class="subscribe-modal membership-confirm-modal"
      :closable="!payQrUrl"
      :mask-closable="!payQrUrl"
      :keyboard="!payQrUrl"
      :footer="null"
      @cancel="handleModalCancel"
    >
      <div class="subscribe-pay-panel">
        <CoinDiscountPanel
          v-if="coinBalance > 0 && getMaxCoinAmount() > 0 && !payQrUrl"
          v-model:selectedCoinAmount="selectedCoinAmount"
          :coinBalance="coinBalance"
          :maxCoinAmount="getMaxCoinAmount()"
          :coinToYuanRatio="COIN_TO_YUAN_RATIO"
          :finalCash="getFinalCash()"
        />
        <template v-if="isTestMode()">
          <p class="subscribe-pay-tip">
            测试阶段，请输入支付码 <strong>123456</strong> 完成{{ upgradePreview ? '升级' : '订阅' }}。
          </p>
          <a-input
            v-model:value="payCode"
            placeholder="请输入 6 位支付码"
            maxlength="6"
            size="large"
            @pressEnter="handlePay"
          />
          <div class="subscribe-pay-actions">
            <a-button type="primary" :loading="subscribeLoading" size="large" block @click="handlePay">
              确认{{ upgradePreview ? '升级' : '订阅' }}
            </a-button>
          </div>
        </template>
        <template v-else-if="payQrUrl">
          <div class="qr-pay-panel">
            <div class="qr-pay-left">
              <div class="qr-pay-amount">
                <span class="qr-pay-amount-label">微信支付</span>
                <span class="qr-pay-amount-value">¥{{ getFinalCash() }}</span>
              </div>
              <div class="qr-code-wrap">
                <img :src="payQrUrl" alt="微信支付二维码" class="qr-code-img" />
                <div class="qr-code-logo">微信</div>
              </div>
              <p class="qr-code-tip">请使用微信扫一扫完成支付</p>
            </div>
            <div class="qr-pay-right">
              <h4 class="qr-pay-title">{{ upgradePreview ? '升级' : '订阅' }}{{ selectedPlan?.name }}</h4>
              <ul class="qr-pay-terms">
                <li>开通会员{{ selectedPlan?.name }}{{ cycleLabel[activeCycle] }}套餐</li>
                <li>会员服务属于虚拟商品，一经支付无法退款</li>
                <li>支付完成后会员将自动开通，无需手动刷新</li>
              </ul>
            </div>
          </div>
          <div class="subscribe-pay-actions">
            <a-button size="large" block @click="handleModalCancel">关闭</a-button>
          </div>
        </template>
        <template v-else>
          <p class="subscribe-pay-tip">
            确认{{ upgradePreview ? '升级' : '订阅' }}{{ selectedPlan?.name }}{{ cycleLabel[activeCycle] }}套餐，支付成功后会员将自动开通。
          </p>
          <div class="subscribe-pay-actions">
            <a-button type="primary" :loading="subscribeLoading" size="large" block @click="handlePay">
              微信支付
            </a-button>
          </div>
        </template>
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import NavBar from '@/components/layout/NavBar.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
import MobilePricing from '@/views/MobilePricing.vue'
import CoinDiscountPanel from '@/components/pricing/CoinDiscountPanel.vue'
import { computed } from 'vue'
import { useDevice } from '@/composables/useDevice.js'
import { usePricing } from '@/composables/usePricing.js'
import { landingNavLinks, landingTopCta } from '@/data/siteConfig.js'

const { isMobile } = useDevice()

const {
  modalVisible,
  selectedPlan,
  payCode,
  subscribeLoading,
  selectedCoinAmount,
  payQrUrl,
  currentOrderNo,
  plans,
  compareRows,
  catalogLoading,
  newcomerOffer,
  activeCycle,
  cycles,
  cycleLocked,
  setCycle,
  isCycleDisabled,
  isTestMode,
  upgradeModalVisible,
  upgradePreview,
  upgradeLoading,
  cycleLabel,
  getPeriodLabel,
  getPrice,
  getArticles,
  getSavings,
  getMonthlyEquivalent,
  maxYearSavings,
  getCell,
  getPlanButton,
  handleSubscribe,
  handleNewcomerSubscribe,
  confirmUpgrade,
  handlePay,
  stopPolling,
  scrollToCompare,
  coinBalance,
  COIN_TO_YUAN_RATIO,
  getMaxCoinAmount,
  getFinalCash
} = usePricing()

const modalTitle = computed(() => {
  if (payQrUrl.value) {
    return '微信扫码支付'
  }
  return upgradePreview.value ? '确认支付升级' : `确认订阅 ${selectedPlan.value ? selectedPlan.value.name : ''}`
})

const handleModalCancel = () => {
  modalVisible.value = false
  payQrUrl.value = ''
  currentOrderNo.value = ''
  stopPolling()
}
</script>

<style scoped>
.pricing-page {
  min-height: 100vh;
  background: #f8f9fa;
  display: flex;
  flex-direction: column;
}

/* 主内容 */
.pricing-body {
  flex: 1;
  padding: 40px 24px;
}

.pricing-content {
  max-width: 960px;
  margin: 0 auto;
  text-align: center;
}

.pricing-title {
  font-size: 28px;
  margin-bottom: 8px;
  color: #1a1a1a;
}

.pricing-subtitle {
  color: #595959;
  margin-bottom: 28px;
}

/* 周期切换 */
.billing-toggle {
  display: inline-flex;
  background: #f5f5f5;
  border-radius: 10px;
  padding: 4px;
  margin-bottom: 12px;
  gap: 0;
}

.toggle-btn {
  padding: 8px 24px;
  background: transparent;
  color: #595959;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 4px;
}

.toggle-btn.active {
  background: #fff;
  color: #FF2442;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
}

.toggle-btn.disabled {
  color: #bfbfbf;
  cursor: not-allowed;
}

.billing-toggle.locked .toggle-btn.active {
  cursor: default;
}

.toggle-badge {
  color: #FF2442;
  font-size: 12px;
  font-weight: 500;
}

/* 查看对比 */
.compare-link {
  margin-bottom: 28px;
}

.compare-link span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #FF2442;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  border-bottom: 1px solid #FF2442;
  padding-bottom: 1px;
}

.arrow {
  font-size: 12px;
}

/* 新人首冲优惠卡片 */
.newcomer-offer-card {
  background: linear-gradient(135deg, #fff5f7 0%, #ffe8ec 100%);
  border: 2px solid #ff2442;
  border-radius: 16px;
  padding: 24px 28px;
  margin-bottom: 28px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  text-align: left;
  position: relative;
  overflow: hidden;
}
.newcomer-offer-badge {
  position: absolute;
  top: 12px;
  right: -28px;
  background: #ff2442;
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  padding: 4px 32px;
  transform: rotate(30deg);
}
.newcomer-offer-main {
  flex: 1;
}
.newcomer-offer-title {
  font-size: 20px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 4px;
}
.newcomer-offer-desc {
  font-size: 13px;
  color: #8c8c8c;
  margin-bottom: 10px;
}
.newcomer-offer-price-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 4px;
}
.newcomer-offer-final {
  font-size: 36px;
  font-weight: 800;
  color: #ff2442;
}
.newcomer-offer-period {
  font-size: 14px;
  color: #595959;
}
.newcomer-offer-regular {
  font-size: 16px;
  color: #8c8c8c;
  text-decoration: line-through;
}
.newcomer-offer-original {
  font-size: 14px;
  color: #bfbfbf;
  text-decoration: line-through;
}
.newcomer-offer-savings {
  font-size: 13px;
  color: #ff2442;
  font-weight: 500;
}
.newcomer-offer-monthly {
  font-size: 13px;
  color: #8c8c8c;
  margin-bottom: 4px;
}
.newcomer-offer-btn {
  flex-shrink: 0;
  padding: 12px 32px;
  background: #ff2442;
  color: #fff;
  border: none;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}
.newcomer-offer-btn:hover {
  background: #e61e3a;
}

/* 升级弹框 */
.upgrade-panel {
  padding: 8px 0 16px;
}
.upgrade-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px dashed #f0f0f0;
  font-size: 14px;
}
.upgrade-row:last-child {
  border-bottom: none;
}
.upgrade-row.total {
  padding-top: 16px;
  margin-top: 4px;
  border-top: 2px solid #f0f0f0;
  font-size: 16px;
  font-weight: 600;
}
.upgrade-label {
  color: #595959;
}
.upgrade-value {
  color: #1a1a1a;
  font-weight: 500;
}
.upgrade-value.credit {
  color: #ff2442;
}
.upgrade-value.final {
  color: #ff2442;
  font-size: 20px;
  font-weight: 700;
}
.upgrade-tip {
  margin-top: 16px;
  padding: 12px;
  background: #fff5f7;
  border-radius: 8px;
  color: #595959;
  font-size: 13px;
  line-height: 1.6;
}

/* 定价卡片 */
.pricing-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
  margin-bottom: 40px;
  text-align: left;
}

.pricing-loading {
  grid-column: 1 / -1;
  text-align: center;
  color: #8c8c8c;
  font-size: 14px;
  padding: 48px 0;
}

.pricing-card {
  background: #fff;
  border-radius: 16px;
  padding: 28px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.05);
  position: relative;
  border: 2px solid transparent;
  transition: box-shadow 0.25s, border-color 0.25s, transform 0.25s;
  cursor: pointer;
}

.pricing-card:hover {
  box-shadow: 0 8px 32px rgba(255,36,66,0.18);
  border-color: #FFCBD4;
  transform: translateY(-4px);
}

.pricing-card.recommended {
  border-color: #FF2442;
  box-shadow: 0 4px 24px rgba(255,36,66,0.15);
}

.pricing-card.recommended:hover {
  box-shadow: 0 12px 40px rgba(255,36,66,0.25);
  transform: translateY(-6px);
}

.recommended-badge {
  position: absolute;
  top: -12px;
  left: 50%;
  transform: translateX(-50%);
  background: #FF2442;
  color: #fff;
  padding: 4px 16px;
  border-radius: 12px;
  font-size: 12px;
  white-space: nowrap;
}

.plan-name {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 8px;
  color: #1a1a1a;
}

.plan-original {
  font-size: 14px;
  color: #8c8c8c;
  text-decoration: line-through;
  margin-bottom: 4px;
}

.plan-price {
  font-size: 32px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 6px;
}

.plan-period {
  font-size: 14px;
  color: #8c8c8c;
  font-weight: 400;
}

.plan-articles {
  color: #FF2442;
  font-size: 13px;
  margin-bottom: 16px;
}

.plan-monthly {
  color: #8c8c8c;
  font-size: 13px;
  margin-bottom: 8px;
}

.plan-savings {
  color: #FF2442;
  font-size: 13px;
  margin-bottom: 16px;
  text-align: left;
}

.plan-btn {
  width: 100%;
  padding: 12px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  background: #fff;
  color: #FF2442;
  border: 1px solid #FF2442;
  transition: all 0.2s;
  margin-bottom: 20px;
}

.plan-btn:hover {
  background: #FFF5F7;
}

.plan-btn.primary {
  background: #FF2442;
  color: #fff;
  border: none;
}

.plan-btn.primary:hover {
  background: #E61E3A;
}

.plan-btn.disabled {
  background: #f5f5f5;
  color: #8c8c8c;
  border-color: #d9d9d9;
  cursor: not-allowed;
}

.plan-btn.disabled:hover {
  background: #f5f5f5;
}

/* 权益对比表 */
.compare-section {
  max-width: 960px;
  margin: 0 auto;
  background: #fff;
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.05);
  text-align: left;
}

.compare-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.compare-header h2 {
  font-size: 20px;
  color: #1a1a1a;
  margin: 0;
}

.compare-hint {
  font-size: 13px;
  color: #8c8c8c;
}

.compare-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.compare-table th,
.compare-table td {
  text-align: center;
  padding: 14px 12px;
}

.compare-table th {
  font-weight: 600;
  color: #1a1a1a;
  border-bottom: 2px solid #f0f0f0;
}

.compare-table th.recommended-col {
  background: #FFE5EB;
  color: #FF2442;
  border-top-left-radius: 8px;
  border-top-right-radius: 8px;
}

.compare-table th span {
  display: block;
  font-size: 12px;
  font-weight: 500;
  color: #FF2442;
}

.compare-table tr:hover td {
  background: #FFF5F7;
}

.compare-table td {
  border-bottom: 1px solid #f5f5f5;
  color: #595959;
}

.compare-table td.recommended-col {
  background: #FFE5EB;
  font-weight: 500;
}

/* ========== 媒体查询：手机端 ≤768px ========== */
@media (max-width: 768px) {
  .pricing-body {
    padding: 24px 16px;
  }
  .pricing-title {
    font-size: 22px;
  }
  .pricing-subtitle {
    font-size: 14px;
    margin-bottom: 20px;
  }
  .billing-toggle {
    margin-bottom: 8px;
  }
  .toggle-btn {
    padding: 6px 14px;
    font-size: 13px;
  }
  .toggle-badge {
    font-size: 11px;
  }
  .compare-link {
    margin-bottom: 20px;
  }
  .newcomer-offer-card {
    flex-direction: column;
    align-items: stretch;
    padding: 20px;
  }
  .newcomer-offer-badge {
    top: 8px;
    right: -32px;
    padding: 3px 36px;
  }
  .newcomer-offer-title {
    font-size: 18px;
  }
  .newcomer-offer-final {
    font-size: 30px;
  }
  .newcomer-offer-btn {
    width: 100%;
  }
  .pricing-cards {
    grid-template-columns: 1fr;
    gap: 16px;
    margin-bottom: 32px;
  }
  .pricing-card {
    padding: 20px;
  }
  .plan-name {
    font-size: 16px;
  }
  .plan-price {
    font-size: 26px;
  }
  .compare-table-wrap {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
    width: 100%;
    margin: 0 -16px;
    padding: 0 16px;
  }
  .compare-section {
    padding: 20px 16px;
    min-width: 0;
  }
  .compare-table {
    min-width: 480px;
  }
  .compare-header h2 {
    font-size: 18px;
  }
  .compare-table th,
  .compare-table td {
    padding: 10px 8px;
    font-size: 13px;
  }
  .pricing-footer {
    display: flex;
    flex-direction: column;
    gap: 4px;
    font-size: 12px;
    padding: 16px 20px;
  }
  .pricing-footer span + span::before {
    display: none;
  }
}

/* ========== 暗色主题 ========== */
body[data-theme="dark"] .newcomer-offer-card {
  background: linear-gradient(135deg, #331018 0%, #2a0d12 100%);
  border-color: #ff4d6f;
}
body[data-theme="dark"] .newcomer-offer-badge {
  background: #ff4d6f;
}
body[data-theme="dark"] .newcomer-offer-title {
  color: #e0e0e0;
}
body[data-theme="dark"] .newcomer-offer-desc,
body[data-theme="dark"] .newcomer-offer-period,
body[data-theme="dark"] .newcomer-offer-regular,
body[data-theme="dark"] .newcomer-offer-original,
body[data-theme="dark"] .newcomer-offer-monthly {
  color: #a6a6a6;
}
body[data-theme="dark"] .newcomer-offer-final,
body[data-theme="dark"] .newcomer-offer-savings {
  color: #ff4d6f;
}
body[data-theme="dark"] .newcomer-offer-btn {
  background: linear-gradient(135deg, #FF6B8A 0%, #FF2442 100%);
}
body[data-theme="dark"] .newcomer-offer-btn:hover {
  background: linear-gradient(135deg, #FF4D6F 0%, #E61E3A 100%);
}

body[data-theme="dark"] .upgrade-row {
  border-bottom-color: #303030;
}
body[data-theme="dark"] .upgrade-row.total {
  border-top-color: #303030;
}
body[data-theme="dark"] .upgrade-label {
  color: #a6a6a6;
}
body[data-theme="dark"] .upgrade-value {
  color: #e0e0e0;
}
body[data-theme="dark"] .upgrade-tip {
  background: rgba(255, 36, 66, 0.12);
  color: #a6a6a6;
}

body[data-theme="dark"] .pricing-page {
  background: #141414;
}

body[data-theme="dark"] .pricing-title {
  color: #e0e0e0;
}

body[data-theme="dark"] .pricing-subtitle {
  color: #a6a6a6;
}

body[data-theme="dark"] .billing-toggle {
  background: #262626;
}

body[data-theme="dark"] .toggle-btn {
  color: #a6a6a6;
}

body[data-theme="dark"] .toggle-btn.active {
  background: #1f1f1f;
  color: #ff4d6f;
}

body[data-theme="dark"] .toggle-btn.disabled {
  color: #666;
}

body[data-theme="dark"] .compare-link span {
  color: #ff4d6f;
  border-bottom-color: #ff4d6f;
}

body[data-theme="dark"] .pricing-card {
  background: #1f1f1f;
}

body[data-theme="dark"] .pricing-card:hover {
  border-color: #ff4d6f;
}

body[data-theme="dark"] .pricing-card.recommended {
  border-color: #ff4d6f;
}

body[data-theme="dark"] .plan-name,
body[data-theme="dark"] .plan-price {
  color: #e0e0e0;
}

body[data-theme="dark"] .plan-original,
body[data-theme="dark"] .plan-period,
body[data-theme="dark"] .plan-monthly {
  color: #8c8c8c;
}

body[data-theme="dark"] .plan-articles,
body[data-theme="dark"] .plan-savings {
  color: #ff4d6f;
}

body[data-theme="dark"] .plan-btn {
  background: #1f1f1f;
  color: #ff4d6f;
  border-color: #ff4d6f;
}

body[data-theme="dark"] .plan-btn:hover {
  background: rgba(255, 36, 66, 0.12);
}

body[data-theme="dark"] .plan-btn.primary {
  background: linear-gradient(135deg, #FF6B8A 0%, #FF2442 100%);
  border: none;
  color: #fff;
}

body[data-theme="dark"] .plan-btn.primary:hover {
  background: linear-gradient(135deg, #FF4D6F 0%, #E61E3A 100%);
  color: #fff;
}

body[data-theme="dark"] .plan-btn.disabled {
  background: #2a2a2a;
  color: #666;
  border-color: #434343;
}

body[data-theme="dark"] .plan-btn.disabled:hover {
  background: #2a2a2a;
}

body[data-theme="dark"] .compare-section {
  background: #1f1f1f;
}

body[data-theme="dark"] .compare-header h2 {
  color: #e0e0e0;
}

body[data-theme="dark"] .compare-hint {
  color: #8c8c8c;
}

body[data-theme="dark"] .compare-table th {
  color: #e0e0e0;
  border-bottom-color: #303030;
}

body[data-theme="dark"] .compare-table th.recommended-col {
  background: rgba(255, 36, 66, 0.18);
  color: #ff4d6f;
}

body[data-theme="dark"] .compare-table th span {
  color: #ff4d6f;
}

body[data-theme="dark"] .compare-table tr:hover td {
  background: rgba(255, 36, 66, 0.06);
}

body[data-theme="dark"] .compare-table td {
  border-bottom-color: #262626;
  color: #a6a6a6;
}

body[data-theme="dark"] .compare-table td.recommended-col {
  background: rgba(255, 36, 66, 0.1);
  color: #e0e0e0;
}

.subscribe-pay-panel {
  padding: 8px 0 16px;
}

.subscribe-pay-tip {
  color: #595959;
  font-size: 14px;
  margin-bottom: 16px;
}

.subscribe-pay-tip strong {
  color: #FF2442;
}

body[data-theme="dark"] .subscribe-pay-tip {
  color: #a6a6a6;
}

body[data-theme="dark"] .subscribe-pay-tip strong {
  color: #ff4d6f;
}

.subscribe-pay-actions {
  margin-top: 20px;
}

.qr-pay-panel {
  display: flex;
  gap: 24px;
  padding: 8px 0;
}

.qr-pay-left {
  flex: 0 0 180px;
  text-align: center;
}

.qr-pay-right {
  flex: 1;
  min-width: 0;
}

.qr-pay-amount {
  margin-bottom: 16px;
}

.qr-pay-amount-label {
  display: block;
  font-size: 14px;
  color: #595959;
  margin-bottom: 4px;
}

.qr-pay-amount-value {
  display: block;
  font-size: 28px;
  font-weight: 700;
  color: #1a1a1a;
}

.qr-code-wrap {
  position: relative;
  width: 160px;
  height: 160px;
  margin: 0 auto 12px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 8px;
  background: #fff;
}

.qr-code-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.qr-code-logo {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  width: 36px;
  height: 36px;
  background: #07c160;
  color: #fff;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 600;
}

.qr-code-tip {
  color: #8c8c8c;
  font-size: 12px;
  margin: 0;
}

.qr-pay-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 16px;
}

.qr-pay-terms {
  margin: 0;
  padding-left: 18px;
  color: #595959;
  font-size: 13px;
  line-height: 1.8;
}

.qr-pay-terms li {
  margin-bottom: 8px;
}

@media (max-width: 768px) {
  .qr-pay-panel {
    flex-direction: column;
    align-items: center;
  }
  .qr-pay-right {
    width: 100%;
  }
}

body[data-theme="dark"] .qr-pay-amount-label {
  color: #a6a6a6;
}

body[data-theme="dark"] .qr-pay-amount-value {
  color: #e0e0e0;
}

body[data-theme="dark"] .qr-code-wrap {
  background: #fff;
  border-color: #2a2a2a;
}

body[data-theme="dark"] .qr-pay-title {
  color: #e0e0e0;
}

body[data-theme="dark"] .qr-pay-terms {
  color: #a6a6a6;
}
</style>
