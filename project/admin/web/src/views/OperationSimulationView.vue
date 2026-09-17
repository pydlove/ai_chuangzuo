<template>
  <div class="simulation-admin">
    <a-page-header title="模拟运营" sub-title="批量生成机器人用户，走真实用户端接口产生运营数据" />

    <div class="filter-card">
      <span class="filter-label">统计数据包含机器人数据</span>
      <a-switch v-model:checked="includeRobots" :loading="filterLoading" @change="onToggleIncludeRobots" />
      <span class="filter-hint">关闭后，概览与订单统计将排除模拟机器人产生的数据（约 1 分钟内生效）</span>
    </div>

    <a-tabs v-model:activeKey="activeKey" class="simulation-tabs" @change="onTabChange">
      <!-- 模拟新用户 -->
      <a-tab-pane key="new-users" tab="模拟新用户">
        <div class="section-bar">
          <span class="section-title">模拟批次</span>
          <a-button type="primary" @click="openCreateModal">创建批次</a-button>
        </div>

        <a-table :columns="batchColumns" :data-source="batches" :loading="loading"
                 :pagination="batchPagination" row-key="id" @change="handleBatchTableChange">
          <template #bodyCell="{ column, record }">
            <span v-if="column.key === 'progress'">
              {{ record.completedCount || 0 }}/{{ record.totalCount || 0 }}
              <span v-if="record.failedCount > 0" class="failed-count">+{{ record.failedCount }}</span>
            </span>
            <span v-else-if="column.key === 'status'">
              <a-tag :color="batchStatusColor(record.status)">{{ batchStatusText(record.status) }}</a-tag>
            </span>
            <span v-else-if="column.key === 'plan'">{{ record.planName }}（{{ record.cycle }}）</span>
            <span v-else-if="column.key === 'createdAt'">{{ formatTime(record.createdAt) }}</span>
            <span v-else-if="column.key === 'action'">
              <a-space>
                <a-button size="small" type="link" @click="openDetail(record)">详情</a-button>
                <a-popconfirm v-if="record.status === 'PENDING' || record.status === 'RUNNING'"
                              title="确认取消该批次？未执行的机器人将停止" @confirm="cancelBatchById(record.id)">
                  <a-button size="small" type="link" danger>取消</a-button>
                </a-popconfirm>
              </a-space>
            </span>
            <span v-else>{{ record[column.key] }}</span>
          </template>
        </a-table>
      </a-tab-pane>

      <!-- 模拟生成文章 -->
      <a-tab-pane key="free-create" tab="模拟生成文章">
        <div class="section-bar">
          <span class="section-title">模拟生成文章批次</span>
          <a-button type="primary" @click="openFreeCreateModal">创建批次</a-button>
        </div>

        <a-table :columns="fcBatchColumns" :data-source="fcBatches" :loading="fcLoading"
                 :pagination="fcPagination" row-key="id" @change="handleFcTableChange">
          <template #bodyCell="{ column, record }">
            <span v-if="column.key === 'progress'">
              {{ record.completedCount || 0 }}/{{ record.totalCount || 0 }}
              <span v-if="record.failedCount > 0" class="failed-count">+{{ record.failedCount }}</span>
            </span>
            <span v-else-if="column.key === 'status'">
              <a-tag :color="batchStatusColor(record.status)">{{ batchStatusText(record.status) }}</a-tag>
            </span>
            <span v-else-if="column.key === 'scope'">{{ scopeText(record.stageConfig) }}</span>
            <span v-else-if="column.key === 'createdAt'">{{ formatTime(record.createdAt) }}</span>
            <span v-else-if="column.key === 'action'">
              <a-space>
                <a-button size="small" type="link" @click="openDetail(record)">详情</a-button>
                <a-popconfirm v-if="record.status === 'PENDING' || record.status === 'RUNNING'"
                              title="确认取消该批次？未执行的用户将停止" @confirm="cancelFcBatch(record.id)">
                  <a-button size="small" type="link" danger>取消</a-button>
                </a-popconfirm>
              </a-space>
            </span>
            <span v-else>{{ record[column.key] }}</span>
          </template>
        </a-table>
      </a-tab-pane>

      <!-- 昵称库 -->
      <a-tab-pane key="nickname-lib" tab="昵称库">
        <div class="section-bar">
          <span class="section-title">昵称库 <a-tag color="blue">{{ nicknameCount }}</a-tag></span>
          <a-space>
            <a-input-search v-model:value="nicknameKeyword" placeholder="搜索昵称" style="width: 200px" @search="onNicknameSearch" />
            <a-button @click="downloadNicknameTemplate">下载模板</a-button>
            <a-upload accept=".xlsx" :show-upload-list="false" :before-upload="importNicknameExcelFile">
              <a-button :loading="nicknameImporting">Excel 导入</a-button>
            </a-upload>
            <a-button type="primary" @click="openNicknameModal">批量添加</a-button>
          </a-space>
        </div>
        <a-table :columns="nicknameColumns" :data-source="nicknames" :loading="nicknameLoading"
                 :pagination="nicknamePagination" row-key="id" @change="handleNicknameTableChange">
          <template #bodyCell="{ column, record }">
            <span v-if="column.key === 'createdAt'">{{ formatTime(record.createdAt) }}</span>
            <span v-else-if="column.key === 'action'">
              <a-popconfirm title="确认删除该昵称？" @confirm="removeNickname(record.id)">
                <a-button size="small" type="link" danger>删除</a-button>
              </a-popconfirm>
            </span>
            <span v-else>{{ record[column.key] }}</span>
          </template>
        </a-table>
      </a-tab-pane>

      <!-- 头像库 -->
      <a-tab-pane key="avatar-lib" tab="头像库">
        <div class="section-bar">
          <span class="section-title">头像库 <a-tag color="blue">{{ avatarCount }}</a-tag></span>
          <a-space>
            <span class="filter-hint">支持多选，上传后自动压缩（最长边 256px）</span>
            <a-upload accept="image/jpeg,image/png" multiple :show-upload-list="false" :before-upload="pickAvatarFiles">
              <a-button>选择图片</a-button>
            </a-upload>
            <a-button type="primary" :disabled="avatarPending.length === 0" :loading="avatarUploading"
                      @click="submitAvatarUpload">
              上传（{{ avatarPending.length }}）
            </a-button>
          </a-space>
        </div>
        <a-empty v-if="!avatarLoading && avatars.length === 0" description="暂无头像，请先上传" />
        <div class="avatar-grid">
          <div v-for="a in avatars" :key="a.id" class="avatar-cell">
            <img :src="a.dataUrl" alt="avatar" />
            <div class="avatar-cell-footer">
              <span class="avatar-cell-time">{{ formatTime(a.createdAt) }}</span>
              <a-popconfirm title="确认删除该头像？" @confirm="removeAvatar(a.id)">
                <a-button size="small" type="link" danger>删除</a-button>
              </a-popconfirm>
            </div>
          </div>
        </div>
        <a-pagination v-if="avatarTotal > 0" class="avatar-pagination"
                      v-model:current="avatarQuery.page" v-model:page-size="avatarQuery.size"
                      :total="avatarTotal" :show-size-changer="true"
                      @change="loadAvatars" />
      </a-tab-pane>
    </a-tabs>

    <!-- 批量添加昵称 -->
    <a-modal v-model:open="nicknameModalVisible" title="批量添加昵称" :confirm-loading="nicknameSaving" @ok="submitNicknameBatch">
      <a-alert type="info" show-icon style="margin-bottom: 12px" message="每行一个昵称，重复或空行自动忽略。" />
      <a-textarea v-model:value="nicknameBatchText" :rows="10" placeholder="晚风轻轻吹&#10;山间的雾" />
    </a-modal>

    <!-- 创建批次 -->
    <a-modal v-model:open="createModalVisible" title="创建模拟批次" width="640px"
             :confirm-loading="creating" @ok="submitCreate">
      <a-form :model="createForm" layout="vertical">
        <a-form-item label="生成数量（1-500）" required>
          <a-input-number v-model:value="createForm.userCount" :min="1" :max="500" style="width: 200px" />
        </a-form-item>
        <a-form-item label="会员版本" required>
          <a-select v-model:value="createForm.planKey" style="width: 200px" placeholder="选择套餐">
            <a-select-option v-for="p in planOptions" :key="p.planKey" :value="p.planKey">
              {{ p.displayName }}（{{ p.planKey }}）
            </a-select-option>
          </a-select>
          <a-select v-model:value="createForm.cycle" style="width: 130px; margin-left: 8px">
            <a-select-option value="month">按月</a-select-option>
            <a-select-option value="quarter">按季</a-select-option>
            <a-select-option value="year">按年</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="自由创作提示词范围" required>
          <a-radio-group v-model:value="createForm.promptScope">
            <a-radio value="ALL">所有人</a-radio>
            <a-radio value="ROBOT">仅机器人</a-radio>
            <a-radio value="REAL">仅真实用户</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item v-for="s in stageFields" :key="s.key" :label="s.label">
          <a-checkbox v-model:checked="createForm[s.enabledKey]">执行</a-checkbox>
          <a-input-number v-model:value="createForm[s.probKey]" :min="0" :max="100" :disabled="!createForm[s.enabledKey]"
                          style="width: 100px; margin-left: 12px" />
          <span class="prob-suffix">% 概率</span>
        </a-form-item>
        <a-form-item label="用户间隔（秒）">
          <a-input-number v-model:value="createForm.userIntervalMin" :min="0" style="width: 100px" />
          <span class="interval-sep">~</span>
          <a-input-number v-model:value="createForm.userIntervalMax" :min="0" style="width: 100px" />
        </a-form-item>
        <a-form-item label="阶段间隔（秒）">
          <a-input-number v-model:value="createForm.stageIntervalMin" :min="0" style="width: 100px" />
          <span class="interval-sep">~</span>
          <a-input-number v-model:value="createForm.stageIntervalMax" :min="0" style="width: 100px" />
        </a-form-item>
        <a-form-item label="备注">
          <a-input v-model:value="createForm.remark" maxlength="200" placeholder="可空" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 创建模拟生成文章批次 -->
    <a-modal v-model:open="fcModalVisible" title="创建模拟生成文章批次" width="560px"
             :confirm-loading="fcCreating" @ok="submitFreeCreate">
      <a-alert type="info" show-icon style="margin-bottom: 16px"
               message="随机抽取存量真实用户，随机使用市场提示词，使提示词作者获得创作币收益；不真正生成文章，不消耗 token。" />
      <a-form :model="fcForm" layout="vertical">
        <a-form-item label="用户数量（1-500）" required>
          <a-input-number v-model:value="fcForm.userCount" :min="1" :max="500" style="width: 200px" />
        </a-form-item>
        <a-form-item label="提示词范围（按发布者）" required>
          <a-radio-group v-model:value="fcForm.promptScope">
            <a-radio value="REAL">真实用户</a-radio>
            <a-radio value="ROBOT">机器人</a-radio>
            <a-radio value="ALL">全部</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="用户间隔（秒）">
          <a-input-number v-model:value="fcForm.userIntervalMin" :min="0" style="width: 100px" />
          <span class="interval-sep">~</span>
          <a-input-number v-model:value="fcForm.userIntervalMax" :min="0" style="width: 100px" />
        </a-form-item>
        <a-form-item label="备注">
          <a-input v-model:value="fcForm.remark" maxlength="200" placeholder="可空" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 批次详情 -->
    <a-drawer v-model:open="detailDrawerVisible" width="720px" :title="`批次详情 · ${detailBatchNo}`">
      <a-table :columns="robotColumns" :data-source="detailRobots" :loading="detailLoading"
               :pagination="false" row-key="id" size="small">
        <template #bodyCell="{ column, record }">
          <span v-if="column.key === 'email'">
            <a-tooltip v-if="record.email" :title="record.email" placement="topLeft">
              <span>{{ record.nickname || record.email }}</span>
            </a-tooltip>
            <span v-else>{{ record.nickname || '-' }}</span>
          </span>
          <span v-else-if="column.key === 'status'">
            <a-tag :color="robotStatusColor(record.status)">{{ robotStatusText(record.status) }}</a-tag>
          </span>
          <span v-else-if="column.key === 'currentStage'">{{ stageText(record.currentStage) }}</span>
          <span v-else-if="column.key === 'action'">
            <a-button size="small" type="link" @click="openRobotLogs(record)">日志</a-button>
          </span>
          <span v-else>{{ record[column.key] ?? '-' }}</span>
        </template>
      </a-table>

      <div v-if="logRobot" class="log-section">
        <div class="section-bar">
          <span class="section-title">执行日志 · {{ logRobot.nickname || logRobot.email }}</span>
        </div>
        <a-table :columns="logColumns" :data-source="logs" :loading="logLoading"
                 :pagination="false" row-key="id" size="small">
          <template #bodyCell="{ column, record }">
            <span v-if="column.key === 'stage'">{{ stageText(record.stage) }}</span>
            <span v-else-if="column.key === 'status'">
              <a-tag :color="logStatusColor(record.status)">{{ record.status }}</a-tag>
            </span>
            <span v-else-if="column.key === 'detail'">
              <a-tooltip v-if="record.detail" :title="record.detail" placement="topLeft">
                <span class="cell-ellipsis">{{ record.detail }}</span>
              </a-tooltip>
              <span v-else>-</span>
            </span>
            <span v-else-if="column.key === 'errorMsg'">
              <a-tooltip v-if="record.errorMsg" :title="record.errorMsg" placement="topLeft">
                <span class="cell-ellipsis">{{ record.errorMsg }}</span>
              </a-tooltip>
              <span v-else>-</span>
            </span>
            <span v-else-if="column.key === 'createdAt'">{{ formatTime(record.createdAt) }}</span>
            <span v-else>{{ record[column.key] ?? '-' }}</span>
          </template>
        </a-table>
      </div>
    </a-drawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { createBatch, createFreeCreateBatch, listBatches, getBatch, listBatchLogs, cancelBatch, getStatsFilter, updateStatsFilter, listNicknames, countNicknames, addNicknames, importNicknamesExcel, deleteNickname, listAvatars, countAvatars, uploadAvatars, deleteAvatar } from '@/api/simulation'
