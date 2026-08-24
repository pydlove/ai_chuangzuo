<template>
  <div class="account-query">
    <a-card :bordered="false">
      <div class="page-header">
        <h3 class="page-title">账户明细</h3>
        <p class="page-desc">查看用户账户综合信息</p>
      </div>

      <div class="toolbar">
        <a-input v-model:value="query.userId" placeholder="用户ID" style="width: 120px" />
        <a-input v-model:value="query.nickname" placeholder="昵称" style="width: 180px" />
        <a-input v-model:value="query.email" placeholder="邮箱" style="width: 200px" />
        <a-select v-model:value="query.userType" allow-clear placeholder="全部类型" style="width: 120px">
          <a-select-option :value="0">机器人</a-select-option>
          <a-select-option :value="1">真实用户</a-select-option>
        </a-select>
        <a-button type="primary" @click="fetchAccounts">查询</a-button>
        <a-radio-group :value="viewMode" option-type="button" @change="e => onViewModeChange(e.target.value)" style="margin-left: auto">
          <a-radio-button value="all">全部用户</a-radio-button>
          <a-radio-button value="heavyEarnings">深度用户（按收益）</a-radio-button>
          <a-radio-button value="heavyCoin">深度用户（按余额）</a-radio-button>
        </a-radio-group>
        <a-popover title="显示列" placement="bottomRight" trigger="click">
          <template #content>
            <div class="column-toggle">
              <a-checkbox v-model:checked="columnVisibility.userId">用户ID</a-checkbox>
              <a-checkbox v-model:checked="columnVisibility.nickname">昵称</a-checkbox>
              <a-checkbox v-model:checked="columnVisibility.email">邮箱</a-checkbox>
              <a-checkbox v-model:checked="columnVisibility.userType">类型</a-checkbox>
              <a-checkbox v-model:checked="columnVisibility.totalEarnings">累计收益</a-checkbox>
              <a-checkbox v-model:checked="columnVisibility.coinBalance">创作币余额</a-checkbox>
              <a-checkbox v-model:checked="columnVisibility.withdrawnAmount">已经提现金额</a-checkbox>
            </div>
          </template>
          <a-button>
            <template #icon><SettingOutlined /></template>
            列设置
          </a-button>
        </a-popover>
      </div>

      <a-table
        :columns="displayColumns"
        :data-source="accounts"
        :loading="loading"
        :pagination="false"
        row-key="userId"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'userType'">
            <a-tag :color="record.userType === 0 ? 'orange' : 'blue'">
              {{ record.userType === 0 ? '机器人' : '真实用户' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-button type="link" size="small" @click="openDetail(record.userId)">查看详情</a-button>
            <a-button v-if="record.userType === 0" type="link" size="small" @click="openAddCoin(record)">增加创作币</a-button>
          </template>
        </template>
      </a-table>

      <div class="pagination">
        <a-pagination
          :current="query.page"
          :page-size="query.size"
          :total="total"
          show-size-changer
          @change="handlePageChange"
        />
      </div>
    </a-card>

    <a-modal v-model:open="detailVisible" title="账户详情" :width="1280" :footer="null">
      <a-spin :spinning="detailLoading">
        <template v-if="detail">
          <div class="detail-header">
            <div class="detail-title">{{ detail.nickname || '-' }}</div>
            <div class="detail-meta">
              <span>邮箱：{{ detail.email || '-' }}</span>
              <span>注册时间：{{ formatTime(detail.registeredAt) }}</span>
            </div>
          </div>

          <div class="stat-grid">
            <div class="stat-card">
              <div class="stat-label">累计收益</div>
              <div class="stat-value">{{ detail.totalEarnings ?? 0 }}</div>
            </div>
            <div class="stat-card">
              <div class="stat-label">已结算</div>
              <div class="stat-value">{{ detail.settledEarnings ?? 0 }}</div>
            </div>
            <div class="stat-card">
              <div class="stat-label">未结算</div>
              <div class="stat-value">{{ detail.unsettledEarnings ?? 0 }}</div>
            </div>
            <div class="stat-card">
              <div class="stat-label">创作币余额</div>
              <div class="stat-value">{{ detail.coinBalance ?? 0 }}</div>
            </div>
            <div class="stat-card">
              <div class="stat-label">创作币累计收入</div>
              <div class="stat-value">{{ detail.totalCoinIncome ?? 0 }}</div>
            </div>
            <div class="stat-card">
              <div class="stat-label">创作币累计支出</div>
              <div class="stat-value">{{ detail.totalCoinExpense ?? 0 }}</div>
            </div>
            <div class="stat-card">
              <div class="stat-label">奖励次数</div>
              <div class="stat-value">{{ detail.rewardCount ?? 0 }}</div>
            </div>
          </div>

          <a-tabs v-model:activeKey="detailActiveKey" class="detail-tabs">
            <a-tab-pane key="coin" tab="创作币明细">
              <div class="sub-toolbar">
                <a-select v-model:value="coinQuery.direction" style="width: 120px" placeholder="方向" allow-clear>
                  <a-select-option :value="1">收入</a-select-option>
                  <a-select-option :value="2">支出</a-select-option>
                </a-select>
                <a-date-picker v-model:value="coinQuery.startTime" show-time format="YYYY-MM-DD HH:mm:ss" placeholder="开始时间" />
                <a-date-picker v-model:value="coinQuery.endTime" show-time format="YYYY-MM-DD HH:mm:ss" placeholder="结束时间" />
                <a-button type="primary" @click="searchCoinRecords">查询</a-button>
                <a-button @click="resetCoinQuery">重置</a-button>
              </div>
              <a-table
                :columns="coinColumns"
                :data-source="coinRecords"
                :loading="coinLoading"
                :pagination="{ current: coinQuery.page, pageSize: coinQuery.size, total: coinTotal, showSizeChanger: true }"
                row-key="id"
                size="small"
                @change="handleCoinPageChange"
              >
                <template #bodyCell="{ column, record }">
                  <span v-if="column.key === 'direction'">
                    <a-tag :color="record.direction === 1 ? 'green' : 'red'">
                      {{ record.direction === 1 ? '收入' : '支出' }}
                    </a-tag>
                  </span>
                  <span v-else-if="column.key === 'bizType'">{{ formatBizType(record.bizType) }}</span>
                  <span v-else-if="column.key === 'bizTime'">{{ formatTime(record.bizTime) }}</span>
                  <span v-else>{{ record[column.dataIndex || column.key] }}</span>
                </template>
              </a-table>
            </a-tab-pane>

            <a-tab-pane key="earnings" tab="收益明细">
              <div class="sub-toolbar">
                <a-input v-model:value="earningsQuery.type" style="width: 160px" placeholder="类型" />
                <a-date-picker v-model:value="earningsQuery.startTime" show-time format="YYYY-MM-DD HH:mm:ss" placeholder="开始时间" />
                <a-date-picker v-model:value="earningsQuery.endTime" show-time format="YYYY-MM-DD HH:mm:ss" placeholder="结束时间" />
                <a-button type="primary" @click="searchEarningsRecords">查询</a-button>
                <a-button @click="resetEarningsQuery">重置</a-button>
              </div>
              <a-table
                :columns="earningsColumns"
                :data-source="earningsRecords"
                :loading="earningsLoading"
                :pagination="{ current: earningsQuery.page, pageSize: earningsQuery.size, total: earningsTotal, showSizeChanger: true }"
                row-key="id"
                size="small"
                @change="handleEarningsPageChange"
              >
                <template #bodyCell="{ column, record }">
                  <span v-if="column.key === 'createdAt'">{{ formatTime(record.createdAt) }}</span>
                  <span v-else>{{ record[column.dataIndex || column.key] }}</span>
                </template>
              </a-table>
            </a-tab-pane>

            <a-tab-pane key="reward" tab="奖励明细">
              <div class="sub-toolbar">
                <a-select v-model:value="rewardQuery.leaderboardType" style="width: 140px" placeholder="榜单类型" allow-clear>
                  <a-select-option :value="1">创作币榜</a-select-option>
                  <a-select-option :value="2">自媒体收入榜</a-select-option>
                </a-select>
                <a-date-picker v-model:value="rewardQuery.startTime" show-time format="YYYY-MM-DD HH:mm:ss" placeholder="开始时间" />
                <a-date-picker v-model:value="rewardQuery.endTime" show-time format="YYYY-MM-DD HH:mm:ss" placeholder="结束时间" />
                <a-button type="primary" @click="searchRewardRecords">查询</a-button>
                <a-button @click="resetRewardQuery">重置</a-button>
              </div>
              <a-table
                :columns="rewardColumns"
                :data-source="rewardRecords"
                :loading="rewardLoading"
                :pagination="{ current: rewardQuery.page, pageSize: rewardQuery.size, total: rewardTotal, showSizeChanger: true }"
                row-key="id"
                size="small"
                @change="handleRewardPageChange"
              >
                <template #bodyCell="{ column, record }">
                  <span v-if="column.key === 'leaderboardType'">
                    {{ record.leaderboardType === 1 ? '创作币榜' : '自媒体收入榜' }}
                  </span>
                  <span v-else-if="column.key === 'grantedAt'">{{ formatTime(record.grantedAt) }}</span>
                  <span v-else>{{ record[column.dataIndex || column.key] }}</span>
                </template>
              </a-table>
            </a-tab-pane>
          </a-tabs>
        </template>
      </a-spin>
    </a-modal>

    <a-modal
      v-model:open="addCoinVisible"
      title="增加创作币"
      ok-text="确认增加"
      cancel-text="取消"
      :confirm-loading="addCoinLoading"
      @ok="submitAddCoin"
      @cancel="closeAddCoin"
    >
      <p v-if="addCoinTarget">
        用户：<strong>{{ addCoinTarget.nickname || addCoinTarget.email || addCoinTarget.userId }}</strong>
        <a-tag color="orange" style="margin-left: 8px">机器人</a-tag>
      </p>
      <a-form ref="addCoinFormRef" :model="addCoinForm" :rules="addCoinRules" layout="vertical">
        <a-form-item label="增加数量" name="amount">
          <a-input-number
            v-model:value="addCoinForm.amount"
            :min="0.01"
            :precision="2"
            style="width: 100%"
            placeholder="请输入要增加的创作币数量"
          />
        </a-form-item>
        <a-form-item label="备注" name="remark">
          <a-textarea v-model:value="addCoinForm.remark" :rows="3" placeholder="可选，记录调整原因" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import dayjs from 'dayjs'
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { SettingOutlined } from '@ant-design/icons-vue'
import { useAccountQuery } from '@/composables/useAccountQuery.js'
import { addCoinToUser } from '@/api/earnings.js'

const {
  accounts, total, loading, query,
  detail, detailVisible, detailLoading, detailActiveKey,
  coinRecords, coinTotal, coinLoading, coinQuery,
  earningsRecords, earningsTotal, earningsLoading, earningsQuery,
  rewardRecords, rewardTotal, rewardLoading, rewardQuery,
  viewMode, fetchAccounts, openDetail, onViewModeChange,
  handlePageChange, handleCoinPageChange, handleEarningsPageChange, handleRewardPageChange,
  searchCoinRecords, resetCoinQuery,
  searchEarningsRecords, resetEarningsQuery,
  searchRewardRecords, resetRewardQuery
} = useAccountQuery()

const baseColumns = [
  { title: '用户ID', dataIndex: 'userId', key: 'userId' },
  { title: '昵称', dataIndex: 'nickname', key: 'nickname' },
  { title: '邮箱', dataIndex: 'email', key: 'email' },
  { title: '类型', dataIndex: 'userType', key: 'userType' },
  { title: '累计收益', dataIndex: 'totalEarnings', key: 'totalEarnings' },
  { title: '创作币余额', dataIndex: 'coinBalance', key: 'coinBalance' },
  { title: '已经提现金额', dataIndex: 'withdrawnAmount', key: 'withdrawnAmount' },
  { title: '操作', key: 'actions' }
]

const columnVisibility = reactive({
  userId: true,
  nickname: true,
  email: true,
  userType: true,
  totalEarnings: true,
  coinBalance: true,
  withdrawnAmount: true
})

const displayColumns = computed(() =>
  baseColumns.filter((col) => col.key === 'actions' || columnVisibility[col.key] !== false)
)

const addCoinVisible = ref(false)
const addCoinLoading = ref(false)
const addCoinTarget = ref(null)
const addCoinFormRef = ref()
const addCoinForm = reactive({
  amount: null,
  remark: ''
})

const addCoinRules = {
  amount: [
    { required: true, message: '请输入创作币数量', trigger: 'blur' },
    { type: 'number', min: 0.01, message: '数量必须大于 0', trigger: 'blur', transform: (v) => Number(v) }
  ]
}

const openAddCoin = (record) => {
  addCoinTarget.value = record
  addCoinForm.amount = null
  addCoinForm.remark = ''
  addCoinVisible.value = true
}

const closeAddCoin = () => {
  addCoinVisible.value = false
  addCoinTarget.value = null
  addCoinFormRef.value?.resetFields()
}

const submitAddCoin = () => {
  addCoinFormRef.value?.validate().then(async () => {
    if (!addCoinTarget.value) return
    addCoinLoading.value = true
    try {
      await addCoinToUser(addCoinTarget.value.userId, {
        amount: addCoinForm.amount,
        remark: addCoinForm.remark?.trim() || undefined
      })
      message.success('创作币已增加')
      closeAddCoin()
      fetchAccounts()
    } catch (error) {
      message.error(error.message || '增加创作币失败')
    } finally {
      addCoinLoading.value = false
    }
  })
}

onMounted(fetchAccounts)

const BIZ_TYPE_LABEL = {
  leaderboard_reward: '榜单奖励',
  admin_adjust: '管理员调整',
  invite_reward: '邀请奖励',
  lottery_coin: '抽奖获得',
  skill_market_usage: '提示词使用收益',
  withdraw: '提现扣减',
  withdraw_refund: '提现退款',
  commission_reward: '约稿采纳奖励',
  invite_register_reward: '邀请注册奖励',
  invite_ladder_reward: '邀请阶梯奖励',
  subscribe_coin_discount: '订阅抵扣'
}

function formatBizType(type) {
  return BIZ_TYPE_LABEL[type] || type || '-'
}

function formatTime(t) {
  if (!t) return '-'
  return dayjs(t).format('YYYY-MM-DD HH:mm')
}
</script>

<style scoped>
.page-header { margin-bottom: 16px; }
.page-title { font-size: 18px; font-weight: 600; margin: 0 0 4px; }
.page-desc { color: #8c8c8c; margin: 0; }
.toolbar { display: flex; gap: 8px; margin-bottom: 16px; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }

.column-toggle {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 120px;
}

.detail-header { margin-bottom: 16px; }
.detail-title { font-size: 18px; font-weight: 600; margin-bottom: 4px; }
.detail-meta { color: #8c8c8c; font-size: 13px; }
.detail-meta span { margin-right: 16px; }

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}
.stat-card {
  background: #fff0f6;
  border-radius: 8px;
  padding: 12px;
  text-align: center;
}
.stat-label { color: #8c8c8c; font-size: 13px; margin-bottom: 4px; }
.stat-value { font-size: 18px; font-weight: 600; color: #262626; }

.detail-tabs :deep(.ant-tabs-content) {
  padding-top: 8px;
}
.sub-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}
</style>
