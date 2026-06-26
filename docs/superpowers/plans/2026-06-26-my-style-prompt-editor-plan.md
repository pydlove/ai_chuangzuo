# 我的风格提示词编辑器实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在风格库弹窗的「我的风格」tab 中实现新建/编辑风格提示词的子视图编辑器，含基础校验。

**Architecture:** 在 `shared.js` 的 `openStyleLibrary()` 内部新增 `renderStyleEditor(mode, index)`，负责把弹窗内容区切换为编辑表单；保存后修改内存中的 `userStylePresets` 并重新渲染列表。`shared.css` 补充编辑器与错误态样式。无需改动各页面 HTML。

**Tech Stack:** 纯 HTML / CSS / JavaScript（浏览器原生 API），无外部依赖。

## Global Constraints

- 数据仅保存在前端内存，刷新后丢失。
- 用户可见字段仅「风格名称」和「风格提示词」。
- 风格名称最多 20 字，风格提示词最多 1000 字。
- 新建与编辑共用同一套表单组件。
- 编辑器以弹窗内容区子视图形式呈现，不叠加新弹窗。
- 校验失败时保存按钮不可用，并给出明确错误提示。

---

## 文件清单

| 文件 | 操作 | 说明 |
|---|---|---|
| `.superpowers/brainstorm/6491-1782131242/content/shared.css` | 修改 | 新增编辑器子视图、表单字段、错误态、字数统计样式 |
| `.superpowers/brainstorm/6491-1782131242/content/shared.js` | 修改 | 在 `openStyleLibrary()` 内新增 `renderStyleEditor()`，并修改「新建/编辑」按钮事件 |
| `.superpowers/brainstorm/6491-1782131242/content/create.html` | 无需改动 | 复用 `openStyleLibrary()` |

---

### Task 1: 添加编辑器样式

**Files:**
- Modify: `.superpowers/brainstorm/6491-1782131242/content/shared.css`

**Interfaces:**
- Consumes: 无
- Produces: `.style-editor-header`, `.style-editor-back`, `.style-editor-form`, `.style-editor-field`, `.style-editor-label`, `.style-editor-input`, `.style-editor-textarea`, `.style-editor-error`, `.style-editor-counter`, `.style-editor-counter.over`, `.style-editor-footer`, `.style-editor-btn-primary:disabled`, `.style-editor-btn-secondary` 等 CSS 类

- [ ] **Step 1: 编写 CSS 代码**

在 `shared.css` 的「风格库」样式区块之后追加以下内容：

```css
/* ===== 我的风格编辑器 ===== */
.style-editor-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}
.style-editor-back {
  background: none;
  border: none;
  color: #595959;
  font-size: 14px;
  cursor: pointer;
  padding: 4px 8px 4px 0;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.style-editor-back:hover { color: #07c160; }
.style-editor-title {
  font-size: 18px;
  font-weight: 700;
  color: #1a1a1a;
}
.style-editor-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.style-editor-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.style-editor-label {
  font-size: 14px;
  font-weight: 500;
  color: #262626;
}
.style-editor-label .required {
  color: #ff4d4f;
  margin-left: 2px;
}
.style-editor-input,
.style-editor-textarea {
  padding: 10px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 14px;
  color: #1a1a1a;
  font-family: inherit;
  outline: none;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.style-editor-input:focus,
.style-editor-textarea:focus {
  border-color: #07c160;
  box-shadow: 0 0 0 2px rgba(7,193,96,0.15);
}
.style-editor-input.error,
.style-editor-textarea.error {
  border-color: #ff4d4f;
}
.style-editor-input.error:focus,
.style-editor-textarea.error:focus {
  box-shadow: 0 0 0 2px rgba(255,77,79,0.15);
}
.style-editor-textarea {
  min-height: 180px;
  resize: vertical;
  line-height: 1.6;
}
.style-editor-hint {
  font-size: 12px;
  color: #8c8c8c;
  line-height: 1.5;
}
.style-editor-error {
  font-size: 12px;
  color: #ff4d4f;
  line-height: 1.5;
  min-height: 18px;
}
.style-editor-counter {
  font-size: 12px;
  color: #8c8c8c;
  text-align: right;
}
.style-editor-counter.over {
  color: #ff4d4f;
  font-weight: 500;
}
.style-editor-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 8px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}
.style-editor-btn-secondary {
  padding: 8px 20px;
  background: #fff;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 14px;
  color: #595959;
  cursor: pointer;
}
.style-editor-btn-secondary:hover {
  border-color: #07c160;
  color: #07c160;
}
.style-editor-btn-primary {
  padding: 8px 20px;
  background: #07c160;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  color: #fff;
  font-weight: 600;
  cursor: pointer;
}
.style-editor-btn-primary:hover { background: #06ad56; }
.style-editor-btn-primary:disabled {
  background: #d9d9d9;
  cursor: not-allowed;
}

@media (max-width: 600px) {
  .style-editor-textarea { min-height: 140px; }
  .style-editor-footer { padding-top: 12px; }
}
```

- [ ] **Step 2: 验证样式文件可加载**

打开浏览器访问 `create.html`，确认页面无样式异常，控制台无 404。

