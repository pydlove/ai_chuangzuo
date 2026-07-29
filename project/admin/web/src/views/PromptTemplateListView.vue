<template>
  <div class="prompt-template">
    <a-card :bordered="false">
      <div class="page-header">
        <h3 class="page-title">创作提示词</h3>
        <p class="page-desc">
          多模板共存，runtime 仅 1 个已发布。worker 会读取最新已发布版本（latestPublishedVersion）去生成用户内容。
          状态机：草稿 → 发布 → 下线 → 重新发布。占位符：<code>{{title}}</code> <code>{{description}}</code> <code>{{platform}}</code> <code>{{wordCount}}</code> <code>{{userSkillPrompt}}</code>（用户风格在提交时由系统自动快照）
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

    <a-modal
      v-model:open="cloneModalOpen"
      title="克隆模板"
      :confirm-loading="cloneSubmitting"
      :mask-closable="false"
      :destroy-on-close="true"
      ok-text="克隆"
      cancel-text="取消"
      @ok="handleCloneOk"
    >
      <div class="clone-form">
        <div class="form-row">
          <span class="form-label required">新模板名称</span>
          <a-input
            v-model:value="cloneForm.name"
            placeholder="请输入新模板名称"
            :max-length="100"
            show-count
          />
        </div>
        <div class="form-row form-row-top">
          <span class="form-label">备注</span>
          <a-textarea
            v-model:value="cloneForm.remark"
            :rows="3"
            placeholder="可选，备注信息会复制到副本"
            :max-length="200"
            show-count
          />
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
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

const cloneModalOpen = ref(false)
const cloneSubmitting = ref(false)
const cloneSource = ref(null)
const cloneForm = reactive({ name: '', remark: '' })

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

const onClone = (record) => {
  cloneSource.value = record
  cloneForm.name = `${record.name}-副本`
  cloneForm.remark = ''
  cloneModalOpen.value = true
}

const handleCloneOk = async () => {
  const name = cloneForm.name?.trim()
  if (!name) {
    message.warning('请输入新模板名称')
    return
  }
  cloneSubmitting.value = true
  try {
    await handleClone(cloneSource.value.id, name, cloneForm.remark?.trim())
    cloneModalOpen.value = false
  } catch (e) {
    message.error(e.message || '克隆失败')
  } finally {
    cloneSubmitting.value = false
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
.clone-form {
  padding: 8px 0 0;
}
.form-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}
.form-row-top {
  align-items: flex-start;
  margin-bottom: 0;
}
.form-label {
  flex-shrink: 0;
  width: 90px;
  color: #595959;
  font-size: 13px;
}
.form-row-top .form-label {
  padding-top: 5px;
}
.form-label.required::before {
  content: '*';
  margin-right: 4px;
  color: #ff4d4f;
}
</style>
