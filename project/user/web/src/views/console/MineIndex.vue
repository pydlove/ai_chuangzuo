<template>
  <div class="mine-page">
    <!-- 用户卡 -->
    <section class="mine-user-card">
      <div class="mine-user-header">
        <div class="mine-user-avatar" @click="triggerAvatarUpload">
          <img
            v-if="profileForm.avatarUrl"
            :src="profileForm.avatarUrl"
            alt="avatar"
            class="mine-avatar-img"
          />
          <span v-else>{{ avatarLetter }}</span>
        </div>
        <input
          ref="mineAvatarInput"
          type="file"
          accept="image/jpeg,image/png,image/jpg"
          style="display: none"
          @change="onMineAvatarChange"
        />
        <div class="mine-user-info">
          <div class="mine-user-name-row">
            <span class="mine-user-name">{{ profileForm.nickname || '爱创作工坊用户' }}</span>
            <span v-if="hasMembership" class="mine-user-vip">
              <CrownOutlined class="mine-user-vip-icon" />
              会员
            </span>
          </div>
          <div v-if="profileForm.bio" class="mine-user-id">{{ phoneForm.phone || emailForm.email || '完善资料，解锁更多权益' }}</div>
          <div class="mine-user-bio" :class="{ empty: !profileForm.bio }" @click="router.push('/console/profile/edit')">
            {{ profileForm.bio || '点击添加简介' }}
          </div>
        </div>
        <div class="mine-user-actions-top">
          <button class="mine-header-icon-btn" @click="router.push('/console/messages')">
            <BellOutlined />
          </button>
          <button class="mine-header-icon-btn" @click="settingsModalVisible = true">
            <SettingOutlined />
          </button>
        </div>
      </div>

      <!-- 核心数据 -->
      <div class="mine-user-stats">
        <div class="mine-user-stat" @click="router.push('/console/earnings')">
          <div class="mine-user-stat-value">{{ coinBalance }}</div>
          <div class="mine-user-stat-label">创作币</div>
        </div>
        <div class="mine-user-stat" @click="router.push('/console/benefits')">
          <div class="mine-user-stat-value">{{ hasMembership ? benefitCount : 0 }}</div>
          <div class="mine-user-stat-label">会员权益</div>
        </div>
        <div class="mine-user-stat" @click="router.push('/console/coupons')">
          <div class="mine-user-stat-value">{{ couponCount }}</div>
          <div class="mine-user-stat-label">优惠券</div>
        </div>
        <div class="mine-user-stat" @click="router.push('/console/invite')">
          <div class="mine-user-stat-value">{{ inviteStats.invitedCount }}</div>
          <div class="mine-user-stat-label">已邀请</div>
        </div>
      </div>
    </section>

    <!-- VIP 卡片 -->
    <section class="mine-vip-card">
      <div class="mine-vip-card-top" @click="router.push('/console/benefits')">
        <div class="mine-vip-content">
          <div class="mine-vip-title">
            <CrownOutlined class="mine-vip-title-icon" />
            {{ hasMembership ? (membershipLevel + '会员') : '开通会员' }}
            <img
              v-if="vipIconSrc"
              :src="vipIconSrc"
              class="mine-vip-title-badge"
              alt=""
            />
          </div>
          <div class="mine-vip-desc">
            {{ hasMembership ? '畅享全部 AI 创作权益' : '解锁更多高级功能' }}
          </div>
        </div>
        <div class="mine-vip-mascot">
          <img
            v-for="(src, idx) in catFrames"
            :key="src"
            :src="src"
            class="mine-vip-mascot-img"
            :style="{ animationDelay: `${idx * 3}s` }"
            alt="会员 mascot"
          />
        </div>
      </div>

      <div class="mine-vip-order-grid">
        <div class="mine-vip-order-item" @click="router.push({ path: '/console/orders', query: { status: 'pending' } })">
          <div class="mine-vip-order-icon mine-vip-order-icon--img">
            <img src="/assets/images/待支付-v1.png" alt="待支付" />
          </div>
          <span class="mine-vip-order-label">待支付</span>
        </div>
        <div class="mine-vip-order-item" @click="router.push({ path: '/console/orders', query: { status: 'paid' } })">
          <div class="mine-vip-order-icon mine-vip-order-icon--img">
            <img src="/assets/images/已支付-v1.jpg" alt="已支付" />
          </div>
          <span class="mine-vip-order-label">已支付</span>
        </div>
        <div class="mine-vip-order-item" @click="router.push('/console/orders')">
          <div class="mine-vip-order-icon mine-vip-order-icon--img">
            <img src="/assets/images/全部订单-v1.jpg" alt="全部订单" />
          </div>
          <span class="mine-vip-order-label">全部订单</span>
        </div>
      </div>
    </section>

    <!-- 热门服务 -->
    <section class="mine-block mine-hot-services-block">
      <div class="mine-section-header">
        <h3 class="mine-section-title">热门服务</h3>
      </div>
      <div class="mine-hot-services">
        <div class="mine-hot-service-item" @click="$router.push('/console/works')">
          <div class="mine-hot-service-info">
            <div class="mine-hot-service-title">我的作品</div>
            <div class="mine-hot-service-subtitle">查看全部创作内容</div>
          </div>
          <div class="mine-hot-service-icon mine-hot-service-icon--img">
            <img src="/assets/images/我的作品-v1.jpg" alt="我的作品" />
          </div>
        </div>
        <div class="mine-hot-service-item" @click="$router.push('/console/skills')">
          <div class="mine-hot-service-info">
            <div class="mine-hot-service-title">我的提示词</div>
            <div class="mine-hot-service-subtitle">管理常用 Prompt</div>
          </div>
          <div class="mine-hot-service-icon mine-hot-service-icon--img">
            <img src="/assets/images/我的提示词-v1.jpg" alt="我的提示词" />
          </div>
        </div>
        <div class="mine-hot-service-item" @click="openAccountCheck">
          <div class="mine-hot-service-info">
            <div class="mine-hot-service-title">账号检测</div>
            <div class="mine-hot-service-subtitle">检测账号健康状态</div>
          </div>
          <div class="mine-hot-service-icon mine-hot-service-icon--img">
            <img src="/assets/images/账号检测-v1.jpg" alt="账号检测" />
          </div>
        </div>
        <div class="mine-hot-service-item" @click="$router.push('/console/hot-search')">
          <div class="mine-hot-service-info">
            <div class="mine-hot-service-title">热搜榜</div>
            <div class="mine-hot-service-subtitle">获取实时热点灵感</div>
          </div>
          <div class="mine-hot-service-icon mine-hot-service-icon--img">
            <img src="/assets/images/热搜榜-v1.jpg" alt="热搜榜" />
          </div>
        </div>
      </div>
    </section>

    <!-- 常用功能 -->
    <section class="mine-block mine-common-functions-block">
      <div class="mine-section-header">
        <h3 class="mine-section-title">常用功能</h3>
      </div>
      <div class="mine-grid">
        <div class="mine-grid-item" @click="actions.openInviteBindingModal">
          <div class="mine-grid-icon mine-grid-icon--img"><img src="/assets/images/changyong/绑定邀请人_compressed-v1.jpg" alt="绑定邀请人" /></div>
          <span class="mine-grid-label">绑定邀请人</span>
        </div>
        <div class="mine-grid-item" @click="actions.openTutorialModal">
          <div class="mine-grid-icon mine-grid-icon--img"><img src="/assets/images/changyong/教程与帮助_compressed-v1.jpg" alt="教程与帮助" /></div>
          <span class="mine-grid-label">教程与帮助</span>
        </div>
        <div class="mine-grid-item" @click="actions.openFeedbackModal">
          <div class="mine-grid-icon mine-grid-icon--img"><img src="/assets/images/changyong/意见反馈_compressed-v1.jpg" alt="意见反馈" /></div>
          <span class="mine-grid-label">意见反馈</span>
        </div>
        <div class="mine-grid-item" @click="actions.openAboutModal">
          <div class="mine-grid-icon mine-grid-icon--img"><img src="/assets/images/changyong/关于我们_compressed-v1.jpg" alt="关于我们" /></div>
          <span class="mine-grid-label">关于我们</span>
        </div>
        <div class="mine-grid-item" @click="actions.openTermsModal">
          <div class="mine-grid-icon mine-grid-icon--img"><img src="/assets/images/changyong/用户协议_compressed-v1.jpg" alt="用户协议" /></div>
          <span class="mine-grid-label">用户协议</span>
        </div>
        <div class="mine-grid-item" @click="actions.openPrivacyModal">
          <div class="mine-grid-icon mine-grid-icon--img"><img src="/assets/images/changyong/隐私政策_compressed-v1.jpg" alt="隐私政策" /></div>
          <span class="mine-grid-label">隐私政策</span>
        </div>
        <div class="mine-grid-item" @click="actions.openWechatModal">
          <div class="mine-grid-icon mine-grid-icon--img"><img src="/assets/images/changyong/关注微信_compressed-v1.jpg" alt="关注微信" /></div>
          <span class="mine-grid-label">关注微信</span>
        </div>
        <div class="mine-grid-item" @click="openOfficialSite">
          <div class="mine-grid-icon mine-grid-icon--img"><img src="/assets/images/changyong/访问官网_compressed-v1.jpg" alt="访问官网" /></div>
          <span class="mine-grid-label">访问官网</span>
        </div>
      </div>
    </section>

    <button class="mine-logout" @click="confirmLogout">
      <LogoutOutlined class="mine-logout-icon" />
      <span>退出登录</span>
    </button>

    <p class="mine-footer">© 2026 爱创作工坊 · 杭州爱启云网络科技有限公司</p>
    <p class="mine-icp">浙ICP备2025200943号-2</p>
    <a-modal
      v-model:open="settingsModalVisible"
      title="设置"
      :footer="null"
      :width="360"
      centered
      class="mine-settings-modal"
    >
      <div class="mine-settings-content">
        <div class="mine-settings-avatar" @click="triggerAvatarUpload">
          <img
            v-if="profileForm.avatarUrl"
            :src="profileForm.avatarUrl"
            alt="avatar"
          />
          <span v-else>{{ avatarLetter }}</span>
          <div class="mine-settings-avatar-text">
            {{ profileForm.avatarUrl ? '更换头像' : '上传头像' }}
          </div>
        </div>
        <div class="mine-settings-item" @click="openProfileFromSettings">
          <span class="mine-settings-label">修改个人信息</span>
          <span class="mine-settings-value">{{ profileForm.nickname || '未设置' }}</span>
          <RightOutlined class="mine-settings-arrow" />
        </div>
        <div class="mine-settings-item" @click="openPhoneFromSettings">
          <span class="mine-settings-label">手机号</span>
          <span class="mine-settings-value">{{ phoneForm.phone || '未绑定' }}</span>
          <RightOutlined class="mine-settings-arrow" />
        </div>
        <div class="mine-settings-item" @click="openEmailFromSettings">
          <span class="mine-settings-label">邮箱</span>
          <span class="mine-settings-value">{{ emailForm.email || '未绑定' }}</span>
          <RightOutlined class="mine-settings-arrow" />
        </div>
      </div>
    </a-modal>

    <AccountCheckModal v-model:visible="accountModalVisible" />
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { inject } from 'vue'
import { Modal } from 'ant-design-vue'
import { useUserProfile } from '@/composables/useUserProfile.js'
import { useBenefits } from '@/composables/useBenefits.js'
import {
  CrownOutlined,
  BellOutlined,
  SettingOutlined,
  RightOutlined,
  LogoutOutlined
} from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import { getMyCoupons } from '@/api/lottery'
import AccountCheckModal from '@/components/AccountCheckModal.vue'

