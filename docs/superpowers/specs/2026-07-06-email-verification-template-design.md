# 邮箱验证码邮件模板 redesign (2026-07-06)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 把当前 `EmailMessageFactory.populateCodeEmail()` 里简陋的验证码邮件正文，升级为带有品牌视觉、产品价值介绍、核心功能亮点和会员引导的营销型邮件模板，同时保证主流邮件客户端兼容性。

**Scope:** 仅改造 outbound 验证码邮件的 HTML / text 内容，不动发邮件流程、缓存、限频、错误码。

**Tech Stack:** Spring Boot Mail (`MimeMessage` / `MimeMultipart`)、inline CSS + table 布局。

---

## Global Constraints

- **不引入新中间件/模板引擎**: 继续使用字符串拼接构造 HTML，不引入 Thymeleaf / Freemarker / Velocity 等模板依赖。
- **保持 multipart/alternative**: 必须同时提供 `text/plain` 和 `text/html`，确保不支持 HTML 的客户端也能读取验证码。
- **邮件客户端兼容优先**: HTML 使用 table 布局 + inline style；避免 flexbox、grid、自定义字体、CSS 变量、RGBA 半透明阴影。
- **不添加外部图片**: 为避免被邮件客户端屏蔽/显示红叉，本次不使用远程图片 Logo，只用文字品牌名作为品牌头。
- **营销内容可配置化（可选）**: 营销文案先硬编码在工厂类中，若后续频繁变更再抽成配置；本次以视觉和内容升级为主。
- **Global Constraints 来自本仓库 architecture 文档**: MySQL 8 + Flyway + JWT + Caffeine，已配置完毕。

---

## 设计要点

### 1. 整体布局

```
┌─────────────────────────────────────────────┐
│  浅灰背景 #f8f9fa                            │
│                                             │
│   ┌─────────────────────────────────────┐   │
│   │  白色卡片 #ffffff，圆角 12px          │   │
│   │                                     │   │
│   │  [品牌头] 爱创作  AI 自媒体写作助手   │   │
│   │                                     │   │
│   │  [问候语] Hi，欢迎加入爱创作           │   │
│   │                                     │   │
│   │  [说明] 你的验证码是                  │   │
│   │                                     │   │
│   │  ┌─────────────────────────────┐   │   │
│   │  │         1 2 3 4 5 6         │   │   │
│   │  │      （大号 40px 主色）      │   │   │
│   │  └─────────────────────────────┘   │   │
│   │                                     │   │
│   │  [有效期] 5 分钟内有效，请勿泄露      │   │
│   │                                     │   │
│   │  ───────── 分割线 ─────────        │   │
│   │                                     │   │
│   │  [营销区]                            │   │
│   │  3 分钟生成多平台自媒体文章           │   │
│   │  [多平台适配] [一键导出 Word]         │   │
│   │  了解会员权益 →                      │   │
│   │                                     │   │
│   │  [页脚] 系统自动发出，请勿回复        │   │
│   │  © 2026 杭州爱启云网络科技有限公司    │   │
│   └─────────────────────────────────────┘   │
│                                             │
└─────────────────────────────────────────────┘
```

最大宽度 `600px`，在桌面端居中；移动端自动撑满。

### 2. 视觉风格

| 元素 | 样式 |
|------|------|
| 邮件画布背景 | `#f8f9fa` |
| 卡片背景 | `#ffffff` |
| 卡片圆角 | `12px` |
| 品牌主色 | `#ff2442`（爱创作红） |
| 主标题/品牌名 | `#1a1a1a`，字号 `20px`，字重 `700` |
| 正文 | `#262626`，字号 `14px`，行高 `1.6` |
| 次要文字 | `#595959`，字号 `12px` |
| 验证码框背景 | `#fff0f2`（主色淡背景） |
| 验证码框圆角 | `8px` |
| 验证码字号 | `40px`，字重 `700`，字间距 `8px`，颜色 `#ff2442` |
| 功能标签背景 | `#fff0f2`，文字 `#ff2442`，圆角 `4px`，字号 `12px` |
| 链接颜色 | `#ff2442`，hover `#e61e3a` |
| 分割线 | `1px solid #eeeeee` |
| 字体栈 | `PingFang SC, Microsoft YaHei, Hiragino Sans GB, Helvetica Neue, Arial, sans-serif` |

