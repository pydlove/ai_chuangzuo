package com.aichuangzuo.user.modules.skill.market.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
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
    private final UserMarketFavoriteService userMarketFavoriteService;

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

    /**
     * 获取当前用户收藏的市场 skill id 列表。
     */
    @Operation(summary = "获取收藏的市场 skill id 列表")
    @GetMapping("/favorites")
    public Result<List<String>> listFavoriteIds() {
        Long userId = SecurityUserContext.getCurrentUserId();
        return Result.success(userMarketFavoriteService.listFavoriteIds(userId));
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
}
