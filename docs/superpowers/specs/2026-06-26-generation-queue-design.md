# 文章生成异步队列原型设计

**日期**: 2026-06-26  
**状态**: 待实现  
**关联页面**: create.html、works.html、shared.js

---

## 1. 设计目标

- 点击"生成文章"后，任务进入队列，页面不阻塞、不跳转。
- 用户可继续添加下一篇文章到队列。
- 清晰表达三种状态：排队中、生成中、已完成。
- 前端用 localStorage + setInterval 模拟后端消息队列，后端开发时可平滑迁移为 WebSocket/SSE。

---

## 2. 状态定义

| 状态值 | 展示文案 | 视觉 | 用户可感知 | 可交互 |
|---|---|---|---|---|
| `queued` | 排队中 | 橙色标签 + 排队序号 | "前面还有 N 篇在生成" | 可取消 |
| `generating` | 生成中 | 绿色标签 + 进度条 | "正在生成，预计 30 秒" | 不可取消 |
| `completed` | 已完成 | 深绿色标签 + ✓ | "生成完成" | 预览、导出、删除 |

状态流转：`queued → generating → completed`

---

## 3. 页面交互

### 3.1 创作页 create.html

- 点击"生成文章"后不再跳转 loading.html。
- 右下角滑出"生成队列"悬浮面板，展示当前所有任务。
- 新任务默认状态为 `queued`；若当前无 `generating` 任务，则立即变为 `generating`。
- 用户可关闭面板，继续填写并提交下一篇文章。
- 当任务变为 `completed` 时，弹出全局 Toast 通知，点击可跳转 preview.html。
- 顶部导航显示"生成中"任务数量角标。

### 3.2 我的作品页 works.html

- 作品列表展示所有任务，包括 `queued`、`generating`、`completed`。
- 不同状态用不同颜色标签区分。
- `completed` 文章保留现有预览/导出/删除操作。
- `queued` 和 `generating` 文章显示取消/查看进度操作。

---

## 4. 数据模拟（前端）

任务对象结构：

```javascript
{
  id: 'gen_xxx',
  title: '文章标题或选题',
  topic: '用户输入的选题/方向',
  wordCount: 1500,
  style: '年度总结',
  template: '公众号标准模板',
  status: 'queued' | 'generating' | 'completed',
  progress: 0,           // 0-100
  queuePosition: 1,      // 排队位置
  createdAt: '2026-06-26T12:00:00',
  startedAt: null,
  completedAt: null
}
```

存储：使用 `localStorage` 持久化任务列表，键名为 `aichuangzuo_generation_queue`。

队列消费模拟：

- 全局 `setInterval` 每秒检查一次队列。
- 并发限制 `maxConcurrent = 1`（模拟后端消息队列单 worker 消费）。
- 若存在 `generating` 任务，其他 `queued` 任务保持等待，并更新 `queuePosition`。
- 若不存在 `generating` 任务，将最旧的 `queued` 任务状态改为 `generating`，并记录 `startedAt`。
- `generating` 任务每 500ms 增加 `progress`，达到 100 后变为 `completed`，记录 `completedAt`，并触发 Toast。

后端迁移：

- 移除前端 `setInterval` 消费逻辑。
- 通过 WebSocket 或 SSE 监听后端消息队列状态变更。
- 前端仅负责展示状态和响应通知。

---

## 5. 组件清单

| 组件 | 位置 | 说明 |
|---|---|---|
| 生成队列悬浮面板 | create.html | 右下角滑出，展示最近任务 |
| 全局 Toast 通知 | shared.js | 任务完成/失败时弹出 |
| 顶部角标 | 各页面导航 | 显示生成中任务数量 |
| 状态标签 | works.html | 列表中展示任务状态 |
| 任务进度条 | create.html / works.html | generating 状态展示 |

---

## 6. 关键交互细节

- 用户提交文章时，若本月额度不足，提示"额度不足"，不入队。
- `queued` 任务可取消，取消后从队列移除。
- `generating` 任务不可取消，提示"生成中，无法取消"。
- 页面刷新后，队列状态从 `localStorage` 恢复，继续模拟。
- 同一浏览器不同标签页共享队列（通过 `localStorage` + `storage` 事件同步）。

---

## 7. 原型验证要点

- 连续点击"生成文章"3 次，应看到 1 个 generating、2 个 queued。
- generating 完成后，下一个 queued 自动变为 generating。
- completed 任务出现在"我的作品"列表顶部。
- 关闭队列面板后仍可继续提交新任务。
