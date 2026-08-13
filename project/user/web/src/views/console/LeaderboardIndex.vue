<template>
  <div class="leaderboard-page">
    <div class="leaderboard-layout">
      <div class="leaderboard-main">
        <!-- 头部运营区 -->
        <header id="toc-overview" class="leaderboard-hero">
          <div class="leaderboard-hero__copy">
            <div class="leaderboard-hero__badge">
              <TrophyOutlined class="leaderboard-hero__badge-icon" />
              创作激励榜
            </div>
            <h1 class="leaderboard-hero__title">收益排行榜</h1>
            <p class="leaderboard-hero__subtitle">创作赚币，上榜有礼 · 每月 TOP {{ rewardConfig.topLimit }} 瓜分创作币奖励</p>
            <span class="leaderboard-hero__rules" @click="rulesVisible = true">规则说明</span>
          </div>
          <div class="leaderboard-hero__glow" aria-hidden="true"></div>
          <div class="leaderboard-hero__sparkle" aria-hidden="true"></div>
        </header>

        <!-- 周期切换 -->
        <div class="leaderboard-period-bar">
          <button
            v-for="p in periods"
            :key="p.value"
            :class="['leaderboard-period-btn', { active: activePeriod === p.value }]"
            @click="activePeriod = p.value"
          >
            {{ p.label }}
          </button>
        </div>

        <!-- 我的排名 -->
        <section id="toc-my-rank" class="leaderboard-section leaderboard-section--my">
          <div v-if="myCoinItem" class="leaderboard-my-card">
            <div class="leaderboard-my__rank">
              <div class="leaderboard-my__rank-num">{{ myCoinItem.rank }}</div>
              <div class="leaderboard-my__rank-label">当前排名</div>
            </div>
            <div class="leaderboard-my__info">
              <div class="leaderboard-my__name">{{ myCoinItem.nickname || '匿名用户' }}</div>
              <div class="leaderboard-my__amount">{{ myCoinItem.amount.toFixed(2) }} 创作币</div>
            </div>
            <div v-if="myCoinItem.rank <= rewardConfig.topLimit" class="leaderboard-my__reward">
              <div class="leaderboard-my__reward-label">预计奖励</div>
              <div class="leaderboard-my__reward-value">+{{ rewardConfig.rewardAmount }}</div>
            </div>
            <div v-else class="leaderboard-my__reward leaderboard-my__reward--no">
              <div class="leaderboard-my__reward-label">距 TOP {{ rewardConfig.topLimit }}</div>
              <div class="leaderboard-my__reward-value">差 {{ rankGap }} 名</div>
            </div>
          </div>
          <div v-else class="leaderboard-my-card leaderboard-my-card--empty">
            <RocketOutlined class="leaderboard-my__empty-icon" />
            <div class="leaderboard-my__empty-body">
              <div class="leaderboard-my__empty-title">暂未上榜</div>
              <div class="leaderboard-my__empty-desc">本月再创作几篇，就有机会进入 TOP {{ rewardConfig.topLimit }}</div>
            </div>
            <router-link to="/console/create" class="leaderboard-my__empty-action">去创作 →</router-link>
          </div>
        </section>

        <!-- TOP3 领奖台 -->
        <section id="toc-top3" class="leaderboard-section">
          <div class="leaderboard-section-header">
            <span class="leaderboard-section-tag">荣耀榜</span>
            <h2 class="leaderboard-section-title">TOP 3</h2>
          </div>

          <div v-if="coinTop3.length === 0" class="leaderboard-empty">
            暂无排名数据
          </div>
          <div v-else class="leaderboard-podium">
            <!-- 第二名 -->
            <div v-if="coinTop3[1]" class="podium-card podium-card--second">
              <div class="podium-card__rank">2</div>
              <div class="podium-card__avatar">{{ (coinTop3[1].nickname || '?').charAt(0) }}</div>
              <div class="podium-card__name">{{ coinTop3[1].nickname || '匿名用户' }}</div>
              <div class="podium-card__amount">{{ coinTop3[1].amount.toFixed(2) }}</div>
              <div class="podium-card__unit">创作币</div>
              <div v-if="coinTop3[1].isMe" class="podium-card__me">我</div>
            </div>

            <!-- 第一名 -->
            <div v-if="coinTop3[0]" class="podium-card podium-card--first">
              <div class="podium-card__crown"><CrownOutlined /></div>
              <div class="podium-card__avatar">{{ (coinTop3[0].nickname || '?').charAt(0) }}</div>
              <div class="podium-card__name">{{ coinTop3[0].nickname || '匿名用户' }}</div>
              <div class="podium-card__amount">{{ coinTop3[0].amount.toFixed(2) }}</div>
              <div class="podium-card__unit">创作币</div>
              <div v-if="coinTop3[0].isMe" class="podium-card__me">我</div>
            </div>

            <!-- 第三名 -->
            <div v-if="coinTop3[2]" class="podium-card podium-card--third">
              <div class="podium-card__rank">3</div>
              <div class="podium-card__avatar">{{ (coinTop3[2].nickname || '?').charAt(0) }}</div>
              <div class="podium-card__name">{{ coinTop3[2].nickname || '匿名用户' }}</div>
              <div class="podium-card__amount">{{ coinTop3[2].amount.toFixed(2) }}</div>
              <div class="podium-card__unit">创作币</div>
              <div v-if="coinTop3[2].isMe" class="podium-card__me">我</div>
            </div>
          </div>
        </section>

        <!-- 完整榜单 -->
        <section id="toc-full-list" class="leaderboard-section">
          <div class="leaderboard-section-header">
            <span class="leaderboard-section-tag">完整榜单</span>
            <h2 class="leaderboard-section-title">TOP 4 - 100</h2>
          </div>

          <div v-if="coinListAfter3.length === 0" class="leaderboard-empty">
            暂无更多排名数据
          </div>
          <div v-else class="leaderboard-list">
            <div
              v-for="item in coinListAfter3"
              :key="item.userId"
              :class="['leaderboard-row', { 'is-me': item.isMe }, 'rank-' + item.rank]"
            >
              <div class="leaderboard-row__rank">
                <span v-if="item.rank <= 10" class="leaderboard-row__badge">{{ item.rank }}</span>
                <span v-else">{{ item.rank }}</span>
              </div>
              <div class="leaderboard-row__avatar">{{ (item.nickname || '?').charAt(0) }}</div>
              <div class="leaderboard-row__info">
                <div class="leaderboard-row__name">
                  {{ item.nickname || '匿名用户' }}
                  <span v-if="item.isMe" class="leaderboard-row__me">我</span>
                </div>
                <div v-if="item.rank <= rewardConfig.topLimit" class="leaderboard-row__tag">TOP {{ rewardConfig.topLimit }} 奖励</div>
              </div>
              <div class="leaderboard-row__amount">
                <span class="leaderboard-row__num">{{ item.amount.toFixed(2) }}</span>
                <span class="leaderboard-row__unit">创作币</span>
              </div>
            </div>
          </div>
        </section>
      </div>

      <!-- PC 端目录侧边栏 -->
      <aside class="leaderboard-toc-sidebar">
        <nav class="leaderboard-toc">
          <div class="leaderboard-toc-title">
            <UnorderedListOutlined class="leaderboard-toc-title-icon" />
            目录
          </div>
          <div class="leaderboard-toc-track">
            <a
              v-for="item in tocItems"
              :key="item.id"
              :class="['leaderboard-toc-item', { active: item.id === activeHeading }]"
              href="#"
              @click.prevent="scrollToHeading(item.id)"
            >
              <span class="leaderboard-toc-dot"></span>
              <span class="leaderboard-toc-text">{{ item.text }}</span>
            </a>
          </div>
        </nav>
      </aside>
    </div>

    <!-- 移动端目录浮钮 -->
    <button
      class="mobile-toc-btn"
      type="button"
      aria-label="目录"
      @click="tocDrawerOpen = true"
    >
      <UnorderedListOutlined />
    </button>

    <!-- 移动端目录 drawer -->
    <a-drawer
      v-model:open="tocDrawerOpen"
      title="目录"
      placement="bottom"
      :height="'60vh'"
      :closable="true"
      class="mobile-toc-drawer"
    >
      <div class="mobile-toc-list">
        <a
          v-for="item in tocItems"
          :key="item.id"
          :class="['mobile-toc-item', { active: item.id === activeHeading }]"
          href="#"
          @click.prevent="onTocItemClick(item.id)"
        >
          <span class="mobile-toc-dot"></span>
          <span class="mobile-toc-text">{{ item.text }}</span>
        </a>
      </div>
    </a-drawer>

    <!-- 规则说明 -->
    <a-modal
      v-model:open="rulesVisible"
      title="收益排行榜规则"
      :footer="null"
      :width="520"
      centered
      class="leaderboard-rules-modal"
    >
      <ol class="leaderboard-rules-list">
        <li><span class="leaderboard-rules-highlight">创作币榜</span>按自然月统计平台创作币收益，数据自动汇总，无需手动申报。</li>
        <li>每个自然月的 <span class="leaderboard-rules-highlight">创作币榜 TOP {{ rewardConfig.topLimit }}</span> 均可获得 <span class="leaderboard-rules-highlight">{{ rewardConfig.rewardAmount }} 创作币</span>奖励。</li>
        <li>奖励在榜单结算后自动发放至账户余额，同一人同一周期只发放一次。</li>
      </ol>
      <div class="leaderboard-rules-footer">* 活动最终解释权归平台所有。</div>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { message } from 'ant-design-vue'
