<template>
  <MobileLogin v-if="isMobile" />
  <PullToRefresh v-else :full-page="true">
    <div class="login-page">
    <!-- 背景装饰 -->
    <div class="login-bg">
      <div class="bg-circle bg-circle-1"></div>
      <div class="bg-circle bg-circle-2"></div>
    </div>

    <!-- 导航栏 -->
    <NavBar :links="navLinks" :cta-to="ctaTo" :cta-label="ctaLabel" />

    <!-- 登录卡片 -->
    <div ref="cardRef" class="login-card">
      <!-- 标签切换 -->
      <div class="auth-tabs">
        <button
          :class="['auth-tab', { active: activeTab === 'login' }]"
          @click="activeTab = 'login'"
        >
          登录
        </button>
        <button
          :class="['auth-tab', { active: activeTab === 'register' }]"
          @click="activeTab = 'register'"
        >
          注册
        </button>
      </div>

      <!-- 登录表单 -->
      <div v-show="activeTab === 'login'" class="auth-form">
        <h2 class="form-title">欢迎回来</h2>
        <p class="form-subtitle">登录后即可开始创作</p>

        <div class="form-item">
          <label class="form-label">邮箱</label>
          <input
            v-model="loginForm.email"
            type="email"
            class="form-input"
            placeholder="请输入邮箱"
          />
        </div>

       <div class="form-item">
         <label class="form-label">密码</label>
         <input
           v-model="loginForm.password"
           type="password"
           class="form-input"
           placeholder="请输入密码"
         />
       </div>

        <div class="form-item remember-row">
          <label class="remember-label">
            <input
              v-model="rememberMe"
              type="checkbox"
              class="remember-checkbox"
            />
            <span class="remember-text">记住我</span>
          </label>
        </div>

       <AgreementCheckbox v-model="agreed" :shake-count="agreementShakeCount" />

        <button class="submit-btn" @click="handleLogin">登录</button>

        <div class="form-footer">
          <span class="footer-text">还没有账号？</span>
          <span class="footer-link" @click="activeTab = 'register'">请注册</span>
          <span class="footer-sep">·</span>
          <span class="footer-text">忘记密码？</span>
          <span class="footer-link" @click="$router.push('/forgot')">请重置密码</span>
        </div>
      </div>

      <!-- 注册表单 -->
      <div v-show="activeTab === 'register'" class="auth-form">
        <h2 class="form-title">创建账号</h2>
        <p class="form-subtitle">注册后即可开始生成文章</p>

        <div class="form-item">
          <label class="form-label">邮箱</label>
          <input
            v-model="registerForm.email"
            type="email"
            class="form-input"
            placeholder="请输入邮箱"
          />
        </div>

        <div class="form-item">
          <label class="form-label">邮箱验证码</label>
          <div class="captcha-row">
            <input
              v-model="registerForm.code"
              type="text"
              class="form-input captcha-input"
              placeholder="输入 6 位验证码"
              maxlength="6"
            />
            <button
              class="code-btn"
              :disabled="codeCountdown > 0"
              @click="openSliderModal"
            >
              {{ codeCountdown > 0 ? `${codeCountdown}s` : '获取验证码' }}
            </button>
          </div>
        </div>

        <div class="form-item">
          <label class="form-label">设置密码</label>
          <input
            v-model="registerForm.password"
            type="password"
            class="form-input"
            placeholder="6-20 位密码"
          />
        </div>

        <div class="form-item">
          <label class="form-label">确认密码</label>
          <input
            v-model="registerForm.confirmPassword"
            type="password"
            class="form-input"
            placeholder="再次输入密码"
          />
        </div>

        <!-- 邀请 banner（仅 ref 存在时显示） -->
        <a-alert
          v-if="showInviteBanner"
          type="success"
          show-icon
          class="invite-banner"
        >
          <template #message>
            你收到了好友的邀请，注册并完成邮箱验证后可获得
            <CoinInfoTooltip>
              <span class="invite-coin-trigger">
                <b>50 个创作币</b>
                <svg class="invite-info-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="12" cy="12" r="10"/>
                  <line x1="12" y1="16" x2="12" y2="12"/>
                  <line x1="12" y1="8" x2="12.01" y2="8"/>
                </svg>
              </span>
            </CoinInfoTooltip>
            。
          </template>
        </a-alert>

        <div class="form-item">
          <label class="form-label">
            邀请码 <span class="form-label-optional">（选填）</span>
          </label>
          <input
            v-model="registerForm.inviteCode"
            type="text"
            class="form-input"
            placeholder="如没有可留空"
            maxlength="6"
          />
        </div>

        <AgreementCheckbox v-model="agreed" :shake-count="agreementShakeCount" />

        <button class="submit-btn" @click="handleRegister">注册</button>
      </div>
    </div>

    <!-- 底部 -->
    <footer class="login-footer">
      <span>© 2026 爱创作 · 杭州爱启云网络科技有限公司 · All Rights Reserved</span>
      <span>浙ICP备XXXXXXXX号-1</span>
    </footer>

    <!-- 注册流程：发送邮箱验证码前的人机验证弹框 -->
    <a-modal
      v-model:open="sliderModalVisible"
      title="人机验证"
      :footer="null"
      :mask-closable="false"
      :keyboard="false"
      width="420px"
      class="slider-modal slider-modal-register"
    >
      <p class="slider-modal-tip">
        按顺序点击下方成语中的汉字完成验证后将向
        <b>{{ registerMode === 'email' ? (registerForm.email || '当前邮箱') : (registerForm.phone || '当前手机号') }}</b>
        发送 6 位{{ registerMode === 'email' ? '邮箱' : '短信' }}验证码
      </p>
      <GridClickCaptcha v-model="sliderModalPassed" />
    </a-modal>

    <!-- 登录流程：调用后端登录接口前的人机验证弹框 -->
    <a-modal
      v-model:open="loginSliderModalVisible"
      title="人机验证"
      :footer="null"
      :mask-closable="false"
      :keyboard="false"
      width="420px"
      class="slider-modal slider-modal-login"
    >
      <p class="slider-modal-tip">
        按顺序点击下方成语中的汉字完成验证后将登录账号
        <b v-if="loginIdentifier">「{{ loginIdentifier }}」</b>
      </p>
      <GridClickCaptcha v-model="loginModalPassed" />
    </a-modal>
  </div>
  </PullToRefresh>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import NavBar from '@/components/layout/NavBar.vue'
