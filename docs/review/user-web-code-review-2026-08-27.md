# 用户端 Web 代码审查报告

**审查日期**: 2026-08-27  
**审查范围**: `project/user/web/src` 下全部 Vue 3 / Vite 前端代码  
**审查人**: Claude Code

---

## 总体评估

项目功能基本成型，模块划分清晰（`api` / `composables` / `views` / `components` / `utils`），核心流程（登录、创作、队列、预览、导出）已跑通。但代码质量和可维护性问题较多，存在 **XSS、目录遍历、无限轮询、localStorage 数据与后端不一致** 等需要尽快修复的隐患；同时存在大量 `console.*`、硬编码外链、PC/Mobile 重复代码等整洁性问题。

---

## 一、严重 / 必须修

### 1. `v-html` 直接渲染用户/运营可控内容，存在 XSS 风险

| 文件 | 位置 | 问题描述 |
|------|------|----------|
| `src/views/console/PreviewIndex.vue` | 第 30 行 | `v-html="formattedBody"`，正文来自后端 `article.body`，渲染链路没有统一消毒。 |
| `src/views/console/PreviewIndex.vue` | 第 40 行 | `v-html="renderBlockHtml(block)"`，`block.html` 来自 `contenteditable` 编辑，用户可绕过 `escapeHtml`。 |
| `src/components/learn/LearnMarkdown.vue` | 第 2 行 | `MarkdownIt` 配置 `html: true`，直接 `v-html` 渲染 `source`；运营后台内容被污染时前端会执行任意脚本。 |
| `src/views/console/LotteryPage.vue` | 第 121 行 | `v-html="renderedRules"`，`campaign.rules` 为运营配置，未消毒。 |
| `src/views/MobileGuide.vue` | 第 87 行 | `v-html="article.content"`。 |
| `src/views/GuideIndex.vue` | 第 11 行 | `v-html="article.content"`。 |
| `src/components/guide/GuideArticle.vue` | 第 11 行 | `v-html="article.content"`。 |
| `src/views/Pricing.vue` | 第 100-102 行 | `v-html="getCell(...)"`，目前数据源为前端静态数据，风险较低，但建议统一关闭。 |
| `src/views/console/create/CreateFlowModal.vue` | 第 295、668 行 | `v-html="currentTemplatePreview"`。 |
| `src/views/console/create/modals/TemplateModal.vue` | 第 36 行 | `v-html="currentTemplatePreview"`。 |
| `src/views/console/MessagesIndex.vue` | 第 65、95 行 | `v-html="iconSvg(msg.type)"`，icon 为前端 SVG 字符串，相对安全，但建议避免 `v-html`。 |

**修复建议**：
- 对后端/用户来源的 HTML 统一使用 DOMPurify 或白名单消毒。
- `MarkdownIt` 关闭 `html: true`，或渲染后做二次消毒。
- 图标渲染改为组件映射，避免 `v-html`。

---

### 2. 开发服务器 `/uploads` 静态中间件路径校验有缺陷

**文件**: `vite.config.js` 第 74-109 行

```js
const filePath = path.join(uploadRoot, relativePath)
if (!filePath.startsWith(uploadRoot)) { ... }
```

`path.join` 不会规范化所有路径穿越形式，`startsWith` 大小写敏感，开发环境存在目录遍历风险。

**修复建议**：
- 使用 `path.resolve(uploadRoot, relativePath)`。
- 比较 `path.dirname(resolvedFilePath) === uploadRoot` 或 `resolvedFilePath.startsWith(uploadRoot + path.sep)`。
- 限定允许的文件扩展名白名单。

---

### 3. 轮询任务缺乏最大重试/超时保护

| 文件 | 位置 | 问题 |
|------|------|------|
| `src/composables/useGenerationTask.js` | 第 32-51 行 | 任务失败或后端无响应时，会无限 1.5 秒轮询。 |
| `src/views/console/create/useGenerationQueue.js` | 第 53-57 行 | 全局 5 秒轮询，没有页面隐藏暂停逻辑。 |
| `src/composables/useQrLogin.js` | 第 78-96 行 | 二维码轮询没有最大次数，过期后仍可能继续。 |
| `src/composables/useMessages.js` | 第 40-81 行 | 虽然做了 `visibilitychange` 暂停，但 `pollRefCount` 全局计数在页面异常卸载时可能永远大于 0。 |

