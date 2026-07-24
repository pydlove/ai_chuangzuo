<template>
  <div class="style-review">
    <a-card :bordered="false" class="style-review-card">
      <div class="style-review-header">
        <h3 class="style-review-title">风格审核</h3>
        <p class="style-review-desc">审核用户提交到风格市场的风格</p>
      </div>

      <!-- 工具栏 -->
      <div class="style-review-toolbar">
        <a-tabs
          v-model:activeKey="activeTab"
          size="small"
          @change="onTabChange"
        >
          <a-tab-pane key="pending" tab="待审核" />
          <a-tab-pane key="reviewed" tab="已审核" />
        </a-tabs>
        <a-input
          v-model:value="keyword"
          placeholder="风格名称或创作者"
          allow-clear
          style="width: 280px"
          @press-enter="handleSearch"
        />
        <a-button type="primary" @click="handleSearch">查询</a-button>
        <a-button @click="handleReset">重置</a-button>
        <a-button @click="fetchStyles">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
        <a-button
          v-if="activeTab === 'pending'"
          type="primary"
          :disabled="!canBatchApprove"
          @click="openBatchApproveModal"
        >
          批量通过
        </a-button>
      </div>

      <!-- 表格 -->
      <a-table
        :columns="columns"
        :data-source="styles"
        :loading="loading"
        :pagination="false"
        row-key="id"
        size="middle"
        :row-selection="activeTab === 'pending' ? { selectedRowKeys, onChange: onSelectChange } : null"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'sourceType'">
            {{ record.sourceType === 'my' ? '我的风格' : '学习的风格' }}
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="record.status === 'pending' ? '#ff4d6f'
                          : record.status === 'approved' ? 'green'
                          : 'error'">
              {{ record.status === 'pending' ? '待审核'
                  : record.status === 'approved' ? '已通过'
                  : '已打回' }}
            </a-tag>
            <a-button
              v-if="record.status === 'rejected'"
              type="link"
              size="small"
              class="reason-link"
              @click="openReasonModal(record)"
            >
              查看原因
            </a-button>
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-button
              type="link"
              size="small"
              @click="openDetailModal(record)"
            >
              查看
            </a-button>
            <a-button
              type="link"
              size="small"
              :disabled="record.status !== 'pending'"
              @click="openApproveModal(record)"
            >
              通过
            </a-button>
            <a-button
              type="link"
              size="small"
              danger
              :disabled="record.status !== 'pending'"
              @click="openRejectModal(record)"
            >
              打回
            </a-button>
          </template>
        </template>
      </a-table>

      <!-- 分页 -->
      <div class="style-review-pagination">
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

    <!-- 通过弹框 -->
    <a-modal
      v-model:open="approveVisible"
      title="通过风格"
      ok-text="确认通过"
      cancel-text="取消"
      :confirm-loading="approveSubmitting"
      @ok="confirmApprove"
    >
      <p v-if="approveTarget">风格名称：<strong>{{ approveTarget.name }}</strong></p>
      <p v-if="approveTarget" style="margin-top: 8px">创作者：<strong>{{ approveTarget.creatorName }}</strong></p>
      <p style="margin-top: 16px; color: #595959">通过后该风格将立即上架到风格市场，是否确认？</p>
    </a-modal>

    <!-- 批量通过弹框 -->
    <a-modal
      v-model:open="batchApproveVisible"
      title="批量通过风格"
      ok-text="确认通过"
      cancel-text="取消"
      :confirm-loading="batchApproveSubmitting"
      @ok="confirmBatchApprove"
    >
      <p>已选择 <strong>{{ selectedRowKeys.length }}</strong> 条风格</p>
      <p style="margin-top: 8px; color: #595959">仅状态为「待审核」的记录会被通过并上架，其他状态会自动跳过。是否确认？</p>
    </a-modal>

    <!-- 打回弹框 -->
    <a-modal
      v-model:open="rejectVisible"
      title="打回风格"
      ok-text="确认打回"
      cancel-text="取消"
      :confirm-loading="rejectSubmitting"
      @ok="confirmReject"
    >
      <p v-if="rejectTarget">风格名称：<strong>{{ rejectTarget.name }}</strong></p>
      <p v-if="rejectTarget" style="margin-top: 8px">创作者：<strong>{{ rejectTarget.creatorName }}</strong></p>
      <div style="margin-top: 16px">
        <label style="display: block; margin-bottom: 6px; font-weight: 500">打回原因 <span style="color: #ff4d4f">*</span></label>
        <a-textarea
          v-model:value="rejectReason"
          placeholder="请输入打回原因，用户将看到此说明"
          :maxlength="200"
          :rows="4"
          show-count
        />
      </div>
    </a-modal>

    <!-- 查看原因弹框 -->
    <a-modal
      v-model:open="reasonVisible"
      title="打回原因"
      :footer="null"
    >
      <p v-if="reasonTarget">风格名称：<strong>{{ reasonTarget.name }}</strong></p>
      <a-divider style="margin: 12px 0" />
      <p>{{ reasonTarget?.rejectReason || '—' }}</p>
    </a-modal>

    <!-- 风格详情弹框 -->
    <a-modal
      v-model:open="detailVisible"
      title="风格详情"
      :footer="null"
      :width="640"
    >
      <div v-if="detailTarget" class="style-detail-body">
        <div class="style-detail-item">
          <div class="style-detail-label">风格名称</div>
          <div class="style-detail-content">{{ detailTarget.name }}</div>
        </div>
        <div class="style-detail-item">
          <div class="style-detail-label">来源类型</div>
          <div class="style-detail-content">
            {{ detailTarget.sourceType === 'my' ? '我的风格' : '学习的风格' }}
          </div>
        </div>
        <div class="style-detail-item">
          <div class="style-detail-label">创作者</div>
          <div class="style-detail-content">{{ detailTarget.creatorName }}</div>
        </div>
        <div class="style-detail-item">
          <div class="style-detail-label">适用范围</div>
          <div class="style-detail-content">{{ detailTarget.scope || '—' }}</div>
        </div>
        <div class="style-detail-item">
          <div class="style-detail-label">风格提示词</div>
          <div class="style-detail-prompt">{{ detailTarget.prompt || '—' }}</div>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import { useStyleReview } from '@/composables/useStyleReview.js'

