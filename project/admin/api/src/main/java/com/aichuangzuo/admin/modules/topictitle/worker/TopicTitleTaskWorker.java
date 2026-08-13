package com.aichuangzuo.admin.modules.topictitle.worker;

import com.aichuangzuo.admin.modules.topictitle.entity.TopicTitleTask;
import com.aichuangzuo.admin.modules.topictitle.mapper.TopicTitleTaskMapper;
import com.aichuangzuo.admin.modules.topictitle.service.TopicTitleService;
import com.aichuangzuo.admin.modules.scheduler.annotation.ScheduledTask;
import com.aichuangzuo.admin.modules.scheduler.executor.ScheduledTaskExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

/**
 * 标题生成任务 worker。
 *
 * <p>用 application 级 @Scheduled(fixedDelay=1000) 每秒扫一次 t_topic_title_task，
 * 抢到一条 QUEUED → 丢到独立 {@code topicTitleTaskExecutor} 执行真正的 AI 调用。
 *
 * <p>为什么 @Scheduled 里只做「抢任务 + submit」而不直接 await AI？
 * 因为 GenerationSchedulerConfig 的 ThreadPoolTaskScheduler 是单线程（pool=1），
 * 跑 AI 可能几十秒，会阻塞其他 cron job。所以 AI 调用必须丢到独立 executor。
 */
@Slf4j
@Component
public class TopicTitleTaskWorker {

    private final TopicTitleTaskMapper taskMapper;
    private final TopicTitleService topicTitleService;
    private final Executor executor;
    private final ScheduledTaskExecutor scheduledTaskExecutor;

    public TopicTitleTaskWorker(TopicTitleTaskMapper taskMapper,
                                TopicTitleService topicTitleService,
                                @Qualifier("topicTitleTaskExecutor") Executor executor,
                                ScheduledTaskExecutor scheduledTaskExecutor) {
        this.taskMapper = taskMapper;
        this.topicTitleService = topicTitleService;
        this.executor = executor;
        this.scheduledTaskExecutor = scheduledTaskExecutor;
    }

    @ScheduledTask(key = "topic_title_task_poll", name = "AI 标题生成任务轮询", description = "每秒从 t_topic_title_task 抢一条 QUEUED 任务并提交 AI 执行", triggerType = "fixed_delay", expression = "1000", sortOrder = 80)
    @Scheduled(fixedDelay = 1000)
    public void poll() {
        scheduledTaskExecutor.executeAuto("topic_title_task_poll", () -> {
            TopicTitleTask next;
            try {
                next = taskMapper.selectNextQueued();
            } catch (Exception e) {
                log.warn("TopicTitleTaskWorker poll DB 失败: {}", e.getMessage());
                return;
            }
            if (next == null) {
                return;
            }
            log.info("TopicTitleTaskWorker 抢到任务 taskId={} count={}", next.getId(), next.getCount());
            executor.execute(() -> {
                try {
                    topicTitleService.executeTask(next.getId());
                } catch (Exception e) {
                    log.error("TopicTitleTaskWorker 执行 taskId={} 异常: {}",
                            next.getId(), e.getMessage(), e);
                }
            });
        });
    }
}