**修复建议**：
- 补最大轮询次数或总超时时间。
- 使用指数退避或固定间隔 + 最大重试。
- 页面隐藏时暂停轮询，回到前台时立即补一次。
- 对全局单例增加更健壮的引用计数或 `AbortController` 清理。

---

### 4. 收益/创作币数据存在前端 localStorage mock，与后端真实数据易不一致 ✅ 已修复

**文件**: `src/composables/useSkillMarket.js` 第 13-63 行、`src/composables/useInviteStats.js`

**问题**:
- `aichuangzuo_earnings_records`、`aichuangzuo_coin_balance` 等敏感数据存在 localStorage。
- `getUserId()` 在没有 `aichuangzuo_user_id` 时会生成随机 ID，可能与后端真实用户 ID 不一致。
- 换账号时依赖 `USER_SCOPED_KEYS` 清理，但多设备/浏览器无法同步，用户会看到错乱数据。

**修复内容**:
1. `src/composables/useSkillMarket.js`: 完全移除 `earningsRecords`、`EARNINGS_KEY`、`COIN_BALANCE_KEY`、`USER_ID_KEY` 及 localStorage 读写；删除 `useMarketSkill`、`simulateExternalUse` 及所有收益计算函数；余额/收益完全以服务端 `marketOverview` 和 `useEarnings` / `useInviteStats` 为准。
2. `src/composables/useInviteStats.js`: 移除 `COIN_BALANCE_KEY`、`DEFAULT_COIN_BALANCE`、localStorage 读写、前端 `setCoinBalance` / `adjustCoinBalance`；`coinBalance` 仅由 `loadInviteStats` 从后端接口填充。
3. `src/composables/usePricing.js`: 订阅成功后不再前端 `adjustCoinBalance` 扣减，改为调用 `loadInviteStats()` 重新拉取服务端余额。
4. `src/views/console/create/modals/SkillModal.vue`、`src/views/console/SkillsIndex.vue`、`src/views/console/SkillMarketIndex.vue`: 移除对 `useMarketSkill` / `simulateExternalUse` 的调用；选择/使用市场提示词时不再前端 mock 收益，仅跳转到创作页或应用 skill。
5. `src/composables/useLogin.js`: 从 `USER_SCOPED_KEYS` 中移除已不再使用的 `aichuangzuo_earnings_records` 和 `aichuangzuo_coin_balance`。

**验证**: `npm run build` 通过，无引用错误。

---

## 二、中等 / 建议修

### 5. `import` 语句被普通代码隔断

**文件**: `src/views/console/ConsoleLayout.vue` 第 1536-1555 行

```js
const logoUrl = 'https://foruda.gitee.com/images/1782986808430461164/e0ab39dc_8060302.png'
import {
  LoadingOutlined,
  ...
} from '@ant-design/icons-vue'
```

`const` 出现在 `import` 中间，语法虽未报错，但极不规范。

**修复建议**：将所有 `import` 移到文件最顶部。

---

### 6. 大量 `console.log/warn/error` 未清理 ✅ 已修复

不完全统计约 30+ 处，典型位置：

- `src/views/console/ConsoleLayout.vue:1914` 的 `console.log('关于链接:', type)`
- `src/composables/useSkillMarket.js` 多处
- `src/views/console/WorksIndex.vue` 多处
- `src/views/console/PreviewIndex.vue` 多处
- `src/views/console/WorkbenchIndex.vue` 多处
- `src/views/console/CreateIndex.vue`
- `src/utils/articleStorage.js`
- `src/components/AccountCheckModal.vue`
- `src/views/console/AccountCheckIndex.vue`
- 其他 `console.warn('加载...失败', e)` / `console.error('...失败', e)` 等

