<template>
  <div class="wechat-bind-external">
    <div class="wechat-bind-external__header">
      <h1>绑定公众号</h1>
    </div>

    <div class="wechat-bind-external__content">
      <!-- 已绑定 -->
      <div v-if="bound" class="wechat-bind-state wechat-bind-success">
        <div class="wechat-bind-icon wechat-bind-icon--success"></div>
        <h2>已绑定公众号</h2>
        <p v-if="nickname">微信昵称：{{ nickname }}</p>
        <p class="wechat-bind-hint">后续可在微信内接收服务提醒</p>
      </div>

      <!-- 加载中 -->
      <div v-else-if="loading" class="wechat-bind-state">
        <div class="wechat-bind-spinner"></div>
        <p>正在生成绑定码...</p>
      </div>

      <!-- 绑定码 -->
      <div v-else-if="bindCode" class="wechat-bind-code-section">
        <p class="wechat-bind-tip">请按以下步骤完成绑定</p>

        <div class="wechat-bind-code-card">
          <span class="wechat-bind-code-label">你的绑定码</span>
          <span class="wechat-bind-code-value">{{ bindCode }}</span>
          <button class="wechat-bind-copy" @click="copyCode">
            {{ copied ? '已复制' : '复制' }}
          </button>
        </div>

        <ol class="wechat-bind-steps">
          <li>关注「爱创作工坊」公众号</li>
          <li>在公众号对话框粘贴并发送：<strong>绑定 {{ bindCode }}</strong></li>
          <li>收到「绑定成功」提示即可</li>
        </ol>

        <p class="wechat-bind-hint">绑定码 {{ remainingSeconds }} 秒后过期</p>
        <button class="wechat-bind-refresh" @click="loadBindCode">重新生成</button>
      </div>

      <!-- 错误 -->
      <div v-else-if="error" class="wechat-bind-state wechat-bind-error">
        <div class="wechat-bind-icon wechat-bind-icon--error"></div>
        <h2>生成失败</h2>
        <p>{{ error }}</p>
        <button class="wechat-bind-refresh" @click="loadBindCode">重新生成</button>
      </div>

      <!-- 未登录提示 -->
      <div v-else-if="!hasToken" class="wechat-bind-state">
        <p>请先登录后再绑定公众号</p>
        <button class="wechat-bind-refresh" @click="goLogin">去登录</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, computed } from 'vue'
import { useRouter } from 'vue-router'
import { STORAGE_KEYS } from '@/constants/storage.js'
import { generateWechatBindCode, getWechatBindStatus } from '@/api/wechat.js'

const router = useRouter()

const loading = ref(false)
const error = ref('')
const bindCode = ref('')
const expireTime = ref(0)
const bound = ref(false)
const nickname = ref('')
const avatarUrl = ref('')
const copied = ref(false)
const hasToken = ref(!!localStorage.getItem(STORAGE_KEYS.ACCESS_TOKEN))

let pollTimer = null
let countdownTimer = null
const POLL_INTERVAL = 2000

const remainingSeconds = computed(() => {
  if (!expireTime.value) return 0
  const ms = expireTime.value - Date.now()
  return Math.max(0, Math.ceil(ms / 1000))
})

const goLogin = () => {
  router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
}

const copyCode = async () => {
  try {
    await navigator.clipboard.writeText(`绑定 ${bindCode.value}`)
    copied.value = true
    setTimeout(() => {
      copied.value = false
    }, 2000)
  } catch (e) {
    // ignore
  }
}

const loadBindCode = async () => {
  stopPolling()
  stopCountdown()
  loading.value = true
  error.value = ''
  bindCode.value = ''
  try {
    const res = await generateWechatBindCode()
    if (res.code !== 0) {
      error.value = res.message || '生成绑定码失败'
      return
    }
    bindCode.value = res.data.bindCode
    expireTime.value = res.data.expireTime
    startPolling()
    startCountdown()
  } catch (e) {
    error.value = '生成绑定码失败，请重试'
  } finally {
    loading.value = false
  }
}

const checkInitialStatus = async () => {
  try {
    const res = await getWechatBindStatus()
    if (res.code === 0 && res.data.bound) {
      bound.value = true
      nickname.value = res.data.nickname || ''
      avatarUrl.value = res.data.avatarUrl || ''
    }
  } catch (e) {
    // ignore
  }
}

