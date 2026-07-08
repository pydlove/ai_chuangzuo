package com.aichuangzuo.user.modules.article.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.article.dto.request.SaveDraftRequest;
import com.aichuangzuo.user.modules.article.dto.request.UpdateDraftRequest;
import com.aichuangzuo.user.modules.article.service.DraftService;
import com.aichuangzuo.user.modules.article.vo.DraftPageVO;
import com.aichuangzuo.user.modules.article.vo.DraftVO;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户草稿 REST 接口。
 *
 * <p>路径前缀：/api/v1/user/drafts，鉴权由 SecurityConfig 统一拦截。
 */
@Tag(name = "用户草稿")
@RestController
@RequestMapping("/api/v1/user/drafts")
@RequiredArgsConstructor
public class DraftController {

    private final DraftService draftService;

    /**
     * 分页查询当前用户的草稿列表。
     *
     * @param keyword  关键字（自定义标题/需求模糊）
     * @param page     页码（从 1 开始）
     * @param pageSize 每页条数
     */
    @Operation(summary = "我的草稿列表")
    @GetMapping
    public Result<DraftPageVO> list(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "1") long page,
            @RequestParam(name = "pageSize", defaultValue = "20") long pageSize) {
        Long userId = SecurityUserContext.getCurrentUserId();
        return Result.success(draftService.list(userId, keyword, page, pageSize));
    }

    /**
     * 查询单条草稿详情。
     */
    @Operation(summary = "草稿详情")
    @GetMapping("/{bizNo}")
    public Result<DraftVO> get(@PathVariable("bizNo") String bizNo) {
        Long userId = SecurityUserContext.getCurrentUserId();
        return Result.success(draftService.get(userId, bizNo));
    }

    /**
     * 保存草稿。
     *
     * @return 新草稿的 bizNo
     */
    @Operation(summary = "保存草稿")
    @PostMapping
    public Result<String> save(@Valid @RequestBody SaveDraftRequest request) {
        Long userId = SecurityUserContext.getCurrentUserId();
        return Result.success(draftService.save(userId, request));
    }

    /**
     * 修改草稿。
     */
    @Operation(summary = "修改草稿")
    @PutMapping("/{bizNo}")
    public Result<Void> update(@PathVariable("bizNo") String bizNo,
                                @Valid @RequestBody UpdateDraftRequest request) {
        Long userId = SecurityUserContext.getCurrentUserId();
        draftService.update(userId, bizNo, request);
        return Result.success();
    }

    /**
     * 删除草稿。
     */
    @Operation(summary = "删除草稿")
    @DeleteMapping("/{bizNo}")
    public Result<Void> delete(@PathVariable("bizNo") String bizNo) {
        Long userId = SecurityUserContext.getCurrentUserId();
        draftService.delete(userId, bizNo);
        return Result.success();
    }
}