**修复内容**：
- 删除 `project/user/web/src` 下所有 `console.log` / `console.warn` / `console.error` 调试输出，共清理 30+ 处。
- 保留原有错误处理语义：
  - 已调用 `message.error` / `message.success` 的保持 UI 提示；
  - 仅含 `console` 的 `catch` 块改为空块并补充注释说明吞掉异常的原因；
  - 返回默认值的保持返回默认值。
- 本次不引入统一日志工具，按项目当前阶段直接移除生产调试输出。

**验证**：`npm run build` 通过，无引用错误。

### 7. 硬编码外部资源与域名

| 文件 | 位置 | 硬编码内容 |
|------|------|------------|
| `src/views/console/ConsoleLayout.vue` | 744、1133、1536 行 | Gitee logo 图片 |
| `src/components/layout/NavBar.vue` | 第 6 行 | Gitee logo 图片 |
| `src/views/Home.vue` | 第 295 行 | 飞书帮助文档链接 |
| `src/views/MobileHome.vue` | 第 264 行 | 飞书帮助文档链接 |
| `src/views/console/ConsoleLayout.vue` | 第 1822 行 | 飞书帮助文档常量 |
| `src/views/console/ConsoleLayout.vue` | 第 2704 行 | `https://www.ichuang.top/login?ref=...` |
| `src/views/console/InviteIndex.vue` | 第 185 行 | `https://www.ichuang.top/login?ref=...` |
| `src/views/console/LotteryPage.vue` | 第 281 行 | `https://api.dicebear.com/7.x/avataaars/svg?seed=default` |
| `src/views/console/PreviewIndex.vue` | 310、318、327、335、343、351、359 行 | Gitee 标签帮助示意图 |

**修复建议**：
- logo、帮助文档链接走配置中心或环境变量（如 `VITE_HELP_DOC_URL`）。
- 邀请链接域名用 `window.location.origin` 或 `VITE_APP_BASE_URL`。
- 默认头像应使用本地占位图，避免依赖外部 API。

---

### 8. localStorage key 零散，没有统一常量 ✅ 已修复

检索到约 30+ 个不同的 `aichuangzuo_*` key，分散在 20 多个文件中，例如：

- `aichuangzuo_access_token`
- `aichuangzuo_refresh_token`
- `aichuangzuo_remember_me`
- `aichuangzuo_user_id`
- `aichuangzuo_membership`
- `aichuangzuo_current_article`
- `aichuangzuo_drafts`
- `aichuangzuo_create_form`
- `aichuangzuo_create_mode`
- `aichuangzuo_create_last_skill`
- `aichuangzuo_newcomer_modal_dismissed`
- `aichuangzuo_newcomer_banner_dismissed`
- `aichuangzuo_invite_modal_dismissed`
- `aichuangzuo_selfmedia_plan_modal_dismissed`
- `aichuangzuo_redeem_codes`
- `aichuangzuo_redeem_history`
- `aichuangzuo_withdraw_agreement_accepted`
- `aichuangzuo_account_check_last_*, aichuangzuo_account_recommend_last_*`
- `aichuangzuo_today_done_*`
- `aichuangzuo_onboarding_draft:*, aichuangzuo_onboarding_done:*`
- `aichuangzuo_mine_nav_expanded`

**修复内容**：
1. 新增 `src/constants/storage.js`，集中定义 `STORAGE_KEYS` 常量对象，覆盖认证、用户、主题、创作、账户、新手引导、兑换码、自媒体方案等全部 key。
2. 新增用户隔离 key helper：`getAccountCheckLastKey(userId)`、`getAccountRecommendLastKey(userId)`、`getOnboardingDraftKey(userId)`、`getOnboardingDoneKey(userId)`。
3. 新增动态 key helper：`getTodayDoneKey(date)`、`getCouponWarnKey(couponId)`。
4. 维护 `USER_SCOPED_STORAGE_KEYS` 数组，供登录态切换时统一清理用户相关缓存。
5. 已迁移位置包括：`router/index.js`、`utils/request.js`、`utils/articleStorage.js`、`utils/membershipLimits.js`、`composables/useLogin.js`、`usePricing.js`、`useLearn.js`、`useSkills.js`、`views/console/create/useCreateForm.js`、`ConsoleLayout.vue`、`WorkbenchIndex.vue`、`CreateIndex.vue`、`WorksIndex.vue`、`SkillsIndex.vue`、`SkillMarketIndex.vue`、`AccountCheckIndex.vue`、`AccountCheckModal.vue`、`CouponIndex.vue`、`LotteryPage.vue`、`QrLoginScan.vue`、`NavBar.vue`、`LeaderboardPreview.vue`、`CreateFlowLauncher.vue`、`CreateFreePage.vue`、`CreateRecommendedPage.vue`、`OnboardingIndex.vue`、`WithdrawIndex.vue`。

