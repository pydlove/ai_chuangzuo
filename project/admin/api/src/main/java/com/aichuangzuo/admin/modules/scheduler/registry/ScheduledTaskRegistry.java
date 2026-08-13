package com.aichuangzuo.admin.modules.scheduler.registry;

import com.aichuangzuo.admin.modules.scheduler.annotation.ScheduledTask;
import com.aichuangzuo.admin.modules.scheduler.entity.ScheduledTaskEntity;
import com.aichuangzuo.admin.modules.scheduler.mapper.ScheduledTaskMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 定时任务注册中心。
 * 启动时扫描所有 Spring Bean 上标记了 {@link ScheduledTask} 的方法，并将元数据写入 a_scheduled_task 表。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledTaskRegistry {

    private final ApplicationContext applicationContext;
    private final ScheduledTaskMapper taskMapper;

    @Getter
    private final Map<String, TaskHolder> holders = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            Object bean;
            try {
                bean = applicationContext.getBean(beanName);
            } catch (Exception e) {
                continue;
            }
            Class<?> clazz = bean.getClass();
            for (Method method : clazz.getDeclaredMethods()) {
                ScheduledTask annotation = AnnotatedElementUtils.findMergedAnnotation(method, ScheduledTask.class);
                if (annotation == null) {
                    continue;
                }
                if (!StringUtils.hasText(annotation.key())) {
                    log.warn("定时任务 key 为空，忽略 bean={} method={}", beanName, method.getName());
                    continue;
                }
                String key = annotation.key();
                holders.put(key, new TaskHolder(beanName, method, bean));
                upsertTask(key, annotation, beanName, method.getName());
                log.info("注册定时任务 key={} bean={} method={}", key, beanName, method.getName());
            }
        }
    }

    private void upsertTask(String key, ScheduledTask annotation, String beanName, String methodName) {
        ScheduledTaskEntity exist = taskMapper.selectOne(
                new LambdaQueryWrapper<ScheduledTaskEntity>().eq(ScheduledTaskEntity::getTaskKey, key));
        if (exist != null) {
            exist.setTaskName(annotation.name());
            exist.setDescription(annotation.description());
            exist.setTriggerType(annotation.triggerType());
            exist.setExpression(annotation.expression());
            exist.setBeanName(beanName);
            exist.setMethodName(methodName);
            exist.setSortOrder(annotation.sortOrder());
            taskMapper.updateById(exist);
            return;
        }
        ScheduledTaskEntity task = new ScheduledTaskEntity();
        task.setTaskKey(key);
        task.setTaskName(annotation.name());
        task.setDescription(annotation.description());
        task.setModule("admin");
        task.setTriggerType(annotation.triggerType());
        task.setExpression(annotation.expression());
        task.setBeanName(beanName);
        task.setMethodName(methodName);
        task.setEnabled(1);
        task.setSortOrder(annotation.sortOrder());
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        task.setCreatedBy(0L);
        task.setUpdatedBy(0L);
        task.setIsDeleted(0);
        taskMapper.insert(task);
    }

    public TaskHolder getHolder(String key) {
        return holders.get(key);
    }

    public record TaskHolder(String beanName, Method method, Object bean) {
    }
}
