<template>
  <div class="market-skill-edit">
    <a-card :bordered="false">
      <a-page-header
        :title="editingBizNo ? '编辑提示词市场条目' : '新建提示词市场条目'"
        :sub-title="editingBizNo ? `ID ${editingBizNo}` : ''"
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
              <a-form-item label="提示词名称" name="skillName" :rules="[{ required: true, message: '请输入提示词名称', trigger: 'blur' }]">
                <a-input
                  v-model:value="form.skillName"
                  placeholder="例如：爆款情感文"
                  :maxlength="64"
                  show-count
                />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :md="12">
              <a-form-item label="发布者" name="publisherUserId" :rules="[{ required: true, message: '请选择发布者', trigger: 'change' }]">
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
            </a-col>
          </a-row>

          <a-row :gutter="16">
            <a-col :xs="24" :md="12">
              <a-form-item label="上架状态" name="enableStatus" :rules="[{ required: true, message: '请选择上架状态', trigger: 'change' }]">
                <a-select v-model:value="form.enableStatus" placeholder="请选择">
                  <a-select-option :value="1">上架</a-select-option>
                  <a-select-option :value="0">下架</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :xs="24" :md="12">
              <a-form-item label="官方精选" name="featured" :rules="[{ required: true, message: '请选择官方精选状态', trigger: 'change' }]">
                <a-select v-model:value="form.featured" placeholder="请选择">
                  <a-select-option :value="1">官方精选</a-select-option>
                  <a-select-option :value="0">普通</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
          </a-row>

          <a-form-item label="简短描述" name="description">
            <a-input
              v-model:value="form.description"
              placeholder="一句话描述，方便用户浏览"
              :maxlength="256"
            />
          </a-form-item>

          <a-form-item label="提示词摘要" name="promptSummary">
            <a-textarea
              v-model:value="form.promptSummary"
              :rows="3"
              placeholder="提示词摘要，最多 512 字"
              :maxlength="512"
            />
          </a-form-item>

          <a-form-item label="提示词" name="prompt" :rules="[{ required: true, message: '请输入提示词', trigger: 'blur' }]">
            <a-textarea
              v-model:value="form.prompt"
              :rows="12"
              placeholder="喂给 AI 的完整提示词"
            />
          </a-form-item>

          <a-form-item label="适用范围" name="scope">
            <div class="market-skill-scope-tags">
              <a-tag
                v-for="tag in scopeTags"
                :key="tag"
                closable
                @close.prevent="removeTag(tag)"
              >
                {{ tag }}
              </a-tag>
              <input
                v-if="scopeTags.length < MAX_SCOPE_TAGS"
                v-model="scopeInput"
                type="text"
                class="market-skill-scope-input"
                placeholder="输入标签后按 Tab 或回车"
                :maxlength="MAX_SCOPE_TAG_LENGTH"
                @keydown.enter.prevent="addTag"
                @keydown.tab="handleScopeTab"
              />
            </div>
            <div class="market-skill-scope-hint">
              最多 {{ MAX_SCOPE_TAGS }} 个标签，每个不超过 {{ MAX_SCOPE_TAG_LENGTH }} 个字（可选）
            </div>
          </a-form-item>

          <a-form-item label="累计使用" name="totalUses">
            <a-input-number
              v-model:value="form.totalUses"
              :min="0"
              style="width: 160px"
            />
          </a-form-item>

          <a-form-item>
            <a-space>
              <a-button type="primary" :loading="submitting" @click="onSubmit">保存</a-button>
              <a-button @click="goBack">取消</a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </a-spin>
    </a-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useScopeTags } from '@/composables/useScopeTags.js'
import { listUserOptions } from '@/api/userOptions.js'
import { createMarketSkill, updateMarketSkill, getMarketSkill } from '@/api/marketSkill.js'

const route = useRoute()
const router = useRouter()

const editingBizNo = computed(() => {
  const id = route.params.id
  if (!id || id === 'new') return null
  return id
})

const loading = ref(false)
const submitting = ref(false)
const formRef = ref()

const form = reactive({
  skillName: '',
  publisherUserId: null,
  description: '',
  promptSummary: '',
  prompt: '',
  totalUses: 0,
  enableStatus: 1,
  featured: 0
})

const rules = {
  skillName: [{ required: true, message: '请输入提示词名称', trigger: 'blur' }],
  publisherUserId: [{ required: true, message: '请选择发布者', trigger: 'change' }],
  prompt: [{ required: true, message: '请输入提示词', trigger: 'blur' }],
  enableStatus: [{ required: true, message: '请选择上架状态', trigger: 'change' }],
  featured: [{ required: true, message: '请选择官方精选状态', trigger: 'change' }]
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

const publisherOptions = ref([])
const publisherLoading = ref(false)
const publisherKeyword = ref('')

const searchPublisher = async (kw) => {
  publisherKeyword.value = kw
  await loadPublisherOptions(kw)
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

const handleScopeTab = (e) => {
  const raw = scopeInput.value?.trim()
  if (raw) {
    e.preventDefault()
    addTag()
  }
}

const loadDetail = async () => {
  if (!editingBizNo.value) return
  loading.value = true
  try {
    const data = await getMarketSkill(editingBizNo.value)
    Object.assign(form, {
      skillName: data.name || '',
      publisherUserId: data.publisherUserId,
      description: data.description || '',
      promptSummary: data.promptSummary || '',
      prompt: data.prompt || '',
      totalUses: data.totalUses || 0,
      enableStatus: data.status === 'enabled' ? 1 : 0,
      featured: data.featured === 1 ? 1 : 0
    })
    scopeRef.value = data.scope || ''
    publisherOptions.value = [{
      label: data.publisherName ? `${data.publisherName}（${data.publisherUserId}）` : String(data.publisherUserId),
      value: data.publisherUserId
    }]
  } catch (e) {
    message.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const onSubmit = async () => {
  await formRef.value?.validate()
  if (scopeError.value) {
    message.error(scopeError.value)
    return
  }
  submitting.value = true
  try {
    const payload = {
      skillName: form.skillName.trim(),
      publisherUserId: form.publisherUserId,
      description: form.description || '',
      promptSummary: form.promptSummary || '',
      prompt: form.prompt.trim(),
      scope: scopeRef.value || '',
      totalUses: form.totalUses || 0,
      enableStatus: form.enableStatus,
      featured: form.featured
    }
    if (editingBizNo.value) {
      await updateMarketSkill(editingBizNo.value, payload)
    } else {
      const bizNo = await createMarketSkill(payload)
      if (bizNo) {
        router.replace(`/console/market-skills/${bizNo}`)
        return
      }
    }
    message.success('保存成功')
    goBack()
  } catch (e) {
    message.error(e.message || '保存失败')
  } finally {
    submitting.value = false
  }
}

const goBack = () => router.push('/console/market-skills')

onMounted(() => loadDetail())

watch(() => route.params.id, () => {
  loadDetail()
})
</script>

<style scoped>
.market-skill-scope-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.market-skill-scope-input {
  min-width: 160px;
  flex: 1;
  padding: 4px 8px;
  border: 1px dashed #d9d9d9;
  border-radius: 4px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
}

.market-skill-scope-input:focus {
  border-color: #ff2442;
}

.market-skill-scope-input::placeholder {
  color: #bfbfbf;
}

.market-skill-scope-hint {
  margin-top: 4px;
  font-size: 12px;
  color: #8c8c8c;
}
</style>
