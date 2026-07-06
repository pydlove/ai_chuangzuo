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

      <div class="form-item" :class="{ 'has-error': errors.username }">
        <label class="form-label">账号</label>
        <input
          v-model="form.username"
          type="text"
          class="form-input"
          placeholder="请输入管理员账号"
          @blur="validateField('username')"
          @input="clearError('username')"
          @keyup.enter="openSliderModal"
        />
        <span v-if="errors.username" class="error-text">{{ errors.username }}</span>
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
            @keyup.enter="openSliderModal"
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

      <a-button
        type="primary"
        size="large"
        :loading="loading"
        block
        class="submit-btn"
        @click="openSliderModal"
      >
        登录
      </a-button>
    </div>

    <!-- 底部 -->
    <footer class="login-footer">
      <span>© 2026 爱创作 · 杭州爱启云网络科技有限公司 · All Rights Reserved</span>
      <span>浙ICP备XXXXXXXX号-1</span>
    </footer>

    <!-- 滑块验证弹框 -->
    <a-modal
      v-model:open="sliderVisible"
      title="人机验证"
      :footer="null"
      :mask-closable="false"
      :keyboard="false"
      width="420px"
      class="slider-modal"
      @cancel="resetSlider"
    >
      <p class="slider-modal-tip">
        拖动滑块完成验证后将登录账号
        <b v-if="form.username">「{{ form.username }}」</b>
      </p>
      <SliderCaptcha v-model="sliderPassed" />
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import SliderCaptcha from '@/components/SliderCaptcha.vue'
import { useTheme } from '@/composables/useTheme.js'
import { adminAuthLogin } from '@/api/auth.js'
import { useUserStore } from '@/stores/user.js'

const router = useRouter()
const userStore = useUserStore()
const { currentTheme, loadTheme, toggleTheme } = useTheme()

const form = reactive({
  username: '',
  password: ''
})

const errors = reactive({
  username: '',
  password: ''
})

const showPassword = ref(false)
const loading = ref(false)
const errorMessage = ref('')

const sliderVisible = ref(false)
const sliderPassed = ref(false)
let sliderSending = false

const validateField = (field) => {
  errors[field] = ''
  if (!form[field]) {
    const labels = {
      username: '请输入管理员账号',
      password: '请输入密码'
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
  return [validateField('username'), validateField('password')].every(Boolean)
}

const openSliderModal = () => {
  if (loading.value) return
  if (!validateForm()) return
  sliderPassed.value = false
  sliderVisible.value = true
}

const resetSlider = () => {
  sliderPassed.value = false
  sliderVisible.value = false
}

watch(sliderPassed, async (val) => {
  if (!val || sliderSending) return
  sliderSending = true
  loading.value = true
  errorMessage.value = ''

  try {
    const res = await adminAuthLogin({
      username: form.username,
      password: form.password
    })

    userStore.setToken(res.data?.accessToken || '')
    userStore.setUserInfo(res.data?.user || null)

    // 同时保存 refreshToken，便于后续续期
    if (res.data?.refreshToken) {
      localStorage.setItem('admin_refresh_token', res.data.refreshToken)
    }

    message.success('登录成功')
    resetSlider()
    router.push('/console/users')
  } catch (error) {
    errorMessage.value = error?.message || '登录失败，请稍后重试'
    resetSlider()
  } finally {
    loading.value = false
    sliderSending = false
  }
})

onMounted(() => {
  loadTheme()
  // 测试阶段预填内置管理员账号，避免反复输入
  form.username = 'admin'
  form.password = 'Root1qaz!QAZ'
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
  box-sizing: border-box;
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

.error-text {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  color: #ff4d4f;
}

.submit-btn {
  height: 44px;
  font-size: 15px;
  font-weight: 600;
  background: #ff2442;
  border-color: #ff2442;
}

.submit-btn:hover,
.submit-btn:focus {
  background: #e61e3a;
  border-color: #e61e3a;
}

.submit-btn:disabled {
  background: #ff9aae;
  border-color: #ff9aae;
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
  box-sizing: border-box;
}

.login-footer span + span::before {
  content: '|';
  margin: 0 12px;
  color: #eeeeee;
}

/* 滑块弹框 */
.slider-modal-tip {
  font-size: 13px;
  color: #595959;
  margin-bottom: 16px;
  line-height: 1.6;
}

.slider-modal-tip b {
  color: #ff2442;
  font-weight: 500;
  word-break: break-all;
}

body[data-theme='dark'] .slider-modal-tip {
  color: #a6a6a6;
}

body[data-theme='dark'] .slider-modal-tip b {
  color: #ff4d6f;
}

.slider-modal :deep(.ant-modal-header) {
  margin-bottom: 12px;
}

body[data-theme='dark'] .slider-modal :deep(.ant-modal-content) {
  background: #1f1f1f;
}

body[data-theme='dark'] .slider-modal :deep(.ant-modal-header) {
  background: transparent;
}

body[data-theme='dark'] .slider-modal :deep(.ant-modal-title) {
  color: #e0e0e0;
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

body[data-theme='dark'] .form-subtitle {
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

/* ========== 媒体查询：手机端 ≤768px ========== */
@media (max-width: 768px) {
  .login-card {
    width: calc(100% - 32px);
    padding: 24px 20px;
    margin-top: 56px;
  }
}
</style>
