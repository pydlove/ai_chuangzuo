# 意见反馈 / 关于我们弹框实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让 `console.html` 顶部 header 的「反馈」和「关于我们」弹框与「消息铃铛」弹框使用一致的 modal 实现，居中显示，并支持 `×` 按钮、点击遮罩、按 `ESC` 三种手动关闭方式。

**Architecture:** 复用现有 `shared.css` 的 `.modal-overlay` / `.modal-content` 样式和 `shared.js` 的动态创建 overlay 模式；补充全局 `keydown` 监听实现 `ESC` 关闭；通过 Playwright 脚本截图验证居中位置和关闭行为。

**Tech Stack:** 纯前端 HTML/CSS/JS 原型，无构建工具；验证使用 Python + Playwright。

## Global Constraints

- 保持原型前端-only，不引入后端或构建步骤。
- 修改范围限于 `shared.js`、`shared.css` 和新增测试脚本。
- 不改动 `console.html` 入口图标。
- 弹框结构需与 `openMessageModal()` 保持一致。
- 关闭方式必须包含：`×` 按钮、点击遮罩、按 `ESC`。

---

## File Structure

| File | Responsibility |
|------|----------------|
| `.superpowers/brainstorm/6491-1782131242/content/shared.js` | 定义 `openFeedbackModal()`、`openAboutModal()`、关闭函数、全局 `ESC` 监听 |
| `.superpowers/brainstorm/6491-1782131242/content/shared.css` | 定义 `.modal-overlay` 居中遮罩和 `.modal-content` 卡片样式 |
| `tests/e2e/verify_feedback_about_modals.py` | Playwright 测试脚本：打开弹框、截图、验证居中、验证三种关闭方式 |

---

### Task 1: 添加全局 ESC 关闭弹框监听

**Files:**
- Modify: `.superpowers/brainstorm/6491-1782131242/content/shared.js`
- Test: `tests/e2e/verify_feedback_about_modals.py`（在 Task 3 中完整编写）

**Interfaces:**
- Consumes: 现有 modal overlay DOM（id 以 `-modal` 结尾）
- Produces: 全局 `keydown` 监听器，按 `ESC` 时移除最上层 modal overlay

- [ ] **Step 1: 在 `shared.js` 初始化位置添加 ESC 监听**

找到 `shared.js` 中 DOMContentLoaded 或页面初始化代码附近（例如在 `renderGlobalFooter` 调用之后），添加：

```javascript
  // 全局 ESC 关闭最上层弹框
  document.addEventListener('keydown', function(e) {
    if (e.key !== 'Escape') return;
    var modals = document.querySelectorAll('.modal-overlay');
    if (modals.length === 0) return;
    var topModal = modals[modals.length - 1];
    topModal.remove();
  });
```

- [ ] **Step 2: 手动验证 ESC 关闭**

启动本地服务器：

```bash
./scripts/local/start.sh
```

打开 `http://localhost:28585/.superpowers/brainstorm/6491-1782131242/content/console.html`，点击 header 中的「反馈」图标，按 `ESC`，确认弹框关闭。

- [ ] **Step 3: 提交**

