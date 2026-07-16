import { api } from '@/api/auth'

/**
 * 查询启用的导出模板列表。
 * @returns {Promise<Array<{templateKey:string, name:string, platform:string, description:string, bgColor:string, textColor:string, visualStyle:Object, signatureText:string, signaturePosition:string, sortOrder:number}>>}
 */
export function listExportTemplates() {
  return api.get('/export-templates').then((res) => res.data || [])
}
