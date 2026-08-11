import request from '@/utils/request'

export function getShareConfig(sceneKey) {
  return request({ url: `/share-config/${sceneKey}`, method: 'get' })
}
