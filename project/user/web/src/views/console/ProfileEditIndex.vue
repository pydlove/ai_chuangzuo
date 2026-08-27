<template>
  <div class="profile-edit-page">
    <div class="profile-avatar-item" @click="triggerAvatarUpload">
      <div class="profile-avatar-preview">
        <img
          v-if="profileEditForm.avatarUrl"
          :src="profileEditForm.avatarUrl"
          alt="avatar"
        />
        <UserOutlined v-else />
      </div>
      <span class="profile-avatar-text">{{ profileEditForm.avatarUrl ? '更换头像' : '上传头像' }}</span>
    </div>

    <div class="profile-item">
      <label class="profile-label">平台ID</label>
      <div class="profile-userid-row">
        <input
          v-model="profileEditForm.userId"
          type="text"
          class="profile-input profile-userid-input"
          readonly
        />
        <button class="profile-copy-btn" @click="copyUserId">
          <CopyOutlined />
          <span>复制</span>
        </button>
      </div>
    </div>

    <div class="profile-item">
      <label class="profile-label">昵称 <span class="required">*</span></label>
      <input
        v-model="profileEditForm.nickname"
        type="text"
        class="profile-input"
        placeholder="请输入昵称"
        maxlength="20"
      />
    </div>

    <div class="profile-item">
      <div class="profile-label-row">
        <label class="profile-label">简介</label>
        <span class="profile-bio-count">{{ profileEditForm.bio.length }}/50</span>
      </div>
      <textarea
        v-model="profileEditForm.bio"
        class="profile-textarea"
        placeholder="写点什么介绍自己"
        maxlength="50"
        rows="3"
      />
    </div>

    <div class="profile-item">
      <label class="profile-label">性别</label>
      <div class="profile-gender-row">
        <button
          v-for="opt in genderOptions"
          :key="opt.value"
          class="profile-gender-btn"
          :class="{ active: profileEditForm.gender === opt.value }"
          @click="profileEditForm.gender = opt.value"
        >
          {{ opt.label }}
        </button>
      </div>
    </div>

    <div class="profile-item">
      <label class="profile-label">生日</label>
      <a-date-picker
        v-model:value="profileEditForm.birthday"
        format="YYYY-MM-DD"
        value-format="YYYY-MM-DD"
        placeholder="请选择生日"
        class="profile-birthday-picker"
        :allow-clear="true"
      />
    </div>

    <div class="profile-item">
      <label class="profile-label">所在地</label>
      <input
        v-model="profileEditForm.location"
        type="text"
        class="profile-input"
        placeholder="请输入所在地"
        maxlength="128"
      />
    </div>

    <div class="profile-item">
      <label class="profile-label">职业</label>
      <input
        v-model="profileEditForm.occupation"
        type="text"
        class="profile-input"
        placeholder="请输入职业"
        maxlength="128"
      />
    </div>

    <button class="profile-submit" @click="handleProfileSubmit">保存</button>

    <input
      ref="avatarInput"
      type="file"
      accept="image/jpeg,image/png,image/jpg"
      style="display: none"
      @change="onAvatarFileChange"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { UserOutlined, CopyOutlined } from '@ant-design/icons-vue'
import { useUserProfile } from '@/composables/useUserProfile.js'
import { useCopy } from '@/composables/useCopy.js'

const router = useRouter()
const { profile, loadProfile, saveProfile, saveAvatar } = useUserProfile()

const avatarInput = ref(null)

const genderOptions = [
  { value: 0, label: '保密' },
  { value: 1, label: '男' },
  { value: 2, label: '女' }
]

const profileEditForm = reactive({
  userId: '',
  nickname: '',
  avatarUrl: '',
  bio: '',
  gender: 0,
  birthday: '',
  location: '',
  occupation: ''
})

onMounted(async () => {
  if (!profile.value) {
    await loadProfile()
  }
  const p = profile.value
  if (p) {
    profileEditForm.userId = p.userId ?? ''
    profileEditForm.nickname = p.nickname ?? ''
    profileEditForm.avatarUrl = p.avatarUrl ?? ''
    profileEditForm.bio = p.bio ?? ''
    profileEditForm.gender = p.gender ?? 0
    profileEditForm.birthday = p.birthday ?? ''
    profileEditForm.location = p.location ?? ''
    profileEditForm.occupation = p.occupation ?? ''
  }
})

const handleProfileSubmit = async () => {
  const trimmed = profileEditForm.nickname.trim()
  if (!trimmed) {
    message.warning('昵称不能为空')
    return
  }
  if (trimmed.length > 20) {
    message.warning('昵称长度不能超过 20 个字符')
    return
  }
  if (profileEditForm.bio.trim().length > 50) {
    message.warning('简介长度不能超过 50 个字符')
    return
  }
  try {
    await saveProfile({
      nickname: trimmed,
      bio: profileEditForm.bio.trim(),
      gender: profileEditForm.gender,
      birthday: profileEditForm.birthday || undefined,
      location: profileEditForm.location.trim(),
      occupation: profileEditForm.occupation.trim()
    })
    router.back()
  } catch {
    // composable 已 message.error
  }
}

const { copy } = useCopy({
  successText: '平台ID已复制',
  errorText: '复制失败'
})
const copyUserId = () => copy(profileEditForm.userId)

