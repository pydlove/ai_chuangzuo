import { ref } from 'vue'
import { fetchPlatforms } from '@/api/platform.js'

/**
 * 用户端自媒体平台配置（制定自媒体方案第一步）。
 * 平台列表由管理端配置，用户端登录后从 /api/v1/user/platforms 读取。
 */
export const platforms = ref([])

export async function loadPlatforms() {
  try {
    const list = await fetchPlatforms()
    platforms.value = (list || []).map((p) => ({
      id: p.id,
      key: p.platformKey,
      name: p.platformName,
      desc: p.description || '',
      recommendWords: p.recommendWords || 0,
      trait: p.trait || '',
      isDefault: p.isDefault === 1,
      iconUrl: p.iconUrl || '',
      wordCountPresets: p.wordCountPresets || [],
      tagline: p.tagline || '',
      contentForm: p.contentForm || [],
      monetization: p.monetization || [],
      threshold: p.threshold || '',
      bestFor: p.bestFor || '',
      reason: p.reason || '',
      monetizationEase: p.monetizationEase || '',
      timeToIncome: p.timeToIncome || '',
      incomeRange: p.incomeRange || '',
      difficulty: p.difficulty || ''
    }))
  } catch (e) {
    console.warn('加载平台配置失败', e)
    platforms.value = []
  }
}
