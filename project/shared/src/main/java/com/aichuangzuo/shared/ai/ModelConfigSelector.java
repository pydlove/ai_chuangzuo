package com.aichuangzuo.shared.ai;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 模型资源池轮询选择器：按「供应商轮询 → key 轮询」从启用配置中选出下一个。
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
 *
 * <p>admin / user 两端共用同一份实现，由 {@link ModelConfigSelectorAutoConfig} 注册为 Spring bean。
 */
public class ModelConfigSelector {

    private final AtomicInteger providerRound = new AtomicInteger(0);
    private final ConcurrentHashMap<String, AtomicInteger> keyRound = new ConcurrentHashMap<>();

    /**
     * 从启用配置列表中按「供应商轮询 → key 轮询」选出下一个配置。
     *
     * @param configs 调用方查到的启用配置列表（顺序按 priority ASC, id ASC）
     * @return 下一个启用的模型配置；列表为空时返回 {@code null}
     */
    public ActiveModelConfig next(List<ActiveModelConfig> configs) {
        if (configs == null || configs.isEmpty()) {
            return null;
        }

        // 按供应商分组，保留优先级/id 顺序
        Map<String, List<ActiveModelConfig>> grouped = new LinkedHashMap<>();
        for (ActiveModelConfig cfg : configs) {
            grouped.computeIfAbsent(cfg.getProviderType(), k -> new ArrayList<>()).add(cfg);
        }

        List<String> providers = new ArrayList<>(grouped.keySet());
        int providerIdx = Math.floorMod(providerRound.getAndIncrement(), providers.size());
        String provider = providers.get(providerIdx);
        List<ActiveModelConfig> keys = grouped.get(provider);

        int keyIdx = Math.floorMod(
                keyRound.computeIfAbsent(provider, k -> new AtomicInteger(0)).getAndIncrement(),
                keys.size());
        return keys.get(keyIdx);
    }
}
