package com.aichuangzuo.admin.modules.learn.controller;

import com.aichuangzuo.admin.modules.learn.dto.request.LearnArticlePageQuery;
import com.aichuangzuo.admin.modules.learn.dto.request.LearnArticleReq;
import com.aichuangzuo.admin.modules.learn.dto.request.LearnCategoryReq;
import com.aichuangzuo.admin.modules.learn.dto.request.LearnSortReq;
import com.aichuangzuo.admin.modules.learn.service.LearnArticleService;
import com.aichuangzuo.admin.modules.learn.service.LearnCategoryService;
import com.aichuangzuo.admin.modules.learn.vo.LearnArticleDetail;
import com.aichuangzuo.admin.modules.learn.vo.LearnCategoryTreeNode;
import com.aichuangzuo.shared.result.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "创作学院管理")
@RestController
@RequestMapping("/api/v1/admin/learn")
@RequiredArgsConstructor
public class LearnAdminController {

    private final LearnCategoryService categoryService;
    private final LearnArticleService articleService;

    // ---------- 分类 ----------

    @Operation(summary = "分类树")
    @GetMapping("/category/tree")
    public Result<List<LearnCategoryTreeNode>> categoryTree() {
        return Result.success(categoryService.tree());
    }

    @Operation(summary = "新增分类")
    @PostMapping("/category")
    public Result<Long> createCategory(@Valid @RequestBody LearnCategoryReq req) {
        return Result.success(categoryService.create(req));
    }

    @Operation(summary = "更新分类")
    @PutMapping("/category/{id}")
    public Result<Void> updateCategory(@PathVariable Long id, @Valid @RequestBody LearnCategoryReq req) {
        categoryService.update(id, req);
        return Result.success();
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/category/{id}")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.success();
    }

    @Operation(summary = "批量拖拽排序（分类）")
    @PostMapping("/category/sort")
    public Result<Void> sortCategory(@RequestBody LearnSortReq req) {
        categoryService.sortBatch(req.getItems());
        return Result.success();
    }

    // ---------- 文章 ----------

    @Operation(summary = "文章分页")
    @GetMapping("/article/page")
    public Result<IPage<LearnArticleDetail>> articlePage(LearnArticlePageQuery q) {
        return Result.success(articleService.page(q));
    }

    @Operation(summary = "文章详情")
    @GetMapping("/article/{id}")
    public Result<LearnArticleDetail> articleDetail(@PathVariable Long id) {
        return Result.success(articleService.detail(id));
    }

    @Operation(summary = "新增文章")
    @PostMapping("/article")
    public Result<Long> createArticle(@Valid @RequestBody LearnArticleReq req) {
        return Result.success(articleService.create(req));
    }

    @Operation(summary = "更新文章")
    @PutMapping("/article/{id}")
    public Result<Void> updateArticle(@PathVariable Long id, @Valid @RequestBody LearnArticleReq req) {
        articleService.update(id, req);
        return Result.success();
    }

    @Operation(summary = "删除文章")
    @DeleteMapping("/article/{id}")
    public Result<Void> deleteArticle(@PathVariable Long id) {
        articleService.delete(id);
        return Result.success();
    }

    @Operation(summary = "发布文章")
    @PostMapping("/article/{id}/publish")
    public Result<Void> publishArticle(@PathVariable Long id) {
        articleService.publish(id);
        return Result.success();
    }

    @Operation(summary = "下线文章")
    @PostMapping("/article/{id}/unpublish")
    public Result<Void> unpublishArticle(@PathVariable Long id) {
        articleService.unpublish(id);
        return Result.success();
    }

    @Operation(summary = "移动文章分类")
    @PostMapping("/article/{id}/move")
    public Result<Void> moveArticle(@PathVariable Long id, @RequestBody LearnArticleReq req) {
        articleService.move(id, req.getCategoryId());
        return Result.success();
    }

    @Operation(summary = "批量拖拽排序（文章）")
    @PostMapping("/article/sort")
    public Result<Void> sortArticle(@RequestBody LearnSortReq req) {
        articleService.sortBatch(req.getItems());
        return Result.success();
    }
}