import { fetchPlans } from '@/api/plan.js'
import request from '@/utils/request.js'

const activeKey = ref('new-users')
// ---------- 统计过滤开关 ----------
const includeRobots = ref(true)
const filterLoading = ref(false)

async function loadStatsFilter() {
  try {
    const res = await getStatsFilter()
    includeRobots.value = res?.includeRobots !== false
  } catch (e) {
    // 开关加载失败不阻断页面
  }
}

async function onToggleIncludeRobots(checked) {
  filterLoading.value = true
  try {
    await updateStatsFilter(checked)
    message.success(checked ? '已开启：统计包含机器人数据' : '已关闭：统计将排除机器人数据')
  } catch (e) {
    includeRobots.value = !checked
    message.error(e.message || '开关更新失败')
  } finally {
    filterLoading.value = false
  }
}

// ---------- 批次列表 ----------
const batches = ref([])
const loading = ref(false)
const batchQuery = ref({ page: 1, size: 20 })
const batchTotal = ref(0)
const batchPagination = computed(() => ({
  current: batchQuery.value.page,
  pageSize: batchQuery.value.size,
  total: batchTotal.value,
  showSizeChanger: true
}))
let pollTimer = null

const batchColumns = [
  { title: '批次号', dataIndex: 'batchNo', key: 'batchNo' },
  { title: '机器人数', dataIndex: 'userCount', key: 'userCount', width: 90 },
  { title: '会员', key: 'plan', width: 140 },
  { title: '进度', key: 'progress', width: 110 },
  { title: '状态', key: 'status', width: 90 },
  { title: '创建时间', key: 'createdAt', width: 110 },
  { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true },
  { title: '操作', key: 'action', width: 120 }
]

