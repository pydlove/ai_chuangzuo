# 字数设置弹窗 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the create-page word count `<select>` with a popup that supports 4 selection dimensions (by platform, by scenario, by tier, custom), each option annotated with a use-case label, max 3000 chars.

**Architecture:** Pure HTML/CSS/JS prototype. Add a `wordCountPresets` data object, an `openWordCountModal()` function that dynamically builds the modal DOM (mirroring the existing template-library modal style), and replace the two `<select>` elements in the create page with trigger buttons bound to that function. State held in two globals: `currentWordCount` (number) and `currentWordLabel` (string).

**Tech Stack:** Vanilla JS (no framework), Playwright for verification.

## Global Constraints

- Pure frontend HTML/CSS/JS prototype, no backend.
- Custom word count max = 3000 chars, min = 1 char; input out of range clamps to the boundary.
- 7 platforms: wechat, xiaohongshu, toutiao, baijiahao, zhihu, douyin, general.
- 4 scenarios: 教程/步骤 (1200), 测评/对比 (1000), 清单/种草 (500), 故事/叙事 (1800).
- 5 tiers: 500/1000/1500/2500/3000 with duration labels.
- Modal visual style must mirror existing `openTemplateLibrary()` (white card on dark overlay, top-right close, green selected state).
- Both PC and Mobile create pages must switch from `<select>` to a button trigger.
- Trigger button text format: `📝 {count} 字 · {label} ✏️`.
- Default selected tab when opening modal: if a publish-platform radio/select on the create page has a value, default to "按平台推荐" and pre-select that platform's row; otherwise default to "按字数档位".

---

### Task 1: Add word count preset data and globals

**Files:**
- Modify: `.superpowers/brainstorm/6491-1782131242/content/full-prototype-v20.html`

**Interfaces:**
- Consumes: spec §3, §4, §5, §11
- Produces: global `wordCountPresets`, `currentWordCount = 1500`, `currentWordLabel = '标准深度文'`

- [ ] **Step 1: Locate a stable spot for the new globals**

Search for an existing global such as `var templatePresets` (around line 3698) — place the new globals immediately AFTER it so they share the data-definition section.

- [ ] **Step 2: Insert `wordCountPresets` and the two globals**

Insert the following block immediately after the closing `];` of `var templatePresets`:

```javascript
  var wordCountPresets = {
    platform: {
      wechat: [
        { count: 800,  label: '早报 / 简评' },
        { count: 1500, label: '标准深度文' },
        { count: 2500, label: '专题报道' },
        { count: 3000, label: '行业研究（上限）' }
      ],
      xiaohongshu: [
        { count: 300,  label: '标题种草' },
        { count: 500,  label: '图文分享' },
        { count: 800,  label: '详细测评' },
        { count: 1200, label: '步骤拆解教程' }
      ],
      toutiao: [
        { count: 400,  label: '热点快讯' },
        { count: 800,  label: '事件报道' },
        { count: 1500, label: '专题分析' },
        { count: 2000, label: '观点长文' }
      ],
      baijiahao: [
        { count: 1000, label: '知识科普' },
        { count: 1500, label: '生活攻略' },
        { count: 2000, label: '人文叙事' },
        { count: 2500, label: '行业洞察' }
      ],
      zhihu: [
        { count: 800,  label: '精炼回答' },
        { count: 1500, label: '专业回答' },
        { count: 2500, label: '长篇分析' }
      ],
      douyin: [
        { count: 150, label: '封面金句' },
        { count: 300, label: '图配文' },
        { count: 600, label: '情感短篇' }
      ],
      general: [
        { count: 500,  label: '短文' },
        { count: 1000, label: '中等' },
        { count: 1500, label: '标准' },
        { count: 2500, label: '长文' }
      ]
    },
    scenario: [
      { count: 1200, label: '教程 / 步骤', desc: '操作步骤详细说明，适合图文对照' },
      { count: 1000, label: '测评 / 对比', desc: '优缺点详细对比，附评分' },
      { count: 500,  label: '清单 / 种草', desc: '快速清单 + 标签，重点突出' },
      { count: 1800, label: '故事 / 叙事', desc: '沉浸式叙事，节奏完整' }
    ],
    tier: [
      { count: 500,  label: '短文', desc: '速读，3 分钟读完' },
      { count: 1000, label: '中等', desc: '标准阅读，5 分钟' },
      { count: 1500, label: '标准', desc: '深度阅读，8 分钟' },
      { count: 2500, label: '长文', desc: '专题阅读，12 分钟' },
      { count: 3000, label: '超长', desc: '深度专题（上限）' }
    ]
  };

  var currentWordCount = 1500;
  var currentWordLabel = '标准深度文';
```