const router = useRouter()
const actions = inject('consoleActions')

const accountModalVisible = ref(false)

function openAccountCheck() {
  if (window.innerWidth <= 768) {
    router.push('/console/account-check')
  } else {
    accountModalVisible.value = true
  }
}
const settingsModalVisible = ref(false)

const catFrames = [
  '/assets/images/猫咪1-v1.svg',
  '/assets/images/猫咪2-v1.svg',
  '/assets/images/猫咪3-v1.svg',
  '/assets/images/猫咪4-v1.svg',
  '/assets/images/猫咪5-v1.svg'
]

const userProfile = useUserProfile()
const { benefits, loadBenefits } = useBenefits()
const mineAvatarInput = ref(null)

const profileForm = actions.profileForm
const emailForm = actions.emailForm
const phoneForm = actions.phoneForm
const coinBalance = actions.coinBalance
const inviteStats = actions.inviteStats
const membershipLevel = actions.membershipLevel
const membershipExpiry = actions.membershipExpiry
const hasMembership = actions.hasMembership

// 头像字母：取昵称第一个字符
const avatarLetter = computed(() => {
  const name = profileForm.nickname || '爱创作工坊用户'
  return name.charAt(0).toUpperCase()
})

// 会员剩余天数
const membershipDaysLeft = computed(() => {
  if (!membershipExpiry.value) return 0
  const end = dayjs(membershipExpiry.value)
  const now = dayjs()
  return Math.max(0, end.diff(now, 'day'))
})