async function loadBatches() {
  loading.value = true
  try {
    const res = await listBatches({ page: batchQuery.value.page, size: batchQuery.value.size, batchType: 'ROBOT_JOURNEY' })
    batches.value = res.items || []
    batchTotal.value = res.total || 0
    schedulePoll()
  } catch (e) {
    message.error(e.message || '加载批次失败')
  } finally {
    loading.value = false
  }
}

function schedulePoll() {
  clearTimeout(pollTimer)
  const hasRunning = [...(batches.value || []), ...(fcBatches.value || [])]
    .some((b) => b.status === 'RUNNING' || b.status === 'PENDING')
  if (hasRunning) {
    pollTimer = setTimeout(() => {
      loadBatches()
      loadFcBatches()
    }, 10000)
  }
}

function handleBatchTableChange(pagination) {
  batchQuery.value.page = pagination.current
  batchQuery.value.size = pagination.pageSize
  loadBatches()
}

async function cancelBatchById(id) {
  try {
    await cancelBatch(id)
    message.success('批次已取消')
    loadBatches()
  } catch (e) {
    message.error(e.message || '取消失败')
  }
}

// ---------- 模拟生成文章 ----------
const fcBatches = ref([])
const fcLoading = ref(false)
const fcQuery = ref({ page: 1, size: 20 })
const fcTotal = ref(0)
const fcPagination = computed(() => ({
  current: fcQuery.value.page,
  pageSize: fcQuery.value.size,
  total: fcTotal.value,
  showSizeChanger: true
}))

