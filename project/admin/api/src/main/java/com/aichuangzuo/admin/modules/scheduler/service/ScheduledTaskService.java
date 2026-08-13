package com.aichuangzuo.admin.modules.scheduler.service;

import com.aichuangzuo.admin.modules.scheduler.client.UserScheduledTaskClient;
import com.aichuangzuo.admin.modules.scheduler.entity.ScheduledTaskEntity;
import com.aichuangzuo.admin.modules.scheduler.entity.ScheduledTaskLog;
import com.aichuangzuo.admin.modules.scheduler.executor.ScheduledTaskExecutor;
import com.aichuangzuo.admin.modules.scheduler.mapper.ScheduledTaskLogMapper;
import com.aichuangzuo.admin.modules.scheduler.mapper.ScheduledTaskMapper;
import com.aichuangzuo.admin.modules.scheduler.vo.ScheduledTaskLogVO;
import com.aichuangzuo.admin.modules.scheduler.vo.ScheduledTaskPageVO;
import com.aichuangzuo.admin.modules.scheduler.vo.ScheduledTaskVO;
import com.aichuangzuo.shared.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 定时任务管理服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledTaskService {

    private final ScheduledTaskMapper taskMapper;
    private final ScheduledTaskLogMapper logMapper;
    private final ScheduledTaskExecutor executor;
    private final UserScheduledTaskClient userClient;

    public ScheduledTaskPageVO list(String module, long page, long pageSize) {
        LambdaQueryWrapper<ScheduledTaskEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScheduledTaskEntity::getIsDeleted, 0);
        if (module != null && !module.isBlank()) {
            wrapper.eq(ScheduledTaskEntity::getModule, module);
        }
        wrapper.orderByAsc(ScheduledTaskEntity::getSortOrder);
        List<ScheduledTaskEntity> tasks = taskMapper.selectList(wrapper);

        // 同步用户端任务的最新执行状态
        syncUserTaskStatus(tasks);

        List<ScheduledTaskVO> vos = tasks.stream().map(this::toVo).toList();

        long total = vos.size();
        long start = (page - 1) * pageSize;
        long end = Math.min(start + pageSize, total);
        List<ScheduledTaskVO> pageList = start >= total ? List.of() : vos.subList((int) start, (int) end);

        ScheduledTaskPageVO vo = new ScheduledTaskPageVO();
        vo.setList(pageList);
        vo.setTotal(total);
        vo.setPage(page);
        vo.setPageSize(pageSize);
        return vo;
    }

    private void syncUserTaskStatus(List<ScheduledTaskEntity> tasks) {
        boolean hasUserTask = tasks.stream().anyMatch(t -> "user".equalsIgnoreCase(t.getModule()));
        if (!hasUserTask) {
            return;
        }
        try {
            List<ScheduledTaskVO> userTasks = userClient.listTasks();
            Map<String, ScheduledTaskVO> userTaskMap = userTasks.stream()
                    .collect(Collectors.toMap(ScheduledTaskVO::getTaskKey, t -> t));
            for (ScheduledTaskEntity task : tasks) {
                if (!"user".equalsIgnoreCase(task.getModule())) {
                    continue;
                }
                ScheduledTaskVO remote = userTaskMap.get(task.getTaskKey());
                if (remote != null) {
                    task.setLastRunAt(remote.getLastRunAt());
                    task.setLastRunStatus(remote.getLastRunStatus());
                    task.setLastRunMessage(remote.getLastRunMessage());
                    task.setEnabled(remote.getEnabled());
                }
            }
        } catch (Exception e) {
            log.warn("同步用户端定时任务状态失败：{}", e.getMessage());
        }
    }

    public Long trigger(Long taskId, Long adminId) {
        ScheduledTaskEntity task = taskMapper.selectById(taskId);
        if (task == null || task.getIsDeleted() == 1) {
            throw new BusinessException(404, "任务不存在");
        }
        if (task.getEnabled() == null || task.getEnabled() == 0) {
            throw new BusinessException(400, "任务已禁用");
        }

        if ("user".equalsIgnoreCase(task.getModule())) {
            return userClient.triggerTask(task.getTaskKey(), adminId);
        }
        return executor.triggerManual(task, adminId);
    }

    public List<ScheduledTaskLogVO> logs(Long taskId, long limit) {
        LambdaQueryWrapper<ScheduledTaskLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScheduledTaskLog::getTaskId, taskId);
        wrapper.orderByDesc(ScheduledTaskLog::getStartedAt);
        Page<ScheduledTaskLog> page = new Page<>(1, limit);
        List<ScheduledTaskLog> records = logMapper.selectPage(page, wrapper).getRecords();
        return records.stream().map(this::toLogVo).collect(Collectors.toList());
    }

    private ScheduledTaskVO toVo(ScheduledTaskEntity entity) {
        ScheduledTaskVO vo = new ScheduledTaskVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    private ScheduledTaskLogVO toLogVo(ScheduledTaskLog entity) {
        ScheduledTaskLogVO vo = new ScheduledTaskLogVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
