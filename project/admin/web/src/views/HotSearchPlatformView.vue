<template>
  <div class="hot-search-platforms">
    <a-card title="热搜平台管理">
      <template #extra>
        <a-button type="primary" @click="openCreate">新增平台</a-button>
      </template>
      <a-table
        :data-source="state.platforms"
        :columns="columns"
        :loading="state.loading"
        row-key="id"
        :pagination="{ pageSize: 20 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'enabled'">
            <a-tag :color="record.enabled === 1 ? 'green' : 'default'">
              {{ record.enabled === 1 ? '启用' : '停用' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button size="small" @click="openEdit(record)">编辑</a-button>
              <a-popconfirm title="确认删除？" @confirm="handleDelete(record.id)">
                <a-button size="small" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="modalOpen" :title="editing.id ? '编辑平台' : '新增平台'" @ok="handleSubmit">
      <a-form layout="vertical" :model="editing">
        <a-form-item label="编码" :required="true">
          <a-input v-model:value="editing.code" :disabled="!!editing.id" placeholder="如 weibo" />
        </a-form-item>
        <a-form-item label="名称" :required="true">
          <a-input v-model:value="editing.name" />
        </a-form-item>
        <a-form-item label="图标 URL">
          <a-input v-model:value="editing.icon" />
        </a-form-item>
        <a-form-item label="排序">
          <a-input-number v-model:value="editing.sortOrder" :min="0" />
        </a-form-item>
        <a-form-item label="状态">
          <a-switch v-model:checked="enabledBool" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, computed } from 'vue'
import { message } from 'ant-design-vue'
import { useHotSearch } from '@/composables/useHotSearch.js'

const { state, fetchPlatforms, savePlatform, updatePlatform, removePlatform } = useHotSearch()

const columns = [
  { title: '编码', dataIndex: 'code', key: 'code', width: 120 },
  { title: '名称', dataIndex: 'name', key: 'name', width: 140 },
  { title: '图标', dataIndex: 'icon', key: 'icon' },
  { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder', width: 80 },
  { title: '状态', key: 'enabled', width: 100 },
  { title: '操作', key: 'action', width: 180 }
]

const modalOpen = ref(false)
const editing = reactive({ id: null, code: '', name: '', icon: '', sortOrder: 0, enabled: 1 })
const enabledBool = computed({
  get: () => editing.enabled === 1,
  set: (v) => (editing.enabled = v ? 1 : 0)
})

const openCreate = () => {
  editing.id = null
  editing.code = ''
  editing.name = ''
  editing.icon = ''
  editing.sortOrder = 0
  editing.enabled = 1
  modalOpen.value = true
}
const openEdit = (r) => {
  Object.assign(editing, r)
  modalOpen.value = true
}
const handleSubmit = async () => {
  if (!editing.code || !editing.name) {
    message.warning('请填写编码和名称')
    return
  }
  const payload = { ...editing }
  delete payload.id
  if (editing.id) {
    await updatePlatform(editing.id, payload)
  } else {
    await savePlatform(payload)
  }
  modalOpen.value = false
  fetchPlatforms()
}
const handleDelete = async (id) => {
  await removePlatform(id)
  fetchPlatforms()
}

onMounted(fetchPlatforms)
</script>

<style scoped>.hot-search-platforms { padding: 0; }</style>
