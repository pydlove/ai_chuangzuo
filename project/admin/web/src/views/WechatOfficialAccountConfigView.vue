<template>
  <div class="wechat-config">
    <a-card :bordered="false">
      <a-page-header title="公众号配置" sub-title="配置微信公众号 AppID、AppSecret 与服务器 Token" style="padding-left: 0; padding-top: 0" />

      <a-alert
        message="使用说明"
        description="启用后，用户端可在「我的 → 绑定公众号」中扫描二维码完成绑定；Token 用于微信服务器配置校验，AppSecret 将使用 Jasypt 加密存储。"
        type="info"
        show-icon
        style="margin-bottom: 16px"
      />

      <a-spin :spinning="loading">
        <a-form layout="vertical" style="max-width: 720px" :model="form">
          <a-form-item label="公众号 AppID" required>
            <a-input v-model:value="form.appId" placeholder="请输入公众号 AppID" maxlength="64" />
          </a-form-item>

          <a-form-item label="公众号 AppSecret" required>
            <a-input-password
              v-model:value="form.appSecret"
              placeholder="留空或全为 * 号表示不修改原密钥"
              maxlength="255"
            />
          </a-form-item>

          <a-form-item label="服务器配置 Token" required>
            <a-input v-model:value="form.token" placeholder="请输入微信服务器配置 Token" maxlength="64" />
          </a-form-item>

          <a-form-item label="明文模式">
            <a-switch
              v-model:checked="plaintextModeChecked"
              checked-children="开启"
              un-checked-children="关闭"
            />
          </a-form-item>

          <a-form-item label="启用状态">
            <a-switch
              v-model:checked="enabledChecked"
              checked-children="启用"
              un-checked-children="禁用"
            />
          </a-form-item>

          <a-form-item>
            <a-space>
              <a-button type="primary" :loading="submitting" @click="onSubmit">保存</a-button>
              <a-button @click="load">重置</a-button>
              <a-button :loading="publishing" @click="onPublishMenu">发布菜单</a-button>
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
import {
  getWechatOfficialAccountConfig,
  updateWechatOfficialAccountConfig,
  publishWechatOfficialAccountMenu
} from '@/api/wechatOfficialAccountConfig.js'

const loading = ref(false)
const submitting = ref(false)
const publishing = ref(false)
const plaintextModeChecked = ref(true)
const enabledChecked = ref(false)

const form = reactive({
  appId: '',
  appSecret: '',
  token: '',
  plaintextMode: 1,
  enabled: 0
})

function resetForm(data = {}) {
  form.appId = data.appId || ''
  form.appSecret = data.appSecret || ''
  form.token = data.token || ''
  form.plaintextMode = data.plaintextMode === 0 ? 0 : 1
  form.enabled = data.enabled === 1 ? 1 : 0
  plaintextModeChecked.value = form.plaintextMode === 1
  enabledChecked.value = form.enabled === 1
}

async function load() {
  loading.value = true
  try {
    const data = await getWechatOfficialAccountConfig()
    resetForm(data || {})
  } catch (e) {
    message.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function onSubmit() {
  if (!form.appId.trim()) {
    message.warning('请填写公众号 AppID')
    return
  }
  if (!form.token.trim()) {
    message.warning('请填写服务器配置 Token')
    return
  }

  submitting.value = true
  try {
    const payload = {
      appId: form.appId.trim(),
      appSecret: form.appSecret,
      token: form.token.trim(),
      plaintextMode: plaintextModeChecked.value ? 1 : 0,
      enabled: enabledChecked.value ? 1 : 0
    }
    const data = await updateWechatOfficialAccountConfig(payload)
    resetForm(data || {})
    message.success('保存成功')
  } catch (e) {
    message.error(e?.message || '保存失败')
  } finally {
    submitting.value = false
  }
}

async function onPublishMenu() {
  publishing.value = true
  try {
    await publishWechatOfficialAccountMenu()
    message.success('菜单发布成功')
  } catch (e) {
    message.error(e?.message || '菜单发布失败')
  } finally {
    publishing.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.wechat-config {
  max-width: 1200px;
}
</style>
