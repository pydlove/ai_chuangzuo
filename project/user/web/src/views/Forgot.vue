<template>
  <div class="forgot-page">
    <!-- 背景装饰 -->
    <div class="forgot-bg">
      <div class="bg-circle bg-circle-1"></div>
      <div class="bg-circle bg-circle-2"></div>
    </div>

    <!-- 导航栏 -->
    <header class="forgot-nav">
      <div class="nav-brand" @click="$router.push('/')">
        <img
          src="https://foruda.gitee.com/images/1782805324201637771/ee4f5810_8060302.png"
          alt="爱创作"
          class="nav-logo"
        />
        <span class="nav-brand-name">爱创作</span>
      </div>
    </header>

    <!-- 重置密码卡片 -->
    <div class="forgot-card">
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
            @click="sendCode"
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
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const form = reactive({
  email: '',
  code: '',
  password: '',
  confirmPassword: ''
})

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

const handleReset = () => {
  // TODO: 调用重置密码接口
  console.log('重置密码', form)
}
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

/* 导航栏 */
.forgot-nav {
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
</style>
