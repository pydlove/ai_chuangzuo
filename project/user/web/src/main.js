import { createApp } from 'vue'
import { createPinia } from 'pinia'
import Antd from 'ant-design-vue'
import App from './App.vue'
import router from './router'
import 'ant-design-vue/dist/reset.css'
import './styles/index.css'
import { loadSystemSkills } from './composables/useSkills.js'
import { STORAGE_KEYS } from './constants/storage.js'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(Antd)

app.mount('#app')

// 启动时预热系统预设 skills（仅登录用户）
if (localStorage.getItem(STORAGE_KEYS.ACCESS_TOKEN)) {
  loadSystemSkills()
}
