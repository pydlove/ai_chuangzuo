<template>
  <div class="hot-search-page">
    <!-- 日期选择 -->
    <div class="date-bar">
      <button
        v-for="date in dateList"
        :key="date.value"
        :class="['date-chip', { active: activeDate === date.value }]"
        @click="activeDate = date.value"
      >
        <span class="date-label">{{ date.label }}</span>
        <span class="date-value">{{ date.short }}</span>
      </button>
    </div>

    <!-- 页面标题 -->
    <div class="hot-search-header">
      <div class="hot-search-header-main">
        <div class="hot-search-icon">🔥</div>
        <div>
          <h2 class="hot-search-title">全网热搜榜</h2>
          <p class="hot-search-desc">聚合抖音、今日头条、B 站、微博、百度五大平台实时热点，追热点快人一步。</p>
        </div>
      </div>
      <div class="hot-search-current">{{ currentDateText }}</div>
    </div>

    <!-- 平台榜单 -->
    <div class="hot-search-section">
      <div class="platform-tabs">
        <button
          v-for="platform in platforms"
          :key="platform.key"
          :class="['platform-tab', { active: activePlatform === platform.key }, platform.key]"
          @click="activePlatform = platform.key"
        >
          <span class="platform-dot" />
          {{ platform.label }}
        </button>
      </div>

      <div class="hot-search-list">
        <div
          v-for="(item, index) in currentList"
          :key="`${activePlatform}-${index}`"
          class="hot-search-item"
          @click="copyTitle(item.title)"
        >
          <span :class="['hot-search-rank', `rank-${index + 1}`]">{{ index + 1 }}</span>
          <span class="hot-search-text" :title="item.title">{{ item.title }}</span>
          <span class="hot-search-heat">{{ item.heat }}</span>
          <span :class="['hot-search-trend', item.trend]">{{ trendText(item.trend) }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { message } from 'ant-design-vue'

const activePlatform = ref('douyin')
const activeDate = ref('')
const cache = ref(new Map())

const trendText = (trend) => {
  switch (trend) {
    case 'up': return '热'
    case 'down': return '降'
    case 'new': return '新'
    default: return ''
  }
}

const pad = (n) => String(n).padStart(2, '0')

const formatDate = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`

const getWeekLabel = (d) => {
  const days = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return days[d.getDay()]
}

const today = new Date()
const dateList = Array.from({ length: 5 }, (_, i) => {
  const d = new Date(today)
  d.setDate(d.getDate() - i)
  const value = formatDate(d)
  if (i === 0) activeDate.value = value
  return {
    value,
    label: i === 0 ? '今天' : i === 1 ? '昨天' : getWeekLabel(d),
    short: `${pad(d.getMonth() + 1)}/${pad(d.getDate())}`
  }
})

const currentDateText = computed(() => {
  const item = dateList.find((d) => d.value === activeDate.value)
  return item ? `${item.value} ${item.label}` : activeDate.value
})

const copyTitle = (title) => {
  if (navigator.clipboard) {
    navigator.clipboard.writeText(title).then(() => message.success('标题已复制'))
  } else {
    message.info(title)
  }
}

const shuffle = (list) => {
  return [...list].sort(() => Math.random() - 0.5)
}

const makeList = (titles) => {
  return titles.map((title, i) => ({
    title,
    heat: `${(Math.random() * 500 + 50).toFixed(1)}万`,
    trend: ['up', 'down', 'new'][Math.floor(Math.random() * 3)]
  }))
}

const basePlatforms = [
  {
    key: 'douyin',
    label: '抖音',
    titles: [
      '这是属于我们的夏天',
      '普通人如何靠副业月入过万',
      '原来这些方法真的能让人变自律',
      '90 后夫妻裸辞返乡创业日记',
      '被这条视频治愈了一整天',
      '打工人必备的高效休息法',
      '当代年轻人的精神状态 be like',
      '这个隐藏功能 90% 的人不知道',
      '挑战 30 天早起会发生什么',
      '终于有人把这件事说清楚了',
      '看完这条视频我立刻去行动了',
      '为什么我们越来越不爱发朋友圈',
      '小户型显大的 5 个神操作',
      '这届网友的评论区太有才了',
      '一个动作改善圆肩驼背',
      '周末去哪儿玩？这份清单收好',
      '成年人最顶级的自律是什么',
      '这些东西千万别再买了',
      '学会这招，做饭快了一倍',
      '全网都在找的BGM终于找到了'
    ]
  },
  {
    key: 'toutiao',
    label: '今日头条',
    titles: [
      '多地出台楼市新政',
      '新能源汽车销量再创新高',
      '高考志愿填报避坑指南',
      '存款利率下调意味着什么',
      '专家建议年轻人先就业再择业',
      '这个夏天去哪儿避暑',
      '农村电商如何助力乡村振兴',
      '养老第三支柱最新进展',
      '职场人如何提升核心竞争力',
      '人工智能会取代哪些岗位',
      '健康管理从体检报告开始',
      '中小微企业的减税新机遇',
      '旅行热度飙升的十大小城',
      '家庭教育中父母最容易犯的错',
      '为什么现在年轻人爱存钱',
      '医保改革最新政策解读',
      '高温天气防范指南',
      '退休人员养老金调整方案',
      '农产品直播带货新趋势',
      '城市更新带来的生活变化'
    ]
  },
  {
    key: 'bilibili',
    label: 'B 站',
    titles: [
      '耗时三个月，我做出了什么',
      '这部电影到底好在哪',
      '一个视频看懂某学科',
      '我在 B 站学习的一天',
      '那些年被我们误解的梗',
      '从零开始学剪辑',
      '史上最难游戏挑战',
      'UP 主的一天真实记录',
      '这部番为什么封神',
      '实验室里的大发现',
      '我用代码实现了童年梦想',
      '挑战 24 小时不用手机',
      '深度解析某经典作品',
      '普通人如何开始健身',
      '这个BUG让程序员崩溃',
      '我采访了 100 个陌生人',
      '音乐区神仙打架现场',
      '低成本改造出租屋',
      '那些教科书里的隐藏彩蛋',
      '科技改变生活的瞬间'
    ]
  },
  {
    key: 'weibo',
    label: '微博',
    titles: [
      '#今日份好心情#',
      '#这届网友太会了#',
      '#明星同款穿搭#',
      '#打工人的周一#',
      '#电视剧名场面#',
      '#夏日美食推荐#',
      '#宠物成精了#',
      '#旅行中的意外惊喜#',
      '#养生从年轻开始#',
      '#一部电影一句台词#',
      '#童年回忆杀#',
      '#朋友圈不敢发系列#',
      '#成年人的崩溃瞬间#',
      '#被陌生人暖到的瞬间#',
      '#第一次见家长#',
      '#我的租房改造#',
      '#上班摸鱼指南#',
      '#高温天气穿搭#',
      '#电竞圈最新消息#',
      '#偶像的新造型#'
    ]
  },
  {
    key: 'baidu',
    label: '百度',
    titles: [
      '今天是什么日子',
      '最新天气预报查询',
      '国内油价调整时间',
      '如何查询医保余额',
      '个人所得税退税流程',
      '附近医院挂号预约',
      '火车票候补购票规则',
      '身份证到期如何换领',
      '驾驶证期满换证流程',
      '异地就医备案怎么办理',
      '公积金提取条件',
      '社保断缴有什么影响',
      '新生儿上户口需要什么',
      '居住证办理流程',
      '护照办理最新要求',
      '签证进度怎么查询',
      '手机话费套餐怎么改',
      '宽带故障如何报修',
      '快递单号查询入口',
      '附近核酸检测点'
    ]
  }
]

const generateData = (dateValue) => {
  return basePlatforms.map((p) => ({
    key: p.key,
    label: p.label,
    list: makeList(shuffle(p.titles))
  }))
}

const getDataByDate = (dateValue) => {
  if (!cache.value.has(dateValue)) {
    cache.value.set(dateValue, generateData(dateValue))
  }
  return cache.value.get(dateValue)
}

const platforms = computed(() => getDataByDate(activeDate.value))

const currentList = computed(() => {
  const platform = platforms.value.find((p) => p.key === activePlatform.value)
  return platform ? platform.list : []
})
</script>

<style scoped>
.hot-search-page {
  padding: 24px 32px;
  margin: 0 auto;
}

.date-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  overflow-x: auto;
  padding-bottom: 4px;
}

.date-chip {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-width: 64px;
  padding: 10px 14px;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
  gap: 2px;
}

.date-chip:hover {
  border-color: #d9d9d9;
}

.date-chip.active {
  background: var(--color-primary, #FF2442);
  border-color: var(--color-primary, #FF2442);
  color: #fff;
  box-shadow: 0 4px 12px rgba(255, 36, 66, 0.18);
}

.date-label {
  font-size: 12px;
  font-weight: 500;
}

.date-value {
  font-size: 13px;
  font-weight: 600;
}

.hot-search-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.hot-search-header-main {
  display: flex;
  align-items: center;
  gap: 14px;
}

.hot-search-icon {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  background: linear-gradient(135deg, #ff4d4f 0%, #ff7875 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(255, 77, 79, 0.2);
}

.hot-search-title {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: var(--text-primary, #1f1f1f);
}

.hot-search-desc {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--text-secondary, #595959);
}

.hot-search-current {
  font-size: 13px;
  color: #8c8c8c;
  background: #f5f5f5;
  padding: 6px 12px;
  border-radius: 20px;
  flex-shrink: 0;
}

.hot-search-section {
  background: #fff;
  border-radius: 14px;
  padding: 24px 28px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.platform-tabs {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.platform-tab {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: #f5f5f5;
  border: none;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
  color: #595959;
  cursor: pointer;
  transition: all 0.2s;
}

.platform-tab:hover {
  background: #e8e8e8;
}

.platform-tab.active {
  background: var(--color-primary, #FF2442);
  color: #fff;
}

.platform-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
  opacity: 0.7;
}

.hot-search-list {
  display: flex;
  flex-direction: column;
}

.hot-search-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 15px 12px;
  border-radius: 10px;
  border-bottom: 1px solid #f5f5f5;
  cursor: pointer;
  transition: all 0.15s;
}

.hot-search-item:last-child {
  border-bottom: none;
}

.hot-search-item:hover {
  background: #fafafa;
  transform: translateX(2px);
}

.hot-search-rank {
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 700;
  color: #8c8c8c;
  flex-shrink: 0;
}

.hot-search-rank.rank-1 {
  background: #fff1f0;
  color: #cf1322;
}

.hot-search-rank.rank-2 {
  background: #fff7e6;
  color: #d48806;
}

.hot-search-rank.rank-3 {
  background: #f6ffed;
  color: #389e0d;
}

.hot-search-text {
  flex: 1;
  font-size: 15px;
  color: var(--text-primary, #1f1f1f);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hot-search-heat {
  font-size: 13px;
  color: #8c8c8c;
  flex-shrink: 0;
}

.hot-search-trend {
  font-size: 12px;
  padding: 3px 8px;
  border-radius: 4px;
  font-weight: 500;
  flex-shrink: 0;
}

.hot-search-trend.up {
  background: #fff1f0;
  color: #cf1322;
}

.hot-search-trend.down {
  background: #f5f5f5;
  color: #8c8c8c;
}

.hot-search-trend.new {
  background: #fff7e6;
  color: #d48806;
}

/* 暗色主题 */
body[data-theme="dark"] .date-chip {
  background: #1f1f1f;
  border-color: #303030;
  color: rgba(255, 255, 255, 0.85);
}

body[data-theme="dark"] .date-chip:hover {
  border-color: #434343;
}

body[data-theme="dark"] .date-chip.active {
  background: var(--color-primary, #FF2442);
  border-color: var(--color-primary, #FF2442);
  color: #fff;
}

body[data-theme="dark"] .hot-search-title,
body[data-theme="dark"] .hot-search-text {
  color: rgba(255, 255, 255, 0.92);
}

body[data-theme="dark"] .hot-search-desc,
body[data-theme="dark"] .hot-search-heat {
  color: rgba(255, 255, 255, 0.55);
}

body[data-theme="dark"] .hot-search-section {
  background: #1f1f1f;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.2);
}

body[data-theme="dark"] .hot-search-current {
  background: #262626;
  color: rgba(255, 255, 255, 0.55);
}

body[data-theme="dark"] .platform-tab {
  background: #262626;
  color: rgba(255, 255, 255, 0.65);
}

body[data-theme="dark"] .platform-tab:hover {
  background: #303030;
}

body[data-theme="dark"] .hot-search-item {
  border-bottom-color: #262626;
}

body[data-theme="dark"] .hot-search-item:hover {
  background: #262626;
}

body[data-theme="dark"] .hot-search-trend.down {
  background: #262626;
  color: rgba(255, 255, 255, 0.55);
}

@media (max-width: 768px) {
  .hot-search-page {
    padding: 16px 12px;
  }

  .hot-search-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .hot-search-current {
    align-self: flex-start;
  }

  /* 日期 chips：已经 overflow-x:auto，但缺可视提示，加右侧渐隐 + 隐藏滚动条 */
  .date-bar {
    margin-left: -16px;
    margin-right: -16px;
    padding-left: 16px;
    padding-right: 16px;
    scrollbar-width: none;
    -webkit-mask-image: linear-gradient(to right, #000 0, #000 calc(100% - 24px), transparent 100%);
    mask-image: linear-gradient(to right, #000 0, #000 calc(100% - 24px), transparent 100%);
  }

  .date-bar::-webkit-scrollbar {
    display: none;
  }

  .date-chip {
    min-width: 60px;
    padding: 8px 12px;
  }

  .date-label,
  .date-value {
    font-size: 12px;
  }

  /* 平台 tabs：桌面默认 flex-wrap，移动端改为可横滑 */
  .platform-tabs {
    flex-wrap: nowrap;
    overflow-x: auto;
    margin-left: -16px;
    margin-right: -16px;
    padding-left: 16px;
    padding-right: 16px;
    scrollbar-width: none;
    -webkit-mask-image: linear-gradient(to right, #000 0, #000 calc(100% - 24px), transparent 100%);
    mask-image: linear-gradient(to right, #000 0, #000 calc(100% - 24px), transparent 100%);
  }

  .platform-tabs::-webkit-scrollbar {
    display: none;
  }

  .platform-tab {
    flex-shrink: 0;
    padding: 6px 12px;
    font-size: 12px;
  }

  /* 列表项：标题和热度数字压缩 */
  .hot-search-item {
    padding: 10px 12px;
    gap: 8px;
  }

  .hot-search-text {
    font-size: 13px;
  }

  .hot-search-heat {
    font-size: 11px;
    min-width: 56px;
  }
}
</style>
