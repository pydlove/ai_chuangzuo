export const PLATFORM_NAME_MAP = {
  wechat: '微信公众号',
  xiaohongshu: '小红书',
  toutiao: '今日头条',
  baijiahao: '百家号',
  douyin: '抖音图文',
  kuaishou: '快手图文',
  zhihu: '知乎',
  bilibili: 'B站'
}

export const PLATFORM_OPTIONS = [
  { key: 'wechat', label: PLATFORM_NAME_MAP.wechat },
  { key: 'xiaohongshu', label: PLATFORM_NAME_MAP.xiaohongshu },
  { key: 'toutiao', label: PLATFORM_NAME_MAP.toutiao },
  { key: 'baijiahao', label: PLATFORM_NAME_MAP.baijiahao },
  { key: 'douyin', label: PLATFORM_NAME_MAP.douyin },
  { key: 'kuaishou', label: PLATFORM_NAME_MAP.kuaishou },
  { key: 'zhihu', label: PLATFORM_NAME_MAP.zhihu },
  { key: 'bilibili', label: PLATFORM_NAME_MAP.bilibili }
]

// 复制标签时按平台习惯加 # 号：百家号是 #话题#，抖音/快手/小红书/公众号/头条是 #话题，
// 知乎/B站话题在发布页搜索选择，不带 # 原样复制
const TAG_COPY_FORMATTERS = {
  baijiahao: tag => `#${tag}#`,
  douyin: tag => `#${tag}`,
  kuaishou: tag => `#${tag}`,
  xiaohongshu: tag => `#${tag}`,
  wechat: tag => `#${tag}`,
  toutiao: tag => `#${tag}`
}

export function formatTagForCopy(tag, platform) {
  const formatter = TAG_COPY_FORMATTERS[platform]
  return formatter ? formatter(tag) : tag
}
