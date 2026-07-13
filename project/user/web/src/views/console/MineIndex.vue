<template>
  <div class="mine-page">
    <!-- 用户卡 -->
    <section class="mine-user-card">
      <div class="mine-user-avatar">{{ avatarLetter }}</div>
      <div class="mine-user-info">
        <div class="mine-user-name-row">
          <span class="mine-user-name">{{ profileForm.nickname || '爱创作用户' }}</span>
          <span
            v-if="hasMembership"
            class="mine-user-vip"
            :class="{ 'mine-user-vip-pro': membershipLevel === '专业版会员' }"
          >
            <CrownOutlined class="mine-user-vip-icon" />
            {{ membershipLevel }}
          </span>
          <span v-else class="mine-user-vip mine-user-vip-free">免费版</span>
        </div>
        <div class="mine-user-email">{{ emailForm.email || 'user@example.com' }}</div>
        <div v-if="hasMembership && membershipExpiry" class="mine-user-expiry">
          有效期至 {{ membershipExpiry }}
        </div>
      </div>
      <button class="mine-edit-btn" @click="actions.openProfileModal">
        <EditOutlined />
      </button>
    </section>

    <!-- 数据区 -->
    <section class="mine-stats">
      <div class="mine-stat-item">
        <div class="mine-stat-value">{{ monthlyWorks }}</div>
        <div class="mine-stat-label">本月已生成</div>
      </div>
      <div class="mine-stat-divider"></div>
      <div class="mine-stat-item mine-stat-item-coin">
        <div class="mine-stat-value">{{ coinBalance }}</div>
        <div class="mine-stat-label">创作币余额</div>
      </div>
      <div class="mine-stat-divider"></div>
      <div class="mine-stat-item">
        <div class="mine-stat-value">{{ inviteStats.invitedCount }}</div>
        <div class="mine-stat-label">已邀请</div>
      </div>
    </section>

    <!-- 我的资产 -->
    <!-- 我的资产 -->
    <section class="mine-block">
      <h3 class="mine-section-title">我的资产</h3>
      <ul class="mine-list">
        <li class="mine-list-item" @click="$router.push('/console/earnings')">
          <DollarOutlined class="mine-list-icon" />
          <span class="mine-list-label">我的账户</span>
          <RightOutlined class="mine-list-arrow" />
        </li>
        <li class="mine-list-item" @click="$router.push('/console/coin')">
          <WalletOutlined class="mine-list-icon" />
          <span class="mine-list-label">创作币提现</span>
          <span class="mine-list-extra">{{ coinBalance }} 币</span>
          <RightOutlined class="mine-list-arrow" />
        </li>
      </ul>
    </section>

    <!-- 我的创作 -->
    <section class="mine-block">
      <h3 class="mine-section-title">我的创作</h3>
      <ul class="mine-list">
        <li class="mine-list-item" @click="$router.push('/console/styles')">
          <SmileOutlined class="mine-list-icon" />
          <span class="mine-list-label">我的风格</span>
          <RightOutlined class="mine-list-arrow" />
        </li>
        <li class="mine-list-item" @click="$router.push('/console/style-market')">
          <ShopOutlined class="mine-list-icon" />
          <span class="mine-list-label">风格市场</span>
          <RightOutlined class="mine-list-arrow" />
        </li>
        <li class="mine-list-item" @click="$router.push('/console/hot-search')">
          <FireOutlined class="mine-list-icon" />
          <span class="mine-list-label">热搜榜</span>
          <RightOutlined class="mine-list-arrow" />
        </li>
      </ul>
    </section>

    <!-- 邀请与帮助 -->
    <section class="mine-block">
      <h3 class="mine-section-title">邀请与帮助</h3>
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
        <li class="mine-list-item" @click="$router.push('/console/invite')">
          <GiftOutlined class="mine-list-icon mine-list-icon-gift" />
          <span class="mine-list-label">邀请有礼</span>
          <span class="mine-list-tag mine-list-tag-hot">HOT</span>
          <RightOutlined class="mine-list-arrow" />
        </li>
        <li class="mine-list-item" @click="actions.openRedeemModal">
          <TagOutlined class="mine-list-icon" />
          <span class="mine-list-label">兑换码</span>
          <RightOutlined class="mine-list-arrow" />
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
        <li class="mine-list-item" @click="actions.toggleTheme">
          <component
            :is="actions.currentTheme.value === 'light' ? BulbOutlined : BulbFilled"
            class="mine-list-icon"
          />
          <span class="mine-list-label">主题切换</span>
          <span class="mine-list-extra">
            {{ actions.currentTheme.value === 'light' ? '浅色' : '深色' }}
          </span>
          <RightOutlined class="mine-list-arrow" />
        </li>
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

    <!-- 退出登录：单独、最显眼、放在最底部 -->
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
  WalletOutlined,
  SmileOutlined,
  ShopOutlined,
  FireOutlined,
  GiftOutlined,
  TagOutlined,
  BookOutlined,
  MessageOutlined,
  BulbOutlined,
  BulbFilled,
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
  window.open('http://localhost:28585/.superpowers/brainstorm/6491-1782131242/content/index.html', '_blank')
}
</script>

<style scoped>
.mine-page {
  padding: 20px 16px calc(80px + env(safe-area-inset-bottom));
  max-width: 720px;
  margin: 0 auto;
}

/* ========== 用户卡 ========== */
.mine-user-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 20px;
  background: linear-gradient(135deg, #FF2442 0%, #E61E3A 100%);
  border-radius: 16px;
  color: #fff;
  position: relative;
  overflow: hidden;
}