- [ ] **Step 3: Verify**

Run:
```bash
python3 - <<'PY'
import re
with open('.superpowers/brainstorm/6491-1782131242/content/full-prototype-v20.html') as f:
    txt = f.read()

assert 'var wordCountPresets = {' in txt, 'wordCountPresets missing'
assert 'var currentWordCount = 1500;' in txt, 'currentWordCount missing'
assert "var currentWordLabel = '标准深度文';" in txt, 'currentWordLabel missing'

# Count platform entries
plat_section = re.search(r"platform:\s*\{(.*?)\},\s*scenario:", txt, re.S)
plats = re.findall(r"^\s*([\w-]+):\s*\[", plat_section.group(1), re.M)
print('Platforms:', plats)
assert set(plats) == {'wechat','xiaohongshu','toutiao','baijiahao','zhihu','douyin','general'}, 'Wrong platforms'

# Count scenario entries
sc_section = re.search(r"scenario:\s*\[(.*?)\],\s*tier:", txt, re.S)
sc = re.findall(r"\{ count: \d+, label:", sc_section.group(1))
print('Scenarios:', len(sc))
assert len(sc) == 4, 'Expected 4 scenarios'

# Count tier entries
t_section = re.search(r"tier:\s*\[(.*?)\]\s*\};", txt, re.S)
t = re.findall(r"\{ count: \d+, label:", t_section.group(1))
print('Tiers:', len(t))
assert len(t) == 5, 'Expected 5 tiers'

print('All checks passed')
PY
```

Expected: `Platforms: ['wechat', 'xiaohongshu', 'toutiao', 'baijiahao', 'zhihu', 'douyin', 'general']`, `Scenarios: 4`, `Tiers: 5`, `All checks passed`.

- [ ] **Step 4: Commit**

```bash
git add .superpowers/brainstorm/6491-1782131242/content/full-prototype-v20.html
git commit -m "feat(word-count): add preset data and globals"
```

---

### Task 2: Add `openWordCountModal()` shell with 4 tabs

**Files:**
- Modify: `.superpowers/brainstorm/6491-1782131242/content/full-prototype-v20.html`

**Interfaces:**
- Consumes: `wordCountPresets`, `currentWordCount`, `currentWordLabel` (Task 1)
- Produces: global function `openWordCountModal()` that creates `#word-count-modal` with header / 4-tab bar / content area / footer; tab switching is wired up; default tab is selected per Global Constraints.

- [ ] **Step 1: Locate insertion point**

Find `function openTemplateLibrary()` (around line 4546). Insert the new function immediately AFTER its closing `}`. Keep them adjacent so all "open modal" functions live together.

- [ ] **Step 2: Implement `openWordCountModal()` shell**

Insert the following:

