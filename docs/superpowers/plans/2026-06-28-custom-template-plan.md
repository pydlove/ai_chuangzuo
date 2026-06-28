# 自定义导出模板实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在纯前端原型中实现低门槛自定义导出模板：用户基于现有预设模板，通过可视化选项创建、保存、应用自己的模板。

**Architecture:** 自定义模板数据存 `localStorage`，页面加载时与 `templatePresets` 合并为运行时列表；渲染时先应用基础预设模板，再通过 `applyTemplateOverrides` 追加用户选择的样式覆盖；创建/编辑面板内嵌在模板库弹窗中，左侧实时预览、右侧选项配置。

**Tech Stack:** 纯 HTML/CSS/JS 原型，无构建工具，无后端，localStorage 持久化，Playwright 做功能验证。

## Global Constraints

- 无后端、无构建工具、无包管理器。
- 自定义模板仅保存在浏览器 `localStorage`。
- 不允许直接编辑 HTML/CSS，必须通过可视化选项。
- 自定义模板名称最多 20 字，不能为空或纯空格。
- 本期不做导入导出、云端同步、字体选择、复杂动画。

---

## File Structure

| File | Responsibility |
|------|----------------|
| `.superpowers/brainstorm/6491-1782131242/content/shared.js` | 自定义模板存储、合并、渲染覆盖、模板库弹窗扩展、创建/编辑面板 |
| `.superpowers/brainstorm/6491-1782131242/content/shared.css` | 自定义模板相关动画、覆盖样式、创建面板布局辅助类 |
| `tests/e2e/verify_custom_template.py` | Playwright 验证脚本：创建自定义模板并应用到预览页 |

---

### Task 1: 自定义模板 localStorage 存储层

**Files:**
- Modify: `.superpowers/brainstorm/6491-1782131242/content/shared.js`（在 `templatePresets` 定义之后新增存储函数）

**Interfaces:**
- Produces: `loadCustomTemplates()`, `saveCustomTemplates(templates)`, `createCustomTemplate(data)`, `updateCustomTemplate(id, data)`, `deleteCustomTemplate(id)`

- [ ] **Step 1: 新增存储函数**

在 `shared.js` 中 `templatePresets` 数组定义之后、`publishPlatforms` 之前插入：

```js
  var CUSTOM_TEMPLATES_KEY = 'aichuangzuo_custom_templates';

  function loadCustomTemplates() {
    try {
      var raw = localStorage.getItem(CUSTOM_TEMPLATES_KEY);
      if (!raw) return [];
      var parsed = JSON.parse(raw);
      if (!Array.isArray(parsed)) return [];
      return parsed.filter(function(t) {
        return t && t.id && t.name && t.baseKey && t.overrides;
      });
    } catch (e) {
      return [];
    }
  }

  function saveCustomTemplates(templates) {
    try {
      localStorage.setItem(CUSTOM_TEMPLATES_KEY, JSON.stringify(templates));
      return true;
    } catch (e) {
      showToast('保存失败，请检查浏览器存储权限');
      return false;
    }
  }

  function createCustomTemplate(data) {
    var templates = loadCustomTemplates();
    var tpl = {
      id: 'custom_' + Date.now(),
      name: (data.name || '自定义模板').trim().slice(0, 20),
      baseKey: data.baseKey,
      overrides: data.overrides,
      createdAt: Date.now()
    };
    templates.unshift(tpl);
    if (saveCustomTemplates(templates)) {
      showToast('模板已保存');
      return tpl;
    }
    return null;
  }

  function updateCustomTemplate(id, data) {
    var templates = loadCustomTemplates();
    var idx = templates.findIndex(function(t) { return t.id === id; });
    if (idx === -1) return null;
    templates[idx].name = (data.name || templates[idx].name).trim().slice(0, 20);
    templates[idx].baseKey = data.baseKey || templates[idx].baseKey;
    templates[idx].overrides = data.overrides || templates[idx].overrides;
    if (saveCustomTemplates(templates)) {
      showToast('模板已更新');
      return templates[idx];
    }
    return null;
  }

  function deleteCustomTemplate(id) {
    var templates = loadCustomTemplates();
    var filtered = templates.filter(function(t) { return t.id !== id; });
    if (filtered.length === templates.length) return false;
    if (saveCustomTemplates(filtered)) {
      showToast('模板已删除');
      return true;
    }
    return false;
  }

  function getCustomTemplateById(id) {
    return loadCustomTemplates().find(function(t) { return t.id === id; }) || null;
  }
```

