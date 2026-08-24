import { api } from '@/api/auth'

const BASE = '/workbench/weekly-articles'

/**
 * 查询当前用户本周的文章数据。
 * @returns {Promise<{code:number, data:Array<{title:string, reads:number}>, message:string}>}
 */
export function getWeeklyArticles() {
  return api.get(BASE)
}

/**
 * 保存当前用户本周的文章数据。
 * @param {{articles:Array<{title:string, reads:number}>}} data
 * @returns {Promise<{code:number, message:string}>}
 */
export function saveWeeklyArticles(data) {
  return api.post(BASE, data)
}
