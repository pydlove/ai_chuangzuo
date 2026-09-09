<template>
  <div class="sms-config">
    <a-card :bordered="false">
      <a-page-header title="短信配置" sub-title="查看短信验证码发送记录，配置发送策略" style="padding-left: 0; padding-top: 0" />

      <a-tabs v-model:activeKey="activeTab">
        <!-- 发送记录 -->
        <a-tab-pane key="records" tab="发送记录">
          <div class="search-bar">
            <a-input v-model:value="recordQuery.phone" placeholder="手机号" style="width: 160px" allow-clear @press-enter="handleRecordSearch" />
            <a-select
              v-model:value="recordQuery.scene"
              placeholder="发送场景"
              style="width: 160px"
              allow-clear
              :options="sceneOptions"
            />
            <a-select
              v-model:value="recordQuery.sendStatus"
              placeholder="发送状态"
              style="width: 120px"
              allow-clear
              :options="statusOptions"
            />
            <a-range-picker v-model:value="recordDateRange" style="width: 260px" show-time />
            <a-button type="primary" @click="handleRecordSearch">搜索</a-button>
            <a-button @click="handleRecordReset">重置</a-button>
          </div>

          <a-table
            :columns="recordColumns"
            :data-source="recordList"
            :loading="recordLoading"
            :pagination="recordPagination"
            row-key="id"
            size="middle"
            @change="onRecordTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'scene'">
                <a-tag>{{ sceneText(record.scene) }}</a-tag>
              </template>
              <template v-else-if="column.key === 'sendStatus'">
                <a-tag :color="record.sendStatus === 1 ? 'green' : 'red'">
                  {{ record.sendStatus === 1 ? '成功' : '失败' }}
                </a-tag>
              </template>
              <template v-else-if="column.key === 'failReason'">
                <span v-if="record.failReason" class="fail-reason">{{ record.failReason }}</span>
                <span v-else>-</span>
              </template>
              <template v-else-if="column.key === 'createdAt'">
                {{ formatTime(record.createdAt) }}
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <!-- 配置 -->
        <a-tab-pane key="config" tab="配置">
          <a-alert
            message="使用说明"
            description="当前仅支持阿里云短信服务。请填写阿里云 AccessKey、签名、模板 Code 与 RegionId；保存后用户端注册 / 忘记密码即可使用短信验证码。"
            type="info"
            show-icon
            style="margin-bottom: 16px"
          />

          <a-spin :spinning="loading">
            <a-form layout="vertical" style="max-width: 720px" :model="form">
              <a-form-item label="短信服务商">
                <a-input value="阿里云" disabled />
              </a-form-item>

              <a-form-item label="AccessKey ID" required>
                <a-input v-model:value="form.accessKeyId" placeholder="请输入阿里云 AccessKey ID" maxlength="128" />
              </a-form-item>

              <a-form-item label="AccessKey Secret" required>
                <a-input-password
                  v-model:value="form.accessKeySecret"
                  placeholder="留空或全为 * 号表示不修改原密钥"
                  maxlength="256"
                />
              </a-form-item>

              <a-form-item label="短信签名" required>
                <a-input v-model:value="form.signName" placeholder="例如：爱创作" maxlength="64" />
              </a-form-item>

              <a-form-item label="短信模板 Code" required>
                <a-input v-model:value="form.templateCode" placeholder="例如：SMS_12345678" maxlength="64" />
              </a-form-item>

              <a-form-item label="RegionId（可选）">
                <a-input v-model:value="form.regionId" placeholder="dypnsapi 可不填" maxlength="32" />
              </a-form-item>

              <a-form-item label="启用短信验证码">
                <a-switch
                  v-model:checked="enabledChecked"
                  checked-children="启用"
                  un-checked-children="关闭"
                />
              </a-form-item>

              <a-divider orientation="left">安全策略</a-divider>

              <a-row :gutter="16">
                <a-col :span="12">
                  <a-form-item label="发送间隔（秒）">
                    <a-input-number
                      v-model:value="form.sendIntervalSeconds"
                      :min="1"
                      :max="3600"
                      :precision="0"
                      style="width: 100%"
                    />
                  </a-form-item>
                </a-col>
                <a-col :span="12">
                  <a-form-item label="单手机号日限（条）">
                    <a-input-number
                      v-model:value="form.dailyMaxPerPhone"
                      :min="1"
                      :max="100"
                      :precision="0"
                      style="width: 100%"
                    />
                  </a-form-item>
                </a-col>
              </a-row>

              <a-row :gutter="16">
                <a-col :span="12">
                  <a-form-item label="单 IP 日限（条）">
                    <a-input-number
                      v-model:value="form.dailyMaxPerIp"
                      :min="1"
                      :max="1000"
                      :precision="0"
                      style="width: 100%"
                    />
                  </a-form-item>
                </a-col>
                <a-col :span="12">
                  <a-form-item label="全站日限（条）">
                    <a-input-number
                      v-model:value="form.globalDailyMax"
                      :min="1"
                      :max="100000"
                      :precision="0"
                      style="width: 100%"
                    />
                  </a-form-item>
                </a-col>
              </a-row>

              <a-form-item>
                <a-space>
                  <a-button type="primary" :loading="submitting" @click="onSubmit">保存</a-button>
                  <a-button @click="load">重置</a-button>
                </a-space>
              </a-form-item>
            </a-form>
          </a-spin>
        </a-tab-pane>
      </a-tabs>
    </a-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { getSmsConfig, updateSmsConfig, fetchSmsSendRecords } from '@/api/security.js'

