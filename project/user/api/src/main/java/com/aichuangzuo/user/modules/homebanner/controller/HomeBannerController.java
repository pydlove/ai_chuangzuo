package com.aichuangzuo.user.modules.homebanner.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.modules.homebanner.service.HomeBannerService;
import com.aichuangzuo.user.modules.homebanner.vo.HomeBannerVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "首页 Banner")
@RestController
@RequestMapping("/api/v1/user/home/banners")
@RequiredArgsConstructor
public class HomeBannerController {

    private final HomeBannerService service;

    @Operation(summary = "首页 Banner 列表")
    @GetMapping
    public Result<List<HomeBannerVO>> list() {
        return Result.success(service.list());
    }
}
