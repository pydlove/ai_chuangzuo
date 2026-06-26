# 发布平台选择 + SEO 描述/标签 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在创作页增加发布平台选择器，联动默认模板和字数；在预览页根据平台自动生成发布描述和推荐标签。

**Architecture:** 纯前端 HTML/CSS/JS 原型实现。`shared.js` 新增平台数据、状态变量、平台选择弹窗、默认值应用函数、描述/标签生成函数。`create.html` 增加平台选择模块并复用现有 chip 样式。`preview.html` 在文章正文下方增加「发布描述 & 推荐标签」卡片。Playwright 脚本做端到端验证。

**Tech Stack:** Vanilla JS，无构建工具，Playwright 验证。

## Global Constraints

- 纯前端 HTML/CSS/JS 原型，无后端。
- 平台 key 必须与现有模板/字数体系保持一致：`wechat`、`xiaohongshu`、`toutiao`、`baijiahao`、`zhihu`、`douyin`、`general`。
- 交互风格必须与现有「字数设置 / 风格库 / 模板库」一致（label + `+` 触发按钮 + 当前状态 chip）。
- 平台选择只联动默认值，不强制覆盖用户已手动修改的字数/模板。
- 描述和标签使用静态 mock 数据，模拟 AI 生成效果。
- 描述长度、标签数量、标签格式按 spec §8 的平台规则输出。
- PC 和 Mobile 两个视图必须同步更新。

---

### Task 1: Add platform data, state, and `applyPlatformDefaults` to shared.js

**Files:**
- Modify: `.superpowers/brainstorm/6491-1782131242/content/shared.js` (after `templatePresets`, before `wordCountPresets`)

**Interfaces:**
- Consumes: `templatePresets`, `wordCountPresets` (existing)
- Produces: `publishPlatforms`, `platformDefaults`, `publishDescTemplates`, `publishTagPresets`, `currentPublishPlatform`, `applyPlatformDefaults(platformKey)`

- [ ] **Step 1: Locate insertion point**

Find the end of `var templatePresets = [...];` in `shared.js` (around line 2070, just before `// 字数设置弹窗的数据与全局状态`). Insert the new data block immediately after the closing `];`.

- [ ] **Step 2: Insert platform data and state**

Insert:

```javascript
  // 发布平台选择数据与状态
  var publishPlatforms = [
    { key: 'wechat', name: '公众号', desc: '适合深度长文和订阅号推送' },
    { key: 'xiaohongshu', name: '小红书', desc: '种草图文，重标签和封面' },
    { key: 'toutiao', name: '今日头条', desc: '资讯和观点长文' },
    { key: 'baijiahao', name: '百家号', desc: '百度生态搜索流量' },
    { key: 'zhihu', name: '知乎', desc: '问答和专业分析' },
    { key: 'douyin', name: '抖音图文', desc: '短视频配图和短文案' },
    { key: 'general', name: '通用', desc: '不指定平台，通用输出' }
  ];

  var platformDefaults = {
    wechat: { template: 'wechat', wordCount: 1500, wordLabel: '标准深度文' },
    xiaohongshu: { template: 'xiaohongshu', wordCount: 500, wordLabel: '图文分享' },
    toutiao: { template: 'toutiao', wordCount: 1500, wordLabel: '专题分析' },
    baijiahao: { template: 'baijiahao', wordCount: 1500, wordLabel: '生活攻略' },
    zhihu: { template: 'zhihu-answer', wordCount: 1500, wordLabel: '专业回答' },
    douyin: { template: 'douyin-graphic', wordCount: 300, wordLabel: '图配文' },
    general: { template: 'business', wordCount: 1500, wordLabel: '标准' }
  };

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

  var publishTagPresets = {
    wechat: ['时间管理', '职场效率', '自我提升', '自律', '成长'],
    xiaohongshu: ['#时间管理', '#自律打卡', '#职场干货', '#效率神器', '#自我提升', '#生活方式', '#打工人', '#成长笔记'],
    toutiao: ['#时间管理', '#职场', '#效率提升', '#自我成长', '#干货分享'],
    baijiahao: ['时间管理', '职场效率', '自我提升', '知识科普', '生活技巧'],
    zhihu: ['时间管理', '职场效率', '自我提升', '个人成长', '自律'],
    douyin: ['#时间管理', '#自律', '#职场干货', '#效率提升', '#自我提升', '#成长'],
    general: ['#时间管理', '#自我提升', '#职场效率', '#干货分享', '#自律', '#成长']
  };

  var currentPublishPlatform = 'wechat';
```

- [ ] **Step 3: Add `applyPlatformDefaults`, load/save helpers**

