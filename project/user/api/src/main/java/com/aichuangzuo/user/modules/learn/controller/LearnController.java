package com.aichuangzuo.user.modules.learn.controller;

import com.aichuangzuo.shared.exception.NotFoundException;
import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.modules.learn.service.LearnBrowseService;
import com.aichuangzuo.user.modules.learn.vo.LearnArticleVO;
import com.aichuangzuo.user.modules.learn.vo.LearnCategoryDetailVO;
import com.aichuangzuo.user.modules.learn.vo.LearnCategoryTreeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "创作学院公共浏览")
@RestController
@RequestMapping("/api/v1/user/learn")
@RequiredArgsConstructor
public class LearnController {

    private final LearnBrowseService service;

    @Operation(summary = "分类树")
    @GetMapping("/category/tree")
    public Result<List<LearnCategoryTreeVO>> tree() {
        return Result.success(service.tree());
    }

    @Operation(summary = "分类详情 + 已发布文章列表")
    @GetMapping("/category/{id}")
    public Result<LearnCategoryDetailVO> categoryDetail(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        LearnCategoryDetailVO vo = service.categoryDetail(id, page, size);
        if (vo == null) {
            throw new NotFoundException("分类不存在");
        }
        return Result.success(vo);
    }

    @Operation(summary = "文章详情")
    @GetMapping("/article/{id}")
    public Result<LearnArticleVO> articleDetail(@PathVariable Long id) {
        LearnArticleVO vo = service.articleDetail(id);
        if (vo == null) {
            throw new NotFoundException("文章不存在或已下线");
        }
        return Result.success(vo);
    }
}
