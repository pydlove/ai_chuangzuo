import { ref, onMounted } from 'vue'

function isMobileDevice() {
  return /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent)
}

function isWechatBrowser() {
  return /MicroMessenger/i.test(navigator.userAgent)
}

/**
 * 尝试检测是否处于浏览器的无痕/隐私模式。
 *
 * <p>说明：不同浏览器对无痕模式的实现差异很大，没有 100% 可靠的检测方式。
 * 这里用 localStorage 写入试捕：iOS Safari 等部分浏览器在无痕模式下会立即抛 QuotaExceededError。
 * Chrome Android 无痕模式在当前会话中 localStorage 表现正常，无法被此方法检出。</p>
 *
 * @returns {Promise<boolean>} 是否可能是隐私模式
 */
async function detectPrivateMode() {
  try {
    const key = '__aichuangzuo_private_mode_test__'
    localStorage.setItem(key, '1')
    localStorage.removeItem(key)
    return false
  } catch {
    return true
  }
}

/**
 * 登录页顶部提示：手机普通浏览器（非微信）可能开启无痕模式时显示。
 *
 * @returns {{ showTip: import('vue').Ref<boolean>, closeTip: () => void }}
 */
export function usePrivateModeTip() {
  const showTip = ref(false)

  onMounted(async () => {
    if (!isMobileDevice() || isWechatBrowser()) {
      return
    }
    const isPrivate = await detectPrivateMode()
    showTip.value = isPrivate
  })

  function closeTip() {
    showTip.value = false
  }

  return {
    showTip,
    closeTip
  }
}
