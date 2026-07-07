# Git 工作流规范

> 本文档定义爱创作（AI Creation）项目的 Git 分支模型、提交规范、PR 与发版流程。

---

## 1. 分支模型

采用简化版 Git Flow：

```text
main        # 生产分支，始终保持可发布状态
  ↑
develop     # 开发分支，日常功能合并至此
  ↑
feature/*   # 功能分支，从 develop 检出
hotfix/*    # 热修分支，从 main 检出，合并回 main 和 develop
release/*   # 发布分支，从 develop 检出，合并回 main（可选）
```

- 小型项目可直接用 `main` + `feature/*`，不强制 `develop`。
- 当前阶段推荐：`main` + `feature/*` + `hotfix/*`。

---

## 2. 分支命名

| 分支类型 | 命名 | 示例 |
|---|---|---|
| 功能分支 | `feature/{模块}-{简述}` | `feature/user-login` |
| 修复分支 | `fix/{模块}-{简述}` | `fix/article-export-pdf` |
| 热修分支 | `hotfix/{简述}` | `hotfix/fix-token-leak` |
| 发布分支 | `release/{版本号}` | `release/v1.0.0` |

- 使用小写，单词间用 `-` 连接。
- 禁止个人名字、日期作为分支名。

---

## 3. 提交信息格式

采用 Conventional Commits：

```text
<类型>(<模块>): <简短描述>

<可选正文>

<可选脚注>
```

### 3.1 类型

| 类型 | 说明 |
|---|---|
| `feat` | 新功能 |
| `fix` | Bug 修复 |
| `docs` | 文档更新 |
| `style` | 代码格式（不影响功能） |
| `refactor` | 重构 |
| `perf` | 性能优化 |
| `test` | 测试相关 |
| `chore` | 构建/工具/依赖变更 |
| `ci` | CI/CD 配置 |

### 3.2 示例

```text
feat(auth): 增加手机号登录接口

fix(article): 修复导出 Word 时图片丢失问题
docs(api): 更新登录接口文档
```

---

## 4. PR 流程

1. 从 `main` 或 `develop` 切出功能分支。
2. 本地开发并自测，确保能编译通过。
3. 提交 PR，标题使用提交信息格式。
4. PR 描述需说明：
   - 改动内容
   - 关联需求/缺陷
   - 测试方式
5. 至少 1 人 Code Review 通过后方可合并。
6. 使用 **Squash and Merge** 或 **Create a merge commit** 视团队约定，推荐 Squash 保持主干清晰。
7. 合并后删除功能分支。

---

## 5. 版本号与标签

采用语义化版本 `MAJOR.MINOR.PATCH`：

| 位 | 说明 |
|---|---|
| MAJOR | 不兼容改动 |
| MINOR | 新增功能，向后兼容 |
| PATCH | Bug 修复 |

发版时从 `main` 打标签：

```bash
git tag -a v1.0.0 -m "Release v1.0.0"
git push origin v1.0.0
```

---

## 6. 禁止事项

- 禁止直接向 `main` 推送代码。
- 禁止在 `main` 上开发或调试。
- 禁止提交包含密码、API Key、私钥等敏感信息。
- 禁止提交大文件（图片、视频、二进制）到 Git。
- 禁止提交 `.env`、本地日志、IDE 配置等环境相关文件。

---

## 7. 变更记录

| 日期 | 版本 | 变更说明 | 变更人 |
|---|---|---|---|
| 2026-06-26 | v1.0 | 初稿：简化 Git Flow、分支命名、Conventional Commits、PR 流程、版本标签 | - |