### 3. 内容文案

**主题（Subject）**
```
你的爱创作验证码
```

**text/plain 版本**
```
Hi，欢迎加入爱创作！

你的验证码是：123456
验证码 5 分钟内有效，请勿泄露给他人。

爱创作是一款 AI 自媒体写作助手。只需输入一个写作方向，AI 会在 3 分钟内生成一篇结构完整、适配公众号、小红书、今日头条、抖音图文、百家号等平台的自媒体文章。

核心功能：多平台适配、一键导出 Word
了解会员权益：https://aichuangzuo.com/pricing

本邮件由系统自动发出，请勿直接回复。
© 2026 杭州爱启云网络科技有限公司
```

**text/html 版本（结构）**
```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>你的爱创作验证码</title>
</head>
<body style="margin:0;padding:0;background-color:#f8f9fa;font-family:PingFang SC,Microsoft YaHei,Hiragino Sans GB,Helvetica Neue,Arial,sans-serif;">
  <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0">
    <tr><td align="center" style="padding:32px 16px;">
      <table role="presentation" width="600" cellspacing="0" cellpadding="0" border="0" style="max-width:600px;width:100%;background-color:#ffffff;border-radius:12px;overflow:hidden;">
        <!-- 品牌头 -->
        <tr><td style="padding:32px 32px 16px;">
          <div style="font-size:20px;font-weight:700;color:#1a1a1a;">爱创作</div>
          <div style="font-size:12px;color:#595959;margin-top:4px;">AI 自媒体写作助手</div>
        </td></tr>
        <!-- 问候 -->
        <tr><td style="padding:8px 32px 16px;font-size:16px;color:#262626;">
          Hi，欢迎加入爱创作
        </td></tr>
        <!-- 验证码说明 -->
        <tr><td style="padding:8px 32px 0;font-size:14px;color:#262626;">
          你的验证码是
        </td></tr>
        <!-- 验证码 -->
        <tr><td style="padding:16px 32px;">
          <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="background-color:#fff0f2;border-radius:8px;">
            <tr><td align="center" style="padding:24px;font-size:40px;font-weight:700;color:#ff2442;letter-spacing:8px;">
              123456
            </td></tr>
          </table>
        </td></tr>
        <!-- 有效期 -->
        <tr><td style="padding:0 32px 24px;font-size:12px;color:#595959;">
          验证码 5 分钟内有效，请勿泄露给他人。
        </td></tr>
        <!-- 分割线 -->
        <tr><td style="padding:0 32px;"><div style="height:1px;background-color:#eeeeee;"></div></td></tr>
        <!-- 营销区 -->
        <tr><td style="padding:24px 32px;">
          <div style="font-size:14px;font-weight:600;color:#1a1a1a;margin-bottom:8px;">AI 帮你高效创作自媒体内容</div>
          <div style="font-size:13px;color:#595959;line-height:1.7;margin-bottom:12px;">
            输入一个写作方向，AI 会在 3 分钟内生成一篇结构完整、适配公众号、小红书、今日头条、抖音图文、百家号等平台的自媒体文章。
          </div>
          <div style="margin-bottom:16px;">
            <span style="display:inline-block;background-color:#fff0f2;color:#ff2442;font-size:12px;padding:4px 10px;border-radius:4px;margin-right:8px;">多平台适配</span>
            <span style="display:inline-block;background-color:#fff0f2;color:#ff2442;font-size:12px;padding:4px 10px;border-radius:4px;">一键导出 Word</span>
          </div>
          <a href="https://aichuangzuo.com/pricing" style="font-size:13px;color:#ff2442;text-decoration:none;">了解会员权益 →</a>
        </td></tr>
        <!-- 页脚 -->
        <tr><td style="padding:16px 32px 32px;font-size:11px;color:#8c8c8c;line-height:1.6;border-top:1px solid #eeeeee;">
          本邮件由系统自动发出，请勿直接回复。<br>
          © 2026 杭州爱启云网络科技有限公司 · All Rights Reserved
        </td></tr>
      </table>
    </td></tr>
  </table>
</body>
</html>
```

