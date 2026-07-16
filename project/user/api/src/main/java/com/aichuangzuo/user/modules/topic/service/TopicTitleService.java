package com.aichuangzuo.user.modules.topic.service;

import com.aichuangzuo.shared.entity.TopicTitle;
import com.aichuangzuo.shared.exception.NotFoundException;
import com.aichuangzuo.user.modules.topic.mapper.TopicTitleMapper;
import com.aichuangzuo.user.modules.topic.mapper.TopicTitleUsageMapper;
import com.aichuangzuo.user.modules.topic.vo.TopicTitleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户端-选题标题库：随机拉取 + 使用上报。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TopicTitleService {

    /** 单次拉取数量上限（前端固定 6，这里兜底防刷）。 */
    private static final int MAX_RANDOM_COUNT = 20;

    private final TopicTitleMapper topicTitleMapper;
    private final TopicTitleUsageMapper topicTitleUsageMapper;

    /**
     * 随机拉取标题：排除「我已用过 + 已删除」。库中可用不足 count 时有多少返回多少。
     */
    public List<TopicTitleVO> random(Long userId, int count) {
        int c = Math.min(Math.max(count, 1), MAX_RANDOM_COUNT);
        return topicTitleMapper.selectRandomExcludeUsed(userId, c).stream()
                .map(TopicTitleVO::from)
                .toList();
    }

    /**
     * 上报使用：插 usage（唯一键冲突忽略，幂等）；仅当 usage 行实际插入成功才 use_count + 1。
     *
     * @throws NotFoundException 标题不存在或已删除
     */
    @Transactional
    public void use(Long userId, Long titleId) {
        TopicTitle title = topicTitleMapper.selectById(titleId);
        if (title == null) {
            throw new NotFoundException("标题不存在");
        }
        int inserted = topicTitleUsageMapper.insertIgnore(userId, titleId);
        if (inserted == 1) {
            topicTitleMapper.incrementUseCount(titleId);
        }
    }
}
