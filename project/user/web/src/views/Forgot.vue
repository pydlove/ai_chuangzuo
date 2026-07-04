<template>
  <div class="forgot-page">
    <!-- 背景装饰 -->
    <div class="forgot-bg">
      <div class="bg-circle bg-circle-1"></div>
      <div class="bg-circle bg-circle-2"></div>
    </div>

    <!-- 导航栏 -->
    <NavBar :links="navLinks" :cta-to="ctaTo" :cta-label="ctaLabel" />

    <!-- 重置密码卡片 -->
    <div ref="cardRef" class="forgot-card">
      <h2 class="form-title">重置密码</h2>
      <p class="form-subtitle">验证邮箱后即可设置新密码</p>

      <div class="form-item">
        <label class="form-label">邮箱</label>
        <input
          v-model="form.email"
          type="email"
          class="form-input"
          placeholder="请输入注册邮箱"
        />
      </div>

      <div class="form-item">
        <label class="form-label">邮箱验证码</label>
        <div class="captcha-row">
          <input
            v-model="form.code"
            type="text"
            class="form-input captcha-input"
            placeholder="输入 6 位验证码"
          />
          <button
            class="code-btn"
            :disabled="codeCountdown > 0"
            @click="openCodeSlider"
          >
            {{ codeCountdown > 0 ? `${codeCountdown}s` : '获取验证码' }}
          </button>
        </div>
      </div>

      <div class="form-item">
        <label class="form-label">新密码</label>
        <input
          v-model="form.password"
          type="password"
          class="form-input"
          placeholder="6-20 位新密码"
        />
      </div>

      <div class="form-item">
        <label class="form-label">确认新密码</label>
        <input
          v-model="form.confirmPassword"
          type="password"
          class="form-input"
          placeholder="再次输入新密码"
        />
      </div>

      <button class="submit-btn" @click="handleReset">重置密码</button>

      <div class="form-footer">
        <span class="back-link" @click="$router.push('/login')">返回登录</span>
      </div>
    </div>

    <!-- 底部 -->
    <footer class="forgot-footer">
      <span>© 2026 爱创作 · 杭州爱启云网络科技有限公司 · All Rights Reserved</span>
      <span>浙ICP备XXXXXXXX号-1</span>
    </footer>

    <!-- 发送邮箱验证码前的滑块弹框 -->
    <a-modal
      v-model:open="codeModalVisible"
      title="人机验证"
      :footer="null"
      :mask-closable="false"
      :keyboard="false"
      width="420px"
      class="slider-modal slider-modal-send-code"
    >
      <p class="slider-modal-tip">
        拖动滑块完成验证后将向
        <b>{{ form.email || '当前邮箱' }}</b>
        发送 6 位邮箱验证码
      </p>
      <SliderCaptcha v-model="codeModalPassed" />
    </a-modal>

    <!-- 重置密码前的滑块弹框 -->
    <a-modal
      v-model:open="resetModalVisible"
      title="人机验证"
      :footer="null"
      :mask-closable="false"
      :keyboard="false"
      width="420px"
      class="slider-modal slider-modal-reset"
    >
      <p class="slider-modal-tip">
        拖动滑块完成验证后将重置账号
        <b v-if="form.email">「{{ form.email }}」</b>
        的密码
      </p>
      <SliderCaptcha v-model="resetModalPassed" />
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import NavBar from '@/components/layout/NavBar.vue'
import SliderCaptcha from '@/components/SliderCaptcha.vue'
import { getCaptcha, sendEmailCode, resetPassword } from '@/api/auth'

const router = useRouter()

const navLinks = [
  { to: '/', label: '首页' },
  { to: '/pricing', label: '会员' },
  { to: '/guide', label: '玩法指南' }
]
const ctaTo = '/login'
const ctaLabel = '开始创作'

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

// ---------- 主题切换由 NavBar 组件统一处理 ----------

// ---------- 人机验证常量（与 Login.vue 一致） ----------
const SLIDER_CAPTCHA_VALUE = 'TEST12'

// ---------- 后端 captcha 会话 ----------
const captchaKey = ref('')

// ---------- 发送邮箱验证码弹框状态 ----------
const codeModalVisible = ref(false)
const codeModalPassed = ref(false)
let codeModalSending = false

// ---------- 重置密码弹框状态 ----------
const resetModalVisible = ref(false)
const resetModalPassed = ref(false)
let resetModalSending = false


const form = reactive({
  email: '',
  code: '',
  password: '',
  confirmPassword: ''
})

// 验证码倒计时
const codeCountdown = ref(0)
let countdownTimer = null

const startCodeCountdown = () => {
  if (countdownTimer) clearInterval(countdownTimer)
  codeCountdown.value = 60
  countdownTimer = setInterval(() => {
    codeCountdown.value--
    if (codeCountdown.value <= 0) {
      clearInterval(countdownTimer)
      countdownTimer = null
    }
  }, 1000)
}

