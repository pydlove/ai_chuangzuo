<template>
  <div class="style-review">
    <a-card :bordered="false" class="style-review-card">
      <div class="style-review-header">
        <h3 class="style-review-title">风格审核</h3>
        <p class="style-review-desc">审核用户提交到风格市场的风格</p>
      </div>

      <!-- 工具栏 -->
      <div class="style-review-toolbar">
        <a-select
          v-model:value="status"
          style="width: 140px"
          :options="statusOptions"
          @change="handleSearch"
        />
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
      </div>

      <!-- 表格 -->
      <a-table
        :columns="columns"
        :data-source="styles"
        :loading="loading"
        :pagination="false"
        row-key="id"
        size="middle"
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
              danger
              :disabled="record.status === 'rejected'"
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
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import { useStyleReview } from '@/composables/useStyleReview.js'

const {
  styles,
  total,
  loading,
  page,
  pageSize,
  keyword,
  status,
  fetchStyles,
  handleSearch,
  handleReset,
  handlePageChange,
  handleReject
} = useStyleReview()

const statusOptions = [
  { label: '待审核', value: 0 },
  { label: '已通过', value: 1 },
  { label: '已拒绝', value: 2 },
  { label: '全部', value: '' }
]

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

const reasonVisible = ref(false)
const reasonTarget = ref(null)

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

.style-review-pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.reason-link {
  padding: 0 0 0 8px;
  font-size: 12px;
}
</style>
