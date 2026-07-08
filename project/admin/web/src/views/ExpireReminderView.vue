<template>
  <div class="expire-reminder">
    <!-- 配置卡片 -->
    <a-card :bordered="false" class="config-card">
      <div class="config-header">
        <h3 class="config-title">提醒配置</h3>
        <p class="config-desc">设置到期提醒的提前天数、提醒时间点、通知形式；保存后定时任务立即重排。</p>
      </div>
      <a-form
        v-if="config"
        :model="config"
        :rules="rules"
        ref="formRef"
        layout="vertical"
        class="config-form"
      >
        <a-row :gutter="16">
          <a-col :span="6">
            <a-form-item label="提前天数 N" name="advanceDays">
              <a-input-number v-model:value="config.advanceDays" :min="1" :max="90" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item label="提醒时间点（0-23）" name="notifyHour">
              <a-select v-model:value="config.notifyHour" style="width: 100%">
                <a-select-option v-for="h in 24" :key="h - 1" :value="h - 1">
                  {{ String(h - 1).padStart(2, '0') }}:00
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item label="通知形式" name="notifyChannel">
              <a-radio-group v-model:value="config.notifyChannel">
                <a-radio value="message">站内信</a-radio>
                <a-radio value="email">邮件</a-radio>
                <a-radio value="message_email">站内信+邮件</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item label="定时开关" name="enabled">
              <a-switch
                :checked="config.enabled === 1"
                @update:checked="(v) => (config.enabled = v ? 1 : 0)"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-button type="primary" :loading="saving" @click="handleSaveConfig">保存配置</a-button>
      </a-form>
    </a-card>

    <!-- 列表卡片 -->
    <a-card :bordered="false" class="list-card">
      <div class="list-header">
        <h3 class="list-title">近 {{ advanceDays }} 天到期用户</h3>
        <div class="list-tools">
          <a-input-number v-model:value="advanceDays" :min="1" :max="90" addon-before="提前天数" />
          <a-button type="primary" @click="fetchUsers">刷新</a-button>
        </div>
      </div>
      <a-table
        :columns="columns"
        :data-source="items"
        :loading="loading"
        :pagination="{ current: page, pageSize: pageSize, total: total, showSizeChanger: true }"
        row-key="userId"
        size="middle"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'remainingDays'">
            <a-tag :color="record.remainingDays <= 1 ? 'red' : record.remainingDays <= 3 ? 'orange' : 'blue'">
              {{ record.remainingDays }} 天
            </a-tag>
          </template>
          <template v-else-if="column.key === 'lastRemindedAt'">
            <span v-if="record.lastRemindedAt">{{ record.lastRemindedAt }}（{{ record.lastReminderChannel }}）</span>
            <span v-else>—</span>
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-button type="link" size="small" :loading="reminding[record.userId]" @click="handleRemind(record)">
              立即提醒
            </a-button>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  getReminderConfig,
  saveReminderConfig,
  listExpiringUsers,
  remindUser
} from '@/api/expireReminder.js'

const config = ref(null)
const saving = ref(false)
const formRef = ref()

const items = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const advanceDays = ref(7)
const loading = ref(false)
const reminding = reactive({})

const rules = {
  advanceDays: [{ required: true, type: 'number', min: 1, max: 90, message: '1-90' }],
  notifyHour: [{ required: true, type: 'number', min: 0, max: 23, message: '0-23' }],
  notifyChannel: [{ required: true, message: '请选择' }],
  enabled: [{ required: true, message: '请选择' }]
}

const columns = [
  { title: '用户ID', dataIndex: 'userId', key: 'userId', width: 90 },
  { title: '邮箱', dataIndex: 'email', key: 'email', width: 200 },
  { title: '昵称', dataIndex: 'nickname', key: 'nickname', width: 120 },
  { title: '到期时间', dataIndex: 'membershipExpireAt', key: 'membershipExpireAt', width: 170 },
  { title: '剩余天数', key: 'remainingDays', width: 90 },
  { title: '最近提醒', key: 'lastRemindedAt', width: 240 },
  { title: '操作', key: 'actions', width: 100 }
]

const fetchConfig = async () => {
  config.value = await getReminderConfig()
  if (config.value) advanceDays.value = config.value.advanceDays
}

const fetchUsers = async () => {
  loading.value = true
  try {
    const res = await listExpiringUsers({ advanceDays: advanceDays.value, page: page.value, pageSize: pageSize.value })
    items.value = res.items || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

const handleSaveConfig = async () => {
  await formRef.value.validate()
  saving.value = true
  try {
    const saved = await saveReminderConfig(config.value)
    config.value = saved
    message.success('配置已保存，定时任务已重排')
    fetchUsers()
  } finally {
    saving.value = false
  }
}

const handleRemind = async (record) => {
  reminding[record.userId] = true
  try {
    await remindUser(record.userId)
    message.success(`已提醒：${record.email}`)
    fetchUsers()
  } finally {
    reminding[record.userId] = false
  }
}

const handleTableChange = (pagination) => {
  page.value = pagination.current
  pageSize.value = pagination.pageSize
  fetchUsers()
}

onMounted(() => {
  fetchConfig()
  fetchUsers()
})
</script>

<style scoped>
.expire-reminder {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.config-card,
.list-card {
  border-radius: 8px;
}

.config-header,
.list-header {
  margin-bottom: 16px;
}

.config-title,
.list-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 4px 0;
}

.config-desc {
  font-size: 13px;
  color: #8c8c8c;
  margin: 0;
}

.list-tools {
  display: flex;
  gap: 8px;
  align-items: center;
}
</style>
