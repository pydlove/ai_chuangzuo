# 发布平台选择 + SEO 描述/标签方案

**日期**: 2026-06-26  
**状态**: 待实现  
**关联原型**: `.superpowers/brainstorm/6491-1782131242/content/create.html`, `preview.html`, `shared.js`

---

## 1. 功能概述

在创作页增加显式的「发布平台」选择，并将平台选择作为上游参数，联动导出模板、字数要求的默认值；在预览页根据所选平台自动生成对应的「发布描述」和「推荐标签」。

覆盖平台：公众号、小红书、今日头条、百家号、知乎、抖音图文、通用。

---

## 2. 设计目标

- 让用户在创作时就明确目标平台，减少预览/导出时的反复调整。
- 描述和标签按平台规则生成，直接可用于各平台发布。
- 交互风格与现有「字数设置 / 风格库 / 模板库」保持一致。

---

## 3. 用户流程

1. 用户进入创作页。
2. 选择发布平台（默认公众号）。
3. 平台联动：导出模板、字数要求默认切换到该平台推荐值。
4. 用户点击「生成文章」。
5. 预览页展示文章正文 + 当前平台对应的「发布描述」和「推荐标签」。
6. 用户可以复制描述、复制标签、换一批描述/标签。

---

## 4. 创作页：发布平台选择

### 4.1 位置

放在创作表单的模块区域内，与「字数要求 / 文章风格 / 导出模板」并列，建议放在最上方（作为最先决定全局的参数）。

### 4.2 UI 结构

与现有模块保持一致的布局：

```html
<div>
  <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px;">
    <label style="font-weight: 500; color: #262626;">发布平台</label>
    <button onclick="openPlatformLibrary()" style="background: none; border: none; color: #07c160; font-size: 13px; cursor: pointer; font-weight: 500;">+ 平台选择</button>
  </div>
  <div id="pc-current-platform" class="current-style-chip" data-platform="wechat" style="...">
    <span style="color: #595959;">当前发布平台：</span>
    <span id="pc-current-platform-name" style="color: #07c160; font-weight: 600;">公众号</span>
  </div>
</div>
```

移动端结构相同，字号和间距按移动端比例缩小。

### 4.3 当前状态 chip

- 左侧灰色文字：当前发布平台：
- 右侧绿色文字：平台名称
- `data-platform` 属性记录当前平台 key，供其他模块读取

---

## 5. 平台选择弹窗（`openPlatformLibrary`）

### 5.1 弹窗结构

复用现有模板库弹窗的视觉风格：

- 白色卡片、圆角、深色遮罩、右上角关闭按钮
- 标题：选择发布平台
- 副标题：选择目标平台，AI 将按平台规则推荐模板、字数和标签
- 内容区：平台卡片网格
- 底部：取消 / 确认按钮

### 5.2 平台卡片

每个卡片展示：

- 平台名称（如「公众号」）
- 一句话说明（如「适合深度长文和订阅号推送」）
- 选中态：绿色边框 + 绿色背景

布局：PC 端 2 列或 3 列网格；移动端 1 列。

### 5.3 交互

- 点击卡片 → 更新选中态
- 点击「确认」→ 更新创作页 `currentPublishPlatform` 和 chip，关闭弹窗
- 点击「取消」或遮罩 → 关闭弹窗，不应用

---

## 6. 平台联动规则

选中平台后，自动更新以下默认值（用户仍可手动修改）：

| 平台 key | 默认模板 key | 默认模板名称 | 默认字数 | 默认字数标签 |
|---|---|---|---|---|
| wechat | `wechat` | 公众号标准模板 | 1500 | 标准深度文 |
| xiaohongshu | `xiaohongshu` | 小红书图文模板 | 500 | 图文分享 |
| toutiao | `toutiao` | 今日头条模板 | 1500 | 专题分析 |
| baijiahao | `baijiahao` | 百家号模板 | 1500 | 生活攻略 |
| zhihu | `zhihu-answer` | 知乎回答体 | 1500 | 专业回答 |
| douyin | `douyin-graphic` | 抖音图文模板 | 300 | 图配文 |
| general | `business` | 简约商务模板 | 1500 | 标准 |

> 注：默认模板 key 以 `templatePresets` 中实际存在的 key 为准；如果对应 key 不存在，保持当前模板不变。用户在「模板库」中手动选择跨平台模板时，不反向修改当前发布平台。

---

## 7. 预览页：发布描述 & 推荐标签

### 7.1 位置