- [ ] **Step 2: 验证存储函数可用**

在浏览器控制台执行：

```js
var t = createCustomTemplate({ name: '测试', baseKey: 'wechat', overrides: { theme: 'blue', titleStyle: 'center', highlightStyle: 'border', useCards: false } });
console.log(t);
console.log(loadCustomTemplates());
deleteCustomTemplate(t.id);
```

Expected: 创建返回对象，加载返回包含该对象的数组，删除后数组为空。

- [ ] **Step 3: 提交**

```bash
git add .superpowers/brainstorm/6491-1782131242/content/shared.js
git commit -m "feat(template): add custom template localStorage helpers"
```

---

### Task 2: 运行时模板列表合并与模板库「我的模板」分类

**Files:**
- Modify: `.superpowers/brainstorm/6491-1782131242/content/shared.js`

**Interfaces:**
- Consumes: `loadCustomTemplates()`
- Produces: `getRuntimeTemplates()`, custom templates rendered in `openTemplateLibrary()` under "我的模板" tab

- [ ] **Step 1: 新增运行时模板合并函数**

在 `loadCustomTemplates` 之后新增：

```js
  function getRuntimeTemplates() {
    return templatePresets.concat(loadCustomTemplates().map(function(t) {
      var base = templatePresets.find(function(p) { return p.key === t.baseKey; }) || templatePresets[0];
      return {
        key: t.id,
        name: t.name,
        platform: 'custom',
        desc: '基于 ' + base.name,
        iconColor: base.iconColor,
        iconChar: '我',
        previewBg: base.previewBg,
        previewBorder: base.previewBorder,
        previewHtml: base.previewHtml,
        isCustom: true,
        baseKey: t.baseKey,
        overrides: t.overrides
      };
    }));
  }
```

- [ ] **Step 2: 修改模板库弹窗的 tab 和数据源**

在 `openTemplateLibrary` 中：

1. 将 `platformTabs` 数组末尾增加 `{ key: 'custom', label: '我的模板' }`。
2. 将 `var selectedTemplate = templatePresets[0];` 改为 `var runtimeTemplates = getRuntimeTemplates(); var selectedTemplate = runtimeTemplates[0];`。
3. 将 `renderList` 函数中的 `templatePresets` 替换为 `runtimeTemplates`。
4. 当 `selectedPlatform === 'custom'` 时，过滤 `runtimeTemplates` 中 `isCustom === true` 的项。
5. 自定义模板卡片左上角显示「我」标签：在 `row.innerHTML` 顶部增加一个绝对定位小标签。

示例修改后的 `renderList` 核心逻辑：

