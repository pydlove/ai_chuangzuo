import request from '@/utils/request'

/**
 * 首页 Banner 列表（按 sort ASC，仅未删除）。
 * @returns {Promise<Array<{id:number, imageUrl:string, linkUrl:string}>>}
 */
export function fetchHomeBanners() {
  return request.get('/home/banners').then((res) => res.data || [])
}

/**
 * 首页用户评价列表（仅启用，按 sort ASC）。
 * @returns {Promise<Array<{id:number, avatarUrl:string, name:string, title:string, starRating:number, reviewText:string}>>}
 */
export function fetchHomeTestimonials() {
  return request.get('/home/testimonials').then((res) => res.data || [])
}