在文章正文下方增加一个「发布描述 & 标签」卡片区域。PC 端和移动端都直接展示，无需额外点击。

### 7.2 UI 结构

```html
<div id="publish-meta-card" style="background: #fff; border-radius: 12px; padding: 20px; margin-top: 16px;">
  <div style="font-weight: 600; color: #1a1a1a; margin-bottom: 12px;">发布描述</div>
  <textarea id="pc-publish-desc" style="...">...</textarea>
  <div style="display: flex; justify-content: flex-end; gap: 8px; margin-top: 8px;">
    <button onclick="regeneratePublishDesc()">换一版</button>
    <button onclick="copyPublishDesc()">复制描述</button>
  </div>

  <div style="font-weight: 600; color: #1a1a1a; margin: 20px 0 12px;">推荐标签</div>
  <div id="pc-publish-tags" style="display: flex; flex-wrap: wrap; gap: 8px;">
    <span class="publish-tag" style="...">#时间管理</span>
  </div>
  <div style="display: flex; justify-content: flex-end; gap: 8px; margin-top: 12px;">
    <button onclick="regeneratePublishTags()">换一批</button>
    <button onclick="copyPublishTags()">复制全部标签</button>
  </div>
</div>
```

### 7.3 描述区

- 多行文本框，默认可编辑
- 根据平台生成不同长度和风格的描述
- 提供「换一版」按钮，从预设模板中随机切换
- 提供「复制描述」按钮

### 7.4 标签区

- 以 chip 形式展示推荐标签
- 点击单个标签可复制该标签
- 提供「换一批」按钮，随机切换一组标签
- 提供「复制全部标签」按钮，按平台格式拼接复制

---

## 8. 按平台的输出规则

| 平台 | 描述长度 | 描述风格 | 标签数量 | 标签格式 |
|---|---|---|---|---|
| 公众号 | 100-150 字 | 正式摘要 | 3-5 个 | 纯关键词，不带 # |
| 小红书 | 80-120 字 | 种草、口语化 | 8-12 个 | `#关键词` |
| 今日头条 | 80-120 字 | 资讯摘要 | 5-8 个 | 领域关键词，可带 # |
| 百家号 | 80-120 字 | 知识科普摘要 | 4-6 个 | 纯关键词或 #关键词 |
| 知乎 | 80-120 字 | 问题引入式 | 3-5 个 | 话题词，不带 # |
| 抖音图文 | 30-60 字 | 短文案、引导互动 | 5-8 个 | `#热点标签` |
| 通用 | 80-120 字 | 标准摘要 | 6-8 个 | `#关键词` |

### 8.1 标签复制格式

- 小红书 / 抖音图文 / 通用：标签之间用空格连接，例如 `#时间管理 #自律打卡 #职场干货`
- 公众号 / 知乎：标签之间用逗号连接，例如 `时间管理，职场效率，自我提升`
- 今日头条 / 百家号：标签之间用空格或逗号均可，默认用空格

---

## 9. 数据结构

### 9.1 平台列表

```javascript
var publishPlatforms = [
  { key: 'wechat', name: '公众号', desc: '适合深度长文和订阅号推送' },
  { key: 'xiaohongshu', name: '小红书', desc: '种草图文，重标签和封面' },
  { key: 'toutiao', name: '今日头条', desc: '资讯和观点长文' },
  { key: 'baijiahao', name: '百家号', desc: '百度生态搜索流量' },
  { key: 'zhihu', name: '知乎', desc: '问答和专业分析' },
  { key: 'douyin', name: '抖音图文', desc: '短视频配图和短文案' },
  { key: 'general', name: '通用', desc: '不指定平台，通用输出' }
];

var currentPublishPlatform = 'wechat';
```

### 9.2 平台默认配置

```javascript
var platformDefaults = {
  wechat: { template: 'wechat', wordCount: 1500, wordLabel: '标准深度文' },
  xiaohongshu: { template: 'xiaohongshu', wordCount: 500, wordLabel: '图文分享' },
  toutiao: { template: 'toutiao', wordCount: 1500, wordLabel: '专题分析' },
  baijiahao: { template: 'baijiahao', wordCount: 1500, wordLabel: '生活攻略' },
  zhihu: { template: 'zhihu-answer', wordCount: 1500, wordLabel: '专业回答' },
  douyin: { template: 'douyin-graphic', wordCount: 300, wordLabel: '图配文' },
  general: { template: 'business', wordCount: 1500, wordLabel: '标准' }
};
```

### 9.3 描述模板

