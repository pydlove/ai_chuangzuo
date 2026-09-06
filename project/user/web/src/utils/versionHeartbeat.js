// 版本心跳：检测服务升级 / 新版本发布
//
// 两种模式：
// 1. 升级中：version.json 连续拉取失败 → 阻断式弹框「系统升级中，完成后自动刷新」，
//    每 5 秒探测一次，服务恢复后自动 reload（若版本已变更则必然 reload 加载新前端）。
// 2. 新版本：version.json 版本号与当前页面不一致 → 底部非阻断提示「发现新版本，点击刷新」。
//    点击后不立即 reload，先探测后端健康接口：就绪才刷新，未就绪转入升级中模式等待。
//
// dev 模式（__APP_VERSION__ 为 'dev'）不启用。

const CURRENT_VERSION = window.__APP_VERSION__
const IS_DEV = !CURRENT_VERSION || CURRENT_VERSION === 'dev'

const HEARTBEAT_INTERVAL = 60 * 1000 // 正常轮询间隔
const RECOVER_INTERVAL = 5 * 1000 // 升级中探测间隔
const FETCH_TIMEOUT = 5 * 1000
const FAIL_THRESHOLD = 2 // 连续失败次数达到才判定升级中，避免瞬时抖动误报
const API_FAILURE_DEBOUNCE = 15 * 1000 // API 失败触发即时检测的最小间隔
const HEALTH_URL = '/api/v1/public/health'

let heartbeatTimer = null
let recoverTimer = null
let failCount = 0
let upgrading = false
let newVersionNotified = false
let started = false
let lastApiFailureCheck = 0

