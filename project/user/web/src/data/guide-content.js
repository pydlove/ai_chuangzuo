export const guideSections = [
  {
    id: 'what',
    title: '平台能干嘛',
    articles: [
      {
        id: 'what-intro',
        title: '爱创作是做什么的',
        content: `
          <p>爱创作是一款 AI 自媒体写作助手。</p>
          <p>你只需要输入一个写作方向，AI 会在 3 分钟内生成一篇结构完整、语言通顺、适配多平台的自媒体文章。</p>
          <p>不管是公众号长文、小红书笔记、今日头条文章，还是抖音图文、百家号内容，都可以一次生成，复制到对应平台直接发布。</p>
        `
      },
      {
        id: 'what-output',
        title: '3 分钟能写出什么',
        content: `
          <p>一次生成可得：</p>
          <ul>
            <li>高打开率标题（3-5 个备选）</li>
            <li>带钩子开头的正文</li>
            <li>分段清晰的结构</li>
            <li>金句结尾与 CTA</li>
            <li>适配目标平台的排版与字数</li>
          </ul>
          <p>生成后可直接导出 Word，或复制正文到公众号、小红书、抖音、今日头条、百家号等平台发布。</p>
        `
      },
      {
        id: 'what-platforms',
        title: '支持哪些平台',
        content: `
          <p>目前已适配主流自媒体平台：</p>
          <ul>
            <li>微信公众号</li>
            <li>小红书</li>
            <li>今日头条</li>
            <li>百家号</li>
            <li>知乎</li>
            <li>抖音图文</li>
          </ul>
          <p>每个平台都有对应的模板和排版规则，生成时一键切换。</p>
        `
      }
    ]
  },
  {
    id: 'money',
    title: '能赚多少钱',
    articles: [
      {
        id: 'money-platform',
        title: '平台内收益：创作币、排行榜、邀请',
        content: `
          <p>在爱创作，你可以通过以下方式获得创作币：</p>
          <ul>
            <li><strong>风格市场</strong>：发布自己的写作风格，被其他用户使用时获得收益。</li>
            <li><strong>里程碑奖励</strong>：完成创作任务获得一次性奖励。</li>
            <li><strong>收益排行榜</strong>：每月创作币榜或自媒体收入榜进入前 10，获得 100 创作币奖励。</li>
            <li><strong>邀请返利</strong>：邀请好友注册并订阅，获得返利。</li>
          </ul>
          <p>1 创作币 = 1 元人民币，满 100 创作币可申请提现。</p>
        `
      },
      {
        id: 'money-external',
        title: '外部自媒体变现：流量主、商单、带货',
        content: `
          <p>爱创作帮你把内容生产时间从 3 小时压缩到 3 分钟，省下来的时间可以用来运营账号、接商单、做流量主。</p>
          <ul>
            <li><strong>公众号</strong>：流量主广告分成 + 商务合作。</li>
            <li><strong>小红书</strong>：笔记带货 + 品牌商单。</li>
            <li><strong>抖音</strong>：中视频计划 + 橱窗带货 + 星图商单。</li>
            <li><strong>今日头条/百家号</strong>：广告分成。</li>
          </ul>
          <p>多发、多发平台、持续优化标题，是提升外部收入的关键。</p>
        `
      },
      {
        id: 'money-calculator',
        title: '时间节省计算器',
        component: 'TimeCalculator'
      }
    ]
  },
  {
    id: 'how',
    title: '怎么赚',
    articles: [
      {
        id: 'how-step1',
        title: '第一步：生成第一篇内容',
        content: `
          <p>点击「开始创作」，输入你的写作方向。</p>
          <p>可以是热点观点、产品测评、经验分享、情感故事等。AI 会根据你的方向生成完整文章。</p>
        `
      },
      {
        id: 'how-step2',
        title: '第二步：选择发布平台',
        content: `
          <p>根据内容选择最适合的平台。</p>
          <p>小红书适合短图文和情绪化表达，公众号适合深度长文，今日头条和百家号适合资讯类内容。</p>
        `
      },
      {
        id: 'how-step3',
        title: '第三步：多平台分发',
        content: `
          <p>同一篇内容可以改写后分发到多个平台，最大化流量价值。</p>
          <p>爱创作支持一次生成多平台版本，也可以导出后微调标题和开头再发布。</p>
        `
      },
      {
        id: 'how-step4',
        title: '第四步：申报收入/冲击榜单',
        content: `
          <p>发布内容后，回到爱创作申报你的自媒体收入。</p>
          <p>申报审核通过后，金额会累加进「自媒体收入榜」，每月前 10 名可获得 100 创作币奖励。</p>
        `
      },
      {
        id: 'how-leaderboard',
        title: '本月创作币榜',
        component: 'LeaderboardPreview'
      }
    ]
  },
  {
    id: 'withdraw',
    title: '怎么提现',
    articles: [
      {
        id: 'withdraw-coin',
        title: '创作币是什么',
        content: `
          <p>创作币是爱创作平台的虚拟货币。</p>
          <p>你可以通过风格市场、排行榜、邀请返利等方式获得创作币，也可以在平台内消费（如订阅会员、购买生成额度）。</p>
          <p>1 创作币 = 1 元人民币。</p>
        `
      },
      {
        id: 'withdraw-settle',
        title: '如何结算收益',
        content: `
          <p>收益按自然周（周一至周日）统计，每周一可手动结算上周收益。</p>
          <p>点击「结算上周」后，未结算收益会立即转入账户余额。</p>
        `
      },
      {
        id: 'withdraw-rule',
        title: '提现门槛与到账说明',
        content: `
          <p>账户余额满 100 创作币可申请提现到支付宝。</p>
          <p>未结算收益不可提现，结算前请确认收益明细无误。</p>
          <p>提现申请提交后，通常在 1-3 个工作日内到账。</p>
        `
      }
    ]
  }
]
