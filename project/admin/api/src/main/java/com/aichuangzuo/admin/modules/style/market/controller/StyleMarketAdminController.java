package com.aichuangzuo.admin.modules.style.market.controller;

import com.aichuangzuo.admin.modules.style.market.dto.request.CreateStyleMarketRequest;
import com.aichuangzuo.admin.modules.style.market.dto.request.StyleMarketPageRequest;
import com.aichuangzuo.admin.modules.style.market.dto.request.UpdateStyleMarketRequest;
import com.aichuangzuo.admin.modules.style.market.service.StyleMarketAdminService;
import com.aichuangzuo.admin.modules.style.market.vo.StyleMarketVO;
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
@RequestMapping("/api/v1/admin/market-styles")
@RequiredArgsConstructor
public class StyleMarketAdminController {

    private final StyleMarketAdminService styleMarketAdminService;

    @Operation(summary = "风格市场列表（分页）")
    @GetMapping
    public Result<IPage<StyleMarketVO>> page(StyleMarketPageRequest request) {
        return Result.success(styleMarketAdminService.page(request));
    }

    @Operation(summary = "创建风格市场条目")
    @PostMapping
    public Result<String> create(@Valid @RequestBody CreateStyleMarketRequest request) {
        return Result.success(styleMarketAdminService.create(request));
    }

    @Operation(summary = "更新风格市场条目")
    @PutMapping("/{bizNo}")
    public Result<Void> update(@PathVariable String bizNo,
                               @Valid @RequestBody UpdateStyleMarketRequest request) {
        styleMarketAdminService.update(bizNo, request);
        return Result.success();
    }

    @Operation(summary = "软删除风格市场条目")
    @DeleteMapping("/{bizNo}")
    public Result<Void> delete(@PathVariable String bizNo) {
        styleMarketAdminService.delete(bizNo);
        return Result.success();
    }
}