// 会员等级图标
const vipIconSrc = computed(() => {
  const level = String(membershipLevel.value || '')
  const upper = level.toUpperCase()
  if (upper.includes('SSVIP') || level.includes('旗舰')) return '/assets/images/vip/SSVIP.svg'
  if (upper.includes('SVIP') || level.includes('专业')) return '/assets/images/vip/SVIP.svg'
  if (upper.includes('VIP') || level.includes('基础')) return '/assets/images/vip/VIP.svg'
  return ''
})

// 已开通权益数量
const benefitCount = computed(() => Object.keys(benefits.value || {}).length)

// 优惠券数量
const couponCount = ref(0)
onMounted(async () => {
  loadBenefits()
  try {
    const coupons = await getMyCoupons()
    couponCount.value = (coupons.data || []).length
  } catch {
    couponCount.value = 0
  }
})

const confirmLogout = () => {
  Modal.confirm({
    title: '退出登录',
    content: '确定要退出当前账号吗？',
    okText: '退出',
    cancelText: '取消',
    okButtonProps: { danger: true },
    centered: true,
    onOk: () => actions.handleLogout()
  })
}

const openProfileFromSettings = () => {
  settingsModalVisible.value = false
  router.push('/console/profile/edit')
}

const openPhoneFromSettings = () => {
  settingsModalVisible.value = false
  actions.openPhoneModal()
}

