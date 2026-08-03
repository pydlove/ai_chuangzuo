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
          style="width: 200px"
          @press-enter="handleSearch"
        />
        <a-input
          v-model:value="inviteCode"
          placeholder="邀请码"
          allow-clear
          style="width: 160px"
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
        <a-button @click="downloadTemplate">
          <template #icon><DownloadOutlined /></template>
          下载导入模板
        </a-button>
        <a-upload
          accept=".xlsx"
          :show-upload-list="false"
          :before-upload="beforeImport"
          :custom-request="handleImport"
        >
          <a-button :loading="importing">
            <template #icon><UploadOutlined /></template>
            导入用户
          </a-button>
        </a-upload>
      </div>

      <!-- 表格 -->
      <a-table
        :columns="columns"
        :data-source="users"
        :loading="loading"
        :pagination="false"
        :scroll="{ x: 'max-content' }"
        row-key="id"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'email'">
            <span>{{ record.email }}</span>
            <a-button type="link" size="small" class="email-copy-btn" @click="copyEmail(record.email)">
              <template #icon><CopyOutlined /></template>
            </a-button>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="record.status === 'enabled' ? 'green' : 'red'">
              {{ record.status === 'enabled' ? '启用' : '禁用' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'userType'">
            <a-tag :color="record.userType === 'robot' ? 'orange' : 'blue'">
              {{ record.userType === 'robot' ? '机器人' : '真实用户' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'inviteCode'">
            <span v-if="record.inviteCode">{{ record.inviteCode }}</span>
            <span v-else style="color: #8c8c8c">—</span>
            <a-button
              v-if="record.inviteCode"
              type="link"
              size="small"
              class="invite-code-copy-btn"
              @click="copyInviteCode(record.inviteCode)"
            >
              <template #icon><CopyOutlined /></template>
            </a-button>
          </template>
          <template v-else-if="column.key === 'inviter'">
            <span v-if="record.inviterEmail">
              {{ record.inviterNickname || record.inviterEmail }}
              <span style="color: #8c8c8c; font-size: 12px">({{ record.inviterEmail }})</span>
            </span>
            <span v-else style="color: #8c8c8c">—</span>
          </template>
          <template v-else-if="column.key === 'invitedCount'">
            <a-tag :color="record.invitedCount > 0 ? 'green' : 'default'">{{ record.invitedCount || 0 }}</a-tag>
          </template>
          <template v-else-if="column.key === 'membershipExpireAt'">
            <span v-if="record.membershipExpireAt">{{ formatDateTime(record.membershipExpireAt) }}</span>
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
            <a-dropdown>
              <a-button size="small">
                设置
                <DownOutlined />
              </a-button>
              <template #overlay>
                <a-menu>
                  <a-menu-item @click="confirmStatusChange(record)">
                    {{ record.status === 'enabled' ? '禁用' : '启用' }}
                  </a-menu-item>
                  <a-menu-item @click="openEditModal(record)">编辑</a-menu-item>
                  <a-menu-item @click="openResetPasswordModal(record)">重置密码</a-menu-item>
                  <a-menu-item @click="openInviteModal(record)">邀请关系</a-menu-item>
                  <a-menu-item @click="openDetailDrawer(record)">查看详情</a-menu-item>
                  <a-menu-item danger @click="confirmDelete(record)">删除</a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
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
        重置后密码将设为 <code>Aichuangzuo@123</code>，请通知用户及时修改。
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

    <!-- 查看详情弹框 -->
    <a-modal
      v-model:open="detailVisible"
      title="用户详情"
      width="100%"
      class="user-detail-modal"
      :footer="null"
      @cancel="closeDetailModal"
    >
      <a-tabs v-model:activeKey="detailTabKey" type="card">
        <a-tab-pane key="basic" tab="基础信息">
          <a-descriptions v-if="detailUser" :column="1" bordered>
            <a-descriptions-item label="ID">{{ detailUser.id }}</a-descriptions-item>
            <a-descriptions-item label="邮箱/账号">{{ detailUser.email }}</a-descriptions-item>
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
              <span v-if="detailUser.membershipExpireAt">{{ formatDateTime(detailUser.membershipExpireAt) }}</span>
              <span v-else>非会员</span>
            </a-descriptions-item>
            <a-descriptions-item label="注册时间">{{ formatDateTime(detailUser.createdAt) }}</a-descriptions-item>
            <a-descriptions-item label="最后登录">{{ formatDateTime(detailUser.lastLoginAt) || '—' }}</a-descriptions-item>
          </a-descriptions>
        </a-tab-pane>

        <a-tab-pane key="skills" tab="我的提示词">
          <a-spin :spinning="skillsLoading">
            <div class="skill-tab-toolbar">
              <a-space>
                <span>释放额度数量：</span>
                <a-input-number
                  v-model:value="releaseCount"
                  :min="1"
                  :max="100"
                  style="width: 120px"
                  placeholder="数量"
                />
                <a-button type="primary" :loading="releaseLoading" @click="handleReleaseCustomQuota">
                  释放额度
                </a-button>
              </a-space>
            </div>
            <a-table
              :columns="skillColumns"
              :data-source="userSkills"
              :pagination="false"
              size="small"
              row-key="bizNo"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'auditStatus'">
                  <a-tag :color="auditStatusColor(record.auditStatus)">
                    {{ auditStatusLabel(record.auditStatus) }}
                  </a-tag>
                </template>
                <template v-else-if="column.key === 'prompt'">
                  <span :title="record.prompt">{{ record.prompt || '—' }}</span>
                </template>
                <template v-else-if="column.key === 'scope'">
                  {{ record.scope || '—' }}
                </template>
                <template v-else-if="column.key === 'createdAt'">
                  {{ formatDateTime(record.createdAt) }}
                </template>
              </template>
            </a-table>
          </a-spin>
        </a-tab-pane>

        <a-tab-pane key="learned" tab="学习的提示词">
          <a-spin :spinning="learnedLoading">
            <a-table
              :columns="learnedColumns"
              :data-source="learnedMonths"
              :pagination="false"
              size="small"
              row-key="period"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'skillCount'">
                  {{ record.skillCount || 0 }}
                </template>
                <template v-else-if="column.key === 'period'">
                  <span>
                    {{ record.period }}
                    <a-tag v-if="record.period === currentPeriod" color="blue" style="margin-left: 8px">当月</a-tag>
                  </span>
                </template>
                <template v-else-if="column.key === 'action'">
                  <a-button
                    type="link"
                    size="small"
                    :loading="resettingPeriod === record.period"
                    @click="handleResetLearnedQuota(record)"
                  >
                    重置当月额度
                  </a-button>
                </template>
              </template>
            </a-table>
          </a-spin>
        </a-tab-pane>
      </a-tabs>
    </a-modal>

    <!-- 邀请关系弹框 -->
    <a-modal
      v-model:open="inviteModalVisible"
      title="邀请关系"
      width="720"
      :footer="null"
      @cancel="closeInviteModal"
    >
      <a-spin :spinning="inviteLoading">
        <div v-if="inviteDetail" class="invite-modal-content">
          <a-descriptions :column="2" bordered size="small">
            <a-descriptions-item label="当前用户">
              {{ inviteTarget?.nickname || inviteTarget?.email }}
              <span style="color: #8c8c8c">({{ inviteTarget?.email }})</span>
            </a-descriptions-item>
            <a-descriptions-item label="邀请码">{{ inviteDetail.inviteCode || '—' }}</a-descriptions-item>
            <a-descriptions-item label="邀请人" :span="2">
              <div v-if="inviteDetail.inviter">
                <div>
                  {{ inviteDetail.inviter.nickname || inviteDetail.inviter.email }}
                  <span style="color: #8c8c8c">({{ inviteDetail.inviter.email }})</span>
                </div>
                <div style="color: #8c8c8c; font-size: 12px; margin-top: 4px">
                  绑定时间：{{ formatDateTime(inviteDetail.inviter.createdAt) }}
                </div>
              </div>
              <span v-else style="color: #8c8c8c">—</span>
            </a-descriptions-item>
          </a-descriptions>

          <div class="invite-section-title">邀请列表（{{ inviteDetail.invitees.length }} 人）</div>
          <a-table
            :columns="inviteColumns"
            :data-source="inviteDetail.invitees"
            :pagination="false"
            size="small"
            row-key="id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'email'">
                {{ record.email }}
              </template>
              <template v-else-if="column.key === 'nickname'">
                {{ record.nickname || '—' }}
              </template>
              <template v-else-if="column.key === 'createdAt'">
                {{ formatDateTime(record.createdAt) || '—' }}
              </template>
            </template>
          </a-table>
        </div>
      </a-spin>
    </a-modal>

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
            placeholder="留空则使用默认密码 Aichuangzuo@123"
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

    <!-- 导入结果弹框 -->
    <a-modal
      v-model:open="importResultVisible"
      :title="importResult?.success ? '导入成功' : '导入失败'"
      :footer="null"
      @cancel="closeImportResultModal"
    >
      <div v-if="importResult" class="import-result">
        <p>
          共解析 <strong>{{ importResult.totalRows }}</strong> 行，成功导入
          <strong>{{ importResult.importedCount }}</strong> 条。
        </p>
        <div v-if="!importResult.success && importResult.errors?.length" class="import-errors">
          <div class="import-errors-title">错误明细：</div>
          <a-list
            :data-source="importResult.errors"
            size="small"
            :pagination="{ pageSize: 5 }"
          >
            <template #renderItem="{ item }">
              <a-list-item>
                <div class="import-error-item">
                  <div class="import-error-row">第 {{ item.rowIndex }} 行（{{ item.email || '邮箱为空' }}）</div>
                  <ul>
                    <li v-for="(err, idx) in item.errors" :key="idx">{{ err }}</li>
                  </ul>
                </div>
              </a-list-item>
            </template>
          </a-list>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { CopyOutlined, DownOutlined, PlusOutlined, ReloadOutlined, UploadOutlined, DownloadOutlined } from '@ant-design/icons-vue'
import { useUserManagement } from '@/composables/useUserManagement.js'
import { getUser, getUserInvites, updateUser, listUserSkills, listUserLearnedSkillsByMonth, resetLearnedSkillQuota, releaseCustomSkillQuota, importUsers, downloadUserImportTemplate } from '@/api/user.js'

const {
  users,
  total,
  loading,
  page,
  pageSize,
  keyword,
  inviteCode,
  fetchUsers,
  handleSearch,
  handleReset,
  handlePageChange,
  handleStatusChange,
  handleResetPassword,
  handleCreateUser,
  handleDeleteUser
} = useUserManagement()

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '邮箱/账号', dataIndex: 'email', key: 'email', width: 220 },
  { title: '昵称', dataIndex: 'nickname', key: 'nickname', width: 140 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '类型', dataIndex: 'userType', key: 'userType', width: 100 },
  { title: '邀请码', dataIndex: 'inviteCode', key: 'inviteCode', width: 140 },
  { title: '邀请人', key: 'inviter', width: 180 },
  { title: '邀请人数', key: 'invitedCount', width: 100 },
  { title: '会员套餐', dataIndex: 'membershipPlan', key: 'membershipPlan', width: 100 },
  { title: '会员到期', dataIndex: 'membershipExpireAt', key: 'membershipExpireAt', width: 170 },
  { title: '注册时间', dataIndex: 'createdAt', key: 'createdAt', width: 170 },
  { title: '最后登录', key: 'lastLoginAt', width: 170 },
  { title: '操作', key: 'actions', fixed: 'right', width: 100 }
]

const inviteColumns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '邮箱', dataIndex: 'email', key: 'email', ellipsis: true },
  { title: '昵称', dataIndex: 'nickname', key: 'nickname' },
  { title: '绑定/注册时间', dataIndex: 'createdAt', key: 'createdAt', width: 170 }
]

