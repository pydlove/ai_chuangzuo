<template>
  <div class="user-list">
    <a-card :bordered="false" class="user-list-card">
      <div class="user-list-header">
        <h3 class="user-list-title">注册用户管理</h3>
        <p class="user-list-desc">查看与管理平台注册用户</p>
      </div>

      <!-- 工具栏 -->
      <div class="user-list-toolbar">
        <a-input
          v-model:value="keyword"
          placeholder="账号或邮箱"
          allow-clear
          style="width: 280px"
          @press-enter="handleSearch"
        />
        <a-button type="primary" @click="handleSearch">查询</a-button>
        <a-button @click="handleReset">重置</a-button>
        <a-button @click="fetchUsers">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
        <a-button type="primary" @click="openCreateModal">
          <template #icon><PlusOutlined /></template>
          手动创建
        </a-button>
      </div>

      <!-- 表格 -->
      <a-table
        :columns="columns"
        :data-source="users"
        :loading="loading"
        :pagination="false"
        row-key="id"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 'enabled' ? 'green' : 'red'">
              {{ record.status === 'enabled' ? '启用' : '禁用' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'userType'">
            <a-tag :color="record.userType === 'robot' ? 'orange' : 'blue'">
              {{ record.userType === 'robot' ? '机器人' : '真实用户' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'membershipExpireAt'">
            <span v-if="record.membershipExpireAt">{{ record.membershipExpireAt }}</span>
            <span v-else style="color: #8c8c8c">非会员</span>
          </template>
          <template v-else-if="column.key === 'membershipPlan'">
            <span v-if="record.membershipPlan">{{ planLabel(record.membershipPlan) }}</span>
            <span v-else style="color: #8c8c8c">—</span>
          </template>
          <template v-else-if="column.key === 'lastLoginAt'">
            {{ record.lastLoginAt || '—' }}
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-space>
              <a-popconfirm
                :title="record.status === 'enabled' ? '确定禁用该用户？' : '确定启用该用户？'"
                ok-text="确认"
                cancel-text="取消"
                @confirm="handleStatusChange(record)"
              >
                <a-button type="link" size="small">
                  {{ record.status === 'enabled' ? '禁用' : '启用' }}
                </a-button>
              </a-popconfirm>
              <a-button type="link" size="small" @click="openEditModal(record)">
                编辑
              </a-button>
              <a-button type="link" size="small" @click="openResetPasswordModal(record)">
                重置密码
              </a-button>
              <a-button type="link" size="small" @click="openDetailDrawer(record)">
                查看详情
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>

      <!-- 分页 -->
      <div class="user-list-pagination">
        <a-pagination
          :current="page"
          :page-size="pageSize"
          :total="total"
          :page-size-options="['10', '20', '50']"
          show-size-changer
          show-total
          @change="handlePageChange"
          @show-size-change="handlePageChange"
        />
      </div>
    </a-card>

    <!-- 重置密码弹框 -->
    <a-modal
      v-model:open="resetPasswordVisible"
      title="重置用户密码"
      ok-text="确认重置"
      cancel-text="取消"
      @ok="confirmResetPassword"
    >
      <p>账号：<strong>{{ resetPasswordTarget?.account }}</strong></p>
      <p style="color: #8c8c8c; margin-top: 12px">
        重置后密码将设为 <code>adc123456</code>，请通知用户及时修改。
      </p>
    </a-modal>

    <!-- 编辑用户弹框 -->
    <a-modal
      v-model:open="editModalVisible"
      title="编辑用户"
      ok-text="保存"
      cancel-text="取消"
      :confirm-loading="editLoading"
      @ok="submitEditForm"
      @cancel="closeEditModal"
    >
      <a-form
        ref="editFormRef"
        :model="editForm"
        :rules="editRules"
        layout="vertical"
      >
        <a-form-item label="邮箱" name="email">
          <a-input v-model:value="editForm.email" placeholder="请输入邮箱" />
        </a-form-item>
        <a-form-item label="昵称" name="nickname">
          <a-input v-model:value="editForm.nickname" placeholder="请输入昵称" />
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-radio-group v-model:value="editForm.status">
            <a-radio value="enabled">启用</a-radio>
            <a-radio value="disabled">禁用</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="用户类型" name="userType">
          <a-radio-group v-model:value="editForm.userType">
            <a-radio :value="1">真实用户</a-radio>
            <a-radio :value="0">机器人</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="会员套餐" name="membershipPlan">
          <a-select v-model:value="editForm.membershipPlan" allow-clear placeholder="选择会员套餐（清空=无套餐）" style="width: 100%">
            <a-select-option value="monthly">月度会员</a-select-option>
            <a-select-option value="quarterly">季度会员</a-select-option>
            <a-select-option value="yearly">年度会员</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="会员到期" name="expireDate">
          <a-date-picker
            v-model:value="editForm.expireDate"
            value-format="YYYY-MM-DD"
            style="width: 100%"
            placeholder="选择到期日（清空=非会员）"
            allow-clear
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 查看详情抽屉 -->
    <a-drawer
      v-model:open="detailVisible"
      title="用户详情"
      :width="480"
      placement="right"
    >
      <a-descriptions v-if="detailUser" :column="1" bordered>
        <a-descriptions-item label="ID">{{ detailUser.id }}</a-descriptions-item>
        <a-descriptions-item label="账号">{{ detailUser.account }}</a-descriptions-item>
        <a-descriptions-item label="邮箱">{{ detailUser.email }}</a-descriptions-item>
        <a-descriptions-item label="昵称">{{ detailUser.nickname }}</a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="detailUser.status === 'enabled' ? 'green' : 'red'">
            {{ detailUser.status === 'enabled' ? '启用' : '禁用' }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="类型">
          <a-tag :color="detailUser.userType === 'robot' ? 'orange' : 'blue'">
            {{ detailUser.userType === 'robot' ? '机器人' : '真实用户' }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="邀请码">{{ detailUser.inviteCode }}</a-descriptions-item>
        <a-descriptions-item label="会员套餐">
          <span v-if="detailUser.membershipPlan">{{ planLabel(detailUser.membershipPlan) }}</span>
          <span v-else>—</span>
        </a-descriptions-item>
        <a-descriptions-item label="会员到期">
          <span v-if="detailUser.membershipExpireAt">{{ detailUser.membershipExpireAt }}</span>
          <span v-else>非会员</span>
        </a-descriptions-item>
        <a-descriptions-item label="注册时间">{{ detailUser.createdAt }}</a-descriptions-item>
        <a-descriptions-item label="最后登录">{{ detailUser.lastLoginAt || '—' }}</a-descriptions-item>
      </a-descriptions>
      <template #footer>
        <a-button @click="detailVisible = false">关闭</a-button>
      </template>
    </a-drawer>

    <!-- 手动创建用户弹框 -->
    <a-modal
      v-model:open="createModalVisible"
      title="手动创建用户"
      ok-text="创建"
      cancel-text="取消"
      :confirm-loading="createLoading"
      @ok="submitCreateForm"
      @cancel="closeCreateModal"
    >
      <a-form
        ref="createFormRef"
        :model="createForm"
        :rules="createRules"
        layout="vertical"
      >
        <a-form-item label="邮箱" name="email">
          <a-input v-model:value="createForm.email" placeholder="请输入邮箱" />
        </a-form-item>
        <a-form-item label="昵称" name="nickname">
          <a-input v-model:value="createForm.nickname" placeholder="请输入昵称" />
        </a-form-item>
        <a-form-item label="密码" name="password">
          <a-input-password
            v-model:value="createForm.password"
            placeholder="留空则使用默认密码 adc123456"
          />
        </a-form-item>
        <a-form-item label="用户类型" name="userType">
          <a-radio-group v-model:value="createForm.userType">
            <a-radio :value="1">真实用户</a-radio>
            <a-radio :value="0">机器人</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { useUserManagement } from '@/composables/useUserManagement.js'
import { getUser, updateUser } from '@/api/user.js'

const {
  users,
  total,
  loading,
  page,
  pageSize,
  keyword,
  fetchUsers,
  handleSearch,
  handleReset,
  handlePageChange,
  handleStatusChange,
  handleResetPassword,
  handleCreateUser
} = useUserManagement()

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '账号', dataIndex: 'account', key: 'account', width: 140 },
  { title: '邮箱', dataIndex: 'email', key: 'email', width: 200 },
  { title: '昵称', dataIndex: 'nickname', key: 'nickname', width: 120 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '类型', dataIndex: 'userType', key: 'userType', width: 100 },
  { title: '会员套餐', dataIndex: 'membershipPlan', key: 'membershipPlan', width: 100 },
  { title: '会员到期', dataIndex: 'membershipExpireAt', key: 'membershipExpireAt', width: 170 },
  { title: '注册时间', dataIndex: 'createdAt', key: 'createdAt', width: 170 },
  { title: '最后登录', key: 'lastLoginAt', width: 170 },
  { title: '操作', key: 'actions', width: 320 }
]

const resetPasswordVisible = ref(false)
const resetPasswordTarget = ref(null)
const detailVisible = ref(false)
const detailUser = ref(null)

const editModalVisible = ref(false)
const editFormRef = ref()
const editForm = reactive({
  id: null,
  email: '',
  nickname: '',
  status: 'enabled',
  userType: 1,
  membershipPlan: null,
  expireDate: null
})
const editLoading = ref(false)

const editRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 1, max: 64, message: '昵称长度 1-64 字符', trigger: 'blur' }
  ],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
  userType: [{ required: true, message: '请选择用户类型', trigger: 'change' }]
}

const createModalVisible = ref(false)
const createFormRef = ref()
const createForm = reactive({
  email: '',
  nickname: '',
  password: '',
  userType: 1
})
const createLoading = ref(false)

const createRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 1, max: 64, message: '昵称长度 1-64 字符', trigger: 'blur' }
  ],
  password: [
    { min: 6, max: 32, message: '密码长度 6-32 字符，留空则使用默认密码', trigger: 'blur' }
  ],
  userType: [
    { required: true, message: '请选择用户类型', trigger: 'change' }
  ]
}