```js
    function renderList() {
      listPane.innerHTML = '';
      var filtered = selectedPlatform === 'all'
        ? runtimeTemplates
        : selectedPlatform === 'custom'
          ? runtimeTemplates.filter(function(t) { return t.isCustom; })
          : runtimeTemplates.filter(function(t) { return t.platform === selectedPlatform; });

      if (selectedPlatform === 'custom' && filtered.length === 0) {
        var empty = document.createElement('div');
        empty.style.cssText = 'text-align: center; padding: 40px 20px; color: #8c8c8c; font-size: 14px;';
        empty.innerHTML = '<div style="margin-bottom: 12px;">还没有自定义模板</div><button id="create-custom-from-empty" style="padding: 8px 16px; background: #07c160; color: #fff; border: none; border-radius: 6px; cursor: pointer;">创建自定义模板</button>';
        listPane.appendChild(empty);
        empty.querySelector('#create-custom-from-empty').onclick = function() {
          overlay.remove();
          openCustomTemplateEditor();
        };
        return;
      }

      filtered.forEach(function(t, idx) {
        var row = document.createElement('div');
        row.className = 'template-lib-row';
        row.style.cssText = 'border: 1px solid #e8e8e8; border-radius: 8px; padding: 10px 12px; margin-bottom: 8px; cursor: pointer; background: #fff; display: flex; gap: 10px; align-items: center; transition: all 0.15s; box-sizing: border-box; position: relative;';
        var badge = t.isCustom ? '<span style="position: absolute; top: -6px; left: -6px; background: #07c160; color: #fff; font-size: 10px; padding: 1px 5px; border-radius: 8px;">我</span>' : '';
        row.innerHTML =
          badge +
          '<svg width="22" height="22" viewBox="0 0 16 16" style="flex-shrink: 0;"><rect width="16" height="16" rx="4" fill="' + t.iconColor + '"/><text x="8" y="12" text-anchor="middle" fill="#fff" font-size="10" font-weight="bold" font-family="sans-serif">' + t.iconChar + '</text></svg>' +
          '<div style="flex: 1; min-width: 0;">' +
            '<div style="font-weight: 600; color: #1a1a1a; font-size: 13px; margin-bottom: 2px;">' + t.name + '</div>' +
            '<div style="font-size: 11px; color: #8c8c8c; line-height: 1.4;">' + t.desc + '</div>' +
          '</div>';
        // ... existing hover/select handlers
      });
    }
```

- [ ] **Step 3: 验证模板库能显示「我的模板」分类**

1. 在浏览器控制台手动创建一个自定义模板：

```js
createCustomTemplate({ name: '测试模板', baseKey: 'wechat', overrides: { theme: 'blue', titleStyle: 'center', highlightStyle: 'border', useCards: false } });
```

2. 打开创作页，点击「+ 模板库」。
3. 确认 tab 栏出现「我的模板」，点击后显示「测试模板」，卡片左上角有「我」标签。

- [ ] **Step 4: 提交**

```bash
git add .superpowers/brainstorm/6491-1782131242/content/shared.js
git commit -m "feat(template): merge custom templates into template library"
```

---

### Task 3: 预设模板渲染引入 CSS 变量

**Files:**
- Modify: `.superpowers/brainstorm/6491-1782131242/content/shared.js`（`applyTemplateToPreview` 函数）
- Modify: `.superpowers/brainstorm/6491-1782131242/content/shared.css`

**Interfaces:**
- Produces: CSS custom properties on `.article-preview`, base classes for title/highlight/paragraph

- [ ] **Step 1: 修改 `applyTemplateToPreview` 注入 CSS 变量**

在 `applyTemplateToPreview` 中，找到设置背景色和边框色的位置，改为通过 CSS 变量：

```js
  function applyTemplateToPreview(mockupEl, templateKey) {
    var preview = mockupEl.querySelector('.article-preview');
    if (!preview || !templateKey) return;
    var isMobile = mockupEl.querySelector('.mockup-header').textContent.includes('移动端');

    var tpl = getRuntimeTemplates().find(function(t) { return t.key === templateKey; }) || templatePresets[0];
    var base = tpl.isCustom
      ? (templatePresets.find(function(p) { return p.key === tpl.baseKey; }) || templatePresets[0])
      : tpl;

    // 设置 CSS 变量，供自定义覆盖使用
    preview.style.setProperty('--template-primary', base.iconColor);
    preview.style.setProperty('--template-bg', base.previewBg);
    preview.style.setProperty('--template-border', base.previewBorder);

    // 清除之前的覆盖 class
    preview.classList.remove('tpl-title-center', 'tpl-title-left', 'tpl-title-underline');
    preview.classList.remove('tpl-highlight-border', 'tpl-highlight-background', 'tpl-highlight-quote');
    preview.classList.remove('tpl-use-cards');

    // 原有模板样式继续生效...
    // （保留现有 applyTemplateToPreview 的主体逻辑，仅把硬编码颜色引用改为 var(--template-primary) 等）
  }
```

