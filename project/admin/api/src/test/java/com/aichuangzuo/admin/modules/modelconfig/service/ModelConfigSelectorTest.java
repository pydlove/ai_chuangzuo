package com.aichuangzuo.admin.modules.modelconfig.service;

import com.aichuangzuo.admin.modules.modelconfig.entity.ModelConfig;
import com.aichuangzuo.admin.modules.modelconfig.mapper.ModelConfigMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModelConfigSelectorTest {

    @Mock
    private ModelConfigMapper modelConfigMapper;

    @InjectMocks
    private ModelConfigSelector selector;

    @Test
    void nextActiveConfigId_shouldReturnNullWhenNoActiveConfig() {
        when(modelConfigMapper.selectActiveByPriority()).thenReturn(List.of());

        assertNull(selector.nextActiveConfigId());
    }

    @Test
    void nextActiveConfigId_shouldRoundRobinProvidersFirst() {
        // 2 个供应商，各 1 个 key
        ModelConfig kimi = config(1L, "kimi", 0);
        ModelConfig minimax = config(2L, "minimax", 0);
        when(modelConfigMapper.selectActiveByPriority()).thenReturn(List.of(kimi, minimax));

        assertEquals(1L, selector.nextActiveConfigId());
        assertEquals(2L, selector.nextActiveConfigId());
        assertEquals(1L, selector.nextActiveConfigId());
        assertEquals(2L, selector.nextActiveConfigId());
    }

    @Test
    void nextActiveConfigId_shouldRoundRobinKeysWithinProvider() {
        // 1 个供应商，2 个 key
        ModelConfig k1 = config(1L, "kimi", 0);
        ModelConfig k2 = config(2L, "kimi", 1);
        when(modelConfigMapper.selectActiveByPriority()).thenReturn(List.of(k1, k2));

        assertEquals(1L, selector.nextActiveConfigId());
        assertEquals(2L, selector.nextActiveConfigId());
        assertEquals(1L, selector.nextActiveConfigId());
    }

    @Test
    void nextActiveConfigId_shouldProviderFirstThenKey() {
        // 2 个供应商：kimi 有 2 个 key，minimax 有 1 个 key
        ModelConfig kimi1 = config(1L, "kimi", 0);
        ModelConfig kimi2 = config(2L, "kimi", 1);
        ModelConfig minimax1 = config(3L, "minimax", 0);
        when(modelConfigMapper.selectActiveByPriority()).thenReturn(List.of(kimi1, kimi2, minimax1));

        List<Long> sequence = IntStream.range(0, 6)
                .mapToObj(i -> selector.nextActiveConfigId())
                .toList();
        // 期望：kimi1, minimax1, kimi2, minimax1, kimi1, minimax1
        assertEquals(List.of(1L, 3L, 2L, 3L, 1L, 3L), sequence);
    }

    @Test
    void nextActiveConfigId_shouldBeThreadSafe() throws InterruptedException {
        ModelConfig kimi1 = config(1L, "kimi", 0);
        ModelConfig kimi2 = config(2L, "kimi", 1);
        ModelConfig minimax1 = config(3L, "minimax", 0);
        when(modelConfigMapper.selectActiveByPriority()).thenReturn(List.of(kimi1, kimi2, minimax1));

        int threads = 4;
        int callsPerThread = 100;
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(threads);
        java.util.List<Long> collected = java.util.Collections.synchronizedList(new java.util.ArrayList<>());

        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                for (int j = 0; j < callsPerThread; j++) {
                    collected.add(selector.nextActiveConfigId());
                }
                latch.countDown();
            }).start();
        }
        latch.await();

        Map<Long, Long> counts = collected.stream().collect(Collectors.groupingBy(k -> k, Collectors.counting()));
        // 4 线程 * 100 次 = 400 次；providerRound 被均分给 2 个供应商，kimi 的 keyRound 被均分给 2 个 key
        assertEquals(400L, counts.values().stream().mapToLong(Long::longValue).sum());
        assertEquals(100L, counts.get(1L));
        assertEquals(100L, counts.get(2L));
        assertEquals(200L, counts.get(3L));
    }

    private ModelConfig config(Long id, String providerType, int priority) {
        ModelConfig cfg = new ModelConfig();
        cfg.setId(id);
        cfg.setProviderType(providerType);
        cfg.setPriority(priority);
        cfg.setIsActive(1);
        cfg.setIsDeleted(0);
        return cfg;
    }
}
