<template>
  <div class="platform-screenshot-page">
    <a-card :bordered="false" class="list-card">
      <div class="list-header">
        <div>
          <h3 class="list-title">平台截图</h3>
          <p class="list-desc">1:1 复刻各平台创作者后台界面，修改左侧数据后可直接对手机区域截图使用。数据仅保存在浏览器本地。</p>
        </div>
        <a-button @click="handleReset">恢复默认</a-button>
      </div>

      <a-tabs v-model:activeKey="activeTab">
        <a-tab-pane key="toutiao" tab="今日头条" />
        <a-tab-pane key="wechat" tab="公众号" />
        <a-tab-pane key="baijiahao" tab="百家号" disabled />
        <a-tab-pane key="douyin" tab="抖音" disabled />
        <a-tab-pane key="xiaohongshu" tab="小红书" disabled />
        <a-tab-pane key="zhihu" tab="知乎" disabled />
      </a-tabs>

      <div v-if="activeTab === 'toutiao'" class="screenshot-body">
        <!-- 数据编辑面板 -->
        <div class="edit-panel">
          <a-form layout="vertical" :model="form">
            <a-form-item label="头像">
              <div class="avatar-edit">
                <img :src="form.avatar" class="avatar-preview" alt="头像" />
                <a-upload
                  :show-upload-list="false"
                  accept="image/*"
                  :before-upload="(file) => handleAvatarUpload(file, form)"
                >
                  <a-button size="small">上传头像</a-button>
                </a-upload>
                <a-button type="link" size="small" @click="form.avatar = DEFAULT_AVATAR">使用默认</a-button>
              </div>
            </a-form-item>
            <a-form-item label="昵称">
              <a-input v-model:value="form.nickname" placeholder="如 赚够1000w就退休" />
            </a-form-item>
            <a-form-item label="权益文案">
              <a-input v-model:value="form.benefitText" placeholder="如 创作权益" />
            </a-form-item>
            <a-row :gutter="12">
              <a-col :span="12">
                <a-form-item label="总阅读/播放">
                  <a-input v-model:value="form.totalRead" placeholder="如 133081" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="总粉丝">
                  <a-input v-model:value="form.totalFans" placeholder="如 105" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-form-item label="总收益(元)">
              <a-input v-model:value="form.totalIncome" placeholder="如 11.04" />
            </a-form-item>
            <a-row :gutter="12">
              <a-col :span="12">
                <a-form-item label="昨日阅读副文案">
                  <a-input v-model:value="form.yesterdayRead" placeholder="如 昨日 计算中" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="昨日粉丝副文案">
                  <a-input v-model:value="form.yesterdayFans" placeholder="如 昨日 计算中" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-form-item label="昨日收益副文案">
              <a-input v-model:value="form.yesterdayIncome" placeholder="如 昨日0" />
            </a-form-item>
            <a-row :gutter="12">
              <a-col :span="12">
                <a-form-item label="已获得加油包">
                  <a-input v-model:value="form.boostCount" placeholder="如 0" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="已获得现金(元)">
                  <a-input v-model:value="form.cashCount" placeholder="如 0.4" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-form-item label="公告文案（两行）">
              <a-textarea v-model:value="form.announcement" :rows="2" placeholder="每行一条公告" />
            </a-form-item>
            <a-divider style="margin: 8px 0" />
            <a-form-item label="任务一标题">
              <a-input v-model:value="form.task1Title" />
            </a-form-item>
            <a-form-item label="任务一描述">
              <a-input v-model:value="form.task1Desc" />
            </a-form-item>
            <a-form-item label="任务一奖励(元)">
              <a-input v-model:value="form.task1Reward" placeholder="如 0.3" />
            </a-form-item>
            <a-form-item label="任务二标题">
              <a-input v-model:value="form.task2Title" />
            </a-form-item>
            <a-form-item label="任务二描述">
              <a-input v-model:value="form.task2Desc" />
            </a-form-item>
            <a-form-item label="任务二奖励(元)">
              <a-input v-model:value="form.task2Reward" placeholder="如 0.2" />
            </a-form-item>
          </a-form>
        </div>

        <!-- 手机mockup：1:1 复刻今日头条创作者中心 -->
        <div class="phone-area">
          <div class="phone-frame">
            <div class="phone-screen">
              <!-- 状态栏 + 顶部导航 + 个人信息：浅蓝灰背景 -->
              <div class="top-section">
              <!-- 状态栏 -->
              <img class="statusbar-img" :src="toutiaoStatusbarImg" alt="状态栏" />

              <!-- 顶部导航 -->
              <div class="nav-bar">
                <svg class="nav-back" viewBox="0 0 24 24" fill="none" stroke="#1a1a1a" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M14.5 5.5L8 12l6.5 6.5" />
                </svg>
                <img class="nav-right-img" :src="navRightImg" alt="发布" />
              </div>

              <!-- 个人信息 -->
              <div class="profile-section">
                <div class="avatar-wrap">
                  <img :src="form.avatar" class="profile-avatar" alt="头像" />
                </div>
                <div class="profile-info">
                  <div class="profile-nickname">{{ form.nickname }}</div>
                  <div class="profile-benefit">{{ form.benefitText }} ›</div>
                </div>
              </div>
              </div>

              <!-- 数据统计 -->
              <div class="stats-row">
                <div class="stat-item">
                  <div class="stat-label">总阅读/播放</div>
                  <div class="stat-value">{{ formatNum(form.totalRead) }}</div>
                  <div class="stat-sub">{{ form.yesterdayRead }}</div>
                </div>
                <div class="stat-item">
                  <div class="stat-label">总粉丝</div>
                  <div class="stat-value">{{ formatNum(form.totalFans) }}</div>
                  <div class="stat-sub">{{ form.yesterdayFans }}</div>
                </div>
                <div class="stat-item">
                  <div class="stat-label">总收益(元)</div>
                  <div class="stat-value">{{ form.totalIncome }}</div>
                  <div class="stat-sub">{{ form.yesterdayIncome }}</div>
                </div>
              </div>

              <!-- 服务入口 -->
              <div class="services-row">
                <img class="services-img" :src="servicesImg" alt="服务入口" />
              </div>

              <!-- 公告 -->
              <div class="announce-section">
                <div class="announce-line" v-for="(line, idx) in announcementLines" :key="idx">
                  <span class="announce-icon"></span>
                  <span class="announce-text">{{ line }}</span>
                  <span v-if="idx === 0" class="announce-more">更多 ›</span>
                </div>
              </div>

              <!-- 任务区 -->
              <div class="task-section">
                <div class="task-tabs">
                  <span class="task-tab active">激励任务</span>
                  <span class="task-tab inactive">活动广场</span>
                </div>
                <div class="task-nums">
                  <div class="task-num-item">
                    <div class="task-num red">{{ form.boostCount }}</div>
                    <div class="task-num-label">已获得加油包</div>
                  </div>
                  <div class="task-num-item">
                    <div class="task-num red">{{ form.cashCount }}</div>
                    <div class="task-num-label">已获得现金 (元)</div>
                  </div>
                </div>
                <div class="task-tip">审核通过后，即可获得任务奖励，任务每日0点更新</div>
                <div class="task-card">
                  <div class="task-card-main">
                    <div class="task-title">{{ form.task1Title }}</div>
                    <div class="task-desc" v-html="highlightReward(form.task1Desc, form.task1Reward)"></div>
                  </div>
                  <span class="task-btn">去完成</span>
                </div>
                <div class="task-card">
                  <div class="task-card-main">
                    <div class="task-title">{{ form.task2Title }}</div>
                    <div class="task-desc" v-html="highlightReward(form.task2Desc, form.task2Reward)"></div>
                  </div>
                  <span class="task-btn">去完成</span>
                </div>
              </div>
            </div>
          </div>
          <p class="phone-hint">可直接对上方手机区域截图；手机内容支持上下滚动到目标位置。</p>
        </div>
      </div>

      <!-- 公众号：创作者中心「我」页 -->
      <div v-else-if="activeTab === 'wechat'" class="screenshot-body">
        <div class="edit-panel">
          <a-form layout="vertical" :model="wechatForm">
            <a-form-item label="头像">
              <div class="avatar-edit">
                <img :src="wechatForm.avatar" class="avatar-preview" alt="头像" />
                <a-upload
                  :show-upload-list="false"
                  accept="image/*"
                  :before-upload="(file) => handleAvatarUpload(file, wechatForm)"
                >
                  <a-button size="small">上传头像</a-button>
                </a-upload>
                <a-button type="link" size="small" @click="wechatForm.avatar = DEFAULT_AVATAR">使用默认</a-button>
              </div>
            </a-form-item>
            <a-form-item label="昵称">
              <a-input v-model:value="wechatForm.nickname" placeholder="如 赚够1000w就退休" />
            </a-form-item>
            <a-form-item label="个性签名">
              <a-input v-model:value="wechatForm.bio" placeholder="如 以提升认知为过渡目标…" />
            </a-form-item>
            <a-row :gutter="12">
              <a-col :span="12">
                <a-form-item label="关注我的人">
                  <a-input v-model:value="wechatForm.followers" placeholder="如 539" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="昨日阅读">
                  <a-input v-model:value="wechatForm.yesterdayRead" placeholder="如 1" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="12">
              <a-col :span="12">
                <a-form-item label="昨日分享">
                  <a-input v-model:value="wechatForm.yesterdayShare" placeholder="如 0" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="广告昨日收入(元)">
                  <a-input v-model:value="wechatForm.adIncome" placeholder="如 0.00" />
                </a-form-item>
              </a-col>
            </a-row>
          </a-form>
        </div>

        <div class="phone-area">
          <div class="phone-frame">
            <div class="oa-screen">
              <div class="oa-body">
                <!-- 头部：浅蓝渐变背景 -->
                <div class="oa-header">
                  <!-- 状态栏 -->
                  <img class="statusbar-img oa-statusbar" :src="oaStatusbarImg" alt="状态栏" />

                  <div class="oa-settings">
                    <img class="oa-settings-img" :src="oaSettingsImg" alt="设置" />
                  </div>

                  <div class="oa-profile">
                    <img :src="wechatForm.avatar" class="oa-avatar" alt="头像" />
                    <span class="oa-nickname">{{ wechatForm.nickname }} ›</span>
                  </div>
                  <div class="oa-bio">{{ wechatForm.bio }}</div>
                </div>

                <!-- 数据统计卡片 -->
                <div class="oa-card oa-stats">
                  <div class="oa-stat-item">
                    <div class="oa-stat-num">{{ wechatForm.followers }}</div>
                    <div class="oa-stat-label">关注我的人 ›</div>
                  </div>
                  <div class="oa-stat-divider"></div>
                  <div class="oa-stat-item">
                    <div class="oa-stat-num">{{ wechatForm.yesterdayRead }}</div>
                    <div class="oa-stat-label">昨日阅读 ›</div>
                  </div>
                  <div class="oa-stat-divider"></div>
                  <div class="oa-stat-item">
                    <div class="oa-stat-num">{{ wechatForm.yesterdayShare }}</div>
                    <div class="oa-stat-label">昨日分享 ›</div>
                  </div>
                </div>

                <!-- 功能入口卡片 -->
                <div class="oa-card oa-services">
                  <img class="oa-services-img" :src="oaServicesRow1Img" alt="功能入口" />
                  <div class="oa-service-sub">昨日收入 ¥{{ wechatForm.adIncome }}</div>
                  <img class="oa-services-img" :src="oaServicesRow2Img" alt="功能入口" />
                </div>
              </div>

              <!-- 底部 TabBar -->
              <div class="oa-tabbar">
                <img class="oa-tabbar-img" :src="oaTabbarImg" alt="底部导航" />
              </div>
            </div>
          </div>
          <p class="phone-hint">可直接对上方手机区域截图；手机内容支持上下滚动到目标位置。</p>
        </div>
      </div>

      <a-empty v-else description="该平台截图模板即将上线，敬请期待" style="padding: 80px 0" />
    </a-card>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import servicesImg from '@/assets/platform/toutiao-services.png'
