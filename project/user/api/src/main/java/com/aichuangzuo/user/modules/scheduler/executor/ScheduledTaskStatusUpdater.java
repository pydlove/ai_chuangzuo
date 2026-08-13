package com.aichuangzuo.user.modules.scheduler.executor;

import com.aichuangzuo.user.modules.scheduler.entity.ScheduledTaskEntity;
import com.aichuangzuo.user.modules.scheduler.mapper.ScheduledTaskMapper;
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

    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
