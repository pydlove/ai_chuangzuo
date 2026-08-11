package com.aichuangzuo.user.modules.homebanner.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.homebanner.service.HomeBannerService;
import com.aichuangzuo.user.modules.homebanner.vo.HomeBannerVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "首页 Banner")
@RestController
@RequestMapping("/api/v1/user/home/banners")
@RequiredArgsConstructor
@Slf4j
public class HomeBannerController {

    private final HomeBannerService service;

    @Operation(summary = "首页 Banner 列表")
    @GetMapping
    public Result<List<HomeBannerVO>> list() {
        log.info("首页Banner列表, userId={}", SecurityUserContext.getCurrentUserId());
        return Result.success(service.list());
    }
}