import navRightImg from '@/assets/platform/toutiao-nav-right.png'
import oaServicesRow1Img from '@/assets/platform/oa-services-row1.png'
import oaServicesRow2Img from '@/assets/platform/oa-services-row2.png'
import oaTabbarImg from '@/assets/platform/oa-tabbar.png'
import oaSettingsImg from '@/assets/platform/oa-settings.png'
import oaStatusbarImg from '@/assets/platform/oa-statusbar.png'
import toutiaoStatusbarImg from '@/assets/platform/toutiao-statusbar.png'

const STORAGE_KEY = 'admin_platform_screenshot_toutiao'
const STORAGE_KEY_WECHAT = 'admin_platform_screenshot_wechat'

const DEFAULT_AVATAR =
  'data:image/svg+xml;utf8,' +
  encodeURIComponent(
    '<svg xmlns="http://www.w3.org/2000/svg" width="120" height="120"><rect width="120" height="120" rx="60" fill="#e8e8e8"/><circle cx="60" cy="46" r="20" fill="#bdbdbd"/><ellipse cx="60" cy="92" rx="32" ry="22" fill="#bdbdbd"/></svg>'
  )

const defaults = {
  avatar: DEFAULT_AVATAR,
  nickname: '赚够1000w就退休',
  benefitText: '创作权益',
  totalRead: '133081',
  totalFans: '105',
  totalIncome: '11.04',
  yesterdayRead: '昨日 计算中',
  yesterdayFans: '昨日 计算中',
  yesterdayIncome: '昨日0',
  boostCount: '0',
  cashCount: '0.4',
  announcement: '7日头条优质账号专项治理公告（…\n今日头条低质账号专项治理公告（…',
  task1Title: '发日常微头条笔记',
  task1Desc: '>15字且带图和定位，得',
  task1Reward: '0.3',
  task2Title: '特效视频投稿',
  task2Desc: '3秒以上并带特效，得',
  task2Reward: '0.2'
}

