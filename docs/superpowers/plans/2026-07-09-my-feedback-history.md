# 用户端「我的反馈」历史功能实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在用户端「意见反馈」弹框内新增「我的反馈」tab,显示本人提交过的所有反馈(待回复 + 已回复)及管理员回复,后端新增 `GET /api/v1/user/feedback/mine` 查询接口。

**Architecture:** 后端 user-api 加 2 个查询方法(`pageByUser` + `countByUser`)和 1 个 Controller 端点,复用现有 `u_feedback` 表 + `idx_u_feedback_user_created` 索引,**无 Flyway 迁移**。前端 ConsoleLayout 弹框改成 tabs 容器,「我的反馈」tab 内嵌筛选 + 列表 + 详情(同 modal 内切换,**不嵌套 drawer**)。

**Tech Stack:** Spring Boot 3 + MyBatis-Plus + Flyway + MySQL 8 + Vue 3 + Ant Design Vue + Playwright (E2E)

**Spec:** `docs/superpowers/specs/2026-07-09-my-feedback-history-design.md`

## Global Constraints

- 路径命名:`/api/v1/user/feedback/mine`(`user` 端,非 admin)
- 返回结构:`{list, total, page, size}` 包装在 `Result.success(data)`,与 `AdminFeedbackController.list` 保持一致(`project/admin/api/src/main/java/com/aichuangzuo/admin/modules/feedback/controller/AdminFeedbackController.java:34-45`)
- size 范围:clamp 到 `[1, 100]`,page clamp 到 `≥ 1`
- 跨用户隔离:`WHERE user_id = currentUserId AND is_deleted=0`
- 时间格式:复用 MineIndex 已有的 `formatTime`(ConsoleLayout 也已有同名工具,line 2299)
- 提交反馈后:自动切到「我的反馈」tab 并 `loadHistory()`
- 弹框宽度 560 → 640(原 line 544)
- 弹框位置:`project/user/web/src/views/console/ConsoleLayout.vue`(**不是 MineIndex.vue**)
- 风格:沿用现有 `.feedback-*` 类名,新增 `.history-*` / `.detail-*` 类
- **无 Flyway / 无新枚举 / 无新依赖 / 无新路由**

---

## Task 1: 后端 user-api — Mapper + Service + 单测

**Files:**
- Modify: `project/user/api/src/main/java/com/aichuangzuo/user/modules/feedback/mapper/FeedbackMapper.java`
- Modify: `project/user/api/src/main/java/com/aichuangzuo/user/modules/feedback/service/FeedbackService.java`
- Modify: `project/user/api/src/main/java/com/aichuangzuo/user/modules/feedback/service/impl/FeedbackServiceImpl.java`
- Modify: `project/user/api/src/test/java/com/aichuangzuo/user/modules/feedback/service/FeedbackServiceTest.java`

**Interfaces (produced for downstream tasks):**
- `FeedbackMapper.pageByUser(userId, status, offset, size) -> List<Feedback>`
- `FeedbackMapper.countByUser(userId, status) -> long`
- `FeedbackService.pageByUser(userId, status, page, size) -> List<FeedbackVO>`
- `FeedbackService.countByUser(userId, status) -> long`

- [ ] **Step 1: 写 3 个失败的单测**

打开 `project/user/api/src/test/java/com/aichuangzuo/user/modules/feedback/service/FeedbackServiceTest.java`,在最后一个 `}` 之前添加以下 3 个测试方法(参照现有 `makeUser` / `baseReq` 辅助方法):

