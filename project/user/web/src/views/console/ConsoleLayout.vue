<template>
  <div class="console-layout">
    <!-- 侧边栏 -->
    <aside class="console-sidebar">
      <div class="console-sidebar-brand">
        <img
          :src="currentTheme === 'dark' ? 'https://foruda.gitee.com/images/1782816881530259552/332b2985_8060302.png' : 'https://foruda.gitee.com/images/1782805324201637771/ee4f5810_8060302.png'"
          alt="爱创作"
          class="brand-logo"
        />
        <span class="brand-name">爱创作</span>
      </div>
      <nav class="console-sidebar-nav">
        <router-link
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="console-sidebar-item"
          :class="{ active: isActive(item.path) }"
        >
          <component :is="item.icon" class="nav-icon" />
          <span class="nav-label">{{ item.label }}</span>
        </router-link>
      </nav>
    </aside>

    <!-- 主内容区 -->
    <div class="console-main">
      <!-- 顶部栏 -->
      <header class="console-header">
        <div class="header-left">
        </div>

        <div class="header-right">
          <!-- 消息弹框 -->
          <a-modal
            v-model:open="notifVisible"
            :footer="null"
            :width="640"
            centered
            class="notif-modal"
          >
            <div class="notif-panel">
              <!-- 面板头部 -->
              <div class="notif-header">
                <span class="notif-title">消息中心</span>
                <button
                  v-if="unreadCount > 0"
                  class="notif-read-all"
                  @click="markAllRead"
                >
                  全部已读
                </button>
              </div>

              <!-- Tab 栏 -->
              <div class="notif-tabs">
                <button
                  v-for="tab in notifTabs"
                  :key="tab.type"
                  :class="['notif-tab', { active: activeTab === tab.type }]"
                  @click="switchTab(tab.type)"
                >
                  {{ tab.label }}
                  <span v-if="getUnreadByType(tab.type) > 0" class="notif-tab-badge">
                    {{ getUnreadByType(tab.type) }}
                  </span>
                </button>
              </div>

              <!-- 消息列表 -->
              <div class="notif-list">
                <div v-if="currentNotifs.length === 0" class="notif-empty">
                  <a-empty :description="`暂无 ${activeTabLabel} 消息`" />
                </div>
                <div
                  v-for="n in currentNotifs"
                  :key="n.id"
                  :class="['notif-item', { unread: !n.read }]"
                  @click="handleNotifClick(n)"
                >
                  <div class="notif-item-dot" v-if="!n.read"></div>
                  <div class="notif-item-body">
                    <div class="notif-item-title">{{ n.title }}</div>
                    <div class="notif-item-summary">{{ n.summary }}</div>
                    <div class="notif-item-time">{{ formatTime(n.createdAt) }}</div>
                  </div>
                </div>
              </div>
            </div>
          </a-modal>

          <!-- 消息铃铛 -->
          <div class="bell-wrap">
            <a-tooltip title="消息">
              <button class="console-icon-btn bell-btn" @click="notifVisible = true">
                <svg class="console-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9"/>
                  <path d="M10.3 21a1.94 1.94 0 0 0 3.4 0"/>
                </svg>
                <span v-if="unreadCount > 0" class="bell-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
              </button>
            </a-tooltip>
          </div>

          <!-- 教程下拉 -->
          <a-dropdown
            v-model:open="tutorialVisible"
            :trigger="['click']"
            placement="bottomRight"
          >
            <a-tooltip title="教程">
              <button class="console-icon-btn">
                <svg class="console-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"/>
                  <path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"/>
                </svg>
              </button>
            </a-tooltip>
            <template #overlay>
              <div class="tutorial-panel">
                <div class="tutorial-item" @click="handleTutorial('doc')">
                  <div class="tutorial-icon">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                      <polyline points="14 2 14 8 20 8"/>
                      <line x1="16" y1="13" x2="8" y2="13"/>
                      <line x1="16" y1="17" x2="8" y2="17"/>
                      <polyline points="10 9 9 9 8 9"/>
                    </svg>
                  </div>
                  <div class="tutorial-body">
                    <div class="tutorial-name">帮助 / 文档</div>
                    <div class="tutorial-desc">从基础到专业技巧的快速指南，助你充分利用爱创作的功能。</div>
                  </div>
                </div>
                <div class="tutorial-item" @click="tutorialVisible = false; openWechatModal()">
                  <div class="tutorial-icon">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
                    </svg>
                  </div>
                  <div class="tutorial-body">
                    <div class="tutorial-name">加入微信交流群</div>
                    <div class="tutorial-desc">一个充满活力的作者网络，提供帮助的渠道，分享创作技巧、经验和最佳实践。</div>
                  </div>
                </div>
              </div>
            </template>
          </a-dropdown>
          <!-- 反馈下拉 -->
          <a-dropdown
            v-model:open="feedbackVisible"
            :trigger="['click']"
            placement="bottomRight"
          >
            <a-tooltip title="反馈">
              <button class="console-icon-btn">
                <svg class="console-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
                </svg>
              </button>
            </a-tooltip>
            <template #overlay>
              <div class="feedback-panel">
                <div class="feedback-title">意见反馈</div>
                <div class="feedback-type">
                  <label class="feedback-label">反馈类型</label>
                  <div class="feedback-type-btns">
                    <button
                      v-for="t in feedbackTypes"
                      :key="t"
                      :class="['type-btn', { active: feedbackType === t }]"
                      @click="feedbackType = t"
                    >
                      {{ t }}
                    </button>
                  </div>
                </div>
                <div class="feedback-content">
                  <label class="feedback-label">反馈内容</label>
                  <textarea
                    v-model="feedbackContent"
                    class="feedback-textarea"
                    placeholder="请详细描述你的问题或建议..."
                    rows="4"
                  ></textarea>
                </div>
                <button class="feedback-submit" @click="submitFeedback">提交反馈</button>
              </div>
            </template>
          </a-dropdown>
          <!-- 关于我们下拉 -->
          <a-dropdown
            v-model:open="aboutVisible"
            :trigger="['click']"
            placement="bottomRight"
          >
            <a-tooltip title="关于我们">
              <button class="console-icon-btn">
                <svg class="console-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="12" cy="12" r="10"/>
                  <path d="M12 16v-4"/>
                  <path d="M12 8h.01"/>
                </svg>
              </button>
            </a-tooltip>
            <template #overlay>
              <div class="about-panel">
                <div class="about-header">
                  <div class="about-logo">
                    <img src="https://foruda.gitee.com/images/1782805324201637771/ee4f5810_8060302.png" alt="爱创作" />
                  </div>
                  <div class="about-brand">
                    <div class="about-name">爱创作</div>
                    <div class="about-tagline">创作者灵感旅程中的同行者</div>
                  </div>
                </div>
                <div class="about-desc">
                  <p>爱创作希望成为创作者灵感旅程中的同行者。我们希望让写作不再被"文笔"所限制，哪怕不擅长表达的人，也能把脑海里的想法顺利写出来。</p>
                  <p>AI 在这里不是替代者，而是帮助作者整理思路、激发灵感、拓展想象的辅助工具。</p>
                  <p>我们珍惜每一位作者投入在作品里的情绪、时间与热爱，也尊重原创应有的价值。</p>
                </div>
                <div class="about-links">
                  <button class="about-link-btn" @click="openTermsModal">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                      <polyline points="14 2 14 8 20 8"/>
                    </svg>
                    用户协议
                  </button>
                  <button class="about-link-btn" @click="openPrivacyModal">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
                    </svg>
                    隐私政策
                  </button>
                  <button class="about-link-btn" @click="openWechatModal">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
                    </svg>
                    关注微信
                  </button>
                </div>
                <div class="about-footer">
                  © 2026 爱创作 · All Rights Reserved
                </div>
              </div>
            </template>
          </a-dropdown>
          <a-tooltip title="官网">
            <a href="http://localhost:28585/.superpowers/brainstorm/6491-1782131242/content/index.html" target="_blank" class="console-icon-btn">
              <svg class="console-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"/>
                <path d="M12 2a14.5 14.5 0 0 0 0 20 14.5 14.5 0 0 0 0-20"/>
                <path d="M2 12h20"/>
              </svg>
            </a>
          </a-tooltip>
          <span
            :class="['console-membership-badge', { 'has-membership': hasMembership, 'no-membership': !hasMembership }]"
            @click="handleMembershipClick"
          >
            {{ hasMembership ? membershipLevel : '开通会员' }}
          </span>
          <!-- 个人中心下拉 -->
          <a-dropdown
            v-model:open="userCenterVisible"
            :trigger="['click']"
            placement="bottomRight"
          >
            <div class="console-avatar">U</div>
            <template #overlay>
              <div class="user-center-panel">
                <!-- 会员卡 -->
                <div class="membership-card" @click="router.push('/pricing')">
                  <div class="membership-left">
                    <div class="membership-label">当前会员</div>
                    <div class="membership-name">{{ hasMembership ? membershipLevel : '免费版' }}</div>
                    <div class="membership-expiry" v-if="hasMembership">有效期至 2026-12-31</div>
                  </div>
                  <div class="membership-right">
                    <button class="membership-btn">{{ hasMembership ? '续费' : '开通' }}</button>
                  </div>
                </div>

                <!-- 账号信息 -->
                <div class="user-section">
                  <div class="user-section-title">账号信息</div>
                  <div class="user-row">
                    <span class="user-row-label">用户ID</span>
                    <span class="user-row-value">88886666</span>
                  </div>
                  <div class="user-row" @click="openProfileModal">
                    <span class="user-row-label">昵称</span>
                    <span class="user-row-value user-row-edit">爱创作用户 <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg></span>
                  </div>
                  <div class="user-row" @click="openEmailModal">
                    <span class="user-row-label">邮箱</span>
                    <span class="user-row-value user-row-edit">user@example.com <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg></span>
                  </div>
                  <div class="user-row">
                    <span class="user-row-label">本月已生成</span>
                    <span class="user-row-value">12 篇</span>
                  </div>
                </div>

                <!-- 快捷操作 -->
                <div class="user-section">
                  <div class="user-section-title">设置</div>
                  <div class="user-action" @click="openPasswordModal">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                      <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                      <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
                    </svg>
                    修改密码
                  </div>
                  <div class="user-action user-action-logout" @click="handleLogout">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
                      <polyline points="16 17 21 12 16 7"/>
                      <line x1="21" y1="12" x2="9" y2="12"/>
                    </svg>
                    退出登录
                  </div>
                </div>
              </div>
            </template>
          </a-dropdown>
        </div>
      </header>

      <!-- 内容区 -->
      <div class="console-content">
        <router-view />
      </div>

      <!-- 底部 -->
      <footer class="console-footer">
        <span>© 2026 爱创作 · 杭州爱启云网络科技有限公司 · All Rights Reserved</span>
        <span>浙ICP备XXXXXXXX号-1</span>
      </footer>
    </div>
  </div>

  <!-- 用户协议弹框 -->
  <a-modal
    v-model:open="termsVisible"
    title="用户协议"
    :footer="null"
    :width="640"
    centered
  >
    <div class="terms-content">
      <p>欢迎使用爱创作服务（以下简称"本服务"）。在您使用本服务之前，请仔细阅读本用户协议。</p>
      <h4>一、服务说明</h4>
      <p>爱创作是一款 AI 自媒体写作助手，通过人工智能技术帮助用户生成文章内容。</p>
      <h4>二、账号注册</h4>
      <p>您在使用本服务前需要注册账号。您承诺提供真实、准确、完整的注册信息，并及时更新。</p>
      <h4>三、使用规范</h4>
      <p>您承诺不会利用本服务从事任何违法活动，包括但不限于：</p>
      <ul>
        <li>发布违反法律法规的内容</li>
        <li>侵犯他人知识产权的内容</li>
        <li>利用 AI 进行内容搬运、洗稿、恶意拼接等行为</li>
      </ul>
      <h4>四、知识产权</h4>
      <p>本服务生成的 AI 内容，仅作为创作过程中的参考与辅助，相关输出并不代表平台立场或价值观点。</p>
      <h4>五、免责声明</h4>
      <p>因使用本服务产生的任何直接或间接损失，由用户自行承担。</p>
      <h4>六、协议更新</h4>
      <p>本协议内容如有更新，平台将提前通知用户，更新后的协议自公布之日起生效。</p>
    </div>
  </a-modal>

  <!-- 隐私政策弹框 -->
  <a-modal
    v-model:open="privacyVisible"
    title="隐私政策"
    :footer="null"
    :width="640"
    centered
  >
    <div class="terms-content">
      <p>我们非常重视您的个人隐私保护，在您使用爱创作服务时，我们会按照本隐私政策的规定收集、使用、存储和保护您的个人信息。</p>
      <h4>一、信息收集</h4>
      <p>我们收集的信息包括：账号信息（邮箱）、创作内容、使用记录等。</p>
      <h4>二、信息使用</h4>
      <p>我们使用收集的信息用于：提供和改进服务、发送通知、账号安全保护等。</p>
      <h4>三、信息共享</h4>
      <p>未经您同意，我们不会与任何第三方共享您的个人信息，法律法规要求的除外。</p>
      <h4>四、信息存储</h4>
      <p>您的信息将存储在中华人民共和国境内的服务器上。</p>
      <h4>五、联系我们</h4>
      <p>如您对隐私政策有任何疑问，请通过官方渠道与我们联系。</p>
    </div>
  </a-modal>

  <!-- 客服微信弹框 -->
  <a-modal
    v-model:open="wechatVisible"
    title="客服微信"
    :footer="null"
    :width="400"
    centered
  >
    <div class="wechat-modal-content">
      <img
        class="wechat-qr-large"
        src="https://foruda.gitee.com/images/1782817803473013600/4f94eac9_8060302.png"
        alt="客服微信"
      />
      <p class="wechat-hint">扫码添加客服微信</p>
    </div>
  </a-modal>

  <!-- 修改密码弹框 -->
  <a-modal
    v-model:open="passwordVisible"
    title="修改密码"
    :footer="null"
    :width="400"
    centered
  >
    <div class="password-modal-content">
      <div class="password-item">
        <label class="password-label">当前密码</label>
        <input
          v-model="passwordForm.oldPassword"
          type="password"
          class="password-input"
          placeholder="请输入当前密码"
        />
      </div>
      <div class="password-item">
        <label class="password-label">新密码</label>
        <input
          v-model="passwordForm.newPassword"
          type="password"
          class="password-input"
          placeholder="6-20 位新密码"
        />
      </div>
      <div class="password-item">
        <label class="password-label">确认新密码</label>
        <input
          v-model="passwordForm.confirmPassword"
          type="password"
          class="password-input"
          placeholder="再次输入新密码"
        />
      </div>
      <button class="password-submit" @click="handlePasswordSubmit">确认修改</button>
    </div>
  </a-modal>

  <!-- 修改昵称弹框 -->
  <a-modal
    v-model:open="profileVisible"
    title="修改昵称"
    :footer="null"
    :width="400"
    centered
  >
    <div class="profile-modal-content">
      <div class="profile-item">
        <label class="profile-label">昵称</label>
        <input
          v-model="profileForm.nickname"
          type="text"
          class="profile-input"
          placeholder="请输入昵称"
          maxlength="20"
        />
      </div>
      <button class="profile-submit" @click="handleProfileSubmit">保存</button>
    </div>
  </a-modal>

  <!-- 修改邮箱弹框 -->
  <a-modal
    v-model:open="emailVisible"
    title="修改邮箱"
    :footer="null"
    :width="400"
    centered
  >
    <div class="email-modal-content">
      <div class="email-item">
        <label class="email-label">新邮箱</label>
        <input
          v-model="emailForm.email"
          type="email"
          class="email-input"
          placeholder="请输入新邮箱"
        />
      </div>
      <div class="email-item">
        <label class="email-label">验证码</label>
        <div class="email-code-row">
          <input
            v-model="emailForm.code"
            type="text"
            class="email-input email-code-input"
            placeholder="输入 6 位验证码"
          />
          <button
            class="email-code-btn"
            :disabled="codeCountdown > 0"
            @click="sendEmailCode"
          >
            {{ codeCountdown > 0 ? `${codeCountdown}s` : '获取验证码' }}
          </button>
        </div>
      </div>
      <button class="email-submit" @click="handleEmailSubmit">保存</button>
    </div>
  </a-modal>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  EditOutlined,
  LoadingOutlined,
  EyeOutlined,
  FolderOutlined
} from '@ant-design/icons-vue'

