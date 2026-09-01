/** 玩法指南页（PC + Mobile）页面级文案配置
 *
 * 修改此文件即可调整 http://localhost:22345/guide 页面的 Hero、CTA 等文案。
 * 顶部导航与 CTA 已抽到 siteConfig.js，所有落地页共用。
 * 具体章节内容见下方 guideSections。
 */

/** Hero 区域（PC / Mobile 文案略有差异） */
export const guideHero = {
  pc: {
    title: '玩法指南',
    desc: '爱创作工坊专为「不会做、没时间、难坚持」的人打造：3 分钟了解我们如何帮你把自媒体做起来。'
  },
  mobile: {
    badge: '玩法指南',
    title: '3 分钟了解爱创作工坊',
    desc: '不会做、没时间、难坚持，我们帮你把自媒体做起来'
  }
}

/** 收益方式 banner */
export const guideMoneyBanner = {
  src: 'https://foruda.gitee.com/images/1784102377496111264/6e108169_8060302.png',
  alt: '收益方式：把内容变成持续收入'
}

/** 页面底部 CTA */
export const guideCta = {
  title: '准备好开始了吗？',
  desc: '每天 3 分钟，把内容变成账号流量和收入。',
  btn: {
    text: '立即开始创作',
    to: '/console/workbench'
  }
}

/** 品牌名 */
export const guideBrand = '爱创作工坊'

