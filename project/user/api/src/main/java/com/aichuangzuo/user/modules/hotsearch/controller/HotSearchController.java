package com.aichuangzuo.user.modules.hotsearch.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.modules.hotsearch.service.HotSearchService;
import com.aichuangzuo.user.modules.hotsearch.vo.HotSearchItemVO;
import com.aichuangzuo.user.modules.hotsearch.vo.HotSearchPlatformVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 热搜榜单查询接口。
 */
@Tag(name = "热搜榜单")
@RestController
@RequestMapping("/api/v1/user/hot-search")
@RequiredArgsConstructor
public class HotSearchController {

    private final HotSearchService hotSearchService;

    @Operation(summary = "获取热搜平台列表")
    @GetMapping("/platforms")
    public Result<List<HotSearchPlatformVO>> listPlatforms() {
        return Result.success(hotSearchService.listPlatforms());
    }

    @Operation(summary = "查询某日某平台热搜榜单")
    @GetMapping
    public Result<List<HotSearchItemVO>> listByPlatformAndDate(
            @RequestParam("platform") String platformCode,
            @RequestParam(name = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.success(hotSearchService.listByPlatformAndDate(platformCode, date));
    }
}
