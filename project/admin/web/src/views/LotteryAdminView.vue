<template>
  <div class="lottery-admin">
    <a-page-header title="抽奖活动运营" sub-title="配置活动轮次、奖项、查看记录与展示墙" />

    <a-tabs v-model:activeKey="activeKey" class="lottery-tabs">
      <!-- 活动配置 -->
      <a-tab-pane key="campaigns" tab="活动配置">
        <a-button type="primary" class="add-btn" @click="openCampaignModal()">新建活动</a-button>
        <a-table :columns="campaignColumns" :data-source="campaigns" :loading="loading" :pagination="pagination"
                 row-key="id" @change="handleCampaignTableChange">
          <template #bodyCell="{ column, record }">
            <span v-if="column.key === 'status'">
              <a-tag :color="statusColor(record.status)">{{ statusText(record.status) }}</a-tag>
            </span>
            <span v-else-if="column.key === 'action'">
              <a-space>
                <a-button v-if="record.status === 0 || record.status === 1" size="small" type="link" @click="openCampaignModal(record)">编辑</a-button>
                <a-button v-if="(record.status === 0 || record.status === 3) && !hasOpenCampaign" size="small" type="link" @click="openCampaign(record.id)">开启</a-button>
                <a-tooltip v-else-if="record.status === 0 || record.status === 3" title="已有其他活动开启，不能同时开启多个活动">
                  <a-button size="small" type="link" disabled>开启</a-button>
                </a-tooltip>
                <a-button v-if="record.status === 1" size="small" type="link" @click="closeCampaign(record.id)">关闭</a-button>
                <a-button size="small" type="link" @click="openCloneModal(record)">复制</a-button>
                <a-popconfirm title="确认删除？" @confirm="removeCampaign(record.id)">
                  <a-button size="small" type="link" danger>删除</a-button>
                </a-popconfirm>
              </a-space>
            </span>
            <span v-else>{{ record[column.key] }}</span>
          </template>
        </a-table>
      </a-tab-pane>

      <!-- 奖项设置 -->
      <a-tab-pane key="tiers" tab="奖项设置" :disabled="!selectedCampaign">
        <a-alert v-if="!selectedCampaign" message="请先选择一个进行中的活动" type="info" />
        <div v-else>
          <div class="section-bar">
            <span class="section-title">{{ selectedCampaign.name }} - 奖项配置</span>
            <a-space>
              <a-tag :color="tierTotalProbability > 1 ? 'red' : 'blue'">累计概率 {{ (tierTotalProbability * 100).toFixed(2) }}%</a-tag>
              <a-button type="primary" @click="openTierModal()">新增奖项</a-button>
            </a-space>
          </div>
          <a-table :columns="tierColumns" :data-source="tiers" :loading="tierLoading" row-key="id"
                   :pagination="false">
            <template #bodyCell="{ column, record }">
              <span v-if="column.key === 'prizeLevel'">{{ prizeLevelText(record.prizeLevel) }}</span>
              <span v-else-if="column.key === 'rewardType'">
                <a-tag>{{ rewardTypeText(record.rewardType) }}</a-tag>
              </span>
              <span v-else-if="column.key === 'probability'">{{ (record.probability * 100).toFixed(2) }}%</span>
              <span v-else-if="column.key === 'remaining'">{{ record.remainingWinCount ?? '-' }}/{{ record.maxWinCount ?? '-' }}</span>
              <span v-else-if="column.key === 'displayRemaining'">{{ record.displayRemaining === 1 ? (record.displayRemainingCount != null ? record.displayRemainingCount : '真实剩余') : '不显示' }}</span>
              <span v-else-if="column.key === 'action'">
                <a-space>
                  <a-button size="small" type="link" @click="openTierModal(record)">编辑</a-button>
                  <a-popconfirm title="确认删除？" @confirm="removeTier(record.id)">
                    <a-button size="small" type="link" danger>删除</a-button>
                  </a-popconfirm>
                </a-space>
              </span>
              <span v-else>{{ record[column.key] }}</span>
            </template>
          </a-table>
        </div>
      </a-tab-pane>

      <!-- 兑换记录 -->
      <a-tab-pane key="codes" tab="兑换记录">
        <a-form layout="inline" class="record-query-form">
          <a-form-item label="活动">
            <a-select v-model:value="codeQuery.campaignId" style="width: 160px" placeholder="选择活动" allow-clear @change="onCodeCampaignChange">
              <a-select-option v-for="c in campaigns" :key="c.id" :value="c.id">{{ c.name }}</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="奖项">
            <a-select v-model:value="codeQuery.tierId" style="width: 140px" placeholder="选择奖项" allow-clear>
              <a-select-option v-for="t in codeTierOptions" :key="t.id" :value="t.id">{{ t.tierName }}</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="状态">
            <a-select v-model:value="codeQuery.status" style="width: 120px" placeholder="状态" allow-clear>
              <a-select-option value="unused">未使用</a-select-option>
              <a-select-option value="used">已使用</a-select-option>
              <a-select-option value="expired">已过期</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="用户">
            <a-input v-model:value="codeQuery.userKeyword" style="width: 160px" placeholder="邮箱/昵称" />
          </a-form-item>
          <a-form-item label="开始">
            <a-date-picker v-model:value="codeQuery.startTime" show-time format="YYYY-MM-DD HH:mm:ss" placeholder="开始时间" />
          </a-form-item>
          <a-form-item label="结束">
            <a-date-picker v-model:value="codeQuery.endTime" show-time format="YYYY-MM-DD HH:mm:ss" placeholder="结束时间" />
          </a-form-item>
          <a-form-item>
            <a-space>
              <a-button type="primary" @click="searchCodes">搜索</a-button>
              <a-button @click="resetCodeQuery">重置</a-button>
            </a-space>
          </a-form-item>
        </a-form>
        <a-table :columns="codeColumns" :data-source="codes" :loading="codeLoading" :pagination="codePagination"
                 row-key="id" @change="handleCodeTableChange">
          <template #bodyCell="{ column, record }">
            <span v-if="column.key === 'status'">
              <a-tag :color="record.status === 'used' ? 'default' : record.status === 'expired' ? 'red' : 'green'">{{ statusTextCode(record.status) }}</a-tag>
            </span>
            <span v-else-if="column.key === 'createdAt'">{{ formatTime(record.createdAt) }}</span>
            <span v-else-if="column.key === 'usedAt'">{{ formatTime(record.usedAt) }}</span>
            <span v-else>{{ record[column.key] }}</span>
          </template>
        </a-table>
      </a-tab-pane>

      <!-- 抽奖记录 -->
      <a-tab-pane key="records" tab="抽奖记录">
        <a-form layout="inline" class="record-query-form">
          <a-form-item label="类型">
            <a-select v-model:value="recordQuery.drawType" style="width: 100px" placeholder="类型" allow-clear>
              <a-select-option value="free">免费</a-select-option>
              <a-select-option value="invite">邀请</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="邮箱">
            <a-input v-model:value="recordQuery.email" style="width: 160px" placeholder="邮箱" />
          </a-form-item>
          <a-form-item label="昵称">
            <a-input v-model:value="recordQuery.nickname" style="width: 160px" placeholder="昵称" />
          </a-form-item>
          <a-form-item label="开始">
            <a-date-picker v-model:value="recordQuery.startTime" show-time format="YYYY-MM-DD HH:mm:ss" placeholder="开始时间" />
          </a-form-item>
          <a-form-item label="结束">
            <a-date-picker v-model:value="recordQuery.endTime" show-time format="YYYY-MM-DD HH:mm:ss" placeholder="结束时间" />
          </a-form-item>
          <a-form-item>
            <a-space>
              <a-button type="primary" @click="searchRecords">搜索</a-button>
              <a-button @click="resetRecordQuery">重置</a-button>
            </a-space>
          </a-form-item>
        </a-form>
        <a-table :columns="recordColumns" :data-source="drawRecords" :loading="recordLoading" :pagination="recordPagination"
                 row-key="id" @change="handleRecordTableChange">
          <template #bodyCell="{ column, record }">
            <span v-if="column.key === 'drawType'">
              <a-tag>{{ record.drawType === 'invite' ? '邀请' : '免费' }}</a-tag>
            </span>
            <span v-else-if="column.key === 'createdAt'">{{ formatTime(record.createdAt) }}</span>
            <span v-else-if="column.key === 'action'">
              <a-button size="small" type="link" @click="openResetChanceModal(record.campaignId, record.userId)">重置次数</a-button>
            </span>
            <span v-else>{{ record[column.key] }}</span>
          </template>
        </a-table>
      </a-tab-pane>

      <!-- 展示墙 -->
      <a-tab-pane key="winners" tab="展示墙">
        <a-button type="primary" class="add-btn" @click="openWinnerModal()">新增机器人记录</a-button>
        <a-table :columns="winnerColumns" :data-source="winners" :loading="winnerLoading" row-key="id" :pagination="false">
          <template #bodyCell="{ column, record }">
            <span v-if="column.key === 'isReal'">{{ record.isReal ? '真实' : '机器人' }}</span>
            <span v-else-if="column.key === 'status'">
              <a-tag :color="record.status === 1 ? 'green' : 'default'">{{ record.status === 1 ? '展示' : '隐藏' }}</a-tag>
            </span>
            <span v-else-if="column.key === 'winTime'">{{ formatTime(record.winTime) }}</span>
            <span v-else-if="column.key === 'action'">
              <a-space>
                <a-button size="small" type="link" @click="openWinnerModal(record)">编辑</a-button>
                <a-button size="small" type="link" @click="toggleWinner(record.id, record.status === 1 ? 0 : 1)">{{ record.status === 1 ? '隐藏' : '展示' }}</a-button>
                <a-popconfirm title="确认删除？" @confirm="removeWinner(record.id)">
                  <a-button size="small" type="link" danger>删除</a-button>
                </a-popconfirm>
              </a-space>
            </span>
            <span v-else>{{ record[column.key] }}</span>
          </template>
        </a-table>
      </a-tab-pane>
    </a-tabs>

    <!-- 活动弹窗 -->
    <a-modal v-model:open="campaignModalVisible" title="活动配置" width="1280px" :confirm-loading="saving" @ok="saveCampaignForm"
               @cancel="campaignModalVisible = false">
      <a-form :model="campaignForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="活动名称" required>
          <a-input v-model:value="campaignForm.name" />
        </a-form-item>
        <a-form-item label="活动描述">
          <a-textarea v-model:value="campaignForm.description" :rows="2" />
        </a-form-item>
        <a-form-item label="活动规则">
          <md-editor v-model="campaignForm.rules" @onChange="v => campaignForm.rules = v" placeholder="可填写奖项说明、参与方式等，支持 Markdown" />
        </a-form-item>
        <a-form-item label="开始时间" required>
          <a-date-picker v-model:value="campaignForm.startTime" show-time format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </a-form-item>
        <a-form-item label="结束时间" required>
          <a-date-picker v-model:value="campaignForm.endTime" show-time format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </a-form-item>
        <a-form-item label="免费次数" required>
          <a-input-number v-model:value="campaignForm.freeDrawsPerUser" :min="1" style="width: 100%" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 复制活动弹窗 -->
    <a-modal v-model:open="cloneModalVisible" title="复制活动" :confirm-loading="cloneSaving" @ok="saveClone"
             @cancel="cloneModalVisible = false">
      <a-form :model="cloneForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="新活动名称" required>
          <a-input v-model:value="cloneForm.name" :max-length="100" placeholder="请输入新活动名称" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 奖项弹窗 -->
    <a-modal v-model:open="tierModalVisible" title="奖项配置" :confirm-loading="tierSaving" @ok="saveTierForm"
               @cancel="tierModalVisible = false">
      <a-form :model="tierForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item v-show="false" label="奖项标识" required>
          <a-input v-model:value="tierForm.tierKey" />
        </a-form-item>
        <a-form-item label="奖项等级" required>
          <a-select v-model:value="tierForm.prizeLevel" style="width: 100%" placeholder="请选择奖项等级">
            <a-select-option :value="1">特等奖</a-select-option>
            <a-select-option :value="2">一等奖</a-select-option>
            <a-select-option :value="3">二等奖</a-select-option>
            <a-select-option :value="4">三等奖</a-select-option>
            <a-select-option :value="5">四等奖</a-select-option>
            <a-select-option :value="6">五等奖</a-select-option>
            <a-select-option :value="7">六等奖</a-select-option>
            <a-select-option :value="8">七等奖</a-select-option>
            <a-select-option :value="9">八等奖</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="奖项名称" required>
          <a-input v-model:value="tierForm.tierName" placeholder="如：100 创作币、7 天会员、9 折券" />
        </a-form-item>
        <a-form-item label="概率(%)" required :extra="`填写 0~100 之间的数字，如 5 表示 5%；所有奖项概率之和不能超过 100%（当前已用 ${(tierProbabilityInfo.used * 100).toFixed(2)}%，保存后预计 ${(tierProbabilityInfo.projected * 100).toFixed(2)}%）`">
          <a-input-number v-model:value="tierForm.probability" :min="0" :max="1" :step="0.0001" style="width: 100%" placeholder="5"
                          :formatter="value => `${(Number(value || 0) * 100).toFixed(2)}%`"
                          :parser="value => { const n = parseFloat(String(value).replace(/[^0-9.]/g, '')); return isNaN(n) ? 0 : n / 100 }" />
        </a-form-item>
        <a-form-item label="上限" extra="该奖项最多可被抽中几次，留空表示不限量">
          <a-input-number v-model:value="tierForm.maxWinCount" :min="1" style="width: 100%" placeholder="100" />
        </a-form-item>
        <a-form-item label="前端显示剩余" extra="开启后用户端奖品卡片会展示剩余数量">
          <a-switch v-model:checked="tierForm.displayRemaining" :checkedValue="1" :unCheckedValue="0" />
        </a-form-item>
        <a-form-item v-if="tierForm.displayRemaining === 1" label="显示剩余数量" extra="留空则展示真实剩余数量">
          <a-input-number v-model:value="tierForm.displayRemainingCount" :min="0" style="width: 100%" placeholder="不填则使用真实剩余" />
        </a-form-item>
        <a-form-item label="奖励类型" required>
          <a-select v-model:value="tierForm.rewardType" @change="onRewardTypeChange">
            <a-select-option value="coin">创作币</a-select-option>
            <a-select-option value="membership">会员</a-select-option>
            <a-select-option value="coupon">折扣券</a-select-option>
            <a-select-option value="none">谢谢回顾</a-select-option>
          </a-select>
        </a-form-item>

        <!-- 创作币奖励参数 -->
        <template v-if="tierForm.rewardType === 'coin'">
          <a-form-item label="创作币数量" required>
            <a-input-number v-model:value="rewardParams.coin.amount" :min="1" style="width: 100%" placeholder="100" />
          </a-form-item>
        </template>

        <!-- 会员奖励参数 -->
        <template v-if="tierForm.rewardType === 'membership'">
          <a-form-item label="会员类型" required>
            <a-input v-model:value="rewardParams.membership.plan_key" placeholder="如：pro" />
          </a-form-item>
          <a-form-item label="时长方式" required>
            <a-radio-group v-model:value="rewardParams.membership.durationType">
              <a-radio value="days">按天</a-radio>
              <a-radio value="cycle">按周期</a-radio>
            </a-radio-group>
          </a-form-item>
          <a-form-item v-if="rewardParams.membership.durationType === 'days'" label="天数" required>
            <a-input-number v-model:value="rewardParams.membership.days" :min="1" style="width: 100%" placeholder="30" />
          </a-form-item>
          <a-form-item v-if="rewardParams.membership.durationType === 'cycle'" label="周期" required>
            <a-select v-model:value="rewardParams.membership.cycle" style="width: 100%">
              <a-select-option value="month">月（30 天）</a-select-option>
              <a-select-option value="quarter">季（90 天）</a-select-option>
              <a-select-option value="year">年（365 天）</a-select-option>
            </a-select>
          </a-form-item>
        </template>

        <!-- 折扣券奖励参数 -->
        <template v-if="tierForm.rewardType === 'coupon'">
          <a-form-item label="券类型" required>
            <a-radio-group v-model:value="rewardParams.coupon.coupon_type">
              <a-radio value="percent">百分比折扣</a-radio>
              <a-radio value="fixed">固定金额抵扣</a-radio>
            </a-radio-group>
          </a-form-item>
          <a-form-item label="折扣值" required :extra="rewardParams.coupon.coupon_type === 'percent' ? '如 0.8 表示 8 折' : '如 10 表示抵扣 10 元'">
            <a-input v-model:value="rewardParams.coupon.discount_value" placeholder="0.8 或 10" />
          </a-form-item>
          <a-form-item label="适用周期" extra="留空或 all 表示全部周期">
            <a-input v-model:value="rewardParams.coupon.applicable_cycle" placeholder="all" />
          </a-form-item>
          <a-form-item label="适用套餐" extra="留空或 all 表示全部套餐">
            <a-input v-model:value="rewardParams.coupon.applicable_plan" placeholder="all" />
          </a-form-item>
        </template>

        <!-- 谢谢回顾 -->
        <template v-if="tierForm.rewardType === 'none'">
          <a-form-item label="奖励参数" required>
            <a-input value="{}" disabled />
          </a-form-item>
        </template>

        <a-form-item label="兑换码前缀" extra="生成兑换码时的前缀，如 GIFT">
          <a-input v-model:value="tierForm.codePrefix" placeholder="GIFT" />
        </a-form-item>
        <a-form-item label="兑换码长度" extra="兑换码总长度（含前缀），留空则由系统默认">
          <a-input-number v-model:value="tierForm.codeLength" :min="1" style="width: 100%" placeholder="12" />
        </a-form-item>
        <a-form-item label="有效期(天)" required extra="兑换码自生成起多少天内有效">
          <a-input-number v-model:value="tierForm.codeValidityDays" :min="1" style="width: 100%" placeholder="30" />
        </a-form-item>
        <a-form-item label="排序" extra="数字越小越靠前，默认 0">
          <a-input-number v-model:value="tierForm.sortOrder" style="width: 100%" placeholder="0" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 展示墙弹窗 -->
    <a-modal v-model:open="winnerModalVisible" title="展示墙记录" :confirm-loading="winnerSaving" @ok="saveWinnerForm"
               @cancel="winnerModalVisible = false">
      <a-form :model="winnerForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="活动" required>
          <a-select v-model:value="winnerForm.campaignId" placeholder="选择活动" @change="onWinnerCampaignChange">
            <a-select-option v-for="c in campaigns" :key="c.id" :value="c.id">{{ c.name }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="奖项">
          <a-select v-model:value="winnerForm.tierId" placeholder="选择奖项" allow-clear @change="onWinnerTierChange">
            <a-select-option v-for="t in winnerTierOptions" :key="t.id" :value="t.id">{{ t.tierName }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="奖品名">
          <a-input v-model:value="winnerForm.prizeName" disabled placeholder="选择奖项后自动填充" />
        </a-form-item>
        <a-form-item label="用户">
          <a-select v-model:value="winnerForm.userId" placeholder="搜索选择用户" show-search allow-clear
                    :filter-option="false" :options="userOptions" @search="fetchUserOptions" @change="onWinnerUserChange" />
        </a-form-item>
        <a-form-item label="昵称" required>
          <a-input v-model:value="winnerForm.nickname" />
        </a-form-item>
        <a-form-item label="中奖时间" required>
          <a-date-picker v-model:value="winnerForm.winTime" show-time format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </a-form-item>
        <a-form-item label="排序">
          <a-input-number v-model:value="winnerForm.sortOrder" style="width: 100%" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 重置抽奖次数弹窗 -->
    <a-modal v-model:open="resetChanceModalVisible" title="重置抽奖次数" :confirm-loading="resetChanceSaving" @ok="saveResetChance"
               @cancel="resetChanceModalVisible = false">
      <a-form :model="resetChanceForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="活动" required>
          <a-select v-if="!resetChanceForm.preSet" v-model:value="resetChanceForm.campaignId" placeholder="选择活动">
            <a-select-option v-for="c in campaigns" :key="c.id" :value="c.id">{{ c.name }}</a-select-option>
          </a-select>
          <a-input v-else :value="campaignName(resetChanceForm.campaignId)" disabled />
        </a-form-item>
        <a-form-item label="用户ID" required>
          <a-input-number v-if="!resetChanceForm.preSet" v-model:value="resetChanceForm.userId" style="width: 100%" />
          <a-input v-else :value="resetChanceForm.userId" disabled />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { MdEditor } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import {
  listCampaigns, saveCampaign, openCampaign as apiOpenCampaign, closeCampaign as apiCloseCampaign, deleteCampaign, cloneCampaign as apiCloneCampaign,
  listTiers, saveTier, deleteTier,
  listRedemptionCodes, listDrawRecords, resetDrawChance, listDisplayWinners, saveDisplayWinner, toggleDisplayWinner, deleteDisplayWinner
} from '@/api/lottery'
import { listUserOptions } from '@/api/userOptions'

const activeKey = ref('campaigns')
const campaigns = ref([])
const loading = ref(false)
const pagination = ref({ current: 1, pageSize: 20, total: 0 })
const selectedCampaign = ref(null)
const hasOpenCampaign = computed(() => campaigns.value.some(c => c.status === 1))

const campaignColumns = [
  { title: 'ID', dataIndex: 'id', key: 'id' },
  { title: '名称', dataIndex: 'name', key: 'name' },
  { title: '状态', key: 'status' },
  { title: '免费次数', dataIndex: 'freeDrawsPerUser', key: 'freeDrawsPerUser' },
  { title: '开始时间', dataIndex: 'startTime', key: 'startTime' },
  { title: '结束时间', dataIndex: 'endTime', key: 'endTime' },
  { title: '操作', key: 'action' }
]

const campaignModalVisible = ref(false)
const saving = ref(false)
const campaignForm = ref({ name: '', description: '', rules: '', startTime: null, endTime: null, freeDrawsPerUser: 1 })

const cloneModalVisible = ref(false)
const cloneSaving = ref(false)
const cloneSource = ref(null)
const cloneForm = ref({ name: '' })

const tiers = ref([])
const tierLoading = ref(false)
const tierColumns = [
  { title: 'ID', dataIndex: 'id', key: 'id' },
  { title: '标识', dataIndex: 'tierKey', key: 'tierKey' },
  { title: '名称', dataIndex: 'tierName', key: 'tierName' },
  { title: '等级', key: 'prizeLevel' },
  { title: '概率(%)', key: 'probability' },
  { title: '剩余/上限', key: 'remaining' },
  { title: '前端显示剩余', key: 'displayRemaining' },
  { title: '奖励类型', key: 'rewardType' },
  { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder' },
  { title: '操作', key: 'action' }
]

const tierModalVisible = ref(false)
const tierSaving = ref(false)
const tierForm = ref({ tierKey: '', tierName: '', prizeLevel: null, probability: 0, maxWinCount: null, displayRemaining: 0, displayRemainingCount: null, rewardType: 'coin', rewardValueJson: '{}', codePrefix: '', codeLength: null, codeValidityDays: 30, sortOrder: 0 })

const tierProbabilityInfo = computed(() => {
  const currentProb = Number(tierForm.value.probability) || 0
  const editingId = tierForm.value.id
  const oldProb = editingId ? Number(tiers.value.find(t => t.id === editingId)?.probability) || 0 : 0
  const used = tiers.value.reduce((sum, t) => sum + (Number(t.probability) || 0), 0)
  const projected = used - oldProb + currentProb
  return { used, projected }
})

const tierTotalProbability = computed(() => {
  return tiers.value.reduce((sum, t) => sum + (Number(t.probability) || 0), 0)
})

const rewardParams = ref({
  coin: { amount: 100 },
  membership: { plan_key: 'pro', durationType: 'days', days: 30, cycle: 'month' },
  coupon: { coupon_type: 'percent', discount_value: '0.8', applicable_cycle: 'all', applicable_plan: 'all' }
})

const codes = ref([])
const codeLoading = ref(false)
const codePagination = ref({ current: 1, pageSize: 20, total: 0 })
const codeColumns = [
  { title: '兑换码', dataIndex: 'code', key: 'code' },
  { title: '活动', dataIndex: 'campaignName', key: 'campaignName' },
  { title: '奖项', dataIndex: 'tierName', key: 'tierName' },
  { title: '奖励内容', dataIndex: 'rewardContent', key: 'rewardContent' },
  { title: '用户', dataIndex: 'userDisplay', key: 'userDisplay' },
  { title: '状态', key: 'status' },
  { title: '创建时间', key: 'createdAt' },
  { title: '兑换时间', key: 'usedAt' }
]

const codeQuery = ref({
  campaignId: null,
  tierId: null,
  status: null,
  userKeyword: '',
  startTime: null,
  endTime: null
})
const codeTierOptions = ref([])

const drawRecords = ref([])
const recordLoading = ref(false)
const recordPagination = ref({ current: 1, pageSize: 20, total: 0 })
const recordQuery = ref({
  drawType: null,
  email: '',
  nickname: '',
  startTime: null,
  endTime: null
})

const resetChanceModalVisible = ref(false)
const resetChanceSaving = ref(false)
const resetChanceForm = ref({ campaignId: null, userId: null })

const recordColumns = [
  { title: '业务号', dataIndex: 'bizNo', key: 'bizNo' },
  { title: '活动', dataIndex: 'campaignName', key: 'campaignName' },
  { title: '昵称', dataIndex: 'nickname', key: 'nickname' },
  { title: '邮箱', dataIndex: 'email', key: 'email' },
  { title: '奖项', dataIndex: 'tierName', key: 'tierName' },
  { title: '类型', key: 'drawType' },
  { title: '时间', key: 'createdAt' },
  { title: '操作', key: 'action' }
]

const winners = ref([])
const winnerLoading = ref(false)
const winnerColumns = [
  { title: 'ID', dataIndex: 'id', key: 'id' },
  { title: '活动', dataIndex: 'campaignName', key: 'campaignName' },
  { title: '奖品名', dataIndex: 'prizeName', key: 'prizeName' },
  { title: '昵称', dataIndex: 'nickname', key: 'nickname' },
  { title: '类型', key: 'isReal' },
  { title: '状态', key: 'status' },
  { title: '中奖时间', key: 'winTime' },
  { title: '操作', key: 'action' }
]

const winnerModalVisible = ref(false)
const winnerSaving = ref(false)
const winnerForm = ref({ campaignId: null, tierId: null, userId: null, prizeName: '', nickname: '', winTime: null, sortOrder: 0 })
const winnerTierOptions = ref([])
const userOptions = ref([])

onMounted(() => {
  loadCampaigns()
})

watch(activeKey, (key) => {
  if (key === 'campaigns') loadCampaigns()
  if (key === 'tiers' && selectedCampaign.value) loadTiers()
  if (key === 'codes') {
    if (campaigns.value.length === 0) loadCampaigns()
    loadCodes()
  }
  if (key === 'records') loadRecords()
  if (key === 'winners') loadWinners()
})

async function loadCampaigns() {
  loading.value = true
  try {
    const res = await listCampaigns({ page: pagination.value.current, size: pagination.value.pageSize })
    campaigns.value = res.data.items || []
    pagination.value.total = res.data.total || 0
    if (campaigns.value.length > 0 && !selectedCampaign.value) {
      selectedCampaign.value = campaigns.value.find(c => c.status === 1) || campaigns.value[0]
    }
  } catch (e) {
    message.error('加载活动失败')
  } finally {
    loading.value = false
  }
}

async function loadTiers() {
  if (!selectedCampaign.value) return
  tierLoading.value = true
  try {
    const res = await listTiers(selectedCampaign.value.id)
    tiers.value = res.data || []
  } catch (e) {
    message.error('加载奖项失败')
  } finally {
    tierLoading.value = false
  }
}

async function loadCodes() {
  codeLoading.value = true
  try {
    const params = buildCodeQueryParams()
    const res = await listRedemptionCodes(params)
    codes.value = res.data.items || []
    codePagination.value.total = res.data.total || 0
  } catch (e) {
    message.error('加载兑换记录失败')
  } finally {
    codeLoading.value = false
  }
}

function buildCodeQueryParams() {
  const params = {
    page: codePagination.value.current,
    size: codePagination.value.pageSize
  }
  const q = codeQuery.value
  if (q.campaignId) params.campaignId = q.campaignId
  if (q.tierId) params.tierId = q.tierId
  if (q.status) params.status = q.status
  if (q.userKeyword && q.userKeyword.trim()) params.userKeyword = q.userKeyword.trim()
  if (q.startTime) params.startTime = q.startTime.format('YYYY-MM-DDTHH:mm:ss')
  if (q.endTime) params.endTime = q.endTime.format('YYYY-MM-DDTHH:mm:ss')
  return params
}

function searchCodes() {
  codePagination.value.current = 1
  loadCodes()
}

function resetCodeQuery() {
  codeQuery.value = {
    campaignId: null,
    tierId: null,
    status: null,
    userKeyword: '',
    startTime: null,
    endTime: null
  }
  codeTierOptions.value = []
  codePagination.value.current = 1
  loadCodes()
}

async function onCodeCampaignChange(campaignId) {
  codeQuery.value.tierId = null
  codeTierOptions.value = []
  if (!campaignId) return
  try {
    const res = await listTiers(campaignId)
    codeTierOptions.value = res.data || []
  } catch (e) {
    // ignore
  }
}

async function loadRecords() {
  recordLoading.value = true
  try {
    const params = buildRecordQueryParams()
    const res = await listDrawRecords(params)
    drawRecords.value = res.data.items || []
    recordPagination.value.total = res.data.total || 0
  } catch (e) {
    message.error('加载抽奖记录失败')
  } finally {
    recordLoading.value = false
  }
}

function buildRecordQueryParams() {
  const params = {
    page: recordPagination.value.current,
    size: recordPagination.value.pageSize
  }
  const q = recordQuery.value
  if (q.drawType) params.drawType = q.drawType
  if (q.email) params.email = q.email
  if (q.nickname) params.nickname = q.nickname
  if (q.startTime) params.startTime = q.startTime.format('YYYY-MM-DDTHH:mm:ss')
  if (q.endTime) params.endTime = q.endTime.format('YYYY-MM-DDTHH:mm:ss')
  return params
}

function searchRecords() {
  recordPagination.value.current = 1
  loadRecords()
}

function resetRecordQuery() {
  recordQuery.value = {
    drawType: null,
    email: '',
    nickname: '',
    startTime: null,
    endTime: null
  }
  recordPagination.value.current = 1
  loadRecords()
}

async function loadWinners() {
  winnerLoading.value = true
  try {
    const res = await listDisplayWinners(selectedCampaign.value ? selectedCampaign.value.id : '')
    winners.value = res.data || []
  } catch (e) {
    message.error('加载展示墙失败')
  } finally {
    winnerLoading.value = false
  }
}

function handleCampaignTableChange(p) {
  pagination.value.current = p.current
  pagination.value.pageSize = p.pageSize
  loadCampaigns()
}

function handleCodeTableChange(p) {
  codePagination.value.current = p.current
  codePagination.value.pageSize = p.pageSize
  loadCodes()
}

function handleRecordTableChange(p) {
  recordPagination.value.current = p.current
  recordPagination.value.pageSize = p.pageSize
  loadRecords()
}

function openCampaignModal(record) {
  if (record) {
    campaignForm.value = {
      id: record.id,
      name: record.name,
      description: record.description,
      rules: record.rules || '',
      startTime: dayjs(record.startTime),
      endTime: dayjs(record.endTime),
      freeDrawsPerUser: record.freeDrawsPerUser
    }
  } else {
    campaignForm.value = { name: '', description: '', rules: '', startTime: dayjs(), endTime: dayjs().add(7, 'day'), freeDrawsPerUser: 1 }
  }
  campaignModalVisible.value = true
}

async function saveCampaignForm() {
  saving.value = true
  try {
    const payload = { ...campaignForm.value }
    payload.startTime = payload.startTime.format('YYYY-MM-DDTHH:mm:ss')
    payload.endTime = payload.endTime.format('YYYY-MM-DDTHH:mm:ss')
    await saveCampaign(payload)
    message.success('保存成功')
    campaignModalVisible.value = false
    loadCampaigns()
  } catch (e) {
    message.error(e.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function openCampaign(id) {
  try {
    await apiOpenCampaign(id)
    message.success('已开启')
    loadCampaigns()
  } catch (e) {
    message.error(e.response?.data?.message || '操作失败')
  }
}

async function closeCampaign(id) {
  try {
    await apiCloseCampaign(id)
    message.success('已关闭')
    loadCampaigns()
  } catch (e) {
    message.error(e.response?.data?.message || '操作失败')
  }
}

function openCloneModal(record) {
  cloneSource.value = record
  cloneForm.value = { name: `${record.name}-副本` }
  cloneModalVisible.value = true
}

async function saveClone() {
  const name = cloneForm.value.name?.trim()
  if (!name) {
    message.warning('请输入新活动名称')
    return
  }
  cloneSaving.value = true
  try {
    await apiCloneCampaign(cloneSource.value.id, { name })
    message.success('复制成功')
    cloneModalVisible.value = false
    loadCampaigns()
  } catch (e) {
    message.error(e.response?.data?.message || '复制失败')
  } finally {
    cloneSaving.value = false
  }
}

async function removeCampaign(id) {
  try {
    await deleteCampaign(id)
    message.success('已删除')
    loadCampaigns()
  } catch (e) {
    message.error(e.response?.data?.message || '删除失败')
  }
}

function generateTierKey() {
  return `tier_${Date.now()}_${Math.floor(Math.random() * 1000)}`
}

function resetRewardParams() {
  rewardParams.value = {
    coin: { amount: 100 },
    membership: { plan_key: 'pro', durationType: 'days', days: 30, cycle: 'month' },
    coupon: { coupon_type: 'percent', discount_value: '0.8', applicable_cycle: 'all', applicable_plan: 'all' }
  }
}

function onRewardTypeChange() {
  resetRewardParams()
  tierForm.value.rewardValueJson = buildRewardValueJson(tierForm.value.rewardType)
}

function buildRewardValueJson(type) {
  if (type === 'coin') {
    return JSON.stringify({ amount: rewardParams.value.coin.amount })
  }
  if (type === 'membership') {
    const p = rewardParams.value.membership
    const obj = { plan_key: p.plan_key }
    if (p.durationType === 'days') {
      obj.days = p.days
    } else {
      obj.cycle = p.cycle
    }
    return JSON.stringify(obj)
  }
  if (type === 'coupon') {
    const p = rewardParams.value.coupon
    return JSON.stringify({
      coupon_type: p.coupon_type,
      discount_value: p.discount_value,
      applicable_cycle: p.applicable_cycle,
      applicable_plan: p.applicable_plan
    })
  }
  return '{}'
}

function parseRewardValueJson(type, json) {
  resetRewardParams()
  try {
    const obj = JSON.parse(json || '{}')
    if (type === 'coin') {
      rewardParams.value.coin.amount = obj.amount ?? 100
    } else if (type === 'membership') {
      rewardParams.value.membership.plan_key = obj.plan_key ?? 'pro'
      if (obj.days !== undefined) {
        rewardParams.value.membership.durationType = 'days'
        rewardParams.value.membership.days = obj.days
      } else if (obj.cycle) {
        rewardParams.value.membership.durationType = 'cycle'
        rewardParams.value.membership.cycle = obj.cycle
      }
    } else if (type === 'coupon') {
      rewardParams.value.coupon.coupon_type = obj.coupon_type ?? 'percent'
      rewardParams.value.coupon.discount_value = obj.discount_value ?? '0.8'
      rewardParams.value.coupon.applicable_cycle = obj.applicable_cycle ?? 'all'
      rewardParams.value.coupon.applicable_plan = obj.applicable_plan ?? 'all'
    }
  } catch (e) {
    // 解析失败时使用默认值
  }
}

function openTierModal(record) {
  if (record) {
    tierForm.value = { ...record }
    parseRewardValueJson(record.rewardType, record.rewardValueJson)
  } else {
    tierForm.value = { tierKey: generateTierKey(), tierName: '', prizeLevel: null, probability: 0, maxWinCount: null, displayRemaining: 0, displayRemainingCount: null, rewardType: 'coin', rewardValueJson: '{}', codePrefix: '', codeLength: null, codeValidityDays: 30, sortOrder: 0 }
    resetRewardParams()
    tierForm.value.rewardValueJson = buildRewardValueJson('coin')
  }
  tierModalVisible.value = true
}

async function saveTierForm() {
  tierSaving.value = true
  try {
    tierForm.value.rewardValueJson = buildRewardValueJson(tierForm.value.rewardType)
    if (tierProbabilityInfo.value.projected > 1) {
      message.error(`累计概率不能超过 100%（保存后预计 ${(tierProbabilityInfo.value.projected * 100).toFixed(2)}%）`)
      return
    }
    await saveTier(selectedCampaign.value.id, tierForm.value)
    message.success('保存成功')
    tierModalVisible.value = false
    loadTiers()
  } catch (e) {
    message.error(e.response?.data?.message || '保存失败')
  } finally {
    tierSaving.value = false
  }
}

async function removeTier(id) {
  try {
    await deleteTier(selectedCampaign.value.id, id)
    message.success('已删除')
    loadTiers()
  } catch (e) {
    message.error(e.response?.data?.message || '删除失败')
  }
}

async function openWinnerModal(record) {
  if (record) {
    winnerForm.value = { ...record, winTime: dayjs(record.winTime) }
  } else {
    winnerForm.value = { campaignId: selectedCampaign.value ? selectedCampaign.value.id : null, tierId: null, userId: null, prizeName: '', nickname: '', winTime: dayjs(), sortOrder: 0 }
  }
  winnerTierOptions.value = []
  userOptions.value = []
  await loadWinnerTierOptions(winnerForm.value.campaignId)
  await fetchUserOptions('')
  winnerModalVisible.value = true
}

async function loadWinnerTierOptions(campaignId) {
  winnerTierOptions.value = []
  if (!campaignId) return
  try {
    const res = await listTiers(campaignId)
    winnerTierOptions.value = res.data || []
  } catch (e) {
    // ignore
  }
}

async function onWinnerCampaignChange(campaignId) {
  winnerForm.value.tierId = null
  winnerForm.value.prizeName = ''
  await loadWinnerTierOptions(campaignId)
}

function onWinnerTierChange(tierId) {
  const tier = winnerTierOptions.value.find(t => t.id === tierId)
  winnerForm.value.prizeName = tier ? tier.tierName : ''
}

async function fetchUserOptions(keyword) {
  try {
    const list = await listUserOptions(keyword, 20)
    userOptions.value = list.map(u => ({ value: u.id, label: `${u.email || ''} / ${u.nickname || ''}`.replace(/^\s*\/\s*|\s*\/\s*$/g, '') }))
  } catch (e) {
    userOptions.value = []
  }
}

function onWinnerUserChange(userId) {
  const user = userOptions.value.find(u => u.value === userId)
  if (user) {
    const parts = user.label.split(' / ')
    const nickname = parts.length > 1 ? parts[1] : parts[0]
    winnerForm.value.nickname = nickname || winnerForm.value.nickname
  }
}

async function saveWinnerForm() {
  winnerSaving.value = true
  try {
    const payload = { ...winnerForm.value }
    if (payload.tierId && !payload.prizeName) {
      const tier = winnerTierOptions.value.find(t => t.id === payload.tierId)
      if (tier) payload.prizeName = tier.tierName
    }
    payload.winTime = payload.winTime.format('YYYY-MM-DDTHH:mm:ss')
    await saveDisplayWinner(payload)
    message.success('保存成功')
    winnerModalVisible.value = false
    loadWinners()
  } catch (e) {
    message.error(e.response?.data?.message || '保存失败')
  } finally {
    winnerSaving.value = false
  }
}

async function toggleWinner(id, status) {
  try {
    await toggleDisplayWinner(id, status)
    message.success('操作成功')
    loadWinners()
  } catch (e) {
    message.error(e.response?.data?.message || '操作失败')
  }
}

async function removeWinner(id) {
  try {
    await deleteDisplayWinner(id)
    message.success('已删除')
    loadWinners()
  } catch (e) {
    message.error(e.response?.data?.message || '删除失败')
  }
}

function campaignName(campaignId) {
  const c = campaigns.value.find(x => x.id === campaignId)
  return c ? c.name : campaignId
}

function openResetChanceModal(campaignId, userId) {
  if (campaignId != null && userId != null) {
    resetChanceForm.value = { campaignId, userId, preSet: true }
  } else {
    resetChanceForm.value = { campaignId: selectedCampaign.value ? selectedCampaign.value.id : null, userId: null, preSet: false }
  }
  resetChanceModalVisible.value = true
}

async function saveResetChance() {
  const { campaignId, userId } = resetChanceForm.value
  if (!campaignId || !userId) {
    message.warning('请选择活动并填写用户ID')
    return
  }
  resetChanceSaving.value = true
  try {
    await resetDrawChance({ campaignId, userId })
    message.success('重置成功')
    resetChanceModalVisible.value = false
  } catch (e) {
    message.error(e.response?.data?.message || '重置失败')
  } finally {
    resetChanceSaving.value = false
  }
}

function statusText(status) {
  const map = { 0: '草稿', 1: '进行中', 2: '已结束', 3: '已关闭' }
  return map[status] || status
}

function statusColor(status) {
  const map = { 0: 'default', 1: 'green', 2: 'blue', 3: 'orange' }
  return map[status] || 'default'
}

function statusTextCode(status) {
  const map = { unused: '未使用', used: '已使用', expired: '已过期' }
  return map[status] || status
}

function rewardTypeText(type) {
  const map = { coin: '创作币', membership: '会员', coupon: '折扣券', none: '谢谢回顾' }
  return map[type] || type
}

function prizeLevelText(level) {
  const map = { 1: '特等奖', 2: '一等奖', 3: '二等奖', 4: '三等奖', 5: '四等奖', 6: '五等奖', 7: '六等奖', 8: '七等奖', 9: '八等奖' }
  return map[level] || '-'
}

function formatTime(t) {
  if (!t) return '-'
  return dayjs(t).format('MM-DD HH:mm')
}
</script>

<style scoped>
.lottery-admin {
  background: #fff;
  padding: 24px;
  border-radius: 12px;
  min-height: calc(100vh - 112px);
}

.add-btn {
  margin-bottom: 16px;
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

.lottery-tabs :deep(.ant-tabs-content) {
  padding-top: 8px;
}

.record-query-form {
  margin-bottom: 16px;
  padding: 16px;
  background: #f5f5f5;
  border-radius: 8px;
}

.record-query-form :deep(.ant-form-item) {
  margin-bottom: 12px;
}
</style>
