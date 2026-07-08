package com.aichuangzuo.user.modules.article.service;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.article.dto.request.SaveDraftRequest;
import com.aichuangzuo.user.modules.article.dto.request.UpdateDraftRequest;
import com.aichuangzuo.user.modules.article.entity.Draft;
import com.aichuangzuo.user.modules.article.mapper.DraftMapper;
import com.aichuangzuo.user.modules.article.vo.DraftPageVO;
import com.aichuangzuo.user.modules.article.vo.DraftVO;
import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
@Rollback
class DraftServiceTest {

    @Autowired
    private DraftService draftService;

    @Autowired
    private DraftMapper draftMapper;

    @Autowired
    private UserMapper userMapper;

    @Test
    void save_shouldInsertDraftAndReturnBizNo() {
        User user = createUser("draft-save@test.com");
        SaveDraftRequest request = new SaveDraftRequest();
        request.setCustomTitle("草稿标题");
        request.setCustomRequirement("草稿需求描述");
        request.setPlatform("wechat");
        request.setWordCount(500);
        request.setStyle("warm");
        request.setTemplate("card-02");

        String bizNo = draftService.save(user.getId(), request);

        assertNotNull(bizNo);
        assertTrue(bizNo.startsWith("D"));
        Draft stored = draftMapper.selectOne(new LambdaQueryWrapper<Draft>()
                .eq(Draft::getBizNo, bizNo));
        assertNotNull(stored);
        assertEquals(user.getId(), stored.getUserId());
        assertEquals("草稿标题", stored.getCustomTitle());
        assertEquals("草稿需求描述", stored.getCustomRequirement());
        assertEquals("wechat", stored.getPlatform());
        assertEquals(Integer.valueOf(500), stored.getWordCount());
        assertNotNull(stored.getSavedAt());
    }

    @Test
    void save_shouldClampNegativeWordCountToZero() {
        User user = createUser("draft-save-negative@test.com");
        SaveDraftRequest request = new SaveDraftRequest();
        request.setCustomTitle("t");
        request.setWordCount(-5);

        String bizNo = draftService.save(user.getId(), request);

        Draft stored = draftMapper.selectOne(new LambdaQueryWrapper<Draft>()
                .eq(Draft::getBizNo, bizNo));
        assertEquals(Integer.valueOf(0), stored.getWordCount());
    }

    @Test
    void list_shouldReturnCurrentUserOnlyOrderedBySavedAtDesc() {
        User alice = createUser("draft-list-alice@test.com");
        User bob = createUser("draft-list-bob@test.com");
        String bizNo1 = createDraft(alice.getId(), "A1", "r");
        createDraft(alice.getId(), "A2", "r");
        createDraft(bob.getId(), "B1", "r");

        DraftPageVO page = draftService.list(alice.getId(), null, 1, 10);

        assertEquals(2, page.getTotal());
        // 后插入的 A2 savedAt 更新，应该在前
        assertEquals("A2", page.getList().get(0).getCustomTitle());
        assertEquals(bizNo1, page.getList().get(1).getBizNo());
    }

    @Test
    void list_shouldFilterByTitleOrRequirement() {
        User user = createUser("draft-list-kw@test.com");
        createDraftWithRequirement(user.getId(), "标题1", "我的春天故事");
        createDraftWithRequirement(user.getId(), "标题2", "我的夏天故事");

        DraftPageVO page = draftService.list(user.getId(), "夏天", 1, 10);

        assertEquals(1, page.getTotal());
        assertEquals("标题2", page.getList().get(0).getCustomTitle());
    }

    @Test
    void list_shouldIgnoreBlankKeyword() {
        User user = createUser("draft-list-blank-kw@test.com");
        createDraft(user.getId(), "d1", "r");
        createDraft(user.getId(), "d2", "r");

        DraftPageVO page = draftService.list(user.getId(), "  ", 1, 10);

        assertEquals(2, page.getTotal());
    }

