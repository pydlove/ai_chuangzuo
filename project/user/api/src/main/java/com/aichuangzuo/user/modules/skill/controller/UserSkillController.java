package com.aichuangzuo.user.modules.skill.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.benefit.vo.BenefitCheckVO;
import com.aichuangzuo.user.modules.skill.dto.request.AnalyzeSkillRequest;
import com.aichuangzuo.user.modules.skill.dto.request.CreateSkillRequest;
import com.aichuangzuo.user.modules.skill.dto.request.UpdateSkillRequest;
import com.aichuangzuo.user.modules.skill.service.SkillAnalyzeService;
import com.aichuangzuo.user.modules.skill.service.SystemSkillService;
import com.aichuangzuo.user.modules.skill.service.UserSkillService;
import com.aichuangzuo.user.modules.skill.vo.SkillAnalyzeVO;
import com.aichuangzuo.user.modules.skill.vo.SystemSkillVO;
import com.aichuangzuo.user.modules.skill.vo.UserSkillVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户风格 REST 接口。
 *
 * <p>路径前缀：/api/v1/user/skills，鉴权由 SecurityConfig 统一拦截。
 */
@Tag(name = "用户风格")
@RestController
@RequestMapping("/api/v1/user/skills")
@RequiredArgsConstructor
public class UserSkillController {

    private final UserSkillService userSkillService;
    private final SystemSkillService systemSkillService;
    private final SkillAnalyzeService skillAnalyzeService;

    /**
     * 获取当前登录用户的风格列表。
     *
     * @param sourceType 来源类型：1-自定义（默认），2-学习
     * @return 风格列表
     */
    @Operation(summary = "获取我的风格列表")
    @GetMapping
    public Result<List<UserSkillVO>> listMySkills(
            @RequestParam(name = "sourceType", required = false, defaultValue = "1") Integer sourceType) {
        return Result.success(userSkillService.listMySkills(sourceType));
    }

    /**
     * 创建自定义风格。
     *
     * @param request 创建请求
     * @return 创建后的风格
     */
    @Operation(summary = "创建风格")
    @PostMapping
    public Result<UserSkillVO> createSkill(@Valid @RequestBody CreateSkillRequest request) {
        return Result.success(userSkillService.createSkill(request));
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
    public Result<UserSkillVO> updateSkill(
            @PathVariable String bizNo,
            @Valid @RequestBody UpdateSkillRequest request) {
        return Result.success(userSkillService.updateSkill(bizNo, request));
    }

    /**
     * 删除当前用户的风格。
     *
     * @param bizNo 风格业务编号
     * @return 成功响应
     */
    @Operation(summary = "删除风格")
    @DeleteMapping("/{bizNo}")
    public Result<Void> deleteSkill(@PathVariable String bizNo) {
        userSkillService.deleteSkill(bizNo);
        return Result.success();
    }

    /**
     * 将当前用户的风格发布到提示词市场（进入待审核状态）。
     *
     * @param bizNo 风格业务编号
     */
    @Operation(summary = "发布风格到市场")
    @PostMapping("/{bizNo}/publish")
    public Result<Void> publishSkill(@PathVariable String bizNo) {
        userSkillService.publishSkill(bizNo);
        return Result.success();
    }

    /**
     * 获取当前启用的系统预设风格。
     */
    @Operation(summary = "获取系统预设风格")
    @GetMapping("/system-skills")
    public Result<List<SystemSkillVO>> listSystemSkills(
            @RequestParam(name = "keyword", required = false) String keyword) {
        return Result.success(systemSkillService.listEnabled(keyword));
    }

    /**
     * AI 分析参考文章写作风格，返回风格提示词与 2 段原文摘录。
     *
     * <p>本接口不直接消费额度，请先在调用前执行 /analyze/pre-consume 预扣额度。
     *
     * @param request 含参考文章正文（200-1000 字）
     * @return 分析结果（excerpt 仅供展示，不入库）
     */
    @Operation(summary = "AI 分析参考文章写作风格并提取风格")
    @PostMapping("/analyze")
    public Result<SkillAnalyzeVO> analyzeSkill(@Valid @RequestBody AnalyzeSkillRequest request) {
        Long userId = SecurityUserContext.getCurrentUserId();
        return Result.success(skillAnalyzeService.analyze(userId, request.getText()));
    }

    /**
     * 预扣 skill_learn_analyze 月度额度。
     */
    @Operation(summary = "预扣学习提示词额度")
    @PostMapping("/analyze/pre-consume")
    public Result<BenefitCheckVO> preConsumeAnalyze(@RequestBody AnalyzeSkillRequest request) {
        Long userId = SecurityUserContext.getCurrentUserId();
        return Result.success(skillAnalyzeService.preConsume(userId));
    }

    /**
     * 确认扣减：用户保存学习结果时调用，将预扣额度转为正式用量。
     */
    @Operation(summary = "确认扣减学习提示词额度")
    @PostMapping("/analyze/confirm")
    public Result<Void> confirmAnalyzeConsume(@RequestBody AnalyzeSkillRequest request) {
        Long userId = SecurityUserContext.getCurrentUserId();
        skillAnalyzeService.confirmConsume(userId);
        return Result.success();
    }

    /**
     * 释放预扣：用户取消或关闭弹框时调用，退回预扣额度。
     */
    @Operation(summary = "释放学习提示词预扣额度")
    @PostMapping("/analyze/cancel")
    public Result<Void> cancelAnalyzeConsume(@RequestBody AnalyzeSkillRequest request) {
        Long userId = SecurityUserContext.getCurrentUserId();
        skillAnalyzeService.cancelConsume(userId);
        return Result.success();
    }
}
