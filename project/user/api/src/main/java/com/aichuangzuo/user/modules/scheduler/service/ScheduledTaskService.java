package com.aichuangzuo.user.modules.scheduler.service;

import com.aichuangzuo.user.modules.scheduler.entity.ScheduledTaskEntity;
import com.aichuangzuo.user.modules.scheduler.executor.ScheduledTaskExecutor;
import com.aichuangzuo.user.modules.scheduler.mapper.ScheduledTaskMapper;
import com.aichuangzuo.user.modules.scheduler.registry.ScheduledTaskRegistry;
import com.aichuangzuo.shared.enums.error.SystemErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户端定时任务查询/触发服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledTaskService {

    private final ScheduledTaskMapper taskMapper;
    private final ScheduledTaskExecutor executor;

    public List<ScheduledTaskEntity> list() {
        LambdaQueryWrapper<ScheduledTaskEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScheduledTaskEntity::getIsDeleted, 0);
        wrapper.orderByAsc(ScheduledTaskEntity::getSortOrder);
        return taskMapper.selectList(wrapper);
    }

    public Long trigger(String taskKey, Long adminId) {
        ScheduledTaskEntity task = taskMapper.selectOne(
                new LambdaQueryWrapper<ScheduledTaskEntity>().eq(ScheduledTaskEntity::getTaskKey, taskKey));
        if (task == null || task.getIsDeleted() == 1) {
            throw new BusinessException(SystemErrorCode.RESOURCE_NOT_FOUND.getCode(), "任务不存在");
        }
        if (task.getEnabled() == null || task.getEnabled() == 0) {
            throw new BusinessException(SystemErrorCode.PARAM_VALIDATION_ERROR.getCode(), "任务已禁用");
        }
        return executor.triggerManual(task, adminId);
    }
}