const wechatDefaults = {
  avatar: DEFAULT_AVATAR,
  nickname: '赚够1000w就退休',
  bio: '以提升认知为过渡目标，以赚米为终极目标',
  followers: '539',
  yesterdayRead: '1',
  yesterdayShare: '0',
  adIncome: '0.00'
}

function loadForm() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? { ...defaults, ...JSON.parse(raw) } : { ...defaults }
  } catch (e) {
    return { ...defaults }
  }
}

function loadWechatForm() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY_WECHAT)
    return raw ? { ...wechatDefaults, ...JSON.parse(raw) } : { ...wechatDefaults }
  } catch (e) {
    return { ...wechatDefaults }
  }
}

const activeTab = ref('toutiao')
const form = reactive(loadForm())
const wechatForm = reactive(loadWechatForm())

watch(
  form,
  (val) => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(val))
  },
  { deep: true }
)

watch(
  wechatForm,
  (val) => {
    localStorage.setItem(STORAGE_KEY_WECHAT, JSON.stringify(val))
  },
  { deep: true }
)

const announcementLines = computed(() =>
  String(form.announcement || '')
    .split('\n')
    .map((s) => s.trim())
    .filter(Boolean)
)

function formatNum(val) {
  const n = Number(String(val).replace(/,/g, ''))
  if (Number.isNaN(n)) return val
  return n.toLocaleString('en-US')
}

