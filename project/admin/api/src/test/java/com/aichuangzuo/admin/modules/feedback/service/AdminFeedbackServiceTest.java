package com.aichuangzuo.admin.modules.feedback.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
@Rollback
class AdminFeedbackServiceTest {

    @Autowired private AdminFeedbackService adminFeedbackService;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void reply_persistsAndMarksReplied() {
        Long userId = insertUser();
        Long fbId = insertFeedback(userId, 0, null);

        adminFeedbackService.reply(fbId, 1L, "已收到，感谢反馈");

        String reply = jdbc.queryForObject("SELECT reply_content FROM u_feedback WHERE id = ?", String.class, fbId);
        Integer status = jdbc.queryForObject("SELECT status FROM u_feedback WHERE id = ?", Integer.class, fbId);
        assertNotNull(reply);
        assertEquals("已收到，感谢反馈", reply);
        assertEquals(1, status);
    }

    @Test
    void reply_alreadyReplied_throws() {
        Long userId = insertUser();
        Long fbId = insertFeedback(userId, 1, "旧回复");

        Exception ex = assertThrows(Exception.class,
                () -> adminFeedbackService.reply(fbId, 1L, "再次回复"));
        assertTrue(ex.getMessage().contains("已回复"));
    }

    @Test
    void detail_missing_throws() {
        assertThrows(Exception.class, () -> adminFeedbackService.detail(9999999L));
    }

    private Long insertUser() {
        long nano = System.nanoTime() % 1_000_000_000L;
        jdbc.update("INSERT INTO u_user (biz_no, email, password_hash, invite_code, user_status, user_type, email_verified, coin_balance, tenant_id, is_deleted, created_by, updated_by) " +
                        "VALUES (?, ?, ?, ?, 1, 1, 1, 0, 0, 0, 0, 0)",
                "AD" + nano, "fbk-ad-" + nano + "@test.com", "x", "I" + nano);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private Long insertFeedback(Long userId, int status, String replyContent) {
        jdbc.update("INSERT INTO u_feedback (user_id, type, content, reply_content, reply_admin_id, replied_at, status, tenant_id, is_deleted, created_by, updated_by) " +
                        "VALUES (?, '功能建议', '测试', ?, NULL, NULL, ?, 0, 0, ?, ?)",
                userId, replyContent, status, userId, userId);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }
}
