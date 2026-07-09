# 用户端"我的反馈"历史(弹框内嵌 tab)

> 2026-07-09 · 续 2026-07-08 反馈功能。补全"用户查看自己提交过的反馈及管理员回复"。

## Context

`docs/superpowers/specs/2026-07-08-feedback-design.md` 把"用户端主动查看自己的反馈历史"列为非目标,本期走消息中心间接看到回复。

但实际存在 UX 缺口:
- 用户提交过 N 条反馈后,消息中心只对**已回复**的留通知,已读后**无法回看**
- **待回复**的反馈(`status=0`)用户根本看不到,无法判断管理员是否已收到
- 通知列表里所有 `feedback` 类消息堆在一起,无法按提交时间顺序浏览

目标:在现有「意见反馈」弹框里嵌一个"我的反馈"tab,显示**本人全部**提交记录(待回复 + 已回复),可点开看完整内容 + 管理员回复。

非目标:
- 继续追问 / 多轮对话
- 编辑 / 删除已提交的反馈
- 用户主动撤回(业务上无意义)

## 决策

| 主题 | 选择 | 理由 |
|---|---|---|
| 入口 | 弹框内嵌 tab(提交 / 我的反馈) | 用户选定;不新增路由 |
| 后端变更 | 只加查询接口,无 Flyway 迁移 | 现有 `u_feedback` 表 + `idx_u_feedback_user_created(user_id, created_at)` 已够用 |
| 状态过滤 | 客户端 `status` 参数控制(0/1/不传) | 接口简单;空态时前端用 empty 视图 |
| 列表规模 | 分页 size=20,与 admin 端一致 | 与 admin 体验一致 |
| 详情交互 | 同 modal 内「列表 ↔ 详情」切换,**不嵌套 drawer** | 手机端 modal 满屏,嵌套 drawer 体感差 |
| VO | 复用现有 `FeedbackVO` | 字段已含 id/type/content/status/replyContent/repliedAt/createdAt |
| 提交后行为 | 成功后自动切到「我的反馈」tab 并刷新 | 让用户立刻看到自己刚提交的记录 |
| 时间格式 | 复用现有 `formatTime`(已在 MineIndex) | 与通知中心一致 |
| 主题适配 | 复用弹框的浅/深色变量 | 一致性 |

## 数据形状

**无 Flyway 迁移**。`u_feedback` 当前结构(V021 创建 + V022 删除 contact 列后):

| 字段 | 类型 | 用途 |
|---|---|---|
| id | BIGINT UNSIGNED PK | 主键 |
| user_id | BIGINT UNSIGNED | 提交人(查询条件) |
| type | VARCHAR(32) | 类型 label |
| content | VARCHAR(2000) | 反馈正文 |
| reply_content | VARCHAR(2000) NULL | 管理员回复 |
| reply_admin_id | BIGINT UNSIGNED NULL | 回复管理员 |
| replied_at | DATETIME(3) NULL | 回复时间 |
| status | TINYINT UNSIGNED | 0 待回复 / 1 已回复 |
| ... | audit 字段 | - |

索引 `idx_u_feedback_user_created(user_id, created_at)` 完美匹配本次查询。

## 模块划分

### 后端 user-api

**改** `feedback/mapper/FeedbackMapper.java`:

```java
@Select("""
    SELECT id, user_id, type, content, reply_content AS replyContent,
           reply_admin_id AS replyAdminId, replied_at AS repliedAt,
           status, created_at AS createdAt
    FROM u_feedback
    WHERE user_id = #{userId} AND is_deleted = 0
      AND (#{status} IS NULL OR status = #{status})
    ORDER BY created_at DESC
    LIMIT #{offset}, #{size}
""")
List<Feedback> pageByUser(@Param("userId") Long userId,
                          @Param("status") Integer status,
                          @Param("offset") int offset,
                          @Param("size") int size);

@Select("""
    SELECT COUNT(*) FROM u_feedback
    WHERE user_id = #{userId} AND is_deleted = 0
      AND (#{status} IS NULL OR status = #{status})
""")
long countByUser(@Param("userId") Long userId,
                 @Param("status") Integer status);
```

**改** `feedback/service/FeedbackService.java`:

```java
List<FeedbackVO> pageByUser(Long userId, Integer status, int page, int size);
long countByUser(Long userId, Integer status);
```

**改** `feedback/service/impl/FeedbackServiceImpl.java`:

