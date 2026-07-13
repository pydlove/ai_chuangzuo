package com.aichuangzuo.user.modules.article.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.article.dto.request.SaveArticleRequest;
import com.aichuangzuo.user.modules.article.dto.request.UpdateArticleRequest;
import com.aichuangzuo.user.modules.article.service.ArticleService;
import com.aichuangzuo.user.modules.article.vo.ArticlePageVO;
import com.aichuangzuo.user.modules.article.vo.ArticleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户作品 REST 接口。
 *
 * <p>路径前缀：/api/v1/user/articles，鉴权由 SecurityConfig 统一拦截。
 */
@Tag(name = "用户作品")
@RestController
@RequestMapping("/api/v1/user/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    /**
     * 分页查询当前用户的作品列表。
     *
     * @param keyword  关键字（标题模糊）
     * @param page     页码（从 1 开始）
     * @param pageSize 每页条数
     */
    @Operation(summary = "我的作品列表")
    @GetMapping
    public Result<ArticlePageVO> list(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "1") long page,
            @RequestParam(name = "pageSize", defaultValue = "20") long pageSize) {
        Long userId = SecurityUserContext.getCurrentUserId();
        return Result.success(articleService.list(userId, keyword, page, pageSize));
    }

    /**
     * 查询单篇作品详情。
     */
    @Operation(summary = "作品详情")
    @GetMapping("/{bizNo}")
    public Result<ArticleVO> get(@PathVariable("bizNo") String bizNo) {
        Long userId = SecurityUserContext.getCurrentUserId();
        return Result.success(articleService.get(userId, bizNo));
    }

    /**
     * 创建作品（生成完成时调用）。
     *
     * @return 新作品的 bizNo
     */
    @Operation(summary = "保存作品")
    @PostMapping
    public Result<String> save(@Valid @RequestBody SaveArticleRequest request) {
        Long userId = SecurityUserContext.getCurrentUserId();
        return Result.success(articleService.save(userId, request));
    }

    /**
     * 修改作品（编辑保存时调用）。
     */
    @Operation(summary = "修改作品")
    @PutMapping("/{bizNo}")
    public Result<Void> update(@PathVariable("bizNo") String bizNo,
                                @Valid @RequestBody UpdateArticleRequest request) {
        Long userId = SecurityUserContext.getCurrentUserId();
        articleService.update(userId, bizNo, request);
        return Result.success();
    }

    /**
     * 软删除作品。
     */
    @Operation(summary = "删除作品")
    @DeleteMapping("/{bizNo}")
    public Result<Void> delete(@PathVariable("bizNo") String bizNo) {
        Long userId = SecurityUserContext.getCurrentUserId();
        articleService.delete(userId, bizNo);
        return Result.success();
    }

    /**
     * 查询当前用户本月已生成作品数。
     */
    @Operation(summary = "本月已生成作品数")
    @GetMapping("/monthly-count")
    public Result<Long> monthlyCount() {
        Long userId = SecurityUserContext.getCurrentUserId();
        return Result.success(articleService.monthlyCount(userId));
    }
}