// ── tab ──
const activeTab = ref('records')

// ── 发送记录 ──
const SCENE_TEXT = {
  register: '注册',
  reset_password: '忘记密码',
  bind_phone: '绑定手机号'
}
const sceneOptions = Object.entries(SCENE_TEXT).map(([value, label]) => ({ value, label }))
const statusOptions = [
  { value: 1, label: '成功' },
  { value: 0, label: '失败' }
]

const recordQuery = reactive({
  phone: '',
  scene: undefined,
  sendStatus: undefined
})
const recordDateRange = ref(null)
const recordList = ref([])
const recordLoading = ref(false)
const recordTotal = ref(0)
const recordPage = ref(1)
const recordPageSize = ref(20)

const recordColumns = [
  { title: '手机号', dataIndex: 'phone', key: 'phone', width: 130 },
  { title: '场景', key: 'scene', width: 110 },
  { title: 'IP', dataIndex: 'clientIp', key: 'clientIp', width: 130 },
  { title: '用户ID', dataIndex: 'userId', key: 'userId', width: 90 },
  { title: '状态', key: 'sendStatus', width: 80 },
  { title: '失败原因', key: 'failReason', ellipsis: true },
  { title: '返回码', dataIndex: 'responseCode', key: 'responseCode', width: 110 },
  { title: '发送时间', key: 'createdAt', width: 170 }
]

const recordPagination = computed(() => ({
  current: recordPage.value,
  pageSize: recordPageSize.value,
  total: recordTotal.value,
  showTotal: (t) => `共 ${t} 条`,
  showSizeChanger: true
}))

function sceneText(scene) {
  return SCENE_TEXT[scene] || scene || '-'
}

function formatTime(t) {
  if (!t) return '-'
  return new Date(t).toLocaleString('zh-CN')
}

async function reloadRecords() {
  recordLoading.value = true
  try {
    const params = { page: recordPage.value, size: recordPageSize.value }
    if (recordQuery.phone) params.phone = recordQuery.phone.trim()
    if (recordQuery.scene) params.scene = recordQuery.scene
    if (recordQuery.sendStatus !== undefined && recordQuery.sendStatus !== null) {
      params.sendStatus = recordQuery.sendStatus
    }
    if (recordDateRange.value && recordDateRange.value.length === 2) {
      params.startTime = recordDateRange.value[0].format('YYYY-MM-DDTHH:mm:ss')
      params.endTime = recordDateRange.value[1].format('YYYY-MM-DDTHH:mm:ss')
    }
    const data = await fetchSmsSendRecords(params)
    recordList.value = data.items || []
    recordTotal.value = data.total || 0
  } catch (e) {
    // error handled by interceptor
  } finally {
    recordLoading.value = false
  }
}