```java
@Test
void pageByUser_returnsOnlyCurrentUser() {
    User a = makeUser("fb-page-a@test.com");
    User b = makeUser("fb-page-b@test.com");
    feedbackService.submit(a.getId(), baseReq());
    feedbackService.submit(a.getId(), baseReq());
    feedbackService.submit(b.getId(), baseReq());

    List<FeedbackVO> listA = feedbackService.pageByUser(a.getId(), null, 1, 20);
    assertEquals(2, listA.size());
    assertEquals(2L, feedbackService.countByUser(a.getId(), null));
    List<FeedbackVO> listB = feedbackService.pageByUser(b.getId(), null, 1, 20);
    assertEquals(1, listB.size());
}

@Test
void pageByUser_statusFilter_excludesOther() {
    User u = makeUser("fb-status@test.com");
    Long id1 = feedbackService.submit(u.getId(), baseReq());
    feedbackService.submit(u.getId(), baseReq());
    // 手动改第 1 条为已回复
    jdbc.update("UPDATE u_feedback SET status = 1, reply_content = '已回', replied_at = NOW(3), reply_admin_id = 1 WHERE id = ?", id1);

    List<FeedbackVO> pending = feedbackService.pageByUser(u.getId(), 0, 1, 20);
    assertEquals(1, pending.size());
    assertEquals(0, pending.get(0).getStatus());
}

@Test
void pageByUser_emptyResult_returnsEmpty() {
    User u = makeUser("fb-empty@test.com");
    List<FeedbackVO> list = feedbackService.pageByUser(u.getId(), null, 1, 20);
    assertTrue(list.isEmpty());
    assertEquals(0L, feedbackService.countByUser(u.getId(), null));
}
```

并在文件顶部 imports 区追加:
```java
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import com.aichuangzuo.user.modules.feedback.vo.FeedbackVO;
import java.util.List;
```

并在 class 体内追加:
```java
@Autowired private JdbcTemplate jdbc;
```