```javascript
var publishDescTemplates = {
  wechat: [
    '本文围绕「{title}」展开，总结了 {count} 个实用方法，建议收藏转发。',
    '关于「{title}」，我们梳理了 {count} 个关键要点，适合公众号读者深度阅读。'
  ],
  xiaohongshu: [
    '{title} 真的很有用！{count} 个小技巧，建议姐妹们收藏～\n#自我提升',
    '亲测有效！{title}，{count} 个方法帮你快速上手。'
  ],
  toutiao: [
    '{title} 你怎么看？本文梳理了 {count} 个核心观点，欢迎评论交流。',
    '关于「{title}」的深度解读，{count} 个要点帮你快速抓住重点。'
  ],
  baijiahao: [
    '本文从「{title}」出发，总结了 {count} 个实用知识点，建议收藏。',
    '关于「{title}」的科普解读，{count} 个要点帮你建立系统认知。'
  ],
  zhihu: [
    '谢邀。针对「{title}」，分享 {count} 个我认为最关键的要点。',
    '{title} 这个问题，核心在于 {count} 个方面，下面逐一说明。'
  ],
  douyin: [
    '{title}，{count} 个方法直接抄作业！\n\n你做到了几个？评论区见',
    '{title} 亲测有效，{count} 个技巧，快@需要的朋友来看'
  ],
  general: [
    '本文围绕「{title}」展开，分享了 {count} 个实用方法。',
    '关于「{title}」，整理了 {count} 个关键要点，希望对你有帮助。'
  ]
};
```

> 占位符说明：`{title}` 替换为文章标题；`{count}` 替换为文章核心方法数量（mock 数据可固定为 3-5）。

### 9.4 标签预设

```javascript
var publishTagPresets = {
  wechat: ['时间管理', '职场效率', '自我提升', '自律', '成长'],
  xiaohongshu: ['#时间管理', '#自律打卡', '#职场干货', '#效率神器', '#自我提升', '#生活方式', '#打工人', '#成长笔记'],
  toutiao: ['#时间管理', '#职场', '#效率提升', '#自我成长', '#干货分享'],
  baijiahao: ['时间管理', '职场效率', '自我提升', '知识科普', '生活技巧'],
  zhihu: ['时间管理', '职场效率', '自我提升', '个人成长', '自律'],
  douyin: ['#时间管理', '#自律', '#职场干货', '#效率提升', '#自我提升', '#成长'],
  general: ['#时间管理', '#自我提升', '#职场效率', '#干货分享', '#自律', '#成长']
};
```

---

## 10. 状态管理

- 新增全局变量 `currentPublishPlatform`，默认 `'wechat'`。
- 平台选择弹窗内维护临时选中状态，确认后回写到全局变量。
- 预览页根据 `currentPublishPlatform` 渲染描述和标签。
- 描述和标签每次打开预览页时自动生成，不持久化。

---

## 11. 涉及原型页面

- `create.html`：增加发布平台选择模块（PC + Mobile）
- `preview.html`：增加发布描述 & 推荐标签卡片（PC + Mobile）
- `shared.js`：增加 `publishPlatforms`、`platformDefaults`、`publishDescTemplates`、`publishTagPresets`、`openPlatformLibrary()`、`renderPublishMeta()` 等函数

---

## 12. 技术要点

- 纯前端 HTML/CSS/JS 原型，无后端依赖。
- 弹窗 DOM 动态生成，与现有模板库弹窗风格保持一致。
- 描述和标签使用静态 mock 数据，模拟 AI 生成效果。
- 平台 key 与现有模板 key 保持一致（`wechat`、`xiaohongshu`、`toutiao`、`baijiahao`、`zhihu`、`douyin`、`general`）。
- 创作页和预览页都需要同时适配 PC 和 Mobile 两个视图。

---

## 13. 不在本次范围内

- 真实的 SEO 关键词搜索热度、竞争度分析。
- 后端 AI 生成描述和标签。
- 用户自定义保存多套描述/标签模板。
- 一键发布到各平台的能力。

---

## 14. 验证方式

- 启动本地预览服务器，手动检查创作页平台选择、联动效果。
- 编写 Playwright 脚本：
  1. 打开创作页，确认平台选择按钮存在。
  2. 点击按钮打开弹窗，选择「小红书」。
  3. 确认创作页当前平台 chip 更新为「小红书」。
  4. 进入预览页，确认描述和标签符合小红书风格（带 `#` 标签）。
  5. 复制描述和标签，验证格式正确。