**验证**: `npm run build` 通过，`src` 下已无硬编码 `aichuangzuo_*` 字符串。

---

### 9. 轮询/定时器清理不彻底 ✅ 已修复

虽然大部分在 `onBeforeUnmount` 中清理，但仍有隐患：

- `src/composables/useLogin.js:94-140`：`watch(sliderModalPassed/loginModalPassed)` 是异步 watch，组件卸载后回调仍可能执行并调用 `message`、`router.push`。
- `src/views/Home.vue:324` / `src/views/MobileHome.vue:291` / `src/views/MobileLearn.vue:344`：banner 轮播 `setInterval` 需要确认是否清理。
- `src/views/console/CouponIndex.vue:231`：`expiryTimer` 需要确认页面离开时是否清理。
- `src/views/console/ConsoleLayout.vue:2165, 2214`：邮箱/手机验证码倒计时需要确认。

**修复内容**：
1. `src/composables/useLogin.js`：新增 `isMounted` 标志；在 `onBeforeUnmount` 中置为 `false`；在两个异步 `watch` 的每个 `await` 后增加 `if (!isMounted) return` 守卫，避免卸载后继续调用 `message`/`router.push`。
2. `src/composables/useQrLogin.js`：新增 `isMounted` 守卫；轮询到达终端状态或过期时同时停止 polling 和 expire countdown；过期倒计时至 0 时自我清理。
3. `src/views/QrLoginScan.vue`：新增 `isMounted` 守卫；连续轮询失败超过 5 次后自动停止并提示错误。
4. `src/views/console/CouponIndex.vue`：`onUnmounted` 中清空 `expiryTimer` 后将其置为 `null`。
5. `src/views/console/ConsoleLayout.vue`：`onUnmounted` 中同时清理邮箱/手机验证码倒计时定时器。
6. `src/views/console/create/useGenerationQueue.js`：新增模块级 `pollRefCount` 引用计数和 `visibilitychange` 监听，页面切后台暂停轮询、切前台恢复；所有消费者停止后才真正清除 interval。
7. `src/composables/useGenerationTask.js` + `src/api/generation.js`：`getGenerationTask` 支持 `signal`；轮询使用 `AbortController`，`stop()` 时中断正在进行的请求；新增 `MAX_RETRIES` 错误重试上限；新增 `visibilitychange` 暂停/恢复；防止重叠启动。
8. `src/composables/useMessages.js`：`resumePolling` 在 `document.hidden` 时不启动 interval，避免后台无意义轮询。
9. `src/views/console/LotteryPage.vue`：新增 `pageMounted` 标志；追踪并清理抽奖动画相关的 `shakeTimer`、`revealTimer`、`minAnimTimer`、`rollTimer`；在 `onBeforeUnmount` 中全部清除；动画递归步进和 `performDraw` 的异步回调均增加卸载守卫。

**验证**: `npm run build` 通过，无引用错误。

---

### 10. 路由守卫对 `/lottery` 未做认证要求

**文件**: `src/router/index.js` 第 268-276 行

只有 `/console/*` 和 `meta.requireAuth` 路由会跳转登录。`/lottery` 没有 `requireAuth`，但该页面会调用 `loadProfile` 等需登录接口，未登录时会静默失败。

**修复建议**：给需要登录的页面统一加 `meta: { requireAuth: true }`；`/lottery` 等公共页面不应调用登录态接口，或做未登录兼容。

