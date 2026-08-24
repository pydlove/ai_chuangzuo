package com.aichuangzuo.user.modules.workbench.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.workbench.dto.request.SaveWeeklyArticlesRequest;
import com.aichuangzuo.user.modules.workbench.service.WeeklyArticleService;
import com.aichuangzuo.user.modules.workbench.vo.WeeklyArticleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 工作台每周文章数据 REST 接口。
 *
 * <p>路径前缀：/api/v1/user/workbench/weekly-articles，鉴权由 SecurityConfig 统一拦截。</p>
 */
@Tag(name = "用户端-工作台")
@RestController
@RequestMapping("/api/v1/user/workbench/weekly-articles")
@Slf4j
@RequiredArgsConstructor
public class WeeklyArticleController {

    private final WeeklyArticleService weeklyArticleService;

    /**
     * 查询当前用户本周的文章数据。
     */
    @Operation(summary = "查询本周文章数据")
    @GetMapping
    public Result<List<WeeklyArticleVO>> getCurrentWeek() {
        Long userId = SecurityUserContext.getCurrentUserId();
        return Result.success(weeklyArticleService.getCurrentWeekArticles(userId));
    }

    /**
     * 保存当前用户本周的文章数据（整单替换）。
     */
    @Operation(summary = "保存本周文章数据")
    @PostMapping
    public Result<Void> saveCurrentWeek(@Valid @RequestBody SaveWeeklyArticlesRequest request) {
        Long userId = SecurityUserContext.getCurrentUserId();
        weeklyArticleService.saveCurrentWeekArticles(userId, request);
        return Result.success();
    }
}