function highlightReward(desc, reward) {
  const safeDesc = String(desc || '').replace(/</g, '&lt;')
  const safeReward = String(reward || '').replace(/</g, '&lt;')
  return `${safeDesc}<span class="red">${safeReward}元</span>`
}

function handleAvatarUpload(file, target) {
  const reader = new FileReader()
  reader.onload = (e) => {
    const img = new Image()
    img.onload = () => {
      const canvas = document.createElement('canvas')
      const size = 160
      canvas.width = size
      canvas.height = size
      const ctx = canvas.getContext('2d')
      const min = Math.min(img.width, img.height)
      ctx.drawImage(img, (img.width - min) / 2, (img.height - min) / 2, min, min, 0, 0, size, size)
      target.avatar = canvas.toDataURL('image/jpeg', 0.85)
    }
    img.src = e.target.result
  }
  reader.readAsDataURL(file)
  return false
}

function handleReset() {
  Object.assign(form, defaults)
  Object.assign(wechatForm, wechatDefaults)
}
</script>

<style scoped>
.platform-screenshot-page {
  padding: 16px;
}

.list-card {
  background: #fff;
  border-radius: 8px;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 4px;
}

.list-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
}

.list-desc {
  color: #86909c;
  font-size: 13px;
  margin: 6px 0 0;
}