const resetPasswordVisible = ref(false)
const resetPasswordTarget = ref(null)
const detailVisible = ref(false)
const detailUser = ref(null)
const detailTabKey = ref('basic')
const userSkills = ref([])
const skillsLoading = ref(false)
const releaseCount = ref(1)
const releaseLoading = ref(false)
const learnedMonths = ref([])
const learnedLoading = ref(false)
const resettingPeriod = ref(null)

const currentPeriod = (() => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
})()

const skillColumns = [
  { title: '业务编号', dataIndex: 'bizNo', key: 'bizNo', width: 150 },
  { title: '提示词名称', dataIndex: 'skillName', key: 'skillName', width: 150 },
  { title: '提示词内容', dataIndex: 'prompt', key: 'prompt', ellipsis: true, width: 320 },
  { title: '适用范围', key: 'scope', width: 120 },
  { title: '使用次数', dataIndex: 'useCount', key: 'useCount', width: 90 },
  { title: '审核状态', key: 'auditStatus', width: 100 },
  { title: '创建时间', key: 'createdAt', width: 170 }
]

const learnedColumns = [
  { title: '月份', key: 'period', width: 120 },
  { title: '已用次数', dataIndex: 'usedCount', key: 'usedCount', width: 100 },
  { title: '预扣次数', dataIndex: 'preUsedCount', key: 'preUsedCount', width: 100 },
  { title: '产生提示词数', key: 'skillCount', width: 120 },
  { title: '操作', key: 'action', width: 140 }
]