import { UnorderedListOutlined, TrophyOutlined, CrownOutlined, RocketOutlined } from '@ant-design/icons-vue'
import { getCoinLeaderboard, getLeaderboardRewardConfig } from '@/api/leaderboard.js'

function getMonthLabel(period) {
  const now = new Date()
  if (period === 'current') {
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
  }
  if (period === 'last') {
    const d = new Date(now.getFullYear(), now.getMonth() - 1, 1)
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
  }
  return 'all'
}

const periods = [
  { label: '本月', value: 'current' },
  { label: '上月', value: 'last' },
  { label: '总榜', value: 'all' }
]

const activePeriod = ref('current')
const coinList = ref([])
const loading = ref(false)
const rulesVisible = ref(false)
const rewardConfig = ref({ topLimit: 3, rewardAmount: 500 })

async function loadRewardConfig() {
  try {
    const res = await getLeaderboardRewardConfig()
    const data = res?.data
    if (data) {
      rewardConfig.value = {
        topLimit: data.topLimit ?? 3,
        rewardAmount: data.rewardAmount ?? 500
      }
    }
  } catch (err) {
    // 使用默认值，不阻断页面
    console.error('加载奖励配置失败', err)
  }
}

async function loadCoinLeaderboard() {
  try {
    loading.value = true
    const res = await getCoinLeaderboard(getMonthLabel(activePeriod.value))
    const list = res?.data?.topList || []
    const me = res?.data?.me
    if (me && me.rank != null && !list.some(item => item.isMe)) {
      list.push(me)
    }
    coinList.value = list
  } catch (err) {
    message.error(err.message || '创作币榜加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadRewardConfig()
  loadCoinLeaderboard()
  nextTick(buildTocObserver)
})

watch(activePeriod, () => {
  loadCoinLeaderboard()
})

const coinTop3 = computed(() => coinList.value.slice(0, 3))
const coinListAfter3 = computed(() => coinList.value.slice(3))
const myCoinItem = computed(() => coinList.value.find(i => i.isMe))
const rankGap = computed(() => {
  if (!myCoinItem.value || myCoinItem.value.rank <= rewardConfig.value.topLimit) return 0
  return myCoinItem.value.rank - rewardConfig.value.topLimit
})

// ---- 目录 ----
const tocItems = [
  { id: 'toc-overview', text: '榜单概览' },
  { id: 'toc-my-rank', text: '我的排名' },
  { id: 'toc-top3', text: '荣耀榜 TOP3' },
  { id: 'toc-full-list', text: '完整榜单' }
]

const activeHeading = ref('')
const tocDrawerOpen = ref(false)
let tocObserver = null

function buildTocObserver() {
  if (tocObserver) {
    tocObserver.disconnect()
    tocObserver = null
  }
  activeHeading.value = ''

  const sections = document.querySelectorAll('.leaderboard-section, .leaderboard-hero')
  if (!sections.length) return

  tocObserver = new IntersectionObserver(entries => {
    for (const entry of entries) {
      if (entry.isIntersecting) activeHeading.value = entry.target.id
    }
  }, { rootMargin: '-88px 0px -70% 0px' })

  sections.forEach(el => tocObserver.observe(el))
}

function scrollToHeading(id) {
  const el = document.getElementById(id)
  if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function onTocItemClick(id) {
  scrollToHeading(id)
  tocDrawerOpen.value = false
}

onUnmounted(() => {
  if (tocObserver) tocObserver.disconnect()
})
</script>

<style scoped>
.leaderboard-page {
  width: 100%;
  height: 100%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px 24px;
  overflow-y: auto;
  box-sizing: border-box;
}

.leaderboard-layout {
  display: flex;
  gap: 24px;
}

.leaderboard-main {
  flex: 1;
  min-width: 0;
}

/* Hero */
.leaderboard-hero {
  position: relative;
  border-radius: 24px;
  padding: 32px 24px;
  margin-bottom: 20px;
  overflow: hidden;
  background: linear-gradient(135deg, #fff5e6 0%, #fff0f2 40%, #f3e8ff 100%);
  box-shadow: 0 10px 40px rgba(255, 36, 66, 0.1);
}

.leaderboard-hero__copy {
  position: relative;
  z-index: 2;
  text-align: center;
}

.leaderboard-hero__badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 1px;
  color: #ff2442;
  background: rgba(255, 255, 255, 0.7);
  padding: 6px 14px;
  border-radius: 999px;
  margin-bottom: 14px;
  backdrop-filter: blur(4px);
}

.leaderboard-hero__badge-icon {
  font-size: 14px;
}

.leaderboard-hero__title {
  font-size: 30px;
  font-weight: 800;
  color: #1a1a1a;
  margin: 0 0 8px;
  letter-spacing: -0.5px;
}

.leaderboard-hero__subtitle {
  font-size: 14px;
  color: #595959;
  margin: 0 0 16px;
}

.leaderboard-hero__rules {
  display: inline-block;
  font-size: 13px;
  color: #ff2442;
  font-weight: 500;
  text-decoration: underline;
  text-underline-offset: 3px;
  cursor: pointer;
}

.leaderboard-hero__rules:hover {
  color: #e61e3a;
}

.leaderboard-hero__glow {
  position: absolute;
  top: -60px;
  right: -60px;
  width: 220px;
  height: 220px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 200, 100, 0.35) 0%, transparent 70%);
  pointer-events: none;
}