const route = useRoute()
const router = useRouter()

const navItems = [
  { path: '/console/create', label: '创作', icon: EditOutlined },
  { path: '/console/works', label: '我的作品', icon: FolderOutlined }
]

const isActive = (path) => {
  return route.path === path || route.path.startsWith(path + '/')
}

// ---------- 主题切换 ----------
const THEME_KEY = 'aichuangzuo_theme'
const currentTheme = ref('light')

const toggleTheme = () => {
  const next = currentTheme.value === 'light' ? 'dark' : 'light'
  currentTheme.value = next
  document.body.setAttribute('data-theme', next)
  localStorage.setItem(THEME_KEY, next)
}

const loadTheme = () => {
  const saved = localStorage.getItem(THEME_KEY) || 'light'
  currentTheme.value = saved
  document.body.setAttribute('data-theme', saved)
}

// ---------- 消息通知 ----------
const STORAGE_KEY = 'aichuangzuo_notifications'
const notifVisible = ref(false)
const activeTab = ref('generation')
const notifications = ref([])

// ---------- 教程 ----------
const tutorialVisible = ref(false)

const handleTutorial = (type) => {
  tutorialVisible.value = false
  // TODO: 跳转对应页面
  console.log('教程入口:', type)
}

// ---------- 反馈 ----------
const feedbackVisible = ref(false)
const feedbackType = ref('功能建议')
const feedbackTypes = ['功能建议', '问题反馈', '其他']
const feedbackContent = ref('')

