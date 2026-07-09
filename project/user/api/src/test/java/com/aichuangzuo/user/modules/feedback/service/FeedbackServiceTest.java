package com.aichuangzuo.user.modules.feedback.service;

import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import com.aichuangzuo.user.modules.feedback.dto.request.SubmitFeedbackRequest;
import com.aichuangzuo.user.modules.feedback.entity.Feedback;
import com.aichuangzuo.user.modules.feedback.mapper.FeedbackMapper;
import com.aichuangzuo.user.modules.feedback.vo.FeedbackVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

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

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void submit_valid_createsRecord() {
        User u = makeUser("fb-valid@test.com");
        SubmitFeedbackRequest req = new SubmitFeedbackRequest();
        req.setType("功能建议");
        req.setContent("希望增加 XX 功能");

        Long id = feedbackService.submit(u.getId(), req);
        Feedback persisted = feedbackMapper.selectById(id);
        assertNotNull(persisted);
        assertEquals(u.getId(), persisted.getUserId());
        assertEquals("功能建议", persisted.getType());
        assertEquals("希望增加 XX 功能", persisted.getContent());
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

    @Test
    void pageByUser_returnsOnlyCurrentUser() {
        User a = makeUser("fb-page-a@test.com");
        User b = makeUser("fb-page-b@test.com");
        feedbackService.submit(a.getId(), baseReq());
        feedbackService.submit(a.getId(), baseReq());
        feedbackService.submit(b.getId(), baseReq());

        List<FeedbackVO> listA = feedbackService.pageByUser(a.getId(), null, 1, 20);
        assertEquals(2, listA.size());
        assertEquals(2L, feedbackService.countByUser(a.getId(), null));
        List<FeedbackVO> listB = feedbackService.pageByUser(b.getId(), null, 1, 20);
        assertEquals(1, listB.size());
    }

    @Test
    void pageByUser_statusFilter_excludesOther() {
        User u = makeUser("fb-status@test.com");
        Long id1 = feedbackService.submit(u.getId(), baseReq());
        feedbackService.submit(u.getId(), baseReq());
        // 手动改第 1 条为已回复
        jdbc.update("UPDATE u_feedback SET status = 1, reply_content = '已回', replied_at = NOW(3), reply_admin_id = 1 WHERE id = ?", id1);

        List<FeedbackVO> pending = feedbackService.pageByUser(u.getId(), 0, 1, 20);
        assertEquals(1, pending.size());
        assertEquals(0, pending.get(0).getStatus());
    }

    @Test
    void pageByUser_emptyResult_returnsEmpty() {
        User u = makeUser("fb-empty@test.com");
        List<FeedbackVO> list = feedbackService.pageByUser(u.getId(), null, 1, 20);
        assertTrue(list.isEmpty());
        assertEquals(0L, feedbackService.countByUser(u.getId(), null));
    }

    private SubmitFeedbackRequest baseReq() {
        SubmitFeedbackRequest req = new SubmitFeedbackRequest();
        req.setType("功能建议");
        req.setContent("测试内容");
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
