package com.aichuangzuo.user.modules.scheduler.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记一个方法为可被管理端触发或查询的定时任务。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ScheduledTask {

    String key();

    String name();

    String description() default "";

    String triggerType();

    String expression();

    int sortOrder() default 0;
}
