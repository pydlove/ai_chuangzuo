<template>
  <div class="home-testimonial-view">
    <a-card title="首页评价管理" :bordered="false">
      <template #extra>
        <a-button type="primary" @click="onCreate">新增评价</a-button>
      </template>
      <a-table :columns="columns" :data-source="testimonials" :loading="loading" row-key="id" :pagination="false">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'avatar'">
            <img
              v-if="record.avatarUrl"
              :src="record.avatarUrl"
              class="avatar-preview"
              alt="avatar"
            />
            <div v-else class="avatar-fallback">{{ record.name ? record.name[0] : 'U' }}</div>
          </template>
          <template v-else-if="column.key === 'starRating'">
            <a-rate :value="record.starRating" disabled />
          </template>
          <template v-else-if="column.key === 'reviewText'">
            <span class="review-text">{{ record.reviewText }}</span>
          </template>
          <template v-else-if="column.key === 'isEnabled'">
            <a-switch
              :checked="record.isEnabled === 1"
              @change="(checked) => onToggleStatus(record, checked)"
            />
          </template>
          <template v-else-if="column.key === 'action'">
            <a @click="onEdit(record)">编辑</a>
            <a-divider type="vertical" />
            <a-popconfirm title="确认删除该评价？" @confirm="onDelete(record)">
              <a class="danger">删除</a>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
      <div v-if="!loading && testimonials.length === 0" class="empty-tip">暂无评价，请点击右上角新增</div>
    </a-card>

    <a-modal
      v-model:open="modalOpen"
      :title="editing ? '编辑评价' : '新增评价'"
      :confirm-loading="submitting"
      @ok="onSubmit"
      :width="560"
    >
      <a-form ref="formRef" layout="vertical" :model="form" :rules="rules">
        <a-form-item label="头像" name="avatarUrl">
          <div class="avatar-uploader">
            <img v-if="form.avatarUrl" :src="form.avatarUrl" class="avatar-preview-large" alt="avatar" />
            <div v-else class="avatar-placeholder">{{ form.name ? form.name[0] : 'U' }}</div>
            <a-upload
              accept="image/png,image/jpeg,image/jpg"
              :show-upload-list="false"
              :custom-request="handleAvatarUpload"
              :before-upload="beforeAvatarUpload"
            >
              <a-button :loading="uploading" class="upload-btn">
                <template #icon><UploadOutlined /></template>
                {{ form.avatarUrl ? '更换头像' : '上传头像' }}
              </a-button>
            </a-upload>
            <a-input v-model:value="form.avatarUrl" placeholder="或填写图片 URL" class="avatar-url-input" />
          </div>
        </a-form-item>
        <a-form-item label="姓名" name="name">
          <a-input v-model:value="form.name" placeholder="如：张明" />
        </a-form-item>
        <a-form-item label="身份/职位" name="title">
          <a-input v-model:value="form.title" placeholder="如：计算机专业学生" />
        </a-form-item>
        <a-form-item label="星级" name="starRating">
          <a-rate v-model:value="form.starRating" :max="5" />
        </a-form-item>
        <a-form-item label="评价内容" name="reviewText">
          <a-textarea v-model:value="form.reviewText" :rows="4" placeholder="请输入评价内容" />
        </a-form-item>
        <a-form-item label="排序" name="sort">
          <a-input-number v-model:value="form.sort" :min="0" style="width: 100%" />
          <div class="form-hint">数字越小越靠前</div>
        </a-form-item>
        <a-form-item label="启用状态" name="isEnabled">
          <a-switch v-model:checked="form.isEnabled" :checkedValue="1" :unCheckedValue="0" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { UploadOutlined } from '@ant-design/icons-vue'
import {
  listTestimonials,
  createTestimonial,
  updateTestimonial,
  deleteTestimonial,
  updateTestimonialStatus,
  uploadTestimonialAvatar
} from '@/api/homeTestimonial.js'

const testimonials = ref([])
const loading = ref(false)
const modalOpen = ref(false)
const submitting = ref(false)
const uploading = ref(false)
const editing = ref(null)
const formRef = ref()

const form = reactive({
  avatarUrl: '',
  name: '',
  title: '',
  starRating: 5,
  reviewText: '',
  sort: 0,
  isEnabled: 1
})