const fcBatchColumns = [
  { title: '批次号', dataIndex: 'batchNo', key: 'batchNo' },
  { title: '用户数', dataIndex: 'userCount', key: 'userCount', width: 90 },
  { title: '提示词范围', key: 'scope', width: 110 },
  { title: '进度', key: 'progress', width: 110 },
  { title: '状态', key: 'status', width: 90 },
  { title: '创建时间', key: 'createdAt', width: 110 },
  { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true },
  { title: '操作', key: 'action', width: 120 }
]

async function loadFcBatches() {
  fcLoading.value = true
  try {
    const res = await listBatches({ page: fcQuery.value.page, size: fcQuery.value.size, batchType: 'FREE_CREATE' })
    fcBatches.value = res.items || []
    fcTotal.value = res.total || 0
    schedulePoll()
  } catch (e) {
    message.error(e.message || '加载批次失败')
  } finally {
    fcLoading.value = false
  }
}

function handleFcTableChange(pagination) {
  fcQuery.value.page = pagination.current
  fcQuery.value.size = pagination.pageSize
  loadFcBatches()
}

async function cancelFcBatch(id) {
  try {
    await cancelBatch(id)
    message.success('批次已取消')
    loadFcBatches()
  } catch (e) {
    message.error(e.message || '取消失败')
  }
}