const inviteModalVisible = ref(false)
const inviteDetail = ref(null)
const inviteLoading = ref(false)
const inviteTarget = ref(null)

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

const importing = ref(false)
const importResultVisible = ref(false)
const importResult = ref(null)

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
    { min: 6, max: 32, message: '密码不符合要求，长度需在 6-32 字符之间', trigger: 'blur' }
  ],
  userType: [
    { required: true, message: '请选择用户类型', trigger: 'change' }
  ]
}

const planLabel = (code) => {
  const map = { monthly: '月度会员', quarterly: '季度会员', yearly: '年度会员' }
  return map[code] || code
}

const formatDateTime = (s) => {
  if (!s) return ''
  return s.replace('T', ' ').slice(0, 19)
}

const copyEmail = async (email) => {
  try {
    await navigator.clipboard.writeText(email)
    message.success('邮箱已复制')
  } catch (error) {
    message.error('复制失败')
  }
}

const copyInviteCode = async (inviteCode) => {
  try {
    await navigator.clipboard.writeText(inviteCode)
    message.success('邀请码已复制')
  } catch (error) {
    message.error('复制失败')
  }
}

const confirmStatusChange = (user) => {
  const nextStatus = user.status === 'enabled' ? 'disabled' : 'enabled'
  Modal.confirm({
    title: `确定${nextStatus === 'enabled' ? '启用' : '禁用'}该用户？`,
    okText: '确认',
    cancelText: '取消',
    onOk: () => handleStatusChange(user)
  })
}

