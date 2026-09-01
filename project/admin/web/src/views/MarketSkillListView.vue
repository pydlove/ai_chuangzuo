<template>
  <div class="market-style">
    <a-card :bordered="false" class="market-style-card">
      <div class="market-style-header">
        <h3 class="market-style-title">提示词市场</h3>
        <p class="market-style-desc">管理用户端提示词市场中展示的提示词条目</p>
      </div>

      <a-tabs v-model:activeKey="activeTab" @change="onTabChange">
        <a-tab-pane key="list" tab="提示词列表">
          <div class="market-style-toolbar">
            <a-select
              v-model:value="status"
              style="width: 140px"
              :options="statusOptions"
              @change="handleSearch"
            />
            <a-select
              v-model:value="featured"
              style="width: 140px"
              :options="featuredOptions"
              @change="handleSearch"
            />
            <a-input
              v-model:value="keyword"
              placeholder="按提示词名或发布者搜索"
              allow-clear
              style="width: 240px"
              @press-enter="handleSearch"
            />
            <a-button type="primary" @click="handleSearch">查询</a-button>
            <a-button @click="handleReset">重置</a-button>
            <a-button type="primary" @click="openCreateModal">
              <template #icon><PlusOutlined /></template>
              新建提示词
            </a-button>
            <a-button
              danger
              :disabled="selectedRowKeys?.length === 0"
              @click="onBatchDelete"
            >
              <template #icon><DeleteOutlined /></template>
              批量删除
            </a-button>
          </div>

          <a-table
            :columns="columns"
            :data-source="list"
            :loading="loading"
            :pagination="false"
            :scroll="{ x: 1300 }"
            :row-selection="rowSelection"
            row-key="id"
            size="middle"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <a-tag :color="record.status === 'enabled' ? 'green' : 'default'">
                  {{ record.status === 'enabled' ? '已上架' : '已下架' }}
                </a-tag>
              </template>
              <template v-else-if="column.key === 'featured'">
                <a-tag :color="record.featured === 1 ? 'red' : 'default'">
                  {{ record.featured === 1 ? '官方精选' : '普通' }}
                </a-tag>
              </template>
              <template v-else-if="column.key === 'description'">
                <span class="cell-ellipsis" :title="record.description">{{ record.description || '—' }}</span>
              </template>
              <template v-else-if="column.key === 'publisherName'">
                <span>{{ record.publisherName || record.publisherUserId }}</span>
              </template>
              <template v-else-if="column.key === 'actions'">
                <a-button type="link" size="small" @click="openDetailModal(record)">详情</a-button>
                <a-button type="link" size="small" @click="openEditModal(record)">编辑</a-button>
                <a-popconfirm
                  :title="record.status === 'enabled' ? '确定下架此提示词？' : '确定上架此提示词？'"
                  ok-text="确定"
                  cancel-text="取消"
                  @confirm="toggleStatus(record)"
                >
                  <a-button type="link" size="small" :danger="record.status === 'enabled'">{{ record.status === 'enabled' ? '下架' : '上架' }}</a-button>
                </a-popconfirm>
                <a-popconfirm
                  title="确定删除此提示词市场条目？"
                  ok-text="删除"
                  cancel-text="取消"
                  @confirm="confirmDelete(record)"
                >
                  <a-button type="link" size="small" danger>删除</a-button>
                </a-popconfirm>
              </template>
            </template>
          </a-table>

          <div class="market-style-pagination">
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
        </a-tab-pane>

        <a-tab-pane key="stats" tab="数据概览">
          <div class="stats-panel">
            <a-spin :spinning="statsLoading">
              <div class="stats-cards">
                <a-card v-for="card in statCards" :key="card.label" :bordered="false" class="stat-card">
                  <div class="stat-label">{{ card.label }}</div>
                  <div class="stat-value">{{ card.value }}</div>
                </a-card>
              </div>

              <a-card :bordered="false" class="chart-card">
                <div class="chart-header">
                  <h4 class="chart-title">近7天使用趋势</h4>
                </div>
                <v-chart :option="trendOption" style="height: 320px" autoresize />
              </a-card>

              <div class="stats-tables">
                <a-card :bordered="false" class="stat-table-card" title="热门提示词 Top10">
                  <a-table
                    :columns="topSkillColumns"
                    :data-source="topSkills"
                    :pagination="false"
                    :row-key="(record, index) => index"
                    size="small"
                  >
                    <template #bodyCell="{ column, index }">
                      <template v-if="column.key === 'rank'">
                        <span>{{ index + 1 }}</span>
                      </template>
                    </template>
                  </a-table>
                </a-card>
                <a-card :bordered="false" class="stat-table-card" title="热门发布者 Top10">
                  <a-table
                    :columns="topPublisherColumns"
                    :data-source="topPublishers"
                    :pagination="false"
                    row-key="publisherUserId"
                    size="small"
                  >
                    <template #bodyCell="{ column, index }">
                      <template v-if="column.key === 'rank'">
                        <span>{{ index + 1 }}</span>
                      </template>
                    </template>
                  </a-table>
                </a-card>
              </div>
            </a-spin>
          </div>
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <!-- 新建 / 编辑 Modal -->
    <a-modal
      v-model:open="editorVisible"
      :title="editingBizNo ? '编辑提示词市场条目' : '新建提示词市场条目'"
      ok-text="保存"
      cancel-text="取消"
      :confirm-loading="submitting"
      :width="720"
      @ok="confirmSubmit"
    >
      <a-form :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
        <a-form-item label="提示词名称" required>
          <a-input
            v-model:value="form.skillName"
            placeholder="例如：爆款情感文"
            :maxlength="64"
            show-count
          />
        </a-form-item>
        <a-form-item label="发布者" required>
          <a-select
            v-model:value="form.publisherUserId"
            placeholder="搜索并选择发布者"
            show-search
            :filter-option="false"
            :options="publisherOptions"
            :loading="publisherLoading"
            @search="searchPublisher"
            @dropdown-visible-change="onPublisherDropdownOpen"
          />
        </a-form-item>
        <a-form-item label="简短描述">
          <a-input
            v-model:value="form.description"
            placeholder="一句话描述，方便用户浏览"
            :maxlength="256"
          />
        </a-form-item>
        <a-form-item label="提示词" required>
          <a-textarea
            v-model:value="form.prompt"
            placeholder="喂给 AI 的完整提示词"
            :rows="6"
          />
        </a-form-item>
        <a-form-item label="适用范围">
          <div class="market-style-scope-tags">
            <a-tag
              v-for="tag in scopeTags"
              :key="tag"
              closable
              :disable="!editingBizNo && scopeTags.length >= MAX_SCOPE_TAGS"
              @close.prevent="removeTag(tag)"
            >
              {{ tag }}
            </a-tag>
            <input
              v-if="scopeTags.length < MAX_SCOPE_TAGS"
              v-model="scopeInput"
              type="text"
              class="market-style-scope-input"
              placeholder="输入标签后按 Tab 或回车"
              :maxlength="MAX_SCOPE_TAG_LENGTH"
              @keydown.enter.prevent="addTag"
              @keydown.tab="handleScopeTab"
            />
          </div>
          <div class="market-style-scope-hint">
            最多 {{ MAX_SCOPE_TAGS }} 个标签，每个不超过 {{ MAX_SCOPE_TAG_LENGTH }} 个字（可选）
          </div>
        </a-form-item>
        <a-form-item label="累计使用">
          <a-input-number
            v-model:value="form.totalUses"
            :min="0"
            style="width: 160px"
          />
          <a-button
            v-if="editingBizNo"
            style="margin-left: 8px"
            @click="openSimulateUsageModal"
          >
            +1
          </a-button>
        </a-form-item>
        <a-form-item label="上架状态">
          <a-switch
            v-model:checked="form.enableStatus"
            checked-children="上架"
            un-checked-children="下架"
          />
        </a-form-item>
        <a-form-item label="官方精选">
          <a-switch
            v-model:checked="form.featured"
            checked-children="精选"
            un-checked-children="普通"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 模拟使用 Modal -->
    <a-modal
      v-model:open="simulateUsageVisible"
      title="模拟使用一次"
      ok-text="确认"
      cancel-text="取消"
      :confirm-loading="simulateUsageLoading"
      @ok="confirmSimulateUsage"
    >
      <a-form :label-col="{ span: 5 }" :wrapper-col="{ span: 19 }">
        <a-form-item label="选择使用者" required>
          <a-select
            v-model:value="simulateUsageUserId"
            placeholder="搜索并选择使用者"
            show-search
            :filter-option="false"
            :options="simulateUserOptions"
            :loading="simulateUserLoading"
            @search="searchSimulateUser"
            @dropdown-visible-change="onSimulateUserDropdownOpen"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 详情 Modal -->
    <a-modal
      v-model:open="detailVisible"
      title="提示词详情"
      :footer="null"
      :width="800"
    >
      <a-tabs v-model:activeKey="detailActiveTab" @change="onDetailTabChange">
        <a-tab-pane key="basic" tab="基本信息">
          <a-descriptions bordered :column="2" size="small">
            <a-descriptions-item label="ID">{{ detailRecord.id }}</a-descriptions-item>
            <a-descriptions-item label="提示词名称">{{ detailRecord.name }}</a-descriptions-item>
            <a-descriptions-item label="发布者">{{ detailRecord.publisherName || detailRecord.publisherUserId }}</a-descriptions-item>
            <a-descriptions-item label="价格">{{ detailRecord.price }}</a-descriptions-item>
            <a-descriptions-item label="累计使用">{{ detailRecord.totalUses }}</a-descriptions-item>
            <a-descriptions-item label="本周使用">{{ detailRecord.weeklyUses }}</a-descriptions-item>
            <a-descriptions-item label="本周收益">{{ detailRecord.weeklyEarnings }}</a-descriptions-item>
            <a-descriptions-item label="里程碑奖励">{{ detailRecord.milestoneBonus }}</a-descriptions-item>
            <a-descriptions-item label="官方精选">
              <a-tag :color="detailRecord.featured === 1 ? 'red' : 'default'">
                {{ detailRecord.featured === 1 ? '官方精选' : '普通' }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="状态">
              <a-tag :color="detailRecord.status === 'enabled' ? 'green' : 'default'">
                {{ detailRecord.status === 'enabled' ? '已上架' : '已下架' }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="创建时间">{{ detailRecord.createdAt }}</a-descriptions-item>
            <a-descriptions-item label="适用范围" :span="2">{{ detailRecord.scope || '—' }}</a-descriptions-item>
            <a-descriptions-item label="提示词摘要" :span="2">{{ detailRecord.promptSummary || '—' }}</a-descriptions-item>
            <a-descriptions-item label="描述" :span="2">{{ detailRecord.description || '—' }}</a-descriptions-item>
            <a-descriptions-item label="提示词" :span="2">
              <a-textarea :value="detailRecord.prompt" :rows="8" disabled />
            </a-descriptions-item>
          </a-descriptions>
        </a-tab-pane>
        <a-tab-pane key="usage" tab="使用记录">
          <a-table
            :columns="usageColumns"
            :data-source="usageRecords"
            :loading="usageLoading"
            :pagination="false"
            row-key="taskBizNo"
            size="small"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'userNickname'">
                <span>{{ record.userNickname || `用户${record.userId}` }}</span>
              </template>
            </template>
          </a-table>
          <div class="detail-pagination">
            <a-pagination
              :current="usagePage"
              :page-size="usagePageSize"
              :total="usageTotal"
              :page-size-options="['10', '20', '50']"
              show-size-changer
              show-total
              @change="handleUsagePaginationChange"
              @show-size-change="handleUsagePaginationChange"
            />
          </div>
        </a-tab-pane>
      </a-tabs>
    </a-modal>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { PlusOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import { message, Modal } from 'ant-design-vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import { useMarketStyleManagement } from '@/composables/useMarketStyleManagement.js'
import { useMarketSkillStats } from '@/composables/useMarketSkillStats.js'
import { useScopeTags } from '@/composables/useScopeTags.js'
import { listUserOptions } from '@/api/userOptions.js'
import { simulateMarketSkillUsage } from '@/api/marketSkill.js'

use([CanvasRenderer, LineChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

const activeTab = ref('list')
const detailActiveTab = ref('basic')
const selectedRowKeys = ref([])

const rowSelection = {
  selectedRowKeys,
  onChange: (keys) => {
    selectedRowKeys.value = keys
  }
}

const {
  list,
  total,
  loading,
  submitting,
  page,
  pageSize,
  keyword,
  status,
  featured,
  fetch,
  handleSearch,
  handleReset,
  handlePageChange,
  handleCreate,
  handleUpdate,
  handleDelete,
  handleBatchDelete
} = useMarketStyleManagement()

const {
  stats,
  statsLoading,
  fetchStats,
  usageRecords,
  usageTotal,
  usagePage,
  usagePageSize,
  usageLoading,
  fetchUsageRecords,
  handleUsagePageChange,
  resetUsageRecords
} = useMarketSkillStats()

const onTabChange = (key) => {
  if (key === 'stats' && !stats.value && !statsLoading.value) {
    fetchStats()
  }
}

const onDetailTabChange = (key) => {
  if (key === 'usage' && detailRecord.value?.id) {
    fetchUsageRecords(detailRecord.value.id)
  }
}

const statCards = computed(() => {
  const o = stats.value?.overview || {}
  return [
    { label: '总提示词', value: o.totalSkills ?? 0 },
    { label: '已上架', value: o.enabledSkills ?? 0 },
    { label: '官方精选', value: o.featuredSkills ?? 0 },
    { label: '累计使用', value: o.totalUses ?? 0 },
    { label: '本周使用', value: o.weeklyUses ?? 0 },
    { label: '累计收益', value: `¥${o.totalEarnings ?? '0.00'}` },
    { label: '本周收益', value: `¥${o.weeklyEarnings ?? '0.00'}` }
  ]
})

const topSkills = computed(() => stats.value?.topSkills || [])
const topPublishers = computed(() => stats.value?.topPublishers || [])
const trendData = computed(() => stats.value?.usageTrend || [])

const trendOption = computed(() => {
  const data = trendData.value
  const dates = data.map(item => item.date)
  const uses = data.map(item => item.uses)
  const earnings = data.map(item => item.earnings)
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['使用次数', '收益(元)'] },
    grid: { left: 60, right: 60, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: dates },
    yAxis: [
      { type: 'value', name: '使用次数', position: 'left' },
      { type: 'value', name: '收益(元)', position: 'right' }
    ],
    series: [
      {
        name: '使用次数',
        type: 'line',
        data: uses,
        smooth: true,
        itemStyle: { color: '#07c160' },
        areaStyle: { color: 'rgba(7,193,96,0.1)' }
      },
      {
        name: '收益(元)',
        type: 'line',
        yAxisIndex: 1,
        data: earnings,
        smooth: true,
        itemStyle: { color: '#1890ff' }
      }
    ]
  }
})

const topSkillColumns = [
  { title: '排名', key: 'rank', width: 60 },
  { title: '提示词名称', dataIndex: 'skillName', key: 'skillName' },
  { title: '发布者', dataIndex: 'publisherName', key: 'publisherName' },
  { title: '累计使用', dataIndex: 'totalUses', key: 'totalUses', width: 100 },
  { title: '本周使用', dataIndex: 'weeklyUses', key: 'weeklyUses', width: 100 }
]

const topPublisherColumns = [
  { title: '排名', key: 'rank', width: 60 },
  { title: '发布者', dataIndex: 'publisherName', key: 'publisherName' },
  { title: '提示词数', dataIndex: 'skillCount', key: 'skillCount', width: 100 },
  { title: '累计使用', dataIndex: 'totalUses', key: 'totalUses', width: 100 },
  { title: '本周收益', dataIndex: 'weeklyEarnings', key: 'weeklyEarnings', width: 120 }
]

const usageColumns = [
  { title: '使用者', dataIndex: 'userNickname', key: 'userNickname' },
  { title: '任务编号', dataIndex: 'taskBizNo', key: 'taskBizNo', width: 180 },
  { title: '文章编号', dataIndex: 'articleBizNo', key: 'articleBizNo', width: 180 },
  { title: '完成时间', dataIndex: 'completedAt', key: 'completedAt', width: 170 }
]

const handleUsagePaginationChange = (newPage, newPageSize) => {
  handleUsagePageChange(newPage, newPageSize)
  if (detailRecord.value?.id) {
    fetchUsageRecords(detailRecord.value.id)
  }
}

const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '已上架', value: 1 },
  { label: '已下架', value: 0 }
]

const featuredOptions = [
  { label: '全部精选', value: '' },
  { label: '官方精选', value: 1 },
  { label: '普通', value: 0 }
]

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 160 },
  { title: '提示词名称', dataIndex: 'name', key: 'name', width: 140 },
  { title: '描述', dataIndex: 'description', key: 'description', width: 160 },
  { title: '发布者', dataIndex: 'publisherName', key: 'publisherName', width: 110 },
  { title: '累计使用', dataIndex: 'totalUses', key: 'totalUses', width: 90 },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 170 },
  { title: '官方精选', dataIndex: 'featured', key: 'featured', width: 90 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
  { title: '操作', key: 'actions', width: 240, fixed: 'right' }
]

