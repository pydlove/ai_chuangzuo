<template>
  <div class="prompt-template-edit">
    <a-card :bordered="false">
      <a-page-header
        :title="editingId ? '编辑模板' : '新建模板'"
        :sub-title="editingId ? `ID #${editingId}` : ''"
        @back="goBack"
      />

      <a-spin :spinning="loading">
        <!-- 模板基础信息 -->
        <a-form
          ref="formRef"
          :model="form"
          :rules="rules"
          layout="vertical"
          style="max-width: 1400px; margin-top: 16px"
        >
          <a-row :gutter="16">
            <a-col :xs="24" :md="12">
              <a-form-item label="模板名称" name="name">
                <a-input v-model:value="form.name" placeholder="如：公众号通用模板" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :md="12">
              <a-form-item label="备注" name="remark">
                <a-input v-model:value="form.remark" placeholder="可选" />
              </a-form-item>
            </a-col>
          </a-row>

          <!-- 老模板未初始化 12 阶段时显示 -->
          <a-alert
            v-if="editingId && stagesInitialized === false"
            type="warning"
            show-icon
            style="margin-bottom: 16px"
            message="该模板还没初始化 12 阶段"
            description="点击下方按钮会用设计文档里的默认值补齐 12 个阶段配置（可后续逐个微调）。"
          >
            <template #action>
              <a-button size="small" type="primary" @click="onInitStages">
                初始化 12 阶段
              </a-button>
            </template>
          </a-alert>

          <!-- 12 阶段编辑器 -->
          <a-row :gutter="16" v-if="form.stages && form.stages.length">
            <!-- 左：12 阶段列表 -->
            <a-col :xs="24" :md="9" :lg="8">
              <div class="stage-list">
                <div
                  v-for="stage in form.stages"
                  :key="stage.stageIndex"
                  class="stage-item"
                  :class="{ active: activeIndex === stage.stageIndex, disabled: !stage.enabled }"
                  @click="activeIndex = stage.stageIndex"
                >
                  <div class="stage-item-num">{{ stage.stageIndex }}</div>
                  <div class="stage-item-body">
                    <div class="stage-item-name">
                      <span :class="['tag', 'tag-' + stage.stageType]">
                        {{ stage.typeLabel || typeLabelOf(stage.stageType) }}
                      </span>
                      <span class="stage-item-title">{{ stage.displayName || keyToName(stage.stageKey) }}</span>
                      <span v-if="!stage.enabled" class="stage-item-off">已停用</span>
                    </div>
                    <div class="stage-item-desc">{{ stage.description }}</div>
                  </div>
                </div>
              </div>
            </a-col>

            <!-- 右：当前选中阶段详情 -->
            <a-col :xs="24" :md="15" :lg="16">
              <div class="stage-detail">
                <div class="stage-detail-header">
                  <h4>
                    第 {{ currentStage.stageIndex }} 阶段：{{ currentStage.displayName || keyToName(currentStage.stageKey) }}
                    <a-tag :class="['tag-' + currentStage.stageType]">
                      {{ currentStage.typeLabel || typeLabelOf(currentStage.stageType) }}
                    </a-tag>
                  </h4>
                  <a-space>
                    <a-switch
                      v-model:checked="currentStage.enabled"
                      :checked-value="1"
                      :un-checked-value="0"
                      checked-children="启用"
                      un-checked-children="停用"
                    />
                    <a-button
                      size="small"
                      :disabled="currentStage.stageType === 'passthrough'"
                      @click="restoreStageDefault(currentStage)"
                    >
                      恢复默认
                    </a-button>
                  </a-space>
                </div>
                <p class="stage-detail-desc">{{ currentStage.description }}</p>

                <!-- AI 阶段：可编辑 prompt + 占位符 -->
                <template v-if="currentStage.stageType === 'ai_prompt'">
                  <a-textarea
                    v-model:value="currentStage.aiPrompt"
                    :rows="18"
                    :maxlength="100000"
                    show-count
                  />
                  <div class="hint">
                    可用占位符（点击插入到上方）：
                    <a-tag
                      v-for="p in currentStage.placeholders"
                      :key="p.name"
                      class="ph-tag"
                      @click="insertPlaceholder(p.name)"
                    >{{ p.name }}（{{ p.desc }}）</a-tag>
                  </div>
                </template>

                <!-- 规则阶段：表单字段 -->
                <template v-else-if="currentStage.stageType === 'rule_config'">
                  <a-form layout="vertical">
                    <a-form-item
                      v-for="field in currentStage.configFields"
                      :key="field.key"
                      :label="field.label"
                    >
                      <template #extra>
                        <span class="field-desc">{{ field.description }}</span>
                      </template>

                      <a-input-number
                        v-if="field.type === 'number'"
                        :value="getRuleValue(currentStage, field.key)"
                        :min="field.min"
                        :max="field.max"
                        style="width: 200px"
                        @change="(v) => setRuleValue(currentStage, field.key, v)"
                      />

                      <a-switch
                        v-else-if="field.type === 'boolean'"
                        :checked="getRuleValue(currentStage, field.key) === true"
                        @change="(v) => setRuleValue(currentStage, field.key, v === true)"
                      />

                      <a-select
                        v-else-if="field.type === 'select'"
                        :value="getRuleValue(currentStage, field.key)"
                        style="width: 320px"
                        @change="(v) => setRuleValue(currentStage, field.key, v)"
                      >
                        <a-select-option
                          v-for="opt in field.options"
                          :key="opt.value"
                          :value="opt.value"
                        >{{ opt.label }}</a-select-option>
                      </a-select>

                      <a-input
                        v-else
                        :value="getRuleValue(currentStage, field.key)"
                        @change="(e) => setRuleValue(currentStage, field.key, e.target.value)"
                      />
                    </a-form-item>
                  </a-form>
                </template>

                <!-- passthrough 阶段 -->
                <template v-else>
                  <a-alert
                    type="info"
                    show-icon
                    message="此阶段为程序化处理（无 AI 调用、无可配置项）"
                    description="由后端流水线自动完成；管理员无需也不能修改。"
                  />
                </template>
              </div>
            </a-col>
          </a-row>

          <!-- 操作按钮 -->
          <a-form-item style="margin-top: 24px">
            <a-space>
              <a-button type="primary" :loading="submitting" @click="onSubmit">保存全部</a-button>
              <a-button @click="goBack">取消</a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </a-spin>
    </a-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { usePromptTemplate } from '@/composables/usePromptTemplate.js'

