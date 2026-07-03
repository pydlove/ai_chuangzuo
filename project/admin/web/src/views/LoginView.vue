<template>
  <div class="login-page">
    <!-- 背景装饰 -->
    <div class="login-bg">
      <div class="bg-circle bg-circle-1"></div>
      <div class="bg-circle bg-circle-2"></div>
    </div>

    <!-- 导航栏 -->
    <header class="login-nav">
      <div class="nav-brand">
        <img
          src="https://foruda.gitee.com/images/1782986808430461164/e0ab39dc_8060302.png"
          alt="爱创作"
          class="nav-logo"
        />
        <span class="nav-brand-name">爱创作</span>
        <span class="nav-brand-tag">管理控制台</span>
      </div>
      <button
        class="theme-toggle"
        :title="currentTheme === 'light' ? '切换深色主题' : '切换浅色主题'"
        @click="toggleTheme"
      >
        <svg
          v-if="currentTheme === 'light'"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="1.5"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" />
        </svg>
        <svg
          v-else
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="1.5"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <circle cx="12" cy="12" r="5" />
          <line x1="12" y1="1" x2="12" y2="3" />
          <line x1="12" y1="21" x2="12" y2="23" />
          <line x1="4.22" y1="4.22" x2="5.64" y2="5.64" />
          <line x1="18.36" y1="18.36" x2="19.78" y2="19.78" />
          <line x1="1" y1="12" x2="3" y2="12" />
          <line x1="21" y1="12" x2="23" y2="12" />
          <line x1="4.22" y1="19.78" x2="5.64" y2="18.36" />
          <line x1="18.36" y1="5.64" x2="19.78" y2="4.22" />
        </svg>
      </button>
    </header>

    <!-- 登录卡片 -->
    <div class="login-card">
      <h2 class="form-title">管理控制台登录</h2>
      <p class="form-subtitle">仅限授权管理员访问</p>

      <a-alert
        v-if="errorMessage"
        type="error"
        show-icon
        :message="errorMessage"
        class="form-alert"
        closable
        @close="errorMessage = ''"
      />

      <div class="form-item" :class="{ 'has-error': errors.account }">
        <label class="form-label">账号</label>
        <input
          v-model="form.account"
          type="text"
          class="form-input"
          placeholder="请输入管理员账号"
          @blur="validateField('account')"
          @input="clearError('account')"
        />
        <span v-if="errors.account" class="error-text">{{ errors.account }}</span>
      </div>

      <div class="form-item" :class="{ 'has-error': errors.password }">
        <label class="form-label">密码</label>
        <div class="password-row">
          <input
            v-model="form.password"
            :type="showPassword ? 'text' : 'password'"
            class="form-input password-input"
            placeholder="请输入密码"
            @blur="validateField('password')"
            @input="clearError('password')"
            @keyup.enter="handleLogin"
          />
          <button class="password-toggle" @click="showPassword = !showPassword">
            <svg
              v-if="showPassword"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="1.5"
            >
              <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
              <circle cx="12" cy="12" r="3" />
            </svg>
            <svg
              v-else
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="1.5"
            >
              <path
                d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"
              />
              <line x1="1" y1="1" x2="23" y2="23" />
            </svg>
          </button>
        </div>
        <span v-if="errors.password" class="error-text">{{ errors.password }}</span>
      </div>

      <div class="form-item" :class="{ 'has-error': errors.captcha }">
        <label class="form-label">图形验证码</label>
        <div class="captcha-row">
          <input
            v-model="form.captcha"
            type="text"
            class="form-input captcha-input"
            placeholder="输入验证码"
            maxlength="4"
            @blur="validateField('captcha')"
            @input="clearError('captcha')"
            @keyup.enter="handleLogin"
          />
          <div class="captcha-box" @click="refreshCaptcha">{{ captchaText }}</div>
        </div>
        <span v-if="errors.captcha" class="error-text">{{ errors.captcha }}</span>
      </div>

      <div class="form-item remember-row">
        <label class="remember-label"
          <input v-model="rememberMe" type="checkbox" class="remember-checkbox" />
          记住我
        </label>
      </div>

      <a-button
        type="primary"
        size="large"
        :loading="loading"
        :disabled="isLocked"
        block
        class="submit-btn"
        @click="handleLogin"
      >
        {{ isLocked ? '已锁定' : '登录' }}
      </a-button>
    </div>

    <!-- 底部 -->
    <footer class="login-footer">
      <span>© 2026 爱创作 · 杭州爱启云网络科技有限公司 · All Rights Reserved</span>
      <span>浙ICP备XXXXXXXX号-1</span>
    </footer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useTheme } from '@/composables/useTheme.js'
