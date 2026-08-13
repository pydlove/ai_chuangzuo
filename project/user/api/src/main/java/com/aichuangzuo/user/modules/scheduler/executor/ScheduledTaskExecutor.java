package com.aichuangzuo.user.modules.scheduler.executor;

import com.aichuangzuo.user.modules.scheduler.entity.ScheduledTaskEntity;
import com.aichuangzuo.user.modules.scheduler.entity.ScheduledTaskLog;
import com.aichuangzuo.user.modules.scheduler.mapper.ScheduledTaskLogMapper;
import com.aichuangzuo.user.modules.scheduler.mapper.ScheduledTaskMapper;
import com.aichuangzuo.user.modules.scheduler.registry.ScheduledTaskRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 用户端定时任务执行器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledTaskExecutor {

    private static final ThreadLocal<Boolean> MANUAL_INVOCATION = new ThreadLocal<>();

    private final ScheduledTaskRegistry registry;
    private final ScheduledTaskMapper taskMapper;
    private final ScheduledTaskLogMapper logMapper;
    private final ScheduledTaskStatusUpdater statusUpdater;

    private final ThreadPoolTaskExecutor manualTaskExecutor = createExecutor();

    private static ThreadPoolTaskExecutor createExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("user-scheduled-manual-");
        executor.initialize();
        return executor;
    }

    /**
     * 标记当前线程为手动触发，供自动执行入口识别并跳过重复记录。
     */
    public static void setManualInvocation(boolean manual) {
        if (manual) {
            MANUAL_INVOCATION.set(Boolean.TRUE);
        } else {
            MANUAL_INVOCATION.remove();
        }
    }

    /**
     * 执行自动调度任务，并在独立事务中更新最后执行状态。
     * 手动触发时已设置 {@link #setManualInvocation(true)}，不会重复记录状态。
     *
     * @param taskKey 任务唯一标识
     * @param action  实际业务逻辑
     */
    public void executeAuto(String taskKey, Runnable action) {
        if (Boolean.TRUE.equals(MANUAL_INVOCATION.get())) {
            action.run();
            return;
        }

        ScheduledTaskEntity task = statusUpdater.findByTaskKey(taskKey);
        if (task == null) {
            log.warn("自动任务 key={} 未注册，跳过状态记录", taskKey);
            action.run();
            return;
        }

        statusUpdater.updateLastRun(task.getId(), "running", "执行中");
        try {
            action.run();
            statusUpdater.updateLastRun(task.getId(), "success", "执行成功");
        } catch (Exception e) {
            String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            statusUpdater.updateLastRun(task.getId(), "failed", truncate(msg));
            throw e;
        }
    }

    public Long triggerManual(ScheduledTaskEntity task, Long adminId) {
        ScheduledTaskLog record = new ScheduledTaskLog();
        record.setTaskId(task.getId());
        record.setTriggerType("manual");
        record.setStartedAt(LocalDateTime.now());
        record.setRunStatus("running");
        record.setCreatedBy(adminId == null ? 0L : adminId);
        logMapper.insert(record);

        ScheduledTaskRegistry.TaskHolder holder = registry.getHolder(task.getTaskKey());
        if (holder == null) {
            finishLog(record.getId(), false, "任务未注册或已卸载", task);
            return record.getId();
        }

        manualTaskExecutor.execute(() -> runTask(holder, record.getId(), task));
        return record.getId();
    }

    private void runTask(ScheduledTaskRegistry.TaskHolder holder, Long logId, ScheduledTaskEntity task) {
        statusUpdater.updateLastRun(task.getId(), "running", "执行中");
        setManualInvocation(true);
        try {
            Method method = holder.method();
            Object bean = holder.bean();
            method.setAccessible(true);
            method.invoke(bean);
            finishLog(logId, true, "执行成功", task);
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            log.error("手动触发定时任务失败 key={}", task.getTaskKey(), cause);
            finishLog(logId, false, cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage(), task);
        } finally {
            setManualInvocation(false);
        }
    }

    private void finishLog(Long logId, boolean success, String message, ScheduledTaskEntity task) {
        ScheduledTaskLog record = logMapper.selectById(logId);
        if (record != null) {
            record.setFinishedAt(LocalDateTime.now());
            record.setRunStatus(success ? "success" : "failed");
            record.setMessage(truncate(message));
            logMapper.updateById(record);
        }
        statusUpdater.updateLastRun(task.getId(), success ? "success" : "failed", truncate(message));
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