实际改动时，需要把 `applyTemplateToPreview` 内部所有直接写死的颜色（如 `#07c160`）替换为 `var(--template-primary)` 等变量引用。若模板有自身特殊色（如小红书粉 `#ff2442`），则该模板预设的 CSS 变量值就是 `#ff2442`，自定义模板通过覆盖变量来换色。

- [ ] **Step 2: 在 shared.css 添加覆盖样式**

```css
/* 自定义模板标题样式 */
.tpl-title-center .preview-title {
  text-align: center;
}
.tpl-title-left .preview-title {
  text-align: left;
}
.tpl-title-underline .preview-title {
  text-align: left;
  border-bottom: 2px solid var(--template-primary);
  padding-bottom: 8px;
}

/* 自定义模板重点高亮 */
.tpl-highlight-border .preview-highlight {
  border-left: 4px solid var(--template-primary);
  background: var(--template-bg);
}
.tpl-highlight-background .preview-highlight {
  background: var(--template-bg);
  border: 1px solid var(--template-border);
}
.tpl-highlight-quote .preview-highlight {
  background: transparent;
  border-left: 4px solid var(--template-primary);
  font-style: italic;
}

/* 自定义模板卡片段落 */
.tpl-use-cards .article-preview > p {
  background: #fff;
  border: 1px solid var(--template-border);
  border-radius: 8px;
  padding: 12px 16px;
  margin-bottom: 12px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}
```

- [ ] **Step 3: 验证现有预设模板颜色未丢失**

打开预览页 `preview.html`，切换不同预设模板，确认：
- 公众号模板仍为绿色。
- 小红书模板仍为粉红。
- 今日头条模板仍为橙色。

- [ ] **Step 4: 提交**

```bash
git add .superpowers/brainstorm/6491-1782131242/content/shared.js .superpowers/brainstorm/6491-1782131242/content/shared.css
git commit -m "feat(template): add css variables and override classes for custom templates"
```

---

### Task 4: 实现 `applyTemplateOverrides` 渲染覆盖

**Files:**
- Modify: `.superpowers/brainstorm/6491-1782131242/content/shared.js`

**Interfaces:**
- Consumes: `overrides` object with `theme`, `titleStyle`, `highlightStyle`, `useCards`
- Produces: `applyTemplateOverrides(mockupEl, overrides)`

- [ ] **Step 1: 新增主题色映射**

在 `applyTemplateToPreview` 附近新增：

```js
  var themeColorMap = {
    brand:  { primary: '#07c160', bg: '#f6ffed', border: '#d9f7be' },
    blue:   { primary: '#1677ff', bg: '#f0f5ff', border: '#d6e4ff' },
    red:    { primary: '#cf1322', bg: '#fff2f0', border: '#ffccc7' },
    gray:   { primary: '#595959', bg: '#fafafa', border: '#e8e8e8' },
    pink:   { primary: '#ff2442', bg: '#fff0f3', border: '#ffd1d9' },
    orange: { primary: '#ff6600', bg: '#fff7e6', border: '#ffd591' }
  };
```

- [ ] **Step 2: 实现 `applyTemplateOverrides`**

