import { reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  getSkillMonthlyRewardConfig,
  updateSkillMonthlyRewardConfig
} from '@/api/skillMonthlyRewardConfig.js'

export function useSkillMonthlyRewardConfig() {
  const form = reactive({
    firstAmount: 600,
    secondAmount: 300,
    thirdAmount: 150,
    fourthAmount: 100,
    fifthAmount: 50,
    settlementCron: '0 0 3 1 * ?',
    enabled: 1
  })
  const loading = ref(false)
  const submitting = ref(false)
  const updatedAt = ref(null)
  const updatedBy = ref(null)

  const fetchDetail = async () => {
    loading.value = true
    try {
      const res = await getSkillMonthlyRewardConfig()
      Object.assign(form, {
        firstAmount: res.firstAmount != null ? Number(res.firstAmount) : 600,
        secondAmount: res.secondAmount != null ? Number(res.secondAmount) : 300,
        thirdAmount: res.thirdAmount != null ? Number(res.thirdAmount) : 150,
        fourthAmount: res.fourthAmount != null ? Number(res.fourthAmount) : 100,
        fifthAmount: res.fifthAmount != null ? Number(res.fifthAmount) : 50,
        settlementCron: res.settlementCron ?? '0 0 3 1 * ?',
        enabled: res.enabled ?? 1
      })
      updatedAt.value = res.updatedAt
      updatedBy.value = res.updatedBy
    } catch (e) {
      message.error(e.message || '加载配置失败')
    } finally {
      loading.value = false
    }
  }

  const submit = async () => {
    submitting.value = true
    try {
      const res = await updateSkillMonthlyRewardConfig({
        firstAmount: Number(form.firstAmount),
        secondAmount: Number(form.secondAmount),
        thirdAmount: Number(form.thirdAmount),
        fourthAmount: Number(form.fourthAmount),
        fifthAmount: Number(form.fifthAmount),
        settlementCron: form.settlementCron,
        enabled: form.enabled
      })
      message.success('已保存')
      Object.assign(form, {
        firstAmount: res.firstAmount != null ? Number(res.firstAmount) : form.firstAmount,
        secondAmount: res.secondAmount != null ? Number(res.secondAmount) : form.secondAmount,
        thirdAmount: res.thirdAmount != null ? Number(res.thirdAmount) : form.thirdAmount,
        fourthAmount: res.fourthAmount != null ? Number(res.fourthAmount) : form.fourthAmount,
        fifthAmount: res.fifthAmount != null ? Number(res.fifthAmount) : form.fifthAmount,
        settlementCron: res.settlementCron ?? form.settlementCron,
        enabled: res.enabled ?? form.enabled
      })
      updatedAt.value = res.updatedAt
      updatedBy.value = res.updatedBy
    } catch (e) {
      message.error(e.message || '保存失败')
    } finally {
      submitting.value = false
    }
  }

  return { form, loading, submitting, updatedAt, updatedBy, fetchDetail, submit }
}
