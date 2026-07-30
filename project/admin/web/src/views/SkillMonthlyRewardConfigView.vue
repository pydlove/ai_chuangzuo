<template>
  <div class="skill-monthly-rewards">
    <a-card :bordered="false">
      <a-page-header title="月度奖励配置" sub-title="设置提示词市场收益潜力榜 Top1–Top5 的月度奖励金额" />

      <a-spin :spinning="loading">
        <a-alert
          v-if="updatedAt"
          type="info"
          show-icon
          style="margin-bottom: 16px"
          :message="`最近更新：${updatedAt}（by ${updatedBy || 'system'}）`"
        />

        <a-form
          :model="form"
          layout="vertical"
          style="max-width: 720px; margin-top: 8px"
        >
          <a-divider orientation="left">排行榜奖励金额（创作币）</a-divider>

          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="Top1 奖励" name="firstAmount">
                <a-input-number v-model:value="form.firstAmount" :min="0" :precision="2" style="width: 100%" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="Top2 奖励" name="secondAmount">
                <a-input-number v-model:value="form.secondAmount" :min="0" :precision="2" style="width: 100%" />
              </a-form-item>
            </a-col>
          </a-row>

          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="Top3 奖励" name="thirdAmount">
                <a-input-number v-model:value="form.thirdAmount" :min="0" :precision="2" style="width: 100%" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="Top4 奖励" name="fourthAmount">
                <a-input-number v-model:value="form.fourthAmount" :min="0" :precision="2" style="width: 100%" />
              </a-form-item>
            </a-col>
          </a-row>

          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="Top5 奖励" name="fifthAmount">
                <a-input-number v-model:value="form.fifthAmount" :min="0" :precision="2" style="width: 100%" />
              </a-form-item>
            </a-col>
          </a-row>

          <a-divider orientation="left">结算任务</a-divider>

          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="结算 cron" name="settlementCron" extra="默认每月 1 日 03:00 执行，修改后需重启 admin-api 生效">
                <a-input v-model:value="form.settlementCron" placeholder="0 0 3 1 * ?" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="启用结算" name="enabled">
                <a-switch
                  v-model:checked="enabledChecked"
                  checked-children="启用"
                  un-checked-children="禁用"
                />
              </a-form-item>
            </a-col>
          </a-row>

          <a-form-item>
            <a-space>
              <a-button type="primary" :loading="submitting" @click="submit">保存</a-button>
              <a-button @click="fetchDetail">重置</a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </a-spin>
    </a-card>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useSkillMonthlyRewardConfig } from '@/composables/useSkillMonthlyRewardConfig.js'

const {
  form,
  loading,
  submitting,
  updatedAt,
  updatedBy,
  fetchDetail,
  submit
} = useSkillMonthlyRewardConfig()

const enabledChecked = computed({
  get: () => form.enabled === 1,
  set: (val) => {
    form.enabled = val ? 1 : 0
  }
})

onMounted(() => {
  fetchDetail()
})
</script>

<style scoped>
.skill-monthly-rewards {
  padding: 24px;
}
</style>
