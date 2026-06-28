# 生成内容手动编辑实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在预览页和独立编辑页中，让用户通过 contenteditable 直接修改 AI 生成文章的标题、段落、小标题和高亮块，并手动保存到 localStorage。

**Architecture:** 在 `shared.js` 中新增文章编辑存储层（load/save/serialize/apply），并在 `preview.html` 顶部工具栏增加编辑开关与悬浮操作栏；独立页 `edit.html` 复用同一套存储函数，按块渲染所有可编辑区域。编辑态通过 `contenteditable` 开启，保存时把每块 innerHTML 序列化为 JSON 写入 localStorage，页面加载时反向覆盖静态内容。

**Tech Stack:** 纯 HTML/CSS/JS 原型，无构建工具，无后端，localStorage 持久化，Playwright 做功能验证。

## Global Constraints

- 无后端、无构建工具、无包管理器。
- 编辑结果仅保存在浏览器 `localStorage`。
- 可编辑范围：文章标题、小标题、正文段落、重点高亮块、列表项。
- 本期不做 AI 改写、段落增删/排序、版本历史、发布描述/标签编辑。
- 保存前校验标题不能为空。
- 粘贴时强制纯文本，过滤外部样式。

---

## File Structure

| File | Responsibility |
|------|----------------|
| `.superpowers/brainstorm/6491-1782131242/content/shared.js` | 编辑存储层、启用/禁用编辑态、粘贴过滤、序列化/反序列化 |
| `.superpowers/brainstorm/6491-1782131242/content/shared.css` | 编辑态高亮边框、悬浮操作栏、编辑图标样式 |
| `.superpowers/brainstorm/6491-1782131242/content/preview.html` | 顶部编辑开关/进入编辑页按钮、调用编辑逻辑 |
| `.superpowers/brainstorm/6491-1782131242/content/edit.html` | 独立编辑页，按块渲染可编辑区域 |
| `tests/e2e/verify_content_editing.py` | Playwright 验证脚本：内联编辑保存、独立编辑页、刷新保留 |

---

### Task 1: 文章编辑存储层

**Files:**
- Modify: `.superpowers/brainstorm/6491-1782131242/content/shared.js`（在自定义模板相关代码之后新增）
- Test: 浏览器控制台

**Interfaces:**
- Produces: `loadArticleEdits()`, `saveArticleEdits(edits)`, `serializeArticleBlocks(mockupEl)`, `applyArticleEdits(mockupEl, edits)`

- [ ] **Step 1: 新增存储与序列化函数**

在 `shared.js` 中 `openCustomTemplateEditor` 之后插入：

```js
  var ARTICLE_EDITS_KEY = 'aichuangzuo_article_edits';

  function loadArticleEdits() {
    try {
      var raw = localStorage.getItem(ARTICLE_EDITS_KEY);
      if (!raw) return null;
      var parsed = JSON.parse(raw);
      if (!parsed || !Array.isArray(parsed.blocks)) return null;
      return parsed;
    } catch (e) {
      return null;
    }
  }

  function saveArticleEdits(edits) {
    try {
      localStorage.setItem(ARTICLE_EDITS_KEY, JSON.stringify(edits));
      return true;
    } catch (e) {
      showToast('保存失败，请检查浏览器存储权限');
      return false;
    }
  }

  function getEditableSelectors() {
    return [
      '.preview-title',
      '.preview-heading',
      '.article-preview > p',
      '.preview-highlight',
      '.preview-list li'
    ];
  }

  function serializeArticleBlocks(mockupEl) {
    var article = mockupEl.closest('.article-preview');
    if (!article) return [];
    var combinedSelector = getEditableSelectors().join(', ');
    return Array.from(article.querySelectorAll(combinedSelector)).map(function(el) {
      var type = 'paragraph';
      if (el.classList.contains('preview-title')) type = 'title';
      else if (el.classList.contains('preview-heading')) type = 'heading';
      else if (el.classList.contains('preview-highlight')) type = 'highlight';
      else if (el.tagName === 'LI' || (el.parentElement && el.parentElement.classList.contains('preview-list'))) type = 'list-item';
      return { type: type, html: el.innerHTML.trim() };
    });
  }

  function applyArticleEdits(mockupEl, edits) {
    if (!edits || !Array.isArray(edits.blocks)) return;
    var article = mockupEl.closest('.article-preview');
    if (!article) return;
    var combinedSelector = getEditableSelectors().join(', ');
    var editableEls = Array.from(article.querySelectorAll(combinedSelector));
    edits.blocks.forEach(function(block, idx) {
      if (idx < editableEls.length && block.html) {
        editableEls[idx].innerHTML = block.html;
      }
    });
  }
```