const submitFeedback = () => {
  if (!feedbackContent.value.trim()) return
  console.log('反馈:', { type: feedbackType.value, content: feedbackContent.value })
  feedbackContent.value = ''
  feedbackType.value = '功能建议'
  feedbackVisible.value = false
}

// ---------- 关于我们 ----------
const aboutVisible = ref(false)

const handleAboutLink = (type) => {
  aboutVisible.value = false
  console.log('关于链接:', type)
}

// ---------- 个人中心 ----------
const userCenterVisible = ref(false)

const openPasswordModal = () => {
  userCenterVisible.value = false
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordVisible.value = true
}

const termsVisible = ref(false)
const privacyVisible = ref(false)
const wechatVisible = ref(false)
const passwordVisible = ref(false)
const profileVisible = ref(false)
const emailVisible = ref(false)

const profileForm = reactive({
  nickname: '爱创作用户'
})

const emailForm = reactive({
  email: 'user@example.com',
  code: ''
})

const codeCountdown = ref(0)
let countdownTimer = null

const sendEmailCode = () => {
  if (codeCountdown.value > 0) return
  codeCountdown.value = 60
  countdownTimer = setInterval(() => {
    codeCountdown.value--
    if (codeCountdown.value <= 0) {
      clearInterval(countdownTimer)
    }
  }, 1000)
}

