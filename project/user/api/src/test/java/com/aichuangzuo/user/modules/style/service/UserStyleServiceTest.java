package com.aichuangzuo.user.modules.style.service;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import com.aichuangzuo.user.modules.style.dto.request.CreateStyleRequest;
import com.aichuangzuo.user.modules.style.dto.request.UpdateStyleRequest;
import com.aichuangzuo.user.modules.style.entity.UserStyle;
import com.aichuangzuo.user.modules.style.enums.StyleErrorCode;
import com.aichuangzuo.user.modules.style.mapper.UserStyleMapper;
import com.aichuangzuo.user.modules.style.vo.UserStyleVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
@Rollback
class UserStyleServiceTest {

    @Autowired
    private UserStyleService userStyleService;

    @Autowired
    private UserStyleMapper userStyleMapper;

    @Autowired
    private UserMapper userMapper;

    @AfterEach
    void clear() {
        SecurityUserContext.clear();
    }

    @Test
    void shouldCreateStyleSuccessfully() {
        User user = createUser("create-style@test.com");
        SecurityUserContext.setCurrentUserId(user.getId());

        CreateStyleRequest request = new CreateStyleRequest();
        request.setStyleName("我的小红书风");
        request.setPrompt("你是一位擅长小红书种草的写手...");
        request.setScope("小红书,种草");

        UserStyleVO vo = userStyleService.createStyle(request);

        assertNotNull(vo);
        assertNotNull(vo.getBizNo());
        assertEquals("我的小红书风", vo.getStyleName());
        assertEquals("小红书,种草", vo.getScope());
        assertEquals(1, vo.getSourceType());
    }

    @Test
    void shouldCreateLearnedStyleWithSourceType2() {
        User user = createUser("create-learned@test.com");
        SecurityUserContext.setCurrentUserId(user.getId());

        CreateStyleRequest request = new CreateStyleRequest();
        request.setStyleName("学习的情感文风");
        request.setPrompt("模仿参考文的克制语气...");
        request.setScope("公众号,情感文");
        request.setSourceType(2);

        UserStyleVO vo = userStyleService.createStyle(request);

        assertNotNull(vo.getBizNo());
        assertEquals(2, vo.getSourceType());
        // sourceType=2 只出现在学习列表，不出现在自定义列表
        assertEquals(1, userStyleService.listMyStyles(2).size());
        assertEquals(0, userStyleService.listMyStyles(1).size());
    }

    @Test
    void shouldDefaultSourceTypeToCustomWhenNull() {
        User user = createUser("create-default@test.com");
        SecurityUserContext.setCurrentUserId(user.getId());

        CreateStyleRequest request = new CreateStyleRequest();
        request.setStyleName("未传来源");
        request.setPrompt("prompt");

        UserStyleVO vo = userStyleService.createStyle(request);

        assertEquals(1, vo.getSourceType());
    }