.leaderboard-hero__sparkle {
  position: absolute;
  bottom: -40px;
  left: -40px;
  width: 180px;
  height: 180px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 36, 66, 0.12) 0%, transparent 70%);
  pointer-events: none;
}

/* 周期切换 */
.leaderboard-period-bar {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-bottom: 20px;
  padding: 4px;
  background: #f5f5f5;
  border-radius: 12px;
  width: fit-content;
  margin-left: auto;
  margin-right: auto;
}

.leaderboard-period-btn {
  padding: 8px 24px;
  border: none;
  background: transparent;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  color: #595959;
  cursor: pointer;
  transition: all 0.2s;
}

.leaderboard-period-btn.active {
  background: #fff;
  color: #ff2442;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  font-weight: 600;
}

/* Section 通用 */
.leaderboard-section {
  margin-bottom: 28px;
}

.leaderboard-section-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}

.leaderboard-section-tag {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 1px;
  color: #fff;
  background: linear-gradient(135deg, #ff2442 0%, #ff6b81 100%);
  padding: 4px 10px;
  border-radius: 6px;
}

.leaderboard-section-title {
  font-size: 17px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0;
}

/* 我的排名 */
.leaderboard-my-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  border-radius: 20px;
  background: linear-gradient(135deg, #fff5f7 0%, #fff0f2 100%);
  border: 1px solid #ffd1d9;
  box-shadow: 0 6px 20px rgba(255, 36, 66, 0.08);
}

.leaderboard-my-card--empty {
  background: #fff;
  border-color: #f0f0f0;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
}

.leaderboard-my__rank {
  text-align: center;
  min-width: 60px;
}

.leaderboard-my__rank-num {
  font-size: 34px;
  font-weight: 800;
  color: #ff2442;
  line-height: 1;
}

.leaderboard-my__rank-label {
  font-size: 11px;
  color: #8c8c8c;
  margin-top: 4px;
}

.leaderboard-my__info {
  flex: 1;
  min-width: 0;
}

.leaderboard-my__name {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 4px;
}

.leaderboard-my__amount {
  font-size: 14px;
  color: #595959;
}

.leaderboard-my__reward {
  text-align: right;
}

.leaderboard-my__reward-label {
  font-size: 11px;
  color: #8c8c8c;
  margin-bottom: 2px;
}

.leaderboard-my__reward-value {
  font-size: 24px;
  font-weight: 800;
  color: #ff2442;
  line-height: 1;
}

.leaderboard-my__reward--no .leaderboard-my__reward-value {
  color: #8c8c8c;
  font-size: 18px;
}

.leaderboard-my__empty-icon {
  font-size: 32px;
}

.leaderboard-my__empty-body {
  flex: 1;
}

.leaderboard-my__empty-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 4px;
}

