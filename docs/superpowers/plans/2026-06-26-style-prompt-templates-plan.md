# 风格提示词参考模板实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在「我的风格」提示词编辑器中新增 4 个可点击的参考模板卡片，点击后自动填充对应提示词。

**Architecture:** 在 `shared.js` 的 `renderStyleEditor()` 中，于提示词文本域下方插入模板卡片栏。模板数据直接读取现有的 `systemStylePresets` 数组中对应索引的 `prompt`。`shared.css` 补充模板卡片横向滚动样式。

**Tech Stack:** 纯 HTML / CSS / JavaScript（浏览器原生 API），无外部依赖。

## Global Constraints

- 模板数据必须复用现有 `systemStylePresets`，不新增数据源。
- 模板卡片位于提示词文本域下方，横向滚动。
- 点击卡片即用模板 `prompt` 覆盖当前文本域内容。
- 填充后必须调用现有的 `updateCounter()` 同步字数统计和保存按钮状态。
- 样式主色保持 `#07c160`，hover 状态与现有卡片一致。

---

## 文件清单

| 文件 | 操作 | 说明 |
|---|---|---|
| `.superpowers/brainstorm/6491-1782131242/content/shared.css` | 修改 | 新增模板卡片栏、卡片、标题、描述样式 |
| `.superpowers/brainstorm/6491-1782131242/content/shared.js` | 修改 | 在 `renderStyleEditor()` 内新增模板卡片渲染逻辑 |

---

### Task 1: 添加模板卡片样式

**Files:**
- Modify: `.superpowers/brainstorm/6491-1782131242/content/shared.css`

**Interfaces:**
- Consumes: 无
- Produces: `.style-template-bar`, `.style-template-label`, `.style-template-cards`, `.style-template-card`, `.style-template-title`, `.style-template-desc` 等 CSS 类

- [ ] **Step 1: 编写 CSS 代码**

在 `shared.css` 的「我的风格编辑器」样式区块之后追加以下内容：

```css
  /* ===== 风格提示词参考模板 ===== */
  .style-template-bar {
    margin-top: 4px;
  }
  .style-template-label {
    font-size: 13px;
    font-weight: 500;
    color: #595959;
    margin-bottom: 8px;
  }
  .style-template-cards {
    display: flex;
    gap: 10px;
    overflow-x: auto;
    padding-bottom: 4px;
    scrollbar-width: none; /* Firefox */
  }
  .style-template-cards::-webkit-scrollbar {
    display: none; /* Chrome/Safari */
  }
  .style-template-card {
    flex: 0 0 160px;
    border: 1px solid #e8e8e8;
    border-radius: 8px;
    padding: 10px 12px;
    background: #fff;
    cursor: pointer;
    transition: all 0.15s;
  }
  .style-template-card:hover {
    border-color: #07c160;
    background: #f6ffed;
  }
  .style-template-title {
    font-size: 14px;
    font-weight: 600;
    color: #1a1a1a;
    margin-bottom: 4px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  .style-template-desc {
    font-size: 12px;
    color: #8c8c8c;
    line-height: 1.5;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
```

- [ ] **Step 2: 验证样式文件无语法错误**

打开浏览器访问 `create.html`，确认页面正常加载，控制台无 CSS 解析错误。

- [ ] **Step 3: 提交**

```bash
git add .superpowers/brainstorm/6491-1782131242/content/shared.css
git commit -m "feat(style-templates): add prompt template card styles"
```

---

### Task 2: 在编辑器中渲染模板卡片

**Files:**
- Modify: `.superpowers/brainstorm/6491-1782131242/content/shared.js`

**Interfaces:**
- Consumes: `systemStylePresets` 数组、`promptTextarea` DOM 元素、`updateCounter()` 函数
- Produces: 模板卡片点击事件处理逻辑

- [ ] **Step 1: 定义模板索引映射**

在 `renderStyleEditor()` 函数内、创建表单字段之后，找到「Prompt field」区块结束的位置，在其后追加模板数据定义：

```javascript
      // Template presets for quick fill
      var templatePresets = [
        { index: 1, title: '产品评测', desc: '客观中立、参数对比' },
        { index: 2, title: '情感散文', desc: '细腻温暖、意象留白' },
        { index: 3, title: '职场干货', desc: '专业务实、可执行 checklist' },
        { index: 7, title: '营销文案', desc: '紧迫感 + 利益点突出' }
      ];
```

- [ ] **Step 2: 渲染模板卡片栏**

在 `form.appendChild(promptField);` 之后、`// Footer` 注释之前，插入以下代码：