const openProfileModal = () => {
  userCenterVisible.value = false
  profileForm.nickname = '爱创作用户'
  profileVisible.value = true
}

const openEmailModal = () => {
  userCenterVisible.value = false
  emailForm.email = 'user@example.com'
  emailForm.code = ''
  emailVisible.value = true
}

const handleProfileSubmit = () => {
  if (!profileForm.nickname.trim()) return
  console.log('修改昵称', profileForm.nickname)
  profileVisible.value = false
}

const handleEmailSubmit = () => {
  if (!emailForm.email.trim() || !emailForm.code.trim()) return
  console.log('修改邮箱', emailForm)
  emailVisible.value = false
}

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const handlePasswordSubmit = () => {
  if (!passwordForm.oldPassword || !passwordForm.newPassword || !passwordForm.confirmPassword) return
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    console.log('两次密码不一致')
    return
  }
  if (passwordForm.newPassword.length < 6 || passwordForm.newPassword.length > 20) {
    console.log('密码长度需6-20位')
    return
  }
  console.log('修改密码', passwordForm)
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordVisible.value = false
}

const openTermsModal = () => {
  aboutVisible.value = false
  termsVisible.value = true
}

const openPrivacyModal = () => {
  aboutVisible.value = false
  privacyVisible.value = true
}

const openWechatModal = () => {
  aboutVisible.value = false
  wechatVisible.value = true
}

const handleLogout = () => {
  userCenterVisible.value = false
  localStorage.removeItem('aichuangzuo_membership')
  localStorage.removeItem('aichuangzuo_notif_seeded')
  router.push('/login')
}

// ---------- 会员 ----------
const MEMBERSHIP_KEY = 'aichuangzuo_membership'
const hasMembership = ref(false)
const membershipLevel = ref('年会员')

const loadMembership = () => {
  const level = localStorage.getItem(MEMBERSHIP_KEY)
  if (level) {
    hasMembership.value = true
    membershipLevel.value = level
  }
}

const handleMembershipClick = () => {
  router.push('/pricing')
}

const notifTabs = [
  { type: 'announcement', label: '公告' },
  { type: 'generation', label: '生成完成' },
  { type: 'membership', label: '会员提醒' },
  { type: 'feature', label: '新功能' },
  { type: 'promotion', label: '优惠活动' }
]

const activeTabLabel = computed(() => {
  return notifTabs.find(t => t.type === activeTab.value)?.label || ''
})

const currentNotifs = computed(() => {
  return notifications.value
    .filter(n => n.type === activeTab.value)
    .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
})

const unreadCount = computed(() => notifications.value.filter(n => !n.read).length)

const getUnreadByType = (type) => {
  return notifications.value.filter(n => n.type === type && !n.read).length
}

const formatTime = (iso) => {
  const date = new Date(iso)
  const now = new Date()
  const diff = Math.floor((now - date) / 1000)
  if (diff < 60) return '刚刚'
  if (diff < 3600) return Math.floor(diff / 60) + ' 分钟前'
  if (diff < 86400) return Math.floor(diff / 3600) + ' 小时前'
  if (diff < 604800) return Math.floor(diff / 86400) + ' 天前'
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

const switchTab = (type) => {
  activeTab.value = type
}

const loadNotifications = () => {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    notifications.value = raw ? JSON.parse(raw) : []
  } catch {
    notifications.value = []
  }
}

