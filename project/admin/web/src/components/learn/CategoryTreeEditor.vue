<template>
  <div class="category-editor">
    <a-card title="分类管理" :bordered="false">
      <template #extra>
        <a-button type="primary" @click="onCreateRoot">新增顶级分类</a-button>
      </template>
      <a-tree
        :tree-data="treeData"
        :field-names="{ title: 'name', key: 'id', children: 'children' }"
        block-node
        :default-expand-all="true"
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
  deleteCategory
} from '@/api/learn'

const treeData = ref([])
const modalOpen = ref(false)
const submitting = ref(false)
const editing = ref(null)
const form = reactive({ parentId: null, name: '', sort: 0 })

const parentOptions = computed(() => [{ id: null, name: '（顶级）', children: [] }, ...treeData.value])

async function load() {
  treeData.value = await fetchCategoryTree()
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
