package com.aichuangzuo.user.modules.selfmedia.service;

import com.aichuangzuo.user.modules.selfmedia.dto.request.SavePlanRequest;
import com.aichuangzuo.user.modules.selfmedia.entity.SelfMediaPlan;
import com.aichuangzuo.user.modules.selfmedia.mapper.SelfMediaPlanMapper;
import com.aichuangzuo.user.modules.selfmedia.service.impl.SelfMediaPlanServiceImpl;
import com.aichuangzuo.user.modules.selfmedia.vo.PillarVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SelfMediaPlanServiceImplTest {

    private final SelfMediaPlanAiService aiService = mock(SelfMediaPlanAiService.class);
    private final SelfMediaPlanMapper planMapper = mock(SelfMediaPlanMapper.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private SelfMediaPlanServiceImpl service() {
        return new SelfMediaPlanServiceImpl(aiService, planMapper, null, objectMapper);
    }

    @Test
    void savePlan_shouldInsertAndReturnVo() {
        SavePlanRequest req = new SavePlanRequest();
        req.setPlatformKey("xiaohongshu");
        req.setPlatformName("小红书");
        req.setGoal("靠生活经验变现");
        req.setBackground("职场/管理");
        req.setHasProduct(false);
        req.setNicheKey("zhichangzhuanxing");
        req.setNicheName("35+ 职场转型");
        req.setPersonaKey("experiencer");
        req.setPersonaName("实战记录者");
        PillarVO p = new PillarVO();
        p.setName("干货复盘");
        p.setPercent(60);
        req.setPillars(List.of(p));

        when(planMapper.selectByUserId(1L)).thenReturn(null);

        service().savePlan(1L, req);

        ArgumentCaptor<SelfMediaPlan> captor = ArgumentCaptor.forClass(SelfMediaPlan.class);
        verify(planMapper).insert(captor.capture());
        SelfMediaPlan inserted = captor.getValue();

        assertEquals(1L, inserted.getUserId());
        assertEquals("xiaohongshu", inserted.getPlatformKey());
        assertEquals("小红书", inserted.getPlatformName());
        assertEquals("靠生活经验变现", inserted.getGoal());
        assertEquals("zhichangzhuanxing", inserted.getNicheKey());
        assertEquals("experiencer", inserted.getPersonaKey());
        assertEquals(0, inserted.getHasProduct());
        assertEquals(0, inserted.getIsRecommendedByAi());
        assertNotNull(inserted.getContentPillarsJson());
    }
}
