  var isLoggedIn = false;

  function handleGenerate() {
    var data = collectCreatePageData();
    submitGenerationTask(data);
    openGenerationQueue();
  }

  function simulateAuth(type) {
    isLoggedIn = true;
    sessionStorage.setItem('isLoggedIn', 'true');
    location.href='create.html';
  }

  function simulateReset() {
    location.href='login.html';
  }

  function startCountdown(btn, seconds) {
    if (btn.getAttribute('data-counting') === 'true') return;
    var originalText = btn.textContent;
    btn.setAttribute('data-original', originalText);
    btn.setAttribute('data-counting', 'true');
    btn.disabled = true;
    btn.style.opacity = '0.6';
    btn.style.cursor = 'not-allowed';
    var remaining = seconds;
    btn.textContent = remaining + 's';
    var timer = setInterval(function() {
      remaining--;
      if (remaining <= 0) {
        clearInterval(timer);
        btn.textContent = originalText;
        btn.disabled = false;
        btn.style.opacity = '1';
        btn.style.cursor = 'pointer';
        btn.removeAttribute('data-counting');
      } else {
        btn.textContent = remaining + 's';
      }
    }, 1000);
  }

  function switchCreateMode(platform, mode) {
    var prefix = platform + '-mode-';
    document.querySelectorAll('[id^="' + prefix + '"]').forEach(function(el) {
      el.classList.remove('active');
    });
    document.getElementById(prefix + mode).classList.add('active');

    document.querySelectorAll('.' + platform + '-mode-tab').forEach(function(btn) {
      var isActive = btn.getAttribute('onclick').includes("'" + platform + "', '" + mode + "'");
      btn.classList.toggle('active', isActive);
      btn.classList.toggle('inactive', !isActive);
    });
  }

  function selectTopic(card) {
    var container = card.parentElement;
    container.querySelectorAll('.topic-card').forEach(function(c) {
      c.classList.remove('selected');
      c.style.border = '1px solid #e8e8e8';
      c.style.background = '#fff';
    });
    card.classList.add('selected');
    card.style.border = '2px solid #07c160';
    card.style.background = '#f6ffed';
  }

  var topicSets = [
    [
      { title: '工作 3 年没升职？可能是这 3 个习惯在拖后腿', tag: '职场效率', hot: '3.2k' },
      { title: '我用 AI 写作月入过万：新手可复制的 5 个步骤', tag: 'AI 副业', hot: '5.8k' },
      { title: '为什么你越努力越焦虑？3 个思维陷阱正在消耗你', tag: '情感成长', hot: '4.1k' },
      { title: '月薪 5000 如何一年存下 3 万？我的省钱清单公开', tag: '生活技巧', hot: '6.5k' },
      { title: '30 岁后才明白：真正成熟的人，都懂得边界感', tag: '情感成长', hot: '2.9k' },
      { title: 'AI 时代，普通人如何抓住新机会的 4 个思路', tag: 'AI 副业', hot: '7.2k' }
    ],
    [
      { title: '每天早起 1 小时，我是如何彻底改变人生的', tag: '职场效率', hot: '4.5k' },
      { title: '不做自媒体就晚了？2026 年最适合普通人的 3 个赛道', tag: 'AI 副业', hot: '8.1k' },
      { title: '为什么好人总是受伤？建立健康关系的 4 个底线', tag: '情感成长', hot: '3.7k' },
      { title: '租房也要精致：月花 500 打造舒适小窝', tag: '生活技巧', hot: '5.2k' },
      { title: '被领导批评后，高情商的人都在这样做', tag: '职场效率', hot: '6.3k' },
      { title: '从 0 到 1 做小红书：普通人涨粉的 5 个真相', tag: 'AI 副业', hot: '9.0k' }
    ]
  ];
  var currentTopicSet = 0;

  function refreshTopics(platform, btn) {
    var list = document.getElementById(platform + '-topic-list');
    if (!list) return;
    var triggerBtn = btn || list.parentElement.querySelector('button');
    if (triggerBtn) {
      triggerBtn.disabled = true;
      triggerBtn.style.opacity = '0.6';
      triggerBtn.style.cursor = 'not-allowed';
      triggerBtn.innerHTML = '<span style="display: inline-block; width: 12px; height: 12px; border: 2px solid #07c160; border-top-color: transparent; border-radius: 50%; animation: spin 1s linear infinite; vertical-align: middle; margin-right: 6px;"></span>加载中...';
    }

    setTimeout(function() {
      currentTopicSet = (currentTopicSet + 1) % topicSets.length;
      var isMobile = platform === 'mobile';
      list.innerHTML = topicSets[currentTopicSet].map(function(t) {
        return '<div class="topic-card" onclick="selectTopic(this)" style="' + (isMobile ? 'padding: 12px;' : '') + '">' +
          '<div style="font-weight: 600; color: #1a1a1a; margin-bottom: ' + (isMobile ? '6px' : '8px') + '; font-size: ' + (isMobile ? '14px' : '14px') + '; line-height: 1.4;">' + t.title + '</div>' +
          '<div style="display: flex; gap: 6px; flex-wrap: wrap;">' +
            '<span style="font-size: 11px; color: #07c160; background: #f6ffed; padding: 2px 6px; border-radius: 4px;">' + t.tag + '</span>' +
            '<span style="font-size: 11px; color: #8c8c8c;">' + t.hot + ' 热度</span>' +
          '</div>' +
        '</div>';
      }).join('');

      if (triggerBtn) {
        var originalText = platform === 'mobile' ? '换一批' : '换一批灵感';
        triggerBtn.textContent = originalText;
        triggerBtn.disabled = false;
        triggerBtn.style.opacity = '1';
        triggerBtn.style.cursor = 'pointer';
      }
    }, 1500);
  }

  function showScreen(name) {
    document.querySelectorAll('.prototype-screen').forEach(s => s.classList.remove('active'));
    var screen = document.getElementById('screen-' + name);
    if (screen) screen.classList.add('active');
    document.querySelectorAll('.prototype-nav button').forEach(b => b.classList.remove('active'));
    var navBtn = document.querySelector('.prototype-nav button[data-screen="' + name + '"]');
    if (navBtn) navBtn.classList.add('active');
    // 预览页显示底部悬浮栏
    var floatBar = document.getElementById('floating-action-bar');
    if (floatBar) floatBar.style.display = (name === 'preview') ? 'flex' : 'none';
  }

  function switchPricing(platform, cycle) {
    var prefix = platform === 'pc' ? 'pricing-set-pc' : 'pricing-set-mobile';
    var togglePrefix = platform === 'pc' ? 'pricing-toggle-pc' : 'pricing-toggle-mobile';
    document.querySelectorAll('.' + prefix).forEach(function(set) {
      set.style.display = set.getAttribute('data-cycle') === cycle ? 'grid' : 'none';
    });
    document.querySelectorAll('.' + togglePrefix).forEach(function(btn) {
      var isActive = btn.getAttribute('data-cycle') === cycle;
      btn.style.background = isActive ? '#fff' : 'transparent';
      btn.style.color = isActive ? '#07c160' : '#595959';
      btn.style.boxShadow = isActive ? '0 1px 3px rgba(0,0,0,0.08)' : 'none';
      btn.style.fontWeight = isActive ? '600' : '500';
    });
  }

  function switchAuth(platform, type) {
    var prefix = platform + '-';
    document.querySelectorAll('[id^="' + prefix + '"].auth-form').forEach(f => f.classList.remove('active'));
    document.getElementById(prefix + type).classList.add('active');

    var container = document.getElementById('screen-login');
    container.querySelectorAll('.auth-tab').forEach(function(btn) {
      if (btn.getAttribute('onclick').includes("'" + platform + "', '" + type + "'")) {
        btn.classList.add('active');
        btn.classList.remove('inactive');
      } else if (btn.getAttribute('onclick').includes("'" + platform + "',")) {
        btn.classList.remove('active');
        btn.classList.add('inactive');
      }
    });
  }

    function getTemplateStyles(isMobile) {
      return {
      wechat: {
        container: { fontSize: isMobile ? '15px' : '16px', lineHeight: isMobile ? '1.75' : '1.8', color: '#262626', fontFamily: 'inherit' },
        title: { fontSize: isMobile ? '20px' : '26px', color: '#1a1a1a', fontWeight: '700' },
        heading: { fontSize: isMobile ? '16px' : '18px', color: '#1a1a1a', borderLeft: 'none', borderBottom: 'none', paddingLeft: '0', background: 'transparent', fontWeight: '600', display: 'block', borderRadius: '0' },
        highlight: { background: '#f6ffed', borderLeft: '4px solid #07c160', border: 'none', color: '#262626' },
        cover: { background: 'linear-gradient(135deg, #07c160 0%, #95de64 100%)', color: '#fff', borderRadius: '8px' },
        meta: { color: '#8c8c8c' },
        list: { color: '#262626' },
        divider: { background: '#eee' },
        tag: { color: '#07c160', background: '#f6ffed' },
        cta: { color: '#07c160', background: 'transparent', border: '1px dashed #07c160' }
      },
      toutiao: {
        container: { fontSize: isMobile ? '16px' : '17px', lineHeight: isMobile ? '1.75' : '1.8', color: '#222', fontFamily: 'inherit' },
        title: { fontSize: isMobile ? '22px' : '28px', color: '#1a1a1a', fontWeight: '800' },
        heading: { fontSize: isMobile ? '17px' : '19px', color: '#ff6600', borderLeft: '4px solid #ff6600', borderBottom: 'none', paddingLeft: '10px', background: 'transparent', fontWeight: '700', display: 'block', borderRadius: '0' },
        highlight: { background: '#fff7e6', borderLeft: '4px solid #ff6600', border: 'none', color: '#262626' },
        cover: { height: isMobile ? '6px' : '8px', background: '#ff6600', borderRadius: '4px', color: 'transparent', fontSize: '0' },
        meta: { color: '#595959', fontWeight: '500' },
        list: { color: '#262626' },
        divider: { background: '#ffd591' },
        tag: { color: '#ff6600', background: '#fff7e6' },
        cta: { color: '#fff', background: '#ff6600', border: 'none', borderRadius: '4px', fontWeight: '600' }
      },
      xiaohongshu: {
        container: { fontSize: isMobile ? '14px' : '15px', lineHeight: isMobile ? '1.8' : '1.85', color: '#333', fontFamily: 'inherit' },
        title: { fontSize: isMobile ? '20px' : '26px', color: '#ff2442', fontWeight: '700' },
        heading: { fontSize: isMobile ? '15px' : '17px', color: '#ff2442', borderLeft: 'none', borderBottom: 'none', paddingLeft: '0', background: '#fff0f3', fontWeight: '600', display: 'inline-block', borderRadius: '12px', padding: '4px 12px' },
        highlight: { background: '#fff0f3', border: '1px solid #ff2442', borderLeft: '1px solid #ff2442', color: '#262626', borderRadius: '8px' },
        cover: { background: 'linear-gradient(135deg, #ff2442 0%, #ff7a8a 100%)', color: '#fff', borderRadius: isMobile ? '12px' : '16px' },
        meta: { color: '#999' },
        list: { color: '#333' },
        divider: { background: '#ffd1d9' },
        tag: { color: '#ff2442', background: '#fff0f3', borderRadius: '12px' },
        cta: { color: '#fff', background: '#ff2442', border: 'none', borderRadius: isMobile ? '14px' : '16px', fontWeight: '600' }
      },
      baijiahao: {
        container: { fontSize: isMobile ? '15px' : '16px', lineHeight: isMobile ? '1.75' : '1.8', color: '#262626', fontFamily: 'inherit' },
        title: { fontSize: isMobile ? '20px' : '26px', color: '#1a1a1a', fontWeight: '700' },
        heading: { fontSize: isMobile ? '16px' : '18px', color: '#1677ff', borderLeft: 'none', borderBottom: '2px solid #1677ff', paddingLeft: '0', background: 'transparent', fontWeight: '600', display: 'block', borderRadius: '0' },
        highlight: { background: '#e6f4ff', borderLeft: '4px solid #1677ff', border: 'none', color: '#262626' },
        cover: { display: 'none' },
        meta: { color: '#8c8c8c' },
        list: { color: '#262626' },
        divider: { background: '#d6e4ff' },
        tag: { color: '#1677ff', background: '#e6f4ff' },
        cta: { color: '#1677ff', background: 'transparent', border: '1px solid #1677ff', borderRadius: '4px', fontWeight: '600' }
      },
      business: {
        container: { fontSize: '14px', lineHeight: isMobile ? '1.7' : '1.75', color: '#262626', fontFamily: 'inherit' },
        title: { fontSize: isMobile ? '19px' : '24px', color: '#1677ff', fontWeight: '700' },
        heading: { fontSize: isMobile ? '15px' : '16px', color: '#1677ff', borderLeft: '3px solid #1677ff', borderBottom: 'none', paddingLeft: '10px', background: 'transparent', fontWeight: '600', display: 'block', borderRadius: '0' },
        highlight: { background: '#f0f5ff', borderLeft: '4px solid #1677ff', border: 'none', color: '#262626' },
        cover: { display: 'none' },
        meta: { color: '#8c8c8c' },
        list: { color: '#262626' },
        divider: { background: '#d9d9d9' },
        tag: { color: '#1677ff', background: '#f0f5ff' },
        cta: { color: '#fff', background: '#1677ff', border: 'none', borderRadius: '4px', fontWeight: '600' }
      },
      marketing: {
        container: { fontSize: isMobile ? '17px' : '18px', lineHeight: isMobile ? '1.75' : '1.8', color: '#262626', fontFamily: 'inherit' },
        title: { fontSize: isMobile ? '22px' : '28px', color: '#cf1322', fontWeight: '800' },
        heading: { fontSize: isMobile ? '18px' : '20px', color: '#cf1322', borderLeft: 'none', borderBottom: 'none', paddingLeft: '0', background: 'transparent', fontWeight: '700', display: 'block', borderRadius: '0' },
        highlight: { background: '#fff2f0', borderLeft: '4px solid #cf1322', border: 'none', color: '#262626' },
        cover: { background: 'linear-gradient(135deg, #cf1322 0%, #ff7875 100%)', color: '#fff', borderRadius: '8px' },
        meta: { color: '#595959', fontWeight: '500' },
        list: { color: '#262626' },
        divider: { background: '#ffccc7' },
        tag: { color: '#cf1322', background: '#fff2f0' },
        cta: { color: '#fff', background: '#cf1322', border: 'none', borderRadius: '8px', fontWeight: '700' }
      },
      story: {
        container: { fontSize: isMobile ? '15px' : '16px', lineHeight: isMobile ? '1.8' : '1.85', color: '#262626', fontFamily: 'Georgia, serif' },
        title: { fontSize: isMobile ? '20px' : '26px', color: '#5a3e2b', fontWeight: '700' },
        heading: { fontSize: isMobile ? '16px' : '18px', color: '#5a3e2b', borderLeft: '3px solid #d4a373', borderBottom: 'none', paddingLeft: '10px', background: 'transparent', fontWeight: '600', display: 'block', borderRadius: '0' },
        highlight: { background: '#faf5ef', borderLeft: '4px solid #d4a373', border: 'none', color: '#262626' },
        cover: { background: 'linear-gradient(135deg, #5a3e2b 0%, #d4a373 100%)', color: '#fff', borderRadius: '8px' },
        meta: { color: '#8b5e34', fontFamily: 'Georgia, serif' },
        list: { color: '#5a3e2b' },
        divider: { background: '#f0e6d8' },
        tag: { color: '#8b5e34', background: '#faf5ef', border: '1px solid #f0e6d8' },
        cta: { color: '#fff', background: '#d4a373', border: 'none', borderRadius: '4px', fontWeight: '600' }
      },
      academic: {
        container: { fontSize: isMobile ? '15px' : '16px', lineHeight: isMobile ? '1.6' : '1.5', color: '#262626', fontFamily: 'serif' },
        title: { fontSize: isMobile ? '19px' : '24px', color: '#1a1a1a', fontWeight: '700' },
        heading: { fontSize: isMobile ? '16px' : '17px', color: '#1a1a1a', borderLeft: 'none', borderBottom: 'none', paddingLeft: '0', background: 'transparent', fontWeight: '600', display: 'block', borderRadius: '0' },
        highlight: { background: '#fafafa', borderLeft: '1px solid #d9d9d9', border: 'none', color: '#262626' },
        cover: { display: 'none' },
        meta: { color: '#595959', fontFamily: 'serif' },
        list: { color: '#262626' },
        divider: { background: '#d9d9d9' },
        tag: { color: '#262626', background: '#fafafa', border: '1px solid #d9d9d9' },
        cta: { color: '#262626', background: '#fafafa', border: '1px solid #d9d9d9', fontFamily: 'serif' }
      },
      magazine: {
        container: { fontSize: isMobile ? '15px' : '16px', lineHeight: isMobile ? '1.8' : '1.9', color: '#262626', fontFamily: 'Georgia, serif' },
        title: { fontSize: isMobile ? '24px' : '32px', color: '#1a1a1a', fontWeight: '700', textAlign: 'center', fontFamily: 'Georgia, serif' },
        heading: { fontSize: isMobile ? '16px' : '18px', color: '#1a1a1a', borderLeft: 'none', borderBottom: '1px solid #d9d9d9', borderTop: 'none', paddingLeft: '0', paddingBottom: '8px', background: 'transparent', fontWeight: '600', display: 'block', borderRadius: '0', fontFamily: 'Georgia, serif', fontStyle: 'italic', boxShadow: 'none', margin: '24px 0 12px' },
        highlight: { background: '#fafafa', border: 'none', borderTop: '1px solid #d9d9d9', borderBottom: '1px solid #d9d9d9', borderLeft: 'none', color: '#262626', textAlign: 'center', boxShadow: 'none' },
        cover: { display: 'none' },
        meta: { color: '#8c8c8c', textAlign: 'center' },
        list: { color: '#262626' },
        divider: { background: '#d9d9d9' },
        tag: { color: '#595959', background: '#fafafa', border: '1px solid #d9d9d9' },
        cta: { color: '#1a1a1a', background: 'transparent', border: '1px solid #1a1a1a', borderRadius: '0', fontFamily: 'Georgia, serif' }
      },
      card: {
        container: { fontSize: isMobile ? '14px' : '15px', lineHeight: isMobile ? '1.75' : '1.8', color: '#262626', fontFamily: 'inherit' },
        title: { fontSize: isMobile ? '20px' : '24px', color: '#07c160', fontWeight: '700' },
        heading: { fontSize: isMobile ? '15px' : '17px', color: '#1a1a1a', borderLeft: 'none', borderBottom: 'none', borderTop: '4px solid #07c160', paddingLeft: '0', background: '#fff', fontWeight: '600', display: 'block', borderRadius: '8px', padding: '12px', boxShadow: '0 2px 8px rgba(0,0,0,0.08)', margin: '16px 0 12px', fontStyle: 'normal', fontFamily: 'inherit' },
        highlight: { background: '#fff', border: '1px solid #d9f7be', borderLeft: '1px solid #d9f7be', color: '#262626', borderRadius: '8px', boxShadow: '0 2px 8px rgba(0,0,0,0.06)' },
        cover: { height: isMobile ? '5px' : '6px', background: '#07c160', borderRadius: '3px', color: 'transparent', fontSize: '0' },
        meta: { color: '#8c8c8c' },
        list: { color: '#262626' },
        divider: { background: '#e8e8e8' },
        tag: { color: '#07c160', background: '#f6ffed' },
        cta: { color: '#fff', background: '#07c160', border: 'none', borderRadius: '8px', fontWeight: '600' }
      },
      checklist: {
        container: { fontSize: isMobile ? '14px' : '15px', lineHeight: isMobile ? '1.75' : '1.8', color: '#262626', fontFamily: 'inherit' },
        title: { fontSize: isMobile ? '20px' : '24px', color: '#1a1a1a', fontWeight: '700' },
        heading: { fontSize: isMobile ? '15px' : '17px', color: '#07c160', borderLeft: 'none', borderBottom: 'none', borderTop: 'none', paddingLeft: '0', background: 'transparent', fontWeight: '700', display: 'block', borderRadius: '0', boxShadow: 'none', fontStyle: 'normal', fontFamily: 'inherit' },
        highlight: { background: '#f6ffed', borderLeft: '4px solid #07c160', border: 'none', color: '#262626', boxShadow: 'none', textAlign: 'left' },
        cover: { display: 'none' },
        meta: { color: '#8c8c8c' },
        list: { color: '#262626', listStyleType: 'none', paddingLeft: '0' },
        divider: { background: '#e8e8e8' },
        tag: { color: '#07c160', background: '#f6ffed' },
        cta: { color: '#07c160', background: 'transparent', border: '1px solid #07c160', borderRadius: '4px', fontWeight: '600' }
      },
      dark: {
        container: { fontSize: isMobile ? '15px' : '16px', lineHeight: isMobile ? '1.75' : '1.8', color: '#f0f0f0', fontFamily: 'inherit', background: '#1a1a1a', padding: isMobile ? '16px' : '24px' },
        title: { fontSize: isMobile ? '20px' : '26px', color: '#fff', fontWeight: '700', textAlign: 'left' },
        heading: { fontSize: isMobile ? '16px' : '18px', color: '#95de64', borderLeft: '4px solid #07c160', borderBottom: 'none', borderTop: 'none', paddingLeft: '10px', background: 'transparent', fontWeight: '600', display: 'block', borderRadius: '0', boxShadow: 'none', fontStyle: 'normal', fontFamily: 'inherit', margin: '24px 0 12px' },
        highlight: { background: '#333', borderLeft: '4px solid #07c160', border: 'none', color: '#f0f0f0', boxShadow: 'none', textAlign: 'left' },
        cover: { background: 'linear-gradient(135deg, #07c160 0%, #1a1a1a 100%)', color: 'transparent', fontSize: '0', borderRadius: '8px' },
        meta: { color: '#8c8c8c' },
        list: { color: '#f0f0f0' },
        divider: { background: '#333' },
        tag: { color: '#95de64', background: '#333', border: '1px solid #07c160' },
        cta: { color: '#1a1a1a', background: '#07c160', border: 'none', borderRadius: '4px', fontWeight: '600' }
      },
      'wechat-minimal': {
        container: { fontSize: isMobile ? '15px' : '16px', lineHeight: isMobile ? '1.8' : '1.9', color: '#262626', fontFamily: 'inherit', background: '#fff' },
        title: { fontSize: isMobile ? '20px' : '26px', color: '#1a1a1a', fontWeight: '400' },
        heading: { fontSize: isMobile ? '15px' : '16px', color: '#1a1a1a', borderLeft: 'none', borderBottom: '1px solid #e8e8e8', paddingLeft: '0', background: 'transparent', fontWeight: '500', display: 'block', borderRadius: '0', paddingBottom: '8px', textAlign: 'center' },
        highlight: { background: '#fafafa', borderLeft: '1px solid #e8e8e8', border: 'none', color: '#595959', textAlign: 'center' },
        cover: { display: 'none' },
        meta: { color: '#bfbfbf', textAlign: 'center' },
        list: { color: '#262626' },
        divider: { background: '#f0f0f0' },
        tag: { color: '#07c160', background: '#f6ffed' },
        cta: { color: '#07c160', background: 'transparent', border: '1px solid #07c160', borderRadius: '4px', fontWeight: '500' }
      },
      'wechat-dialogue': {
        container: { fontSize: isMobile ? '14px' : '15px', lineHeight: isMobile ? '1.75' : '1.8', color: '#262626', fontFamily: 'inherit' },
        title: { fontSize: isMobile ? '18px' : '22px', color: '#1a1a1a', fontWeight: '600' },
        heading: { fontSize: isMobile ? '14px' : '15px', color: '#07c160', borderLeft: 'none', borderBottom: 'none', paddingLeft: '0', background: '#f6ffed', fontWeight: '500', display: 'inline-block', borderRadius: '16px', padding: '6px 14px' },
        highlight: { background: '#f6ffed', border: '1px solid #b7eb8f', borderLeft: '1px solid #b7eb8f', color: '#262626', borderRadius: '16px' },
        cover: { display: 'none' },
        meta: { color: '#8c8c8c' },
        list: { color: '#262626' },
        divider: { background: '#e8e8e8' },
        tag: { color: '#07c160', background: '#f6ffed', borderRadius: '12px' },
        cta: { color: '#fff', background: '#07c160', border: 'none', borderRadius: '16px', fontWeight: '500' }
      },
      'wechat-brand': {
        container: { fontSize: isMobile ? '15px' : '16px', lineHeight: isMobile ? '1.8' : '1.85', color: '#262626', fontFamily: 'inherit' },
        title: { fontSize: isMobile ? '22px' : '28px', color: '#07c160', fontWeight: '700', letterSpacing: '1px' },
        heading: { fontSize: isMobile ? '16px' : '18px', color: '#07c160', borderLeft: '4px solid #07c160', borderBottom: 'none', paddingLeft: '10px', background: 'transparent', fontWeight: '600', display: 'block', borderRadius: '0' },
        highlight: { background: '#f6ffed', borderLeft: '4px solid #07c160', border: 'none', color: '#262626' },
        cover: { background: 'linear-gradient(135deg, #07c160 0%, #95de64 100%)', color: '#fff', borderRadius: '8px' },
        meta: { color: '#8c8c8c' },
        list: { color: '#262626' },
        divider: { background: '#d9f7be' },
        tag: { color: '#07c160', background: '#f6ffed' },
        cta: { color: '#07c160', background: 'transparent', border: '2px solid #07c160', borderRadius: '4px', fontWeight: '600' }
      },
      'wechat-infographic': {
        container: { fontSize: isMobile ? '14px' : '15px', lineHeight: isMobile ? '1.7' : '1.75', color: '#262626', fontFamily: 'inherit' },
        title: { fontSize: isMobile ? '20px' : '24px', color: '#1a1a1a', fontWeight: '700' },
        heading: { fontSize: isMobile ? '15px' : '16px', color: '#fff', borderLeft: 'none', borderBottom: 'none', paddingLeft: '0', background: '#07c160', fontWeight: '600', display: 'inline-block', borderRadius: '4px', padding: '4px 10px' },
        highlight: { background: '#f6ffed', border: '1px solid #d9f7be', borderLeft: '1px solid #d9f7be', color: '#262626', borderRadius: '4px' },
        cover: { background: '#07c160', color: '#fff', borderRadius: '4px' },
        meta: { color: '#8c8c8c' },
        list: { color: '#262626' },
        divider: { background: '#d9f7be' },
        tag: { color: '#fff', background: '#07c160', borderRadius: '4px' },
        cta: { color: '#fff', background: '#07c160', border: 'none', borderRadius: '4px', fontWeight: '600' }
      },
      'xiaohongshu-list': {
        container: { fontSize: isMobile ? '14px' : '15px', lineHeight: isMobile ? '1.8' : '1.85', color: '#333', fontFamily: 'inherit' },
        title: { fontSize: isMobile ? '20px' : '24px', color: '#ff2442', fontWeight: '700' },
        heading: { fontSize: isMobile ? '15px' : '16px', color: '#ff2442', borderLeft: 'none', borderBottom: 'none', paddingLeft: '0', background: 'transparent', fontWeight: '700', display: 'block', borderRadius: '0' },
        highlight: { background: '#fff0f3', border: '1px solid #ffd1d9', borderLeft: '1px solid #ffd1d9', color: '#262626', borderRadius: '12px' },
        cover: { background: 'linear-gradient(135deg, #ff2442 0%, #ff7a8a 100%)', color: '#fff', borderRadius: isMobile ? '12px' : '16px' },
        meta: { color: '#999' },
        list: { color: '#333', listStyleType: 'decimal', paddingLeft: '20px' },
        divider: { background: '#ffd1d9' },
        tag: { color: '#ff2442', background: '#fff0f3', borderRadius: '12px' },
        cta: { color: '#fff', background: '#ff2442', border: 'none', borderRadius: isMobile ? '14px' : '16px', fontWeight: '600' }
      },
      'xiaohongshu-review': {
        container: { fontSize: isMobile ? '14px' : '15px', lineHeight: isMobile ? '1.8' : '1.85', color: '#333', fontFamily: 'inherit' },
        title: { fontSize: isMobile ? '20px' : '24px', color: '#ff2442', fontWeight: '700' },
        heading: { fontSize: isMobile ? '15px' : '16px', color: '#ff6600', borderLeft: 'none', borderBottom: 'none', paddingLeft: '0', background: 'transparent', fontWeight: '700', display: 'block', borderRadius: '0' },
        highlight: { background: '#fff7e6', border: '1px solid #ffd591', borderLeft: '1px solid #ffd591', color: '#262626', borderRadius: '8px' },
        cover: { background: 'linear-gradient(135deg, #ff2442 0%, #ff7a8a 100%)', color: '#fff', borderRadius: isMobile ? '12px' : '16px' },
        meta: { color: '#999' },
        list: { color: '#333' },
        divider: { background: '#ffd1d9' },
        tag: { color: '#ff6600', background: '#fff7e6', borderRadius: '8px' },
        cta: { color: '#fff', background: '#ff2442', border: 'none', borderRadius: isMobile ? '14px' : '16px', fontWeight: '600' }
      },
      'xiaohongshu-tutorial': {
        container: { fontSize: isMobile ? '14px' : '15px', lineHeight: isMobile ? '1.8' : '1.85', color: '#333', fontFamily: 'inherit' },
        title: { fontSize: isMobile ? '20px' : '24px', color: '#ff2442', fontWeight: '700' },
        heading: { fontSize: isMobile ? '15px' : '16px', color: '#fff', borderLeft: 'none', borderBottom: 'none', paddingLeft: '0', background: '#ff2442', fontWeight: '600', display: 'inline-block', borderRadius: '50%', padding: '6px 12px', width: '28px', height: '28px', textAlign: 'center', lineHeight: '28px', boxSizing: 'border-box' },
        highlight: { background: '#fff0f3', border: '1px solid #ffd1d9', borderLeft: '1px solid #ffd1d9', color: '#262626', borderRadius: '8px' },
        cover: { background: 'linear-gradient(135deg, #ff2442 0%, #ff7a8a 100%)', color: '#fff', borderRadius: isMobile ? '12px' : '16px' },
        meta: { color: '#999' },
        list: { color: '#333' },
        divider: { background: '#ffd1d9' },
        tag: { color: '#ff2442', background: '#fff0f3', borderRadius: '12px' },
        cta: { color: '#fff', background: '#ff2442', border: 'none', borderRadius: isMobile ? '14px' : '16px', fontWeight: '600' }
      },
      'xiaohongshu-emotion': {
        container: { fontSize: isMobile ? '14px' : '15px', lineHeight: isMobile ? '1.8' : '1.85', color: '#333', fontFamily: 'inherit' },
        title: { fontSize: isMobile ? '20px' : '24px', color: '#ff2442', fontWeight: '700' },
        heading: { fontSize: isMobile ? '15px' : '16px', color: '#ff2442', borderLeft: 'none', borderBottom: 'none', paddingLeft: '0', background: 'transparent', fontWeight: '600', display: 'block', borderRadius: '0' },
        highlight: { background: '#fff0f3', border: '1px solid #ffd1d9', borderLeft: '1px solid #ffd1d9', color: '#262626', borderRadius: '12px' },
        cover: { background: 'linear-gradient(135deg, #ff2442 0%, #ff7a8a 100%)', color: '#fff', borderRadius: isMobile ? '12px' : '16px' },
        meta: { color: '#999' },
        list: { color: '#333' },
        divider: { background: '#ffd1d9' },
        tag: { color: '#ff2442', background: '#fff0f3', borderRadius: '12px' },
        cta: { color: '#fff', background: '#ff2442', border: 'none', borderRadius: isMobile ? '14px' : '16px', fontWeight: '600' }
      },
      'toutiao-news': {
        container: { fontSize: isMobile ? '16px' : '17px', lineHeight: isMobile ? '1.75' : '1.8', color: '#222', fontFamily: 'inherit' },
        title: { fontSize: isMobile ? '22px' : '28px', color: '#1a1a1a', fontWeight: '800' },
        heading: { fontSize: isMobile ? '17px' : '19px', color: '#ff6600', borderLeft: '4px solid #ff6600', borderBottom: 'none', paddingLeft: '10px', background: 'transparent', fontWeight: '700', display: 'block', borderRadius: '0' },
        highlight: { background: '#fff7e6', borderLeft: '4px solid #ff6600', border: 'none', color: '#262626' },
        cover: { height: isMobile ? '6px' : '8px', background: '#ff6600', borderRadius: '4px', color: 'transparent', fontSize: '0' },
        meta: { color: '#595959', fontWeight: '500' },
        list: { color: '#262626' },
        divider: { background: '#ffd591' },
        tag: { color: '#ff6600', background: '#fff7e6' },
        cta: { color: '#fff', background: '#ff6600', border: 'none', borderRadius: '4px', fontWeight: '600' }
      },
      'toutiao-depth': {
        container: { fontSize: isMobile ? '15px' : '16px', lineHeight: isMobile ? '1.8' : '1.85', color: '#222', fontFamily: 'inherit' },
        title: { fontSize: isMobile ? '20px' : '26px', color: '#1a1a1a', fontWeight: '700' },
        heading: { fontSize: isMobile ? '16px' : '18px', color: '#1a1a1a', borderLeft: 'none', borderBottom: '2px solid #ff6600', paddingLeft: '0', background: 'transparent', fontWeight: '600', display: 'block', borderRadius: '0' },
        highlight: { background: '#fff7e6', borderLeft: '4px solid #ff6600', border: 'none', color: '#262626' },
        cover: { display: 'none' },
        meta: { color: '#595959', fontWeight: '500' },
        list: { color: '#262626' },
        divider: { background: '#ffd591' },
        tag: { color: '#ff6600', background: '#fff7e6' },
        cta: { color: '#fff', background: '#ff6600', border: 'none', borderRadius: '4px', fontWeight: '600' }
      },
      'toutiao-hot': {
        container: { fontSize: isMobile ? '16px' : '17px', lineHeight: isMobile ? '1.75' : '1.8', color: '#222', fontFamily: 'inherit' },
        title: { fontSize: isMobile ? '22px' : '28px', color: '#ff6600', fontWeight: '800' },
        heading: { fontSize: isMobile ? '17px' : '19px', color: '#ff6600', borderLeft: '4px solid #ff6600', borderBottom: 'none', paddingLeft: '10px', background: 'transparent', fontWeight: '700', display: 'block', borderRadius: '0' },
        highlight: { background: '#fff7e6', borderLeft: '4px solid #ff6600', border: 'none', color: '#262626' },
        cover: { height: isMobile ? '6px' : '8px', background: '#ff6600', borderRadius: '4px', color: 'transparent', fontSize: '0' },
        meta: { color: '#595959', fontWeight: '500' },
        list: { color: '#262626' },
        divider: { background: '#ffd591' },
        tag: { color: '#ff6600', background: '#fff7e6' },
        cta: { color: '#fff', background: '#ff6600', border: 'none', borderRadius: '4px', fontWeight: '600' }
      },
      'toutiao-qa': {
        container: { fontSize: isMobile ? '15px' : '16px', lineHeight: isMobile ? '1.75' : '1.8', color: '#222', fontFamily: 'inherit' },
        title: { fontSize: isMobile ? '20px' : '24px', color: '#ff6600', fontWeight: '700' },
        heading: { fontSize: isMobile ? '16px' : '17px', color: '#ff6600', borderLeft: '4px solid #ff6600', borderBottom: 'none', paddingLeft: '10px', background: 'transparent', fontWeight: '600', display: 'block', borderRadius: '0' },
        highlight: { background: '#fff7e6', borderLeft: '4px solid #ff6600', border: 'none', color: '#262626' },
        cover: { display: 'none' },
        meta: { color: '#595959' },
        list: { color: '#262626' },
        divider: { background: '#ffd591' },
        tag: { color: '#ff6600', background: '#fff7e6' },
        cta: { color: '#fff', background: '#ff6600', border: 'none', borderRadius: '4px', fontWeight: '600' }
      },
      'baijiahao-science': {
        container: { fontSize: isMobile ? '15px' : '16px', lineHeight: isMobile ? '1.75' : '1.8', color: '#262626', fontFamily: 'inherit' },
        title: { fontSize: isMobile ? '20px' : '26px', color: '#1a1a1a', fontWeight: '700' },
        heading: { fontSize: isMobile ? '16px' : '18px', color: '#1677ff', borderLeft: '4px solid #1677ff', borderBottom: 'none', paddingLeft: '10px', background: 'transparent', fontWeight: '600', display: 'block', borderRadius: '0' },
        highlight: { background: '#e6f4ff', borderLeft: '4px solid #1677ff', border: 'none', color: '#262626' },
        cover: { display: 'none' },
        meta: { color: '#8c8c8c' },
        list: { color: '#262626' },
        divider: { background: '#d6e4ff' },
        tag: { color: '#1677ff', background: '#e6f4ff' },
        cta: { color: '#1677ff', background: 'transparent', border: '1px solid #1677ff', borderRadius: '4px', fontWeight: '600' }
      },
      'baijiahao-history': {
        container: { fontSize: isMobile ? '15px' : '16px', lineHeight: isMobile ? '1.85' : '1.9', color: '#262626', fontFamily: 'Georgia, serif' },
        title: { fontSize: isMobile ? '20px' : '26px', color: '#1a1a1a', fontWeight: '700', fontFamily: 'Georgia, serif' },
        heading: { fontSize: isMobile ? '16px' : '18px', color: '#1677ff', borderLeft: 'none', borderBottom: '1px solid #1677ff', paddingLeft: '0', background: 'transparent', fontWeight: '600', display: 'block', borderRadius: '0', fontFamily: 'Georgia, serif' },
        highlight: { background: '#e6f4ff', borderLeft: '4px solid #1677ff', border: 'none', color: '#262626' },
        cover: { display: 'none' },
        meta: { color: '#8c8c8c', fontFamily: 'Georgia, serif' },
        list: { color: '#262626' },
        divider: { background: '#d6e4ff' },
        tag: { color: '#1677ff', background: '#e6f4ff' },
        cta: { color: '#1677ff', background: 'transparent', border: '1px solid #1677ff', borderRadius: '4px', fontWeight: '600' }
      },
      'baijiahao-guide': {
        container: { fontSize: isMobile ? '15px' : '16px', lineHeight: isMobile ? '1.75' : '1.8', color: '#262626', fontFamily: 'inherit' },
        title: { fontSize: isMobile ? '20px' : '26px', color: '#1677ff', fontWeight: '700' },
        heading: { fontSize: isMobile ? '16px' : '18px', color: '#1677ff', borderLeft: 'none', borderBottom: 'none', paddingLeft: '0', background: '#e6f4ff', fontWeight: '600', display: 'inline-block', borderRadius: '4px', padding: '4px 10px' },
        highlight: { background: '#e6f4ff', border: '1px solid #bae0ff', borderLeft: '1px solid #bae0ff', color: '#262626', borderRadius: '4px' },
        cover: { display: 'none' },
        meta: { color: '#8c8c8c' },
        list: { color: '#262626', listStyleType: 'decimal', paddingLeft: '20px' },
        divider: { background: '#d6e4ff' },
        tag: { color: '#1677ff', background: '#e6f4ff' },
        cta: { color: '#fff', background: '#1677ff', border: 'none', borderRadius: '4px', fontWeight: '600' }
      },
      'douyin-graphic': {
        container: { fontSize: isMobile ? '16px' : '18px', lineHeight: isMobile ? '1.7' : '1.75', color: '#1a1a1a', fontFamily: 'inherit', background: '#f5f5f5', padding: isMobile ? '16px' : '24px' },
        title: { fontSize: isMobile ? '24px' : '32px', color: '#1a1a1a', fontWeight: '800', textAlign: 'center' },
        heading: { fontSize: isMobile ? '16px' : '18px', color: '#1a1a1a', borderLeft: 'none', borderBottom: 'none', paddingLeft: '0', background: 'transparent', fontWeight: '700', display: 'block', borderRadius: '0', textAlign: 'center' },
        highlight: { background: '#fff', border: '1px solid #d9d9d9', borderLeft: '1px solid #d9d9d9', color: '#1a1a1a', borderRadius: '8px', textAlign: 'center' },
        cover: { background: '#1a1a1a', color: '#fff', borderRadius: '8px' },
        meta: { color: '#8c8c8c', textAlign: 'center' },
        list: { color: '#1a1a1a' },
        divider: { background: '#d9d9d9' },
        tag: { color: '#fff', background: '#1a1a1a', borderRadius: '4px' },
        cta: { color: '#fff', background: '#1a1a1a', border: 'none', borderRadius: '8px', fontWeight: '700' }
      },
      'douyin-quote': {
        container: { fontSize: isMobile ? '16px' : '18px', lineHeight: isMobile ? '1.6' : '1.65', color: '#1a1a1a', fontFamily: 'inherit', background: '#f5f5f5', padding: isMobile ? '16px' : '24px' },
        title: { fontSize: isMobile ? '22px' : '28px', color: '#1a1a1a', fontWeight: '700', textAlign: 'center' },
        heading: { fontSize: isMobile ? '16px' : '18px', color: '#1a1a1a', borderLeft: 'none', borderBottom: 'none', paddingLeft: '0', background: 'transparent', fontWeight: '600', display: 'block', borderRadius: '0', textAlign: 'center' },
        highlight: { background: '#1a1a1a', border: 'none', color: '#fff', borderRadius: '8px', textAlign: 'center' },
        cover: { background: '#1a1a1a', color: '#fff', borderRadius: '8px' },
        meta: { color: '#8c8c8c', textAlign: 'center' },
        list: { color: '#1a1a1a' },
        divider: { background: '#d9d9d9' },
        tag: { color: '#fff', background: '#1a1a1a', borderRadius: '4px' },
        cta: { color: '#1a1a1a', background: '#fff', border: '2px solid #1a1a1a', borderRadius: '8px', fontWeight: '700' }
      },
      'zhihu-answer': {
        container: { fontSize: isMobile ? '15px' : '16px', lineHeight: isMobile ? '1.75' : '1.8', color: '#262626', fontFamily: 'inherit' },
        title: { fontSize: isMobile ? '20px' : '24px', color: '#1a1a1a', fontWeight: '700' },
        heading: { fontSize: isMobile ? '16px' : '17px', color: '#0066ff', borderLeft: '4px solid #0066ff', borderBottom: 'none', paddingLeft: '10px', background: 'transparent', fontWeight: '600', display: 'block', borderRadius: '0' },
        highlight: { background: '#f0f5ff', borderLeft: '4px solid #0066ff', border: 'none', color: '#262626' },
        cover: { display: 'none' },
        meta: { color: '#8c8c8c' },
        list: { color: '#262626' },
        divider: { background: '#d6e4ff' },
        tag: { color: '#0066ff', background: '#f0f5ff' },
        cta: { color: '#0066ff', background: 'transparent', border: '1px solid #0066ff', borderRadius: '4px', fontWeight: '600' }
      }
      };
    }


    var templateHeadingTexts = {
      wechat: ['01｜优先级排序：先做重要的事', '02｜时间块：给任务一个容器'],
      toutiao: ['01｜优先级排序：先做重要的事', '02｜时间块：给任务一个容器'],
      xiaohongshu: ['· 优先级排序：先做重要的事', '· 时间块：给任务一个容器'],
      baijiahao: ['一、优先级排序：先做重要的事', '二、时间块：给任务一个容器'],
      business: ['01｜优先级排序：先做重要的事', '02｜时间块：给任务一个容器'],
      marketing: ['01｜优先级排序：先做重要的事', '02｜时间块：给任务一个容器'],
      story: ['一、优先级排序：先做重要的事', '二、时间块：给任务一个容器'],
      academic: ['一、优先级排序：先做重要的事', '二、时间块：给任务一个容器'],
      magazine: ['一、优先级排序：先做重要的事', '二、时间块：给任务一个容器'],
      card: ['01｜优先级排序：先做重要的事', '02｜时间块：给任务一个容器'],
      checklist: ['✓ 优先级排序：先做重要的事', '✓ 时间块：给任务一个容器'],
      dark: ['01｜优先级排序：先做重要的事', '02｜时间块：给任务一个容器'],
      'wechat-minimal': ['— 极简留白：让内容自己说话', '— 呼吸感：段落之间留足空间'],
      'wechat-dialogue': ['A: 你平时怎么管理时间？', 'B: 我会先列出最重要的三件事'],
      'wechat-brand': ['品牌故事：从初心到坚持', '用户证言：真实的声音最有力'],
      'wechat-infographic': ['数据可视化：一图胜千言', '信息密度：让阅读更高效'],
      'xiaohongshu-list': ['1. 清单体：让信息一目了然', '2. 种草标签：快速定位需求'],
      'xiaohongshu-review': ['★★★★☆ 真实测评体验', '优缺点对比：理性种草'],
      'xiaohongshu-tutorial': ['Step 1: 准备工作要做好', 'Step 2: 操作步骤详解'],
      'xiaohongshu-emotion': ['情绪共鸣：你并不孤单', '温暖治愈：每一个情绪都值得被看见'],
      'toutiao-news': ['【快讯】第一时间掌握动态', '【资讯】深度解读热点事件'],
      'toutiao-depth': ['深度分析：透过现象看本质', '数据支撑：用事实说话'],
      'toutiao-hot': ['热点追踪：今日最火话题', '观点交锋：你怎么看？'],
      'toutiao-qa': ['Q: 为什么有人能高效管理时间？', 'A: 因为他们掌握了优先级'],
      'baijiahao-science': ['科学原理：时间管理的底层逻辑', '实验验证：数据不会说谎'],
      'baijiahao-history': ['历史回顾：古人如何管理时间', '人文视角：时间背后的文化意义'],
      'baijiahao-guide': ['攻略一：列出今日最重要的三件事', '攻略二：先完成最难的那一件'],
      'douyin-graphic': ['大字标题：第一眼就抓住你', '视觉冲击：让内容更有力量'],
      'douyin-quote': ['金句时刻：一句话点亮一天', '分享感：让好内容传播更远'],
      'zhihu-answer': ['Q: 为什么有人能在24小时内完成更多事？', 'A: 因为他们在管理注意力']
    };

  function applyTemplateToPreview(mockupEl, templateKey) {
    var preview = mockupEl.querySelector('.article-preview');
    if (!preview || !templateKey) return;

    var isCustom = templateKey.indexOf('custom_') === 0;
    var customTpl = isCustom ? getRuntimeTemplates().find(function(t) { return t.key === templateKey; }) : null;
    var baseKey = customTpl ? customTpl.baseKey : templateKey;

    var headerEl = mockupEl.querySelector('.mockup-header');
    var isMobile = headerEl ? headerEl.textContent.includes('移动端') : false;
    var styles = getTemplateStyles(isMobile);
    var s = styles[baseKey];
    if (!s) return;

    preview.setAttribute('data-template', templateKey);
    Object.assign(preview.style, s.container);
    preview.querySelectorAll('.preview-title').forEach(function(el) { Object.assign(el.style, s.title); });
    preview.querySelectorAll('.preview-heading').forEach(function(el, i) {
      Object.assign(el.style, s.heading);
      if (templateHeadingTexts[baseKey] && templateHeadingTexts[baseKey][i]) {
        el.textContent = templateHeadingTexts[baseKey][i];
      }
    });
    preview.querySelectorAll('.preview-highlight').forEach(function(el) { Object.assign(el.style, s.highlight); });
    preview.querySelectorAll('.preview-cover').forEach(function(el) { Object.assign(el.style, s.cover || {}); });
    preview.querySelectorAll('.preview-meta').forEach(function(el) { Object.assign(el.style, s.meta || {}); });
    preview.querySelectorAll('.preview-list').forEach(function(el) { Object.assign(el.style, s.list || {}); });
    preview.querySelectorAll('.preview-divider').forEach(function(el) { Object.assign(el.style, s.divider || {}); });
    preview.querySelectorAll('.preview-tag').forEach(function(el) { Object.assign(el.style, s.tag || {}); });
    preview.querySelectorAll('.preview-cta').forEach(function(el) { Object.assign(el.style, s.cta || {}); });

    if (isCustom && customTpl && customTpl.overrides) {
      applyTemplateOverrides(mockupEl, customTpl.overrides);
    }
  }

  var themeColorMap = {
    brand:  { primary: '#07c160', bg: '#f6ffed', border: '#d9f7be' },
    blue:   { primary: '#1677ff', bg: '#f0f5ff', border: '#d6e4ff' },
    red:    { primary: '#cf1322', bg: '#fff2f0', border: '#ffccc7' },
    gray:   { primary: '#595959', bg: '#fafafa', border: '#e8e8e8' },
    pink:   { primary: '#ff2442', bg: '#fff0f3', border: '#ffd1d9' },
    orange: { primary: '#ff6600', bg: '#fff7e6', border: '#ffd591' }
  };

  function applyTemplateOverrides(mockupEl, overrides) {
    if (!overrides) return;
    var preview = mockupEl.querySelector('.article-preview');
    if (!preview) return;

    var theme = themeColorMap[overrides.theme];
    var primary = theme ? theme.primary : '#07c160';
    var bg = theme ? theme.bg : '#f6ffed';
    var border = theme ? theme.border : '#d9f7be';

    preview.querySelectorAll('.preview-title').forEach(function(el) {
      el.style.color = primary;
      if (overrides.titleStyle === 'center') {
        el.style.textAlign = 'center';
        el.style.borderBottom = 'none';
        el.style.paddingBottom = '0';
      } else if (overrides.titleStyle === 'left') {
        el.style.textAlign = 'left';
        el.style.borderBottom = 'none';
        el.style.paddingBottom = '0';
      } else if (overrides.titleStyle === 'underline') {
        el.style.textAlign = 'left';
        el.style.borderBottom = '2px solid ' + primary;
        el.style.paddingBottom = '8px';
      }
    });

    preview.querySelectorAll('.preview-heading').forEach(function(el) {
      el.style.color = primary;
      el.style.borderLeftColor = primary;
      el.style.borderBottomColor = primary;
    });

    preview.querySelectorAll('.preview-highlight').forEach(function(el) {
      if (overrides.highlightStyle === 'border') {
        el.style.background = bg;
        el.style.borderLeft = '4px solid ' + primary;
        el.style.border = 'none';
        el.style.borderLeft = '4px solid ' + primary;
        el.style.fontStyle = 'normal';
      } else if (overrides.highlightStyle === 'background') {
        el.style.background = bg;
        el.style.border = '1px solid ' + border;
        el.style.borderLeft = '1px solid ' + border;
        el.style.fontStyle = 'normal';
      } else if (overrides.highlightStyle === 'quote') {
        el.style.background = 'transparent';
        el.style.border = 'none';
        el.style.borderLeft = '4px solid ' + primary;
        el.style.fontStyle = 'italic';
      }
    });

    preview.querySelectorAll('.preview-list li').forEach(function(el) {
      el.style.color = primary;
    });

    if (overrides.useCards) {
      preview.querySelectorAll('p').forEach(function(el) {
        if (el.classList.contains('preview-title') || el.closest('.preview-highlight')) return;
        el.style.background = '#fff';
        el.style.border = '1px solid ' + border;
        el.style.borderRadius = '8px';
        el.style.padding = '12px 16px';
        el.style.boxShadow = '0 1px 3px rgba(0,0,0,0.04)';
      });
    }
  }

  function selectTemplate(card) {
    var container = card.parentElement;
    var isPill = card.style.borderRadius === '20px' || card.style.borderRadius === '20px';
    container.querySelectorAll('.template-card').forEach(function(c) {
      if (isPill) {
        c.style.borderColor = '#d9d9d9';
        c.style.borderWidth = '1px';
        c.style.background = '#fff';
        c.style.color = '#595959';
        c.style.fontWeight = '500';
      } else {
        c.style.borderColor = '#e8e8e8';
        c.style.borderWidth = '1px';
      }
    });
    if (isPill) {
      card.style.borderColor = '#07c160';
      card.style.borderWidth = '2px';
      card.style.background = '#f6ffed';
      card.style.color = '#07c160';
      card.style.fontWeight = '600';
    } else {
      card.style.borderColor = '#07c160';
      card.style.borderWidth = '2px';
    }

    var template = card.getAttribute('data-template');
    var mockup = card.closest('.mockup');
    var preview = mockup.querySelector('.article-preview');
    if (!preview || !template) return;

    applyTemplateToPreview(mockup, template);
  }

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
          return '<span class="publish-tag" onclick="copySingleTag(this)" style="padding: 4px 10px; background: #f6ffed; color: #07c160; border-radius: 12px; font-size: 13px; cursor: pointer; border: 1px solid #b7eb8f;" onmouseover="this.style.background=\'#e6f7ff\'" onmouseout="this.style.background=\'#f6ffed\'">' + t + '</span>';
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

  function exportWord(btn) {
    var mockup = btn.closest('.mockup') || document.querySelector('#screen-preview .mockup');
    var titleEl = mockup.querySelector('.preview-title');
    var preview = mockup.querySelector('.article-preview');
    if (!preview) return;

    var originalText = btn.textContent;
    btn.textContent = '导出中...';
    btn.disabled = true;

    setTimeout(function() {
      var title = titleEl ? titleEl.textContent.trim() : '未命名文章';
      var clone = preview.cloneNode(true);
      clone.querySelectorAll('script, style').forEach(function(el) { el.remove(); });
      var containerStyle = preview.getAttribute('style') || '';

      var html = '<html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:w="urn:schemas-microsoft-com:office:word" xmlns="http://www.w3.org/1999/xhtml">' +
        '<head><meta charset="UTF-8"><title>' + title + '</title></head>' +
        '<body style="font-family: -apple-system, BlinkMacSystemFont, sans-serif; padding: 40px;">' +
        '<h1 style="font-size: 24px; margin-bottom: 16px; line-height: 1.4;">' + title + '</h1>' +
        '<div style="' + containerStyle + '">' + clone.innerHTML + '</div>' +
        '</body></html>';

      var blob = new Blob(['\ufeff', html], { type: 'application/msword' });
      var url = URL.createObjectURL(blob);
      var a = document.createElement('a');
      a.href = url;
      a.download = title.replace(/[\\/:*?"<>|]/g, '_') + '.doc';
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);

      btn.textContent = originalText;
      btn.disabled = false;
    }, 400);
  }

  function copyArticleText(btn) {
    var mockup = btn.closest('.mockup') || document.querySelector('#screen-preview .mockup');
    var titleEl = mockup.querySelector('.preview-title');
    var preview = mockup.querySelector('.article-preview');
    if (!preview) return;

    var originalText = btn.textContent;
    btn.textContent = '复制中...';
    btn.disabled = true;

    setTimeout(function() {
      var title = titleEl ? titleEl.textContent.trim() : '未命名文章';
      var clone = preview.cloneNode(true);
      clone.querySelectorAll('script, style').forEach(function(el) { el.remove(); });

      var text = title + '\n\n' + clone.innerText.trim();
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(text).then(function() {
          btn.textContent = '已复制';
          setTimeout(function() { btn.textContent = originalText; btn.disabled = false; }, 1500);
        }).catch(function() {
          btn.textContent = originalText;
          btn.disabled = false;
          alert('复制失败，请手动复制');
        });
      } else {
        btn.textContent = originalText;
        btn.disabled = false;
        alert('当前环境不支持复制，请手动复制正文');
      }
    }, 300);
  }

  function wrapCardText(ctx, text, x, y, maxWidth, lineHeight, maxLines) {
    if (!text) return y;
    var chars = text.split('');
    var line = '';
    var yy = y;
    var drawnLines = 0;
    for (var i = 0; i < chars.length; i++) {
      line += chars[i];
      if (ctx.measureText(line).width > maxWidth) {
        line = line.slice(0, -1);
        if (drawnLines >= maxLines - 1 && i < chars.length - 1) {
          while (ctx.measureText(line + '...').width > maxWidth) line = line.slice(0, -1);
          ctx.fillText(line + '...', x, yy);
          return yy + lineHeight;
        }
        ctx.fillText(line, x, yy);
        drawnLines++;
        yy += lineHeight;
        line = chars[i];
      }
    }
    if (line) {
      if (drawnLines >= maxLines - 1) {
        while (ctx.measureText(line + '...').width > maxWidth) line = line.slice(0, -1);
        ctx.fillText(line + '...', x, yy);
      } else {
        ctx.fillText(line, x, yy);
      }
    }
    return yy;
  }

  var cardStyles = {
    xiaohongshu: {
      label: '小红书',
      accent: '#ff2442',
      coverGrad: ['#ff2442', '#ff7a8a', '#ffd1d9'],
      coverCircle: 'rgba(255,255,255,0.18)',
      coverCircle2: 'rgba(255,255,255,0.12)',
      tagBg: 'rgba(255,255,255,0.95)',
      tagText: '干货分享',
      brandText: '— 作者昵称 —',
      contentBg: '#fff',
      headingColor: '#1a1a1a',
      bodyColor: '#595959',
      numColor: '#fff',
      footerBg: '#fff0f3',
      font: '"PingFang SC", sans-serif'
    },
    wechat: {
      label: '公众号',
      accent: '#07c160',
      coverGrad: ['#07c160', '#95de64', '#d9f7be'],
      coverCircle: 'rgba(255,255,255,0.2)',
      coverCircle2: 'rgba(255,255,255,0.14)',
      tagBg: 'rgba(255,255,255,0.95)',
      tagText: '深度好文',
      brandText: '— 作者昵称 —',
      contentBg: '#fff',
      headingColor: '#1a1a1a',
      bodyColor: '#595959',
      numColor: '#fff',
      footerBg: '#f6ffed',
      font: '"PingFang SC", sans-serif'
    },
    douyin: {
      label: '抖音',
      accent: '#25f4ee',
      coverGrad: ['#0a0a0a', '#1a1a1a', '#fe2c55'],
      coverCircle: 'rgba(37,244,238,0.25)',
      coverCircle2: 'rgba(254,44,85,0.25)',
      tagBg: '#25f4ee',
      tagText: '上热门',
      brandText: '— 作者昵称 —',
      contentBg: '#1a1a1a',
      headingColor: '#fff',
      bodyColor: '#d9d9d9',
      numColor: '#0a0a0a',
      footerBg: '#000',
      font: '"PingFang SC", sans-serif'
    },
    literary: {
      label: '文艺',
      accent: '#8b5e34',
      coverGrad: ['#8b5e34', '#d4a373', '#f0e6d8'],
      coverCircle: 'rgba(255,255,255,0.18)',
      coverCircle2: 'rgba(255,255,255,0.12)',
      tagBg: 'rgba(255,255,255,0.92)',
      tagText: '慢读时光',
      brandText: '— 作者昵称 —',
      contentBg: '#faf5ef',
      headingColor: '#5a3e2b',
      bodyColor: '#8b5e34',
      numColor: '#fff',
      footerBg: '#f0e6d8',
      font: 'Georgia, "Songti SC", serif'
    },
    minimal: {
      label: '极简',
      accent: '#1a1a1a',
      coverGrad: ['#1a1a1a', '#262626', '#404040'],
      coverCircle: 'rgba(255,255,255,0.06)',
      coverCircle2: 'rgba(255,255,255,0.04)',
      tagBg: '#fff',
      tagText: 'NOTE',
      brandText: '— YOUR NAME —',
      contentBg: '#fff',
      headingColor: '#000',
      bodyColor: '#262626',
      numColor: '#fff',
      footerBg: '#fafafa',
      font: '"Helvetica Neue", "PingFang SC", sans-serif'
    },
    business: {
      label: '商务',
      accent: '#1677ff',
      coverGrad: ['#003a8c', '#1677ff', '#bae0ff'],
      coverCircle: 'rgba(255,255,255,0.15)',
      coverCircle2: 'rgba(255,255,255,0.1)',
      tagBg: 'rgba(255,255,255,0.95)',
      tagText: 'INSIGHT',
      brandText: '— YOUR NAME —',
      contentBg: '#fff',
      headingColor: '#003a8c',
      bodyColor: '#595959',
      numColor: '#fff',
      footerBg: '#f0f5ff',
      font: '"PingFang SC", sans-serif'
    }
  };

  function drawCard(canvas, data, styleName) {
    var style = cardStyles[styleName] || cardStyles.xiaohongshu;
    var ctx = canvas.getContext('2d');
    var w = canvas.width, h = canvas.height;
    ctx.clearRect(0, 0, w, h);

    if (data.type === 'cover') {
      var grad = ctx.createLinearGradient(0, 0, w, h);
      grad.addColorStop(0, style.coverGrad[0]);
      grad.addColorStop(0.5, style.coverGrad[1]);
      grad.addColorStop(1, style.coverGrad[2]);
      ctx.fillStyle = grad;
      ctx.fillRect(0, 0, w, h);

      ctx.fillStyle = style.coverCircle || 'rgba(255,255,255,0.18)';
      ctx.beginPath(); ctx.arc(w - 80, 120, 160, 0, Math.PI * 2); ctx.fill();
      ctx.beginPath(); ctx.arc(60, h - 220, 120, 0, Math.PI * 2); ctx.fill();
      ctx.fillStyle = style.coverCircle2 || 'rgba(255,255,255,0.12)';
      ctx.beginPath(); ctx.arc(w - 200, h - 120, 90, 0, Math.PI * 2); ctx.fill();

      ctx.fillStyle = style.tagBg;
      ctx.beginPath();
      ctx.moveTo(60 + 80, 80);
      ctx.arcTo(60 + 160, 80, 60 + 160, 80 + 44, 22);
      ctx.arcTo(60 + 160, 80 + 44, 60, 80 + 44, 22);
      ctx.arcTo(60, 80 + 44, 60, 80, 22);
      ctx.arcTo(60, 80, 60 + 160, 80, 22);
      ctx.closePath();
      ctx.fill();
      ctx.fillStyle = style.accent;
      ctx.font = 'bold 22px ' + style.font;
      ctx.textAlign = 'center';
      ctx.fillText(style.tagText, 140, 111);

      ctx.fillStyle = '#fff';
      ctx.font = 'bold 60px ' + style.font;
      ctx.textAlign = 'left';
      ctx.shadowColor = 'rgba(0,0,0,0.1)';
      ctx.shadowBlur = 8;
      ctx.shadowOffsetY = 4;
      wrapCardText(ctx, data.title, 60, 320, w - 120, 78, 4);
      ctx.shadowColor = 'transparent';
      ctx.shadowBlur = 0;

      ctx.fillStyle = 'rgba(255,255,255,0.92)';
      ctx.font = '26px ' + style.font;
      wrapCardText(ctx, data.desc, 60, h - 220, w - 120, 40, 2);

      ctx.fillStyle = '#fff';
      ctx.font = 'bold 26px ' + style.font;
      ctx.fillText(style.brandText, 60, h - 80);
    } else {
      var bg = style.contentBg || '#fff';
      ctx.fillStyle = bg;
      ctx.fillRect(0, 0, w, h);

      ctx.fillStyle = style.accent;
      ctx.fillRect(0, 0, w, 14);

      ctx.fillStyle = style.accent;
      ctx.beginPath();
      ctx.arc(110, 140, 56, 0, Math.PI * 2);
      ctx.fill();
      ctx.fillStyle = style.numColor || '#fff';
      ctx.font = 'bold 48px ' + style.font;
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      var numStr = String(data.num).padStart(2, '0');
      ctx.fillText(numStr, 110, 148);
      ctx.textBaseline = 'alphabetic';

      ctx.fillStyle = style.headingColor;
      ctx.font = 'bold 46px ' + style.font;
      ctx.textAlign = 'left';
      var titleEndY = wrapCardText(ctx, data.title, 60, 260, w - 120, 60, 2);

      ctx.fillStyle = style.accent;
      ctx.fillRect(60, titleEndY + 8, 80, 5);

      ctx.fillStyle = style.bodyColor;
      ctx.font = '28px ' + style.font;
      var contentY = titleEndY + 60;
      var lines = (data.content || '').split('\n');
      lines.forEach(function(line) {
        if (!line.trim()) return;
        wrapCardText(ctx, line.trim(), 60, contentY, w - 120, 44, 1);
        contentY += 50;
      });

      ctx.fillStyle = style.footerBg;
      ctx.fillRect(0, h - 110, w, 110);
      ctx.fillStyle = style.accent;
      ctx.font = 'bold 22px ' + style.font;
      ctx.fillText(style.brandText, 60, h - 55);
    }
  }

  function renderCardToCanvas(data, styleName) {
    var canvas = document.createElement('canvas');
    canvas.width = 750;
    canvas.height = 1000;
    drawCard(canvas, data, styleName);
    return canvas;
  }

  function downloadCanvas(canvas, filename) {
    canvas.toBlob(function(blob) {
      var url = URL.createObjectURL(blob);
      var a = document.createElement('a');
      a.href = url;
      a.download = filename;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    }, 'image/png');
  }

  function generateCards(btn) {
    var mockup = btn.closest('.mockup') || document.querySelector('#screen-preview .mockup');
    var titleEl = mockup.querySelector('.preview-title');
    var preview = mockup.querySelector('.article-preview');
    if (!preview) return;

    var originalText = btn.textContent;
    btn.textContent = '生成中...';
    btn.disabled = true;

    setTimeout(function() {
      var title = titleEl ? titleEl.textContent.trim() : '未命名文章';
      var paragraphs = preview.querySelectorAll('p');
      var headings = preview.querySelectorAll('.preview-heading');

      var cards = [];
      var firstP = paragraphs[0];
      cards.push({
        type: 'cover',
        title: title,
        desc: firstP ? firstP.textContent.trim().slice(0, 80) : ''
      });

      headings.forEach(function(h, i) {
        var contentParts = [];
        var nxt = h.nextElementSibling;
        while (nxt) {
          if (nxt.classList.contains('preview-highlight') || nxt.classList.contains('preview-tags') || nxt.classList.contains('preview-cta') || nxt.classList.contains('preview-divider')) break;
          if (nxt.tagName === 'P') {
            contentParts.push(nxt.textContent.trim().slice(0, 120));
          } else if (nxt.tagName === 'UL' || nxt.tagName === 'OL') {
            nxt.querySelectorAll('li').forEach(function(li) { contentParts.push('· ' + li.textContent.trim().slice(0, 50)); });
          }
          nxt = nxt.nextElementSibling;
        }
        cards.push({
          type: 'content',
          num: i + 1,
          title: h.textContent.trim(),
          content: contentParts.slice(0, 5).join('\n')
        });
      });

      btn.textContent = originalText;
      btn.disabled = false;

      showCardsModal(cards, title);
    }, 500);
  }

  function showCardsModal(cards, articleTitle) {
    var existing = document.getElementById('cards-modal');
    if (existing) existing.remove();

    var modal = document.createElement('div');
    modal.id = 'cards-modal';
    var isMobile = window.innerWidth < 600;
    modal.style.cssText = 'position: fixed; inset: 0; background: rgba(0,0,0,0.6); z-index: 9999; display: flex; align-items: center; justify-content: center; padding: ' + (isMobile ? '12px' : '40px') + '; font-family: -apple-system, BlinkMacSystemFont, sans-serif;';

    var box = document.createElement('div');
    box.style.cssText = 'background: #fff; border-radius: ' + (isMobile ? '12px' : '16px') + '; padding: ' + (isMobile ? '16px' : '32px') + '; max-width: 1200px; max-height: 92vh; overflow-y: auto; position: relative; width: 100%; box-sizing: border-box;';

    var closeBtn = document.createElement('button');
    closeBtn.textContent = '×';
    closeBtn.style.cssText = 'position: absolute; top: ' + (isMobile ? '8px' : '12px') + '; right: ' + (isMobile ? '12px' : '16px') + '; background: none; border: none; font-size: ' + (isMobile ? '26px' : '32px') + '; cursor: pointer; color: #595959; line-height: 1; z-index: 2;';
    closeBtn.onclick = function() { modal.remove(); };
    box.appendChild(closeBtn);

    var currentStyle = 'xiaohongshu';

    var header = document.createElement('div');
    header.style.cssText = 'display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; padding-right: ' + (isMobile ? '28px' : '40px') + '; flex-wrap: wrap; gap: 8px;';
    header.innerHTML = '<h2 style="margin: 0; font-size: ' + (isMobile ? '17px' : '22px') + '; color: #1a1a1a;">生成贴图</h2>';
    var dlAll = document.createElement('button');
    dlAll.textContent = '全部下载';
    dlAll.style.cssText = 'padding: ' + (isMobile ? '7px 14px' : '10px 20px') + '; background: #07c160; color: #fff; border: none; border-radius: 8px; font-size: ' + (isMobile ? '13px' : '14px') + '; font-weight: 600; cursor: pointer;';
    dlAll.onclick = function() {
      var i = 0;
      function next() {
        if (i >= cards.length) return;
        var c = renderCardToCanvas(cards[i], currentStyle);
        var styleLabel = cardStyles[currentStyle].label;
        downloadCanvas(c, styleLabel + '_贴图_' + (i + 1) + '_' + cards.length + '.png');
        i++;
        setTimeout(next, 400);
      }
      next();
    };
    header.appendChild(dlAll);
    box.appendChild(header);

    var tabs = document.createElement('div');
    tabs.style.cssText = 'display: flex; gap: ' + (isMobile ? '6px' : '10px') + '; margin: 12px 0 ' + (isMobile ? '8px' : '12px') + '; flex-wrap: wrap;';

    Object.keys(cardStyles).forEach(function(key) {
      var s = cardStyles[key];
      var tab = document.createElement('button');
      tab.textContent = s.label;
      tab.dataset.style = key;
      tab.style.cssText = 'padding: ' + (isMobile ? '6px 12px' : '8px 18px') + '; border: 1px solid #d9d9d9; background: #fff; border-radius: 20px; font-size: ' + (isMobile ? '12px' : '13px') + '; cursor: pointer; color: #595959; font-weight: 500;';
      tab.onclick = function() {
        currentStyle = key;
        tabs.querySelectorAll('button').forEach(function(b) {
          var active = b.dataset.style === key;
          b.style.background = active ? s.accent : '#fff';
          b.style.color = active ? '#fff' : '#595959';
          b.style.borderColor = active ? s.accent : '#d9d9d9';
          b.style.fontWeight = active ? '600' : '500';
        });
        renderGrid();
      };
      tabs.appendChild(tab);
    });
    tabs.firstChild.click();
    box.appendChild(tabs);

    var sub = document.createElement('div');
    sub.style.cssText = 'color: #8c8c8c; font-size: ' + (isMobile ? '12px' : '13px') + '; margin-bottom: ' + (isMobile ? '12px' : '16px') + ';';
    box.appendChild(sub);

    var grid = document.createElement('div');
    grid.style.cssText = 'display: grid; grid-template-columns: repeat(auto-fill, minmax(' + (isMobile ? '150px' : '220px') + ', 1fr)); gap: ' + (isMobile ? '12px' : '20px') + ';';
    box.appendChild(grid);

    function renderGrid() {
      var s = cardStyles[currentStyle];
      sub.textContent = '共 ' + cards.length + ' 张 · ' + s.label + '风格 · 点击单张可下载';
      grid.innerHTML = '';
      cards.forEach(function(c, i) {
        var wrap = document.createElement('div');
        wrap.style.cssText = 'text-align: center; cursor: pointer;';

        var canvas = renderCardToCanvas(c, currentStyle);
        canvas.style.cssText = 'width: 100%; height: auto; border-radius: 12px; box-shadow: 0 4px 16px rgba(0,0,0,0.1); transition: transform 0.2s;';
        wrap.appendChild(canvas);

        var label = document.createElement('div');
        label.style.cssText = 'margin-top: ' + (isMobile ? '6px' : '10px') + '; font-size: ' + (isMobile ? '11px' : '13px') + '; color: #595959; line-height: 1.4;';
        label.textContent = '图 ' + (i + 1) + ' / ' + cards.length + (c.type === 'cover' ? ' · 封面' : ' · ' + c.title);
        wrap.appendChild(label);

        wrap.onmouseover = function() { canvas.style.transform = 'translateY(-4px)'; };
        wrap.onmouseout = function() { canvas.style.transform = 'none'; };
        wrap.onclick = function() {
          downloadCanvas(canvas, s.label + '_贴图_' + (i + 1) + '_' + cards.length + '.png');
        };

        grid.appendChild(wrap);
      });
    }

    box.appendChild(grid);
    modal.appendChild(box);
    document.body.appendChild(modal);

    modal.onclick = function(e) {
      if (e.target === modal) modal.remove();
    };
  }

  function confirmDelete(btn) {
    var card = btn.closest('.work-card');
    if (!card) {
      var wrapper = btn.parentElement;
      while (wrapper && wrapper.parentElement && !wrapper.style.background) wrapper = wrapper.parentElement;
      card = wrapper;
    }
    var titleEl = card ? card.querySelector('div[style*="font-weight: 600"]') : null;
    var title = titleEl ? titleEl.textContent.trim() : '该文章';
    if (title.length > 40) title = title.slice(0, 40) + '…';

    var existing = document.getElementById('confirm-modal');
    if (existing) existing.remove();

    var modal = document.createElement('div');
    modal.id = 'confirm-modal';
    modal.style.cssText = 'position: fixed; inset: 0; background: rgba(0,0,0,0.5); z-index: 10000; display: flex; align-items: center; justify-content: center; font-family: -apple-system, BlinkMacSystemFont, sans-serif;';

    var box = document.createElement('div');
    box.style.cssText = 'background: #fff; border-radius: 12px; padding: 28px; width: 380px; max-width: 90vw; box-shadow: 0 8px 32px rgba(0,0,0,0.15);';
    box.innerHTML =
      '<div style="display: flex; align-items: center; gap: 12px; margin-bottom: 12px;">' +
        '<div style="width: 36px; height: 36px; border-radius: 50%; background: #fff1f0; display: flex; align-items: center; justify-content: center; flex-shrink: 0; font-size: 22px; font-weight: 700; color: #ff4d4f; line-height: 1;">!</div>' +
        '<h3 style="margin: 0; font-size: 17px; color: #1a1a1a;">确认删除</h3>' +
      '</div>' +
      '<p style="margin: 0 0 24px; color: #595959; font-size: 14px; line-height: 1.6;">确定要删除「<span style="color: #1a1a1a; font-weight: 600;">' + title + '</span>」吗？删除后无法恢复。</p>' +
      '<div style="display: flex; gap: 12px; justify-content: flex-end;">' +
        '<button id="confirm-cancel" style="padding: 8px 18px; background: #fff; border: 1px solid #d9d9d9; border-radius: 6px; font-size: 14px; color: #262626; cursor: pointer;">取消</button>' +
        '<button id="confirm-ok" style="padding: 8px 18px; background: #ff4d4f; border: none; border-radius: 6px; font-size: 14px; color: #fff; cursor: pointer; font-weight: 600;">删除</button>' +
      '</div>';
    modal.appendChild(box);
    document.body.appendChild(modal);

    document.getElementById('confirm-cancel').onclick = function() { modal.remove(); };
    document.getElementById('confirm-ok').onclick = function() {
      if (card) {
        card.style.transition = 'opacity 0.3s, transform 0.3s';
        card.style.opacity = '0';
        card.style.transform = 'translateX(20px)';
      }
      setTimeout(function() {
        if (card) card.remove();
        modal.remove();
      }, 300);
    };
    modal.onclick = function(e) { if (e.target === modal) modal.remove(); };
  }

  /* ===== AI 标题优化 ===== */

  var titleOptSets = [
    [
      '如何高效管理时间：从混乱到掌控的 5 个方法',
      '时间管理终极指南：5 个技巧让你每天多出 2 小时',
      '别再瞎忙了！这 5 个时间管理方法让你的效率翻倍'
    ],
    [
      '时间不够用？掌握这 5 个方法，告别低效人生',
      '从忙乱到有序：5 个提升效率的时间管理秘诀',
      '为什么你总感觉时间不够？5 个方法帮你找回掌控感'
    ]
  ];
  var titleOptSetIndex = 0;

  var platformTitleData = {
    wechat: [
      '如何高效管理时间：从混乱到掌控的 5 个方法',
      '告别忙乱！这 5 个时间管理术，让你的每一天都井井有条'
    ],
    toutiao: [
      '每天瞎忙却一事无成？这 5 个方法让你效率翻 3 倍',
      '时间管理大师不会告诉你的 5 个秘诀，学会你就赢了'
    ],
    xiaohongshu: [
      '⏰ 拯救拖延症｜5 个超实用时间管理法，亲测有效！',
      '打工人必看✨ 5 个时间管理技巧，让你工作生活两不误'
    ],
    baijiahao: [
      '高效时间管理的 5 个核心方法，助你成为时间的主人',
      '深度解析：为什么时间管理是当代人最稀缺的能力'
    ],
    zhihu: [
      '如何高效地管理时间？5 个经过验证的系统方法',
      '时间管理到底管什么？从认知到实践的 5 个步骤'
    ]
  };

  var platformTabMeta = [
    { key: 'wechat', label: '公众号' },
    { key: 'toutiao', label: '头条' },
    { key: 'xiaohongshu', label: '小红书' },
    { key: 'baijiahao', label: '百家号' },
    { key: 'zhihu', label: '知乎' }
  ];

  function setupTitleOptimizeTriggers() {
    document.querySelectorAll('.preview-title').forEach(function(titleEl) {
      if (titleEl.parentElement.classList.contains('title-wrapper')) return;

      var wrapper = document.createElement('span');
      wrapper.className = 'title-wrapper';
      titleEl.parentElement.insertBefore(wrapper, titleEl);
      wrapper.appendChild(titleEl);

      var trigger = document.createElement('span');
      trigger.className = 'title-optimize-trigger';
      trigger.innerHTML = '&#10047; AI 优化标题';
      trigger.onclick = function(e) {
        e.stopPropagation();
        openTitleOptimize(titleEl);
      };
      wrapper.appendChild(trigger);
    });
  }

  function openTitleOptimize(titleEl) {
    // 清理旧弹窗
    var existing = document.getElementById('title-opt-modal');
    if (existing) existing.remove();

    // 从底部悬浮栏调用时，找到预览页中的标题
    if (!titleEl) {
      titleEl = document.querySelector('#screen-preview .preview-title');
    }
    if (!titleEl) return;

    var originalTitle = titleEl.textContent.trim();
    var isMobile = window.innerWidth < 600;
    var selectedTitle = null;

    // 当前选中平台（默认根据模板推断）
    var currentPlatform = 'wechat';
    var mockup = titleEl.closest('.mockup');
    if (mockup) {
      var preview = mockup.querySelector('.article-preview');
      if (preview) {
        var t = preview.getAttribute('data-template');
        if (t && platformTitleData[t]) currentPlatform = t;
      }
    }

    // ---- 构建弹窗 ----
    var overlay = document.createElement('div');
    overlay.id = 'title-opt-modal';
    overlay.className = 'title-opt-overlay';

    var box = document.createElement('div');
    box.className = 'title-opt-box';

    // 关闭按钮
    var closeBtn = document.createElement('button');
    closeBtn.className = 'title-opt-close';
    closeBtn.innerHTML = '&times;';
    closeBtn.onclick = function() { overlay.remove(); };
    box.appendChild(closeBtn);

    // 标题
    var header = document.createElement('div');
    header.className = 'title-opt-header';
    header.textContent = 'AI 标题优化';
    box.appendChild(header);

    // ---- 上半区：AI 推荐标题 ----
    var sectionAI = document.createElement('div');
    sectionAI.style.marginBottom = '24px';

    // 原标题
    var originalRef = document.createElement('div');
    originalRef.className = 'title-opt-original';
    originalRef.innerHTML = '<span>原标题</span>' + originalTitle;
    sectionAI.appendChild(originalRef);

    // 标签行
    var labelRow = document.createElement('div');
    labelRow.className = 'title-opt-section-label';
    labelRow.innerHTML = '<span>AI 推荐标题</span>';
    var refreshBtn = document.createElement('button');
    refreshBtn.className = 'title-opt-refresh';
    refreshBtn.textContent = '换一批';
    labelRow.appendChild(refreshBtn);
    sectionAI.appendChild(labelRow);

    // 标题列表容器
    var aiList = document.createElement('div');
    sectionAI.appendChild(aiList);

    function renderAITitles(titles, animate) {
      aiList.innerHTML = '';
      titles.forEach(function(t, i) {
        var item = document.createElement('div');
        item.className = 'title-opt-item';
        if (selectedTitle === t) item.classList.add('selected');
        item.innerHTML =
          '<div class="title-opt-radio"></div>' +
          '<div class="title-opt-item-text">' + t + '</div>';
        item.onclick = function() {
          aiList.querySelectorAll('.title-opt-item').forEach(function(el) { el.classList.remove('selected'); });
          platformList.querySelectorAll('.title-opt-item').forEach(function(el) { el.classList.remove('selected'); });
          item.classList.add('selected');
          selectedTitle = t;
          confirmBtn.disabled = false;
        };
        if (animate) {
          item.style.opacity = '0';
          item.style.transform = 'translateY(8px)';
          item.style.transition = 'all 0.3s ease ' + (i * 0.08) + 's';
          aiList.appendChild(item);
          requestAnimationFrame(function() {
            item.style.opacity = '1';
            item.style.transform = 'translateY(0)';
          });
        } else {
          aiList.appendChild(item);
        }
      });
    }

    renderAITitles(titleOptSets[0]);

    // 换一批
    refreshBtn.onclick = function() {
      refreshBtn.disabled = true;
      refreshBtn.textContent = '加载中...';
      setTimeout(function() {
        titleOptSetIndex = (titleOptSetIndex + 1) % titleOptSets.length;
        renderAITitles(titleOptSets[titleOptSetIndex], true);
        refreshBtn.textContent = '换一批';
        refreshBtn.disabled = false;
      }, 600);
    };

    box.appendChild(sectionAI);

    // ---- 下半区：平台适配标题 ----
    var sectionPlatform = document.createElement('div');

    var platformLabel = document.createElement('div');
    platformLabel.className = 'title-opt-section-label';
    platformLabel.innerHTML = '<span>平台适配标题</span>';
    sectionPlatform.appendChild(platformLabel);

    // 平台标签栏
    var tabBar = document.createElement('div');
    tabBar.className = 'title-opt-platform-tabs';
    platformTabMeta.forEach(function(meta) {
      var tab = document.createElement('button');
      tab.className = 'title-opt-platform-tab';
      tab.textContent = meta.label;
      if (meta.key === currentPlatform) tab.classList.add('active');
      tab.onclick = function() {
        currentPlatform = meta.key;
        tabBar.querySelectorAll('.title-opt-platform-tab').forEach(function(b) { b.classList.remove('active'); });
        tab.classList.add('active');
        renderPlatformTitles(currentPlatform);
      };
      tabBar.appendChild(tab);
    });
    sectionPlatform.appendChild(tabBar);

    // 平台标题列表容器
    var platformList = document.createElement('div');
    sectionPlatform.appendChild(platformList);

    function renderPlatformTitles(platform) {
      var titles = platformTitleData[platform] || platformTitleData['wechat'];
      platformList.innerHTML = '';
      titles.forEach(function(t) {
        var item = document.createElement('div');
        item.className = 'title-opt-item';
        if (selectedTitle === t) item.classList.add('selected');
        item.innerHTML =
          '<div class="title-opt-radio"></div>' +
          '<div class="title-opt-item-text">' + t + '</div>';
        item.onclick = function() {
          aiList.querySelectorAll('.title-opt-item').forEach(function(el) { el.classList.remove('selected'); });
          platformList.querySelectorAll('.title-opt-item').forEach(function(el) { el.classList.remove('selected'); });
          item.classList.add('selected');
          selectedTitle = t;
          confirmBtn.disabled = false;
        };
        platformList.appendChild(item);
      });
    }

    renderPlatformTitles(currentPlatform);
    box.appendChild(sectionPlatform);

    // ---- 底部按钮 ----
    var footer = document.createElement('div');
    footer.className = 'title-opt-footer';

    var cancelBtn = document.createElement('button');
    cancelBtn.className = 'title-opt-btn-cancel';
    cancelBtn.textContent = '取消';
    cancelBtn.onclick = function() { overlay.remove(); };
    footer.appendChild(cancelBtn);

    var confirmBtn = document.createElement('button');
    confirmBtn.className = 'title-opt-btn-confirm';
    confirmBtn.textContent = '确认替换';
    confirmBtn.disabled = true;
    confirmBtn.onclick = function() {
      if (!selectedTitle) return;
      // 更新所有可见的 preview-title
      var screen = document.getElementById('screen-preview');
      if (screen) {
        screen.querySelectorAll('.preview-title').forEach(function(el) {
          el.textContent = selectedTitle;
        });
      } else {
        titleEl.textContent = selectedTitle;
      }
      overlay.remove();
    };
    footer.appendChild(confirmBtn);

    box.appendChild(footer);
    overlay.appendChild(box);
    document.body.appendChild(overlay);

    // 点击遮罩关闭
    overlay.addEventListener('click', function(e) {
      if (e.target === overlay) overlay.remove();
    });
  }


  /* ===== 风格库 ===== */
  var systemStylePresets = [
    {
      name: '年度总结',
      desc: '回顾、复盘、展望',
      promptSummary: '语气：回顾性、感恩 + 数据自省\n结构：成绩 + 反思 + 明年目标\n长度：1500-2500 字，带小标题分章',
      prompt: '你是一位资深作者，帮用户写一篇年度总结文章。要求：\n1. 语气回顾性、感恩 + 数据自省\n2. 结构清晰：今年成绩 → 不足反思 → 明年目标\n3. 长度 1500-2500 字\n4. 用 3-4 个小标题分章节\n5. 适度引用数据、时间、具体场景\n6. 结尾给出真诚的展望，不要空洞鸡汤'
    },
    {
      name: '产品评测',
      desc: '客观、数据驱动、多角度对比',
      promptSummary: '语气：客观中立、有理有据\n结构：外观 + 性能 + 体验 + 总结\n要素：必带参数对比表 + 优缺点',
      prompt: '你是一位客观的产品评测人，写一篇产品评测文章。要求：\n1. 语气客观中立、有理有据\n2. 结构：外观设计 → 核心性能 → 实际体验 → 优缺点总结\n3. 必带参数对比表（与 1-2 款竞品对比）\n4. 优点和缺点并列\n5. 给出明确购买建议人群'
    },
    {
      name: '情感散文',
      desc: '细腻、共情、个人化表达',
      promptSummary: '语气：细腻、温暖、第一人称\n修辞：善用比喻、意象、留白\n结构：场景 + 情绪 + 升华',
      prompt: '你是一位细腻的散文家，写一篇情感散文。要求：\n1. 语气细腻、温暖、共情\n2. 大量使用比喻、意象、留白\n3. 第一人称叙述\n4. 结构：具体场景 → 内心情绪 → 情感升华\n5. 段落短而精，留给读者回味空间\n6. 不要说教、不要大道理'
    },
    {
      name: '职场干货',
      desc: '实操性强、结构清晰',
      promptSummary: '语气：专业务实、老板视角\n结构：痛点 + 方案 + 步骤 + 案例\n要素：可执行的 checklist',
      prompt: '你是一位资深职场导师，写一篇职场干货文章。要求：\n1. 语气专业务实、像老板对下属\n2. 结构：行业痛点 → 核心方案 → 具体步骤 → 真实案例\n3. 必带可执行的 checklist\n4. 每节给出可量化的指标或时间\n5. 避免假大空、避免鸡汤'
    },
    {
      name: '知识科普',
      desc: '通俗易懂、有趣案例',
      promptSummary: '语气：老师讲解、循循善诱\n技巧：类比生活化 + 类比图示\n结构：是什么 + 为什么 + 怎么用',
      prompt: '你是一位会讲故事的科普作家。要求：\n1. 把复杂概念用生活类比解释\n2. 像一个好老师在给小白讲课\n3. 结构：是什么 → 为什么 → 怎么用 → 常见误区\n4. 多用具体例子，避免堆砌术语\n5. 适度幽默，但不失专业'
    },
    {
      name: '热点评论',
      desc: '时效性强、观点鲜明',
      promptSummary: '语气：犀利、立场鲜明\n结构：事件复述 + 观点输出 + 深度\n要素：开头一句话亮明立场',
      prompt: '你是一位观点鲜明的评论员，写一篇热点评论。要求：\n1. 开头一句话亮明观点立场\n2. 语气犀利、立场鲜明但不说教\n3. 结构：事件复述 → 核心观点 → 多角度分析 → 结论\n4. 时效性强，引数据 / 引权威观点\n5. 引发读者思考，不要无脑站队'
    },
    {
      name: '故事叙事',
      desc: '沉浸感、有冲突与转折',
      promptSummary: '语气：克制、文学化\n结构：起承转合 + 人物对话\n要素：场景细节 + 心理活动',
      prompt: '你是一位会讲故事的小说家。要求：\n1. 语气克制、文学化\n2. 起承转合清晰：起 → 冲突 → 转折 → 收尾\n3. 必带人物对话、场景细节、心理活动\n4. 节奏张弛有度\n5. 结尾有余味'
    },
    {
      name: '营销转化',
      desc: '引导行动、强说服',
      promptSummary: '语气：紧迫感 + 利益点突出\n结构：痛点共鸣 + 方案 + 案例 + CTA\n要素：必带限时/优惠/倒计时',
      prompt: '你是一位营销高手，写一篇转化型文案。要求：\n1. 开头制造紧迫感 / 共鸣痛点\n2. 突出 3 个核心利益点（数字 + 效果）\n3. 真实用户案例 / 见证\n4. 必带强 CTA（限时 / 优惠 / 倒计时）\n5. 语气坚定、有说服力、不浮夸'
    }
  ];

  var userStylePresets = [
    {
      name: '我的小红书风',
      desc: '轻松活泼、emoji 多、段落短',
      count: 8,
      prompt: '模仿小红书爆款笔记的写法：\n- 标题用 emoji + 数字 + 钩子词\n- 开头 3 句话内必须出爆点\n- 段落不超过 2 行，多用换行\n- 关键句加 emoji 标注\n- 结尾必带 3-5 个 #话题标签\n- 全程"姐妹们冲！"的口吻'
    },
    {
      name: '财经专栏风',
      desc: '专业严谨、数据翔实',
      count: 3,
      prompt: '你是《财新》专栏作家：\n- 引用权威数据源（央行 / 统计局 / 上市公司公告）\n- 段落开头先给结论，再展开论证\n- 关键术语首次出现时给出中英对照\n- 慎用形容词，量化优先\n- 涉及预测时给出置信区间\n- 不预设读者有金融背景，但尊重其智商'
    },
    {
      name: '我的公众号深夜风',
      desc: '走心、克制、留白多',
      count: 5,
      prompt: '你是公众号情感号「夜读」的主笔：\n- 标题克制，不用感叹号\n- 开头 50 字内必须出场景（不说道理）\n- 每段不超过 4 行\n- 多用短句、留白、省略号\n- 不喊口号、不说教\n- 结尾留 1 句未说完的话'
    }
  ];

  function openStyleLibrary() {
    var existing = document.getElementById('style-lib-modal');
    if (existing) existing.remove();

    var currentTab = 'system';
    var selectedStyle = null;

    var isMobile = window.innerWidth < 600;
    var overlay = document.createElement('div');
    overlay.id = 'style-lib-modal';
    overlay.style.cssText = 'position: fixed; inset: 0; background: rgba(0,0,0,0.5); z-index: 10001; display: flex; align-items: center; justify-content: center; padding: 20px; font-family: -apple-system, BlinkMacSystemFont, sans-serif;';

    var box = document.createElement('div');
    box.style.cssText = 'background: #fff; border-radius: 16px; width: 720px; max-width: 100%; max-height: 90vh; overflow-y: auto; box-shadow: 0 8px 32px rgba(0,0,0,0.15); padding: 24px; position: relative;';

    // Close
    var closeBtn = document.createElement('button');
    closeBtn.innerHTML = '&times;';
    closeBtn.style.cssText = 'position: absolute; top: 12px; right: 16px; background: none; border: none; font-size: 24px; cursor: pointer; color: #8c8c8c; line-height: 1; padding: 4px;';
    closeBtn.onclick = function() { overlay.remove(); };
    box.appendChild(closeBtn);

    // Header
    var header = document.createElement('div');
    header.style.cssText = 'font-size: 18px; font-weight: 700; color: #1a1a1a; margin-bottom: 4px; padding-right: 28px;';
    header.textContent = '风格库';
    box.appendChild(header);

    var sub = document.createElement('div');
    sub.style.cssText = 'font-size: 13px; color: #8c8c8c; margin-bottom: 20px;';
    sub.textContent = '选择一套预设风格，让 AI 写出你想要的调性';
    box.appendChild(sub);

    // Tabs
    var tabBar = document.createElement('div');
    tabBar.style.cssText = 'display: flex; gap: 24px; border-bottom: 1px solid #eee; margin-bottom: 20px;';

    var systemTab = document.createElement('div');
    systemTab.textContent = '系统预设';
    systemTab.style.cssText = 'padding: 8px 0; font-size: 14px; font-weight: 600; color: #07c160; border-bottom: 2px solid #07c160; cursor: pointer;';

    var myTab = document.createElement('div');
    myTab.textContent = '我的风格';
    myTab.style.cssText = 'padding: 8px 0; font-size: 14px; font-weight: 500; color: #595959; border-bottom: 2px solid transparent; cursor: pointer;';

    tabBar.appendChild(systemTab);
    tabBar.appendChild(myTab);
    box.appendChild(tabBar);

    // Content area
    var contentArea = document.createElement('div');
    box.appendChild(contentArea);

    function renderSystem() {
      currentTab = 'system';
      systemTab.style.color = '#07c160';
      systemTab.style.fontWeight = '600';
      systemTab.style.borderBottomColor = '#07c160';
      myTab.style.color = '#595959';
      myTab.style.fontWeight = '500';
      myTab.style.borderBottomColor = 'transparent';
      contentArea.innerHTML = '';

      var grid = document.createElement('div');
      grid.className = 'style-lib-grid';
      systemStylePresets.forEach(function(s) {
        var card = document.createElement('div');
        card.className = 'style-lib-card';
        card.innerHTML =
          '<div class="style-lib-card-title">' + s.name + '</div>' +
          '<div class="style-lib-card-desc">' + s.desc + '</div>' +
          '<div class="style-lib-prompt">' + s.promptSummary.replace(/\n/g, '<br>') + '</div>';
        card.onclick = function() {
          grid.querySelectorAll('.style-lib-card').forEach(function(c) { c.classList.remove('selected'); });
          card.classList.add('selected');
          selectedStyle = s.name;
          applyBtn.disabled = false;
          applyBtn.style.background = '#07c160';
          applyBtn.style.cursor = 'pointer';
        };
        grid.appendChild(card);
      });
      contentArea.appendChild(grid);
    }

    function renderMy() {
      currentTab = 'my';
      myTab.style.color = '#07c160';
      myTab.style.fontWeight = '600';
      myTab.style.borderBottomColor = '#07c160';
      systemTab.style.color = '#595959';
      systemTab.style.fontWeight = '500';
      systemTab.style.borderBottomColor = 'transparent';
      contentArea.innerHTML = '';

      var grid = document.createElement('div');
      grid.className = 'style-lib-grid';

      // Add new card
      var addCard = document.createElement('div');
      addCard.className = 'style-lib-add';
      addCard.innerHTML = '<div style="font-size: 24px; margin-bottom: 6px;">+</div><div style="font-size: 13px;">新建我的风格</div>';
      addCard.onclick = function() { renderStyleEditor('create'); };
      grid.appendChild(addCard);

      userStylePresets.forEach(function(s, idx) {
        var card = document.createElement('div');
        card.className = 'style-lib-card style-lib-user-card';
        card.innerHTML =
          '<div class="style-lib-card-title">' + s.name + '</div>' +
          '<div class="style-lib-card-desc">' + s.desc + ' · 已用 ' + s.count + ' 次</div>' +
          '<div class="style-lib-prompt-toggle">查看完整提示词 ▾</div>' +
          '<div class="style-lib-prompt-full" data-idx="' + idx + '">' + s.prompt.replace(/\n/g, '<br>') + '</div>' +
          '<div class="style-lib-prompt-actions" data-idx="' + idx + '">' +
            '<button class="style-lib-action-btn style-lib-edit-btn">编辑提示词</button>' +
            '<button class="style-lib-action-btn style-lib-del-btn">删除</button>' +
          '</div>';
        // Click on the prompt toggle to expand
        var promptToggle = card.querySelector('.style-lib-prompt-toggle');
        var promptFull = card.querySelector('.style-lib-prompt-full');
        var promptActions = card.querySelector('.style-lib-prompt-actions');
        promptToggle.onclick = function(e) {
          e.stopPropagation();
          var isOpen = promptFull.style.display === 'block';
          promptFull.style.display = isOpen ? 'none' : 'block';
          promptActions.style.display = isOpen ? 'none' : 'flex';
          promptToggle.textContent = isOpen ? '查看完整提示词 ▾' : '收起 ▴';
        };
        // Click card to select
        card.onclick = function() {
          grid.querySelectorAll('.style-lib-card').forEach(function(c) { c.classList.remove('selected'); });
          card.classList.add('selected');
          selectedStyle = s.name;
          applyBtn.disabled = false;
          applyBtn.style.background = '#07c160';
          applyBtn.style.cursor = 'pointer';
        };
        // Edit button
        card.querySelector('.style-lib-edit-btn').onclick = function(e) {
          e.stopPropagation();
          renderStyleEditor('edit', idx);
        };
        // Delete button
        card.querySelector('.style-lib-del-btn').onclick = function(e) {
          e.stopPropagation();
          if (confirm('确定要删除「' + s.name + '」吗？')) {
            card.remove();
          }
        };
        grid.appendChild(card);
      });

      contentArea.appendChild(grid);
    }

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

      // Template presets for quick fill
      var templatePresets = [
        { index: 1, title: '产品评测', desc: '客观中立、参数对比' },
        { index: 2, title: '情感散文', desc: '细腻温暖、意象留白' },
        { index: 3, title: '职场干货', desc: '专业务实、可执行 checklist' },
        { index: 7, title: '营销文案', desc: '紧迫感 + 利益点突出' }
      ];

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

      function updateCounter() {
        var len = promptTextarea.value.length;
        promptCounter.textContent = len + ' / 1000';
        var over = len > 1000;
        promptCounter.classList.toggle('over', over);
        saveBtn.disabled = over;
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

      promptTextarea.addEventListener('input', updateCounter);
      updateCounter();

      footer.appendChild(cancelBtn);
      footer.appendChild(saveBtn);
      form.appendChild(footer);

      wrap.appendChild(form);
      contentArea.appendChild(wrap);

      // Auto focus
      if (isCreate || !nameInput.value.trim()) nameInput.focus();
      else promptTextarea.focus();
    }

    systemTab.onclick = renderSystem;
    myTab.onclick = renderMy;
    renderSystem();

    // Footer with apply button
    var footer = document.createElement('div');
    footer.style.cssText = 'display: flex; justify-content: space-between; align-items: center; margin-top: 20px; padding-top: 16px; border-top: 1px solid #f0f0f0;';

    var hint = document.createElement('div');
    hint.style.cssText = 'font-size: 12px; color: #8c8c8c;';
    hint.textContent = '在个人中心可管理所有「我的风格」';
    footer.appendChild(hint);

    var applyBtn = document.createElement('button');
    applyBtn.textContent = '应用此风格';
    applyBtn.disabled = true;
    applyBtn.style.cssText = 'padding: 8px 20px; background: #07c160; color: #fff; border: none; border-radius: 8px; font-size: 14px; font-weight: 600; cursor: pointer;';
    applyBtn.onmouseover = function() { if (!applyBtn.disabled) applyBtn.style.background = '#06ad56'; };
    applyBtn.onmouseout = function() { if (!applyBtn.disabled) applyBtn.style.background = '#07c160'; };
    applyBtn.onclick = function() {
      if (!selectedStyle) return;
      // Populate the pill picker for the currently visible section (PC or mobile)
      var targets = document.querySelectorAll('.style-quick-picks');
      targets.forEach(function(container) {
        var target = container.getAttribute('data-target');
        var customEl = document.getElementById(target + '-style-custom');
        var nameEl = document.getElementById(target + '-style-custom-name');
        if (!customEl || !nameEl) return;
        // Unselect all default pills in this container
        container.querySelectorAll('.style-pill').forEach(function(p) {
          p.style.border = '1px solid #d9d9d9';
          p.style.background = '#fff';
          p.style.color = '#595959';
          p.style.fontWeight = '500';
        });
        // If the picked style matches a default pill, select that one instead of showing custom
        var matchedDefault = false;
        container.querySelectorAll('.style-pill').forEach(function(p) {
          if (p.getAttribute('data-style') === selectedStyle) {
            p.style.border = '1px solid #07c160';
            p.style.background = '#f6ffed';
            p.style.color = '#07c160';
            p.style.fontWeight = '600';
            matchedDefault = true;
          }
        });
        if (matchedDefault) {
          customEl.style.display = 'none';
        } else {
          nameEl.textContent = selectedStyle;
          customEl.style.display = 'inline-flex';
        }
      });
      overlay.remove();
      applyStyleFeedback(selectedStyle);
    };
    footer.appendChild(applyBtn);
    box.appendChild(footer);

    // Update apply button state on selection
    var observer = new MutationObserver(function() {
      applyBtn.disabled = !selectedStyle;
      applyBtn.style.background = selectedStyle ? '#07c160' : '#d9d9d9';
      applyBtn.style.cursor = selectedStyle ? 'pointer' : 'not-allowed';
    });

    overlay.appendChild(box);
    document.body.appendChild(overlay);
    overlay.onclick = function(e) { if (e.target === overlay) overlay.remove(); };

    // Hook selection state to apply button
    var origRenderSystem = renderSystem;
    var origRenderMy = renderMy;
    var origAppendChild = contentArea.appendChild.bind(contentArea);
    contentArea.appendChild = function(node) {
      origAppendChild(node);
      // Listen for clicks on cards
      setTimeout(function() {
        node.querySelectorAll && node.querySelectorAll('.style-lib-card').forEach(function(card) {
          card.addEventListener('click', function() {
            applyBtn.disabled = false;
            applyBtn.style.background = '#07c160';
            applyBtn.style.cursor = 'pointer';
          });
        });
      }, 0);
    };
  }

  // 显示「已应用 X 风格」toast + 更新预览页 meta 行 badge
  function applyStyleFeedback(styleName) {
    // 1) 更新所有可见的 article-style-badge（预览页 meta 行）
    document.querySelectorAll('.article-style-badge').forEach(function(badge) {
      badge.textContent = '风格:' + styleName;
      badge.classList.remove('flash');
      void badge.offsetWidth;
      badge.classList.add('flash');
      setTimeout(function() { badge.classList.remove('flash'); }, 1500);
    });

    // 2) 更新创作页当前选择 chip + 触发脉冲动画
    ['pc-current-style', 'mobile-current-style'].forEach(function(id) {
      var chipDiv = document.getElementById(id);
      if (!chipDiv) return;
      var nameEl = chipDiv.querySelector('span[id$="-current-style-name"]');
      if (nameEl) nameEl.textContent = styleName;
      chipDiv.classList.remove('flash');
      void chipDiv.offsetWidth;
      chipDiv.classList.add('flash');
      setTimeout(function() { chipDiv.classList.remove('flash'); }, 1500);
    });

    // 3) 顶部 toast（应用成功）
    var existing = document.querySelector('.style-apply-toast');
    if (existing) existing.remove();
    var toast = document.createElement('div');
    toast.className = 'style-apply-toast';
    toast.innerHTML = '<span class="toast-check">✓</span><span>应用成功：' + styleName + '</span>';
    document.body.appendChild(toast);
    void toast.offsetWidth;
      toast.classList.add('show');
    setTimeout(function() {
      toast.classList.remove('show');
      setTimeout(function() { toast.remove(); }, 300);
    }, 1800);
  }

  // 12 个导出模板预设（与预览页/移动端 chip 共享）
  var templatePresets = [
    { key: 'wechat', name: '公众号标准模板', platform: 'wechat', desc: '16px 正文 / 18px 小标题 / 绿色强调',
      iconColor: '#07c160', iconChar: '公', previewBg: '#f6ffed', previewBorder: '#d9f7be',
      previewHtml: '<div style="font-weight: 700; color: #07c160; margin-bottom: 3px; font-size: 9px;">文章标题</div><div style="margin-bottom: 2px;">正文正文正文</div><div style="background: #07c160; color: #fff; padding: 1px 4px; border-radius: 2px; display: inline-block;">强调</div>' },
    { key: 'business', name: '简约商务模板', platform: 'general', desc: '14px 正文 / 深蓝标题 / 清晰层级',
      iconColor: '#1677ff', iconChar: '商', previewBg: '#f0f5ff', previewBorder: '#d6e4ff',
      previewHtml: '<div style="font-weight: 700; color: #1677ff; margin-bottom: 3px; font-size: 9px;">文章标题</div><div style="margin-bottom: 2px; color: #595959;">正文正文正文</div><div style="border-left: 2px solid #1677ff; padding-left: 3px; color: #1677ff;">引用</div>' },
    { key: 'marketing', name: '营销转化模板', platform: 'general', desc: '18px 正文 / 红色强调 / 引导行动',
      iconColor: '#cf1322', iconChar: '营', previewBg: '#fff2f0', previewBorder: '#ffccc7',
      previewHtml: '<div style="font-weight: 700; color: #cf1322; margin-bottom: 3px; font-size: 9px;">文章标题</div><div style="margin-bottom: 2px; font-size: 9px;">正文正文正文</div><div style="background: #cf1322; color: #fff; padding: 1px 4px; border-radius: 2px; display: inline-block;">立即查看</div>' },
    { key: 'academic', name: '学术报告模板', platform: 'general', desc: '宋体 / 1.5 倍行距 / 自动编号',
      iconColor: '#595959', iconChar: '学', previewBg: '#fafafa', previewBorder: '#e8e8e8',
      previewHtml: '<div style="font-weight: 700; margin-bottom: 3px; font-size: 9px;">一、标题</div><div style="margin-bottom: 2px;">1.1 正文正文</div><div>1.2 正文正文</div>' },
    { key: 'toutiao', name: '今日头条模板', platform: 'toutiao', desc: '17px 正文 / 橙色强调 / 资讯感标题',
      iconColor: '#ed1c24', iconChar: '头', previewBg: '#fff7e6', previewBorder: '#ffd591',
      previewHtml: '<div style="font-weight: 700; color: #ff6600; margin-bottom: 3px; font-size: 9px;">文章标题</div><div style="margin-bottom: 2px;">正文正文正文</div><div style="border-left: 2px solid #ff6600; padding-left: 3px; color: #ff6600;">重点</div>' },
    { key: 'xiaohongshu', name: '小红书图文模板', platform: 'xiaohongshu', desc: '15px 正文 / 粉红标签 / 轻松活泼',
      iconColor: '#ff2442', iconChar: '红', previewBg: '#fff0f3', previewBorder: '#ffd1d9',
      previewHtml: '<div style="font-weight: 700; color: #ff2442; margin-bottom: 3px; font-size: 9px;">文章标题</div><div style="margin-bottom: 2px;">正文正文</div><div style="background: #ff2442; color: #fff; padding: 1px 4px; border-radius: 8px; display: inline-block;">标签</div>' },
    { key: 'baijiahao', name: '百家号模板', platform: 'baijiahao', desc: '16px 正文 / 蓝色层级 / 信息密度高',
      iconColor: '#1677ff', iconChar: '百', previewBg: '#e6f4ff', previewBorder: '#bae0ff',
      previewHtml: '<div style="font-weight: 700; color: #1677ff; margin-bottom: 3px; font-size: 9px;">文章标题</div><div style="margin-bottom: 2px;">正文正文</div><div style="border-bottom: 1px solid #1677ff; color: #1677ff;">小标题</div>' },
    { key: 'story', name: '故事叙事模板', platform: 'general', desc: '16px 正文 / 暖棕标题 / 沉浸阅读',
      iconColor: '#8b5e34', iconChar: '故', previewBg: '#faf5ef', previewBorder: '#f0e6d8',
      previewHtml: '<div style="font-weight: 700; color: #8b5e34; margin-bottom: 3px; font-size: 9px;">文章标题</div><div style="margin-bottom: 2px;">正文正文</div><div style="border-left: 2px solid #d4a373; padding-left: 3px; color: #8b5e34;">引子</div>' },
    { key: 'magazine', name: '杂志大字模板', platform: 'general', desc: '大标题居中 / 衬线字体 / 留白呼吸',
      iconColor: '#8c8c8c', iconChar: '志', previewBg: '#fafafa', previewBorder: '#e8e8e8',
      previewHtml: '<div style="font-weight: 700; margin-bottom: 4px; font-size: 10px;">标题</div><div style="border-top: 1px solid #d9d9d9; padding-top: 3px; color: #595959;">正文</div>' },
    { key: 'card', name: '卡片分块模板', platform: 'general', desc: '分块卡片 / 阴影层级 / 信息聚焦',
      iconColor: '#07c160', iconChar: '卡', previewBg: '#fff', previewBorder: '#e8e8e8',
      previewHtml: '<div style="background: #07c160; height: 3px; margin-bottom: 4px;"></div><div style="background: #f6ffed; padding: 2px; border-radius: 2px; margin-bottom: 3px;">卡片1</div><div style="background: #f6ffed; padding: 2px; border-radius: 2px;">卡片2</div>' },
    { key: 'checklist', name: '极简清单模板', platform: 'general', desc: '清单体 / 勾选符号 / 行动导向',
      iconColor: '#07c160', iconChar: '清', previewBg: '#fff', previewBorder: '#e8e8e8',
      previewHtml: '<div style="color: #07c160; margin-bottom: 2px;">✓ 事项一</div><div style="color: #07c160; margin-bottom: 2px;">✓ 事项二</div><div style="color: #07c160;">✓ 事项三</div>' },
    { key: 'wechat-minimal', name: '公众号极简模板', platform: 'wechat', desc: '极简排版 / 大量留白 / 呼吸感阅读',
      iconColor: '#07c160', iconChar: '简', previewBg: '#fff', previewBorder: '#e8e8e8',
      previewHtml: '<div style="font-weight: 700; color: #1a1a1a; margin-bottom: 4px; font-size: 9px;">标题</div><div style="margin-bottom: 6px; color: #595959;">正文</div><div style="color: #07c160; font-size: 8px;">— 完 —</div>' },
    { key: 'wechat-dialogue', name: '公众号对话体', platform: 'wechat', desc: '对话气泡 / 左右交替 / 轻松互动',
      iconColor: '#07c160', iconChar: '对', previewBg: '#f6ffed', previewBorder: '#d9f7be',
      previewHtml: '<div style="background: #e8e8e8; padding: 2px 4px; border-radius: 2px; margin-bottom: 2px; font-size: 8px;">A: 你好</div><div style="background: #07c160; color: #fff; padding: 2px 4px; border-radius: 2px; font-size: 8px; text-align: right;">B: 你好</div>' },
    { key: 'wechat-brand', name: '公众号品牌故事', platform: 'wechat', desc: '品牌叙事 / 情感连接 / 视觉统一',
      iconColor: '#07c160', iconChar: '品', previewBg: '#f6ffed', previewBorder: '#d9f7be',
      previewHtml: '<div style="font-weight: 700; color: #07c160; margin-bottom: 3px; font-size: 9px;">品牌故事</div><div style="margin-bottom: 2px; color: #262626;">正文正文</div><div style="border-top: 1px solid #07c160; padding-top: 2px; color: #07c160; font-size: 8px;">关注我们</div>' },
    { key: 'wechat-infographic', name: '公众号信息图', platform: 'wechat', desc: '数据可视化 / 图标丰富 / 信息密度高',
      iconColor: '#07c160', iconChar: '图', previewBg: '#f6ffed', previewBorder: '#d9f7be',
      previewHtml: '<div style="background: #07c160; color: #fff; padding: 2px; border-radius: 2px; margin-bottom: 2px; font-size: 8px; text-align: center;">标题</div><div style="display: flex; gap: 2px;"><div style="flex: 1; background: #d9f7be; height: 12px;"></div><div style="flex: 1; background: #b7eb8f; height: 12px;"></div></div>' },
    { key: 'xiaohongshu-list', name: '小红书清单体', platform: 'xiaohongshu', desc: '清单编号 / 种草标签 / 快速浏览',
      iconColor: '#ff2442', iconChar: '单', previewBg: '#fff0f3', previewBorder: '#ffd1d9',
      previewHtml: '<div style="font-weight: 700; color: #ff2442; margin-bottom: 3px; font-size: 9px;">清单标题</div><div style="margin-bottom: 2px; font-size: 8px;">1. 事项一</div><div style="margin-bottom: 2px; font-size: 8px;">2. 事项二</div><div style="background: #ff2442; color: #fff; padding: 1px 3px; border-radius: 8px; display: inline-block; font-size: 7px;">#标签</div>' },
    { key: 'xiaohongshu-review', name: '小红书测评体', platform: 'xiaohongshu', desc: '评分星级 / 优缺点对比 / 真实体验',
      iconColor: '#ff2442', iconChar: '评', previewBg: '#fff0f3', previewBorder: '#ffd1d9',
      previewHtml: '<div style="font-weight: 700; color: #ff2442; margin-bottom: 2px; font-size: 9px;">测评标题</div><div style="color: #ff6600; margin-bottom: 2px; font-size: 8px;">★★★★☆</div><div style="font-size: 8px;">优点：...</div>' },
    { key: 'xiaohongshu-tutorial', name: '小红书教程步骤', platform: 'xiaohongshu', desc: '步骤编号 / 图文对照 / 操作指南',
      iconColor: '#ff2442', iconChar: '教', previewBg: '#fff0f3', previewBorder: '#ffd1d9',
      previewHtml: '<div style="font-weight: 700; color: #ff2442; margin-bottom: 3px; font-size: 9px;">教程标题</div><div style="background: #ff2442; color: #fff; width: 10px; height: 10px; border-radius: 50%; text-align: center; line-height: 10px; font-size: 7px; margin-bottom: 2px;">1</div><div style="font-size: 8px;">步骤说明</div>' },
    { key: 'xiaohongshu-emotion', name: '小红书情绪共鸣', platform: 'xiaohongshu', desc: '情感表达 / 共鸣话题 / 温暖治愈',
      iconColor: '#ff2442', iconChar: '情', previewBg: '#fff0f3', previewBorder: '#ffd1d9',
      previewHtml: '<div style="font-weight: 700; color: #ff2442; margin-bottom: 3px; font-size: 9px;">情绪标题</div><div style="margin-bottom: 2px; font-size: 8px;">正文正文</div><div style="color: #ff2442; font-size: 8px;">#情绪共鸣</div>' },
    { key: 'toutiao-news', name: '头条资讯快讯', platform: 'toutiao', desc: '快讯格式 / 时间戳 / 资讯感强',
      iconColor: '#ff6600', iconChar: '讯', previewBg: '#fff7e6', previewBorder: '#ffd591',
      previewHtml: '<div style="font-weight: 700; color: #ff6600; margin-bottom: 2px; font-size: 9px;">快讯标题</div><div style="color: #8c8c8c; font-size: 7px; margin-bottom: 2px;">10:30</div><div style="font-size: 8px;">正文正文</div>' },
    { key: 'toutiao-depth', name: '头条深度报道', platform: 'toutiao', desc: '深度分析 / 逻辑清晰 / 数据支撑',
      iconColor: '#ff6600', iconChar: '深', previewBg: '#fff7e6', previewBorder: '#ffd591',
      previewHtml: '<div style="font-weight: 700; color: #ff6600; margin-bottom: 3px; font-size: 9px;">深度标题</div><div style="margin-bottom: 2px; font-size: 8px;">正文正文</div><div style="border-left: 2px solid #ff6600; padding-left: 3px; color: #ff6600; font-size: 8px;">数据</div>' },
    { key: 'toutiao-hot', name: '头条热点评论', platform: 'toutiao', desc: '热点追踪 / 观点鲜明 / 互动引导',
      iconColor: '#ff6600', iconChar: '热', previewBg: '#fff7e6', previewBorder: '#ffd591',
      previewHtml: '<div style="font-weight: 700; color: #ff6600; margin-bottom: 3px; font-size: 9px;">热点标题</div><div style="margin-bottom: 2px; font-size: 8px;">评论正文</div><div style="color: #ff6600; font-size: 8px;">你怎么看？</div>' },
    { key: 'toutiao-qa', name: '头条问答体', platform: 'toutiao', desc: '问答形式 / 标题即问题 / 解答清晰',
      iconColor: '#ff6600', iconChar: '问', previewBg: '#fff7e6', previewBorder: '#ffd591',
      previewHtml: '<div style="font-weight: 700; color: #ff6600; margin-bottom: 3px; font-size: 9px;">Q: 问题？</div><div style="border-left: 2px solid #ff6600; padding-left: 3px; font-size: 8px;">A: 回答正文</div>' },
    { key: 'baijiahao-science', name: '百家号知识科普', platform: 'baijiahao', desc: '科普知识 / 权威引用 / 通俗易懂',
      iconColor: '#1677ff', iconChar: '科', previewBg: '#e6f4ff', previewBorder: '#bae0ff',
      previewHtml: '<div style="font-weight: 700; color: #1677ff; margin-bottom: 3px; font-size: 9px;">科普标题</div><div style="margin-bottom: 2px; font-size: 8px;">正文正文</div><div style="color: #1677ff; font-size: 7px;">来源：...</div>' },
    { key: 'baijiahao-history', name: '百家号历史人文', platform: 'baijiahao', desc: '历史叙事 / 人文情怀 / 时间线清晰',
      iconColor: '#1677ff', iconChar: '史', previewBg: '#e6f4ff', previewBorder: '#bae0ff',
      previewHtml: '<div style="font-weight: 700; color: #1677ff; margin-bottom: 3px; font-size: 9px;">历史标题</div><div style="margin-bottom: 2px; font-size: 8px;">正文正文</div><div style="border-top: 1px solid #1677ff; padding-top: 2px; color: #1677ff; font-size: 7px;">公元前...</div>' },
    { key: 'baijiahao-guide', name: '百家号生活攻略', platform: 'baijiahao', desc: '实用攻略 / 步骤清晰 / 生活场景',
      iconColor: '#1677ff', iconChar: '攻', previewBg: '#e6f4ff', previewBorder: '#bae0ff',
      previewHtml: '<div style="font-weight: 700; color: #1677ff; margin-bottom: 3px; font-size: 9px;">攻略标题</div><div style="margin-bottom: 2px; font-size: 8px;">步骤一</div><div style="background: #1677ff; color: #fff; padding: 1px 3px; border-radius: 2px; display: inline-block; font-size: 7px;">收藏</div>' },
    { key: 'douyin-graphic', name: '抖音图文模板', platform: 'douyin', desc: '竖屏图文 / 大字标题 / 视觉冲击',
      iconColor: '#1a1a1a', iconChar: '抖', previewBg: '#f5f5f5', previewBorder: '#d9d9d9',
      previewHtml: '<div style="font-weight: 700; color: #1a1a1a; margin-bottom: 4px; font-size: 10px;">大字标题</div><div style="background: #1a1a1a; height: 16px; margin-bottom: 2px;"></div><div style="font-size: 8px; color: #595959;">正文</div>' },
    { key: 'douyin-quote', name: '抖音金句海报', platform: 'douyin', desc: '金句突出 / 海报风格 / 分享感强',
      iconColor: '#1a1a1a', iconChar: '金', previewBg: '#f5f5f5', previewBorder: '#d9d9d9',
      previewHtml: '<div style="background: #1a1a1a; color: #fff; padding: 4px; text-align: center; margin-bottom: 2px;"><div style="font-size: 8px; font-weight: 700;">金句内容</div></div><div style="font-size: 7px; color: #8c8c8c; text-align: center;">— 作者</div>' },
    { key: 'zhihu-answer', name: '知乎回答体', platform: 'zhihu', desc: '问答结构 / 逻辑严谨 / 专业深度',
      iconColor: '#0066ff', iconChar: '知', previewBg: '#f0f5ff', previewBorder: '#d6e4ff',
      previewHtml: '<div style="font-weight: 700; color: #0066ff; margin-bottom: 3px; font-size: 9px;">Q: 问题</div><div style="border-left: 2px solid #0066ff; padding-left: 3px; font-size: 8px;">A: 回答正文</div>' },
    { key: 'dark', name: '深色沉浸模板', platform: 'general', desc: '深色背景 / 高对比 / 沉浸阅读',
      iconColor: '#1a1a1a', iconChar: '深', previewBg: '#1a1a1a', previewBorder: '#333',
      previewHtml: '<div style="font-weight: 700; color: #fff; margin-bottom: 3px; font-size: 9px;">标题</div><div style="margin-bottom: 2px; color: #d9d9d9;">正文</div><div style="border-left: 2px solid #07c160; padding-left: 3px; color: #95de64;">重点</div>' }
  ];

  // ===== 自定义模板存储 =====
  var CUSTOM_TEMPLATES_KEY = 'aichuangzuo_custom_templates';
  var SELECTED_TEMPLATE_KEY = 'aichuangzuo_selected_template';

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

  function saveSelectedTemplate(key) {
    try {
      localStorage.setItem(SELECTED_TEMPLATE_KEY, key);
    } catch (e) {}
  }

  function loadSelectedTemplate() {
    try {
      return localStorage.getItem(SELECTED_TEMPLATE_KEY) || 'wechat';
    } catch (e) {
      return 'wechat';
    }
  }

  function applySelectedTemplateToPreview() {
    var key = loadSelectedTemplate();
    document.querySelectorAll('#screen-preview .mockup').forEach(function(mockup) {
      applyTemplateToPreview(mockup, key);
    });
  }

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
      '{title} 真的很有用！{count} 个小技巧，建议姐妹们收藏～',
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

  function applyPlatformDefaults(platformKey, flash) {
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
      if (flash && chip) {
        chip.classList.remove('flash');
        void chip.offsetWidth;
        chip.classList.add('flash');
        setTimeout(function() { chip.classList.remove('flash'); }, 1500);
      }
    });

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

  // 字数设置弹窗的数据与全局状态
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

  // 30 个模板的大预览样式定义（驱动 buildLargePreview 渲染）
  var templateLargeStyles = {
    wechat:    { bg: '#fff', font: '-apple-system, sans-serif',          titleColor: '#1a1a1a', titleSize: '22px', metaColor: '#8c8c8c', metaBorder: '#eee', bodyColor: '#262626', bodySize: '14px', bodyLine: '1.85', headingColor: '#1a1a1a', headingSize: '16px', headingBorder: 'none', headingPl: '0', calloutBg: '#f6ffed', calloutBorder: '4px solid #07c160', calloutColor: '#262626' },
    business:  { bg: '#fff', font: '-apple-system, sans-serif',          titleColor: '#003a8c', titleSize: '22px', metaColor: '#8c8c8c', metaBorder: '#d6e4ff', bodyColor: '#262626', bodySize: '14px', bodyLine: '1.85', headingColor: '#003a8c', headingSize: '16px', headingBorder: '3px solid #003a8c', headingPl: '10px', calloutBg: '#f0f5ff', calloutBorder: '3px solid #1677ff', calloutColor: '#003a8c' },
    marketing: { bg: '#fff', font: '-apple-system, sans-serif',          titleColor: '#cf1322', titleSize: '24px', metaColor: '#8c8c8c', metaBorder: '#ffccc7', bodyColor: '#262626', bodySize: '15px', bodyLine: '1.85', headingColor: '#cf1322', headingSize: '17px', headingBorder: '3px solid #cf1322', headingPl: '10px', calloutVariant: 'cta' },
    academic:  { bg: '#fafaf5', font: 'Georgia, "Songti SC", serif',     titleColor: '#1a1a1a', titleSize: '22px', metaColor: '#8c8c8c', metaBorder: '#d9d9d9', bodyColor: '#262626', bodySize: '14px', bodyLine: '1.9',  headingColor: '#1a1a1a', headingSize: '16px', headingBorder: 'none', headingPl: '0', calloutBg: '#f5f5f0', calloutBorder: 'none', calloutColor: '#595959', numbered: true },
    toutiao:   { bg: '#fff', font: '-apple-system, sans-serif',          titleColor: '#1a1a1a', titleSize: '24px', metaColor: '#8c8c8c', metaBorder: '#eee', bodyColor: '#222',    bodySize: '15px', bodyLine: '1.9',  headingColor: '#ff6600', headingSize: '17px', headingBorder: '4px solid #ff6600', headingPl: '10px', calloutBg: '#fff7e6', calloutBorder: '3px solid #ff6600', calloutColor: '#262626' },
    xiaohongshu:{ bg: '#fff', font: '-apple-system, sans-serif',         titleColor: '#ff2442', titleSize: '22px', metaColor: '#8c8c8c', metaBorder: '#ffd1d9', bodyColor: '#262626', bodySize: '14px', bodyLine: '1.8',  headingColor: '#ff2442', headingSize: '15px', headingBorder: 'none', headingPl: '0', calloutVariant: 'pill' },
    baijiahao: { bg: '#fff', font: '-apple-system, sans-serif',          titleColor: '#1a1a1a', titleSize: '22px', metaColor: '#8c8c8c', metaBorder: '#bae0ff', bodyColor: '#262626', bodySize: '14px', bodyLine: '1.85', headingColor: '#1677ff', headingSize: '16px', headingBorder: 'none', headingPl: '0', headingBorderBottom: '2px solid #1677ff', calloutBg: '#e6f4ff', calloutBorder: '3px solid #1677ff', calloutColor: '#1677ff' },
    story:     { bg: '#faf5ef', font: 'Georgia, "Songti SC", serif',     titleColor: '#8b5e34', titleSize: '22px', metaColor: '#a89378', metaBorder: '#e8dccb', bodyColor: '#3a2e22', bodySize: '15px', bodyLine: '1.95', headingColor: '#8b5e34', headingSize: '16px', headingBorder: '3px solid #d4a373', headingPl: '10px', calloutBg: '#f0e6d8', calloutBorder: 'none', calloutColor: '#8b5e34' },
    magazine:  { bg: '#fafafa', font: 'Georgia, "Songti SC", serif',     titleColor: '#1a1a1a', titleSize: '26px', titleAlign: 'center', metaColor: '#8c8c8c', metaBorder: '#d9d9d9', metaAlign: 'center', bodyColor: '#595959', bodySize: '14px', bodyLine: '1.95', bodyAlign: 'center', headingColor: '#1a1a1a', headingSize: '17px', headingBorder: 'none', headingPl: '0', headingAlign: 'center', calloutBg: '#f5f5f5', calloutBorder: 'none', calloutColor: '#262626' },
    card:      { bg: '#f5f5f5', font: '-apple-system, sans-serif',       titleColor: '#1a1a1a', titleSize: '20px', metaColor: '#8c8c8c', metaBorder: '#e8e8e8', bodyColor: '#262626', bodySize: '13px', bodyLine: '1.7',  headingColor: '#07c160', headingSize: '15px', headingBorder: 'none', headingPl: '0', calloutVariant: 'card' },
    checklist: { bg: '#fff', font: '-apple-system, sans-serif',          titleColor: '#1a1a1a', titleSize: '22px', metaColor: '#8c8c8c', metaBorder: '#eee', bodyColor: '#262626', bodySize: '14px', bodyLine: '1.85', headingColor: '#07c160', headingSize: '16px', headingBorder: 'none', headingPl: '0', calloutVariant: 'checklist' },
    dark:      { bg: '#1a1a1a', font: '-apple-system, sans-serif',       titleColor: '#fff',    titleSize: '22px', metaColor: '#8c8c8c', metaBorder: '#333', bodyColor: '#d9d9d9', bodySize: '14px', bodyLine: '1.85', headingColor: '#95de64', headingSize: '16px', headingBorder: '3px solid #95de64', headingPl: '10px', calloutBg: '#262626', calloutBorder: 'none', calloutColor: '#95de64' },
    'wechat-minimal': { bg: '#fff', font: '-apple-system, sans-serif', titleColor: '#1a1a1a', titleSize: '22px', metaColor: '#8c8c8c', metaBorder: '#eee', bodyColor: '#262626', bodySize: '15px', bodyLine: '1.9', headingColor: '#1a1a1a', headingSize: '15px', headingBorder: 'none', headingPl: '0', calloutBg: '#fafafa', calloutBorder: '1px solid #e8e8e8', calloutColor: '#595959' },
    'wechat-dialogue': { bg: '#fff', font: '-apple-system, sans-serif', titleColor: '#1a1a1a', titleSize: '20px', metaColor: '#8c8c8c', metaBorder: '#eee', bodyColor: '#262626', bodySize: '14px', bodyLine: '1.8', headingColor: '#07c160', headingSize: '14px', headingBorder: 'none', headingPl: '0', calloutVariant: 'pill' },
    'wechat-brand': { bg: '#fff', font: '-apple-system, sans-serif', titleColor: '#07c160', titleSize: '24px', metaColor: '#8c8c8c', metaBorder: '#d9f7be', bodyColor: '#262626', bodySize: '15px', bodyLine: '1.85', headingColor: '#07c160', headingSize: '16px', headingBorder: '4px solid #07c160', headingPl: '10px', calloutBg: '#f6ffed', calloutBorder: '3px solid #07c160', calloutColor: '#07c160' },
    'wechat-infographic': { bg: '#fff', font: '-apple-system, sans-serif', titleColor: '#1a1a1a', titleSize: '22px', metaColor: '#8c8c8c', metaBorder: '#eee', bodyColor: '#262626', bodySize: '14px', bodyLine: '1.75', headingColor: '#07c160', headingSize: '15px', headingBorder: 'none', headingPl: '0', calloutVariant: 'card' },
    'xiaohongshu-list': { bg: '#fff', font: '-apple-system, sans-serif', titleColor: '#ff2442', titleSize: '22px', metaColor: '#8c8c8c', metaBorder: '#ffd1d9', bodyColor: '#262626', bodySize: '14px', bodyLine: '1.8', headingColor: '#ff2442', headingSize: '15px', headingBorder: 'none', headingPl: '0', calloutVariant: 'checklist' },
    'xiaohongshu-review': { bg: '#fff', font: '-apple-system, sans-serif', titleColor: '#ff2442', titleSize: '22px', metaColor: '#8c8c8c', metaBorder: '#ffd1d9', bodyColor: '#262626', bodySize: '14px', bodyLine: '1.8', headingColor: '#ff6600', headingSize: '15px', headingBorder: 'none', headingPl: '0', calloutBg: '#fff7e6', calloutBorder: '3px solid #ff6600', calloutColor: '#262626' },
    'xiaohongshu-tutorial': { bg: '#fff', font: '-apple-system, sans-serif', titleColor: '#ff2442', titleSize: '22px', metaColor: '#8c8c8c', metaBorder: '#ffd1d9', bodyColor: '#262626', bodySize: '14px', bodyLine: '1.8', headingColor: '#ff2442', headingSize: '15px', headingBorder: 'none', headingPl: '0', calloutVariant: 'pill' },
    'xiaohongshu-emotion': { bg: '#fff', font: '-apple-system, sans-serif', titleColor: '#ff2442', titleSize: '22px', metaColor: '#8c8c8c', metaBorder: '#ffd1d9', bodyColor: '#262626', bodySize: '14px', bodyLine: '1.9', headingColor: '#ff2442', headingSize: '15px', headingBorder: 'none', headingPl: '0', calloutBg: '#fff0f3', calloutBorder: '1px solid #ffd1d9', calloutColor: '#262626' },
    'toutiao-news': { bg: '#fff', font: '-apple-system, sans-serif', titleColor: '#1a1a1a', titleSize: '24px', metaColor: '#8c8c8c', metaBorder: '#eee', bodyColor: '#222', bodySize: '15px', bodyLine: '1.9', headingColor: '#ff6600', headingSize: '17px', headingBorder: '4px solid #ff6600', headingPl: '10px', calloutBg: '#fff7e6', calloutBorder: '3px solid #ff6600', calloutColor: '#262626' },
    'toutiao-depth': { bg: '#fff', font: '-apple-system, sans-serif', titleColor: '#1a1a1a', titleSize: '22px', metaColor: '#8c8c8c', metaBorder: '#eee', bodyColor: '#222', bodySize: '15px', bodyLine: '1.85', headingColor: '#1a1a1a', headingSize: '16px', headingBorder: 'none', headingPl: '0', headingBorderBottom: '2px solid #ff6600', calloutBg: '#fff7e6', calloutBorder: '3px solid #ff6600', calloutColor: '#262626' },
    'toutiao-hot': { bg: '#fff', font: '-apple-system, sans-serif', titleColor: '#ff6600', titleSize: '24px', metaColor: '#8c8c8c', metaBorder: '#eee', bodyColor: '#222', bodySize: '15px', bodyLine: '1.9', headingColor: '#ff6600', headingSize: '17px', headingBorder: '4px solid #ff6600', headingPl: '10px', calloutVariant: 'cta' },
    'toutiao-qa': { bg: '#fff', font: '-apple-system, sans-serif', titleColor: '#ff6600', titleSize: '22px', metaColor: '#8c8c8c', metaBorder: '#eee', bodyColor: '#222', bodySize: '15px', bodyLine: '1.8', headingColor: '#ff6600', headingSize: '16px', headingBorder: '4px solid #ff6600', headingPl: '10px', calloutBg: '#fff7e6', calloutBorder: '3px solid #ff6600', calloutColor: '#262626' },
    'baijiahao-science': { bg: '#fff', font: '-apple-system, sans-serif', titleColor: '#1a1a1a', titleSize: '22px', metaColor: '#8c8c8c', metaBorder: '#bae0ff', bodyColor: '#262626', bodySize: '14px', bodyLine: '1.85', headingColor: '#1677ff', headingSize: '16px', headingBorder: '4px solid #1677ff', headingPl: '10px', calloutBg: '#e6f4ff', calloutBorder: '3px solid #1677ff', calloutColor: '#1677ff' },
    'baijiahao-history': { bg: '#fafafa', font: 'Georgia, "Songti SC", serif', titleColor: '#1a1a1a', titleSize: '22px', metaColor: '#8c8c8c', metaBorder: '#d9d9d9', bodyColor: '#262626', bodySize: '15px', bodyLine: '1.9', headingColor: '#1677ff', headingSize: '16px', headingBorder: 'none', headingPl: '0', headingBorderBottom: '1px solid #1677ff', calloutBg: '#e6f4ff', calloutBorder: '3px solid #1677ff', calloutColor: '#1677ff' },
    'baijiahao-guide': { bg: '#fff', font: '-apple-system, sans-serif', titleColor: '#1677ff', titleSize: '22px', metaColor: '#8c8c8c', metaBorder: '#bae0ff', bodyColor: '#262626', bodySize: '14px', bodyLine: '1.75', headingColor: '#1677ff', headingSize: '15px', headingBorder: 'none', headingPl: '0', calloutVariant: 'card' },
    'douyin-graphic': { bg: '#f5f5f5', font: '-apple-system, sans-serif', titleColor: '#1a1a1a', titleSize: '26px', titleAlign: 'center', metaColor: '#8c8c8c', metaBorder: '#d9d9d9', metaAlign: 'center', bodyColor: '#1a1a1a', bodySize: '15px', bodyLine: '1.75', bodyAlign: 'center', headingColor: '#1a1a1a', headingSize: '16px', headingBorder: 'none', headingPl: '0', headingAlign: 'center', calloutBg: '#fff', calloutBorder: '1px solid #d9d9d9', calloutColor: '#1a1a1a' },
    'douyin-quote': { bg: '#f5f5f5', font: '-apple-system, sans-serif', titleColor: '#1a1a1a', titleSize: '24px', titleAlign: 'center', metaColor: '#8c8c8c', metaBorder: '#d9d9d9', metaAlign: 'center', bodyColor: '#1a1a1a', bodySize: '16px', bodyLine: '1.65', bodyAlign: 'center', headingColor: '#1a1a1a', headingSize: '16px', headingBorder: 'none', headingPl: '0', headingAlign: 'center', calloutBg: '#1a1a1a', calloutBorder: 'none', calloutColor: '#fff' },
    'zhihu-answer': { bg: '#fff', font: '-apple-system, sans-serif', titleColor: '#1a1a1a', titleSize: '22px', metaColor: '#8c8c8c', metaBorder: '#eee', bodyColor: '#262626', bodySize: '15px', bodyLine: '1.85', headingColor: '#0066ff', headingSize: '16px', headingBorder: '4px solid #0066ff', headingPl: '10px', calloutBg: '#f0f5ff', calloutBorder: '3px solid #0066ff', calloutColor: '#0066ff' }
  };

  // 渲染单个模板的大预览 HTML（左侧面板内容）
  function buildLargePreview(t) {
    var s = templateLargeStyles[t.key] || templateLargeStyles.wechat;
    var titleAlign = s.titleAlign || 'left';
    var metaAlign  = s.metaAlign  || 'left';
    var bodyAlign  = s.bodyAlign  || 'left';
    var headingAlign = s.headingAlign || 'left';
    var headingPl  = s.headingPl  || '0';
    var headingBorderBottom = s.headingBorderBottom || 'none';

    var calloutHtml;
    if (s.calloutVariant === 'cta') {
      calloutHtml = '<div style="background: #fff2f0; padding: 12px 14px; color: #cf1322; font-size: 13px; line-height: 1.6; border-radius: 6px; margin-top: 14px; display: flex; align-items: center; justify-content: space-between; gap: 8px;"><span><strong style="color:#1a1a1a;">关键结论：</strong>管理注意力，而非塞满日程。</span><span style="background: #cf1322; color: #fff; padding: 5px 12px; border-radius: 4px; font-size: 12px; font-weight: 600; white-space: nowrap;">立即查看</span></div>';
    } else if (s.calloutVariant === 'pill') {
      calloutHtml = '<div style="background: #fff0f3; padding: 12px 14px; color: #262626; font-size: 13px; line-height: 1.6; border-radius: 12px; margin-top: 14px;"><strong>关键结论：</strong>管理注意力 <span style="background: #ff2442; color: #fff; padding: 2px 10px; border-radius: 10px; font-size: 11px; margin-left: 4px;">#时间管理</span> <span style="background: #ff2442; color: #fff; padding: 2px 10px; border-radius: 10px; font-size: 11px; margin-left: 2px;">#干货</span></div>';
    } else if (s.calloutVariant === 'card') {
      calloutHtml = '<div style="background: #fff; padding: 12px 14px; color: #262626; font-size: 13px; line-height: 1.6; border-radius: 8px; margin-top: 14px; box-shadow: 0 2px 8px rgba(0,0,0,0.08); border-left: 3px solid #07c160;"><strong style="color:#07c160;">关键结论：</strong>管理时间本质是管理注意力。</div>';
    } else if (s.calloutVariant === 'checklist') {
      calloutHtml = '<div style="background: #f6ffed; padding: 12px 14px; color: #262626; font-size: 13px; line-height: 1.9; border-radius: 6px; margin-top: 14px;"><div style="color: #07c160; font-weight: 500;">✓ 列出今日最重要的 3 件事</div><div style="color: #07c160; font-weight: 500;">✓ 先完成最难的那一件</div><div style="color: #07c160; font-weight: 500;">✓ 时间块专注单线程</div></div>';
    } else {
      var borderStyle = s.calloutBorder === 'none' ? 'border: none;' : ('border-left: ' + s.calloutBorder + ';');
      calloutHtml = '<div style="background: ' + s.calloutBg + '; ' + borderStyle + ' padding: 12px 14px; color: ' + s.calloutColor + '; font-size: 13px; line-height: 1.6; border-radius: 0 6px 6px 0; margin-top: 14px;"><strong style="color:#1a1a1a;">关键结论：</strong>管理时间本质是管理注意力。</div>';
    }

    var headingStyleExtra = (headingBorderBottom !== 'none') ? ('padding-bottom: 6px; ') : '';
    var headingText = s.numbered ? '一、优先级排序：先做重要的事' : '01｜优先级排序：先做重要的事';

    return '<div style="background: ' + s.bg + '; padding: 24px; height: 100%; box-sizing: border-box; font-family: ' + s.font + '; overflow-y: auto; color: ' + s.bodyColor + ';">' +
      '<h1 style="font-size: ' + s.titleSize + '; font-weight: 700; color: ' + s.titleColor + '; margin: 0 0 12px; line-height: 1.4; text-align: ' + titleAlign + ';">如何高效管理时间</h1>' +
      '<div style="color: ' + s.metaColor + '; font-size: 12px; margin-bottom: 16px; padding-bottom: 10px; border-bottom: 1px solid ' + s.metaBorder + '; text-align: ' + metaAlign + ';">2026-06-22 · 约 1500 字 · 风格:专业严谨</div>' +
      '<p style="font-size: ' + s.bodySize + '; line-height: ' + s.bodyLine + '; color: ' + s.bodyColor + '; margin: 0 0 12px; text-align: ' + bodyAlign + ';">时间对每个人来说都是公平的，但为什么有人能在 24 小时内完成更多事情？关键不在于你有多忙，而在于你如何管理注意力。</p>' +
      '<h3 style="font-size: ' + s.headingSize + '; font-weight: 600; color: ' + s.headingColor + '; border-left: ' + s.headingBorder + '; padding-left: ' + headingPl + '; border-bottom: ' + headingBorderBottom + '; ' + headingStyleExtra + 'margin: 18px 0 8px; text-align: ' + headingAlign + ';">' + headingText + '</h3>' +
      '<p style="font-size: ' + s.bodySize + '; line-height: ' + s.bodyLine + '; color: ' + s.bodyColor + '; margin: 0 0 12px; text-align: ' + bodyAlign + ';">很多人一早打开手机就被消息牵着走。高效的人会在每天开始前列出 3 件最重要的事，并优先完成它们。</p>' +
      calloutHtml +
    '</div>';
  }

  // 打开导出模板库弹窗（左大预览 + 右模板列表，2 列布局）
  function openTemplateLibrary() {
    var existing = document.getElementById('template-lib-modal');
    if (existing) existing.remove();

    var selectedTemplate = templatePresets[0];

    var platformTabs = [
      { key: 'all', label: '全部' },
      { key: 'wechat', label: '公众号' },
      { key: 'xiaohongshu', label: '小红书' },
      { key: 'toutiao', label: '今日头条' },
      { key: 'baijiahao', label: '百家号' },
      { key: 'zhihu', label: '知乎' },
      { key: 'douyin', label: '抖音图文' },
      { key: 'general', label: '通用风格' },
      { key: 'custom', label: '我的模板' }
    ];
    var selectedPlatform = 'all';

    var runtimeTemplates = getRuntimeTemplates();
    var selectedTemplate = runtimeTemplates[0];

    var overlay = document.createElement('div');
    overlay.id = 'template-lib-modal';
    overlay.style.cssText = 'position: fixed; inset: 0; background: rgba(0,0,0,0.5); z-index: 10001; display: flex; align-items: center; justify-content: center; padding: 20px; font-family: -apple-system, BlinkMacSystemFont, sans-serif;';

    var box = document.createElement('div');
    box.style.cssText = 'background: #fff; border-radius: 16px; width: 960px; max-width: 100%; max-height: 90vh; display: flex; flex-direction: column; box-shadow: 0 8px 32px rgba(0,0,0,0.2); position: relative; overflow: hidden;';

    // Close button (absolute, top-right)
    var closeBtn = document.createElement('button');
    closeBtn.innerHTML = '&times;';
    closeBtn.style.cssText = 'position: absolute; top: 10px; right: 14px; background: none; border: none; font-size: 22px; cursor: pointer; color: #8c8c8c; line-height: 1; padding: 4px 8px; z-index: 2;';
    closeBtn.onclick = function() { overlay.remove(); };
    box.appendChild(closeBtn);

    // Header
    var headerWrap = document.createElement('div');
    headerWrap.style.cssText = 'padding: 22px 24px 14px; flex-shrink: 0;';
    var header = document.createElement('div');
    header.style.cssText = 'font-size: 18px; font-weight: 700; color: #1a1a1a; margin-bottom: 4px; padding-right: 28px;';
    header.textContent = '导出模板库';
    var sub = document.createElement('div');
    sub.style.cssText = 'font-size: 13px; color: #8c8c8c;';
    sub.textContent = '共 ' + runtimeTemplates.length + ' 个模板 · 左侧实时预览 · 右侧选择模板';
    headerWrap.appendChild(header);
    headerWrap.appendChild(sub);
    box.appendChild(headerWrap);

    // Platform tab bar
    var tabBar = document.createElement('div');
    tabBar.style.cssText = 'display: flex; gap: 8px; padding: 0 24px 14px; flex-shrink: 0; overflow-x: auto; border-bottom: 1px solid #f0f0f0;';
    box.appendChild(tabBar);

    // 2-column body
    var body = document.createElement('div');
    body.style.cssText = 'display: flex; gap: 16px; padding: 0 24px; flex: 1; min-height: 0;';

    // Left: large preview pane
    var previewPane = document.createElement('div');
    previewPane.style.cssText = 'flex: 0 0 420px; background: #f5f5f5; border-radius: 10px; overflow: hidden; height: 480px; box-shadow: inset 0 0 0 1px rgba(0,0,0,0.05); position: relative;';
    body.appendChild(previewPane);

    // Right: list pane
    var listPane = document.createElement('div');
    listPane.style.cssText = 'flex: 1; min-width: 0; height: 480px; overflow-y: auto; padding-right: 4px;';
    body.appendChild(listPane);

    function updatePreview(tpl) {
      previewPane.innerHTML = buildLargePreview(tpl);
    }

    function selectInList(tpl, row) {
      listPane.querySelectorAll('.template-lib-row').forEach(function(r) {
        r.classList.remove('selected');
        r.style.background = '#fff';
        r.style.borderColor = '#e8e8e8';
        r.style.boxShadow = 'none';
      });
      row.classList.add('selected');
      row.style.background = '#f6ffed';
      row.style.borderColor = '#07c160';
      row.style.boxShadow = '0 0 0 2px rgba(7,193,96,0.25)';
      selectedTemplate = tpl;
      updatePreview(tpl);
      applyBtn.disabled = false;
      applyBtn.style.background = '#07c160';
      applyBtn.style.cursor = 'pointer';
    }

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

    function renderList() {
      listPane.innerHTML = '';
      var filtered = selectedPlatform === 'all'
        ? runtimeTemplates
        : selectedPlatform === 'custom'
          ? runtimeTemplates.filter(function(t) { return t.isCustom; })
          : runtimeTemplates.filter(function(t) { return t.platform === selectedPlatform; });

      if (selectedPlatform === 'custom') {
        var createBtn = document.createElement('button');
        createBtn.textContent = '+ 创建自定义模板';
        createBtn.style.cssText = 'width: 100%; padding: 10px; margin-bottom: 12px; background: #f6ffed; color: #07c160; border: 1px dashed #b7eb8f; border-radius: 8px; cursor: pointer; font-size: 14px; font-weight: 500;';
        createBtn.onmouseover = function() { createBtn.style.background = '#d9f7be'; };
        createBtn.onmouseout = function() { createBtn.style.background = '#f6ffed'; };
        createBtn.onclick = function() {
          overlay.remove();
          openCustomTemplateEditor();
        };
        listPane.appendChild(createBtn);

        if (filtered.length === 0) {
          var empty = document.createElement('div');
          empty.style.cssText = 'text-align: center; padding: 40px 20px; color: #8c8c8c; font-size: 14px;';
          empty.innerHTML = '<div style="margin-bottom: 12px;">还没有自定义模板</div><div style="font-size: 12px;">点击上方按钮创建你的第一个模板</div>';
          listPane.appendChild(empty);
          return;
        }
      }

      filtered.forEach(function(t, idx) {
        var row = document.createElement('div');
        row.className = 'template-lib-row';
        row.style.cssText = 'border: 1px solid #e8e8e8; border-radius: 8px; padding: 10px 12px; margin-bottom: 8px; cursor: pointer; background: #fff; display: flex; gap: 10px; align-items: center; transition: all 0.15s; box-sizing: border-box; position: relative;';
        var badge = t.isCustom ? '<span style="position: absolute; top: -6px; left: -6px; background: #07c160; color: #fff; font-size: 10px; padding: 1px 5px; border-radius: 8px; z-index: 1;">我</span>' : '';
        row.innerHTML =
          badge +
          '<svg width="22" height="22" viewBox="0 0 16 16" style="flex-shrink: 0;"><rect width="16" height="16" rx="4" fill="' + t.iconColor + '"/><text x="8" y="12" text-anchor="middle" fill="#fff" font-size="10" font-weight="bold" font-family="sans-serif">' + t.iconChar + '</text></svg>' +
          '<div style="flex: 1; min-width: 0;">' +
            '<div style="font-weight: 600; color: #1a1a1a; font-size: 13px; margin-bottom: 2px;">' + t.name + '</div>' +
            '<div style="font-size: 11px; color: #8c8c8c; line-height: 1.4;">' + t.desc + '</div>' +
          '</div>';
        row.onmouseover = function() { if (!row.classList.contains('selected')) { row.style.borderColor = '#07c160'; row.style.boxShadow = '0 2px 8px rgba(7,193,96,0.1)'; } };
        row.onmouseout = function() { if (!row.classList.contains('selected')) { row.style.borderColor = '#e8e8e8'; row.style.boxShadow = 'none'; } };
        row.onclick = function() { selectInList(t, row); };
        listPane.appendChild(row);

        if (t.isCustom) {
          var actions = document.createElement('div');
          actions.style.cssText = 'display: none; gap: 6px; position: absolute; top: 8px; right: 8px;';
          actions.innerHTML = '<button class="tpl-edit-btn" style="padding: 2px 8px; background: #fff; border: 1px solid #d9d9d9; border-radius: 4px; font-size: 11px; color: #595959; cursor: pointer;">编辑</button><button class="tpl-delete-btn" style="padding: 2px 8px; background: #fff; border: 1px solid #ffccc7; border-radius: 4px; font-size: 11px; color: #cf1322; cursor: pointer;">删除</button>';
          row.onmouseenter = function() { if (!row.classList.contains('selected')) { row.style.borderColor = '#07c160'; row.style.boxShadow = '0 2px 8px rgba(7,193,96,0.1)'; } actions.style.display = 'flex'; };
          row.onmouseleave = function() { if (!row.classList.contains('selected')) { row.style.borderColor = '#e8e8e8'; row.style.boxShadow = 'none'; } actions.style.display = 'none'; };
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
              runtimeTemplates = getRuntimeTemplates();
              renderList();
            }
          };
        }

        if (idx === 0 && selectedPlatform !== 'custom') {
          row.classList.add('selected');
          row.style.background = '#f6ffed';
          row.style.borderColor = '#07c160';
          row.style.boxShadow = '0 0 0 2px rgba(7,193,96,0.25)';
          selectedTemplate = t;
          updatePreview(t);
        }
      });
    }

    renderTabs();
    renderList();

    box.appendChild(body);

    // Footer
    var footer = document.createElement('div');
    footer.style.cssText = 'display: flex; justify-content: space-between; align-items: center; padding: 16px 24px 22px; flex-shrink: 0; border-top: 1px solid #f0f0f0; margin-top: 16px;';

    var hint = document.createElement('div');
    hint.style.cssText = 'font-size: 12px; color: #8c8c8c;';
    hint.textContent = '选择后预览页将以选定模板渲染';
    footer.appendChild(hint);

    var applyBtn = document.createElement('button');
    applyBtn.textContent = '应用此模板';
    applyBtn.disabled = false; // first row is auto-selected
    applyBtn.style.cssText = 'padding: 8px 20px; background: #07c160; color: #fff; border: none; border-radius: 8px; font-size: 14px; font-weight: 600; cursor: pointer;';
    applyBtn.onmouseover = function() { if (!applyBtn.disabled) applyBtn.style.background = '#06ad56'; };
    applyBtn.onmouseout = function() { if (!applyBtn.disabled) applyBtn.style.background = '#07c160'; };
    applyBtn.onclick = function() {
      if (!selectedTemplate) return;
      applyTemplateFeedback(selectedTemplate);
      overlay.remove();
    };
    footer.appendChild(applyBtn);
    box.appendChild(footer);

    overlay.appendChild(box);
    document.body.appendChild(overlay);
    overlay.onclick = function(e) { if (e.target === overlay) overlay.remove(); };
  }

  // 打开自定义模板编辑器（创建/编辑）
  function openCustomTemplateEditor(customId) {
    var existing = document.getElementById('custom-template-editor-modal');
    if (existing) existing.remove();

    var isEdit = !!customId;
    var custom = isEdit ? getCustomTemplateById(customId) : null;
    var state = {
      baseKey: custom ? custom.baseKey : templatePresets[0].key,
      name: custom ? custom.name : '',
      overrides: custom ? Object.assign({}, custom.overrides) : { theme: 'brand', titleStyle: 'left', highlightStyle: 'border', useCards: false }
    };

    var overlay = document.createElement('div');
    overlay.id = 'custom-template-editor-modal';
    overlay.style.cssText = 'position: fixed; inset: 0; background: rgba(0,0,0,0.5); z-index: 10002; display: flex; align-items: center; justify-content: center; padding: 20px; font-family: -apple-system, BlinkMacSystemFont, sans-serif;';

    var box = document.createElement('div');
    box.style.cssText = 'background: #fff; border-radius: 16px; width: 960px; max-width: 100%; max-height: 90vh; display: flex; flex-direction: column; box-shadow: 0 8px 32px rgba(0,0,0,0.2); position: relative; overflow: hidden;';

    var closeBtn = document.createElement('button');
    closeBtn.innerHTML = '&times;';
    closeBtn.style.cssText = 'position: absolute; top: 10px; right: 14px; background: none; border: none; font-size: 22px; cursor: pointer; color: #8c8c8c; line-height: 1; padding: 4px 8px; z-index: 2;';
    closeBtn.onclick = function() { overlay.remove(); };
    box.appendChild(closeBtn);

    var headerWrap = document.createElement('div');
    headerWrap.style.cssText = 'padding: 22px 24px 14px; flex-shrink: 0;';
    var header = document.createElement('div');
    header.style.cssText = 'font-size: 18px; font-weight: 700; color: #1a1a1a; margin-bottom: 4px; padding-right: 28px;';
    header.textContent = isEdit ? '编辑自定义模板' : '创建自定义模板';
    var sub = document.createElement('div');
    sub.style.cssText = 'font-size: 13px; color: #8c8c8c;';
    sub.textContent = '基于现有模板，通过可视化选项快速定制你的专属风格';
    headerWrap.appendChild(header);
    headerWrap.appendChild(sub);
    box.appendChild(headerWrap);

    var body = document.createElement('div');
    body.style.cssText = 'display: flex; gap: 16px; padding: 0 24px; flex: 1; min-height: 0;';

    var previewPane = document.createElement('div');
    previewPane.style.cssText = 'flex: 0 0 55%; background: #f5f5f5; border-radius: 10px; overflow: hidden; min-height: 0; box-shadow: inset 0 0 0 1px rgba(0,0,0,0.05); position: relative;';
    body.appendChild(previewPane);

    var configPane = document.createElement('div');
    configPane.style.cssText = 'flex: 1; min-width: 0; overflow-y: auto; padding-right: 4px;';
    body.appendChild(configPane);

    function buildSamplePreview() {
      var base = templatePresets.find(function(p) { return p.key === state.baseKey; }) || templatePresets[0];
      var wrap = document.createElement('div');
      wrap.className = 'mockup';
      wrap.style.cssText = 'height: 100%; overflow-y: auto; padding: 24px; box-sizing: border-box;';
      var inner = document.createElement('div');
      inner.className = 'mockup-body';
      inner.style.cssText = 'background: #fff; border-radius: 8px; padding: 24px; min-height: 100%; box-sizing: border-box;';
      var article = document.createElement('div');
      article.className = 'article-preview';
      article.setAttribute('data-template', state.baseKey);
      article.innerHTML =
        '<h1 class="preview-title" style="margin-bottom: 16px; line-height: 1.4;">文章标题示例</h1>' +
        '<p style="margin-bottom: 16px;">这是一段正文示例，用来预览模板效果。你可以通过右侧选项调整颜色、标题样式和段落卡片。</p>' +
        '<h3 class="preview-heading" style="margin: 24px 0 12px;">01｜小标题示例</h3>' +
        '<p style="margin-bottom: 16px;">这是另一段正文，展示模板的段落和行距效果。</p>' +
        '<div class="preview-highlight" style="margin: 20px 0; padding: 16px; border-radius: 0 8px 8px 0;">这是重点高亮块的示例。</div>' +
        '<p style="margin-bottom: 16px;">最后一段正文，帮助你判断整体阅读体验。</p>';
      inner.appendChild(article);
      wrap.appendChild(inner);
      previewPane.innerHTML = '';
      previewPane.appendChild(wrap);
      applyTemplateToPreview(wrap, isEdit ? customId : state.baseKey);
      if (!isEdit) {
        applyTemplateOverrides(wrap, state.overrides);
      }
    }

    function renderConfig() {
      configPane.innerHTML = '';

      function addLabel(text) {
        var label = document.createElement('div');
        label.style.cssText = 'font-weight: 600; color: #1a1a1a; font-size: 14px; margin-bottom: 10px;';
        label.textContent = text;
        configPane.appendChild(label);
      }

      function addSpacer() {
        var spacer = document.createElement('div');
        spacer.style.cssText = 'height: 20px;';
        configPane.appendChild(spacer);
      }

      addLabel('基于模板');
      var baseSelect = document.createElement('select');
      baseSelect.style.cssText = 'width: 100%; padding: 8px 12px; border: 1px solid #d9d9d9; border-radius: 6px; font-size: 14px; margin-bottom: 4px; color: #262626;';
      templatePresets.forEach(function(p) {
        var opt = document.createElement('option');
        opt.value = p.key;
        opt.textContent = p.name;
        if (p.key === state.baseKey) opt.selected = true;
        baseSelect.appendChild(opt);
      });
      baseSelect.onchange = function() {
        state.baseKey = baseSelect.value;
        buildSamplePreview();
      };
      configPane.appendChild(baseSelect);
      addSpacer();

      addLabel('配色主题');
      var themeWrap = document.createElement('div');
      themeWrap.style.cssText = 'display: flex; gap: 10px; flex-wrap: wrap; margin-bottom: 4px;';
      var themeOptions = [
        { key: 'brand', color: '#07c160', name: '品牌绿' },
        { key: 'blue', color: '#1677ff', name: '商务蓝' },
        { key: 'red', color: '#cf1322', name: '营销红' },
        { key: 'gray', color: '#595959', name: '学术灰' },
        { key: 'pink', color: '#ff2442', name: '小红书粉' },
        { key: 'orange', color: '#ff6600', name: '头条橙' }
      ];
      themeOptions.forEach(function(opt) {
        var btn = document.createElement('button');
        btn.className = 'custom-theme-btn';
        var active = state.overrides.theme === opt.key;
        btn.title = opt.name;
        btn.style.cssText = 'width: 32px; height: 32px; border-radius: 50%; border: 3px solid ' + (active ? '#1a1a1a' : 'transparent') + '; background: ' + opt.color + '; cursor: pointer; box-shadow: 0 0 0 1px #d9d9d9;';
        btn.onclick = function() {
          state.overrides.theme = opt.key;
          renderConfig();
          buildSamplePreview();
        };
        themeWrap.appendChild(btn);
      });
      configPane.appendChild(themeWrap);
      addSpacer();

      addLabel('标题样式');
      var titleWrap = document.createElement('div');
      titleWrap.style.cssText = 'display: flex; gap: 10px; margin-bottom: 4px;';
      var titleOptions = [
        { key: 'left', label: '左对齐' },
        { key: 'center', label: '居中' },
        { key: 'underline', label: '下划线' }
      ];
      titleOptions.forEach(function(opt) {
        var btn = document.createElement('button');
        btn.className = 'custom-title-btn';
        var active = state.overrides.titleStyle === opt.key;
        btn.textContent = opt.label;
        btn.style.cssText = 'flex: 1; padding: 8px; border: 1px solid ' + (active ? '#07c160' : '#d9d9d9') + '; background: ' + (active ? '#f6ffed' : '#fff') + '; color: ' + (active ? '#07c160' : '#595959') + '; border-radius: 6px; cursor: pointer; font-size: 13px;';
        btn.onclick = function() {
          state.overrides.titleStyle = opt.key;
          renderConfig();
          buildSamplePreview();
        };
        titleWrap.appendChild(btn);
      });
      configPane.appendChild(titleWrap);
      addSpacer();

      addLabel('重点高亮');
      var highlightWrap = document.createElement('div');
      highlightWrap.style.cssText = 'display: flex; gap: 10px; margin-bottom: 4px;';
      var highlightOptions = [
        { key: 'border', label: '左边框' },
        { key: 'background', label: '背景块' },
        { key: 'quote', label: '引用体' }
      ];
      highlightOptions.forEach(function(opt) {
        var btn = document.createElement('button');
        btn.className = 'custom-highlight-btn';
        var active = state.overrides.highlightStyle === opt.key;
        btn.textContent = opt.label;
        btn.style.cssText = 'flex: 1; padding: 8px; border: 1px solid ' + (active ? '#07c160' : '#d9d9d9') + '; background: ' + (active ? '#f6ffed' : '#fff') + '; color: ' + (active ? '#07c160' : '#595959') + '; border-radius: 6px; cursor: pointer; font-size: 13px;';
        btn.onclick = function() {
          state.overrides.highlightStyle = opt.key;
          renderConfig();
          buildSamplePreview();
        };
        highlightWrap.appendChild(btn);
      });
      configPane.appendChild(highlightWrap);
      addSpacer();

      addLabel('段落卡片');
      var cardWrap = document.createElement('div');
      cardWrap.style.cssText = 'display: flex; align-items: center; gap: 10px; margin-bottom: 4px;';
      var cardToggle = document.createElement('button');
      cardToggle.id = 'custom-card-toggle';
      cardToggle.style.cssText = 'width: 44px; height: 24px; border-radius: 12px; border: none; cursor: pointer; position: relative; transition: background 0.2s; background: ' + (state.overrides.useCards ? '#07c160' : '#d9d9d9') + ';';
      cardToggle.innerHTML = '<span style="position: absolute; top: 2px; left: ' + (state.overrides.useCards ? '22px' : '2px') + '; width: 20px; height: 20px; border-radius: 50%; background: #fff; transition: left 0.2s;"></span>';
      cardToggle.onclick = function() {
        state.overrides.useCards = !state.overrides.useCards;
        renderConfig();
        buildSamplePreview();
      };
      var cardLabel = document.createElement('span');
      cardLabel.style.cssText = 'font-size: 13px; color: #595959;';
      cardLabel.textContent = state.overrides.useCards ? '已开启' : '已关闭';
      cardWrap.appendChild(cardToggle);
      cardWrap.appendChild(cardLabel);
      configPane.appendChild(cardWrap);
      addSpacer();

      addLabel('模板名称');
      var nameInput = document.createElement('input');
      nameInput.id = 'custom-template-name';
      nameInput.type = 'text';
      nameInput.value = state.name;
      nameInput.placeholder = '给你的模板起个名字';
      nameInput.style.cssText = 'width: 100%; padding: 8px 12px; border: 1px solid #d9d9d9; border-radius: 6px; font-size: 14px; box-sizing: border-box; color: #262626;';
      nameInput.oninput = function() {
        state.name = nameInput.value.slice(0, 20);
      };
      configPane.appendChild(nameInput);
      addSpacer();
    }

    renderConfig();
    buildSamplePreview();

    box.appendChild(body);

    var footer = document.createElement('div');
    footer.style.cssText = 'display: flex; justify-content: flex-end; align-items: center; gap: 10px; padding: 16px 24px 22px; flex-shrink: 0; border-top: 1px solid #f0f0f0; margin-top: 16px;';

    var cancelBtn = document.createElement('button');
    cancelBtn.textContent = '取消';
    cancelBtn.style.cssText = 'padding: 8px 18px; border-radius: 8px; border: 1px solid #d9d9d9; background: #fff; color: #595959; cursor: pointer; font-size: 14px;';
    cancelBtn.onclick = function() { overlay.remove(); };
    footer.appendChild(cancelBtn);

    var saveBtn = document.createElement('button');
    saveBtn.id = 'custom-template-save';
    saveBtn.textContent = isEdit ? '保存' : '创建';
    saveBtn.style.cssText = 'padding: 8px 18px; border-radius: 8px; border: 1px solid #07c160; background: #07c160; color: #fff; cursor: pointer; font-size: 14px; font-weight: 600;';
    saveBtn.onclick = function() {
      if (!state.name.trim()) {
        showToast('请输入模板名称');
        return;
      }
      var data = {
        name: state.name.trim(),
        baseKey: state.baseKey,
        overrides: state.overrides
      };
      if (isEdit) {
        updateCustomTemplate(customId, data);
      } else {
        createCustomTemplate(data);
      }
      overlay.remove();
    };
    footer.appendChild(saveBtn);

    box.appendChild(footer);
    overlay.appendChild(box);
    overlay.onclick = function(e) { if (e.target === overlay) overlay.remove(); };
    document.body.appendChild(overlay);
  }

  // 打开字数设置弹窗（按平台推荐 / 按内容场景 / 按字数档位 / 自定义字数）
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
    confirmBtn.onclick = function () {
      currentWordCount = state.selectedCount;
      currentWordLabel = state.selectedLabel;
      // 新版创作页使用 label 元素展示当前字数
      ['pc-current-word-count-label', 'mobile-current-word-count-label'].forEach(function (id) {
        var el = document.getElementById(id);
        if (el) el.textContent = currentWordCount + ' 字 · ' + currentWordLabel;
      });
      // 兼容旧版 trigger 按钮
      ['pc-word-count-trigger', 'mobile-word-count-trigger'].forEach(function (id) {
        var el = document.getElementById(id);
        if (el) el.textContent = '📝 ' + currentWordCount + ' 字 · ' + currentWordLabel + ' ✏️';
      });
      overlay.remove();
    };
    footer.appendChild(cancelBtn);
    footer.appendChild(confirmBtn);
    box.appendChild(footer);

    overlay.appendChild(box);

    var state = {
      activeTab: initialTab,
      selectedPlatform: defaultPlatform && validPlatforms.indexOf(defaultPlatform) >= 0 ? defaultPlatform : 'wechat',
      selectedCount: currentWordCount,
      selectedLabel: currentWordLabel,
      customValue: currentWordCount
    };

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

    renderTabBar();
    renderContent();
    document.body.appendChild(overlay);
  }

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
      applyPlatformDefaults(selectedKey, true);
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

  // 显示「已应用 X 模板」toast + 更新创作页 chip
  function applyTemplateFeedback(tpl, skipFlash) {
    ['pc-current-template', 'mobile-current-template'].forEach(function(id) {
      var chipDiv = document.getElementById(id);
      if (!chipDiv) return;
      var nameEl = chipDiv.querySelector('span[id$="-current-template-name"]');
      if (nameEl) nameEl.textContent = tpl.name;
      if (!skipFlash) {
        chipDiv.classList.remove('flash');
        void chipDiv.offsetWidth;
        chipDiv.classList.add('flash');
        setTimeout(function() { chipDiv.classList.remove('flash'); }, 1500);
      }
    });

    saveSelectedTemplate(tpl.key);

    // 如果在预览页，同步更新主预览区样式和模板卡片选中状态
    var previewScreen = document.getElementById('screen-preview');
    if (previewScreen && previewScreen.offsetParent !== null) {
      previewScreen.querySelectorAll('.mockup').forEach(function(mockup) {
        applyTemplateToPreview(mockup, tpl.key);
        var firstCard = mockup.querySelector('.template-card');
        if (firstCard) {
          var container = firstCard.parentElement;
          var isPill = firstCard.style.borderRadius === '20px';
          container.querySelectorAll('.template-card').forEach(function(c) {
            var selected = c.getAttribute('data-template') === tpl.key;
            if (isPill) {
              c.style.borderColor = selected ? '#07c160' : '#d9d9d9';
              c.style.borderWidth = selected ? '2px' : '1px';
              c.style.background = selected ? '#f6ffed' : '#fff';
              c.style.color = selected ? '#07c160' : '#595959';
              c.style.fontWeight = selected ? '600' : '500';
            } else {
              c.style.borderColor = selected ? '#07c160' : '#e8e8e8';
              c.style.borderWidth = selected ? '2px' : '1px';
            }
          });
        }
      });
    }

    var existing = document.querySelector('.style-apply-toast');
    if (existing) existing.remove();
    var toast = document.createElement('div');
    toast.className = 'style-apply-toast';
    toast.innerHTML = '<span class="toast-check">✓</span><span>已应用模板：' + tpl.name + '</span>';
    document.body.appendChild(toast);
    void toast.offsetWidth;
    toast.classList.add('show');
    setTimeout(function() {
      toast.classList.remove('show');
      setTimeout(function() { toast.remove(); }, 300);
    }, 1800);
  }

  // 标题优化触发已移至底部悬浮栏

  // 文章风格 pill 选择器
  function selectStylePill(pillEl, styleName) {
    // Find this pill's container (PC or mobile)
    var container = pillEl.parentElement;
    if (!container) return;
    // Unselect all default pills + hide custom
    container.querySelectorAll('.style-pill').forEach(function(p) {
      p.style.border = '1px solid #d9d9d9';
      p.style.background = '#fff';
      p.style.color = '#595959';
      p.style.fontWeight = '500';
    });
    var customEl = container.querySelector('.style-pill-custom');
    if (customEl) customEl.style.display = 'none';
    // Highlight this pill
    pillEl.style.border = '1px solid #07c160';
    pillEl.style.background = '#f6ffed';
    pillEl.style.color = '#07c160';
    pillEl.style.fontWeight = '600';
    // 如果在预览页，实时同步 badge
    var previewScreen = document.getElementById('screen-preview');
    if (previewScreen && previewScreen.offsetParent !== null) {
      applyStyleFeedback(styleName);
    }
  }

  function clearStylePill(target) {
    var container = document.querySelector('.style-quick-picks[data-target="' + target + '"]');
    if (!container) return;
    var customEl = document.getElementById(target + '-style-custom');
    if (customEl) {
      customEl.style.display = 'none';
      var nameEl = document.getElementById(target + '-style-custom-name');
      if (nameEl) nameEl.textContent = '';
    }
    // Reset to default first pill (专业严谨)
    var firstPill = container.querySelector('.style-pill[data-style="专业严谨"]');
    if (firstPill) selectStylePill(firstPill, '专业严谨');
  }

  // ===== 生成队列 =====
  var GENERATION_QUEUE_KEY = 'aichuangzuo_generation_queue';
  var MAX_CONCURRENT = 1;

  function generateId() {
    return 'gen_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
  }

  function getGenerationQueue() {
    try {
      var data = localStorage.getItem(GENERATION_QUEUE_KEY);
      return data ? JSON.parse(data) : [];
    } catch (e) {
      return [];
    }
  }

  function saveGenerationQueue(queue) {
    try {
      localStorage.setItem(GENERATION_QUEUE_KEY, JSON.stringify(queue));
    } catch (e) {}
  }

  function getQueueStats() {
    var queue = getGenerationQueue();
    var generating = queue.filter(function(t) { return t.status === 'generating'; }).length;
    var queued = queue.filter(function(t) { return t.status === 'queued'; }).length;
    var completed = queue.filter(function(t) { return t.status === 'completed'; }).length;
    return { total: queue.length, generating: generating, queued: queued, completed: completed };
  }

  function getQueueStatusStyle(status) {
    if (status === 'completed') return { color: '#07c160', bg: '#f6ffed' };
    if (status === 'generating') return { color: '#1677ff', bg: '#e6f4ff' };
    return { color: '#fa8c16', bg: '#fff7e6' };
  }

  function formatQueueTime(isoString) {
    if (!isoString) return '';
    var date = new Date(isoString);
    var now = new Date();
    var diff = Math.floor((now - date) / 1000);
    if (diff < 60) return '刚刚';
    if (diff < 3600) return Math.floor(diff / 60) + ' 分钟前';
    if (diff < 86400) return Math.floor(diff / 3600) + ' 小时前';
    var pad = function(n) { return n < 10 ? '0' + n : n; };
    return pad(date.getMonth() + 1) + '-' + pad(date.getDate()) + ' ' + pad(date.getHours()) + ':' + pad(date.getMinutes());
  }

  function getQueueTimeLabel(task) {
    if (task.status === 'completed' && task.completedAt) return formatQueueTime(task.completedAt) + ' 完成';
    if (task.status === 'generating' && task.startedAt) return formatQueueTime(task.startedAt) + ' 开始生成';
    return formatQueueTime(task.createdAt) + ' 提交';
  }

  function submitGenerationTask(taskData) {
    var queue = getGenerationQueue();
    var task = {
      id: generateId(),
      title: taskData.title || '未命名文章',
      topic: taskData.topic || '',
      wordCount: taskData.wordCount || (typeof currentWordCount !== 'undefined' ? currentWordCount : 1500),
      style: taskData.style || '年度总结',
      template: taskData.template || '公众号标准模板',
      status: 'queued',
      progress: 0,
      createdAt: new Date().toISOString(),
      startedAt: null,
      completedAt: null
    };

    var generatingCount = queue.filter(function(t) { return t.status === 'generating'; }).length;
    if (generatingCount < MAX_CONCURRENT) {
      task.status = 'generating';
      task.startedAt = new Date().toISOString();
    }

    queue.push(task);
    saveGenerationQueue(queue);
    updateGenerationBadge();
    renderGenerationQueue();
    showGenerationToast('已加入生成队列', 'info');
    startQueueConsumer();
    return task;
  }

  function cancelGenerationTask(taskId) {
    var queue = getGenerationQueue();
    var task = queue.find(function(t) { return t.id === taskId; });
    if (!task) return;
    if (task.status === 'generating') {
      showGenerationToast('生成中的任务无法取消', 'error');
      return;
    }
    queue = queue.filter(function(t) { return t.id !== taskId; });
    saveGenerationQueue(queue);
    updateGenerationBadge();
    renderGenerationQueue();
    renderWorksQueueItems();
    showGenerationToast('已取消排队', 'info');
  }

  function startQueueConsumer() {
    if (window.__generationQueueConsumer) return;
    window.__generationQueueConsumer = setInterval(function() {
      processGenerationQueue();
    }, 500);
  }

  function processGenerationQueue() {
    var queue = getGenerationQueue();
    var changed = false;

    queue.forEach(function(task) {
      if (task.status === 'generating') {
        task.progress += Math.random() * 15 + 5;
        if (task.progress >= 100) {
          task.progress = 100;
          task.status = 'completed';
          task.completedAt = new Date().toISOString();
          changed = true;
          showGenerationToast('《' + task.title + '》生成完成', 'success');
        } else {
          changed = true;
        }
      }
    });

    var generatingCount = queue.filter(function(t) { return t.status === 'generating'; }).length;
    if (generatingCount < MAX_CONCURRENT) {
      var nextTask = queue.find(function(t) { return t.status === 'queued'; });
      if (nextTask) {
        nextTask.status = 'generating';
        nextTask.startedAt = new Date().toISOString();
        changed = true;
      }
    }

    if (changed) {
      saveGenerationQueue(queue);
      updateGenerationBadge();
      renderGenerationQueue();
      renderWorksQueueItems();
    }
  }

  function updateGenerationBadge() {
    var stats = getQueueStats();
    var count = stats.generating + stats.queued;
    document.querySelectorAll('.generation-badge').forEach(function(el) {
      el.textContent = count;
      el.style.display = count > 0 ? 'flex' : 'none';
    });
    updateGenerationFabVisibility();
  }

  function updateGenerationFabVisibility() {
    var fab = document.getElementById('generation-queue-fab');
    var panel = document.getElementById('generation-queue-panel');
    if (!fab) return;
    var stats = getQueueStats();
    var hasTasks = stats.total > 0;
    var panelVisible = panel && panel.style.display !== 'none';
    fab.style.display = (hasTasks && !panelVisible) ? 'flex' : 'none';
  }

  function showGenerationToast(message, type) {
    var existing = document.getElementById('generation-toast');
    if (existing) existing.remove();

    var bg = type === 'success' ? '#07c160' : type === 'error' ? '#ff4d4f' : '#1a1a1a';
    var toast = document.createElement('div');
    toast.id = 'generation-toast';
    toast.style.cssText = 'position: fixed; top: 24px; left: 50%; transform: translateX(-50%) translateY(-20px); background: ' + bg + '; color: #fff; padding: 10px 20px; border-radius: 24px; font-size: 14px; font-weight: 500; z-index: 10003; opacity: 0; transition: all 0.25s ease; box-shadow: 0 6px 24px rgba(0,0,0,0.2); display: inline-flex; align-items: center; gap: 8px; pointer-events: none; max-width: 80%;';
    toast.textContent = message;
    document.body.appendChild(toast);

    requestAnimationFrame(function() {
      toast.style.opacity = '1';
      toast.style.transform = 'translateX(-50%) translateY(0)';
    });

    setTimeout(function() {
      toast.style.opacity = '0';
      toast.style.transform = 'translateX(-50%) translateY(-20px)';
      setTimeout(function() {
        toast.remove();
      }, 250);
    }, 3000);
  }

  function renderGenerationQueue() {
    var panel = document.getElementById('generation-queue-panel');
    if (!panel) return;
    var list = panel.querySelector('.generation-queue-list');
    if (!list) return;

    var queue = getGenerationQueue();
    list.innerHTML = '';

    if (queue.length === 0) {
      list.innerHTML = '<div style="text-align:center;color:#8c8c8c;font-size:13px;padding:24px 0;">暂无生成任务</div>';
      return;
    }

    var displayQueue = queue.slice().reverse().slice(0, 5);
    displayQueue.forEach(function(task) {
      list.appendChild(createQueueItem(task));
    });

    if (queue.length > 5) {
      var moreHint = document.createElement('div');
      moreHint.style.cssText = 'text-align:center;padding:12px 0;font-size:13px;color:#8c8c8c;border-bottom:1px solid #f0f0f0;';
      moreHint.textContent = '还有 ' + (queue.length - 5) + ' 个任务未显示';
      list.appendChild(moreHint);
    }
  }

  function createQueueItem(task) {
    var el = document.createElement('div');
    el.style.cssText = 'padding: 12px; border-bottom: 1px solid #f0f0f0;';

    var statusStyle = getQueueStatusStyle(task.status);
    var statusText = task.status === 'completed' ? '已完成' : task.status === 'generating' ? '生成中' : '排队中';

    var progressHtml = '';
    if (task.status === 'generating') {
      progressHtml = '<div style="margin-top:8px;height:4px;background:#f0f0f0;border-radius:2px;overflow:hidden;"><div style="height:100%;background:#1677ff;width:' + Math.round(task.progress) + '%;transition:width 0.3s ease;"></div></div>';
    }

    var actionHtml = '';
    if (task.status === 'queued') {
      actionHtml = '<button onclick="cancelGenerationTask(\'' + task.id + '\')" style="background:none;border:none;color:#8c8c8c;font-size:12px;cursor:pointer;padding:2px 4px;">取消</button>';
    } else if (task.status === 'completed') {
      actionHtml = '<button onclick="location.href=\'preview.html?id=' + task.id + '\'" style="background:none;border:none;color:#07c160;font-size:12px;cursor:pointer;padding:2px 4px;">预览</button>';
    }

    el.innerHTML =
      '<div style="display:flex;justify-content:space-between;align-items:flex-start;gap:8px;">' +
        '<div style="flex:1;min-width:0;">' +
          '<div style="font-size:14px;font-weight:600;color:#1a1a1a;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">' + task.title + '</div>' +
          '<div style="display:flex;align-items:center;gap:8px;margin-top:6px;">' +
            '<span style="display:inline-block;padding:2px 8px;border-radius:4px;font-size:11px;font-weight:500;background:' + statusStyle.bg + ';color:' + statusStyle.color + ';">' + statusText + '</span>' +
            '<span style="font-size:12px;color:#8c8c8c;">' + task.wordCount + ' 字 · ' + getQueueTimeLabel(task) + '</span>' +
          '</div>' +
        '</div>' +
        '<div>' + actionHtml + '</div>' +
      '</div>' +
      progressHtml;

    return el;
  }

  function renderWorksQueueItems() {
    ['works-queue-items', 'mobile-works-queue-items'].forEach(function(containerId) {
      var container = document.getElementById(containerId);
      if (!container) return;

      var queue = getGenerationQueue().filter(function(t) {
        return t.status === 'queued' || t.status === 'generating';
      });

      container.innerHTML = '';

      queue.forEach(function(task) {
        var isMobile = containerId.indexOf('mobile') >= 0;
        var el = document.createElement('div');
        el.style.cssText = isMobile
          ? 'background: #fff; border-radius: 12px; padding: 16px; box-shadow: 0 2px 12px rgba(0,0,0,0.05); margin-bottom: 12px;'
          : 'background: #fff; border-radius: 12px; padding: 20px; box-shadow: 0 2px 12px rgba(0,0,0,0.05); margin-bottom: 16px; display: flex; justify-content: space-between; align-items: center;';

        var statusStyle = getQueueStatusStyle(task.status);
        var statusText = task.status === 'generating' ? '生成中' : '排队中';

        var progressHtml = '';
        if (task.status === 'generating') {
          progressHtml = '<div style="margin-top:8px;height:6px;background:#f0f0f0;border-radius:3px;overflow:hidden;' + (isMobile ? '' : 'max-width:300px;') + '"><div style="height:100%;background:#1677ff;width:' + Math.round(task.progress) + '%;transition:width 0.3s ease;"></div></div>';
        }

        var actionHtml = '';
        if (task.status === 'queued') {
          actionHtml = '<button onclick="cancelGenerationTask(\'' + task.id + '\')" style="' + (isMobile ? 'flex: 1; padding: 8px; background: #fff; border: 1px solid #d9d9d9; border-radius: 6px; font-size: 13px; color: #ff4d4f; cursor: pointer;' : 'padding: 8px 14px; background: #fff; border: 1px solid #d9d9d9; border-radius: 6px; font-size: 13px; color: #ff4d4f; cursor: pointer;') + '">取消排队</button>';
        } else {
          actionHtml = '<span style="color: #8c8c8c; font-size: 13px;">请稍候…</span>';
        }

        if (isMobile) {
          el.innerHTML =
            '<div style="font-weight: 600; font-size: 15px; margin-bottom: 6px; color: #1a1a1a;">' + task.title + '</div>' +
            '<div style="display:flex;align-items:center;gap:10px;margin-bottom:6px;">' +
              '<span style="display:inline-block;padding:2px 8px;border-radius:4px;font-size:12px;font-weight:500;background:' + statusStyle.bg + ';color:' + statusStyle.color + ';">' + statusText + '</span>' +
              '<span style="color: #8c8c8c; font-size: 12px;">' + task.wordCount + ' 字 · ' + task.style + ' · ' + getQueueTimeLabel(task) + '</span>' +
            '</div>' +
            progressHtml +
            '<div style="display: flex; gap: 8px; margin-top: 12px;">' + actionHtml + '</div>';
        } else {
          el.innerHTML =
            '<div>' +
              '<div style="font-weight: 600; font-size: 16px; margin-bottom: 6px; color: #1a1a1a;">' + task.title + '</div>' +
              '<div style="display:flex;align-items:center;gap:10px;margin-bottom:6px;">' +
                '<span style="display:inline-block;padding:2px 8px;border-radius:4px;font-size:12px;font-weight:500;background:' + statusStyle.bg + ';color:' + statusStyle.color + ';">' + statusText + '</span>' +
                '<span style="color: #8c8c8c; font-size: 13px;">' + task.wordCount + ' 字 · ' + task.style + ' · ' + getQueueTimeLabel(task) + '</span>' +
              '</div>' +
              progressHtml +
            '</div>' +
            '<div>' + actionHtml + '</div>';
        }

        container.appendChild(el);
      });
    });
  }

  function openGenerationQueue() {
    var panel = document.getElementById('generation-queue-panel');
    if (panel) {
      panel.style.display = 'block';
      renderGenerationQueue();
    }
    updateGenerationFabVisibility();
  }

  function closeGenerationQueue() {
    var panel = document.getElementById('generation-queue-panel');
    if (panel) panel.style.display = 'none';
    updateGenerationFabVisibility();
  }

  function toggleGenerationQueue() {
    var panel = document.getElementById('generation-queue-panel');
    if (!panel) return;
    var isVisible = panel.style.display !== 'none';
    if (isVisible) closeGenerationQueue();
    else openGenerationQueue();
  }

  function collectCreatePageData() {
    var isPc = !!document.getElementById('pc-mode-topic');
    var prefix = isPc ? 'pc' : 'mobile';

    var selectedTopic = document.querySelector('#' + prefix + '-topic-list .topic-card.selected');
    var title = '';
    if (selectedTopic) {
      title = selectedTopic.querySelector('div').textContent.trim();
    } else {
      var customTextarea = document.querySelector('#' + prefix + '-mode-custom textarea');
      if (customTextarea && customTextarea.value.trim()) {
        var text = customTextarea.value.trim();
        title = text.substring(0, 20) + (text.length > 20 ? '…' : '');
      }
    }

    if (!title) title = '未命名文章';

    var wordCount = (typeof currentWordCount !== 'undefined') ? currentWordCount : 1500;
    var styleNameEl = document.getElementById(prefix + '-current-style-name');
    var styleName = styleNameEl ? styleNameEl.textContent.trim() : '年度总结';
    var templateNameEl = document.getElementById(prefix + '-current-template-name');
    var templateName = templateNameEl ? templateNameEl.textContent.trim() : '公众号标准模板';

    return { title: title, wordCount: wordCount, style: styleName, template: templateName };
  }

  // 跨标签页同步
  window.addEventListener('storage', function(e) {
    if (e.key === GENERATION_QUEUE_KEY) {
      updateGenerationBadge();
      renderGenerationQueue();
      renderWorksQueueItems();
    }
  });

  // 页面加载时初始化
  document.addEventListener('DOMContentLoaded', function() {
    initDemoQueueData();
    updateGenerationBadge();
    startQueueConsumer();
    renderGenerationQueue();
    renderWorksQueueItems();
  });

  function initDemoQueueData() {
    var queue = getGenerationQueue();
    if (queue.length > 0) return;

    var now = new Date();
    var demoTasks = [
      {
        id: 'gen_demo_generating',
        title: '2026 年 AI 写作行业趋势深度分析',
        topic: 'AI 写作',
        wordCount: 2500,
        style: '年度总结',
        template: '公众号标准模板',
        status: 'generating',
        progress: 62,
        createdAt: new Date(now - 120000).toISOString(),
        startedAt: new Date(now - 60000).toISOString(),
        completedAt: null
      },
      {
        id: 'gen_demo_queued_1',
        title: '月薪 5000 如何一年存下 3 万？',
        topic: '生活技巧',
        wordCount: 1500,
        style: '实用清单',
        template: '小红书爆款模板',
        status: 'queued',
        progress: 0,
        createdAt: new Date(now - 90000).toISOString(),
        startedAt: null,
        completedAt: null
      },
      {
        id: 'gen_demo_queued_2',
        title: '30 岁后才明白：真正成熟的人，都懂得边界感',
        topic: '情感成长',
        wordCount: 1800,
        style: '情感故事',
        template: '公众号标准模板',
        status: 'queued',
        progress: 0,
        createdAt: new Date(now - 60000).toISOString(),
        startedAt: null,
        completedAt: null
      },
      {
        id: 'gen_demo_completed_1',
        title: '工作 3 年没升职？可能是这 3 个习惯在拖后腿',
        topic: '职场效率',
        wordCount: 2000,
        style: '职场干货',
        template: '公众号标准模板',
        status: 'completed',
        progress: 100,
        createdAt: new Date(now - 600000).toISOString(),
        startedAt: new Date(now - 580000).toISOString(),
        completedAt: new Date(now - 560000).toISOString()
      },
      {
        id: 'gen_demo_completed_2',
        title: '我用 AI 写作月入过万：新手可复制的 5 个步骤',
        topic: 'AI 副业',
        wordCount: 1600,
        style: '副业经验',
        template: '百家号标准模板',
        status: 'completed',
        progress: 100,
        createdAt: new Date(now - 900000).toISOString(),
        startedAt: new Date(now - 880000).toISOString(),
        completedAt: new Date(now - 860000).toISOString()
      }
    ];

    saveGenerationQueue(demoTasks);
  }
