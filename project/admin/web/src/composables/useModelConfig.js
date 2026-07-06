import { ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  listConfigs,
  saveConfig,
  deleteConfig,
  fetchModels,
  testConnection,
  toggleActive
} from '@/api/modelConfig.js'

export function useModelConfig() {
  const providers = ref([])
  const loading = ref(false)

  const fetchProviders = async () => {
    loading.value = true
    try {
      providers.value = await listConfigs()
    } catch (error) {
      message.error(error.message || '加载模型配置失败')
    } finally {
      loading.value = false
    }
  }

  const saveProvider = async (providerType, form) => {
    await saveConfig(providerType, form)
    message.success('保存成功')
    await fetchProviders()
  }

  const removeProvider = async (providerType) => {
    await deleteConfig(providerType)
    message.success('删除成功')
    await fetchProviders()
  }

  const fetchModelOptions = async (providerType, form) => {
    return await fetchModels(providerType, form)
  }

  const testProviderConnection = async (providerType, form) => {
    const res = await testConnection(providerType, form)
    message[res.success ? 'success' : 'error'](res.success ? '连接成功' : '连接失败')
    return res.success
  }

  const toggleProviderActive = async (providerType, isActive) => {
    await toggleActive(providerType, { isActive })
    message.success(isActive ? '已启用' : '已停用')
    await fetchProviders()
  }

  return {
    providers,
    loading,
    fetchProviders,
    saveProvider,
    removeProvider,
    fetchModelOptions,
    testProviderConnection,
    toggleProviderActive
  }
}
