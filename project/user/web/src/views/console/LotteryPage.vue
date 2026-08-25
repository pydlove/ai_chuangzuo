<template>
  <div class="lottery-page">
    <NavBar v-if="showNavBar" :links="navLinks" :cta-to="ctaTo" :cta-label="ctaLabel" />

    <!-- 活动头图 -->
    <section class="lottery-hero" :class="{ 'has-image': hasHeroImage }" :style="heroStyle">
      <div class="lottery-hero__mask" :style="maskStyle" />
      <div class="lottery-hero__content">
        <span class="lottery-hero__badge"><GiftOutlined /> 幸运抽奖</span>
        <h1 class="lottery-hero__title">{{ campaign?.name || '幸运大抽奖' }}</h1>
        <p class="lottery-hero__desc">{{ campaign?.description || '参与活动，赢取会员、创作币、折扣券等惊喜好礼' }}</p>
        <p v-if="campaign" class="lottery-hero__time">
          活动时间：{{ formatTime(campaign.startTime) }} ~ {{ formatTime(campaign.endTime) }}
        </p>
      </div>
    </section>

    <div v-if="campaign" class="lottery-body">
      <div class="lottery-content">
        <!-- 奖项展示 -->
        <section class="prize-section">
          <div class="section-title">
            <span class="section-title__tag" />
            <span>丰厚奖品</span>
          </div>
          <div class="prize-preview-grid">
            <div v-for="tier in previewTiers" :key="tier.id" class="prize-preview-card">
              <div v-if="tier.prizeLevel" class="prize-preview-card__badge" :class="levelClass(tier.prizeLevel)">{{ prizeLevelText(tier.prizeLevel) }}</div>
              <div class="prize-preview-card__icon"><component :is="prizeIcon(tier.rewardType)" /></div>
              <div class="prize-preview-card__info">
                <div class="prize-preview-card__name">{{ tier.tierName }}</div>
                <div class="prize-preview-card__remaining">
                  剩余 {{ tier.displayRemainingCount != null ? tier.displayRemainingCount : (tier.remainingWinCount != null ? tier.remainingWinCount : '充足') }}
                </div>
              </div>
            </div>
          </div>
          <button v-if="hasMoreTiers" class="prize-view-all" @click="prizeModalVisible = true">
            查看全部 {{ campaign.tiers.length }} 个奖品
          </button>
        </section>

        <!-- 盲盒抽奖 -->
        <section class="wheel-section">
          <div class="section-title">
            <span class="section-title__tag" />
            <span>开启宝箱</span>
          </div>

          <div class="chest-stage">
            <div
              v-for="n in boxes"
              :key="n"
              class="chest-wrap"
              :class="{
                disabled: !canDraw,
                rolling: highlightedBox === n && boxState === 'rolling',
                shaking: selectedBox === n && boxState === 'shaking',
                opening: selectedBox === n && boxState === 'opening',
                revealing: selectedBox === n && boxState === 'revealing'
              }"
              @click="handleBoxClick(n)"
            >
              <div class="chest">
                <div class="chest-glow" />
                <img src="/gift-box.svg" class="gift-box-img" alt="礼盒" />
              </div>
              <div class="chest-label">礼盒 {{ n }}</div>

              <div v-if="selectedBox === n && boxState === 'revealing'" class="prize-fly">
                <div class="prize-fly__icon"><component :is="resultIcon" /></div>
                <div class="prize-fly__name">{{ resultTitle }}</div>
              </div>

              <div v-if="selectedBox === n && boxState === 'revealing'" class="confetti">
                <span
                  v-for="i in 8"
                  :key="i"
                  class="confetti-piece"
                  :style="confettiStyle(i)"
                />
              </div>
            </div>
          </div>

          <div class="draw-actions">
            <button
              class="draw-action-btn"
              :class="{ disabled: !canDraw, drawing: drawing }"
              :disabled="!canDraw"
              @click="handleRollDraw"
            >
              {{ drawButtonText }}
            </button>

            <button class="share-action-btn" @click="handleShare">
              <ShareAltOutlined /> 分享活动
            </button>
          </div>

          <p class="wheel-hint">{{ wheelHintText }}</p>
        </section>

        <!-- 活动规则 -->
        <section v-if="campaign.rules" class="rules-section">
          <div class="section-title rules-section__header" @click="rulesExpanded = !rulesExpanded">
            <span class="section-title__tag" />
            <span>活动规则</span>
            <span class="rules-toggle">{{ rulesExpanded ? '收起' : '展开' }}</span>
          </div>
          <div v-show="rulesExpanded" class="rules-card">
            <div class="rules-content" v-html="renderedRules" />
          </div>
        </section>

        <!-- 我的兑换码 -->
        <section class="codes-section">
          <div class="section-title">
            <span class="section-title__tag" />
            <span>我的兑换码</span>
          </div>
          <div class="codes-card">
            <a-empty v-if="!myCodes.length" description="还没有兑换码" />
            <div v-else class="code-list">
              <div v-for="item in myCodes" :key="item.id" class="code-row">
                <div class="code-row__main">
                  <div class="code-row__prize-line">
                    <a-tag color="green">{{ prizeTypeLabel(item.rewardType) }}</a-tag>
                    <span class="code-row__prize">{{ prizeSummary(item) }}</span>
                  </div>
                  <div class="code-row__code-line">
                    <span class="code-row__value">{{ item.code }}</span>
                  </div>
                </div>
                <div class="code-row__actions">
                  <span class="code-row__status" :class="item.status">{{ statusText(item.status) }}</span>
                  <a-button v-if="item.status === 'unused'" size="small" type="primary" @click="handleRedeem(item.code)">
                    立即兑换
                  </a-button>
                </div>
              </div>
            </div>
          </div>
        </section>

        <!-- 中奖展示墙 -->
        <section class="winners-section">
          <div class="section-title">
            <span class="section-title__tag" />
            <span>中奖展示墙</span>
          </div>
          <div class="winners-card">
            <a-empty v-if="!displayWinners.length" description="暂无中奖记录" />
            <div v-else class="winner-list">
              <div v-for="w in displayWinners" :key="w.id" class="winner-row">
                <a-avatar :src="w.avatarUrl || defaultAvatar" />
                <div class="winner-row__meta">
                  <span class="winner-row__name">{{ w.nickname || '幸运用户' }}</span>
                  <span class="winner-row__prize">{{ w.prizeName }}</span>
                </div>
                <span class="winner-row__time">{{ formatTime(w.winTime) }}</span>
              </div>
            </div>
          </div>
        </section>
      </div>
    </div>

    <div v-else class="lottery-body">
      <div class="lottery-content">
        <div class="empty-state">
          <div class="empty-state__icon"><GiftOutlined /></div>
          <div class="empty-state__title">暂无进行中的抽奖活动</div>
          <p class="empty-state__desc">活动筹备中，敬请期待下一次惊喜～</p>
        </div>
      </div>
    </div>

    <!-- 底部 -->
    <footer class="lottery-footer">
      <span>© 2026 爱创作 · 杭州爱启云网络科技有限公司 · All Rights Reserved</span>
      <span>浙ICP备2025200943号-2</span>
    </footer>

    <!-- 中奖结果弹窗 -->
    <a-modal
      v-model:open="resultVisible"
      :footer="null"
      :closable="true"
      width="380px"
      centered
      class="lottery-result-modal"
    >
      <div class="result-body">
        <div class="result-ribbon"><GiftOutlined /> 恭喜中奖</div>
        <div class="result-prize">
          <div class="result-prize__icon"><component :is="resultIcon" /></div>
          <div class="result-prize__name">{{ resultTitle }}</div>
        </div>
        <div v-if="resultCode" class="result-code-box">
          <span class="result-code-box__text">{{ resultCode }}</span>
          <a-button size="small" type="primary" @click="copyCode">复制</a-button>
        </div>
        <p class="result-tip">{{ resultCode ? '兑换码可在“我的兑换码”中查看' : '感谢参与，下次好运' }}</p>
        <div class="result-actions">
          <a-button v-if="resultCode" type="primary" shape="round" size="large" @click="handleRedeem(resultCode)">立即兑换</a-button>
          <a-button shape="round" size="large" :block="!resultCode" @click="resultVisible = false">知道了</a-button>
        </div>
      </div>
    </a-modal>

    <!-- 全部奖品弹窗 -->
    <a-modal
      v-model:open="prizeModalVisible"
      title="丰厚奖品"
      :footer="null"
      :closable="true"
      width="92vw"
      centered
      class="prize-modal"
    >
      <div class="prize-modal-list">
        <div v-for="tier in sortedTiers" :key="tier.id" class="prize-modal-item">
          <div class="prize-modal-item__icon"><component :is="prizeIcon(tier.rewardType)" /></div>
          <div class="prize-modal-item__info">
            <div class="prize-modal-item__name">
              <span v-if="tier.prizeLevel" class="prize-modal-item__badge" :class="levelClass(tier.prizeLevel)">{{ prizeLevelText(tier.prizeLevel) }}</span>
              <span class="prize-modal-item__name-text">{{ tier.tierName }}</span>
            </div>
            <div class="prize-modal-item__meta">
              {{ prizeTypeLabel(tier.rewardType) }} · 剩余 {{ tier.displayRemainingCount != null ? tier.displayRemainingCount : (tier.remainingWinCount != null ? tier.remainingWinCount : '充足') }}
            </div>
          </div>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import MarkdownIt from 'markdown-it'