.mine-user-card::before {
  content: '';
  position: absolute;
  top: -40px;
  right: -40px;
  width: 140px;
  height: 140px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 50%;
}

.mine-user-card::after {
  content: '';
  position: absolute;
  bottom: -30px;
  left: -20px;
  width: 100px;
  height: 100px;
  background: rgba(255, 255, 255, 0.06);
  border-radius: 50%;
}

.mine-user-avatar {
  flex-shrink: 0;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.22);
  border: 2px solid rgba(255, 255, 255, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  font-weight: 700;
  color: #fff;
  position: relative;
  z-index: 1;
}

.mine-user-info {
  flex: 1;
  min-width: 0;
  position: relative;
  z-index: 1;
}

.mine-user-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.mine-user-name {
  font-size: 17px;
  font-weight: 600;
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
}

.mine-user-vip-pro {
  background: linear-gradient(135deg, #ffd700 0%, #ffaa00 100%);
  color: #5a2a00;
  border-color: rgba(255, 215, 0, 0.6);
}

.mine-user-vip-free {
  background: rgba(255, 255, 255, 0.18);
  color: rgba(255, 255, 255, 0.85);
}

.mine-user-vip-icon {
  font-size: 11px;
}

.mine-user-email {
  margin-top: 4px;
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
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.4);
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

.mine-edit-btn:hover {
  background: rgba(255, 255, 255, 0.3);
}

/* ========== 数据区 ========== */
.mine-stats {
  display: flex;
  align-items: stretch;
  background: #fff;
  border-radius: 14px;
  padding: 18px 8px;
  margin-top: 14px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.mine-stat-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  cursor: default;
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
  margin-top: 18px;
}

.mine-section-title {
  font-size: 13px;
  font-weight: 600;
  color: #8c8c8c;
  padding: 8px 16px 12px;
  margin-bottom: 0;
  letter-spacing: 0.5px;
}

/* ========== 列表项 ========== */
.mine-list {
  list-style: none;
  margin: 0;
  padding: 0;
  background: #fff;
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.mine-list-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
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

.mine-list-icon-gift {
  color: #fa8c16;
}

.mine-list-icon-wechat {
  color: #07c160;
}

.mine-list-label {
  flex: 1;
  font-size: 15px;
  color: #1a1a1a;
}

.mine-list-extra {
  font-size: 13px;
  color: #8c8c8c;
}

.mine-list-tag {
  flex-shrink: 0;
  padding: 1px 6px;
  font-size: 10px;
  font-weight: 700;
  border-radius: 4px;
  color: #fff;
  letter-spacing: 0.5px;
}

.mine-list-tag-hot {
  background: linear-gradient(135deg, #ff4d4f 0%, #FF2442 100%);
}

.mine-list-arrow {
  flex-shrink: 0;
  font-size: 12px;
  color: #bfbfbf;
}

/* ========== 退出登录：最底部、最显眼 ========== */
.mine-logout {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  width: 100%;
  margin-top: 28px;
  padding: 18px 20px;
  background: #fff;
  color: #FF2442;
  border: 1px solid rgba(255, 36, 66, 0.2);
  border-radius: 14px;
  font-size: 17px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(255, 36, 66, 0.1);
  transition: all 0.2s;
}

.mine-logout:hover {
  background: #FFF5F7;
  border-color: #FF2442;
  box-shadow: 0 6px 24px rgba(255, 36, 66, 0.18);
}

.mine-logout:active {
  transform: scale(0.99);
}

.mine-logout-icon {
  font-size: 20px;
}

.mine-footer {
  margin-top: 16px;
  text-align: center;
  font-size: 11px;
  color: #bfbfbf;
}

@media (max-width: 768px) {
  .mine-page {
    padding: 16px 12px calc(80px + env(safe-area-inset-bottom));
    max-width: 100%;
    margin: 0;
  }

  .mine-user-card {
    padding: 16px;
  }

  .mine-list-item {
    padding: 14px 12px;
  }

  .mine-section-title {
    padding: 8px 12px 12px;
  }
}

/* ========== 暗色主题 ========== */

body[data-theme="dark"] .mine-stats {
  background: #1f1f1f;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.4);
}

body[data-theme="dark"] .mine-stat-value {
  color: #e0e0e0;
}

body[data-theme="dark"] .mine-stat-item-coin .mine-stat-value {
  color: #ff4d6f;
}

body[data-theme="dark"] .mine-stat-divider {
  background: #303030;
}

body[data-theme="dark"] .mine-section-title {
  color: #a6a6a6;
  background: #141414;
}

body[data-theme="dark"] .mine-block {
  background: #141414;
}

body[data-theme="dark"] .mine-list {
  background: #1f1f1f;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.4);
}

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
  color: #ff4d6f;
}

body[data-theme="dark"] .mine-list-icon-gift {
  color: #ffa940;
}

body[data-theme="dark"] .mine-list-icon-wechat {
  color: #10b981;
}

body[data-theme="dark"] .mine-logout {
  background: #1f1f1f;
  color: #ff4d6f;
  border-color: rgba(255, 77, 111, 0.4);
  box-shadow: 0 4px 16px rgba(255, 77, 111, 0.12);
}

body[data-theme="dark"] .mine-logout:hover {
  background: rgba(255, 77, 111, 0.08);
  border-color: #ff4d6f;
}

body[data-theme="dark"] .mine-footer {
  color: #666;
}
</style>