```javascript
  function openWordCountModal() {
    var existing = document.getElementById('word-count-modal');
    if (existing) existing.remove();

    // Decide default tab from spec Global Constraints.
    // Look for a checked radio / select with name "publishPlatform" on the create page.
    var platEl = document.querySelector('[name="publishPlatform"]:checked')
              || document.querySelector('[data-publish-platform].selected')
              || document.querySelector('#pc-current-platform');
    var defaultPlatform = (platEl && platEl.dataset && platEl.dataset.platform)
                       || (platEl && platEl.getAttribute('data-platform'))
                       || null;
    var validPlatforms = ['wechat','xiaohongshu','toutiao','baijiahao','zhihu','douyin','general'];
    var initialTab = (defaultPlatform && validPlatforms.indexOf(defaultPlatform) >= 0)
                     ? 'platform'
                     : 'tier';

    var tabs = [
      { key: 'platform', label: '按平台推荐' },
      { key: 'scenario', label: '按内容场景' },
      { key: 'tier',     label: '按字数档位' },
      { key: 'custom',   label: '自定义字数' }
    ];

    var overlay = document.createElement('div');
    overlay.id = 'word-count-modal';
    overlay.style.cssText = 'position: fixed; inset: 0; background: rgba(0,0,0,0.5); z-index: 10002; display: flex; align-items: center; justify-content: center; padding: 20px; font-family: -apple-system, BlinkMacSystemFont, sans-serif;';

    var box = document.createElement('div');
    box.style.cssText = 'background: #fff; border-radius: 16px; width: 640px; max-width: 100%; max-height: 90vh; display: flex; flex-direction: column; box-shadow: 0 8px 32px rgba(0,0,0,0.2); position: relative; overflow: hidden;';

    var closeBtn = document.createElement('button');
    closeBtn.innerHTML = '&times;';
    closeBtn.style.cssText = 'position: absolute; top: 10px; right: 14px; background: none; border: none; font-size: 22px; cursor: pointer; color: #8c8c8c; line-height: 1; padding: 4px 8px; z-index: 2;';
    closeBtn.onclick = function () { overlay.remove(); };
    box.appendChild(closeBtn);

    var headerWrap = document.createElement('div');
    headerWrap.style.cssText = 'padding: 22px 24px 12px; flex-shrink: 0;';
    var header = document.createElement('div');
    header.style.cssText = 'font-size: 18px; font-weight: 700; color: #1a1a1a; margin-bottom: 4px; padding-right: 28px;';
    header.textContent = '设置文章字数';
    var sub = document.createElement('div');
    sub.style.cssText = 'font-size: 13px; color: #8c8c8c;';
    sub.textContent = '选择合适的字数，让 AI 写出更精准的内容';
    headerWrap.appendChild(header);
    headerWrap.appendChild(sub);
    box.appendChild(headerWrap);

    var tabBar = document.createElement('div');
    tabBar.style.cssText = 'display: flex; gap: 6px; padding: 0 24px 12px; flex-shrink: 0; overflow-x: auto; border-bottom: 1px solid #f0f0f0;';
    box.appendChild(tabBar);

    var content = document.createElement('div');
    content.style.cssText = 'padding: 16px 24px; flex: 1; min-height: 0; overflow-y: auto;';
    box.appendChild(content);

    var footer = document.createElement('div');
    footer.style.cssText = 'padding: 12px 24px 16px; flex-shrink: 0; border-top: 1px solid #f0f0f0; display: flex; justify-content: flex-end; gap: 8px;';
    var cancelBtn = document.createElement('button');
    cancelBtn.textContent = '取消';
    cancelBtn.style.cssText = 'padding: 8px 18px; border-radius: 8px; border: 1px solid #d9d9d9; background: #fff; color: #595959; cursor: pointer; font-size: 14px;';
    cancelBtn.onclick = function () { overlay.remove(); };
    var confirmBtn = document.createElement('button');
    confirmBtn.textContent = '确认';
    confirmBtn.style.cssText = 'padding: 8px 18px; border-radius: 8px; border: 1px solid #07c160; background: #07c160; color: #fff; cursor: pointer; font-size: 14px; font-weight: 600;';
    // Wired up in Task 4.
    footer.appendChild(cancelBtn);
    footer.appendChild(confirmBtn);
    box.appendChild(footer);

    overlay.appendChild(box);

    function renderTabBar() {
      tabBar.innerHTML = '';
      tabs.forEach(function (tab) {
        var btn = document.createElement('button');
        btn.textContent = tab.label;
        var active = tab.key === state.activeTab;
        btn.style.cssText = 'padding: 6px 14px; border-radius: 16px; border: 1px solid ' + (active ? '#07c160' : '#d9d9d9') + '; background: ' + (active ? '#f6ffed' : '#fff') + '; color: ' + (active ? '#07c160' : '#595959') + '; font-size: 13px; cursor: pointer; white-space: nowrap; font-weight: ' + (active ? '600' : '500') + ';';
        btn.onclick = function () {
          state.activeTab = tab.key;
          renderTabBar();
          renderContent();
        };
        tabBar.appendChild(btn);
      });
    }

    function renderContent() {
      // Populated in Task 3.
      content.innerHTML = '<div style="color:#8c8c8c;font-size:13px;padding:24px 0;text-align:center;">（占位，Task 3 填充）</div>';
    }

    var state = {
      activeTab: initialTab,
      selectedPlatform: defaultPlatform && validPlatforms.indexOf(defaultPlatform) >= 0 ? defaultPlatform : 'wechat',
      selectedCount: currentWordCount,
      selectedLabel: currentWordLabel,
      customValue: currentWordCount
    };

    renderTabBar();
    renderContent();
    document.body.appendChild(overlay);
  }
```