Find a spot near the platform data block (e.g., right after the data) and insert:

```javascript
  function loadPublishPlatform() {
    try {
      var saved = localStorage.getItem('aichuangzuo_publish_platform');
      if (saved && platformDefaults[saved]) currentPublishPlatform = saved;
    } catch (e) {}
  }

  function savePublishPlatform(platformKey) {
    try {
      localStorage.setItem('aichuangzuo_publish_platform', platformKey);
    } catch (e) {}
  }

  function applyPlatformDefaults(platformKey) {
    var cfg = platformDefaults[platformKey];
    if (!cfg) return;

    currentPublishPlatform = platformKey;
    savePublishPlatform(platformKey);

    // Update platform chip on create page
    ['pc', 'mobile'].forEach(function(prefix) {
      var chip = document.getElementById(prefix + '-current-platform');
      var nameEl = document.getElementById(prefix + '-current-platform-name');
      var plat = publishPlatforms.find(function(p) { return p.key === platformKey; });
      if (chip && plat) chip.setAttribute('data-platform', platformKey);
      if (nameEl && plat) nameEl.textContent = plat.name;
    });

    // Update template chip via existing feedback helper if template exists
    var tpl = templatePresets.find(function(t) { return t.key === cfg.template; });
    if (tpl) applyTemplateFeedback(tpl);

    // Update word count globals and chip
    currentWordCount = cfg.wordCount;
    currentWordLabel = cfg.wordLabel;
    ['pc-current-word-count-label', 'mobile-current-word-count-label'].forEach(function(id) {
      var el = document.getElementById(id);
      if (el) el.textContent = cfg.wordCount + ' 字 · ' + cfg.wordLabel;
    });
  }

  loadPublishPlatform();
  applyPlatformDefaults(currentPublishPlatform);
```

- [ ] **Step 4: Verify data insertion**

Run:

```bash
python3 - <<'PY'
with open('.superpowers/brainstorm/6491-1782131242/content/shared.js') as f:
    txt = f.read()

assert 'var publishPlatforms = [' in txt, 'publishPlatforms missing'
assert 'var platformDefaults = {' in txt, 'platformDefaults missing'
assert 'var publishDescTemplates = {' in txt, 'publishDescTemplates missing'
assert 'var publishTagPresets = {' in txt, 'publishTagPresets missing'
assert "var currentPublishPlatform = 'wechat';" in txt, 'currentPublishPlatform missing'
assert 'function applyPlatformDefaults(platformKey)' in txt, 'applyPlatformDefaults missing'
assert 'function loadPublishPlatform()' in txt, 'loadPublishPlatform missing'
assert 'function savePublishPlatform(platformKey)' in txt, 'savePublishPlatform missing'
assert 'loadPublishPlatform();' in txt, 'loadPublishPlatform call missing'
assert 'applyPlatformDefaults(currentPublishPlatform);' in txt, 'applyPlatformDefaults init call missing'
assert "localStorage.setItem('aichuangzuo_publish_platform'" in txt, 'localStorage save missing'

for p in ['wechat','xiaohongshu','toutiao','baijiahao','zhihu','douyin','general']:
    assert f"key: '{p}'" in txt or f"'{p}'" in txt, f'platform {p} missing'

print('Task 1 checks passed')
PY
```

Expected: `Task 1 checks passed`

- [ ] **Step 5: Commit**

```bash
git add .superpowers/brainstorm/6491-1782131242/content/shared.js
git commit -m "feat(platform): add publish platform data, state, defaults and persistence"
```

---

### Task 2: Add `openPlatformLibrary()` modal to shared.js

**Files:**
- Modify: `.superpowers/brainstorm/6491-1782131242/content/shared.js` (after `openWordCountModal()`)

**Interfaces:**
- Consumes: `publishPlatforms`, `currentPublishPlatform`, `applyPlatformDefaults` (Task 1)
- Produces: `openPlatformLibrary()`

- [ ] **Step 1: Locate insertion point**

Find the closing of `function openWordCountModal()` in `shared.js` (around line 2630, after its inner helper functions and closing brace). Insert the new function immediately after it.

- [ ] **Step 2: Insert `openPlatformLibrary()`**

Insert:

```javascript
  function openPlatformLibrary() {
    var existing = document.getElementById('platform-library-modal');
    if (existing) existing.remove();

    var overlay = document.createElement('div');
    overlay.id = 'platform-library-modal';
    overlay.style.cssText = 'position: fixed; inset: 0; background: rgba(0,0,0,0.5); z-index: 10003; display: flex; align-items: center; justify-content: center; padding: 20px; font-family: -apple-system, BlinkMacSystemFont, sans-serif;';

    var box = document.createElement('div');
    box.style.cssText = 'background: #fff; border-radius: 16px; width: 560px; max-width: 100%; max-height: 90vh; display: flex; flex-direction: column; box-shadow: 0 8px 32px rgba(0,0,0,0.2); position: relative; overflow: hidden;';

    var closeBtn = document.createElement('button');
    closeBtn.innerHTML = '&times;';
    closeBtn.style.cssText = 'position: absolute; top: 10px; right: 14px; background: none; border: none; font-size: 22px; cursor: pointer; color: #8c8c8c; line-height: 1; padding: 4px 8px; z-index: 2;';
    closeBtn.onclick = function() { overlay.remove(); };
    box.appendChild(closeBtn);

    var headerWrap = document.createElement('div');
    headerWrap.style.cssText = 'padding: 22px 24px 12px; flex-shrink: 0;';
    var header = document.createElement('div');
    header.style.cssText = 'font-size: 18px; font-weight: 700; color: #1a1a1a; margin-bottom: 4px; padding-right: 28px;';
    header.textContent = '选择发布平台';
    var sub = document.createElement('div');
    sub.style.cssText = 'font-size: 13px; color: #8c8c8c;';
    sub.textContent = '选择目标平台，AI 将按平台规则推荐模板、字数和标签';
    headerWrap.appendChild(header);
    headerWrap.appendChild(sub);
    box.appendChild(headerWrap);

    var content = document.createElement('div');
    content.style.cssText = 'padding: 8px 24px 16px; flex: 1; min-height: 0; overflow-y: auto; display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px;';

    var selectedKey = currentPublishPlatform;

    publishPlatforms.forEach(function(p) {
      var card = document.createElement('div');
      var selected = p.key === selectedKey;
      card.style.cssText = 'border: 2px solid ' + (selected ? '#07c160' : '#e8e8e8') +
        '; border-radius: 10px; padding: 14px; background: ' + (selected ? '#f6ffed' : '#fff') +
        '; cursor: pointer; display: flex; flex-direction: column; gap: 4px;';
      var name = document.createElement('div');
      name.style.cssText = 'font-weight: 600; color: #1a1a1a; font-size: 15px;';
      name.textContent = p.name;
      card.appendChild(name);
      var desc = document.createElement('div');
      desc.style.cssText = 'color: #8c8c8c; font-size: 12px; line-height: 1.5;';
      desc.textContent = p.desc;
      card.appendChild(desc);
      card.onclick = function() {
        selectedKey = p.key;
        Array.from(content.children).forEach(function(c, idx) {
          var plat = publishPlatforms[idx];
          var isSel = plat.key === selectedKey;
          c.style.borderColor = isSel ? '#07c160' : '#e8e8e8';
          c.style.background = isSel ? '#f6ffed' : '#fff';
        });
      };
      content.appendChild(card);
    });

    box.appendChild(content);

    var footer = document.createElement('div');
    footer.style.cssText = 'padding: 12px 24px 16px; flex-shrink: 0; border-top: 1px solid #f0f0f0; display: flex; justify-content: flex-end; gap: 8px;';
    var cancelBtn = document.createElement('button');
    cancelBtn.textContent = '取消';
    cancelBtn.style.cssText = 'padding: 8px 18px; border-radius: 8px; border: 1px solid #d9d9d9; background: #fff; color: #595959; cursor: pointer; font-size: 14px;';
    cancelBtn.onclick = function() { overlay.remove(); };
    var confirmBtn = document.createElement('button');
    confirmBtn.textContent = '确认';
    confirmBtn.style.cssText = 'padding: 8px 18px; border-radius: 8px; border: 1px solid #07c160; background: #07c160; color: #fff; cursor: pointer; font-size: 14px; font-weight: 600;';
    confirmBtn.onclick = function() {
      applyPlatformDefaults(selectedKey);
      overlay.remove();
    };
    footer.appendChild(cancelBtn);
    footer.appendChild(confirmBtn);
    box.appendChild(footer);

    overlay.appendChild(box);
    overlay.onclick = function(e) {
      if (e.target === overlay) overlay.remove();
    };
    document.body.appendChild(overlay);
  }
```

- [ ] **Step 3: Verify modal function exists**

Run:

```bash
python3 - <<'PY'
with open('.superpowers/brainstorm/6491-1782131242/content/shared.js') as f:
    txt = f.read()

assert 'function openPlatformLibrary()' in txt, 'openPlatformLibrary missing'
assert "overlay.id = 'platform-library-modal'" in txt, 'modal id missing'
assert '选择发布平台' in txt, 'modal title missing'
assert 'applyPlatformDefaults(selectedKey)' in txt, 'confirm wiring missing'
print('Task 2 checks passed')
PY
```

