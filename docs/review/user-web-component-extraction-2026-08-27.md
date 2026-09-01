# 用户端 Web 可抽取公共组件分析

**分析日期**: 2026-08-27  
**分析范围**: `project/user/web/src` 下全部 Vue 文件  
**分析人**: Claude Code

---

## 结论

用户端 web 存在**大量可抽取的重复 UI 模式**。最值得优先落地的五类是：**空状态、复制按钮、移动端返回头、Tab 切换头部、内联 SVG 图标**。这些模式重复最多、改造成本最低、收益最大。

**当前进度**：`EmptyState` 空状态组件、`CopyButton` / `CopyRow` 复制组件、`MobileSubpageHeader` 移动端子页面返回头、`Tabs` 自定义切换头部、`Icon` 图标组件、`SectionTitle` 区块标题、`StatCard` / `StatCardGroup` 统计卡片、`SkeletonList` 骨架屏、`useConfirm` 二次确认、`ActionGroup` 操作按钮组、`ListCard` 列表项卡片容器、本地存储 key 统一常量已抽取并迁移完成。

---

## 一、高优先级（建议优先抽取）

### 1. EmptyState 空状态组件 ✅ 已落地

**组件位置**: `src/components/common/EmptyState.vue`

**能力**: 支持 `icon`（Ant Design Vue 图标组件 / emoji / 默认图标）、`title`、`description`、`actionText`、`actionTo`（路由跳转）、`actionHandler`（自定义回调）、`size`、`compact`，并提供 `#icon`、`#title`、`#description`、`#action` 插槽。

**已迁移位置**:
- `src/views/console/PreviewIndex.vue`
- `src/views/console/create/QueueDrawer.vue`
- `src/views/console/OrderIndex.vue`
- `src/views/console/CouponIndex.vue`
- `src/views/console/MessagesIndex.vue`
- `src/views/console/HotSearchIndex.vue`
- `src/views/console/LotteryPage.vue`
- `src/views/console/EarningsIndex.vue`
- `src/views/console/SkillsIndex.vue`
- `src/views/console/SkillMarketIndex.vue`
- `src/views/console/WorksIndex.vue`

**收益**: 统一空状态视觉和交互，减少重复模板代码；移除各页面中 `<a-empty>` 与自定义空状态 div 的混用。`npm run build` 验证通过。

**仍可继续迁移的位置**（本次未处理，后续可逐步替换）：
- `src/views/console/WorkbenchIndex.vue`（多处带引导按钮的复杂空状态）
- `src/views/console/EditIndex.vue`
- `src/views/console/WeeklyDataIndex.vue`
- `src/views/console/ConsoleLayout.vue`（消息下拉空状态）

---

### 2. CopyButton / CopyRow 复制组件 ✅ 已落地

**组件位置**: 
- `src/components/common/CopyButton.vue`
- `src/components/common/CopyRow.vue`
- `src/composables/useCopy.js`

**能力**: 
- `useCopy` 封装复制逻辑（`navigator.clipboard` + `execCommand` 降级）、成功/失败 message 提示、空内容提示、支持异步获取文本。
- `CopyButton` 提供默认/主要/文字/图标四种按钮形态，内置 `loading`、`disabled` 状态。
- `CopyRow` 提供「标签 + 值 + 复制按钮」的一行展示，支持显示值与复制值不一致。

**已迁移位置**:
- `src/views/console/InviteIndex.vue`
- `src/views/console/ProfileEditIndex.vue`
- `src/views/console/EarningsIndex.vue`
- `src/views/console/WorkbenchIndex.vue`
- `src/components/AccountCheckModal.vue`
- `src/views/console/AccountCheckIndex.vue`
- `src/views/console/PreviewIndex.vue`
- `src/views/console/ConsoleLayout.vue`
- `src/views/console/LotteryPage.vue`
- `src/views/console/HotSearchIndex.vue`

**收益**: 统一复制降级方案和成功/失败提示；移除各页面中直接调用 `navigator.clipboard` 与重复手写 `execCommand` 降级的代码。`npm run build` 验证通过。

**说明**: 对于已有强自定义样式的复制按钮（如「我的 ID」胶囊按钮、资料页复制按钮），保留原有 DOM 和样式，仅将复制逻辑替换为 `useCopy`，避免视觉回归。新功能可直接使用 `CopyButton` / `CopyRow`。