```js
  function applyTemplateOverrides(mockupEl, overrides) {
    if (!overrides) return;
    var preview = mockupEl.querySelector('.article-preview');
    if (!preview) return;

    var theme = themeColorMap[overrides.theme];
    if (theme) {
      preview.style.setProperty('--template-primary', theme.primary);
      preview.style.setProperty('--template-bg', theme.bg);
      preview.style.setProperty('--template-border', theme.border);
    }

    preview.classList.remove('tpl-title-center', 'tpl-title-left', 'tpl-title-underline');
    if (overrides.titleStyle) {
      preview.classList.add('tpl-title-' + overrides.titleStyle);
    }

    preview.classList.remove('tpl-highlight-border', 'tpl-highlight-background', 'tpl-highlight-quote');
    if (overrides.highlightStyle) {
      preview.classList.add('tpl-highlight-' + overrides.highlightStyle);
    }

    preview.classList.toggle('tpl-use-cards', !!overrides.useCards);
  }
```

- [ ] **Step 3: 在 `applyTemplateToPreview` 末尾调用覆盖函数**

```js
    // ... existing applyTemplateToPreview logic

    if (tpl.isCustom && tpl.overrides) {
      applyTemplateOverrides(mockupEl, tpl.overrides);
    }
  }
```

- [ ] **Step 4: 验证覆盖生效**

1. 在控制台创建自定义模板：

```js
createCustomTemplate({ name: '蓝底居中', baseKey: 'wechat', overrides: { theme: 'blue', titleStyle: 'center', highlightStyle: 'background', useCards: true } });
```

2. 打开模板库，切换到「我的模板」，应用该模板。
3. 进入预览页，确认文章标题居中、高亮块为蓝色背景、正文段落变为卡片。

- [ ] **Step 5: 提交**

```bash
git add .superpowers/brainstorm/6491-1782131242/content/shared.js
git commit -m "feat(template): apply custom template overrides to preview"
```

---

### Task 5: 创建/编辑自定义模板面板

**Files:**
- Modify: `.superpowers/brainstorm/6491-1782131242/content/shared.js`
- Modify: `.superpowers/brainstorm/6491-1782131242/content/shared.css`

**Interfaces:**
- Produces: `openCustomTemplateEditor(customId?)` —— 不传 `customId` 为创建，传则为编辑

- [ ] **Step 1: 新增 `openCustomTemplateEditor` 函数**

在 `openTemplateLibrary` 之后新增函数。函数内部：

1. 创建遮罩和弹窗（与模板库弹窗同宽）。
2. 左侧 55% 为大预览区，右侧 45% 为配置区。
3. 默认状态：
   - 创建模式：基于第一个预设模板，主题 `brand`，标题 `left`，高亮 `border`，卡片 `false`，名称为空。
   - 编辑模式：读取 `getCustomTemplateById(customId)` 回填。
4. 配置区控件：
   - 基于模板：下拉选择 `templatePresets` 的 `name`。
   - 配色主题：6 个圆形色块按钮。
   - 标题样式：3 个缩略图卡片。
   - 重点高亮：3 个缩略图卡片。
   - 段落卡片：开关。
   - 模板名称：输入框。
5. 实时预览：任何选项变化时，调用 `updatePreview()`。
6. 保存按钮：校验名称非空，调用 `createCustomTemplate` 或 `updateCustomTemplate`。

核心预览更新逻辑：

```js
    function updatePreview() {
      var base = templatePresets.find(function(p) { return p.key === state.baseKey; }) || templatePresets[0];
      previewPane.innerHTML = buildLargePreview(base);
      var mockup = previewPane.querySelector('.mockup-preview-inner');
      if (mockup) {
        applyTemplateOverrides({ querySelector: function(s) { return mockup.querySelector(s); }, querySelectorAll: function(s) { return mockup.querySelectorAll(s); } }, state.overrides);
      }
    }
```

由于 `applyTemplateOverrides` 接收 `mockupEl` 并调用 `querySelector`，可以构造一个轻量适配对象，或者改造 `applyTemplateOverrides` 同时支持直接传入 `preview` DOM。更简单的做法是把预览区做成一个 `.mockup` 结构，直接复用 `applyTemplateToPreview(mockupEl, base.key)` 再调用 `applyTemplateOverrides`。

