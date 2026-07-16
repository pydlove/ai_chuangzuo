package com.aichuangzuo.admin.modules.topictitle.controller;

import com.aichuangzuo.admin.modules.topictitle.dto.request.TopicTitleGenerateRequest;
import com.aichuangzuo.admin.modules.topictitle.dto.request.TopicTitleQueryRequest;
import com.aichuangzuo.admin.modules.topictitle.service.TopicTitleService;
import com.aichuangzuo.admin.modules.topictitle.vo.TopicTitleGenerateVO;
import com.aichuangzuo.admin.modules.topictitle.vo.TopicTitlePageVO;
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
     * AI 批量生成标题入库（同步调用），返回实际入库条数。
     */
    @PostMapping("/generate")
    public Result<TopicTitleGenerateVO> generate(@Valid @RequestBody TopicTitleGenerateRequest request) {
        int generated = topicTitleService.generate(request.getCount(), request.getDirection());
        return Result.success(new TopicTitleGenerateVO(generated));
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