---

### 3. MobileSubpageHeader 移动端子页面返回头 ✅ 已落地

**组件位置**: `src/components/common/MobileSubpageHeader.vue`

**能力**: 支持 `title`、`showBack`（默认 true）、`backText`（默认「返回」）、`autoBack`（默认 true）以及 `#right` 插槽；点击返回默认 `router.back()`，也可通过 `@back` 自定义行为。

**已迁移位置**:
- `src/views/MobileWatermarkRemove.vue`
- `src/views/TextToImageToolView.vue`
- `src/views/CutoutToolView.vue`
- `src/views/MobileQrCode.vue`
- `src/views/MobileImageCompress.vue`
- `src/views/console/create/CreateFlowModal.vue`

**收益**: 统一移动端返回行为与视觉样式，移除各工具页重复的返回箭头 SVG、`.mw-subpage-header` / `.cutout-subpage-header` 等样式块，以及冗余的 `useRouter` + `goBack` 代码。`npm run build` 验证通过。

**仍可继续迁移的位置**（本次未处理，后续可逐步替换）：
- `src/views/MobileLearn.vue`

---

### 4. Tabs 自定义切换头部 ✅ 已落地

**组件位置**: `src/components/common/Tabs.vue`

**能力**: 支持 `v-model` 绑定当前值；`tabs` 传入 `{ label, value, count, disabled, dot }`；`variant` 可选 `pill` / `segment`；`activeType` 可选 `primary`（主色底白字）/ `surface`（白底黑字/灰底主色字）；`equalWidth`（均分宽度）、`scrollable`（横向滚动隐藏滚动条）、`sticky`（吸顶）、`shape`（`default` / `pill`，影响 segment 圆角）。内置 count badge（`>99` 显示 `99+`）、`dot` 小圆点、禁用态与暗色主题。

**已迁移位置**:
- `src/views/console/OrderIndex.vue`
- `src/views/console/EarningsIndex.vue`
- `src/views/console/HotSearchIndex.vue`
- `src/views/console/MessagesIndex.vue`
- `src/views/MobileLogin.vue`
- `src/views/MobileGuide.vue`
- `src/views/console/CommissionIndex.vue`

**收益**: 统一 tab 视觉与交互，移除各页面重复的 `.order-tabs` / `.account-tabs` / `.messages-tabs` / `.platform-tabs` / `.ml-tabs` / `.mg-tabs` / `.commission-tabs` 等样式块与手写 button 列表；count badge 与横向滚动行为统一。`npm run build` 验证通过。

**说明**: 
- 消息页 tab 数组由 `type` 字段统一改为 `value`；约稿中心 `tabItems` 由 `key` 改为 `value`。
- 热搜榜平台 tab 通过 `dot: true` 保留原有的平台色点前缀。
- `MobileGuide` 保留外层 `.mg-tabs-wrap` 以保证 `top: 57px` 的吸顶位置，内部替换为 `Tabs`。
- `OrderIndex` 桌面端原有的下划线 tab 样式一并收归到 segment 胶囊样式，实现全端统一。

---

### 5. Icon 图标组件 ✅ 已落地

**组件位置**: `src/components/common/Icon.vue`

**能力**: 基于 SVG path 映射的通用图标组件，支持 `name`、`size`、`strokeWidth`、`fill`；统一 `viewBox="0 0 24 24"`、`stroke="currentColor"`、`stroke-linecap="round"`、`stroke-linejoin="round"`，与现有 Feather/Ant Design 风格保持一致。新增图标包括：
- 基础方向/操作：`arrow-left`、`arrow-right`、`chevron-left`、`chevron-right`、`chevron-up`、`chevron-down`、`plus`、`close`、`check`、`menu`
- 常用状态：`info`、`info-circle`、`alert-circle`、`question-circle`
- 通用对象：`home`、`mail`、`lock`、`eye`、`eye-off`、`user`、`users`、`settings`、`log-out`、`search`、`trash`、`download`、`upload`、`copy`、`external-link`、`share`、`refresh`
- 业务图标：`bell`、`file`、`file-minus`、`star`、`clock`、`grid`、`image`、`inbox`、`scissors`、`picture`、`gift`、`wallet`、`shield`、`crown`、`message-circle`、`message-square`、`fire`、`zap`、`heart`、`trending-up`、`dollar-sign`、`coin`、`award`、`trophy`、`shopping-cart`、`book-open`、`layout`、`pie-chart`、`edit`、`globe`、`ticket`

