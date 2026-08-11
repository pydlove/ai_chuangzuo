package com.aichuangzuo.admin.modules.learn.controller;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.learn.dto.request.LearnArticlePageQuery;
import com.aichuangzuo.admin.modules.learn.dto.request.LearnArticleReq;
import com.aichuangzuo.admin.modules.learn.dto.request.LearnBannerReq;
import com.aichuangzuo.admin.modules.learn.dto.request.LearnCategoryReq;
import com.aichuangzuo.admin.modules.learn.dto.request.LearnSortReq;
import com.aichuangzuo.admin.modules.learn.service.LearnArticleService;
import com.aichuangzuo.admin.modules.learn.service.LearnBannerService;
import com.aichuangzuo.admin.modules.learn.service.LearnCategoryService;
import com.aichuangzuo.admin.modules.learn.vo.LearnArticleDetail;
import com.aichuangzuo.admin.modules.learn.vo.LearnBannerVO;
import com.aichuangzuo.admin.modules.learn.vo.LearnCategoryTreeNode;
import com.aichuangzuo.shared.result.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "创作学院管理")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/learn")
@RequiredArgsConstructor
public class LearnAdminController {

    private final LearnCategoryService categoryService;
    private final LearnArticleService articleService;
    private final LearnBannerService bannerService;

    // ---------- 分类 ----------

    @Operation(summary = "分类树")
    @GetMapping("/category/tree")
    public Result<List<LearnCategoryTreeNode>> categoryTree() {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员查询创作学院分类树, adminUserId={}", adminUserId);
        return Result.success(categoryService.tree());
    }

