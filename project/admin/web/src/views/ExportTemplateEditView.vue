<template>
  <div class="export-template-edit">
    <a-card :bordered="false">
      <div class="page-header">
        <h3 class="page-title">导出模板</h3>
        <p class="page-desc">
          左侧是模板列表，可增删改；右侧表单按「参数定义」渲染控件（颜色 picker / 字号输入 / 下拉选择），
          底部实时预览同一篇占位文章。修改后立即保存，下次用户端拉取即生效。
        </p>
      </div>

      <div class="layout">
        <!-- 左侧：模板列表 -->
        <div class="layout-left">
          <div class="left-toolbar">
            <a-input
              v-model:value="keyword"
              placeholder="搜索模板 key / 名称"
              allow-clear
              size="small"
            />
            <a-button type="primary" size="small" @click="openCreateModal">
              <template #icon><PlusOutlined /></template>
              新增模板
            </a-button>
          </div>
          <a-list
            :data-source="filteredTemplates"
            :loading="loading"
            size="small"
            class="template-list"
          >
            <template #renderItem="{ item }">
              <a-list-item
                :class="['template-list-item', { active: selectedId === item.id }]"
                @click="selectTemplate(item)"
              >
                <div class="item-main">
                  <div class="item-name">{{ item.name }}</div>
                  <div class="item-key">{{ item.templateKey }}</div>
                </div>
                <a-tag :color="item.status === 1 ? 'green' : 'default'" size="small">
                  {{ item.status === 1 ? '启用' : '禁用' }}
                </a-tag>
              </a-list-item>
            </template>
          </a-list>
        </div>

        <!-- 右侧：表单 + 预览 -->
        <div class="layout-right">
          <a-empty v-if="!current" description="选择左侧模板开始编辑" />
          <template v-else>
            <a-tabs v-model:active-key="activeGroup" class="group-tabs">
              <a-tab-pane v-for="group in groups" :key="group.key" :tab="group.label">
                <a-form
                  v-for="param in group.params"
                  :key="param.id"
                  layout="vertical"
                  class="param-form"
                >
                  <a-form-item :label="param.displayLabel">
                    <template v-if="param.fieldType === 'color'">
                      <div class="color-row">
                        <a-input
                          :value="currentValues[param.paramKey]"
                          size="small"
                          @update:value="(v) => updateValue(param.paramKey, v)"
                        />
                        <input
                          type="color"
                          class="color-picker"
                          :value="normalizeColor(currentValues[param.paramKey])"
                          @input="(e) => updateValue(param.paramKey, e.target.value)"
                        />
                      </div>
                    </template>
                    <template v-else-if="param.fieldType === 'number'">
                      <a-input-number
                        :value="currentValues[param.paramKey]"
                        size="small"
                        style="width: 100%"
                        @update:value="(v) => updateValue(param.paramKey, v)"
                      />
                    </template>
                    <template v-else-if="param.fieldType === 'select'">
                      <a-select
                        :value="currentValues[param.paramKey]"
                        :options="(param.options || []).map((o) => ({ value: o, label: o }))"
                        size="small"
                        allow-clear
                        style="width: 100%"
                        @update:value="(v) => updateValue(param.paramKey, v)"
                      />
                    </template>
                    <template v-else>
                      <a-input
                        :value="currentValues[param.paramKey]"
                        size="small"
                        @update:value="(v) => updateValue(param.paramKey, v)"
                      />
                    </template>
                  </a-form-item>
                </a-form>
              </a-tab-pane>
            </a-tabs>

            <div class="action-bar">
              <a-button type="primary" :loading="saving" @click="onSave">
                保存模板
              </a-button>
              <a-popconfirm
                title="确定删除此模板？"
                ok-text="删除"
                cancel-text="取消"
                @confirm="onDelete"
              >
                <a-button danger>删除</a-button>
              </a-popconfirm>
            </div>

            <!-- 实时预览 -->
            <a-divider>实时预览</a-divider>
            <ArticlePreview
              :visual-style="currentVisualStyleJson"
              :signature-text="current.signatureText"
              class="preview-area"
            />
          </template>
        </div>
      </div>
    </a-card>

    <!-- 新增模板弹框 -->
    <a-modal
      v-model:open="createModalOpen"
      title="新增导出模板"
      ok-text="创建"
      cancel-text="取消"
      @ok="onCreate"
    >
      <a-form layout="vertical">
        <a-form-item label="模板 key" required>
          <a-input v-model:value="newTemplate.templateKey" placeholder="如：my-platform-v1（英文+数字+连字符）" />
        </a-form-item>
        <a-form-item label="模板名称" required>
          <a-input v-model:value="newTemplate.name" placeholder="如：我的公众号模板" />
        </a-form-item>
        <a-form-item label="平台">
          <a-input v-model:value="newTemplate.platform" placeholder="如：wechat / xiaohongshu / general" />
        </a-form-item>
        <a-form-item label="备注">
          <a-input v-model:value="newTemplate.description" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import {
  listExportTemplates,
  getExportTemplate,
  createExportTemplate,
  updateExportTemplate,
  deleteExportTemplate,
  listExportTemplateParams
} from '@/api/exportTemplate.js'
import ArticlePreview from '@/components/ArticlePreview.vue'

