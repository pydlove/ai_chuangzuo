package com.aichuangzuo.admin.modules.message.controller;

import com.aichuangzuo.admin.modules.message.dto.request.MessageCreateRequest;
import com.aichuangzuo.admin.modules.message.dto.request.MessageQueryRequest;
import com.aichuangzuo.admin.modules.message.dto.request.MessageUpdateRequest;
import com.aichuangzuo.admin.modules.message.service.MessageAdminService;
import com.aichuangzuo.admin.modules.message.vo.MessageAdminPageVO;
import com.aichuangzuo.admin.modules.message.vo.MessageAdminVO;
import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.shared.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理端-消息管理")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/messages")
@RequiredArgsConstructor
public class MessageAdminController {

    private final MessageAdminService messageAdminService;

    @Operation(summary = "消息列表（按 msgType + keyword 分页）")
    @GetMapping
    public Result<MessageAdminPageVO> list(@Valid MessageQueryRequest request) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员查询消息列表, adminUserId={}, msgType={}, keyword={}, page={}, size={}",
                adminUserId, request.getMsgType(), request.getKeyword(), request.getPage(), request.getSize());
        return Result.success(messageAdminService.list(request));
    }

    @Operation(summary = "消息详情")
    @GetMapping("/{id}")
    public Result<MessageAdminVO> detail(@PathVariable("id") Long id) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员查询消息详情, adminUserId={}, messageId={}", adminUserId, id);
        return Result.success(messageAdminService.detail(id));
    }

    @Operation(summary = "新建消息")
    @PostMapping
    public Result<MessageAdminVO> create(@Valid @RequestBody MessageCreateRequest request) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员新建消息, adminUserId={}, msgType={}, title={}, scope={}",
                adminUserId, request.getMsgType(), request.getTitle(), request.getScope());
        return Result.success(messageAdminService.create(request));
    }

    @Operation(summary = "编辑消息（仅 title / summary / linkUrl）")
    @PutMapping("/{id}")
    public Result<MessageAdminVO> update(@PathVariable("id") Long id,
                                         @Valid @RequestBody MessageUpdateRequest request) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员编辑消息, adminUserId={}, messageId={}, title={}",
                adminUserId, id, request.getTitle());
        return Result.success(messageAdminService.update(id, request));
    }
}
