<template>
  <div class="ml-tree-node" :style="{ paddingLeft: depth * 16 + 'px' }">
    <button
      class="ml-tree-node__header"
      :class="{ leaf: !node.children?.length }"
      @click="handleClick"
    >
      <span class="ml-tree-node__name">{{ node.name }}</span>
      <span v-if="node.children?.length" class="ml-tree-node__icon" :class="{ expanded: isExpanded }"></span>
    </button>
    <div v-if="node.children?.length && isExpanded" class="ml-tree-node__children">
      <MobileLearnNode
        v-for="child in node.children"
        :key="child.id"
        :node="child"
        :depth="depth + 1"
        :expanded-ids="expandedIds"
        @toggle="$emit('toggle', $event)"
        @select="$emit('select', $event)"
      />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  node: { type: Object, required: true },
  depth: { type: Number, default: 0 },
  expandedIds: { type: Set, required: true }
})

const emit = defineEmits(['toggle', 'select'])

const isExpanded = computed(() => props.expandedIds.has(props.node.id))

function handleClick() {
  if (props.node.children?.length) {
    emit('toggle', props.node.id)
  } else {
    emit('select', props.node.id)
  }
}
</script>

<style scoped>
.ml-tree-node__header {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 12px 16px;
  background: transparent;
  border: none;
  font-size: 14px;
  color: #1a1a1a;
  text-align: left;
  cursor: pointer;
}
.ml-tree-node__header.leaf {
  color: #595959;
}
.ml-tree-node__name {
  line-height: 1.45;
}
.ml-tree-node__icon {
  width: 18px;
  height: 18px;
  position: relative;
  flex-shrink: 0;
}
.ml-tree-node__icon::before,
.ml-tree-node__icon::after {
  content: '';
  position: absolute;
  background: #FF2442;
  border-radius: 1px;
  transition: transform 0.2s ease;
}
.ml-tree-node__icon::before {
  width: 10px;
  height: 2px;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}
.ml-tree-node__icon::after {
  width: 2px;
  height: 10px;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}
.ml-tree-node__icon.expanded::after {
  transform: translate(-50%, -50%) rotate(90deg);
}
.ml-tree-node__children {
  border-left: 1px solid #f0f0f0;
  margin-left: 16px;
}

body[data-theme="dark"] .ml-tree-node__header {
  color: #e0e0e0;
}
body[data-theme="dark"] .ml-tree-node__header.leaf {
  color: #a6a6a6;
}
body[data-theme="dark"] .ml-tree-node__children {
  border-left-color: #2a2a2a;
}
</style>
