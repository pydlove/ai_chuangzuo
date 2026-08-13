package com.aichuangzuo.admin.modules.message.job;

import com.aichuangzuo.admin.modules.message.entity.NotifyOutbox;
import com.aichuangzuo.admin.modules.message.handler.MessageNotifyHandler;
import com.aichuangzuo.admin.modules.message.mapper.NotifyOutboxMapper;
import com.aichuangzuo.admin.modules.scheduler.executor.ScheduledTaskExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotifyOutboxDispatcherJobTest {

    @Mock
    private NotifyOutboxMapper outboxMapper;

    @Mock
    private ScheduledTaskExecutor scheduledTaskExecutor;

    @InjectMocks
    private NotifyOutboxDispatcherJob job;

    private void stubExecutor() {
        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(1);
            task.run();
            return null;
        }).when(scheduledTaskExecutor).executeAuto(any(), any(Runnable.class));
    }

    private NotifyOutbox row(String bizType, int retryCount) {
        NotifyOutbox row = new NotifyOutbox();
        row.setId(1L);
        row.setBizType(bizType);
        row.setRetryCount(retryCount);
        row.setNextRetryAt(LocalDateTime.now());
        return row;
    }

    @Test
    void dispatch_shouldRouteToHandlerAndMarkSent() {
        stubExecutor();
        NotifyOutbox row = row("generation_completed", 0);
        MessageNotifyHandler handler = mock(MessageNotifyHandler.class);
        when(handler.bizType()).thenReturn("generation_completed");
        when(outboxMapper.selectPending(any(LocalDateTime.class), eq(50))).thenReturn(List.of(row));

        job = new NotifyOutboxDispatcherJob(outboxMapper, List.of(handler), scheduledTaskExecutor);
        job.dispatch();

        verify(handler).dispatch(row);
        verify(outboxMapper).markSent(eq(1L), any(LocalDateTime.class));
        verify(outboxMapper, never()).markFailed(eq(1L), any());
        verify(outboxMapper, never()).scheduleRetry(eq(1L), any(), any());
    }

    @Test
    void dispatch_shouldMarkFailedForUnknownBizType() {
        stubExecutor();
        NotifyOutbox row = row("unknown_biz", 0);
        when(outboxMapper.selectPending(any(LocalDateTime.class), eq(50))).thenReturn(List.of(row));

        job = new NotifyOutboxDispatcherJob(outboxMapper, Collections.emptyList(), scheduledTaskExecutor);
        job.dispatch();

        verify(outboxMapper).markFailed(eq(1L), eq("未知 biz_type: unknown_biz"));
        verify(outboxMapper, never()).markSent(eq(1L), any());
    }

    @Test
    void dispatch_shouldScheduleRetryOnHandlerException() {
        stubExecutor();
        NotifyOutbox row = row("generation_failed", 1);
        MessageNotifyHandler handler = mock(MessageNotifyHandler.class);
        when(handler.bizType()).thenReturn("generation_failed");
        doThrow(new RuntimeException("user-api timeout")).when(handler).dispatch(row);
        when(outboxMapper.selectPending(any(LocalDateTime.class), eq(50))).thenReturn(List.of(row));

        job = new NotifyOutboxDispatcherJob(outboxMapper, List.of(handler), scheduledTaskExecutor);
        LocalDateTime before = LocalDateTime.now();
        job.dispatch();
        LocalDateTime after = LocalDateTime.now();

        verify(handler).dispatch(row);
        verify(outboxMapper, never()).markSent(eq(1L), any());
        verify(outboxMapper, never()).markFailed(eq(1L), any());

        // 第 2 次失败（retryCount 从 1 变 2），下次重试在 5 * 2^2 = 20s 后
        org.mockito.ArgumentCaptor<LocalDateTime> captor = org.mockito.ArgumentCaptor.forClass(LocalDateTime.class);
        verify(outboxMapper).scheduleRetry(eq(1L), captor.capture(), eq("user-api timeout"));
        LocalDateTime nextRetry = captor.getValue();
        LocalDateTime expectedMin = before.plus(20, ChronoUnit.SECONDS);
        LocalDateTime expectedMax = after.plus(20, ChronoUnit.SECONDS);
        assertTrue(!nextRetry.isBefore(expectedMin) && !nextRetry.isAfter(expectedMax));
    }

    @Test
    void dispatch_shouldMarkFailedWhenMaxRetryReached() {
        stubExecutor();
        NotifyOutbox row = row("generation_failed", 4);
        MessageNotifyHandler handler = mock(MessageNotifyHandler.class);
        when(handler.bizType()).thenReturn("generation_failed");
        doThrow(new RuntimeException("final failure")).when(handler).dispatch(row);
        when(outboxMapper.selectPending(any(LocalDateTime.class), eq(50))).thenReturn(List.of(row));

        job = new NotifyOutboxDispatcherJob(outboxMapper, List.of(handler), scheduledTaskExecutor);
        job.dispatch();

        verify(handler, times(1)).dispatch(row);
        verify(outboxMapper).markFailed(eq(1L), eq("final failure"));
        verify(outboxMapper, never()).scheduleRetry(eq(1L), any(), any());
    }

    @Test
    void dispatch_shouldProcessMultipleRowsIndependently() {
        stubExecutor();
        NotifyOutbox rowA = row("generation_completed", 0);
        rowA.setId(1L);
        NotifyOutbox rowB = row("generation_failed", 0);
        rowB.setId(2L);
        MessageNotifyHandler handlerA = mock(MessageNotifyHandler.class);
        when(handlerA.bizType()).thenReturn("generation_completed");
        MessageNotifyHandler handlerB = mock(MessageNotifyHandler.class);
        when(handlerB.bizType()).thenReturn("generation_failed");
        doThrow(new RuntimeException("boom")).when(handlerB).dispatch(rowB);
        when(outboxMapper.selectPending(any(LocalDateTime.class), eq(50))).thenReturn(List.of(rowA, rowB));

        job = new NotifyOutboxDispatcherJob(outboxMapper, List.of(handlerA, handlerB), scheduledTaskExecutor);
        job.dispatch();

        verify(handlerA).dispatch(rowA);
        verify(handlerB).dispatch(rowB);
        verify(outboxMapper).markSent(eq(1L), any(LocalDateTime.class));
        verify(outboxMapper).scheduleRetry(eq(2L), any(LocalDateTime.class), eq("boom"));
    }

    @Test
    void dispatch_shouldDoNothingWhenNoPendingRows() {
        stubExecutor();
        when(outboxMapper.selectPending(any(LocalDateTime.class), eq(50))).thenReturn(Collections.emptyList());

        job = new NotifyOutboxDispatcherJob(outboxMapper, Collections.emptyList(), scheduledTaskExecutor);
        job.dispatch();

        verify(outboxMapper, never()).markSent(any(), any());
        verify(outboxMapper, never()).markFailed(any(), any());
        verify(outboxMapper, never()).scheduleRetry(any(), any(), any());
    }
}
