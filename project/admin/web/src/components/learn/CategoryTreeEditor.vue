<template>
  <div class="category-editor">
    <a-card title="分类管理" :bordered="false">
      <template #extra>
        <a-button type="primary" @click="onCreateRoot">新增顶级分类</a-button>
      </template>
      <a-tree
        :tree-data="workingTree"
        :field-names="{ title: 'name', key: 'id', children: 'children' }"
        block-node
        draggable
        :default-expand-all="true"
        @drop="onDrop"
      >
        <template #title="{ dataRef }">
          <span>{{ dataRef.name }}</span>
          <span class="row-actions">
            <a @click.stop="onAddChild(dataRef)">+ 子分类</a>
            <a-divider type="vertical" />
            <a @click.stop="onEdit(dataRef)">编辑</a>
            <a-divider type="vertical" />
            <a-popconfirm
              title="确认删除该分类？子分类或文章非空时会拒绝"
              @confirm="onDelete(dataRef)"
            >
              <a class="danger" @click.stop>删除</a>
            </a-popconfirm>
          </span>
        </template>
      </a-tree>
    </a-card>

    <a-modal
      v-model:open="modalOpen"
      :title="editing ? '编辑分类' : '新增分类'"
      :confirm-loading="submitting"
      @ok="onSubmit"
    >
      <a-form layout="vertical" :model="form">
        <a-form-item label="父分类">
          <a-tree-select
            v-model:value="form.parentId"
            :tree-data="parentOptions"
            :field-names="{ label: 'name', value: 'id', children: 'children' }"
            :tree-default-expand-all="true"
            allow-clear
            placeholder="（顶级分类）"
          />
        </a-form-item>
        <a-form-item label="名称" required>
          <a-input v-model:value="form.name" maxlength="64" />
        </a-form-item>
        <a-form-item label="排序值">
          <a-input-number v-model:value="form.sort" :min="0" style="width: 100%" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  fetchCategoryTree,
  createCategory,
  updateCategory,
  deleteCategory,
  sortCategory
} from '@/api/learn'

const treeData = ref([])
const workingTree = ref([]) // 本地可变副本，用于拖拽
const originalIndex = ref(new Map()) // id -> { parentId, sort }
const modalOpen = ref(false)
const submitting = ref(false)
const editing = ref(null)
const form = reactive({ parentId: null, name: '', sort: 0 })

const parentOptions = computed(() => [{ id: null, name: '（顶级）', children: [] }, ...workingTree.value])

async function load() {
  const tree = await fetchCategoryTree()
  treeData.value = tree
  workingTree.value = cloneTree(tree)
  originalIndex.value = indexTree(workingTree.value)
}

function cloneTree(nodes) {
  return (nodes || []).map((n) => ({ ...n, children: cloneTree(n.children) }))
}

function indexTree(nodes, parentId = null, sort = 0) {
  const map = new Map()
  function walk(list, pId, baseSort) {
    (list || []).forEach((n, i) => {
      map.set(n.id, { parentId: pId, sort: baseSort + i })
      if (n.children?.length) walk(n.children, n.id, 0)
    })
  }
  walk(nodes, parentId, sort)
  return map
}

// ---------- 拖拽排序 ----------
function findNode(list, id) {
  for (const n of list || []) {
    if (n.id === id) return n
    if (n.children?.length) {
      const f = findNode(n.children, id)
      if (f) return f
    }
  }
  return null
}
function findParent(list, id) {
  for (const n of list || []) {
    if (n.children?.some((c) => c.id === id)) return n
    if (n.children?.length) {
      const f = findParent(n.children, id)
      if (f) return f
    }
  }
  return null // null = 顶级
}
function removeNode(list, id) {
  for (let i = 0; i < list.length; i++) {
    if (list[i].id === id) { list.splice(i, 1); return true }
    if (list[i].children?.length && removeNode(list[i].children, id)) return true
  }
  return false
}

