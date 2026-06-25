# 导出模板库扩充至 30 个模板 — 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 `full-prototype-v20.html` 的导出模板库从 12 个扩充到 30 个，新增平台标签过滤和模板总数显示。

**Architecture:** 在现有弹窗组件内新增平台标签栏；为每个模板预设补充 `platform` 字段用于分组；渲染时按当前标签过滤右侧列表，并自动将左侧大预览切换到该标签首个模板。新增 18 套模板的大预览样式与预览页模板卡片样式。

**Tech Stack:** 纯前端 HTML/CSS/JS（无框架），使用 Playwright 截图验证。

## Global Constraints

- 所有改动集中在 `.superpowers/brainstorm/6491-1782131242/content/full-prototype-v20.html`。
- 不引入新依赖。
- 保持现有交互：弹窗内点击模板行实时预览，点击「应用此模板」后同步更新 `screen-preview` 主预览区。
- 新增模板必须同时支持「模板库弹窗大预览」和「预览页直接点击模板卡片」两种入口。
- 移动端/PC 端预览区都要能正确渲染新增模板。

---

## 文件结构

| 文件 | 职责 |
|------|------|
| `.superpowers/brainstorm/6491-1782131242/content/full-prototype-v20.html` | 唯一修改文件。包含模板预设数据、弹窗渲染、预览页模板卡片、样式应用逻辑。 |

---

### Task 1: 为现有 12 个模板补充 `platform` 字段

**Files:**
- Modify: `.superpowers/brainstorm/6491-1782131242/content/full-prototype-v20.html`（`templatePresets` 数组，约第 3685 行区域）

**Interfaces:**
- Consumes: 无
- Produces: 每个模板对象新增 `platform: '<platformKey>'` 字段

- [ ] **Step 1: 定位 `templatePresets` 数组**

  搜索 `var templatePresets = [`，确认现有 12 条记录。

- [ ] **Step 2: 为每条记录添加 `platform` 字段**

  按以下映射补充：
  - `wechat` → `platform: 'wechat'`
  - `business`, `marketing`, `academic`, `story`, `magazine`, `card`, `checklist`, `dark` → `platform: 'general'`
  - `toutiao` → `platform: 'toutiao'`
  - `xiaohongshu` → `platform: 'xiaohongshu'`
  - `baijiahao` → `platform: 'baijiahao'`

  示例修改（以 `business` 为例）：

  ```javascript
  { key: 'business', name: '简约商务模板', platform: 'general', desc: '14px 正文 / 深蓝标题 / 清晰层级', ... }
  ```

- [ ] **Step 3: 验证字段补齐**

  运行：
  ```bash
  grep -c "platform:" /Users/panyong/aio_project/ai_chuangzuo/.superpowers/brainstorm/6491-1782131242/content/full-prototype-v20.html
  ```
  Expected: 至少 12 处。

- [ ] **Step 4: Commit**

  ```bash
  git add .superpowers/brainstorm/6491-1782131242/content/full-prototype-v20.html
  git commit -m "chore(template): add platform field to existing 12 presets"
  ```

---

### Task 2: 新增 18 个模板预设

**Files:**
- Modify: `.superpowers/brainstorm/6491-1782131242/content/full-prototype-v20.html`（`templatePresets` 数组）

**Interfaces:**
- Consumes: 无
- Produces: `templatePresets` 数组扩展为 30 条记录，新增模板均有 `platform` 字段

