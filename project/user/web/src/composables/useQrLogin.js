import { ref, computed, onBeforeUnmount } from 'vue'
import QRCode from 'qrcode'
import {
  createQrLogin,
  getQrLoginStatus,
  authorizeQrLogin,
  cancelQrLogin,
} from '@/api/qrLogin.js'

const STATUS = {
  PENDING: 0,
  SCANNED: 1,
  AUTHORIZED: 2,
  CANCELLED: 3,
  EXPIRED: 4,
}

const POLL_INTERVAL = 2000

export function useQrLogin({ onAuthorized }) {
  const qrCode = ref('')
  const qrUrl = ref('')
  const qrDataUrl = ref('')
  const status = ref(STATUS.PENDING)
  const scannerNickname = ref('')
  const expiresIn = ref(0)
  const loading = ref(false)
  const error = ref('')

  let pollTimer = null
  let expireTimer = null

  const isPending = computed(() => status.value === STATUS.PENDING)
  const isScanned = computed(() => status.value === STATUS.SCANNED)
  const isAuthorized = computed(() => status.value === STATUS.AUTHORIZED)
  const isCancelled = computed(() => status.value === STATUS.CANCELLED)
  const isExpired = computed(() => status.value === STATUS.EXPIRED)

  const buildQrUrl = (code) => {
    const baseUrl = window.__QR_BASE_URL__ || window.location.origin
    return `${baseUrl}/qr-login/${code}`
  }

  const generateQr = async () => {
    try {
      loading.value = true
      error.value = ''
      const res = await createQrLogin()
      if (res.code !== 0) {
        error.value = res.message || '创建二维码失败'
        return
      }
      qrCode.value = res.data.qrCode
      expiresIn.value = res.data.expiresIn
      qrUrl.value = buildQrUrl(qrCode.value)
      qrDataUrl.value = await QRCode.toDataURL(qrUrl.value, {
        errorCorrectionLevel: 'H',
        margin: 1,
        width: 200,
        color: { dark: '#1a1a1a', light: '#ffffff' },
      })
      status.value = STATUS.PENDING
      scannerNickname.value = ''
      startPolling()
      startExpireCountdown()
    } catch (e) {
      error.value = '创建二维码失败，请重试'
    } finally {
      loading.value = false
    }
  }

  const refreshQr = () => {
    stopPolling()
    generateQr()
  }

  const startPolling = () => {
    stopPolling()
    pollTimer = setInterval(async () => {
      if (!qrCode.value) return
      try {
        const res = await getQrLoginStatus(qrCode.value)
        if (res.code !== 0) return
        const data = res.data
        status.value = data.status
        scannerNickname.value = data.scannerNickname || ''
        expiresIn.value = data.expiresIn || 0
        if (data.status === STATUS.AUTHORIZED || data.status === STATUS.CANCELLED || data.status === STATUS.EXPIRED) {
          stopPolling()
        }
      } catch (e) {
        // ignore polling errors
      }
    }, POLL_INTERVAL)
  }

  const startExpireCountdown = () => {
    if (expireTimer) clearInterval(expireTimer)
    expireTimer = setInterval(() => {
      if (expiresIn.value > 0) {
        expiresIn.value--
      } else if (!isAuthorized.value && !isCancelled.value) {
        status.value = STATUS.EXPIRED
        stopPolling()
      }
    }, 1000)
  }

  const stopPolling = () => {
    if (pollTimer) {
      clearInterval(pollTimer)
      pollTimer = null
    }
  }

  const stopExpireCountdown = () => {
    if (expireTimer) {
      clearInterval(expireTimer)
      expireTimer = null
    }
  }

  const authorize = async () => {
    if (!qrCode.value) return
    try {
      loading.value = true
      const res = await authorizeQrLogin(qrCode.value)
      if (res.code !== 0) {
        error.value = res.message || '授权失败'
        return
      }
      status.value = STATUS.AUTHORIZED
      stopPolling()
      stopExpireCountdown()
      if (onAuthorized) {
        onAuthorized(res.data)
      }
    } catch (e) {
      error.value = '授权失败，请重试'
    } finally {
      loading.value = false
    }
  }

  const cancel = async () => {
    if (!qrCode.value) return
    try {
      await cancelQrLogin(qrCode.value)
      status.value = STATUS.CANCELLED
    } catch (e) {
      // ignore
    } finally {
      stopPolling()
      stopExpireCountdown()
    }
  }

  const reset = () => {
    stopPolling()
    stopExpireCountdown()
    qrCode.value = ''
    qrUrl.value = ''
    qrDataUrl.value = ''
    status.value = STATUS.PENDING
    scannerNickname.value = ''
    expiresIn.value = 0
    error.value = ''
  }

  onBeforeUnmount(() => {
    stopPolling()
    stopExpireCountdown()
  })

  return {
    qrCode,
    qrUrl,
    qrDataUrl,
    status,
    scannerNickname,
    expiresIn,
    loading,
    error,
    isPending,
    isScanned,
    isAuthorized,
    isCancelled,
    isExpired,
    generateQr,
    refreshQr,
    authorize,
    cancel,
    reset,
  }
}
