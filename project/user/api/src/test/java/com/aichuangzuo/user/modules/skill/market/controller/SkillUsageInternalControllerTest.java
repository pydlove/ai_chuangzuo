package com.aichuangzuo.user.modules.skill.market.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.modules.skill.market.service.SkillUsageRecordService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SkillUsageInternalControllerTest {

    @Mock
    private SkillUsageRecordService skillUsageRecordService;

    @InjectMocks
    private SkillUsageInternalController controller;

    @Test
    void recordUsage_shouldCallService() {
        Map<String, Object> payload = Map.of(
                "taskId", 100L,
                "userId", 10L,
                "skillRef", "SK123"
        );

        Result<Void> result = controller.recordUsage(payload);

        assertEquals(0, result.getCode());
        verify(skillUsageRecordService).record("SK123", 10L);
    }

    @Test
    void recordUsage_shouldNoopWhenSkillRefMissing() {
        Map<String, Object> payload = Map.of("taskId", 100L, "userId", 10L);

        Result<Void> result = controller.recordUsage(payload);

        assertEquals(0, result.getCode());
        verify(skillUsageRecordService, never()).record(any(), any());
    }
}
