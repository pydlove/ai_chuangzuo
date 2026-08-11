<template>
  <div class="mine-page">
    <!-- 用户卡 -->
    <section class="mine-user-card">
      <div class="mine-user-main">
        <div class="mine-user-avatar">{{ avatarLetter }}</div>
        <div class="mine-user-info">
          <div class="mine-user-name-row">
            <span class="mine-user-name">{{ profileForm.nickname || '爱创作用户' }}</span>
            <span
              :class="[
                'mine-user-vip',
                { 'mine-user-vip-pro': membershipLevel === '专业版会员', 'mine-user-vip-free': !hasMembership }
              ]"
              @click="router.push('/console/benefits')"
            >
              <CrownOutlined v-if="hasMembership" class="mine-user-vip-icon" />
              {{ membershipLevel || '免费版' }}
            </span>
          </div>
          <div class="mine-user-email">{{ emailForm.email || 'user@example.com' }}</div>
          <div v-if="hasMembership && membershipExpiry" class="mine-user-expiry">
            有效期至 {{ membershipExpiry }}
          </div>
        </div>
      </div>
      <button class="mine-edit-btn" @click="actions.openProfileModal">
        <EditOutlined />
      </button>
    </section>

    <!-- 数据卡 -->
    <section class="mine-stats-card">
      <div class="mine-stat-item" @click="router.push('/console/works')">
        <div class="mine-stat-value">{{ monthlyWorks }}</div>
        <div class="mine-stat-label">本月已生成</div>
      </div>
      <div class="mine-stat-divider"></div>
      <div class="mine-stat-item mine-stat-item-coin" @click="router.push('/console/earnings')">
        <div class="mine-stat-value">{{ coinBalance }}</div>
        <div class="mine-stat-label">创作币余额</div>
      </div>
      <div class="mine-stat-divider"></div>
      <div class="mine-stat-item" @click="router.push('/console/invite')">
        <div class="mine-stat-value">{{ inviteStats.invitedCount }}</div>
        <div class="mine-stat-label">已邀请</div>
      </div>
    </section>

    <!-- 常用功能 -->
    <section class="mine-block">
      <h3 class="mine-section-title">常用功能</h3>
      <div class="mine-grid">
        <div class="mine-grid-item" @click="$router.push('/console/works')">
          <div class="mine-grid-icon mine-grid-icon--works"><ContainerOutlined /></div>
          <span class="mine-grid-label">我的作品</span>
        </div>
        <div class="mine-grid-item" @click="$router.push('/console/earnings')">
          <div class="mine-grid-icon mine-grid-icon--earnings"><DollarOutlined /></div>
          <span class="mine-grid-label">我的账户</span>
        </div>
        <div class="mine-grid-item" @click="$router.push('/console/benefits')">
          <div class="mine-grid-icon mine-grid-icon--benefits"><CrownOutlined /></div>
          <span class="mine-grid-label">我的权益</span>
        </div>
        <div class="mine-grid-item" @click="$router.push('/console/invite')">
          <div class="mine-grid-icon mine-grid-icon--invite">
            <GiftOutlined />
            <span class="mine-grid-gift-badge">🎁</span>
          </div>
          <span class="mine-grid-label">邀请有礼</span>
        </div>
        <div class="mine-grid-item" @click="$router.push('/console/skills')">
          <div class="mine-grid-icon mine-grid-icon--skills"><SmileOutlined /></div>
          <span class="mine-grid-label">我的提示词</span>
        </div>
        <div class="mine-grid-item" @click="$router.push('/console/skill-market')">
          <div class="mine-grid-icon mine-grid-icon--market"><ShopOutlined /></div>
          <span class="mine-grid-label">提示词市场</span>
        </div>
        <div class="mine-grid-item" @click="$router.push('/console/hot-search')">
          <div class="mine-grid-icon mine-grid-icon--hot"><FireOutlined /></div>
          <span class="mine-grid-label">热搜榜</span>
        </div>
        <div class="mine-grid-item" @click="$router.push('/console/coupons')">
          <div class="mine-grid-icon mine-grid-icon--coupon"><TagsOutlined /></div>
          <span class="mine-grid-label">我的优惠券</span>
        </div>
        <div class="mine-grid-item" @click="actions.openRedeemModal">
          <div class="mine-grid-icon mine-grid-icon--redeem"><TagOutlined /></div>
          <span class="mine-grid-label">兑换码</span>
        </div>
      </div>
    </section>

    <!-- 服务与帮助 -->
    <section class="mine-block">
      <h3 class="mine-section-title">服务与帮助</h3>
      <ul class="mine-list">
        <li
          v-if="actions.profile?.value?.inviterUserId == null"
          class="mine-list-item"
          @click="actions.openInviteBindingModal"
        >
          <UserAddOutlined class="mine-list-icon" />
          <span class="mine-list-label">绑定邀请人</span>
          <RightOutlined class="mine-list-arrow" />
        </li>
        <li v-else class="mine-list-item">
          <UserAddOutlined class="mine-list-icon" />
          <span class="mine-list-label">我的邀请人</span>
          <span class="mine-list-extra">{{ actions.profile?.value?.inviterNickname || '已绑定' }}</span>
        </li>
        <li class="mine-list-item" @click="actions.openTutorialModal">
          <BookOutlined class="mine-list-icon" />
          <span class="mine-list-label">教程与帮助</span>
          <RightOutlined class="mine-list-arrow" />
        </li>
        <li class="mine-list-item" @click="actions.openFeedbackModal">
          <MessageOutlined class="mine-list-icon" />
          <span class="mine-list-label">意见反馈</span>
          <RightOutlined class="mine-list-arrow" />
        </li>
      </ul>
    </section>

    <!-- 设置 -->
    <section class="mine-block">
      <h3 class="mine-section-title">设置</h3>
      <ul class="mine-list">
        <li class="mine-list-item" @click="actions.openPasswordModal">
          <LockOutlined class="mine-list-icon" />
          <span class="mine-list-label">修改密码</span>
          <RightOutlined class="mine-list-arrow" />
        </li>
        <li class="mine-list-item" @click="actions.openEmailModal">
          <MailOutlined class="mine-list-icon" />
          <span class="mine-list-label">修改邮箱</span>
          <RightOutlined class="mine-list-arrow" />
        </li>
      </ul>
    </section>

    <!-- 关于 -->
    <section class="mine-block">
      <h3 class="mine-section-title">关于</h3>
      <ul class="mine-list">
        <li class="mine-list-item" @click="actions.openAboutModal">
          <InfoCircleOutlined class="mine-list-icon" />
          <span class="mine-list-label">关于我们</span>
          <RightOutlined class="mine-list-arrow" />
        </li>
        <li class="mine-list-item" @click="actions.openTermsModal">
          <FileTextOutlined class="mine-list-icon" />
          <span class="mine-list-label">用户协议</span>
          <RightOutlined class="mine-list-arrow" />
        </li>
        <li class="mine-list-item" @click="actions.openPrivacyModal">
          <SafetyOutlined class="mine-list-icon" />
          <span class="mine-list-label">隐私政策</span>
          <RightOutlined class="mine-list-arrow" />
        </li>
        <li class="mine-list-item" @click="actions.openWechatModal">
          <WechatOutlined class="mine-list-icon mine-list-icon-wechat" />
          <span class="mine-list-label">关注微信</span>
          <RightOutlined class="mine-list-arrow" />
        </li>
        <li class="mine-list-item" @click="openOfficialSite">
          <GlobalOutlined class="mine-list-icon" />
          <span class="mine-list-label">访问官网</span>
          <RightOutlined class="mine-list-arrow" />
        </li>
      </ul>
    </section>

    <button class="mine-logout" @click="confirmLogout">
      <LogoutOutlined class="mine-logout-icon" />
      <span>退出登录</span>
    </button>

    <p class="mine-footer">© 2026 爱创作 · 杭州爱启云网络科技有限公司</p>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { inject } from 'vue'