- [ ] **Step 2: 验证存储函数可用**

在浏览器控制台执行：

```js
var edits = {
  articleId: 'default_preview',
  savedAt: Date.now(),
  blocks: [
    { type: 'title', html: '测试标题' },
    { type: 'paragraph', html: '测试段落。' }
  ]
};
saveArticleEdits(edits);
console.log(loadArticleEdits());
```

Expected: 控制台打印出刚才保存的 edits 对象。

- [ ] **Step 3: 提交**

```bash
git add .superpowers/brainstorm/6491-1782131242/content/shared.js
git commit -m "feat(edit): add article editing storage helpers"
```

---

### Task 2: 编辑态样式与粘贴过滤

**Files:**
- Modify: `.superpowers/brainstorm/6491-1782131242/content/shared.css`
- Modify: `.superpowers/brainstorm/6491-1782131242/content/shared.js`

**Interfaces:**
- Consumes: `loadArticleEdits()`, `saveArticleEdits(edits)`, `serializeArticleBlocks(mockupEl)`, `applyArticleEdits(mockupEl, edits)`
- Produces: `enableArticleEditing(mockupEl)`, `disableArticleEditing(mockupEl, isCancel)`, `sanitizePaste(e)`

- [ ] **Step 1: 在 shared.css 添加编辑态样式**

在 `shared.css` 末尾追加：

```css
/* 文章编辑态 */
.article-editing .preview-title,
.article-editing .preview-heading,
.article-editing .article-preview > p,
.article-editing .preview-highlight,
.article-editing .preview-list li {
  outline: none;
  border-radius: 4px;
  transition: background 0.15s, box-shadow 0.15s;
}

.article-editing .preview-title:hover,
.article-editing .preview-heading:hover,
.article-editing .article-preview > p:hover,
.article-editing .preview-highlight:hover,
.article-editing .preview-list li:hover {
  background: #f6ffed;
  box-shadow: 0 0 0 2px #b7eb8f;
}

.article-editing .preview-title:focus,
.article-editing .preview-heading:focus,
.article-editing .article-preview > p:focus,
.article-editing .preview-highlight:focus,
.article-editing .preview-list li:focus {
  background: #fff;
  box-shadow: 0 0 0 2px #07c160;
}

.article-edit-fab {
  position: fixed;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 28px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.1);
  z-index: 10003;
  font-size: 14px;
}

.article-edit-fab .edit-hint {
  color: #595959;
  white-space: nowrap;
}

.article-edit-fab button {
  padding: 6px 14px;
  border-radius: 16px;
  border: none;
  font-size: 13px;
  cursor: pointer;
}

.article-edit-fab .cancel-btn {
  background: #f5f5f5;
  color: #595959;
}

.article-edit-fab .save-btn {
  background: #07c160;
  color: #fff;
}

/* 非编辑态 hover 显示编辑提示 */
.article-preview:not(.article-editing) .preview-title,
.article-preview:not(.article-editing) .preview-heading,
.article-preview:not(.article-editing) > p,
.article-preview:not(.article-editing) .preview-highlight,
.article-preview:not(.article-editing) .preview-list li {
  cursor: text;
  position: relative;
}

.article-preview:not(.article-editing) .preview-title:hover::after,
.article-preview:not(.article-editing) .preview-heading:hover::after,
.article-preview:not(.article-editing) > p:hover::after,
.article-preview:not(.article-editing) .preview-highlight:hover::after,
.article-preview:not(.article-editing) .preview-list li:hover::after {
  content: '编辑';
  position: absolute;
  top: 4px;
  right: 4px;
  padding: 2px 6px;
  background: #07c160;
  color: #fff;
  font-size: 11px;
  border-radius: 4px;
  pointer-events: auto;
  cursor: pointer;
  z-index: 5;
}
```

- [ ] **Step 2: 在 shared.js 实现启用/禁用编辑态**

在 `shared.js` 中 `applyArticleEdits` 之后插入：