const GROUP_ORDER = ['整体', '排版', '标题', 'Meta', '正文', '小标题', '高亮块']
const GROUP_LABEL = {
  '整体': '整体样式',
  '排版': '排版',
  '标题': '标题',
  'Meta': 'Meta 行',
  '正文': '正文段落',
  '小标题': '小标题',
  '高亮块': '高亮块（callout）'
}

const templates = ref([])
const params = ref([])
const loading = ref(false)
const saving = ref(false)
const keyword = ref('')
const selectedId = ref(null)
const current = ref(null)
const currentValues = ref({})
const activeGroup = ref(GROUP_ORDER[0])
const createModalOpen = ref(false)
const newTemplate = ref({ templateKey: '', name: '', platform: 'general', description: '' })

const filteredTemplates = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return templates.value
  return templates.value.filter((t) =>
    `${t.templateKey} ${t.name}`.toLowerCase().includes(kw)
  )
})

const groups = computed(() => {
  return GROUP_ORDER
    .map((key) => ({
      key,
      label: GROUP_LABEL[key] || key,
      params: params.value.filter((p) => p.groupLabel === key)
    }))
    .filter((g) => g.params.length > 0)
})

// 当前模板的 visualStyleJson（用 currentValues 即时拼成 JSON 字符串，供预览用）
const currentVisualStyleJson = computed(() => JSON.stringify(currentValues.value))

watch(activeGroup, () => {})

const fetchTemplates = async () => {
  loading.value = true
  try {
    templates.value = await listExportTemplates()
    if (templates.value.length && selectedId.value == null) {
      selectTemplate(templates.value[0])
    }
  } catch (e) {
    message.error(e?.message || '加载模板失败')
  } finally {
    loading.value = false
  }
}

const fetchParams = async () => {
  try {
    params.value = await listExportTemplateParams()
  } catch (e) {
    message.error(e?.message || '加载参数定义失败')
  }
}

const selectTemplate = async (item) => {
  selectedId.value = item.id
  try {
    const detail = await getExportTemplate(item.id)
    current.value = detail
    initValuesFromStyle(detail.visualStyleJson)
  } catch (e) {
    message.error(e?.message || '加载模板详情失败')
  }
}

const initValuesFromStyle = (visualStyleJson) => {
  const parsed = parseStyleJson(visualStyleJson)
  const values = {}
  for (const p of params.value) {
    values[p.paramKey] = parsed[p.paramKey] != null
      ? String(parsed[p.paramKey])
      : (p.defaultValue != null ? String(p.defaultValue) : '')
  }
  currentValues.value = values
}

const parseStyleJson = (raw) => {
  if (!raw) return {}
  if (typeof raw === 'object') return raw
  try {
    return JSON.parse(raw)
  } catch (e) {
    return {}
  }
}

const updateValue = (key, value) => {
  currentValues.value = { ...currentValues.value, [key]: value == null ? '' : String(value) }
}