function fetchRemoteVersion() {
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error('timeout')), FETCH_TIMEOUT)
    fetch(`/version.json?t=${Date.now()}`, { cache: 'no-store' })
      .then((res) => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`)
        return res.json()
      })
      .then((data) => {
        clearTimeout(timer)
        if (data && data.version) {
          resolve(data.version)
        } else {
          reject(new Error('bad version.json'))
        }
      })
      .catch((err) => {
        clearTimeout(timer)
        reject(err)
      })
  })
}

function checkBackendHealthy() {
  return new Promise((resolve) => {
    const timer = setTimeout(() => resolve(false), FETCH_TIMEOUT)
    fetch(`${HEALTH_URL}?t=${Date.now()}`, { cache: 'no-store' })
      .then((res) => {
        clearTimeout(timer)
        resolve(res.ok)
      })
      .catch(() => {
        clearTimeout(timer)
        resolve(false)
      })
  })
}

function stopRecover() {
  if (recoverTimer) {
    clearInterval(recoverTimer)
    recoverTimer = null
  }
}

function startHeartbeat() {
  if (heartbeatTimer) clearInterval(heartbeatTimer)
  heartbeatTimer = setInterval(check, HEARTBEAT_INTERVAL)
}

async function check() {
  try {
    const remoteVersion = await fetchRemoteVersion()
    failCount = 0
    if (upgrading) {
      // version.json 已恢复，但后端可能仍在重启（静态文件由 nginx 直接服务，
      // 不代表后端就绪），必须等健康检查通过再恢复/刷新，否则 reload 后页面报错
      if (!(await checkBackendHealthy())) return
      stopRecover()
      upgrading = false
      hideUpgradingModal()
      if (remoteVersion !== CURRENT_VERSION) {
        window.location.reload()
      } else {
        startHeartbeat()
      }
    } else if (remoteVersion !== CURRENT_VERSION && !newVersionNotified) {
      showNewVersionToast()
    }
  } catch {
    failCount++
    if (!upgrading && failCount >= FAIL_THRESHOLD) {
      enterUpgrading()
    }
  }
}

function enterUpgrading() {
  if (upgrading) return
  upgrading = true
  if (heartbeatTimer) clearInterval(heartbeatTimer)
  heartbeatTimer = null
  showUpgradingModal()
  check() // 立即探测一次，不等第一个 5 秒
  recoverTimer = setInterval(check, RECOVER_INTERVAL)
}

// API 层上报：网络错误或网关 5xx 时加速进入升级检测
// 单个超时（慢网）不触发，避免误报
export function reportApiFailure(error) {
  if (IS_DEV || upgrading || newVersionNotified) return
  const status = error?.response?.status
  const isGatewayError = status === 502 || status === 503 || status === 504
  const isNetworkError = !error?.response && error?.code !== 'ECONNABORTED'
  if (!isGatewayError && !isNetworkError) return

  const now = Date.now()
  if (now - lastApiFailureCheck < API_FAILURE_DEBOUNCE) return
  lastApiFailureCheck = now
  failCount = FAIL_THRESHOLD - 1 // 一次即时探测失败即进入升级中
  check()
}

// ---------- UI ----------

const CSS = `
.vhb-mask {
  position: fixed; inset: 0; z-index: 10000;
  background: rgba(0, 0, 0, 0.45);
  display: flex; align-items: center; justify-content: center;
}
.vhb-card {
  background: #fff; border-radius: 16px; padding: 40px 48px;
  display: flex; flex-direction: column; align-items: center;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.12);
  max-width: 320px;
}
.vhb-spinner {
  width: 40px; height: 40px; border-radius: 50%;
  border: 3px solid #ffe1e6; border-top-color: #FF2442;
  animation: vhb-spin 0.9s linear infinite;
}
@keyframes vhb-spin { to { transform: rotate(360deg); } }
.vhb-title { margin-top: 20px; font-size: 17px; font-weight: 600; color: rgba(0, 0, 0, 0.88); }
.vhb-desc { margin-top: 10px; font-size: 13px; line-height: 1.7; color: rgba(0, 0, 0, 0.55); text-align: center; }
.vhb-toast {
  position: fixed; left: 50%; bottom: 64px; transform: translateX(-50%);
  z-index: 10000; cursor: pointer; user-select: none;
  background: #fff; border-radius: 999px; padding: 10px 20px;
  display: flex; align-items: center; gap: 8px;
  font-size: 13px; color: rgba(0, 0, 0, 0.88);
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.14);
  border: 1px solid #ffe1e6;
}
.vhb-toast:hover { border-color: #FF2442; }
.vhb-dot { width: 8px; height: 8px; border-radius: 50%; background: #FF2442; }
`

function injectStyles() {
  if (document.getElementById('vhb-styles')) return
  const style = document.createElement('style')
  style.id = 'vhb-styles'
  style.textContent = CSS
  document.head.appendChild(style)
}

function showUpgradingModal() {
  injectStyles()
  hideUpgradingModal()
  const mask = document.createElement('div')
  mask.className = 'vhb-mask'
  mask.id = 'vhb-upgrading-modal'
  mask.innerHTML = `
    <div class="vhb-card">
      <div class="vhb-spinner"></div>
      <div class="vhb-title">系统升级中</div>
      <div class="vhb-desc">我们正在升级服务，完成后页面将自动刷新，请稍候…</div>
    </div>
  `
  document.body.appendChild(mask)
}

function hideUpgradingModal() {
  document.getElementById('vhb-upgrading-modal')?.remove()
}

function showNewVersionToast() {
  newVersionNotified = true
  injectStyles()
  document.getElementById('vhb-new-version-toast')?.remove()
  const toast = document.createElement('div')
  toast.className = 'vhb-toast'
  toast.id = 'vhb-new-version-toast'
  toast.innerHTML = `<span class="vhb-dot"></span><span>发现新版本，点击刷新</span>`
  toast.addEventListener('click', onToastClick)
  document.body.appendChild(toast)
}

function hideNewVersionToast() {
  document.getElementById('vhb-new-version-toast')?.remove()
}

// 点击刷新：先确认后端就绪。新版本静态文件由 nginx 直接服务，
// 提示出现时后端可能仍在重启，此时 reload 会撞上报错页面，
// 因此未就绪就转入升级中弹框等待，就绪后自动刷新。
async function onToastClick() {
  if (await checkBackendHealthy()) {
    window.location.reload()
    return
  }
  hideNewVersionToast()
  enterUpgrading()
}

export function startVersionHeartbeat() {
  if (IS_DEV || started) return
  started = true
  startHeartbeat()
  // 切回页面时立即检查一次
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible') check()
  })
  // 构建产物 chunk 被替换后，旧页面懒加载 404 → 提示刷新
  window.addEventListener('vite:preloadError', () => showNewVersionToast())
}