**已迁移位置**:
- `src/views/MobileLogin.vue`（登录/注册表单字段图标、密码显隐、邀请信息图标）
- `src/views/MobileGuide.vue`（菜单图标）
- `src/views/MobileHome.vue`（菜单、feature 图标）
- `src/views/Home.vue`（feature 与收益玩法图标）
- `src/views/console/MessagesIndex.vue`（空状态 bell、消息类型图标）
- `src/views/console/ActivitiesIndex.vue`（徽章、活动卡片、箭头图标）
- `src/views/console/ConsoleLayout.vue`（通知铃铛、教程/反馈/关于/官网图标、用户中心复制/编辑/密码/兑换码/退出图标、移动端返回箭头）
- `src/views/console/create/CreateFlowModal.vue`（观点选中勾选、删除 chip 关闭图标）

**收益**: 将散落在模板中的 `<svg>` 路径集中到 `Icon.vue` 统一管理，减少重复代码；通过 `currentColor` 继承父级颜色，使暗色主题与 hover 状态可通过 CSS `color` 统一控制。`npm run build` 验证通过。

**说明**:
- 组件默认 `fill="none"`，需要填充的图标可通过 `fill` prop 覆盖。
- 对于带固定品牌色的场景（如首页 feature 图标），将颜色从 SVG 的 `stroke` 属性迁移到父容器的 `color`，保持视觉一致。
- 仍有少量复杂/装饰性 SVG（如 `Home.vue` 的 `asset-chart`、Captcha 组件）保留原样，后续可视情况处理。

---

## 二、中优先级（可后续逐步抽取）

### 6. SectionTitle 区块标题 ✅ 已落地

**组件位置**: `src/components/common/SectionTitle.vue`

**能力**: 支持 `title`、`subtitle`、`tag`（标签/徽章文字）、`bar`（左侧色条）、`pill`（渐变胶囊标签）、`centered`（居中布局）、`size`（`sm`/`md`/`lg`）、`disabled` 态；提供 `#default` 和 `#right` 插槽。覆盖左条标题、渐变徽章标题、居中 tag+标题+副标题、简单小标题、标题+右侧操作等常见模式。

**已迁移位置**:
- `src/views/console/LotteryPage.vue`（5 处左条色块标题，含一处带右侧「展开/收起」切换）
- `src/views/console/LeaderboardIndex.vue`（2 处渐变 pill 标签 + 标题）
- `src/views/console/PreviewIndex.vue`（4 处发布描述/推荐标签/冷启动策略/一文多发方案，含带右侧升级 badge）
- `src/views/Home.vue`（特色功能、收益玩法 2 处居中 tag+标题+副标题）
- `src/views/console/BenefitsIndex.vue`（权益明细简单小标题）
- `src/views/console/SkillMarketIndex.vue`（全部提示词标题 + 数量副标题）

**收益**: 将 6 种页面中重复的标题结构（色条、pill 标签、居中 tag、右侧操作、内联副标题）集中到 `SectionTitle`；删除各页面中重复的 `.section-title`、`.leaderboard-section-header`、`.meta-section-title`、`.features-title`、`.benefits-section-title`、`.market-section-title` 等样式块。`npm run build` 验证通过。

**说明**:
- `LotteryPage` 的「活动规则」标题保留了 `@click` 和 `.rules-section__header` 类，右侧切换文案通过 `#right` 插槽注入。
- `PreviewIndex` 的「推荐标签」保留 `a-popover` 帮助图标和 `pro-badge-mini`，通过默认插槽与 `#right` 插槽实现。
- `Home.vue` 保留外层 `.features-header` / `.earnings-header` 以兼容 `reveal` 滚动动画，内部替换为 `SectionTitle`。

---

### 7. StatCard 统计卡片 ✅ 已落地

**组件位置**:
- `src/components/common/StatCard.vue`
- `src/components/common/StatCardGroup.vue`

