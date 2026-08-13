import { ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  listConfigs,
  createConfig,
  updateConfig,
  deleteConfig,
  fetchModels,
  listProviderModels,
  testConnection,
  toggleActive,
  chatTest
} from '@/api/modelConfig.js'

export function useModelConfig() {
  const configs = ref([])
  const loading = ref(false)

  const fetchConfigs = async () => {
    loading.value = true
    try {
      configs.value = await listConfigs()
    } catch (error) {
      message.error(error.message || '加载模型配置失败')
    } finally {
      loading.value = false
    }
  }

  const saveConfig = async (id, form) => {
    if (id) {
      await updateConfig(id, form)
      message.success('更新成功')
    } else {
      await createConfig(form)
      message.success('创建成功')
    }
    await fetchConfigs()
  }

  const removeConfig = async (id) => {
    await deleteConfig(id)
    message.success('删除成功')
    await fetchConfigs()
  }

  const fetchModelOptions = async (form) => {
    return await fetchModels(form)
  }

  const loadProviderModels = async (providerType) => {
    return await listProviderModels(providerType)
  }

  const testConfigConnection = async (form) => {
    const res = await testConnection(form)
    message[res.success ? 'success' : 'error'](res.success ? '连接成功' : '连接失败')
    return res.success
  }

  const toggleConfigActive = async (id, isActive) => {
    await toggleActive(id, { isActive })
    message.success(isActive ? '已启用' : '已停用')
    await fetchConfigs()
  }

  const chatTestConfig = async (payload) => {
    return await chatTest(payload)
  }

  return {
    configs,
    loading,
    fetchConfigs,
    saveConfig,
    removeConfig,
    fetchModelOptions,
    loadProviderModels,
    testConfigConnection,
    toggleConfigActive,
    chatTestConfig
  }
}