import { adminAuthLogin } from '@/api/auth.js'
import { useUserStore } from '@/stores/user.js'
import storage from '@/utils/storage.js'

const router = useRouter()
const userStore = useUserStore()
const { currentTheme, loadTheme, toggleTheme } = useTheme()

const REMEMBER_KEY = 'aichuangzuo_admin_remember_account'
const MAX_FAIL_COUNT = 5

const form = reactive({
  account: '',
  password: '',
  captcha: ''
})

const errors = reactive({
  account: '',
  password: '',
  captcha: ''
})

const rememberMe = ref(false)
const showPassword = ref(false)
const loading = ref(false)
const errorMessage = ref('')
const isLocked = ref(false)
const failCount = ref(0)

const captchaText = ref('')
const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'

const generateCaptcha = () => {
  let result = ''
  for (let i = 0; i < 4; i++) {
    result += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  captchaText.value = result
}

const refreshCaptcha = () => {
  generateCaptcha()
}

const validateField = (field) => {
  errors[field] = ''
  if (!form[field]) {
    const labels = {
      account: '请输入管理员账号',
      password: '请输入密码',
      captcha: '请输入验证码'
    }
    errors[field] = labels[field]
    return false
  }
  return true
}

const clearError = (field) => {
  errors[field] = ''
}

const validateForm = () => {
  const results = [validateField('account'), validateField('password'), validateField('captcha')]
  return results.every(Boolean)
}

const handleLogin = async () => {
  if (isLocked.value) return
  if (!validateForm()) return

  if (form.captcha.toUpperCase() !== captchaText.value) {
    errors.captcha = '验证码错误'
    refreshCaptcha()
    return
  }

  loading.value = true
  errorMessage.value = ''

  try {
    const res = await adminAuthLogin({
      account: form.account,
      password: form.password,
      captcha: form.captcha
    })

    userStore.setToken(res.data?.token || 'mock-token')
    userStore.setUserInfo(res.data?.userInfo || null)

    if (rememberMe.value) {
      storage.set(REMEMBER_KEY, form.account)
    } else {
      storage.remove(REMEMBER_KEY)
    }

    message.success('登录成功')
    router.push('/console')
  } catch (error) {
    failCount.value++
    errorMessage.value = '账号、密码或验证码错误'

    if (failCount.value >= MAX_FAIL_COUNT) {
      isLocked.value = true
      errorMessage.value = '失败次数过多，请 15 分钟后重试或联系超级管理员'
    }

    refreshCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadTheme()
  generateCaptcha()

  const remembered = storage.get(REMEMBER_KEY)
  if (remembered) {
    form.account = remembered
    rememberMe.value = true
  }
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
}

.login-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.bg-circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 36, 66, 0.05);
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

.login-nav {
  width: 100%;
  padding: 14px 48px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  position: relative;
  z-index: 1;
  box-shadow: 0 1px 0 rgba(0, 0, 0, 0.05);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.nav-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  user-select: none;
}

.nav-logo {
  height: 32px;
  width: auto;
}

.nav-brand-name {
  font-weight: 700;
  font-size: 18px;
  color: #1a1a1a;
}

.nav-brand-tag {
  font-size: 12px;
  color: #ff2442;
  background: #fff0f2;
  padding: 2px 8px;
  border-radius: 9999px;
  font-weight: 500;
}

.theme-toggle {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  border: none;
  background: transparent;
  color: #595959;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.theme-toggle:hover {
  background: #fff5f7;
  color: #ff2442;
}

.theme-toggle svg {
  width: 18px;
  height: 18px;
}

.login-card {
  background: rgba(255, 255, 255, 0.97);
  border-radius: 20px;
  padding: 40px;
  width: 420px;
  margin-top: 40px;
  box-shadow: 0 25px 80px rgba(0, 0, 0, 0.4);
  position: relative;
  z-index: 1;
}

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

.form-alert {
  margin-bottom: 16px;
}

.form-item {
  margin-bottom: 16px;
}

.form-item.has-error .form-input {
  border-color: #ff4d4f;
  box-shadow: 0 0 0 3px rgba(255, 77, 79, 0.1);
}

.form-label {
  display: block;
  margin-bottom: 6px;
  font-size: 14px;
  color: #262626;
  font-weight: 500;
}

.form-input {
  width: 100%;
  padding: 12px 14px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 14px;
  color: #1a1a1a;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.form-input:focus {
  outline: none;
  border-color: #ff2442;
  box-shadow: 0 0 0 3px rgba(255, 36, 66, 0.1);
}

.form-input::placeholder {
  color: #bfbfbf;
}

.password-row {
  position: relative;
}

.password-input {
  padding-right: 42px;
}

.password-toggle {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  width: 20px;
  height: 20px;
  border: none;
  background: transparent;
  color: #8c8c8c;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
}

.password-toggle:hover {
  color: #595959;
}

.password-toggle svg {
  width: 18px;
  height: 18px;
}

.captcha-row {
  display: flex;
  gap: 10px;
}

.captcha-input {
  flex: 1;
}

.captcha-box {
  width: 90px;
  height: 42px;
  background: linear-gradient(135deg, #fff0f0 0%, #fff 100%);
  border: 1px solid #ffbdc5;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 700;
  color: #ff2442;
  letter-spacing: 4px;
  cursor: pointer;
  user-select: none;
}

.error-text {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  color: #ff4d4f;
}

.remember-row {
  margin-bottom: 8px;
}

.remember-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #595959;
  cursor: pointer;
}

.remember-checkbox {
  width: 14px;
  height: 14px;
  accent-color: #ff2442;
  cursor: pointer;
}

.submit-btn {
  height: 44px;
  font-size: 15px;
  font-weight: 600;
}

.login-footer {
  margin-top: auto;
  padding: 16px 24px;
  border-top: 1px solid #eeeeee;
  color: #595959;
  font-size: 13px;
  text-align: center;
  background: #ffffff;
  width: 100%;
}

.login-footer span + span::before {
  content: '|';
  margin: 0 12px;
  color: #eeeeee;
}

/* 深色主题 */
body[data-theme='dark'] .login-page {
  background: linear-gradient(180deg, #1a1a1a 0%, #141414 100%);
}

body[data-theme='dark'] .bg-circle {
  background: rgba(255, 36, 66, 0.05);
}

body[data-theme='dark'] .login-nav {
  background: rgba(31, 31, 31, 0.9);
  box-shadow: 0 1px 0 rgba(255, 255, 255, 0.05);
}

body[data-theme='dark'] .nav-brand-name {
  color: #e0e0e0;
}

body[data-theme='dark'] .nav-brand-tag {
  background: rgba(255, 36, 66, 0.15);
  color: #ff4d6f;
}

body[data-theme='dark'] .theme-toggle {
  color: #a6a6a6;
}

body[data-theme='dark'] .theme-toggle:hover {
  background: rgba(255, 36, 66, 0.15);
  color: #ff4d6f;
}

body[data-theme='dark'] .login-card {
  background: #1f1f1f;
  box-shadow: 0 25px 80px rgba(0, 0, 0, 0.5);
}

body[data-theme='dark'] .form-title,
body[data-theme='dark'] .form-label {
  color: #e0e0e0;
}

body[data-theme='dark'] .form-subtitle,
body[data-theme='dark'] .remember-label {
  color: #a6a6a6;
}

body[data-theme='dark'] .form-input {
  background: #262626;
  border-color: #404040;
  color: #e0e0e0;
}

body[data-theme='dark'] .form-input:focus {
  border-color: #ff4d6f;
  box-shadow: 0 0 0 3px rgba(255, 77, 111, 0.2);
}

body[data-theme='dark'] .form-input::placeholder {
  color: #666666;
}

body[data-theme='dark'] .captcha-box {
  background: linear-gradient(135deg, #2a1a1d 0%, #1f1f1f 100%);
  border-color: rgba(255, 77, 111, 0.4);
  color: #ff4d6f;
}

body[data-theme='dark'] .password-toggle {
  color: #8c8c8c;
}

body[data-theme='dark'] .password-toggle:hover {
  color: #a6a6a6;
}

body[data-theme='dark'] .login-footer {
  background: #1f1f1f;
  border-top-color: #303030;
  color: #a6a6a6;
}

body[data-theme='dark'] .login-footer span + span::before {
  color: #303030;
}

body[data-theme='dark'] .submit-btn {
  background: linear-gradient(135deg, #ff6b8a 0%, #ff2442 100%);
  border-color: transparent;
}

body[data-theme='dark'] .submit-btn:hover {
  background: linear-gradient(135deg, #ff4d6f 0%, #e61e3a 100%);
}
</style>
