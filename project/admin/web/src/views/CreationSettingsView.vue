<template>
  <div class="creation-settings">
    <a-card :bordered="false">
      <a-page-header title="创作设置" sub-title="管理 worker 线程池 / 拉取 / lease / 重试 / 归档 cron" />

      <a-spin :spinning="loading">
        <a-alert
          v-if="updatedAt"
          type="info"
          show-icon
          style="margin-bottom: 16px"
          :message="`最近更新：${updatedAt}（by ${updatedBy || 'system'}）`"
        />

        <a-form
          ref="formRef"
          :model="form"
          :rules="rules"
          layout="vertical"
          style="max-width: 720px; margin-top: 8px"
        >
          <a-divider orientation="left">worker 线程池</a-divider>

          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="线程池大小" name="poolSize" extra="1-10。修改后需重启 admin-api 生效（重建线程池）">
                <a-input-number v-model:value="form.poolSize" :min="1" :max="10" style="width: 100%" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="单次拉取数" name="claimBatchSize" extra="1-10，每轮从队列表抢占几个任务">
                <a-input-number v-model:value="form.claimBatchSize" :min="1" :max="10" style="width: 100%" />
              </a-form-item>
            </a-col>
          </a-row>

          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="lease 超时（分钟）" name="leaseMinutes" extra="1-60。worker 抢占后多少分钟没完成视为卡死">
                <a-input-number v-model:value="form.leaseMinutes" :min="1" :max="60" style="width: 100%" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="轮询间隔（ms）" name="pollIntervalMs" extra="100-5000。空轮询睡眠时间">
                <a-input-number v-model:value="form.pollIntervalMs" :min="100" :max="5000" :step="100" style="width: 100%" />
              </a-form-item>
            </a-col>
          </a-row>

          <a-divider orientation="left">worker 实例</a-divider>

          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="worker 实例 ID" name="workerId" extra="单实例约定；多实例部署时区分用">
                <a-input v-model:value="form.workerId" placeholder="worker-1" />
              </a-form-item>
            </a-col>
          </a-row>

          <a-divider orientation="left">AI 调用默认参数</a-divider>

          <p class="section-tip">
            13 阶段模板未配置 model_params 时回落到这里。
            修改后下一个任务立即生效。
          </p>

          <a-row :gutter="16">
            <a-col :span="8">
              <a-form-item label="temperature" name="defaultTemperature" extra="0.00-2.00。越高越发散">
                <a-input-number v-model:value="form.defaultTemperature" :min="0" :max="2" :step="0.1" style="width: 100%" />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="max_tokens" name="defaultMaxTokens" extra="1-128000。M3 推理也吃此预算">
                <a-input-number v-model:value="form.defaultMaxTokens" :min="1" :max="128000" :step="512" style="width: 100%" />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="top_p" name="defaultTopP" extra="0.00-1.00。核采样阈值">
                <a-input-number v-model:value="form.defaultTopP" :min="0" :max="1" :step="0.05" style="width: 100%" />
              </a-form-item>
            </a-col>
          </a-row>

          <a-row :gutter="16">
            <a-col :span="8">
              <a-form-item label="AI 读取超时（秒）" name="aiReadTimeoutSeconds" extra="30-3600。MiniMax-M3 推理慢，建议 180+">
                <a-input-number v-model:value="form.aiReadTimeoutSeconds" :min="30" :max="3600" style="width: 100%" />
              </a-form-item>
            </a-col>
          </a-row>

          <a-divider orientation="left">AI 调用重试（按 stage）</a-divider>

          <p class="section-tip">
            单 stage 内 AI 调用失败时自动重试（指数退避）。
            修改后下一个任务立即生效。
          </p>

          <a-row :gutter="16">
            <a-col :span="8">
              <a-form-item label="最大尝试次数" name="llmRetryMaxAttempts" extra="1-10。包含首次失败">
                <a-input-number v-model:value="form.llmRetryMaxAttempts" :min="1" :max="10" style="width: 100%" />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="首次重试前等待（ms）" name="llmRetryBaseDelayMs" extra="100-10000。每次失败后按倍数叠加">
                <a-input-number v-model:value="form.llmRetryBaseDelayMs" :min="100" :max="10000" :step="100" style="width: 100%" />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="退避倍数" name="llmRetryBackoffMultiplier" extra="1-5。第 N 次重试睡 base × mult^(N-1)">
                <a-input-number v-model:value="form.llmRetryBackoffMultiplier" :min="1" :max="5" style="width: 100%" />
              </a-form-item>
            </a-col>
          </a-row>

          <a-divider orientation="left">归档</a-divider>

          <a-form-item label="归档 cron" name="retentionCron" extra="把过期 completed/failed 任务迁移到 history 的定时任务。修改后需重启 admin-api 生效">
            <a-input v-model:value="form.retentionCron" placeholder="0 0 3 * * ?" />
          </a-form-item>

          <a-form-item label="备注" name="remark">
            <a-input v-model:value="form.remark" placeholder="可选" />
          </a-form-item>

          <a-form-item>
            <a-space>
              <a-button type="primary" :loading="submitting" @click="onSubmit">保存</a-button>
              <a-button @click="fetchDetail">重置</a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </a-spin>
    </a-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useCreationSettings } from '@/composables/useCreationSettings.js'

const {
  form,
  loading,
  submitting,
  updatedAt,
  updatedBy,
  fetchDetail,
  submit
} = useCreationSettings()

const formRef = ref()

const rules = reactive({
  poolSize: [{ required: true, type: 'number', min: 1, max: 10, message: '1-10' }],
  claimBatchSize: [{ required: true, type: 'number', min: 1, max: 10, message: '1-10' }],
  leaseMinutes: [{ required: true, type: 'number', min: 1, max: 60, message: '1-60' }],
  pollIntervalMs: [{ required: true, type: 'number', min: 100, max: 5000, message: '100-5000' }],
  retentionCron: [{ required: true, message: '不能为空' }],
  workerId: [{ required: true, message: '不能为空' }],
  llmRetryMaxAttempts: [{ required: true, type: 'number', min: 1, max: 10, message: '1-10' }],
  llmRetryBaseDelayMs: [{ required: true, type: 'number', min: 100, max: 10000, message: '100-10000' }],
  llmRetryBackoffMultiplier: [{ required: true, type: 'number', min: 1, max: 5, message: '1-5' }],
  defaultTemperature: [{ required: true, type: 'number', min: 0, max: 2, message: '0-2' }],
  defaultMaxTokens: [{ required: true, type: 'number', min: 1, max: 128000, message: '1-128000' }],
  defaultTopP: [{ required: true, type: 'number', min: 0, max: 1, message: '0-1' }],
  aiReadTimeoutSeconds: [{ required: true, type: 'number', min: 30, max: 3600, message: '30-3600' }]
})

const onSubmit = async () => {
  await formRef.value?.validate()
  await submit()
}

onMounted(() => fetchDetail())
</script>

<style scoped>
.creation-settings {
  max-width: 1000px;
}
.section-tip {
  margin: 0 0 16px;
  font-size: 13px;
  color: #595959;
}
</style>