const editorVisible = ref(false)
const editingBizNo = ref(null)
const publisherOptions = ref([])
const publisherLoading = ref(false)
const publisherKeyword = ref('')

const detailVisible = ref(false)
const detailRecord = ref({})
const simulateUsageVisible = ref(false)
const simulateUsageLoading = ref(false)
const simulateUsageUserId = ref(null)
const simulateUserOptions = ref([])
const simulateUserLoading = ref(false)
const simulateUserKeyword = ref('')

const openDetailModal = (record) => {
  detailRecord.value = { ...record }
  detailActiveTab.value = 'basic'
  detailVisible.value = true
  resetUsageRecords()
}

const scopeRef = ref('')
const {
  scopeInput,
  scopeTags,
  scopeError,
  addTag,
  removeTag,
  MAX_SCOPE_TAGS,
  MAX_SCOPE_TAG_LENGTH
} = useScopeTags(scopeRef)

const form = reactive({
  skillName: '',
  publisherUserId: null,
  description: '',
  prompt: '',
  totalUses: 0,
  enableStatus: true,
  featured: false
})

function resetForm() {
  form.skillName = ''
  form.publisherUserId = null
  form.description = ''
  form.prompt = ''
  scopeRef.value = ''
  scopeInput.value = ''
  form.totalUses = 0
  form.enableStatus = true
  form.featured = false
  publisherOptions.value = []
}

