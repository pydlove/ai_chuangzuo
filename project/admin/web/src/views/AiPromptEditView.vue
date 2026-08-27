<template>
  <div class="ai-prompt-edit">
    <a-card :bordered="false">
      <a-page-header
        :title="editingId ? '编辑提示词' : '新建提示词'"
        :sub-title="editingId ? `ID #${editingId}` : ''"
        @back="goBack"
      />

      <a-spin :spinning="loading">
        <a-form
          ref="formRef"
          :model="form"
          :rules="rules"
          layout="vertical"
          style="max-width: 900px; margin-top: 16px"
        >
          <a-row :gutter="16">
            <a-col :xs="24" :md="12">
              <a-form-item label="提示词编码" name="promptCode" :rules="[{ required: true, message: '请输入编码' }]">
                <a-input v-model:value="form.promptCode" :disabled="!!editingId" placeholder="如 topic_title_v1" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :md="12">
              <a-form-item label="显示名称" name="promptName" :rules="[{ required: true, message: '请输入名称' }]">
                <a-input v-model:value="form.promptName" placeholder="如 爆款标题生成" />
              </a-form-item>
            </a-col>
          </a-row>

          <a-row :gutter="16">
            <a-col :xs="24" :md="8">
              <a-form-item label="归属端" name="module" :rules="[{ required: true, message: '请选择归属端' }]">
                <a-select v-model:value="form.module" placeholder="请选择">
                  <a-select-option value="admin">管理端</a-select-option>
                  <a-select-option value="user">用户端</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :xs="24" :md="8">
              <a-form-item label="业务分类" name="category">
                <a-input v-model:value="form.category" placeholder="如 topic_title" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :md="8">
              <a-form-item label="状态" name="status" :rules="[{ required: true, message: '请选择状态' }]">
                <a-select v-model:value="form.status">
                  <a-select-option :value="1">启用</a-select-option>
                  <a-select-option :value="0">停用</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
          </a-row>

          <a-form-item label="系统角色（system role）" name="systemRole">
            <a-textarea v-model:value="form.systemRole" :rows="4" placeholder="AI 身份设定" />
          </a-form-item>

          <a-form-item label="用户提示词（user prompt）" name="userPrompt" :rules="[{ required: true, message: '请输入用户提示词' }]">
            <a-textarea v-model:value="form.userPrompt" :rows="16" placeholder="支持 {{variable}} 变量占位符" />
          </a-form-item>

          <a-form-item label="变量定义">
            <a-table
              :columns="variableColumns"
              :data-source="form.variableSchema"
              :pagination="false"
              size="small"
              bordered
            >
              <template #bodyCell="{ column, record, index }">
                <template v-if="column.key === 'name'">
                  <a-input v-model:value="record.name" placeholder="变量名" />
                </template>
                <template v-else-if="column.key === 'required'">
                  <a-checkbox v-model:checked="record.required">必填</a-checkbox>
                </template>
                <template v-else-if="column.key === 'description'">
                  <a-input v-model:value="record.description" placeholder="描述" />
                </template>
                <template v-else-if="column.key === 'example'">
                  <a-input v-model:value="record.example" placeholder="示例值" />
                </template>
                <template v-else-if="column.key === 'actions'">
                  <a-button type="link" danger size="small" @click="removeVariable(index)">删除</a-button>
                </template>
              </template>
            </a-table>
            <a-button type="dashed" size="small" style="margin-top: 8px" @click="addVariable">
              + 添加变量
            </a-button>
            <a-button type="link" size="small" style="margin-top: 8px" @click="syncVariables">
              从提示词中自动解析变量
            </a-button>
          </a-form-item>

          <a-form-item label="备注" name="description">
            <a-input v-model:value="form.description" placeholder="可选" />
          </a-form-item>

          <a-form-item>
            <a-space>
              <a-button type="primary" :loading="submitting" @click="onSubmit">保存</a-button>
              <a-button :disabled="!editingId" @click="openTestModal">测试</a-button>
              <a-button @click="goBack">取消</a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </a-spin>
    </a-card>

    <AiPromptTestModal
      v-model:open="modalVisible"
      :prompt="form"
      @close="modalVisible = false"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useAiPrompt } from '@/composables/useAiPrompt.js'
import AiPromptTestModal from '@/components/AiPromptTestModal.vue'

const route = useRoute()
const router = useRouter()
const { getDetail, save } = useAiPrompt()

const editingId = computed(() => {
  const id = route.params.id
  if (!id || id === 'new') return null
  return Number(id)
})

const loading = ref(false)
const submitting = ref(false)
const formRef = ref()
const modalVisible = ref(false)

const form = reactive({
  id: null,
  promptCode: '',
  promptName: '',
  module: 'admin',
  category: '',
  systemRole: '',
  userPrompt: '',
  variableSchema: [],
  status: 1,
  sortOrder: 0,
  description: ''
})

const rules = {
  promptCode: [{ required: true, message: '请输入提示词编码', trigger: 'blur' }],
  promptName: [{ required: true, message: '请输入显示名称', trigger: 'blur' }],
  module: [{ required: true, message: '请选择归属端', trigger: 'change' }],
  userPrompt: [{ required: true, message: '请输入用户提示词', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const variableColumns = [
  { title: '变量名', key: 'name', width: 160 },
  { title: '必填', key: 'required', width: 80 },
  { title: '描述', key: 'description' },
  { title: '示例值', key: 'example', width: 180 },
  { title: '操作', key: 'actions', width: 80 }
]

const addVariable = () => {
  form.variableSchema.push({ name: '', required: false, description: '', example: '' })
}

const removeVariable = (index) => {
  form.variableSchema.splice(index, 1)
}

const syncVariables = () => {
  const text = (form.systemRole || '') + (form.userPrompt || '')
  const matches = text.match(/\{\{(\w+)\}\}/g) || []
  const names = [...new Set(matches.map((m) => m.replace(/[{}]/g, '')))]
  const existing = new Map(form.variableSchema.map((v) => [v.name, v]))
  form.variableSchema = names.map((name) => {
    if (existing.has(name)) {
      return existing.get(name)
    }
    return { name, required: true, description: '', example: '' }
  })
}

const loadDetail = async () => {
  if (!editingId.value) return
  loading.value = true
  try {
    const data = await getDetail(editingId.value)
    Object.assign(form, {
      id: data.id,
      promptCode: data.promptCode,
      promptName: data.promptName,
      module: data.module,
      category: data.category || '',
      systemRole: data.systemRole || '',
      userPrompt: data.userPrompt || '',
      variableSchema: data.variableSchema || [],
      status: data.status,
      sortOrder: data.sortOrder || 0,
      description: data.description || ''
    })
  } catch (e) {
    message.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const onSubmit = async () => {
  await formRef.value?.validate()
  submitting.value = true
  try {
    const payload = {
      promptCode: form.promptCode,
      promptName: form.promptName,
      module: form.module,
      category: form.category || undefined,
      systemRole: form.systemRole || undefined,
      userPrompt: form.userPrompt,
      variableSchema: form.variableSchema.filter((v) => v.name?.trim()),
      status: form.status,
      sortOrder: form.sortOrder,
      description: form.description || undefined
    }
    const savedId = await save(editingId.value, payload)
    if (!editingId.value && savedId) {
      router.replace(`/console/ai-prompts/${savedId}`)
    }
  } catch (e) {
    message.error(e.message || '保存失败')
  } finally {
    submitting.value = false
  }
}

const openTestModal = () => {
  modalVisible.value = true
}

const goBack = () => router.push('/console/ai-prompts')

onMounted(() => loadDetail())
</script>