import { GiftOutlined, CrownOutlined, MoneyCollectOutlined, TagsOutlined, SmileOutlined, ShareAltOutlined } from '@ant-design/icons-vue'
import { getCurrentCampaign, getChances, draw, getDisplayWinners, getMyCodes, redeem } from '@/api/lottery'
import { getShareConfig } from '@/api/shareConfig'
import { copyToClipboard } from '@/utils/copy.js'
import { useUserProfile } from '@/composables/useUserProfile.js'
import NavBar from '@/components/layout/NavBar.vue'

const router = useRouter()
const route = useRoute()
const md = new MarkdownIt()
const { profile, loadProfile } = useUserProfile()

const campaign = ref(null)
const chances = ref(null)
const myCodes = ref([])
const displayWinners = ref([])
const drawing = ref(false)
const resultVisible = ref(false)
const resultTitle = ref('')
const resultCode = ref('')
const resultIcon = ref(GiftOutlined)
const selectedBox = ref(null)
const highlightedBox = ref(null)
const boxState = ref('idle')
const rulesExpanded = ref(false)
const prizeModalVisible = ref(false)
const defaultAvatar = 'https://api.dicebear.com/7.x/avataaars/svg?seed=default'
const shareConfig = ref(null)

const BOX_COUNT = 10
const boxes = Array.from({ length: BOX_COUNT }, (_, i) => i + 1)
const MIN_ANIMATION_MS = 2200