```js
  var __articleEditSnapshot = null;
  var __articleEditFab = null;

  function sanitizePaste(e) {
    e.preventDefault();
    var text = (e.clipboardData || window.clipboardData).getData('text/plain');
    document.execCommand('insertText', false, text);
  }

  function attachPasteSanitizer(mockupEl) {
    var selectors = getEditableSelectors();
    selectors.forEach(function(selector) {
      mockupEl.querySelectorAll(selector).forEach(function(el) {
        el.addEventListener('paste', sanitizePaste);
      });
    });
  }

  function detachPasteSanitizer(mockupEl) {
    var selectors = getEditableSelectors();
    selectors.forEach(function(selector) {
      mockupEl.querySelectorAll(selector).forEach(function(el) {
        el.removeEventListener('paste', sanitizePaste);
      });
    });
  }

  function markEditableModified(el) {
    el.setAttribute('data-edited', 'true');
    updateEditFabHint();
  }

  function updateEditFabHint() {
    if (!__articleEditFab) return;
    var count = document.querySelectorAll('[data-edited="true"]').length;
    var hint = __articleEditFab.querySelector('.edit-hint');
    if (hint) hint.textContent = count > 0 ? '已修改 ' + count + ' 处' : '正在编辑';
  }

  function createEditFab(onCancel, onSave) {
    if (__articleEditFab) return __articleEditFab;
    var fab = document.createElement('div');
    fab.className = 'article-edit-fab';
    fab.innerHTML =
      '<span class="edit-hint">正在编辑</span>' +
      '<button class="cancel-btn">取消</button>' +
      '<button class="save-btn">保存修改</button>';
    fab.querySelector('.cancel-btn').onclick = onCancel;
    fab.querySelector('.save-btn').onclick = onSave;
    document.body.appendChild(fab);
    __articleEditFab = fab;
    return fab;
  }

  function removeEditFab() {
    if (__articleEditFab) {
      __articleEditFab.remove();
      __articleEditFab = null;
    }
  }

  function enableArticleEditing(mockupEl) {
    if (!mockupEl) mockupEl = document.querySelector('.mockup .article-preview');
    if (!mockupEl) return;
    var article = mockupEl.closest('.article-preview');
    if (!article) return;

    __articleEditSnapshot = article.innerHTML;
    article.classList.add('article-editing');

    var selectors = getEditableSelectors();
    selectors.forEach(function(selector) {
      article.querySelectorAll(selector).forEach(function(el) {
        el.setAttribute('contenteditable', 'true');
        el.addEventListener('input', function() { markEditableModified(el); });
      });
    });

    attachPasteSanitizer(article);

    createEditFab(
      function() { disableArticleEditing(mockupEl, true); },
      function() { disableArticleEditing(mockupEl, false); }
    );
    updateEditFabHint();
  }

  function disableArticleEditing(mockupEl, isCancel) {
    if (!mockupEl) mockupEl = document.querySelector('.mockup .article-preview');
    if (!mockupEl) return;
    var article = mockupEl.closest('.article-preview');
    if (!article) return;

    if (isCancel && __articleEditSnapshot !== null) {
      article.innerHTML = __articleEditSnapshot;
    } else {
      var title = article.querySelector('.preview-title');
      if (title && !title.innerText.trim()) {
        showToast('标题不能为空');
        return;
      }
      var edits = {
        articleId: 'default_preview',
        savedAt: Date.now(),
        blocks: serializeArticleBlocks(article)
      };
      if (saveArticleEdits(edits)) {
        showToast('内容已保存');
      } else {
        return;
      }
    }

    article.classList.remove('article-editing');
    var selectors = getEditableSelectors();
    selectors.forEach(function(selector) {
      article.querySelectorAll(selector).forEach(function(el) {
        el.removeAttribute('contenteditable');
        el.removeAttribute('data-edited');
      });
    });

    detachPasteSanitizer(article);
    removeEditFab();
    __articleEditSnapshot = null;
  }
```

- [ ] **Step 3: 验证编辑态可启用**

在浏览器控制台执行（确保当前在 preview.html）：

```js
enableArticleEditing();
```

Expected: 文章标题、段落、高亮块出现淡绿色 hover 效果，底部出现悬浮操作栏。

- [ ] **Step 4: 提交**

