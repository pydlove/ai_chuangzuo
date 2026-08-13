package com.aichuangzuo.admin.modules.scheduler.executor;

import com.aichuangzuo.admin.modules.scheduler.entity.ScheduledTaskEntity;
import com.aichuangzuo.admin.modules.scheduler.entity.ScheduledTaskLog;
import com.aichuangzuo.admin.modules.scheduler.mapper.ScheduledTaskLogMapper;
import com.aichuangzuo.admin.modules.scheduler.mapper.ScheduledTaskMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 定时任务最后执行状态更新器。
 * 使用独立事务，避免被业务事务回滚导致状态丢失。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledTaskStatusUpdater {

    private final ScheduledTaskMapper taskMapper;
    private final ScheduledTaskLogMapper logMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void updateLastRun(Long taskId, String status, String message) {
        ScheduledTaskEntity task = taskMapper.selectById(taskId);
        if (task == null) {
            return;
        }
        task.setLastRunAt(LocalDateTime.now());
        task.setLastRunStatus(status);
        task.setLastRunMessage(truncate(message));
        taskMapper.updateById(task);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public ScheduledTaskEntity findByTaskKey(String taskKey) {
        return taskMapper.selectOne(
                new LambdaQueryWrapper<ScheduledTaskEntity>().eq(ScheduledTaskEntity::getTaskKey, taskKey));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public Long insertLog(ScheduledTaskLog record) {
        logMapper.insert(record);
        return record.getId();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void finishLog(Long logId, boolean success, String message) {
        ScheduledTaskLog record = logMapper.selectById(logId);
        if (record == null) {
            return;
        }
        record.setFinishedAt(LocalDateTime.now());
        record.setRunStatus(success ? "success" : "failed");
        record.setMessage(truncate(message));
        logMapper.updateById(record);
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