const navLinks = [
  { to: '/', label: '首页' },
  { to: '/pricing', label: '会员' },
  { to: '/lottery', label: '活动' },
  { to: '/guide', label: '玩法指南' },
  { to: '/learn', label: '创作学院' }
]
const ctaTo = '/console/workbench'
const ctaLabel = '开始创作'

const isLoggedIn = computed(() => !!localStorage.getItem('aichuangzuo_access_token'))

const inviteCode = computed(() => profile.value?.inviteCode || '')

const showNavBar = computed(() => route.name !== 'ConsoleLottery')

const PREVIEW_COUNT = 4
const previewTiers = computed(() => {
  const sorted = [...(campaign.value?.tiers || [])].sort((a, b) => (a.prizeLevel ?? 99) - (b.prizeLevel ?? 99))
  return sorted.slice(0, PREVIEW_COUNT)
})
const sortedTiers = computed(() => {
  return [...(campaign.value?.tiers || [])].sort((a, b) => (a.prizeLevel ?? 99) - (b.prizeLevel ?? 99))
})
const hasMoreTiers = computed(() => (campaign.value?.tiers?.length || 0) > PREVIEW_COUNT)

const LOTTERY_HERO_IMAGE = 'https://foruda.gitee.com/images/1787578298608980753/dfb1def2_8060302.png'

const isConsoleLottery = computed(() => route.name === 'ConsoleLottery')

const heroImageUrl = computed(() => {
  if (isConsoleLottery.value) return campaign.value?.imageUrl || ''
  return campaign.value?.imageUrl || LOTTERY_HERO_IMAGE
})
const hasHeroImage = computed(() => !!heroImageUrl.value)

const heroStyle = computed(() => {
  const style = {}
  if (heroImageUrl.value) {
    style.backgroundImage = `url(${heroImageUrl.value})`
  } else {
    style.backgroundImage = 'linear-gradient(180deg, #FFE5EB 0%, #fff 100%)'
  }
  if (!showNavBar.value) {
    style.paddingTop = '40px'
  }
  return style
})

const maskStyle = computed(() => {
  if (hasHeroImage.value) {
    return { background: 'linear-gradient(180deg, rgba(0,0,0,0.2) 0%, rgba(0,0,0,0.5) 100%)' }
  }
  return { background: 'linear-gradient(180deg, rgba(255,255,255,0.1) 0%, rgba(255,255,255,0.3) 100%)' }
})

const renderedRules = computed(() => {
  if (!campaign.value?.rules) return ''
  return md.render(campaign.value.rules)
})

const canDraw = computed(() => {
  if (!isLoggedIn.value) return true
  if (!campaign.value) return false
  if (drawing.value) return false
  if (!chances.value) return false
  return chances.value.availableChances > 0
})

const drawButtonText = computed(() => {
  if (!isLoggedIn.value) return '登录后即可抽奖'
  if (!chances.value) return '加载中...'
  if (chances.value.availableChances <= 0) return '今日次数已用完'
  return '立即开启宝箱'
})

const wheelHintText = computed(() => {
  if (!isLoggedIn.value) return '登录后即可参与抽奖，赢取丰厚奖品'
  if (!chances.value) return ''
  if (chances.value.availableChances <= 0) return '暂无可用次数，邀请好友注册可获得更多机会'
  return `剩余 ${chances.value.availableChances} 次抽奖机会`
})

function prizeIcon(type) {
  const map = { coin: MoneyCollectOutlined, membership: CrownOutlined, coupon: TagsOutlined, none: SmileOutlined }
  return map[type] || GiftOutlined
}

function prizeTypeLabel(type) {
  const map = { coin: '创作币', membership: '会员', coupon: '折扣券', none: '好礼' }
  return map[type] || type
}

const prizeLevelMap = { 1: '特等奖', 2: '一等奖', 3: '二等奖', 4: '三等奖', 5: '四等奖', 6: '五等奖', 7: '六等奖', 8: '七等奖', 9: '八等奖' }

function prizeLevelText(level) {
  return prizeLevelMap[level] || ''
}

function levelClass(level) {
  return level != null ? `level-${level}` : ''
}

function isHighPrizeLevel(level) {
  return level != null && level <= 2
}

onMounted(() => {
  loadCampaign()
  loadShareConfig()
  if (isLoggedIn.value) {
    loadProfile()
  }
})

