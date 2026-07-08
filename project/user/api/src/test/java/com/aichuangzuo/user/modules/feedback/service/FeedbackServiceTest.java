package com.aichuangzuo.user.modules.feedback.service;

import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import com.aichuangzuo.user.modules.feedback.dto.request.SubmitFeedbackRequest;
import com.aichuangzuo.user.modules.feedback.entity.Feedback;
import com.aichuangzuo.user.modules.feedback.mapper.FeedbackMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@Rollback
class FeedbackServiceTest {

    @Autowired
    private FeedbackService feedbackService;

    @Autowired
    private FeedbackMapper feedbackMapper;

    @Autowired
    private UserMapper userMapper;

    @Test
    void submit_valid_createsRecord() {
        User u = makeUser("fb-valid@test.com");
        SubmitFeedbackRequest req = new SubmitFeedbackRequest();
        req.setType("功能建议");
        req.setContent("希望增加 XX 功能");
        req.setContact("13800001111");

        Long id = feedbackService.submit(u.getId(), req);
        Feedback persisted = feedbackMapper.selectById(id);
        assertNotNull(persisted);
        assertEquals(u.getId(), persisted.getUserId());
        assertEquals("功能建议", persisted.getType());
        assertEquals("希望增加 XX 功能", persisted.getContent());
        assertEquals("13800001111", persisted.getContact());
        assertEquals(0, persisted.getStatus());
    }

    @Test
    void submit_dailyLimit_throws() {
        User u = makeUser("fb-limit@test.com");
        SubmitFeedbackRequest req = baseReq();
        for (int i = 0; i < 5; i++) {
            feedbackService.submit(u.getId(), req);
        }
        Exception ex = assertThrows(Exception.class, () -> feedbackService.submit(u.getId(), req));
        assertTrue(ex.getMessage().contains("今日反馈次数已达上限"));
    }

    @Test
    void submit_blankContent_throws() {
        User u = makeUser("fb-blank@test.com");
        SubmitFeedbackRequest req = new SubmitFeedbackRequest();
        req.setType("其他");
        req.setContent(" ");
        assertThrows(Exception.class, () -> feedbackService.submit(u.getId(), req));
    }

    @Test
    void submit_returnsId() {
        User u = makeUser("fb-id@test.com");
        Long id = feedbackService.submit(u.getId(), baseReq());
        assertNotNull(id);
        assertTrue(id > 0);
    }

    private SubmitFeedbackRequest baseReq() {
        SubmitFeedbackRequest req = new SubmitFeedbackRequest();
        req.setType("功能建议");
        req.setContent("测试内容");
        req.setContact("test@x.com");
        return req;
    }

    private User makeUser(String email) {
        User u = new User();
        u.setBizNo("FB" + System.nanoTime());
        u.setEmail(email);
        u.setPasswordHash("x");
        u.setInviteCode("I" + System.nanoTime());
        u.setUserStatus(1);
        u.setUserType(1);
        u.setEmailVerified(1);
        u.setCoinBalance(BigDecimal.ZERO);
        userMapper.insert(u);
        return u;
    }
}
