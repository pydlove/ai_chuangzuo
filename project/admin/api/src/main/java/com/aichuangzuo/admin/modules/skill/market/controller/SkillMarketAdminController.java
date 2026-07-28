package com.aichuangzuo.admin.modules.skill.market.controller;

import com.aichuangzuo.admin.modules.skill.market.dto.request.CreateSkillMarketRequest;
import com.aichuangzuo.admin.modules.skill.market.dto.request.SkillMarketPageRequest;
import com.aichuangzuo.admin.modules.skill.market.dto.request.UpdateSkillMarketRequest;
import com.aichuangzuo.admin.modules.skill.market.service.SkillMarketAdminService;
import com.aichuangzuo.admin.modules.skill.market.vo.SkillMarketVO;
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
 * 管理端 - 风格市场 CRUD 接口。
 */
@Tag(name = "管理端 - 风格市场")
@RestController
@RequestMapping("/api/v1/admin/market-skills")
@RequiredArgsConstructor
public class SkillMarketAdminController {

    private final SkillMarketAdminService skillMarketAdminService;

    @Operation(summary = "风格市场列表（分页）")
    @GetMapping
    public Result<IPage<SkillMarketVO>> page(SkillMarketPageRequest request) {
        return Result.success(skillMarketAdminService.page(request));
    }

    @Operation(summary = "创建风格市场条目")
    @PostMapping
    public Result<String> create(@Valid @RequestBody CreateSkillMarketRequest request) {
        return Result.success(skillMarketAdminService.create(request));
    }

    @Operation(summary = "更新风格市场条目")
    @PutMapping("/{bizNo}")
    public Result<Void> update(@PathVariable String bizNo,
                               @Valid @RequestBody UpdateSkillMarketRequest request) {
        skillMarketAdminService.update(bizNo, request);
        return Result.success();
    }

    @Operation(summary = "软删除风格市场条目")
    @DeleteMapping("/{bizNo}")
    public Result<Void> delete(@PathVariable String bizNo) {
        skillMarketAdminService.delete(bizNo);
        return Result.success();
    }
}
