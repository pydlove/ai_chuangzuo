package com.aichuangzuo.user.modules.message.service.impl;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.message.entity.Message;
import com.aichuangzuo.user.modules.message.entity.MessageRead;
import com.aichuangzuo.user.modules.message.entity.MessageScope;
import com.aichuangzuo.user.modules.message.enums.MessageErrorCode;
import com.aichuangzuo.user.modules.message.mapper.MessageMapper;
import com.aichuangzuo.user.modules.message.mapper.MessageReadMapper;
import com.aichuangzuo.user.modules.message.service.MessageService;
import com.aichuangzuo.user.modules.message.vo.MessageVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户消息中心服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageMapper messageMapper;
    private final MessageReadMapper messageReadMapper;

    @Override
    public List<MessageVO> listVisibleMessages(Long userId) {
        return messageMapper.selectVisibleMessages(userId);
    }

    @Override
    @Transactional
    public void markRead(Long userId, Long messageId) {
        Long visibleId = messageMapper.selectVisibleMessageId(messageId, userId);
        if (visibleId == null) {
            throw new BusinessException(MessageErrorCode.MESSAGE_NOT_FOUND);
        }

        MessageRead existing = messageReadMapper.selectOne(
                new LambdaQueryWrapper<MessageRead>()
                        .eq(MessageRead::getUserId, userId)
                        .eq(MessageRead::getMessageId, messageId)
        );
        if (existing != null) {
            return;
        }

        MessageRead readRecord = new MessageRead();
        readRecord.setUserId(userId);
        readRecord.setMessageId(messageId);
        readRecord.setReadAt(LocalDateTime.now());
        readRecord.setTenantId(0L);
        messageReadMapper.insert(readRecord);
        log.info("消息标记已读 userId={}, messageId={}", userId, messageId);
    }

    @Override
    @Transactional
    public void markAllRead(Long userId) {
        List<MessageVO> unreadMessages = listVisibleMessages(userId).stream()
                .filter(m -> Boolean.FALSE.equals(m.getRead()))
                .toList();

        LocalDateTime now = LocalDateTime.now();
        for (MessageVO message : unreadMessages) {
            MessageRead readRecord = new MessageRead();
            readRecord.setUserId(userId);
            readRecord.setMessageId(message.getId());
            readRecord.setReadAt(now);
            readRecord.setTenantId(0L);
            messageReadMapper.insert(readRecord);
        }

        if (!unreadMessages.isEmpty()) {
            log.info("消息全部已读 userId={}, count={}", userId, unreadMessages.size());
        }
    }

    @Override
    @Transactional
    public Long pushPersonal(Long userId, String msgType, String title, String summary, String linkUrl, String content, String subType) {
        Message message = new Message();
        message.setMsgType(msgType);
        message.setScope(MessageScope.PERSONAL.getCode());
        message.setTargetUserId(userId);
        message.setTitle(title);
        message.setSummary(summary);
        message.setContent(content);
        message.setSubType(subType);
        message.setLinkUrl(linkUrl);
        message.setTenantId(0L);
        messageMapper.insert(message);
        log.info("推送个人消息 userId={}, msgType={}, subType={}, messageId={}", userId, msgType, subType, message.getId());
        return message.getId();
    }

    @Override
    @Transactional
    public Long publishBroadcast(String msgType, String title, String summary, String linkUrl, String content, String subType) {
        Message message = new Message();
        message.setMsgType(msgType);
        message.setScope(MessageScope.BROADCAST.getCode());
        message.setTitle(title);
        message.setSummary(summary);
        message.setContent(content);
        message.setSubType(subType);
        message.setLinkUrl(linkUrl);
        message.setTenantId(0L);
        messageMapper.insert(message);
        log.info("发布广播消息 msgType={}, subType={}, messageId={}", msgType, subType, message.getId());
        return message.getId();
    }
}