```java
@Override
public List<FeedbackVO> pageByUser(Long userId, Integer status, int page, int size) {
    int safeSize = Math.min(Math.max(1, size), 100);
    int safePage = Math.max(1, page);
    int offset = (safePage - 1) * safeSize;
    List<Feedback> rows = feedbackMapper.pageByUser(userId, status, offset, safeSize);
    return rows.stream().map(this::toVO).toList();
}

@Override
public long countByUser(Long userId, Integer status) {
    return feedbackMapper.countByUser(userId, status);
}

private FeedbackVO toVO(Feedback fb) {
    FeedbackVO vo = new FeedbackVO();
    vo.setId(fb.getId());
    vo.setType(fb.getType());
    vo.setContent(fb.getContent());
    vo.setStatus(fb.getStatus());
    vo.setReplyContent(fb.getReplyContent());
    vo.setRepliedAt(fb.getRepliedAt());
    vo.setCreatedAt(fb.getCreatedAt());
    return vo;
}
```

> 返回类型约定:与 `AdminFeedbackController.list` 保持一致,用 `Map<String, Object>` 包装 `{list, total, page, size}`(项目内无通用 `PageResult<T>`,沿用 admin 端既有模式,避免引入新共享类)。

**改** `feedback/controller/FeedbackController.java`:

```java
@Operation(summary = "我的反馈历史")
@GetMapping("/mine")
public Result<Map<String, Object>> mine(
        @RequestParam(required = false) Integer status,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size) {
    Long userId = SecurityUserContext.getCurrentUserId();
    int safePage = Math.max(1, page);
    int safeSize = Math.min(Math.max(1, size), 100);
    List<FeedbackVO> rows = feedbackService.pageByUser(userId, status, safePage, safeSize);
    long total = feedbackService.countByUser(userId, status);
    Map<String, Object> data = new HashMap<>();
    data.put("list", rows);
    data.put("total", total);
    data.put("page", safePage);
    data.put("size", safeSize);
    return Result.success(data);
}
```

### 前端 user-web

**改** `views/console/ConsoleLayout.vue` 的「意见反馈」弹框(原 line 540-577,提交逻辑在 line 1308-1340):

- 弹框宽度 560 → 640(放得下历史 tab 内容)
- 弹框内容改成 `<a-tabs v-model:active-key="feedbackTab">`,两个 pane:「提交反馈」/「我的反馈」
- 「提交反馈」tab:把现有 type 按钮 + content textarea + submit button 包进来
- 「我的反馈」tab:见下方代码骨架
- 提交成功回调里:清表单 + 切 `feedbackTab.value = 'history'` + `loadHistory()`

**改** `api/feedback.js`:

```js
export const pageMyFeedbacks = (params) => request.get('/feedback/mine', { params })
```

**新建状态**(挂在 ConsoleLayout setup 内,与现有 `feedbackVisible` 平级,line 1309 附近):

```js
const feedbackTab = ref('submit')
const historyFilter = ref('all')
const historyFilterOptions = [
  { label: '全部',   value: 'all' },
  { label: '待回复', value: '0' },
  { label: '已回复', value: '1' }
]
const historyList = ref([])
const historyLoading = ref(false)
const historyTotal = ref(0)
const historyPage = ref(1)
const historySize = ref(20)
const historyDetail = ref(null)

const loadHistory = async () => {
  historyLoading.value = true
  try {
    const params = { page: historyPage.value, size: historySize.value }
    if (historyFilter.value !== 'all') params.status = Number(historyFilter.value)
    const res = await pageMyFeedbacks(params)
    historyList.value = res.list
    historyTotal.value = res.total
    historyPage.value = res.page
    historySize.value = res.size
  } catch (e) {
    message.error(e?.message || '加载失败')
  } finally {
    historyLoading.value = false
  }
}

const onHistoryPageChange = (p) => {
  historyPage.value = p
  loadHistory()
}

const openHistoryDetail = (fb) => {
  historyDetail.value = fb   // 列表里已是完整 VO,无需二次请求
}

const closeHistoryDetail = () => {
  historyDetail.value = null
}
```

**模板**「我的反馈」tab:

```vue
<template v-if="feedbackTab === 'history'">
  <div v-if="!historyDetail" class="history-list-pane">
    <a-segmented v-model:value="historyFilter" :options="historyFilterOptions"
                 @change="historyPage = 1; loadHistory()" block class="history-filter" />
    <a-spin :spinning="historyLoading">
      <div v-if="historyList.length === 0 && !historyLoading" class="history-empty">
        <p>还没有反馈记录</p>
        <a-button type="primary" @click="feedbackTab = 'submit'">去提交</a-button>
      </div>
      <ul v-else class="history-list">
        <li v-for="fb in historyList" :key="fb.id" class="history-item" @click="openHistoryDetail(fb)">
          <div class="history-item-row1">
            <a-tag color="blue">{{ fb.type }}</a-tag>
            <a-tag :color="fb.status === 0 ? 'orange' : 'green'">
              {{ fb.status === 0 ? '待回复' : '已回复' }}
            </a-tag>
            <span class="history-item-time">{{ formatTime(fb.createdAt) }}</span>
          </div>
          <div class="history-item-content">{{ fb.content }}</div>
          <div v-if="fb.status === 1 && fb.replyContent"
               class="history-item-reply-preview">
            管理员回复:{{ fb.replyContent }}
          </div>
        </li>
      </ul>
      <a-pagination
        v-if="historyTotal > historySize"
        :current="historyPage"
        :page-size="historySize"
        :total="historyTotal"
        @change="onHistoryPageChange"
        simple
        class="history-pager"
      />
    </a-spin>
  </div>
  <div v-else class="history-detail-pane">
    <a-button type="link" class="history-back-btn" @click="closeHistoryDetail">← 返回列表</a-button>
    <div class="detail-row">
      <span class="detail-label">类型</span>
      <a-tag color="blue">{{ historyDetail.type }}</a-tag>
      <a-tag :color="historyDetail.status === 0 ? 'orange' : 'green'">
        {{ historyDetail.status === 0 ? '待回复' : '已回复' }}
      </a-tag>
    </div>
    <div class="detail-row">
      <span class="detail-label">提交时间</span>
      <span>{{ formatTime(historyDetail.createdAt) }}</span>
    </div>
    <div class="detail-row detail-row-stack">
      <span class="detail-label">反馈内容</span>
      <pre class="detail-content">{{ historyDetail.content }}</pre>
    </div>
    <template v-if="historyDetail.status === 1">
      <a-divider />
      <div class="detail-row detail-row-stack">
        <span class="detail-label">管理员回复</span>
        <pre class="detail-content detail-content-admin">{{ historyDetail.replyContent }}</pre>
      </div>
      <div class="detail-row">
        <span class="detail-label">回复时间</span>
        <span>{{ formatTime(historyDetail.repliedAt) }}</span>
      </div>
    </template>
    <div v-else class="history-pending-hint">提交成功,我们会尽快处理,谢谢反馈!</div>
  </div>
</template>
```

**切换 tab 钩子**:在 ConsoleLayout 加 `watch(feedbackTab, ...)`:

```js
watch(feedbackTab, (t) => {
  if (t === 'history' && historyList.value.length === 0 && !historyDetail.value) {
    loadHistory()
  }
})
```

**样式**(新加在 ConsoleLayout 现有 `<style scoped>` 内,`.feedback-panel` 选择器附近,line 3024 之后):

```css
.feedback-modal .ant-modal-body { max-height: 70vh; overflow-y: auto; }
.feedback-tabs :deep(.ant-tabs-nav) { margin-bottom: 16px; }

.history-list-pane { padding: 0 4px; }
.history-filter { margin-bottom: 12px; }
.history-list { list-style: none; padding: 0; margin: 0; }
.history-item {
  padding: 12px;
  border: 1px solid var(--border-color, #f0f0f0);
  border-radius: 8px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: background 0.15s;
}
.history-item:hover { background: var(--hover-bg, #fafafa); }
.history-item-row1 {
  display: flex; align-items: center; gap: 8px;
  margin-bottom: 6px; flex-wrap: wrap;
}
.history-item-time { font-size: 12px; color: #8c8c8c; margin-left: auto; }
.history-item-content {
  font-size: 14px; color: var(--text-primary, #1a1a1a);
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical;
  overflow: hidden; text-overflow: ellipsis;
  line-height: 1.5;
}
.history-item-reply-preview {
  font-size: 12px; color: #1677ff; margin-top: 6px;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.history-empty {
  text-align: center; padding: 32px 0; color: #8c8c8c;
}
.history-empty p { margin-bottom: 12px; }
.history-pager { text-align: center; margin-top: 12px; }

.history-detail-pane { padding: 0 4px; }
.history-back-btn { padding-left: 0; margin-bottom: 8px; }
.detail-row {
  display: flex; align-items: center; gap: 12px; margin-bottom: 10px;
}
.detail-row-stack { align-items: flex-start; flex-direction: column; gap: 6px; }
.detail-label {
  display: inline-block; min-width: 80px;
  color: #8c8c8c; font-size: 13px;
}
.detail-content {
  margin: 0; white-space: pre-wrap; word-break: break-word;
  background: #f7f7f7; padding: 12px; border-radius: 6px;
  font-family: inherit; font-size: 13px; line-height: 1.7;
  width: 100%; box-sizing: border-box;
}
.detail-content-admin { background: #e6f7ff; }
.history-pending-hint {
  text-align: center; color: #8c8c8c; padding: 16px 0; font-size: 13px;
}
```

## 接口契约

| 方法 | 路径 | 入参 | 返回 | 说明 |
|---|---|---|---|---|
| `GET` | `/api/v1/user/feedback/mine` | `status`(可选 0/1)、`page=1`、`size=20` | `{list: FeedbackVO[], total, page, size}` | 仅返回当前用户,`is_deleted=0`,按 `created_at DESC` |