    @Test
    void shouldRejectDuplicateStyleName() {
        User user = createUser("duplicate-style@test.com");
        SecurityUserContext.setCurrentUserId(user.getId());

        CreateStyleRequest request = new CreateStyleRequest();
        request.setStyleName("重复名称");
        request.setPrompt("prompt 1");
        userStyleService.createStyle(request);

        CreateStyleRequest request2 = new CreateStyleRequest();
        request2.setStyleName("重复名称");
        request2.setPrompt("prompt 2");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userStyleService.createStyle(request2));
        assertEquals(StyleErrorCode.STYLE_NAME_EXISTS.getCode(), ex.getCode());
    }

    @Test
    void shouldListOnlyCurrentUserStyles() {
        User userA = createUser("list-a@test.com");
        User userB = createUser("list-b@test.com");

        createStyleDirectly(userA.getId(), "A 的风格");
        createStyleDirectly(userB.getId(), "B 的风格");

        SecurityUserContext.setCurrentUserId(userA.getId());
        List<UserStyleVO> list = userStyleService.listMyStyles(1);

        assertEquals(1, list.size());
        assertEquals("A 的风格", list.get(0).getStyleName());
    }

    @Test
    void shouldUpdateStyleSuccessfully() {
        User user = createUser("update-style@test.com");
        SecurityUserContext.setCurrentUserId(user.getId());

        CreateStyleRequest createRequest = new CreateStyleRequest();
        createRequest.setStyleName("原名称");
        createRequest.setPrompt("原提示词");
        createRequest.setScope("原标签");
        UserStyleVO created = userStyleService.createStyle(createRequest);

        UpdateStyleRequest updateRequest = new UpdateStyleRequest();
        updateRequest.setStyleName("新名称");
        updateRequest.setPrompt("新提示词");
        updateRequest.setScope("新标签1,新标签2");

        UserStyleVO updated = userStyleService.updateStyle(created.getBizNo(), updateRequest);

        assertEquals("新名称", updated.getStyleName());
        assertEquals("新提示词", updated.getPrompt());
        assertEquals("新标签1,新标签2", updated.getScope());
    }

    @Test
    void shouldRejectUpdateNonExistentStyle() {
        User user = createUser("update-none@test.com");
        SecurityUserContext.setCurrentUserId(user.getId());

        UpdateStyleRequest updateRequest = new UpdateStyleRequest();
        updateRequest.setStyleName("名称");
        updateRequest.setPrompt("提示词");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userStyleService.updateStyle("SNOTEXIST", updateRequest));
        assertEquals(StyleErrorCode.STYLE_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void shouldDeleteStyleSuccessfully() {
        User user = createUser("delete-style@test.com");
        SecurityUserContext.setCurrentUserId(user.getId());

        CreateStyleRequest request = new CreateStyleRequest();
        request.setStyleName("待删除");
        request.setPrompt("prompt");
        UserStyleVO created = userStyleService.createStyle(request);

        userStyleService.deleteStyle(created.getBizNo());

        UserStyle deleted = userStyleMapper.selectById(created.getBizNo());
        assertTrue(deleted == null || deleted.getIsDeleted() == 1);
    }

    @Test
    void shouldRejectCrossUserAccess() {
        User userA = createUser("cross-a@test.com");
        User userB = createUser("cross-b@test.com");

        UserStyle style = createStyleDirectly(userA.getId(), "A 的私有风格");

        SecurityUserContext.setCurrentUserId(userB.getId());

        UpdateStyleRequest updateRequest = new UpdateStyleRequest();
        updateRequest.setStyleName("越权修改");
        updateRequest.setPrompt("prompt");

        BusinessException updateEx = assertThrows(BusinessException.class,
                () -> userStyleService.updateStyle(style.getBizNo(), updateRequest));
        assertEquals(StyleErrorCode.STYLE_NOT_FOUND.getCode(), updateEx.getCode());

        BusinessException deleteEx = assertThrows(BusinessException.class,
                () -> userStyleService.deleteStyle(style.getBizNo()));
        assertEquals(StyleErrorCode.STYLE_NOT_FOUND.getCode(), deleteEx.getCode());
    }

    private User createUser(String email) {
        User user = new User();
        user.setBizNo("B" + System.nanoTime());
        user.setEmail(email);
        user.setPasswordHash("x");
        user.setInviteCode("X" + System.nanoTime());
        user.setUserStatus(1);
        user.setEmailVerified(1);
        user.setCreatedAt(LocalDateTime.now());
        userMapper.insert(user);
        return user;
    }

    private UserStyle createStyleDirectly(Long userId, String styleName) {
        UserStyle style = new UserStyle();
        style.setBizNo("S" + System.nanoTime());
        style.setUserId(userId);
        style.setStyleName(styleName);
        style.setPrompt("prompt for " + styleName);
        style.setScope("标签");
        style.setSourceType(1);
        style.setUseCount(0);
        userStyleMapper.insert(style);
        return style;
    }
}