- [ ] **Step 3: Verify**

Run:
```bash
python3 - <<'PY'
import re
with open('.superpowers/brainstorm/6491-1782131242/content/full-prototype-v20.html') as f:
    txt = f.read()

assert 'function openWordCountModal()' in txt, 'openWordCountModal not defined'
m = re.search(r'function openWordCountModal\(\)\s*\{', txt)
assert m, 'function declaration not found'

# Crude check that 4 tabs are wired up.
assert '按平台推荐' in txt, 'platform tab label missing'
assert '按内容场景' in txt, 'scenario tab label missing'
assert '按字数档位' in txt, 'tier tab label missing'
assert '自定义字数' in txt, 'custom tab label missing'

# Default tab logic checks for publishPlatform.
assert "publishPlatform" in txt, 'publishPlatform lookup missing'

# Modal id is word-count-modal.
assert "id = 'word-count-modal'" in txt or "id=\"word-count-modal\"" in txt, 'modal id wrong'

print('All checks passed')
PY
```

Expected: `All checks passed`.

- [ ] **Step 4: Commit**

```bash
git add .superpowers/brainstorm/6491-1782131242/content/full-prototype-v20.html
git commit -m "feat(word-count): add openWordCountModal shell with 4 tabs"
```

---

### Task 3: Render content for each tab

**Files:**
- Modify: `.superpowers/brainstorm/6491-1782131242/content/full-prototype-v20.html`

**Interfaces:**
- Consumes: `wordCountPresets` (Task 1), `state` object inside `openWordCountModal()` (Task 2)
- Produces: `renderContent()` actually draws the 4 different bodies. Selecting a card updates `state.selectedCount`, `state.selectedLabel` (custom updates `state.customValue` and downstream).

- [ ] **Step 1: Locate the placeholder `renderContent`**

