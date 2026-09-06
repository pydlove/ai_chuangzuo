package com.aichuangzuo.shared.ai;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.Collections;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ModelConfigSelectorTest {

    @Test
    void next_shouldReturnNullWhenNoActiveConfig() {
        ModelConfigSelector selector = new ModelConfigSelector();
        assertNull(selector.next(List.of()));
        assertNull(selector.next(null));
    }

    @Test
    void next_shouldRoundRobinProvidersFirst() {
        // 2 个供应商，各 1 个 key
        ModelConfigSelector selector = new ModelConfigSelector();
        List<ActiveModelConfig> configs = List.of(config(1L, "kimi"), config(2L, "minimax"));

        assertEquals(1L, selector.next(configs).getId());
        assertEquals(2L, selector.next(configs).getId());
        assertEquals(1L, selector.next(configs).getId());
        assertEquals(2L, selector.next(configs).getId());
    }

    @Test
    void next_shouldRoundRobinKeysWithinProvider() {
        // 1 个供应商，2 个 key
        ModelConfigSelector selector = new ModelConfigSelector();
        List<ActiveModelConfig> configs = List.of(config(1L, "kimi"), config(2L, "kimi"));

        assertEquals(1L, selector.next(configs).getId());
        assertEquals(2L, selector.next(configs).getId());
        assertEquals(1L, selector.next(configs).getId());
    }

    @Test
    void next_shouldProviderFirstThenKey() {
        // 2 个供应商：kimi 有 2 个 key，minimax 有 1 个 key
        ModelConfigSelector selector = new ModelConfigSelector();
        List<ActiveModelConfig> configs = List.of(
                config(1L, "kimi"), config(2L, "kimi"), config(3L, "minimax"));

        List<Long> sequence = IntStream.range(0, 6)
                .mapToObj(i -> selector.next(configs).getId())
                .toList();
        // 期望：kimi1, minimax1, kimi2, minimax1, kimi1, minimax1
        assertEquals(List.of(1L, 3L, 2L, 3L, 1L, 3L), sequence);
    }

    @Test
    void next_shouldBeThreadSafe() throws InterruptedException {
        ModelConfigSelector selector = new ModelConfigSelector();
        List<ActiveModelConfig> configs = List.of(
                config(1L, "kimi"), config(2L, "kimi"), config(3L, "minimax"));

        int threads = 4;
        int callsPerThread = 100;
        CountDownLatch latch = new CountDownLatch(threads);
        List<Long> collected = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                for (int j = 0; j < callsPerThread; j++) {
                    collected.add(selector.next(configs).getId());
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

    private ActiveModelConfig config(Long id, String providerType) {
        ActiveModelConfig cfg = new ActiveModelConfig();
        cfg.setId(id);
        cfg.setProviderType(providerType);
        return cfg;
    }
}
