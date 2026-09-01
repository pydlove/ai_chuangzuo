package com.aichuangzuo.user.modules.skill.service;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import com.aichuangzuo.user.modules.membership.entity.UserMembership;
import com.aichuangzuo.user.modules.membership.mapper.UserMembershipMapper;
import com.aichuangzuo.user.modules.skill.dto.request.CreateSkillRequest;
import com.aichuangzuo.user.modules.skill.dto.request.UpdateSkillRequest;
import com.aichuangzuo.user.modules.skill.entity.UserSkill;
import com.aichuangzuo.shared.enums.error.SkillErrorCode;
import com.aichuangzuo.user.modules.skill.mapper.UserSkillMapper;
import com.aichuangzuo.user.modules.skill.vo.UserSkillVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
@Rollback
class UserSkillServiceTest {

    @Autowired
    private UserSkillService userSkillService;

    @Autowired
    private UserSkillMapper userSkillMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserMembershipMapper userMembershipMapper;

    @AfterEach
    void clear() {
        SecurityUserContext.clear();
    }

    @Test
    void shouldCreateSkillSuccessfully() {
        User user = createUser("create-skill@test.com");
        SecurityUserContext.setCurrentUserId(user.getId());

        CreateSkillRequest request = new CreateSkillRequest();
        request.setSkillName("我的小红书风");
        request.setPrompt("你是一位擅长小红书种草的写手...");
        request.setScope("小红书,种草");

        UserSkillVO vo = userSkillService.createSkill(request);

        assertNotNull(vo);
        assertNotNull(vo.getBizNo());
        assertEquals("我的小红书风", vo.getSkillName());
        assertEquals("小红书,种草", vo.getScope());
        assertEquals(1, vo.getSourceType());
    }

    @Test
    void shouldCreateLearnedSkillWithSourceType2() {
        User user = createUser("create-learned@test.com");
        SecurityUserContext.setCurrentUserId(user.getId());

        CreateSkillRequest request = new CreateSkillRequest();
        request.setSkillName("学习的情感文风");
        request.setPrompt("模仿参考文的克制语气...");
        request.setScope("公众号,情感文");
        request.setSourceType(2);

        UserSkillVO vo = userSkillService.createSkill(request);

        assertNotNull(vo.getBizNo());
        assertEquals(2, vo.getSourceType());
        // sourceType=2 只出现在学习列表，不出现在自定义列表
        assertEquals(1, userSkillService.listMySkills(2, null, 1, 10).getRecords().size());
        assertEquals(0, userSkillService.listMySkills(1, null, 1, 10).getRecords().size());
    }

    @Test
    void shouldDefaultSourceTypeToCustomWhenNull() {
        User user = createUser("create-default@test.com");
        SecurityUserContext.setCurrentUserId(user.getId());

        CreateSkillRequest request = new CreateSkillRequest();
        request.setSkillName("未传来源");
        request.setPrompt("prompt");

        UserSkillVO vo = userSkillService.createSkill(request);

        assertEquals(1, vo.getSourceType());
    }

