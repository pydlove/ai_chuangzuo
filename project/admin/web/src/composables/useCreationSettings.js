import { reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  getGenerationConfig,
  updateGenerationConfig
} from '@/api/creationSettings.js'

export function useCreationSettings() {
  const form = reactive({
    poolSize: 2,
    claimBatchSize: 1,
    leaseMinutes: 5,
    maxRetry: 3,
    pollIntervalMs: 500,
    retentionCron: '0 0 3 * * ?',
    workerId: 'worker-1',
    llmRetryMaxAttempts: 3,
    llmRetryBaseDelayMs: 500,
    llmRetryBackoffMultiplier: 2,
    remark: ''
  })
  const loading = ref(false)
  const submitting = ref(false)
  const updatedAt = ref(null)
  const updatedBy = ref(null)

  const fetchDetail = async () => {
    loading.value = true
    try {
      const res = await getGenerationConfig()
      Object.assign(form, {
        poolSize: res.poolSize ?? 2,
        claimBatchSize: res.claimBatchSize ?? 1,
        leaseMinutes: res.leaseMinutes ?? 5,
        maxRetry: res.maxRetry ?? 3,
        pollIntervalMs: res.pollIntervalMs ?? 500,
        retentionCron: res.retentionCron ?? '0 0 3 * * ?',
        workerId: res.workerId ?? 'worker-1',
        llmRetryMaxAttempts: res.llmRetryMaxAttempts ?? 3,
        llmRetryBaseDelayMs: res.llmRetryBaseDelayMs ?? 500,
        llmRetryBackoffMultiplier: res.llmRetryBackoffMultiplier ?? 2,
        remark: res.remark ?? ''
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
      const res = await updateGenerationConfig({ ...form })
      message.success('已保存。worker 将在下一个轮询周期（< 1 秒）读到新配置；线程池大小需重启 admin-api 生效')
      Object.assign(form, {
        poolSize: res.poolSize ?? form.poolSize,
        claimBatchSize: res.claimBatchSize ?? form.claimBatchSize,
        leaseMinutes: res.leaseMinutes ?? form.leaseMinutes,
        maxRetry: res.maxRetry ?? form.maxRetry,
        pollIntervalMs: res.pollIntervalMs ?? form.pollIntervalMs,
        retentionCron: res.retentionCron ?? form.retentionCron,
        workerId: res.workerId ?? form.workerId,
        llmRetryMaxAttempts: res.llmRetryMaxAttempts ?? form.llmRetryMaxAttempts,
        llmRetryBaseDelayMs: res.llmRetryBaseDelayMs ?? form.llmRetryBaseDelayMs,
        llmRetryBackoffMultiplier: res.llmRetryBackoffMultiplier ?? form.llmRetryBackoffMultiplier,
        remark: res.remark ?? form.remark
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
