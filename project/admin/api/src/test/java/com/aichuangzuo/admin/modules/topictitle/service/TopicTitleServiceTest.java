package com.aichuangzuo.admin.modules.topictitle.service;

import com.aichuangzuo.admin.modules.generation.service.AiCallResult;
import com.aichuangzuo.admin.modules.generation.service.GenerationAiService;
import com.aichuangzuo.admin.modules.modelconfig.entity.ModelConfig;
import com.aichuangzuo.admin.modules.modelconfig.mapper.ModelConfigMapper;
import com.aichuangzuo.admin.modules.topictitle.dto.request.TopicTitleQueryRequest;
import com.aichuangzuo.admin.modules.topictitle.entity.TopicTitleTask;
import com.aichuangzuo.admin.modules.topictitle.mapper.TopicTitleMapper;
import com.aichuangzuo.admin.modules.topictitle.mapper.TopicTitleTaskMapper;
import com.aichuangzuo.admin.modules.topictitle.vo.TopicTitlePageVO;
import com.aichuangzuo.shared.entity.TopicTitle;
import com.aichuangzuo.shared.enums.error.AdminGenerationErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
@Rollback
class TopicTitleServiceTest {

    @Autowired
    private TopicTitleService topicTitleService;

    @Autowired
    private TopicTitleMapper topicTitleMapper;

    @Autowired
    private TopicTitleTaskMapper topicTitleTaskMapper;

