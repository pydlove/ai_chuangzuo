package com.aichuangzuo.user.modules.generation.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.modules.generation.service.PromptTemplateQueryService;
import com.aichuangzuo.user.modules.generation.vo.PromptTemplatePublicVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * User 端-创作模板只读 API。
 *
 * <p>用于创作页模板选择器：列出所有已发布模板，按 ID 查单个。
 *
 * <p>设计文档：§5.15.4
 */
@Tag(name = "用户端-创作模板")
@RestController
@RequestMapping("/api/v1/user/prompt-templates")
@RequiredArgsConstructor
public class PromptTemplateQueryController {

    private final PromptTemplateQueryService service;

    @GetMapping
    public Result<List<PromptTemplatePublicVO>> list() {
        return Result.success(service.listPublished());
    }

    @GetMapping("/{id}")
    public Result<PromptTemplatePublicVO> detail(@PathVariable Long id) {
        return Result.success(service.detail(id));
    }
}