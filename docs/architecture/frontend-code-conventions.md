# 前端代码规范

> 本文档定义爱创作（AI Creation）项目用户端与管理端前端的代码组织与开发规范，基于 Vue 3 + Vite + Pinia + Vue Router 4 + Ant Design Vue。

---

## 1. 目录结构

```text
src/
├── api/                    # 接口请求（按模块分文件）
│   ├── auth.js
│   ├── article.js
│   └── generation.js
├── assets/                 # 静态资源
│   ├── images/
│   └── styles/
├── components/             # 公共组件
│   ├── common/             # 全局通用组件
│   └── business/           # 业务组件
├── composables/            # 组合式函数
├── constants/              # 常量
├── directives/             # 自定义指令
├── layouts/                # 布局组件
├── router/                 # 路由配置
├── stores/                 # Pinia 状态管理（按模块）
│   ├── user.js
│   ├── article.js
│   └── app.js
├── utils/                  # 工具函数
│   ├── request.js          # Axios 封装
│   ├── storage.js
│   └── validate.js
├── views/                  # 页面组件
│   ├── auth/
│   ├── article/
│   └── generation/
├── App.vue
└── main.js
```

---

## 2. 命名规范

| 类型 | 规则 | 示例 |
|---|---|---|
| 组件文件 | 大驼峰 | `ArticleList.vue` |
| 组件名 | 大驼峰，多单词 | `ArticleList` |
| 页面文件 | 大驼峰 | `CreateArticleView.vue` |
| API 文件 | 小写，业务模块 | `article.js` |
| Store 文件 | 小写，业务模块 | `article.js` |
| 工具函数文件 | 小写，camelCase | `formatDate.js` |
| 组合式函数 | `use` 开头，camelCase | `useArticleList.js` |
| 常量 | 全大写 + 下划线 | `MAX_WORD_COUNT` |
| 枚举 | 全大写 + 下划线 | `ARTICLE_STATUS` |

---

## 3. 组件规范

- 使用 `<script setup>` 语法。
- 组件名与文件名一致。
- Props 必须定义类型和默认值：

```vue
<script setup>
defineProps({
  title: {
    type: String,
    required: true
  },
  visible: {
    type: Boolean,
    default: false
  }
});
</script>
```

- 事件命名：`on{Action}` 或 `update:{prop}`。
- 样式使用 `scoped`，全局样式放 `assets/styles/`。

---

## 4. API 封装

统一在 `utils/request.js` 中封装 Axios：

```javascript
import axios from 'axios';

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 30000
});

// 请求拦截：加 Token
request.interceptors.request.use(config => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// 响应拦截：统一错误处理
request.interceptors.response.use(
  response => response.data,
  error => {
    const { code, message } = error.response?.data || {};
    handleBusinessError(code, message);
    return Promise.reject(error);
  }
);

export default request;
```

API 文件示例：

```javascript
import request from '@/utils/request.js';

export function login(data) {
  return request.post('/auth/login', data);
}

export function getArticles(params) {
  return request.get('/articles', { params });
}
```

---

## 5. Pinia Store 组织

按模块拆分 Store，使用 Option Store 或 Setup Store 均可，建议统一：

```javascript
import { defineStore } from 'pinia';
import { getUserInfo } from '@/api/user.js';

export const useUserStore = defineStore('user', {
  state: () => ({
    userInfo: null,
    token: localStorage.getItem('accessToken') || ''
  }),
  getters: {
    isLoggedIn: state => !!state.token
  },
  actions: {
    async fetchUserInfo() {
      this.userInfo = await getUserInfo();
    }
  }
});
```

- Store ID 必须唯一，使用模块名。
- 状态变更必须通过 Actions，禁止直接修改 State。

---

## 6. 路由规范

- 路由配置集中放在 `src/router/`。
- 路由路径使用小写 + `-` 连接。
- 懒加载页面组件：

```javascript
{
  path: '/articles',
  name: 'ArticleList',
  component: () => import('@/views/article/ArticleListView.vue')
}
```

- 路由守卫统一处理登录校验和权限。

---

## 7. 代码风格

- 使用 ESLint + Prettier 统一格式。
- 单引号、无分号、2 空格缩进。
- 优先使用 `const`，需要重新赋值时用 `let`，禁止 `var`。
- 优先使用 `async/await`，避免回调地狱。
- 注释使用 `//`，复杂逻辑必须说明原因。

---

## 8. 环境变量

- 环境变量以 `VITE_` 开头。
- `.env.development`、`.env.production` 本地使用，不提交 Git。
- `.env.example` 作为模板提交。

---

## 9. 禁止事项

- 禁止在组件中直接调用 Axios，必须通过 `api/` 模块。
- 禁止在模板中写复杂表达式。
- 禁止直接操作 `localStorage`，统一通过 `utils/storage.js`。
- 禁止在 `watch` 中做大量计算或异步请求。
- 禁止复制粘贴大量重复样式，优先使用 Ant Design Vue 组件。

---

## 10. 变更记录

| 日期 | 版本 | 变更说明 | 变更人 |
|---|---|---|---|
| 2026-06-26 | v1.0 | 初稿：目录结构、命名、组件、API 封装、Pinia、路由、代码风格 | - |