    @Autowired
    private ModelConfigMapper modelConfigMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** mock AI 调用，不真请求模型厂商。 */
    @MockBean
    private GenerationAiService generationAiService;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM t_topic_title_task");
        jdbcTemplate.update("DELETE FROM u_topic_title_usage");
        jdbcTemplate.update("DELETE FROM u_topic_title");
        // 保证存在一条 active 模型配置（事务回滚，不污染库）
        ModelConfig cfg = new ModelConfig();
        cfg.setProviderType("test-provider");
        cfg.setBaseUrl("https://example.test");
        cfg.setApiKeyEncrypted("dummy");
        cfg.setModelCode("test-model");
        cfg.setModelName("测试模型");
        cfg.setIsActive(1);
        modelConfigMapper.insert(cfg);
    }

    @Test
    void executeTask_validJson_insertsRows() {
        mockAiReturn("{\"titles\": ["
                + "{\"title\": \"工作 3 年没升职？可能是这 3 个习惯在拖后腿\", \"summary\": \"围绕以下观点创作，1、指出大多数人把“努力”等同于“存在感”，反而忽略了结果导向；2、列举 3 个看似无害却消耗信任的工作习惯；3、给出可立即调整的沟通与汇报方式，让晋升水到渠成。\"},"
                + "{\"title\": \"我用 AI 写作月入过万\", \"summary\": \"围绕以下观点创作，1、坦诚分享从 0 到 1 的接单渠道与定价策略；2、对比纯人工与 AI 辅助的工作流差异；3、提醒新手避开的 3 个常见坑，强调持续输出比工具更重要。\"}"
                + "]}");

        TopicTitleTask task = runSync(2, "职场效率类");

        assertEquals(2, task.getStatus()); // COMPLETED
        assertEquals(2, task.getGeneratedCount());
        TopicTitlePageVO page = topicTitleService.list(new TopicTitleQueryRequest());
        assertEquals(2, page.getTotal());
        assertEquals("职场效率类", page.getList().get(0).getDirection());
        assertEquals(0, page.getList().get(0).getUseCount());
    }

    @Test
    void executeTask_jsonWithPreambleAndFence_parsesAfterCleaning() {
        mockAiReturn("好的，以下是为您生成的标题：\n```json\n"
                + "{\"titles\": [{\"title\": \"为什么你越努力越焦虑\", \"summary\": \"围绕以下观点创作，1、拆解“努力=安全”这一默认等式为何失效；2、指出 3 个让人越忙越慌的思维陷阱；3、给出 2 个把努力从消耗转为积累的具体视角。\"}]}\n"
                + "```\n希望对您有帮助！");

        TopicTitleTask task = runSync(1, "情感成长");

        assertEquals(2, task.getStatus());
        assertEquals(1, task.getGeneratedCount());
        assertEquals(1, topicTitleService.list(new TopicTitleQueryRequest()).getTotal());
    }

    @Test
    void executeTask_pureGarbage_marksFailedAndInsertsNothing() {
        mockAiReturn("这不是 JSON，完全是垃圾输出");

        TopicTitleTask task = runSync(3, "职场");

        assertEquals(3, task.getStatus()); // FAILED
        assertEquals(0, task.getGeneratedCount());
        assertEquals(0, topicTitleService.list(new TopicTitleQueryRequest()).getTotal());
        // failedReason 携带错误码字符串
        assertNotNull(task.getFailedReason());
        assertTrue(task.getFailedReason().contains(String.valueOf(
                AdminGenerationErrorCode.TOPIC_TITLE_GENERATE_FAILED.getCode())),
                "failedReason 应包含错误码，实际: " + task.getFailedReason());
    }

    @Test
    void executeTask_emptyTitlesArray_marksFailed() {
        mockAiReturn("{\"titles\": []}");

        TopicTitleTask task = runSync(2, null);

        assertEquals(3, task.getStatus());
        assertEquals(0, task.getGeneratedCount());
    }

    @Test
    void executeTask_blankEntriesSkipped_validRemainderInserted() {
        mockAiReturn("{\"titles\": ["
                + "{\"title\": \"\", \"summary\": \"标题为空应被剔除\"},"
                + "{\"title\": \"月薪 5000 如何一年存下 3 万\", \"summary\": \"围绕以下观点创作，1、先破除“存钱=降低生活质量”的误解；2、列出可复制的月度预算分配比例；3、给出 3 个不牺牲日常愉悦感的节流动作。\"}"
                + "]}");

        TopicTitleTask task = runSync(2, "生活技巧");

        assertEquals(2, task.getStatus());
        assertEquals(1, task.getGeneratedCount());
    }

    @Test
    void executeTask_unescapedQuotesInsideSummary_areEscapedAndParsed() {
        // 模拟 MiniMax 等模型在 summary 中输出未转义的英文双引号
        mockAiReturn("{\"titles\": [{\"title\": \"AI 时代，普通人如何保住自己的饭碗\", "
                + "\"summary\": \"围绕以下观点创作，1、AI 正在改变岗位结构，但\"被替代\"和\"被增强\"是两种不同结果，从案例角度说明普通人和 AI 协作的可行路径；2、列出三类不容易被替代的底层能力。\"}]}");

        TopicTitleTask task = runSync(1, "AI 职场");

        assertEquals(2, task.getStatus());
        assertEquals(1, task.getGeneratedCount());
        TopicTitlePageVO page = topicTitleService.list(new TopicTitleQueryRequest());
        assertEquals(1, page.getTotal());
        assertTrue(page.getList().get(0).getSummary().contains("\"被替代\""));
    }

    @Test
    void submitTask_noActiveModel_throws() {
        // 把 active=1 的配置清掉
        jdbcTemplate.update("UPDATE t_model_config SET is_active = 0");
        assertThrows(RuntimeException.class, () -> topicTitleService.submitTask(2, "职场"));
    }

    @Test
    void list_paginationAndKeywordFilter() {
        insertTitle("职场效率提升指南");
        insertTitle("情感成长必修课");
        insertTitle("职场沟通的艺术");

        TopicTitleQueryRequest req = new TopicTitleQueryRequest();
        req.setKeyword("职场");
        req.setPage(1);
        req.setPageSize(10);
        TopicTitlePageVO page = topicTitleService.list(req);
        assertEquals(2, page.getTotal());
        assertTrue(page.getList().stream().allMatch(v -> v.getTitle().contains("职场")));

        req.setKeyword(null);
        req.setPageSize(2);
        TopicTitlePageVO paged = topicTitleService.list(req);
        assertEquals(3, paged.getTotal());
        assertEquals(2, paged.getList().size());
    }

    @Test
    void list_usedStatusFilter() {
        insertTitle("未使用标题");
        TopicTitle used = insertTitle("已使用标题");
        used.setUseCount(3);
        topicTitleMapper.updateById(used);

        TopicTitleQueryRequest req = new TopicTitleQueryRequest();
        req.setUsedStatus(0);
        TopicTitlePageVO unused = topicTitleService.list(req);
        assertEquals(1, unused.getTotal());
        assertEquals("未使用标题", unused.getList().get(0).getTitle());

        req.setUsedStatus(1);
        TopicTitlePageVO usedPage = topicTitleService.list(req);
        assertEquals(1, usedPage.getTotal());
        assertEquals("已使用标题", usedPage.getList().get(0).getTitle());

        req.setUsedStatus(null);
        assertEquals(2, topicTitleService.list(req).getTotal());
    }

    @Test
    void delete_logicalDelete_excludedFromPool() {
        TopicTitle t = insertTitle("待删除标题");

        topicTitleService.delete(t.getId());

        // @TableLogic：selectById 自动过滤 is_deleted=1，用户端随机池同样不可见
        assertNull(topicTitleMapper.selectById(t.getId()));
        assertEquals(0, topicTitleService.list(new TopicTitleQueryRequest()).getTotal());
    }

    /**
     * 同步执行一个任务：submit → execute → 重新加载 task。模拟 worker 在单测里同步跑。
     */
    private TopicTitleTask runSync(int count, String direction) {
        Long taskId = topicTitleService.submitTask(count, direction);
        topicTitleService.executeTask(taskId);
        return topicTitleTaskMapper.selectById(taskId);
    }

    private void mockAiReturn(String content) {
        when(generationAiService.call(any(), any(), any(), isNull()))
                .thenReturn(new AiCallResult(content, null, null, null));
    }

    private TopicTitle insertTitle(String title) {
        TopicTitle entity = new TopicTitle();
        entity.setTitle(title);
        entity.setSummary("概要：" + title);
        entity.setDirection("测试方向");
        entity.setUseCount(0);
        entity.setTenantId(0L);
        topicTitleMapper.insert(entity);
        return entity;
    }
}