const openEmailFromSettings = () => {
  settingsModalVisible.value = false
  actions.openEmailModal()
}

const openOfficialSite = () => {
  router.push('/')
}

const triggerAvatarUpload = () => {
  mineAvatarInput.value?.click()
}

const onMineAvatarChange = async (e) => {
  const file = e.target.files?.[0]
  if (!file) return
  e.target.value = ''
  try {
    await userProfile.saveAvatar(file)
  } catch {
    // composable 已 message.error
  }
}
</script>

<style scoped>
.mine-page {
  max-width: 720px;
  margin: 0 auto;
  box-sizing: border-box;
  background: #F5F6FA;
  min-height: 100vh;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'PingFang SC', 'Helvetica Neue', Arial, sans-serif;
  color: #1a1a1a;
  -webkit-font-smoothing: antialiased;
}

/* ========== 用户卡（参考图头部布局） ========== */
.mine-user-card {
  position: relative;
  background:
    radial-gradient(circle at 18% 22%, transparent 22px, rgba(255, 36, 66, 0.08) 23px, rgba(255, 36, 66, 0.08) 42px, transparent 43px),
    radial-gradient(circle at 82% 78%, transparent 30px, rgba(255, 36, 66, 0.06) 31px, rgba(255, 36, 66, 0.06) 54px, transparent 55px),
    radial-gradient(circle at 78% 18%, transparent 16px, rgba(255, 36, 66, 0.07) 17px, rgba(255, 36, 66, 0.07) 34px, transparent 35px),
    linear-gradient(135deg, #FFF8FA 0%, #FFEBEF 100%);
  padding: 20px 16px 44px;
  box-shadow: 0 4px 16px rgba(255, 36, 66, 0.12);
  display: flex;
  flex-direction: column;
  gap: 18px;
  overflow: hidden;
}

.mine-user-header {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}

.mine-user-avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.9);
  border: 2px solid rgba(255, 255, 255, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  font-weight: 700;
  color: #FF2442;
  overflow: hidden;
  cursor: pointer;
  flex-shrink: 0;
}

.mine-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.mine-user-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 5px;
}

.mine-user-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.mine-user-name {
  font-size: 18px;
  font-weight: 700;
  color: #1a1a1a;
  letter-spacing: -0.2px;
}

