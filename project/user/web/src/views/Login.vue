<template>
  <div class="login-page">
    <!-- 背景装饰 -->
    <div class="login-bg">
      <div class="bg-circle bg-circle-1"></div>
      <div class="bg-circle bg-circle-2"></div>
    </div>

    <!-- 导航栏 -->
    <header class="login-nav">
      <div class="nav-brand" @click="$router.push('/')">
        <img
          src="https://foruda.gitee.com/images/1782805324201637771/ee4f5810_8060302.png"
          alt="爱创作"
          class="nav-logo"
        />
        <span class="nav-brand-name">爱创作</span>
      </div>
    </header>

    <!-- 登录卡片 -->
    <div class="login-card">
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

        <div class="form-item">
          <label class="form-label">图形验证码</label>
          <div class="captcha-row">
            <input
              v-model="loginForm.captcha"
              type="text"
              class="form-input captcha-input"
              placeholder="输入验证码"
            />
            <div class="captcha-box" @click="refreshCaptcha">{{ captchaText }}</div>
          </div>
        </div>

        <button class="submit-btn" @click="handleLogin">登录</button>

        <div class="form-footer">
          <span class="forgot-link" @click="$router.push('/forgot')">忘记密码？</span>
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
            />
            <button
              class="code-btn"
              :disabled="codeCountdown > 0"
              @click="sendCode"
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
          message="你收到了好友的邀请，注册并完成邮箱验证后可获得 5 个创作币。"
          type="success"
          show-icon
          class="invite-banner"
        />

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

        <button class="submit-btn" @click="handleRegister">注册</button>
      </div>
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
import { getInviteCode, getRefFromUrl, getStoredRef, setStoredRef, awardNewUserCoins } from '@/composables/useInviteCode'

const router = useRouter()

const activeTab = ref('login')
const showInviteBanner = ref(false)

const loginForm = reactive({
  email: '',
  password: '',
  captcha: ''
})

const registerForm = reactive({
  email: '',
  code: '',
  password: '',
  confirmPassword: '',
  inviteCode: ''
})

// 验证码
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

// 验证码倒计时
const codeCountdown = ref(0)
let countdownTimer = null

const sendCode = () => {
  if (codeCountdown.value > 0) return
  codeCountdown.value = 60
  countdownTimer = setInterval(() => {
    codeCountdown.value--
    if (codeCountdown.value <= 0) {
      clearInterval(countdownTimer)
    }
  }, 1000)
}

const handleLogin = () => {
  // TODO: 调用登录接口
  console.log('登录', loginForm)
  // 模拟登录成功，跳转控制台
  router.push('/console')
}

const handleRegister = async () => {
  const inviteCode = registerForm.inviteCode.trim().toUpperCase()

  // 1. 自邀请校验
  const selfCode = getInviteCode()
  if (inviteCode && inviteCode === selfCode) {
    message.warning('不能填写自己的邀请码')
    return
  }

  // 2. 输入框是唯一真值；空字符串显式清除残留 ref
  setStoredRef(inviteCode)

  // TODO: 调用注册接口
  console.log('注册', registerForm)

  // 3. 注册成功后发放创作币并提示
  const coins = awardNewUserCoins()
  if (coins > 0) {
    message.success(`注册成功，邀请奖励 +${coins} 创作币`)
  }

  router.push('/console')
}

// 初始化验证码
generateCaptcha()

onMounted(() => {
  const ref = getRefFromUrl()
  if (ref) {
    setStoredRef(ref)
    registerForm.inviteCode = ref
    showInviteBanner.value = true
    // 带邀请链接访问时默认展示注册 tab
    activeTab.value = 'register'
  } else if (getStoredRef()) {
    // localStorage 残留 ref（用户刷新页面），banner 仍显示
    showInviteBanner.value = true
    activeTab.value = 'register'
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

/* 导航栏 */
.login-nav {
  width: 100%;
  padding: 14px 48px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  position: relative;
  z-index: 1;
  box-shadow: 0 1px 0 rgba(0, 0, 0, 0.05);
}

.nav-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
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
  color: #FF2442;
  letter-spacing: 4px;
  cursor: pointer;
  user-select: none;
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

/* 表单底部 */
.form-footer {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}

.forgot-link {
  font-size: 13px;
  color: #595959;
  cursor: pointer;
  transition: color 0.2s;
}

.forgot-link:hover {
  color: #FF2442;
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
</style>
