import { ref } from 'vue'
import storage from '@/utils/storage.js'

const THEME_KEY = 'aichuangzuo_admin_theme'

export function useTheme() {
  const currentTheme = ref('light')

  const loadTheme = () => {
    const saved = storage.get(THEME_KEY) || 'light'
    currentTheme.value = saved
    document.body.setAttribute('data-theme', saved)
  }

  const toggleTheme = () => {
    const next = currentTheme.value === 'light' ? 'dark' : 'light'
    currentTheme.value = next
    document.body.setAttribute('data-theme', next)
    storage.set(THEME_KEY, next)
  }

  return {
    currentTheme,
    loadTheme,
    toggleTheme
  }
}