.mine-user-vip {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 2px 7px;
  border-radius: 4px;
  background: linear-gradient(135deg, #FF4D6F 0%, #FF2442 100%);
  font-size: 10px;
  font-weight: 600;
  color: #fff;
  line-height: 1.4;
  box-shadow: 0 2px 6px rgba(255, 36, 66, 0.25);
}

.mine-user-vip-icon {
  font-size: 10px;
}

.mine-user-id {
  font-size: 13px;
  color: #999;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mine-user-bio {
  font-size: 12px;
  line-height: 1.5;
  color: #666;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  cursor: pointer;
}

.mine-user-bio.empty {
  color: #999;
}

.mine-user-actions-top {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.mine-header-icon-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: none;
  background: rgba(255, 36, 66, 0.08);
  color: #FF2442;
  font-size: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background 0.15s;
  -webkit-tap-highlight-color: transparent;
}

.mine-header-icon-btn:active {
  background: rgba(255, 36, 66, 0.16);
}

/* ========== 核心数据 ========== */
.mine-user-stats {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 0 8px;
}

.mine-user-stat {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  transition: opacity 0.15s;
  -webkit-tap-highlight-color: transparent;
  min-width: 0;
}

.mine-user-stat:active {
  opacity: 0.75;
}

.mine-user-stat-value {
  font-size: 18px;
  font-weight: 700;
  color: #1a1a1a;
  line-height: 1.2;
  letter-spacing: -0.3px;
}

.mine-user-stat-label {
  font-size: 12px;
  color: #999;
}

/* ========== VIP 卡片 ========== */
.mine-vip-card {
  position: relative;
  z-index: 1;
  margin: -24px 12px 0;
  border-radius: 16px;
  background:
    radial-gradient(circle at 18% 22%, transparent 20px, rgba(255, 255, 255, 0.06) 21px, rgba(255, 255, 255, 0.06) 38px, transparent 39px),
    radial-gradient(circle at 82% 78%, transparent 26px, rgba(255, 255, 255, 0.05) 27px, rgba(255, 255, 255, 0.05) 48px, transparent 49px),
    radial-gradient(circle at 78% 18%, transparent 14px, rgba(255, 255, 255, 0.05) 15px, rgba(255, 255, 255, 0.05) 30px, transparent 31px),
    linear-gradient(135deg, #FF6B7D 0%, #FF2442 45%, #E61E3A 100%);
  box-shadow: 0 6px 18px rgba(255, 36, 66, 0.25);
  display: flex;
  flex-direction: column;
  -webkit-tap-highlight-color: transparent;
}

.mine-vip-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 90px;
  padding: 0 16px;
  cursor: pointer;
  transition: transform 0.15s;
}

.mine-vip-card-top:active {
  transform: translateY(1px);
}

.mine-vip-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.mine-vip-title {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 17px;
  font-weight: 700;
  color: #fff;
  line-height: 1.2;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
  padding-right: 80px;
}

.mine-vip-title-icon {
  font-size: 14px;
}
.mine-vip-title-badge {
  position: absolute;
  right: 14px;
  top: 50%;
  transform: translateY(-50%);
  width: 76px;
  height: 38px;
  object-fit: contain;
}

.mine-vip-desc {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.85);
  line-height: 1.3;
}

.mine-vip-mascot {
  position: relative;
  width: 100px;
  height: 100px;
  flex-shrink: 0;
  margin-top: -40px;
  margin-right: -8px;
}

.mine-vip-mascot-img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: contain;
  opacity: 0;
  animation: cat-blink 15s infinite;
  pointer-events: none;
}

.mine-vip-order-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  height: 80px;
  padding: 0;
  background: linear-gradient(135deg, #FFF0F3 0%, #fff 30%, #fff 100%);
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.08);
}

.mine-vip-order-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 6px 2px;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.15s;
  -webkit-tap-highlight-color: transparent;
}

.mine-vip-order-item:active {
  background: #F5F6FA;
}

.mine-vip-order-icon {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: #FF2442;
  background: #FFF5F7;
}

.mine-vip-order-icon--img {
  background: transparent;
}

.mine-vip-order-icon--img img {
  width: 40px;
  height: 40px;
  object-fit: contain;
  border-radius: 8px;
}

.mine-vip-order-label {
  font-size: 12px;
  color: #595959;
  text-align: center;
  line-height: 1.2;
}

@keyframes cat-blink {
  0%, 18% { opacity: 1; }
  20%, 100% { opacity: 0; }
}

/* ========== 通用 section ========== */
.mine-block {
  margin-top: 12px;
}

.mine-user-card + .mine-block,
.mine-vip-card + .mine-block {
  margin-top: 16px;
}

