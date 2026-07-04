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
          src="https://foruda.gitee.com/images/1782986808430461164/e0ab39dc_8060302.png"
          alt="爱创作"
          class="nav-logo"
        />
        <span class="nav-brand-name">爱创作</span>
      </div>
      <div class="nav-links">
        <router-link to="/" class="nav-link">首页</router-link>
        <router-link to="/pricing" class="nav-link">会员</router-link>
        <router-link to="/guide" class="nav-link">玩法指南</router-link>
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
    </div>
    </header>

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
                <b>5 个创作币</b>
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

        <button class="submit-btn" @click="handleRegister">注册</button>
      </div>
    </div>

    <!-- 底部 -->
    <footer class="login-footer">
      <span>© 2026 爱创作 · 杭州爱启云网络科技有限公司 · All Rights Reserved</span>
      <span>浙ICP备XXXXXXXX号-1</span>
    </footer>

    <!-- 注册流程：发送邮箱验证码前的滑块弹框 -->
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
        拖动滑块完成验证后将向
        <b>{{ registerForm.email || '当前邮箱' }}</b>
        发送 6 位邮箱验证码
      </p>
      <SliderCaptcha v-model="sliderModalPassed" />
    </a-modal>

    <!-- 登录流程：调用后端登录接口前的滑块弹框 -->
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
        拖动滑块完成验证后将登录账号
        <b v-if="loginForm.email">「{{ loginForm.email }}」</b>
      </p>
      <SliderCaptcha v-model="loginModalPassed" />
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import CoinInfoTooltip from '@/components/CoinInfoTooltip.vue'
import SliderCaptcha from '@/components/SliderCaptcha.vue'
import { getInviteCode, getRefFromUrl, getStoredRef, setStoredRef, awardNewUserCoins } from '@/composables/useInviteCode'
import { getCaptcha, sendEmailCode, register as registerApi, login as loginApi } from '@/api/auth'

const router = useRouter()

// ---------- 鼠标方向律动：卡片轻微朝鼠标方向平移 ----------
// 在 window 上监听 mousemove，根据鼠标相对卡片中心的距离，
// 把卡片朝鼠标方向 translate 一个像素量（最大 ±8px）。
// transition 让移动有一点"律动"延迟感。
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
  // 归一化到 [-1, 1]：鼠标越偏离屏幕中心，偏移越接近最大值
  const nx = Math.max(-1, Math.min(1, dx / (window.innerWidth / 2)))
  const ny = Math.max(-1, Math.min(1, dy / (window.innerHeight / 2)))
  card.style.setProperty('--mx', `${nx * MAGNET_OFFSET_PX}px`)
  card.style.setProperty('--my', `${ny * MAGNET_OFFSET_PX}px`)
}

// ---------- 主题切换 ----------
const THEME_KEY = 'aichuangzuo_theme'
const currentTheme = ref('light')

const toggleTheme = () => {
  const next = currentTheme.value === 'light' ? 'dark' : 'light'
  currentTheme.value = next
  document.body.setAttribute('data-theme', next)
  localStorage.setItem(THEME_KEY, next)
}

const loadTheme = () => {
  const saved = localStorage.getItem(THEME_KEY) || 'light'
  currentTheme.value = saved
  document.body.setAttribute('data-theme', saved)
}

const activeTab = ref('login')
const showInviteBanner = ref(false)

const loginForm = reactive({
  email: '',
  password: ''
})

const registerForm = reactive({
  email: '',
  code: '',
  password: '',
  confirmPassword: '',
  inviteCode: ''
})

// ---------- 后端 captcha 会话 ----------
// 滑块是纯前端 UX mock；后端 captcha 接口仍调用以拿到 captchaKey
// （限流/会话锚点），但前端不再展示图形码图片。
const captchaKey = ref('')

// 滑块拖到末端后，前端把该值写入 form.captcha 随接口发出
// dev/test profile 后端 captcha mock 模式固定返回该值
const SLIDER_CAPTCHA_VALUE = 'TEST12'

// 注册流程：滑块弹框状态
const sliderModalVisible = ref(false)
const sliderModalPassed = ref(false)
let modalSending = false  // 防止 watch 在 close → reset 路径上重复触发

// 登录流程：滑块弹框状态
const loginSliderModalVisible = ref(false)
const loginModalPassed = ref(false)
let loginModalSending = false

// 注册弹框内滑块通过 → 调发送邮箱验证码接口
watch(sliderModalPassed, async (val) => {
  if (!val || modalSending) return
  modalSending = true
  try {
    await sendEmailCode({
      email: registerForm.email,
      captchaKey: captchaKey.value,
      captchaCode: SLIDER_CAPTCHA_VALUE
    })
    startCodeCountdown()
    message.success('验证码已发送')
    sliderModalVisible.value = false
    loadCaptcha()  // 重新拿 captchaKey 给下次发送
  } catch (err) {
    message.error(err?.message || '发送失败')
    sliderModalVisible.value = false
  } finally {
    modalSending = false
  }
})