/** 具体章节内容 */
export const guideSections = [
  {
    id: 'fit',
    title: '适合谁',
    articles: [
      {
        id: 'fit-intro',
        title: '我们的平台适合谁',
        content: `
          <p>如果你符合下面任意一种情况，爱创作工坊就是为你准备的：</p>
          <ul>
            <li>✅ 想做自媒体，但不知道写什么、怎么写</li>
            <li>✅ 有表达欲，但工作太忙，没空日更</li>
            <li>✅ 坚持不下去，发几篇就断更</li>
            <li>✅ 想多平台分发，但一篇稿子要改半天</li>
            <li>✅ 会写文案，但标题和开头总是没人点</li>
          </ul>
          <p>你不用先成为写作高手，只需要输入一个想法，剩下的交给 AI。</p>
        `
      }
    ]
  },
  {
    id: 'what',
    title: '产品功能',
    articles: [
      {
        id: 'what-intro',
        title: '爱创作工坊是做什么的',
        content: `
          <p>爱创作工坊是面向普通人的「自媒体运营流水线」，把「不知道写什么、不知道怎么发、不知道怎么变现」变成一套可执行的 AI 辅助流程。</p>
          <p>不只是帮你生成一篇文章：第一次使用会先制定自媒体方案——选平台、定目标、选赛道、做人设；日常使用时，工作台会基于你的方案推荐选题和差异化角度，再注入你的个人素材生成内容，最后给出发布策略和运营复盘建议。</p>
          <p>核心差异是「先定位，再创作」：通过低粉高赞数据 + 蓝海细分赛道，帮你减少同质化风险，做出有个人印记、能持续变现的自媒体账号。</p>
        `
      },
      {
        id: 'what-output',
        title: '一次创作能得到什么',
        content: `
          <p>基于你的自媒体方案和个人素材，一次完整创作流程会产出：</p>
          <ul>
            <li><strong>差异化选题与切入角度</strong>：不是追热点，而是基于低粉高赞数据推荐蓝海方向</li>
            <li><strong>3-5 个高打开率标题备选</strong></li>
            <li><strong>注入个人素材的正文</strong>：带钩子开头、清晰结构、金句收尾，降低同质化风险</li>
            <li><strong>平台适配排版</strong>：自动匹配公众号、小红书、抖音图文等平台的字数、段落和表情符号风格</li>
            <li><strong>发布与运营建议</strong>：发布时间、标题选择、冷启动方向等</li>
          </ul>
          <p>生成后可在「我的作品」中继续编辑、导出 Word，或复制正文发布。</p>
        `
      },
      {
        id: 'what-onboarding',
        title: '自媒体方案：先定位，再创作',
        content: `
          <p>第一次使用爱创作工坊时，系统会引导你完成 5 步自媒体方案制定：</p>
          <ul>
            <li><strong>选平台</strong>：基于你的时间、目标和内容形式，AI 推荐最适合的主攻平台</li>
            <li><strong>定目标</strong>：选择核心变现方向，比如副业增收、打造个人 IP 或小生意引流</li>
            <li><strong>选赛道</strong>：AI 推荐蓝海细分赛道，标注目标人群、变现方式和同质化风险</li>
            <li><strong>做人设</strong>：生成 3-4 个人设定位，并给出默认内容支柱比例</li>
            <li><strong>出方案</strong>：确认平台、目标、赛道、人设，保存后在工作台随时查看</li>
          </ul>
          <p>方案不是一次性问卷，而是后续选题推荐、内容生成和运营建议的底层依据。</p>
        `
      },
      {
        id: 'what-platforms',
        title: '支持哪些自媒体平台',
        content: `
          <p>目前内置主流平台模板，生成时一键切换：</p>
          <ul>
            <li>微信公众号</li>
            <li>小红书</li>
            <li>今日头条</li>
            <li>百家号</li>
            <li>知乎</li>
            <li>抖音图文</li>
            <li>B 站专栏</li>
          </ul>
          <p>每个平台都有对应的标题风格、段落长度和排版规则，避免同一篇稿子生硬搬运。</p>
        `
      },
      {
        id: 'what-skills',
        title: '提示词市场与风格市场',
        content: `
          <p>为了让 AI 输出更稳定、更有个人印记，平台提供两类市场：</p>
          <ul>
            <li><strong>提示词市场</strong>：发现或购买优质 Prompt，覆盖热点评论、产品测评、情感故事、知识干货等场景；也可以上传原创提示词，被他人使用时获得创作币收益。</li>
            <li><strong>风格市场</strong>：浏览其他用户分享并通过审核的写作风格，或上传参考文章让 AI 学习你的文风；使用市场风格创作时，创作者可获得收益分成。</li>
          </ul>
          <p>两者结合，能让你在「不重复别人」和「保持个人风格」之间找到平衡。</p>
        `
      },
      {
        id: 'what-commission',
        title: '约稿中心：接任务赚创作币',
        content: `
          <p>在约稿中心，你可以看到其他用户发布的定向约稿任务。</p>
          <ul>
            <li>按任务要求生成内容并提交</li>
            <li>任务发布方验收通过后，创作币自动到账</li>
            <li>适合想用碎片时间变现、或者想练习垂直领域写作的用户</li>
          </ul>
          <p>约稿中心把「我会写」直接变成「我有收益」。</p>
        `
      },
      {
        id: 'what-tools',
        title: 'AI 小工具：配图与排版辅助',
        content: `
          <p>除了写作，平台还提供几个常用的 AI 小工具：</p>
          <ul>
            <li><strong>AI 抠图</strong>：一键去除图片背景，制作干净封面。</li>
            <li><strong>文字转图片</strong>：把金句、标题转成适合发布的图片卡片。</li>
            <li><strong>图片压缩</strong>：降低图片体积，避免平台上传限制。</li>
            <li><strong>二维码生成</strong>：为文章、活动页快速生成二维码。</li>
            <li><strong>AI 去水印</strong>：处理素材图片，减少发布风险。</li>
          </ul>
          <p>这些小工具覆盖了自媒体发布前常见的配图处理需求。</p>
        `
      }
    ]
  },
  {
    id: 'money',
    title: '收益方式',
    articles: [
      {
        id: 'money-platform',
        title: '平台内收益：创作币、排行榜、邀请',
        content: `
          <p>在爱创作工坊，你可以通过以下方式获得创作币：</p>
          <ul>
            <li><strong>提示词市场</strong>：发布自己的写作提示词，被其他用户使用时获得收益。</li>
            <li><strong>约稿中心</strong>：接受其他用户发布的定向约稿任务，按要求交付内容赚取创作币。</li>
            <li><strong>里程碑奖励</strong>：完成创作任务获得一次性奖励。</li>
            <li><strong>收益排行榜</strong>：每月创作币榜进入前 3，获得 500 创作币奖励。</li>
            <li><strong>邀请返利</strong>：邀请好友注册并订阅，获得返利。</li>
          </ul>
          <p>10 创作币 = 1 元人民币，满 1000 创作币可申请提现。</p>
        `
      },
      {
        id: 'money-external',
        title: '外部自媒体变现：流量主、商单、带货',
        content: `
          <p>爱创作工坊帮你把内容生产时间从 3 小时压缩到 3 分钟，省下来的时间可以用来运营账号、接商单、做流量主。</p>
          <ul>
            <li><strong>公众号</strong>：流量主广告分成 + 商务合作。</li>
            <li><strong>小红书</strong>：笔记带货 + 品牌商单。</li>
            <li><strong>抖音</strong>：中视频计划 + 橱窗带货 + 星图商单。</li>
            <li><strong>今日头条/百家号</strong>：广告分成。</li>
            <li><strong>B站</strong>：专栏创作激励 + 商单合作。</li>
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
    title: '创作流程',
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
          <p>小红书适合短图文和情绪化表达，公众号适合深度长文，今日头条和百家号适合资讯类内容，B站适合年轻化的专栏长图文。</p>
        `
      },
      {
        id: 'how-step3',
        title: '第三步：多平台分发',
        content: `
          <p>同一篇内容可以改写后分发到多个平台，最大化流量价值。</p>
          <p>爱创作工坊支持一次生成多平台版本，也可以导出后微调标题和开头再发布。</p>
        `
      },
      {
        id: 'how-step4',
        title: '第四步：申报收入/冲击榜单',
        content: `
          <p>发布内容后，回到爱创作工坊申报你的自媒体收入。</p>
          <p>申报审核通过后，金额会累加进「自媒体收入榜」，每月前 3 名可获得 500 创作币奖励。</p>
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
    title: '提现与结算',
    articles: [
      {
        id: 'withdraw-coin',
        title: '创作币是什么',
        content: `
          <p>创作币是爱创作工坊平台的虚拟货币。</p>
          <p>你可以通过 提示词市场、约稿中心、排行榜、邀请返利等方式获得创作币，也可以在平台内消费（如订阅会员、购买生成额度）。</p>
          <p>10 创作币 = 1 元人民币。</p>
        `
      },
      {
        id: 'withdraw-rule',
        title: '提现门槛与到账说明',
        content: `
          <p>账户余额满 1000 创作币可申请提现到支付宝。</p>
          <p>未结算收益不可提现，结算前请确认收益明细无误。</p>
          <p>提现申请提交后，通常在 1~7 个工作日到账。</p>
        `
      }
    ]
  }
]
