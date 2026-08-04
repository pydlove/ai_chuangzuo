package com.aichuangzuo.user.modules.skill.market.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.skill.market.config.service.SkillMonthlyRewardConfigService;
import com.aichuangzuo.user.modules.skill.market.config.vo.SkillMonthlyRewardConfigVO;
import com.aichuangzuo.user.modules.skill.market.service.SkillMarketCommandService;
import com.aichuangzuo.user.modules.skill.market.service.SkillMarketQueryService;
import com.aichuangzuo.user.modules.skill.market.service.UserMarketFavoriteService;
import com.aichuangzuo.user.modules.skill.market.vo.MarketSkillOverviewVO;
import com.aichuangzuo.user.modules.skill.market.vo.MarketSkillVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * 用户端 - 风格市场接口。
 */
@Tag(name = "风格市场")
@RestController
@RequestMapping("/api/v1/user/market-skills")
@RequiredArgsConstructor
public class SkillMarketController {

    private final SkillMarketQueryService skillMarketQueryService;
    private final SkillMarketCommandService skillMarketCommandService;
    private final UserMarketFavoriteService userMarketFavoriteService;
    private final SkillMonthlyRewardConfigService monthlyRewardConfigService;

    @Operation(summary = "获取全部已上架的风格市场列表")
    @GetMapping
    public Result<List<MarketSkillVO>> listEnabled() {
        return Result.success(skillMarketQueryService.listEnabled());
    }

    @Operation(summary = "分页查询已上架的风格市场列表")
    @GetMapping("/paged")
    public Result<IPage<MarketSkillVO>> listEnabledPaged(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "all") String sortType) {
        return Result.success(skillMarketQueryService.pageEnabled(page, pageSize, keyword, sortType));
    }

    @Operation(summary = "获取提示词市场概览")
    @GetMapping("/overview")
    public Result<MarketSkillOverviewVO> overview() {
        return Result.success(skillMarketQueryService.getOverview());
    }

    @Operation(summary = "获取提示词市场月度排行榜奖励配置")
    @GetMapping("/monthly-reward-config")
    public Result<SkillMonthlyRewardConfigVO> monthlyRewardConfig() {
        return Result.success(monthlyRewardConfigService.getEnabledConfig());
    }

    @Operation(summary = "获取提示词市场单次使用收益单价")
    @GetMapping("/price-per-use")
    public Result<BigDecimal> pricePerUse() {
        return Result.success(monthlyRewardConfigService.getPricePerUse());
    }

    /**
     * 获取当前用户收藏的市场 skill 详情列表（含已下架）。
     */
    @Operation(summary = "获取收藏的市场 skill 详情列表")
    @GetMapping("/favorites")
    public Result<List<MarketSkillVO>> listFavoriteSkills() {
        Long userId = SecurityUserContext.getCurrentUserId();
        return Result.success(userMarketFavoriteService.listFavoriteSkills(userId));
    }

    /**
     * 收藏市场 skill。
     */
    @Operation(summary = "收藏市场 skill")
    @PostMapping("/favorites/{marketSkillId}")
    public Result<Void> addFavorite(@PathVariable String marketSkillId) {
        Long userId = SecurityUserContext.getCurrentUserId();
        userMarketFavoriteService.addFavorite(userId, marketSkillId);
        return Result.success();
    }

    /**
     * 取消收藏市场 skill。
     */
    @Operation(summary = "取消收藏市场 skill")
    @DeleteMapping("/favorites/{marketSkillId}")
    public Result<Void> removeFavorite(@PathVariable String marketSkillId) {
        Long userId = SecurityUserContext.getCurrentUserId();
        userMarketFavoriteService.removeFavorite(userId, marketSkillId);
        return Result.success();
    }

    /**
     * 发布者删除（下架）自己的市场 skill。
     */
    @Operation(summary = "下架自己的市场 skill")
    @DeleteMapping("/{marketSkillId}")
    public Result<Void> deleteOwnMarketSkill(@PathVariable String marketSkillId) {
        Long userId = SecurityUserContext.getCurrentUserId();
        skillMarketCommandService.deleteOwnMarketSkill(marketSkillId, userId);
        return Result.success();
    }

    /**
     * 获取当前用户的全部市场提交记录（含待审核/已通过/已打回）。
     */
    @Operation(summary = "我的市场提交记录")
    @GetMapping("/my-submissions")
    public Result<List<MarketSkillVO>> listMySubmissions() {
        Long userId = SecurityUserContext.getCurrentUserId();
        return Result.success(skillMarketQueryService.listMySubmissions(userId));
    }
}
