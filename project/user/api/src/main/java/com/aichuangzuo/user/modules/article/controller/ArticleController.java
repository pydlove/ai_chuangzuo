package com.aichuangzuo.user.modules.article.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.article.dto.request.SaveArticleRequest;
import com.aichuangzuo.user.modules.article.dto.request.UpdateArticleRequest;
import com.aichuangzuo.user.modules.article.service.ArticleService;
import com.aichuangzuo.user.modules.article.service.TitleOptimizeService;
import com.aichuangzuo.user.modules.article.vo.ArticlePageVO;
import com.aichuangzuo.user.modules.article.vo.ArticleVO;
import com.aichuangzuo.user.modules.article.vo.TitleOptimizeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;
    private final TitleOptimizeService titleOptimizeService;

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
        log.info("查询作品列表, userId={}, keyword={}, page={}, pageSize={}", userId, keyword, page, pageSize);
        return Result.success(articleService.list(userId, keyword, page, pageSize));
    }

    /**
     * 查询单篇作品详情。
     */
    @Operation(summary = "作品详情")
    @GetMapping("/{bizNo}")
    public Result<ArticleVO> get(@PathVariable("bizNo") String bizNo) {
        Long userId = SecurityUserContext.getCurrentUserId();
        log.info("查询作品详情, userId={}, bizNo={}", userId, bizNo);
        return Result.success(articleService.get(userId, bizNo));
    }

    /**
     * 根据生成任务 ID 查询作品详情（工作台查看兜底）。
     */
    @Operation(summary = "根据任务ID查询作品")
    @GetMapping("/by-task/{taskId}")
    public Result<ArticleVO> getByTaskId(@PathVariable("taskId") Long taskId) {
        Long userId = SecurityUserContext.getCurrentUserId();
        log.info("根据任务ID查询作品, userId={}, taskId={}", userId, taskId);
        return Result.success(articleService.getByTaskId(userId, taskId));
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
        log.info("保存作品, userId={}, title={}, platform={}, wordCount={}", userId, request.getTitle(), request.getPlatform(), request.getWordCount());
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
        log.info("修改作品, userId={}, bizNo={}, title={}", userId, bizNo, request.getTitle());
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
        log.info("删除作品, userId={}, bizNo={}", userId, bizNo);
        articleService.delete(userId, bizNo);
        return Result.success();
    }

    /**
     * 生成单篇作品的临时导出 token，用于免登录下载 Word。
     */
    @Operation(summary = "生成作品导出 token")
    @GetMapping("/{bizNo}/export-token")
    public Result<String> generateExportToken(@PathVariable("bizNo") String bizNo) {
        Long userId = SecurityUserContext.getCurrentUserId();
        log.info("生成作品导出token, userId={}, bizNo={}", userId, bizNo);
        return Result.success(articleService.generateExportToken(userId, bizNo));
    }

    /**
     * AI 标题优化：有权益的用户首次点击调用大模型生成，之后返回首次缓存结果。
     */
    @Operation(summary = "AI 标题优化")
    @PostMapping("/{bizNo}/title-optimize")
    public Result<TitleOptimizeVO> titleOptimize(@PathVariable("bizNo") String bizNo) {
        Long userId = SecurityUserContext.getCurrentUserId();
        log.info("AI标题优化, userId={}, bizNo={}", userId, bizNo);
        return Result.success(titleOptimizeService.optimize(userId, bizNo));
    }

    /**
     * 查询当前用户本月已生成作品数。
     */
    @Operation(summary = "本月已生成作品数")
    @GetMapping("/monthly-count")
    public Result<Long> monthlyCount() {
        Long userId = SecurityUserContext.getCurrentUserId();
        log.info("查询本月已生成作品数, userId={}", userId);
        return Result.success(articleService.monthlyCount(userId));
    }
}