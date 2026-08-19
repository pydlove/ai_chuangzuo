package com.aichuangzuo.user.modules.platform.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.platform.service.PlatformService;
import com.aichuangzuo.user.modules.platform.vo.PlatformVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户端自媒体平台配置查询接口。
 *
 * <p>供制定自媒体方案第一步读取启用中的平台列表。</p>
 */
@Tag(name = "用户端平台配置")
@RestController
@RequestMapping("/api/v1/user/platforms")
@Slf4j
@RequiredArgsConstructor
public class PlatformController {

    private final PlatformService platformService;

    @Operation(summary = "查询启用的自媒体平台列表")
    @GetMapping
    public Result<List<PlatformVO>> list() {
        Long userId = SecurityUserContext.getCurrentUserId();
        log.info("查询启用的自媒体平台列表, userId={}", userId);
        return Result.success(platformService.listEnabled());
    }
}
