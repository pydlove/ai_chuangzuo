import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { copyToClipboard } from '@/utils/copy.js'

/**
 * 通用复制逻辑封装
 * @param {Object} options
 * @param {string} options.successText 复制成功提示
 * @param {string} options.errorText   复制失败提示
 * @param {string} options.emptyText   复制内容为空提示
 * @returns {{ loading: Ref<boolean>, copy: (text: string | (() => string | Promise<string>)) => Promise<void> }}
 */
export function useCopy(options = {}) {
  const {
    successText = '已复制',
    errorText = '复制失败',
    emptyText = '复制内容为空'
  } = options

  const loading = ref(false)

  const copy = async (text) => {
    let resolved = text

    if (typeof resolved === 'function') {
      loading.value = true
      try {
        resolved = await resolved()
      } finally {
        loading.value = false
      }
    }

    if (!resolved) {
      message.warning(emptyText)
      throw new Error('empty')
    }

    try {
      await copyToClipboard(resolved)
      message.success(successText)
    } catch (e) {
      message.error(errorText)
      throw e
    }
  }

  return {
    loading,
    copy
  }
}