Expected: `Task 2 checks passed`

- [ ] **Step 4: Commit**

```bash
git add .superpowers/brainstorm/6491-1782131242/content/shared.js
git commit -m "feat(platform): add openPlatformLibrary modal"
```

---

### Task 3: Add platform selector to create.html

**Files:**
- Modify: `.superpowers/brainstorm/6491-1782131242/content/create.html`

**Interfaces:**
- Consumes: `openPlatformLibrary()`, `applyPlatformDefaults()` (Task 1, Task 2)
- Produces: `#pc-current-platform`, `#pc-current-platform-name`, `#mobile-current-platform`, `#mobile-current-platform-name` markup

- [ ] **Step 1: Insert PC platform selector**

Locate the closing `</div>` of `id="pc-mode-custom"` (around line 126) and the following `<div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 20px;">` (line 127). Insert the platform selector between them.

Old boundary:

```html
              </div>
            </div>
          </div>
          <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 20px;">
```

Insert this block after the third `</div>` (the one closing `pc-mode-custom`) and before the grid:

```html
          <!-- 发布平台选择 -->
          <div style="margin-bottom: 20px;">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px;">
              <label style="font-weight: 500; color: #262626;">发布平台</label>
              <button onclick="openPlatformLibrary()" style="background: none; border: none; color: #07c160; font-size: 13px; cursor: pointer; font-weight: 500; padding: 2px 6px; border-radius: 4px;" onmouseover="this.style.background='#f6ffed'" onmouseout="this.style.background='none'">+ 平台选择</button>
            </div>
            <div id="pc-current-platform" class="current-style-chip" data-platform="wechat" style="display: flex; align-items: center; gap: 8px; padding: 8px 12px; background: linear-gradient(135deg, #f6ffed 0%, #e6f7ff 100%); border: 1px solid #b7eb8f; border-radius: 8px; font-size: 13px;">
              <span style="color: #595959;">当前发布平台：</span>
              <span id="pc-current-platform-name" style="color: #07c160; font-weight: 600;">公众号</span>
            </div>
          </div>
```

- [ ] **Step 2: Insert mobile platform selector**

Locate the closing `</div>` of `id="mobile-mode-custom"` (around line 265) and the following `<div style="margin-bottom: 16px;">` (the word count section). Insert the platform selector between them.

Old boundary:

```html
              </div>
            </div>
          </div>
          <div style="margin-bottom: 16px;">
```

Insert this block:

```html
          <!-- 发布平台选择 -->
          <div style="margin-bottom: 16px;">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
              <label style="font-weight: 500; color: #262626; font-size: 14px;">发布平台</label>
              <button onclick="openPlatformLibrary()" style="background: none; border: none; color: #07c160; font-size: 12px; cursor: pointer; font-weight: 500; padding: 2px 6px; border-radius: 4px;" onmouseover="this.style.background='#f6ffed'" onmouseout="this.style.background='none'">+ 平台选择</button>
            </div>
            <div id="mobile-current-platform" class="current-style-chip" data-platform="wechat" style="display: flex; align-items: center; gap: 6px; padding: 6px 10px; background: linear-gradient(135deg, #f6ffed 0%, #e6f7ff 100%); border: 1px solid #b7eb8f; border-radius: 6px; font-size: 12px;">
              <span style="color: #595959;">当前：</span>
              <span id="mobile-current-platform-name" style="color: #07c160; font-weight: 600;">公众号</span>
            </div>
          </div>
```

- [ ] **Step 3: Verify create.html changes**

Run:

```bash
python3 - <<'PY'
with open('.superpowers/brainstorm/6491-1782131242/content/create.html') as f:
    txt = f.read()

assert 'onclick="openPlatformLibrary()"' in txt, 'platform trigger missing'
assert 'id="pc-current-platform"' in txt, 'pc platform chip missing'
assert 'id="pc-current-platform-name"' in txt, 'pc platform name missing'
assert 'id="mobile-current-platform"' in txt, 'mobile platform chip missing'
assert 'id="mobile-current-platform-name"' in txt, 'mobile platform name missing'
assert 'data-platform="wechat"' in txt, 'default platform missing'
print('Task 3 checks passed')
PY
```

Expected: `Task 3 checks passed`

- [ ] **Step 4: Manual smoke test**

Start the server:

```bash
./scripts/local/start.sh
```

Open `http://localhost:28585/.superpowers/brainstorm/6491-1782131242/content/create.html`.

Confirm:
1. 创作页显示「发布平台」模块，默认「公众号」。
2. 点击「+ 平台选择」弹出平台选择弹窗。
3. 选择「小红书」并确认，当前平台 chip 变为「小红书」。
4. 导出模板 chip 变为「小红书图文模板」，字数 chip 变为「500 字 · 图文分享」。

