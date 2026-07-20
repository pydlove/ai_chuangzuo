package com.aichuangzuo.user.modules.benefit.service;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import com.aichuangzuo.user.modules.benefit.entity.BenefitUsage;
import com.aichuangzuo.user.modules.benefit.enums.BenefitErrorCode;
import com.aichuangzuo.user.modules.benefit.mapper.BenefitUsageMapper;
import com.aichuangzuo.user.modules.benefit.vo.BenefitCheckVO;
import com.aichuangzuo.user.modules.benefit.vo.UserBenefitVO;
import com.aichuangzuo.user.modules.membership.entity.UserMembership;
import com.aichuangzuo.user.modules.membership.mapper.UserMembershipMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@Rollback
class BenefitServiceTest {

    @Autowired
    private BenefitService benefitService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserMembershipMapper userMembershipMapper;

    @Autowired
    private BenefitUsageMapper benefitUsageMapper;

    // ── getMyBenefits ──

    @Test
    void getMyBenefits_noMembership_returnsFreeWithEmptyBenefits() {
        User user = createUser("benefit-free@test.com");

        UserBenefitVO vo = benefitService.getMyBenefits(user.getId());

        assertEquals("free", vo.getPlanKey());
        assertEquals("免费版", vo.getPlanName());
        assertNull(vo.getExpiresAt());
        assertNotNull(vo.getBenefits());
        assertTrue(vo.getBenefits().isEmpty());
    }

    @Test
    void getMyBenefits_expiredMembership_returnsFree() {
        User user = createUser("benefit-expired@test.com");
        createMembership(user.getId(), "pro", LocalDate.now().minusDays(1));

        UserBenefitVO vo = benefitService.getMyBenefits(user.getId());

        assertEquals("free", vo.getPlanKey());
        assertTrue(vo.getBenefits().isEmpty());
    }

    @Test
    void getMyBenefits_proMembership_returns17BenefitsWithQuotaUsage() {
        User user = createUser("benefit-pro@test.com");
        createMembership(user.getId(), "pro", LocalDate.now().plusDays(30));

        UserBenefitVO vo = benefitService.getMyBenefits(user.getId());

        assertEquals("pro", vo.getPlanKey());
        assertEquals("专业版", vo.getPlanName());
        assertEquals(LocalDate.now().plusDays(30).toString(), vo.getExpiresAt());
        // 15 历史权益 + style_market_publish / style_learn_analyze (V1.0.0_029)
        assertEquals(17, vo.getBenefits().size());

        UserBenefitVO.BenefitItem quota = vo.getBenefits().stream()
                .filter(b -> "ai_article_quota".equals(b.getCode()))
                .findFirst().orElseThrow();
        assertEquals("quota", quota.getType());
        assertEquals("100", quota.getValue());
        assertEquals(0, quota.getUsed());
        assertEquals(100, quota.getRemaining());

        UserBenefitVO.BenefitItem bool = vo.getBenefits().stream()
                .filter(b -> "ai_title_optimize".equals(b.getCode()))
                .findFirst().orElseThrow();
        assertEquals("boolean", bool.getType());
        assertEquals("true", bool.getValue());
        assertNull(bool.getUsed());
    }

    // ── check ──