```bash
git add .superpowers/brainstorm/6491-1782131242/content/shared.js
git commit -m "$(cat <<'EOF'
feat(modal): ESC 关闭最上层弹框

按 Escape 键时移除当前最上层的 .modal-overlay，适用于反馈、关于我们等弹框。

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

---

### Task 2: 确保反馈 / 关于我们弹框与消息铃铛一致并居中

**Files:**
- Modify: `.superpowers/brainstorm/6491-1782131242/content/shared.js`
- Modify: `.superpowers/brainstorm/6491-1782131242/content/shared.css`
- Test: `tests/e2e/verify_feedback_about_modals.py`（在 Task 3 中完整编写）

**Interfaces:**
- Consumes: `openMessageModal()` 的 modal 结构模式
- Produces: 结构一致的 `openFeedbackModal()`、`openAboutModal()`

- [ ] **Step 1: 检查并统一 `openFeedbackModal()` 结构**

`shared.js` 中 `openFeedbackModal()` 当前实现已使用 `.modal-overlay` + `.modal-content`，结构与小铃铛一致。确认其代码为：

```javascript
  function openFeedbackModal() {
    if (document.getElementById('feedback-modal')) return;
    var overlay = document.createElement('div');
    overlay.className = 'modal-overlay';
    overlay.id = 'feedback-modal';
    overlay.innerHTML = '<div class="modal-content" style="max-width:420px;" onclick="event.stopPropagation()">' +
      '<div class="modal-header"><div class="modal-title">意见反馈</div><button class="modal-close" onclick="closeFeedbackModal()">×</button></div>' +
      '<div class="modal-body">' +
        '<div style="margin-bottom:12px;">' +
          '<label style="display:block;font-size:13px;color:var(--text-secondary);margin-bottom:6px;">反馈类型</label>' +
          '<select id="feedback-type" style="width:100%;padding:8px;border:1px solid var(--border-color);border-radius:6px;background:var(--card-bg);color:var(--text-color);">' +
            '<option>功能建议</option><option>问题反馈</option><option>其他</option>' +
          '</select>' +
        '</div>' +
        '<div style="margin-bottom:16px;">' +
          '<label style="display:block;font-size:13px;color:var(--text-secondary);margin-bottom:6px;">反馈内容</label>' +
          '<textarea id="feedback-content" rows="4" style="width:100%;padding:8px;border:1px solid var(--border-color);border-radius:6px;background:var(--card-bg);color:var(--text-color);resize:vertical;"></textarea>' +
        '</div>' +
        '<button onclick="submitFeedback()" style="width:100%;padding:10px;background:#07c160;color:#fff;border:none;border-radius:8px;font-size:14px;cursor:pointer;">提交反馈</button>' +
      '</div>' +
    '</div>';
    overlay.onclick = closeFeedbackModal;
    document.body.appendChild(overlay);
  }
```

如 `overlay.onclick` 缺失，补上：`overlay.onclick = closeFeedbackModal;`

- [ ] **Step 2: 检查并统一 `openAboutModal()` 结构**

`shared.js` 中 `openAboutModal()` 当前实现已使用 `.modal-overlay` + `.modal-content`。确认其代码包含：

```javascript
  function openAboutModal() {
    if (document.getElementById('about-modal')) return;
    var overlay = document.createElement('div');
    overlay.className = 'modal-overlay';
    overlay.id = 'about-modal';
    overlay.innerHTML = '<div class="modal-content" onclick="event.stopPropagation()">' +
      '<div class="modal-header"><div class="modal-title">关于我们</div><button class="modal-close" onclick="closeAboutModal()">×</button></div>' +
      // ... existing body and footer ...
    '</div>';
    overlay.onclick = closeAboutModal;
    document.body.appendChild(overlay);
  }
```

如 `overlay.onclick` 缺失，补上：`overlay.onclick = closeAboutModal;`

- [ ] **Step 3: 加固 `.modal-overlay` 居中样式**

在 `shared.css` 的 `.modal-overlay` 规则中，确保已有并显式声明居中：

```css
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10020;
  padding: 24px;
}
```

如 `align-items: center; justify-content: center;` 缺失，添加。

- [ ] **Step 4: 手动验证居中**

刷新 `console.html`，分别点击「反馈」和「关于我们」图标，确认弹框出现在屏幕正中间（PC 视图），关闭按钮可见。

- [ ] **Step 5: 提交**

```bash
git add .superpowers/brainstorm/6491-1782131242/content/shared.js .superpowers/brainstorm/6491-1782131242/content/shared.css
git commit -m "$(cat <<'EOF'
feat(modal): 统一反馈/关于我们弹框居中与关闭行为

确保两个弹框与消息铃铛使用一致的 modal 结构，.modal-overlay 显式 flex 居中，点击遮罩可关闭。

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

---

### Task 3: 编写 Playwright 验证脚本

**Files:**
- Create: `tests/e2e/verify_feedback_about_modals.py`

**Interfaces:**
- Consumes: 本地运行的 `console.html` 页面
- Produces: 测试脚本 + 截图文件

- [ ] **Step 1: 创建测试脚本**