const saveNotifications = () => {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(notifications.value))
}

const markAllRead = () => {
  notifications.value.forEach(n => { n.read = true })
  saveNotifications()
}

const handleNotifClick = (n) => {
  if (!n.read) {
    n.read = true
    saveNotifications()
  }
  notifVisible.value = false
  if (n.type === 'generation') {
    router.push('/console/works')
  } else if (n.type === 'membership') {
    router.push('/pricing')
  }
}

// 种子数据
const seedNotifications = () => {
  const seeded = localStorage.getItem('aichuangzuo_notif_seeded')
  if (seeded) return
  notifications.value = [
    {
      id: '1',
      type: 'announcement',
      title: '系统维护通知',
      summary: '爱创作将于 6 月 30 日 22:00-23:00 进行系统维护，届时部分功能暂停使用',
      read: false,
      createdAt: new Date(Date.now() - 1000 * 60 * 5).toISOString()
    },
    {
      id: '2',
      type: 'feature',
      title: '新功能上线：标题优化器',
      summary: '预览页新增 AI 标题优化，一键生成多平台爆款标题',
      read: false,
      createdAt: new Date(Date.now() - 1000 * 60 * 10).toISOString()
    },
    {
      id: '3',
      type: 'promotion',
      title: '限时优惠：年会员 7 折',
      summary: '即日起至月底，年会员低至 199 元，点击了解详情',
      read: false,
      createdAt: new Date(Date.now() - 1000 * 60 * 30).toISOString()
    },
    {
      id: '4',
      type: 'membership',
      title: '会员即将到期提醒',
      summary: '您的会员将于 7 天后到期，续费可享续费优惠',
      read: false,
      createdAt: new Date(Date.now() - 1000 * 60 * 60 * 2).toISOString()
    },
    {
      id: '5',
      type: 'generation',
      title: '文章生成完成',
      summary: '《如何写出爆款小红书文案》已生成完毕，点击查看',
      read: true,
      createdAt: new Date(Date.now() - 1000 * 60 * 60 * 5).toISOString()
    }
  ]
  saveNotifications()
  localStorage.setItem('aichuangzuo_notif_seeded', '1')

  // 演示用：默认开通年会员
  if (!localStorage.getItem(MEMBERSHIP_KEY)) {
    localStorage.setItem(MEMBERSHIP_KEY, '年会员')
    hasMembership.value = true
    membershipLevel.value = '年会员'
  }
}

onMounted(() => {
  loadTheme()
  loadNotifications()
  seedNotifications()
  loadMembership()
})
</script>

<style scoped>
.console-layout {
  display: flex;
  height: 100vh;
}

