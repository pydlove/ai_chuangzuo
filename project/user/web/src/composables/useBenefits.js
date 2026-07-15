import { ref } from 'vue'
import { getMyBenefits } from '@/api/benefit'

// 模块级 ref：单例模式，整个 console 共享一份权益数据（同 useUserProfile）。
const benefits = ref({})
const planKey = ref('free')
const planName = ref('免费版')
const expiresAt = ref('')
const loaded = ref(false)

/**
 * 会员权益 composable。
 * 登录后调用 loadBenefits() 加载；页面用 hasBenefit/benefitValue/benefitRemaining 控制功能。
 */
export function useBenefits() {

  async function loadBenefits() {
    try {
      const res = await getMyBenefits()
      const data = res.data || res
      planKey.value = data.planKey || 'free'
      planName.value = data.planName || '免费版'
      expiresAt.value = data.expiresAt || ''
      const map = {}
      for (const item of data.benefits || []) {
        map[item.code] = item
      }
      benefits.value = map
      loaded.value = true
    } catch (e) {
      // 加载失败按无权益处理，不打扰页面
      benefits.value = {}
      planKey.value = 'free'
    }
  }

  /** boolean 类权益是否开通。 */
  function hasBenefit(code) {
    const item = benefits.value[code]
    return !!item && item.value === 'true'
  }

  /** 原始权益值（tier/quota 类用）。 */
  function benefitValue(code) {
    const item = benefits.value[code]
    return item ? item.value : null
  }

  /** quota 类剩余额度。 */
  function benefitRemaining(code) {
    const item = benefits.value[code]
    return item && item.remaining != null ? item.remaining : 0
  }

  return {
    benefits,
    planKey,
    planName,
    expiresAt,
    loaded,
    loadBenefits,
    hasBenefit,
    benefitValue,
    benefitRemaining
  }
}