**能力**:
- `StatCard` 支持 `label`、`value`、`unit`、`hint`、`image`、`variant`、`valueFirst` 等 props，并提供 `#label`、`#value`、`#hint`、`#action`、`#image` 插槽。
- `variant` 预设多种视觉风格：`default`（白底卡片）、`flat`（无阴影浅卡片）、`primary`（粉色渐变主卡片）、`gradient`（浅粉渐变卡片）、`commission`（带右下角插图的渐变卡片）、`muted`（灰底紧凑卡片）、`glass`（毛玻璃卡片）、`transparent`（透明居中）。
- `valueFirst` 支持数值在上的布局，方便账户余额类卡片。
- `StatCardGroup` 支持 `columns`、`inline`、`gap`，作为统计卡片的网格/弹性容器。

**已迁移位置**:
- `src/views/console/EarningsIndex.vue`（账户余额、累计收益）
- `src/views/console/WithdrawIndex.vue`（可提现金额、已提现金额）
- `src/views/console/CommissionIndex.vue`（PC hero 统计、移动端进行/投稿统计卡片）
- `src/views/console/ConsoleLayout.vue`（邀请有礼弹框中的邀请/奖励/余额统计）

**收益**: 统一收益、余额、额度等统计卡片的视觉与结构，减少各页面重复的 `.account-stat-card`、`.coin-stat-card`、`.invite-stat-item`、`.commission-stats .stat-card` 等模板与样式；通过 `variant` 保留各页面原有视觉风格。`npm run build` 验证通过。

---

### 8. localStorage Key 统一常量 ✅ 已落地

**常量文件位置**: `src/constants/storage.js`

**能力**:
- 统一所有 `localStorage` key 的前缀与命名，避免各文件硬编码字符串。
- 提供 `STORAGE_KEYS` 常量对象覆盖认证、用户、主题、创作、账户、新手引导、兑换码、自媒体方案等全部 key。
- 提供用户隔离 key 的 helper：`getAccountCheckLastKey(userId)`、`getAccountRecommendLastKey(userId)`、`getOnboardingDraftKey(userId)`、`getOnboardingDoneKey(userId)`。
- 提供动态 key helper：`getTodayDoneKey(date)`、`getCouponWarnKey(couponId)`。
- 维护 `USER_SCOPED_STORAGE_KEYS` 数组，供登录态切换时统一清理用户相关缓存。

**已迁移位置**:
- `src/router/index.js`
- `src/utils/request.js`
- `src/utils/articleStorage.js`
- `src/utils/membershipLimits.js`
- `src/composables/useLogin.js`
- `src/composables/usePricing.js`
- `src/composables/useLearn.js`
- `src/composables/useSkills.js`
- `src/views/console/create/useCreateForm.js`
- `src/views/console/WithdrawIndex.vue`
- `src/views/console/OnboardingIndex.vue`
- `src/views/console/ConsoleLayout.vue`
- `src/views/console/WorkbenchIndex.vue`
- `src/views/console/CreateIndex.vue`
- `src/views/console/CreateFreePage.vue`
- `src/views/console/CreateRecommendedPage.vue`
- `src/views/console/WorksIndex.vue`
- `src/views/console/SkillsIndex.vue`
- `src/views/console/SkillMarketIndex.vue`
- `src/views/console/AccountCheckIndex.vue`
- `src/views/console/AccountCheckModal.vue`
- `src/views/console/CouponIndex.vue`
- `src/views/console/LotteryPage.vue`
- `src/views/QrLoginScan.vue`
- `src/components/layout/NavBar.vue`
- `src/components/guide/LeaderboardPreview.vue`
- `src/components/CreateFlowLauncher.vue`

**收益**: 消除 `aichuangzuo_*` 字符串在业务代码中的重复硬编码；用户隔离 key 的生成规则集中管理，避免切换账号后缓存污染；新增 key 可直接在 `STORAGE_KEYS` 中维护，降低遗漏和命名冲突风险。`npm run build` 验证通过。

**说明**:
- 约稿中心引导（`OnboardingIndex.vue`）的 draft/done key 改为按用户 ID 动态计算，未登录时使用通用 key。
- 账号检测相关 key 在获取到当前用户 ID 后，会清理旧版未区分用户的缓存，避免切换账号后看到他人数据。
- 会员相关 key 统一为 `STORAGE_KEYS.MEMBERSHIP`，旧演示数据（如 `"年会员"` 字符串）和旧 string 格式仍兼容读取并迁移为新格式。

