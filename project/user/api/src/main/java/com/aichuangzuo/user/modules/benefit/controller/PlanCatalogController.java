package com.aichuangzuo.user.modules.benefit.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.modules.benefit.service.PlanCatalogService;
import com.aichuangzuo.user.modules.benefit.vo.PlanCatalogVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公开接口：定价页套餐目录（无需登录）。
 */
@Tag(name = "用户端-套餐目录")
@RestController
@RequestMapping("/api/v1/user/plans")
@RequiredArgsConstructor
public class PlanCatalogController {

    private final PlanCatalogService planCatalogService;

    @GetMapping
    public Result<PlanCatalogVO> getCatalog() {
        return Result.success(planCatalogService.getCatalog());
    }
}