const route = useRoute()
const router = useRouter()
const {
  handleCreate,
  handleUpdate,
  getTemplate,
  buildPayload,
  handleInitStages
} = usePromptTemplate()

const editingId = computed(() => {
  const id = route.params.id
  if (!id || id === 'new') return null
  return Number(id)
})

const loading = ref(false)
const submitting = ref(false)
const formRef = ref()
const activeIndex = ref(1) // 默认选中第 1 阶段
const stagesInitialized = ref(true) // 老模板可能为 false

const form = reactive({
  name: '',
  remark: '',
  stages: []
})

const rules = {
  name: [{ required: true, message: '请输入模板名称', trigger: 'blur' }]
}

const currentStage = computed(() => {
  if (!form.stages || !form.stages.length) return { stageIndex: 0 }
  return form.stages.find((s) => s.stageIndex === activeIndex.value) || form.stages[0]
})

const typeLabelOf = (t) => {
  if (t === 'ai_prompt') return 'AI'
  if (t === 'rule_config') return '规则'
  if (t === 'passthrough') return '系统'
  return '-'
}

const keyToName = (k) => {
  if (!k) return ''
  return k
    .split('_')
    .map((seg) => seg.charAt(0).toUpperCase() + seg.slice(1))
    .join(' ')
}

// 解析/写回 ruleConfig JSON
const getRuleValue = (stage, key) => {
  if (!stage.ruleConfig) return undefined
  try {
    const obj = JSON.parse(stage.ruleConfig)
    return obj[key]
  } catch (e) {
    return undefined
  }
}

const setRuleValue = (stage, key, value) => {
  let obj = {}
  try {
    obj = stage.ruleConfig ? JSON.parse(stage.ruleConfig) : {}
  } catch (e) {
    obj = {}
  }
  obj[key] = value
  stage.ruleConfig = JSON.stringify(obj)
}

// 把占位符插入到当前 stage 的 aiPrompt 末尾
const insertPlaceholder = (name) => {
  if (currentStage.value.stageType !== 'ai_prompt') return
  const tag = '{{' + name + '}}'
  currentStage.value.aiPrompt = (currentStage.value.aiPrompt || '') + tag
}

