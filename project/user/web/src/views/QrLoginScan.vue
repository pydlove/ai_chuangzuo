<template>
  <div class="qr-login-scan-page">
    <div class="qr-scan-card">
      <div class="qr-scan-status">
        <!-- 加载中 -->
        <div v-if="loading" class="qr-scan-state">
          <div class="qr-scan-spinner"></div>
          <p>正在处理...</p>
        </div>

        <!-- 待扫描/处理中 -->
        <div v-else-if="status === STATUS.PENDING || status === STATUS.SCANNED" class="qr-scan-state">
          <div class="qr-scan-icon qr-scan-success"></div>
          <h2>扫描成功</h2>
          <p>请在电脑端点击「确定授权」完成登录</p>
          <button class="qr-scan-cancel" @click="handleCancel">取消授权</button>
        </div>

        <!-- 已授权 -->
        <div v-else-if="status === STATUS.AUTHORIZED" class="qr-scan-state">
          <div class="qr-scan-icon qr-scan-success"></div>
          <h2>授权成功</h2>
          <p>电脑端已登录，可以关闭此页面</p>
        </div>

        <!-- 已取消 -->
        <div v-else-if="status === STATUS.CANCELLED" class="qr-scan-state">
          <div class="qr-scan-icon qr-scan-error"></div>
          <h2>已取消授权</h2>
          <p>您已取消本次登录授权</p>
        </div>

        <!-- 已过期 -->
        <div v-else-if="status === STATUS.EXPIRED" class="qr-scan-state">
          <div class="qr-scan-icon qr-scan-error"></div>
          <h2>二维码已过期</h2>
          <p>请返回电脑端刷新二维码后重新扫描</p>
        </div>

        <!-- 错误 -->
        <div v-else-if="error" class="qr-scan-state">
          <div class="qr-scan-icon qr-scan-error"></div>
          <h2>处理失败</h2>
          <p>{{ error }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { scanQrLogin, getQrLoginStatus, cancelQrLogin } from '@/api/qrLogin.js'
import { STORAGE_KEYS } from '@/constants/storage.js'

const STATUS = {
  PENDING: 0,
  SCANNED: 1,
  AUTHORIZED: 2,
  CANCELLED: 3,
  EXPIRED: 4,
}

const route = useRoute()
const router = useRouter()

const qrCode = ref('')
const status = ref(STATUS.PENDING)
const loading = ref(false)
const error = ref('')

let pollTimer = null
let pollErrorCount = 0
const MAX_POLL_ERRORS = 5
let isMounted = true

const startPolling = () => {
  stopPolling()
  pollErrorCount = 0
  pollTimer = setInterval(async () => {
    if (!qrCode.value || !isMounted) return
    try {
      const res = await getQrLoginStatus(qrCode.value)
      if (!isMounted) return
      if (res.code !== 0) return
      pollErrorCount = 0
      status.value = res.data.status
      if (status.value === STATUS.AUTHORIZED || status.value === STATUS.CANCELLED || status.value === STATUS.EXPIRED) {
        stopPolling()
      }
    } catch (e) {
      pollErrorCount++
      if (pollErrorCount > MAX_POLL_ERRORS) {
        stopPolling()
        error.value = '轮询异常，请刷新页面重试'
      }
    }
  }, 2000)
}

const stopPolling = () => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

const handleScan = async () => {
  const token = localStorage.getItem(STORAGE_KEYS.ACCESS_TOKEN)
  if (!token) {
    router.push(`/login?redirect=${encodeURIComponent(window.location.pathname)}`)
    return
  }

  loading.value = true
  try {
    const res = await scanQrLogin(qrCode.value)
    if (res.code !== 0) {
      error.value = res.message || '扫描失败'
      return
    }
    status.value = res.data.status
    startPolling()
  } catch (e) {
    error.value = '扫描失败，请重试'
  } finally {
    loading.value = false
  }
}

const handleCancel = async () => {
  try {
    await cancelQrLogin(qrCode.value)
    status.value = STATUS.CANCELLED
    stopPolling()
  } catch (e) {
    // ignore
  }
}

onMounted(() => {
  qrCode.value = route.params.qrCode
  if (!qrCode.value) {
    error.value = '无效的二维码'
    return
  }
  handleScan()
})

onBeforeUnmount(() => {
  isMounted = false
  stopPolling()
})
</script>

<style scoped>
.qr-login-scan-page {
  min-height: 100vh;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.qr-scan-card {
  width: 100%;
  max-width: 360px;
  background: #fff;
  border-radius: 16px;
  padding: 40px 24px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06);
  text-align: center;
}

.qr-scan-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.qr-scan-spinner {
  width: 48px;
  height: 48px;
  border: 3px solid #f0f0f0;
  border-top-color: #07c160;
  border-radius: 50%;
  animation: qr-spin 1s linear infinite;
}

@keyframes qr-spin {
  to {
    transform: rotate(360deg);
  }
}

.qr-scan-icon {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background-size: 32px 32px;
  background-position: center;
  background-repeat: no-repeat;
}

.qr-scan-success {
  background-color: #e6f7ed;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%2307c160' stroke-width='3' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='20 6 9 17 4 12'%3E%3C/polyline%3E%3C/svg%3E");
}

.qr-scan-error {
  background-color: #fff0f1;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23ff2442' stroke-width='3' stroke-linecap='round' stroke-linejoin='round'%3E%3Cline x1='18' y1='6' x2='6' y2='18'%3E%3C/line%3E%3Cline x1='6' y1='6' x2='18' y2='18'%3E%3C/line%3E%3C/svg%3E");
}

.qr-scan-state h2 {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0;
}

.qr-scan-state p {
  font-size: 14px;
  color: #595959;
  margin: 0;
  line-height: 1.6;
}

.qr-scan-cancel {
  margin-top: 8px;
  padding: 10px 24px;
  border-radius: 20px;
  border: 1px solid #e0e0e0;
  background: #fff;
  color: #595959;
  font-size: 14px;
  cursor: pointer;
}

@media (max-width: 768px) {
  .qr-login-scan-page {
    background: #fff;
    padding: 16px;
  }

  .qr-scan-card {
    box-shadow: none;
    padding: 24px 16px;
  }
}
</style>