- [ ] **Step 2: 运行测试,确认失败**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project
mvn -pl user/api -am test -Dtest=FeedbackServiceTest -DfailIfNoTests=false
```

预期:`pageByUser_returnsOnlyCurrentUser` / `pageByUser_statusFilter_excludesOther` / `pageByUser_emptyResult_returnsEmpty` 编译失败或运行失败(因为 `pageByUser` / `countByUser` 方法不存在)。

- [ ] **Step 3: 实现 Mapper 方法**

修改 `project/user/api/src/main/java/com/aichuangzuo/user/modules/feedback/mapper/FeedbackMapper.java`,在 `@Select("SELECT COUNT(*) ...")` 之后追加:

```java
@Select("""
        SELECT id, user_id AS userId, type, content,
               reply_content AS replyContent,
               reply_admin_id AS replyAdminId,
               replied_at AS repliedAt,
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

- [ ] **Step 4: 实现 Service 接口 + 实现**

修改 `project/user/api/src/main/java/com/aichuangzuo/user/modules/feedback/service/FeedbackService.java`,整文件改为:

```java
package com.aichuangzuo.user.modules.feedback.service;

import com.aichuangzuo.user.modules.feedback.dto.request.SubmitFeedbackRequest;
import com.aichuangzuo.user.modules.feedback.vo.FeedbackVO;

import java.util.List;

public interface FeedbackService {
    Long submit(Long userId, SubmitFeedbackRequest request);

    List<FeedbackVO> pageByUser(Long userId, Integer status, int page, int size);

    long countByUser(Long userId, Integer status);
}
```

修改 `project/user/api/src/main/java/com/aichuangzuo/user/modules/feedback/service/impl/FeedbackServiceImpl.java`,在 `submit` 方法的 `}` 之后、class 的 `}` 之前追加:

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

并添加 import:
```java
import com.aichuangzuo.user.modules.feedback.vo.FeedbackVO;
import java.util.List;
```

- [ ] **Step 5: 跑测试,确认通过**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project
mvn -pl user/api -am test -Dtest=FeedbackServiceTest -DfailIfNoTests=false
```

预期:7 个用例全绿(4 旧 + 3 新)。

- [ ] **Step 6: 提交**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/feedback/ \
        project/user/api/src/test/java/com/aichuangzuo/user/modules/feedback/
git commit -m "feat(user-feedback): pageByUser + countByUser + 单元测试"
```

---

## Task 2: 后端 user-api — Controller 加 `GET /feedback/mine`

**Files:**
- Modify: `project/user/api/src/main/java/com/aichuangzuo/user/modules/feedback/controller/FeedbackController.java`

**Interfaces (produced for Task 3):**
- `GET /api/v1/user/feedback/mine?status=&page=&size= -> Result<Map<String,Object>> {list, total, page, size}`

- [ ] **Step 1: 替换 FeedbackController 整文件**

`project/user/api/src/main/java/com/aichuangzuo/user/modules/feedback/controller/FeedbackController.java` 整文件替换为:

```java
package com.aichuangzuo.user.modules.feedback.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.feedback.dto.request.SubmitFeedbackRequest;
import com.aichuangzuo.user.modules.feedback.service.FeedbackService;
import com.aichuangzuo.user.modules.feedback.vo.FeedbackVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "用户端-意见反馈")
@RestController
@RequestMapping("/api/v1/user/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @Operation(summary = "提交反馈")
    @PostMapping("/submit")
    public Result<Long> submit(@Valid @RequestBody SubmitFeedbackRequest request) {
        Long userId = SecurityUserContext.getCurrentUserId();
        Long id = feedbackService.submit(userId, request);
        return Result.success(id);
    }

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
}
```

- [ ] **Step 2: 编译验证**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project
mvn -pl user/api -am compile -DskipTests
```

预期:`BUILD SUCCESS`。

- [ ] **Step 3: 重启 user-api 加载新端点**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo
./scripts/local/user-full-stack/restart.sh
```

- [ ] **Step 4: curl 自测端点**

```bash
# 登录拿 token(用任一已有测试用户,或建一个)
TOKEN=$(curl -s -X POST http://localhost:25050/api/v1/user/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"<已有测试邮箱>","password":"Test1234!"}' | python3 -c "import json,sys;print(json.load(sys.stdin)['data']['accessToken'])")

# 调 /mine
curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:25050/api/v1/user/feedback/mine?page=1&size=20" | python3 -m json.tool
```

预期:返回 `{"code":0, "data":{"list":[...], "total":N, "page":1, "size":20}, ...}`,无 5xx。

- [ ] **Step 5: 提交**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/feedback/controller/FeedbackController.java
git commit -m "feat(user-feedback): Controller 加 GET /api/v1/user/feedback/mine"
```

---

## Task 3: 前端 user-web — API + 弹框 tabs 改造

**Files:**
- Modify: `project/user/web/src/api/feedback.js`
- Modify: `project/user/web/src/views/console/ConsoleLayout.vue`(弹框模板 line 540-577,提交逻辑 line 1308-1340,样式 line 3024 附近)

- [ ] **Step 1: 加 API 导出**

修改 `project/user/web/src/api/feedback.js`,在末尾追加:

```js
export function pageMyFeedbacks(params) {
  return request.get('/feedback/mine', { params })
}
```

- [ ] **Step 2: 替换弹框模板(tabs 化)**

修改 `project/user/web/src/views/console/ConsoleLayout.vue`,把 line 540-577 的 `<a-modal v-model:open="feedbackVisible">...</a-modal>` 整段替换为:

```vue
          <!-- 反馈弹框(提交 / 我的反馈 tabs) -->
          <a-modal
            v-model:open="feedbackVisible"
            :footer="null"
            :width="640"
            centered
            class="feedback-modal"
            :destroy-on-close="true"
            @cancel="closeFeedbackModal"
          >
            <a-tabs v-model:active-key="feedbackTab" class="feedback-tabs">
              <a-tab-pane key="submit" tab="提交反馈">
                <div class="feedback-panel">
                  <div class="feedback-title">请告诉我们你的想法</div>
                  <div class="feedback-type">
                    <label class="feedback-label">反馈类型</label>
                    <div class="feedback-type-btns">
                      <button
                        v-for="t in feedbackTypes"
                        :key="t"
                        :class="['type-btn', { active: feedbackType === t }]"
                        @click="feedbackType = t"
                      >
                        {{ t }}
                      </button>
                    </div>
                  </div>
                  <div class="feedback-content">
                    <label class="feedback-label">反馈内容</label>
                    <textarea
                      v-model="feedbackContent"
                      class="feedback-textarea"
                      placeholder="请详细描述你的问题或建议..."
                      rows="6"
                      maxlength="2000"
                    ></textarea>
                  </div>
                  <button class="feedback-submit" :disabled="feedbackSubmitting" @click="submitFeedback">
                    {{ feedbackSubmitting ? '提交中...' : '提交反馈' }}
                  </button>
                </div>
              </a-tab-pane>
              <a-tab-pane key="history" tab="我的反馈">
                <div v-if="!historyDetail" class="history-list-pane">
                  <a-segmented
                    v-model:value="historyFilter"
                    :options="historyFilterOptions"
                    block
                    class="history-filter"
                    @change="onHistoryFilterChange"
                  />
                  <a-spin :spinning="historyLoading">
                    <div v-if="historyList.length === 0 && !historyLoading" class="history-empty">
                      <p>还没有反馈记录</p>
                      <a-button type="primary" @click="feedbackTab = 'submit'">去提交</a-button>
                    </div>
                    <ul v-else class="history-list">
                      <li
                        v-for="fb in historyList"
                        :key="fb.id"
                        class="history-item"
                        @click="openHistoryDetail(fb)"
                      >
                        <div class="history-item-row1">
                          <a-tag color="blue">{{ fb.type }}</a-tag>
                          <a-tag :color="fb.status === 0 ? 'orange' : 'green'">
                            {{ fb.status === 0 ? '待回复' : '已回复' }}
                          </a-tag>
                          <span class="history-item-time">{{ formatTime(fb.createdAt) }}</span>
                        </div>
                        <div class="history-item-content">{{ fb.content }}</div>
                        <div v-if="fb.status === 1 && fb.replyContent" class="history-item-reply-preview">
                          管理员回复:{{ fb.replyContent }}
                        </div>
                      </li>
                    </ul>
                    <a-pagination
                      v-if="historyTotal > historySize"
                      :current="historyPage"
                      :page-size="historySize"
                      :total="historyTotal"
                      simple
                      class="history-pager"
                      @change="onHistoryPageChange"
                    />
                  </a-spin>
                </div>
                <div v-else class="history-detail-pane">
                  <a-button type="link" class="history-back-btn" @click="closeHistoryDetail">
                    ← 返回列表
                  </a-button>
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
                  <div v-else class="history-pending-hint">
                    提交成功,我们会尽快处理,谢谢反馈!
                  </div>
                </div>
              </a-tab-pane>
            </a-tabs>
          </a-modal>
```

- [ ] **Step 3: 修改 import + 替换 state/JS**

在 `project/user/web/src/views/console/ConsoleLayout.vue` 的 script setup 内,找到 import 行(line 1077 附近):

```js
import { submitFeedback as submitFeedbackApi } from '@/api/feedback'
```

替换为:

```js
import { submitFeedback as submitFeedbackApi, pageMyFeedbacks } from '@/api/feedback'
```

无需新增 import:`watch` 已在 line 1065 导入(`import { ref, computed, reactive, onMounted, watch, nextTick, provide } from 'vue'`)。

找到 `const feedbackSubmitting = ref(false)` (line 1313),在它之后追加:

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
    historyList.value = res.list || []
    historyTotal.value = res.total || 0
    historyPage.value = res.page
    historySize.value = res.size
  } catch (e) {
    message.error(e?.message || '加载失败')
  } finally {
    historyLoading.value = false
  }
}

const onHistoryFilterChange = () => {
  historyPage.value = 1
  loadHistory()
}

const onHistoryPageChange = (p) => {
  historyPage.value = p
  loadHistory()
}

const openHistoryDetail = (fb) => {
  historyDetail.value = fb
}

const closeHistoryDetail = () => {
  historyDetail.value = null
}

const closeFeedbackModal = () => {
  feedbackTab.value = 'submit'
  historyDetail.value = null
  historyList.value = []
  historyPage.value = 1
  historyFilter.value = 'all'
}

watch(feedbackTab, (t) => {
  if (t === 'history' && historyList.value.length === 0 && !historyDetail.value) {
    loadHistory()
  }
})
```

修改 `submitFeedback` 函数(line 1315-1340),把成功后的逻辑改为:

```js
const submitFeedback = async () => {
  if (!feedbackContent.value.trim()) {
    message.warning('请填写反馈内容')
    return
  }
  if (feedbackSubmitting.value) return
  feedbackSubmitting.value = true
  try {
    await submitFeedbackApi({
      type: feedbackType.value,
      content: feedbackContent.value
    })
    message.success('反馈已收到，我们会尽快处理')
    feedbackContent.value = ''
    feedbackType.value = '功能建议'
    // 切到「我的反馈」tab 并刷新,让用户立刻看到自己的新记录
    feedbackTab.value = 'history'
    historyPage.value = 1
    historyList.value = []  // 强制 watch 重新触发 load
    await loadHistory()
  } catch (e) {
    if (e?.code === 117001) {
      message.warning(e.message || '今日反馈次数已达上限，明天再来')
    } else {
      message.error(e?.message || '提交失败，请稍后再试')
    }
  } finally {
    feedbackSubmitting.value = false
  }
}
```

- [ ] **Step 4: 追加样式**

在 `project/user/web/src/views/console/ConsoleLayout.vue` 的 `<style scoped>` 内,`.feedback-panel {` 选择器之前(line 3024 之前)插入:

```css
.feedback-modal .ant-modal-body { max-height: 70vh; overflow-y: auto; }
.feedback-tabs { margin-top: -8px; }
.feedback-tabs :deep(.ant-tabs-nav) { margin-bottom: 16px; }

.history-list-pane { padding: 0 4px; }
.history-filter { margin-bottom: 12px; }
.history-list { list-style: none; padding: 0; margin: 0; }
.history-item {
  padding: 12px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: background 0.15s;
}
.history-item:hover { background: #fafafa; }
.history-item-row1 {
  display: flex; align-items: center; gap: 8px;
  margin-bottom: 6px; flex-wrap: wrap;
}
.history-item-time { font-size: 12px; color: #8c8c8c; margin-left: auto; }
.history-item-content {
  font-size: 14px; color: #1a1a1a;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical;
  overflow: hidden; text-overflow: ellipsis;
  line-height: 1.5;
}
.history-item-reply-preview {
  font-size: 12px; color: #1677ff; margin-top: 6px;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.history-empty { text-align: center; padding: 32px 0; color: #8c8c8c; }
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

- [ ] **Step 5: 验证 build**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo/project/user/web && npm run build
```

预期:`✓ built in < 5s`,无报错。

- [ ] **Step 6: 重启 user-web + 浏览器自测**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo
./scripts/local/user-web/restart.sh
```

打开 `http://localhost:22345/console/mine`,点铃铛旁「意见反馈」按钮 → 看到「提交反馈」/「我的反馈」两 tab → 切到「我的反馈」tab → 看到列表(空则显示「去提交」)→ 点条目 → 看到详情 → 点「← 返回列表」。

- [ ] **Step 7: 提交**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo
git add project/user/web/src/api/feedback.js \
        project/user/web/src/views/console/ConsoleLayout.vue
git commit -m "feat(user-web): 反馈弹框加「我的反馈」tab + 历史列表/详情"
```

---

## Task 4: E2E 验证 — 扩展 `verify_feedback.py`

**Files:**
- Modify: `tests/e2e/verify_feedback.py`

- [ ] **Step 1: 在 `test_submit_and_reply_and_notify` 函数末尾追加新函数**

打开 `tests/e2e/verify_feedback.py`,在 `if __name__ == "__main__":` 之前追加:

```python
def test_user_feedback_history_api():
    """用户端 GET /feedback/mine:分页 + status 过滤 + 跨用户隔离。"""
    uid_a, token_a, email_a = make_user()
    uid_b, token_b, email_b = make_user()
    print(f"  + created user A id={uid_a} email={email_a}")
    print(f"  + created user B id={uid_b} email={email_b}")

    # 用户 A 提交 2 条待回复
    for i in range(2):
        r = requests.post(
            f"{USER_API}/api/v1/user/feedback/submit",
            headers={"Authorization": f"Bearer {token_a}"},
            json={"type": "功能建议", "content": f"A 的反馈 {i}"},
            timeout=10,
        )
        assert r.status_code == 200 and r.json()["code"] == 0, r.text
    # 用户 B 提交 1 条
    r = requests.post(
        f"{USER_API}/api/v1/user/feedback/submit",
        headers={"Authorization": f"Bearer {token_b}"},
        json={"type": "问题反馈", "content": "B 的反馈"},
        timeout=10,
    )
    assert r.status_code == 200, r.text
    print("  + A 提交 2 条, B 提交 1 条")

    # A 查自己的历史 → 应只看到 2 条
    list_a = requests.get(
        f"{USER_API}/api/v1/user/feedback/mine",
        headers={"Authorization": f"Bearer {token_a}"},
        timeout=10,
    )
    assert list_a.status_code == 200, list_a.text
    body_a = list_a.json()
    assert body_a["code"] == 0, body_a
    assert body_a["data"]["total"] == 2, body_a
    assert len(body_a["data"]["list"]) == 2
    assert all(item["userId"] == uid_a for item in body_a["data"]["list"]), "A 看到 B 的数据了"
    print("  + A 调 /mine 只看到自己 2 条,跨用户隔离 OK")

    # A 加 status=0 过滤 → 应还是 2 条(都待回复)
    list_pending = requests.get(
        f"{USER_API}/api/v1/user/feedback/mine?status=0",
        headers={"Authorization": f"Bearer {token_a}"},
        timeout=10,
    )
    assert list_pending.json()["data"]["total"] == 2, list_pending.text
    # A 加 status=1 过滤 → 应是 0 条
    list_replied = requests.get(
        f"{USER_API}/api/v1/user/feedback/mine?status=1",
        headers={"Authorization": f"Bearer {token_a}"},
        timeout=10,
    )
    assert list_replied.json()["data"]["total"] == 0, list_replied.text
    print("  + status 过滤 OK(0→2 条,1→0 条)")

    # 字段齐全
    item = body_a["data"]["list"][0]
    for key in ("id", "type", "content", "status", "createdAt"):
        assert key in item, f"missing key: {key}"
    print("  + VO 字段齐全")

    # 未登录访问应 401
    noauth = requests.get(
        f"{USER_API}/api/v1/user/feedback/mine", timeout=10
    )
    assert noauth.status_code in (401, 403), f"expected 401/403, got {noauth.status_code}"
    print(f"  + 未登录返回 {noauth.status_code}")

    print("PASS  反馈历史 API:分页 / status 过滤 / 跨用户隔离")
```

- [ ] **Step 2: 修改 `__main__` 调用新函数**

把 `tests/e2e/verify_feedback.py` 末尾的:

```python
if __name__ == "__main__":
    test_submit_and_reply_and_notify()
    sys.exit(0)
```

改为:

```python
if __name__ == "__main__":
    test_submit_and_reply_and_notify()
    test_user_feedback_history_api()
    sys.exit(0)
```

- [ ] **Step 3: 跑 E2E**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo
python3 tests/e2e/verify_feedback.py
```

预期:两个 `PASS` 都打印,无 assertion 失败。

- [ ] **Step 4: 提交**

```bash
cd /Users/panyong/aio_project/ai_chuangzuo
git add tests/e2e/verify_feedback.py
git commit -m "test(e2e): 反馈历史 API(分页/status 过滤/跨用户隔离)"
```

---

## 实施顺序总结

1. **Task 1** (后端 Mapper + Service + 单测)— 7 个用例全绿后 commit
2. **Task 2** (后端 Controller)— 编译 + curl 自测后 commit
3. **Task 3** (前端 API + 弹框)— `npm run build` 通过 + 浏览器目测后 commit
4. **Task 4** (E2E)— 4 段断言全过(`submit/limit/reply/notify` + `mine API`)后 commit

每步独立 commit,可独立回滚。