const confirmDelete = (user) => {
  Modal.confirm({
    title: '确定删除该用户？',
    content: `账号：${user.email}，删除后不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: () => handleDeleteUser(user)
  })
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
    } catch (error) {
      // 错误已在 useUserManagement 中提示，此处仅阻止弹框关闭
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
  detailTabKey.value = 'basic'
  releaseCount.value = 1
  userSkills.value = []
  learnedMonths.value = []
  try {
    detailUser.value = await getUser(user.id)
  } catch (error) {
    detailUser.value = user
  }
  detailVisible.value = true
  loadUserSkillsTab()
  loadLearnedSkillsTab()
}

const closeDetailModal = () => {
  detailVisible.value = false
  detailUser.value = null
  detailTabKey.value = 'basic'
}

const loadUserSkillsTab = async () => {
  if (!detailUser.value?.id) return
  skillsLoading.value = true
  try {
    userSkills.value = await listUserSkills(detailUser.value.id, 1)
  } catch (error) {
    message.error(error.message || '加载我的提示词失败')
    userSkills.value = []
  } finally {
    skillsLoading.value = false
  }
}

const loadLearnedSkillsTab = async () => {
  if (!detailUser.value?.id) return
  learnedLoading.value = true
  try {
    learnedMonths.value = await listUserLearnedSkillsByMonth(detailUser.value.id)
  } catch (error) {
    message.error(error.message || '加载学习的提示词失败')
    learnedMonths.value = []
  } finally {
    learnedLoading.value = false
  }
}

const handleReleaseCustomQuota = () => {
  if (!detailUser.value?.id) return
  if (!releaseCount.value || releaseCount.value < 1) {
    message.warning('请输入有效的释放数量')
    return
  }
  Modal.confirm({
    title: '确定释放自定义提示词额度？',
    content: `将为用户「${detailUser.value.nickname || detailUser.value.email}」释放 ${releaseCount.value} 个自定义提示词额度。`,
    okText: '确认释放',
    cancelText: '取消',
    onOk: async () => {
      releaseLoading.value = true
      try {
        await releaseCustomSkillQuota(detailUser.value.id, releaseCount.value)
        message.success('额度已释放')
        loadUserSkillsTab()
      } catch (error) {
        message.error(error.message || '释放额度失败')
      } finally {
        releaseLoading.value = false
      }
    }
  })
}

const handleResetLearnedQuota = (record) => {
  if (!detailUser.value?.id) return
  Modal.confirm({
    title: `确定重置 ${record.period} 学习提示词额度？`,
    content: '重置后该月份的已用次数和预扣次数将清零，仅用于额度被异常占用的兜底场景。',
    okText: '确认重置',
    cancelText: '取消',
    onOk: async () => {
      resettingPeriod.value = record.period
      try {
        await resetLearnedSkillQuota(detailUser.value.id, record.period)
        message.success(`${record.period} 额度已重置`)
        loadLearnedSkillsTab()
      } catch (error) {
        message.error(error.message || '重置额度失败')
      } finally {
        resettingPeriod.value = null
      }
    }
  })
}

const auditStatusLabel = (status) => {
  const map = { 0: '待审核', 1: '已通过', 2: '已拒绝' }
  return map[status] || '未知'
}

const auditStatusColor = (status) => {
  const map = { 0: 'default', 1: 'green', 2: 'red' }
  return map[status] || 'default'
}

const openInviteModal = async (user) => {
  inviteTarget.value = user
  inviteLoading.value = true
  inviteModalVisible.value = true
  try {
    inviteDetail.value = await getUserInvites(user.id)
  } catch (error) {
    message.error(error.message || '加载邀请关系失败')
    inviteDetail.value = { userId: user.id, inviteCode: user.inviteCode, inviter: null, invitees: [] }
  } finally {
    inviteLoading.value = false
  }
}

const closeInviteModal = () => {
  inviteModalVisible.value = false
  inviteDetail.value = null
  inviteTarget.value = null
}

const downloadTemplate = async () => {
  try {
    const res = await downloadUserImportTemplate()
    const blob = new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '用户导入模板.xlsx'
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  } catch (error) {
    message.error(error.message || '下载模板失败')
  }
}

const beforeImport = (file) => {
  if (!file.name.toLowerCase().endsWith('.xlsx')) {
    message.error('请上传 .xlsx 格式的 Excel 文件')
    return false
  }
  if (file.size > 10 * 1024 * 1024) {
    message.error('文件大小不能超过 10MB')
    return false
  }
  return true
}

const handleImport = async ({ file }) => {
  importing.value = true
  try {
    const result = await importUsers(file)
    importResult.value = result
    importResultVisible.value = true
    if (result.success) {
      message.success(`成功导入 ${result.importedCount} 个用户`)
      fetchUsers()
    }
  } catch (error) {
    message.error(error.message || '导入失败')
  } finally {
    importing.value = false
  }
}

const closeImportResultModal = () => {
  importResultVisible.value = false
  importResult.value = null
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

.email-copy-btn {
  padding: 0 4px;
  height: auto;
  line-height: 1;
  vertical-align: middle;
  margin-left: 2px;
}

.email-copy-btn:hover {
  color: #07c160;
}

.invite-code-copy-btn {
  padding: 0 4px;
  height: auto;
  line-height: 1;
  vertical-align: middle;
  margin-left: 2px;
}

.invite-code-copy-btn:hover {
  color: #07c160;
}

.invite-modal-content .invite-section-title {
  margin: 16px 0 8px 0;
  font-weight: 500;
  font-size: 14px;
}

.invite-modal-content .ant-descriptions {
  margin-bottom: 8px;
}

.skill-tab-toolbar {
  margin-bottom: 16px;
}

:deep(.user-detail-modal) .ant-modal-content {
  max-width: 1280px;
  margin: 0 auto;
}

.import-result p {
  margin: 0 0 12px 0;
}

.import-errors-title {
  font-weight: 500;
  margin-bottom: 8px;
}

.import-error-item {
  width: 100%;
}

.import-error-row {
  font-weight: 500;
  margin-bottom: 4px;
}

.import-error-item ul {
  margin: 0;
  padding-left: 18px;
}

.import-error-item li {
  color: #cf1322;
  font-size: 13px;
  line-height: 1.6;
}
</style>
