package com.aichuangzuo.admin.modules.message.service;

import com.aichuangzuo.admin.modules.message.entity.NotifyOutbox;
import com.aichuangzuo.admin.modules.message.mapper.NotifyOutboxMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotifyOutboxServiceTest {

    @Mock
    private NotifyOutboxMapper outboxMapper;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private NotifyOutboxService service;

    @Test
    void insertPending_shouldSerializePayloadAndSetPendingStatus() {
        doAnswer(inv -> {
            NotifyOutbox row = inv.getArgument(0);
            row.setId(1L);
            return 1;
        }).when(outboxMapper).insert(any(NotifyOutbox.class));

        Map<String, Object> payload = Map.of(
                "taskId", 100L,
                "userId", 10L,
                "status", "completed"
        );
        LocalDateTime executeAfter = LocalDateTime.now().plusMinutes(1);

        Long id = service.insertPending("generation_completed", 100L, 10L, payload, executeAfter);

        assertEquals(1L, id);
        ArgumentCaptor<NotifyOutbox> captor = ArgumentCaptor.forClass(NotifyOutbox.class);
        verify(outboxMapper).insert(captor.capture());
        NotifyOutbox row = captor.getValue();
        assertEquals("generation_completed", row.getBizType());
        assertEquals(Long.valueOf(100L), row.getBizId());
        assertEquals(Long.valueOf(10L), row.getTargetUserId());
        assertEquals(Integer.valueOf(NotifyOutbox.Status.PENDING.code()), row.getStatus());
        assertEquals(Integer.valueOf(0), row.getRetryCount());
        assertEquals(executeAfter, row.getNextRetryAt());
        assertEquals(Long.valueOf(0L), row.getTenantId());
        assertEquals(Integer.valueOf(0), row.getIsDeleted());
        assertTrue(row.getPayload().contains("\"taskId\":100"));
    }

    @Test
    void insertPending_shouldDefaultExecuteAfterToNow() {
        doAnswer(inv -> {
            NotifyOutbox row = inv.getArgument(0);
            row.setId(2L);
            return 1;
        }).when(outboxMapper).insert(any(NotifyOutbox.class));

        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        service.insertPending("generation_failed", 200L, 20L, Map.of("taskId", 200L));
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        ArgumentCaptor<NotifyOutbox> captor = ArgumentCaptor.forClass(NotifyOutbox.class);
        verify(outboxMapper).insert(captor.capture());
        NotifyOutbox row = captor.getValue();
        assertNotNull(row.getNextRetryAt());
        assertTrue(!row.getNextRetryAt().isBefore(before) && !row.getNextRetryAt().isAfter(after));
    }

    @Test
    void insertPending_shouldThrowWhenPayloadCannotBeSerialized() {
        // Jackson 调用 getter 时抛出 RuntimeException，service 会包装为 IllegalArgumentException
        BadPayload bad = new BadPayload();
        Map<String, Object> badPayload = Map.of("bad", bad);

        assertThrows(IllegalArgumentException.class,
                () -> service.insertPending("generation_completed", 1L, 1L, badPayload));
        verify(outboxMapper, never()).insert(any(NotifyOutbox.class));
    }

    public static class BadPayload {
        public String getValue() {
            throw new RuntimeException("serialization error");
        }
    }
}