// 登录弹框内滑块通过 → 调后端登录接口
watch(loginModalPassed, async (val) => {
  if (!val || loginModalSending) return
  loginModalSending = true
  try {
    const res = await loginApi({
      email: loginForm.email,
      password: loginForm.password,
      captchaKey: captchaKey.value,
      captchaCode: SLIDER_CAPTCHA_VALUE
    })
    persistTokens(res.data)
    message.success('登录成功')
    loginSliderModalVisible.value = false
    router.push('/console')
  } catch (err) {
    message.error(err?.message || '登录失败')
    loginSliderModalVisible.value = false
    loadCaptcha()
  } finally {
    loginModalSending = false
  }
})

const loadCaptcha = async () => {
  try {
    const res = await getCaptcha()
    captchaKey.value = res.data.captchaKey
  } catch (err) {
    message.error(err?.message || '验证码加载失败')
  }
}

// 打开注册滑块弹框前先取一个 captchaKey（限流锚点）
const openSliderModal = async () => {
  if (codeCountdown.value > 0) return
  if (!registerForm.email) {
    message.warning('请先填写邮箱')
    return
  }
  try {
    const res = await getCaptcha()
    captchaKey.value = res.data.captchaKey
    sliderModalPassed.value = false
    sliderModalVisible.value = true
  } catch (err) {
    message.error(err?.message || '验证码加载失败')
  }
}

// 打开登录滑块弹框：先校验邮箱/密码，再取 captchaKey
const openLoginSliderModal = async () => {
  if (!loginForm.email) {
    message.warning('请填写邮箱')
    return
  }
  if (!loginForm.password) {
    message.warning('请填写密码')
    return
  }
  try {
    const res = await getCaptcha()
    captchaKey.value = res.data.captchaKey
    loginModalPassed.value = false
    loginSliderModalVisible.value = true
  } catch (err) {
    message.error(err?.message || '验证码加载失败')
  }
}

// ---------- 邮箱验证码倒计时 ----------
const codeCountdown = ref(0)
let countdownTimer = null

const startCodeCountdown = () => {
  codeCountdown.value = 60
  if (countdownTimer) clearInterval(countdownTimer)
  countdownTimer = setInterval(() => {
    codeCountdown.value--
    if (codeCountdown.value <= 0) {
      clearInterval(countdownTimer)
      countdownTimer = null
    }
  }, 1000)
}

const persistTokens = (data) => {
  localStorage.setItem('aichuangzuo_access_token', data.accessToken)
  localStorage.setItem('aichuangzuo_refresh_token', data.refreshToken)
}

const handleLogin = async () => {
  await openLoginSliderModal()
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

  try {
    const res = await registerApi({
      email: registerForm.email,
      emailCode: registerForm.code,
      password: registerForm.password,
      confirmPassword: registerForm.confirmPassword,
      inviteCode: inviteCode || undefined
    })
    persistTokens(res.data)

    // 3. 注册成功后发放创作币并提示
    const coins = awardNewUserCoins()
    if (coins > 0) {
      message.success(`注册成功，邀请奖励 +${coins} 创作币`)
    } else {
      message.success('注册成功')
    }

    router.push('/console')
  } catch (err) {
    message.error(err?.message || '注册失败')
    loadCaptcha()
  }
}

onMounted(() => {
  loadTheme()
  loadCaptcha()
  window.addEventListener('mousemove', onPageMouseMove)
  const ref = getRefFromUrl()
  if (ref) {
    setStoredRef(ref)
    registerForm.inviteCode = ref
    showInviteBanner.value = true
    activeTab.value = 'register'
  } else if (getStoredRef()) {
    showInviteBanner.value = true
    activeTab.value = 'register'
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('mousemove', onPageMouseMove)
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
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
  display: flex;
  align-items: center;
  justify-content: space-between;
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
  background: #FFF5F7;
  color: #FF2442;
}

.theme-toggle svg {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 24px;
}

.nav-link {
  font-size: 14px;
  color: #262626;
  cursor: pointer;
  transition: color 0.2s;
}

.nav-link:hover,
.nav-link.active {
  color: #FF2442;
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

/* ========== 暗色主题 ========== */
body[data-theme="dark"] .login-page {
  background: linear-gradient(180deg, #1a1a1a 0%, #141414 100%);
}

body[data-theme="dark"] .bg-circle {
  background: rgba(255, 36, 66, 0.05);
}

body[data-theme="dark"] .login-nav {
  background: rgba(31, 31, 31, 0.9);
  box-shadow: 0 1px 0 rgba(255, 255, 255, 0.05);
}

body[data-theme="dark"] .nav-link {
  color: #a6a6a6;
}

body[data-theme="dark"] .nav-link:hover,
body[data-theme="dark"] .nav-link.active {
  color: #ff4d6f;
}

body[data-theme="dark"] .nav-brand-name {
  color: #e0e0e0;
}

body[data-theme="dark"] .theme-toggle {
  color: #a6a6a6;
}

body[data-theme="dark"] .theme-toggle:hover {
  background: rgba(255, 36, 66, 0.15);
  color: #ff4d6f;
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