// 恢复单个 stage 默认值（AI 阶段恢复 prompt；规则阶段恢复 config）
const restoreStageDefault = (stage) => {
  // 后端没回传 default 值；前端先弹窗让用户从后端拿（这里简单处理：清空 + 让后端兜底）
  // 实际做法是调一次后端的 detail 拿 defaults，但因为后端已经返回了当前 stage 的 aiPrompt / ruleConfig，
  // 我们可以让用户手动「重置」时发请求让后端 reset。简化：直接清空当前字段，后端会兜底默认。
  if (stage.stageType === 'ai_prompt') {
    stage.aiPrompt = ''
  } else if (stage.stageType === 'rule_config') {
    stage.ruleConfig = '{}'
  }
  message.info('已清空该阶段内容，保存后后端会用默认兜底')
}

const loadEditing = async () => {
  if (!editingId.value) return
  loading.value = true
  try {
    const data = await getTemplate(editingId.value)
    Object.assign(form, {
      name: data.name,
      remark: data.remark,
      stages: data.stages || []
    })
    stagesInitialized.value = data.stagesInitialized !== false
    // 默认选中第一个 AI 阶段
    if (form.stages && form.stages.length) {
      const firstAi = form.stages.find((s) => s.stageType === 'ai_prompt')
      activeIndex.value = firstAi ? firstAi.stageIndex : form.stages[0].stageIndex
    }
  } catch (e) {
    message.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const onInitStages = async () => {
  if (!editingId.value) return
  loading.value = true
  try {
    await handleInitStages(editingId.value)
    await loadEditing()
  } finally {
    loading.value = false
  }
}

const onSubmit = async () => {
  await formRef.value?.validate()
  submitting.value = true
  try {
    const payload = buildPayload(form)
    if (editingId.value) {
      await handleUpdate(editingId.value, payload)
    } else {
      await handleCreate(payload)
    }
    goBack()
  } finally {
    submitting.value = false
  }
}

const goBack = () => router.push('/console/prompt-templates')

onMounted(() => loadEditing())
</script>

<style scoped>
.prompt-template-edit {
  max-width: 1400px;
}

/* 左：stage 列表 */
.stage-list {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  background: #fafafa;
  max-height: 720px;
  overflow-y: auto;
}
.stage-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 12px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  background: #fff;
  transition: background 0.15s;
}
.stage-item:last-child {
  border-bottom: none;
}
.stage-item:hover {
  background: #f5f5f5;
}
.stage-item.active {
  background: #e6f7ff;
  border-left: 3px solid #1890ff;
}
.stage-item.disabled {
  opacity: 0.5;
}
.stage-item-num {
  width: 22px;
  height: 22px;
  line-height: 22px;
  text-align: center;
  background: #f0f0f0;
  border-radius: 4px;
  font-size: 12px;
  color: #595959;
  flex-shrink: 0;
  margin-top: 2px;
}
.stage-item.active .stage-item-num {
  background: #1890ff;
  color: #fff;
}
.stage-item-body {
  flex: 1;
  min-width: 0;
}
.stage-item-name {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.stage-item-title {
  font-size: 13px;
  font-weight: 500;
  color: #262626;
}
.stage-item-off {
  font-size: 11px;
  color: #8c8c8c;
  background: #f5f5f5;
  padding: 0 4px;
  border-radius: 2px;
}
.stage-item-desc {
  margin-top: 4px;
  font-size: 12px;
  color: #8c8c8c;
  line-height: 1.4;
}

/* 类型 tag */
.tag {
  display: inline-block;
  padding: 0 6px;
  border-radius: 3px;
  font-size: 11px;
  line-height: 18px;
  border: 1px solid;
}
.tag-ai_prompt {
  background: #e6f4ff;
  color: #0958d9;
  border-color: #91caff;
}
.tag-rule_config {
  background: #f6ffed;
  color: #389e0d;
  border-color: #b7eb8f;
}
.tag-passthrough {
  background: #fafafa;
  color: #595959;
  border-color: #d9d9d9;
}

/* 右：详情 */
.stage-detail {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 16px 20px;
  min-height: 600px;
}
.stage-detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
  flex-wrap: wrap;
  gap: 8px;
}
.stage-detail-header h4 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #262626;
  display: flex;
  align-items: center;
  gap: 8px;
}
.stage-detail-desc {
  margin: 8px 0 16px;
  font-size: 13px;
  color: #595959;
  line-height: 1.6;
}
.hint {
  margin-top: 6px;
  color: #8c8c8c;
  font-size: 12px;
}
.ph-tag {
  cursor: pointer;
  margin-right: 4px;
  margin-bottom: 4px;
}
.field-desc {
  color: #8c8c8c;
  font-size: 12px;
}
</style>
