import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { listSkills, rejectSkill, approveSkill, approveBatch } from '@/api/skill.js'

export function useSkillReview() {
  const skills = ref([])
  const total = ref(0)
  const loading = ref(false)
  const page = ref(1)
  const pageSize = ref(20)
  const keyword = ref('')
  const activeTab = ref('pending')

  const fetchSkills = async () => {
    loading.value = true
    try {
      const isReviewed = activeTab.value === 'reviewed'
      const res = await listSkills({
        keyword: keyword.value,
        pageNum: page.value,
        pageSize: pageSize.value,
        status: isReviewed ? undefined : 0,
        reviewed: isReviewed ? true : undefined
      })
      skills.value = res.list
      total.value = res.total
    } catch (error) {
      message.error(error.message || '加载提示词列表失败')
    } finally {
      loading.value = false
    }
  }

  const handleSearch = () => {
    page.value = 1
    fetchSkills()
  }

  const handleReset = () => {
    keyword.value = ''
    page.value = 1
    fetchSkills()
  }

  const handlePageChange = (newPage, newPageSize) => {
    page.value = newPage
    pageSize.value = newPageSize
    fetchSkills()
  }

  const handleTabChange = () => {
    page.value = 1
    fetchSkills()
  }

  const handleApprove = async (skill) => {
    try {
      await approveSkill(skill.id)
      message.success('提示词已通过')
      fetchSkills()
      return true
    } catch (error) {
      message.error(error.message || '通过失败')
      return false
    }
  }

  const handleReject = async (skill, reason) => {
    try {
      await rejectSkill(skill.id, reason)
      message.success('提示词已打回')
      fetchSkills()
      return true
    } catch (error) {
      message.error(error.message || '打回失败')
      return false
    }
  }

  const handleApproveBatch = async (ids) => {
    try {
      const count = await approveBatch(ids)
      message.success(`批量通过 ${count} 条提示词`)
      fetchSkills()
      return true
    } catch (error) {
      message.error(error.message || '批量通过失败')
      return false
    }
  }

  return {
    skills,
    total,
    loading,
    page,
    pageSize,
    keyword,
    activeTab,
    fetchSkills,
    handleSearch,
    handleReset,
    handlePageChange,
    handleTabChange,
    handleReject,
    handleApprove,
    handleApproveBatch
  }
}