const planLabel = (code) => {
  const map = { monthly: '月度会员', quarterly: '季度会员', yearly: '年度会员' }
  return map[code] || code
}

const openEditModal = async (user) => {
  editLoading.value = true
  try {
    const detail = await getUser(user.id)
    editForm.id = detail.id
    editForm.email = detail.email
    editForm.nickname = detail.nickname
    editForm.status = detail.status
    editForm.userType = detail.userType === 'robot' ? 0 : 1
    editForm.membershipPlan = detail.membershipPlan || null
    editForm.expireDate = detail.membershipExpireAt ? detail.membershipExpireAt.substring(0, 10) : null
    editModalVisible.value = true
  } finally {
    editLoading.value = false
  }
}

const closeEditModal = () => {
  editModalVisible.value = false
  editFormRef.value?.resetFields()
}

const submitEditForm = () => {
  editFormRef.value?.validate().then(async () => {
    editLoading.value = true
    try {
      await updateUser(editForm.id, {
        email: editForm.email.trim(),
        nickname: editForm.nickname.trim(),
        status: editForm.status,
        userType: editForm.userType,
        membershipPlan: editForm.membershipPlan || null,
        expireDate: editForm.expireDate || null
      })
      message.success('用户信息已更新')
      closeEditModal()
      fetchUsers()
    } finally {
      editLoading.value = false
    }
  })
}