const triggerAvatarUpload = () => {
  avatarInput.value?.click()
}

const onAvatarFileChange = async (e) => {
  const file = e.target.files?.[0]
  if (!file) return
  e.target.value = ''
  try {
    const avatarUrl = await saveAvatar(file)
    profileEditForm.avatarUrl = avatarUrl
  } catch {
    // composable 已 message.error
  }
}
</script>

<style scoped>
.profile-edit-page {
  width: 100%;
  max-width: 1280px;
  margin: 0 auto;
  padding: 32px 32px 24px;
  box-sizing: border-box;
}

.profile-avatar-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  margin-bottom: 24px;
  cursor: pointer;
}

.profile-avatar-preview {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  color: #8c8c8c;
  font-size: 28px;
}

.profile-avatar-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-avatar-text {
  font-size: 13px;
  color: #ff2442;
}

.profile-item {
  margin-bottom: 18px;
}

.profile-userid-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.profile-userid-input {
  flex: 1;
  background: #f5f5f5 !important;
  color: #595959 !important;
  cursor: default;
}

.profile-copy-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 0 14px;
  height: 40px;
  border: 1px solid #ff2442;
  background: #fff;
  border-radius: 8px;
  color: #ff2442;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
}

.profile-copy-btn:hover {
  background: #ff2442;
  color: #fff;
}

.profile-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #1a1a1a;
  margin-bottom: 8px;
}

.profile-label .required {
  color: #ff2442;
}

.profile-label-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.profile-label-row .profile-label {
  margin-bottom: 0;
}

.profile-bio-count {
  font-size: 12px;
  color: #999;
}

.profile-input,
.profile-textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 14px;
  color: #1a1a1a;
  background: #fff;
  box-sizing: border-box;
  outline: none;
  transition: border-color 0.2s;
}

.profile-input:focus,
.profile-textarea:focus {
  border-color: #ff2442;
  box-shadow: 0 0 0 2px rgba(255, 36, 66, 0.15);
}

.profile-birthday-picker {
  width: 100%;
}

.profile-birthday-picker.ant-picker {
  width: 100%;
  height: 44px;
  border-radius: 8px;
  border: 1px solid #d9d9d9;
  padding: 0 12px;
}

.profile-birthday-picker.ant-picker-focused,
.profile-birthday-picker.ant-picker:hover {
  border-color: #ff2442;
  box-shadow: 0 0 0 2px rgba(255, 36, 66, 0.15);
}

.profile-birthday-picker :deep(.ant-picker-suffix) {
  color: #ff2442;
}

:global(.ant-picker-dropdown [class*="ant-picker-cell-selected"] [class*="ant-picker-cell-inner"]) {
  background: #ff2442 !important;
}

:global(.ant-picker-dropdown [class*="ant-picker-cell-today"] [class*="ant-picker-cell-inner"]::before) {
  border-color: #ff2442 !important;
}

:global(.ant-picker-dropdown [class*="ant-picker-today-btn"]) {
  color: #ff2442 !important;
}

.profile-textarea {
  resize: vertical;
}

.profile-gender-row {
  display: flex;
  gap: 10px;
}

.profile-gender-btn {
  padding: 8px 20px;
  border: 1px solid #d9d9d9;
  background: #fff;
  border-radius: 20px;
  font-size: 14px;
  color: #595959;
  cursor: pointer;
  transition: all 0.2s;
}

.profile-gender-btn.active {
  background: #ff2442;
  border-color: #ff2442;
  color: #fff;
}

.profile-submit {
  width: 100%;
  padding: 12px;
  background: #ff2442;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.profile-submit:hover {
  background: #e61e3a;
}

/* 移动端 */
@media (max-width: 768px) {
  .profile-edit-page {
    padding: 20px 12px 16px;
  }
}

/* 暗色主题 */
body[data-theme="dark"] .profile-label {
  color: #e0e0e0;
}

body[data-theme="dark"] .profile-input,
body[data-theme="dark"] .profile-textarea {
  background: #2a2a2a;
  border-color: #434343;
  color: #e0e0e0;
}

body[data-theme="dark"] .profile-birthday-picker :deep(.ant-picker) {
  background: #2a2a2a;
  border-color: #434343;
  color: #e0e0e0;
}

body[data-theme="dark"] .profile-birthday-picker :deep(.ant-picker-input > input) {
  color: #e0e0e0;
}

body[data-theme="dark"] .profile-birthday-picker :deep(.ant-picker-suffix) {
  color: #ff2442;
}

body[data-theme="dark"] .profile-input:focus,
body[data-theme="dark"] .profile-textarea:focus {
  border-color: #ff2442;
  box-shadow: 0 0 0 2px rgba(255, 36, 66, 0.15);
}

body[data-theme="dark"] .profile-gender-btn {
  background: #2a2a2a;
  border-color: #434343;
  color: #a6a6a6;
}

body[data-theme="dark"] .profile-gender-btn.active {
  background: #ff2442;
  border-color: #ff2442;
  color: #fff;
}

body[data-theme="dark"] .profile-avatar-preview {
  background: #2a2a2a;
  color: #a6a6a6;
}
</style>