- [ ] **Step 2: 在「我的模板」列表顶部添加创建按钮**

在 `openTemplateLibrary` 的「我的模板」tab 渲染时，列表顶部常驻一个按钮：

```js
    var createBtn = document.createElement('button');
    createBtn.textContent = '+ 创建自定义模板';
    createBtn.style.cssText = 'width: 100%; padding: 10px; margin-bottom: 12px; background: #f6ffed; color: #07c160; border: 1px dashed #b7eb8f; border-radius: 8px; cursor: pointer; font-size: 14px; font-weight: 500;';
    createBtn.onclick = function() {
      overlay.remove();
      openCustomTemplateEditor();
    };
```

- [ ] **Step 3: 为自定义模板卡片添加编辑/删除按钮**

在 `renderList` 渲染自定义模板时，hover 显示两个图标按钮：

```js
        if (t.isCustom) {
          var actions = document.createElement('div');
          actions.style.cssText = 'display: none; gap: 6px; position: absolute; top: 8px; right: 8px;';
          actions.innerHTML = '<button class="tpl-edit-btn" style="...">编辑</button><button class="tpl-delete-btn" style="...">删除</button>';
          row.onmouseenter = function() { actions.style.display = 'flex'; };
          row.onmouseleave = function() { actions.style.display = 'none'; };
          row.appendChild(actions);
          actions.querySelector('.tpl-edit-btn').onclick = function(e) {
            e.stopPropagation();
            overlay.remove();
            openCustomTemplateEditor(t.key);
          };
          actions.querySelector('.tpl-delete-btn').onclick = function(e) {
            e.stopPropagation();
            if (confirm('确定删除「' + t.name + '」？删除后不可恢复')) {
              deleteCustomTemplate(t.key);
              renderList();
            }
          };
        }
```

- [ ] **Step 4: 验证完整流程**

1. 打开模板库 → 我的模板 → 创建自定义模板。
2. 选择基于「小红书图文模板」，主题「商务蓝」，标题「居中」，高亮「背景块」，开启卡片。
3. 输入名称「蓝调小红书」。
4. 保存后返回我的模板列表，出现「蓝调小红书」。
5. 点击应用，进入预览页确认效果。
6. 返回模板库，编辑该模板，改名为「蓝调小红书 v2」，保存生效。
7. 删除该模板，列表清空。

- [ ] **Step 5: 提交**

```bash
git add .superpowers/brainstorm/6491-1782131242/content/shared.js .superpowers/brainstorm/6491-1782131242/content/shared.css
git commit -m "feat(template): add custom template editor and management UI"
```

---

### Task 6: Playwright 功能验证脚本

**Files:**
- Create: `tests/e2e/verify_custom_template.py`

**Interfaces:**
- Consumes: 页面 DOM（模板库弹窗、创建面板、预览页）

- [ ] **Step 1: 编写验证脚本**

```python
from playwright.sync_api import sync_playwright
import time

BASE = 'http://localhost:28585/.superpowers/brainstorm/6491-1782131242/content'

def test_custom_template():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page(viewport={'width': 1440, 'height': 900})

        # 1. 打开创作页
        page.goto(BASE + '/create.html', wait_until='networkidle')
        time.sleep(0.5)

        # 2. 打开模板库
        page.click('#screen-create button:has-text("+ 模板库")')
        time.sleep(0.3)

        # 3. 切到「我的模板」
        page.click('#template-lib-modal button:has-text("我的模板")')
        time.sleep(0.3)

        # 4. 点击创建自定义模板
        page.click('#template-lib-modal button:has-text("创建自定义模板")')
        time.sleep(0.3)

        # 5. 输入名称
        page.fill('#custom-template-name input', '蓝调小红书')

        # 6. 选择商务蓝主题（第二个色块）
        page.click('.theme-option:nth-child(2)')

        # 7. 保存
        page.click('#custom-template-save')
        time.sleep(0.3)

        # 8. 确认我的模板列表出现
        assert page.locator('.template-lib-row:has-text("蓝调小红书")').count() > 0

        # 9. 应用该模板
        page.click('.template-lib-row:has-text("蓝调小红书")')
        time.sleep(0.3)

        # 10. 进入预览页
        page.goto(BASE + '/preview.html', wait_until='networkidle')
        time.sleep(0.5)

        # 11. 截图留档
        page.screenshot(path='/tmp/custom_template_preview.png', full_page=True)

        browser.close()
        print('custom template verification passed')

if __name__ == '__main__':
    test_custom_template()
```