import { Modal } from 'ant-design-vue'
import {
  CrownOutlined,
  EditOutlined,
  DollarOutlined,
  ContainerOutlined,
  SmileOutlined,
  ShopOutlined,
  FireOutlined,
  GiftOutlined,
  TagOutlined,
  TagsOutlined,
  BookOutlined,
  MessageOutlined,
  LockOutlined,
  MailOutlined,
  InfoCircleOutlined,
  FileTextOutlined,
  SafetyOutlined,
  WechatOutlined,
  GlobalOutlined,
  RightOutlined,
  LogoutOutlined,
  UserAddOutlined
} from '@ant-design/icons-vue'
import { getMonthlyCount } from '@/api/article'

const router = useRouter()
const actions = inject('consoleActions')

const profileForm = actions.profileForm
const emailForm = actions.emailForm
const coinBalance = actions.coinBalance
const inviteStats = actions.inviteStats
const membershipLevel = actions.membershipLevel
const membershipExpiry = actions.membershipExpiry
const hasMembership = actions.hasMembership

// 头像字母：取昵称第一个字符
const avatarLetter = computed(() => {
  const name = profileForm.nickname || '爱创作用户'
  return name.charAt(0).toUpperCase()
})

// 本月已生成：从后端统计接口读取
const monthlyWorks = ref(0)
onMounted(async () => {
  try {
    monthlyWorks.value = await getMonthlyCount()
  } catch {
    monthlyWorks.value = 0
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

const openOfficialSite = () => {
  router.push('/')
}
</script>

<style scoped>
.mine-page {
  padding: 16px 12px calc(80px + env(safe-area-inset-bottom));
  max-width: 720px;
  margin: 0 auto;
  box-sizing: border-box;
}

/* ========== 用户卡 ========== */
.mine-user-card {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 22px 18px;
  background: linear-gradient(135deg, #FF2442 0%, #FF4D6F 100%);
  border-radius: 20px;
  color: #fff;
  overflow: hidden;
  box-shadow: 0 8px 24px rgba(255, 36, 66, 0.22);
}

.mine-user-card::before {
  content: '';
  position: absolute;
  top: -50px;
  right: -40px;
  width: 160px;
  height: 160px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 50%;
}

.mine-user-card::after {
  content: '';
  position: absolute;
  bottom: -40px;
  left: -30px;
  width: 120px;
  height: 120px;
  background: rgba(255, 255, 255, 0.06);
  border-radius: 50%;
}

.mine-user-main {
  display: flex;
  align-items: center;
  gap: 14px;
  position: relative;
  z-index: 1;
  min-width: 0;
}

.mine-user-avatar {
  flex-shrink: 0;
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.22);
  border: 2px solid rgba(255, 255, 255, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 700;
  color: #fff;
}

.mine-user-info {
  flex: 1;
  min-width: 0;
}

.mine-user-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.mine-user-name {
  font-size: 18px;
  font-weight: 700;
  color: #fff;
}

.mine-user-vip {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.22);
  font-size: 11px;
  font-weight: 600;
  color: #fff;
  border: 1px solid rgba(255, 255, 255, 0.3);
  cursor: pointer;
  transition: opacity 0.15s;
  -webkit-tap-highlight-color: transparent;
}

.mine-user-vip:active {
  opacity: 0.8;
}

.mine-user-vip-pro {
  background: linear-gradient(135deg, #ffd700 0%, #ffaa00 100%);
  color: #5a2a00;
  border-color: rgba(255, 215, 0, 0.6);
}

.mine-user-vip-free {
  background: rgba(255, 255, 255, 0.18);
  color: rgba(255, 255, 255, 0.9);
}

.mine-user-vip-icon {
  font-size: 11px;
}

.mine-user-email {
  margin-top: 5px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.85);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mine-user-expiry {
  margin-top: 2px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.75);
}

.mine-edit-btn {
  flex-shrink: 0;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.35);
  background: rgba(255, 255, 255, 0.15);
  color: #fff;
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  position: relative;
  z-index: 1;
}

.mine-edit-btn:active {
  background: rgba(255, 255, 255, 0.3);
}

/* ========== 数据卡 ========== */
.mine-stats-card {
  display: flex;
  align-items: stretch;
  background: #fff;
  border-radius: 18px;
  padding: 18px 8px;
  margin-top: 12px;
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.04);
}

.mine-stat-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
  cursor: pointer;
  transition: opacity 0.15s;
  -webkit-tap-highlight-color: transparent;
}