    @Test
    void shouldRejectDuplicateSkillName() {
        User user = createUser("duplicate-skill@test.com");
        SecurityUserContext.setCurrentUserId(user.getId());

        CreateSkillRequest request = new CreateSkillRequest();
        request.setSkillName("重复名称");
        request.setPrompt("prompt 1");
        userSkillService.createSkill(request);

        CreateSkillRequest request2 = new CreateSkillRequest();
        request2.setSkillName("重复名称");
        request2.setPrompt("prompt 2");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userSkillService.createSkill(request2));
        assertEquals(SkillErrorCode.SKILL_NAME_EXISTS.getCode(), ex.getCode());
    }

    @Test
    void shouldListOnlyCurrentUserSkills() {
        User userA = createUser("list-a@test.com");
        User userB = createUser("list-b@test.com");

        createSkillDirectly(userA.getId(), "A 的风格");
        createSkillDirectly(userB.getId(), "B 的风格");

        SecurityUserContext.setCurrentUserId(userA.getId());
        List<UserSkillVO> list = userSkillService.listMySkills(1, null, 1, 10).getRecords();

        assertEquals(1, list.size());
        assertEquals("A 的风格", list.get(0).getSkillName());
    }

    @Test
    void shouldRejectDuplicateSkillNameAcrossSourceTypes() {
        User user = createUser("cross-type-duplicate@test.com");
        SecurityUserContext.setCurrentUserId(user.getId());

        CreateSkillRequest customRequest = new CreateSkillRequest();
        customRequest.setSkillName("跨类型重复");
        customRequest.setPrompt("prompt 1");
        customRequest.setSourceType(1);
        userSkillService.createSkill(customRequest);

        CreateSkillRequest learnedRequest = new CreateSkillRequest();
        learnedRequest.setSkillName("跨类型重复");
        learnedRequest.setPrompt("prompt 2");
        learnedRequest.setSourceType(2);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userSkillService.createSkill(learnedRequest));
        assertEquals(SkillErrorCode.SKILL_NAME_EXISTS.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("我的提示词"));
    }

    @Test
    void shouldRejectUpdateSkillNameToExistingCrossSourceType() {
        User user = createUser("update-cross-type-duplicate@test.com");
        SecurityUserContext.setCurrentUserId(user.getId());

        CreateSkillRequest customRequest = new CreateSkillRequest();
        customRequest.setSkillName("已有名称");
        customRequest.setPrompt("prompt 1");
        customRequest.setSourceType(1);
        userSkillService.createSkill(customRequest);

        CreateSkillRequest learnedRequest = new CreateSkillRequest();
        learnedRequest.setSkillName("待修改名称");
        learnedRequest.setPrompt("prompt 2");
        learnedRequest.setSourceType(2);
        UserSkillVO learned = userSkillService.createSkill(learnedRequest);

        UpdateSkillRequest updateRequest = new UpdateSkillRequest();
        updateRequest.setSkillName("已有名称");
        updateRequest.setPrompt("prompt 2");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userSkillService.updateSkill(learned.getBizNo(), updateRequest));
        assertEquals(SkillErrorCode.SKILL_NAME_EXISTS.getCode(), ex.getCode());
    }

    @Test
    void shouldUpdateSkillSuccessfully() {
        User user = createUser("update-skill@test.com");
        SecurityUserContext.setCurrentUserId(user.getId());

        CreateSkillRequest createRequest = new CreateSkillRequest();
        createRequest.setSkillName("原名称");
        createRequest.setPrompt("原提示词");
        createRequest.setScope("原标签");
        UserSkillVO created = userSkillService.createSkill(createRequest);

        UpdateSkillRequest updateRequest = new UpdateSkillRequest();
        updateRequest.setSkillName("新名称");
        updateRequest.setPrompt("新提示词");
        updateRequest.setScope("新标签1,新标签2");

        UserSkillVO updated = userSkillService.updateSkill(created.getBizNo(), updateRequest);

        assertEquals("新名称", updated.getSkillName());
        assertEquals("新提示词", updated.getPrompt());
        assertEquals("新标签1,新标签2", updated.getScope());
    }

    @Test
    void shouldUpdateSkillWithSameNameSucceed() {
        User user = createUser("update-same-name@test.com");
        SecurityUserContext.setCurrentUserId(user.getId());

        CreateSkillRequest createRequest = new CreateSkillRequest();
        createRequest.setSkillName("原名称");
        createRequest.setPrompt("原提示词");
        UserSkillVO created = userSkillService.createSkill(createRequest);

        UpdateSkillRequest updateRequest = new UpdateSkillRequest();
        updateRequest.setSkillName("原名称");
        updateRequest.setPrompt("修改后的提示词");

        UserSkillVO updated = userSkillService.updateSkill(created.getBizNo(), updateRequest);

        assertEquals("原名称", updated.getSkillName());
        assertEquals("修改后的提示词", updated.getPrompt());
    }

    @Test
    void shouldResetAuditStatusToPendingWhenUpdatingRejectedSkill() {
        User user = createUser("update-rejected@test.com");
        SecurityUserContext.setCurrentUserId(user.getId());

        CreateSkillRequest createRequest = new CreateSkillRequest();
        createRequest.setSkillName("被打回的风格");
        createRequest.setPrompt("原提示词");
        UserSkillVO created = userSkillService.createSkill(createRequest);

        // 模拟管理端打回
        UserSkill rejected = userSkillMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserSkill>()
                        .eq(UserSkill::getBizNo, created.getBizNo()));
        rejected.setAuditStatus(2);
        rejected.setRejectReason("过于宽泛");
        userSkillMapper.updateById(rejected);

        // 用户修改后重新提交
        UpdateSkillRequest updateRequest = new UpdateSkillRequest();
        updateRequest.setSkillName("被打回的风格-修订");
        updateRequest.setPrompt("修订后的提示词");

        UserSkillVO updated = userSkillService.updateSkill(created.getBizNo(), updateRequest);
        assertEquals("被打回的风格-修订", updated.getSkillName());
        assertEquals(Integer.valueOf(0), updated.getAuditStatus());

        UserSkill afterUpdate = userSkillMapper.selectById(rejected.getId());
        assertEquals(Integer.valueOf(0), afterUpdate.getAuditStatus());
        assertEquals(null, afterUpdate.getRejectReason());
    }

    @Test
    void shouldRejectUpdateNonExistentSkill() {
        User user = createUser("update-none@test.com");
        SecurityUserContext.setCurrentUserId(user.getId());

        UpdateSkillRequest updateRequest = new UpdateSkillRequest();
        updateRequest.setSkillName("名称");
        updateRequest.setPrompt("提示词");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userSkillService.updateSkill("SNOTEXIST", updateRequest));
        assertEquals(SkillErrorCode.SKILL_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void shouldDeleteSkillSuccessfully() {
        User user = createUser("delete-skill@test.com");
        SecurityUserContext.setCurrentUserId(user.getId());

        CreateSkillRequest request = new CreateSkillRequest();
        request.setSkillName("待删除");
        request.setPrompt("prompt");
        UserSkillVO created = userSkillService.createSkill(request);

        userSkillService.deleteSkill(created.getBizNo());

        UserSkill deleted = userSkillMapper.selectById(created.getBizNo());
        assertTrue(deleted == null || deleted.getIsDeleted() == 1);
    }

    @Test
    void shouldRejectCrossUserAccess() {
        User userA = createUser("cross-a@test.com");
        User userB = createUser("cross-b@test.com");

        UserSkill skill = createSkillDirectly(userA.getId(), "A 的私有风格");

        SecurityUserContext.setCurrentUserId(userB.getId());

        UpdateSkillRequest updateRequest = new UpdateSkillRequest();
        updateRequest.setSkillName("越权修改");
        updateRequest.setPrompt("prompt");

        BusinessException updateEx = assertThrows(BusinessException.class,
                () -> userSkillService.updateSkill(skill.getBizNo(), updateRequest));
        assertEquals(SkillErrorCode.SKILL_NOT_FOUND.getCode(), updateEx.getCode());

        BusinessException deleteEx = assertThrows(BusinessException.class,
                () -> userSkillService.deleteSkill(skill.getBizNo()));
        assertEquals(SkillErrorCode.SKILL_NOT_FOUND.getCode(), deleteEx.getCode());
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

        // 默认给测试用户一个旗舰版会员，确保我的风格额度充足
        UserMembership membership = new UserMembership();
        membership.setUserId(user.getId());
        membership.setLevel("flagship");
        membership.setStartedAt(LocalDate.now());
        membership.setExpiresAt(LocalDate.now().plusDays(30));
        membership.setTenantId(0L);
        userMembershipMapper.insert(membership);
        return user;
    }

    private UserSkill createSkillDirectly(Long userId, String skillName) {
        UserSkill skill = new UserSkill();
        skill.setBizNo("S" + System.nanoTime());
        skill.setUserId(userId);
        skill.setSkillName(skillName);
        skill.setPrompt("prompt for " + skillName);
        skill.setScope("标签");
        skill.setSourceType(1);
        skill.setUseCount(0);
        userSkillMapper.insert(skill);
        return skill;
    }
}