const searchPublisher = async (kw) => {
  publisherKeyword.value = kw
  await loadPublisherOptions(kw)
}

const handleScopeTab = (e) => {
  const raw = scopeInput.value?.trim()
  if (raw) {
    e.preventDefault()
    addTag()
  }
}

const onPublisherDropdownOpen = async (open) => {
  if (open && publisherOptions.value.length === 0) {
    await loadPublisherOptions(publisherKeyword.value)
  }
}

const loadPublisherOptions = async (kw = '') => {
  publisherLoading.value = true
  try {
    const users = await listUserOptions(kw, 20)
    publisherOptions.value = users.map((u) => ({
      label: u.nickname ? `${u.nickname}（${u.email}）` : u.email,
      value: u.id
    }))
  } catch (error) {
    message.error(error.message || '加载发布者失败')
  } finally {
    publisherLoading.value = false
  }
}

const openSimulateUsageModal = () => {
  simulateUsageUserId.value = null
  simulateUserOptions.value = []
  simulateUserKeyword.value = ''
  simulateUsageVisible.value = true
}

const searchSimulateUser = async (kw) => {
  simulateUserKeyword.value = kw
  await loadSimulateUserOptions(kw)
}

const onSimulateUserDropdownOpen = async (open) => {
  if (open && simulateUserOptions.value.length === 0) {
    await loadSimulateUserOptions(simulateUserKeyword.value)
  }
}

