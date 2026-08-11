package com.aichuangzuo.user.modules.learn.controller;

import com.aichuangzuo.shared.exception.NotFoundException;
import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.modules.learn.service.LearnBrowseService;
import com.aichuangzuo.user.modules.learn.vo.LearnArticleVO;
import com.aichuangzuo.user.modules.learn.vo.LearnBannerVO;
import com.aichuangzuo.user.modules.learn.vo.LearnCategoryDetailVO;
import com.aichuangzuo.user.modules.learn.vo.LearnCategoryTreeVO;
import com.aichuangzuo.user.modules.learn.vo.LearnRecommendedArticleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

@Slf4j
@Tag(name = "创作学院公共浏览")
@RestController
@RequestMapping("/api/v1/user/learn")
@RequiredArgsConstructor
public class LearnController {

    private final LearnBrowseService service;

    @Operation(summary = "分类树")
    @GetMapping("/category/tree")
    public Result<List<LearnCategoryTreeVO>> tree() {
        log.info("查询创作学院分类树");
        return Result.success(service.tree());
    }

    @Operation(summary = "分类详情 + 已发布文章列表")
    @GetMapping("/category/{id}")
    public Result<LearnCategoryDetailVO> categoryDetail(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("查询创作学院分类详情 categoryId={} page={} size={}", id, page, size);
        LearnCategoryDetailVO vo = service.categoryDetail(id, page, size);
        if (vo == null) {
            throw new NotFoundException("分类不存在");
        }
        return Result.success(vo);
    }

    @Operation(summary = "文章详情")
    @GetMapping("/article/{id}")
    public Result<LearnArticleVO> articleDetail(@PathVariable Long id) {
        log.info("查询创作学院文章详情 articleId={}", id);
        LearnArticleVO vo = service.articleDetail(id);
        if (vo == null) {
            throw new NotFoundException("文章不存在或已下线");
        }
        return Result.success(vo);
    }

    @Operation(summary = "Banner 列表")
    @GetMapping("/banner")
    public Result<List<LearnBannerVO>> banners() {
        log.info("查询创作学院 Banner 列表");
        return Result.success(service.banners());
    }

    @Operation(summary = "推荐文章列表")
    @GetMapping("/article/recommended")
    public Result<List<LearnRecommendedArticleVO>> recommendedArticles() {
        log.info("查询创作学院推荐文章列表");
        return Result.success(service.recommendedArticles());
    }

    @Operation(summary = "全部文章列表")
    @GetMapping("/article/all")
    public Result<Page<LearnRecommendedArticleVO>> allArticles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("查询创作学院全部文章列表 page={} size={}", page, size);
        return Result.success(service.allArticles(page, size));
    }
}
