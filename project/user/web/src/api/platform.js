 import request from '@/utils/request.js'
 
 const BASE = '/platforms'
 
 /** 获取启用的自媒体平台列表（用户端制定自媒体方案第一步）。 */
 export function fetchPlatforms() {
   return request.get(BASE).then((res) => res.data || [])
 }