.mine-stat-item:active {
  opacity: 0.7;
}

.mine-stat-value {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a1a;
  line-height: 1.1;
}

.mine-stat-item-coin .mine-stat-value {
  color: #FF2442;
}

.mine-stat-label {
  font-size: 12px;
  color: #8c8c8c;
}

.mine-stat-divider {
  width: 1px;
  background: #f0f0f0;
  margin: 4px 0;
}

/* ========== 通用 section ========== */
.mine-block {
  margin-top: 16px;
}

.mine-section-title {
  font-size: 14px;
  font-weight: 700;
  color: #1a1a1a;
  padding: 10px 12px;
  margin-bottom: 0;
  letter-spacing: 0.5px;
}

/* ========== 常用功能网格 ========== */
.mine-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  background: #fff;
  border-radius: 18px;
  padding: 16px 10px;
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.04);
}

.mine-grid-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 8px 4px;
  border-radius: 12px;
  cursor: pointer;
  transition: background 0.15s;
  -webkit-tap-highlight-color: transparent;
}

.mine-grid-item:active {
  background: #f5f5f5;
}

.mine-grid-icon {
  width: 46px;
  height: 46px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: #FF2442;
  background: #FFF5F7;
}

.mine-grid-icon--works { background: #FFF5F7; color: #FF2442; }
.mine-grid-icon--earnings { background: #FFF5F7; color: #FF2442; }
.mine-grid-icon--benefits { background: #FFF5F7; color: #FF2442; }
.mine-grid-icon--invite { background: #FFF7F0; color: #fa8c16; position: relative; }

.mine-grid-gift-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #FF2442;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  line-height: 1;
  box-shadow: 0 2px 5px rgba(255, 36, 66, 0.35);
  border: 2px solid #fff;
}
.mine-grid-icon--skills { background: #F0F9FF; color: #1890ff; }
.mine-grid-icon--market { background: #F0F9FF; color: #1890ff; }
.mine-grid-icon--hot { background: #FFF5F7; color: #FF2442; }
.mine-grid-icon--coupon { background: #FFF7F0; color: #fa8c16; }
.mine-grid-icon--redeem { background: #F6FFED; color: #52c41a; }

.mine-grid-label {
  font-size: 12px;
  color: #262626;
  text-align: center;
  line-height: 1.2;
}

/* ========== 列表项 ========== */
.mine-list {
  list-style: none;
  margin: 0;
  padding: 0;
  background: #fff;
  border-radius: 18px;
  overflow: hidden;
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.04);
}

.mine-list-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 14px;
  cursor: pointer;
  transition: background 0.15s;
  user-select: none;
}

.mine-list-item:active {
  background: #f5f5f5;
}

.mine-list-item + .mine-list-item {
  border-top: 1px solid #f5f5f5;
}

.mine-list-icon {
  flex-shrink: 0;
  width: 22px;
  height: 22px;
  font-size: 18px;
  color: #FF2442;
  display: flex;
  align-items: center;
  justify-content: center;
}

.mine-list-icon-wechat {
  color: #07c160;
}

.mine-list-label {
  flex: 1;
  font-size: 14px;
  color: #262626;
}

.mine-list-extra {
  font-size: 13px;
  color: #8c8c8c;
}

.mine-list-arrow {
  flex-shrink: 0;
  font-size: 12px;
  color: #bfbfbf;
}

/* ========== 退出登录 ========== */
.mine-logout {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  width: 100%;
  margin-top: 24px;
  padding: 16px 20px;
  background: #fff;
  color: #FF2442;
  border: 1px solid rgba(255, 36, 66, 0.2);
  border-radius: 16px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 4px 18px rgba(255, 36, 66, 0.08);
  transition: all 0.2s;
}

.mine-logout:active {
  background: #FFF5F7;
  transform: scale(0.99);
}

.mine-logout-icon {
  font-size: 18px;
}

.mine-footer {
  margin-top: 16px;
  text-align: center;
  font-size: 11px;
  color: #bfbfbf;
}

/* ========== PC 端适配 ========== */
@media (min-width: 769px) {
  .mine-page {
    padding: 24px 16px calc(80px + env(safe-area-inset-bottom));
  }

  .mine-user-card {
    padding: 26px 24px;
  }

  .mine-user-avatar {
    width: 68px;
    height: 68px;
    font-size: 26px;
  }

  .mine-user-name {
    font-size: 20px;
  }

  .mine-grid {
    grid-template-columns: repeat(8, 1fr);
    padding: 18px 16px;
  }

  .mine-grid-icon {
    width: 50px;
    height: 50px;
    font-size: 22px;
  }
}

/* ========== 暗色主题 ========== */
body[data-theme="dark"] .mine-user-card {
  background: linear-gradient(135deg, #ff4d6f 0%, #e61e3a 100%);
  box-shadow: 0 8px 24px rgba(255, 36, 66, 0.25);
}

body[data-theme="dark"] .mine-stats-card,
body[data-theme="dark"] .mine-list,
body[data-theme="dark"] .mine-grid {
  background: #1f1f1f;
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.3);
}

body[data-theme="dark"] .mine-stat-value {
  color: #f5f5f5;
}

body[data-theme="dark"] .mine-stat-item-coin .mine-stat-value {
  color: #ff6b81;
}

body[data-theme="dark"] .mine-stat-divider {
  background: #303030;
}

body[data-theme="dark"] .mine-section-title {
  color: #f5f5f5;
}

body[data-theme="dark"] .mine-grid-item:active {
  background: #2a2a2a;
}

body[data-theme="dark"] .mine-grid-label {
  color: #e0e0e0;
}

body[data-theme="dark"] .mine-grid-icon {
  background: rgba(255, 36, 66, 0.12);
  color: #ff6b81;
}

body[data-theme="dark"] .mine-grid-icon--works { background: rgba(255, 36, 66, 0.12); color: #ff6b81; }
body[data-theme="dark"] .mine-grid-icon--invite { background: rgba(250, 140, 22, 0.12); color: #ffc53d; position: relative; }

body[data-theme="dark"] .mine-grid-gift-badge {
  background: #ff4d6f;
  color: #fff;
  border-color: #1f1f1f;
  box-shadow: 0 2px 5px rgba(255, 77, 111, 0.35);
}
body[data-theme="dark"] .mine-grid-icon--skills { background: rgba(24, 144, 255, 0.12); color: #69c0ff; }
body[data-theme="dark"] .mine-grid-icon--market { background: rgba(24, 144, 255, 0.12); color: #69c0ff; }
body[data-theme="dark"] .mine-grid-icon--coupon { background: rgba(250, 140, 22, 0.12); color: #ffc53d; }
body[data-theme="dark"] .mine-grid-icon--redeem { background: rgba(82, 196, 26, 0.12); color: #95de64; }

body[data-theme="dark"] .mine-list-item + .mine-list-item {
  border-top-color: #303030;
}

body[data-theme="dark"] .mine-list-item:active {
  background: #2a2a2a;
}

body[data-theme="dark"] .mine-list-label {
  color: #e0e0e0;
}

body[data-theme="dark"] .mine-list-extra {
  color: #a6a6a6;
}

body[data-theme="dark"] .mine-list-arrow {
  color: #666;
}

body[data-theme="dark"] .mine-list-icon {
  color: #ff6b81;
}

body[data-theme="dark"] .mine-list-icon-wechat {
  color: #10b981;
}

body[data-theme="dark"] .mine-logout {
  background: #1f1f1f;
  color: #ff6b81;
  border-color: rgba(255, 77, 111, 0.4);
  box-shadow: 0 4px 18px rgba(255, 77, 111, 0.1);
}

body[data-theme="dark"] .mine-logout:active {
  background: rgba(255, 77, 111, 0.08);
}

body[data-theme="dark"] .mine-footer {
  color: #666;
}
</style>