- [ ] **Step 1: 在 `templatePresets` 末尾追加 18 条记录**

  新增模板清单：

  **公众号 4 个：**
  - `wechat-minimal` / 公众号极简模板 / `platform: 'wechat'`
  - `wechat-dialogue` / 公众号对话体 / `platform: 'wechat'`
  - `wechat-brand` / 公众号品牌故事 / `platform: 'wechat'`
  - `wechat-infographic` / 公众号信息图 / `platform: 'wechat'`

  **小红书 4 个：**
  - `xiaohongshu-list` / 小红书清单体 / `platform: 'xiaohongshu'`
  - `xiaohongshu-review` / 小红书测评体 / `platform: 'xiaohongshu'`
  - `xiaohongshu-tutorial` / 小红书教程步骤 / `platform: 'xiaohongshu'`
  - `xiaohongshu-emotion` / 小红书情绪共鸣 / `platform: 'xiaohongshu'`

  **今日头条 4 个：**
  - `toutiao-news` / 头条资讯快讯 / `platform: 'toutiao'`
  - `toutiao-depth` / 头条深度报道 / `platform: 'toutiao'`
  - `toutiao-hot` / 头条热点评论 / `platform: 'toutiao'`
  - `toutiao-qa` / 头条问答体 / `platform: 'toutiao'`

  **百家号 3 个：**
  - `baijiahao-science` / 百家号知识科普 / `platform: 'baijiahao'`
  - `baijiahao-history` / 百家号历史人文 / `platform: 'baijiahao'`
  - `baijiahao-guide` / 百家号生活攻略 / `platform: 'baijiahao'`

  **抖音图文 2 个：**
  - `douyin-graphic` / 抖音图文模板 / `platform: 'douyin'`
  - `douyin-quote` / 抖音金句海报 / `platform: 'douyin'`

  **知乎 1 个：**
  - `zhihu-answer` / 知乎回答体 / `platform: 'zhihu'`

  每条记录格式与现有 preset 一致，包含 `key, name, platform, desc, iconColor, iconChar, previewBg, previewBorder, previewHtml`。

  图标颜色参考：
  - 公众号系列：`#07c160`
  - 小红书系列：`#ff2442`
  - 头条系列：`#ff6600`（或 `#ed1c24`）
  - 百家号系列：`#1677ff`
  - 抖音系列：`#000000` 或 `#1a1a1a`
  - 知乎系列：`#0066ff`

- [ ] **Step 2: 验证总数**

  运行：
  ```bash
  grep -c "key: '" /Users/panyong/aio_project/ai_chuangzuo/.superpowers/brainstorm/6491-1782131242/content/full-prototype-v20.html
  ```
  注意此命令会匹配 `templatePresets` 和 `templateLargeStyles`，应通过脚本精确计数：

  ```bash
  python3 - <<'PY'
  import re
  with open('.superpowers/brainstorm/6491-1782131242/content/full-prototype-v20.html') as f:
      txt = f.read()
  presets = re.search(r'var templatePresets = \[(.*?)\];', txt, re.S).group(1)
  print(len(re.findall(r"key:\s*'", presets)))
  PY
  ```
  Expected: 30

- [ ] **Step 3: Commit**

  ```bash
  git add .superpowers/brainstorm/6491-1782131242/content/full-prototype-v20.html
  git commit -m "feat(template): add 18 new template presets (30 total)"
  ```

---

### Task 3: 在弹窗内添加平台标签栏和模板总数

**Files:**
- Modify: `.superpowers/brainstorm/6491-1782131242/content/full-prototype-v20.html`（`openTemplateLibrary` 函数）

**Interfaces:**
- Consumes: `templatePresets`（含 `platform` 字段）
- Produces: 弹窗 header 中渲染「共 30 个模板」；body 顶部渲染平台标签栏；`selectedPlatform` 状态控制过滤

- [ ] **Step 1: 在 `openTemplateLibrary` 中定义平台标签配置**

  在函数开头（`var selectedTemplate = templatePresets[0];` 附近）添加：

  ```javascript
  var platformTabs = [
    { key: 'all', label: '全部' },
    { key: 'wechat', label: '公众号' },
    { key: 'xiaohongshu', label: '小红书' },
    { key: 'toutiao', label: '今日头条' },
    { key: 'baijiahao', label: '百家号' },
    { key: 'zhihu', label: '知乎' },
    { key: 'douyin', label: '抖音图文' },
    { key: 'general', label: '通用风格' }
  ];
  var selectedPlatform = 'all';
  ```