.mine-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 4px 10px;
}

.mine-section-title {
  font-size: 15px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0;
  letter-spacing: -0.2px;
}

/* ========== 热门服务 ========== */
.mine-hot-services-block {
  margin-left: 12px;
  margin-right: 12px;
}

.mine-hot-services {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
  background: #fff;
  border-radius: 16px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.mine-hot-service-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 14px 12px;
  background: #FFF8FA;
  border-radius: 12px;
  cursor: pointer;
  transition: background 0.15s;
  -webkit-tap-highlight-color: transparent;
}

.mine-hot-service-item:active {
  background: #FFEBEF;
}

.mine-hot-service-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.mine-hot-service-title {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
}

.mine-hot-service-subtitle {
  font-size: 11px;
  color: #999;
}

.mine-hot-service-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.mine-hot-service-icon--img {
  background: transparent;
}

.mine-hot-service-icon--img img {
  width: 40px;
  height: 40px;
  object-fit: contain;
  border-radius: 8px;
}

/* ========== 常用功能 ========== */
.mine-common-functions-block {
  margin-left: 12px;
  margin-right: 12px;
}

/* ========== 常用功能网格 ========== */
.mine-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  background: #fff;
  border-radius: 16px;
  padding: 16px 10px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.mine-grid-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 6px 2px;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.15s;
  -webkit-tap-highlight-color: transparent;
}

.mine-grid-item:active {
  background: #F5F6FA;
}

.mine-grid-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  color: #FF2442;
  background: #FFF5F7;
}

.mine-grid-icon--img {
  background: transparent;
}

.mine-grid-icon--img img {
  width: 40px;
  height: 40px;
  object-fit: contain;
  border-radius: 8px;
}

.mine-grid-label {
  font-size: 12px;
  color: #666;
  text-align: center;
  line-height: 1.2;
}

/* ========== 退出登录 ========== */
.mine-logout {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: calc(100% - 24px);
  margin: 24px 12px 0;
  padding: 14px 20px;
  background: #fff;
  color: #FF4D4F;
  border: none;
  border-radius: 16px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  transition: background 0.15s;
}

.mine-logout:active {
  background: #FFF2F0;
}

.mine-logout-icon {
  font-size: 16px;
}

.mine-footer {
  margin-top: 16px;
  text-align: center;
  font-size: 11px;
  color: #999;
}

.mine-icp {
  margin-top: 8px;
  text-align: center;
  font-size: 11px;
  color: #999;
}

/* ========== 设置弹框 ========== */
.mine-settings-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px 0 16px;
  gap: 8px;
}

.mine-settings-avatar {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  margin-bottom: 8px;
}

.mine-settings-avatar img,
.mine-settings-avatar span {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  object-fit: cover;
  background: #F5F6FA;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  font-weight: 700;
  color: #999;
}

.mine-settings-avatar-text {
  font-size: 13px;
  color: #FF2442;
}

.mine-settings-item {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: #F5F6FA;
  border-radius: 12px;
  cursor: pointer;
  transition: background 0.15s;
  -webkit-tap-highlight-color: transparent;
}

.mine-settings-item:active {
  background: #EBEDF0;
}

.mine-settings-label {
  flex-shrink: 0;
  min-width: 56px;
  width: auto;
  font-size: 14px;
  color: #1a1a1a;
  white-space: nowrap;
}