---

### 9. SkeletonList 骨架屏 ✅ 已落地

**组件位置**: `src/components/common/SkeletonList.vue`

**能力**: 支持 `type`（`card` / `list`）、`rows`、`gap`、`paragraphRows`；`type="card"` 使用 `a-skeleton` 生成卡片式段落骨架；`type="list"` 生成带头像/图标占位和多条文字行的列表骨架，支持 `avatar`、`avatarRound`、`lines`、`lineWidths`、`lineHeights`、`active`（脉冲动画）。

**已迁移位置**:
- `src/views/console/OrderIndex.vue`（移动端订单卡片加载骨架）
- `src/views/console/CouponIndex.vue`（移动端优惠券卡片加载骨架）
- `src/views/console/MessagesIndex.vue`（消息列表加载骨架）

**收益**: 统一卡片与列表两种骨架屏实现，移除各页面重复的 `<a-skeleton>` 循环、`.order-skeleton`、`.coupon-skeleton`、`.message-skeleton` 等模板与样式，暗色主题由组件统一处理。`npm run build` 验证通过。

---

### 10. ConfirmModal / useConfirm 二次确认 ✅ 已落地

**组合式函数位置**: `src/composables/useConfirm.js`

**能力**: 在 `ant-design-vue` 的 `Modal.confirm` 之上统一常用配置，默认 `centered: true`、取消文案为「取消」；支持 `danger` 快捷设置确认按钮为危险样式；保留 `okButtonProps`、`wrapClassName` 等属性用于自定义。

**已迁移位置**:
- `src/views/console/SkillsIndex.vue`（删除提示词、删除学习提示词、取消收藏、下架提示词）
- `src/views/console/create/QueueDrawer.vue`（停止生成任务）
- `src/views/console/create/FreeCreateModal.vue`（订阅引导、额度用完引导）
- `src/views/console/create/MobileCreate.vue`（订阅引导、额度用完引导）
- `src/views/console/WorksIndex.vue`（删除草稿、删除作品）

**收益**: 将分散的 `Modal.confirm({...})` 调用收敛到 `useConfirm`，统一默认居中、取消文案和危险按钮样式；移除 4 个文件中直接引入的 `Modal`，降低与 Ant Design Vue 的耦合。保留各页面原有的 `wrapClassName` 与 `okButtonProps`，无视觉回归。`npm run build` 验证通过。

**说明**:
- 当前所有二次确认均为命令式调用，`useConfirm` 已覆盖全部场景，暂未单独抽取 `ConfirmModal.vue` 模板组件。
- 若后续出现需要在模板中声明控制的确认弹框，再考虑补充 `ConfirmModal.vue`。

**建议方案**:
```js
const { confirm } = useConfirm()
confirm({
  title: '删除提示词',
  content: '确定删除吗？删除后不可恢复。',
  okText: '删除',
  danger: true,
  onOk: deleteSkill
})
```

---

## 三、低优先级（可视情况抽取）

### 11. PageHeader 页面头部

**重复位置**:
- `src/views/console/OrderIndex.vue:3-10`
- `src/views/console/EarningsIndex.vue:3-6`
- `src/views/console/WorksIndex.vue`

**建议组件**:
```vue
<PageHeader title="我的账户" subtitle="查看账户余额、收益明细" />
```

---

### 12. ActionGroup 操作按钮组 ✅ 已落地

**组件位置**: `src/components/common/ActionGroup.vue`

**能力**: 支持 `actions` 数组，每个动作可配置 `label`、`handler`、`type`（`default` / `primary` / `danger` / `success` / `active` / `outline`）、`visible`、`disabled`、`title`、`badge`、`class`；提供 `variant`（`default` / `link` / `bar`）、`size`（`small` / `default` / `large`）、`vertical`、`wrap`、`tag` 等 props；通过 `@action` 事件也可统一响应。

**已迁移位置**:
- `src/components/SkillCard.vue`（我的提示词 / 学习的提示词 / 收藏的提示词 / 提示词市场的操作列）
- `src/views/console/WorkbenchIndex.vue`（生成记录操作：一文多发、查看）

