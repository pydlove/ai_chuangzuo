import { api } from '@/api/auth'

/**
 * 获取热搜平台列表。
 * @returns {Promise<Array<{code:string,name:string,icon:string,sortOrder:number}>>}
 */
export function getHotSearchPlatforms() {
  return api.get('/hot-search/platforms').then(res => res.data || [])
}

/**
 * 查询指定平台和日期的热搜榜单。
 * @param {string} platform 平台编码
 * @param {string} date 日期 yyyy-MM-dd
 * @returns {Promise<Array<{rank:number,title:string,hotValue:string,url:string,searchCount:number}>>}
 */
export function getHotSearchList(platform, date) {
  return api.get('/hot-search', { params: { platform, date } }).then(res => res.data || [])
}