Inside `openWordCountModal()`, find the line `content.innerHTML = '<div style=...占位...'. Replace the entire body of `renderContent` with the implementation below.

- [ ] **Step 2: Replace `renderContent()` body**

Replace the function body so it becomes:

```javascript
    function makeCard(count, label, desc) {
      var card = document.createElement('div');
      var selected = state.selectedCount === count && state.selectedLabel === label;
      card.style.cssText = 'border: 2px solid ' + (selected ? '#07c160' : '#e8e8e8') +
        '; border-radius: 10px; padding: 12px 14px; background: ' + (selected ? '#f6ffed' : '#fff') +
        '; cursor: pointer; display: flex; flex-direction: column; gap: 4px; margin-bottom: 8px;';
      var top = document.createElement('div');
      top.style.cssText = 'display: flex; justify-content: space-between; align-items: center;';
      var left = document.createElement('div');
      left.style.cssText = 'font-weight: 700; color: #1a1a1a; font-size: 16px;';
      left.textContent = count + ' 字';
      top.appendChild(left);
      if (selected) {
        var check = document.createElement('span');
        check.textContent = '✓';
        check.style.cssText = 'color: #07c160; font-weight: 700; font-size: 16px;';
        top.appendChild(check);
      }
      card.appendChild(top);
      var labelDiv = document.createElement('div');
      labelDiv.style.cssText = 'color: #595959; font-size: 13px;';
      labelDiv.textContent = label;
      card.appendChild(labelDiv);
      if (desc) {
        var descDiv = document.createElement('div');
        descDiv.style.cssText = 'color: #8c8c8c; font-size: 12px;';
        descDiv.textContent = desc;
        card.appendChild(descDiv);
      }
      card.onclick = function () {
        state.selectedCount = count;
        state.selectedLabel = label;
        renderContent();
      };
      return card;
    }

    function renderPlatformTab() {
      var platKeys = Object.keys(wordCountPresets.platform);
      // Platform sub-tabs as a row of small chips.
      var platBar = document.createElement('div');
      platBar.style.cssText = 'display: flex; gap: 6px; margin-bottom: 12px; overflow-x: auto; padding-bottom: 4px;';
      platKeys.forEach(function (pk) {
        var b = document.createElement('button');
        b.textContent = ({wechat:'公众号',xiaohongshu:'小红书',toutiao:'今日头条',baijiahao:'百家号',zhihu:'知乎',douyin:'抖音图文',general:'通用风格'})[pk];
        var active = pk === state.selectedPlatform;
        b.style.cssText = 'padding: 4px 12px; border-radius: 12px; border: 1px solid ' + (active ? '#07c160' : '#d9d9d9') +
          '; background: ' + (active ? '#f6ffed' : '#fff') + '; color: ' + (active ? '#07c160' : '#595959') +
          '; font-size: 12px; cursor: pointer; white-space: nowrap;';
        b.onclick = function () { state.selectedPlatform = pk; renderContent(); };
        platBar.appendChild(b);
      });
      content.appendChild(platBar);

      var list = wordCountPresets.platform[state.selectedPlatform] || [];
      list.forEach(function (o) { content.appendChild(makeCard(o.count, o.label, '')); });
    }

    function renderScenarioTab() {
      wordCountPresets.scenario.forEach(function (o) {
        content.appendChild(makeCard(o.count, o.label, o.desc));
      });
    }

    function renderTierTab() {
      wordCountPresets.tier.forEach(function (o) {
        content.appendChild(makeCard(o.count, o.label, o.desc));
      });
    }

    function renderCustomTab() {
      var wrap = document.createElement('div');
      wrap.style.cssText = 'padding: 8px 4px;';
      var hint = document.createElement('div');
      hint.style.cssText = 'color: #595959; font-size: 13px; margin-bottom: 12px;';
      hint.textContent = '自定义 1-3000 字，AI 将按字数精确生成。';
      wrap.appendChild(hint);

      var display = document.createElement('div');
      display.style.cssText = 'font-size: 36px; font-weight: 700; color: #07c160; text-align: center; margin: 16px 0;';
      display.textContent = state.customValue + ' 字';
      wrap.appendChild(display);

      var input = document.createElement('input');
      input.type = 'number';
      input.min = 1;
      input.max = 3000;
      input.value = state.customValue;
      input.style.cssText = 'width: 100%; padding: 12px 16px; border: 1px solid #d9d9d9; border-radius: 8px; font-size: 18px; text-align: center; box-sizing: border-box;';
      input.oninput = function () {
        var v = parseInt(input.value, 10);
        if (isNaN(v)) return;
        if (v < 1) v = 1;
        if (v > 3000) v = 3000;
        state.customValue = v;
        state.selectedCount = v;
        state.selectedLabel = '自定义';
        slider.value = v;
        display.textContent = v + ' 字';
      };
      wrap.appendChild(input);

      var slider = document.createElement('input');
      slider.type = 'range';
      slider.min = 1;
      slider.max = 3000;
      slider.value = state.customValue;
      slider.style.cssText = 'width: 100%; margin-top: 16px; accent-color: #07c160;';
      slider.oninput = function () {
        var v = parseInt(slider.value, 10);
        state.customValue = v;
        state.selectedCount = v;
        state.selectedLabel = '自定义';
        input.value = v;
        display.textContent = v + ' 字';
      };
      wrap.appendChild(slider);

      var footer = document.createElement('div');
      footer.style.cssText = 'color: #8c8c8c; font-size: 12px; margin-top: 12px; text-align: center;';
      footer.textContent = 'AI 将生成约 ' + state.customValue + ' 字的文章';
      wrap.appendChild(footer);

      content.appendChild(wrap);
    }

    function renderContent() {
      content.innerHTML = '';
      if (state.activeTab === 'platform') renderPlatformTab();
      else if (state.activeTab === 'scenario') renderScenarioTab();
      else if (state.activeTab === 'tier') renderTierTab();
      else renderCustomTab();
    }
```

- [ ] **Step 3: Verify**

Run:
```bash
python3 - <<'PY'
with open('.superpowers/brainstorm/6491-1782131242/content/full-prototype-v20.html') as f:
    txt = f.read()
