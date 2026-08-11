package com.aichuangzuo.user.modules.share.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.modules.share.service.ShareConfigService;
import com.aichuangzuo.user.modules.share.vo.ShareConfigVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户端-分享配置")
@Slf4j
@RestController
@RequestMapping("/api/v1/user/share-config")
@RequiredArgsConstructor
public class ShareConfigController {

    private final ShareConfigService shareConfigService;

    @Operation(summary = "按场景获取分享文案")
    @GetMapping("/{sceneKey}")
    public Result<ShareConfigVO> getBySceneKey(@PathVariable("sceneKey") String sceneKey) {
        log.info("查询分享配置, sceneKey={}", sceneKey);
        return Result.success(shareConfigService.getEnabledBySceneKey(sceneKey));
    }
}