- [ ] **Step 2: 在 header 副标题位置显示总数**

  修改 sub.textContent 为动态文本：

  ```javascript
  sub.textContent = '共 ' + templatePresets.length + ' 个模板 · 左侧实时预览 · 右侧选择模板';
  ```

- [ ] **Step 3: 在 header 与 body 之间插入标签栏容器**

  在 `box.appendChild(headerWrap);` 之后、`// 2-column body` 之前插入：

  ```javascript
  var tabBar = document.createElement('div');
  tabBar.style.cssText = 'display: flex; gap: 8px; padding: 0 24px 14px; flex-shrink: 0; overflow-x: auto; border-bottom: 1px solid #f0f0f0;';
  box.appendChild(tabBar);
  ```

- [ ] **Step 4: 渲染标签并绑定点击事件**

  在 `updatePreview` / `selectInList` 附近添加渲染函数：

  ```javascript
  function renderTabs() {
    tabBar.innerHTML = '';
    platformTabs.forEach(function(tab) {
      var btn = document.createElement('button');
      btn.textContent = tab.label;
      var active = tab.key === selectedPlatform;
      btn.style.cssText = 'padding: 6px 14px; border-radius: 16px; border: 1px solid ' + (active ? '#07c160' : '#d9d9d9') + '; background: ' + (active ? '#f6ffed' : '#fff') + '; color: ' + (active ? '#07c160' : '#595959') + '; font-size: 13px; cursor: pointer; white-space: nowrap; font-weight: ' + (active ? '600' : '500') + ';';
      btn.onclick = function() {
        selectedPlatform = tab.key;
        renderTabs();
        renderList();
      };
      tabBar.appendChild(btn);
    });
  }
  ```

- [ ] **Step 5: 重构列表渲染为 `renderList` 函数**

  将现有的 `templatePresets.forEach` 循环封装为 `renderList`，根据 `selectedPlatform` 过滤：

  ```javascript
  function renderList() {
    listPane.innerHTML = '';
    var filtered = selectedPlatform === 'all'
      ? templatePresets
      : templatePresets.filter(function(t) { return t.platform === selectedPlatform; });

    filtered.forEach(function(t, idx) {
      // 原有 row 创建逻辑
      var row = document.createElement('div');
      row.className = 'template-lib-row';
      row.style.cssText = 'border: 1px solid #e8e8e8; border-radius: 8px; padding: 10px 12px; margin-bottom: 8px; cursor: pointer; background: #fff; display: flex; gap: 10px; align-items: center; transition: all 0.15s; box-sizing: border-box;';
      row.innerHTML =
        '<svg width="22" height="22" viewBox="0 0 16 16" style="flex-shrink: 0;"><rect width="16" height="16" rx="4" fill="' + t.iconColor + '"/><text x="8" y="12" text-anchor="middle" fill="#fff" font-size="10" font-weight="bold" font-family="sans-serif">' + t.iconChar + '</text></svg>' +
        '<div style="flex: 1; min-width: 0;">' +
          '<div style="font-weight: 600; color: #1a1a1a; font-size: 13px; margin-bottom: 2px;">' + t.name + '</div>' +
          '<div style="font-size: 11px; color: #8c8c8c; line-height: 1.4;">' + t.desc + '</div>' +
        '</div>';
      row.onmouseover = function() { if (!row.classList.contains('selected')) { row.style.borderColor = '#07c160'; row.style.boxShadow = '0 2px 8px rgba(7,193,96,0.1)'; } };
      row.onmouseout = function() { if (!row.classList.contains('selected')) { row.style.borderColor = '#e8e8e8'; row.style.boxShadow = 'none'; } };
      row.onclick = function() { selectInList(t, row); };
      listPane.appendChild(row);

      if (idx === 0) {
        row.classList.add('selected');
        row.style.background = '#f6ffed';
        row.style.borderColor = '#07c160';
        row.style.boxShadow = '0 0 0 2px rgba(7,193,96,0.25)';
        selectedTemplate = t;
        updatePreview(t);
      }
    });
  }
  ```

  替换原有循环为 `renderTabs(); renderList();`，并删除旧的 auto-select 逻辑。

