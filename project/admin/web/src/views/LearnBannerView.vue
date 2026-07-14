<template>
  <div class="banner-view">
    <a-card title="Banner 管理" :bordered="false">
      <template #extra>
        <a-button type="primary" @click="onCreate">新增 Banner</a-button>
      </template>
      <a-table :columns="columns" :data-source="banners" :loading="loading" row-key="id" :pagination="false">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'imageUrl'">
            <img :src="record.imageUrl" style="width: 120px; height: 60px; object-fit: cover; border-radius: 4px;" />
          </template>
          <template v-else-if="column.key === 'linkUrl'">
            <span>{{ record.linkUrl || '-' }}</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a @click="onEdit(record)">编辑</a>
            <a-divider type="vertical" />
            <a-popconfirm title="确认删除该 Banner？" @confirm="onDelete(record)">
              <a class="danger">删除</a>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="modalOpen"
      :title="editing ? '编辑 Banner' : '新增 Banner'"
      :confirm-loading="submitting"
      @ok="onSubmit"
    >
      <a-form layout="vertical" :model="form">
        <a-form-item label="图片 URL" required>
          <a-input v-model:value="form.imageUrl" placeholder="https://..." />
        </a-form-item>
        <a-form-item label="跳转链接">
          <a-input v-model:value="form.linkUrl" placeholder="（可选）https://..." />
        </a-form-item>
        <a-form-item label="排序">
          <a-input-number v-model:value="form.sort" :min="0" style="width: 100%" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { fetchBanners, createBanner, updateBanner, deleteBanner } from '@/api/learn'

const banners = ref([])
const loading = ref(false)
const modalOpen = ref(false)
const submitting = ref(false)
const editing = ref(null)
const form = reactive({ imageUrl: '', linkUrl: '', sort: 0 })

const columns = [
  { title: '预览', key: 'imageUrl', width: 140 },
  { title: '图片 URL', dataIndex: 'imageUrl', key: 'imageUrl', ellipsis: true },
  { title: '跳转链接', key: 'linkUrl', ellipsis: true },
  { title: '排序', dataIndex: 'sort', key: 'sort', width: 80 },
  { title: '操作', key: 'action', width: 120 }
]

async function load() {
  loading.value = true
  try {
    banners.value = await fetchBanners()
  } catch (e) {
    message.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function onCreate() {
  editing.value = null
  form.imageUrl = ''; form.linkUrl = ''; form.sort = 0
  modalOpen.value = true
}

function onEdit(record) {
  editing.value = record
  form.imageUrl = record.imageUrl
  form.linkUrl = record.linkUrl || ''
  form.sort = record.sort
  modalOpen.value = true
}

async function onDelete(record) {
  try {
    await deleteBanner(record.id)
    message.success('已删除')
    await load()
  } catch (e) {
    message.error(e?.message || '删除失败')
  }
}

async function onSubmit() {
  if (!form.imageUrl.trim()) { message.error('图片 URL 不能为空'); return }
  submitting.value = true
  try {
    if (editing.value) {
      await updateBanner(editing.value.id, form)
    } else {
      await createBanner(form)
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
.banner-view { padding: 0; }
.danger { color: #ff4d4f; }
</style>
