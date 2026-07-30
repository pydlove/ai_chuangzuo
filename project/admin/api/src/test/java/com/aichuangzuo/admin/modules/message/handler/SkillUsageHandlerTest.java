package com.aichuangzuo.admin.modules.message.handler;

import com.aichuangzuo.admin.modules.generation.service.SkillUsageInternalClient;
import com.aichuangzuo.admin.modules.message.entity.NotifyOutbox;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SkillUsageHandlerTest {

    @Mock
    private SkillUsageInternalClient usageClient;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private SkillUsageHandler handler;

    @Test
    void dispatch_shouldCallClient() throws Exception {
        NotifyOutbox row = new NotifyOutbox();
        row.setPayload(objectMapper.writeValueAsString(Map.of(
                "taskId", 100L,
                "userId", 10L,
                "skillRef", "SK123"
        )));

        handler.dispatch(row);

        verify(usageClient).recordUsage(100L, 10L, "SK123");
    }

    @Test
    void dispatch_shouldThrowWhenSkillRefMissing() throws Exception {
        NotifyOutbox row = new NotifyOutbox();
        row.setPayload(objectMapper.writeValueAsString(Map.of("userId", 10L)));

        assertThrows(IllegalArgumentException.class, () -> handler.dispatch(row));
    }
}