    @Test
    void check_unknownCode_throwsNotFound() {
        User user = createUser("benefit-check-unknown@test.com");
        createMembership(user.getId(), "pro", LocalDate.now().plusDays(30));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> benefitService.check(user.getId(), "not_exist_code"));
        assertEquals(BenefitErrorCode.BENEFIT_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void check_booleanAllowed_returnsTrue() {
        User user = createUser("benefit-check-bool@test.com");
        createMembership(user.getId(), "pro", LocalDate.now().plusDays(30));

        BenefitCheckVO vo = benefitService.check(user.getId(), "ai_title_optimize");

        assertTrue(vo.getAllowed());
        assertEquals("boolean", vo.getType());
        assertEquals("true", vo.getValue());
    }

    @Test
    void check_booleanDenied_returnsFalseWithMessage() {
        User user = createUser("benefit-check-denied@test.com");
        createMembership(user.getId(), "basic", LocalDate.now().plusDays(30));

        BenefitCheckVO vo = benefitService.check(user.getId(), "ai_title_optimize");

        assertFalse(vo.getAllowed());
        assertEquals("false", vo.getValue());
        assertNotNull(vo.getMessage());
    }

    @Test
    void check_freeUser_returnsNotSupported() {
        User user = createUser("benefit-check-free@test.com");

        BenefitCheckVO vo = benefitService.check(user.getId(), "ai_title_optimize");

        assertFalse(vo.getAllowed());
        assertEquals(BenefitErrorCode.BENEFIT_NOT_SUPPORTED.getMessage(), vo.getMessage());
    }

    @Test
    void check_quota_returnsUsedAndRemaining() {
        User user = createUser("benefit-check-quota@test.com");
        createMembership(user.getId(), "pro", LocalDate.now().plusDays(30));
        createUsage(user.getId(), "ai_article_quota", 28);

        BenefitCheckVO vo = benefitService.check(user.getId(), "ai_article_quota");

        assertTrue(vo.getAllowed());
        assertEquals(28, vo.getUsed());
        assertEquals(72, vo.getRemaining());
    }

    @Test
    void check_tier_returnsValueForBusinessDecision() {
        User user = createUser("benefit-check-tier@test.com");
        createMembership(user.getId(), "flagship", LocalDate.now().plusDays(30));

        BenefitCheckVO vo = benefitService.check(user.getId(), "queue_priority");

        assertTrue(vo.getAllowed());
        assertEquals("tier", vo.getType());
        assertEquals("express", vo.getValue());
    }

    // ── consume ──

    @Test
    void consume_quotaFirstTime_insertsRowAndReturnsRemaining() {
        User user = createUser("benefit-consume-first@test.com");
        createMembership(user.getId(), "basic", LocalDate.now().plusDays(30));

        BenefitCheckVO vo = benefitService.consume(user.getId(), "ai_article_quota");

        assertTrue(vo.getAllowed());
        assertEquals(1, vo.getUsed());
        assertEquals(29, vo.getRemaining());
    }

    @Test
    void consume_quotaIncrementsExistingRow() {
        User user = createUser("benefit-consume-incr@test.com");
        createMembership(user.getId(), "basic", LocalDate.now().plusDays(30));
        createUsage(user.getId(), "ai_article_quota", 10);

        BenefitCheckVO vo = benefitService.consume(user.getId(), "ai_article_quota");

        assertTrue(vo.getAllowed());
        assertEquals(11, vo.getUsed());
        assertEquals(19, vo.getRemaining());
    }

    @Test
    void consume_quotaExhausted_throws() {
        User user = createUser("benefit-consume-full@test.com");
        createMembership(user.getId(), "basic", LocalDate.now().plusDays(30));
        createUsage(user.getId(), "ai_article_quota", 30);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> benefitService.consume(user.getId(), "ai_article_quota"));
        assertEquals(BenefitErrorCode.QUOTA_EXHAUSTED.getCode(), ex.getCode());
    }

    @Test
    void consume_freeUser_throwsNotSupported() {
        User user = createUser("benefit-consume-free@test.com");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> benefitService.consume(user.getId(), "ai_article_quota"));
        assertEquals(BenefitErrorCode.BENEFIT_NOT_SUPPORTED.getCode(), ex.getCode());
    }

    @Test
    void consume_booleanBenefit_throwsNotQuota() {
        User user = createUser("benefit-consume-bool@test.com");
        createMembership(user.getId(), "pro", LocalDate.now().plusDays(30));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> benefitService.consume(user.getId(), "ai_title_optimize"));
        assertEquals(BenefitErrorCode.NOT_QUOTA_BENEFIT.getCode(), ex.getCode());
    }

    // ── refund ──

    @Test
    void refund_existingUsage_decrementsUsedCount() {
        User user = createUser("benefit-refund-dec@test.com");
        createMembership(user.getId(), "pro", LocalDate.now().plusDays(30));
        createUsage(user.getId(), "ai_article_quota", 5);

        benefitService.refund(user.getId(), "ai_article_quota");

        BenefitUsage usage = benefitUsageMapper.selectByUserAndCodeAndPeriod(
                user.getId(), "ai_article_quota", YearMonth.now().toString());
        assertEquals(4, usage.getUsedCount());
    }

    @Test
    void refund_zeroUsage_staysAtZero() {
        User user = createUser("benefit-refund-zero@test.com");
        createMembership(user.getId(), "pro", LocalDate.now().plusDays(30));
        createUsage(user.getId(), "ai_article_quota", 0);

        benefitService.refund(user.getId(), "ai_article_quota");

        BenefitUsage usage = benefitUsageMapper.selectByUserAndCodeAndPeriod(
                user.getId(), "ai_article_quota", YearMonth.now().toString());
        assertEquals(0, usage.getUsedCount());
    }

    @Test
    void refund_noUsageRow_noopDoesNotThrow() {
        User user = createUser("benefit-refund-none@test.com");
        createMembership(user.getId(), "pro", LocalDate.now().plusDays(30));

        benefitService.refund(user.getId(), "ai_article_quota");

        BenefitUsage usage = benefitUsageMapper.selectByUserAndCodeAndPeriod(
                user.getId(), "ai_article_quota", YearMonth.now().toString());
        assertNull(usage);
    }

    // ── helpers ──

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

    private void createMembership(Long userId, String level, LocalDate expiresAt) {
        UserMembership membership = new UserMembership();
        membership.setUserId(userId);
        membership.setLevel(level);
        membership.setStartedAt(LocalDate.now());
        membership.setExpiresAt(expiresAt);
        membership.setTenantId(0L);
        userMembershipMapper.insert(membership);
    }

    private void createUsage(Long userId, String code, int usedCount) {
        BenefitUsage usage = new BenefitUsage();
        usage.setUserId(userId);
        usage.setBenefitCode(code);
        usage.setPeriod(YearMonth.now().toString());
        usage.setUsedCount(usedCount);
        usage.setTenantId(0L);
        benefitUsageMapper.insert(usage);
    }
}
