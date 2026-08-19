package com.aichuangzuo.admin.modules.security.accesscontrol.controller;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.security.accesscontrol.dto.request.AccessControlCreateRequest;
import com.aichuangzuo.admin.modules.security.accesscontrol.dto.request.AccessControlUpdateRequest;
import com.aichuangzuo.admin.modules.security.accesscontrol.service.AccessControlService;
import com.aichuangzuo.admin.modules.security.accesscontrol.vo.AccessControlVO;
import com.aichuangzuo.shared.result.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端-访问控制规则")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/security/access-control")
@RequiredArgsConstructor
public class AccessControlAdminController {

    private final AccessControlService accessControlService;

    @GetMapping("/rules")
    public Result<IPage<AccessControlVO>> page(
            @RequestParam(required = false) Integer ruleType,
            @RequestParam(required = false) Integer listType,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员查询访问控制规则 adminUserId={} ruleType={} listType={} keyword={} page={} size={}",
                adminUserId, ruleType, listType, keyword, page, size);
        return Result.success(accessControlService.page(ruleType, listType, keyword, page, size));
    }

    @PostMapping("/rules")
    public Result<AccessControlVO> create(@Valid @RequestBody AccessControlCreateRequest request) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员创建访问控制规则 adminUserId={} ruleType={} listType={}",
                adminUserId, request.getRuleType(), request.getListType());
        return Result.success(accessControlService.create(request, adminUserId));
    }

    @PutMapping("/rules/{id}")
    public Result<AccessControlVO> update(@PathVariable Long id,
                                        @Valid @RequestBody AccessControlUpdateRequest request) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员更新访问控制规则 adminUserId={} id={}", adminUserId, id);
        return Result.success(accessControlService.update(id, request, adminUserId));
    }

    @DeleteMapping("/rules/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员删除访问控制规则 adminUserId={} id={}", adminUserId, id);
        accessControlService.delete(id, adminUserId);
        return Result.success();
    }
}
