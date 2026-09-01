package com.aichuangzuo.admin.modules.skill.market.controller;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.earnings.vo.PageResult;
import com.aichuangzuo.admin.modules.skill.market.dto.request.BatchDeleteSkillMarketRequest;
import com.aichuangzuo.admin.modules.skill.market.dto.request.CreateSkillMarketRequest;
import com.aichuangzuo.admin.modules.skill.market.dto.request.SkillMarketPageRequest;
import com.aichuangzuo.admin.modules.skill.market.dto.request.SimulateSkillUsageRequest;
import com.aichuangzuo.admin.modules.skill.market.dto.request.UpdateSkillMarketRequest;
import com.aichuangzuo.admin.modules.skill.market.service.SkillMarketAdminService;
import com.aichuangzuo.admin.modules.skill.market.vo.MarketSkillStatsVO;
import com.aichuangzuo.admin.modules.skill.market.vo.SkillMarketUsageRecordVO;
import com.aichuangzuo.admin.modules.skill.market.vo.SkillMarketVO;
import com.aichuangzuo.shared.result.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
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

/**
 * 管理端 - 风格市场 CRUD 接口。
 */
@Tag(name = "管理端 - 风格市场")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/market-skills")
@RequiredArgsConstructor
public class SkillMarketAdminController {

    private final SkillMarketAdminService skillMarketAdminService;

    @Operation(summary = "风格市场列表（分页）")
    @GetMapping
    public Result<IPage<SkillMarketVO>> page(SkillMarketPageRequest request) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员查询提示词市场列表, adminUserId={}, enableStatus={}, featured={}, keyword={}, pageNum={}, pageSize={}",
                adminUserId, request.getEnableStatus(), request.getFeatured(), request.getKeyword(),
                request.getPageNum(), request.getPageSize());
        return Result.success(skillMarketAdminService.page(request));
    }

    @Operation(summary = "创建风格市场条目")
    @PostMapping
    public Result<String> create(@Valid @RequestBody CreateSkillMarketRequest request) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员创建提示词市场条目, adminUserId={}, skillName={}, publisherUserId={}",
                adminUserId, request.getSkillName(), request.getPublisherUserId());
        return Result.success(skillMarketAdminService.create(request));
    }

    @Operation(summary = "更新风格市场条目")
    @PutMapping("/{bizNo}")
    public Result<Void> update(@PathVariable String bizNo,
                               @Valid @RequestBody UpdateSkillMarketRequest request) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员更新提示词市场条目, adminUserId={}, bizNo={}, skillName={}",
                adminUserId, bizNo, request.getSkillName());
        skillMarketAdminService.update(bizNo, request);
        return Result.success();
    }

    @Operation(summary = "软删除风格市场条目")
    @DeleteMapping("/{bizNo}")
    public Result<Void> delete(@PathVariable String bizNo) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员删除提示词市场条目, adminUserId={}, bizNo={}", adminUserId, bizNo);
        skillMarketAdminService.delete(bizNo);
        return Result.success();
    }

    @Operation(summary = "批量软删除风格市场条目")
    @PostMapping("/batch-delete")
    public Result<Void> batchDelete(@Valid @RequestBody BatchDeleteSkillMarketRequest request) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员批量删除提示词市场条目, adminUserId={}, count={}",
                adminUserId, request.getBizNos() == null ? 0 : request.getBizNos().size());
        skillMarketAdminService.deleteBatch(request.getBizNos());
        return Result.success();
    }

    @Operation(summary = "提示词市场统计概览")
    @GetMapping("/stats")
    public Result<MarketSkillStatsVO> stats() {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员查询提示词市场统计概览, adminUserId={}", adminUserId);
        return Result.success(skillMarketAdminService.stats());
    }

    @Operation(summary = "提示词市场使用记录")
    @GetMapping("/{bizNo}/usage-records")
    public Result<PageResult<SkillMarketUsageRecordVO>> usageRecords(
            @PathVariable String bizNo,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员查询提示词使用记录, adminUserId={}, bizNo={}, pageNum={}, pageSize={}",
                adminUserId, bizNo, pageNum, pageSize);
        return Result.success(skillMarketAdminService.listUsageRecords(bizNo, pageNum, pageSize));
    }

    @Operation(summary = "模拟使用一次提示词")
    @PostMapping("/{bizNo}/simulate-usage")
    public Result<Void> simulateUsage(@PathVariable String bizNo,
                                      @Valid @RequestBody SimulateSkillUsageRequest request) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员模拟使用提示词, adminUserId={}, bizNo={}, consumerUserId={}",
                adminUserId, bizNo, request.getUserId());
        skillMarketAdminService.simulateUsage(bizNo, request.getUserId());
        return Result.success();
    }
}