/* 侧边栏 */
.console-sidebar {
  width: 200px;
  background: var(--color-bg-card);
  border-right: 1px solid var(--color-border-light);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.console-sidebar-brand {
  display: flex;
  align-items: center;
  height: 56px;
  padding: 0 20px;
  border-bottom: 1px solid var(--color-border-light);
  flex-shrink: 0;
  font-weight: 700;
  font-size: 18px;
  color: var(--color-primary);
}

.brand-logo {
  height: 28px;
  width: auto;
  margin-right: 8px;
}

.brand-name {
  font-weight: 700;
  font-size: 16px;
  color: #000;
}

.console-sidebar-nav {
  flex: 1;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.console-sidebar-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  color: var(--color-text-primary);
  transition: all 0.2s;
  font-size: 14px;
  cursor: pointer;
}

.console-sidebar-item:hover {
  background: var(--color-primary-light);
}

.console-sidebar-item.active {
  background: var(--color-primary-light);
  color: var(--color-primary);
  font-weight: 600;
}

.nav-icon {
  font-size: 18px;
}

/* 主内容区 */
.console-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

/* 顶部栏 */
.console-header {
  height: 56px;
  background: var(--color-bg-card);
  border-bottom: 1px solid var(--color-border-light);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  position: relative;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.console-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--color-primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
}

.console-membership-badge {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.console-membership-badge.has-membership {
  background: #fff7e6;
  color: #fa8c16;
  border: 1px solid #ffd591;
}

.console-membership-badge.no-membership {
  background: #fff;
  color: var(--color-primary);
  border: 1px solid var(--color-primary);
}

.console-membership-badge.no-membership:hover {
  background: var(--color-primary);
  color: #fff;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.console-icon-btn {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  border: none;
  background: transparent;
  cursor: pointer;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-primary);
  transition: all 0.2s;
}

.console-icon-btn:hover {
  background: var(--color-primary-light);
}

.console-icon {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

/* 内容区 */
.console-content {
  flex: 1;
  min-height: 0;
  padding: 24px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  background: var(--color-bg-page);
}

/* 底部 */
.console-footer {
  padding: 16px 24px;
  border-top: 1px solid var(--color-border-light);
  color: var(--color-text-secondary);
  font-size: 13px;
  text-align: center;
  background: var(--color-bg-card);
}

.console-footer span + span::before {
  content: '|';
  margin: 0 12px;
  color: var(--color-border-light);
}

/* ========== 消息通知 ========== */
.bell-wrap {
  position: relative;
}

.bell-btn {
  position: relative;
}

.bell-badge {
  position: absolute;
  top: 2px;
  right: 2px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  background: #FF2442;
  color: #fff;
  border-radius: 8px;
  font-size: 10px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
  pointer-events: none;
}

/* 通知面板 */
.notif-panel {
  width: 100%;
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  user-select: none;
}

.notif-modal .ant-modal-body {
  padding: 0;
}

.notif-modal .ant-modal-header {
  margin-bottom: 0;
}

.notif-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px 12px;
  border-bottom: 1px solid #f0f0f0;
}

.notif-title {
  font-weight: 600;
  font-size: 15px;
  color: #1a1a1a;
}

.notif-read-all {
  font-size: 12px;
  color: var(--color-primary);
  cursor: pointer;
  background: none;
  border: none;
  padding: 0;
  transition: opacity 0.2s;
}

.notif-read-all:hover {
  opacity: 0.75;
}

/* Tab 栏 */
.notif-tabs {
  display: flex;
  padding: 0 8px;
  gap: 0;
  border-bottom: 1px solid #f0f0f0;
}

.notif-tab {
  flex: 1;
  padding: 10px 6px;
  font-size: 12px;
  color: #8c8c8c;
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 3px;
  white-space: nowrap;
}

.notif-tab.active {
  color: var(--color-primary);
  border-bottom-color: var(--color-primary);
  font-weight: 600;
}

.notif-tab-badge {
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  background: #FF2442;
  color: #fff;
  border-radius: 8px;
  font-size: 10px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 消息列表 */
.notif-list {
  max-height: 520px;
  min-height: 320px;
  overflow-y: auto;
}

.notif-empty {
  padding: 40px 0;
  text-align: center;
}

.notif-empty-icon {
  font-size: 32px;
  margin-bottom: 8px;
}

.notif-empty-text {
  font-size: 13px;
  color: #8c8c8c;
}

.notif-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.15s;
  border-bottom: 1px solid #f9f9f9;
}

.notif-item:last-child {
  border-bottom: none;
}

.notif-item:hover {
  background: #fafafa;
}

.notif-item.unread {
  background: #fff9f9;
}

.notif-item.unread:hover {
  background: #fff0f2;
}

.notif-item-dot {
  width: 8px;
  height: 8px;
  background: #FF2442;
  border-radius: 50%;
  margin-top: 5px;
  flex-shrink: 0;
}

.notif-item-body {
  flex: 1;
  min-width: 0;
}

.notif-item-title {
  font-size: 13px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 3px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.notif-item-summary {
  font-size: 12px;
  color: #8c8c8c;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 4px;
}

.notif-item-time {
  font-size: 11px;
  color: #bfbfbf;
}

/* ========== 教程面板 ========== */
.tutorial-panel {
  width: 340px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  overflow: hidden;
  user-select: none;
}

.tutorial-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 16px;
  cursor: pointer;
  transition: background 0.15s;
  border-bottom: 1px solid #f0f0f0;
}

.tutorial-item:last-child {
  border-bottom: none;
}

.tutorial-item:hover {
  background: #fafafa;
}

.tutorial-icon {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
  color: var(--color-primary);
}

.tutorial-icon svg {
  width: 100%;
  height: 100%;
}

.tutorial-body {
  flex: 1;
  min-width: 0;
}

.tutorial-name {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 4px;
}

.tutorial-desc {
  font-size: 12px;
  color: #8c8c8c;
  line-height: 1.5;
}

/* ========== 反馈面板 ========== */
.feedback-panel {
  width: 320px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  padding: 16px;
  user-select: none;
}

.feedback-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 14px;
}

.feedback-label {
  display: block;
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 8px;
}

.feedback-type {
  margin-bottom: 12px;
}

.feedback-type-btns {
  display: flex;
  gap: 6px;
}

.type-btn {
  flex: 1;
  padding: 6px 0;
  font-size: 12px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  background: #fff;
  color: #595959;
  cursor: pointer;
  transition: all 0.2s;
}

.type-btn.active {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: #fff0f2;
}

.type-btn:hover:not(.active) {
  border-color: #bfbfbf;
  color: #1a1a1a;
}

.feedback-content {
  margin-bottom: 12px;
}

.feedback-textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 13px;
  color: #1a1a1a;
  resize: vertical;
  transition: border-color 0.2s, box-shadow 0.2s;
  box-sizing: border-box;
  font-family: inherit;
}

.feedback-textarea:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(255, 36, 66, 0.1);
}

.feedback-textarea::placeholder {
  color: #bfbfbf;
}