const fcModalVisible = ref(false)
const fcCreating = ref(false)

function defaultFcForm() {
  return {
    userCount: 10,
    promptScope: 'REAL',
    userIntervalMin: 10,
    userIntervalMax: 30,
    remark: ''
  }
}

const fcForm = ref(defaultFcForm())

function openFreeCreateModal() {
  fcForm.value = defaultFcForm()
  fcModalVisible.value = true
}

async function submitFreeCreate() {
  const f = fcForm.value
  if (f.userIntervalMin > f.userIntervalMax) {
    message.warning('用户间隔最小值不能大于最大值')
    return
  }
  fcCreating.value = true
  try {
    await createFreeCreateBatch({
      userCount: f.userCount,
      promptScope: f.promptScope,
      userIntervalMin: f.userIntervalMin,
      userIntervalMax: f.userIntervalMax,
      remark: f.remark || undefined
    })
    message.success('批次创建成功')
    fcModalVisible.value = false
    loadFcBatches()
  } catch (e) {
    message.error(e.message || '创建失败')
  } finally {
    fcCreating.value = false
  }
}

// ---------- 创建批次 ----------
const createModalVisible = ref(false)
const creating = ref(false)
const planOptions = ref([])

const stageFields = [
  { key: 'lottery', label: '参与抽奖', enabledKey: 'lotteryEnabled', probKey: 'lotteryProbability' },
  { key: 'membership', label: '购买会员', enabledKey: 'membershipEnabled', probKey: 'membershipProbability' },
  { key: 'create', label: '自由创作', enabledKey: 'createEnabled', probKey: 'createProbability' },
  { key: 'commission', label: '约稿投稿', enabledKey: 'commissionEnabled', probKey: 'commissionProbability' }
]

function defaultCreateForm() {
  return {
    userCount: 10,
    planKey: 'pro',
    cycle: 'month',
    promptScope: 'ALL',
    lotteryEnabled: true,
    lotteryProbability: 100,
    membershipEnabled: true,
    membershipProbability: 100,
    createEnabled: true,
    createProbability: 100,
    commissionEnabled: true,
    commissionProbability: 50,
    userIntervalMin: 10,
    userIntervalMax: 30,
    stageIntervalMin: 3,
    stageIntervalMax: 8,
    remark: ''
  }
}

