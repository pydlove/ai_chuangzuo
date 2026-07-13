import { reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  listTemplates,
  createTemplate,
  updateTemplate,
  deleteTemplate,
  getTemplate,
  initTemplateStages,
  publishTemplate,
  offlineTemplate,
  cloneTemplate,
  listTemplateVersions
} from '@/api/promptTemplate.js'

export function usePromptTemplate() {
  const list = ref([])
  const total = ref(0)
  const loading = ref(false)
  const page = ref(1)
  const pageSize = ref(20)
  const keyword = ref('')

  const fetch = async () => {
    loading.value = true
    try {
      const res = await listTemplates({
        keyword: keyword.value,
        page: page.value,
        pageSize: pageSize.value
      })
      list.value = res.list
      total.value = res.total
    } catch (error) {
      message.error(error.message || '加载模板列表失败')
    } finally {
      loading.value = false
    }
  }

  const handleSearch = () => {
    page.value = 1
    fetch()
  }

  const handleReset = () => {
    keyword.value = ''
    page.value = 1
    fetch()
  }

  const handlePageChange = (p, ps) => {
    page.value = p
    pageSize.value = ps
    fetch()
  }

  /**
   * 12 阶段编辑器用的 payload 构造。
   * 每次保存时按 stages 数组里每个 stage 的 stageIndex 1-12 一起提交。
   */
  const buildPayload = (form) => ({
    name: form.name?.trim(),
    remark: form.remark,
    baseContent: form.baseContent || '',
    stages: (form.stages || []).map((s) => ({
      stageIndex: s.stageIndex,
      enabled: s.enabled ?? 1,
      aiPrompt: s.aiPrompt ?? '',
      ruleConfig: s.ruleConfig ?? '{}'
    }))
  })

  const handleCreate = async (payload) => {
    const id = await createTemplate(payload)
    message.success('已创建 12 阶段默认模板')
    return id
  }

  const handleUpdate = async (id, payload) => {
    await updateTemplate(id, payload)
    message.success('已保存')
  }

  const handleDelete = async (id) => {
    await deleteTemplate(id)
    message.success('已删除')
    await fetch()
  }

  /** 老模板补齐 12 阶段默认值。 */
  const handleInitStages = async (id) => {
    const inserted = await initTemplateStages(id)
    if (inserted > 0) {
      message.success(`已初始化 ${inserted} 个阶段`)
    } else {
      message.info('该模板已有 12 阶段，无需初始化')
    }
    return inserted
  }

  // ===== 阶段 2：发布 / 下线 / 克隆 =====

  /** 发布模板，返回新版本号。 */
  const handlePublish = async (id, changeNote) => {
    const res = await publishTemplate(id, changeNote || '')
    message.success(`已发布版本 v${res.data}`)
    await fetch()
    return res.data
  }

  /** 下线模板（仅 PUBLISHED 可下线）。 */
  const handleOffline = async (id) => {
    await offlineTemplate(id)
    message.success('已下线')
    await fetch()
  }

  /**
   * 克隆模板，返回新模板 id。
   * @param {number} sourceId 源模板 id
   * @param {string} newName 新模板名称
   * @param {string} [remark] 备注
   * @param {number} [sourceVersion] 可选，从指定历史版本克隆
   */
  const handleClone = async (sourceId, newName, remark, sourceVersion) => {
    const res = await cloneTemplate(sourceId, {
      name: newName,
      remark: remark || undefined,
      sourceVersion: sourceVersion || undefined
    })
    message.success(`已克隆，新模板 ID: ${res.data}`)
    await fetch()
    return res.data
  }

  /** 取模板的全部版本快照摘要。 */
  const handleListVersions = async (id) => {
    return await listTemplateVersions(id)
  }

  return {
    list,
    total,
    loading,
    page,
    pageSize,
    keyword,
    fetch,
    handleSearch,
    handleReset,
    handlePageChange,
    handleCreate,
    handleUpdate,
    handleDelete,
    handleInitStages,
    handlePublish,
    handleOffline,
    handleClone,
    handleListVersions,
    buildPayload,
    getTemplate
  }
}
