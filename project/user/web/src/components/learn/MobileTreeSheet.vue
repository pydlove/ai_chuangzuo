<template>
  <a-drawer
    v-model:open="openModel"
    title="分类"
    placement="bottom"
    :height="'70vh'"
    :closable="true"
  >
    <LearnSidebar
      :nodes="nodes"
      :active-id="activeId"
      @select="onSelect"
    />
  </a-drawer>
</template>

<script setup>
import { computed } from 'vue'
import LearnSidebar from './LearnSidebar.vue'

const props = defineProps({
  open: { type: Boolean, required: true },
  nodes: { type: Array, required: true },
  activeId: { type: Number, default: null }
})
const emit = defineEmits(['update:open', 'select'])

const openModel = computed({
  get() { return props.open },
  set(v) { emit('update:open', v) }
})

function onSelect(id) {
  emit('select', id)
  openModel.value = false
}
</script>