- [ ] **Step 3: 提交**

```bash
git add .superpowers/brainstorm/6491-1782131242/content/shared.css
git commit -m "feat(style-editor): add editor sub-view styles"
```

---

### Task 2: 实现编辑器渲染与保存逻辑

**Files:**
- Modify: `.superpowers/brainstorm/6491-1782131242/content/shared.js`

**Interfaces:**
- Consumes: `userStylePresets` 数组、`contentArea` DOM 元素、`renderMy()` 函数
- Produces: `renderStyleEditor(mode, index)` 函数，其中 `mode` 为 `'create'` 或 `'edit'`，`index` 为编辑时的数组下标

- [ ] **Step 1: 在 `openStyleLibrary()` 内定义 `renderStyleEditor`**

在 `renderMy()` 函数之后、`systemTab.onclick = renderSystem;` 之前插入以下函数：

```javascript
    function renderStyleEditor(mode, index) {
      var isCreate = mode === 'create';
      var existing = isCreate ? null : userStylePresets[index];
      var formData = {
        name: isCreate ? '' : (existing ? existing.name : ''),
        prompt: isCreate ? '' : (existing ? existing.prompt : '')
      };

      contentArea.innerHTML = '';

      var wrap = document.createElement('div');

      // Header
      var header = document.createElement('div');
      header.className = 'style-editor-header';
      var backBtn = document.createElement('button');
      backBtn.className = 'style-editor-back';
      backBtn.innerHTML = '← 返回';
      backBtn.onclick = renderMy;
      var title = document.createElement('div');
      title.className = 'style-editor-title';
      title.textContent = isCreate ? '新建我的风格' : '编辑提示词';
      header.appendChild(backBtn);
      header.appendChild(title);
      wrap.appendChild(header);

      // Form
      var form = document.createElement('div');
      form.className = 'style-editor-form';

      // Name field
      var nameField = document.createElement('div');
      nameField.className = 'style-editor-field';
      var nameLabel = document.createElement('label');
      nameLabel.className = 'style-editor-label';
      nameLabel.innerHTML = '风格名称<span class="required">*</span>';
      var nameInput = document.createElement('input');
      nameInput.className = 'style-editor-input';
      nameInput.type = 'text';
      nameInput.placeholder = '例如：我的小红书风';
      nameInput.value = formData.name;
      nameInput.maxLength = 20;
      var nameError = document.createElement('div');
      nameError.className = 'style-editor-error';
      nameField.appendChild(nameLabel);
      nameField.appendChild(nameInput);
      nameField.appendChild(nameError);
      form.appendChild(nameField);

      // Prompt field
      var promptField = document.createElement('div');
      promptField.className = 'style-editor-field';
      var promptLabel = document.createElement('label');
      promptLabel.className = 'style-editor-label';
      promptLabel.innerHTML = '风格提示词<span class="required">*</span>';
      var promptTextarea = document.createElement('textarea');
      promptTextarea.className = 'style-editor-textarea';
      promptTextarea.placeholder = '描述你希望 AI 采用的语气、结构、用词习惯等...';
      promptTextarea.value = formData.prompt;
      var promptHint = document.createElement('div');
      promptHint.className = 'style-editor-hint';
      promptHint.textContent = '提示词会作为系统提示的一部分影响生成结果。';
      var promptCounter = document.createElement('div');
      promptCounter.className = 'style-editor-counter';
      var promptError = document.createElement('div');
      promptError.className = 'style-editor-error';
      promptField.appendChild(promptLabel);
      promptField.appendChild(promptTextarea);
      promptField.appendChild(promptHint);
      promptField.appendChild(promptCounter);
      promptField.appendChild(promptError);
      form.appendChild(promptField);

      function updateCounter() {
        var len = promptTextarea.value.length;
        promptCounter.textContent = len + ' / 1000';
        promptCounter.classList.toggle('over', len > 1000);
      }
      function clearErrors() {
        nameInput.classList.remove('error');
        promptTextarea.classList.remove('error');
        nameError.textContent = '';
        promptError.textContent = '';
      }
      function validate() {
        clearErrors();
        var valid = true;
        if (!nameInput.value.trim()) {
          nameInput.classList.add('error');
          nameError.textContent = '请输入风格名称';
          valid = false;
        }
        if (!promptTextarea.value.trim()) {
          promptTextarea.classList.add('error');
          promptError.textContent = '请输入风格提示词';
          valid = false;
        } else if (promptTextarea.value.length > 1000) {
          promptTextarea.classList.add('error');
          promptError.textContent = '提示词不能超过 1000 字';
          valid = false;
        }
        return valid;
      }

      promptTextarea.addEventListener('input', updateCounter);
      updateCounter();

      // Footer
      var footer = document.createElement('div');
      footer.className = 'style-editor-footer';
      var cancelBtn = document.createElement('button');
      cancelBtn.className = 'style-editor-btn-secondary';
      cancelBtn.textContent = '取消';
      cancelBtn.onclick = renderMy;
      var saveBtn = document.createElement('button');
      saveBtn.className = 'style-editor-btn-primary';
      saveBtn.textContent = '保存';
      saveBtn.onclick = function() {
        if (!validate()) {
          if (!nameInput.value.trim()) nameInput.focus();
          else promptTextarea.focus();
          return;
        }
        var newStyle = {
          name: nameInput.value.trim().slice(0, 20),
          desc: '',
          count: isCreate ? 0 : (existing ? existing.count : 0),
          prompt: promptTextarea.value.trim().slice(0, 1000)
        };
        if (isCreate) {
          userStylePresets.unshift(newStyle);
        } else if (existing) {
          userStylePresets[index] = newStyle;
        }
        renderMy();
      };
      footer.appendChild(cancelBtn);
      footer.appendChild(saveBtn);
      form.appendChild(footer);

      wrap.appendChild(form);
      contentArea.appendChild(wrap);

      // Auto focus
      if (isCreate || !nameInput.value.trim()) nameInput.focus();
      else promptTextarea.focus();
    }
```

