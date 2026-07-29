package com.aichuangzuo.admin.modules.topictitle.controller;

import com.aichuangzuo.admin.modules.topictitle.dto.request.TopicTitleGenerateRequest;
import com.aichuangzuo.admin.modules.topictitle.dto.request.TopicTitleQueryRequest;
import com.aichuangzuo.admin.modules.topictitle.entity.TopicTitleTask;
import com.aichuangzuo.admin.modules.topictitle.service.TopicTitleService;
import com.aichuangzuo.admin.modules.topictitle.vo.TopicTitlePageVO;
import com.aichuangzuo.admin.modules.topictitle.vo.TopicTitleTaskVO;
import com.aichuangzuo.shared.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端-标题管理 API。
 */
@Tag(name = "管理端-标题管理")
@RestController
@RequestMapping("/api/v1/admin/topic-titles")
@RequiredArgsConstructor
public class TopicTitleAdminController {

    private final TopicTitleService topicTitleService;

    /**
     * 分页列表：keyword + page/pageSize。
     */
    @GetMapping
    public Result<TopicTitlePageVO> list(@ModelAttribute TopicTitleQueryRequest request) {
        return Result.success(topicTitleService.list(request));
    }

    /**
     * AI 批量生成标题（异步）：立刻入队并返回 taskId，不等 AI 返回。
     * 前端按 taskId 轮询 {@code /tasks/{taskId}} 获取进度和最终状态。
     */
    @PostMapping("/generate")
    public Result<Long> generate(@Valid @RequestBody TopicTitleGenerateRequest request) {
        Long taskId = topicTitleService.submitTask(request.getCount(), request.getDirection());
        return Result.success(taskId);
    }

    /**
     * 查询任务状态：status / generatedCount / failedReason 等。
     */
    @GetMapping("/tasks/{taskId}")
    public Result<TopicTitleTaskVO> getTask(@PathVariable Long taskId) {
        TopicTitleTask task = topicTitleService.getTask(taskId);
        return Result.success(TopicTitleTaskVO.from(task));
    }

    /**
     * 逻辑删除标题。
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        topicTitleService.delete(id);
        return Result.success();
    }
}