**收益**: 将 `SkillCard` 内部重复的操作按钮渲染逻辑抽到 `ActionGroup`，统一按钮样式、hover、禁用、危险/主色/成功态、角标；`SkillMarketIndex.vue` 与 `SkillsIndex.vue` 因复用 `SkillCard` 一并受益。`WorkbenchIndex.vue` 使用 `variant="link"` 替换手写 `<a-button type="link">` 组。`npm run build` 验证通过。

**说明**:
- `PreviewIndex.vue` 的浮动操作栏按钮样式高度定制（`float-btn` / `title-opt-btn` / `is-disabled` 等），暂不纳入 `ActionGroup`，避免视觉回归。

**建议组件**:
```vue
<ActionGroup :actions="[
  { label: '查看', handler: openDetail },
  { label: '删除', danger: true, handler: handleDelete }
]" />
```

---

### 13. ListCard 列表项卡片容器 ✅ 已落地

**组件位置**: `src/components/common/ListCard.vue`

**能力**: 提供通用列表卡片容器，支持 `clickable`（点击态）、`hover`（悬浮效果）、`tag`（渲染标签，默认 `div`）、`customClass`；提供 `#header`、`#body`、`#footer` 插槽以及默认插槽，满足多种列表项布局。

**已迁移位置**:
- `src/views/console/OrderIndex.vue`（移动端订单卡片）
- `src/views/console/MessagesIndex.vue`（消息卡片，含图标 + 正文 + 未读点）
- `src/views/console/EarningsIndex.vue`（月度收益条目，两处）
- `src/views/console/CommissionIndex.vue`（约稿任务卡片、我的投稿卡片）

**收益**: 统一列表卡片的白底、圆角、阴影、hover 抬升与边框高亮；各页面只保留业务布局样式，移除重复的 `background`、`border-radius`、`box-shadow`、`cursor`、`transition` 等容器样式。支持暗色主题。`npm run build` 验证通过。

**说明**:
- 对于不需要阴影/hover 的轻量条目（如 `EarningsIndex` 月度收益），通过 `:hover="false"` 关闭悬浮效果，并在业务类中覆盖背景色。
- 业务卡片的布局、字号、状态标签等仍由各页面维护，保持视觉不变。

**建议**: 先抽取通用的卡片容器（圆角、阴影、hover、padding），再按需扩展业务卡片。

---

## 四、落地建议

### 推荐抽取顺序

1. **~~EmptyState~~** ✅ 已完成
2. **~~CopyButton / CopyRow~~** ✅ 已完成
3. **~~MobileSubpageHeader~~** ✅ 已完成
4. **~~Tabs~~** ✅ 已完成
5. **~~Icon 图标组件~~** ✅ 已完成
6. **~~SectionTitle~~** ✅ 已完成
7. **~~StatCard / StatCardGroup~~** ✅ 已完成
8. **~~localStorage Key 统一常量~~** ✅ 已完成
9. **~~SkeletonList~~** ✅ 已完成
10. **~~ConfirmModal / useConfirm~~** ✅ 已完成 — 统一二次确认弹框
11. **PageHeader** — 统一页面头部标题区
12. **~~ActionGroup~~** ✅ 已完成 — 统一操作按钮组
13. **~~ListCard~~** ✅ 已完成 — 统一列表项卡片容器

### 目录建议

```text
src/components/common/
  ├── EmptyState.vue
  ├── CopyButton.vue
  ├── CopyRow.vue
  ├── MobileSubpageHeader.vue
  ├── Tabs.vue
  ├── Icon.vue
  ├── SectionTitle.vue
  ├── StatCard.vue
  ├── StatCardGroup.vue
  ├── SkeletonList.vue
  ├── ActionGroup.vue
  └── ListCard.vue

src/composables/
  ├── useCopy.js
  ├── useConfirm.js
  └── ...

src/constants/
  └── storage.js
```

### 注意事项

- 抽取时保持现有样式不变，避免视觉回归。
- 优先使用 props + slot，保留足够的扩展性。
- 对于业务强相关的卡片（如订单卡片、消息卡片），先保证容器通用，再封装业务组件。
- 抽取后应运行 `npm run build` 验证无引用错误。

---

*分析结束*
