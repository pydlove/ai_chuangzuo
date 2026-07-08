import { computed, ref } from 'vue'

const MAX_SCOPE_TAGS = 3
const MAX_SCOPE_TAG_LENGTH = 8

const parseScopeTags = (scopeStr) => {
  if (!scopeStr) return []
  return scopeStr.split(/[,，]/).map((t) => t.trim()).filter(Boolean)
}

const formatScopeTags = (tags) => tags.join(',')

const validateScopeTags = (tags) => {
  if (tags.length > MAX_SCOPE_TAGS) {
    return `适用范围最多 ${MAX_SCOPE_TAGS} 个标签`
  }
  for (const tag of tags) {
    if (tag.length > MAX_SCOPE_TAG_LENGTH) {
      return `单个标签不超过 ${MAX_SCOPE_TAG_LENGTH} 个字`
    }
  }
  return ''
}

/**
 * 适用范围（scope）标签输入的复合式 composable。
 *
 * 用法（推荐）：
 *   const scopeRef = ref('')
 *   const { scopeInput, scopeTags, scopeError, addTag, removeTag } = useScopeTags(scopeRef)
 *   scopeRef.value             // 整段 scope 字符串（v-model 到 form 字段）
 *   scopeTags.value             // 当前标签数组（用于渲染 chips）
 *   addTag()                    // 用户回车时调用
 *   removeTag(tag)              // 标签关闭时调用
 *
 * scope 是可选字段；当 tags 为空时不报错，仅超过 MAX_SCOPE_TAGS 才提示。
 */
export const useScopeTags = (scopeRef) => {
  const scopeInput = ref('')

  const scopeTags = computed(() => parseScopeTags(scopeRef?.value || ''))
  const scopeError = computed(() => validateScopeTags(scopeTags.value))

  const addTag = () => {
    if (!scopeRef) return
    const raw = scopeInput.value.trim()
    if (!raw) return
    const tags = parseScopeTags(scopeRef.value)
    if (tags.length >= MAX_SCOPE_TAGS) {
      scopeInput.value = ''
      return
    }
    const incoming = raw.split(/[,，]/).map((t) => t.trim()).filter(Boolean)
    for (const tag of incoming) {
      if (tags.length >= MAX_SCOPE_TAGS) break
      if (!tags.includes(tag)) {
        tags.push(tag)
      }
    }
    scopeRef.value = formatScopeTags(tags)
    scopeInput.value = ''
  }

  const removeTag = (tag) => {
    if (!scopeRef) return
    scopeRef.value = formatScopeTags(
      parseScopeTags(scopeRef.value).filter((t) => t !== tag)
    )
  }

  return {
    MAX_SCOPE_TAGS,
    MAX_SCOPE_TAG_LENGTH,
    scopeInput,
    scopeTags,
    scopeError,
    addTag,
    removeTag
  }
}