- [ ] **Step 5: Commit**

```bash
git add .superpowers/brainstorm/6491-1782131242/content/create.html
git commit -m "feat(platform): add publish platform selector to create page"
```

---

### Task 4: Add publish meta functions to shared.js

**Files:**
- Modify: `.superpowers/brainstorm/6491-1782131242/content/shared.js` (near export/preview helpers)

**Interfaces:**
- Consumes: `publishDescTemplates`, `publishTagPresets`, `currentPublishPlatform` (Task 1)
- Produces: `renderPublishMeta()`, `regeneratePublishDesc()`, `regeneratePublishTags()`, `copyPublishDesc()`, `copyPublishTags()`, `copySingleTag(el)`

- [ ] **Step 1: Locate insertion point**

Find `function exportWord(btn)` (around line 625). Insert the new publish meta helpers immediately before it.

- [ ] **Step 2: Insert helper functions**

Insert:

```javascript
  // ===== 发布描述 & 标签 =====
  function getPublishMetaForArticle() {
    var titleEl = document.querySelector('.preview-title');
    var title = titleEl ? titleEl.textContent.trim() : '如何高效管理时间';
    return { title: title, count: '5' };
  }

  function formatDesc(template, meta) {
    return template.replace(/\{title\}/g, meta.title).replace(/\{count\}/g, meta.count);
  }

  function getTagsForPlatform(platform, count) {
    var tags = publishTagPresets[platform] || publishTagPresets.general;
    var shuffled = tags.slice().sort(function() { return Math.random() - 0.5; });
    return shuffled.slice(0, Math.min(count, shuffled.length));
  }

  function renderPublishMeta() {
    var platform = currentPublishPlatform;
    var meta = getPublishMetaForArticle();
    var templates = publishDescTemplates[platform] || publishDescTemplates.general;
    var desc = formatDesc(templates[Math.floor(Math.random() * templates.length)], meta);

    var descCounts = { wechat: [5], xiaohongshu: [8, 10], toutiao: [5, 6], baijiahao: [4, 5], zhihu: [3, 4], douyin: [5, 6], general: [6, 7] };
    var tagCount = descCounts[platform] ? descCounts[platform][Math.floor(Math.random() * descCounts[platform].length)] : 6;
    var tags = getTagsForPlatform(platform, tagCount);

    ['pc', 'mobile'].forEach(function(prefix) {
      var descEl = document.getElementById(prefix + '-publish-desc');
      if (descEl) descEl.value = desc;

      var tagsEl = document.getElementById(prefix + '-publish-tags');
      if (tagsEl) {
        tagsEl.innerHTML = tags.map(function(t) {
          return '<span class="publish-tag" onclick="copySingleTag(this)" style="padding: 4px 10px; background: #f6ffed; color: #07c160; border-radius: 12px; font-size: 13px; cursor: pointer; border: 1px solid #b7eb8f;" onmouseover="this.style.background=\'#e6f7ff\'" onmouseout="this.style.background=\'#f6ffed\'"'>' + t + '</span>';
        }).join('');
      }
    });
  }

  function regeneratePublishDesc() {
    renderPublishMeta();
  }

  function regeneratePublishTags() {
    renderPublishMeta();
  }

  function copyPublishDesc() {
    var el = document.getElementById('pc-publish-desc') || document.getElementById('mobile-publish-desc');
    if (!el) return;
    el.select();
    document.execCommand('copy');
    window.getSelection().removeAllRanges();
    showToast('描述已复制');
  }

  function copyPublishTags() {
    var platform = currentPublishPlatform;
    var tagsEl = document.getElementById('pc-publish-tags') || document.getElementById('mobile-publish-tags');
    if (!tagsEl) return;
    var tags = Array.from(tagsEl.querySelectorAll('.publish-tag')).map(function(s) { return s.textContent; });
    var joined;
    if (platform === 'wechat' || platform === 'zhihu') {
      joined = tags.join('，');
    } else {
      joined = tags.join(' ');
    }
    copyToClipboard(joined);
    showToast('标签已复制');
  }

  function copySingleTag(el) {
    copyToClipboard(el.textContent);
    showToast('标签已复制');
  }

  function copyToClipboard(text) {
    if (navigator.clipboard) {
      navigator.clipboard.writeText(text);
    } else {
      var ta = document.createElement('textarea');
      ta.value = text;
      ta.style.position = 'fixed';
      ta.style.opacity = '0';
      document.body.appendChild(ta);
      ta.select();
      document.execCommand('copy');
      document.body.removeChild(ta);
    }
  }

  function showToast(message) {
    var existing = document.querySelector('.aichuangzuo-toast');
    if (existing) existing.remove();
    var toast = document.createElement('div');
    toast.className = 'aichuangzuo-toast';
    toast.textContent = message;
    toast.style.cssText = 'position: fixed; top: 50%; left: 50%; transform: translate(-50%, -50%); background: rgba(0,0,0,0.75); color: #fff; padding: 10px 18px; border-radius: 8px; font-size: 14px; z-index: 10010; pointer-events: none;';
    document.body.appendChild(toast);
    setTimeout(function() {
      toast.style.opacity = '0';
      toast.style.transition = 'opacity 0.3s';
      setTimeout(function() { toast.remove(); }, 300);
    }, 1500);
  }
```

