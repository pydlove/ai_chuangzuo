package com.aichuangzuo.user.modules.exporttemplate.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.modules.exporttemplate.service.ExportTemplateService;
import com.aichuangzuo.user.modules.exporttemplate.vo.ExportTemplateVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户端导出模板查询接口。
 *
 * <p>创作页弹框和预览页共用，返回启用中的模板列表。
 */
@Tag(name = "用户端导出模板")
@RestController
@RequestMapping("/api/v1/user/export-templates")
@RequiredArgsConstructor
public class ExportTemplateController {

    private final ExportTemplateService exportTemplateService;

    @Operation(summary = "查询启用的导出模板列表")
    @GetMapping
    public Result<List<ExportTemplateVO>> list() {
        return Result.success(exportTemplateService.listEnabled());
    }
}
