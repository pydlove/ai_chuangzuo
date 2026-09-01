<template>
  <div class="payment-config">
    <a-card :bordered="false">
      <a-page-header title="支付设置" sub-title="配置虎皮椒（Xunhupay）支付参数与模式" style="padding-left: 0; padding-top: 0" />

      <a-alert
        message="使用说明"
        description="测试模式开启时，用户端订阅输入支付码 123456 即可立即成功；关闭测试模式后，系统将调用真实虎皮椒网关发起支付。"
        type="info"
        show-icon
        style="margin-bottom: 16px"
      />

      <a-spin :spinning="loading">
        <a-form layout="vertical" style="max-width: 720px" :model="form">
          <a-form-item label="支付服务商">
            <a-input value="虎皮椒（Xunhupay）" disabled />
          </a-form-item>

          <a-form-item label="App ID" required>
            <a-input v-model:value="form.appId" placeholder="请输入虎皮椒 App ID" maxlength="255" />
          </a-form-item>

          <a-form-item label="App Secret" required>
            <a-input-password
              v-model:value="form.appSecret"
              placeholder="留空或全为 * 号表示不修改原密钥"
              maxlength="512"
            />
          </a-form-item>

          <a-form-item label="网关地址" required>
            <a-input v-model:value="form.gatewayUrl" placeholder="例如：https://api.xunhupay.com/payment/do.html" maxlength="512" />
          </a-form-item>

          <a-form-item label="退款网关地址">
            <a-input v-model:value="form.refundUrl" placeholder="例如：https://api.xunhupay.com/payment/refund.html" maxlength="512" />
          </a-form-item>

          <a-form-item label="异步通知地址（notify_url）">
            <a-input v-model:value="form.notifyUrl" placeholder="例如：https://your-domain.com/api/v1/public/payment/xunhupay/notify" maxlength="512" />
          </a-form-item>

          <a-form-item label="支付回跳地址（return_url）">
            <a-input v-model:value="form.returnUrl" placeholder="例如：https://your-domain.com/pricing" maxlength="512" />
          </a-form-item>

          <a-form-item label="测试模式">
            <a-switch
              v-model:checked="testModeChecked"
              checked-children="开启"
              un-checked-children="关闭"
            />
          </a-form-item>

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
import { getPaymentConfig, updatePaymentConfig } from '@/api/paymentConfig.js'

const loading = ref(false)
const submitting = ref(false)
const testModeChecked = ref(true)

const form = reactive({
  provider: 'xunhupay',
  appId: '',
  appSecret: '',
  gatewayUrl: 'https://api.xunhupay.com/payment/do.html',
  refundUrl: 'https://api.xunhupay.com/payment/refund.html',
  notifyUrl: '',
  returnUrl: '',
  testMode: 1
})

function resetForm(data = {}) {
  form.provider = data.provider || 'xunhupay'
  form.appId = data.appId || ''
  form.appSecret = data.appSecret || ''
  form.gatewayUrl = data.gatewayUrl || 'https://api.xunhupay.com/payment/do.html'
  form.refundUrl = data.refundUrl || 'https://api.xunhupay.com/payment/refund.html'
  form.notifyUrl = data.notifyUrl || ''
  form.returnUrl = data.returnUrl || ''
  form.testMode = data.testMode === 0 ? 0 : 1
  testModeChecked.value = form.testMode === 1
}

async function load() {
  loading.value = true
  try {
    const data = await getPaymentConfig()
    resetForm(data || {})
  } catch (e) {
    message.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function onSubmit() {
  if (!form.appId.trim()) {
    message.warning('请填写 App ID')
    return
  }
  if (!form.gatewayUrl.trim()) {
    message.warning('请填写网关地址')
    return
  }

  submitting.value = true
  try {
    const payload = {
      provider: form.provider,
      appId: form.appId.trim(),
      appSecret: form.appSecret,
      gatewayUrl: form.gatewayUrl.trim(),
      refundUrl: form.refundUrl?.trim() || 'https://api.xunhupay.com/payment/refund.html',
      notifyUrl: form.notifyUrl?.trim() || '',
      returnUrl: form.returnUrl?.trim() || '',
      testMode: testModeChecked.value ? 1 : 0
    }
    const data = await updatePaymentConfig(payload)
    resetForm(data || {})
    message.success('保存成功')
  } catch (e) {
    message.error(e?.message || '保存失败')
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.payment-config {
  max-width: 1200px;
}
</style>