- [ ] **Step 3: Verify functions exist**

Run:

```bash
python3 - <<'PY'
with open('.superpowers/brainstorm/6491-1782131242/content/shared.js') as f:
    txt = f.read()

for fn in ['renderPublishMeta', 'regeneratePublishDesc', 'regeneratePublishTags', 'copyPublishDesc', 'copyPublishTags', 'copySingleTag', 'copyToClipboard', 'showToast']:
    assert ('function ' + fn + '(') in txt, f'{fn} missing'
print('Task 4 checks passed')
PY
```

Expected: `Task 4 checks passed`

- [ ] **Step 4: Commit**

```bash
git add .superpowers/brainstorm/6491-1782131242/content/shared.js
git commit -m "feat(platform): add publish meta generation and copy helpers"
```

---

### Task 5: Add publish meta card to preview.html

**Files:**
- Modify: `.superpowers/brainstorm/6491-1782131242/content/preview.html`

**Interfaces:**
- Consumes: `renderPublishMeta()`, `regeneratePublishDesc()`, `regeneratePublishTags()`, `copyPublishDesc()`, `copyPublishTags()`, `copySingleTag()` (Task 4)
- Produces: `#pc-publish-meta-card`, `#pc-publish-desc`, `#pc-publish-tags`, `#mobile-publish-meta-card`, `#mobile-publish-desc`, `#mobile-publish-tags` markup

- [ ] **Step 1: Insert PC publish meta card**

Locate the closing `</div>` of the PC article preview (the white card containing `.article-preview`, around line 66-67). Insert the publish meta card immediately after the `</div>` that closes `.article-preview` container but before the outer `</div>` that closes the white card.

The exact insertion point is after:

```html
                  </div>
                </div>
              </div>
            </div>
          </div>
```

But before the next `</div>` that closes `pc-mobile-wrap`'s first child. To be precise: after the `</div>` that closes the inner `div style="max-width: 680px; margin: 0 auto;"`.

Look for this pattern and insert after it:

```html
              </div>
            </div>
          </div>
        </div>
```

Actually, inspect the file visually. The PC mockup body contains:

```html
<div style="display: flex; max-width: 1200px; margin: 24px auto; gap: 24px; padding: 0 24px;">
  <div style="flex: 1; background: #fff; border-radius: 12px; padding: 40px; box-shadow: 0 2px 12px rgba(0,0,0,0.05); min-height: 600px;">
    <div style="max-width: 680px; margin: 0 auto;">
      ... title, meta, article-preview ...
    </div>
  </div>
</div>
```

Insert the publish meta card inside the white card `div` but after the `max-width: 680px` inner div. So insert before the second `</div>` (closing white card).

Insert:

```html
        <!-- 发布描述 & 推荐标签 -->
        <div id="pc-publish-meta-card" style="max-width: 680px; margin: 32px auto 0; padding-top: 28px; border-top: 1px solid #f0f0f0;">
          <div style="font-weight: 600; color: #1a1a1a; margin-bottom: 12px; font-size: 15px;">发布描述</div>
          <textarea id="pc-publish-desc" style="width: 100%; min-height: 90px; border: 1px solid #d9d9d9; border-radius: 8px; padding: 12px; font-size: 14px; line-height: 1.6; color: #262626; resize: vertical; box-sizing: border-box;"></textarea>
          <div style="display: flex; justify-content: flex-end; gap: 8px; margin-top: 10px;">
            <button onclick="regeneratePublishDesc()" style="padding: 6px 14px; background: #fff; border: 1px solid #d9d9d9; border-radius: 6px; font-size: 13px; color: #595959; cursor: pointer;">换一版</button>
            <button onclick="copyPublishDesc()" style="padding: 6px 14px; background: #fff; border: 1px solid #07c160; border-radius: 6px; font-size: 13px; color: #07c160; cursor: pointer;">复制描述</button>
          </div>

          <div style="font-weight: 600; color: #1a1a1a; margin: 24px 0 12px; font-size: 15px;">推荐标签</div>
          <div id="pc-publish-tags" style="display: flex; flex-wrap: wrap; gap: 8px;"></div>
          <div style="display: flex; justify-content: flex-end; gap: 8px; margin-top: 12px;">
            <button onclick="regeneratePublishTags()" style="padding: 6px 14px; background: #fff; border: 1px solid #d9d9d9; border-radius: 6px; font-size: 13px; color: #595959; cursor: pointer;">换一批</button>
            <button onclick="copyPublishTags()" style="padding: 6px 14px; background: #fff; border: 1px solid #07c160; border-radius: 6px; font-size: 13px; color: #07c160; cursor: pointer;">复制全部标签</button>
          </div>
        </div>
```

