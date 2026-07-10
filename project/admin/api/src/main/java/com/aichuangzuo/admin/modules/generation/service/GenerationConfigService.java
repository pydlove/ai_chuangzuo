package com.aichuangzuo.admin.modules.generation.service;

import com.aichuangzuo.admin.modules.generation.dto.request.GenerationConfigUpdateRequest;
import com.aichuangzuo.admin.modules.generation.entity.GenerationConfig;
import com.aichuangzuo.admin.modules.generation.mapper.GenerationConfigMapper;
import com.aichuangzuo.admin.modules.generation.vo.GenerationConfigVO;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.shared.enums.error.AdminGenerationErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 创作运行时配置服务。
 *
 * <p>单行配置（id=1）。内部维护一份内存缓存 {@code cached}，worker 与 admin 端都通过 {@link #getCurrent()} 读取。
 * 缓存每 10s 自动从 DB 刷新一次；admin 端 update 后立即失效缓存。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GenerationConfigService {

    private static final long CACHE_ID = 1L;

    private final GenerationConfigMapper mapper;

    /** 内存缓存（volatile 保证多线程可见）。 */
    private volatile GenerationConfig cached;

    /** 上次从 DB 读的时间（毫秒）。 */
    private volatile long lastLoadedAt = 0L;

    /**
     * 取当前生效的配置（worker / admin 端共用）。
     * 首次或超过 10s 重新从 DB 加载。
     */
    public GenerationConfig getCurrent() {
        long now = System.currentTimeMillis();
        if (cached == null || now - lastLoadedAt > 10_000L) {
            synchronized (this) {
                if (cached == null || now - lastLoadedAt > 10_000L) {
                    refreshFromDb();
                }
            }
        }
        return cached;
    }

    /** 立即从 DB 重新加载缓存（admin update 后调）。 */
    public void refreshFromDb() {
        GenerationConfig fresh = mapper.selectById(CACHE_ID);
        if (fresh == null) {
            throw new BusinessException(AdminGenerationErrorCode.GENERATION_CONFIG_NOT_FOUND);
        }
        cached = fresh;
        lastLoadedAt = System.currentTimeMillis();
    }

    /**
     * 兜底：每 60s 兜一次（即使没有调用方触发）。
     */
    @Scheduled(fixedDelay = 60_000L, initialDelay = 60_000L)
    public void scheduledRefresh() {
        try {
            refreshFromDb();
        } catch (Exception e) {
            log.warn("scheduled 刷新创作配置失败：{}", e.getMessage());
        }
    }

    @Transactional
    public GenerationConfigVO update(GenerationConfigUpdateRequest req, Long adminUserId) {
        refreshFromDb();
        GenerationConfig exist = requireById(CACHE_ID);

        exist.setPoolSize(req.getPoolSize());
        exist.setClaimBatchSize(req.getClaimBatchSize());
        exist.setLeaseMinutes(req.getLeaseMinutes());
        exist.setMaxRetry(req.getMaxRetry());
        exist.setPollIntervalMs(req.getPollIntervalMs());
        exist.setRetentionCron(req.getRetentionCron());
        exist.setWorkerId(req.getWorkerId());
        exist.setLlmRetryMaxAttempts(req.getLlmRetryMaxAttempts());
        exist.setLlmRetryBaseDelayMs(req.getLlmRetryBaseDelayMs());
        exist.setLlmRetryBackoffMultiplier(req.getLlmRetryBackoffMultiplier());
        exist.setRemark(req.getRemark());
        exist.setUpdatedBy(adminUserId == null ? 0L : adminUserId);
        mapper.updateById(exist);

        // 立即刷新缓存，让 worker 下个轮询看到
        refreshFromDb();
        log.info("admin={} 更新创作配置 pool={} batch={} lease={}min maxRetry={} poll={}ms cron={} workerId={} llmRetry={}/{}ms×{}",
                adminUserId, exist.getPoolSize(), exist.getClaimBatchSize(),
                exist.getLeaseMinutes(), exist.getMaxRetry(), exist.getPollIntervalMs(),
                exist.getRetentionCron(), exist.getWorkerId(),
                exist.getLlmRetryMaxAttempts(), exist.getLlmRetryBaseDelayMs(),
                exist.getLlmRetryBackoffMultiplier());
        return toVo(exist);
    }

    public GenerationConfigVO detail() {
        return toVo(requireById(CACHE_ID));
    }

    private GenerationConfig requireById(Long id) {
        GenerationConfig c = mapper.selectById(id);
        if (c == null) {
            throw new BusinessException(AdminGenerationErrorCode.GENERATION_CONFIG_NOT_FOUND);
        }
        return c;
    }

    private GenerationConfigVO toVo(GenerationConfig c) {
        GenerationConfigVO vo = new GenerationConfigVO();
        BeanUtils.copyProperties(c, vo);
        return vo;
    }
}