脚本中的选择器需与实际 DOM 保持一致，若实现时选择器不同则同步调整。

- [ ] **Step 2: 运行脚本**

确保本地服务已启动：

```bash
./scripts/local/start.sh
```

运行验证：

```bash
python3 tests/e2e/verify_custom_template.py
```

Expected: 终端打印 `custom template verification passed`，且 `/tmp/custom_template_preview.png` 显示按自定义模板渲染的文章。

- [ ] **Step 3: 提交**

```bash
git add tests/e2e/verify_custom_template.py
git commit -m "test(template): add custom template e2e verification"
```

---

### Task 7: 缓存刷新与最终验证

**Files:**
- Modify: `.superpowers/brainstorm/6491-1782131242/content/*.html`（所有引用 `shared.js` 的页面，版本号加 1）

- [ ] **Step 1: 批量更新 shared.js 版本号**

由于 `shared.js` 大幅改动，将所有 HTML 中的 `shared.js?v=6` 升级到 `shared.js?v=7`，避免浏览器使用旧缓存。

可用脚本：

```bash
python3 -c "
import os, glob
base = '.superpowers/brainstorm/6491-1782131242/content'
for path in glob.glob(os.path.join(base, '*.html')):
    with open(path, 'r', encoding='utf-8') as f:
        text = f.read()
    new_text = text.replace('shared.js?v=6', 'shared.js?v=7')
    if new_text != text:
        with open(path, 'w', encoding='utf-8') as f:
            f.write(new_text)
"
```

- [ ] **Step 2: 最终手动验证清单**

- [ ] 创作页点击「+ 模板库」弹出模板库弹窗。
- [ ] tab 栏出现「我的模板」。
- [ ] 空状态时显示占位提示和创建按钮。
- [ ] 创建自定义模板后，我的模板列表显示新模板。
- [ ] 应用自定义模板后，预览页渲染正确。
- [ ] 编辑自定义模板后，名称和效果同步更新。
- [ ] 删除自定义模板后，列表清空且 localStorage 同步。
- [ ] 刷新页面后，我的模板列表保留。

- [ ] **Step 3: 提交**

```bash
git add .superpowers/brainstorm/6491-1782131242/content/*.html
git commit -m "chore(template): bump shared.js cache version for custom templates"
```

---

## Self-Review

**1. Spec coverage:**

- 我的模板分类 ✓ Task 2
- 创建自定义模板入口 ✓ Task 2 & 5
- 基于预设 + 4 组可视化选项 ✓ Task 5
- 保存/应用/编辑/删除 ✓ Task 1 & 5
- localStorage 持久化 ✓ Task 1
- 不上线分享/导入导出/云端同步 ✓ 未涉及

**2. Placeholder scan:**

无 TBD/TODO，所有步骤包含具体代码或命令。

**3. Type consistency：**

- `overrides` 字段统一为 `{ theme, titleStyle, highlightStyle, useCards }`。
- 自定义模板 `key` 统一使用 `id`，运行时模板通过 `isCustom` 区分。
- CSS 变量名 `--template-primary` / `--template-bg` / `--template-border` 全文一致。

无未覆盖需求，计划可执行。