- [ ] **Step 2: Insert mobile publish meta card**

Locate the closing of the mobile article preview card (after `.article-preview` div, around line 100-101). Insert the mobile publish meta card inside the white card container after the article content.

The mobile structure is:

```html
<div style="background: #fff; padding: 24px 16px; margin-bottom: 12px;">
  ... title, meta, article-preview ...
</div>
```

Insert before the closing `</div>` of the white card:

```html
        <!-- 发布描述 & 推荐标签 -->
        <div id="mobile-publish-meta-card" style="margin-top: 16px; padding-top: 20px; border-top: 1px solid #f0f0f0;">
          <div style="font-weight: 600; color: #1a1a1a; margin-bottom: 10px; font-size: 14px;">发布描述</div>
          <textarea id="mobile-publish-desc" style="width: 100%; min-height: 70px; border: 1px solid #d9d9d9; border-radius: 6px; padding: 10px; font-size: 13px; line-height: 1.5; color: #262626; resize: vertical; box-sizing: border-box;"></textarea>
          <div style="display: flex; justify-content: flex-end; gap: 6px; margin-top: 8px;">
            <button onclick="regeneratePublishDesc()" style="padding: 5px 10px; background: #fff; border: 1px solid #d9d9d9; border-radius: 4px; font-size: 12px; color: #595959; cursor: pointer;">换一版</button>
            <button onclick="copyPublishDesc()" style="padding: 5px 10px; background: #fff; border: 1px solid #07c160; border-radius: 4px; font-size: 12px; color: #07c160; cursor: pointer;">复制描述</button>
          </div>

          <div style="font-weight: 600; color: #1a1a1a; margin: 16px 0 10px; font-size: 14px;">推荐标签</div>
          <div id="mobile-publish-tags" style="display: flex; flex-wrap: wrap; gap: 6px;"></div>
          <div style="display: flex; justify-content: flex-end; gap: 6px; margin-top: 10px;">
            <button onclick="regeneratePublishTags()" style="padding: 5px 10px; background: #fff; border: 1px solid #d9d9d9; border-radius: 4px; font-size: 12px; color: #595959; cursor: pointer;">换一批</button>
            <button onclick="copyPublishTags()" style="padding: 5px 10px; background: #fff; border: 1px solid #07c160; border-radius: 4px; font-size: 12px; color: #07c160; cursor: pointer;">复制全部标签</button>
          </div>
        </div>
```

- [ ] **Step 3: Trigger render on page load**

In `preview.html`, find the existing inline script at the bottom:

```html
<script>
  document.addEventListener('DOMContentLoaded', function() {
    var floatBar = document.getElementById('floating-action-bar');
    if (floatBar) floatBar.style.display = 'flex';
  });
</script>
```

Replace it with:

```html
<script>
  document.addEventListener('DOMContentLoaded', function() {
    var floatBar = document.getElementById('floating-action-bar');
    if (floatBar) floatBar.style.display = 'flex';
    if (typeof renderPublishMeta === 'function') renderPublishMeta();
  });
</script>
```

- [ ] **Step 4: Verify preview.html changes**

Run:

```bash
python3 - <<'PY'
with open('.superpowers/brainstorm/6491-1782131242/content/preview.html') as f:
    txt = f.read()

assert 'id="pc-publish-meta-card"' in txt, 'pc meta card missing'
assert 'id="pc-publish-desc"' in txt, 'pc desc missing'
assert 'id="pc-publish-tags"' in txt, 'pc tags missing'
assert 'id="mobile-publish-meta-card"' in txt, 'mobile meta card missing'
assert 'id="mobile-publish-desc"' in txt, 'mobile desc missing'
assert 'id="mobile-publish-tags"' in txt, 'mobile tags missing'
assert 'renderPublishMeta()' in txt, 'renderPublishMeta trigger missing'
print('Task 5 checks passed')
PY
```