import CoinInfoTooltip from '@/components/CoinInfoTooltip.vue'
import GridClickCaptcha from '@/components/GridClickCaptcha.vue'
import PullToRefresh from '@/components/PullToRefresh.vue'
import MobileLogin from '@/views/MobileLogin.vue'
import AgreementCheckbox from '@/components/AgreementCheckbox.vue'
import { useDevice } from '@/composables/useDevice.js'
import { useLogin } from '@/composables/useLogin.js'

const { isMobile } = useDevice()

const {
  activeTab,
  showInviteBanner,
  agreed,
  agreementShakeCount,
  loginMode,
  registerMode,
  loginForm,
  registerForm,
  loginIdentifier,
  sliderModalVisible,
  sliderModalPassed,
  loginSliderModalVisible,
  loginModalPassed,
  codeCountdown,
  openSliderModal,
  handleLogin,
  handleRegister,
  rememberMe
} = useLogin()

const navLinks = [
  { to: '/', label: '首页' },
  { to: '/pricing', label: '会员' },
  { to: '/lottery', label: '活动' },
  { to: '/guide', label: '玩法指南' },
  { to: '/learn', label: '创作学院' }
]
const ctaTo = '/login'
const ctaLabel = '开始创作'

// ---------- 鼠标方向律动：卡片轻微朝鼠标方向平移（仅 PC） ----------
const cardRef = ref(null)
const MAGNET_OFFSET_PX = 8