const openCreateModal = () => {
  createForm.email = ''
  createForm.nickname = ''
  createForm.password = ''
  createForm.userType = 1
  createModalVisible.value = true
}

const closeCreateModal = () => {
  createModalVisible.value = false
  createFormRef.value?.resetFields()
}

const submitCreateForm = () => {
  createFormRef.value?.validate().then(async () => {
    createLoading.value = true
    try {
      await handleCreateUser({
        email: createForm.email.trim(),
        nickname: createForm.nickname.trim(),
        password: createForm.password,
        userType: createForm.userType
      })
      closeCreateModal()
    } finally {
      createLoading.value = false
    }
  })
}

const openResetPasswordModal = (user) => {
  resetPasswordTarget.value = user
  resetPasswordVisible.value = true
}

const confirmResetPassword = async () => {
  if (!resetPasswordTarget.value) return
  await handleResetPassword(resetPasswordTarget.value)
  resetPasswordVisible.value = false
}

const openDetailDrawer = async (user) => {
  try {
    detailUser.value = await getUser(user.id)
    detailVisible.value = true
  } catch (error) {
    detailUser.value = user
    detailVisible.value = true
  }
}

onMounted(() => {
  fetchUsers()
})
</script>

<style scoped>
.user-list-card {
  border-radius: 8px;
}

.user-list-header {
  margin-bottom: 16px;
}

.user-list-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 4px 0;
}

.user-list-desc {
  font-size: 13px;
  color: #8c8c8c;
  margin: 0;
}

.user-list-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  align-items: center;
}

.user-list-pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
