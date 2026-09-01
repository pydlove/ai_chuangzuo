<template>
  <div class="mobile-login">
    <!-- 顶部导航 -->
    <header class="ml-header">
      <router-link to="/" class="ml-header__logo">
        <img src="/favicon.png" alt="爱创作工坊" class="ml-header__logo-img" />
        <span class="ml-header__brand">爱创作工坊</span>
      </router-link>
      <router-link to="/" class="ml-header__home">
        <Icon name="home" :size="18" />
      </router-link>
    </header>

    <!-- 表单卡片 -->
    <main class="ml-main">
      <h1 class="ml-title">
        Hey! {{ activeTab === 'login' ? '欢迎回到爱创作工坊' : '欢迎来到爱创作工坊' }}
      </h1>

      <Tabs
        v-model="activeTab"
        :tabs="[
          { label: '登录', value: 'login' },
          { label: '注册', value: 'register' }
        ]"
        variant="segment"
        active-type="surface"
        equal-width
        :scrollable="false"
      />

      <!-- 登录表单 -->
      <div v-show="activeTab === 'login'" class="ml-form">
        <div class="ml-field">
          <div class="ml-input-wrap">
            <Icon name="mail" class="ml-field__icon" />
            <input
              v-model="loginForm.identifier"
              type="text"
              class="ml-field__input"
              placeholder="请输入手机号或邮箱"
              autocomplete="off"
            />
          </div>
        </div>

        <div class="ml-field">
          <div class="ml-input-wrap">
            <Icon name="lock" class="ml-field__icon" />
            <input
              v-model="loginForm.password"
              :type="loginShowPassword ? 'text' : 'password'"
              class="ml-field__input"
              placeholder="请输入密码"
              autocomplete="current-password"
            />
            <button type="button" class="ml-eye-btn" @click="loginShowPassword = !loginShowPassword">
              <Icon v-if="loginShowPassword" name="eye" class="ml-eye-icon" :size="18" />
              <Icon v-else name="eye-off" class="ml-eye-icon" :size="18" />
            </button>
          </div>
        </div>

        <div class="ml-field remember-row">
          <label class="remember-label">
            <input
              v-model="rememberMe"
              type="checkbox"
              class="remember-checkbox"
            />
            <span class="remember-text">记住我</span>
          </label>
          <span class="ml-forgot" @click="$router.push('/forgot')">忘记密码？</span>
        </div>

        <AgreementCheckbox v-model="agreed" :shake-count="agreementShakeCount" />

        <button class="ml-submit" @click="handleLogin">登录</button>

        <div class="ml-footer-links">
          <span @click="activeTab = 'register'">还没有账号，去注册</span>
        </div>
      </div>

      <!-- 注册表单 -->
      <div v-show="activeTab === 'register'" class="ml-form">
        <div class="ml-field">
          <div class="ml-input-wrap">
            <Icon name="mail" class="ml-field__icon" />
            <input
              v-model="registerForm.identifier"
              type="text"
              class="ml-field__input"
              placeholder="请输入手机号或邮箱"
              autocomplete="off"
            />
          </div>
        </div>

        <div class="ml-field">
          <div class="ml-input-wrap">
            <Icon name="id-card" class="ml-field__icon" />
            <input
              v-model="registerForm.code"
              type="text"
              class="ml-field__input"
              placeholder="输入 6 位验证码"
              maxlength="6"
              inputmode="numeric"
              autocomplete="one-time-code"
            />
            <button
              class="ml-captcha__btn"
              :disabled="codeCountdown > 0"
              @click="openSliderModal"
            >
              {{ codeCountdown > 0 ? `${codeCountdown}s` : '获取验证码' }}
            </button>
          </div>
          <p v-if="registerMode === 'phone'" class="ml-sms-provider">短信服务由 恒创联众 提供支持</p>
        </div>

        <div class="ml-field">
          <div class="ml-input-wrap">
            <Icon name="lock" class="ml-field__icon" />
            <input
              v-model="registerForm.password"
              :type="registerShowPassword ? 'text' : 'password'"
              class="ml-field__input"
              placeholder="6-20 位密码"
              autocomplete="new-password"
            />
            <button type="button" class="ml-eye-btn" @click="registerShowPassword = !registerShowPassword">
              <Icon v-if="registerShowPassword" name="eye" class="ml-eye-icon" :size="18" />
              <Icon v-else name="eye-off" class="ml-eye-icon" :size="18" />
            </button>
          </div>
        </div>

        <div class="ml-field">
          <div class="ml-input-wrap">
            <Icon name="lock" class="ml-field__icon" />
            <input
              v-model="registerForm.confirmPassword"
              :type="registerShowPassword ? 'text' : 'password'"
              class="ml-field__input"
              placeholder="再次输入密码"
              autocomplete="new-password"
            />
          </div>
        </div>

        <a-alert
          v-if="showInviteBanner"
          type="success"
          show-icon
          class="ml-invite-banner"
        >
          <template #message>
            你收到了好友的邀请，注册并完成邮箱验证后可获得
            <CoinInfoTooltip>
              <span class="ml-invite-trigger">
                <b>50 个创作币</b>
                <Icon name="info" class="ml-invite-icon" :size="14" />
              </span>
            </CoinInfoTooltip>
            。
          </template>
        </a-alert>

        <div class="ml-field">
          <div class="ml-input-wrap">
            <Icon name="message-circle" class="ml-field__icon" />
            <input
              v-model="registerForm.inviteCode"
              type="text"
              class="ml-field__input"
              placeholder="邀请码（选填）"
              maxlength="6"
            />
          </div>
        </div>

        <AgreementCheckbox v-model="agreed" :shake-count="agreementShakeCount" />

        <button class="ml-submit" @click="handleRegister">注册</button>

        <div class="ml-footer-links">
          <span @click="activeTab = 'login'">已有账号，去登录</span>
        </div>
      </div>
    </main>

    <!-- 注册人机验证弹框 -->
    <a-modal
      v-model:open="sliderModalVisible"
      title="人机验证"
      :footer="null"
      :mask-closable="false"
      :keyboard="false"
      width="min(90vw, 420px)"
      class="ml-slider-modal"
    >
      <p class="ml-slider-tip">
        按顺序点击下方成语中的汉字完成验证后将向
        <b>{{ registerMode === 'email' ? (registerForm.identifier || '当前邮箱') : registerMode === 'phone' ? (registerForm.identifier || '当前手机号') : (registerForm.identifier || '当前账号') }}</b>
        发送 6 位{{ registerMode === 'email' ? '邮箱' : registerMode === 'phone' ? '短信' : '' }}验证码
      </p>
      <GridClickCaptcha v-model="sliderModalPassed" />
    </a-modal>

    <!-- 登录人机验证弹框 -->
    <a-modal
      v-model:open="loginSliderModalVisible"
      title="人机验证"
      :footer="null"
      :mask-closable="false"
      :keyboard="false"
      width="min(90vw, 420px)"
      class="ml-slider-modal"
    >
      <p class="ml-slider-tip">
        按顺序点击下方成语中的汉字完成验证后将登录账号
        <b v-if="loginIdentifier">「{{ loginIdentifier }}」</b>
      </p>
      <GridClickCaptcha v-model="loginModalPassed" />
    </a-modal>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import CoinInfoTooltip from '@/components/CoinInfoTooltip.vue'