### 4. 文件改动表

| 类型 | 文件 | 改动 |
|------|------|------|
| 修改 | `user/api/src/main/java/.../mail/EmailMessageFactory.java` | 重写 `populateCodeEmail()`，构造新的品牌化 HTML + 纯文本正文；验证码通过占位符注入；营销文案硬编码 |
| 新增（可选） | `user/api/src/test/java/.../mail/EmailMessageFactoryTest.java` | 断言生成邮件的 HTML 包含品牌文案、验证码、会员链接；text/plain 包含验证码 |
| 修改 | `tests/e2e/verify_user_auth.py` | 无需改动，仍通过正则从 GreenMail 邮件正文提取 6 位数字 |
| 修改 | `tests/e2e/verify_reset_password.py` | 无需改动 |

### 5. 实现要点

- `EmailMessageFactory.populateCodeEmail(MimeMessage msg, String from, String toEmail, String code)` 保持签名不变。
- HTML 字符串在方法内用 `StringBuilder` 或文本块（Java 15+ text block）拼接，保持可读性。
- 验证码通过字符串替换注入，避免 XSS：验证码是纯 6 位数字，无需额外转义，但替换逻辑需确保只替换占位符。
- `text/plain` 和 `text/html` 两个 `MimeBodyPart` 保持 `multipart/alternative`。
- 所有链接使用完整 URL（生产域名），本地测试不影响（邮件本身只是文本）。

### 6. 邮件客户端兼容性

| 客户端 | 关注点 |
|--------|--------|
| Gmail (Web/App) | 支持 table、inline style；避免 `margin` 负值 |
| Outlook (Windows) | 使用 table 布局，避免 flex/grid；圆角在部分 Outlook 上可能失效但仍可接受 |
| Apple Mail | 完全支持 |
| 企业邮箱/QQ 邮箱 | 支持基础 table + inline style |
| 纯文本客户端 | 通过 `text/plain` 分支正常显示验证码和核心文案 |

### 7. 测试矩阵

| 层 | 用例 | 方法 |
|----|------|------|
| 单元测试 | 生成邮件 HTML 包含品牌名「爱创作」、验证码、有效期文案、会员链接 | 新增 `EmailMessageFactoryTest` |
| 单元测试 | 生成邮件 text/plain 包含验证码和有效期 | 同上 |
| 现有测试 | `EmailCodeServiceTest` 仍能从 GreenMail 邮件正文提取 6 位数字 | 跑现有测试 |
| E2E | `verify_user_auth.py` 正常通过 | 跑 E2E |
| E2E | `verify_reset_password.py` 正常通过 | 跑 E2E |

### 8. 风险与缓解

| 风险 | 缓解 |
|------|------|
| 邮件被识别为推广/垃圾邮件 | 保持验证码为主体、营销内容控制在底部 30% 以内；发件域名配置 SPF/DKIM/DMARC（运维侧） |
| Outlook 显示圆角失效 | 使用 table 背景色块作为 fallback，圆角缺失不影响可读性 |
| 链接域名未上线导致 404 | 先使用 `/pricing` 相对路径或确认线上域名后再写死；本次使用 `https://aichuangzuo.com/pricing` |
| 纯文本版本过长 | 保留核心信息：验证码 + 有效期 + 一句话价值 + 会员链接 |
| 后续频繁改文案 | 当前硬编码，若一个季度改 3 次以上再抽离到 `application.yml` 或模板文件 |

### 9. 实施任务分解（供 writing-plans 阶段展开）

1. 重写 `EmailMessageFactory.populateCodeEmail()` HTML / text 邮件正文
2. 新增 `EmailMessageFactoryTest` 验证 HTML 与纯文本内容
3. 跑现有 `EmailCodeServiceTest` 与 E2E，确认验证码提取正则仍能命中
4. 手动发送测试邮件到 Gmail / Outlook / QQ 邮箱，检查渲染效果
5. 更新本设计文档状态为「已实施」