    @Test
    void get_shouldReturnDraft() {
        User user = createUser("draft-get@test.com");
        String bizNo = createDraft(user.getId(), "d", "r");

        DraftVO vo = draftService.get(user.getId(), bizNo);

        assertEquals(bizNo, vo.getBizNo());
        assertEquals("d", vo.getCustomTitle());
    }

    @Test
    void get_shouldThrowWhenNotFound() {
        User user = createUser("draft-get-missing@test.com");
        assertThrows(BusinessException.class, () -> draftService.get(user.getId(), "D0000000000000000"));
    }

    @Test
    void get_shouldThrowForOtherUsersDraft() {
        User alice = createUser("draft-get-isolation-alice@test.com");
        User bob = createUser("draft-get-isolation-bob@test.com");
        String bizNo = createDraft(alice.getId(), "私密", "r");
        assertThrows(BusinessException.class, () -> draftService.get(bob.getId(), bizNo));
    }

    @Test
    void update_shouldPatchFieldsAndBumpSavedAt() {
        User user = createUser("draft-update@test.com");
        String bizNo = createDraft(user.getId(), "原", "原需求");
        Draft original = draftMapper.selectOne(new LambdaQueryWrapper<Draft>()
                .eq(Draft::getBizNo, bizNo));
        java.time.LocalDateTime originalSavedAt = original.getSavedAt();

        UpdateDraftRequest request = new UpdateDraftRequest();
        request.setCustomTitle("新");
        request.setCustomRequirement("新需求");
        request.setWordCount(800);
        request.setStyle("calm");

        // 故意睡眠一下确保 savedAt 变化
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        draftService.update(user.getId(), bizNo, request);

        Draft stored = draftMapper.selectOne(new LambdaQueryWrapper<Draft>()
                .eq(Draft::getBizNo, bizNo));
        assertEquals("新", stored.getCustomTitle());
        assertEquals("新需求", stored.getCustomRequirement());
        assertEquals(Integer.valueOf(800), stored.getWordCount());
        assertEquals("calm", stored.getStyle());
        assertTrue(stored.getSavedAt().isAfter(originalSavedAt));
    }

    @Test
    void update_shouldThrowForOtherUsersDraft() {
        User alice = createUser("draft-update-isolation-alice@test.com");
        User bob = createUser("draft-update-isolation-bob@test.com");
        String bizNo = createDraft(alice.getId(), "私密", "r");
        UpdateDraftRequest request = new UpdateDraftRequest();
        request.setCustomTitle("尝试改");
        assertThrows(BusinessException.class, () -> draftService.update(bob.getId(), bizNo, request));
    }

    @Test
    void delete_shouldSoftDelete() {
        User user = createUser("draft-delete@test.com");
        String bizNo = createDraft(user.getId(), "d", "r");

        draftService.delete(user.getId(), bizNo);

        Draft stored = draftMapper.selectOne(new LambdaQueryWrapper<Draft>()
                .eq(Draft::getBizNo, bizNo));
        assertNull(stored);
        DraftPageVO page = draftService.list(user.getId(), null, 1, 10);
        assertEquals(0, page.getTotal());
    }

    @Test
    void delete_shouldThrowForOtherUsersDraft() {
        User alice = createUser("draft-delete-isolation-alice@test.com");
        User bob = createUser("draft-delete-isolation-bob@test.com");
        String bizNo = createDraft(alice.getId(), "私密", "r");
        assertThrows(BusinessException.class, () -> draftService.delete(bob.getId(), bizNo));
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

    private String createDraft(Long userId, String title, String requirement) {
        return createDraftWithRequirement(userId, title, requirement);
    }

    private String createDraftWithRequirement(Long userId, String title, String requirement) {
        SaveDraftRequest request = new SaveDraftRequest();
        request.setCustomTitle(title);
        request.setCustomRequirement(requirement);
        request.setPlatform("wechat");
        request.setWordCount(100);
        request.setStyle("warm");
        request.setTemplate("card-01");
        return draftService.save(userId, request);
    }
}