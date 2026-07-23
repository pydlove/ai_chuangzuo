package com.aichuangzuo.user.modules.message.service;

import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import com.aichuangzuo.user.modules.message.entity.Message;
import com.aichuangzuo.user.modules.message.entity.MessageScope;
import com.aichuangzuo.user.modules.message.enums.MessageSubType;
import com.aichuangzuo.user.modules.message.mapper.MessageMapper;
import com.aichuangzuo.user.modules.message.vo.MessageVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
@Rollback
class MessageServiceTest {

    @Autowired
    private MessageService messageService;

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private UserMapper userMapper;

    @Test
    void listVisibleMessages_returnsContentAndSubType() {
        User user = createUser("msg-content@test.com");
        String longContent = "完整公告正文\n第二段\n第三段".repeat(200);
        messageMapper.insert(buildBroadcast("announcement", "测试公告", "列表摘要", longContent, null));

        List<MessageVO> list = messageService.listVisibleMessages(user.getId(), user.getCreatedAt());

        MessageVO vo = list.stream()
                .filter(v -> "测试公告".equals(v.getTitle()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("未找到测试公告"));
        assertEquals(longContent, vo.getContent());
        assertNull(vo.getSubType());
        assertEquals("列表摘要", vo.getSummary());
        assertFalse(vo.getRead());
    }

    @Test
    void listVisibleMessages_returnsMembershipExpiringSubType() {
        User user = createUser("msg-expiring@test.com");
        messageMapper.insert(buildBroadcast("membership", "会员到期", "摘要", "正文", MessageSubType.EXPIRING.getCode()));

        List<MessageVO> list = messageService.listVisibleMessages(user.getId(), user.getCreatedAt());

        MessageVO vo = list.stream()
                .filter(v -> "会员到期".equals(v.getTitle()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("未找到会员到期消息"));
        assertEquals("expiring", vo.getSubType());
        assertEquals("正文", vo.getContent());
    }

    @Test
    void pushPersonal_persistsContentAndSubType() {
        User user = createUser("msg-push@test.com");
        Long id = messageService.pushPersonal(
                user.getId(), "membership", "订阅成功", "您已订阅年会员",
                null, "完整内容\n有效期至 2027-07-08", MessageSubType.SUBSCRIBED.getCode());

        assertNotNull(id);
        Message stored = messageMapper.selectById(id);
        assertNotNull(stored);
        assertEquals(MessageScope.PERSONAL.getCode(), stored.getScope());
        assertEquals(user.getId(), stored.getTargetUserId());
        assertEquals("完整内容\n有效期至 2027-07-08", stored.getContent());
        assertEquals("subscribed", stored.getSubType());

        List<MessageVO> visible = messageService.listVisibleMessages(user.getId(), user.getCreatedAt());
        MessageVO vo = visible.stream()
                .filter(v -> id.equals(v.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("个人消息未出现在可见列表"));
        assertEquals("subscribed", vo.getSubType());
    }

    @Test
    void markRead_andMarkAllRead_unchanged() {
        User user = createUser("msg-read@test.com");
        Long id1 = messageService.publishBroadcast("announcement", "公告A", "摘要A", null, "内容A", null);
        Long id2 = messageService.publishBroadcast("feature", "新功能B", "摘要B", null, "内容B", null);

        // markRead 单条
        messageService.markRead(user.getId(), user.getCreatedAt(), id1);
        List<MessageVO> afterSingle = messageService.listVisibleMessages(user.getId(), user.getCreatedAt());
        MessageVO vo1 = afterSingle.stream().filter(v -> id1.equals(v.getId())).findFirst().orElseThrow();
        MessageVO vo2 = afterSingle.stream().filter(v -> id2.equals(v.getId())).findFirst().orElseThrow();
        assertTrue(vo1.getRead());
        assertFalse(vo2.getRead());

        // markAllRead 把剩余未读全部标记
        messageService.markAllRead(user.getId(), user.getCreatedAt());
        List<MessageVO> afterAll = messageService.listVisibleMessages(user.getId(), user.getCreatedAt());
        assertTrue(afterAll.stream().allMatch(MessageVO::getRead));
    }

    @Test
    void publishBroadcast_broadcastContentVisible() {
        User user = createUser("msg-broadcast@test.com");
        Long id = messageService.publishBroadcast("promotion", "双 11 活动", "限时", "/pricing", "完整活动说明", null);

        assertNotNull(id);
        Message stored = messageMapper.selectById(id);
        assertEquals(MessageScope.BROADCAST.getCode(), stored.getScope());
        assertNull(stored.getTargetUserId());

        List<MessageVO> visible = messageService.listVisibleMessages(user.getId(), user.getCreatedAt());
        MessageVO vo = visible.stream()
                .filter(v -> id.equals(v.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("广播消息对任意用户不可见"));
        assertEquals("完整活动说明", vo.getContent());
        assertEquals("/pricing", vo.getLink());
    }

    @Test
    void messageSubType_of_handlesUnknownCodeGracefully() {
        assertEquals(MessageSubType.EXPIRING, MessageSubType.of("expiring"));
        assertEquals(MessageSubType.SUBSCRIBED, MessageSubType.of("subscribed"));
        assertNull(MessageSubType.of("unknown.code"));
        assertNull(MessageSubType.of(null));
    }

    private User createUser(String email) {
        User user = new User();
        user.setBizNo("B" + System.nanoTime());
        user.setEmail(email);
        user.setPasswordHash("x");
        user.setInviteCode("X" + System.nanoTime());
        user.setUserStatus(1);
        user.setEmailVerified(1);
        userMapper.insert(user);
        return user;
    }

    private Message buildBroadcast(String msgType, String title, String summary, String content, String subType) {
        Message m = new Message();
        m.setMsgType(msgType);
        m.setScope(MessageScope.BROADCAST.getCode());
        m.setTitle(title);
        m.setSummary(summary);
        m.setContent(content);
        m.setSubType(subType);
        m.setTenantId(0L);
        return m;
    }
}
