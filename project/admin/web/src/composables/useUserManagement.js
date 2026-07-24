import { reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { listUsers, updateUserStatus, resetUserPassword, createUser, deleteUser } from '@/api/user.js'

export function useUserManagement() {
  const users = ref([])
  const total = ref(0)
  const loading = ref(false)
  const page = ref(1)
  const pageSize = ref(10)
  const keyword = ref('')
  const inviteCode = ref('')

  const fetchUsers = async () => {
    loading.value = true
    try {
      const res = await listUsers({
        keyword: keyword.value,
        inviteCode: inviteCode.value,
        page: page.value,
        pageSize: pageSize.value
      })
      users.value = res.list
      total.value = res.total
    } catch (error) {
      message.error(error.message || '加载用户列表失败')
    } finally {
      loading.value = false
    }
  }

  const handleSearch = () => {
    page.value = 1
    fetchUsers()
  }

  const handleReset = () => {
    keyword.value = ''
    inviteCode.value = ''
    page.value = 1
    fetchUsers()
  }

  const handlePageChange = (newPage, newPageSize) => {
    page.value = newPage
    pageSize.value = newPageSize
    fetchUsers()
  }

  const handleStatusChange = async (user) => {
    const nextStatus = user.status === 'enabled' ? 'disabled' : 'enabled'
    try {
      await updateUserStatus(user.id, nextStatus)
      message.success(`用户已${nextStatus === 'enabled' ? '启用' : '禁用'}`)
      fetchUsers()
    } catch (error) {
      message.error(error.message || '更新状态失败')
    }
  }

  const handleResetPassword = async (user) => {
    try {
      const res = await resetUserPassword(user.id)
      message.success(`密码已重置为 ${res.newPassword}`)
    } catch (error) {
      message.error(error.message || '重置密码失败')
    }
  }

  const handleCreateUser = async (values) => {
    try {
      const res = await createUser(values)
      message.success('用户创建成功')
      fetchUsers()
      return res
    } catch (error) {
      message.error(error.message || '创建用户失败')
      throw error
    }
  }

  const handleDeleteUser = async (user) => {
    await deleteUser(user.id)
    message.success('用户已删除')
    fetchUsers()
  }

  return {
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
  }
}
