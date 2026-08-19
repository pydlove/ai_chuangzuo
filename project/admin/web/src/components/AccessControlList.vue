<template>
  <a-card :bordered="false" :title="title">
    <div class="access-control-toolbar">
      <a-input-search
        v-model:value="keyword"
        placeholder="搜索规则值或备注"
        style="width: 240px"
        @search="fetch"
      />
      <a-button type="primary" @click="openCreateModal">
        新增{{ typeLabel }}{{ listTypeLabel }}
      </a-button>
    </div>

    <a-table
      :columns="columns"
      :data-source="records"
      :loading="loading"
      :pagination="false"
      row-key="id"
      size="middle"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'ruleStatus'">
          <a-tag :color="record.ruleStatus === 1 ? 'blue' : 'default'">
            {{ record.ruleStatus === 1 ? '启用' : '禁用' }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'actions'">
          <a-space>
            <a-button type="link" size="small" @click="openEditModal(record)">
              编辑
            </a-button>
            <a-popconfirm
              title="确定删除此规则？"
              ok-text="删除"
              cancel-text="取消"
              @confirm="handleDelete(record.id)"
            >
              <a-button type="link" size="small" danger>删除</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>

    <div class="access-control-pagination">
      <a-pagination
        :current="page"
        :page-size="pageSize"
        :total="total"
        :page-size-options="['10', '20', '50']"
        show-size-changer
        show-total
        @change="onPageChange"
        @show-size-change="onPageChange"
      />
    </div>

    <a-modal
      v-model:open="modalVisible"
      :title="modalTitle"
      :confirm-loading="submitting"
      @ok="handleSave"
    >
      <a-form layout="vertical">
        <a-form-item label="规则值" required>
          <a-input
            v-model:value="form.ruleValue"
            :placeholder="valuePlaceholder"
            :maxlength="128"
          />
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea
            v-model:value="form.remark"
            :rows="3"
            :maxlength="256"
            placeholder="可选备注"
          />
        </a-form-item>
        <a-form-item label="状态">
          <a-switch
            v-model:checked="form.ruleStatus"
            checked-children="启用"
            un-checked-children="禁用"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </a-card>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import {
  fetchAccessControlRules,
  createAccessControlRule,
  updateAccessControlRule,
  deleteAccessControlRule
} from '@/api/security.js'

const props = defineProps({
  title: { type: String, required: true },
  ruleType: { type: Number, required: true },
  listType: { type: Number, required: true },
  typeLabel: { type: String, required: true }
})

const keyword = ref('')
const records = ref([])
const total = ref(0)
const loading = ref(false)
const page = ref(1)
const pageSize = ref(20)
const submitting = ref(false)
const modalVisible = ref(false)
const editingId = ref(null)

const form = reactive({
  ruleValue: '',
  remark: '',
  ruleStatus: true
})

const listTypeLabel = computed(() => (props.listType === 1 ? '黑名单' : '白名单'))

const valuePlaceholder = computed(() => {
  return props.ruleType === 1 ? '支持 IPv4/IPv6，如 192.168.1.1' : '用户ID 或 邮箱'
})

const modalTitle = computed(() => {
  const action = editingId.value ? '编辑' : '新增'
  return `${action}${props.typeLabel}${listTypeLabel.value}规则`
})

const columns = [
  { title: '规则值', dataIndex: 'ruleValue', key: 'ruleValue', width: 260 },
  { title: '状态', dataIndex: 'ruleStatus', key: 'ruleStatus', width: 100 },
  { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 170 },
  { title: '操作', key: 'actions', width: 140 }
]

function resetForm() {
  form.ruleValue = ''
  form.remark = ''
  form.ruleStatus = true
  editingId.value = null
}

function openCreateModal() {
  resetForm()
  modalVisible.value = true
}

function openEditModal(record) {
  editingId.value = record.id
  form.ruleValue = record.ruleValue || ''
  form.remark = record.remark || ''
  form.ruleStatus = record.ruleStatus === 1
  modalVisible.value = true
}

async function fetch() {
  loading.value = true
  try {
    const res = await fetchAccessControlRules({
      ruleType: props.ruleType,
      listType: props.listType,
      keyword: keyword.value || undefined,
      page: page.value,
      size: pageSize.value
    })
    records.value = res.records || []
    total.value = res.total || 0
  } catch (e) {
    message.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function onPageChange(newPage, newPageSize) {
  page.value = newPage
  pageSize.value = newPageSize
  fetch()
}

async function handleSave() {
  const value = form.ruleValue.trim()
  if (!value) {
    message.warning('请填写规则值')
    return
  }
  const payload = {
    ruleType: props.ruleType,
    listType: props.listType,
    ruleValue: value,
    remark: form.remark || '',
    ruleStatus: form.ruleStatus ? 1 : 0
  }
  submitting.value = true
  try {
    if (editingId.value) {
      await updateAccessControlRule(editingId.value, payload)
      message.success('更新成功')
    } else {
      await createAccessControlRule(payload)
      message.success('创建成功')
    }
    modalVisible.value = false
    resetForm()
    fetch()
  } catch (e) {
    message.error(e?.message || '保存失败')
  } finally {
    submitting.value = false
  }
}

async function handleDelete(id) {
  try {
    await deleteAccessControlRule(id)
    message.success('删除成功')
    fetch()
  } catch (e) {
    message.error(e?.message || '删除失败')
  }
}

watch(() => [props.ruleType, props.listType], () => {
  page.value = 1
  keyword.value = ''
  fetch()
})

onMounted(fetch)
</script>

<style scoped>
.access-control-toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  align-items: center;
}
.access-control-pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
