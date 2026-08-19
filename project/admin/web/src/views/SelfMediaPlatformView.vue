<template>
  <div class="platform-view">
    <h3 class="page-title">平台管理</h3>
    <p class="page-desc">
      配置用户端「制定你的自媒体方案」第一步中可选的平台，以及每个平台下的推荐字数和字数档位。
      保存后，用户端 /create 页面会实时读取最新配置。
    </p>

    <a-card :bordered="false">
      <div class="table-toolbar">
        <a-button type="primary" @click="onAdd">新增平台</a-button>
      </div>
      <a-table
        :columns="columns"
        :data-source="platforms"
        :loading="loading"
        row-key="id"
        :pagination="false"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-switch
              :checked="record.status === 1"
              :loading="togglingId === record.id"
              checked-children="启用"
              un-checked-children="停用"
              @change="(checked) => onToggleStatus(record, checked)"
            />
          </template>
          <template v-else-if="column.key === 'isDefault'">
            <a-tag v-if="record.isDefault === 1" color="red">默认</a-tag>
            <span v-else>-</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a @click="onEdit(record)">编辑</a>
              <a-popconfirm
                title="确定删除该平台吗？"
                ok-text="删除"
                cancel-text="取消"
                @confirm="onDelete(record.id)"
              >
                <a class="danger-link">删除</a>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="modalOpen"
      :title="editingId ? '编辑平台' : '新增平台'"
      :width="1280"
      :confirm-loading="submitting"
      @ok="onSave"
      @cancel="modalOpen = false"
    >
      <a-form layout="vertical" :model="form" class="platform-form">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="平台 key" required>
              <a-input v-model:value="form.platformKey" placeholder="如 wechat" :disabled="!!editingId" />
              <div class="form-hint">保存后不可修改，建议用英文小写</div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="平台名称" required>
              <a-input v-model:value="form.platformName" placeholder="如 公众号" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="24">
            <a-form-item label="平台简介">
              <a-input v-model:value="form.description" placeholder="一句话描述平台特点" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="推荐字数">
              <a-input-number v-model:value="form.recommendWords" :min="0" :max="3000" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="排序">
              <a-input-number v-model:value="form.sortOrder" :min="0" style="width: 100%" />
              <div class="form-hint">数字越小越靠前</div>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="24">
            <a-form-item label="平台特征">
              <a-textarea v-model:value="form.trait" :rows="2" placeholder="AI 生成时的风格/特征提示" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="24">
            <a-form-item label="一句话卖点">
              <a-input v-model:value="form.tagline" placeholder="如：图文种草社区，女性用户多，适合分享生活经验" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="内容形式">
              <a-select
                v-model:value="form.contentForm"
                mode="tags"
                placeholder="输入后回车，如 图文笔记"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="主要收益">
              <a-select
                v-model:value="form.monetization"
                mode="tags"
                placeholder="输入后回车，如 品牌广告"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="变现门槛">
              <a-input v-model:value="form.threshold" placeholder="如：0粉可带货，1000粉可接蒲公英商单" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="适合谁">
              <a-input v-model:value="form.bestFor" placeholder="如：有生活经验、愿意分享好物/干货的人" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="24">
            <a-form-item label="提示/推荐理由">
              <a-textarea
                v-model:value="form.reason"
                :rows="2"
                placeholder="如：粉丝粘性最强，变现路径稳定，但需要长期坚持"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="6">
            <a-form-item label="变现难度">
              <a-input v-model:value="form.monetizationEase" placeholder="如：中等" />
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item label="预计周期">
              <a-input v-model:value="form.timeToIncome" placeholder="如：2-4个月" />
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item label="收入空间">
              <a-input v-model:value="form.incomeRange" placeholder="如：几千~几万/月" />
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item label="运营难度">
              <a-input v-model:value="form.difficulty" placeholder="如：中" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="状态" required>
              <a-radio-group :value="form.status" @change="(e) => form.status = e.target.value">
                <a-radio :value="1">启用</a-radio>
                <a-radio :value="0">停用</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="是否默认">
              <a-switch
                :checked="form.isDefault === 1"
                checked-children="是"
                un-checked-children="否"
                @change="(checked) => form.isDefault = checked ? 1 : 0"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="24">
            <a-form-item label="图标 URL">
              <a-input v-model:value="form.iconUrl" placeholder="可选，平台图标 URL" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="24">
            <a-form-item label="字数档位配置">
              <div v-for="(item, idx) in form.wordCountPresets" :key="idx" class="preset-item">
                <a-input-number v-model:value="item.count" :min="1" :max="3000" placeholder="字数" />
                <a-input v-model:value="item.label" placeholder="标签，如 标准深度文" />
                <a-button type="link" danger @click="removePreset(idx)">删除</a-button>
              </div>
              <a-button type="dashed" block class="add-preset-btn" @click="addPreset">
                + 添加字数档位
              </a-button>
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  fetchPlatforms,
  createPlatform,
  updatePlatform,
  deletePlatform
} from '@/api/selfMediaPlatform.js'