---

## 三、低优先级 / 代码整洁

### 11. PC 与移动端视图代码大量重复

| PC 文件 | Mobile 文件 | 重复内容 |
|---------|-------------|----------|
| `src/views/Home.vue` | `src/views/MobileHome.vue` | Hero、功能卡片、footer 链接、banner 数据 |
| `src/views/GuideIndex.vue` | `src/views/MobileGuide.vue` | 指南内容、accordion |
| `src/views/Login.vue` | `src/views/MobileLogin.vue` | 登录逻辑、表单校验 |
| `src/views/Pricing.vue` | `src/views/MobilePricing.vue` | 套餐数据、对比表格 |

**修复建议**：把 PC/Mobile 合并为响应式组件，或至少把文案/数据抽到共享模块（如 `src/data/`）。

---

### 12. 重复的工具函数 ✅ 已修复

- `formatDate` 在 `WorksIndex.vue`、`PreviewIndex.vue`、`HotSearchIndex.vue`、`MobileLearn.vue`、`ConsoleLearnIndex.vue`、`LearnContent.vue` 等多个文件中重复实现。
- `stripHtml` 在 `articleBlocks.js`、`PreviewIndex.vue` 中重复。
- 平台映射 `platformMap` / `platformNameMap` 在 `WorksIndex.vue`、`WorkbenchIndex.vue` 中重复。

**修复内容**：
- 新建 `src/utils/format.js`：
  - `formatDateTime(dateStr)` → "M月D日 HH:MM"，供 WorksIndex / PreviewIndex 使用；
  - `formatDate(date)` → "YYYY-MM-DD"，供 HotSearchIndex / MobileLearn / ConsoleLearnIndex / LearnContent 使用。
- 新建 `src/utils/html.js`：统一 `stripHtml(html)`，供 `articleBlocks.js` 和 `PreviewIndex.vue` 使用。
- 新建 `src/utils/platform.js`：统一 `PLATFORM_NAME_MAP` 与 `PLATFORM_OPTIONS`，供 `WorksIndex.vue` 和 `WorkbenchIndex.vue` 使用。
- 删除各文件中的本地重复定义，改为从对应 utils 文件导入。

**验证**：`npm run build` 通过，无引用错误。

---

### 13. 请求层细节待优化

**文件**: `src/utils/request.js`

- token 刷新逻辑整体正确，但 `handleRefresh` 中 `originalRequest._alreadyRetried` 是 monkey-patch，可读性差。
- 错误码 `111010/111011/111022/111023` 硬编码。
- `API_BASE_URL` fallback 为 `/api/v1/user`，如果环境变量配错不易排查。

**修复建议**：
- 错误码抽到常量文件（如 `src/constants/error-codes.js`）。
- 将 `_alreadyRetried` 改为更清晰的请求标记方式。
- fallback 触发时打印 warn，方便本地调试。

---

### 14. 死目录/文件

**路径**: `project/user/web/project/`

该目录为空层级，疑似误创建。

**修复建议**：删除空目录。

---

### 15. 文件扩展名混用

项目同时存在 `.js` 和 `.vue` 文件，但部分 import 显式写 `.js`（如 `import { useDevice } from '@/composables/useDevice.js'`），部分省略。应统一。

**修复建议**：Vite 支持省略 `.js`，建议统一省略；Vue 文件保留 `.vue`。

---

### 16. `index.html` 与 Vite 插件中的缓存策略

`vite.config.js` 第 49-69 行在构建时给入口 JS/CSS 加 `?v=build-${Date.now()}` 版本戳。该逻辑通过字符串替换实现，若 `index.html` 模板变更可能失效。

**修复建议**：使用 Vite 内置的 hash 文件名 + 标准缓存头，减少自定义字符串替换。

---

## 四、修复优先级建议