.screenshot-body {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

/* 左侧编辑面板 */
.edit-panel {
  width: 320px;
  flex-shrink: 0;
  max-height: calc(100vh - 260px);
  overflow-y: auto;
  padding-right: 8px;
}

.avatar-edit {
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar-preview {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid #e5e6eb;
}

/* 右侧手机区域 */
.phone-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.phone-frame {
  width: 390px;
  border-radius: 36px;
  border: 10px solid #1d2129;
  background: #fff;
  overflow: hidden;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.18);
}

.phone-screen {
  height: 760px;
  overflow-y: auto;
  background: #fff;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Helvetica Neue', sans-serif;
  color: #222;
  scrollbar-width: none;
}

.phone-screen::-webkit-scrollbar {
  display: none;
}

.phone-hint {
  color: #86909c;
  font-size: 12px;
  margin-top: 12px;
}

.red {
  color: #f04142;
}

.statusbar-img {
  width: 100%;
  display: block;
}

.oa-statusbar {
  padding: 6px 8px 0;
}

/* 顶部区域：状态栏 + 导航 + 个人信息，浅蓝灰背景 */
.top-section {
  background: #f6f8fe;
}

/* 顶部导航 */
.nav-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 16px 14px;
}

.nav-back {
  width: 26px;
  height: 26px;
}

.nav-right-img {
  height: 30px;
  display: block;
}

/* 个人信息 */
.profile-section {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 8px 18px 20px;
  margin-bottom: 10px;
}

.avatar-wrap {
  width: 62px;
  height: 62px;
  flex-shrink: 0;
}

.profile-avatar {
  width: 62px;
  height: 62px;
  border-radius: 50%;
  object-fit: cover;
  display: block;
}

.profile-nickname {
  font-size: 18px;
  line-height: 1.3;
}

.profile-benefit {
  font-size: 12px;
  color: #86909c;
  margin-top: 6px;
}

/* 数据统计 */
.stats-row {
  display: flex;
  padding: 0 10px 20px;
}

.stat-item {
  flex: 1;
  text-align: center;
}

.stat-label {
  font-size: 12px;
  color: #86909c;
}

.stat-value {
  font-size: 20px;
  margin: 8px 0 6px;
  font-variant-numeric: tabular-nums;
}

.stat-sub {
  font-size: 11px;
  color: #86909c;
  line-height: 1.4;
}

/* 服务入口 */
.services-row {
  background: #fff;
  padding: 4px 10px 18px;
  border-bottom: 6px solid #f7f8fa;
}

.services-img {
  width: 100%;
  display: block;
}

/* 公告：复刻截图中略微上滑、文字被裁切一半的状态 */
.announce-section {
  padding: 14px 16px 16px;
  border-bottom: 6px solid #f7f8fa;
}

.announce-line {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  height: 15px;
  overflow: hidden;
}

.announce-icon {
  width: 14px;
  height: 14px;
  border: 1.6px solid #1a1a1a;
  border-radius: 3px;
  flex-shrink: 0;
  position: relative;
  transform: translateY(-6px);
}

.announce-icon::after {
  content: '';
  position: absolute;
  left: 2px;
  right: 2px;
  top: 4px;
  height: 1.5px;
  background: #1a1a1a;
  box-shadow: 0 3px 0 #1a1a1a;
}