// === 发送邮箱验证码：弹框拖滑块 → 通过后才调 sendEmailCode ===
const openCodeSlider = async () => {
  if (codeCountdown.value > 0) return
  if (!form.email) {
    message.warning('请先填写邮箱')
    return
  }
  try {
    const res = await getCaptcha()
    captchaKey.value = res.data.captchaKey
    codeModalPassed.value = false
    codeModalVisible.value = true
  } catch (err) {
    message.error(err?.message || '验证码加载失败')
  }
}

watch(codeModalPassed, async (val) => {
  if (!val || codeModalSending) return
  codeModalSending = true
  try {
    await sendEmailCode({
      email: form.email,
      captchaKey: captchaKey.value,
      captchaCode: SLIDER_CAPTCHA_VALUE
    })
    startCodeCountdown()
    message.success('验证码已发送')
    codeModalVisible.value = false
  } catch (err) {
    message.error(err?.message || '发送失败')
    codeModalVisible.value = false
  } finally {
    codeModalSending = false
  }
})

// === 重置密码：弹框拖滑块 → 通过后才调 resetPassword API ===
const handleReset = async () => {
  if (form.password !== form.confirmPassword) {
    message.error('两次输入的密码不一致')
    return
  }
  if (!form.email || !form.code || !form.password) {
    message.warning('请完整填写表单')
    return
  }
  if (codeCountdown.value <= 0) {
    message.warning('请先获取邮箱验证码')
    return
  }
  try {
    const res = await getCaptcha()
    captchaKey.value = res.data.captchaKey
    resetModalPassed.value = false
    resetModalVisible.value = true
  } catch (err) {
    message.error(err?.message || '验证码加载失败')
  }
}

watch(resetModalPassed, async (val) => {
  if (!val || resetModalSending) return
  resetModalSending = true
  try {
    await resetPassword({
      email: form.email,
      emailCode: form.code,
      password: form.password,
      confirmPassword: form.confirmPassword,
      captchaKey: captchaKey.value,
      captchaCode: SLIDER_CAPTCHA_VALUE
    })
    message.success('密码已重置，请重新登录')
    resetModalVisible.value = false
    if (countdownTimer) {
      clearInterval(countdownTimer)
      countdownTimer = null
    }
    codeCountdown.value = 0
    router.push('/login')
  } catch (err) {
    message.error(err?.message || '重置失败')
    resetModalVisible.value = false
  } finally {
    resetModalSending = false
  }
})

onMounted(() => {
  window.addEventListener('mousemove', onPageMouseMove)
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
.forgot-page {
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
.forgot-bg {
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

/* 重置密码卡片 */
.forgot-card {
  background: rgba(255, 255, 255, 0.97);
  border-radius: 20px;
  padding: 40px;
  width: 420px;
  margin-top: 40px;
  box-shadow: 0 25px 80px rgba(0, 0, 0, 0.1);
  position: relative;
  z-index: 1;
  /* 鼠标方向律动：卡片朝鼠标方向轻微平移（最大 ±8px） */
  transform: translate(var(--mx, 0px), var(--my, 0px));
  transition: transform 0.5s cubic-bezier(0.2, 0.8, 0.2, 1),
              box-shadow 0.35s ease;
  will-change: transform;
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
  margin-top: 16px;
}

.back-link {
  font-size: 13px;
  color: #595959;
  cursor: pointer;
  transition: color 0.2s;
}

.back-link:hover {
  color: #FF2442;
}

/* 底部 */
.forgot-footer {
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

.forgot-footer span + span::before {
  content: '|';
  margin: 0 12px;
  color: #eee;
}

/* ========== 暗色主题 ========== */
body[data-theme="dark"] .forgot-page {
  background: linear-gradient(180deg, #1a1a1a 0%, #141414 100%);
}

body[data-theme="dark"] .bg-circle {
  background: rgba(255, 36, 66, 0.05);
}

body[data-theme="dark"] .forgot-card {
  background: #1f1f1f;
  box-shadow: 0 25px 80px rgba(0, 0, 0, 0.5);
}

body[data-theme="dark"] .form-title,
body[data-theme="dark"] .form-label {
  color: #e0e0e0;
}

body[data-theme="dark"] .form-subtitle,
body[data-theme="dark"] .back-link {
  color: #a6a6a6;
}

body[data-theme="dark"] .back-link:hover {
  color: #ff4d6f;
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

body[data-theme="dark"] .forgot-footer {
  background: #1f1f1f;
  border-top-color: #303030;
  color: #a6a6a6;
}

body[data-theme="dark"] .forgot-footer span + span::before {
  color: #303030;
}
</style>
