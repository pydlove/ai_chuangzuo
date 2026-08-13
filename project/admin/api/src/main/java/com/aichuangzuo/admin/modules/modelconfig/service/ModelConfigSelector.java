package com.aichuangzuo.admin.modules.modelconfig.service;

import com.aichuangzuo.admin.modules.modelconfig.entity.ModelConfig;
import com.aichuangzuo.admin.modules.modelconfig.mapper.ModelConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 创作队列模型配置选择器：按「供应商轮询 → key 轮询」从启用配置中选出下一个 modelConfigId。
 *
 * <p>设计目标：固定线程池里的多个 worker 线程并发处理文章时，能分散到不同供应商、
 * 不同 key，避免单 key 串行成为瓶颈。所有线程共享计数器，因此跨线程也保持轮询效果。
 *
 * <p>优先级规则：
 * <ul>
 *   <li>先按供应商分组，组间轮询（数字越小越优先的供应商先被轮到，同优先级按 id 稳定）</li>
 *   <li>组内再按 key 轮询（同供应商多 key 时依次使用）</li>
 * </ul>
 *
 * <p>没有启用配置时返回 {@code null}，调用方应视为模型不可用。
 */
@Service
@RequiredArgsConstructor
public class ModelConfigSelector {

    private final ModelConfigMapper modelConfigMapper;

    private final AtomicInteger providerRound = new AtomicInteger(0);
    private final ConcurrentHashMap<String, AtomicInteger> keyRound = new ConcurrentHashMap<>();

    /**
     * 从启用配置中按「供应商轮询 → key 轮询」选出下一个 modelConfigId。
     */
    public Long nextActiveConfigId() {
        List<ModelConfig> configs = modelConfigMapper.selectActiveByPriority();
        if (configs.isEmpty()) {
            return null;
        }

        // 按供应商分组，保留优先级/id 顺序
        Map<String, List<Long>> grouped = new LinkedHashMap<>();
        for (ModelConfig cfg : configs) {
            grouped.computeIfAbsent(cfg.getProviderType(), k -> new ArrayList<>()).add(cfg.getId());
        }

        List<String> providers = new ArrayList<>(grouped.keySet());
        int providerIdx = Math.floorMod(providerRound.getAndIncrement(), providers.size());
        String provider = providers.get(providerIdx);
        List<Long> keys = grouped.get(provider);

        int keyIdx = Math.floorMod(
                keyRound.computeIfAbsent(provider, k -> new AtomicInteger(0)).getAndIncrement(),
                keys.size());
        return keys.get(keyIdx);
    }
}
