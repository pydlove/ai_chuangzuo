package com.aichuangzuo.admin.modules.scheduler.config;

import com.aichuangzuo.admin.modules.scheduler.entity.ScheduledTaskEntity;
import com.aichuangzuo.admin.modules.scheduler.mapper.ScheduledTaskMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 在管理端数据库登记用户端定时任务元数据，便于统一展示和管理。
 * 这些任务的实际执行在用户端，管理端只负责展示和转发手动触发请求。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RemoteScheduledTaskInitializer {

    private final ScheduledTaskMapper taskMapper;

    @PostConstruct
    public void init() {
        upsert("membership_pending_activation", "待生效会员激活",
                "每天凌晨 2 点扫描 u_user_membership_pending 中待生效记录并激活会员",
                "cron", "0 0 2 * * ?", 10);
    }

    private void upsert(String key, String name, String description, String triggerType, String expression, int sortOrder) {
        ScheduledTaskEntity exist = taskMapper.selectOne(
                new LambdaQueryWrapper<ScheduledTaskEntity>().eq(ScheduledTaskEntity::getTaskKey, key));
        if (exist != null) {
            exist.setTaskName(name);
            exist.setDescription(description);
            exist.setTriggerType(triggerType);
            exist.setExpression(expression);
            exist.setSortOrder(sortOrder);
            taskMapper.updateById(exist);
            return;
        }
        ScheduledTaskEntity task = new ScheduledTaskEntity();
        task.setTaskKey(key);
        task.setTaskName(name);
        task.setDescription(description);
        task.setModule("user");
        task.setTriggerType(triggerType);
        task.setExpression(expression);
        task.setBeanName("-");
        task.setMethodName("-");
        task.setEnabled(1);
        task.setSortOrder(sortOrder);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        task.setCreatedBy(0L);
        task.setUpdatedBy(0L);
        task.setIsDeleted(0);
        taskMapper.insert(task);
        log.info("登记用户端定时任务 key={}", key);
    }
}
