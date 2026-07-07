package com.aichuangzuo.user.modules.message.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.message.service.MessageService;
import com.aichuangzuo.user.modules.message.vo.MessageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户端消息中心接口。
 */
@RestController
@RequestMapping("/api/v1/user/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping
    public Result<List<MessageVO>> list() {
        Long userId = SecurityUserContext.getCurrentUserId();
        return Result.success(messageService.listVisibleMessages(userId));
    }

    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable("id") Long messageId) {
        Long userId = SecurityUserContext.getCurrentUserId();
        messageService.markRead(userId, messageId);
        return Result.success();
    }

    @PutMapping("/read-all")
    public Result<Void> markAllRead() {
        Long userId = SecurityUserContext.getCurrentUserId();
        messageService.markAllRead(userId);
        return Result.success();
    }
}
