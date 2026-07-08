<template>
  <div class="message-admin">
    <a-card :bordered="false">
      <div class="page-header">
        <h3 class="page-title">消息管理</h3>
        <p class="page-desc">发布公告、新功能、优惠活动。消息发出后只能编辑标题/摘要，不能删除</p>
      </div>

      <a-tabs v-model:active-key="activeTab" @change="switchTab">
        <a-tab-pane v-for="tab in TAB_ITEMS" :key="tab.key" :tab="tab.label" />
      </a-tabs>

      <div class="toolbar">
        <a-input
          v-model:value="keyword"
          placeholder="按标题或摘要搜索"
          allow-clear
          style="width: 240px"
          @press-enter="handleSearch"
        />
        <a-button type="primary" @click="handleSearch">查询</a-button>
        <a-button @click="handleReset">重置</a-button>
        <a-button type="primary" @click="openCreateModal">
          <template #icon><PlusOutlined /></template>
          新建{{ activeTabLabel }}
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
          <template v-if="column.key === 'title'">
            <span class="cell-title">{{ record.title }}</span>
          </template>
          <template v-else-if="column.key === 'summary'">
            <span class="cell-ellipsis">{{ record.summary }}</span>
          </template>
          <template v-else-if="column.key === 'scope'">
            <a-tag :color="record.scope === 1 ? 'blue' : 'purple'">
              {{ record.scopeLabel }}
            </a-tag>
            <span class="cell-audience">{{ record.audienceLabel }}</span>
          </template>
          <template v-else-if="column.key === 'read'">
            <a-tooltip :title="`已读 ${record.readCount} / 送达 ${record.audienceCount}`">
              <span class="cell-read" :class="{ 'is-zero': !record.readCount }">
                {{ record.readLabel }}
              </span>
            </a-tooltip>
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-button type="link" size="small" @click="openEditModal(record)">编辑</a-button>
            <a-button type="link" size="small" @click="openDetailModal(record)">详情</a-button>
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

    <!-- 新建 / 编辑 Modal -->
    <a-modal
      v-model:open="editorVisible"
      :title="editingId ? `编辑${activeTabLabel}` : `新建${activeTabLabel}`"
      ok-text="保存"
      cancel-text="取消"
      :confirm-loading="submitting"
      :width="640"
      @ok="confirmSubmit"
    >
      <a-form :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
        <a-form-item label="标题" required>
          <a-input
            v-model:value="form.title"
            placeholder="例如：系统维护通知"
            :maxlength="128"
            show-count
          />
        </a-form-item>
        <a-form-item label="摘要" required>
          <a-textarea
            v-model:value="form.summary"
            placeholder="用户消息中心卡片显示的摘要文本"
            :maxlength="512"
            :rows="4"
            show-count
          />
        </a-form-item>
        <a-form-item label="跳转链接">
          <a-input
            v-model:value="form.linkUrl"
            placeholder="用户点击消息后跳转的路由，如 /pricing；可空"
            :maxlength="256"
            allow-clear
          />
        </a-form-item>
        <a-form-item v-if="!editingId" label="发送范围" required>
          <a-radio-group v-model:value="form.scope" :disabled="isFeature">
            <a-radio :value="1">全体</a-radio>
            <a-radio :value="2">指定人</a-radio>
          </a-radio-group>
          <div v-if="isFeature" class="form-hint">新功能仅支持全体发送</div>
        </a-form-item>
        <a-form-item v-if="!editingId && form.scope === 2" label="接收用户" required>
          <a-select
            v-model:value="form.targetUserIds"
            mode="multiple"
            placeholder="搜索并选择接收用户"
            show-search
            :filter-option="false"
            :options="publisherOptions"
            :loading="publisherLoading"
            @search="searchPublisher"
            @popup-scroll="onPublisherScroll"
          />
          <div class="form-hint">已选 {{ form.targetUserIds.length }} 人</div>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 详情 Modal -->
    <a-modal
      v-model:open="detailVisible"
      title="消息详情"
      :footer="null"
      :width="720"
    >
      <div v-if="detail" class="detail-view">
        <a-descriptions :column="1" bordered size="small">
          <a-descriptions-item label="消息类型">{{ detail.msgTypeLabel }}</a-descriptions-item>
          <a-descriptions-item label="标题">{{ detail.title }}</a-descriptions-item>
          <a-descriptions-item label="摘要">{{ detail.summary }}</a-descriptions-item>
          <a-descriptions-item label="跳转链接">{{ detail.linkUrl || '—' }}</a-descriptions-item>
          <a-descriptions-item label="发送范围">
            <a-tag :color="detail.scope === 1 ? 'blue' : 'purple'">{{ detail.scopeLabel }}</a-tag>
            <span class="cell-audience">{{ detail.audienceLabel }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="已读统计">{{ detail.readLabel }}</a-descriptions-item>
          <a-descriptions-item label="发布人">{{ detail.createdByName }}</a-descriptions-item>
          <a-descriptions-item label="发布时间">{{ formatDateTime(detail.createdAt) }}</a-descriptions-item>
        </a-descriptions>

        <template v-if="detail.scope === 2">
          <a-divider>接收用户</a-divider>
          <a-table
            :columns="audienceColumns"
            :data-source="detail.audience"
            :pagination="{ pageSize: 10, showSizeChanger: false }"
            row-key="userId"
            size="small"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'user'">
                <div class="audience-user">
                  <span class="audience-nickname">{{ record.nickname || '用户#' + record.userId }}</span>
                  <span class="audience-email">{{ record.email }}</span>
                </div>
              </template>
              <template v-else-if="column.key === 'read'">
                <a-tag v-if="record.read" color="green">已读</a-tag>
                <a-tag v-else>未读</a-tag>
                <span v-if="record.readAt" class="audience-time">{{ formatDateTime(record.readAt) }}</span>
              </template>
            </template>
          </a-table>
        </template>
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { useMessageManagement } from '@/composables/useMessageManagement.js'
import { listUserOptions } from '@/api/userOptions.js'

const {
  activeTab,
  TAB_ITEMS,
  list,
  total,
  loading,
  submitting,
  page,
  pageSize,
  keyword,
  fetch,
  switchTab,
  handleSearch,
  handleReset,
  handlePageChange,
  loadDetail,
  handleCreate,
  handleUpdate
} = useMessageManagement()

const activeTabLabel = computed(() => {
  return TAB_ITEMS.find(t => t.key === activeTab.value)?.label || ''
})
const isFeature = computed(() => activeTab.value === 'feature')

const columns = [
  { title: '标题', dataIndex: 'title', key: 'title', width: 220 },
  { title: '摘要', dataIndex: 'summary', key: 'summary', width: 260 },
  { title: '范围 / 受众', key: 'scope', width: 180 },
  { title: '已读', key: 'read', width: 100 },
  { title: '发布人', dataIndex: 'createdByName', key: 'createdByName', width: 100 },
  { title: '发布时间', dataIndex: 'createdAt', key: 'createdAt', width: 170 },
  { title: '操作', key: 'actions', width: 130, fixed: 'right' }
]

const audienceColumns = [
  { title: '用户', key: 'user', width: 200 },
  { title: '消息ID', dataIndex: 'messageId', key: 'messageId', width: 100 },
  { title: '发送时间', dataIndex: 'bizTime', key: 'bizTime', width: 170 },
  { title: '已读状态', key: 'read', width: 220 }
]

// ---- Editor modal ----
const editorVisible = ref(false)
const editingId = ref(null)
const form = reactive({
  title: '',
  summary: '',
  linkUrl: '',
  scope: 1,
  targetUserIds: []
})

function resetForm() {
  form.title = ''
  form.summary = ''
  form.linkUrl = ''
  form.scope = isFeature.value ? 1 : 1
  form.targetUserIds = []
  publisherOptions.value = []
  publisherOptionsCache.clear()
  publisherLoaded = false
}

const publisherOptions = ref([])
const publisherLoading = ref(false)
const publisherOptionsCache = new Map()
let publisherLoaded = false

const searchPublisher = async (kw) => {
  publisherLoading.value = true
  try {
    const cacheKey = kw || ''
    if (publisherOptionsCache.has(cacheKey)) {
      publisherOptions.value = publisherOptionsCache.get(cacheKey)
    } else {
      const users = await listUserOptions(kw, 50)
      const opts = users.map((u) => ({
        label: u.nickname ? `${u.nickname}（${u.email}）` : u.email,
        value: u.id
      }))
      publisherOptions.value = opts
      publisherOptionsCache.set(cacheKey, opts)
    }
  } catch (error) {
    message.error(error.message || '加载用户失败')
  } finally {
    publisherLoading.value = false
  }
}

const onPublisherScroll = () => {
  // 简单实现：不滚动加载更多，搜索补全
}

const openCreateModal = () => {
  editingId.value = null
  resetForm()
  if (!isFeature.value) {
    // 提前加载一次默认列表
    searchPublisher('')
  }
  editorVisible.value = true
}

const openEditModal = (record) => {
  editingId.value = record.id
  form.title = record.title || ''
  form.summary = record.summary || ''
  form.linkUrl = record.linkUrl || ''
  // 编辑模式：scope 不可改；targetUserIds 也不可改
  form.scope = record.scope
  form.targetUserIds = []
  editorVisible.value = true
}

const confirmSubmit = async () => {
  if (!form.title.trim() || !form.summary.trim()) {
    message.error('请填写标题和摘要')
    return
  }
  let payload
  let ok
  if (editingId.value) {
    payload = {
      title: form.title.trim(),
      summary: form.summary.trim(),
      linkUrl: form.linkUrl.trim() || null
    }
    ok = await handleUpdate(editingId.value, payload)
  } else {
    payload = {
      title: form.title.trim(),
      summary: form.summary.trim(),
      linkUrl: form.linkUrl.trim() || null,
      scope: isFeature.value ? 1 : form.scope
    }
    if (payload.scope === 2) {
      if (!form.targetUserIds || form.targetUserIds.length === 0) {
        message.error('指定人消息必须选择至少 1 个接收用户')
        return
      }
      payload.targetUserIds = form.targetUserIds
    }
    ok = await handleCreate(payload)
  }
  if (ok) {
    editorVisible.value = false
  }
}

// ---- Detail modal ----
const detailVisible = ref(false)
const detail = ref(null)

const openDetailModal = async (record) => {
  detailVisible.value = true
  detail.value = null
  try {
    detail.value = await loadDetail(record.id)
  } catch (error) {
    message.error(error.message || '加载详情失败')
  }
}

const formatDateTime = (s) => {
  if (!s) return ''
  // 后端返回 LocalDateTime 的 ISO 字符串
  return s.replace('T', ' ').slice(0, 19)
}

onMounted(() => {
  fetch()
})
</script>

<style scoped>
.message-admin :deep(.ant-tabs) {
  margin-bottom: 16px;
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
  margin: 0;
  font-size: 13px;
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

.cell-title {
  font-weight: 500;
  color: #1a1a1a;
}

.cell-ellipsis {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
  color: #595959;
}

.cell-audience {
  margin-left: 8px;
  color: #8c8c8c;
  font-size: 12px;
}

.cell-read {
  font-weight: 500;
  color: #07c160;
}

.cell-read.is-zero {
  color: #8c8c8c;
  font-weight: 400;
}

.form-hint {
  margin-top: 4px;
  color: #8c8c8c;
  font-size: 12px;
}

.detail-view {
  padding: 4px 0;
}

.audience-user {
  display: flex;
  flex-direction: column;
  line-height: 1.4;
}

.audience-nickname {
  font-weight: 500;
  color: #1a1a1a;
}

.audience-email {
  color: #8c8c8c;
  font-size: 12px;
}

.audience-time {
  margin-left: 6px;
  color: #8c8c8c;
  font-size: 12px;
}
</style>