- [ ] **Step 6: 验证弹窗 UI**

  启动本地服务器并打开页面：
  ```bash
  python3 -m http.server 8080 &
  ```
  打开浏览器访问 `http://localhost:8080/.superpowers/brainstorm/6491-1782131242/content/full-prototype-v20.html`，点击「05 预览/导出」→「+ 模板库」。

  Expected：弹窗顶部显示「共 30 个模板」，标签栏显示 8 个平台标签，默认「全部」选中，右侧列出 30 个模板。

- [ ] **Step 7: Commit**

  ```bash
  git add .superpowers/brainstorm/6491-1782131242/content/full-prototype-v20.html
  git commit -m "feat(template): add platform tabs and total count to template library modal"
  ```

---

### Task 4: 为新增 18 个模板补充大预览样式

**Files:**
- Modify: `.superpowers/brainstorm/6491-1782131242/content/full-prototype-v20.html`（`templateLargeStyles` 对象 / `getTemplateStyles` 函数）

**Interfaces:**
- Consumes: 新增模板 key（`wechat-minimal`, `xiaohongshu-list`, ...）
- Produces: 每个新增模板在 `templateLargeStyles` / `getTemplateStyles` 中有对应样式定义

- [ ] **Step 1: 定位样式定义**

  搜索 `function getTemplateStyles(isMobile)` 或 `var templateLargeStyles`。

- [ ] **Step 2: 为新增 18 个 key 补充样式**

  每个新增 key 需要补充一套样式对象，字段与现有模板一致：`bg, font, titleColor, titleSize, metaColor, metaBorder, bodyColor, bodySize, bodyLine, headingColor, headingSize, headingBorder, headingPl, calloutBg, calloutBorder, calloutColor` 等。

  设计原则：
  - 同平台模板保持主色调一致，但在字体大小、标题对齐、强调形式、引用块样式上做出差异。
  - 至少让新增模板在左侧大预览中一眼可区分。

  示例（公众号极简）：
  ```javascript
  'wechat-minimal': {
    bg: '#fff', font: '-apple-system, sans-serif',
    titleColor: '#1a1a1a', titleSize: '22px',
    metaColor: '#8c8c8c', metaBorder: '#eee',
    bodyColor: '#262626', bodySize: '15px', bodyLine: '1.9',
    headingColor: '#1a1a1a', headingSize: '15px', headingBorder: 'none', headingPl: '0',
    calloutBg: '#fafafa', calloutBorder: '1px solid #e8e8e8', calloutColor: '#595959'
  }
  ```

  需要补充的 18 个 key：
  `wechat-minimal`, `wechat-dialogue`, `wechat-brand`, `wechat-infographic`,
  `xiaohongshu-list`, `xiaohongshu-review`, `xiaohongshu-tutorial`, `xiaohongshu-emotion`,
  `toutiao-news`, `toutiao-depth`, `toutiao-hot`, `toutiao-qa`,
  `baijiahao-science`, `baijiahao-history`, `baijiahao-guide`,
  `douyin-graphic`, `douyin-quote`,
  `zhihu-answer`。

