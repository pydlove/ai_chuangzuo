package com.aichuangzuo.admin.modules.generation.service;

import com.aichuangzuo.admin.modules.generation.dto.request.GenerationConfigUpdateRequest;
import com.aichuangzuo.admin.modules.generation.entity.GenerationConfig;
import com.aichuangzuo.admin.modules.generation.mapper.GenerationConfigMapper;
import com.aichuangzuo.admin.modules.generation.vo.GenerationConfigVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerationConfigServiceTest {

    @Mock
    private GenerationConfigMapper mapper;

    @InjectMocks
    private GenerationConfigService service;

    private GenerationConfig sampleConfig() {
        GenerationConfig c = new GenerationConfig();
        c.setId(1L);
        c.setPoolSize(2);
        c.setClaimBatchSize(1);
        c.setLeaseMinutes(5);
        c.setPollIntervalMs(500);
        c.setRetentionCron("0 0 3 * * ?");
        c.setWorkerId("worker-1");
        return c;
    }

    @Test
    void getCurrent_shouldLoadFromDbOnFirstCall() {
        when(mapper.selectById(1L)).thenReturn(sampleConfig());

        GenerationConfig cfg = service.getCurrent();

        assertNotNull(cfg);
        assertEquals(2, cfg.getPoolSize());
        verify(mapper).selectById(1L);
    }

    @Test
    void getCurrent_shouldUseCacheOnSecondCallWithin10s() {
        when(mapper.selectById(1L)).thenReturn(sampleConfig());

        service.getCurrent();
        service.getCurrent();
        service.getCurrent();

        // 10s 内只读一次 DB
        verify(mapper, org.mockito.Mockito.times(1)).selectById(1L);
    }

    @Test
    void update_shouldPersistAllFieldsAndRefreshCache() {
        when(mapper.selectById(1L)).thenReturn(sampleConfig());

        GenerationConfigUpdateRequest req = new GenerationConfigUpdateRequest();
        req.setPoolSize(4);
        req.setClaimBatchSize(2);
        req.setLeaseMinutes(10);
        req.setPollIntervalMs(800);
        req.setRetentionCron("0 0 4 * * ?");
        req.setWorkerId("worker-2");
        req.setRemark("test update");

        GenerationConfigVO vo = service.update(req, 99L);

        assertNotNull(vo);
        assertEquals(4, vo.getPoolSize());
        assertEquals(2, vo.getClaimBatchSize());
        assertEquals(10, vo.getLeaseMinutes());
        assertEquals(800, vo.getPollIntervalMs());
        assertEquals("0 0 4 * * ?", vo.getRetentionCron());
        assertEquals("worker-2", vo.getWorkerId());

        ArgumentCaptor<GenerationConfig> captor = ArgumentCaptor.forClass(GenerationConfig.class);
        verify(mapper).updateById(captor.capture());
        assertEquals(Long.valueOf(99L), captor.getValue().getUpdatedBy());

        // update 内部还会再 select 一次（refreshFromDb）
        verify(mapper, org.mockito.Mockito.atLeast(2)).selectById(eq(1L));
    }

    @Test
    void update_shouldFallbackAdminIdToZero() {
        when(mapper.selectById(1L)).thenReturn(sampleConfig());

        GenerationConfigUpdateRequest req = new GenerationConfigUpdateRequest();
        req.setPoolSize(2);
        req.setClaimBatchSize(1);
        req.setLeaseMinutes(5);
        req.setPollIntervalMs(500);
        req.setRetentionCron("0 0 3 * * ?");
        req.setWorkerId("worker-1");

        service.update(req, null);

        ArgumentCaptor<GenerationConfig> captor = ArgumentCaptor.forClass(GenerationConfig.class);
        verify(mapper).updateById(captor.capture());
        assertEquals(Long.valueOf(0L), captor.getValue().getUpdatedBy());
    }

    @Test
    void detail_shouldReturnVoForExistingConfig() {
        when(mapper.selectById(1L)).thenReturn(sampleConfig());

        GenerationConfigVO vo = service.detail();

        assertNotNull(vo);
        assertEquals(2, vo.getPoolSize());
    }
}