assert 'renderPlatformTab' in txt
assert 'renderScenarioTab' in txt
assert 'renderTierTab' in txt
assert 'renderCustomTab' in txt
assert "input.type = 'number'" in txt
assert "input.type = 'range'" in txt
assert 'state.customValue = v' in txt
print('All checks passed')
PY
```

Expected: `All checks passed`.

- [ ] **Step 4: Commit**

```bash
git add .superpowers/brainstorm/6491-1782131242/content/full-prototype-v20.html
git commit -m "feat(word-count): render content for all 4 tabs"
```

---

### Task 4: Wire confirm button + custom-tab Apply

**Files:**
- Modify: `.superpowers/brainstorm/6491-1782131242/content/full-prototype-v20.html`

**Interfaces:**
- Consumes: `state.selectedCount`, `state.selectedLabel`, `currentWordCount`, `currentWordLabel`
- Produces: clicking 确认 updates globals, refreshes both trigger buttons (`#pc-word-count-trigger`, `#mobile-word-count-trigger`) if they exist, closes modal.

- [ ] **Step 1: Locate the `confirmBtn` setup**

In `openWordCountModal()`, find:
```javascript
    confirmBtn.textContent = '确认';
    confirmBtn.style.cssText = '...';
    // Wired up in Task 4.
```
Replace the `// Wired up in Task 4.` comment with:

```javascript
    confirmBtn.onclick = function () {
      currentWordCount = state.selectedCount;
      currentWordLabel = state.selectedLabel;
      ['pc-word-count-trigger', 'mobile-word-count-trigger'].forEach(function (id) {
        var el = document.getElementById(id);
        if (el) el.textContent = '\uD83D\uDCDD ' + currentWordCount + ' 字 · ' + currentWordLabel + ' ✏️';
      });
      overlay.remove();
    };
```

- [ ] **Step 2: Verify**

Run:
```bash
python3 - <<'PY'
with open('.superpowers/brainstorm/6491-1782131242/content/full-prototype-v20.html') as f:
    txt = f.read()
# confirmBtn.onclick is in place
import re
m = re.search(r'confirmBtn\.onclick\s*=\s*function\s*\(\)\s*\{(.*?)\};', txt, re.S)
assert m, 'confirmBtn onclick not wired'
body = m.group(1)
assert 'currentWordCount = state.selectedCount' in body
assert 'currentWordLabel = state.selectedLabel' in body
assert 'pc-word-count-trigger' in body
assert 'mobile-word-count-trigger' in body
assert 'overlay.remove()' in body
print('All checks passed')
PY
```

Expected: `All checks passed`.

- [ ] **Step 3: Commit**

```bash
git add .superpowers/brainstorm/6491-1782131242/content/full-prototype-v20.html
git commit -m "feat(word-count): wire confirm button to update trigger labels"
```

---

### Task 5: Replace `<select>` with trigger button on create page (PC + Mobile)

**Files:**
- Modify: `.superpowers/brainstorm/6491-1782131242/content/full-prototype-v20.html`

**Interfaces:**
- Consumes: `openWordCountModal()` (Task 2–4), `currentWordCount`, `currentWordLabel` (Task 1)
- Produces: PC and Mobile 「字数要求」 `<select>` elements are replaced with `<button id="pc-word-count-trigger">` / `<button id="mobile-word-count-trigger">`.

- [ ] **Step 1: Locate the PC select**

Find the line `<label ... >字数要求</label>` followed shortly by a `<select class="mock-input">` containing `<option>800 字左右</option>` etc. (the PC one is at the top of `screen-create`). Replace the entire `<div>` that wraps that select (the `<div>` containing the `<label>字数要求</label>` and the `<select>`) with:

```html
<div>
  <label style="display: block; margin-bottom: 8px; font-weight: 500; color: #262626;">字数要求</label>
  <button id="pc-word-count-trigger" onclick="openWordCountModal()" style="width: 100%; padding: 12px 16px; border: 1px solid #d9d9d9; border-radius: 8px; font-size: 15px; background: #fff; color: #1a1a1a; text-align: left; cursor: pointer; display: flex; justify-content: space-between; align-items: center;">
    <span>📝 1500 字 · 标准深度文 ✏️</span>
  </button>
</div>
```

- [ ] **Step 2: Locate the Mobile select**

Find the corresponding mobile select (inside `screen-create` mobile mockup, also labelled `字数要求`, smaller font/padding). Replace the entire `<div>` that wraps it with:

```html
<div style="margin-bottom: 16px;">
  <label style="display: block; margin-bottom: 6px; font-weight: 500; color: #262626; font-size: 14px;">字数要求</label>
  <button id="mobile-word-count-trigger" onclick="openWordCountModal()" style="width: 100%; padding: 10px 12px; border: 1px solid #d9d9d9; border-radius: 6px; font-size: 14px; background: #fff; color: #1a1a1a; text-align: left; cursor: pointer; display: flex; justify-content: space-between; align-items: center;">
    <span>📝 1500 字 · 标准深度文 ✏️</span>
  </button>
</div>
```

- [ ] **Step 3: Verify**

Run:
```bash
python3 - <<'PY'
with open('.superpowers/brainstorm/6491-1782131242/content/full-prototype-v20.html') as f:
    txt = f.read()
# No more word-count <select class="mock-input"> within label "字数要求"
import re
# Find label "字数要求" and the next 500 chars after each occurrence.
for m in re.finditer(r'字数要求', txt):
    snippet = txt[m.start():m.start()+500]
    assert '<select' not in snippet, 'Old <select> still present near label 字数要求'
assert 'id="pc-word-count-trigger"' in txt
assert 'id="mobile-word-count-trigger"' in txt
assert 'onclick="openWordCountModal()"' in txt
print('All checks passed')
PY
```

Expected: `All checks passed`.

- [ ] **Step 4: Commit**

```bash
git add .superpowers/brainstorm/6491-1782131242/content/full-prototype-v20.html
git commit -m "feat(word-count): replace select with trigger buttons on create page"
```

---

### Task 6: End-to-end Playwright verification

**Files:**
- Create: `/tmp/verify_word_count.py` (verification script, not committed)

**Interfaces:**
- Consumes: complete HTML prototype
- Produces: 3 screenshots under `/tmp/word_count_*.png` and console assertion output

- [ ] **Step 1: Write the verification script**

Create `/tmp/verify_word_count.py`:

```python
from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page(viewport={'width': 1400, 'height': 900})
    page.goto('http://localhost:8080/.superpowers/brainstorm/6491-1782131242/content/full-prototype-v20.html')
    page.click('button[data-screen="create"]')
    page.wait_for_timeout(300)

    # 1. Trigger button exists.
    assert page.locator('#pc-word-count-trigger').count() == 1, 'PC trigger missing'

    # 2. Open modal.
    page.click('#pc-word-count-trigger')
    page.wait_for_selector('#word-count-modal')

    # 3. 4 tabs present.
    tabs = page.locator('#word-count-modal button').all()
    tab_labels = [t.inner_text().strip() for t in tabs]
    assert any('按平台推荐' in s for s in tab_labels), 'platform tab missing'
    assert any('按内容场景' in s for s in tab_labels), 'scenario tab missing'
    assert any('按字数档位' in s for s in tab_labels), 'tier tab missing'
    assert any('自定义字数' in s for s in tab_labels), 'custom tab missing'

    page.screenshot(path='/tmp/word_count_modal_open.png')

    # 4. Default tab is "按字数档位" (no platform selected in create page).
    # After clicking tier tab, verify 5 cards.
    page.get_by_role('button', name='按字数档位').first.click()
    page.wait_for_timeout(150)
    cards = page.locator('#word-count-modal [style*="border-radius: 10px"]').all()
    print('Tier cards:', len(cards))
    assert len(cards) == 5, 'Expected 5 tier cards'

    # 5. Click first tier card -> 确认 -> trigger button text updates.
    cards[0].click()
    page.wait_for_timeout(100)
    page.locator('#word-count-modal button:has-text("确认")').click()
    page.wait_for_timeout(200)
    label = page.locator('#pc-word-count-trigger').inner_text()
    print('Trigger after confirm:', label)
    assert '500 字' in label, 'Trigger did not update'

    page.screenshot(path='/tmp/word_count_after_confirm.png')

    # 6. Custom tab: open, set 800, confirm.
    page.click('#pc-word-count-trigger')
    page.wait_for_selector('#word-count-modal')
    page.get_by_role('button', name='自定义字数').first.click()
    page.wait_for_timeout(150)
    num_input = page.locator('#word-count-modal input[type="number"]')
    num_input.fill('800')
    page.wait_for_timeout(100)
    page.locator('#word-count-modal button:has-text("确认")').click()
    page.wait_for_timeout(200)
    label = page.locator('#pc-word-count-trigger').inner_text()
    print('Trigger after custom:', label)
    assert '800' in label

    page.screenshot(path='/tmp/word_count_custom.png')

    browser.close()
    print('Verification passed')
```

