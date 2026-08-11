package com.aichuangzuo.admin.modules.generation.controller;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.generation.dto.request.CloneTemplateRequest;
import com.aichuangzuo.admin.modules.generation.dto.request.PromptTemplateListRequest;
import com.aichuangzuo.admin.modules.generation.dto.request.PromptTemplateSaveRequest;
import com.aichuangzuo.admin.modules.generation.dto.request.PublishTemplateRequest;
import com.aichuangzuo.admin.modules.generation.service.PromptTemplateService;
import com.aichuangzuo.admin.modules.generation.vo.PromptTemplateAdminPageVO;
import com.aichuangzuo.admin.modules.generation.vo.PromptTemplateAdminVO;
import com.aichuangzuo.admin.modules.generation.vo.PromptTemplateVersionVO;
import com.aichuangzuo.shared.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端-创作提示词管理 API。
 */
@Tag(name = "管理端-创作提示词")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/prompt-templates")
@RequiredArgsConstructor
public class PromptTemplateAdminController {

    private final PromptTemplateService service;

    @GetMapping
    public Result<PromptTemplateAdminPageVO> list(PromptTemplateListRequest request) {
        log.info("管理端查询创作提示词模板列表, keyword={}, page={}, pageSize={}",
                request.getKeyword(), request.getPage(), request.getPageSize());
        return Result.success(service.list(request));
    }

    @GetMapping("/{id}")
    public Result<PromptTemplateAdminVO> detail(@PathVariable Long id) {
        log.info("管理端查询创作提示词模板详情, templateId={}", id);
        return Result.success(service.detail(id));
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody PromptTemplateSaveRequest request) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理端创建创作提示词模板, adminUserId={}, name={}", adminUserId, request.getName());
        return Result.success(service.create(request, adminUserId));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody PromptTemplateSaveRequest request) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理端更新创作提示词模板, adminUserId={}, templateId={}", adminUserId, id);
        service.update(id, request, adminUserId);
        return Result.success();
    }

    @PostMapping("/{id}/init-stages")
    public Result<Integer> initStages(@PathVariable Long id) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理端初始化创作提示词模板阶段, adminUserId={}, templateId={}", adminUserId, id);
        int inserted = service.initStages(id, adminUserId);
        return Result.success(inserted);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理端删除创作提示词模板, adminUserId={}, templateId={}", adminUserId, id);
        service.delete(id, adminUserId);
        return Result.success();
    }

    // ===== 阶段 2：发布 / 下线 / 克隆 / 版本列表 =====

    @PostMapping("/{id}/actions/publish")
    public Result<Long> publish(@PathVariable Long id,
                                @RequestBody(required = false) PublishTemplateRequest request) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        String changeNote = request == null ? null : request.getChangeNote();
        log.info("管理端发布创作提示词模板, adminUserId={}, templateId={}", adminUserId, id);
        return Result.success(service.publish(id, changeNote, adminUserId));
    }

    @PostMapping("/{id}/actions/offline")
    public Result<Void> offline(@PathVariable Long id) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理端下线创作提示词模板, adminUserId={}, templateId={}", adminUserId, id);
        service.offline(id, adminUserId);
        return Result.success();
    }

    @PostMapping("/{id}/actions/clone")
    public Result<Long> clone(@PathVariable Long id,
                              @Valid @RequestBody CloneTemplateRequest request) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理端克隆创作提示词模板, adminUserId={}, sourceTemplateId={}, newName={}",
                adminUserId, id, request.getName());
        return Result.success(service.clone(id, request, adminUserId));
    }

    @GetMapping("/{id}/versions")
    public Result<List<PromptTemplateVersionVO>> versions(@PathVariable Long id) {
        log.info("管理端查询创作提示词模板版本列表, templateId={}", id);
        return Result.success(service.listVersions(id));
    }
}