function handleRecordSearch() {
  recordPage.value = 1
  reloadRecords()
}

function handleRecordReset() {
  recordQuery.phone = ''
  recordQuery.scene = undefined
  recordQuery.sendStatus = undefined
  recordDateRange.value = null
  recordPage.value = 1
  reloadRecords()
}

function onRecordTableChange(p) {
  recordPage.value = p.current
  recordPageSize.value = p.pageSize
  reloadRecords()
}

// ── 配置 ──
const loading = ref(false)
const submitting = ref(false)
const enabledChecked = ref(false)

const form = reactive({
  provider: 'aliyun',
  accessKeyId: '',
  accessKeySecret: '',
  signName: '',
  templateCode: '',
  regionId: '',
  enabled: 0,
  sendIntervalSeconds: 60,
  dailyMaxPerPhone: 20,
  dailyMaxPerIp: 100,
  globalDailyMax: 10000
})

function resetForm(data = {}) {
  form.provider = data.provider || 'aliyun'
  form.accessKeyId = data.accessKeyId || ''
  form.accessKeySecret = data.accessKeySecret || ''
  form.signName = data.signName || ''
  form.templateCode = data.templateCode || ''
  form.regionId = data.regionId || ''
  form.enabled = data.enabled === 1 ? 1 : 0
  enabledChecked.value = form.enabled === 1
  form.sendIntervalSeconds = parsePositiveInt(data.sendIntervalSeconds, 60)
  form.dailyMaxPerPhone = parsePositiveInt(data.dailyMaxPerPhone, 20)
  form.dailyMaxPerIp = parsePositiveInt(data.dailyMaxPerIp, 100)
  form.globalDailyMax = parsePositiveInt(data.globalDailyMax, 10000)
}

async function load() {
  loading.value = true
  try {
    const data = await getSmsConfig()
    resetForm(data || {})
  } catch (e) {
    message.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function onSubmit() {
  if (!form.accessKeyId.trim()) {
    message.warning('请填写 AccessKey ID')
    return
  }
  if (!form.signName.trim()) {
    message.warning('请填写短信签名')
    return
  }
  if (!form.templateCode.trim()) {
    message.warning('请填写短信模板 Code')
    return
  }
  if (!form.regionId.trim()) {
    message.warning('请填写 RegionId')
    return
  }

  submitting.value = true
  try {
    const payload = {
      provider: form.provider,
      accessKeyId: form.accessKeyId.trim(),
      accessKeySecret: form.accessKeySecret,
      signName: form.signName.trim(),
      templateCode: form.templateCode.trim(),
      regionId: form.regionId.trim(),
      enabled: enabledChecked.value ? 1 : 0,
      sendIntervalSeconds: form.sendIntervalSeconds,
      dailyMaxPerPhone: form.dailyMaxPerPhone,
      dailyMaxPerIp: form.dailyMaxPerIp,
      globalDailyMax: form.globalDailyMax
    }
    const data = await updateSmsConfig(payload)
    resetForm(data || {})
    message.success('保存成功')
  } catch (e) {
    message.error(e?.message || '保存失败')
  } finally {
    submitting.value = false
  }
}

function parsePositiveInt(value, fallback) {
  if (value == null) return fallback
  const parsed = Number(value)
  return Number.isFinite(parsed) && parsed > 0 ? Math.floor(parsed) : fallback
}

onMounted(() => {
  reloadRecords()
  load()
})
</script>

<style scoped>
.sms-config {
  max-width: 1200px;
}

.search-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
  align-items: center;
}

.fail-reason {
  color: #cf1322;
}
</style>