const onPageMouseMove = (e) => {
  const card = cardRef.value
  if (!card) return
  const rect = card.getBoundingClientRect()
  const cardCenterX = rect.left + rect.width / 2
  const cardCenterY = rect.top + rect.height / 2
  const dx = e.clientX - cardCenterX
  const dy = e.clientY - cardCenterY
  const nx = Math.max(-1, Math.min(1, dx / (window.innerWidth / 2)))
  const ny = Math.max(-1, Math.min(1, dy / (window.innerHeight / 2)))
  card.style.setProperty('--mx', `${nx * MAGNET_OFFSET_PX}px`)
  card.style.setProperty('--my', `${ny * MAGNET_OFFSET_PX}px`)
}

onMounted(() => {
  window.addEventListener('mousemove', onPageMouseMove)
})

onBeforeUnmount(() => {
  window.removeEventListener('mousemove', onPageMouseMove)
})
</script>

<style scoped>
.login-page {
  height: 100dvh;
  background: linear-gradient(180deg, #f0f5ff 0%, #fff 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  overflow: hidden;
  user-select: none;
  -webkit-user-select: none;
}

/* 背景装饰 */
.login-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.bg-circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(24, 144, 255, 0.05);
}

.bg-circle-1 {
  width: 600px;
  height: 600px;
  top: -300px;
  right: -150px;
}

.bg-circle-2 {
  width: 500px;
  height: 500px;
  bottom: -200px;
  left: -150px;
}

/* 登录卡片 */
.login-card {
  background: rgba(255, 255, 255, 0.97);
  border-radius: 20px;
  padding: 40px;
  width: 420px;
  margin-top: 40px;
  box-shadow: 0 25px 80px rgba(0, 0, 0, 0.4);
  position: relative;
  z-index: 1;
  /* 鼠标方向律动：卡片朝鼠标方向轻微平移（最大 ±8px） */
  transform: translate(var(--mx, 0px), var(--my, 0px));
  transition: transform 0.5s cubic-bezier(0.2, 0.8, 0.2, 1),
              box-shadow 0.35s ease;
  will-change: transform;
}

/* 标签切换 */
.auth-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 28px;
  background: #f5f5f5;
  padding: 4px;
  border-radius: 10px;
}

.auth-tab {
  flex: 1;
  padding: 10px;
  border: none;
  background: transparent;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #595959;
  cursor: pointer;
  transition: all 0.2s;
}

