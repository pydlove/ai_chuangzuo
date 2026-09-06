package com.aichuangzuo.admin.modules.selfmediaplan.controller;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.selfmediaplan.dto.request.SelfMediaPlanPageRequest;
import com.aichuangzuo.admin.modules.selfmediaplan.service.SelfMediaPlanAdminService;
import com.aichuangzuo.admin.modules.selfmediaplan.vo.SelfMediaPlanVO;
import com.aichuangzuo.shared.result.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户运营方案管理")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/self-media-plans")
@RequiredArgsConstructor
public class SelfMediaPlanAdminController {

    private final SelfMediaPlanAdminService service;

    @Operation(summary = "运营方案列表（分页）")
    @GetMapping
    public Result<IPage<SelfMediaPlanVO>> page(SelfMediaPlanPageRequest request) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员查询用户运营方案列表, adminUserId={}, keyword={}, page={}, pageSize={}",
                adminUserId, request.getKeyword(), request.getPage(), request.getPageSize());
        return Result.success(service.page(request));
    }
}