- [ ] **Step 2: 修改「新建我的风格」点击事件**

找到 `renderMy()` 中如下代码：

```javascript
      addCard.onclick = function() { alert('新建风格：从历史文章提取 / 手动配置'); };
```

替换为：

```javascript
      addCard.onclick = function() { renderStyleEditor('create'); };
```

- [ ] **Step 3: 修改「编辑提示词」点击事件**

找到 `renderMy()` 中如下代码：

```javascript
        card.querySelector('.style-lib-edit-btn').onclick = function(e) {
          e.stopPropagation();
          alert('编辑提示词：「' + s.name + '」\n\n将打开编辑器（原型暂未实现）');
        };
```

替换为：

```javascript
        card.querySelector('.style-lib-edit-btn').onclick = function(e) {
          e.stopPropagation();
          renderStyleEditor('edit', idx);
        };
```

- [ ] **Step 4: 验证 JavaScript 无语法错误**

在浏览器中打开 `create.html`，按 `F12` 打开控制台，确认无 `SyntaxError` 或 `ReferenceError`。

- [ ] **Step 5: 提交**

```bash
git add .superpowers/brainstorm/6491-1782131242/content/shared.js
git commit -m "feat(style-editor): implement create/edit style prompt editor"
```

---

### Task 3: 端到端验证

**Files:**
- 无需修改代码

**Interfaces:**
- Consumes: Task 1 和 Task 2 完成后的 `shared.css` 与 `shared.js`
- Produces: 确认的功能清单

- [ ] **Step 1: 打开 create.html 并进入风格库**

在浏览器中打开 `.superpowers/brainstorm/6491-1782131242/content/create.html`，滚动到「文章风格」区域，点击「+ 风格库」。

- [ ] **Step 2: 验证新建流程**

1. 切换到「我的风格」tab。
2. 点击「新建我的风格」。
3. 预期：弹窗内容区切换为编辑器子视图，显示「风格名称」「风格提示词」两个字段。
4. 输入名称和提示词，点击「保存」。
5. 预期：回到「我的风格」列表，新建的卡片出现在最前面，标题和提示词正确。

- [ ] **Step 3: 验证编辑流程**

1. 在「我的风格」列表中点击任意已有风格的「编辑提示词」。
2. 预期：进入编辑器子视图，字段回显当前风格的名称和提示词。
3. 修改提示词，点击「保存」。
4. 预期：回到列表，该卡片内容已更新。

- [ ] **Step 4: 验证校验**

1. 点击「新建我的风格」，不填任何内容直接点击「保存」。
2. 预期：名称和提示词输入框边框变红，分别显示「请输入风格名称」「请输入风格提示词」。
3. 在提示词文本域输入超过 1000 字的内容。
4. 预期：右下角字数统计变红，保存按钮不可用（或被禁用），提示「提示词不能超过 1000 字」。

- [ ] **Step 5: 验证取消/返回**

1. 点击「新建我的风格」，输入任意内容。
2. 点击「取消」或「← 返回」。
3. 预期：回到列表，未新增任何风格。

- [ ] **Step 6: 验证移动端**

1. 使用浏览器开发者工具切换到移动设备视口（例如 iPhone 12 Pro）。
2. 重复 Step 2-5，确认表单和按钮在窄屏下正常显示、可点击。

- [ ] **Step 7: 提交验证结果**

```bash
git commit --allow-empty -m "test(style-editor): manual e2e verification passed"
```

---

## Self-Review Checklist

- [ ] **Spec coverage**: 设计文档中的「数据模型」「交互流程」「表单字段」「校验规则」「实现位置」「边界情况」「测试要点」均有对应任务覆盖。
- [ ] **Placeholder scan**: 计划中没有 TBD/TODO，每个步骤包含具体代码或命令。
- [ ] **Type consistency**: `renderStyleEditor(mode, index)` 的参数命名在 Task 2 中保持一致；`userStylePresets` 数组结构与设计文档一致。

---

## 执行方式

计划完成后，可选择：

1. **Subagent-Driven（推荐）**：为每个 Task 派独立子代理，逐任务 review。
2. **Inline Execution**：在当前会话中按 Task 顺序执行，使用 `superpowers:executing-plans`。