创建 `tests/e2e/verify_feedback_about_modals.py`：

```python
import os
import time
from playwright.sync_api import sync_playwright

BASE = "http://localhost:28585"
CONSOLE_URL = f"{BASE}/.superpowers/brainstorm/6491-1782131242/content/console.html"
OUT_DIR = os.path.join(os.path.dirname(__file__), "screenshots")
os.makedirs(OUT_DIR, exist_ok=True)

def capture(page, name):
    path = os.path.join(OUT_DIR, f"{name}.png")
    page.screenshot(path=path, full_page=False)
    print(f"Saved {path}")

def main():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page(viewport={"width": 1280, "height": 800})
        page.goto(CONSOLE_URL)
        page.wait_for_timeout(1000)

        # Open feedback modal
        page.click("button[title='反馈']")
        page.wait_for_timeout(500)
        capture(page, "feedback_modal_open")

        # Close via overlay click (click on the top-left corner of the overlay, outside the modal content)
        page.locator("#feedback-modal").click(force=True, position={"x": 10, "y": 10})
        page.wait_for_timeout(500)
        capture(page, "feedback_modal_closed_overlay")

        # Open about modal
        page.click("button[title='关于我们']")
        page.wait_for_timeout(500)
        capture(page, "about_modal_open")

        # Close via ESC
        page.keyboard.press("Escape")
        page.wait_for_timeout(500)
        capture(page, "about_modal_closed_esc")

        # Re-open feedback and close via X button
        page.click("button[title='反馈']")
        page.wait_for_timeout(500)
        page.click("#feedback-modal .modal-close")
        page.wait_for_timeout(500)
        capture(page, "feedback_modal_closed_x")

        browser.close()
        print("All modal verifications completed.")

if __name__ == "__main__":
    main()
```

- [ ] **Step 2: 运行测试脚本**

确保本地服务器已启动：

```bash
./scripts/local/start.sh
```

运行：

```bash
python3 tests/e2e/verify_feedback_about_modals.py
```

预期输出：

```text
Saved tests/e2e/screenshots/feedback_modal_open.png
Saved tests/e2e/screenshots/feedback_modal_closed_overlay.png
Saved tests/e2e/screenshots/about_modal_open.png
Saved tests/e2e/screenshots/about_modal_closed_esc.png
Saved tests/e2e/screenshots/feedback_modal_closed_x.png
All modal verifications completed.
```

- [ ] **Step 3: 人工检查截图**

打开 `tests/e2e/screenshots/feedback_modal_open.png` 和 `tests/e2e/screenshots/about_modal_open.png`，确认：
- 弹框在屏幕正中间。
- 标题、关闭按钮、内容均可见。

- [ ] **Step 4: 提交**

```bash
git add tests/e2e/verify_feedback_about_modals.py
git commit -m "$(cat <<'EOF'
test(e2e): 验证反馈/关于我们弹框居中与关闭

新增 Playwright 脚本，覆盖打开、× 关闭、遮罩关闭、ESC 关闭三种手动关闭方式。

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

---

## Self-Review

**1. Spec coverage:**
- 弹框居中显示 → Task 2 加固 `.modal-overlay` 居中样式。
- 手动关闭（`×`、遮罩、ESC）→ Task 1 添加 ESC 监听；Task 2 确保 `×` 和遮罩关闭；Task 3 测试三种方式。
- 与消息铃铛实现一致 → Task 2 统一结构。
- 不改入口图标 → 全局约束明确。

**2. Placeholder scan:**
- 无 `TBD`、`TODO`、未填充的代码。
- 所有代码块包含实际可运行内容。

**3. Type consistency:**
- 所有 modal 函数签名和 DOM id 与现有代码一致。
- ESC 监听选择器 `.modal-overlay` 与 CSS class 一致。

---

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-07-01-feedback-about-modals-plan.md`.

**Two execution options:**

1. **Subagent-Driven (recommended)** — I dispatch a fresh subagent per task, review between tasks, fast iteration.
2. **Inline Execution** — Execute tasks in this session using executing-plans, batch execution with checkpoints for review.

**Which approach?**