const loadSimulateUserOptions = async (kw = '') => {
  simulateUserLoading.value = true
  try {
    const users = await listUserOptions(kw, 20)
    simulateUserOptions.value = users.map((u) => ({
      label: u.nickname ? `${u.nickname}（${u.email}）` : u.email,
      value: u.id
    }))
  } catch (error) {
    message.error(error.message || '加载用户失败')
  } finally {
    simulateUserLoading.value = false
  }
}

const confirmSimulateUsage = async () => {
  if (!simulateUsageUserId.value) {
    message.error('请选择使用者')
    return
  }
  simulateUsageLoading.value = true
  try {
    await simulateMarketSkillUsage(editingBizNo.value, simulateUsageUserId.value)
    message.success('已模拟使用一次，发布者已获得收益')
    form.totalUses = (form.totalUses || 0) + 1
    simulateUsageVisible.value = false
    await fetch()
  } catch (error) {
    message.error(error.message || '模拟使用失败')
  } finally {
    simulateUsageLoading.value = false
  }
}
const openCreateModal = () => {
  editingBizNo.value = null
  resetForm()
  editorVisible.value = true
}

const openEditModal = (record) => {
  editingBizNo.value = record.id
  form.skillName = record.name || ''
  form.publisherUserId = record.publisherUserId
  form.description = record.description || ''
  form.prompt = record.prompt || ''
  scopeRef.value = record.scope || ''
  scopeInput.value = ''
  form.totalUses = record.totalUses || 0
  form.enableStatus = record.status === 'enabled'
  form.featured = record.featured === 1
  publisherOptions.value = [{
    label: record.publisherName ? `${record.publisherName}（${record.publisherUserId}）` : String(record.publisherUserId),
    value: record.publisherUserId
  }]
  editorVisible.value = true
}