| 优先级 | 事项 |
|--------|------|
| P0 | 给 `v-html` / `MarkdownIt` 加 XSS 消毒；修 `static-uploads` 路径校验 |
| P1 | 轮询加超时/暂停；清理 `console.*`；~~统一 localStorage key~~；移除外链硬编码 |
| P2 | 合并 PC/Mobile 重复代码；抽取公共工具函数；规范 import 顺序 |
| P3 | 错误码常量化；补充 ESLint/类型检查；删除空目录 |

---

## 五、附录：扫描到的关键位置汇总

### `console.*` 出现位置
- `src/components/AccountCheckModal.vue:168`
- `src/utils/articleStorage.js:23, 37`
- `src/views/console/AccountCheckIndex.vue:179`
- `src/composables/usePlatforms.js:35`
- `src/composables/useSelfMediaPlan.js:32`
- `src/composables/useSkillMarket.js:81, 89, 103, 113, 121, 140`
- `src/composables/useExportTemplates.js:59`
- `src/views/console/WorkbenchIndex.vue:747, 795, 1067`
- `src/views/console/OnboardingIndex.vue:569, 700`
- `src/views/console/EditIndex.vue:159, 339`
- `src/views/console/CreateIndex.vue:103`
- `src/views/console/LeaderboardIndex.vue:259`
- `src/views/console/ConsoleLayout.vue:1976`
- `src/views/console/PreviewIndex.vue:478, 496`
- `src/views/console/WeeklyDataIndex.vue:100`
- `src/views/console/create/MinimalPanel.vue:205`
- `src/views/console/create/modals/SkillModal.vue:434, 448`
- `src/composables/useSkills.js:68`
- `src/views/console/WorksIndex.vue:312, 415, 433, 452, 510`
- `src/views/console/create/MobileCreate.vue:226`

### `v-html` 出现位置
- `src/views/console/create/CreateFlowModal.vue:295, 668`
- `src/views/console/MessagesIndex.vue:65, 95`
- `src/components/guide/GuideArticle.vue:11`
- `src/views/console/PreviewIndex.vue:30, 40`
- `src/components/learn/LearnRichText.vue:2`
- `src/components/learn/LearnMarkdown.vue:2`
- `src/views/MobileGuide.vue:87`
- `src/views/Pricing.vue:100, 101, 102`
- `src/views/console/create/modals/TemplateModal.vue:36`
- `src/views/console/LotteryPage.vue:121`

### 硬编码外部 URL 位置
- `src/views/MobileGuide.vue:7`
- `src/views/MobileHome.vue:7, 264`
- `src/views/console/AccountCheckIndex.vue:103`
- `src/components/AccountCheckModal.vue:90`
- `src/views/MobilePricing.vue:7`
- `src/views/console/PreviewIndex.vue:310, 318, 327, 335, 343, 351, 359`
- `src/views/GuideIndex.vue:23`
- `src/views/Home.vue:295`
- `src/views/console/ConsoleLayout.vue:744, 1133, 1536, 1822, 2704`
- `src/views/console/InviteIndex.vue:185`
- `src/views/console/WorkbenchIndex.vue:960, 1100`
- `src/components/layout/NavBar.vue:6`
- `src/views/MobileLearn.vue:7`
- `src/views/console/create/MobileCreate.vue:6`
- `src/views/console/LotteryPage.vue:281`

### 轮询/定时器位置
- `src/views/Home.vue:324`（banner）
- `src/views/MobileHome.vue:291`（banner）
- `src/views/MobileLearn.vue:344`（banner）
- `src/composables/useGenerationTask.js:47, 50`（任务轮询）
- `src/views/console/create/useGenerationQueue.js:56`（队列轮询）
- `src/composables/useMessages.js:52`（消息角标轮询）
- `src/composables/useQrLogin.js:80, 100`（二维码轮询/倒计时）
- `src/views/QrLoginScan.vue:76`（二维码轮询）
- `src/views/console/ConsoleLayout.vue:2165, 2214`（验证码倒计时）
- `src/views/console/CouponIndex.vue:231`（优惠券过期提醒）
- `src/components/testimonial/TestimonialCarousel.vue:119`（自动滚动）
- `src/views/console/LotteryPage.vue:484, 493, 499, 533`（动画定时器）

---

*报告结束*