- [ ] **Step 3: 验证无遗漏**

  运行：
  ```bash
  python3 - <<'PY'
  import re
  with open('.superpowers/brainstorm/6491-1782131242/content/full-prototype-v20.html') as f:
      txt = f.read()
  presets = re.search(r'var templatePresets = \[(.*?)\];', txt, re.S).group(1)
  styles = re.search(r'function getTemplateStyles\(isMobile\).*?return \{(.*?)\};', txt, re.S).group(1)
  preset_keys = re.findall(r"key:\s*'(.*?)'", presets)
  style_keys = re.findall(r"^\s+'([\w-]+)':\s*\{", styles, re.M)
  missing = [k for k in preset_keys if k not in style_keys]
  print('Missing styles:', missing)
  PY
  ```
  Expected: `Missing styles: []`

- [ ] **Step 4: Commit**

  ```bash
  git add .superpowers/brainstorm/6491-1782131242/content/full-prototype-v20.html
  git commit -m "feat(template): add large preview styles for 18 new templates"
  ```

---

### Task 5: 为新增模板补充预览页模板卡片与样式

**Files:**
- Modify: `.superpowers/brainstorm/6491-1782131242/content/full-prototype-v20.html`（`screen-preview` 区域；`getTemplateStyles` / `templateHeadingTexts`）

**Interfaces:**
- Consumes: 新增模板 key
- Produces: `screen-preview` 右侧模板列表包含 30 个模板卡片；`getTemplateStyles` / `templateHeadingTexts` 支持所有 key

- [ ] **Step 1: 更新 `getTemplateStyles` 中的 headingTexts**

  在 `templateHeadingTexts` 中为新增 18 个 key 提供对应的小标题文本。可复用平台通用文案，也可根据模板特点微调（如知乎问答体可用「Q: ... / A: ...」形式）。

  示例：
  ```javascript
  'wechat-minimal': ['优先级排序：先做重要的事', '时间块：给任务一个容器'],
  'zhihu-answer': ['Q: 为什么有人能在 24 小时内完成更多事？', 'A: 因为他们在管理注意力']
  ```

- [ ] **Step 2: 扩展 PC 端模板卡片列表**

  在 `screen-preview` PC 端「选择导出模板」区域，现有 12 个 `.template-card` 之后追加新增 18 个卡片。

  每条卡片格式：
  ```html
  <div class="template-card" data-template="wechat-minimal" onclick="selectTemplate(this)" style="border: 1px solid #e8e8e8; border-radius: 8px; padding: 12px; margin-bottom: 12px; cursor: pointer;">
    <div style="display: flex; gap: 12px; align-items: center;">
      <div style="flex: 1;">
        <div style="font-weight: 600; margin-bottom: 4px; color: #1a1a1a; display: flex; align-items: center;">
          <svg width="16" height="16" viewBox="0 0 16 16" style="flex-shrink:0;vertical-align:-3px;margin-right:6px;"><rect width="16" height="16" rx="4" fill="#07c160"/><text x="8" y="12" text-anchor="middle" fill="#fff" font-size="10" font-weight="bold" font-family="sans-serif">简</text></svg>
          公众号极简模板
        </div>
        <div style="font-size: 12px; color: #8c8c8c;">15px 正文 / 无衬线 / 大量留白</div>
      </div>
      <div style="width: 72px; height: 54px; background: #fff; border-radius: 4px; padding: 6px; font-size: 8px; line-height: 1.4; color: #262626; overflow: hidden; border: 1px solid #e8e8e8;">
        <div style="font-weight: 700; margin-bottom: 3px; font-size: 9px;">文章标题</div>
        <div style="margin-bottom: 2px;">正文正文</div>
      </div>
    </div>
  </div>
  ```

- [ ] **Step 3: 扩展移动端 pill 列表**

  移动端由于空间限制，不必放满 30 个 pill。优先保留各平台代表模板，建议展示 12-15 个高频 pill：
  公众号标准、公众号极简、小红书图文、小红书清单、今日头条、头条快讯、百家号、知乎回答、抖音图文、简约商务、营销转化、学术报告、故事叙事、极简清单、深色沉浸。

  为新增的 pill 补充对应的 `.template-card`（`border-radius: 20px` 形式）。