```bash
git add .superpowers/brainstorm/6491-1782131242/content/shared.js .superpowers/brainstorm/6491-1782131242/content/shared.css
git commit -m "feat(edit): add inline editing state and paste sanitization"
```

---

### Task 3: 预览页加载时应用已保存编辑

**Files:**
- Modify: `.superpowers/brainstorm/6491-1782131242/content/preview.html`
- Modify: `.superpowers/brainstorm/6491-1782131242/content/shared.js`（如有需要）

**Interfaces:**
- Consumes: `loadArticleEdits()`, `applyArticleEdits(mockupEl, edits)`

- [ ] **Step 1: 在 preview.html 加载时应用 edits**

在 `preview.html` 底部的 `<script>` 中，找到 `DOMContentLoaded` 处理逻辑，在 `applySelectedTemplateToPreview()` 调用之后追加：

```js
    // 应用用户保存的文章编辑
    var edits = loadArticleEdits();
    if (edits) {
      document.querySelectorAll('.article-preview').forEach(function(el) {
        applyArticleEdits(el, edits);
      });
    }
```

假设现有逻辑类似：

```js
document.addEventListener('DOMContentLoaded', function() {
  if (typeof renderPublishMeta === 'function') renderPublishMeta();
  if (typeof applySelectedTemplateToPreview === 'function') applySelectedTemplateToPreview();
});
```

修改后：

```js
document.addEventListener('DOMContentLoaded', function() {
  if (typeof renderPublishMeta === 'function') renderPublishMeta();
  if (typeof applySelectedTemplateToPreview === 'function') applySelectedTemplateToPreview();
  var edits = loadArticleEdits();
  if (edits) {
    document.querySelectorAll('.article-preview').forEach(function(el) {
      applyArticleEdits(el, edits);
    });
  }
});
```

- [ ] **Step 2: 验证编辑内容在刷新后保留**

1. 在浏览器控制台执行：

```js
saveArticleEdits({
  articleId: 'default_preview',
  savedAt: Date.now(),
  blocks: [
    { type: 'title', html: '修改后的标题' }
  ]
});
```

2. 刷新页面。
3. Expected: 文章标题显示为「修改后的标题」。

- [ ] **Step 3: 提交**

```bash
git add .superpowers/brainstorm/6491-1782131242/content/preview.html
git commit -m "feat(edit): apply saved article edits on preview load"
```

---

### Task 4: 预览页顶部编辑入口

**Files:**
- Modify: `.superpowers/brainstorm/6491-1782131242/content/preview.html`

**Interfaces:**
- Consumes: `enableArticleEditing(mockupEl)`

- [ ] **Step 1: 在 PC 和移动端预览区顶部添加编辑按钮**

在 `preview.html` 中找到 PC 端工具栏（通常在 `.mockup-header` 下方的白色条），在右侧操作区追加：

```html
<button onclick="toggleArticleEditing('pc')" id="pc-edit-toggle" style="padding: 6px 12px; background: #fff; border: 1px solid #d9d9d9; border-radius: 6px; font-size: 13px; color: #595959; cursor: pointer;">编辑内容</button>
<button onclick="location.href='edit.html'" style="padding: 6px 12px; background: #f6ffed; border: 1px solid #b7eb8f; border-radius: 6px; font-size: 13px; color: #07c160; cursor: pointer;">进入编辑模式</button>
```

在移动端对应位置也追加同样两个按钮（样式可微调，字号 12px）。

- [ ] **Step 2: 在 preview.html 底部脚本中添加 toggle 函数**

在 `<script>` 中追加：

```js
  function toggleArticleEditing(screen) {
    var mockupId = screen === 'pc' ? 'screen-preview' : 'screen-preview';
    var mockup = document.querySelector('#' + mockupId + ' .article-preview');
    if (!mockup) return;
    if (mockup.closest('.article-editing')) {
      // 已在编辑态，点击按钮无额外作用（保存/取消走悬浮栏）
      return;
    }
    enableArticleEditing(mockup);
  }

  // 点击可编辑元素直接进入编辑态
  document.querySelectorAll('.article-preview').forEach(function(article) {
    article.addEventListener('click', function(e) {
      if (article.classList.contains('article-editing')) return;
      var selectors = ['.preview-title', '.preview-heading', '.article-preview > p', '.preview-highlight', '.preview-list li'];
      var focusEl = null;
      selectors.forEach(function(sel) {
        if (!focusEl && e.target.matches(sel)) focusEl = e.target;
        if (!focusEl && e.target.closest(sel)) focusEl = e.target.closest(sel);
      });
      if (focusEl) {
        enableArticleEditing(article);
        focusEl.focus();
      }
    });
  });
```

