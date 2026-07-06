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
        <a-descriptions-item label="邀请码">{{ detailUser.inviteCode }}</a-descriptions-item>
        <a-descriptions-item label="注册时间">{{ detailUser.createdAt }}</a-descriptions-item>
        <a-descriptions-item label="最后登录">{{ detailUser.lastLoginAt || '—' }}</a-descriptions-item>
      </a-descriptions>
      <template #footer>
        <a-button @click="detailVisible = false">关闭</a-button>
      </template>
    </a-drawer>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import { useUserManagement } from '@/composables/useUserManagement.js'
import { getUser } from '@/api/user.js'

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
  handleResetPassword
} = useUserManagement()

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '账号', dataIndex: 'account', key: 'account', width: 140 },
  { title: '邮箱', dataIndex: 'email', key: 'email', width: 200 },
  { title: '昵称', dataIndex: 'nickname', key: 'nickname', width: 120 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '注册时间', dataIndex: 'createdAt', key: 'createdAt', width: 170 },
  { title: '最后登录', key: 'lastLoginAt', width: 170 },
  { title: '操作', key: 'actions', width: 280 }
]

const resetPasswordVisible = ref(false)
const resetPasswordTarget = ref(null)
const detailVisible = ref(false)
const detailUser = ref(null)

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
    // 失败时使用列表中的数据兜底
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
