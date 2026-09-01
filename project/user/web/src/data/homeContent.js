/**
 * 首页（PC + Mobile）文案配置
 *
 * 修改此文件即可调整 http://localhost:22345/ 首页展示的文字、按钮、链接与数据。
 * 结构分为：Hero、统计数据、特色功能、收益玩法、使用步骤、最终 CTA。
 * 顶部导航与 CTA 已抽到 siteConfig.js，所有落地页共用。
 */

/** Hero 区域 */
export const homeHero = {
  badge: '自媒体运营流水线 · 从定位到变现',
  title: '临渊羡鱼，不如退而结网',
  desc: '别让工资成为你唯一的收入，别让时间白白流走。\n\n' +
         '爱创作工坊不是帮你生成一篇文章，而是帮你建立一套可持续的自媒体运营方案。把“不知道写什么、不知道怎么发、不知道怎么变现”拆成固定流程：先定位、再选题、注入你的个人素材、生成内容、给出发布策略，直到看见收益。\n\n' +
         '有主业想搞副业、有大量时间不会变现、有垂直经验但不知道怎么输出——来这里，把时间和能力织成一张被动收入的网。',
  primaryBtn: {
    text: '立即开始创作',
    to: '/console/workbench'
  },
  secondaryBtn: {
    text: '看看怎么挣钱',
    to: '/guide'
  },
  checkmarks: [
    '定制您的自媒体运营方案',
    '多平台变现',
    '账号越久越值钱'
  ],
  bannerCta: '查看详情'
}

/** 快速数据区（PC / Mobile 分别配置，因为展示文案略有差异） */
export const homeStats = {
  pc: [
//    { num: '¥ 800 万 +', label: '累计为创作者带来收益' },
    { num: '5000 +', label: '累计注册账号' },
    { num: '7 大主流', label: '已覆盖变现平台' },
    { num: '3 分钟', label: '平均成稿时间' }
  ],
  mobile: [
//    { num: '¥ 800 万 +', label: '累计为创作者带来收益' },
    { num: '5000+', label: '累计注册账号' },
    { num: '6 大', label: '已覆盖变现平台' },
    { num: '3 分钟', label: '平均成稿时间' }
  ]
}

/** 特色功能区 */
export const homeFeatures = {
  tag: '为什么选择爱创作工坊',
  title: '从定位到变现，一步不落',
  subtitle: '把“不知道写什么、不知道怎么发、不知道怎么变现”拆成可执行的固定流程',
  items: [
    {
      icon: 'user',
      name: '先定位，再创作',
      desc: '不是空白编辑器，先帮你选平台、定赛道、做人设，不走弯路再动笔。'
    },
    {
      icon: 'search',
      name: '差异化选题',
      desc: '基于低粉高赞数据和蓝海细分赛道，持续找到适合你账号的好角度。'
    },
    {
      icon: 'star',
      name: '注入个人素材',
      desc: '强制导入你的经验、案例、观点，降低 AI 大路货同质化，内容有你的印记。'
    },
    {
      icon: 'clock',
      name: '3 分钟成稿',
      desc: 'AI 自动完成标题、结构、正文，不用懂写作也能产出完整内容。'
    },
    {
      icon: 'grid',
      name: '一稿多发跨平台',
      desc: '一次创作，公众号、小红书、抖音、百家号、头条、知乎全部适配。'
    },
    {
      icon: 'trending-up',
      name: '持续变现',
      desc: '创作币、返利、奖金、外部自媒体收入，配合运营复盘，让账号越写越值钱。'
    }
  ]
}

/** 收益玩法区 */
export const homeEarnings = {
  tag: '4 种变现路径',
  title: '边写边赚',
  subtitle: '平台内赚创作币、返利、奖金，平台外赚自媒体收入，变现路径一目了然',
  items: [
    {
      icon: 'clock',
      name: '创作币奖励',
      desc: '完成任务、活动、上榜，1 创作币 = 1 元。可抵扣会员购买，满 100 创作币提现到支付宝。'
    },
    {
      icon: 'users',
      name: '邀请好友返利',
      desc: '被邀请人完成邮箱验证双方得创作币；好友购买会员首单返 10%、后续返 5%；累计邀请 3 人 +30 币、5 人 +50 币。'
    },
    {
      icon: 'star',
      name: '约稿中心',
      desc: '把你的文章投稿到对应的活动，采纳后即可获得高额的收益。'
    },
    {
      icon: 'layout',
      name: '提示词市场',
      desc: '发布你的提示词，其他用户使用你即可获取收益。'
    }
  ],
  link: {
    pc: { text: '查看完整玩法 · 看看别人赚了多少 →', to: '/guide' },
    mobile: { text: '查看完整玩法 →', to: '/guide' }
  }
}

/** 使用步骤区 */
export const homeSteps = {
  title: '3 步跑通自媒体运营流水线',
  subtitle: '先定位，再选题，最后生成并发布，每一步都有策略兜底',
  items: [
    { num: '1', name: '定位账号', desc: '选平台、定赛道、做人设' },
    { num: '2', name: '拿到选题', desc: '低粉高赞 + 蓝海细分 + 个人素材' },
    { num: '3', name: '生成发布', desc: '成稿、多平台适配、发布策略' }
  ]
}

/** 最终 CTA 区 */
export const homeFinalCta = {
  title: '现在起号，搭一条可执行的自媒体流水线',
  desc: '不是写一篇赚一篇，而是搭一条可持续运转的自媒体流水线。',
  primaryBtn: {
    text: '立即开始创作',
    to: '/console/workbench'
  },
  secondaryBtn: {
    text: '查看玩法指南',
    to: '/guide'
  }
}

/** 品牌名 */
export const homeBrand = '爱创作工坊'