const createForm = ref(defaultCreateForm())

async function openCreateModal() {
  createForm.value = defaultCreateForm()
  createModalVisible.value = true
  if (planOptions.value.length === 0) {
    try {
      planOptions.value = (await fetchPlans()) || []
    } catch (e) {
      planOptions.value = []
    }
  }
}

async function submitCreate() {
  const f = createForm.value
  if (f.userIntervalMin > f.userIntervalMax) {
    message.warning('用户间隔最小值不能大于最大值')
    return
  }
  if (f.stageIntervalMin > f.stageIntervalMax) {
    message.warning('阶段间隔最小值不能大于最大值')
    return
  }
  const plan = planOptions.value.find((p) => p.planKey === f.planKey)
  creating.value = true
  try {
    await createBatch({
      userCount: f.userCount,
      planKey: f.planKey,
      planName: plan ? plan.displayName : f.planKey,
      cycle: f.cycle,
      promptScope: f.promptScope,
      lotteryEnabled: f.lotteryEnabled,
      lotteryProbability: f.lotteryProbability,
      membershipEnabled: f.membershipEnabled,
      membershipProbability: f.membershipProbability,
      createEnabled: f.createEnabled,
      createProbability: f.createProbability,
      commissionEnabled: f.commissionEnabled,
      commissionProbability: f.commissionProbability,
      userIntervalMin: f.userIntervalMin,
      userIntervalMax: f.userIntervalMax,
      stageIntervalMin: f.stageIntervalMin,
      stageIntervalMax: f.stageIntervalMax,
      remark: f.remark || undefined
    })
    message.success('批次创建成功')
    createModalVisible.value = false
    loadBatches()
  } catch (e) {
    message.error(e.message || '创建失败')
  } finally {
    creating.value = false
  }
}

// ---------- 批次详情 + 日志 ----------
const detailDrawerVisible = ref(false)
const detailLoading = ref(false)
const detailBatchNo = ref('')
const detailRobots = ref([])

const robotColumns = [
  { title: '序号', dataIndex: 'seq', key: 'seq', width: 60 },
  { title: '邮箱', dataIndex: 'email', key: 'email', ellipsis: true },
  { title: '状态', key: 'status', width: 90 },
  { title: '当前阶段', key: 'currentStage', width: 90 },
  { title: '失败原因', dataIndex: 'failReason', key: 'failReason', ellipsis: true },
  { title: '操作', key: 'action', width: 70 }
]

const logRobot = ref(null)
const logs = ref([])
const logLoading = ref(false)

const logColumns = [
  { title: '阶段', key: 'stage', width: 90 },
  { title: '结果', key: 'status', width: 90 },
  { title: '明细', key: 'detail' },
  { title: '错误', dataIndex: 'errorMsg', key: 'errorMsg' },
  { title: '时间', key: 'createdAt', width: 110 }
]

async function openDetail(record) {
  detailBatchNo.value = record.batchNo
  detailRobots.value = []
  logRobot.value = null
  logs.value = []
  detailDrawerVisible.value = true
  detailLoading.value = true
  try {
    const res = await getBatch(record.id)
    detailRobots.value = (res && res.robots) || []
  } catch (e) {
    message.error(e.message || '加载详情失败')
  } finally {
    detailLoading.value = false
  }
}

async function openRobotLogs(robot) {
  logRobot.value = robot
  logLoading.value = true
  try {
    const res = await listBatchLogs(robot.batchId, { robotId: robot.id, page: 1, size: 100 })
    logs.value = (res && res.items) || []
  } catch (e) {
    message.error(e.message || '加载日志失败')
  } finally {
    logLoading.value = false
  }
}

// ---------- 展示辅助 ----------
function batchStatusText(status) {
  const map = { PENDING: '待执行', RUNNING: '进行中', COMPLETED: '已完成', CANCELED: '已取消' }
  return map[status] || status
}

function batchStatusColor(status) {
  const map = { PENDING: 'default', RUNNING: 'blue', COMPLETED: 'green', CANCELED: 'orange' }
  return map[status] || 'default'
}