.feedback-submit {
  width: 100%;
  padding: 10px;
  background: var(--color-primary);
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.feedback-submit:hover {
  background: var(--color-primary-hover);
}

/* ========== 关于我们面板 ========== */
.about-panel {
  width: 340px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  overflow: hidden;
  user-select: none;
}

.about-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.about-logo {
  flex-shrink: 0;
}

.about-logo img {
  height: 44px;
  width: auto;
}

.about-brand {
  flex: 1;
}

.about-name {
  font-size: 16px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 2px;
}

.about-tagline {
  font-size: 12px;
  color: #8c8c8c;
}

.about-desc {
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.about-desc p {
  font-size: 12px;
  color: #595959;
  line-height: 1.7;
  margin-bottom: 8px;
}

.about-desc p:last-child {
  margin-bottom: 0;
}

.about-links {
  padding: 8px 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.about-link-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 6px 12px;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  font-size: 12px;
  color: #595959;
  cursor: pointer;
  transition: all 0.2s;
}

.about-link-btn svg {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
}

.about-link-btn:hover {
  background: var(--color-primary-light);
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.about-footer {
  padding: 12px 16px;
  text-align: center;
  font-size: 11px;
  color: #bfbfbf;
  border-top: 1px solid #f0f0f0;
}

/* ========== 暗色主题 ========== */
body[data-theme="dark"] .console-sidebar {
  background: #1f1f1f;
  border-color: #303030;
}

body[data-theme="dark"] .console-sidebar-brand {
  border-color: #303030;
  color: #10b981;
}

body[data-theme="dark"] .brand-name {
  color: #e0e0e0;
}

body[data-theme="dark"] .console-sidebar-item {
  color: #e0e0e0;
}

body[data-theme="dark"] .console-sidebar-item:hover,
body[data-theme="dark"] .console-sidebar-item.active {
  background: #1a2e1a;
  color: #10b981;
}

body[data-theme="dark"] .console-header {
  background: #1f1f1f;
  border-color: #303030;
}

body[data-theme="dark"] .console-content {
  background: #141414;
}

body[data-theme="dark"] .console-footer {
  background: #1f1f1f;
  border-color: #303030;
  color: #a6a6a6;
}

body[data-theme="dark"] .console-icon-btn {
  color: #e0e0e0;
}

body[data-theme="dark"] .console-icon-btn:hover {
  background: #2a2a2a;
}

body[data-theme="dark"] .console-membership-badge.has-membership {
  background: #2b2111;
  border-color: #594214;
  color: #ffa940;
}

body[data-theme="dark"] .notif-panel,
body[data-theme="dark"] .tutorial-panel,
body[data-theme="dark"] .feedback-panel,
body[data-theme="dark"] .about-panel {
  background: #1f1f1f;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5);
}

body[data-theme="dark"] .notif-header,
body[data-theme="dark"] .notif-tabs,
body[data-theme="dark"] .about-header,
body[data-theme="dark"] .about-desc {
  border-color: #303030;
}

body[data-theme="dark"] .notif-title,
body[data-theme="dark"] .form-title,
body[data-theme="dark"] .about-name {
  color: #e0e0e0;
}

body[data-theme="dark"] .notif-tab {
  color: #a6a6a6;
}

body[data-theme="dark"] .notif-tab.active {
  color: #10b981;
  border-bottom-color: #10b981;
}

body[data-theme="dark"] .notif-item {
  border-bottom-color: #303030;
}

body[data-theme="dark"] .notif-item:hover {
  background: #2a2a2a;
}

body[data-theme="dark"] .notif-item.unread {
  background: #1c2418;
}

body[data-theme="dark"] .notif-item.unread:hover {
  background: #232e1f;
}

body[data-theme="dark"] .notif-item-title {
  color: #e0e0e0;
}

body[data-theme="dark"] .tutorial-item {
  border-color: #303030;
}

body[data-theme="dark"] .tutorial-item:hover {
  background: #2a2a2a;
}

body[data-theme="dark"] .tutorial-name {
  color: #e0e0e0;
}

body[data-theme="dark"] .tutorial-icon {
  color: #10b981;
}

body[data-theme="dark"] .feedback-title {
  color: #e0e0e0;
}

body[data-theme="dark"] .feedback-textarea {
  background: #262626;
  border-color: #404040;
  color: #e0e0e0;
}

body[data-theme="dark"] .feedback-textarea:focus {
  border-color: #10b981;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.2);
}

body[data-theme="dark"] .type-btn {
  background: #262626;
  border-color: #404040;
  color: #a6a6a6;
}

body[data-theme="dark"] .type-btn.active {
  border-color: #10b981;
  color: #10b981;
  background: #1c2e1a;
}

body[data-theme="dark"] .about-logo {
}

body[data-theme="dark"] .about-tagline {
  color: #a6a6a6;
}

body[data-theme="dark"] .about-desc p {
  color: #a6a6a6;
}

body[data-theme="dark"] .about-link-btn {
  background: #262626;
  border-color: #404040;
  color: #a6a6a6;
}

body[data-theme="dark"] .about-link-btn:hover {
  background: #1c2e1a;
  border-color: #10b981;
  color: #10b981;
}

body[data-theme="dark"] .about-footer {
  border-color: #303030;
  color: #666;
}

/* ========== 个人中心面板 ========== */
.user-center-panel {
  width: 320px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  overflow: hidden;
  user-select: none;
}

/* 会员卡 */
.membership-card {
  background: linear-gradient(135deg, #FF2442 0%, #cc1730 100%);
  padding: 16px 20px;
  color: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  transition: opacity 0.2s;
}

.membership-card:hover {
  opacity: 0.95;
}

.membership-left {
  flex: 1;
}

.membership-label {
  font-size: 12px;
  opacity: 0.9;
  margin-bottom: 4px;
}

.membership-name {
  font-size: 18px;
  font-weight: 700;
}

.membership-expiry {
  font-size: 12px;
  opacity: 0.8;
  margin-top: 2px;
}

.membership-btn {
  padding: 6px 16px;
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.4);
  color: #fff;
  border-radius: 20px;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.2s;
}

.membership-btn:hover {
  background: rgba(255, 255, 255, 0.3);
}

/* 账号信息 */
.user-section {
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.user-section:last-child {
  border-bottom: none;
}

.user-section-title {
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 10px;
}

.user-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
}

.user-row-label {
  font-size: 13px;
  color: #595959;
}

.user-row-value {
  font-size: 13px;
  color: #1a1a1a;
  font-weight: 500;
}

/* 快捷操作 */
.user-action {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 0;
  font-size: 14px;
  color: #262626;
  cursor: pointer;
  transition: color 0.2s;
}

.user-action svg {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
  color: #8c8c8c;
}

.user-action:hover {
  color: var(--color-primary);
}

.user-action:hover svg {
  color: var(--color-primary);
}

.user-action-logout {
  color: #ff4d4f;
}

.user-action-logout svg {
  color: #ff4d4f;
}

.user-action-logout:hover {
  color: #ff4d4f;
  opacity: 0.8;
}

.user-row-edit {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  color: var(--color-primary);
}

.user-row-edit svg {
  width: 14px;
  height: 14px;
}

.user-row-edit:hover {
  opacity: 0.75;
}

/* 修改昵称 */
.profile-modal-content {
  padding: 8px 0;
}

.profile-item {
  margin-bottom: 16px;
}

.profile-label {
  display: block;
  font-size: 13px;
  color: #595959;
  margin-bottom: 8px;
  font-weight: 500;
}

.profile-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 14px;
  color: #1a1a1a;
  transition: border-color 0.2s, box-shadow 0.2s;
  box-sizing: border-box;
}

.profile-input:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(255, 36, 66, 0.1);
}

.profile-submit {
  width: 100%;
  padding: 10px;
  background: var(--color-primary);
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  margin-top: 8px;
  transition: background 0.2s;
}

.profile-submit:hover {
  background: var(--color-primary-hover);
}

/* 修改邮箱 */
.email-modal-content {
  padding: 8px 0;
}

.email-item {
  margin-bottom: 16px;
}

.email-label {
  display: block;
  font-size: 13px;
  color: #595959;
  margin-bottom: 8px;
  font-weight: 500;
}

.email-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 14px;
  color: #1a1a1a;
  transition: border-color 0.2s, box-shadow 0.2s;
  box-sizing: border-box;
}