const {
  styles,
  total,
  loading,
  page,
  pageSize,
  keyword,
  activeTab,
  fetchStyles,
  handleSearch,
  handleReset,
  handlePageChange,
  handleTabChange,
  handleReject,
  handleApprove,
  handleApproveBatch
} = useStyleReview()

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 120 },
  { title: '风格名称', dataIndex: 'name', key: 'name', width: 160 },
  { title: '来源类型', dataIndex: 'sourceType', key: 'sourceType', width: 100 },
  { title: '创作者', dataIndex: 'creatorName', key: 'creatorName', width: 120 },
  { title: '提交时间', dataIndex: 'createdAt', key: 'createdAt', width: 170 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 160 },
  { title: '操作', key: 'actions', width: 120 }
]

const rejectVisible = ref(false)
const rejectTarget = ref(null)
const rejectReason = ref('')
const rejectSubmitting = ref(false)

const approveVisible = ref(false)
const approveTarget = ref(null)
const approveSubmitting = ref(false)

const batchApproveVisible = ref(false)
const batchApproveSubmitting = ref(false)

const reasonVisible = ref(false)
const reasonTarget = ref(null)

const detailVisible = ref(false)
const detailTarget = ref(null)

const selectedRowKeys = ref([])

const canBatchApprove = computed(() => {
  return selectedRowKeys.value.length > 0
    && styles.value
      .filter(s => selectedRowKeys.value.includes(s.id) && s.status === 'pending')
      .length > 0
})

const onSelectChange = (keys) => {
  selectedRowKeys.value = keys
}

const onTabChange = () => {
  selectedRowKeys.value = []
  handleTabChange()
}

const openBatchApproveModal = () => {
  batchApproveVisible.value = true
}

const confirmBatchApprove = async () => {
  const pendingIds = styles.value
    .filter(s => selectedRowKeys.value.includes(s.id) && s.status === 'pending')
    .map(s => s.id)
  if (pendingIds.length === 0) {
    batchApproveVisible.value = false
    return
  }
  batchApproveSubmitting.value = true
  const ok = await handleApproveBatch(pendingIds)
  batchApproveSubmitting.value = false
  if (ok) {
    selectedRowKeys.value = []
    batchApproveVisible.value = false
  }
}

const openApproveModal = (style) => {
  approveTarget.value = style
  approveVisible.value = true
}

const confirmApprove = async () => {
  if (!approveTarget.value) return
  approveSubmitting.value = true
  const ok = await handleApprove(approveTarget.value)
  approveSubmitting.value = false
  if (ok) {
    approveVisible.value = false
  }
}

const openRejectModal = (style) => {
  rejectTarget.value = style
  rejectReason.value = ''
  rejectVisible.value = true
}

const confirmReject = async () => {
  if (!rejectTarget.value) return
  if (!rejectReason.value.trim()) {
    return
  }
  rejectSubmitting.value = true
  const ok = await handleReject(rejectTarget.value, rejectReason.value)
  rejectSubmitting.value = false
  if (ok) {
    rejectVisible.value = false
  }
}

const openReasonModal = (style) => {
  reasonTarget.value = style
  reasonVisible.value = true
}

const openDetailModal = (style) => {
  detailTarget.value = style
  detailVisible.value = true
}

onMounted(() => {
  fetchStyles()
})
</script>

<style scoped>
.style-review-card {
  border-radius: 8px;
}

.style-review-header {
  margin-bottom: 16px;
}

.style-review-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 4px 0;
}

.style-review-desc {
  font-size: 13px;
  color: #8c8c8c;
  margin: 0;
}

.style-review-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  align-items: center;
}

.style-review-toolbar :deep(.ant-tabs) {
  flex: 1;
  margin-bottom: 0;
}

.style-review-toolbar :deep(.ant-tabs-nav) {
  margin-bottom: 0;
}

.style-review-pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.reason-link {
  padding: 0 0 0 8px;
  font-size: 12px;
}

.style-detail-body {
  max-height: 480px;
  overflow-y: auto;
}

.style-detail-item {
  margin-bottom: 16px;
}

.style-detail-label {
  font-size: 13px;
  font-weight: 500;
  color: #8c8c8c;
  margin-bottom: 6px;
}

.style-detail-content {
  font-size: 14px;
  color: #1a1a1a;
  line-height: 1.6;
  word-break: break-all;
}

.style-detail-prompt {
  font-size: 14px;
  color: #1a1a1a;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-all;
  background: #f6f6f6;
  border-radius: 6px;
  padding: 12px;
}
</style>