watch(campaign, (val) => {
  if (val) {
    loadWinners()
    if (isLoggedIn.value) {
      loadChances()
      loadMyCodes()
    }
  }
})

watch(resultVisible, (val) => {
  if (!val) resetBox()
})

async function loadCampaign() {
  try {
    const res = await getCurrentCampaign()
    campaign.value = res.data
  } catch (e) {
    message.error('加载活动失败')
  }
}

async function loadChances() {
  if (!campaign.value || !isLoggedIn.value) return
  try {
    const res = await getChances(campaign.value.id)
    chances.value = res.data
  } catch (e) {
    // ignore
  }
}

async function loadWinners() {
  if (!campaign.value) return
  try {
    const res = await getDisplayWinners(campaign.value.id)
    displayWinners.value = res.data || []
  } catch (e) {
    // ignore
  }
}

async function loadMyCodes() {
  if (!isLoggedIn.value) return
  try {
    const res = await getMyCodes()
    myCodes.value = res.data || []
  } catch (e) {
    // ignore
  }
}

function handleBoxClick(boxIndex) {
  if (drawing.value) return
  performDraw(boxIndex, false)
}

function handleRollDraw() {
  if (drawing.value) return
  const target = boxes[Math.floor(Math.random() * BOX_COUNT)]
  performDraw(target, true)
}

async function performDraw(targetBox, roll = false) {
  if (!isLoggedIn.value) {
    router.push({ path: '/login', query: { redirect: '/lottery' } })
    return
  }
  if (!campaign.value || !chances.value || chances.value.availableChances <= 0) return

  drawing.value = true
  selectedBox.value = targetBox

  try {
    let animationReady
    if (roll) {
      animationReady = runRollAnimation(targetBox)
    } else {
      boxState.value = 'shaking'
      setTimeout(() => {
        if (boxState.value === 'shaking') boxState.value = 'opening'
      }, 300)
      animationReady = Promise.resolve()
    }

    const [res] = await Promise.all([
      draw(campaign.value.id),
      animationReady,
      new Promise((resolve) => setTimeout(resolve, MIN_ANIMATION_MS))
    ])

    setResultData(res.data)
    boxState.value = 'revealing'
    setTimeout(() => {
      resultVisible.value = true
      loadChances()
      loadMyCodes()
    }, 600)
  } catch (e) {
    message.error(e.response?.data?.message || '抽奖失败')
    resetBox()
  }
}

function runRollAnimation(targetBox) {
  return new Promise((resolve) => {
    boxState.value = 'rolling'
    const start = Date.now()
    const duration = 1600
    let speed = 60
    let current = boxes[0]
    highlightedBox.value = current
    const step = () => {
      const elapsed = Date.now() - start
      if (elapsed >= duration) {
        highlightedBox.value = targetBox
        boxState.value = 'opening'
        resolve()
        return
      }
      let next
      do {
        next = boxes[Math.floor(Math.random() * BOX_COUNT)]
      } while (next === current)
      current = next
      highlightedBox.value = current
      speed = Math.min(260, speed + 12)
      setTimeout(step, speed)
    }
    step()
  })
}

function setResultData(data) {
  resultTitle.value = data.tierName || '谢谢回顾'
  resultCode.value = data.code || ''
  resultIcon.value = data.code ? prizeIcon(data.rewardType) : SmileOutlined
}

function resetBox() {
  drawing.value = false
  boxState.value = 'idle'
  selectedBox.value = null
  highlightedBox.value = null
}

function confettiStyle(index) {
  const angle = (index - 1) * 45 * (Math.PI / 180)
  const distance = 60 + Math.random() * 30
  const tx = Math.cos(angle) * distance
  const ty = Math.sin(angle) * distance - 30
  const rotate = Math.random() * 360
  const hue = (index - 1) * 45
  return {
    backgroundColor: `hsl(${hue}, 85%, 60%)`,
    transform: `translate(-50%, -50%) rotate(${rotate}deg)`,
    '--tx': `${tx}px`,
    '--ty': `${ty}px`
  }
}

async function handleRedeem(code) {
  try {
    await redeem(code)
    message.success('兑换成功')
    loadMyCodes()
  } catch (e) {
    message.error(e.response?.data?.message || '兑换失败')
  }
}

async function copyCode() {
  if (!resultCode.value) return
  try {
    await copyToClipboard(resultCode.value)
    message.success('已复制')
  } catch {
    message.error('复制失败，请长按手动复制')
  }
}

async function loadShareConfig() {
  try {
    const res = await getShareConfig('lottery')
    shareConfig.value = res.data
  } catch (e) {
    // ignore
  }
}

async function handleShare() {
  const campaignName = campaign.value?.name || '幸运大抽奖'
  const url = window.location.href
  const inviteUrl = inviteCode.value
    ? `${window.location.origin}/login?ref=${inviteCode.value}`
    : url
  let text = shareConfig.value?.content
  if (!text) {
    text = `🎁 ${campaignName}来啦！我在爱创作发现了超多好礼，会员、创作币、折扣券等你来抽～\n快来一起参与：{inviteUrl}`
  }
  text = text
    .replace(/{title}/g, campaignName)
    .replace(/{inviteUrl}/g, inviteUrl)
    .replace(/{url}/g, url)
    .replace(/{code}/g, inviteCode.value || '')
  try {
    await copyToClipboard(text)
    Modal.success({
      title: '文案已复制',
      content: '快去粘贴给好友吧～好友使用你的邀请码注册，还能额外获得一次抽奖机会！',
      okText: '知道了',
      class: 'lottery-share-modal'
    })
  } catch {
    message.error('复制失败，请长按手动复制')
  }
}