const dropping = ref(false)
async function onDrop(info) {
  if (dropping.value) return
  const dragKey = info.dragNode.key
  const dropKey = info.dropNode.key
  if (dragKey === dropKey) return

  const dragNode = findNode(workingTree.value, dragKey)
  if (!dragNode) return
  const dragOrig = originalIndex.value.get(dragKey) || { parentId: null }

  // 确定目标父级与位置
  // dropToGap=true 且 dropPosition=-1 → 放到目标节点之前（同父级）
  // dropToGap=true 且 dropPosition=1  → 放到目标节点之后（同父级）
  // dropToGap=false                    → 放到目标节点内部（成为其子）
  let targetParent = null
  let insertIndex = 0
  const dropParent = findParent(workingTree.value, dropKey) // null=顶级

  if (info.dropToGap) {
    targetParent = dropParent
    const siblings = targetParent ? targetParent.children : workingTree.value
    insertIndex = info.dropPosition === -1
      ? siblings.findIndex((n) => n.id === dropKey)
      : siblings.findIndex((n) => n.id === dropKey) + 1
  } else {
    const dropNode = findNode(workingTree.value, dropKey)
    targetParent = dropNode
    insertIndex = (dropNode.children || []).length // append 到末尾
  }

  // 禁止把节点拖到自己的后代里（会成环）
  if (targetParent && isDescendant(dragNode, targetParent.id)) {
    message.warning('不能将分类移动到其子分类下')
    return
  }

  // 从原位置移除
  removeNode(workingTree.value, dragKey)
  // 插入新位置
  const siblings = targetParent ? (targetParent.children = targetParent.children || []) : workingTree.value
  siblings.splice(insertIndex, 0, dragNode)

  // 构造变更项：与 originalIndex 对比，sort 或 parentId 变化的才提交
  const newIndex = indexTree(workingTree.value)
  const items = []
  for (const [id, cur] of newIndex.entries()) {
    const orig = originalIndex.value.get(id)
    if (!orig) continue
    if (orig.parentId !== cur.parentId || orig.sort !== cur.sort) {
      items.push({ id, sort: cur.sort, parentId: cur.parentId })
    }
  }

  if (!items.length) return

  dropping.value = true
  try {
    await sortCategory(items)
    message.success('排序已更新')
    await load()
  } catch (e) {
    message.error(e?.message || '排序失败')
    await load() // 失败时回滚到服务端状态
  } finally {
    dropping.value = false
  }
}

function isDescendant(node, ancestorId) {
  for (const c of node.children || []) {
    if (c.id === ancestorId) return true
    if (isDescendant(c, ancestorId)) return true
  }
  return false
}

function onCreateRoot() {
  editing.value = null
  form.parentId = null; form.name = ''; form.sort = 0
  modalOpen.value = true
}
function onAddChild(node) {
  editing.value = null
  form.parentId = node.id; form.name = ''; form.sort = 0
  modalOpen.value = true
}
function onEdit(node) {
  editing.value = node
  form.parentId = node.parentId; form.name = node.name; form.sort = node.sort
  modalOpen.value = true
}
async function onDelete(node) {
  try {
    await deleteCategory(node.id)
    message.success('已删除')
    await load()
  } catch (e) {
    message.error(e?.message || '删除失败（分类下可能仍有子分类或文章）')
  }
}
async function onSubmit() {
  if (!form.name.trim()) { message.error('名称不能为空'); return }
  submitting.value = true
  try {
    if (editing.value) {
      await updateCategory(editing.value.id, form)
    } else {
      await createCategory(form)
    }
    message.success('已保存')
    modalOpen.value = false
    await load()
  } catch (e) {
    message.error(e?.message || '保存失败')
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.category-editor { padding: 0; }
.row-actions { margin-left: 12px; opacity: 0.7; font-size: 12px; }
.row-actions a { color: #ff2442; }
.row-actions a.danger { color: #ff4d4f; }
</style>