function robotStatusText(status) {
  const map = { WAITING: '等待中', IN_PROGRESS: '执行中', COMPLETED: '已完成', FAILED: '失败', CANCELED: '已取消' }
  return map[status] || status
}

function robotStatusColor(status) {
  const map = { WAITING: 'default', IN_PROGRESS: 'blue', COMPLETED: 'green', FAILED: 'red', CANCELED: 'orange' }
  return map[status] || 'default'
}

function logStatusColor(status) {
  const map = { SUCCESS: 'green', FAILED: 'red', SKIPPED: 'default' }
  return map[status] || 'default'
}

function stageText(stage) {
  const map = {
    REGISTER: '注册', LOGIN: '登录', PROFILE: '资料', LOTTERY: '抽奖',
    MEMBERSHIP: '会员', CREATE: '创作', COMMISSION: '约稿', FREE_CREATE: '模拟创作'
  }
  return map[stage] || stage || '-'
}

function scopeText(stageConfig) {
  try {
    const scope = JSON.parse(stageConfig || '{}').promptScope
    const map = { REAL: '真实用户', ROBOT: '机器人', ALL: '全部' }
    return map[scope] || scope || '-'
  } catch (e) {
    return '-'
  }
}

function formatTime(t) {
  if (!t) return '-'
  return dayjs(t).format('MM-DD HH:mm')
}

function onTabChange(key) {
  if (key === 'free-create' && fcBatches.value.length === 0) {
    loadFcBatches()
  }
  if (key === 'nickname-lib') {
    loadNicknames()
    loadNicknameCount()
  }
  if (key === 'avatar-lib') {
    loadAvatars()
    loadAvatarCount()
  }
}

// ---------- 昵称库 ----------
const nicknames = ref([])
const nicknameLoading = ref(false)
const nicknameCount = ref(0)
const nicknameKeyword = ref('')
const nicknameQuery = ref({ page: 1, size: 20 })
const nicknameTotal = ref(0)
const nicknameImporting = ref(false)
const nicknameModalVisible = ref(false)
const nicknameSaving = ref(false)
const nicknameBatchText = ref('')

const nicknameColumns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 90 },
  { title: '昵称', dataIndex: 'nickname', key: 'nickname' },
  { title: '上传时间', key: 'createdAt', width: 130 },
  { title: '操作', key: 'action', width: 90 }
]

const nicknamePagination = computed(() => ({
  current: nicknameQuery.value.page,
  pageSize: nicknameQuery.value.size,
  total: nicknameTotal.value,
  showSizeChanger: true
}))

async function loadNicknames() {
  nicknameLoading.value = true
  try {
    const res = await listNicknames({
      page: nicknameQuery.value.page,
      size: nicknameQuery.value.size,
      keyword: nicknameKeyword.value || undefined
    })
    nicknames.value = res.items || []
    nicknameTotal.value = res.total || 0
  } catch (e) {
    message.error(e.message || '加载昵称库失败')
  } finally {
    nicknameLoading.value = false
  }
}

async function loadNicknameCount() {
  try {
    nicknameCount.value = (await countNicknames()) || 0
  } catch (e) {
    // 数量加载失败不阻断页面
  }
}

function onNicknameSearch() {
  nicknameQuery.value.page = 1
  loadNicknames()
}

function handleNicknameTableChange(pagination) {
  nicknameQuery.value.page = pagination.current
  nicknameQuery.value.size = pagination.pageSize
  loadNicknames()
}

function openNicknameModal() {
  nicknameBatchText.value = ''
  nicknameModalVisible.value = true
}

async function submitNicknameBatch() {
  const list = nicknameBatchText.value.split('\n').map((s) => s.trim()).filter(Boolean)
  if (list.length === 0) {
    message.warning('请输入至少一个昵称')
    return
  }
  nicknameSaving.value = true
  try {
    const res = await addNicknames(list)
    message.success(`已添加 ${res.inserted} 个昵称`)
    nicknameModalVisible.value = false
    loadNicknames()
    loadNicknameCount()
  } catch (e) {
    message.error(e.message || '添加失败')
  } finally {
    nicknameSaving.value = false
  }
}