const platforms = ref([])
const loading = ref(false)
const submitting = ref(false)
const togglingId = ref('')
const modalOpen = ref(false)
const editingId = ref(null)

const form = reactive(blankForm())

const columns = [
  { title: 'key', dataIndex: 'platformKey', key: 'platformKey', width: 120 },
  { title: '平台名', dataIndex: 'platformName', key: 'platformName', width: 120 },
  { title: '简介', dataIndex: 'description', key: 'description', ellipsis: true },
  { title: '推荐字数', dataIndex: 'recommendWords', key: 'recommendWords', width: 100 },
  { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder', width: 80 },
  { title: '状态', key: 'status', width: 110 },
  { title: '默认', key: 'isDefault', width: 80 },
  { title: '操作', key: 'action', width: 140, fixed: 'right' }
]

function blankForm() {
  return {
    platformKey: '',
    platformName: '',
    description: '',
    recommendWords: 1500,
    trait: '',
    tagline: '',
    contentForm: [],
    monetization: [],
    threshold: '',
    bestFor: '',
    reason: '',
    monetizationEase: '',
    timeToIncome: '',
    incomeRange: '',
    difficulty: '',
    wordCountPresets: [
      { count: 500, label: '短文' },
      { count: 1000, label: '中等' },
      { count: 1500, label: '标准' }
    ],
    sortOrder: 0,
    status: 1,
    isDefault: 0,
    iconUrl: ''
  }
}

async function load() {
  loading.value = true
  try {
    platforms.value = await fetchPlatforms()
  } catch (e) {
    message.error(e?.message || '加载平台列表失败')
  } finally {
    loading.value = false
  }
}

function onAdd() {
  editingId.value = null
  Object.assign(form, blankForm())
  modalOpen.value = true
}

function onEdit(record) {
  editingId.value = record.id
  Object.assign(form, blankForm(), {
    platformKey: record.platformKey,
    platformName: record.platformName,
    description: record.description || '',
    recommendWords: record.recommendWords || 0,
    trait: record.trait || '',
    tagline: record.tagline || '',
    contentForm: record.contentForm || [],
    monetization: record.monetization || [],
    threshold: record.threshold || '',
    bestFor: record.bestFor || '',
    reason: record.reason || '',
    monetizationEase: record.monetizationEase || '',
    timeToIncome: record.timeToIncome || '',
    incomeRange: record.incomeRange || '',
    difficulty: record.difficulty || '',
    wordCountPresets: (record.wordCountPresets || []).map((p) => ({ ...p })),
    sortOrder: record.sortOrder ?? 0,
    status: record.status ?? 1,
    isDefault: record.isDefault ?? 0,
    iconUrl: record.iconUrl || ''
  })
  modalOpen.value = true
}

function addPreset() {
  form.wordCountPresets.push({ count: 500, label: '' })
}

function removePreset(idx) {
  form.wordCountPresets.splice(idx, 1)
}

function validateForm() {
  if (!form.platformKey.trim()) {
    message.error('平台 key 不能为空')
    return false
  }
  if (!form.platformName.trim()) {
    message.error('平台名称不能为空')
    return false
  }
  if (form.sortOrder == null || form.sortOrder < 0) {
    message.error('排序不能为负数')
    return false
  }
  for (const p of form.wordCountPresets) {
    if (!p.label.trim()) {
      message.error('字数档位标签不能为空')
      return false
    }
    if (!p.count || p.count < 1) {
      message.error('字数档位字数必须大于 0')
      return false
    }
  }
  return true
}

function buildPayload() {
  return {
    platformKey: form.platformKey.trim(),
    platformName: form.platformName.trim(),
    description: form.description?.trim() || '',
    recommendWords: form.recommendWords ?? 0,
    trait: form.trait?.trim() || '',
    tagline: form.tagline?.trim() || '',
    contentForm: form.contentForm || [],
    monetization: form.monetization || [],
    threshold: form.threshold?.trim() || '',
    bestFor: form.bestFor?.trim() || '',
    reason: form.reason?.trim() || '',
    monetizationEase: form.monetizationEase?.trim() || '',
    timeToIncome: form.timeToIncome?.trim() || '',
    incomeRange: form.incomeRange?.trim() || '',
    difficulty: form.difficulty?.trim() || '',
    wordCountPresets: form.wordCountPresets.map((p) => ({
      count: p.count,
      label: p.label.trim()
    })),
    sortOrder: form.sortOrder ?? 0,
    status: form.status,
    isDefault: form.isDefault,
    iconUrl: form.iconUrl?.trim() || ''
  }
}

async function onSave() {
  if (!validateForm()) return
  submitting.value = true
  const payload = buildPayload()
  try {
    if (editingId.value) {
      await updatePlatform(editingId.value, payload)
      message.success('平台已更新')
    } else {
      await createPlatform(payload)
      message.success('平台已新增')
    }
    modalOpen.value = false
    await load()
  } catch (e) {
    message.error(e?.message || '保存失败')
  } finally {
    submitting.value = false
  }
}

async function onDelete(id) {
  try {
    await deletePlatform(id)
    message.success('平台已删除')
    await load()
  } catch (e) {
    message.error(e?.message || '删除失败')
  }
}

async function onToggleStatus(record, checked) {
  togglingId.value = record.id
  try {
    await updatePlatform(record.id, {
      platformKey: record.platformKey,
      platformName: record.platformName,
      description: record.description || '',
      recommendWords: record.recommendWords || 0,
      trait: record.trait || '',
      tagline: record.tagline || '',
      contentForm: record.contentForm || [],
      monetization: record.monetization || [],
      threshold: record.threshold || '',
      bestFor: record.bestFor || '',
      reason: record.reason || '',
      monetizationEase: record.monetizationEase || '',
      timeToIncome: record.timeToIncome || '',
      incomeRange: record.incomeRange || '',
      difficulty: record.difficulty || '',
      wordCountPresets: (record.wordCountPresets || []).map((p) => ({ ...p })),
      sortOrder: record.sortOrder ?? 0,
      status: checked ? 1 : 0,
      isDefault: record.isDefault ?? 0,
      iconUrl: record.iconUrl || ''
    })
    message.success(checked ? '已启用' : '已停用')
    await load()
  } catch (e) {
    message.error(e?.message || '切换失败')
  } finally {
    togglingId.value = ''
  }
}

onMounted(load)
</script>

<style scoped>
.platform-view { padding: 0; }
.page-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 4px;
}
.page-desc {
  color: #8c8c8c;
  margin: 0 0 16px;
  font-size: 13px;
}
.table-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 16px;
}
.platform-form { padding-top: 8px; }
.form-hint {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 2px;
}
.preset-item {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 10px;
}
.add-preset-btn {
  margin-top: 4px;
}
.danger-link {
  color: #ff4d4f;
}
</style>
