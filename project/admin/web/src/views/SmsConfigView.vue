<template>
  <div class="sms-config">
    <a-card :bordered="false">
      <a-page-header title="短信配置" sub-title="配置用户端短信验证码发送策略" style="padding-left: 0; padding-top: 0" />

      <a-alert
        message="使用说明"
        description="当前仅支持阿里云短信服务。请填写阿里云 AccessKey、签名、模板 Code 与 RegionId；保存后用户端注册 / 忘记密码即可使用短信验证码。"
        type="info"
        show-icon
        style="margin-bottom: 16px"
      />

      <a-spin :spinning="loading">
        <a-form layout="vertical" style="max-width: 720px" :model="form">
          <a-form-item label="短信服务商">
            <a-input value="阿里云" disabled />
          </a-form-item>

          <a-form-item label="AccessKey ID" required>
            <a-input v-model:value="form.accessKeyId" placeholder="请输入阿里云 AccessKey ID" maxlength="128" />
          </a-form-item>

          <a-form-item label="AccessKey Secret" required>
            <a-input-password
              v-model:value="form.accessKeySecret"
              placeholder="留空或全为 * 号表示不修改原密钥"
              maxlength="256"
            />
          </a-form-item>

          <a-form-item label="短信签名" required>
            <a-input v-model:value="form.signName" placeholder="例如：爱创作" maxlength="64" />
          </a-form-item>

          <a-form-item label="短信模板 Code" required>
            <a-input v-model:value="form.templateCode" placeholder="例如：SMS_12345678" maxlength="64" />
          </a-form-item>

          <a-form-item label="RegionId" required>
            <a-input v-model:value="form.regionId" placeholder="例如：cn-hangzhou" maxlength="32" />
          </a-form-item>

          <a-form-item label="启用短信验证码">
            <a-switch
              v-model:checked="enabledChecked"
              checked-children="启用"
              un-checked-children="关闭"
            />
          </a-form-item>

          <a-divider orientation="left">安全策略</a-divider>

          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="发送间隔（秒）">
                <a-input-number
                  v-model:value="form.sendIntervalSeconds"
                  :min="1"
                  :max="3600"
                  :precision="0"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="单手机号日限（条）">
                <a-input-number
                  v-model:value="form.dailyMaxPerPhone"
                  :min="1"
                  :max="100"
                  :precision="0"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
          </a-row>

          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="单 IP 日限（条）">
                <a-input-number
                  v-model:value="form.dailyMaxPerIp"
                  :min="1"
                  :max="1000"
                  :precision="0"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="全站日限（条）">
                <a-input-number
                  v-model:value="form.globalDailyMax"
                  :min="1"
                  :max="100000"
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
import { getSmsConfig, updateSmsConfig } from '@/api/security.js'

const loading = ref(false)
const submitting = ref(false)
const enabledChecked = ref(false)

const form = reactive({
  provider: 'aliyun',
  accessKeyId: '',
  accessKeySecret: '',
  signName: '',
  templateCode: '',
  regionId: '',
  enabled: 0,
  sendIntervalSeconds: 60,
  dailyMaxPerPhone: 20,
  dailyMaxPerIp: 100,
  globalDailyMax: 10000
})

function resetForm(data = {}) {
  form.provider = data.provider || 'aliyun'
  form.accessKeyId = data.accessKeyId || ''
  form.accessKeySecret = ''
  form.signName = data.signName || ''
  form.templateCode = data.templateCode || ''
  form.regionId = data.regionId || ''
  form.enabled = data.enabled === 1 ? 1 : 0
  enabledChecked.value = form.enabled === 1
  form.sendIntervalSeconds = parsePositiveInt(data.sendIntervalSeconds, 60)
  form.dailyMaxPerPhone = parsePositiveInt(data.dailyMaxPerPhone, 20)
  form.dailyMaxPerIp = parsePositiveInt(data.dailyMaxPerIp, 100)
  form.globalDailyMax = parsePositiveInt(data.globalDailyMax, 10000)
}

async function load() {
  loading.value = true
  try {
    const data = await getSmsConfig()
    resetForm(data || {})
  } catch (e) {
    message.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function onSubmit() {
  if (!form.accessKeyId.trim()) {
    message.warning('请填写 AccessKey ID')
    return
  }
  if (!form.signName.trim()) {
    message.warning('请填写短信签名')
    return
  }
  if (!form.templateCode.trim()) {
    message.warning('请填写短信模板 Code')
    return
  }
  if (!form.regionId.trim()) {
    message.warning('请填写 RegionId')
    return
  }

  submitting.value = true
  try {
    const payload = {
      provider: form.provider,
      accessKeyId: form.accessKeyId.trim(),
      accessKeySecret: form.accessKeySecret,
      signName: form.signName.trim(),
      templateCode: form.templateCode.trim(),
      regionId: form.regionId.trim(),
      enabled: enabledChecked.value ? 1 : 0,
      sendIntervalSeconds: form.sendIntervalSeconds,
      dailyMaxPerPhone: form.dailyMaxPerPhone,
      dailyMaxPerIp: form.dailyMaxPerIp,
      globalDailyMax: form.globalDailyMax
    }
    const data = await updateSmsConfig(payload)
    resetForm(data || {})
    message.success('保存成功')
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
.sms-config {
  max-width: 1200px;
}
</style>
