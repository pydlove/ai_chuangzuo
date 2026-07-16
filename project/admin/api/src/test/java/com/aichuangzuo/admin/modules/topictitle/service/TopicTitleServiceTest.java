package com.aichuangzuo.admin.modules.topictitle.service;

import com.aichuangzuo.admin.modules.generation.service.AiCallResult;
import com.aichuangzuo.admin.modules.generation.service.GenerationAiService;
import com.aichuangzuo.admin.modules.modelconfig.entity.ModelConfig;
import com.aichuangzuo.admin.modules.modelconfig.mapper.ModelConfigMapper;
import com.aichuangzuo.admin.modules.topictitle.dto.request.TopicTitleQueryRequest;
import com.aichuangzuo.admin.modules.topictitle.mapper.TopicTitleMapper;
import com.aichuangzuo.admin.modules.topictitle.vo.TopicTitlePageVO;
import com.aichuangzuo.shared.entity.TopicTitle;
import com.aichuangzuo.shared.enums.error.AdminGenerationErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
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
    private ModelConfigMapper modelConfigMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** mock AI 调用，不真请求模型厂商。 */
    @MockBean
    private GenerationAiService generationAiService;

    @BeforeEach
    void setUp() {
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
    void generate_validJson_insertsRows() {
        mockAiReturn("{\"titles\": ["
                + "{\"title\": \"工作 3 年没升职？可能是这 3 个习惯在拖后腿\", \"summary\": \"分析职场晋升受阻的常见习惯\"},"
                + "{\"title\": \"我用 AI 写作月入过万\", \"summary\": \"新手可复制的 AI 写作变现路径\"}"
                + "]}");

        int generated = topicTitleService.generate(2, "职场效率类");

        assertEquals(2, generated);
        TopicTitlePageVO page = topicTitleService.list(new TopicTitleQueryRequest());
        assertEquals(2, page.getTotal());
        assertEquals("职场效率类", page.getList().get(0).getDirection());
        assertEquals(0, page.getList().get(0).getUseCount());
    }

    @Test
    void generate_jsonWithPreambleAndFence_parsesAfterCleaning() {
        mockAiReturn("好的，以下是为您生成的标题：\n```json\n"
                + "{\"titles\": [{\"title\": \"为什么你越努力越焦虑\", \"summary\": \"3 个思维陷阱正在消耗你\"}]}\n"
                + "```\n希望对您有帮助！");

        int generated = topicTitleService.generate(1, "情感成长");

        assertEquals(1, generated);
        assertEquals(1, topicTitleService.list(new TopicTitleQueryRequest()).getTotal());
    }

    @Test
    void generate_pureGarbage_throwsAndInsertsNothing() {
        mockAiReturn("这不是 JSON，完全是垃圾输出");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> topicTitleService.generate(3, "职场"));
        assertEquals(AdminGenerationErrorCode.TOPIC_TITLE_GENERATE_FAILED.getCode(), ex.getCode());
        assertEquals(0, topicTitleService.list(new TopicTitleQueryRequest()).getTotal());
    }

    @Test
    void generate_emptyTitlesArray_throws() {
        mockAiReturn("{\"titles\": []}");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> topicTitleService.generate(2, null));
        assertEquals(AdminGenerationErrorCode.TOPIC_TITLE_GENERATE_FAILED.getCode(), ex.getCode());
    }

    @Test
    void generate_blankEntriesSkipped_validRemainderInserted() {
        mockAiReturn("{\"titles\": ["
                + "{\"title\": \"\", \"summary\": \"标题为空应被剔除\"},"
                + "{\"title\": \"月薪 5000 如何一年存下 3 万\", \"summary\": \"我的省钱清单公开\"}"
                + "]}");

        int generated = topicTitleService.generate(2, "生活技巧");

        assertEquals(1, generated);
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
    void delete_logicalDelete_excludedFromPool() {
        TopicTitle t = insertTitle("待删除标题");

        topicTitleService.delete(t.getId());

        // @TableLogic：selectById 自动过滤 is_deleted=1，用户端随机池同样不可见
        assertNull(topicTitleMapper.selectById(t.getId()));
        assertEquals(0, topicTitleService.list(new TopicTitleQueryRequest()).getTotal());
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
