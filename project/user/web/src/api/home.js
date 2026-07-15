import request from '@/utils/request'

/**
 * 首页 Banner 列表（按 sort ASC，仅未删除）。
 * @returns {Promise<Array<{id:number, imageUrl:string, linkUrl:string}>>}
 */
export function fetchHomeBanners() {
  return request.get('/home/banners').then((res) => res.data || [])
}