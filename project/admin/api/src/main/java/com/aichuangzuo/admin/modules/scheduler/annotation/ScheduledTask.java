package com.aichuangzuo.admin.modules.scheduler.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记一个方法为可被管理的定时任务。
 * 被标记的方法会被注册到 a_scheduled_task 表，并在管理端展示和手动触发。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ScheduledTask {

    /** 任务唯一标识，全局唯一。 */
    String key();

    /** 任务名称。 */
    String name();

    /** 业务说明。 */
    String description() default "";

    /** 触发类型：cron 或 fixed_delay。 */
    String triggerType();

    /** 触发表达式：cron 表达式或 fixedDelay 毫秒值。 */
    String expression();

    /** 排序号。 */
    int sortOrder() default 0;
}