    @Operation(summary = "新增分类")
    @PostMapping("/category")
    public Result<Long> createCategory(@Valid @RequestBody LearnCategoryReq req) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员新增创作学院分类, adminUserId={}, name={}, parentId={}",
                adminUserId, req.getName(), req.getParentId());
        return Result.success(categoryService.create(req));
    }

    @Operation(summary = "更新分类")
    @PutMapping("/category/{id}")
    public Result<Void> updateCategory(@PathVariable Long id, @Valid @RequestBody LearnCategoryReq req) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员更新创作学院分类, adminUserId={}, categoryId={}, name={}",
                adminUserId, id, req.getName());
        categoryService.update(id, req);
        return Result.success();
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/category/{id}")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员删除创作学院分类, adminUserId={}, categoryId={}", adminUserId, id);
        categoryService.delete(id);
        return Result.success();
    }

    @Operation(summary = "批量拖拽排序（分类）")
    @PostMapping("/category/sort")
    public Result<Void> sortCategory(@RequestBody LearnSortReq req) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员排序创作学院分类, adminUserId={}, itemCount={}",
                adminUserId, req.getItems() == null ? 0 : req.getItems().size());
        categoryService.sortBatch(req.getItems());
        return Result.success();
    }

    // ---------- 文章 ----------

    @Operation(summary = "文章分页")
    @GetMapping("/article/page")
    public Result<IPage<LearnArticleDetail>> articlePage(LearnArticlePageQuery q) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员查询创作学院文章列表, adminUserId={}, categoryId={}, status={}, keyword={}, page={}, size={}",
                adminUserId, q.getCategoryId(), q.getStatus(), q.getKeyword(), q.getPage(), q.getSize());
        return Result.success(articleService.page(q));
    }

    @Operation(summary = "文章详情")
    @GetMapping("/article/{id}")
    public Result<LearnArticleDetail> articleDetail(@PathVariable Long id) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员查询创作学院文章详情, adminUserId={}, articleId={}", adminUserId, id);
        return Result.success(articleService.detail(id));
    }

    @Operation(summary = "新增文章")
    @PostMapping("/article")
    public Result<Long> createArticle(@Valid @RequestBody LearnArticleReq req) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员新增创作学院文章, adminUserId={}, title={}, categoryId={}",
                adminUserId, req.getTitle(), req.getCategoryId());
        return Result.success(articleService.create(req));
    }

    @Operation(summary = "更新文章")
    @PutMapping("/article/{id}")
    public Result<Void> updateArticle(@PathVariable Long id, @Valid @RequestBody LearnArticleReq req) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员更新创作学院文章, adminUserId={}, articleId={}, title={}",
                adminUserId, id, req.getTitle());
        articleService.update(id, req);
        return Result.success();
    }

    @Operation(summary = "删除文章")
    @DeleteMapping("/article/{id}")
    public Result<Void> deleteArticle(@PathVariable Long id) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员删除创作学院文章, adminUserId={}, articleId={}", adminUserId, id);
        articleService.delete(id);
        return Result.success();
    }

    @Operation(summary = "发布文章")
    @PostMapping("/article/{id}/publish")
    public Result<Void> publishArticle(@PathVariable Long id) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员发布创作学院文章, adminUserId={}, articleId={}", adminUserId, id);
        articleService.publish(id);
        return Result.success();
    }

    @Operation(summary = "下线文章")
    @PostMapping("/article/{id}/unpublish")
    public Result<Void> unpublishArticle(@PathVariable Long id) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员下线创作学院文章, adminUserId={}, articleId={}", adminUserId, id);
        articleService.unpublish(id);
        return Result.success();
    }

    @Operation(summary = "移动文章分类")
    @PostMapping("/article/{id}/move")
    public Result<Void> moveArticle(@PathVariable Long id, @RequestBody LearnArticleReq req) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员移动创作学院文章分类, adminUserId={}, articleId={}, categoryId={}",
                adminUserId, id, req.getCategoryId());
        articleService.move(id, req.getCategoryId());
        return Result.success();
    }

    @Operation(summary = "设置文章推荐")
    @PostMapping("/article/{id}/recommend")
    public Result<Void> recommendArticle(@PathVariable Long id, @RequestParam Integer recommended) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员设置创作学院文章推荐, adminUserId={}, articleId={}, recommended={}",
                adminUserId, id, recommended);
        articleService.setRecommended(id, recommended);
        return Result.success();
    }

    @Operation(summary = "批量拖拽排序（文章）")
    @PostMapping("/article/sort")
    public Result<Void> sortArticle(@RequestBody LearnSortReq req) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员排序创作学院文章, adminUserId={}, itemCount={}",
                adminUserId, req.getItems() == null ? 0 : req.getItems().size());
        articleService.sortBatch(req.getItems());
        return Result.success();
    }

    // ---------- Banner ----------

    @Operation(summary = "Banner 列表")
    @GetMapping("/banner")
    public Result<List<LearnBannerVO>> bannerList() {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员查询创作学院 Banner 列表, adminUserId={}", adminUserId);
        return Result.success(bannerService.list());
    }

    @Operation(summary = "新增 Banner")
    @PostMapping("/banner")
    public Result<Long> createBanner(@Valid @RequestBody LearnBannerReq req) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员新增创作学院 Banner, adminUserId={}, linkUrl={}", adminUserId, req.getLinkUrl());
        return Result.success(bannerService.create(req));
    }

    @Operation(summary = "更新 Banner")
    @PutMapping("/banner/{id}")
    public Result<Void> updateBanner(@PathVariable Long id, @Valid @RequestBody LearnBannerReq req) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员更新创作学院 Banner, adminUserId={}, bannerId={}, linkUrl={}",
                adminUserId, id, req.getLinkUrl());
        bannerService.update(id, req);
        return Result.success();
    }

    @Operation(summary = "删除 Banner")
    @DeleteMapping("/banner/{id}")
    public Result<Void> deleteBanner(@PathVariable Long id) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员删除创作学院 Banner, adminUserId={}, bannerId={}", adminUserId, id);
        bannerService.delete(id);
        return Result.success();
    }
}