const normalizeColor = (v) => {
  if (!v) return '#ffffff'
  // input[type=color] 只接受 6 位 #RRGGBB
  if (/^#[0-9a-fA-F]{6}$/.test(v)) return v
  if (/^#[0-9a-fA-F]{3}$/.test(v)) {
    return '#' + v.slice(1).split('').map((c) => c + c).join('')
  }
  return '#ffffff'
}

const onSave = async () => {
  if (!current.value) return
  saving.value = true
  try {
    // 把 currentValues 还原成 visual_style_json：去掉空值、保留非空
    const styleJson = {}
    for (const [k, v] of Object.entries(currentValues.value)) {
      if (v !== '' && v != null) styleJson[k] = v
    }
    await updateExportTemplate(current.value.id, {
      templateKey: current.value.templateKey,
      name: current.value.name,
      platform: current.value.platform,
      description: current.value.description,
      bgColor: current.value.bgColor,
      textColor: current.value.textColor,
      visualStyleJson: JSON.stringify(styleJson),
      signatureText: current.value.signatureText,
      signaturePosition: current.value.signaturePosition,
      sortOrder: current.value.sortOrder,
      status: current.value.status
    })
    message.success('已保存')
    await fetchTemplates()
  } catch (e) {
    message.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const onDelete = async () => {
  if (!current.value) return
  try {
    await deleteExportTemplate(current.value.id)
    message.success('已删除')
    selectedId.value = null
    current.value = null
    await fetchTemplates()
  } catch (e) {
    message.error(e?.message || '删除失败')
  }
}

const openCreateModal = () => {
  newTemplate.value = { templateKey: '', name: '', platform: 'general', description: '' }
  createModalOpen.value = true
}

const onCreate = async () => {
  const t = newTemplate.value
  if (!t.templateKey.trim() || !t.name.trim()) {
    message.error('模板 key 和名称必填')
    return
  }
  try {
    await createExportTemplate({
      templateKey: t.templateKey.trim(),
      name: t.name.trim(),
      platform: t.platform.trim() || 'general',
      description: t.description.trim(),
      bgColor: '#ffffff',
      textColor: '#1a1a1a',
      visualStyleJson: '{}',
      signatureText: '',
      signaturePosition: 'end',
      sortOrder: 99,
      status: 1
    })
    message.success('已创建')
    createModalOpen.value = false
    await fetchTemplates()
  } catch (e) {
    message.error(e?.message || '创建失败')
  }
}

onMounted(async () => {
  await fetchParams()
  await fetchTemplates()
})
</script>

<style scoped>
.export-template-edit { padding: 16px; }
.page-header { margin-bottom: 16px; }
.page-title { margin: 0 0 4px 0; font-size: 18px; font-weight: 600; }
.page-desc { margin: 0; color: #8c8c8c; font-size: 13px; max-width: 720px; }
.layout {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 16px;
  min-height: 600px;
}
.layout-left {
  background: #fafafa;
  border-radius: 8px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.left-toolbar {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.template-list {
  flex: 1;
  overflow-y: auto;
  background: #fff;
  border-radius: 6px;
}
.template-list-item {
  cursor: pointer;
  padding: 10px 12px;
  border-radius: 6px;
}
.template-list-item.active {
  background: #e6f7ff;
}
.template-list-item:hover {
  background: #f0f0f0;
}
.item-main { flex: 1; min-width: 0; }
.item-name { font-weight: 500; font-size: 13px; }
.item-key {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 11px;
  color: #8c8c8c;
  margin-top: 2px;
}
.layout-right {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
}
.group-tabs { margin-bottom: 16px; }
.param-form { margin-bottom: 0; }
.color-row {
  display: flex;
  gap: 8px;
  align-items: center;
}
.color-picker {
  width: 36px;
  height: 28px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  cursor: pointer;
  padding: 0;
  background: transparent;
}
.action-bar {
  display: flex;
  gap: 8px;
  margin-top: 16px;
}
.preview-area {
  max-width: 720px;
  margin: 0 auto;
  border: 1px solid #eeeeee;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
</style>