const confirmSubmit = async () => {
  if (!form.skillName.trim() || !form.prompt.trim()) {
    message.error('请填写提示词名称和提示词')
    return
  }
  if (form.publisherUserId == null) {
    message.error('请选择发布者')
    return
  }
  if (scopeError.value) {
    message.error(scopeError.value)
    return
  }
  const payload = {
    skillName: form.skillName.trim(),
    publisherUserId: form.publisherUserId,
    description: form.description || '',
    prompt: form.prompt.trim(),
    scope: scopeRef.value || '',
    totalUses: form.totalUses || 0,
    enableStatus: form.enableStatus ? 1 : 0,
    featured: form.featured ? 1 : 0
  }
  const ok = editingBizNo.value
    ? await handleUpdate(editingBizNo.value, payload)
    : await handleCreate(payload)
  if (ok) {
    editorVisible.value = false
  }
}

const confirmDelete = async (record) => {
  await handleDelete(record.id)
}

const onBatchDelete = () => {
  const keys = selectedRowKeys.value
  if (!keys || keys.length === 0) {
    message.warning('请先选择要删除的条目')
    return
  }
  Modal.confirm({
    title: `确定批量删除 ${keys.length} 条提示词市场条目？`,
    content: '删除后可在数据库中恢复，前端列表将不再展示。',
    okText: '删除',
    cancelText: '取消',
    okButtonProps: { danger: true },
    onOk: async () => {
      try {
        const ok = await handleBatchDelete(keys)
        if (ok) {
          selectedRowKeys.value = []
        }
      } catch (e) {
        message.error(e.message || '批量删除失败')
      }
    }
  })
}