.auth-tab.active {
  background: #fff;
  color: #1a1a1a;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

/* 表单 */
.form-title {
  text-align: center;
  font-size: 22px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 6px;
}

.form-subtitle {
  text-align: center;
  color: #595959;
  font-size: 14px;
  margin-bottom: 24px;
}

.form-item {
  margin-bottom: 16px;
}

.form-label {
  display: block;
  margin-bottom: 6px;
  font-size: 14px;
  color: #262626;
  font-weight: 500;
}

.form-label-optional {
  color: #8c8c8c;
  font-weight: 400;
}

.invite-banner {
  margin-bottom: 16px;
  border-radius: 8px;
}

.invite-coin-trigger {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  cursor: help;
  border-bottom: 1px dashed currentColor;
  padding-bottom: 1px;
  vertical-align: baseline;
}

.invite-info-icon {
  width: 14px;
  height: 14px;
  color: currentColor;
  flex-shrink: 0;
}

.form-input {
  width: 100%;
  padding: 12px 14px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 14px;
  color: #1a1a1a;
  transition: border-color 0.2s, box-shadow 0.2s;
  box-sizing: border-box;
}

.form-input:focus {
  outline: none;
  border-color: #FF2442;
  box-shadow: 0 0 0 3px rgba(255, 36, 66, 0.1);
}

.form-input::placeholder {
  color: #bfbfbf;
}

.captcha-row {
  display: flex;
  gap: 10px;
}

.captcha-input {
  flex: 1;
}

.code-btn {
  padding: 0 14px;
  background: #fff;
  border: 1px solid #FF2442;
  border-radius: 8px;
  color: #FF2442;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;
}

.code-btn:hover:not(:disabled) {
  background: #FF2442;
  color: #fff;
}

.code-btn:disabled {
  border-color: #d9d9d9;
  color: #8c8c8c;
  cursor: not-allowed;
}

/* 提交按钮 */
.submit-btn {
  width: 100%;
  padding: 13px;
  background: #FF2442;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  margin-top: 8px;
  transition: all 0.2s;
}

.submit-btn:hover {
  background: #E61E3A;
  box-shadow: 0 6px 20px rgba(255, 36, 66, 0.35);
}

/* 记住我 */
.remember-row {
  margin: 4px 0;
}

.remember-label {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  cursor: pointer;
  font-size: 13px;
  line-height: 1.5;
  color: #595959;
}

.remember-checkbox {
  width: 16px;
  height: 16px;
  margin-top: 1px;
  flex-shrink: 0;
  accent-color: #FF2442;
  cursor: pointer;
}

.remember-text {
  flex: 1;
}

body[data-theme="dark"] .remember-label {
  color: #a6a6a6;
}

/* 表单底部 */
.form-footer {
  display: flex;
  justify-content: center;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 16px;
}

.footer-text {
  font-size: 13px;
  color: #595959;
}

.footer-link {
  font-size: 13px;
  color: #FF2442;
  cursor: pointer;
  transition: color 0.2s;
}

.footer-link:hover {
  color: #E61E3A;
}

.footer-sep {
  font-size: 13px;
  color: #d9d9d9;
  margin: 0 4px;
}

/* 底部 */
.login-footer {
  margin-top: auto;
  padding: 16px 24px;
  border-top: 1px solid #eee;
  color: #595959;
  font-size: 13px;
  text-align: center;
  background: #fff;
  width: 100%;
  box-sizing: border-box;
}

.login-footer span + span::before {
  content: '|';
  margin: 0 12px;
  color: #eee;
}

/* 邮箱/手机切换 */
.mode-toggle {
  display: flex;
  gap: 8px;
}
.mode-toggle-btn {
  flex: 1;
  padding: 8px;
  border: 1px solid #d9d9d9;
  background: #fff;
  border-radius: 8px;
  font-size: 13px;
  color: #595959;
  cursor: pointer;
  transition: all 0.2s;
}
.mode-toggle-btn.active {
  border-color: #FF2442;
  color: #FF2442;
  background: #fff0f2;
}
body[data-theme="dark"] .mode-toggle-btn {
  background: #1f1f1f;
  border-color: #404040;
  color: #a6a6a6;
}
body[data-theme="dark"] .mode-toggle-btn.active {
  border-color: #ff4d6f;
  color: #ff4d6f;
  background: rgba(255, 77, 111, 0.12);
}

/* ========== 媒体查询：手机端 ≤768px ========== */
@media (max-width: 768px) {
  .login-card {
    width: calc(100% - 32px);
    padding: 24px 20px;
    margin-top: 56px;
    border-radius: 16px;
    box-shadow: 0 12px 40px rgba(0, 0, 0, 0.25);
  }
  .auth-tabs {
    margin-bottom: 20px;
  }
  .form-title {
    font-size: 20px;
  }
  .form-subtitle {
    font-size: 13px;
    margin-bottom: 20px;
  }
  .form-item {
    margin-bottom: 14px;
  }
  .form-input {
    padding: 10px 12px;
    font-size: 14px;
  }
  .submit-btn {
    padding: 12px;
    font-size: 14px;
  }
  .bg-circle-1 {
    width: 320px;
    height: 320px;
    top: -160px;
    right: -100px;
  }
  .bg-circle-2 {
    width: 280px;
    height: 280px;
    bottom: -120px;
    left: -100px;
  }
}

/* ========== 暗色主题 ========== */
body[data-theme="dark"] .login-page {
  background: linear-gradient(180deg, #1a1a1a 0%, #141414 100%);
}

body[data-theme="dark"] .bg-circle {
  background: rgba(255, 36, 66, 0.05);
}

body[data-theme="dark"] .login-card {
  background: #1f1f1f;
  box-shadow: 0 25px 80px rgba(0, 0, 0, 0.5);
}

body[data-theme="dark"] .auth-tabs {
  background: #262626;
}

body[data-theme="dark"] .auth-tab {
  color: #a6a6a6;
}

body[data-theme="dark"] .auth-tab.active {
  background: #1f1f1f;
  color: #e0e0e0;
}

body[data-theme="dark"] .form-title,
body[data-theme="dark"] .form-label {
  color: #e0e0e0;
}

body[data-theme="dark"] .form-subtitle,
body[data-theme="dark"] .form-label-optional,
body[data-theme="dark"] .footer-text {
  color: #a6a6a6;
}

body[data-theme="dark"] .footer-link {
  color: #ff4d6f;
}

body[data-theme="dark"] .footer-link:hover {
  color: #ff7a99;
}

body[data-theme="dark"] .footer-sep {
  color: #404040;
}

body[data-theme="dark"] .form-input {
  background: #262626;
  border-color: #404040;
  color: #e0e0e0;
}

body[data-theme="dark"] .form-input:focus {
  border-color: #ff4d6f;
  box-shadow: 0 0 0 3px rgba(255, 36, 66, 0.2);
}

body[data-theme="dark"] .form-input::placeholder {
  color: #666;
}

body[data-theme="dark"] .code-btn {
  background: #1f1f1f;
  border-color: #ff4d6f;
  color: #ff4d6f;
}

body[data-theme="dark"] .code-btn:hover:not(:disabled) {
  background: #ff4d6f;
  color: #fff;
}

body[data-theme="dark"] .code-btn:disabled {
  border-color: #404040;
  color: #666;
}

body[data-theme="dark"] .submit-btn {
  background: linear-gradient(135deg, #FF6B8A 0%, #FF2442 100%);
}

body[data-theme="dark"] .submit-btn:hover {
  background: linear-gradient(135deg, #FF4D6F 0%, #E61E3A 100%);
}

body[data-theme="dark"] .login-footer {
  background: #1f1f1f;
  border-top-color: #303030;
  color: #a6a6a6;
}

body[data-theme="dark"] .login-footer span + span::before {
  color: #303030;
}

body[data-theme="dark"] .invite-banner {
  background: rgba(255, 36, 66, 0.12) !important;
  border-color: rgba(255, 77, 111, 0.4) !important;
}

body[data-theme="dark"] .invite-banner :deep(.ant-alert-message) {
  color: #e0e0e0 !important;
}

/* ========== 滑块弹框 ========== */
.slider-modal-tip {
  font-size: 13px;
  color: #595959;
  margin-bottom: 16px;
  line-height: 1.6;
}

.slider-modal-tip b {
  color: #FF2442;
  font-weight: 500;
  word-break: break-all;
}

body[data-theme="dark"] .slider-modal-tip {
  color: #a6a6a6;
}

body[data-theme="dark"] .slider-modal-tip b {
  color: #ff4d6f;
}

.slider-modal :deep(.ant-modal-header) {
  margin-bottom: 12px;
}

body[data-theme="dark"] .slider-modal :deep(.ant-modal-content) {
  background: #1f1f1f;
}

body[data-theme="dark"] .slider-modal :deep(.ant-modal-header) {
  background: transparent;
}

body[data-theme="dark"] .slider-modal :deep(.ant-modal-title) {
  color: #e0e0e0;
}

body[data-theme="dark"] .invite-coin-trigger {
  color: #ff4d6f;
}
</style>
