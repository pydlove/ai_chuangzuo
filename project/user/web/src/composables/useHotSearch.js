import { ref, computed } from 'vue'
import { getHotSearchPlatforms, getHotSearchList } from '@/api/hotSearch'
import { message } from 'ant-design-vue'

const platforms = ref([])
const loading = ref(false)

function errMsg(e) {
  if (!e) return '请求失败'
  if (typeof e === 'string') return e
  return e.message || e.msg || '请求失败'
}

export function useHotSearch() {
  const list = ref([])
  const platformsLoading = ref(false)

  const loadPlatforms = async () => {
    if (platforms.value.length) return
    platformsLoading.value = true
    try {
      platforms.value = await getHotSearchPlatforms()
    } catch (e) {
      message.error(errMsg(e))
    } finally {
      platformsLoading.value = false
    }
  }

  const loadList = async (platform, date) => {
    loading.value = true
    try {
      list.value = await getHotSearchList(platform, date)
    } catch (e) {
      message.error(errMsg(e))
      list.value = []
    } finally {
      loading.value = false
    }
  }

  const platformOptions = computed(() => platforms.value)

  return {
    platforms,
    platformOptions,
    platformsLoading,
    list,
    loading,
    loadPlatforms,
    loadList
  }
}
