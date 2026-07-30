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
class GenerationCompletedHandlerTest {

    @Mock
    private GenerationNotifyInternalClient notifyClient;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private GenerationCompletedHandler handler;

    @Test
    void dispatch_shouldParsePayloadAndNotifyCompleted() {
        NotifyOutbox row = new NotifyOutbox();
        row.setPayload("{\"taskId\":100,\"userId\":10,\"articleBizNo\":\"ART-100\",\"articleTitle\":\"标题\"}");

        handler.dispatch(row);

        verify(notifyClient).notifyCompleted(100L, 10L, "ART-100", "标题");
    }

    @Test
    void dispatch_shouldAllowNullTitle() {
        NotifyOutbox row = new NotifyOutbox();
        row.setPayload("{\"taskId\":101,\"userId\":11,\"articleBizNo\":\"ART-101\"}");

        handler.dispatch(row);

        verify(notifyClient).notifyCompleted(101L, 11L, "ART-101", null);
    }

    @Test
    void dispatch_shouldThrowWhenTaskIdMissing() {
        NotifyOutbox row = new NotifyOutbox();
        row.setPayload("{\"userId\":10,\"articleBizNo\":\"ART-100\"}");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> handler.dispatch(row));
        assertTrue(ex.getMessage().contains("taskId"));
        verifyNoInteractions(notifyClient);
    }

    @Test
    void dispatch_shouldThrowWhenUserIdMissing() {
        NotifyOutbox row = new NotifyOutbox();
        row.setPayload("{\"taskId\":100,\"articleBizNo\":\"ART-100\"}");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> handler.dispatch(row));
        assertTrue(ex.getMessage().contains("userId"));
        verifyNoInteractions(notifyClient);
    }

    @Test
    void dispatch_shouldThrowWhenArticleBizNoMissing() {
        NotifyOutbox row = new NotifyOutbox();
        row.setPayload("{\"taskId\":100,\"userId\":10}");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> handler.dispatch(row));
        assertTrue(ex.getMessage().contains("articleBizNo"));
        verifyNoInteractions(notifyClient);
    }

    @Test
    void dispatch_shouldThrowWhenPayloadIsInvalidJson() {
        NotifyOutbox row = new NotifyOutbox();
        row.setPayload("{not json");

        assertThrows(IllegalArgumentException.class, () -> handler.dispatch(row));
        verifyNoInteractions(notifyClient);
    }

    @Test
    void bizType_shouldReturnConstant() {
        assertEquals("generation_completed", handler.bizType());
    }
}