const toggleStatus = async (record) => {
  const nextStatus = record.status === 'enabled' ? 0 : 1
  const payload = {
    skillName: record.name,
    publisherUserId: record.publisherUserId,
    description: record.description || '',
    prompt: record.prompt || '',
    promptSummary: record.promptSummary || '',
    scope: record.scope || '',
    totalUses: record.totalUses || 0,
    enableStatus: nextStatus,
    featured: record.featured || 0
  }
  const ok = await handleUpdate(record.id, payload)
  if (ok) {
    message.success(nextStatus === 1 ? '提示词已上架' : '提示词已下架')
  }
}

onMounted(() => {
  fetch()
})
</script>

<style scoped>
.market-style-card {
  border-radius: 8px;
}

.market-style-header {
  margin-bottom: 16px;
}

.market-style-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 4px 0;
}

.market-style-desc {
  font-size: 13px;
  color: #8c8c8c;
  margin: 0;
}

.market-style-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  align-items: center;
}

.market-style-pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.cell-ellipsis {
  display: inline-block;
  width: 100%;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
}

.market-style-scope-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.market-style-scope-input {
  min-width: 160px;
  flex: 1;
  padding: 4px 8px;
  border: 1px dashed #d9d9d9;
  border-radius: 4px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
}

.market-style-scope-input:focus {
  border-color: #ff2442;
}

.market-style-scope-input::placeholder {
  color: #bfbfbf;
}

.market-style-scope-hint {
  margin-top: 4px;
  font-size: 12px;
  color: #8c8c8c;
}

.stats-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-card {
  text-align: center;
}

.stat-label {
  font-size: 13px;
  color: #8c8c8c;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 22px;
  font-weight: 600;
  color: #262626;
}

.chart-card {
  border-radius: 8px;
}

.chart-header {
  margin-bottom: 8px;
}

.chart-title {
  font-size: 15px;
  font-weight: 600;
  margin: 0;
}

.stats-tables {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.stat-table-card {
  border-radius: 8px;
}

.detail-pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 768px) {
  .stats-cards {
    grid-template-columns: repeat(2, 1fr);
  }
  .stats-tables {
    grid-template-columns: 1fr;
  }
}
</style>