.announce-text {
  font-size: 13px;
  color: #1d2129;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
  line-height: 22px;
  transform: translateY(-8px);
}

.announce-more {
  font-size: 12px;
  color: #86909c;
  flex-shrink: 0;
  line-height: 22px;
  transform: translateY(-8px);
}

/* 任务区 */
.task-section {
  padding: 32px 16px 24px;
}

.task-tabs {
  display: flex;
  gap: 24px;
  margin-bottom: 22px;
}

.task-tab {
  font-size: 15px;
}

.task-tab.active {
  color: #1d2129;
  position: relative;
}

.task-tab.active::after {
  content: '';
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  bottom: -6px;
  width: 22px;
  height: 3px;
  border-radius: 2px;
  background: #f04142;
}

.task-tab.inactive {
  color: #4e5969;
  background: #f2f3f5;
  padding: 4px 14px;
  border-radius: 16px;
  font-size: 13px;
}

.task-nums {
  display: flex;
  gap: 48px;
  padding-left: 8px;
}

.task-num {
  font-size: 24px;
}

.task-num-label {
  font-size: 12px;
  color: #4e5969;
  margin-top: 8px;
}

.task-tip {
  font-size: 11px;
  color: #86909c;
  margin: 22px 0 6px;
}

.task-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 0;
}

.task-title {
  font-size: 16px;
}

.task-desc {
  font-size: 13px;
  color: #4e5969;
  margin-top: 7px;
}

.task-btn {
  flex-shrink: 0;
  background: #f2f3f5;
  color: #1d2129;
  font-size: 13px;
  padding: 7px 16px;
  border-radius: 18px;
}

/* ===== 公众号创作者中心 ===== */
.oa-screen {
  height: 760px;
  display: flex;
  flex-direction: column;
  background: #f6f8f7;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Helvetica Neue', sans-serif;
  color: #1a1a1a;
}

.oa-body {
  flex: 1;
  overflow-y: auto;
  scrollbar-width: none;
}

.oa-body::-webkit-scrollbar {
  display: none;
}

/* 头部浅蓝渐变背景 */
.oa-header {
  background: linear-gradient(180deg, #eff7f9 55%, #f6f8f7 100%);
  padding-bottom: 14px;
}

.oa-settings {
  display: flex;
  justify-content: flex-end;
  padding: 16px 16px 0;
}

.oa-settings-img {
  height: 24px;
  display: block;
}

.oa-profile {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 10px 20px 0;
}

.oa-avatar {
  width: 74px;
  height: 74px;
  border-radius: 50%;
  object-fit: cover;
  display: block;
}

.oa-nickname {
  font-size: 18px;
}

.oa-bio {
  font-size: 13px;
  color: #3d4a44;
  padding: 14px 20px 0;
}

/* 白色卡片 */
.oa-card {
  background: #fff;
  border-radius: 12px;
  margin: 12px;
}

/* 数据统计 */
.oa-stats {
  display: flex;
  align-items: center;
  padding: 18px 8px;
}

.oa-stat-item {
  flex: 1;
  text-align: center;
}

.oa-stat-num {
  font-size: 21px;
  color: #1b362d;
  font-variant-numeric: tabular-nums;
}

.oa-stat-label {
  font-size: 14px;
  color: #1a1a1a;
  margin-top: 8px;
}

.oa-stat-divider {
  width: 1px;
  height: 40px;
  background: #eef0ef;
}

/* 功能入口 */
.oa-services {
  padding: 20px 10px 16px;
}

.oa-services-img {
  width: 100%;
  display: block;
}

.oa-service-sub {
  width: 33.33%;
  text-align: center;
  font-size: 11px;
  color: #a8adb3;
  margin: 6px 0 10px;
}

/* 底部 TabBar */
.oa-tabbar {
  background: #fff;
  border-top: 1px solid #eceeee;
}

.oa-tabbar-img {
  width: 100%;
  display: block;
}
</style>