```javascript
      // Template bar
      var templateBar = document.createElement('div');
      templateBar.className = 'style-template-bar';
      var templateLabel = document.createElement('div');
      templateLabel.className = 'style-template-label';
      templateLabel.textContent = '参考模板';
      templateBar.appendChild(templateLabel);
      var templateCards = document.createElement('div');
      templateCards.className = 'style-template-cards';
      templatePresets.forEach(function(t) {
        var preset = systemStylePresets[t.index];
        if (!preset) return;
        var card = document.createElement('div');
        card.className = 'style-template-card';
        var cardTitle = document.createElement('div');
        cardTitle.className = 'style-template-title';
        cardTitle.textContent = t.title;
        var cardDesc = document.createElement('div');
        cardDesc.className = 'style-template-desc';
        cardDesc.textContent = t.desc;
        card.appendChild(cardTitle);
        card.appendChild(cardDesc);
        card.onclick = function() {
          promptTextarea.value = preset.prompt;
          updateCounter();
          promptTextarea.focus();
        };
        templateCards.appendChild(card);
      });
      templateBar.appendChild(templateCards);
      form.appendChild(templateBar);
```

- [ ] **Step 3: 验证 JavaScript 无语法错误**

```bash
node --check .superpowers/brainstorm/6491-1782131242/content/shared.js
```

预期：无输出（表示通过）。

- [ ] **Step 4: 提交**

```bash
git add .superpowers/brainstorm/6491-1782131242/content/shared.js
git commit -m "feat(style-templates): render prompt template cards in editor"
```

---

### Task 3: 端到端验证

**Files:**
- 无需修改代码

**Interfaces:**
- Consumes: Task 1 和 Task 2 完成后的 `shared.css` 与 `shared.js`
- Produces: 验证报告

- [ ] **Step 1: 启动本地服务器**

```bash
python3 -m http.server 8765 --directory .superpowers/brainstorm/6491-1782131242/content
```

- [ ] **Step 2: 运行 Playwright 验证脚本**

使用以下脚本（保存为 `/tmp/verify_style_templates.py`）：

```python
from playwright.sync_api import sync_playwright

errors = []

def test_templates():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page(viewport={'width': 1280, 'height': 900})
        page.on('console', lambda msg: errors.append(msg.text) if msg.type == 'error' else None)
        page.on('pageerror', lambda err: errors.append(str(err)))

        page.goto('http://localhost:8765/create.html')
        page.wait_for_load_state('networkidle')
        page.click('text=+ 风格库')
        page.wait_for_selector('#style-lib-modal', state='visible')
        page.click('text=我的风格')
        page.click('text=新建我的风格')
        page.wait_for_selector('.style-editor-form', state='visible')

        # Verify template bar visible
        assert page.is_visible('text=参考模板'), "Template label not visible"

        # Verify 4 template cards
        cards = page.locator('.style-template-card').all()
        assert len(cards) == 4, f"Expected 4 template cards, got {len(cards)}"

        # Click first template and verify textarea filled
        page.click('.style-template-card >> nth=0')
        prompt_value = page.input_value('.style-editor-textarea')
        assert len(prompt_value) > 50, f"Prompt not filled: {prompt_value[:50]}"

        # Verify counter updated
        counter_text = page.inner_text('.style-editor-counter')
        expected_count = len(prompt_value)
        assert str(expected_count) in counter_text, f"Counter not updated: {counter_text}"

        print('All template tests passed!')
        print(f'Console errors: {len(errors)}')
        for e in errors:
            print(f'  ERROR: {e}')

        browser.close()

if __name__ == '__main__':
    test_templates()
```

运行：

```bash
python3 /tmp/verify_style_templates.py
```

预期输出：

```
All template tests passed!
Console errors: 0
```

- [ ] **Step 3: 提交验证结果**

```bash
git commit --allow-empty -m "test(style-templates): e2e verification passed"
```

---

## Self-Review Checklist

- [ ] **Spec coverage**: 设计文档中的「模板内容」「交互流程」「视觉样式」「边界情况」「实现位置」「测试要点」均有对应任务覆盖。
- [ ] **Placeholder scan**: 计划中没有 TBD/TODO，每个步骤包含具体代码或命令。
- [ ] **Type consistency**: `templatePresets` 的 `index` 字段与 `systemStylePresets` 数组索引一致；`updateCounter()` 调用与现有函数签名一致。

---

## 执行方式

计划完成后，可选择：

1. **Subagent-Driven（推荐）**：为每个 Task 派独立子代理，逐任务 review。
2. **Inline Execution**：在当前会话中按 Task 顺序执行，使用 `superpowers:executing-plans`。
