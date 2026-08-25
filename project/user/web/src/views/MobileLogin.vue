<template>
  <div class="mobile-login">
    <header class="ml-header">
      <router-link to="/" class="ml-header__logo">
        <img src="https://foruda.gitee.com/images/1782986808430461164/e0ab39dc_8060302.png" alt="爱创作" class="ml-header__logo-img" />
        <span class="ml-header__brand">爱创作</span>
      </router-link>
      <router-link to="/" class="ml-header__home">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>
          <polyline points="9 22 9 12 15 12 15 22"/>
        </svg>
      </router-link>
    </header>

    <main class="ml-main">
      <h1 class="ml-title">{{ activeTab === 'login' ? '欢迎回来' : '创建账号' }}</h1>
      <p class="ml-subtitle">{{ activeTab === 'login' ? '登录后即可开始创作' : '注册后即可开始生成文章' }}</p>

      <!-- 标签切换 -->
      <div class="ml-tabs">
        <button
          :class="['ml-tab', { active: activeTab === 'login' }]"
          @click="activeTab = 'login'"
        >
          登录
        </button>
        <button
          :class="['ml-tab', { active: activeTab === 'register' }]"
          @click="activeTab = 'register'"
        >
          注册
        </button>
      </div>

      <!-- 登录表单 -->
      <div v-show="activeTab === 'login'" class="ml-form">
        <div class="ml-field">
          <label class="ml-field__label">手机号 / 邮箱</label>
          <input
            v-model="loginForm.identifier"
            type="text"
            class="ml-field__input"
            placeholder="请输入手机号或邮箱"
            autocomplete="off"
          />
        </div>

       <div class="ml-field">
         <label class="ml-field__label">密码</label>
         <input
           v-model="loginForm.password"
           type="password"
           class="ml-field__input"
           placeholder="请输入密码"
           autocomplete="current-password"
         />
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
        </div>

       <AgreementCheckbox v-model="agreed" :shake-count="agreementShakeCount" />

        <button class="ml-submit" @click="handleLogin">登录</button>

        <div class="ml-footer-links">
          <span @click="$router.push('/forgot')">忘记密码？</span>
          <span @click="activeTab = 'register'">还没有账号，去注册</span>
        </div>
      </div>

      <!-- 注册表单 -->
      <div v-show="activeTab === 'register'" class="ml-form">
        <div class="ml-field">
          <label class="ml-field__label">手机号 / 邮箱</label>
          <input
            v-model="registerForm.identifier"
            type="text"
            class="ml-field__input"
            placeholder="请输入手机号或邮箱"
            autocomplete="off"
          />
        </div>

        <div class="ml-field">
          <label class="ml-field__label">{{ registerMode === 'email' ? '邮箱验证码' : registerMode === 'phone' ? '短信验证码' : '验证码' }}</label>
          <div class="ml-captcha">
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
          <label class="ml-field__label">设置密码</label>
          <input
            v-model="registerForm.password"
            type="password"
            class="ml-field__input"
            placeholder="6-20 位密码"
            autocomplete="new-password"
          />
        </div>

        <div class="ml-field">
          <label class="ml-field__label">确认密码</label>
          <input
            v-model="registerForm.confirmPassword"
            type="password"
            class="ml-field__input"
            placeholder="再次输入密码"
            autocomplete="new-password"
          />
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
                <svg class="ml-invite-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="12" cy="12" r="10"/>
                  <line x1="12" y1="16" x2="12" y2="12"/>
                  <line x1="12" y1="8" x2="12.01" y2="8"/>
                </svg>
              </span>
            </CoinInfoTooltip>
            。
          </template>
        </a-alert>

        <div class="ml-field">
          <label class="ml-field__label">
            邀请码 <span class="ml-field__optional">（选填）</span>
          </label>
          <input
            v-model="registerForm.inviteCode"
            type="text"
            class="ml-field__input"
            placeholder="如没有可留空"
            maxlength="6"
          />
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
import CoinInfoTooltip from '@/components/CoinInfoTooltip.vue'
import GridClickCaptcha from '@/components/GridClickCaptcha.vue'
import AgreementCheckbox from '@/components/AgreementCheckbox.vue'
import { useLogin } from '@/composables/useLogin.js'

