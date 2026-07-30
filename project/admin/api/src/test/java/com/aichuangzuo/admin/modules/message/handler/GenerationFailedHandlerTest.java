package com.aichuangzuo.admin.modules.message.handler;

import com.aichuangzuo.admin.modules.generation.service.GenerationNotifyInternalClient;
import com.aichuangzuo.admin.modules.message.entity.NotifyOutbox;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class GenerationFailedHandlerTest {

    @Mock
    private GenerationNotifyInternalClient notifyClient;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private GenerationFailedHandler handler;

    @Test
    void dispatch_shouldParsePayloadAndNotifyFailed() {
        NotifyOutbox row = new NotifyOutbox();
        row.setPayload("{\"taskId\":200,\"userId\":20,\"failReason\":\"AI 超时\"}");

        handler.dispatch(row);

        verify(notifyClient).notifyFailed(200L, 20L, "AI 超时");
    }

    @Test
    void dispatch_shouldAllowNullFailReason() {
        NotifyOutbox row = new NotifyOutbox();
        row.setPayload("{\"taskId\":201,\"userId\":21}");

        handler.dispatch(row);

        verify(notifyClient).notifyFailed(201L, 21L, null);
    }

    @Test
    void dispatch_shouldThrowWhenTaskIdMissing() {
        NotifyOutbox row = new NotifyOutbox();
        row.setPayload("{\"userId\":20,\"failReason\":\"x\"}");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> handler.dispatch(row));
        assertTrue(ex.getMessage().contains("taskId"));
        verifyNoInteractions(notifyClient);
    }

    @Test
    void dispatch_shouldThrowWhenUserIdMissing() {
        NotifyOutbox row = new NotifyOutbox();
        row.setPayload("{\"taskId\":200,\"failReason\":\"x\"}");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> handler.dispatch(row));
        assertTrue(ex.getMessage().contains("userId"));
        verifyNoInteractions(notifyClient);
    }

    @Test
    void dispatch_shouldParseStringNumericIds() {
        // 兼容 Jackson 把 long 读成 String 的场景
        NotifyOutbox row = new NotifyOutbox();
        row.setPayload("{\"taskId\":\"300\",\"userId\":\"30\",\"failReason\":\"reason\"}");

        handler.dispatch(row);

        verify(notifyClient).notifyFailed(300L, 30L, "reason");
    }

    @Test
    void dispatch_shouldThrowWhenPayloadIsInvalidJson() {
        NotifyOutbox row = new NotifyOutbox();
        row.setPayload("{malformed");

        assertThrows(IllegalArgumentException.class, () -> handler.dispatch(row));
        verifyNoInteractions(notifyClient);
    }

    @Test
    void bizType_shouldReturnConstant() {
        assertEquals("generation_failed", handler.bizType());
    }
}
