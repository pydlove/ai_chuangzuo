package com.aichuangzuo.user.modules.skill.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.skill.dto.request.AnalyzeSkillRequest;
import com.aichuangzuo.user.modules.skill.dto.request.CreateSkillRequest;
import com.aichuangzuo.user.modules.skill.dto.request.UpdateSkillRequest;
import com.aichuangzuo.user.modules.skill.service.SkillAnalyzeService;
import com.aichuangzuo.user.modules.skill.service.SystemSkillService;
import com.aichuangzuo.user.modules.skill.service.UserSkillService;
import com.aichuangzuo.user.modules.skill.vo.SkillAnalyzeVO;
import com.aichuangzuo.user.modules.skill.vo.SystemSkillVO;
import com.aichuangzuo.user.modules.skill.vo.UserSkillVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户风格 REST 接口。
 *
 * <p>路径前缀：/api/v1/user/skills，鉴权由 SecurityConfig 统一拦截。
 */
@Slf4j
@Tag(name = "用户风格")
@RestController
@RequestMapping("/api/v1/user/skills")
@RequiredArgsConstructor
public class UserSkillController {

    private final UserSkillService userSkillService;
    private final SystemSkillService systemSkillService;
    private final SkillAnalyzeService skillAnalyzeService;

    /**
     * 分页获取当前登录用户的风格列表。
     *
     * @param sourceType 来源类型：1-自定义（默认），2-学习
     * @param keyword    关键词，匹配名称、适用范围、提示词或描述
     * @param page       页码，默认 1
     * @param pageSize   每页条数，默认 12
     * @return 分页风格列表
     */
    @Operation(summary = "分页获取我的风格列表")
    @GetMapping
    public Result<IPage<UserSkillVO>> listMySkills(
            @RequestParam(name = "sourceType", required = false, defaultValue = "1") Integer sourceType,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", required = false, defaultValue = "1") int page,
            @RequestParam(name = "pageSize", required = false, defaultValue = "12") int pageSize) {
        Long userId = SecurityUserContext.getCurrentUserId();
        log.info("分页获取我的风格列表 userId={} sourceType={} keyword={} page={} pageSize={}", userId, sourceType, keyword, page, pageSize);
        return Result.success(userSkillService.listMySkills(sourceType, keyword, page, pageSize));
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
        Long userId = SecurityUserContext.getCurrentUserId();
        log.info("创建风格 userId={} skillName={} sourceType={}", userId, request.getSkillName(), request.getSourceType());
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
        Long userId = SecurityUserContext.getCurrentUserId();
        log.info("修改风格 userId={} bizNo={} skillName={}", userId, bizNo, request.getSkillName());
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
        Long userId = SecurityUserContext.getCurrentUserId();
        log.info("删除风格 userId={} bizNo={}", userId, bizNo);
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
        Long userId = SecurityUserContext.getCurrentUserId();
        log.info("发布风格到市场 userId={} bizNo={}", userId, bizNo);
        userSkillService.publishSkill(bizNo);
        return Result.success();
    }

    /**
     * 分页获取当前启用的系统预设风格。
     */
    @Operation(summary = "获取系统预设风格")
    @GetMapping("/system-skills")
    public Result<IPage<SystemSkillVO>> listSystemSkills(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", required = false, defaultValue = "1") int page,
            @RequestParam(name = "pageSize", required = false, defaultValue = "12") int pageSize) {
        Long userId = SecurityUserContext.getCurrentUserId();
        log.info("获取系统预设风格 userId={} keyword={} page={} pageSize={}", userId, keyword, page, pageSize);
        return Result.success(systemSkillService.listEnabled(keyword, page, pageSize));
    }

    /**
     * AI 分析参考文章写作风格，返回风格提示词与 2 段原文摘录。
     *
     * <p>本接口会校验每日分析次数上限，但不会直接消费月度学习额度；额度在用户保存学习结果时扣除。
     *
     * @param request 含参考文章正文（200-1000 字）
     * @return 分析结果（excerpt 仅供展示，不入库）
     */
    @Operation(summary = "AI 分析参考文章写作风格并提取风格")
    @PostMapping("/analyze")
    public Result<SkillAnalyzeVO> analyzeSkill(@Valid @RequestBody AnalyzeSkillRequest request) {
        Long userId = SecurityUserContext.getCurrentUserId();
        log.info("AI 分析参考文章写作风格 userId={} textLength={}", userId, request.getText() == null ? 0 : request.getText().length());
        return Result.success(skillAnalyzeService.analyze(userId, request.getText()));
    }
}