.leaderboard-my__empty-desc {
  font-size: 13px;
  color: #8c8c8c;
}

.leaderboard-my__empty-action {
  font-size: 13px;
  font-weight: 600;
  color: #ff2442;
  text-decoration: none;
  white-space: nowrap;
}

/* TOP3 领奖台 */
.leaderboard-podium {
  display: flex;
  align-items: flex-end;
  justify-content: center;
  gap: 12px;
  padding: 12px 0 0;
}

.podium-card {
  position: relative;
  flex: 1;
  max-width: 160px;
  text-align: center;
  border-radius: 20px;
  padding: 20px 12px 16px;
  background: #fff;
  border: 1px solid #f0f0f0;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.05);
}

.podium-card--first {
  order: 2;
  padding-top: 28px;
  padding-bottom: 24px;
  background: linear-gradient(180deg, #fff9e6 0%, #fff 100%);
  border-color: #ffe082;
  box-shadow: 0 8px 28px rgba(255, 193, 7, 0.18);
  z-index: 2;
}

.podium-card--second {
  order: 1;
  transform: translateY(14px);
  background: linear-gradient(180deg, #f5f5f5 0%, #fff 100%);
  border-color: #d9d9d9;
}

.podium-card--third {
  order: 3;
  transform: translateY(24px);
  background: linear-gradient(180deg, #fff5e6 0%, #fff 100%);
  border-color: #ffcc80;
}

.podium-card__crown,
.podium-card__rank {
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 8px;
}

.podium-card__crown {
  font-size: 24px;
  color: #d48806;
}

.podium-card__rank {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  font-size: 14px;
  font-weight: 700;
  color: #fff;
  background: #d9d9d9;
}

.podium-card--second .podium-card__rank {
  background: linear-gradient(135deg, #bfc0c2 0%, #e8e8e8 100%);
  color: #595959;
}

.podium-card--third .podium-card__rank {
  background: linear-gradient(135deg, #cd7f32 0%, #eebb77 100%);
}

.podium-card--first .podium-card__crown {
  font-size: 32px;
  margin-top: -8px;
  margin-bottom: 4px;
  color: #d48806;
}

.podium-card__avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 600;
  color: #595959;
  margin: 0 auto 10px;
}

.podium-card--first .podium-card__avatar {
  width: 58px;
  height: 58px;
  font-size: 22px;
  background: linear-gradient(135deg, #ffd700 0%, #ffed4e 100%);
  color: #fff;
  box-shadow: 0 4px 14px rgba(255, 193, 7, 0.35);
}

.podium-card--second .podium-card__avatar {
  background: linear-gradient(135deg, #bfc0c2 0%, #e8e8e8 100%);
  color: #fff;
}

.podium-card--third .podium-card__avatar {
  background: linear-gradient(135deg, #cd7f32 0%, #eebb77 100%);
  color: #fff;
}

.podium-card__name {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.podium-card__amount {
  font-size: 20px;
  font-weight: 800;
  color: #ff2442;
  line-height: 1;
}

.podium-card__unit {
  font-size: 11px;
  color: #8c8c8c;
  margin-top: 2px;
}

.podium-card__me {
  position: absolute;
  top: 10px;
  left: 10px;
  font-size: 10px;
  font-weight: 600;
  padding: 2px 8px;
  background: #ff2442;
  color: #fff;
  border-radius: 10px;
}

/* 完整榜单 */
.leaderboard-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.leaderboard-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: #fff;
  border-radius: 14px;
  border: 1px solid #f5f5f5;
  transition: transform 0.15s, box-shadow 0.15s;
}

.leaderboard-row.is-me {
  background: linear-gradient(135deg, #fff5f7 0%, #fff0f2 100%);
  border-color: #ffd1d9;
  box-shadow: 0 4px 14px rgba(255, 36, 66, 0.08);
}

.leaderboard-row__rank {
  width: 32px;
  text-align: center;
  font-size: 15px;
  font-weight: 700;
  color: #8c8c8c;
  flex-shrink: 0;
}

.leaderboard-row__badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: #fff5f7;
  color: #ff2442;
  font-size: 13px;
  font-weight: 700;
}

.leaderboard-row.rank-4 .leaderboard-row__badge,
.leaderboard-row.rank-5 .leaderboard-row__badge,
.leaderboard-row.rank-6 .leaderboard-row__badge,
.leaderboard-row.rank-7 .leaderboard-row__badge,
.leaderboard-row.rank-8 .leaderboard-row__badge,
.leaderboard-row.rank-9 .leaderboard-row__badge,
.leaderboard-row.rank-10 .leaderboard-row__badge {
  background: #fff5f7;
  color: #ff2442;
}

.leaderboard-row__avatar {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  color: #595959;
  flex-shrink: 0;
}

.leaderboard-row__info {
  flex: 1;
  min-width: 0;
}

.leaderboard-row__name {
  font-size: 15px;
  font-weight: 500;
  color: #1a1a1a;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.leaderboard-row__me {
  font-size: 10px;
  font-weight: 600;
  padding: 1px 6px;
  background: #ff2442;
  color: #fff;
  border-radius: 8px;
}

.leaderboard-row__tag {
  display: inline-block;
  font-size: 11px;
  color: #ff2442;
  background: #fff5f7;
  padding: 2px 8px;
  border-radius: 999px;
}

.leaderboard-row__amount {
  text-align: right;
  flex-shrink: 0;
}

.leaderboard-row__num {
  display: block;
  font-size: 16px;
  font-weight: 700;
  color: #ff2442;
  line-height: 1;
}

.leaderboard-row__unit {
  font-size: 11px;
  color: #8c8c8c;
}

.leaderboard-empty {
  padding: 48px 24px;
  text-align: center;
  color: #8c8c8c;
  font-size: 14px;
  background: #fff;
  border: 1px dashed #e8e8e8;
  border-radius: 16px;
}

/* PC 端目录侧边栏 */
.leaderboard-toc-sidebar {
  width: 200px;
  flex-shrink: 0;
}

.leaderboard-toc {
  position: sticky;
  top: 88px;
  max-height: calc(100vh - 112px);
  overflow-y: auto;
  padding: 4px 0;
}

.leaderboard-toc-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 12px;
  padding-left: 16px;
}

.leaderboard-toc-title-icon {
  font-size: 16px;
  color: #ff2442;
}

.leaderboard-toc-track {
  position: relative;
  padding-left: 20px;
}

.leaderboard-toc-track::before {
  content: '';
  position: absolute;
  left: 3px;
  top: 6px;
  bottom: 6px;
  width: 2px;
  background: #f0f0f0;
  border-radius: 1px;
}

.leaderboard-toc-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 5px 8px;
  margin-left: -20px;
  padding-left: 0;
  text-decoration: none;
  color: #8c8c8c;
  font-size: 13px;
  line-height: 1.5;
  cursor: pointer;
  border-radius: 6px;
  transition: color 0.2s, background 0.2s;
  position: relative;
}

.leaderboard-toc-item:hover {
  color: #ff2442;
  background: #fff5f7;
}

.leaderboard-toc-item.active {
  color: #ff2442;
  font-weight: 600;
}

.leaderboard-toc-item.active .leaderboard-toc-dot {
  background: #ff2442;
  box-shadow: 0 0 0 3px rgba(255, 36, 66, 0.15);
}

.leaderboard-toc-dot {
  width: 8px;
  height: 8px;
  min-width: 8px;
  border-radius: 50%;
  background: #d9d9d9;
  margin-top: 5px;
  transition: all 0.2s;
}

.leaderboard-toc-text {
  flex: 1;
}

/* 移动端目录浮钮 */
.mobile-toc-btn {
  display: none;
  position: fixed;
  right: 16px;
  bottom: 80px;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  border: none;
  background: #fff;
  color: #ff2442;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  align-items: center;
  justify-content: center;
  font-size: 18px;
  cursor: pointer;
  z-index: 99;
  transition: transform 0.2s, box-shadow 0.2s;
}

.mobile-toc-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.16);
}

.mobile-toc-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.mobile-toc-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 10px;
  border-radius: 8px;
  text-decoration: none;
  color: #595959;
  font-size: 14px;
  line-height: 1.5;
  transition: background 0.2s, color 0.2s;
}

.mobile-toc-item:hover,
.mobile-toc-item.active {
  background: #fff5f7;
  color: #ff2442;
}

.mobile-toc-item.active .mobile-toc-dot {
  background: #ff2442;
}

.mobile-toc-dot {
  width: 8px;
  height: 8px;
  min-width: 8px;
  border-radius: 50%;
  background: #d9d9d9;
  margin-top: 5px;
  transition: background 0.2s;
}

.mobile-toc-text {
  flex: 1;
}

/* 规则说明 */
.leaderboard-rules-list {
  padding-left: 18px;
  margin: 0;
  color: #595959;
  font-size: 14px;
  line-height: 1.8;
}

.leaderboard-rules-list li {
  margin-bottom: 10px;
}

.leaderboard-rules-highlight {
  color: #ff2442;
  font-weight: 500;
}

.leaderboard-rules-footer {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
  font-size: 12px;
  color: #8c8c8c;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .leaderboard-page {
    padding: 12px;
  }

  .leaderboard-layout {
    display: block;
  }

  .leaderboard-toc-sidebar {
    display: none;
  }

  .mobile-toc-btn {
    display: flex;
  }

  .leaderboard-hero {
    padding: 24px 16px;
    border-radius: 20px;
  }

  .leaderboard-hero__title {
    font-size: 26px;
  }

  .leaderboard-hero__subtitle {
    font-size: 13px;
  }

  .leaderboard-period-bar {
    width: 100%;
    box-sizing: border-box;
  }

  .leaderboard-period-btn {
    flex: 1;
    padding: 8px 0;
  }

  .leaderboard-my-card {
    padding: 16px;
    gap: 12px;
  }

  .leaderboard-my__rank-num {
    font-size: 28px;
  }

  .leaderboard-my__name {
    font-size: 15px;
  }

  .leaderboard-my__reward-value {
    font-size: 20px;
  }

  .leaderboard-podium {
    gap: 8px;
    padding-top: 8px;
  }

  .podium-card {
    max-width: none;
    padding: 16px 8px 14px;
    border-radius: 16px;
  }

  .podium-card--first {
    padding-top: 22px;
    padding-bottom: 20px;
  }

  .podium-card--second {
    transform: translateY(10px);
  }

  .podium-card--third {
    transform: translateY(18px);
  }

  .podium-card__crown {
    font-size: 20px;
  }

  .podium-card--first .podium-card__crown {
    font-size: 26px;
  }

  .podium-card__avatar {
    width: 40px;
    height: 40px;
    font-size: 15px;
  }

  .podium-card--first .podium-card__avatar {
    width: 50px;
    height: 50px;
    font-size: 18px;
  }

  .podium-card__name {
    font-size: 12px;
  }

  .podium-card__amount {
    font-size: 16px;
  }

  .leaderboard-row {
    padding: 12px 14px;
  }

  .leaderboard-row__rank {
    width: 28px;
    font-size: 13px;
  }

  .leaderboard-row__avatar {
    width: 34px;
    height: 34px;
    font-size: 12px;
  }

  .leaderboard-row__name {
    font-size: 14px;
  }

  .leaderboard-row__num {
    font-size: 15px;
  }
}
</style>

<style>
/* 暗色主题：全局覆盖 */
body[data-theme="dark"] .leaderboard-page {
  background: #141414;
}

body[data-theme="dark"] .leaderboard-hero {
  background: linear-gradient(135deg, #3a2a1a 0%, #3a1f2a 50%, #2a1f3d 100%);
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.3);
}

body[data-theme="dark"] .leaderboard-hero__badge {
  background: rgba(30, 30, 30, 0.7);
  color: #ff6b81;
}

body[data-theme="dark"] .leaderboard-hero__title,
body[data-theme="dark"] .leaderboard-section-title,
body[data-theme="dark"] .leaderboard-my__name,
body[data-theme="dark"] .leaderboard-row__name,
body[data-theme="dark"] .leaderboard-empty-title,
body[data-theme="dark"] .leaderboard-toc-title {
  color: #f0f0f0;
}

body[data-theme="dark"] .leaderboard-hero__subtitle,
body[data-theme="dark"] .leaderboard-my__amount,
body[data-theme="dark"] .leaderboard-my__rank-label,
body[data-theme="dark"] .leaderboard-my__reward-label,
body[data-theme="dark"] .leaderboard-my__empty-desc,
body[data-theme="dark"] .podium-card__unit,
body[data-theme="dark"] .leaderboard-row__unit,
body[data-theme="dark"] .leaderboard-empty,
body[data-theme="dark"] .leaderboard-rules-footer {
  color: #8c8c8c;
}

body[data-theme="dark"] .leaderboard-hero__rules {
  color: #ff6b81;
}

body[data-theme="dark"] .leaderboard-hero__rules:hover {
  color: #ff9c9c;
}

body[data-theme="dark"] .leaderboard-period-bar {
  background: #1f1f1f;
}

body[data-theme="dark"] .leaderboard-period-btn {
  color: #8c8c8c;
}

body[data-theme="dark"] .leaderboard-period-btn.active {
  background: #2a2a2a;
  color: #ff6b81;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

body[data-theme="dark"] .leaderboard-my-card {
  background: rgba(255, 36, 66, 0.08);
  border-color: rgba(255, 36, 66, 0.25);
}

body[data-theme="dark"] .leaderboard-my-card--empty {
  background: #1f1f1f;
  border-color: #2a2a2a;
}

body[data-theme="dark"] .leaderboard-my__rank-num,
body[data-theme="dark"] .leaderboard-my__reward-value,
body[data-theme="dark"] .podium-card__amount,
body[data-theme="dark"] .leaderboard-row__num {
  color: #ff6b81;
}

body[data-theme="dark"] .leaderboard-my__reward--no .leaderboard-my__reward-value {
  color: #8c8c8c;
}

body[data-theme="dark"] .podium-card {
  background: #1f1f1f;
  border-color: #2a2a2a;
}

body[data-theme="dark"] .podium-card--first {
  background: linear-gradient(180deg, #3a2a1a 0%, #1f1f1f 100%);
  border-color: rgba(255, 193, 7, 0.3);
}

body[data-theme="dark"] .podium-card--second {
  background: linear-gradient(180deg, #2a2a2a 0%, #1f1f1f 100%);
}

body[data-theme="dark"] .podium-card--third {
  background: linear-gradient(180deg, #3a2a1a 0%, #1f1f1f 100%);
  border-color: rgba(205, 127, 50, 0.3);
}

body[data-theme="dark"] .leaderboard-row {
  background: #1f1f1f;
  border-color: #2a2a2a;
}

body[data-theme="dark"] .leaderboard-row.is-me {
  background: rgba(255, 36, 66, 0.08);
  border-color: rgba(255, 36, 66, 0.25);
}

body[data-theme="dark"] .leaderboard-row__avatar,
body[data-theme="dark"] .podium-card__avatar {
  background: #2a2a2a;
  color: #a6a6a6;
}

body[data-theme="dark"] .podium-card--first .podium-card__avatar,
body[data-theme="dark"] .podium-card--second .podium-card__avatar,
body[data-theme="dark"] .podium-card--third .podium-card__avatar {
  color: #fff;
}

body[data-theme="dark"] .leaderboard-row__badge {
  background: rgba(255, 36, 66, 0.15);
  color: #ff6b81;
}

body[data-theme="dark"] .leaderboard-row__tag {
  background: rgba(255, 36, 66, 0.15);
  color: #ff6b81;
}

body[data-theme="dark"] .leaderboard-empty {
  background: #1f1f1f;
  border-color: #2a2a2a;
}

body[data-theme="dark"] .leaderboard-toc-track::before {
  background: #2a2a2a;
}

body[data-theme="dark"] .leaderboard-toc-item {
  color: #8c8c8c;
}

body[data-theme="dark"] .leaderboard-toc-item:hover {
  color: #ff6b81;
  background: rgba(255, 36, 66, 0.12);
}

body[data-theme="dark"] .leaderboard-toc-item.active {
  color: #ff6b81;
}

body[data-theme="dark"] .leaderboard-toc-item.active .leaderboard-toc-dot {
  background: #ff6b81;
  box-shadow: 0 0 0 3px rgba(255, 77, 111, 0.2);
}

body[data-theme="dark"] .leaderboard-toc-dot {
  background: #595959;
}

body[data-theme="dark"] .leaderboard-toc-title-icon {
  color: #ff6b81;
}

body[data-theme="dark"] .mobile-toc-btn {
  background: #1f1f1f;
  color: #ff6b81;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3);
}

body[data-theme="dark"] .mobile-toc-item {
  color: rgba(255, 255, 255, 0.65);
}

body[data-theme="dark"] .mobile-toc-item:hover,
body[data-theme="dark"] .mobile-toc-item.active {
  color: #ff6b81;
  background: rgba(255, 36, 66, 0.12);
}

body[data-theme="dark"] .mobile-toc-item.active .mobile-toc-dot {
  background: #ff6b81;
}

body[data-theme="dark"] .mobile-toc-dot {
  background: #595959;
}

body[data-theme="dark"] .leaderboard-rules-list {
  color: #b0b0b0;
}

body[data-theme="dark"] .leaderboard-rules-highlight {
  color: #ff6b81;
}

body[data-theme="dark"] .leaderboard-rules-footer {
  border-top-color: #2a2a2a;
}

body[data-theme="dark"] .leaderboard-rules-modal .ant-modal-content,
body[data-theme="dark"] .leaderboard-rules-modal .ant-modal-header {
  background: #1f1f1f;
  border-color: #2a2a2a;
}

body[data-theme="dark"] .leaderboard-rules-modal .ant-modal-title {
  color: #f0f0f0;
}

body[data-theme="dark"] .leaderboard-rules-modal .ant-modal-close {
  color: #8c8c8c;
}

body[data-theme="dark"] .leaderboard-rules-modal .ant-modal-close:hover {
  color: #ff2442;
}
</style>