const startPolling = () => {
  stopPolling()
  pollTimer = setInterval(async () => {
    try {
      const res = await getWechatBindStatus()
      if (res.code !== 0) return
      if (res.data.bound) {
        bound.value = true
        nickname.value = res.data.nickname || ''
        avatarUrl.value = res.data.avatarUrl || ''
        stopPolling()
        stopCountdown()
      }
    } catch (e) {
      // ignore
    }
  }, POLL_INTERVAL)
}

const stopPolling = () => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

const startCountdown = () => {
  stopCountdown()
  countdownTimer = setInterval(() => {
    if (remainingSeconds.value <= 0) {
      stopPolling()
      stopCountdown()
      error.value = '绑定码已过期，请重新生成'
      bindCode.value = ''
    }
  }, 1000)
}

const stopCountdown = () => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
}

onMounted(async () => {
  if (!hasToken.value) {
    return
  }
  await checkInitialStatus()
  if (!bound.value) {
    await loadBindCode()
  }
})

onBeforeUnmount(() => {
  stopPolling()
  stopCountdown()
})
</script>

<style scoped>
.wechat-bind-external {
  min-height: 100vh;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 32px 16px;
}

.wechat-bind-external__header {
  text-align: center;
  margin-bottom: 16px;
}

.wechat-bind-external__header h1 {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0;
}

.wechat-bind-external__content {
  width: 100%;
  max-width: 400px;
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
}

.wechat-bind-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 32px 16px;
  text-align: center;
}

.wechat-bind-state h2 {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0;
}

.wechat-bind-state p {
  font-size: 14px;
  color: #595959;
  margin: 0;
}

.wechat-bind-icon {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background-size: 32px 32px;
  background-position: center;
  background-repeat: no-repeat;
}

.wechat-bind-icon--success {
  background-color: #e6f7ed;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%2307c160' stroke-width='3' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='20 6 9 17 4 12'%3E%3C/polyline%3E%3C/svg%3E");
}

.wechat-bind-icon--error {
  background-color: #fff0f1;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23ff2442' stroke-width='3' stroke-linecap='round' stroke-linejoin='round'%3E%3Cline x1='18' y1='6' x2='6' y2='18'%3E%3C/line%3E%3Cline x1='6' y1='6' x2='18' y2='18'%3E%3C/line%3E%3C/svg%3E");
}

.wechat-bind-spinner {
  width: 48px;
  height: 48px;
  border: 3px solid #f0f0f0;
  border-top-color: #ff2442;
  border-radius: 50%;
  animation: wechat-bind-spin 1s linear infinite;
}

@keyframes wechat-bind-spin {
  to {
    transform: rotate(360deg);
  }
}

.wechat-bind-code-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  text-align: center;
}

.wechat-bind-tip {
  font-size: 15px;
  font-weight: 500;
  color: #1a1a1a;
  margin: 0;
}

.wechat-bind-code-card {
  width: 100%;
  background: #fff0f2;
  border: 1px dashed #ffccd5;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.wechat-bind-code-label {
  font-size: 13px;
  color: #595959;
}

.wechat-bind-code-value {
  font-size: 32px;
  font-weight: 700;
  color: #ff2442;
  letter-spacing: 4px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.wechat-bind-copy {
  padding: 6px 16px;
  border-radius: 16px;
  border: none;
  background: #ff2442;
  color: #fff;
  font-size: 13px;
  cursor: pointer;
}

.wechat-bind-copy:active {
  opacity: 0.9;
}

.wechat-bind-steps {
  width: 100%;
  text-align: left;
  font-size: 14px;
  color: #595959;
  line-height: 1.8;
  padding-left: 20px;
  margin: 0;
}

.wechat-bind-hint {
  font-size: 12px;
  color: #8c8c8c;
  margin: 0;
}

.wechat-bind-refresh {
  padding: 10px 24px;
  border-radius: 20px;
  border: none;
  background: #ff2442;
  color: #fff;
  font-size: 14px;
  cursor: pointer;
}

.wechat-bind-refresh:active {
  opacity: 0.9;
}
</style>
