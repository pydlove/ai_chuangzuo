package com.aichuangzuo.user.modules.style.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.modules.style.dto.request.AnalyzeStyleRequest;
import com.aichuangzuo.user.modules.style.dto.request.CreateStyleRequest;
import com.aichuangzuo.user.modules.style.dto.request.UpdateStyleRequest;
import com.aichuangzuo.user.modules.style.service.StyleAnalyzeService;
import com.aichuangzuo.user.modules.style.service.SystemStyleService;
import com.aichuangzuo.user.modules.style.service.UserStyleService;
import com.aichuangzuo.user.modules.style.vo.StyleAnalyzeVO;
import com.aichuangzuo.user.modules.style.vo.SystemStyleVO;
import com.aichuangzuo.user.modules.style.vo.UserStyleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户风格 REST 接口。
 *
 * <p>路径前缀：/api/v1/user/styles，鉴权由 SecurityConfig 统一拦截。
 */
@Tag(name = "用户风格")
@RestController
@RequestMapping("/api/v1/user/styles")
@RequiredArgsConstructor
public class UserStyleController {

    private final UserStyleService userStyleService;
    private final SystemStyleService systemStyleService;
    private final StyleAnalyzeService styleAnalyzeService;

    /**
     * 获取当前登录用户的风格列表。
     *
     * @param sourceType 来源类型：1-自定义（默认），2-学习
     * @return 风格列表
     */
    @Operation(summary = "获取我的风格列表")
    @GetMapping
    public Result<List<UserStyleVO>> listMyStyles(
            @RequestParam(name = "sourceType", required = false, defaultValue = "1") Integer sourceType) {
        return Result.success(userStyleService.listMyStyles(sourceType));
    }

    /**
     * 创建自定义风格。
     *
     * @param request 创建请求
     * @return 创建后的风格
     */
    @Operation(summary = "创建风格")
    @PostMapping
    public Result<UserStyleVO> createStyle(@Valid @RequestBody CreateStyleRequest request) {
        return Result.success(userStyleService.createStyle(request));
    }

    /**
     * 修改当前用户的风格。
     *
     * @param bizNo   风格业务编号
     * @param request 修改请求
     * @return 更新后的风格
     */
    @Operation(summary = "修改风格")
    @PutMapping("/{bizNo}")
    public Result<UserStyleVO> updateStyle(
            @PathVariable String bizNo,
            @Valid @RequestBody UpdateStyleRequest request) {
        return Result.success(userStyleService.updateStyle(bizNo, request));
    }

    /**
     * 删除当前用户的风格。
     *
     * @param bizNo 风格业务编号
     * @return 成功响应
     */
    @Operation(summary = "删除风格")
    @DeleteMapping("/{bizNo}")
    public Result<Void> deleteStyle(@PathVariable String bizNo) {
        userStyleService.deleteStyle(bizNo);
        return Result.success();
    }

    /**
     * 获取当前启用的系统预设风格。
     */
    @Operation(summary = "获取系统预设风格")
    @GetMapping("/system-styles")
    public Result<List<SystemStyleVO>> listSystemStyles(
            @RequestParam(name = "keyword", required = false) String keyword) {
        return Result.success(systemStyleService.listEnabled(keyword));
    }

    /**
     * AI 分析参考文章的写作风格，返回风格提示词与 2 段原文摘录。
     *
     * @param request 含参考文章正文（200-3000 字）
     * @return 分析结果（excerpt 仅供展示，不入库）
     */
    @Operation(summary = "AI 分析参考文章风格")
    @PostMapping("/analyze")
    public Result<StyleAnalyzeVO> analyzeStyle(@Valid @RequestBody AnalyzeStyleRequest request) {
        return Result.success(styleAnalyzeService.analyze(request.getText().trim()));
    }
}
