import { createApp } from 'vue'
import { createPinia } from 'pinia'
import Antd from 'ant-design-vue'
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import App from './App.vue'
import router from './router'
import 'ant-design-vue/dist/reset.css'
import './styles/index.css'
import { loadSystemSkills } from './composables/useSkills.js'
import { STORAGE_KEYS } from './constants/storage.js'
import { initAuth } from './composables/useAuthInit.js'
import { startVersionHeartbeat } from './utils/versionHeartbeat.js'

async function bootstrap() {
  try {
    await initAuth()
  } catch {
    // initAuth 已清理失效凭证，应用继续挂载，由路由守卫引导到登录页
  }

  const app = createApp(App)

  app.use(createPinia())
  app.use(router)
  // 全局中文语言包：弹框默认按钮、Popconfirm、空状态等默认文案显示中文
  app.use(Antd, { locale: zhCN })

  app.mount('#app')

  // 版本心跳：检测服务升级与新版本发布（dev 模式自动跳过）
  startVersionHeartbeat()

  // 启动时预热系统预设 skills（仅登录用户）
  if (localStorage.getItem(STORAGE_KEYS.ACCESS_TOKEN)) {
    loadSystemSkills()
  }
}

bootstrap()
