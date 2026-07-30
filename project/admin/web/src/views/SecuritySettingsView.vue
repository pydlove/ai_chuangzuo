<template>
  <div class="security-settings">
    <a-card :bordered="false">
      <a-page-header title="安全设置" sub-title="配置用户端创作相关的安全策略" />

      <a-spin :spinning="loading">
        <a-form layout="vertical" style="max-width: 720px; margin-top: 8px">
          <a-divider orientation="left">AI 生成频率限制</a-divider>
          <p class="section-tip">
            限制每个会员套餐每分钟内最多可提交的生成任务数（包含重新生成）。
            修改后约 2 分钟内在用户端生效。
          </p>

          <a-row :gutter="16">
            <a-col :span="8">
              <a-form-item label="基础版（次/分钟）">
                <a-input-number
                  v-model:value="values.basic"
                  :min="1"
                  :max="1000"
                  :precision="0"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="专业版（次/分钟）">
                <a-input-number
                  v-model:value="values.pro"
                  :min="1"
                  :max="1000"
                  :precision="0"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="旗舰版（次/分钟）">
                <a-input-number
                  v-model:value="values.flagship"
                  :min="1"
                  :max="1000"
                  :precision="0"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
          </a-row>

          <a-form-item>
            <a-space>
              <a-button type="primary" :loading="submitting" @click="onSubmit">保存</a-button>
              <a-button @click="load">重置</a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </a-spin>
    </a-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { fetchBenefits } from '@/api/benefit.js'
import { fetchPlanBenefits, upsertPlanBenefit } from '@/api/planBenefit.js'

const BENEFIT_CODE = 'generation_rate_limit'
const PLAN_KEYS = ['basic', 'pro', 'flagship']
const DEFAULT_VALUES = { basic: 3, pro: 5, flagship: 8 }

const loading = ref(false)
const submitting = ref(false)
const values = reactive({ ...DEFAULT_VALUES })
const originalValues = reactive({ ...DEFAULT_VALUES })

async function load() {
  loading.value = true
  try {
    const [benefitList, planBenefitList] = await Promise.all([
      fetchBenefits(),
      fetchPlanBenefits()
    ])

    const benefit = benefitList.find((b) => b.code === BENEFIT_CODE)
    if (!benefit) {
      message.error('未找到生成频率限制权益配置')
      return
    }

    PLAN_KEYS.forEach((key) => {
      const row = planBenefitList.find(
        (item) => item.planKey === key && item.benefitCode === BENEFIT_CODE
      )
      const parsed = parsePositiveInt(row?.benefitValue, DEFAULT_VALUES[key])
      values[key] = parsed
      originalValues[key] = parsed
    })
  } catch (e) {
    message.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function onSubmit() {
  submitting.value = true
  try {
    const changed = PLAN_KEYS.filter((key) => values[key] !== originalValues[key])
    await Promise.all(
      changed.map((key) =>
        upsertPlanBenefit({
          planKey: key,
          benefitCode: BENEFIT_CODE,
          benefitValue: String(values[key])
        })
      )
    )
    message.success('保存成功')
    changed.forEach((key) => {
      originalValues[key] = values[key]
    })
  } catch (e) {
    message.error(e?.message || '保存失败')
  } finally {
    submitting.value = false
  }
}

function parsePositiveInt(value, fallback) {
  if (value == null) return fallback
  const parsed = Number(value)
  return Number.isFinite(parsed) && parsed > 0 ? Math.floor(parsed) : fallback
}

onMounted(load)
</script>

<style scoped>
.security-settings {
  max-width: 1000px;
}
.section-tip {
  margin: 0 0 16px;
  font-size: 13px;
  color: #595959;
}
</style>
