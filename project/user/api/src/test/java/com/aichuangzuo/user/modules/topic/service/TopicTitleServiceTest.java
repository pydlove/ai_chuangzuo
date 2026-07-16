package com.aichuangzuo.user.modules.topic.service;

import com.aichuangzuo.shared.entity.TopicTitle;
import com.aichuangzuo.shared.exception.NotFoundException;
import com.aichuangzuo.user.modules.topic.entity.TopicTitleUsage;
import com.aichuangzuo.user.modules.topic.mapper.TopicTitleMapper;
import com.aichuangzuo.user.modules.topic.mapper.TopicTitleUsageMapper;
import com.aichuangzuo.user.modules.topic.vo.TopicTitleVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@Rollback
class TopicTitleServiceTest {

    @Autowired
    private TopicTitleService topicTitleService;

    @Autowired
    private TopicTitleMapper topicTitleMapper;

    @Autowired
    private TopicTitleUsageMapper topicTitleUsageMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** 清空两张表（事务内物理删除，测试结束回滚），保证用例间确定性。 */
    @BeforeEach
    void cleanTables() {
        jdbcTemplate.update("DELETE FROM u_topic_title_usage");
        jdbcTemplate.update("DELETE FROM u_topic_title");
    }

    @Test
    void random_excludesUsedTitles() {
        TopicTitle used = insertTitle("已用过的标题", false);
        TopicTitle fresh1 = insertTitle("新鲜标题一", false);
        TopicTitle fresh2 = insertTitle("新鲜标题二", false);
        topicTitleService.use(1001L, used.getId());

        List<TopicTitleVO> result = topicTitleService.random(1001L, 10);

        List<Long> ids = result.stream().map(TopicTitleVO::getId).toList();
        assertFalse(ids.contains(used.getId()));
        assertTrue(ids.contains(fresh1.getId()));
        assertTrue(ids.contains(fresh2.getId()));
    }

    @Test
    void random_excludesUsed_onlyForCurrentUser() {
        TopicTitle t = insertTitle("A 用过 B 没用", false);
        topicTitleService.use(1001L, t.getId());

        // B 用户不受影响，仍能拉到
        List<TopicTitleVO> forB = topicTitleService.random(1002L, 10);
        assertTrue(forB.stream().anyMatch(v -> v.getId().equals(t.getId())));
    }

    @Test
    void random_excludesDeletedTitles() {
        TopicTitle deleted = insertTitle("已删除的标题", true);
        TopicTitle alive = insertTitle("正常标题", false);

        List<TopicTitleVO> result = topicTitleService.random(1001L, 10);

        List<Long> ids = result.stream().map(TopicTitleVO::getId).toList();
        assertFalse(ids.contains(deleted.getId()));
        assertTrue(ids.contains(alive.getId()));
    }

    @Test
    void random_emptyLibrary_returnsEmptyList() {
        List<TopicTitleVO> result = topicTitleService.random(1001L, 6);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void use_idempotent_duplicateUseCountsOnce() {
        TopicTitle t = insertTitle("幂等标题", false);

        topicTitleService.use(1001L, t.getId());
        topicTitleService.use(1001L, t.getId());
        topicTitleService.use(1001L, t.getId());

        List<TopicTitleUsage> usages = topicTitleUsageMapper.selectList(
                new LambdaQueryWrapper<TopicTitleUsage>()
                        .eq(TopicTitleUsage::getUserId, 1001L)
                        .eq(TopicTitleUsage::getTitleId, t.getId()));
        assertEquals(1, usages.size());
        assertEquals(1, topicTitleMapper.selectById(t.getId()).getUseCount());
    }

    @Test
    void use_countAccumulatesAcrossUsers() {
        TopicTitle t = insertTitle("多人使用标题", false);

        topicTitleService.use(1001L, t.getId());
        topicTitleService.use(1002L, t.getId());

        assertEquals(2, topicTitleMapper.selectById(t.getId()).getUseCount());
    }

    @Test
    void use_deletedTitle_throwsNotFound() {
        TopicTitle t = insertTitle("被删标题", true);

        assertThrows(NotFoundException.class, () -> topicTitleService.use(1001L, t.getId()));
    }

    private TopicTitle insertTitle(String title, boolean deleted) {
        TopicTitle entity = new TopicTitle();
        entity.setTitle(title);
        entity.setSummary("概要：" + title);
        entity.setDirection("测试方向");
        entity.setUseCount(0);
        entity.setTenantId(0L);
        entity.setIsDeleted(deleted ? 1 : 0);
        topicTitleMapper.insert(entity);
        return entity;
    }
}