async function importNicknameExcelFile(file) {
  nicknameImporting.value = true
  try {
    const res = await importNicknamesExcel(file)
    message.success(`导入完成，新增 ${res.inserted} 个昵称`)
    loadNicknames()
    loadNicknameCount()
  } catch (e) {
    message.error(e.message || '导入失败')
  } finally {
    nicknameImporting.value = false
  }
  return false
}

async function downloadNicknameTemplate() {
  try {
    const blob = await request.get('/simulation/library/nicknames/template', { responseType: 'blob' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '昵称导入模板.xlsx'
    a.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    message.error(e.message || '模板下载失败')
  }
}

async function removeNickname(id) {
  try {
    await deleteNickname(id)
    message.success('已删除')
    loadNicknames()
    loadNicknameCount()
  } catch (e) {
    message.error(e.message || '删除失败')
  }
}

// ---------- 头像库 ----------
const avatars = ref([])
const avatarLoading = ref(false)
const avatarCount = ref(0)
const avatarQuery = ref({ page: 1, size: 24 })
const avatarTotal = ref(0)
const avatarPending = ref([])
const avatarUploading = ref(false)

async function loadAvatars() {
  avatarLoading.value = true
  try {
    const res = await listAvatars({ page: avatarQuery.value.page, size: avatarQuery.value.size })
    avatars.value = res.items || []
    avatarTotal.value = res.total || 0
  } catch (e) {
    message.error(e.message || '加载头像库失败')
  } finally {
    avatarLoading.value = false
  }
}

async function loadAvatarCount() {
  try {
    avatarCount.value = (await countAvatars()) || 0
  } catch (e) {
    // 数量加载失败不阻断页面
  }
}

function pickAvatarFiles(file) {
  const isImage = ['image/jpeg', 'image/png'].includes(file.type)
  if (!isImage) {
    message.warning(`${file.name} 不是 JPG/PNG 图片，已跳过`)
    return false
  }
  if (file.size > 5 * 1024 * 1024) {
    message.warning(`${file.name} 超过 5MB，已跳过`)
    return false
  }
  avatarPending.value.push(file)
  return false
}

async function submitAvatarUpload() {
  if (avatarPending.value.length === 0) {
    return
  }
  avatarUploading.value = true
  try {
    const res = await uploadAvatars(avatarPending.value)
    message.success(`已上传 ${res.saved} 张头像`)
    avatarPending.value = []
    loadAvatars()
    loadAvatarCount()
  } catch (e) {
    message.error(e.message || '上传失败')
  } finally {
    avatarUploading.value = false
  }
}

async function removeAvatar(id) {
  try {
    await deleteAvatar(id)
    message.success('已删除')
    loadAvatars()
    loadAvatarCount()
  } catch (e) {
    message.error(e.message || '删除失败')
  }
}

onMounted(() => {
  loadBatches()
  loadStatsFilter()
})
onUnmounted(() => clearTimeout(pollTimer))
</script>

<style scoped>
.simulation-admin {
  background: #fff;
  padding: 24px;
  border-radius: 12px;
  min-height: calc(100vh - 112px);
}

.simulation-tabs :deep(.ant-tabs-content) {
  padding-top: 8px;
}

.filter-card {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #ffffff;
  border-radius: 8px;
  padding: 14px 16px;
  margin-bottom: 12px;
}

.filter-label {
  font-size: 14px;
  font-weight: 500;
  color: #262626;
}

.filter-hint {
  font-size: 12px;
  color: #8c8c8c;
}

.section-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
}

.failed-count {
  color: #ff4d4f;
  margin-left: 4px;
}

.prob-suffix {
  margin-left: 8px;
  color: #999;
}

.interval-sep {
  margin: 0 8px;
  color: #999;
}

.log-section {
  margin-top: 24px;
}

.cell-ellipsis {
  display: inline-block;
  max-width: 260px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: bottom;
}

.avatar-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 16px;
}

.avatar-cell {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}

.avatar-cell img {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
  display: block;
}

.avatar-cell-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 4px 0 8px;
}

.avatar-cell-time {
  font-size: 12px;
  color: #999;
}

.avatar-pagination {
  margin-top: 16px;
  text-align: right;
}
</style>