- [ ] **Step 4: 验证预览页点击卡片**

  打开浏览器，进入「05 预览/导出」，点击 PC 端和移动端新增模板卡片，确认左侧文章预览样式变化，无 JS 报错。

- [ ] **Step 5: Commit**

  ```bash
  git add .superpowers/brainstorm/6491-1782131242/content/full-prototype-v20.html
  git commit -m "feat(template): add preview-page cards and styles for 30 templates"
  ```

---

### Task 6: 端到端截图验证

**Files:**
- 无文件修改，仅验证

**Interfaces:**
- Consumes: 完整 HTML 文件
- Produces: 验证截图

- [ ] **Step 1: 编写 Playwright 验证脚本**

  创建 `/tmp/verify_templates.py`：

  ```python
  from playwright.sync_api import sync_playwright

  with sync_playwright() as p:
      browser = p.chromium.launch(headless=True)
      page = browser.new_page(viewport={'width': 1400, 'height': 900})
      page.goto('http://localhost:8080/.superpowers/brainstorm/6491-1782131242/content/full-prototype-v20.html')
      page.click('button[data-screen="preview"]')
      page.evaluate('openTemplateLibrary()')

      # 1. 检查总数显示
      assert '共 30 个模板' in page.content()

      # 2. 检查标签栏存在
      tabs = page.locator('div', has_text='全部').locator('..').locator('button').all()
      assert len(tabs) == 8

      # 3. 每个标签下都有模板
      tab_labels = ['全部', '公众号', '小红书', '今日头条', '百家号', '知乎', '抖音图文', '通用风格']
      for label in tab_labels:
          page.get_by_role('button', name=label).click()
          rows = page.locator('.template-lib-row').all()
          assert len(rows) > 0, f'{label} 标签下没有模板'

      # 4. 全部标签下 30 个
      page.get_by_role('button', name='全部').click()
      assert len(page.locator('.template-lib-row').all()) == 30

      # 5. 截图留存
      page.screenshot(path='/tmp/template_library_final.png')
      browser.close()
      print('Verification passed')
  ```

- [ ] **Step 2: 运行验证脚本**

  ```bash
  python3 /tmp/verify_templates.py
  ```
  Expected: `Verification passed`

- [ ] **Step 3: 人工抽查关键模板**

  打开 `/tmp/template_library_final.png`，确认：
  - 顶部显示「共 30 个模板」；
  - 8 个平台标签可辨；
  - 右侧列表中 30 个模板名称各不相同；
  - 点击不同标签时列表正确过滤。

- [ ] **Step 4: Commit 验证脚本（可选）**

  若项目有测试目录，可将脚本移入；原型项目通常无需保留，截图即可。

---

## 自我审查

**1. Spec coverage:**
- 30 个模板：Task 2 实现。
- 平台标签过滤：Task 3 实现。
- 总数显示：Task 3 Step 2 实现。
- 左侧大预览实时更新：Task 3 Step 5 渲染列表时保留 `updatePreview` 调用。
- 预览页模板卡片同步：Task 5 实现。
- 移动端适配：Task 3 标签栏支持横向滚动；Task 5 移动端 pill 精简展示。

**2. Placeholder scan:**
- 无 TBD/TODO。
- 所有新增模板 key、名称、平台分配已明确。
- 样式定义给出示例和清单，具体色值由实现者按平台主色填充。

**3. Type consistency:**
- 新增模板均使用 `key: '<kebab-case>'`、`platform: '<platformKey>'`，与现有 12 个一致。
- `templateLargeStyles` / `getTemplateStyles` 中 key 与 `templatePresets` 一一对应。
- `templateHeadingTexts` 中 key 同步补充。

---

## 执行交接

**Plan complete and saved to `docs/superpowers/plans/2026-06-25-template-library-expansion-plan.md`. Two execution options:**

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

**Which approach?**
