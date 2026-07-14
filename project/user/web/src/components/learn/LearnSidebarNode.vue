<template>
  <li class="learn-tree-node">
    <div
      :class="['learn-tree-row', {
        active: node.id === activeId,
        expandable: hasChildren,
        'top-level': depth === 0,
        'child-level': depth > 0
      }]"
      :style="{ paddingLeft: `${depth * 16 + 12}px` }"
      @click="onClick"
    >
      <span v-if="hasChildren" class="learn-tree-caret">{{ open ? '−' : '+' }}</span>
      <span v-else class="learn-tree-caret-spacer"></span>
      <span class="learn-tree-label">{{ node.name }}</span>
    </div>
    <ul v-if="open && hasChildren" class="learn-tree-children">
      <LearnSidebarNode
        v-for="child in node.children"
        :key="child.id"
        :node="child"
        :depth="depth + 1"
        :active-id="activeId"
        @select="(id) => $emit('select', id)"
      />
    </ul>
  </li>
</template>

<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  node: { type: Object, required: true },
  depth: { type: Number, default: 0 },
  activeId: { type: Number, default: null }
})
const emit = defineEmits(['select'])

const open = ref(props.depth < 1)
const hasChildren = computed(() => Array.isArray(props.node.children) && props.node.children.length > 0)

function onClick() {
  if (hasChildren.value) {
    open.value = !open.value
  }
  emit('select', props.node.id)
}
</script>

<style scoped>
.learn-tree-row {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 12px; cursor: pointer; user-select: none;
  font-size: 14px; color: #262626;
  border-left: 3px solid transparent;
}
.learn-tree-row:hover { background: #FFF5F7; }
.learn-tree-row.active {
  background: #FFF5F7; color: #FF2442; font-weight: 600;
  border-left-color: #FF2442;
}
/* 顶级分类：加粗 + 稍大 + 更深色，形成层级对比 */
.learn-tree-row.top-level {
  font-weight: 600;
  font-size: 15px;
  color: #1a1a1a;
  padding-top: 10px;
  padding-bottom: 10px;
}
/* 子级分类：常规字重 + 稍小 + 稍浅色，弱化为从属 */
.learn-tree-row.child-level {
  font-weight: 400;
  font-size: 13px;
  color: #595959;
}
/* 子级 active 时仍突出（红色 + 加粗），不被 child-level 覆盖 */
.learn-tree-row.child-level.active {
  color: #FF2442;
  font-weight: 600;
}
.learn-tree-caret {
  width: 14px; text-align: center; font-size: 12px; color: #999;
}
.learn-tree-caret-spacer { width: 14px; display: inline-block; }
.learn-tree-children { list-style: none; padding: 0; margin: 0; }
</style>
