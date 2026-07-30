package com.aichuangzuo.user.modules.generation.service;

import com.aichuangzuo.user.modules.benefit.service.BenefitService;
import com.aichuangzuo.user.modules.generation.mapper.UserMembershipMirrorMapper;
import com.aichuangzuo.user.modules.membership.enums.MembershipPlan;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerationBenefitResolverTest {

    @Mock
    private UserMembershipMirrorMapper userMapper;

    @Mock
    private BenefitService benefitService;

    @InjectMocks
    private GenerationBenefitResolver resolver;

    @Test
    void ratePerMinute_flagship_returnsConfiguredValue() {
        Long userId = 1L;
        mockMembership(userId, MembershipPlan.FLAGSHIP, LocalDateTime.now().plusDays(1));
        when(benefitService.getPlanBenefitValue(userId, "generation_rate_limit", "8")).thenReturn("10");

        assertEquals(10, resolver.ratePerMinute(userId));
    }

    @Test
    void ratePerMinute_pro_returnsConfiguredValue() {
        Long userId = 2L;
        mockMembership(userId, MembershipPlan.PRO, LocalDateTime.now().plusDays(1));
        when(benefitService.getPlanBenefitValue(userId, "generation_rate_limit", "5")).thenReturn("6");

        assertEquals(6, resolver.ratePerMinute(userId));
    }

    @Test
    void ratePerMinute_basic_returnsConfiguredValue() {
        Long userId = 3L;
        mockMembership(userId, MembershipPlan.BASIC, LocalDateTime.now().plusDays(1));
        when(benefitService.getPlanBenefitValue(userId, "generation_rate_limit", "3")).thenReturn("4");

        assertEquals(4, resolver.ratePerMinute(userId));
    }

    @Test
    void ratePerMinute_missingConfig_fallsBackToDefault() {
        Long userId = 4L;
        mockMembership(userId, MembershipPlan.PRO, LocalDateTime.now().plusDays(1));
        when(benefitService.getPlanBenefitValue(userId, "generation_rate_limit", "5")).thenReturn("5");

        assertEquals(5, resolver.ratePerMinute(userId));
    }

    @Test
    void ratePerMinute_invalidConfig_fallsBackToDefault() {
        Long userId = 5L;
        mockMembership(userId, MembershipPlan.FLAGSHIP, LocalDateTime.now().plusDays(1));
        when(benefitService.getPlanBenefitValue(userId, "generation_rate_limit", "8")).thenReturn("abc");

        assertEquals(8, resolver.ratePerMinute(userId));
    }

    @Test
    void ratePerMinute_nonPositiveConfig_fallsBackToDefault() {
        Long userId = 6L;
        mockMembership(userId, MembershipPlan.BASIC, LocalDateTime.now().plusDays(1));
        when(benefitService.getPlanBenefitValue(userId, "generation_rate_limit", "3")).thenReturn("-1");

        assertEquals(3, resolver.ratePerMinute(userId));
    }

    @Test
    void ratePerMinute_expiredMembership_fallsBackToBasicDefault() {
        Long userId = 7L;
        mockMembership(userId, MembershipPlan.PRO, LocalDateTime.now().minusDays(1));
        when(benefitService.getPlanBenefitValue(userId, "generation_rate_limit", "3")).thenReturn("3");

        assertEquals(3, resolver.ratePerMinute(userId));
    }

    @Test
    void ratePerMinute_noMembership_fallsBackToBasicDefault() {
        Long userId = 8L;
        when(userMapper.selectMembership(userId)).thenReturn(null);
        when(benefitService.getPlanBenefitValue(userId, "generation_rate_limit", "3")).thenReturn("3");

        assertEquals(3, resolver.ratePerMinute(userId));
    }

    private void mockMembership(Long userId, MembershipPlan plan, LocalDateTime expireAt) {
        UserMembershipMirrorMapper.MembershipMirror mirror = new UserMembershipMirrorMapper.MembershipMirror();
        mirror.setPlanKey(plan.getKey());
        mirror.setExpireAt(expireAt);
        when(userMapper.selectMembership(userId)).thenReturn(mirror);
    }
}
