package com.aichuangzuo.admin.modules.topictitle.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * AI 生成标题异步任务的执行线程池。
 *
 * <p>和 {@code GenerationSchedulerConfig.generationTaskScheduler}
 * (单线程 cron scheduler) 物理隔离：cron job 只负责从 t_topic_title_task 抢任务，
 * 真正跑 AI 调用的工作丢到这个 executor。pool=2 足够，标题生成是低频运维操作。
 */
@Configuration
public class TopicTitleTaskExecutorConfig {

    @Bean("topicTitleTaskExecutor")
    public Executor topicTitleTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("topic-title-task-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}