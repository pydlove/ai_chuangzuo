package com.aichuangzuo.admin.modules.commission.service.impl;

import com.aichuangzuo.admin.modules.commission.entity.CommissionSubmission;
import com.aichuangzuo.admin.modules.commission.entity.CommissionTask;
import com.aichuangzuo.admin.modules.commission.mapper.CommissionSubmissionMapper;
import com.aichuangzuo.admin.modules.commission.mapper.CommissionTaskMapper;
import com.aichuangzuo.admin.modules.leaderboard.client.UserApiClient;
import com.aichuangzuo.admin.modules.user.mapper.PlatformUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 约稿管理服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
class AdminCommissionServiceImplTest {

    @Mock
    private CommissionTaskMapper taskMapper;

    @Mock
    private CommissionSubmissionMapper submissionMapper;

    @Mock
    private UserApiClient userApiClient;

    @Mock
    private PlatformUserMapper platformUserMapper;

    @InjectMocks
    private AdminCommissionServiceImpl commissionService;

    @Test
    void adopt_shouldGrantCoinAndRecordEarnings() {
        Long taskId = 1L;
        Long submissionId = 10L;
        Long submitterId = 100L;
        BigDecimal rewardCoin = new BigDecimal("50.00");

        CommissionTask task = new CommissionTask();
        task.setId(taskId);
        task.setTitle("测试约稿任务");
        task.setRewardCoin(rewardCoin);
        task.setNeededCount(5);
        task.setAdoptedCount(0);
        task.setStatus(1); // REVIEW
        task.setSelectionDeadlineAt(LocalDateTime.now().plusDays(1));

        CommissionSubmission submission = new CommissionSubmission();
        submission.setId(submissionId);
        submission.setTaskId(taskId);
        submission.setSubmitterId(submitterId);
        submission.setStatus(0); // SUBMITTED

        when(taskMapper.selectByIdForUpdate(taskId)).thenReturn(task);
        when(submissionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(submission));
        when(userApiClient.grantCoin(anyLong(), anyString(), any(), anyString(), anyString()))
                .thenReturn("CR123");

        commissionService.adopt(taskId, List.of(submissionId));

        verify(userApiClient, times(1)).grantCoin(submitterId, "commission_reward",
                rewardCoin, "commission:" + submissionId, "约稿采纳奖励：测试约稿任务");

        ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> sourceTypeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> sourceIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<BigDecimal> amountCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        ArgumentCaptor<String> monthCaptor = ArgumentCaptor.forClass(String.class);

        verify(userApiClient, times(1)).recordEarnings(anyLong(), typeCaptor.capture(),
                sourceTypeCaptor.capture(), sourceIdCaptor.capture(), anyString(), anyString(),
                amountCaptor.capture(), monthCaptor.capture());

        assertEquals("COMMISSION_REWARD", typeCaptor.getValue());
        assertEquals("commission", sourceTypeCaptor.getValue());
        assertEquals("commission:" + submissionId, sourceIdCaptor.getValue());
        assertEquals(rewardCoin, amountCaptor.getValue());
        assertNotNull(monthCaptor.getValue());
        assertEquals(7, monthCaptor.getValue().length());

        verify(submissionMapper, times(1)).updateById(any(CommissionSubmission.class));
        verify(taskMapper, times(1)).updateById(task);
    }
}