不修改现有 `/feedback/submit` 接口。

## 错误处理

| 场景 | 处理 |
|---|---|
| 未登录 | 401,由现有 JWT 过滤器处理;前端拦截到 401 跳登录 |
| `status` 非法 | 后端忽略(只匹配 0/1,其他视为"全部") |
| `page` < 1 / `size` 越界 | 后端 clamp(page≥1, size ∈ [1, 100]) |
| 列表为空 | 前端空态视图,带「去提交」按钮 |
| 加载失败 | `<a-spin>` 包裹 + try/catch toast 提示 |
| 内容超长 | 列表 2 行截断,详情完整显示 |
| 待回复状态 | 详情不显示回复区块,改为「提交成功,我们会尽快处理」提示 |
| 提交后 | 自动切「我的反馈」tab + refresh,让用户看到新记录 |
| 弹框关闭再开 | tab 重置回「提交」,不记忆上次状态 |
| 跨用户访问 | `WHERE user_id = currentUserId` 强制隔离,不存在越权 |

## 测试

### 后端单测

`project/user/api/src/test/java/com/aichuangzuo/user/modules/feedback/service/FeedbackServiceTest.java` 新增 3 用例:

1. `pageByUser_returnsOnlyCurrentUser` — 用户 A 提交 2 条,用户 B 提交 1 条;A 调 `pageByUser(A.id, null, 1, 20)` 只能看到自己 2 条;`countByUser(A.id)` = 2
2. `pageByUser_statusFilter_excludesOther` — A 提交 2 待回复 + 1 已回复,调 `pageByUser(A.id, 0, 1, 20)` 只返回 2 条
3. `pageByUser_emptyResult_returnsEmpty` — 新用户没有任何反馈,调 `pageByUser(newUserId, null, 1, 20)` 返回空 List,`countByUser` = 0

### 前端

- `npm run build` 通过(已有 build 命令)
- 手动验证:弹框切 tab / 筛选 / 翻页 / 点详情 / 返回 / 空态 / 提交后跳转

### E2E

扩展 `tests/e2e/verify_feedback.py`(已有,2026-07-08 写过),**追加 1 段**(不是新脚本,避免脚本散落):

```
def test_user_history_in_modal():
    # 用已有测试用户(确保 u_feedback 里有数据)
    # 1. 登录 user 端拿 token
    # 2. GET /api/v1/user/feedback/mine 断言 list ≥ 1,字段齐全
    # 3. GET /api/v1/user/feedback/mine?status=0 断言只返回待回复
    # 4. 模拟浏览器:打开 /console/mine → 点「意见反馈」→ 截图
    # 5. 断言「我的反馈」tab 存在,点击 → 断言列表项 ≥ 1
    # 6. 断言 status=0 的项显示「待回复」标签
    # 7. 点列表项 → 断言进入详情视图,显示「反馈内容」+「待回复」提示
    # 8. 若有 status=1 的项,点进去断言显示「管理员回复」区块
    # 9. 点「← 返回列表」回到列表
    # 10. 截图保存:tests/e2e/screenshots/feedback_history_list.png
    #                tests/e2e/screenshots/feedback_history_detail.png
```

## 关键文件清单

- 后端 user-api:3 个改(Mapper/Service/Controller)
- 前端 user-web:2 个改(`api/feedback.js` 加一个导出;`ConsoleLayout.vue` 弹框重构)
- 测试:1 个改(`FeedbackServiceTest` 加 3 用例;`verify_feedback.py` 追加 1 段)
- 设计文档:本文档
- **无 Flyway 迁移 / 无新枚举 / 无新依赖 / 无新路由**

## 风险与回滚

- `feedback/mine` 是新接口,不影响现有 `submit` 流程,失败可独立回滚(改回 MineIndex 不渲染 tab 即可)
- 后端查询走 `idx_u_feedback_user_created` 索引,单用户历史即使上千条也在毫秒级;无性能风险
- 弹框宽度从 480 改 640,可能影响其他在 480 上对齐的样式;验证时关注弹框居中和左右内边距
- 提交后切 tab 的副作用:若后端写入成功但前端切换 tab 失败,用户仍能看到成功提示,只是「我的反馈」tab 不会自动出现,刷新即可

## 实施顺序

1. 后端:`FeedbackMapper` → `FeedbackService` → `FeedbackController` → 单测(必须先绿)
2. 前端:`api/feedback.js` 导出 → ConsoleLayout 弹框重构 → `npm run build` 通过
3. E2E:扩展 `verify_feedback.py`,加 1 段新断言
4. 手动浏览器回归:打开弹框 → 提交 1 条 → 自动跳历史 → 点详情 → 验证显示

步骤 1 + 2 可分别独立 commit;步骤 3 验证整体。
