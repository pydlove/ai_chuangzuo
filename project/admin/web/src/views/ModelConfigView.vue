<template>
  <div class="model-config">
    <h3 class="page-title">模型配置</h3>
    <p class="page-desc">配置 AI 大模型厂商接入参数，全局仅可启用一个配置。</p>

    <a-spin :spinning="loading">
      <a-row :gutter="[16, 16]">
        <a-col
          v-for="provider in providers"
          :key="provider.providerType"
          :xs="24"
          :lg="12"
        >
          <a-card :title="provider.providerName" class="config-card">
            <template #extra>
              <a-tag v-if="provider.isActive" color="green">已启用</a-tag>
              <a-tag v-else color="default">未启用</a-tag>
            </template>

            <a-form layout="vertical">
              <a-form-item label="Base URL">
                <a-input
                  v-model:value="forms[provider.providerType].baseUrl"
                  placeholder="https://api.moonshot.cn"
                />
              </a-form-item>

              <a-form-item label="API Key">
                <a-input-password
                  v-model:value="forms[provider.providerType].apiKey"
                  placeholder="sk-..."
                />
              </a-form-item>

              <a-form-item label="模型">
                <a-select
                  v-model:value="forms[provider.providerType].modelCode"
                  :options="modelOptions[provider.providerType]"
                  placeholder="请选择或获取模型"
                  allow-clear
                  show-search
                />
              </a-form-item>

              <a-form-item>
                <a-space wrap>
                  <a-button @click="handleFetchModels(provider.providerType)">
                    获取模型
                  </a-button>
                  <a-button @click="handleTestConnection(provider.providerType)">
                    测试连接
                  </a-button>
                  <a-button type="primary" @click="handleSave(provider.providerType)">
                    保存
                  </a-button>
                  <a-button
                    v-if="provider.isActive"
                    @click="handleToggle(provider.providerType, 0)"
                  >
                    停用
                  </a-button>
                  <a-button
                    v-else
                    type="primary"
                    ghost
                    @click="handleToggle(provider.providerType, 1)"
                  >
                    启用
                  </a-button>
                  <a-popconfirm
                    title="确定删除该配置？"
                    ok-text="确认"
                    cancel-text="取消"
                    @confirm="handleDelete(provider.providerType)"
                  >
                    <a-button danger>删除</a-button>
                  </a-popconfirm>
                </a-space>
              </a-form-item>
            </a-form>
          </a-card>
        </a-col>
      </a-row>
    </a-spin>
  </div>
</template>

<script setup>
import { onMounted, reactive } from 'vue'
import { message } from 'ant-design-vue'
import { useModelConfig } from '@/composables/useModelConfig.js'

const {
  providers,
  loading,
  fetchProviders,
  saveProvider,
  removeProvider,
  fetchModelOptions,
  testProviderConnection,
  toggleProviderActive
} = useModelConfig()

const forms = reactive({})
const modelOptions = reactive({})

const initForms = () => {
  providers.value.forEach((p) => {
    if (!forms[p.providerType]) {
      forms[p.providerType] = {
        baseUrl: p.baseUrl || '',
        apiKey: '',
        modelCode: p.modelCode || '',
        modelName: p.modelName || '',
        isActive: p.isActive
      }
    }
    if (!modelOptions[p.providerType]) {
      modelOptions[p.providerType] = []
    }
  })
}

const syncFormFromProvider = (providerType) => {
  const updated = providers.value.find((p) => p.providerType === providerType)
  if (!updated) return
  forms[providerType] = {
    baseUrl: updated.baseUrl || '',
    apiKey: '',
    modelCode: updated.modelCode || '',
    modelName: updated.modelName || '',
    isActive: updated.isActive
  }
}

const handleFetchModels = async (providerType) => {
  const form = forms[providerType]
  if (!form.baseUrl || !form.apiKey) {
    message.warning('请先填写 Base URL 和 API Key')
    return
  }
  try {
    const options = await fetchModelOptions(providerType, {
      baseUrl: form.baseUrl,
      apiKey: form.apiKey
    })
    modelOptions[providerType] = options.map((o) => ({
      label: o.modelName,
      value: o.modelCode
    }))
    message.success('获取模型成功')
  } catch (error) {
    // composable 已提示错误
  }
}

const handleTestConnection = async (providerType) => {
  const form = forms[providerType]
  if (!form.baseUrl || !form.apiKey) {
    message.warning('请先填写 Base URL 和 API Key')
    return
  }
  try {
    await testProviderConnection(providerType, {
      baseUrl: form.baseUrl,
      apiKey: form.apiKey
    })
  } catch (error) {
    // composable 已提示错误
  }
}

const handleSave = async (providerType) => {
  const form = forms[providerType]
  const payload = {
    baseUrl: form.baseUrl,
    modelCode: form.modelCode,
    modelName: form.modelName,
    isActive: form.isActive
  }
  if (form.apiKey) {
    payload.apiKey = form.apiKey
  }
  await saveProvider(providerType, payload)
  syncFormFromProvider(providerType)
}

const handleToggle = async (providerType, isActive) => {
  await toggleProviderActive(providerType, isActive)
  syncFormFromProvider(providerType)
}

const handleDelete = async (providerType) => {
  await removeProvider(providerType)
  syncFormFromProvider(providerType)
}

onMounted(async () => {
  await fetchProviders()
  initForms()
})
</script>

<style scoped>
.page-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 4px;
}

.page-desc {
  color: #8c8c8c;
  margin: 0 0 16px;
}

.config-card {
  border-radius: 8px;
}
</style>
