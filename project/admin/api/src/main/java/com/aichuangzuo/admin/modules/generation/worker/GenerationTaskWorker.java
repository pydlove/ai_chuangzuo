package com.aichuangzuo.admin.modules.generation.worker;

import com.aichuangzuo.admin.modules.generation.entity.GenerationConfig;
import com.aichuangzuo.admin.modules.generation.pipeline.GenerationContext;
import com.aichuangzuo.admin.modules.generation.pipeline.GenerationPipeline;
import com.aichuangzuo.admin.modules.generation.service.GenerationConfigService;
import com.aichuangzuo.admin.modules.generation.service.GenerationTaskService;
import com.aichuangzuo.admin.modules.generation.service.QuotaRefundInternalClient;
import com.aichuangzuo.shared.entity.GenerationTask;
import com.aichuangzuo.shared.enums.GenerationTaskStatus;
import com.aichuangzuo.shared.enums.error.AdminGenerationErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

/**
 * 文章生成 worker：固定线程池 + 轮询。
 *
 * <p>每个 worker 线程循环：
 * <ol>
 *   <li>每轮开头做一次 lease 超时回收（防卡死任务）</li>
 *   <li>从队列表抢占 N 个任务（FOR UPDATE SKIP LOCKED，N 由 {@code claimBatchSize} 决定）</li>
 *   <li>推给 {@link GenerationPipeline} 跑 12 阶段流水线（AI 调 + 规则检测 + 落 article）</li>
 *   <li>睡 {@code pollIntervalMs}</li>
 * </ol>
 *
 * <p>线程池大小 / 拉取数 / lease / 轮询间隔 / workerId 全部走 {@link GenerationConfigService}，
 * 由 admin 端可改。{@code poolSize} 改后需重启生效（重建线程池），其他字段在下个轮询周期生效。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GenerationTaskWorker {

    private final GenerationTaskService taskService;
    private final GenerationPipeline pipeline;
    private final GenerationConfigService configService;
    private final QuotaRefundInternalClient refundClient;
    private final com.aichuangzuo.admin.modules.generation.service.GenerationCallLogService callLogService;

    private volatile int currentPoolSize = -1;
    private ExecutorService pool;
    private final AtomicBoolean running = new AtomicBoolean(false);
    /** 线程编号生成器：日志里区分 worker-1 / worker-2，多线程并行时不再混淆。 */
    private final AtomicInteger threadIdx = new AtomicInteger(0);

    @PostConstruct
    public void start() {
        if (!running.compareAndSet(false, true)) return;
        GenerationConfig cfg = configService.getCurrent();
        startPoolWith(cfg);
    }

    @PreDestroy
    public void stop() {
        if (!running.compareAndSet(true, false)) return;
        log.info("GenerationTaskWorker 关闭...");
        shutdownPool();
    }

    private void startPoolWith(GenerationConfig cfg) {
        int poolSize = cfg.getPoolSize() == null ? 2 : cfg.getPoolSize();
        log.info("GenerationTaskWorker 启动 pool={} batch={} pollMs={} leaseMin={} workerId={} cron={}",
                poolSize, cfg.getClaimBatchSize(), cfg.getPollIntervalMs(),
                cfg.getLeaseMinutes(), cfg.getWorkerId(), cfg.getRetentionCron());
        pool = Executors.newFixedThreadPool(poolSize, r -> {
            Thread t = new Thread(r, "generation-worker-" + threadIdx.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
        currentPoolSize = poolSize;
        for (int i = 0; i < poolSize; i++) {
            pool.submit(this::workerLoop);
        }
    }

    private void shutdownPool() {
        if (pool == null) return;
        pool.shutdown();
        try {
            if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            pool.shutdownNow();
        }
    }

    private void workerLoop() {
        while (running.get()) {
            try {
                GenerationConfig cfg = configService.getCurrent();

                try {
                    taskService.releaseExpiredLeases();
                } catch (Exception ignored) {
                }

                int claimBatch = cfg.getClaimBatchSize() == null ? 1 : cfg.getClaimBatchSize();
                int leaseMinutes = cfg.getLeaseMinutes() == null ? 5 : cfg.getLeaseMinutes();
                String workerId = cfg.getWorkerId() == null ? "worker-1" : cfg.getWorkerId();
                long pollMs = cfg.getPollIntervalMs() == null ? 500L : cfg.getPollIntervalMs();

                List<GenerationTask> claimed = taskService.claimNext(claimBatch, workerId, leaseMinutes);
                if (claimed.isEmpty()) {
                    sleepQuiet(pollMs);
                    continue;
                }
                for (GenerationTask task : claimed) {
                    try {
                        processOne(task);
                    } catch (Exception e) {
                        log.error("task={} 执行顶层异常（不应抛到这里）：{}", task.getId(), e.getMessage(), e);
                    }
                }
            } catch (Exception loopErr) {
                log.warn("worker 循环异常，sleep 后重试：{}", loopErr.getMessage());
                sleepQuiet(500L);
            }
        }
    }

    /**
     * 处理单个任务：跑 12 阶段 pipeline → 标完成 / 失败 + 退币。
     */
    private void processOne(GenerationTask task) {
        Long taskId = task.getId();
        // 续约心跳参数：用任务自身的 lockedBy（抢占者），lease 时长取当前配置
        GenerationConfig cfg = configService.getCurrent();
        int leaseMin = cfg.getLeaseMinutes() == null ? 5 : cfg.getLeaseMinutes();
        String owner = task.getLockedBy();

        // 后台续约线程：pipeline 运行期间每 60s 续约一次，防止长 AI stage 把 lease 拖过期
        ScheduledExecutorService leaseRenewer = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "lease-renewer-" + taskId);
            t.setDaemon(true);
            return t;
        });
        ScheduledFuture<?> renewalFuture = leaseRenewer.scheduleAtFixedRate(() -> {
            try {
                taskService.renewLease(taskId, owner, leaseMin);
            } catch (Exception e) {
                log.warn("task={} 后台续约失败: {}", taskId, e.getMessage());
            }
        }, 60, 60, TimeUnit.SECONDS);

        // ctx 提前创建：pipeline 抛异常时也要能拿到 AI 调用留痕落库
        final GenerationContext ctx = new GenerationContext();

        // 协作式取消信号：pipeline 每 stage 前调一次。
        // 条件任一满足即中止：任务被删 / 状态不再 PROCESSING / lockedBy 易主或被清空（admin stopTask）。
        // DB 查询失败不中止，避免网络抖动误杀正常任务。
        final String ownerForCheck = owner;
        BooleanSupplier shouldAbort = () -> {
            GenerationTask cur = taskService.findById(taskId);
            if (cur == null) return false;
            if (cur.getStatus() != GenerationTaskStatus.PROCESSING) return true;
            return ownerForCheck == null || !ownerForCheck.equals(cur.getLockedBy());
        };

        try {
            // 进度回调：每个 stage 完成后回写 progress_pct（user 端轮询可见）
            // 并顺手续约 lease——慢模型（MiniMax-M3）全流程可能超过单个 lease 周期，
            // 心跳让活跃 worker 不被同池其他 worker 误判卡死而回收重提（重复处理）
            // 同时增量落库 AI 调用日志，让管理端"执行过程"抽屉在执行中也能看到已完成的阶段
            pipeline.runInto(ctx, task, (tid, pct) -> {
                taskService.updateProgress(tid, pct);
                taskService.renewLease(tid, owner, leaseMin);
                try {
                    callLogService.persistAll(ctx);
                } catch (Exception logEx) {
                    log.warn("task={} 增量写 call log 失败: {}", tid, logEx.getMessage());
                }
            }, shouldAbort);
            if (ctx.getArticleBizNo() == null) {
                throw new BusinessException(AdminGenerationErrorCode.GENERATION_ARTICLE_PERSIST_FAILED);
            }
            // 强制写 100%：避免最后一步（PersistArticleStep weight=2）累加后 ctx.progressPct=100
            // 但回调可能没有恰好触发到 100 的情况（理论上会，这里兜底保证一致性）
            taskService.updateProgress(taskId, 100);
            taskService.markCompleted(taskId, ctx.getArticleBizNo(), owner);
            log.info("task={} 完成 articleBizNo={} aiCalls={} aiFailed={} totalMs={}",
                    taskId, ctx.getArticleBizNo(),
                    ctx.getAiCallUsed(), ctx.getAiCallFailed(), ctx.getAiCallTotalMs());
        } catch (GenerationPipeline.TaskAbortedException e) {
            // 任务被 admin 停止：stopTask 已置 FAILED 并清 lockedBy。
            // 这里不再 markFailed（避免覆盖 stopTask 的 failedReason），也不退币（admin 主动行为）。
            log.info("task={} 被外部停止，pipeline 协作式中止（不再 markFailed / 退币）", taskId);
        } catch (Exception e) {
            log.warn("task={} pipeline 失败: {}", taskId, e.getMessage());
            var after = taskService.markFailed(taskId, e.getMessage(), false, owner);
            if (after.getStatus() == GenerationTaskStatus.FAILED) {
                try {
                    refundClient.refund(taskId, after.getTargetUserId());
                } catch (Exception refundEx) {
                    log.error("task={} 退币失败，需人工介入: {}", taskId, refundEx.getMessage());
                }
            }
        } finally {
            renewalFuture.cancel(false);
            leaseRenewer.shutdown();
            // 落库 AI 调用日志（成功失败都写，便于排查）
            try {
                callLogService.persistAll(ctx);
            } catch (Exception logEx) {
                log.error("task={} 写 call log 失败: {}", taskId, logEx.getMessage());
            }
        }
    }

    private void sleepQuiet(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