function statusText(status) {
  const map = { unused: '未使用', used: '已使用', expired: '已过期' }
  return map[status] || status
}

function prizeSummary(item) {
  if (!item.tierName) return item.rewardSummary || ''
  if (!item.rewardSummary) return item.tierName
  return `${item.tierName} · ${item.rewardSummary}`
}

function formatTime(t) {
  if (!t) return ''
  const d = new Date(t)
  return `${d.getMonth() + 1}月${d.getDate()}日 ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}
</script>

<style scoped>
.lottery-page {
  min-height: 100vh;
  background: #f8f9fa;
  display: flex;
  flex-direction: column;
}

.lottery-hero {
  position: relative;
  min-height: 280px;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  background-size: cover;
  background-position: center;
  color: #fff;
  padding: 80px 24px 40px;
  box-sizing: border-box;
}

.lottery-hero__mask {
  position: absolute;
  inset: 0;
}

.lottery-hero__content {
  position: relative;
  z-index: 1;
  max-width: 720px;
}

.lottery-hero__badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 1px;
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid #FFCBD4;
  color: #FF2442;
  padding: 6px 14px;
  border-radius: 999px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(255, 36, 66, 0.08);
}

.lottery-hero__badge :deep(svg) {
  width: 14px;
  height: 14px;
}

.lottery-hero__title {
  font-size: 36px;
  font-weight: 800;
  margin: 0 0 12px;
  color: #1a1a1a;
  line-height: 1.2;
}

.lottery-hero__desc {
  font-size: 16px;
  margin: 0 0 16px;
  color: #595959;
  line-height: 1.6;
}

.lottery-hero__time {
  font-size: 13px;
  margin: 0;
  color: #8c8c8c;
}

.lottery-hero.has-image .lottery-hero__title,
.lottery-hero.has-image .lottery-hero__desc,
.lottery-hero.has-image .lottery-hero__time {
  color: #fff;
}

.lottery-hero.has-image .lottery-hero__badge {
  background: rgba(255, 255, 255, 0.95);
}

.lottery-body {
  flex: 1;
  padding: 32px 24px 48px;
}

.lottery-content {
  max-width: 900px;
  margin: 0 auto;
  width: 100%;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 20px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 18px;
}

.section-title__tag {
  width: 4px;
  height: 20px;
  background: var(--color-primary);
  border-radius: 2px;
}

.rules-section__header {
  cursor: pointer;
  user-select: none;
}

.rules-toggle {
  margin-left: auto;
  font-size: 13px;
  font-weight: 500;
  color: var(--color-primary);
}

.prize-section,
.rules-section,
.wheel-section,
.codes-section,
.winners-section {
  margin-bottom: 36px;
}

.prize-preview-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.prize-preview-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.04);
}

.prize-preview-card__badge {
  position: absolute;
  top: -6px;
  right: 8px;
  font-size: 10px;
  font-weight: 700;
  color: #fff;
  background: #bfbfbf;
  padding: 2px 8px;
  border-radius: 999px;
  line-height: 1.4;
}

.prize-preview-card__badge.level-1 {
  background: linear-gradient(135deg, #FFD700 0%, #FF8C00 100%);
  box-shadow: 0 2px 6px rgba(255, 140, 0, 0.3);
}

.prize-preview-card__badge.level-2 {
  background: linear-gradient(135deg, #FF8C00 0%, #FF5E3A 100%);
  box-shadow: 0 2px 6px rgba(255, 94, 58, 0.25);
}

.prize-preview-card__badge.level-3 {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  box-shadow: 0 2px 6px rgba(118, 75, 162, 0.25);
}

.prize-preview-card__badge.level-4 {
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
  box-shadow: 0 2px 6px rgba(17, 153, 142, 0.2);
}

.prize-preview-card__badge.level-5 {
  background: linear-gradient(135deg, #00b4db 0%, #0083b0 100%);
  box-shadow: 0 2px 6px rgba(0, 131, 176, 0.2);
}

.prize-preview-card__badge.level-6,
.prize-preview-card__badge.level-7,
.prize-preview-card__badge.level-8,
.prize-preview-card__badge.level-9 {
  color: #fff;
  background: #8c8c8c;
}

.prize-preview-card__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(255, 36, 66, 0.08);
  color: var(--color-primary);
  flex-shrink: 0;
}

.prize-preview-card__icon :deep(svg) {
  width: 18px;
  height: 18px;
}

.prize-preview-card__info {
  min-width: 0;
}

.prize-preview-card__name {
  font-size: 13px;
  font-weight: 600;
  color: #1a1a1a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 2px;
}

.prize-preview-card__remaining {
  font-size: 11px;
  font-weight: 500;
  color: var(--color-primary);
}

.prize-view-all {
  display: block;
  width: 100%;
  margin-top: 12px;
  padding: 10px 0;
  border: none;
  border-radius: 999px;
  background: #fff;
  color: var(--color-primary);
  font-size: 13px;
  font-weight: 600;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.04);
  cursor: pointer;
}

.prize-modal :deep(.ant-modal) {
  width: 92vw !important;
  max-width: 420px;
}

.prize-modal :deep(.ant-modal-body) {
  max-height: 60vh;
  overflow-y: auto;
  padding: 16px;
}

.prize-modal-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.prize-modal-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  background: #fafafa;
  border-radius: 12px;
}

.prize-modal-item__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: rgba(255, 36, 66, 0.08);
  color: var(--color-primary);
  flex-shrink: 0;
}

.prize-modal-item__icon :deep(svg) {
  width: 20px;
  height: 20px;
}

.prize-modal-item__info {
  min-width: 0;
  flex: 1;
}

.prize-modal-item__name {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 2px;
}

.prize-modal-item__badge {
  flex-shrink: 0;
  font-size: 10px;
  font-weight: 700;
  color: #fff;
  background: #bfbfbf;
  padding: 2px 8px;
  border-radius: 999px;
  line-height: 1.4;
}

.prize-modal-item__badge.level-1 {
  background: linear-gradient(135deg, #FFD700 0%, #FF8C00 100%);
  box-shadow: 0 2px 6px rgba(255, 140, 0, 0.3);
}

.prize-modal-item__badge.level-2 {
  background: linear-gradient(135deg, #FF8C00 0%, #FF5E3A 100%);
  box-shadow: 0 2px 6px rgba(255, 94, 58, 0.25);
}

.prize-modal-item__badge.level-3 {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  box-shadow: 0 2px 6px rgba(118, 75, 162, 0.25);
}

.prize-modal-item__badge.level-4 {
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
  box-shadow: 0 2px 6px rgba(17, 153, 142, 0.2);
}

.prize-modal-item__badge.level-5 {
  background: linear-gradient(135deg, #00b4db 0%, #0083b0 100%);
  box-shadow: 0 2px 6px rgba(0, 131, 176, 0.2);
}

.prize-modal-item__badge.level-6,
.prize-modal-item__badge.level-7,
.prize-modal-item__badge.level-8,
.prize-modal-item__badge.level-9 {
  color: #fff;
  background: #8c8c8c;
}

.prize-modal-item__name-text {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.prize-modal-item__meta {
  font-size: 12px;
  color: #8c8c8c;
}

.rules-card,
.codes-card,
.winners-card {
  background: #fff;
  border-radius: 18px;
  padding: 24px;
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.04);
}

.rules-content {
  font-size: 14px;
  line-height: 1.8;
  color: #595959;
}

.rules-content :deep(h2) {
  font-size: 18px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0 0 12px;
}

.rules-content :deep(p) {
  margin: 0 0 10px;
}

.rules-content :deep(ol),
.rules-content :deep(ul) {
  padding-left: 20px;
  margin: 0 0 10px;
}

.rules-content :deep(li) {
  margin-bottom: 6px;
}

.rules-content :deep(strong) {
  color: #1a1a1a;
  font-weight: 600;
}

.chest-stage {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 20px;
  max-width: 640px;
  margin: 0 auto;
  padding: 32px 0 40px;
}

.chest-wrap {
  position: relative;
  width: 100%;
  height: auto;
  cursor: pointer;
  transition: transform 0.25s ease;
}

.chest-wrap:hover:not(.disabled):not(.drawing) {
  transform: translateY(-6px);
}

.chest-wrap.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.chest-wrap.disabled:hover {
  transform: none;
}

.chest-wrap.drawing {
  cursor: wait;
}

.chest-wrap.rolling .gift-box-img,
.chest-wrap.shaking .chest {
  animation: chest-shake 0.15s ease-in-out infinite;
}

.chest-wrap.rolling .gift-box-img {
  filter: drop-shadow(0 0 18px rgba(255, 36, 66, 0.55));
  transform: scale(1.05);
}

.chest-wrap.opening .gift-box-img,
.chest-wrap.revealing .gift-box-img {
  transform: scale(1.08);
}

.chest {
  position: relative;
  width: 100%;
  padding-bottom: 100%;
}

.gift-box-img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
  transition: transform 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
  filter: drop-shadow(0 8px 20px rgba(255, 36, 66, 0.16));
}

.chest-glow {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 220%;
  height: 220%;
  transform: translate(-50%, -50%) scale(0);
  background: radial-gradient(circle, rgba(255, 36, 66, 0.55) 0%, rgba(255, 36, 66, 0.18) 40%, transparent 70%);
  opacity: 0;
  transition: all 0.6s ease;
  pointer-events: none;
  z-index: 1;
}

.chest-wrap.opening .chest-glow,
.chest-wrap.revealing .chest-glow {
  opacity: 1;
  transform: translate(-50%, -50%) scale(1.25);
}

.chest-label {
  text-align: center;
  margin-top: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #8c8c8c;
}

.draw-action-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 180px;
  padding: 12px 32px;
  border: none;
  border-radius: 999px;
  background: linear-gradient(135deg, var(--color-primary) 0%, #FF4D6F 100%);
  color: #fff;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 6px 20px rgba(255, 36, 66, 0.3);
  transition: all 0.2s ease;
  white-space: nowrap;
}

.draw-action-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(255, 36, 66, 0.38);
}

.draw-action-btn:active:not(:disabled) {
  transform: translateY(0);
}

.draw-action-btn.disabled {
  background: #d9d9d9;
  cursor: not-allowed;
  box-shadow: none;
}

.draw-action-btn.drawing {
  cursor: wait;
}

.draw-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 4px;
}

.share-action-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px 24px;
  border: 1px solid var(--color-primary);
  border-radius: 999px;
  background: #fff;
  color: var(--color-primary);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.share-action-btn:hover {
  background: var(--color-primary);
  color: #fff;
}

.share-action-btn :deep(svg) {
  width: 16px;
  height: 16px;
}

.prize-fly {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%) scale(0);
  opacity: 0;
  text-align: center;
  z-index: 10;
  pointer-events: none;
  animation: prize-pop 0.9s cubic-bezier(0.34, 1.56, 0.64, 1) forwards;
}

.prize-fly__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  margin: 0 auto 8px;
  border-radius: 50%;
  background: #fff;
  color: var(--color-primary);
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.12);
}

.prize-fly__icon :deep(svg) {
  width: 32px;
  height: 32px;
}

.prize-fly__name {
  font-size: 14px;
  font-weight: 700;
  color: #fff;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
  white-space: nowrap;
}

.confetti {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 0;
  height: 0;
  z-index: 20;
  pointer-events: none;
}

.confetti-piece {
  position: absolute;
  top: 0;
  left: 0;
  width: 6px;
  height: 14px;
  border-radius: 2px;
  opacity: 0;
}

.revealing .confetti-piece {
  animation: confetti-burst 0.9s ease-out forwards;
  animation-delay: calc(var(--i) * 0.03s);
}

.wheel-hint {
  text-align: center;
  font-size: 14px;
  color: #8c8c8c;
  margin: 18px 0 0;
}

@keyframes chest-shake {
  0%, 100% { transform: rotate(0deg); }
  25% { transform: rotate(-6deg); }
  75% { transform: rotate(6deg); }
}

@keyframes prize-pop {
  0% { transform: translate(-50%, -50%) scale(0); opacity: 0; }
  50% { transform: translate(-50%, -135%) scale(1.35); opacity: 1; }
  100% { transform: translate(-50%, -105%) scale(1); opacity: 1; }
}

@keyframes confetti-burst {
  0% { transform: translate(-50%, -50%) rotate(0deg) scale(1); opacity: 1; }
  100% { transform: translate(var(--tx), var(--ty)) rotate(720deg) scale(0.4); opacity: 0; }
}

.code-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.code-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  background: #fafafa;
  border-radius: 12px;
}

.code-row__main {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
  flex: 1;
}

.code-row__prize-line {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.code-row__code-line {
  min-width: 0;
}

.code-row__prize {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  min-width: 0;
}

.code-row__value {
  font-family: monospace;
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
}

.code-row__actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.code-row__status {
  font-size: 12px;
  color: #8c8c8c;
}

.code-row__status.used {
  color: #999;
}

.code-row__status.expired {
  color: #ff4d4f;
}

.winner-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.winner-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: #fafafa;
  border-radius: 12px;
}

.winner-row__meta {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.winner-row__name {
  font-size: 14px;
  color: #1a1a1a;
}

.winner-row__prize {
  font-size: 13px;
  color: var(--color-primary);
}

.winner-row__time {
  font-size: 12px;
  color: #bfbfbf;
  white-space: nowrap;
}

.empty-state {
  text-align: center;
  padding: 80px 0;
}

.empty-state__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 80px;
  height: 80px;
  margin: 0 auto 16px;
  border-radius: 50%;
  background: rgba(255, 36, 66, 0.08);
  color: var(--color-primary);
}

.empty-state__icon :deep(svg) {
  width: 40px;
  height: 40px;
}

.empty-state__title {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 8px;
}

.empty-state__desc {
  font-size: 14px;
  color: #8c8c8c;
  margin: 0;
}

.lottery-footer {
  flex: 0 0 auto;
  padding: 24px;
  text-align: center;
  font-size: 12px;
  color: #8c8c8c;
  border-top: 1px solid #f0f0f0;
  background: #fff;
}

.lottery-footer span {
  display: block;
  margin: 4px 0;
}

.result-body {
  text-align: center;
  padding: 8px 4px 4px;
}

.result-ribbon {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 16px;
  font-weight: 700;
  color: var(--color-primary);
  margin-bottom: 16px;
}

.result-ribbon :deep(svg) {
  width: 18px;
  height: 18px;
}

.result-prize__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 80px;
  height: 80px;
  margin: 0 auto 10px;
  border-radius: 50%;
  background: rgba(255, 36, 66, 0.08);
  color: var(--color-primary);
}

.result-prize__icon :deep(svg) {
  width: 40px;
  height: 40px;
}

.result-prize__name {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 20px;
}

.result-code-box {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  background: #FFF5F7;
  border: 1px dashed #FFCBD4;
  border-radius: 12px;
  padding: 12px;
  margin-bottom: 12px;
}

.result-code-box__text {
  font-family: monospace;
  font-size: 18px;
  font-weight: 700;
  color: var(--color-primary);
  letter-spacing: 1px;
}

.result-tip {
  font-size: 13px;
  color: #8c8c8c;
  margin: 0 0 20px;
}

.result-actions {
  display: flex;
  gap: 12px;
}

.result-actions .ant-btn {
  flex: 1;
}

.lottery-result-modal .ant-btn-primary,
.code-row__actions .ant-btn-primary {
  background: var(--color-primary);
  border-color: var(--color-primary);
}

.lottery-result-modal .ant-btn-primary:hover,
.lottery-result-modal .ant-btn-primary:focus,
.code-row__actions .ant-btn-primary:hover,
.code-row__actions .ant-btn-primary:focus {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
}

.lottery-result-modal .ant-btn-primary:active,
.code-row__actions .ant-btn-primary:active {
  background: var(--color-primary-active);
  border-color: var(--color-primary-active);
}

@media (min-width: 769px) {
  .lottery-hero {
    min-height: 320px;
  }

  .lottery-hero__title {
    font-size: 44px;
  }

  .lottery-body {
    padding: 48px 24px 64px;
  }

  .prize-preview-grid {
    grid-template-columns: repeat(4, 1fr);
    gap: 12px;
  }

  .prize-preview-card {
    padding: 12px 14px;
  }

  .prize-preview-card__icon {
    width: 40px;
    height: 40px;
  }

  .prize-preview-card__icon :deep(svg) {
    width: 20px;
    height: 20px;
  }

  .prize-preview-card__name {
    font-size: 14px;
  }

  .prize-preview-card__remaining {
    font-size: 12px;
  }

  .prize-modal :deep(.ant-modal) {
    max-width: 720px;
  }

  .prize-modal :deep(.ant-modal-body) {
    padding: 20px;
  }

  .prize-modal-list {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }

  .prize-modal-item {
    padding: 12px 14px;
  }

  .prize-modal-item__icon {
    width: 44px;
    height: 44px;
  }

  .prize-modal-item__icon :deep(svg) {
    width: 22px;
    height: 22px;
  }

  .prize-modal-item__name-text {
    font-size: 15px;
  }

  .prize-modal-item__meta {
    font-size: 13px;
  }

  .chest-stage {
    grid-template-columns: repeat(5, 1fr);
    gap: 24px;
    max-width: 720px;
  }

  .lottery-footer span {
    display: inline;
    margin: 0 8px;
  }
}

@media (max-width: 768px) {
  .lottery-hero {
    min-height: 220px;
    padding: 72px 16px 32px;
  }

  .lottery-hero__title {
    font-size: 28px;
  }

  .lottery-body {
    padding: 24px 16px;
  }

  .chest-stage {
    grid-template-columns: repeat(5, 1fr);
    gap: 8px;
    max-width: 320px;
    padding: 20px 0 28px;
  }

  .chest-label {
    font-size: 11px;
    margin-top: 4px;
  }

  .draw-actions {
    flex-direction: column;
    gap: 10px;
    width: 100%;
    max-width: 320px;
    margin: 0 auto;
  }

  .draw-action-btn {
    width: 100%;
    min-width: auto;
    padding: 12px 28px;
    font-size: 15px;
  }

  .share-action-btn {
    width: 100%;
    padding: 10px 24px;
  }

  .code-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .code-row__actions {
    width: 100%;
    justify-content: space-between;
  }

  .code-row__prize {
    font-size: 13px;
  }

  .code-row__value {
    font-size: 14px;
  }
}

/* 暗色主题 */
body[data-theme="dark"] .lottery-page {
  background: #141414;
}

body[data-theme="dark"] .section-title {
  color: #f5f5f5;
}

body[data-theme="dark"] .prize-card,
body[data-theme="dark"] .rules-card,
body[data-theme="dark"] .codes-card,
body[data-theme="dark"] .winners-card {
  background: #1f1f1f;
  box-shadow: none;
}

body[data-theme="dark"] .prize-card__name,
body[data-theme="dark"] .winner-row__name,
body[data-theme="dark"] .result-prize__name,
body[data-theme="dark"] .empty-state__title {
  color: #f5f5f5;
}

body[data-theme="dark"] .rules-text,
body[data-theme="dark"] .prize-card__type,
body[data-theme="dark"] .wheel-hint {
  color: #a6a6a6;
}

body[data-theme="dark"] .code-row,
body[data-theme="dark"] .winner-row {
  background: #262626;
}

body[data-theme="dark"] .code-row__value {
  color: #f5f5f5;
}

body[data-theme="dark"] .chest-label {
  color: #a6a6a6;
}

body[data-theme="dark"] .lottery-footer {
  background: #1f1f1f;
  border-top-color: #303030;
  color: #8c8c8c;
}
</style>
