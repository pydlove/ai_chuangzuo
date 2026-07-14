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
      <span v-if="hasChildren" class="learn-tree-caret">{{ open ? '∨' : '›' }}</span>
      <span v-else class="learn-tree-caret-spacer"></span>
      <component
        v-if="depth === 0 && iconComponent"
        :is="iconComponent"
        class="learn-tree-icon"
      />
      <span v-if="node.id === activeId" class="learn-tree-dot"></span>
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
import { CATEGORY_ICONS } from './learnCategoryIcons'

const props = defineProps({
  node: { type: Object, required: true },
  depth: { type: Number, default: 0 },
  activeId: { type: Number, default: null }
})
const emit = defineEmits(['select'])

const open = ref(props.depth < 1)
const hasChildren = computed(() => Array.isArray(props.node.children) && props.node.children.length > 0)
const iconComponent = computed(() => CATEGORY_ICONS[props.node.name] || null)

function onClick() {
  if (hasChildren.value) {
    // 有子分类的节点只做展开/折叠，不触发分类跳转
    open.value = !open.value
  } else {
    emit('select', props.node.id)
  }
}
</script>

<style scoped>
.learn-tree-row {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 12px; cursor: pointer; user-select: none;
  font-size: 14px; color: #262626;
  border-left: 3px solid transparent;
}
.learn-tree-row:hover { background: #FFF5F7; border-left-color: #ffb3c1; }
.learn-tree-row.active {
  background: #FFF5F7; color: #FF2442; font-weight: 600;
  border-left-color: #FF2442;
}
/* 顶级分类：加粗 + 稍大 + 更深色 */
.learn-tree-row.top-level {
  font-weight: 600;
  font-size: 15px;
  color: #1a1a1a;
  padding-top: 10px;
  padding-bottom: 10px;
}
/* 顶级分类之间间距 */
.learn-tree-node + .learn-tree-node > .learn-tree-row.top-level {
  margin-top: 4px;
}
/* 子级分类：常规字重 + 稍小 + 稍浅色 + 层级竖线 */
.learn-tree-row.child-level {
  font-weight: 400;
  font-size: 13px;
  color: #595959;
  border-left: 3px solid #f0f0f0;
}
.learn-tree-row.child-level:hover { border-left-color: #ffb3c1; }
.learn-tree-row.child-level.active {
  color: #FF2442;
  font-weight: 600;
  border-left-color: #FF2442;
}
.learn-tree-caret {
  width: 14px; text-align: center; font-size: 12px; color: #bfbfbf;
  flex-shrink: 0;
}
.learn-tree-caret-spacer { width: 14px; display: inline-block; flex-shrink: 0; }
.learn-tree-icon {
  width: 16px; height: 16px; font-size: 16px; color: #8c8c8c;
  flex-shrink: 0;
}
.learn-tree-row:hover .learn-tree-icon,
.learn-tree-row.active .learn-tree-icon { color: #FF2442; }
.learn-tree-dot {
  width: 4px; height: 4px; border-radius: 50%;
  background: #FF2442; flex-shrink: 0;
}
.learn-tree-children { list-style: none; padding: 0; margin: 0; }

/* 暗色主题 */
body[data-theme="dark"] .learn-tree-row { color: #d0d0d0; }
body[data-theme="dark"] .learn-tree-row.top-level { color: #f0f0f0; }
body[data-theme="dark"] .learn-tree-row.child-level { color: #b0b0b0; }
body[data-theme="dark"] .learn-tree-row:hover { background: #2a1f22; border-left-color: #5a2a30; }
body[data-theme="dark"] .learn-tree-row.active { background: #2a1f22; color: #FF2442; }
body[data-theme="dark"] .learn-tree-row.child-level:hover { border-left-color: #5a2a30; }
body[data-theme="dark"] .learn-tree-caret { color: #595959; }
body[data-theme="dark"] .learn-tree-icon { color: #8c8c8c; }
body[data-theme="dark"] .learn-tree-row:hover .learn-tree-icon,
body[data-theme="dark"] .learn-tree-row.active .learn-tree-icon { color: #FF2442; }
</style>