import GridClickCaptcha from '@/components/GridClickCaptcha.vue'
import AgreementCheckbox from '@/components/AgreementCheckbox.vue'
import Tabs from '@/components/common/Tabs.vue'
import Icon from '@/components/common/Icon.vue'
import { useLogin } from '@/composables/useLogin.js'

const {
  activeTab,
  showInviteBanner,
  agreed,
  agreementShakeCount,
  registerMode,
  loginForm,
  registerForm,
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

const loginShowPassword = ref(false)
const registerShowPassword = ref(false)
</script>

<style scoped>
.mobile-login {
  min-height: 100dvh;
  background: linear-gradient(180deg, rgba(255, 245, 247, 0.2) 0%, rgba(255, 255, 255, 0.6) 55%, rgba(255, 255, 255, 0.85) 100%);
  display: flex;
  flex-direction: column;
  position: relative;
}

.mobile-login::before {
  content: '';
  position: absolute;
  top: 60px;
  left: 0;
  right: 0;
  bottom: 0;
  background: url('/assets/images/登录背景图-v4.png') no-repeat center center / cover;
  z-index: 0;
  pointer-events: none;
}

/* 顶部导航 */
.ml-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  position: sticky;
  top: 0;
  background: rgba(255, 255, 255, 0.55);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  z-index: 2;
}

