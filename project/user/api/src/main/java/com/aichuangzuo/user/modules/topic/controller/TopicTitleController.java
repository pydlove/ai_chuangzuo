package com.aichuangzuo.user.modules.topic.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.topic.service.TopicTitleService;
import com.aichuangzuo.user.modules.topic.vo.TopicTitleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户端-选题标题库接口。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/user/topics")
@RequiredArgsConstructor
public class TopicTitleController {

    private final TopicTitleService topicTitleService;

    /**
     * 随机拉取 N 条标题（排除我已用过 + 已删除）。
     */
    @GetMapping("/random")
    public Result<List<TopicTitleVO>> random(@RequestParam(defaultValue = "6") int count) {
        Long userId = SecurityUserContext.getCurrentUserId();
        log.info("随机拉取选题标题 userId={} count={}", userId, count);
        return Result.success(topicTitleService.random(userId, count));
    }

    /**
     * 上报使用：幂等；标题不存在或已删除返回 404。
     */
    @PostMapping("/{id}/use")
    public Result<Void> use(@PathVariable Long id) {
        Long userId = SecurityUserContext.getCurrentUserId();
        log.info("上报使用选题标题 userId={} topicId={}", userId, id);
        topicTitleService.use(userId, id);
        return Result.success();
    }
}
