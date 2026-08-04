import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { subscribe, getPlanCatalog, getNewcomerOffer, getMyMembership, previewUpgrade } from '@/api/membership'

const PLAN_RANK = {
  free: 0,
  basic: 1,
  pro: 2,
  flagship: 3
}

export function usePricing() {
  const router = useRouter()
  const route = useRoute()

  const modalVisible = ref(false)
  const selectedPlan = ref(null)
  const payCode = ref('')
  const subscribeLoading = ref(false)

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
      plans.value = rawPlans.map(plan => ({
        ...plan,
        features: (plan.features || [])
          .filter(f => f.code !== CARD_BENEFIT_CODE)
          .map(f => f.code === SKILL_CUSTOM_CODE ? { ...f, text: `${SKILL_CUSTOM_LABEL} ${f.text}` } : f)
      }))
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
          getNewcomerOffer()
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

  const cycleLocked = () => {
    return !!currentMembership.value?.cycle
  }

  const currentCycle = () => {
    const membershipCycle = currentMembership.value?.cycle
    if (membershipCycle) {
      activeCycle.value = membershipCycle
      return membershipCycle
    }
    return activeCycle.value
  }

  const setCycle = (key) => {
    if (cycleLocked()) return
    activeCycle.value = key
  }

  const getPlanButton = (plan) => {
    const currentKey = currentPlanKey()
    if (currentKey === plan.key) {
      return { text: '当前订阅', action: 'current', disabled: true }
    }
    if (PLAN_RANK[plan.key] > PLAN_RANK[currentKey]) {
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
    if (PLAN_RANK[plan.key] > PLAN_RANK[currentKey]) {
      upgradeLoading.value = true
      try {
        const res = await previewUpgrade({ planKey: plan.key, cycle: currentCycle() })
        upgradePreview.value = res.data || res
        upgradeModalVisible.value = true
      } catch (err) {
        message.error(err.message || '升级预览失败')
      } finally {
        upgradeLoading.value = false
      }
      return
    }

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
    modalVisible.value = true
  }

  const confirmUpgrade = () => {
    upgradeModalVisible.value = false
    modalVisible.value = true
  }

  const handlePay = async () => {
    if (!payCode.value || payCode.value.length !== 6) {
      message.warning('请输入 6 位支付码')
      return
    }

    const plan = selectedPlan.value
    const cycle = currentCycle()
    const isNewcomerDeal = newcomerOffer.value &&
      plan.key === newcomerOffer.value.planKey &&
      cycle === newcomerOffer.value.cycle
      cycle === newcomerOffer.value.cycle

    let price
    if (upgradePreview.value && upgradePreview.value.targetPlanKey === plan.key && upgradePreview.value.targetCycle === cycle) {
      price = { current: upgradePreview.value.finalPrice }
    } else if (isNewcomerDeal) {
      price = { current: newcomerOffer.value.finalPrice }
    } else {
      price = plan[cycle === 'month' ? 'monthly' : cycle]
    }

    subscribeLoading.value = true
    try {
      const res = await subscribe({
        planKey: plan.key,
        cycle,
        payCode: payCode.value,
        amount: price.current
      })
      const data = res.data
      message.success(upgradePreview.value ? '升级成功' : '订阅成功')
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
    scrollToCompare
  }
}