.ml-header__logo {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
}

.ml-header__logo-img {
  height: 28px;
  width: auto;
  object-fit: contain;
  border-radius: 6px;
}

.ml-header__brand {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.ml-header__home {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(245, 245, 245, 0.85);
  color: #595959;
}

.ml-header__home svg {
  width: 18px;
  height: 18px;
}

/* 表单区域 */
.ml-main {
  margin-top: auto;
  padding: 20px 16px 24px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(255, 255, 255, 0.55);
  border-radius: 24px 24px 0 0;
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  box-shadow: 0 -4px 40px rgba(0, 0, 0, 0.1);
  position: relative;
  z-index: 1;
}

.ml-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0 0 16px;
  line-height: 1.3;
}

.ml-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 16px;
}

.ml-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.ml-input-wrap {
  position: relative;
  display: flex;
  align-items: center;
}

.ml-field__icon {
  position: absolute;
  left: 14px;
  width: 18px;
  height: 18px;
  color: #8c8c8c;
  pointer-events: none;
}

.ml-field__input {
  width: 100%;
  padding: 12px 16px 12px 44px;
  border: 1px solid rgba(232, 232, 232, 0.8);
  border-radius: 12px;
  font-size: 15px;
  color: #1a1a1a;
  background: rgba(250, 250, 250, 0.65);
  transition: border-color 0.2s, box-shadow 0.2s, background 0.2s;
  box-sizing: border-box;
  -webkit-appearance: none;
}

.ml-field__input:focus {
  outline: none;
  border-color: #FF2442;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 0 0 3px rgba(255, 36, 66, 0.1);
}

.ml-field__input::placeholder {
  color: #bfbfbf;
}

.ml-eye-btn {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  background: transparent;
  border: none;
  padding: 4px;
  cursor: pointer;
  color: #8c8c8c;
  display: flex;
  align-items: center;
  justify-content: center;
}

.ml-eye-btn:hover {
  color: #595959;
}

.ml-eye-icon {
  width: 18px;
  height: 18px;
}

.ml-captcha__btn {
  position: absolute;
  right: 6px;
  top: 50%;
  transform: translateY(-50%);
  padding: 8px 14px;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid #FF2442;
  border-radius: 8px;
  color: #FF2442;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;
}

.ml-captcha__btn:hover:not(:disabled) {
  background: #FF2442;
  color: #fff;
}

.ml-captcha__btn:disabled {
  border-color: #d9d9d9;
  color: #8c8c8c;
  cursor: not-allowed;
}

.ml-sms-provider {
  margin: 4px 0 0;
  font-size: 12px;
  color: #8c8c8c;
  line-height: 1.4;
}

body[data-theme="dark"] .ml-sms-provider {
  color: #666;
}

