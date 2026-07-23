package com.aichuangzuo.user.modules.message.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import com.aichuangzuo.user.modules.message.service.MessageService;
import com.aichuangzuo.user.modules.message.vo.MessageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户端消息中心接口。
 */
@RestController
@RequestMapping("/api/v1/user/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final UserMapper userMapper;

    @GetMapping
    public Result<List<MessageVO>> list() {
        Long userId = SecurityUserContext.getCurrentUserId();
        LocalDateTime registerAt = getRegisterAt(userId);
        return Result.success(messageService.listVisibleMessages(userId, registerAt));
    }

    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable("id") Long messageId) {
        Long userId = SecurityUserContext.getCurrentUserId();
        LocalDateTime registerAt = getRegisterAt(userId);
        messageService.markRead(userId, registerAt, messageId);
        return Result.success();
    }

    @PutMapping("/read-all")
    public Result<Void> markAllRead() {
        Long userId = SecurityUserContext.getCurrentUserId();
        LocalDateTime registerAt = getRegisterAt(userId);
        messageService.markAllRead(userId, registerAt);
        return Result.success();
    }

    private LocalDateTime getRegisterAt(Long userId) {
        User user = userMapper.selectById(userId);
        return user != null ? user.getCreatedAt() : LocalDateTime.MIN;
    }
}
