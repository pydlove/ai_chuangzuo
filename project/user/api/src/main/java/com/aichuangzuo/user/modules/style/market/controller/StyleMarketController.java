package com.aichuangzuo.user.modules.style.market.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.modules.style.market.service.StyleMarketQueryService;
import com.aichuangzuo.user.modules.style.market.vo.MarketStyleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户端 - 风格市场接口。
 */
@Tag(name = "风格市场")
@RestController
@RequestMapping("/api/v1/user/market-styles")
@RequiredArgsConstructor
public class StyleMarketController {

    private final StyleMarketQueryService styleMarketQueryService;

    @Operation(summary = "获取已上架的风格市场列表")
    @GetMapping
    public Result<List<MarketStyleVO>> listEnabled() {
        return Result.success(styleMarketQueryService.listEnabled());
    }
}