const rules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  starRating: [{ required: true, message: '请选择星级', trigger: 'change', type: 'number' }],
  reviewText: [{ required: true, message: '请输入评价内容', trigger: 'blur' }]
}

const columns = [
  { title: '头像', key: 'avatar', width: 80 },
  { title: '姓名', dataIndex: 'name', key: 'name', width: 120 },
  { title: '身份/职位', dataIndex: 'title', key: 'title', ellipsis: true },
  { title: '星级', key: 'starRating', width: 160 },
  { title: '评价内容', key: 'reviewText', ellipsis: true },
  { title: '排序', dataIndex: 'sort', key: 'sort', width: 80 },
  { title: '启用', key: 'isEnabled', width: 80 },
  { title: '操作', key: 'action', width: 120 }
]

async function load() {
  loading.value = true
  try {
    testimonials.value = await listTestimonials()
  } catch (e) {
    message.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.avatarUrl = ''
  form.name = ''
  form.title = ''
  form.starRating = 5
  form.reviewText = ''
  form.sort = 0
  form.isEnabled = 1
}

function onCreate() {
  editing.value = null
  resetForm()
  modalOpen.value = true
}

function onEdit(record) {
  editing.value = record
  form.avatarUrl = record.avatarUrl || ''
  form.name = record.name
  form.title = record.title || ''
  form.starRating = record.starRating
  form.reviewText = record.reviewText
  form.sort = record.sort
  form.isEnabled = record.isEnabled
  modalOpen.value = true
}

async function onDelete(record) {
  try {
    await deleteTestimonial(record.id)
    message.success('已删除')
    await load()
  } catch (e) {
    message.error(e?.message || '删除失败')
  }
}

async function onToggleStatus(record, checked) {
  try {
    await updateTestimonialStatus(record.id, { isEnabled: checked ? 1 : 0 })
    record.isEnabled = checked ? 1 : 0
    message.success('已更新')
  } catch (e) {
    message.error(e?.message || '更新失败')
  }
}

function beforeAvatarUpload(file) {
  const isJpgOrPng = file.type === 'image/jpeg' || file.type === 'image/png' || file.type === 'image/jpg'
  if (!isJpgOrPng) {
    message.error('仅支持 JPG/PNG 格式')
    return false
  }
  if (file.size > 5 * 1024 * 1024) {
    message.error('图片大小不能超过 5MB')
    return false
  }
  return true
}

async function handleAvatarUpload({ file }) {
  uploading.value = true
  try {
    const url = await uploadTestimonialAvatar(file)
    form.avatarUrl = url
    message.success('上传成功')
  } catch (e) {
    message.error(e?.message || '上传失败')
  } finally {
    uploading.value = false
  }
}

async function onSubmit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  submitting.value = true
  const payload = {
    avatarUrl: form.avatarUrl,
    name: form.name,
    title: form.title,
    starRating: form.starRating,
    reviewText: form.reviewText,
    sort: form.sort,
    isEnabled: form.isEnabled
  }
  try {
    if (editing.value) {
      await updateTestimonial(editing.value.id, payload)
    } else {
      await createTestimonial(payload)
    }
    message.success('已保存')
    modalOpen.value = false
    await load()
  } catch (e) {
    message.error(e?.message || '保存失败')
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.home-testimonial-view { padding: 0; }
.danger { color: #ff4d4f; }
.empty-tip { text-align: center; color: #8c8c8c; padding: 24px 0; }
.form-hint { font-size: 12px; color: #8c8c8c; margin-top: 4px; }
.avatar-preview {
  width: 48px;
  height: 48px;
  object-fit: cover;
  border-radius: 50%;
  background: #f5f5f5;
}
.avatar-fallback {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: #f0f0f0;
  color: #595959;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 600;
}
.avatar-uploader {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.avatar-preview-large {
  width: 64px;
  height: 64px;
  object-fit: cover;
  border-radius: 50%;
  background: #f5f5f5;
}
.avatar-placeholder {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: #f0f0f0;
  color: #595959;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  font-weight: 600;
}
.avatar-url-input {
  flex: 1;
  min-width: 200px;
}
.review-text {
  display: inline-block;
  max-width: 240px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
