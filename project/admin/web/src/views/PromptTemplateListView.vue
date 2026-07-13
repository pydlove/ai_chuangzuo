<template>
  <div class="prompt-template">
    <a-card :bordered="false">
      <div class="page-header">
        <h3 class="page-title">创作提示词</h3>
        <p class="page-desc">
          多模板共存，runtime 仅 1 个已发布。worker 会读取最新已发布版本（latestPublishedVersion）去生成用户内容。
          状态机：草稿 → 发布 → 下线 → 重新发布。占位符：<code>{{title}}</code> <code>{{description}}</code> <code>{{platform}}</code> <code>{{wordCount}}</code> <code>{{userStylePrompt}}</code>（用户风格在提交时由系统自动快照）
        </p>
      </div>

      <div class="toolbar">
        <a-input
          v-model:value="keyword"
          placeholder="按名称搜索"
          allow-clear
          style="width: 240px"
          @press-enter="handleSearch"
        />
        <a-button type="primary" @click="handleSearch">查询</a-button>
        <a-button @click="handleReset">重置</a-button>
        <a-button type="primary" @click="gotoCreate">
          <template #icon><PlusOutlined /></template>
          新建模板
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
          <template v-if="column.key === 'templateStatus'">
            <a-tag v-if="record.templateStatus === 1" color="green">生效中</a-tag>
            <a-tag v-else-if="record.templateStatus === 2" color="default">{{ record.templateStatusLabel }}</a-tag>
            <a-tag v-else color="orange">{{ record.templateStatusLabel || '草稿' }}</a-tag>
            <span v-if="record.latestPublishedVersion" class="version-tag">
              v{{ record.latestPublishedVersion }}
            </span>
          </template>
          <template v-else-if="column.key === 'name'">
            <span class="cell-name">{{ record.name }}</span>
            <a-tag v-if="record.isBuiltin" color="green" style="margin-left: 8px">内置</a-tag>
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-button type="link" size="small" @click="gotoEdit(record.id)">编辑</a-button>

            <!-- 草稿 / 已下线：显示「发布」 -->
            <a-button
              v-if="record.templateStatus !== 1"
              type="link"
              size="small"
              @click="onPublish(record)"
            >{{ record.templateStatus === 2 ? '重新发布' : '发布' }}</a-button>

            <!-- 已发布：显示「下线」 -->
            <a-button
              v-if="record.templateStatus === 1"
              type="link"
              size="small"
              danger
              @click="onOffline(record)"
            >下线</a-button>

            <a-button type="link" size="small" @click="onClone(record)">克隆</a-button>

            <template v-if="record.isBuiltin">
              <a-tooltip title="内置模板不可删除，如需修改请先复制派生">
                <a-button type="link" size="small" disabled>删除</a-button>
              </a-tooltip>
            </template>
            <a-popconfirm
              v-else
              title="确定删除此模板？"
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
          :current="page"
          :page-size="pageSize"
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
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { usePromptTemplate } from '@/composables/usePromptTemplate.js'

const router = useRouter()
const {
  list,
  total,
  loading,
  page,
  pageSize,
  keyword,
  fetch,
  handleSearch,
  handleReset,
  handlePageChange,
  handlePublish,
  handleOffline,
  handleClone,
  handleDelete
} = usePromptTemplate()

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '模板名称', dataIndex: 'name', key: 'name', width: 200 },
  { title: '状态', key: 'templateStatus', width: 140 },
  { title: '备注', dataIndex: 'remark', key: 'remark' },
  { title: '更新时间', dataIndex: 'updatedAt', key: 'updatedAt', width: 170 },
  { title: '操作', key: 'actions', fixed: 'right', width: 280 }
]

const gotoCreate = () => router.push('/console/prompt-templates/new')
const gotoEdit = (id) => router.push(`/console/prompt-templates/${id}`)

const onPublish = (record) => {
  Modal.confirm({
    title: `${record.templateStatus === 2 ? '重新发布' : '发布'}「${record.name}」？`,
    content: `发布后将自动下线当前已发布模板。将创建版本 v${(record.latestPublishedVersion || 0) + 1}。`,
    okText: '发布',
    cancelText: '取消',
    onOk: async () => {
      try {
        await handlePublish(record.id, '')
      } catch (e) {
        message.error(e.message || '发布失败')
      }
    }
  })
}

const onOffline = (record) => {
  Modal.confirm({
    title: `下线「${record.name}」？`,
    content: '下线后 worker 会因为无已发布模板而报错，新任务无法提交。',
    okText: '下线',
    cancelText: '取消',
    onOk: async () => {
      try {
        await handleOffline(record.id)
      } catch (e) {
        message.error(e.message || '下线失败')
      }
    }
  })
}

const onClone = async (record) => {
  // 简单策略：用 prompt 让用户输入新名；取消则不克隆
  const defaultName = `${record.name}-副本`
  const newName = window.prompt(`输入新模板名称（将作为草稿保存）：`, defaultName)
  if (!newName || !newName.trim()) return
  try {
    await handleClone(record.id, newName.trim())
  } catch (e) {
    message.error(e.message || '克隆失败')
  }
}

const onDelete = async (id) => {
  await handleDelete(id)
}

onMounted(() => fetch())
</script>

<style scoped>
.prompt-template :deep(.ant-table-row) {
  background: #fff;
}
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
.cell-name {
  font-weight: 500;
}
.version-tag {
  margin-left: 6px;
  color: #8c8c8c;
  font-size: 12px;
}
</style>
