<template>
  <div class="template-access-editor">
    <div class="template-access-actions">
      <a-button size="small" @click="selectAll">全选</a-button>
      <a-button size="small" @click="selectNone">清空</a-button>
      <span class="template-access-count">已选 {{ selectedKeys.length }} / {{ templates.length }}</span>
    </div>
    <a-select
      mode="multiple"
      :value="selectedKeys"
      :options="selectOptions"
      placeholder="选择该套餐可访问的导出模板"
      allow-clear
      :max-tag-count="6"
      style="width: 100%"
      @change="onChange"
    />
    <div class="template-access-hint">
      当前仅提供系统预设的导出模板，暂不支持自定义导出模板。可访问范围由所购套餐决定。
    </div>
  </div>
</template>

<script setup>
import { computed, h } from 'vue'

const props = defineProps({
  /** 逗号分隔的 template_key 列表 */
  value: { type: String, default: '' },
  templates: { type: Array, default: () => [] }
})
const emit = defineEmits(['change'])

const selectedKeys = computed(() => {
  if (!props.value) return []
  return props.value.split(',').map((s) => s.trim()).filter(Boolean)
})

const selectOptions = computed(() => {
  // 按 platform 分组，render label 含分组感
  return props.templates.map((t) => ({
    value: t.templateKey,
    label: `${t.name}（${t.platform}）`
  }))
})

function onChange(keys) {
  emit('change', keys.join(','))
}

function selectAll() {
  emit('change', props.templates.map((t) => t.templateKey).join(','))
}

function selectNone() {
  emit('change', '')
}
</script>

<style scoped>
.template-access-editor {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.template-access-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.template-access-count {
  font-size: 12px;
  color: #595959;
  margin-left: auto;
}

.template-access-hint {
  font-size: 11px;
  color: #8c8c8c;
  line-height: 1.5;
}
</style>
