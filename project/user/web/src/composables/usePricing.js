import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  subscribe,
  getPlanCatalog,
  getNewcomerOffer,
  getMyMembership,
  previewUpgrade,
  previewSubscribe
} from '@/api/membership'
import { useInviteStats } from '@/composables/useInviteStats'

const PLAN_RANK = {
  free: 0,
  basic: 1,
  pro: 2,
  flagship: 3
}

const CYCLE_RANK = {
  month: 0,
  quarter: 1,
  year: 2
}

export function usePricing() {
  const router = useRouter()
  const route = useRoute()

  const { coinBalance, loadInviteStats, adjustCoinBalance } = useInviteStats()

  const COIN_TO_YUAN_RATIO = 10
  const modalVisible = ref(false)
  const selectedPlan = ref(null)
  const payCode = ref('')
  const subscribeLoading = ref(false)
  const selectedCoinAmount = ref(0)

  const plans = ref([])
  const compareRows = ref([])
  const catalogLoading = ref(false)

  const newcomerOffer = ref(null)
  const newcomerLoading = ref(false)

  const currentMembership = ref(null)
  const membershipLoading = ref(false)

  const upgradeModalVisible = ref(false)
  const upgradePreview = ref(null)
  const upgradeLoading = ref(false)

  const planKeyToName = {
    basic: '基础版',
    pro: '专业版',
    flagship: '旗舰版'
  }

  const cycleLabel = {
    month: '月度',
    quarter: '季度',
    year: '年度'
  }

  const activeCycle = ref('month')
  const cycles = [
    { key: 'month', label: '月度' },
    { key: 'quarter', label: '季度' },
    { key: 'year', label: '年度' }
  ]

  const isLoggedIn = () => !!localStorage.getItem('aichuangzuo_access_token')

  onMounted(async () => {
    catalogLoading.value = true
    try {
      const res = await getPlanCatalog()
      const rawPlans = res.data.plans || []
      const rawRows = res.data.compareRows || []
      const CARD_BENEFIT_CODE = 'sticker_quota'
      const SKILL_CUSTOM_CODE = 'skill_custom'
      const SKILL_CUSTOM_LABEL = '我的提示词'
      plans.value = rawPlans
      compareRows.value = rawRows
        .filter(row => row.code !== CARD_BENEFIT_CODE)
        .map(row => {
          if (row.code !== SKILL_CUSTOM_CODE) return row
          const prefixCell = (cell) => {
            if (!cell || typeof cell.value !== 'string') return cell
            return { ...cell, value: `${SKILL_CUSTOM_LABEL} ${cell.value}` }
          }
          return {
            ...row,
            label: SKILL_CUSTOM_LABEL,
            basic: prefixCell(row.basic),
            pro: prefixCell(row.pro),
            flagship: prefixCell(row.flagship)
          }
        })
    } catch (err) {
      message.error(err.message || '定价加载失败')
    } finally {
      catalogLoading.value = false
    }

    if (isLoggedIn()) {
      membershipLoading.value = true
      newcomerLoading.value = true
      try {
        const [membershipRes, newcomerRes] = await Promise.all([
          getMyMembership(),
          getNewcomerOffer(),
          loadInviteStats()
        ])
        const membershipData = membershipRes.data || membershipRes
        if (membershipData?.hasMembership) {
          currentMembership.value = membershipData
          if (membershipData.cycle && cycles.some(c => c.key === membershipData.cycle)) {
            activeCycle.value = membershipData.cycle
          }
        }
        const newcomerData = newcomerRes.data || newcomerRes
        if (newcomerData?.eligible) {
          newcomerOffer.value = newcomerData
          if (route.query.newcomer === '1') {
            activeCycle.value = 'year'
          }
        }
      } catch {
        currentMembership.value = null
        newcomerOffer.value = null
      } finally {
        membershipLoading.value = false
        newcomerLoading.value = false
      }
    }
  })

  const currentPlanKey = () => {
    if (!currentMembership.value) return 'free'
    return currentMembership.value.level || 'free'
  }

  const currentCycleKey = () => currentMembership.value?.cycle

  const isCycleDisabled = (cycleKey) => {
    const currentKey = currentPlanKey()
    const memberCycle = currentCycleKey()
    if (currentKey === 'free' || !memberCycle) return false
    return CYCLE_RANK[cycleKey] < CYCLE_RANK[memberCycle]
  }

  const cycleLocked = () => cycles.some(c => isCycleDisabled(c.key))

  const currentCycle = () => {
    return activeCycle.value
  }

  const setCycle = (key) => {
    if (isCycleDisabled(key)) return
    activeCycle.value = key
  }

  const getPlanButton = (plan) => {
    const currentKey = currentPlanKey()
    const currentCycleKey = currentMembership.value?.cycle
    const targetCycleKey = activeCycle.value

    if (currentKey === plan.key) {
      if (currentCycleKey === targetCycleKey) {
        return { text: '当前订阅', action: 'current', disabled: true }
      }
      if (CYCLE_RANK[targetCycleKey] > CYCLE_RANK[currentCycleKey]) {
        return { text: '升级套餐', action: 'upgrade', disabled: false, primary: true }
      }
      return { text: '立即订阅', action: 'disabled', disabled: true }
    }

    if (currentKey === 'free') {
      return { text: '立即订阅', action: 'subscribe', disabled: false, primary: plan.recommended }
    }

    if (PLAN_RANK[plan.key] > PLAN_RANK[currentKey]) {
      if (CYCLE_RANK[targetCycleKey] < CYCLE_RANK[currentCycleKey]) {
        return { text: '立即订阅', action: 'disabled', disabled: true }
      }
      return { text: '升级套餐', action: 'upgrade', disabled: false, primary: true }
    }

    if (PLAN_RANK[plan.key] < PLAN_RANK[currentKey]) {
      return { text: '立即订阅', action: 'disabled', disabled: true }
    }

    return { text: '立即订阅', action: 'subscribe', disabled: false }
  }

  const handleSubscribe = async (plan) => {
    const btn = getPlanButton(plan)
    if (btn.disabled) return

    if (!isLoggedIn()) {
      message.info('请先登录后再订阅')
      router.push('/login')
      return
    }
    selectedPlan.value = plan
    payCode.value = ''
    upgradePreview.value = null

    const currentKey = currentPlanKey()
    const currentCycleKey = currentMembership.value?.cycle
    const isUpgrade = currentKey !== 'free' && (
      PLAN_RANK[plan.key] > PLAN_RANK[currentKey] ||
      (plan.key === currentKey && CYCLE_RANK[activeCycle.value] > CYCLE_RANK[currentCycleKey])
    )
    if (isUpgrade) {
      upgradeLoading.value = true
      try {
        const res = await previewUpgrade({ planKey: plan.key, cycle: currentCycle() })
        upgradePreview.value = res.data || res
        syncCoinSelection()
        upgradeModalVisible.value = true
      } catch (err) {
        message.error(err.message || '升级预览失败')
      } finally {
        upgradeLoading.value = false
      }
      return
    }

    syncCoinSelection()
    modalVisible.value = true
  }

  const handleNewcomerSubscribe = () => {
    if (!isLoggedIn()) {
      message.info('请先登录后再订阅')
      router.push('/login')
      return
    }
    if (!newcomerOffer.value) return
    const plan = plans.value.find(p => p.key === newcomerOffer.value.planKey)
    if (!plan) return
    selectedPlan.value = plan
    activeCycle.value = 'year'
    payCode.value = ''
    upgradePreview.value = null
    syncCoinSelection()
    modalVisible.value = true
  }

  const confirmUpgrade = () => {
    upgradeModalVisible.value = false
    syncCoinSelection()
    modalVisible.value = true
  }

  const getExpectedCash = () => {
    const cycle = currentCycle()
    const keyMap = { month: 'monthly', quarter: 'quarter', year: 'year' }
    const plan = selectedPlan.value
    const isNewcomerDeal = newcomerOffer.value &&
      plan.key === newcomerOffer.value.planKey &&
      cycle === newcomerOffer.value.cycle
    if (upgradePreview.value && upgradePreview.value.targetPlanKey === plan.key && upgradePreview.value.targetCycle === cycle) {
      return Number(upgradePreview.value.finalPrice) || 0
    }
    if (isNewcomerDeal) {
      return Number(newcomerOffer.value.finalPrice) || 0
    }
    return Number(plan[keyMap[cycle]]?.current) || 0
  }

  const getMaxCoinAmount = () => {
    const maxByCash = Math.floor(getExpectedCash() * COIN_TO_YUAN_RATIO)
    return Math.min(Math.floor(coinBalance.value), maxByCash)
  }

  const getCoinDiscountYuan = () => {
    return selectedCoinAmount.value / COIN_TO_YUAN_RATIO
  }

  const getFinalCash = () => {
    return Number(Math.max(0, getExpectedCash() - getCoinDiscountYuan()).toFixed(2))
  }

  const syncCoinSelection = () => {
    selectedCoinAmount.value = getMaxCoinAmount()
  }

  const handlePay = async () => {
    if (!payCode.value || payCode.value.length !== 6) {
      message.warning('请输入 6 位支付码')
      return
    }

    const plan = selectedPlan.value
    const cycle = currentCycle()

    subscribeLoading.value = true
    try {
      const res = await subscribe({
        planKey: plan.key,
        cycle,
        payCode: payCode.value,
        amount: getFinalCash(),
        coinAmount: selectedCoinAmount.value
      })
      const data = res.data
      message.success(upgradePreview.value ? '升级成功' : '订阅成功')
      adjustCoinBalance(-(data.coinAmount || selectedCoinAmount.value))
      selectedCoinAmount.value = 0
      localStorage.setItem('aichuangzuo_membership', JSON.stringify({
        level: planKeyToName[data.level] || plan.name,
        expiresAt: data.expiresAt
      }))
      modalVisible.value = false
      upgradeModalVisible.value = false
      upgradePreview.value = null
      currentMembership.value = {
        hasMembership: true,
        level: data.level,
        levelName: planKeyToName[data.level] || plan.name,
        cycle: data.cycle || cycle,
        expiresAt: data.expiresAt
      }
      router.push('/console/create')
    } catch (err) {
      message.error(err.message || '订阅失败，请重试')
    } finally {
      subscribeLoading.value = false
    }
  }

  const getPeriodLabel = () => {
    return activeCycle.value === 'month' ? '月'
      : activeCycle.value === 'quarter' ? '季' : '年'
  }

  const getPrice = (plan) => {
    const keyMap = { month: 'monthly', quarter: 'quarter', year: 'year' }
    const cycle = plan[keyMap[activeCycle.value]]
    return { original: cycle?.original, current: cycle?.current }
  }

  const getArticles = (plan) => {
    const keyMap = { month: 'monthly', quarter: 'quarter', year: 'year' }
    return plan[keyMap[activeCycle.value]]?.articles
  }

  const getSavings = (plan) => {
    const keyMap = { month: 'monthly', quarter: 'quarter', year: 'year' }
    return plan[keyMap[activeCycle.value]]?.savings || null
  }

  const cellValue = (cell) => (cell == null ? null : cell.value)

  const getCell = (row, col) => {
    const cell = row[col]
    if (cell == null) return ''
    const val = cell.value
    if (val === true) return '<span style="color:#FF2442;font-weight:600;">✓</span>'
    if (val === false) return '<span style="color:#FF2442;font-weight:600;">✗</span>'
    return `<span style="font-weight:500;">${val}</span>`
  }

  const scrollToCompare = () => {
    document.getElementById('pricing-compare')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }

  return {
    modalVisible,
    selectedPlan,
    payCode,
    subscribeLoading,
    selectedCoinAmount,
    plans,
    compareRows,
    catalogLoading,
    newcomerOffer,
    newcomerLoading,
    currentMembership,
    membershipLoading,
    upgradeModalVisible,
    upgradePreview,
    upgradeLoading,
    planKeyToName,
    cycleLabel,
    activeCycle,
    cycles,
    cycleLocked,
    setCycle,
    isCycleDisabled,
    getPeriodLabel,
    getPrice,
    getArticles,
    getSavings,
    cellValue,
    getCell,
    getPlanButton,
    handleSubscribe,
    handleNewcomerSubscribe,
    confirmUpgrade,
    handlePay,
    scrollToCompare,
    coinBalance,
    COIN_TO_YUAN_RATIO,
    getMaxCoinAmount,
    getCoinDiscountYuan,
    getFinalCash
  }
}
