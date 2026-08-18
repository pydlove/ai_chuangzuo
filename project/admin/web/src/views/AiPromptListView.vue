<template>
  <div class="ai-prompt-list">
    <a-card :bordered="false">
      <div class="page-header">
        <h3 class="page-title">AI 提示词管理</h3>
        <p class="page-desc">
          统一管理代码中硬编码的 AI 提示词。支持变量占位符
          <code>{{variableName}}</code>，非变量内容可直接在线编辑。
        </p>
      </div>

      <div class="toolbar">
        <a-select
          v-model:value="query.module"
          placeholder="归属端"
          allow-clear
          style="width: 120px"
          @change="handleSearch"
        >
          <a-select-option value="admin">管理端</a-select-option>
          <a-select-option value="user">用户端</a-select-option>
        </a-select>
        <a-input
          v-model:value="query.keyword"
          placeholder="编码/名称"
          allow-clear
          style="width: 220px"
          @press-enter="handleSearch"
        />
        <a-button type="primary" @click="handleSearch">查询</a-button>
        <a-button @click="handleReset">重置</a-button>
        <a-button type="primary" @click="gotoCreate">
          <template #icon><PlusOutlined /></template>
          新建提示词
        </a-button>
      </div>

      <a-table
        :columns="columns"
        :data-source="list"
        :loading="loading"
        :pagination="false"
        row-key="id"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag v-if="record.status === 1" color="green">启用</a-tag>
            <a-tag v-else color="default">停用</a-tag>
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-button type="link" size="small" @click="gotoEdit(record.id)">编辑</a-button>
            <a-button type="link" size="small" @click="onToggleStatus(record)">
              {{ record.status === 1 ? '停用' : '启用' }}
            </a-button>
            <a-popconfirm
              title="确定删除此提示词？"
              ok-text="删除"
              cancel-text="取消"
              @confirm="onDelete(record.id)"
            >
              <a-button type="link" size="small" danger>删除</a-button>
            </a-popconfirm>
          </template>
        </template>
      </a-table>

      <div class="pagination">
        <a-pagination
          v-model:current="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-size-options="['10', '20', '50']"
          show-size-changer
          show-total
          @change="handlePageChange"
          @show-size-change="handlePageChange"
        />
      </div>
    </a-card>
  </div>
</template>

<script setup>
import { reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { useAiPrompt } from '@/composables/useAiPrompt.js'

const router = useRouter()
const {
  list, total, loading, page, pageSize,
  fetchList, remove, toggleStatus
} = useAiPrompt()

const query = reactive({
  module: undefined,
  keyword: ''
})

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '编码', dataIndex: 'promptCode', key: 'promptCode', width: 180 },
  { title: '名称', dataIndex: 'promptName', key: 'promptName', width: 200 },
  { title: '端', dataIndex: 'module', key: 'module', width: 100 },
  { title: '分类', dataIndex: 'category', key: 'category', width: 140 },
  { title: '状态', key: 'status', width: 100 },
  { title: '更新时间', dataIndex: 'updatedAt', key: 'updatedAt', width: 170 },
  { title: '操作', key: 'actions', fixed: 'right', width: 200 }
]

const handleSearch = () => {
  page.value = 1
  fetchList(query)
}

const handleReset = () => {
  query.module = undefined
  query.keyword = ''
  page.value = 1
  pageSize.value = 20
  fetchList(query)
}

const handlePageChange = () => {
  fetchList(query)
}

const gotoCreate = () => router.push('/console/ai-prompts/new')
const gotoEdit = (id) => router.push(`/console/ai-prompts/${id}`)

const onToggleStatus = async (record) => {
  try {
    await toggleStatus(record)
    await fetchList(query)
  } catch (e) {
    message.error(e.message || '操作失败')
  }
}

const onDelete = async (id) => {
  try {
    await remove(id)
    await fetchList(query)
  } catch (e) {
    message.error(e.message || '删除失败')
  }
}

onMounted(() => fetchList(query))
</script>

<style scoped>
.page-header {
  margin-bottom: 16px;
}
.page-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 4px;
}
.page-desc {
  color: #8c8c8c;
  font-size: 13px;
  margin: 0;
}
.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  align-items: center;
}
.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