const {
  activeTab,
  showInviteBanner,
  agreed,
  agreementShakeCount,
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
</script>

<style scoped>
.mobile-login {
  min-height: 100dvh;
  background: linear-gradient(180deg, #fff5f7 0%, #fff 100%);
  display: flex;
  flex-direction: column;
}

.ml-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  position: sticky;
  top: 0;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(8px);
  z-index: 10;
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
  background: #f5f5f5;
  color: #595959;
}

.ml-header__home svg {
  width: 18px;
  height: 18px;
}

.ml-main {
  flex: 1;
  padding: 24px 20px 40px;
}

.ml-title {
  font-size: 26px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0 0 8px;
}

.ml-subtitle {
  font-size: 14px;
  color: #8c8c8c;
  margin: 0 0 28px;
}

.ml-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 24px;
  background: #f5f5f5;
  padding: 4px;
  border-radius: 10px;
}

.ml-tab {
  flex: 1;
  padding: 10px;
  border: none;
  background: transparent;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 500;
  color: #595959;
  cursor: pointer;
  transition: all 0.2s;
}

.ml-tab.active {
  background: #fff;
  color: #1a1a1a;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.ml-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.ml-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.ml-field__label {
  font-size: 14px;
  font-weight: 500;
  color: #262626;
}

.ml-field__optional {
  color: #8c8c8c;
  font-weight: 400;
}

.ml-field__input {
  width: 100%;
  padding: 14px 16px;
  border: 1px solid #d9d9d9;
  border-radius: 10px;
  font-size: 15px;
  color: #1a1a1a;
  background: #fff;
  transition: border-color 0.2s, box-shadow 0.2s;
  box-sizing: border-box;
  -webkit-appearance: none;
}

.ml-field__input:focus {
  outline: none;
  border-color: #FF2442;
  box-shadow: 0 0 0 3px rgba(255, 36, 66, 0.1);
}

.ml-field__input::placeholder {
  color: #bfbfbf;
}

.ml-captcha {
  display: flex;
  gap: 10px;
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

.ml-captcha .ml-field__input {
  flex: 1;
}

.ml-captcha__btn {
  padding: 0 16px;
  background: #fff;
  border: 1px solid #FF2442;
  border-radius: 10px;
  color: #FF2442;
  font-size: 14px;
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

.ml-submit {
  width: 100%;
  padding: 15px;
  margin-top: 8px;
  background: #FF2442;
  color: #fff;
  border: none;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.ml-submit:hover {
  background: #E61E3A;
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

.ml-footer-links {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #FF2442;
}

.ml-footer-links span {
  cursor: pointer;
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
  background: linear-gradient(180deg, #1a1a1a 0%, #141414 100%);
}

body[data-theme="dark"] .ml-header {
  background: rgba(20, 20, 20, 0.95);
}

body[data-theme="dark"] .ml-header__brand {
  color: #e0e0e0;
}

body[data-theme="dark"] .ml-header__home {
  background: #262626;
  color: #a6a6a6;
}

body[data-theme="dark"] .ml-title {
  color: #e0e0e0;
}

body[data-theme="dark"] .ml-subtitle {
  color: #a6a6a6;
}

body[data-theme="dark"] .ml-tabs {
  background: #262626;
}

body[data-theme="dark"] .ml-tab {
  color: #a6a6a6;
}

body[data-theme="dark"] .ml-tab.active {
  background: #1f1f1f;
  color: #e0e0e0;
}

body[data-theme="dark"] .ml-field__label {
  color: #e0e0e0;
}

body[data-theme="dark"] .ml-field__optional {
  color: #8c8c8c;
}

body[data-theme="dark"] .ml-field__input {
  background: #1f1f1f;
  border-color: #404040;
  color: #e0e0e0;
}

body[data-theme="dark"] .ml-field__input:focus {
  border-color: #ff4d6f;
  box-shadow: 0 0 0 3px rgba(255, 77, 111, 0.2);
}

body[data-theme="dark"] .ml-field__input::placeholder {
  color: #666;
}

body[data-theme="dark"] .ml-captcha__btn {
  background: #1f1f1f;
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
</style>
