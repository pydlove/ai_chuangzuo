package com.aichuangzuo.admin.modules.skill.preset.controller;

import com.aichuangzuo.admin.modules.skill.preset.dto.request.CreateGlobalSkillRequest;
import com.aichuangzuo.admin.modules.skill.preset.dto.request.GlobalSkillPageRequest;
import com.aichuangzuo.admin.modules.skill.preset.dto.request.UpdateGlobalSkillRequest;
import com.aichuangzuo.admin.modules.skill.preset.service.GlobalSkillService;
import com.aichuangzuo.admin.modules.skill.preset.vo.GlobalSkillVO;
import com.aichuangzuo.shared.result.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端 - 预设风格 CRUD 接口。
 */
@Tag(name = "管理端 - 预设风格")
@RestController
@RequestMapping("/api/v1/admin/global-skills")
@RequiredArgsConstructor
public class GlobalSkillAdminController {

    private final GlobalSkillService globalSkillService;

    @Operation(summary = "预设风格列表（分页）")
    @GetMapping
    public Result<IPage<GlobalSkillVO>> page(GlobalSkillPageRequest request) {
        return Result.success(globalSkillService.page(request));
    }

    @Operation(summary = "创建预设风格")
    @PostMapping
    public Result<String> create(@Valid @RequestBody CreateGlobalSkillRequest request) {
        return Result.success(globalSkillService.create(request));
    }

    @Operation(summary = "更新预设风格")
    @PutMapping("/{bizNo}")
    public Result<Void> update(@PathVariable String bizNo,
                                @Valid @RequestBody UpdateGlobalSkillRequest request) {
        globalSkillService.update(bizNo, request);
        return Result.success();
    }

    @Operation(summary = "软删除预设风格")
    @DeleteMapping("/{bizNo}")
    public Result<Void> delete(@PathVariable String bizNo) {
        globalSkillService.delete(bizNo);
        return Result.success();
    }
}