Expected: `Task 5 checks passed`

- [ ] **Step 5: Manual smoke test**

With the server running, open `http://localhost:28585/.superpowers/brainstorm/6491-1782131242/content/preview.html`.

Confirm:
1. 预览页文章正文下方出现「发布描述」和「推荐标签」区域。
2. 默认平台是公众号，描述为正式摘要风格，标签为纯关键词不带 `#`。
3. 点击「换一版」/「换一批」能刷新内容。
4. 点击标签或「复制全部标签」有 toast 提示。

- [ ] **Step 6: Commit**

```bash
git add .superpowers/brainstorm/6491-1782131242/content/preview.html
git commit -m "feat(platform): add publish meta card to preview page"
```

---

### Task 6: End-to-end Playwright verification

**Files:**
- Create: `/tmp/verify_publish_platform.py` (not committed)

**Interfaces:**
- Consumes: complete create.html, preview.html, shared.js
- Produces: screenshots + console assertion output

- [ ] **Step 1: Write verification script**

Create `/tmp/verify_publish_platform.py`:

```python
from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page(viewport={'width': 1400, 'height': 900})

    # 1. Open create page
    page.goto('http://localhost:28585/.superpowers/brainstorm/6491-1782131242/content/create.html')
    page.wait_for_timeout(300)

    # 2. Platform selector exists
    assert page.locator('#pc-current-platform').count() == 1, 'PC platform chip missing'
    assert page.locator('#pc-current-platform-name').inner_text() == '公众号', 'Default platform wrong'

    # 3. Open platform library and select xiaohongshu
    page.click('button:has-text("+ 平台选择")')
    page.wait_for_selector('#platform-library-modal')
    page.locator('#platform-library-modal div').filter(has_text='小红书').first.click()
    page.locator('#platform-library-modal button:has-text("确认")').click()
    page.wait_for_timeout(200)

    assert page.locator('#pc-current-platform-name').inner_text() == '小红书', 'Platform not updated'
    assert '小红书图文模板' in page.locator('#pc-current-template-name').inner_text(), 'Template not updated'
    assert '500 字' in page.locator('#pc-current-word-count-label').inner_text(), 'Word count not updated'

    page.screenshot(path='/tmp/platform_create_xiaohongshu.png')

    # 4. Go to preview page
    page.goto('http://localhost:28585/.superpowers/brainstorm/6491-1782131242/content/preview.html')
    page.wait_for_timeout(300)

    # 5. Publish meta card exists and shows xiaohongshu style tags
    assert page.locator('#pc-publish-meta-card').count() == 1, 'PC meta card missing'
    tags_text = page.locator('#pc-publish-tags').inner_text()
    assert '#时间管理' in tags_text, 'Xiaohongshu style tags missing'

    page.screenshot(path='/tmp/platform_preview_xiaohongshu.png')

    # 6. Copy tags button works (toast appears)
    page.click('button:has-text("复制全部标签")')
    page.wait_for_timeout(300)
    assert page.locator('.aichuangzuo-toast').count() == 1, 'Copy toast missing'

    browser.close()
    print('Verification passed')
```

- [ ] **Step 2: Run verification**

Start the server if not already running:

```bash
./scripts/local/start.sh
```

Then run:

```bash
python3 /tmp/verify_publish_platform.py
```

Expected output:

```
Verification passed
```

- [ ] **Step 3: Visual review of screenshots**

Read and inspect:
- `/tmp/platform_create_xiaohongshu.png` — platform chip shows 小红书，template and word count updated
- `/tmp/platform_preview_xiaohongshu.png` — publish meta card shows xiaohongshu-style description and `#tags`

- [ ] **Step 4: No commit**

Verification artifacts are not committed.

---

## Self-Review

**1. Spec coverage:**
- §4 创作页平台选择 → Task 3
- §5 平台选择弹窗 → Task 2
- §6 平台联动规则 → Task 1 (`applyPlatformDefaults`)
- §7 预览页描述 & 标签 → Task 4 + Task 5
- §8 按平台输出规则 → Task 4 (`renderPublishMeta`)
- §9 数据结构 → Task 1
- §10 状态管理 → Task 1
- §11 涉及页面 → Task 3, Task 5
- §14 验证方式 → Task 6

**2. Placeholder scan:** No TBD/TODO. All code blocks contain complete code.

**3. Type consistency:** `currentPublishPlatform` string, `platformDefaults` keys consistent, tag/desc generation consumes same platform keys.

**4. File responsibility:**
- `shared.js` — data, state, functions
- `create.html` — platform selector markup
- `preview.html` — publish meta card markup
- `/tmp/verify_publish_platform.py` — verification