- [ ] **Step 2: Run the script**

```bash
python3 /tmp/verify_word_count.py
```

Expected output (last lines):
```
Trigger after confirm: 📝 500 字 · 短文 ✏️
Trigger after custom: 📝 800 字 · 自定义 ✏️
Verification passed
```

If any assertion fails, debug by checking the screenshot under `/tmp/word_count_modal_open.png` and reading the implementation.

- [ ] **Step 3: Visual review of screenshots**

Read each of the three PNGs and confirm visually:
1. `word_count_modal_open.png`: modal shows 4 tabs, default tab is selected, options visible
2. `word_count_after_confirm.png`: trigger button shows updated text
3. `word_count_custom.png`: trigger button shows custom count

- [ ] **Step 4: No commit needed**

This task produces only verification artifacts. No code changes → no commit.

---

### Task 7: Final whole-branch review

**Files:**
- Modify: `.superpowers/sdd/progress.md` (append summary line)

**Interfaces:**
- Consumes: full set of feature commits
- Produces: branch summary line in progress ledger

- [ ] **Step 1: Generate review package**

```bash
bash /Users/panyong/.claude/plugins/cache/claude-plugins-official/superpowers/6.0.3/skills/subagent-driven-development/scripts/review-package <BASE_COMMIT_FOR_FEATURE> HEAD
```

(`<BASE_COMMIT_FOR_FEATURE>` is the commit before Task 1 — i.e. `git log --oneline` and pick the latest commit not part of this feature. Usually `HEAD~7` since this plan has 5 implementation commits + 1 merge-base; pick by inspecting the log.)

- [ ] **Step 2: Dispatch final reviewer**

Use `Agent` tool with `subagent_type: general-purpose`, model `opus`. Hand it:
- Design spec path
- Plan path
- Review-package file path
- Progress ledger path

Prompt template:

> You are performing the final whole-branch review for the word count modal feature. Read `/Users/panyong/aio_project/ai_chuangzuo/docs/superpowers/specs/2026-06-26-word-count-modal-design.md` for requirements and `/Users/panyong/aio_project/ai_chuangzuo/docs/superpowers/plans/2026-06-26-word-count-modal-plan.md` for what was implemented. Read the review-package diff file once. Verify spec coverage, code quality, no regressions to the template-library feature. Report Critical/Important/Minor findings with file:line, plus an Approved/Needs-fixes verdict.

- [ ] **Step 3: Apply fixes if any**

If the reviewer returns Critical or Important findings, dispatch a single fix subagent (`Agent` with `subagent_type: general-purpose`, model `sonnet`) carrying the complete findings list. Re-run Playwright after fixes.

- [ ] **Step 4: Append summary line**

Append to `.superpowers/sdd/progress.md`:
```
Word count modal: complete (commits <base7>..<head7>, review clean)
```

(`<base7>` and `<head7>` are the 7-char SHAs from `git log`.)

---

## Self-Review

**1. Spec coverage:**
- §1 功能概述 → Task 1+2+5
- §2 弹窗结构 → Task 2
- §3 维度 1 数据 → Task 1
- §4 维度 2 数据 → Task 1
- §5 维度 3 数据 → Task 1
- §6 维度 4 行为 → Task 3 (custom tab implementation)
- §7 选项卡片样式 → Task 3 (makeCard function)
- §8 创作页集成 → Task 5
- §9 默认值与首次打开 → Task 2 (initialTab logic)
- §10 弹窗交互细节 → Task 2 (close/cancel) + Task 4 (confirm)
- §11 数据结构 → Task 1
- §12 技术要点 → covered across all tasks
- §13 涉及原型页面 → Task 5

**2. Placeholder scan:** No TBD/TODO. All counts explicit.

**3. Type consistency:** `currentWordCount` (number), `currentWordLabel` (string), `state.selectedCount/Label/customValue` consistent across Tasks 1-4. Trigger IDs `pc-word-count-trigger` and `mobile-word-count-trigger` consistent between Task 4 (confirm) and Task 5 (button element).