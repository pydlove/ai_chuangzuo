import request from '@/utils/request'

/**
 * 首页 Banner 列表（按 sort ASC，仅未删除）。
 * @returns {Promise<Array<{id:number, imageUrl:string, linkUrl:string}>>}
 */
export function fetchHomeBanners() {
  return request.get('/home/banners').then((res) => res.data || [])
}

/**
 * 首页用户评价列表（仅启用，分页）。
 * @returns {Promise<Array<{id:number, source:string, avatarUrl:string, name:string, title:string, starRating:number, reviewText:string}>>}
 */
export function fetchHomeTestimonials(page = 1, size = 20) {
  return request.get('/home/testimonials', { params: { page, size } }).then((res) => res.data || [])
}