.mine-settings-value {
  flex: 1;
  font-size: 14px;
  color: #999;
  text-align: right;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mine-settings-arrow {
  flex-shrink: 0;
  font-size: 12px;
  color: #999;
}

/* ========== PC 端适配 ========== */
@media (min-width: 769px) {
  .mine-user-name {
    font-size: 20px;
  }

  .mine-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}

/* ========== 手机端适配 ========== */
@media (max-width: 768px) {
  .mine-icp {
    margin-bottom: 38px;
  }
}

/* ========== 暗色主题 ========== */
body[data-theme="dark"] .mine-page {
  background: #141414;
}

body[data-theme="dark"] .mine-user-card {
  background:
    radial-gradient(circle at 18% 22%, transparent 22px, rgba(255, 36, 66, 0.12) 23px, rgba(255, 36, 66, 0.12) 42px, transparent 43px),
    radial-gradient(circle at 82% 78%, transparent 30px, rgba(255, 36, 66, 0.09) 31px, rgba(255, 36, 66, 0.09) 54px, transparent 55px),
    radial-gradient(circle at 78% 18%, transparent 16px, rgba(255, 36, 66, 0.1) 17px, rgba(255, 36, 66, 0.1) 34px, transparent 35px),
    linear-gradient(135deg, #2A1518 0%, #1F0F12 100%);
  box-shadow: 0 4px 16px rgba(255, 36, 66, 0.12);
}

body[data-theme="dark"] .mine-grid,
body[data-theme="dark"] .mine-logout {
  background: #1f1f1f;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

body[data-theme="dark"] .mine-user-avatar {
  background: rgba(255, 255, 255, 0.9);
  border-color: rgba(255, 255, 255, 0.4);
  color: #FF2442;
}

body[data-theme="dark"] .mine-user-name,
body[data-theme="dark"] .mine-section-title,
body[data-theme="dark"] .mine-grid-label,
body[data-theme="dark"] .mine-user-stat-value {
  color: #f0f0f0;
}

body[data-theme="dark"] .mine-user-id,
body[data-theme="dark"] .mine-user-bio,
body[data-theme="dark"] .mine-user-stat-label,
body[data-theme="dark"] .mine-stat-label,
body[data-theme="dark"] .mine-footer,
body[data-theme="dark"] .mine-icp {
  color: #737373;
}

body[data-theme="dark"] .mine-header-icon-btn {
  background: rgba(255, 36, 66, 0.12);
  color: #FF4D6D;
}

body[data-theme="dark"] .mine-header-icon-btn:active {
  background: rgba(255, 36, 66, 0.2);
}

body[data-theme="dark"] .mine-user-vip {
  background: linear-gradient(135deg, #FF6B8A 0%, #FF2442 100%);
  color: #fff;
  box-shadow: 0 2px 6px rgba(255, 36, 66, 0.2);
}

body[data-theme="dark"] .mine-vip-card {
  background:
    radial-gradient(circle at 18% 22%, transparent 20px, rgba(255, 255, 255, 0.08) 21px, rgba(255, 255, 255, 0.08) 38px, transparent 39px),
    radial-gradient(circle at 82% 78%, transparent 26px, rgba(255, 255, 255, 0.06) 27px, rgba(255, 255, 255, 0.06) 48px, transparent 49px),
    radial-gradient(circle at 78% 18%, transparent 14px, rgba(255, 255, 255, 0.07) 15px, rgba(255, 255, 255, 0.07) 30px, transparent 31px),
    linear-gradient(135deg, #D43A4E 0%, #8B1221 50%, #6B0E1A 100%);
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.35);
}

body[data-theme="dark"] .mine-vip-title {
  color: #fff;
}

body[data-theme="dark"] .mine-vip-desc {
  color: rgba(255, 255, 255, 0.75);
}

body[data-theme="dark"] .mine-vip-order-grid {
  background: linear-gradient(135deg, #2a2a2a 0%, #1f1f1f 30%, #1f1f1f 100%);
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.25);
}

body[data-theme="dark"] .mine-vip-order-item:active {
  background: #2a2a2a;
}

body[data-theme="dark"] .mine-vip-order-icon {
  background: rgba(255, 36, 66, 0.12);
  color: #FF4D6D;
}

body[data-theme="dark"] .mine-vip-order-icon--img {
  background: transparent;
}

body[data-theme="dark"] .mine-vip-order-label {
  color: #a6a6a6;
}

body[data-theme="dark"] .mine-grid-icon {
  background: rgba(255, 36, 66, 0.12);
  color: #FF4D6D;
}

body[data-theme="dark"] .mine-hot-services {
  background: #1f1f1f;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

body[data-theme="dark"] .mine-hot-service-item {
  background: #2a2a2a;
}

body[data-theme="dark"] .mine-hot-service-item:active {
  background: #303030;
}

body[data-theme="dark"] .mine-hot-service-title {
  color: #f0f0f0;
}

body[data-theme="dark"] .mine-hot-service-subtitle {
  color: #737373;
}

body[data-theme="dark"] .mine-hot-service-icon--img {
  background: transparent;
}

body[data-theme="dark"] .mine-grid-item:active {
  background: #2a2a2a;
}

body[data-theme="dark"] .mine-logout:active {
  background: rgba(255, 77, 79, 0.12);
}
</style>
