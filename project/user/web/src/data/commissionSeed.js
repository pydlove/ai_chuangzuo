import { genId, COMMISSION_CONFIG } from '@/api/commission'

// 演示数据:首次访问时播种到 localStorage,方便用户立刻看到多种状态。
// 当前用户 ID 默认 'demo-user-1';可通过 useCommission().setCurrentUserId() 切换。
const NOW = Date.now()
const DAY = 24 * 60 * 60 * 1000

export const DEMO_USER_ID = 'demo-user-1'
export const DEMO_USER_NICKNAMES = {
  'demo-user-1': '柠檬不酸',
  'demo-user-2': '墨鱼写作',
  'demo-user-3': '小桥流水',
  'demo-user-4': '夜半听雨'
}

const TASK_1_ID = genId('cmt')
const TASK_2_ID = genId('cmt')
const TASK_3_ID = genId('cmt')
const TASK_4_ID = genId('cmt')

export const SEED_TASKS = [
  // 进行中,5 天前发布,3 天后截止
  {
    id: TASK_1_ID,
    publisherId: 'demo-user-2',
    publisherNickname: '墨鱼写作',
    title: '征集 5 篇小红书爆款养生选题',
    description: '面向 25-35 岁女性,标题党但要有干货,字数 600-1000。',
    requirements: { minWordCount: 600, maxWordCount: 1000, styleHint: '种草风' },
    rewardCoin: 30,
    platformFeeRate: COMMISSION_CONFIG.PLATFORM_FEE_RATE,
    deadlineAt: new Date(NOW + 3 * DAY).toISOString(),
    graceDeadlineAt: new Date(NOW + 3 * DAY + COMMISSION_CONFIG.GRACE_HOURS * 60 * 60 * 1000).toISOString(),
    status: 'OPEN',
    winnerSubmissionId: null,
    settledAt: null,
    createdAt: new Date(NOW - 5 * DAY).toISOString()
  },
  // 已结算
  {
    id: TASK_2_ID,
    publisherId: 'demo-user-3',
    publisherNickname: '小桥流水',
    title: '招募公众号情感故事作者',
    description: '第一人称情感故事,字数 1200-2000。',
    requirements: { minWordCount: 1200, maxWordCount: 2000, styleHint: '细腻、走心' },
    rewardCoin: 80,
    platformFeeRate: COMMISSION_CONFIG.PLATFORM_FEE_RATE,
    deadlineAt: new Date(NOW - 2 * DAY).toISOString(),
    graceDeadlineAt: new Date(NOW - 1 * DAY).toISOString(),
    status: 'SETTLED',
    winnerSubmissionId: 'cms_seed_winner_1',
    settledAt: new Date(NOW - 1 * DAY).toISOString(),
    createdAt: new Date(NOW - 9 * DAY).toISOString()
  },
  // 已过期无人选
  {
    id: TASK_3_ID,
    publisherId: 'demo-user-4',
    publisherNickname: '夜半听雨',
    title: '求一篇公众号爆款书评',
    description: '近期畅销书,800-1500 字。',
    requirements: { minWordCount: 800, maxWordCount: 1500, styleHint: '书评' },
    rewardCoin: 50,
    platformFeeRate: COMMISSION_CONFIG.PLATFORM_FEE_RATE,
    deadlineAt: new Date(NOW - 2 * DAY).toISOString(),
    graceDeadlineAt: new Date(NOW - 1 * DAY).toISOString(),
    status: 'EXPIRED',
    winnerSubmissionId: null,
    settledAt: null,
    createdAt: new Date(NOW - 8 * DAY).toISOString()
  },
  // 进行中且我投递过
  {
    id: TASK_4_ID,
    publisherId: 'demo-user-3',
    publisherNickname: '小桥流水',
    title: '招募知乎带货短文',
    description: '数码类带货,500-800 字。',
    requirements: { minWordCount: 500, maxWordCount: 800, styleHint: '理性种草' },
    rewardCoin: 25,
    platformFeeRate: COMMISSION_CONFIG.PLATFORM_FEE_RATE,
    deadlineAt: new Date(NOW + 6 * DAY).toISOString(),
    graceDeadlineAt: new Date(NOW + 6 * DAY + COMMISSION_CONFIG.GRACE_HOURS * 60 * 60 * 1000).toISOString(),
    status: 'OPEN',
    winnerSubmissionId: null,
    settledAt: null,
    createdAt: new Date(NOW - 1 * DAY).toISOString()
  }
]

export const SEED_SUBMISSIONS = [
  {
    id: 'cms_seed_winner_1',
    taskId: TASK_2_ID,
    submitterId: 'demo-user-1',
    submitterNickname: '柠檬不酸',
    articleBizNo: 'demo-art-winner',
    articleTitle: '30 岁那年,我学会了不再讨好',
    wordCount: 1650,
    submittedAt: new Date(NOW - 4 * DAY).toISOString(),
    withdrawnAt: null
  },
  // 第二篇投稿,未中标,留着展示列表
  {
    id: genId('cms'),
    taskId: TASK_2_ID,
    submitterId: 'demo-user-2',
    submitterNickname: '墨鱼写作',
    articleBizNo: 'demo-art-2',
    articleTitle: '那年夏天,我与自己和解',
    wordCount: 1480,
    submittedAt: new Date(NOW - 3 * DAY).toISOString(),
    withdrawnAt: null
  },
  // 当前用户投过第四个任务
  {
    id: genId('cms'),
    taskId: TASK_4_ID,
    submitterId: 'demo-user-1',
    submitterNickname: '柠檬不酸',
    articleBizNo: 'demo-art-mine',
    articleTitle: '2026 数码圈三大趋势',
    wordCount: 720,
    submittedAt: new Date(NOW - 6 * 60 * 60 * 1000).toISOString(),
    withdrawnAt: null
  }
]

// 我的作品 seed(给投稿选择器 fallback 数据,后端未实现时也能演示)
export const DEMO_ARTICLES = [
  {
    bizNo: 'demo-art-001',
    title: '从月薪 5k 到年入百万,普通人的逆袭公式',
    wordCount: 1580,
    platformName: '公众号',
    completedAt: new Date(NOW - 2 * DAY).toISOString()
  },
  {
    bizNo: 'demo-art-002',
    title: '5 个被低估的小红书爆款选题',
    wordCount: 680,
    platformName: '小红书',
    completedAt: new Date(NOW - 4 * DAY).toISOString()
  },
  {
    bizNo: 'demo-art-003',
    title: '亲测有效:告别失眠的 7 个小习惯',
    wordCount: 1100,
    platformName: '今日头条',
    completedAt: new Date(NOW - 7 * DAY).toISOString()
  },
  {
    bizNo: 'demo-art-004',
    title: '如何用 ChatGPT 把工作效率拉满',
    wordCount: 2400,
    platformName: '知乎',
    completedAt: new Date(NOW - 10 * DAY).toISOString()
  }
]