.email-input:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(255, 36, 66, 0.1);
}

.email-code-row {
  display: flex;
  gap: 10px;
}

.email-code-input {
  flex: 1;
}

.email-code-btn {
  padding: 0 14px;
  background: #fff;
  border: 1px solid var(--color-primary);
  border-radius: 8px;
  color: var(--color-primary);
  font-size: 13px;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;
}

.email-code-btn:hover:not(:disabled) {
  background: var(--color-primary);
  color: #fff;
}

.email-code-btn:disabled {
  border-color: #d9d9d9;
  color: #8c8c8c;
  cursor: not-allowed;
}

.email-submit {
  width: 100%;
  padding: 10px;
  background: var(--color-primary);
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  margin-top: 8px;
  transition: background 0.2s;
}

.email-submit:hover {
  background: var(--color-primary-hover);
}

/* 暗色主题 */
body[data-theme="dark"] .profile-label,
body[data-theme="dark"] .email-label {
  color: #a6a6a6;
}

body[data-theme="dark"] .profile-input,
body[data-theme="dark"] .email-input {
  background: #262626;
  border-color: #404040;
  color: #e0e0e0;
}

body[data-theme="dark"] .profile-input:focus,
body[data-theme="dark"] .email-input:focus {
  border-color: #10b981;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.2);
}

body[data-theme="dark"] .email-code-btn {
  background: #262626;
  border-color: #404040;
  color: #a6a6a6;
}

body[data-theme="dark"] .email-code-btn:hover:not(:disabled) {
  background: #1c2e1a;
  border-color: #10b981;
  color: #10b981;
}

/* 暗色主题 */
body[data-theme="dark"] .user-center-panel {
  background: #1f1f1f;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5);
}

body[data-theme="dark"] .membership-card {
  background: linear-gradient(135deg, #b01030 0%, #8a0f25 100%);
}

body[data-theme="dark"] .user-section {
  border-color: #303030;
}

body[data-theme="dark"] .user-row-label {
  color: #a6a6a6;
}

body[data-theme="dark"] .user-row-value {
  color: #e0e0e0;
}

body[data-theme="dark"] .user-action {
  color: #e0e0e0;
}

body[data-theme="dark"] .user-action svg {
  color: #666;
}

/* 修改密码弹框 */
.password-modal-content {
  padding: 8px 0;
}

.password-item {
  margin-bottom: 16px;
}

.password-label {
  display: block;
  font-size: 13px;
  color: #595959;
  margin-bottom: 8px;
  font-weight: 500;
}

.password-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 14px;
  color: #1a1a1a;
  transition: border-color 0.2s, box-shadow 0.2s;
  box-sizing: border-box;
}

.password-input:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(255, 36, 66, 0.1);
}

.password-input::placeholder {
  color: #bfbfbf;
}

.password-submit {
  width: 100%;
  padding: 10px;
  background: var(--color-primary);
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  margin-top: 8px;
  transition: background 0.2s;
}

.password-submit:hover {
  background: var(--color-primary-hover);
}

body[data-theme="dark"] .password-label {
  color: #a6a6a6;
}

body[data-theme="dark"] .password-input {
  background: #262626;
  border-color: #404040;
  color: #e0e0e0;
}

body[data-theme="dark"] .password-input:focus {
  border-color: #10b981;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.2);
}

/* 用户协议 / 隐私政策 */
.terms-content {
  max-height: 60vh;
  overflow-y: auto;
  font-size: 14px;
  line-height: 1.8;
  color: #262626;
}

.terms-content h4 {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 16px 0 8px;
}

.terms-content p {
  margin-bottom: 10px;
}

.terms-content ul {
  margin: 8px 0 12px 20px;
}

.terms-content li {
  margin-bottom: 4px;
}

/* 客服微信二维码 */
.wechat-qr {
  width: 120px;
  height: auto;
  display: block;
  margin: 0 auto;
  border-radius: 8px;
}

.wechat-modal-content {
  text-align: center;
  padding: 16px 0;
  user-select: none;
}

.wechat-qr-large {
  width: 200px;
  height: auto;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  pointer-events: none;
}

.wechat-hint {
  margin-top: 16px;
  font-size: 14px;
  color: #595959;
}

/* 移动端：侧边栏收拢为图标栏 */
@media (max-width: 768px) {
  .console-sidebar {
    width: 64px;
  }

  .console-sidebar-brand {
    justify-content: center;
    padding: 0;
  }

  .brand-logo {
    margin-right: 0;
  }

  .brand-name {
    display: none;
  }

  .console-sidebar-nav {
    padding: 12px 8px;
    align-items: center;
  }

  .console-sidebar-item {
    justify-content: center;
    padding: 10px;
  }

  .nav-label {
    display: none;
  }
}
</style>