实际实现时，PC 和移动端文章预览区都在同一个 `#screen-preview` 内，函数会同时启用两者。若未来需要区分屏幕，可分别传入 mockup 选择器。

- [ ] **Step 3: 验证入口可用**

1. 打开 `preview.html`。
2. 点击「编辑内容」。
3. Expected: 可编辑元素出现 hover 效果，底部出现悬浮栏。

- [ ] **Step 4: 提交**

```bash
git add .superpowers/brainstorm/6491-1782131242/content/preview.html
git commit -m "feat(edit): add editing controls to preview page toolbar"
```

---

### Task 5: 独立编辑页 edit.html

**Files:**
- Create: `.superpowers/brainstorm/6491-1782131242/content/edit.html`

**Interfaces:**
- Consumes: `loadArticleEdits()`, `saveArticleEdits(edits)`, `serializeArticleBlocks(mockupEl)`, `applyArticleEdits(mockupEl, edits)`
- Produces: `renderEditBlocks(container, edits)`, `saveEditPage()`

- [ ] **Step 1: 创建 edit.html 骨架**

创建文件 `.superpowers/brainstorm/6491-1782131242/content/edit.html`：

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="utf-8"/>
<meta content="width=device-width, initial-scale=1.0" name="viewport"/>
<title>爱创作 - 编辑内容</title>
<link rel="stylesheet" href="shared.css">
<style>
  .edit-page-wrap { max-width: 720px; margin: 0 auto; padding: 24px; }
  .edit-block { margin-bottom: 16px; }
  .edit-block-label { font-size: 12px; color: #8c8c8c; margin-bottom: 6px; }
  .edit-block-area {
    width: 100%;
    min-height: 60px;
    padding: 12px;
    border: 1px solid #d9d9d9;
    border-radius: 8px;
    font-size: 15px;
    line-height: 1.7;
    color: #262626;
    box-sizing: border-box;
    outline: none;
  }
  .edit-block-area:focus { border-color: #07c160; box-shadow: 0 0 0 2px rgba(7,193,96,0.15); }
  .edit-block-area.title { font-size: 22px; font-weight: 700; }
  .edit-block-area.heading { font-size: 18px; font-weight: 600; }
  .edit-block-area.highlight { background: #f6ffed; border-left: 4px solid #07c160; }
  .edit-actions { position: fixed; bottom: 24px; left: 50%; transform: translateX(-50%); display: flex; gap: 12px; padding: 10px 16px; background: #fff; border: 1px solid #e8e8e8; border-radius: 28px; box-shadow: 0 4px 16px rgba(0,0,0,0.1); z-index: 10003; }
  .edit-actions button { padding: 8px 18px; border-radius: 18px; border: none; font-size: 14px; cursor: pointer; }
  .edit-actions .cancel { background: #f5f5f5; color: #595959; }
  .edit-actions .save { background: #07c160; color: #fff; }
</style>
</head>
<body>
<div class="edit-page-wrap">
  <div style="display: flex; align-items: center; gap: 12px; margin-bottom: 24px;">
    <button onclick="location.href='preview.html'" style="padding: 6px 12px; background: #fff; border: 1px solid #d9d9d9; border-radius: 6px; font-size: 13px; cursor: pointer;">← 返回预览</button>
    <h1 style="font-size: 20px; margin: 0;">编辑内容</h1>
  </div>
  <div id="edit-blocks"></div>
</div>
<div class="edit-actions">
  <button class="cancel" onclick="cancelEditPage()">取消</button>
  <button class="save" onclick="saveEditPage()">保存修改</button>
</div>
<script src="shared.js?v=7"></script>
<script>
  function renderEditBlocks() {
    var container = document.getElementById('edit-blocks');
    var edits = loadArticleEdits();
    var blocks = edits && Array.isArray(edits.blocks) ? edits.blocks : [];
    if (blocks.length === 0) {
      container.innerHTML = '<div style="color: #8c8c8c; text-align: center; padding: 40px;">暂无已保存的编辑内容，请先在预览页生成并编辑文章。</div>';
      return;
    }
    container.innerHTML = '';
    blocks.forEach(function(block, idx) {
      var wrap = document.createElement('div');
      wrap.className = 'edit-block';
      var label = document.createElement('div');
      label.className = 'edit-block-label';
      var labelText = { title: '标题', heading: '小标题', paragraph: '正文段落', highlight: '重点高亮', 'list-item': '列表项' }[block.type] || '内容';
      label.textContent = labelText + ' #' + (idx + 1);
      var area = document.createElement('div');
      area.className = 'edit-block-area ' + block.type;
      area.setAttribute('contenteditable', 'true');
      area.setAttribute('data-type', block.type);
      area.innerHTML = block.html;
      area.addEventListener('paste', function(e) {
        e.preventDefault();
        var text = (e.clipboardData || window.clipboardData).getData('text/plain');
        document.execCommand('insertText', false, text);
      });
      wrap.appendChild(label);
      wrap.appendChild(area);
      container.appendChild(wrap);
    });
  }

  function saveEditPage() {
    var areas = document.querySelectorAll('.edit-block-area');
    var blocks = [];
    var hasEmptyTitle = false;
    areas.forEach(function(area) {
      var type = area.getAttribute('data-type');
      var html = area.innerHTML.trim();
      if (type === 'title' && !area.innerText.trim()) hasEmptyTitle = true;
      blocks.push({ type: type, html: html });
    });
    if (hasEmptyTitle) {
      showToast('标题不能为空');
      return;
    }
    var edits = { articleId: 'default_preview', savedAt: Date.now(), blocks: blocks };
    if (saveArticleEdits(edits)) {
      showToast('内容已保存');
      setTimeout(function() { location.href = 'preview.html'; }, 300);
    }
  }

  function cancelEditPage() {
    location.href = 'preview.html';
  }

  renderEditBlocks();
</script>
</body>
</html>
```

- [ ] **Step 2: 验证独立编辑页**

1. 先在 `preview.html` 保存一次编辑，确保 `localStorage` 有数据。
2. 打开 `edit.html`。
3. Expected: 页面列出所有可编辑块，修改后保存返回 `preview.html`，预览内容更新。

- [ ] **Step 3: 提交**

```bash
git add .superpowers/brainstorm/6491-1782131242/content/edit.html
git commit -m "feat(edit): add standalone content editing page"
```

---

### Task 6: Playwright 功能验证

**Files:**
- Create: `tests/e2e/verify_content_editing.py`

**Interfaces:**
- Consumes: 页面 DOM（preview.html 编辑态、edit.html）

- [ ] **Step 1: 编写验证脚本**

```python
from playwright.sync_api import sync_playwright
import time

BASE = 'http://localhost:28585/.superpowers/brainstorm/6491-1782131242/content'

def test_content_editing():
    errors = []
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page(viewport={'width': 1440, 'height': 900})

        def on_console(msg):
            if msg.type == 'error':
                errors.append(msg.text)
        page.on('console', on_console)

        # 清理旧数据
        page.goto(BASE + '/preview.html', wait_until='networkidle')
        time.sleep(0.5)
        page.evaluate("""() => {
            try { localStorage.removeItem('aichuangzuo_article_edits'); } catch(e) {}
        }""")

        # 1. 打开 preview.html 进入编辑态
        page.goto(BASE + '/preview.html', wait_until='networkidle')
        time.sleep(0.5)
        page.click('button:has-text("编辑内容")')
        time.sleep(0.3)

        # 2. 修改标题
        title = page.locator('.preview-title').first
        title.fill('用户修改后的标题')

        # 3. 保存
        page.click('.article-edit-fab .save-btn')
        time.sleep(0.3)

        # 4. 验证 localStorage
        saved = page.evaluate("""() => {
            try {
                var raw = localStorage.getItem('aichuangzuo_article_edits');
                var edits = raw ? JSON.parse(raw) : null;
                return edits && edits.blocks.some(function(b) {
                    return b.type === 'title' && b.html.includes('用户修改后的标题');
                });
            } catch(e) { return false; }
        }""")
        assert saved, 'edited title not saved to localStorage'

        # 5. 刷新页面确认保留
        page.reload(wait_until='networkidle')
        time.sleep(0.5)
        assert page.locator('.preview-title:has-text("用户修改后的标题")').count() > 0, 'saved edit not applied after reload'

        # 6. 进入 edit.html 修改并保存
        page.goto(BASE + '/edit.html', wait_until='networkidle')
        time.sleep(0.5)
        area = page.locator('.edit-block-area.title').first
        area.fill('独立编辑页修改后的标题')
        page.click('.edit-actions .save')
        time.sleep(0.5)

        # 7. 返回 preview.html 验证
        page.goto(BASE + '/preview.html', wait_until='networkidle')
        time.sleep(0.5)
        assert page.locator('.preview-title:has-text("独立编辑页修改后的标题")').count() > 0, 'edit page save not reflected on preview'

        page.screenshot(path='/tmp/content_editing_verify.png', full_page=True)

        # 清理
        page.evaluate("""() => {
            try { localStorage.removeItem('aichuangzuo_article_edits'); } catch(e) {}
        }""")

        browser.close()
        print('content editing verification passed')
        if errors:
            print(f'console errors: {len(errors)}')
            for e in errors:
                print(f'  ERROR: {e}')

if __name__ == '__main__':
    test_content_editing()
```

- [ ] **Step 2: 运行脚本**

确保本地服务已启动：

```bash
./scripts/local/start.sh
```

运行验证：

```bash
python3 tests/e2e/verify_content_editing.py
```

Expected: 终端打印 `content editing verification passed`，且 `/tmp/content_editing_verify.png` 显示已修改标题的预览页。

- [ ] **Step 3: 提交**

```bash
git add tests/e2e/verify_content_editing.py
git commit -m "test(edit): add content editing e2e verification"
```

---

### Task 7: 缓存刷新与最终验证

**Files:**
- Modify: `.superpowers/brainstorm/6491-1782131242/content/*.html`（shared.js 版本号加 1）

- [ ] **Step 1: 批量更新 shared.js 版本号**

由于 `shared.js` 大幅改动，将所有 HTML 中的 `shared.js?v=7` 升级到 `shared.js?v=8`：

```bash
python3 -c "
import os, glob
base = '.superpowers/brainstorm/6491-1782131242/content'
for path in glob.glob(os.path.join(base, '*.html')):
    with open(path, 'r', encoding='utf-8') as f:
        text = f.read()
    new_text = text.replace('shared.js?v=7', 'shared.js?v=8')
    if new_text != text:
        with open(path, 'w', encoding='utf-8') as f:
            f.write(new_text)
"
```

- [ ] **Step 2: 最终手动验证清单**

- [ ] 预览页点击「编辑内容」进入编辑态，元素可 hover 高亮。
- [ ] 修改标题和段落后点击保存，刷新页面内容保留。
- [ ] 点击取消不保存本次修改。
- [ ] 标题置空时保存被阻止并提示。
- [ ] 从预览页进入 edit.html，修改保存后返回预览页，内容已更新。
- [ ] 粘贴带样式的文本时仅保留纯文本。

- [ ] **Step 3: 提交**

```bash
git add .superpowers/brainstorm/6491-1782131242/content/*.html
git commit -m "chore(edit): bump shared.js cache version for content editing"
```

---

## Self-Review

**1. Spec coverage:**

- 预览页内联编辑 ✓ Task 4
- 独立编辑页 ✓ Task 5
- 可编辑范围：标题/小标题/段落/高亮/列表项 ✓ Task 2
- 手动保存持久化 ✓ Task 2
- 粘贴过滤 ✓ Task 2
- 标题非空校验 ✓ Task 2
- localStorage 失败提示 ✓ Task 1
- 刷新后应用已保存 edits ✓ Task 3
- 不做 AI 改写/撤销/增删段落 ✓ 未涉及

**2. Placeholder scan:**

无 TBD/TODO，所有步骤包含具体代码或命令。

**3. Type consistency：**

- `blocks` 项统一为 `{ type, html }`。
- `type` 取值统一为 `title` / `heading` / `paragraph` / `highlight` / `list-item`。
- `localStorage` 键名统一为 `aichuangzuo_article_edits`。
- 函数名 `loadArticleEdits` / `saveArticleEdits` / `serializeArticleBlocks` / `applyArticleEdits` / `enableArticleEditing` / `disableArticleEditing` 全文一致。

无未覆盖需求，计划可执行。

---

**Plan complete and saved to `docs/superpowers/plans/2026-06-28-content-editing-plan.md`. Two execution options:**

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

**Which approach?**
