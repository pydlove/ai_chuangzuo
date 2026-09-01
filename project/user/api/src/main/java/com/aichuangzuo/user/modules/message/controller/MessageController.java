package com.aichuangzuo.user.modules.message.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import com.aichuangzuo.user.modules.message.service.MessageService;
import com.aichuangzuo.user.modules.message.vo.MessageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户端消息中心接口。
 */
@RestController
@RequestMapping("/api/v1/user/messages")
@RequiredArgsConstructor
@Slf4j
public class MessageController {

    private final MessageService messageService;
    private final UserMapper userMapper;

    @GetMapping
    public Result<List<MessageVO>> list() {
        Long userId = SecurityUserContext.getCurrentUserId();
        log.info("消息列表, userId={}", userId);
        LocalDateTime registerAt = getRegisterAt(userId);
        return Result.success(messageService.listVisibleMessages(userId, registerAt));
    }

    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        Long userId = SecurityUserContext.getCurrentUserId();
        log.debug("未读消息数, userId={}", userId);
        LocalDateTime registerAt = getRegisterAt(userId);
        return Result.success(messageService.countUnread(userId, registerAt));
    }

    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable("id") Long messageId) {
        Long userId = SecurityUserContext.getCurrentUserId();
        log.info("标记消息已读, userId={}, messageId={}", userId, messageId);
        LocalDateTime registerAt = getRegisterAt(userId);
        messageService.markRead(userId, registerAt, messageId);
        return Result.success();
    }

    @PutMapping("/read-all")
    public Result<Void> markAllRead() {
        Long userId = SecurityUserContext.getCurrentUserId();
        log.info("标记全部消息已读, userId={}", userId);
        LocalDateTime registerAt = getRegisterAt(userId);
        messageService.markAllRead(userId, registerAt);
        return Result.success();
    }

    private LocalDateTime getRegisterAt(Long userId) {
        User user = userMapper.selectById(userId);
        return user != null ? user.getCreatedAt() : LocalDateTime.MIN;
    }
}
