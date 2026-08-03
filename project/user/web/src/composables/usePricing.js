import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { subscribe, getPlanCatalog, getNewcomerOffer } from '@/api/membership'

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

    if (localStorage.getItem('aichuangzuo_access_token')) {
      newcomerLoading.value = true
      try {
        const res = await getNewcomerOffer()
        const data = res.data || res
        if (data?.eligible) {
          newcomerOffer.value = data
          if (route.query.newcomer === '1') {
            activeCycle.value = 'year'
          }
        }
      } catch {
        newcomerOffer.value = null
      } finally {
        newcomerLoading.value = false
      }
    }
  })

  const handleSubscribe = (plan) => {
    if (!localStorage.getItem('aichuangzuo_access_token')) {
      message.info('请先登录后再订阅')
      router.push('/login')
      return
    }
    selectedPlan.value = plan
    payCode.value = ''
    modalVisible.value = true
  }

  const handleNewcomerSubscribe = () => {
    if (!localStorage.getItem('aichuangzuo_access_token')) {
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
    modalVisible.value = true
  }

  const handlePay = async () => {
    if (!payCode.value || payCode.value.length !== 6) {
      message.warning('请输入 6 位支付码')
      return
    }

    const plan = selectedPlan.value
    const cycle = activeCycle.value
    const isNewcomerDeal = newcomerOffer.value &&
      plan.key === newcomerOffer.value.planKey &&
      cycle === newcomerOffer.value.cycle
    const price = isNewcomerDeal
      ? { current: newcomerOffer.value.finalPrice }
      : plan[cycle === 'month' ? 'monthly' : cycle]

    subscribeLoading.value = true
    try {
      const res = await subscribe({
        planKey: plan.key,
        cycle,
        payCode: payCode.value,
        amount: price.current
      })
      const data = res.data
      message.success('订阅成功')
      localStorage.setItem('aichuangzuo_membership', JSON.stringify({
        level: planKeyToName[data.level] || plan.name,
        expiresAt: data.expiresAt
      }))
      modalVisible.value = false
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
    planKeyToName,
    cycleLabel,
    activeCycle,
    cycles,
    getPeriodLabel,
    getPrice,
    getArticles,
    getSavings,
    cellValue,
    getCell,
    handleSubscribe,
    handleNewcomerSubscribe,
    handlePay,
    scrollToCompare
  }
}