.ml-submit {
  width: 100%;
  padding: 13px;
  margin-top: 4px;
  background: linear-gradient(135deg, #FF4D6F 0%, #FF2442 100%);
  color: #fff;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 6px 20px rgba(255, 36, 66, 0.25);
}

.ml-submit:hover {
  background: linear-gradient(135deg, #FF2442 0%, #E61E3A 100%);
  box-shadow: 0 8px 24px rgba(255, 36, 66, 0.35);
}

/* 记住我与忘记密码 */
.remember-row {
  flex-direction: row !important;
  align-items: center;
  justify-content: space-between;
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

.ml-forgot {
  font-size: 13px;
  color: #FF2442;
  cursor: pointer;
}

body[data-theme="dark"] .remember-label {
  color: #a6a6a6;
}

body[data-theme="dark"] .ml-forgot {
  color: #ff4d6f;
}

.ml-footer-links {
  display: flex;
  justify-content: center;
  font-size: 13px;
  color: #FF2442;
  margin-top: 4px;
}

.ml-footer-links span {
  cursor: pointer;
  font-weight: 500;
}

.ml-invite-banner {
  border-radius: 10px;
}

.ml-invite-trigger {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  cursor: help;
  border-bottom: 1px dashed currentColor;
  padding-bottom: 1px;
  vertical-align: baseline;
  color: #FF2442;
}

.ml-invite-icon {
  width: 14px;
  height: 14px;
}

.ml-slider-tip {
  font-size: 13px;
  color: #595959;
  margin-bottom: 16px;
  line-height: 1.6;
}

.ml-slider-tip b {
  color: #FF2442;
  font-weight: 500;
  word-break: break-all;
}

/* 暗色主题 */
body[data-theme="dark"] .mobile-login {
  background: linear-gradient(180deg, rgba(20, 14, 16, 0.65) 0%, rgba(20, 20, 20, 0.85) 55%, rgba(20, 20, 20, 0.95) 100%);
}

body[data-theme="dark"] .mobile-login::before {
  background: url('/assets/images/登录背景图-v4.png') no-repeat center center / cover;
}

body[data-theme="dark"] .ml-header {
  background: rgba(20, 20, 20, 0.55);
}

body[data-theme="dark"] .ml-header__brand {
  color: #e0e0e0;
}

body[data-theme="dark"] .ml-header__home {
  background: rgba(38, 38, 38, 0.85);
  color: #a6a6a6;
}

body[data-theme="dark"] .ml-main {
  background: rgba(31, 31, 31, 0.72);
  border-color: rgba(255, 255, 255, 0.08);
}

body[data-theme="dark"] .ml-title {
  color: #e0e0e0;
}

body[data-theme="dark"] .ml-field__input {
  background: rgba(38, 38, 38, 0.6);
  border-color: rgba(64, 64, 64, 0.8);
  color: #e0e0e0;
}

body[data-theme="dark"] .ml-field__input:focus {
  border-color: #ff4d6f;
  background: rgba(38, 38, 38, 0.85);
  box-shadow: 0 0 0 3px rgba(255, 77, 111, 0.2);
}

body[data-theme="dark"] .ml-field__input::placeholder {
  color: #666;
}

body[data-theme="dark"] .ml-field__icon {
  color: #737373;
}

body[data-theme="dark"] .ml-eye-btn {
  color: #737373;
}

body[data-theme="dark"] .ml-eye-btn:hover {
  color: #a6a6a6;
}

body[data-theme="dark"] .ml-captcha__btn {
  background: rgba(31, 31, 31, 0.6);
  border-color: #ff4d6f;
  color: #ff4d6f;
}

body[data-theme="dark"] .ml-captcha__btn:hover:not(:disabled) {
  background: #ff4d6f;
  color: #fff;
}

body[data-theme="dark"] .ml-captcha__btn:disabled {
  border-color: #404040;
  color: #666;
}

body[data-theme="dark"] .ml-submit {
  background: linear-gradient(135deg, #FF6B8A 0%, #FF2442 100%);
  box-shadow: 0 6px 20px rgba(255, 36, 66, 0.3);
}

body[data-theme="dark"] .ml-submit:hover {
  background: linear-gradient(135deg, #FF4D6F 0%, #E61E3A 100%);
}

body[data-theme="dark"] .ml-footer-links {
  color: #ff4d6f;
}

body[data-theme="dark"] .ml-slider-tip {
  color: #a6a6a6;
}

body[data-theme="dark"] .ml-slider-tip b {
  color: #ff4d6f;
}

body[data-theme="dark"] .ml-invite-trigger {
  color: #ff4d6f;
}

body[data-theme="dark"] .ml-invite-banner {
  background: rgba(255, 36, 66, 0.12) !important;
  border-color: rgba(255, 77, 111, 0.4) !important;
}

body[data-theme="dark"] .ml-invite-banner :deep(.ant-alert-message) {
  color: #e0e0e0 !important;
}
</style>
