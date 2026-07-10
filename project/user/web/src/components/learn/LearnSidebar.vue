<template>
  <ul class="learn-tree">
    <LearnSidebarNode
      v-for="node in nodes"
      :key="node.id"
      :node="node"
      :depth="0"
      :active-id="activeId"
      @select="$emit('select', $event)"
    />
  </ul>
</template>

<script setup>
import LearnSidebarNode from './LearnSidebarNode.vue'

defineProps({
  nodes: { type: Array, required: true },
  activeId: { type: Number, default: null }
})
defineEmits(['select'])
</script>

<style scoped>
.learn-tree { list-style: none; margin: 0; padding: 0; }
</style>
