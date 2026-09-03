package com.aichuangzuo.admin.modules.lottery.service;

import com.aichuangzuo.admin.modules.lottery.dto.request.CloneCampaignRequest;
import com.aichuangzuo.admin.modules.lottery.entity.LotteryCampaign;
import com.aichuangzuo.admin.modules.lottery.entity.LotteryPrizeTier;
import com.aichuangzuo.admin.modules.lottery.mapper.LotteryCampaignMapper;
import com.aichuangzuo.admin.modules.lottery.mapper.LotteryPrizeTierMapper;
import com.aichuangzuo.admin.modules.lottery.service.impl.LotteryCampaignAdminServiceImpl;
import com.aichuangzuo.shared.enums.error.AdminLotteryErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LotteryCampaignAdminServiceTest {

    @Mock
    private LotteryCampaignMapper campaignMapper;

    @Mock
    private LotteryPrizeTierMapper prizeTierMapper;

    @InjectMocks
    private LotteryCampaignAdminServiceImpl campaignAdminService;

    @Test
    void openCampaign_shouldReopenClosedCampaign() {
        LotteryCampaign campaign = new LotteryCampaign();
        campaign.setId(1L);
        campaign.setStatus(3);
        campaign.setIsDeleted(0);

        when(campaignMapper.selectById(1L)).thenReturn(campaign);
        when(campaignMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(prizeTierMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        campaignAdminService.openCampaign(1L, 100L);

        assertEquals(1, campaign.getStatus());
        assertEquals(100L, campaign.getUpdatedBy());
    }

    @Test
    void openCampaign_shouldRejectInvalidStatus() {
        LotteryCampaign campaign = new LotteryCampaign();
        campaign.setId(1L);
        campaign.setStatus(2);
        campaign.setIsDeleted(0);

        when(campaignMapper.selectById(1L)).thenReturn(campaign);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> campaignAdminService.openCampaign(1L, 100L));
        assertEquals(AdminLotteryErrorCode.INVALID_CAMPAIGN_STATUS.getCode(), ex.getCode());
        verify(campaignMapper, never()).updateById(any(LotteryCampaign.class));
    }

    @Test
    void cloneCampaign_shouldCreateDraftCopyWithResetTiers() {
        LotteryCampaign source = new LotteryCampaign();
        source.setId(1L);
        source.setName("原活动");
        source.setDescription("描述");
        source.setRules("规则");
        source.setImageUrl("http://img");
        source.setStartTime(LocalDateTime.now());
        source.setEndTime(LocalDateTime.now().plusDays(7));
        source.setFreeDrawsPerUser(3);
        source.setStatus(3);
        source.setTenantId(0L);
        source.setIsDeleted(0);

        LotteryPrizeTier tier = new LotteryPrizeTier();
        tier.setId(10L);
        tier.setCampaignId(1L);
        tier.setTierKey("tier_1");
        tier.setTierName("一等奖");
        tier.setPrizeLevel(1);
        tier.setProbability(BigDecimal.valueOf(0.1));
        tier.setMaxWinCount(100);
        tier.setRemainingWinCount(50);
        tier.setDisplayRemaining(1);
        tier.setDisplayRemainingCount(80);
        tier.setRewardType("coin");
        tier.setRewardValueJson("{\"amount\":100}");
        tier.setCodePrefix("GIFT");
        tier.setCodeLength(12);
        tier.setCodeValidityDays(30);
        tier.setSortOrder(1);
        tier.setStatus(1);
        tier.setTenantId(0L);
        tier.setIsDeleted(0);

        when(campaignMapper.selectById(1L)).thenReturn(source);
        when(prizeTierMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(tier));
        when(campaignMapper.insert(any(LotteryCampaign.class))).thenAnswer(invocation -> {
            LotteryCampaign c = invocation.getArgument(0);
            c.setId(2L);
            return 1;
        });

        CloneCampaignRequest request = new CloneCampaignRequest();
        request.setName("原活动-副本");
        Long newId = campaignAdminService.cloneCampaign(1L, request, 100L);

        assertEquals(2L, newId);

        ArgumentCaptor<LotteryCampaign> campaignCaptor = ArgumentCaptor.forClass(LotteryCampaign.class);
        verify(campaignMapper).insert(campaignCaptor.capture());
        LotteryCampaign copiedCampaign = campaignCaptor.getValue();
        assertEquals("原活动-副本", copiedCampaign.getName());
        assertEquals(0, copiedCampaign.getStatus());
        assertEquals(0, copiedCampaign.getIsDeleted());
        assertEquals(100L, copiedCampaign.getCreatedBy());
        assertEquals(100L, copiedCampaign.getUpdatedBy());
        assertNotNull(copiedCampaign.getDescription());

        ArgumentCaptor<LotteryPrizeTier> tierCaptor = ArgumentCaptor.forClass(LotteryPrizeTier.class);
        verify(prizeTierMapper, times(1)).insert(tierCaptor.capture());
        LotteryPrizeTier copiedTier = tierCaptor.getValue();
        assertEquals(2L, copiedTier.getCampaignId());
        assertEquals("tier_1", copiedTier.getTierKey());
        assertEquals(100, copiedTier.getRemainingWinCount());
        assertEquals(1, copiedTier.getStatus());
        assertEquals(100L, copiedTier.getCreatedBy());
    }
}
