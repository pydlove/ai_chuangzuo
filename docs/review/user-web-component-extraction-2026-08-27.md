# 用户端 Web 可抽取公共组件分析

**分析日期**: 2026-08-27  
**分析范围**: `project/user/web/src` 下全部 Vue 文件  
**分析人**: Claude Code

---

## 结论

用户端 web 存在**大量可抽取的重复 UI 模式**。最值得优先落地的五类是：**空状态、复制按钮、移动端返回头、Tab 切换头部、内联 SVG 图标**。这些模式重复最多、改造成本最低、收益最大。

**当前进度**：`EmptyState` 空状态组件、`CopyButton` / `CopyRow` 复制组件已抽取并迁移完成。

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

### 3. MobileSubpageHeader 移动端子页面返回头

**重复位置**:
- `src/views/MobileWatermarkRemove.vue:3-12`
- `src/views/TextToImageToolView.vue:3-12`
- `src/views/CutoutToolView.vue:3-12`
- `src/views/MobileQrCode.vue:3-12`
- `src/views/MobileImageCompress.vue:3-12`
- `src/views/MobileLearn.vue:14`
- `src/views/console/create/CreateFlowModal.vue:341-344`

**问题**: 移动端工具页/子页面都有几乎一样的返回头：左侧返回箭头 + 标题，有的右侧带操作按钮。

**建议组件**:
```vue
<MobileSubpageHeader
  title="AI 抠图"
  show-back
  @back="router.back()"
>
  <template #right><slot /></template>
</MobileSubpageHeader>
```

**收益**: 统一移动端返回行为，避免每个页面各自维护返回箭头 SVG。

---

### 4. Tabs 自定义切换头部

**重复位置**:
- `src/views/console/OrderIndex.vue:13-24`
- `src/views/console/EarningsIndex.vue:8-21`
- `src/views/console/HotSearchIndex.vue:37-45`
- `src/views/console/MessagesIndex.vue:20-30`
- `src/views/MobileLogin.vue:24-34`
- `src/views/MobileGuide.vue:52-60`
- `src/views/console/CommissionIndex.vue:39-50`

**问题**: 每个页面都手写 button 列表做 tab，active 样式、count badge、横向滚动各自实现。

**建议组件**:
```vue
<Tabs
  v-model="activeTab"
  :tabs="[
    { label: '全部', value: 'all' },
    { label: '未读', value: 'unread', count: unreadCount }
  ]"
  show-count
/>
```

**收益**: 统一 tab 视觉和交互，支持 count badge、横向滚动。

---

### 5. Icon 图标组件 / SVG Sprite

**重复位置**:
- `src/views/Home.vue` / `src/views/MobileHome.vue`（大量 feature icon）
- `src/views/MobileLogin.vue`
- `src/views/console/ActivitiesIndex.vue`
- `src/views/console/MessagesIndex.vue`
- `src/views/CutoutToolView.vue`
- `src/views/console/create/TopicCapsules.vue`
- `src/views/console/PreviewIndex.vue`
- 几乎所有页面都有内联 SVG

**问题**: 大量 `<svg viewBox="0 0 24 24" ...>` 直接写在模板里，重复且难以维护。常用图标（arrow-left、copy、check、bell、upload、eye、trash）反复出现。

**建议方案**:
- 方案 A：建立 `<Icon name="arrow-left" :size="20" color="#07c160" />` 通用组件
- 方案 B：使用 SVG sprite + `<svg><use href="#icon-copy" /></svg>`
- 方案 C：直接使用 `@ant-design/icons-vue` 已提供的图标，减少自定义 SVG

**收益**: 大幅减少模板体积，统一图标风格，便于主题切换。

---

## 二、中优先级（可后续逐步抽取）

### 6. SectionTitle 区块标题

**重复位置**:
- `src/views/console/LeaderboardIndex.vue:65/108`
- `src/views/console/LotteryPage.vue:31/54/115/127/157`
- `src/views/console/PreviewIndex.vue:47/57/128/144`
- `src/views/Home.vue:87/173`
- `src/views/console/BenefitsIndex.vue:30`
- `src/views/console/SkillMarketIndex.vue:133`

**建议组件**:
```vue
<SectionTitle tag="荣耀榜" title="TOP 3" />
<SectionTitle title="发布描述" :bar="true" />
```

---

### 7. StatCard 统计卡片

**重复位置**:
- `src/views/console/EarningsIndex.vue:26-45`
- `src/views/console/WithdrawIndex.vue:55-70`
- `src/views/console/CommissionIndex.vue:41-57`
- `src/views/console/ConsoleLayout.vue:107-133`

**建议组件**:
```vue
<StatCard label="账户余额" :value="summary.coinBalance" unit="创作币" primary />
<StatCardGroup :items="stats" />
```

---

### 8. SkeletonList 骨架屏

**重复位置**:
- `src/views/console/OrderIndex.vue:43-45`
- `src/views/console/CouponIndex.vue:43-45`
- `src/views/console/MessagesIndex.vue:37-43`

**建议组件**:
```vue
<SkeletonList :rows="3" type="card" />
```

---

### 9. ConfirmModal / useConfirm 二次确认

**重复位置**:
- `src/views/console/SkillsIndex.vue:1317/1504/1523/1593`
- `src/views/console/create/QueueDrawer.vue:110-130`
- `src/views/console/create/FreeCreateModal.vue:256/276`
- `src/views/console/create/MobileCreate.vue:232/252`
- `src/views/console/WorksIndex.vue:419-455`

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

### 10. PageHeader 页面头部

**重复位置**:
- `src/views/console/OrderIndex.vue:3-10`
- `src/views/console/EarningsIndex.vue:3-6`
- `src/views/console/WorksIndex.vue`

**建议组件**:
```vue
<PageHeader title="我的账户" subtitle="查看账户余额、收益明细" />
```

---

### 11. ActionGroup 操作按钮组

**重复位置**:
- `src/views/console/PreviewIndex.vue:197-201`
- `src/views/console/WorkbenchIndex.vue`
- `src/views/console/SkillsIndex.vue` 操作列
- `src/views/console/SkillMarketIndex.vue:174-179`

**建议组件**:
```vue
<ActionGroup :actions="[
  { label: '查看', handler: openDetail },
  { label: '删除', danger: true, handler: handleDelete }
]" />
```

---

### 12. ListCard 列表项卡片容器

**重复位置**:
- `src/views/console/OrderIndex.vue:46-75`
- `src/views/console/MessagesIndex.vue:57-80`
- `src/views/console/EarningsIndex.vue:56-72`
- `src/views/console/CommissionIndex.vue`

**建议**: 先抽取通用的卡片容器（圆角、阴影、hover、padding），再按需扩展业务卡片。

---

## 四、落地建议

### 推荐抽取顺序

1. **~~EmptyState~~** ✅ 已完成
2. **~~CopyButton / CopyRow~~** ✅ 已完成
3. **Icon 图标组件** — 影响面最大，先统一图标
4. **MobileSubpageHeader** — 移动端体验统一
5. **Tabs** — 减少大量自定义 tab 代码
6. **SectionTitle / StatCard** — 提升页面一致性
7. **SkeletonList / ConfirmModal** — 完善基础组件库

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
  └── ActionGroup.vue

src/composables/
  ├── useCopy.js
  └── useConfirm.js
```

### 注意事项

- 抽取时保持现有样式不变，避免视觉回归。
- 优先使用 props + slot，保留足够的扩展性。
- 对于业务强相关的卡片（如订单卡片、消息卡片），先保证容器通用，再封装业务组件。
- 抽取后应运行 `npm run build` 验证无引用错误。

---

*分析结束*
